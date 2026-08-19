import { randomUUID } from "node:crypto";
import { mkdir, mkdtemp, rm } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { spawn } from "node:child_process";
import { definePlugin } from "@paperclipai/plugin-sdk";
const leases = new Map();
const setupSessions = new Map();
const templates = new Map();
const DEFAULT_FAKE_SANDBOX_PATH = "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin";
const FAKE_SANDBOX_SIGKILL_GRACE_MS = 250;
const REDACTED_FAKE_SSH_COMMAND = "ssh sandbox@[fake-setup-host-redacted] -p [fake-port-redacted]";
function parseConfig(raw) {
    return {
        image: typeof raw.image === "string" && raw.image.trim().length > 0 ? raw.image.trim() : "fake:latest",
        timeoutMs: typeof raw.timeoutMs === "number" && Number.isFinite(raw.timeoutMs) ? raw.timeoutMs : 300_000,
        reuseLease: raw.reuseLease === true,
    };
}
async function createLeaseState(input) {
    const rootDir = await mkdtemp(path.join(os.tmpdir(), "paperclip-fake-sandbox-"));
    const remoteCwd = path.join(rootDir, "workspace");
    await mkdir(remoteCwd, { recursive: true });
    const state = {
        providerLeaseId: input.providerLeaseId,
        rootDir,
        remoteCwd,
        image: input.image,
        reuseLease: input.reuseLease,
    };
    leases.set(input.providerLeaseId, state);
    return state;
}
function leaseMetadata(state) {
    return {
        provider: "fake-plugin",
        image: state.image,
        reuseLease: state.reuseLease,
        remoteCwd: state.remoteCwd,
        fakeRootDir: state.rootDir,
    };
}
function encodeRefPart(value) {
    return encodeURIComponent(value).replace(/%/g, "_");
}
function buildSetupProviderLeaseId(environmentId, sessionId) {
    return `fake-plugin-setup://${encodeRefPart(environmentId)}/${encodeRefPart(sessionId)}`;
}
function buildTemplateRef(environmentId, sessionId) {
    return `fake-template:${encodeRefPart(environmentId)}:${encodeRefPart(sessionId)}`;
}
function setupMetadata(state) {
    return {
        provider: "fake-plugin",
        image: state.image,
        sourceTemplateRefRedacted: Boolean(state.sourceTemplateRef),
        setupSessionId: state.sessionId,
        redactedConnectionOnly: true,
        capturedTemplateRefRedacted: Boolean(state.capturedTemplateRef),
    };
}
function setupConnectionSummary() {
    return {
        type: "ssh",
        username: "sandbox",
        hostRedacted: true,
        portRedacted: true,
        commandRedacted: true,
        metadata: {
            placeholderOnly: true,
        },
    };
}
function setupConnectionPayload(expiresAt) {
    return {
        type: "ssh",
        command: REDACTED_FAKE_SSH_COMMAND,
        expiresAt,
        metadata: {
            placeholderOnly: true,
        },
    };
}
function presentSetupSession(state, options) {
    const canConnect = state.status === "waiting_for_user";
    return {
        providerLeaseId: state.providerLeaseId,
        status: state.status,
        connectionSummary: canConnect ? setupConnectionSummary() : null,
        connectionPayload: canConnect && options.includeConnectionPayload ? setupConnectionPayload(state.expiresAt) : null,
        expiresAt: state.expiresAt,
        metadata: setupMetadata(state),
    };
}
function cancelStatusFromReason(reason) {
    if (reason === "timed_out")
        return "timed_out";
    if (reason === "failed")
        return "failed";
    return "cancelled";
}
async function removeLease(providerLeaseId) {
    if (!providerLeaseId)
        return;
    const state = leases.get(providerLeaseId);
    leases.delete(providerLeaseId);
    if (state) {
        await rm(state.rootDir, { recursive: true, force: true });
    }
}
function buildCommandLine(command, args) {
    return [command, ...(args ?? [])].join(" ");
}
function buildCommandEnvironment(explicitEnv) {
    return {
        PATH: explicitEnv?.PATH ?? DEFAULT_FAKE_SANDBOX_PATH,
        ...(explicitEnv ?? {}),
    };
}
async function runCommand(params, timeoutMs) {
    const cwd = typeof params.cwd === "string" && params.cwd.length > 0 ? params.cwd : process.cwd();
    const startedAt = new Date().toISOString();
    return await new Promise((resolve, reject) => {
        const child = spawn(params.command, params.args ?? [], {
            cwd,
            env: buildCommandEnvironment(params.env),
            shell: false,
            stdio: [params.stdin != null ? "pipe" : "ignore", "pipe", "pipe"],
        });
        let stdout = "";
        let stderr = "";
        let timedOut = false;
        let killTimer = null;
        const timer = timeoutMs > 0
            ? setTimeout(() => {
                timedOut = true;
                child.kill("SIGTERM");
                killTimer = setTimeout(() => {
                    child.kill("SIGKILL");
                }, FAKE_SANDBOX_SIGKILL_GRACE_MS);
            }, timeoutMs)
            : null;
        child.stdout?.on("data", (chunk) => {
            stdout += String(chunk);
        });
        child.stderr?.on("data", (chunk) => {
            stderr += String(chunk);
        });
        child.on("error", (error) => {
            if (timer)
                clearTimeout(timer);
            if (killTimer)
                clearTimeout(killTimer);
            reject(error);
        });
        child.on("close", (code, signal) => {
            if (timer)
                clearTimeout(timer);
            if (killTimer)
                clearTimeout(killTimer);
            resolve({
                exitCode: timedOut ? null : code,
                signal,
                timedOut,
                stdout,
                stderr,
                metadata: {
                    startedAt,
                    commandLine: buildCommandLine(params.command, params.args),
                },
            });
        });
        if (params.stdin != null && child.stdin) {
            child.stdin.write(params.stdin);
            child.stdin.end();
        }
    });
}
const plugin = definePlugin({
    async setup(ctx) {
        ctx.logger.info("Fake sandbox provider plugin ready");
    },
    async onHealth() {
        return { status: "ok", message: "Fake sandbox provider plugin healthy" };
    },
    async onEnvironmentValidateConfig(params) {
        const config = parseConfig(params.config);
        return {
            ok: true,
            normalizedConfig: { ...config },
        };
    },
    async onEnvironmentProbe(params) {
        const config = parseConfig(params.config);
        return {
            ok: true,
            summary: `Fake sandbox provider is ready for image ${config.image}.`,
            metadata: {
                provider: "fake-plugin",
                image: config.image,
                timeoutMs: config.timeoutMs,
                reuseLease: config.reuseLease,
            },
        };
    },
    async onEnvironmentAcquireLease(params) {
        const config = parseConfig(params.config);
        const providerLeaseId = config.reuseLease
            ? `fake-plugin://${params.environmentId}`
            : `fake-plugin://${params.runId}/${randomUUID()}`;
        const existing = leases.get(providerLeaseId);
        const state = existing ?? await createLeaseState({
            providerLeaseId,
            image: config.image,
            reuseLease: config.reuseLease,
        });
        return {
            providerLeaseId,
            metadata: {
                ...leaseMetadata(state),
                resumedLease: Boolean(existing),
            },
        };
    },
    async onEnvironmentResumeLease(params) {
        const config = parseConfig(params.config);
        const existing = leases.get(params.providerLeaseId);
        const state = existing ?? await createLeaseState({
            providerLeaseId: params.providerLeaseId,
            image: config.image,
            reuseLease: config.reuseLease,
        });
        return {
            providerLeaseId: state.providerLeaseId,
            metadata: {
                ...leaseMetadata(state),
                resumedLease: true,
            },
        };
    },
    async onEnvironmentReleaseLease(params) {
        const config = parseConfig(params.config);
        if (!config.reuseLease) {
            await removeLease(params.providerLeaseId);
        }
    },
    async onEnvironmentDestroyLease(params) {
        await removeLease(params.providerLeaseId);
    },
    async onEnvironmentRealizeWorkspace(params) {
        const state = params.lease.providerLeaseId
            ? leases.get(params.lease.providerLeaseId)
            : null;
        const remoteCwd = state?.remoteCwd ??
            (typeof params.lease.metadata?.remoteCwd === "string" ? params.lease.metadata.remoteCwd : null) ??
            params.workspace.remotePath ??
            params.workspace.localPath ??
            path.join(os.tmpdir(), "paperclip-fake-sandbox-workspace");
        await mkdir(remoteCwd, { recursive: true });
        return {
            cwd: remoteCwd,
            metadata: {
                provider: "fake-plugin",
                remoteCwd,
            },
        };
    },
    async onEnvironmentExecute(params) {
        const config = parseConfig(params.config);
        return await runCommand(params, params.timeoutMs ?? config.timeoutMs);
    },
    async onEnvironmentStartInteractiveSetup(params) {
        const config = parseConfig(params.config);
        const providerLeaseId = buildSetupProviderLeaseId(params.environmentId, params.sessionId);
        const state = setupSessions.get(providerLeaseId) ?? {
            providerLeaseId,
            environmentId: params.environmentId,
            sessionId: params.sessionId,
            image: config.image,
            sourceTemplateRef: params.sourceTemplateRef ?? null,
            expiresAt: params.expiresAt ?? null,
            status: "waiting_for_user",
            capturedTemplateRef: null,
        };
        setupSessions.set(providerLeaseId, state);
        return presentSetupSession(state, { includeConnectionPayload: true });
    },
    async onEnvironmentGetInteractiveSetup(params) {
        const state = params.providerLeaseId ? setupSessions.get(params.providerLeaseId) : undefined;
        if (!state) {
            return {
                providerLeaseId: params.providerLeaseId,
                status: "missing",
                connectionSummary: null,
                connectionPayload: null,
                metadata: {
                    provider: "fake-plugin",
                    found: false,
                },
            };
        }
        return presentSetupSession(state, { includeConnectionPayload: params.includeConnectionPayload === true });
    },
    async onEnvironmentCaptureTemplate(params) {
        const config = parseConfig(params.config);
        const state = params.providerLeaseId ? setupSessions.get(params.providerLeaseId) : undefined;
        if (!state) {
            throw new Error("Fake setup session not found.");
        }
        if (state.status === "cancelled" || state.status === "timed_out" || state.status === "failed") {
            throw new Error(`Fake setup session cannot be captured from status ${state.status}.`);
        }
        state.status = "capturing";
        const templateRef = buildTemplateRef(state.environmentId, state.sessionId);
        const template = {
            templateRef,
            environmentId: state.environmentId,
            sessionId: state.sessionId,
            image: config.image,
            sourceTemplateRef: params.sourceTemplateRef ?? state.sourceTemplateRef,
            previousTemplateRef: params.previousTemplateRef ?? null,
            deleted: false,
        };
        templates.set(templateRef, template);
        state.status = "promoted";
        state.capturedTemplateRef = templateRef;
        await removeLease(state.providerLeaseId);
        return {
            templateRef,
            templateKind: "snapshot",
            metadata: {
                provider: "fake-plugin",
                image: template.image,
                sourceTemplateRefRedacted: Boolean(template.sourceTemplateRef),
                previousTemplateRefRedacted: Boolean(template.previousTemplateRef),
                setupSessionId: state.sessionId,
                promoted: true,
            },
        };
    },
    async onEnvironmentCancelInteractiveSetup(params) {
        const status = cancelStatusFromReason(params.reason);
        const state = params.providerLeaseId ? setupSessions.get(params.providerLeaseId) : undefined;
        if (state) {
            state.status = status;
            await removeLease(state.providerLeaseId);
        }
        return {
            status,
            metadata: {
                provider: "fake-plugin",
                found: Boolean(state),
            },
        };
    },
    async onEnvironmentDeleteTemplate(params) {
        const template = templates.get(params.templateRef);
        if (template) {
            template.deleted = true;
        }
        return {
            deleted: Boolean(template),
            metadata: {
                provider: "fake-plugin",
                templateRef: params.templateRef,
                templateKind: params.templateKind ?? "snapshot",
            },
        };
    },
});
export default plugin;
//# sourceMappingURL=plugin.js.map