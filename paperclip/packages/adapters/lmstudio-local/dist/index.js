export const type = "lmstudio_local";
export const label = "LM Studio (local)";
export const DEFAULT_LMSTUDIO_LOCAL_MODEL = "loaded";
export const DEFAULT_LMSTUDIO_API_BASE = "http://localhost:1234/v1";
export const models = [
    { id: DEFAULT_LMSTUDIO_LOCAL_MODEL, label: "Currently loaded model" },
];
export const agentConfigurationDoc = `# lmstudio_local agent configuration

Adapter: lmstudio_local

Use when:
- You want Paperclip to call a local LM Studio server via its OpenAI-compatible API
- You want to use locally-loaded models without cloud API keys
- You want full privacy with on-device inference

Don't use when:
- You need webhook-style external invocation (use http or openclaw_gateway)
- You only need a one-shot script without an AI agent loop (use process)
- LM Studio is not running on the Paperclip host machine

Core fields:
- model (string, optional): Model identifier. Defaults to "loaded" (uses the currently loaded model in LM Studio).
- cwd (string, optional): default working directory hint for the agent
- instructionsFilePath (string, optional): absolute path to a markdown instructions file prepended to the system prompt
- promptTemplate (string, optional): run prompt template
- env (object, optional): KEY=VALUE environment variables
  - LMSTUDIO_API_BASE (optional): API base URL, defaults to http://localhost:1234/v1

Operational fields:
- timeoutSec (number, optional): run timeout in seconds
- maxTokens (number, optional): max tokens for the response, defaults to 4096
- temperature (number, optional): sampling temperature, defaults to 0.7

Notes:
- Uses OpenAI-compatible chat completions API format.
- LM Studio must be running with a model loaded and the local server enabled.
- No API key is required — LM Studio runs locally without authentication.
- Streaming responses are logged in real-time.
`;
//# sourceMappingURL=index.js.map