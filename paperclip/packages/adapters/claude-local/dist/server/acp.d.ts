import type { AdapterBillingType, AdapterEnvironmentTestContext, AdapterEnvironmentTestResult, AdapterExecutionContext, AdapterExecutionResult } from "@paperclipai/adapter-utils";
import type { AcpxEngineExecutorOptions } from "@paperclipai/adapter-utils/acpx-engine/execute";
export type ClaudeExecutionEngine = "cli" | "acp";
export interface ClaudeEngineSelection {
    engine: ClaudeExecutionEngine;
    explicit: boolean;
    fallbackReason?: string;
}
type ClaudeEngineResolutionInput = Pick<AdapterExecutionContext, "config"> & Partial<Pick<AdapterExecutionContext, "executionTarget" | "executionTransport">>;
type ClaudeAcpExecutorOptions = Omit<AcpxEngineExecutorOptions, "adapterType" | "moduleDir" | "packageRootDir">;
type ClaudeAcpExecutor = (ctx: AdapterExecutionContext) => Promise<AdapterExecutionResult>;
export declare function resolveClaudeExecutionEngine(config: Record<string, unknown>): ClaudeEngineSelection;
export declare function resolveClaudeExecutionEngineForRun(input: ClaudeEngineResolutionInput): Promise<ClaudeEngineSelection>;
export declare function formatClaudeAcpFallbackMessage(reason: string): string;
export declare function buildClaudeAcpConfig(config: Record<string, unknown>): Record<string, unknown>;
/**
 * Classify billing the same way the Claude CLI lane does so ACP runs land in
 * the cost ledger with a real provider/billingType instead of acpx/unknown.
 * Host env only counts for local execution targets; remote targets see just
 * the adapter-config env.
 */
export declare function resolveClaudeAcpBillingIdentity(ctx: Pick<AdapterExecutionContext, "config"> & Partial<Pick<AdapterExecutionContext, "executionTarget" | "executionTransport">>): {
    provider: string;
    biller: string;
    billingType: AdapterBillingType;
};
export declare function createClaudeAcpExecutor(options?: ClaudeAcpExecutorOptions): ClaudeAcpExecutor;
export declare function nodeVersionMeetsClaudeAcpMinimum(version?: string): boolean;
export declare function testClaudeAcpEnvironment(ctx: AdapterEnvironmentTestContext): Promise<AdapterEnvironmentTestResult>;
export {};
//# sourceMappingURL=acp.d.ts.map