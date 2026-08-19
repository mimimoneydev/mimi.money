import type { AdapterExecutionContext, AdapterRuntimeMcpServer } from "@paperclipai/adapter-utils";
import { type AdapterExecutionTarget, type AdapterExecutionTargetShellOptions } from "@paperclipai/adapter-utils/execution-target";
export declare function resolveSharedClaudeConfigDir(env?: NodeJS.ProcessEnv): string;
export declare function resolveManagedClaudeConfigSeedDir(env: NodeJS.ProcessEnv, companyId?: string): string;
export declare function resolveManagedClaudeRuntimeStateDir(env: NodeJS.ProcessEnv, companyId: string, agentId: string): string;
export declare function writePaperclipClaudeMcpConfig(input: {
    stateDir: string;
    runId: string;
    servers: AdapterRuntimeMcpServer[];
}): Promise<string>;
export declare function prepareClaudeConfigSeed(env: NodeJS.ProcessEnv, onLog: AdapterExecutionContext["onLog"], companyId?: string): Promise<string>;
export declare function buildRemoteClaudeConfigMaterializationCommand(input: {
    remoteClaudeConfigDir: string;
    remoteClaudeConfigSeedDir: string;
}): string;
export declare function materializeRemoteClaudeConfig(input: {
    runId: string;
    target: AdapterExecutionTarget | null | undefined;
    remoteClaudeConfigDir: string;
    remoteClaudeConfigSeedDir: string;
    options: AdapterExecutionTargetShellOptions;
}): Promise<void>;
//# sourceMappingURL=claude-config.d.ts.map