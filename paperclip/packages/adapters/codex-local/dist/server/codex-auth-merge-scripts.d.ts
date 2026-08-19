import type { SandboxManagedRuntimeAssetProvision } from "@paperclipai/adapter-utils/sandbox-managed-runtime";
export declare const CODEX_AUTH_MERGE_EXTRACT_SCRIPT_NAME = "codex-auth-merge-extract.sh";
export declare const CODEX_AUTH_MERGE_DECISION_SCRIPT_NAME = "codex-auth-merge-decision.cjs";
/**
 * Builds the inbound (host→sandbox) provisioning contribution for the Codex
 * managed-home asset: stage the two merge scripts into the runtime root and run
 * the merge-extract script instead of a plain `tar -xf`, so a sandbox that
 * already carries a Codex `auth.json` keeps whichever credential is newer.
 *
 * This is behaviour-identical to the extraction the sandbox core previously
 * hardcoded for `adapterKey === "codex" && assetKey === "home"`.
 */
export declare function buildCodexAuthInboundProvision(): SandboxManagedRuntimeAssetProvision;
//# sourceMappingURL=codex-auth-merge-scripts.d.ts.map