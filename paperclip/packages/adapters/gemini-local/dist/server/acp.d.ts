import type { AdapterEnvironmentTestContext, AdapterEnvironmentTestResult, AdapterExecutionContext, AdapterExecutionResult } from "@paperclipai/adapter-utils";
import type { AcpxEngineExecutorOptions } from "@paperclipai/adapter-utils/acpx-engine/execute";
export type GeminiExecutionEngine = "cli" | "acp";
export interface GeminiEngineSelection {
    engine: GeminiExecutionEngine;
    explicit: boolean;
    fallbackReason?: string;
}
type GeminiEngineResolutionInput = Pick<AdapterExecutionContext, "config"> & Partial<Pick<AdapterExecutionContext, "executionTarget" | "executionTransport">>;
type GeminiAcpExecutorOptions = Omit<AcpxEngineExecutorOptions, "adapterType" | "moduleDir" | "packageRootDir">;
type GeminiAcpExecutor = (ctx: AdapterExecutionContext) => Promise<AdapterExecutionResult>;
export declare function resolveGeminiExecutionEngine(config: Record<string, unknown>): GeminiEngineSelection;
export declare function resolveGeminiExecutionEngineForRun(input: GeminiEngineResolutionInput): Promise<GeminiEngineSelection>;
export declare function formatGeminiAcpFallbackMessage(reason: string): string;
export declare function buildGeminiAcpConfig(config: Record<string, unknown>): Record<string, unknown>;
export declare function createGeminiAcpExecutor(options?: GeminiAcpExecutorOptions): GeminiAcpExecutor;
export declare function nodeVersionMeetsGeminiAcpMinimum(version?: string): boolean;
export declare function testGeminiAcpEnvironment(ctx: AdapterEnvironmentTestContext): Promise<AdapterEnvironmentTestResult>;
export {};
//# sourceMappingURL=acp.d.ts.map