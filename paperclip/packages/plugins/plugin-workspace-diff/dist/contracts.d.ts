import { z } from "@paperclipai/plugin-sdk";
export declare const workspaceDiffViewSchema: z.ZodEnum<["working-tree", "head"]>;
export declare const workspaceDiffFileStatusSchema: z.ZodEnum<["added", "modified", "deleted", "renamed", "copied", "type_changed", "untracked", "unknown"]>;
export declare const workspaceDiffPatchKindSchema: z.ZodEnum<["staged", "unstaged", "head", "untracked"]>;
export declare const workspaceDiffWarningCodeSchema: z.ZodEnum<["base_ref_missing", "base_ref_invalid", "binary_file", "file_count_truncated", "file_oversized", "git_command_failed", "missing_cwd", "non_git_workspace", "patch_truncated", "path_filter_invalid", "symlink_target_outside_workspace", "workspace_path_invalid"]>;
export declare const workspaceDiffQuerySchema: z.ZodEffects<z.ZodObject<{
    view: z.ZodDefault<z.ZodOptional<z.ZodEnum<["working-tree", "head"]>>>;
    baseRef: z.ZodNullable<z.ZodOptional<z.ZodString>>;
    includeUntracked: z.ZodDefault<z.ZodOptional<z.ZodEffects<z.ZodUnion<[z.ZodBoolean, z.ZodEnum<["true", "false"]>]>, boolean, boolean | "true" | "false">>>;
    path: z.ZodOptional<z.ZodUnion<[z.ZodString, z.ZodArray<z.ZodString, "many">]>>;
    paths: z.ZodOptional<z.ZodUnion<[z.ZodString, z.ZodArray<z.ZodString, "many">]>>;
}, "passthrough", z.ZodTypeAny, z.objectOutputType<{
    view: z.ZodDefault<z.ZodOptional<z.ZodEnum<["working-tree", "head"]>>>;
    baseRef: z.ZodNullable<z.ZodOptional<z.ZodString>>;
    includeUntracked: z.ZodDefault<z.ZodOptional<z.ZodEffects<z.ZodUnion<[z.ZodBoolean, z.ZodEnum<["true", "false"]>]>, boolean, boolean | "true" | "false">>>;
    path: z.ZodOptional<z.ZodUnion<[z.ZodString, z.ZodArray<z.ZodString, "many">]>>;
    paths: z.ZodOptional<z.ZodUnion<[z.ZodString, z.ZodArray<z.ZodString, "many">]>>;
}, z.ZodTypeAny, "passthrough">, z.objectInputType<{
    view: z.ZodDefault<z.ZodOptional<z.ZodEnum<["working-tree", "head"]>>>;
    baseRef: z.ZodNullable<z.ZodOptional<z.ZodString>>;
    includeUntracked: z.ZodDefault<z.ZodOptional<z.ZodEffects<z.ZodUnion<[z.ZodBoolean, z.ZodEnum<["true", "false"]>]>, boolean, boolean | "true" | "false">>>;
    path: z.ZodOptional<z.ZodUnion<[z.ZodString, z.ZodArray<z.ZodString, "many">]>>;
    paths: z.ZodOptional<z.ZodUnion<[z.ZodString, z.ZodArray<z.ZodString, "many">]>>;
}, z.ZodTypeAny, "passthrough">>, {
    view: "working-tree" | "head";
    baseRef: string | null;
    includeUntracked: boolean;
    paths: string[];
}, z.objectInputType<{
    view: z.ZodDefault<z.ZodOptional<z.ZodEnum<["working-tree", "head"]>>>;
    baseRef: z.ZodNullable<z.ZodOptional<z.ZodString>>;
    includeUntracked: z.ZodDefault<z.ZodOptional<z.ZodEffects<z.ZodUnion<[z.ZodBoolean, z.ZodEnum<["true", "false"]>]>, boolean, boolean | "true" | "false">>>;
    path: z.ZodOptional<z.ZodUnion<[z.ZodString, z.ZodArray<z.ZodString, "many">]>>;
    paths: z.ZodOptional<z.ZodUnion<[z.ZodString, z.ZodArray<z.ZodString, "many">]>>;
}, z.ZodTypeAny, "passthrough">>;
export declare const workspaceDiffWarningSchema: z.ZodObject<{
    code: z.ZodEnum<["base_ref_missing", "base_ref_invalid", "binary_file", "file_count_truncated", "file_oversized", "git_command_failed", "missing_cwd", "non_git_workspace", "patch_truncated", "path_filter_invalid", "symlink_target_outside_workspace", "workspace_path_invalid"]>;
    message: z.ZodString;
    path: z.ZodNullable<z.ZodString>;
}, "strict", z.ZodTypeAny, {
    code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
    message: string;
    path: string | null;
}, {
    code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
    message: string;
    path: string | null;
}>;
export declare const workspaceDiffCapsSchema: z.ZodObject<{
    maxFiles: z.ZodNumber;
    maxFileBytes: z.ZodNumber;
    maxPatchBytes: z.ZodNumber;
    maxTotalPatchBytes: z.ZodNumber;
}, "strict", z.ZodTypeAny, {
    maxFiles: number;
    maxFileBytes: number;
    maxPatchBytes: number;
    maxTotalPatchBytes: number;
}, {
    maxFiles: number;
    maxFileBytes: number;
    maxPatchBytes: number;
    maxTotalPatchBytes: number;
}>;
export declare const workspaceDiffFilePatchSchema: z.ZodObject<{
    kind: z.ZodEnum<["staged", "unstaged", "head", "untracked"]>;
    patch: z.ZodNullable<z.ZodString>;
    additions: z.ZodNumber;
    deletions: z.ZodNumber;
    binary: z.ZodBoolean;
    oversized: z.ZodBoolean;
    truncated: z.ZodBoolean;
    warnings: z.ZodArray<z.ZodObject<{
        code: z.ZodEnum<["base_ref_missing", "base_ref_invalid", "binary_file", "file_count_truncated", "file_oversized", "git_command_failed", "missing_cwd", "non_git_workspace", "patch_truncated", "path_filter_invalid", "symlink_target_outside_workspace", "workspace_path_invalid"]>;
        message: z.ZodString;
        path: z.ZodNullable<z.ZodString>;
    }, "strict", z.ZodTypeAny, {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }, {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }>, "many">;
}, "strict", z.ZodTypeAny, {
    kind: "head" | "untracked" | "staged" | "unstaged";
    warnings: {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }[];
    patch: string | null;
    additions: number;
    deletions: number;
    binary: boolean;
    oversized: boolean;
    truncated: boolean;
}, {
    kind: "head" | "untracked" | "staged" | "unstaged";
    warnings: {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }[];
    patch: string | null;
    additions: number;
    deletions: number;
    binary: boolean;
    oversized: boolean;
    truncated: boolean;
}>;
export declare const workspaceDiffFileSchema: z.ZodObject<{
    path: z.ZodString;
    oldPath: z.ZodNullable<z.ZodString>;
    status: z.ZodEnum<["added", "modified", "deleted", "renamed", "copied", "type_changed", "untracked", "unknown"]>;
    staged: z.ZodBoolean;
    unstaged: z.ZodBoolean;
    untracked: z.ZodBoolean;
    binary: z.ZodBoolean;
    oversized: z.ZodBoolean;
    truncated: z.ZodBoolean;
    additions: z.ZodNumber;
    deletions: z.ZodNumber;
    sizeBytes: z.ZodNullable<z.ZodNumber>;
    patches: z.ZodArray<z.ZodObject<{
        kind: z.ZodEnum<["staged", "unstaged", "head", "untracked"]>;
        patch: z.ZodNullable<z.ZodString>;
        additions: z.ZodNumber;
        deletions: z.ZodNumber;
        binary: z.ZodBoolean;
        oversized: z.ZodBoolean;
        truncated: z.ZodBoolean;
        warnings: z.ZodArray<z.ZodObject<{
            code: z.ZodEnum<["base_ref_missing", "base_ref_invalid", "binary_file", "file_count_truncated", "file_oversized", "git_command_failed", "missing_cwd", "non_git_workspace", "patch_truncated", "path_filter_invalid", "symlink_target_outside_workspace", "workspace_path_invalid"]>;
            message: z.ZodString;
            path: z.ZodNullable<z.ZodString>;
        }, "strict", z.ZodTypeAny, {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }, {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }>, "many">;
    }, "strict", z.ZodTypeAny, {
        kind: "head" | "untracked" | "staged" | "unstaged";
        warnings: {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }[];
        patch: string | null;
        additions: number;
        deletions: number;
        binary: boolean;
        oversized: boolean;
        truncated: boolean;
    }, {
        kind: "head" | "untracked" | "staged" | "unstaged";
        warnings: {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }[];
        patch: string | null;
        additions: number;
        deletions: number;
        binary: boolean;
        oversized: boolean;
        truncated: boolean;
    }>, "many">;
    warnings: z.ZodArray<z.ZodObject<{
        code: z.ZodEnum<["base_ref_missing", "base_ref_invalid", "binary_file", "file_count_truncated", "file_oversized", "git_command_failed", "missing_cwd", "non_git_workspace", "patch_truncated", "path_filter_invalid", "symlink_target_outside_workspace", "workspace_path_invalid"]>;
        message: z.ZodString;
        path: z.ZodNullable<z.ZodString>;
    }, "strict", z.ZodTypeAny, {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }, {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }>, "many">;
}, "strict", z.ZodTypeAny, {
    status: "unknown" | "deleted" | "added" | "modified" | "renamed" | "copied" | "type_changed" | "untracked";
    path: string;
    sizeBytes: number | null;
    warnings: {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }[];
    untracked: boolean;
    staged: boolean;
    unstaged: boolean;
    additions: number;
    deletions: number;
    binary: boolean;
    oversized: boolean;
    truncated: boolean;
    oldPath: string | null;
    patches: {
        kind: "head" | "untracked" | "staged" | "unstaged";
        warnings: {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }[];
        patch: string | null;
        additions: number;
        deletions: number;
        binary: boolean;
        oversized: boolean;
        truncated: boolean;
    }[];
}, {
    status: "unknown" | "deleted" | "added" | "modified" | "renamed" | "copied" | "type_changed" | "untracked";
    path: string;
    sizeBytes: number | null;
    warnings: {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }[];
    untracked: boolean;
    staged: boolean;
    unstaged: boolean;
    additions: number;
    deletions: number;
    binary: boolean;
    oversized: boolean;
    truncated: boolean;
    oldPath: string | null;
    patches: {
        kind: "head" | "untracked" | "staged" | "unstaged";
        warnings: {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }[];
        patch: string | null;
        additions: number;
        deletions: number;
        binary: boolean;
        oversized: boolean;
        truncated: boolean;
    }[];
}>;
export declare const workspaceDiffStatsSchema: z.ZodObject<{
    fileCount: z.ZodNumber;
    stagedFileCount: z.ZodNumber;
    unstagedFileCount: z.ZodNumber;
    untrackedFileCount: z.ZodNumber;
    binaryFileCount: z.ZodNumber;
    oversizedFileCount: z.ZodNumber;
    truncatedFileCount: z.ZodNumber;
    additions: z.ZodNumber;
    deletions: z.ZodNumber;
}, "strict", z.ZodTypeAny, {
    additions: number;
    deletions: number;
    fileCount: number;
    stagedFileCount: number;
    unstagedFileCount: number;
    untrackedFileCount: number;
    binaryFileCount: number;
    oversizedFileCount: number;
    truncatedFileCount: number;
}, {
    additions: number;
    deletions: number;
    fileCount: number;
    stagedFileCount: number;
    unstagedFileCount: number;
    untrackedFileCount: number;
    binaryFileCount: number;
    oversizedFileCount: number;
    truncatedFileCount: number;
}>;
export declare const workspaceDiffResponseSchema: z.ZodObject<{
    workspaceId: z.ZodString;
    companyId: z.ZodString;
    view: z.ZodEnum<["working-tree", "head"]>;
    baseRef: z.ZodNullable<z.ZodString>;
    defaultBaseRef: z.ZodNullable<z.ZodString>;
    headSha: z.ZodNullable<z.ZodString>;
    includeUntracked: z.ZodBoolean;
    paths: z.ZodArray<z.ZodString, "many">;
    files: z.ZodArray<z.ZodObject<{
        path: z.ZodString;
        oldPath: z.ZodNullable<z.ZodString>;
        status: z.ZodEnum<["added", "modified", "deleted", "renamed", "copied", "type_changed", "untracked", "unknown"]>;
        staged: z.ZodBoolean;
        unstaged: z.ZodBoolean;
        untracked: z.ZodBoolean;
        binary: z.ZodBoolean;
        oversized: z.ZodBoolean;
        truncated: z.ZodBoolean;
        additions: z.ZodNumber;
        deletions: z.ZodNumber;
        sizeBytes: z.ZodNullable<z.ZodNumber>;
        patches: z.ZodArray<z.ZodObject<{
            kind: z.ZodEnum<["staged", "unstaged", "head", "untracked"]>;
            patch: z.ZodNullable<z.ZodString>;
            additions: z.ZodNumber;
            deletions: z.ZodNumber;
            binary: z.ZodBoolean;
            oversized: z.ZodBoolean;
            truncated: z.ZodBoolean;
            warnings: z.ZodArray<z.ZodObject<{
                code: z.ZodEnum<["base_ref_missing", "base_ref_invalid", "binary_file", "file_count_truncated", "file_oversized", "git_command_failed", "missing_cwd", "non_git_workspace", "patch_truncated", "path_filter_invalid", "symlink_target_outside_workspace", "workspace_path_invalid"]>;
                message: z.ZodString;
                path: z.ZodNullable<z.ZodString>;
            }, "strict", z.ZodTypeAny, {
                code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
                message: string;
                path: string | null;
            }, {
                code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
                message: string;
                path: string | null;
            }>, "many">;
        }, "strict", z.ZodTypeAny, {
            kind: "head" | "untracked" | "staged" | "unstaged";
            warnings: {
                code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
                message: string;
                path: string | null;
            }[];
            patch: string | null;
            additions: number;
            deletions: number;
            binary: boolean;
            oversized: boolean;
            truncated: boolean;
        }, {
            kind: "head" | "untracked" | "staged" | "unstaged";
            warnings: {
                code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
                message: string;
                path: string | null;
            }[];
            patch: string | null;
            additions: number;
            deletions: number;
            binary: boolean;
            oversized: boolean;
            truncated: boolean;
        }>, "many">;
        warnings: z.ZodArray<z.ZodObject<{
            code: z.ZodEnum<["base_ref_missing", "base_ref_invalid", "binary_file", "file_count_truncated", "file_oversized", "git_command_failed", "missing_cwd", "non_git_workspace", "patch_truncated", "path_filter_invalid", "symlink_target_outside_workspace", "workspace_path_invalid"]>;
            message: z.ZodString;
            path: z.ZodNullable<z.ZodString>;
        }, "strict", z.ZodTypeAny, {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }, {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }>, "many">;
    }, "strict", z.ZodTypeAny, {
        status: "unknown" | "deleted" | "added" | "modified" | "renamed" | "copied" | "type_changed" | "untracked";
        path: string;
        sizeBytes: number | null;
        warnings: {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }[];
        untracked: boolean;
        staged: boolean;
        unstaged: boolean;
        additions: number;
        deletions: number;
        binary: boolean;
        oversized: boolean;
        truncated: boolean;
        oldPath: string | null;
        patches: {
            kind: "head" | "untracked" | "staged" | "unstaged";
            warnings: {
                code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
                message: string;
                path: string | null;
            }[];
            patch: string | null;
            additions: number;
            deletions: number;
            binary: boolean;
            oversized: boolean;
            truncated: boolean;
        }[];
    }, {
        status: "unknown" | "deleted" | "added" | "modified" | "renamed" | "copied" | "type_changed" | "untracked";
        path: string;
        sizeBytes: number | null;
        warnings: {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }[];
        untracked: boolean;
        staged: boolean;
        unstaged: boolean;
        additions: number;
        deletions: number;
        binary: boolean;
        oversized: boolean;
        truncated: boolean;
        oldPath: string | null;
        patches: {
            kind: "head" | "untracked" | "staged" | "unstaged";
            warnings: {
                code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
                message: string;
                path: string | null;
            }[];
            patch: string | null;
            additions: number;
            deletions: number;
            binary: boolean;
            oversized: boolean;
            truncated: boolean;
        }[];
    }>, "many">;
    stats: z.ZodObject<{
        fileCount: z.ZodNumber;
        stagedFileCount: z.ZodNumber;
        unstagedFileCount: z.ZodNumber;
        untrackedFileCount: z.ZodNumber;
        binaryFileCount: z.ZodNumber;
        oversizedFileCount: z.ZodNumber;
        truncatedFileCount: z.ZodNumber;
        additions: z.ZodNumber;
        deletions: z.ZodNumber;
    }, "strict", z.ZodTypeAny, {
        additions: number;
        deletions: number;
        fileCount: number;
        stagedFileCount: number;
        unstagedFileCount: number;
        untrackedFileCount: number;
        binaryFileCount: number;
        oversizedFileCount: number;
        truncatedFileCount: number;
    }, {
        additions: number;
        deletions: number;
        fileCount: number;
        stagedFileCount: number;
        unstagedFileCount: number;
        untrackedFileCount: number;
        binaryFileCount: number;
        oversizedFileCount: number;
        truncatedFileCount: number;
    }>;
    warnings: z.ZodArray<z.ZodObject<{
        code: z.ZodEnum<["base_ref_missing", "base_ref_invalid", "binary_file", "file_count_truncated", "file_oversized", "git_command_failed", "missing_cwd", "non_git_workspace", "patch_truncated", "path_filter_invalid", "symlink_target_outside_workspace", "workspace_path_invalid"]>;
        message: z.ZodString;
        path: z.ZodNullable<z.ZodString>;
    }, "strict", z.ZodTypeAny, {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }, {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }>, "many">;
    caps: z.ZodObject<{
        maxFiles: z.ZodNumber;
        maxFileBytes: z.ZodNumber;
        maxPatchBytes: z.ZodNumber;
        maxTotalPatchBytes: z.ZodNumber;
    }, "strict", z.ZodTypeAny, {
        maxFiles: number;
        maxFileBytes: number;
        maxPatchBytes: number;
        maxTotalPatchBytes: number;
    }, {
        maxFiles: number;
        maxFileBytes: number;
        maxPatchBytes: number;
        maxTotalPatchBytes: number;
    }>;
    truncated: z.ZodBoolean;
}, "strict", z.ZodTypeAny, {
    companyId: string;
    baseRef: string | null;
    files: {
        status: "unknown" | "deleted" | "added" | "modified" | "renamed" | "copied" | "type_changed" | "untracked";
        path: string;
        sizeBytes: number | null;
        warnings: {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }[];
        untracked: boolean;
        staged: boolean;
        unstaged: boolean;
        additions: number;
        deletions: number;
        binary: boolean;
        oversized: boolean;
        truncated: boolean;
        oldPath: string | null;
        patches: {
            kind: "head" | "untracked" | "staged" | "unstaged";
            warnings: {
                code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
                message: string;
                path: string | null;
            }[];
            patch: string | null;
            additions: number;
            deletions: number;
            binary: boolean;
            oversized: boolean;
            truncated: boolean;
        }[];
    }[];
    warnings: {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }[];
    workspaceId: string;
    view: "working-tree" | "head";
    includeUntracked: boolean;
    paths: string[];
    truncated: boolean;
    defaultBaseRef: string | null;
    headSha: string | null;
    stats: {
        additions: number;
        deletions: number;
        fileCount: number;
        stagedFileCount: number;
        unstagedFileCount: number;
        untrackedFileCount: number;
        binaryFileCount: number;
        oversizedFileCount: number;
        truncatedFileCount: number;
    };
    caps: {
        maxFiles: number;
        maxFileBytes: number;
        maxPatchBytes: number;
        maxTotalPatchBytes: number;
    };
}, {
    companyId: string;
    baseRef: string | null;
    files: {
        status: "unknown" | "deleted" | "added" | "modified" | "renamed" | "copied" | "type_changed" | "untracked";
        path: string;
        sizeBytes: number | null;
        warnings: {
            code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
            message: string;
            path: string | null;
        }[];
        untracked: boolean;
        staged: boolean;
        unstaged: boolean;
        additions: number;
        deletions: number;
        binary: boolean;
        oversized: boolean;
        truncated: boolean;
        oldPath: string | null;
        patches: {
            kind: "head" | "untracked" | "staged" | "unstaged";
            warnings: {
                code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
                message: string;
                path: string | null;
            }[];
            patch: string | null;
            additions: number;
            deletions: number;
            binary: boolean;
            oversized: boolean;
            truncated: boolean;
        }[];
    }[];
    warnings: {
        code: "base_ref_missing" | "base_ref_invalid" | "binary_file" | "file_count_truncated" | "file_oversized" | "git_command_failed" | "missing_cwd" | "non_git_workspace" | "patch_truncated" | "path_filter_invalid" | "symlink_target_outside_workspace" | "workspace_path_invalid";
        message: string;
        path: string | null;
    }[];
    workspaceId: string;
    view: "working-tree" | "head";
    includeUntracked: boolean;
    paths: string[];
    truncated: boolean;
    defaultBaseRef: string | null;
    headSha: string | null;
    stats: {
        additions: number;
        deletions: number;
        fileCount: number;
        stagedFileCount: number;
        unstagedFileCount: number;
        untrackedFileCount: number;
        binaryFileCount: number;
        oversizedFileCount: number;
        truncatedFileCount: number;
    };
    caps: {
        maxFiles: number;
        maxFileBytes: number;
        maxPatchBytes: number;
        maxTotalPatchBytes: number;
    };
}>;
export type WorkspaceDiffView = z.infer<typeof workspaceDiffViewSchema>;
export type WorkspaceDiffFileStatus = z.infer<typeof workspaceDiffFileStatusSchema>;
export type WorkspaceDiffPatchKind = z.infer<typeof workspaceDiffPatchKindSchema>;
export type WorkspaceDiffWarningCode = z.infer<typeof workspaceDiffWarningCodeSchema>;
export type WorkspaceDiffQueryOptions = z.infer<typeof workspaceDiffQuerySchema>;
export type WorkspaceDiffWarning = z.infer<typeof workspaceDiffWarningSchema>;
export type WorkspaceDiffCaps = z.infer<typeof workspaceDiffCapsSchema>;
export type WorkspaceDiffFilePatch = z.infer<typeof workspaceDiffFilePatchSchema>;
export type WorkspaceDiffFile = z.infer<typeof workspaceDiffFileSchema>;
export type WorkspaceDiffStats = z.infer<typeof workspaceDiffStatsSchema>;
export type WorkspaceDiffResponse = z.infer<typeof workspaceDiffResponseSchema>;
//# sourceMappingURL=contracts.d.ts.map