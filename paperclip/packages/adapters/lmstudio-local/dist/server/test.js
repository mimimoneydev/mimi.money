import { asString, asNumber, parseObject, } from "@paperclipai/adapter-utils/server-utils";
import { DEFAULT_LMSTUDIO_LOCAL_MODEL, DEFAULT_LMSTUDIO_API_BASE } from "../index.js";
function summarizeStatus(checks) {
    if (checks.some((check) => check.level === "error"))
        return "fail";
    if (checks.some((check) => check.level === "warn"))
        return "warn";
    return "pass";
}
export async function testEnvironment(ctx) {
    const checks = [];
    const config = parseObject(ctx.config);
    const envConfig = parseObject(config.env);
    const apiBase = asString(envConfig.LMSTUDIO_API_BASE, DEFAULT_LMSTUDIO_API_BASE).replace(/\/+$/, "");
    const model = asString(config.model, DEFAULT_LMSTUDIO_LOCAL_MODEL).trim();
    const helloProbeTimeoutSec = Math.max(1, asNumber(config.helloProbeTimeoutSec, 15));
    checks.push({
        code: "lmstudio_no_api_key_needed",
        level: "info",
        message: "LM Studio runs locally — no API key required.",
    });
    const url = `${apiBase}/chat/completions`;
    try {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), helloProbeTimeoutSec * 1000);
        const res = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                model,
                messages: [{ role: "user", content: "Respond with hello." }],
                max_tokens: 16,
                stream: false,
            }),
            signal: controller.signal,
        });
        clearTimeout(timer);
        if (res.ok) {
            const body = await res.json();
            const content = body.choices?.[0]?.message?.content?.trim() ?? "";
            const hasHello = /\bhello\b/i.test(content);
            checks.push({
                code: hasHello ? "lmstudio_hello_probe_passed" : "lmstudio_hello_probe_unexpected_output",
                level: hasHello ? "info" : "warn",
                message: hasHello
                    ? "LM Studio hello probe succeeded."
                    : "LM Studio probe ran but did not return `hello` as expected.",
                ...(content ? { detail: content.slice(0, 240) } : {}),
            });
        }
        else if (res.status === 502 || res.status === 503) {
            checks.push({
                code: "lmstudio_no_model_loaded",
                level: "error",
                message: "LM Studio server is running but no model is loaded.",
                hint: "Load a model in LM Studio, then retry the probe.",
            });
        }
        else {
            const errorBody = await res.text().catch(() => "");
            checks.push({
                code: "lmstudio_hello_probe_failed",
                level: "warn",
                message: `LM Studio hello probe returned HTTP ${res.status}.`,
                detail: errorBody.slice(0, 240),
                hint: "Verify LM Studio is running with the local server enabled and a model loaded.",
            });
        }
    }
    catch (err) {
        if (err instanceof Error && (err.message.includes("ECONNREFUSED") || err.message.includes("fetch failed"))) {
            checks.push({
                code: "lmstudio_not_running",
                level: "error",
                message: "LM Studio is not running or not reachable.",
                hint: "Start LM Studio, enable the local server (default: http://localhost:1234), then retry.",
            });
        }
        else if (err instanceof Error && err.name === "AbortError") {
            checks.push({
                code: "lmstudio_hello_probe_timed_out",
                level: "warn",
                message: "LM Studio hello probe timed out.",
                hint: "The model may be loading or inference is slow. Try again once the model is ready.",
            });
        }
        else {
            checks.push({
                code: "lmstudio_hello_probe_error",
                level: "error",
                message: `LM Studio hello probe failed: ${err instanceof Error ? err.message : String(err)}`,
                hint: "Verify LM Studio is running with the local server enabled.",
            });
        }
    }
    return {
        adapterType: ctx.adapterType,
        status: summarizeStatus(checks),
        checks,
        testedAt: new Date().toISOString(),
    };
}
//# sourceMappingURL=test.js.map