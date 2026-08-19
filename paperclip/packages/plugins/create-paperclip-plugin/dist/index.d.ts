declare const VALID_TEMPLATES: readonly ["default", "connector", "workspace", "environment"];
type PluginTemplate = (typeof VALID_TEMPLATES)[number];
export interface ScaffoldPluginOptions {
    pluginName: string;
    outputDir: string;
    template?: PluginTemplate;
    displayName?: string;
    description?: string;
    author?: string;
    category?: "connector" | "workspace" | "automation" | "ui" | "environment";
    sdkPath?: string;
}
/** Validate npm-style plugin package names (scoped or unscoped). */
export declare function isValidPluginName(name: string): boolean;
export declare function shellQuote(value: string): string;
/**
 * Generate a complete Paperclip plugin starter project.
 *
 * Output includes manifest/worker/UI entries, SDK harness tests, bundler presets,
 * and a local dev server script for hot-reload workflow.
 */
export declare function scaffoldPluginProject(options: ScaffoldPluginOptions): string;
export {};
//# sourceMappingURL=index.d.ts.map