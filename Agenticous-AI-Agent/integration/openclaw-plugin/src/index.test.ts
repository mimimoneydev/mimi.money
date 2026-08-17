import { describe, expect, it } from "vitest";
import entry from "./index.js";
import type { OpenClawPluginApi } from "openclaw/plugin-sdk/plugin-entry";

describe("agenticous-blockchain", () => {
  it("declares stable plugin identity and configuration", () => {
    expect(entry.id).toBe("agenticous-blockchain");
    expect(entry.name).toBe("Agenticous AI agent");
    expect(entry.configSchema).toBeDefined();
  });


  it("registers bounded autonomous tools without a human approval hook", () => {
    const tools: Array<{ name?: string }> = [];
    const hooks: string[] = [];
    const api = {
      pluginConfig: { supportClientUrl: "http://127.0.0.1:4411", supportClientTokenFile: "/etc/openclaw/agenticous-client.token" },
      registerTool(tool: { name?: string }) { tools.push(tool); },
      on(name: string) { hooks.push(name); },
    } as unknown as OpenClawPluginApi;
    entry.register!(api);
    expect(tools.map(tool => tool.name)).toEqual([
      "agenticous_wallet_activity", "agenticous_chain_query", "agenticous_circle_wallet", "agenticous_circle_transfer", "agenticous_x402_search", "agenticous_x402_purchase", "agenticous_autonomous_action",
    ]);
    expect(hooks).not.toContain("before_tool_call");
  });
});
