import type { WorkspaceDiffFile, WorkspaceDiffFilePatch, WorkspaceDiffResponse, WorkspaceDiffWarning } from "./contracts.js";
export type DiffRenderMode = "unified" | "split";
export interface DiffPatchViewModel {
    kind: WorkspaceDiffFilePatch["kind"];
    patch: string | null;
    lineCount: number;
    additions: number;
    deletions: number;
    binary: boolean;
    oversized: boolean;
    truncated: boolean;
    warnings: WorkspaceDiffWarning[];
}
export interface DiffFileViewModel {
    path: string;
    oldPath: string | null;
    status: WorkspaceDiffFile["status"];
    additions: number;
    deletions: number;
    binary: boolean;
    oversized: boolean;
    truncated: boolean;
    warnings: WorkspaceDiffWarning[];
    patchKinds: WorkspaceDiffFilePatch["kind"][];
    patches: DiffPatchViewModel[];
    patch: string | null;
    lineCount: number;
    longDiff: boolean;
}
export interface DiffSummaryViewModel {
    changedLabel: string;
    lineLabel: string;
    warningCount: number;
    truncated: boolean;
}
export declare const LONG_DIFF_LINE_THRESHOLD = 400;
export declare function statusLabel(status: WorkspaceDiffFile["status"]): string;
export declare function fileName(filePath: string): string;
export declare function buildFilePatches(file: WorkspaceDiffFile): DiffPatchViewModel[];
export declare function buildFilePatch(file: WorkspaceDiffFile): string | null;
export declare function isLongDiffFile(file: Pick<DiffFileViewModel, "lineCount">): boolean;
export declare function toFileViewModels(diff: WorkspaceDiffResponse | null | undefined): DiffFileViewModel[];
export declare function diffSummary(diff: WorkspaceDiffResponse | null | undefined): DiffSummaryViewModel;
export declare function nextExpandedFileSet(current: ReadonlySet<string>, filePath: string): Set<string>;
export declare function initialExpandedFileSet(files: readonly DiffFileViewModel[]): Set<string>;
//# sourceMappingURL=diff-model.d.ts.map