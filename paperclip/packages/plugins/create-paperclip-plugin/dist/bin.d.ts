#!/usr/bin/env node
interface RunCliDeps {
    cwd?: string;
    stdout?: (message: string) => void;
    stderr?: (message: string) => void;
    exit?: (code: number) => never;
}
/** CLI wrapper for `scaffoldPluginProject`. */
export declare function runCli(argv?: string[], deps?: RunCliDeps): string | undefined;
export {};
//# sourceMappingURL=bin.d.ts.map