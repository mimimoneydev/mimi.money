import { asString, asNumber, parseObject, buildPaperclipEnv, } from "@paperclipai/adapter-utils/server-utils";
import { DEFAULT_LMSTUDIO_LOCAL_MODEL, DEFAULT_LMSTUDIO_API_BASE } from "../index.js";
export async function execute(ctx) {
    const { runId, agent, config, context, onLog, onMeta, authToken } = ctx;
    const model = asString(config.model, DEFAULT_LMSTUDIO_LOCAL_MODEL).trim();
    const cwd = asString(config.cwd, process.cwd());
    const timeoutSec = asNumber(config.timeoutSec, 0);
    const maxTokens = asNumber(config.maxTokens, 4096);
    const temperature = asNumber(config.temperature, 0.7);
    const envConfig = parseObject(config.env);
    const apiBase = asString(envConfig.LMSTUDIO_API_BASE, DEFAULT_LMSTUDIO_API_BASE).replace(/\/+$/, "");
    const promptTemplate = asString(config.promptTemplate, "You are agent {{agent.id}} ({{agent.name}}). Continue your Paperclip work.");
    const instructionsFilePath = asString(config.instructionsFilePath, "").trim();
    let systemPrompt = "";
    if (instructionsFilePath) {
        try {
            const { default: fs } = await import("node:fs/promises");
            systemPrompt = await fs.readFile(instructionsFilePath, "utf8");
        }
        catch (err) {
            await onLog("stdout", `[paperclip] Warning: could not read instructions file "${instructionsFilePath}": ${err instanceof Error ? err.message : String(err)}\n`);
        }
    }
    const templateData = {
        agentId: agent.id,
        companyId: agent.companyId,
        runId,
        company: { id: agent.companyId },
        agent,
        run: { id: runId, source: "on_demand" },
        context,
    };
    const renderTemplate = (tpl, data) => {
        return tpl.replace(/\{\{(\w[\w.]*)\}\}/g, (_, path) => {
            const keys = path.split(".");
            let val = data;
            for (const key of keys) {
                if (val && typeof val === "object")
                    val = val[key];
                else {
                    val = undefined;
                    break;
                }
            }
            return typeof val === "string" ? val : typeof val === "number" ? String(val) : "";
        });
    };
    const renderedPrompt = renderTemplate(promptTemplate, templateData);
    const messages = [];
    if (systemPrompt) {
        messages.push({ role: "system", content: systemPrompt });
    }
    messages.push({ role: "user", content: renderedPrompt });
    const url = `${apiBase}/chat/completions`;
    const env = { ...buildPaperclipEnv(agent) };
    env.PAPERCLIP_RUN_ID = runId;
    for (const [key, value] of Object.entries(envConfig)) {
        if (typeof value === "string")
            env[key] = value;
    }
    if (!env.PAPERCLIP_API_KEY && authToken) {
        env.PAPERCLIP_API_KEY = authToken;
    }
    if (onMeta) {
        await onMeta({
            adapterType: "lmstudio_local",
            command: `POST ${url}`,
            cwd,
            commandArgs: [`model=${model}`, `maxTokens=${maxTokens}`],
            commandNotes: [
                "OpenAI-compatible chat completions API call to LM Studio.",
                "Streaming response parsed in real-time.",
                "No API key required — LM Studio runs locally.",
            ],
            env: Object.fromEntries(Object.entries(env).filter(([k]) => !k.includes("KEY") && !k.includes("SECRET"))),
            prompt: renderedPrompt,
            promptMetrics: {
                promptChars: renderedPrompt.length,
                systemPromptChars: systemPrompt.length,
                heartbeatPromptChars: renderedPrompt.length,
            },
            context,
        });
    }
    const controller = new AbortController();
    const timer = timeoutSec > 0 ? setTimeout(() => controller.abort(), timeoutSec * 1000) : null;
    try {
        const body = {
            model,
            messages,
            max_tokens: maxTokens,
            temperature,
            stream: true,
        };
        const res = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(body),
            signal: controller.signal,
        });
        if (!res.ok) {
            const errorBody = await res.text().catch(() => "");
            const isConnectionError = res.status === 502 || res.status === 503;
            return {
                exitCode: 1,
                signal: null,
                timedOut: false,
                errorMessage: `LM Studio API returned ${res.status}: ${errorBody.slice(0, 500)}`,
                errorCode: isConnectionError ? "lmstudio_unavailable" : null,
                provider: "lmstudio",
                biller: "local",
                model,
                billingType: "fixed",
            };
        }
        if (!res.body) {
            return {
                exitCode: 1,
                signal: null,
                timedOut: false,
                errorMessage: "LM Studio API returned empty response body",
                provider: "lmstudio",
                biller: "local",
                model,
                billingType: "fixed",
            };
        }
        let fullContent = "";
        let usageInput = 0;
        let usageOutput = 0;
        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        while (true) {
            const { done, value } = await reader.read();
            if (done)
                break;
            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split("\n");
            buffer = lines.pop() ?? "";
            for (const line of lines) {
                const trimmed = line.trim();
                if (!trimmed || trimmed === "data: [DONE]")
                    continue;
                if (!trimmed.startsWith("data: "))
                    continue;
                try {
                    const chunk = JSON.parse(trimmed.slice(6));
                    const delta = chunk.choices?.[0]?.delta;
                    if (delta?.content) {
                        fullContent += delta.content;
                        await onLog("stdout", delta.content);
                    }
                    if (chunk.usage) {
                        usageInput = chunk.usage.prompt_tokens ?? 0;
                        usageOutput = chunk.usage.completion_tokens ?? 0;
                    }
                }
                catch {
                    // Skip malformed chunks
                }
            }
        }
        if (buffer.trim() && buffer.trim() !== "data: [DONE]" && buffer.trim().startsWith("data: ")) {
            try {
                const chunk = JSON.parse(buffer.trim().slice(6));
                const delta = chunk.choices?.[0]?.delta;
                if (delta?.content) {
                    fullContent += delta.content;
                    await onLog("stdout", delta.content);
                }
                if (chunk.usage) {
                    usageInput = chunk.usage.prompt_tokens ?? 0;
                    usageOutput = chunk.usage.completion_tokens ?? 0;
                }
            }
            catch {
                // Skip malformed
            }
        }
        await onLog("stdout", "\n");
        return {
            exitCode: 0,
            signal: null,
            timedOut: false,
            usage: {
                inputTokens: usageInput,
                outputTokens: usageOutput,
            },
            provider: "lmstudio",
            biller: "local",
            model,
            billingType: "fixed",
            resultJson: {
                content: fullContent,
                usage: { inputTokens: usageInput, outputTokens: usageOutput },
            },
            summary: fullContent.slice(0, 500).trim() || "LM Studio response completed",
        };
    }
    catch (err) {
        if (err instanceof Error && err.name === "AbortError") {
            return {
                exitCode: null,
                signal: null,
                timedOut: true,
                errorMessage: `Timed out after ${timeoutSec}s`,
            };
        }
        const isConnectionRefused = err instanceof Error && (err.message.includes("ECONNREFUSED") || err.message.includes("fetch failed"));
        return {
            exitCode: 1,
            signal: null,
            timedOut: false,
            errorMessage: isConnectionRefused
                ? "LM Studio is not running or not reachable. Start LM Studio and enable the local server."
                : err instanceof Error ? err.message : String(err),
            errorCode: isConnectionRefused ? "lmstudio_unavailable" : null,
            provider: "lmstudio",
            biller: "local",
            model,
            billingType: "fixed",
        };
    }
    finally {
        if (timer)
            clearTimeout(timer);
    }
}
//# sourceMappingURL=execute.js.map