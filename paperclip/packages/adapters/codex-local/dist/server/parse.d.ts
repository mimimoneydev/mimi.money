export type CodexAuthRefreshFailureClass = "refresh_token_reused" | "refresh_token_expired" | "refresh_token_invalidated";
export declare function parseCodexJsonl(stdout: string): {
    sessionId: string | null;
    summary: string;
    usage: {
        inputTokens: number;
        cachedInputTokens: number;
        outputTokens: number;
    };
    usageBasis: "per_run";
    errorMessage: string | null;
};
export declare function isCodexUnknownSessionError(stdout: string, stderr: string): boolean;
export declare function classifyCodexAuthRefreshFailure(input: {
    stdout?: string | null;
    stderr?: string | null;
    errorMessage?: string | null;
}): CodexAuthRefreshFailureClass | null;
export declare function extractCodexRetryNotBefore(input: {
    stdout?: string | null;
    stderr?: string | null;
    errorMessage?: string | null;
}, now?: Date): Date | null;
export declare function isCodexTransientUpstreamError(input: {
    stdout?: string | null;
    stderr?: string | null;
    errorMessage?: string | null;
}): boolean;
export declare function isCodexProviderQuotaError(input: {
    stdout?: string | null;
    stderr?: string | null;
    errorMessage?: string | null;
}): boolean;
//# sourceMappingURL=parse.d.ts.map