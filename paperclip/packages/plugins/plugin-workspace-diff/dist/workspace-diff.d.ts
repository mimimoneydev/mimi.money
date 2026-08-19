import type { PluginExecutionWorkspaceMetadata } from "@paperclipai/plugin-sdk";
import type { WorkspaceDiffCaps, WorkspaceDiffQueryOptions, WorkspaceDiffResponse } from "./contracts.js";
export declare const WORKSPACE_DIFF_CAPS: WorkspaceDiffCaps;
type WorkspaceDiffTarget = Pick<PluginExecutionWorkspaceMetadata, "id" | "companyId" | "cwd" | "baseRef">;
export declare function workspaceDiffService(): {
    getDiff(workspace: WorkspaceDiffTarget, query: WorkspaceDiffQueryOptions): Promise<WorkspaceDiffResponse>;
};
export {};
//# sourceMappingURL=workspace-diff.d.ts.map