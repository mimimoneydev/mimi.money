import type { UsageSummary } from "@paperclipai/adapter-utils";
/**
 * Sum the per-model usage ledger from a Claude CLI result event. The result
 * event's top-level `usage` reflects only the main-loop message chain, so it
 * undercounts output tokens whenever subagents or sidechains ran; `modelUsage`
 * is the CLI's authoritative per-model accounting (it is what backs /cost).
 * Cache-creation tokens are billed prompt tokens, so they count as input.
 */
export declare function claudeModelUsageTotals(modelUsage: unknown): UsageSummary | null;
export declare function parseClaudeStreamJson(stdout: string): {
    sessionId: string | null;
    model: string;
    costUsd: number | null;
    usage: UsageSummary | null;
    usageBasis: "per_run" | null;
    summary: string;
    resultJson: Record<string, unknown> | null;
};
export declare function extractClaudeLoginUrl(text: string): string | null;
export declare function detectClaudeLoginRequired(input: {
    parsed: Record<string, unknown> | null;
    stdout: string;
    stderr: string;
}): {
    requiresLogin: boolean;
    loginUrl: string | null;
};
export declare function describeClaudeFailure(parsed: Record<string, unknown>): string | null;
export declare function isClaudeModelNotFoundError(input: {
    parsed?: Record<string, unknown> | null;
    stdout?: string | null;
    stderr?: string | null;
    errorMessage?: string | null;
}): boolean;
export declare function isClaudeMaxTurnsResult(parsed: Record<string, unknown> | null | undefined): boolean;
export declare function isClaudeRefusalResult(parsed: Record<string, unknown> | null | undefined): boolean;
export declare function isClaudeUnknownSessionError(parsed: Record<string, unknown>): boolean;
export declare function isClaudePoisonedPreviousMessageIdError(parsed: Record<string, unknown>): boolean;
export declare function isClaudeImageProcessingError(parsed: Record<string, unknown>): boolean;
export declare function extractClaudeRetryNotBefore(input: {
    parsed?: Record<string, unknown> | null;
    stdout?: string | null;
    stderr?: string | null;
    errorMessage?: string | null;
}, now?: Date): Date | null;
export declare function isClaudeTransientUpstreamError(input: {
    parsed?: Record<string, unknown> | null;
    stdout?: string | null;
    stderr?: string | null;
    errorMessage?: string | null;
}): boolean;
export declare function isClaudeProviderQuotaError(input: {
    parsed?: Record<string, unknown> | null;
    stdout?: string | null;
    stderr?: string | null;
    errorMessage?: string | null;
}): boolean;
//# sourceMappingURL=parse.d.ts.map