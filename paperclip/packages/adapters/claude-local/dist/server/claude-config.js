import { createHash } from "node:crypto";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { runAdapterExecutionTargetShellCommand, } from "@paperclipai/adapter-utils/execution-target";
import { resolvePaperclipInstanceRootForAdapter } from "@paperclipai/adapter-utils/server-utils";
import { shellQuote } from "@paperclipai/adapter-utils/ssh";
const SEEDED_SHARED_FILES = ["settings.json", "CLAUDE.md"];
function nonEmpty(value) {
    return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}
async function pathExists(candidate) {
    return fs.access(candidate).then(() => true).catch(() => false);
}
function isAlreadyExistsError(error) {
    if (!error || typeof error !== "object")
        return false;
    const code = "code" in error ? error.code : null;
    return code === "EEXIST" || code === "ENOTEMPTY";
}
function sanitizeRemoteClaudeSettings(raw) {
    let parsed;
    try {
        parsed = JSON.parse(raw);
    }
    catch {
        return JSON.stringify({ permissions: { defaultMode: "default" } });
    }
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
        return JSON.stringify({ permissions: { defaultMode: "default" } });
    }
    const settings = { ...parsed };
    settings.permissions = { defaultMode: "default" };
    delete settings.hooks;
    delete settings.mcpServers;
    delete settings.permissionMode;
    delete settings.skipDangerousModePermissionPrompt;
    return JSON.stringify(settings);
}
async function collectSeedFiles(sourceDir) {
    const files = [];
    for (const name of SEEDED_SHARED_FILES) {
        const sourcePath = path.join(sourceDir, name);
        if (!(await pathExists(sourcePath)))
            continue;
        const rawContents = await fs.readFile(sourcePath);
        const contents = name === "settings.json"
            ? Buffer.from(sanitizeRemoteClaudeSettings(rawContents.toString("utf8")), "utf8")
            : rawContents;
        files.push({ name, sourcePath, contents });
    }
    return files;
}
async function buildSeedSnapshotKey(files) {
    if (files.length === 0)
        return "empty";
    const hash = createHash("sha256");
    for (const file of files) {
        hash.update(file.name);
        hash.update("\0");
        hash.update(file.contents);
        hash.update("\0");
    }
    return hash.digest("hex").slice(0, 16);
}
async function materializeSeedSnapshot(input) {
    const targetDir = path.join(input.rootDir, input.snapshotKey);
    if (await pathExists(targetDir)) {
        return targetDir;
    }
    await fs.mkdir(input.rootDir, { recursive: true });
    const stagingDir = await fs.mkdtemp(path.join(input.rootDir, ".tmp-"));
    try {
        for (const file of input.files) {
            await fs.writeFile(path.join(stagingDir, file.name), file.contents);
        }
        try {
            await fs.rename(stagingDir, targetDir);
        }
        catch (error) {
            if (!isAlreadyExistsError(error)) {
                throw error;
            }
            await fs.rm(stagingDir, { recursive: true, force: true });
        }
    }
    catch (error) {
        await fs.rm(stagingDir, { recursive: true, force: true }).catch(() => undefined);
        throw error;
    }
    return targetDir;
}
export function resolveSharedClaudeConfigDir(env = process.env) {
    const fromEnv = nonEmpty(env.CLAUDE_CONFIG_DIR);
    return fromEnv ? path.resolve(fromEnv) : path.join(os.homedir(), ".claude");
}
export function resolveManagedClaudeConfigSeedDir(env, companyId) {
    const instanceRoot = resolvePaperclipInstanceRootForAdapter({
        homeDir: nonEmpty(env.PAPERCLIP_HOME) ?? undefined,
        instanceId: nonEmpty(env.PAPERCLIP_INSTANCE_ID) ?? undefined,
        env,
    });
    return companyId
        ? path.resolve(instanceRoot, "companies", companyId, "claude-config-seed")
        : path.resolve(instanceRoot, "claude-config-seed");
}
export function resolveManagedClaudeRuntimeStateDir(env, companyId, agentId) {
    const instanceRoot = resolvePaperclipInstanceRootForAdapter({
        homeDir: nonEmpty(env.PAPERCLIP_HOME) ?? undefined,
        instanceId: nonEmpty(env.PAPERCLIP_INSTANCE_ID) ?? undefined,
        env,
    });
    return path.join(instanceRoot, "companies", companyId, "agents", agentId, "claude-runtime");
}
export async function writePaperclipClaudeMcpConfig(input) {
    const configDir = path.join(input.stateDir, "runs", input.runId, "mcp");
    const configPath = path.join(configDir, "mcp-config.json");
    const usedNames = new Set();
    const mcpServers = {};
    for (const server of input.servers) {
        let name = server.name;
        if (usedNames.has(name))
            name = `${name}-${server.connectionId.slice(0, 8)}`;
        let suffix = 2;
        while (usedNames.has(name)) {
            name = `${server.name}-${server.connectionId.slice(0, 8)}-${suffix}`;
            suffix += 1;
        }
        usedNames.add(name);
        mcpServers[name] = {
            type: "http",
            url: server.url,
            headers: { Authorization: `Bearer ${server.token}` },
        };
    }
    await fs.mkdir(configDir, { recursive: true });
    await fs.writeFile(configPath, JSON.stringify({ mcpServers }), { mode: 0o600 });
    return configPath;
}
export async function prepareClaudeConfigSeed(env, onLog, companyId) {
    const sourceDir = resolveSharedClaudeConfigDir(env);
    const targetRootDir = resolveManagedClaudeConfigSeedDir(env, companyId);
    if (path.resolve(sourceDir) === path.resolve(targetRootDir)) {
        return targetRootDir;
    }
    const copiedFiles = await collectSeedFiles(sourceDir);
    const snapshotKey = await buildSeedSnapshotKey(copiedFiles);
    const targetDir = await materializeSeedSnapshot({
        rootDir: targetRootDir,
        snapshotKey,
        files: copiedFiles,
    });
    if (copiedFiles.length > 0) {
        await onLog("stdout", `[paperclip] Prepared Claude config seed "${targetDir}" from "${sourceDir}" (${copiedFiles.map((file) => file.name).join(", ")}).\n`);
    }
    else {
        await onLog("stdout", `[paperclip] No local Claude config seed files were found in "${sourceDir}". Remote Claude auth may still require login.\n`);
    }
    return targetDir;
}
export function buildRemoteClaudeConfigMaterializationCommand(input) {
    return `mkdir -p ${shellQuote(input.remoteClaudeConfigDir)} && ` +
        `if [ -d ${shellQuote(input.remoteClaudeConfigSeedDir)} ]; then ` +
        `cp -R ${shellQuote(`${input.remoteClaudeConfigSeedDir}/.`)} ${shellQuote(input.remoteClaudeConfigDir)}/; ` +
        `fi; ` +
        `for file in .credentials.json credentials.json; do ` +
        `if [ -n "\${HOME:-}" ] && [ -f "\${HOME}/.claude/\${file}" ] && [ ! -f ${shellQuote(input.remoteClaudeConfigDir)}/"\${file}" ]; then ` +
        `cp "\${HOME}/.claude/\${file}" ${shellQuote(input.remoteClaudeConfigDir)}/"\${file}"; ` +
        `fi; ` +
        `done`;
}
export async function materializeRemoteClaudeConfig(input) {
    await runAdapterExecutionTargetShellCommand(input.runId, input.target, buildRemoteClaudeConfigMaterializationCommand({
        remoteClaudeConfigDir: input.remoteClaudeConfigDir,
        remoteClaudeConfigSeedDir: input.remoteClaudeConfigSeedDir,
    }), input.options);
}
//# sourceMappingURL=claude-config.js.map