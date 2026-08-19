// src/ui/app.tsx
import {
  AssigneePicker,
  FileTree,
  IssuesList as PluginIssuesList,
  ManagedRoutinesList as PluginManagedRoutinesList,
  MarkdownBlock,
  MarkdownEditor,
  ProjectPicker,
  usePluginAction,
  usePluginData,
  usePluginStream,
  usePluginToast,
  useHostLocation,
  useHostNavigation
} from "@paperclipai/plugin-sdk/ui";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

// src/ui/issue-attachments.ts
function readIngestOperationIssueId(result) {
  const issueId = result?.operation?.issue?.id;
  if (typeof issueId === "string" && issueId.trim()) return issueId;
  throw new Error("Ingest operation did not return an issue id; the dropped file could not be attached.");
}
async function readUploadError(response) {
  const body = await response.json().catch(() => null);
  if (body && typeof body === "object") {
    const error = body.error;
    if (typeof error === "string" && error.trim()) return error;
    const message = body.message;
    if (typeof message === "string" && message.trim()) return message;
  }
  return `Attachment upload failed with HTTP ${response.status}.`;
}
async function uploadIssueAttachmentFile(input) {
  const fetchImpl = input.fetchImpl ?? fetch;
  const form = new FormData();
  form.append("file", input.file);
  const response = await fetchImpl(
    `/api/companies/${encodeURIComponent(input.companyId)}/issues/${encodeURIComponent(input.issueId)}/attachments`,
    {
      method: "POST",
      credentials: "include",
      body: form
    }
  );
  if (!response.ok) {
    throw new Error(await readUploadError(response));
  }
  return response.json();
}

// src/ui/app.tsx
import { Fragment, jsx, jsxs } from "react/jsx-runtime";
var tokens = {
  border: "var(--border, oklch(0.269 0 0))",
  card: "var(--card, oklch(0.205 0 0))",
  bg: "var(--background, oklch(0.145 0 0))",
  fg: "var(--foreground, oklch(0.985 0 0))",
  muted: "var(--muted-foreground, oklch(0.708 0 0))",
  accent: "var(--accent, oklch(0.269 0 0))",
  primary: "var(--primary, oklch(0.985 0 0))",
  primaryFg: "var(--primary-foreground, oklch(0.205 0 0))",
  destructive: "var(--destructive, oklch(0.637 0.237 25.331))",
  pluginBg: "oklch(0.3 0.06 70)",
  pluginFg: "oklch(0.92 0.08 80)",
  pluginBorder: "oklch(0.55 0.15 70)",
  hiddenOpBg: "oklch(0.27 0.04 280)",
  hiddenOpFg: "oklch(0.85 0.08 280)",
  hiddenOpBorder: "oklch(0.45 0.1 280)",
  callout: { bg: "oklch(0.2 0.04 250)", fg: "oklch(0.85 0.08 250)", border: "oklch(0.4 0.1 250)" },
  statusDone: "oklch(0.65 0.16 145)",
  statusRunning: "oklch(0.7 0.13 200)",
  statusBlocked: "oklch(0.6 0.21 25)",
  statusInProgress: "oklch(0.58 0.18 280)",
  statusTodo: "oklch(0.6 0.17 250)",
  statusPaused: "oklch(0.72 0.15 70)"
};
var toneStyles = {
  default: { background: "var(--secondary, oklch(0.269 0 0))", color: tokens.fg, border: `1px solid ${tokens.border}` },
  todo: { background: "oklch(0.27 0.06 250)", color: "oklch(0.85 0.1 250)" },
  in_progress: { background: "oklch(0.27 0.06 280)", color: "oklch(0.85 0.1 280)" },
  in_review: { background: "oklch(0.27 0.07 305)", color: "oklch(0.85 0.1 305)" },
  done: { background: "oklch(0.27 0.06 145)", color: "oklch(0.85 0.1 145)" },
  blocked: { background: "oklch(0.27 0.08 25)", color: "oklch(0.82 0.13 25)" },
  running: { background: "oklch(0.27 0.06 200)", color: "oklch(0.83 0.11 200)" },
  paused: { background: "oklch(0.27 0.07 70)", color: "oklch(0.85 0.1 70)" },
  failed: { background: "oklch(0.27 0.08 25)", color: "oklch(0.82 0.13 25)" },
  queued: { background: "oklch(0.27 0.06 250)", color: "oklch(0.85 0.1 250)" }
};
var fontStack = `ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`;
var mobileMediaQuery = "(max-width: 767px)";
var PLUGIN_ID = "paperclipai.plugin-llm-wiki";
var WIKI_SIDEBAR_NAV_STATE_KEY = "paperclipWikiSidebarTreePath";
var ROUTE_SIDEBAR_EXPANDED_STORAGE_PREFIX = `${PLUGIN_ID}:route-sidebar-expanded:v2`;
var WIKI_TOC_STICKY_TOP = 88;
var WIKI_SPACE_PREFETCH_LIMIT = 8;
var DEFAULT_ROUTE_SIDEBAR_EXPANDED_PATHS = [
  "wiki",
  "wiki/sources",
  "wiki/projects",
  "wiki/entities",
  "wiki/concepts",
  "wiki/synthesis"
];
var DEFAULT_SPACE_SLUG = "default";
function Badge({ children, tone = "default", style }) {
  return /* @__PURE__ */ jsx("span", { style: {
    display: "inline-flex",
    alignItems: "center",
    gap: 4,
    padding: "2px 8px",
    borderRadius: 999,
    fontSize: 11,
    fontWeight: 500,
    whiteSpace: "nowrap",
    ...toneStyles[tone],
    ...style
  }, children });
}
function HiddenOpBadge() {
  return /* @__PURE__ */ jsx("span", { style: {
    display: "inline-flex",
    alignItems: "center",
    gap: 4,
    padding: "2px 8px",
    borderRadius: 999,
    fontSize: 11,
    fontWeight: 500,
    background: tokens.hiddenOpBg,
    color: tokens.hiddenOpFg,
    border: `1px solid ${tokens.hiddenOpBorder}`
  }, children: "\u{1F4D6} wiki task" });
}
function StatusIcon({ status }) {
  const map = {
    done: { color: tokens.statusDone, filled: true },
    in_progress: { color: tokens.statusInProgress },
    running: { color: tokens.statusRunning, pulse: true },
    queued: { color: tokens.statusTodo },
    todo: { color: tokens.statusTodo },
    blocked: { color: tokens.statusBlocked },
    failed: { color: tokens.statusBlocked },
    paused: { color: tokens.statusPaused }
  };
  const tone = map[status] ?? { color: tokens.muted };
  return /* @__PURE__ */ jsx("span", { style: {
    width: 12,
    height: 12,
    flexShrink: 0,
    borderRadius: "50%",
    border: `2px solid ${tone.color}`,
    background: tone.filled ? tone.color : "transparent",
    animation: tone.pulse ? "pcWikiPulse 1.6s infinite" : void 0
  }, "aria-hidden": true });
}
function Card({ children, style }) {
  return /* @__PURE__ */ jsx("section", { style: {
    background: tokens.card,
    border: `1px solid ${tokens.border}`,
    borderRadius: 8,
    overflow: "hidden",
    minWidth: 0,
    ...style
  }, children });
}
function CardHeader({ title, right, badges }) {
  return /* @__PURE__ */ jsxs("div", { style: {
    padding: "12px 16px",
    borderBottom: `1px solid ${tokens.border}`,
    display: "flex",
    alignItems: "center",
    flexWrap: "wrap",
    gap: 12,
    minWidth: 0
  }, children: [
    /* @__PURE__ */ jsx("h3", { style: { margin: 0, fontSize: 14, fontWeight: 600, minWidth: 0, overflow: "hidden", textOverflow: "ellipsis" }, children: title }),
    badges,
    right ? /* @__PURE__ */ jsx("div", { style: { marginLeft: "auto", minWidth: 0, maxWidth: "100%" }, children: right }) : null
  ] });
}
function CardBody({ children, padding = 16 }) {
  return /* @__PURE__ */ jsx("div", { style: { padding }, children });
}
var unfilledSurfaceStyle = {
  background: "transparent"
};
function PropRow({ label, value }) {
  return /* @__PURE__ */ jsxs("div", { style: { display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", padding: "4px 0", fontSize: 13, gap: 12, minWidth: 0 }, children: [
    /* @__PURE__ */ jsx("span", { style: { color: tokens.muted, fontSize: 12, flexShrink: 0 }, children: label }),
    /* @__PURE__ */ jsx("span", { style: { flex: "1 1 160px", minWidth: 0, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "normal", overflowWrap: "anywhere", textAlign: "right" }, children: value })
  ] });
}
function Tiny({ children, style }) {
  return /* @__PURE__ */ jsx("div", { style: { fontSize: 11, color: tokens.muted, ...style }, children });
}
function Mono({ children, style }) {
  return /* @__PURE__ */ jsx("span", { style: { fontFamily: "ui-monospace, SFMono-Regular, monospace", fontSize: 12, overflowWrap: "anywhere", wordBreak: "break-word", ...style }, children });
}
function Button({
  variant = "default",
  size = "md",
  disabled,
  loading,
  onClick,
  children,
  type = "button",
  style,
  title
}) {
  const palette = {
    primary: {
      background: tokens.primary,
      color: tokens.primaryFg,
      border: `1px solid transparent`
    },
    default: {
      background: tokens.card,
      color: tokens.fg,
      border: `1px solid ${tokens.border}`
    },
    ghost: {
      background: "transparent",
      color: tokens.fg,
      border: `1px solid transparent`
    },
    destructive: {
      background: "transparent",
      color: "oklch(0.7 0.2 25)",
      border: `1px solid oklch(0.5 0.18 25)`
    }
  };
  return /* @__PURE__ */ jsx(
    "button",
    {
      type,
      title,
      disabled: disabled || loading,
      onClick,
      style: {
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        padding: size === "sm" ? "3px 8px" : "6px 12px",
        borderRadius: 6,
        fontSize: size === "sm" ? 11 : 13,
        fontWeight: 500,
        cursor: disabled || loading ? "not-allowed" : "pointer",
        opacity: disabled || loading ? 0.5 : 1,
        fontFamily: fontStack,
        minWidth: 0,
        whiteSpace: "nowrap",
        ...palette[variant],
        ...style
      },
      children
    }
  );
}
function TextInput(props) {
  return /* @__PURE__ */ jsx(
    "input",
    {
      ...props,
      style: {
        background: "oklch(0.2 0 0)",
        border: `1px solid ${tokens.border}`,
        borderRadius: 6,
        padding: "6px 10px",
        fontSize: 13,
        color: tokens.fg,
        width: "100%",
        boxSizing: "border-box",
        minWidth: 0,
        fontFamily: fontStack,
        ...props.style
      }
    }
  );
}
function TextArea(props) {
  return /* @__PURE__ */ jsx(
    "textarea",
    {
      ...props,
      style: {
        background: "oklch(0.2 0 0)",
        border: `1px solid ${tokens.border}`,
        borderRadius: 6,
        padding: "6px 10px",
        fontSize: 13,
        color: tokens.fg,
        width: "100%",
        boxSizing: "border-box",
        minWidth: 0,
        minHeight: 96,
        fontFamily: fontStack,
        resize: "vertical",
        ...props.style
      }
    }
  );
}
function AutosaveStatusLabel({ status, error }) {
  if (status === "saving") return /* @__PURE__ */ jsx(Tiny, { children: "Saving\u2026" });
  if (status === "saved") return /* @__PURE__ */ jsx(Tiny, { children: "Saved" });
  if (status === "dirty") return /* @__PURE__ */ jsx(Tiny, { children: "Unsaved changes" });
  if (status === "error") return /* @__PURE__ */ jsx(Tiny, { style: { color: "oklch(0.7 0.2 25)" }, children: error ?? "Autosave failed" });
  return /* @__PURE__ */ jsx(Tiny, { children: "Autosave on" });
}
function AutosaveMarkdownEditor({
  value,
  placeholder,
  minHeight,
  resetKey,
  onSave,
  onStatusChange
}) {
  const [draft, setDraft] = useState(value);
  const [lastSaved, setLastSaved] = useState(value);
  const [status, setStatus] = useState("idle");
  const [error, setError] = useState(null);
  useEffect(() => {
    setDraft(value);
    setLastSaved(value);
    setStatus("idle");
    setError(null);
    onStatusChange?.("idle");
  }, [onStatusChange, resetKey, value]);
  useEffect(() => {
    if (draft === lastSaved) return;
    setStatus("dirty");
    setError(null);
    onStatusChange?.("dirty");
    const timeout = window.setTimeout(async () => {
      setStatus("saving");
      onStatusChange?.("saving");
      try {
        await onSave(draft);
        setLastSaved(draft);
        setStatus("saved");
        onStatusChange?.("saved");
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        setError(message);
        setStatus("error");
        onStatusChange?.("error");
      }
    }, 800);
    return () => window.clearTimeout(timeout);
  }, [draft, lastSaved, onSave, onStatusChange]);
  return /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 6, minWidth: 0 }, children: [
    /* @__PURE__ */ jsx(
      MarkdownEditor,
      {
        value: draft,
        onChange: setDraft,
        placeholder,
        bordered: true,
        contentClassName: "min-h-[260px]",
        className: "pc-wiki-markdown-editor"
      }
    ),
    /* @__PURE__ */ jsx("style", { children: `
        .pc-wiki-markdown-editor .mdxeditor-root-contenteditable {
          min-height: ${minHeight ?? 260}px;
        }
      ` }),
    /* @__PURE__ */ jsx(AutosaveStatusLabel, { status, error })
  ] });
}
function Callout({ children, tone = "info" }) {
  const palette = tone === "danger" ? { bg: "oklch(0.22 0.06 25)", fg: "oklch(0.85 0.12 25)", border: "oklch(0.45 0.12 25)" } : tone === "warn" ? { bg: "oklch(0.22 0.06 70)", fg: "oklch(0.85 0.1 70)", border: "oklch(0.45 0.12 70)" } : tokens.callout;
  return /* @__PURE__ */ jsx("div", { style: {
    background: palette.bg,
    color: palette.fg,
    border: `1px solid ${palette.border}`,
    borderRadius: 8,
    padding: "12px 14px",
    fontSize: 13,
    lineHeight: 1.55
  }, children });
}
function Divider() {
  return /* @__PURE__ */ jsx("div", { style: { height: 1, background: tokens.border, margin: "16px 0" } });
}
function useMediaQuery(query) {
  const getSnapshot = () => {
    if (typeof window === "undefined" || typeof window.matchMedia !== "function") return false;
    return window.matchMedia(query).matches;
  };
  const [matches, setMatches] = useState(getSnapshot);
  useEffect(() => {
    if (typeof window === "undefined" || typeof window.matchMedia !== "function") return;
    const mediaQuery = window.matchMedia(query);
    const handleChange = (event) => setMatches(event.matches);
    setMatches(mediaQuery.matches);
    mediaQuery.addEventListener("change", handleChange);
    return () => mediaQuery.removeEventListener("change", handleChange);
  }, [query]);
  return matches;
}
function useIsMobileLayout() {
  return useMediaQuery(mobileMediaQuery);
}
function useOverview(companyId) {
  const params = useMemo(() => companyId ? { companyId } : void 0, [companyId]);
  return usePluginData("overview", params);
}
function useSettings(companyId) {
  const params = useMemo(() => companyId ? { companyId } : void 0, [companyId]);
  return usePluginData("settings", params);
}
function usePages(companyId, opts = {}) {
  const params = useMemo(() => {
    if (!companyId) return void 0;
    const next = { companyId, includeRaw: opts.includeRaw ?? true };
    if (opts.spaceSlug && opts.spaceSlug !== DEFAULT_SPACE_SLUG) next.spaceSlug = opts.spaceSlug;
    else if (opts.spaceSlug === DEFAULT_SPACE_SLUG) next.spaceSlug = DEFAULT_SPACE_SLUG;
    return next;
  }, [companyId, opts.includeRaw, opts.spaceSlug]);
  return usePluginData("pages", params);
}
function useSpaces(companyId) {
  const params = useMemo(() => companyId ? { companyId } : void 0, [companyId]);
  return usePluginData("spaces", params);
}
function useSpaceFolderStatus(companyId, spaceSlug) {
  const params = useMemo(() => {
    if (!companyId || !spaceSlug) return void 0;
    return { companyId, spaceSlug };
  }, [companyId, spaceSlug]);
  return usePluginData("space", params);
}
function usePaperclipIngestionProfile(companyId, spaceSlug) {
  const params = useMemo(() => {
    if (!companyId || !spaceSlug) return void 0;
    return { companyId, spaceSlug };
  }, [companyId, spaceSlug]);
  return usePluginData("paperclip-ingestion-profile", params);
}
function usePageContent(companyId, path, spaceSlug) {
  const params = useMemo(() => {
    if (!companyId || !path) return void 0;
    const next = { companyId, path };
    if (spaceSlug) next.spaceSlug = spaceSlug;
    return next;
  }, [companyId, path, spaceSlug]);
  return usePluginData("page-content", params);
}
function useOperations(companyId, filter = {}) {
  const params = useMemo(() => {
    if (!companyId) return void 0;
    return {
      companyId,
      operationType: filter.operationType ?? null,
      status: filter.status ?? null,
      spaceSlug: filter.spaceSlug ?? null
    };
  }, [companyId, filter.operationType, filter.status, filter.spaceSlug]);
  return usePluginData("operations", params);
}
function useDistillationOverview(companyId) {
  const params = useMemo(() => companyId ? { companyId } : void 0, [companyId]);
  return usePluginData("distillation-overview", params);
}
function useDistillationProvenance(companyId, pagePath) {
  const params = useMemo(() => {
    if (!companyId || !pagePath) return void 0;
    return { companyId, pagePath };
  }, [companyId, pagePath]);
  return usePluginData("distillation-page-provenance", params);
}
function stripYamlInlineComment(value) {
  let quote = null;
  for (let i = 0; i < value.length; i++) {
    const char = value[i];
    const previous = value[i - 1];
    if ((char === "'" || char === '"') && previous !== "\\") {
      quote = quote === char ? null : quote ?? char;
    }
    if (char === "#" && quote === null && (i === 0 || /\s/.test(previous ?? ""))) {
      return value.slice(0, i).trimEnd();
    }
  }
  return value.trim();
}
function unquoteYamlScalar(value) {
  const trimmed = stripYamlInlineComment(value).trim();
  if (trimmed.length >= 2) {
    const first = trimmed[0];
    const last = trimmed[trimmed.length - 1];
    if (first === '"' && last === '"' || first === "'" && last === "'") {
      return trimmed.slice(1, -1);
    }
  }
  return trimmed;
}
function parseYamlInlineArray(value) {
  const trimmed = stripYamlInlineComment(value).trim();
  if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return null;
  const body = trimmed.slice(1, -1).trim();
  if (!body) return [];
  const items = [];
  let current = "";
  let quote = null;
  for (let i = 0; i < body.length; i++) {
    const char = body[i];
    const previous = body[i - 1];
    if ((char === "'" || char === '"') && previous !== "\\") {
      quote = quote === char ? null : quote ?? char;
      current += char;
      continue;
    }
    if (char === "," && quote === null) {
      const item2 = unquoteYamlScalar(current);
      if (item2) items.push(item2);
      current = "";
      continue;
    }
    current += char;
  }
  const item = unquoteYamlScalar(current);
  if (item) items.push(item);
  return items;
}
function parseFrontmatterValue(rawValue, followingList) {
  const inlineArray = parseYamlInlineArray(rawValue);
  if (inlineArray) return inlineArray;
  if (!rawValue.trim() && followingList.length > 0) return followingList;
  return unquoteYamlScalar(rawValue);
}
function parseWikiFrontmatterBlock(block) {
  const lines = block.replace(/\r\n/g, "\n").split("\n");
  const properties = [];
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (!line.trim() || line.trimStart().startsWith("#")) continue;
    if (/^\s+-\s+/.test(line)) continue;
    const match = line.match(/^([A-Za-z0-9_-]+):(?:\s*(.*))?$/);
    if (!match) continue;
    const key = match[1];
    const rawValue = match[2] ?? "";
    const followingList = [];
    let cursor = i + 1;
    while (cursor < lines.length) {
      const listMatch = lines[cursor]?.match(/^\s+-\s+(.+)$/);
      if (!listMatch) break;
      followingList.push(unquoteYamlScalar(listMatch[1] ?? ""));
      cursor += 1;
    }
    if (!rawValue.trim() && followingList.length > 0) i = cursor - 1;
    const value = parseFrontmatterValue(rawValue, followingList);
    if (Array.isArray(value) ? value.length > 0 : value.length > 0) {
      properties.push({ key, value });
    }
  }
  return properties;
}
function parseWikiMarkdown(contents) {
  const normalized = contents.replace(/^\uFEFF/, "").replace(/\r\n/g, "\n");
  if (!normalized.startsWith("---\n")) {
    return { body: contents, frontmatter: [] };
  }
  const closingMatch = normalized.slice(4).match(/\n(?:---|\.\.\.)[ \t]*(?:\n|$)/);
  if (!closingMatch || closingMatch.index == null) {
    return { body: contents, frontmatter: [] };
  }
  const frontmatterBlock = normalized.slice(4, closingMatch.index + 4);
  const bodyStart = 4 + closingMatch.index + closingMatch[0].length;
  return {
    body: normalized.slice(bodyStart).replace(/^\n+/, ""),
    frontmatter: parseWikiFrontmatterBlock(frontmatterBlock)
  };
}
function stripMarkdownHeadingSyntax(text) {
  return text.replace(/\\([\\`*_{}\[\]()#+\-.!|>])/g, "$1").replace(/!\[([^\]]*)\]\([^)]+\)/g, "$1").replace(/\[([^\]]+)\]\([^)]+\)/g, "$1").replace(/\[\[([^\]|]+)\|([^\]]+)\]\]/g, "$2").replace(/\[\[([^\]]+)\]\]/g, "$1").replace(/`([^`]+)`/g, "$1").replace(/[*_~]+/g, "").replace(/<[^>]+>/g, "").trim();
}
function slugifyWikiHeading(text) {
  const slug = stripMarkdownHeadingSyntax(text).toLowerCase().replace(/&[a-z0-9#]+;/g, "").replace(/[^a-z0-9\s-]/g, "").trim().replace(/\s+/g, "-").replace(/-+/g, "-");
  return slug || "section";
}
function extractWikiTocHeadings(markdownBody) {
  const lines = markdownBody.replace(/\r\n/g, "\n").split("\n");
  const headings = [];
  const usedIds = /* @__PURE__ */ new Map();
  let fenced = false;
  for (const line of lines) {
    if (/^\s*(```|~~~)/.test(line)) {
      fenced = !fenced;
      continue;
    }
    if (fenced) continue;
    const match = line.match(/^\s{0,3}(#{2,4})\s+(.+?)\s*#*\s*$/);
    if (!match) continue;
    const text = stripMarkdownHeadingSyntax(match[2] ?? "");
    if (!text) continue;
    const baseId = slugifyWikiHeading(text);
    const count = usedIds.get(baseId) ?? 0;
    usedIds.set(baseId, count + 1);
    headings.push({
      id: count === 0 ? baseId : `${baseId}-${count + 1}`,
      text,
      level: match[1]?.length ?? 2
    });
  }
  return headings;
}
function makeLucideIcon(paths) {
  return function LucideIcon({ size = 16 }) {
    return /* @__PURE__ */ jsx(
      "svg",
      {
        "aria-hidden": "true",
        viewBox: "0 0 24 24",
        fill: "none",
        stroke: "currentColor",
        strokeWidth: "2",
        strokeLinecap: "round",
        strokeLinejoin: "round",
        style: { width: size, height: size, display: "block" },
        children: paths
      }
    );
  };
}
var BookOpenIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "M12 7v14" }),
    /* @__PURE__ */ jsx("path", { d: "M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z" })
  ] })
);
var DownloadCloudIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "M12 13v8l-4-4" }),
    /* @__PURE__ */ jsx("path", { d: "m12 21 4-4" }),
    /* @__PURE__ */ jsx("path", { d: "M4.393 15.269A7 7 0 1 1 15.71 8.071" })
  ] })
);
var MessageSquareTextIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" }),
    /* @__PURE__ */ jsx("path", { d: "M13 8H7" }),
    /* @__PURE__ */ jsx("path", { d: "M17 12H7" })
  ] })
);
var ListChecksIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "m3 17 2 2 4-4" }),
    /* @__PURE__ */ jsx("path", { d: "m3 7 2 2 4-4" }),
    /* @__PURE__ */ jsx("path", { d: "M13 6h8" }),
    /* @__PURE__ */ jsx("path", { d: "M13 12h8" }),
    /* @__PURE__ */ jsx("path", { d: "M13 18h8" })
  ] })
);
var HistoryIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" }),
    /* @__PURE__ */ jsx("path", { d: "M3 3v5h5" }),
    /* @__PURE__ */ jsx("path", { d: "M12 7v5l4 2" })
  ] })
);
var SlidersHorizontalIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("line", { x1: "21", x2: "14", y1: "4", y2: "4" }),
    /* @__PURE__ */ jsx("line", { x1: "10", x2: "3", y1: "4", y2: "4" }),
    /* @__PURE__ */ jsx("line", { x1: "21", x2: "12", y1: "12", y2: "12" }),
    /* @__PURE__ */ jsx("line", { x1: "8", x2: "3", y1: "12", y2: "12" }),
    /* @__PURE__ */ jsx("line", { x1: "21", x2: "16", y1: "20", y2: "20" }),
    /* @__PURE__ */ jsx("line", { x1: "12", x2: "3", y1: "20", y2: "20" }),
    /* @__PURE__ */ jsx("line", { x1: "14", x2: "14", y1: "2", y2: "6" }),
    /* @__PURE__ */ jsx("line", { x1: "8", x2: "8", y1: "10", y2: "14" }),
    /* @__PURE__ */ jsx("line", { x1: "16", x2: "16", y1: "18", y2: "22" })
  ] })
);
var FolderOpenIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "M6 14h.01" }),
    /* @__PURE__ */ jsx("path", { d: "M3 6h5l2 2h11v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" })
  ] })
);
var ActivityIcon = makeLucideIcon(
  /* @__PURE__ */ jsx("path", { d: "M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.5.5 0 0 1-.96 0L9.24 3.18a.5.5 0 0 0-.96 0l-2.35 8.36A2 2 0 0 1 4 13H2" })
);
var InfoIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("circle", { cx: "12", cy: "12", r: "10" }),
    /* @__PURE__ */ jsx("path", { d: "M12 16v-4" }),
    /* @__PURE__ */ jsx("path", { d: "M12 8h.01" })
  ] })
);
var SparklesIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3z" }),
    /* @__PURE__ */ jsx("path", { d: "M5 3v4" }),
    /* @__PURE__ */ jsx("path", { d: "M19 17v4" }),
    /* @__PURE__ */ jsx("path", { d: "M3 5h4" }),
    /* @__PURE__ */ jsx("path", { d: "M17 19h4" })
  ] })
);
var RefreshIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "M3 12a9 9 0 0 1 15-6.7L21 8" }),
    /* @__PURE__ */ jsx("path", { d: "M21 3v5h-5" }),
    /* @__PURE__ */ jsx("path", { d: "M21 12a9 9 0 0 1-15 6.7L3 16" }),
    /* @__PURE__ */ jsx("path", { d: "M3 21v-5h5" })
  ] })
);
var ExternalLinkIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "M15 3h6v6" }),
    /* @__PURE__ */ jsx("path", { d: "M10 14 21 3" }),
    /* @__PURE__ */ jsx("path", { d: "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" })
  ] })
);
var ClockIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("circle", { cx: "12", cy: "12", r: "10" }),
    /* @__PURE__ */ jsx("path", { d: "M12 6v6l4 2" })
  ] })
);
var AlertTriangleIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3z" }),
    /* @__PURE__ */ jsx("path", { d: "M12 9v4" }),
    /* @__PURE__ */ jsx("path", { d: "M12 17h.01" })
  ] })
);
var XIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "M18 6 6 18" }),
    /* @__PURE__ */ jsx("path", { d: "m6 6 12 12" })
  ] })
);
var ChevronLeftIcon = makeLucideIcon(/* @__PURE__ */ jsx("path", { d: "m15 18-6-6 6-6" }));
var ChevronRightIcon = makeLucideIcon(/* @__PURE__ */ jsx("path", { d: "m9 6 6 6-6 6" }));
var ChevronDownIcon = makeLucideIcon(/* @__PURE__ */ jsx("path", { d: "m6 9 6 6 6-6" }));
var PlusIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "M12 5v14" }),
    /* @__PURE__ */ jsx("path", { d: "M5 12h14" })
  ] })
);
var PlusCircleIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("circle", { cx: "12", cy: "12", r: "9" }),
    /* @__PURE__ */ jsx("path", { d: "M12 8v8" }),
    /* @__PURE__ */ jsx("path", { d: "M8 12h8" })
  ] })
);
var FolderIcon = makeLucideIcon(/* @__PURE__ */ jsx("path", { d: "M3 7h6l2 2h10v10H3z" }));
var MoreHorizontalIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("circle", { cx: "6", cy: "12", r: "1" }),
    /* @__PURE__ */ jsx("circle", { cx: "12", cy: "12", r: "1" }),
    /* @__PURE__ */ jsx("circle", { cx: "18", cy: "12", r: "1" })
  ] })
);
var ArchiveIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "M3 6h18v4H3z" }),
    /* @__PURE__ */ jsx("path", { d: "M5 10v10h14V10" }),
    /* @__PURE__ */ jsx("path", { d: "M10 14h4" })
  ] })
);
var PencilIcon = makeLucideIcon(
  /* @__PURE__ */ jsxs(Fragment, { children: [
    /* @__PURE__ */ jsx("path", { d: "M12 20h9" }),
    /* @__PURE__ */ jsx("path", { d: "M16.5 3.5a2.121 2.121 0 1 1 3 3L7 19l-4 1 1-4z" })
  ] })
);
function SidebarLink({ context }) {
  const hostNavigation = useHostNavigation();
  return /* @__PURE__ */ jsxs(
    "a",
    {
      ...hostNavigation.linkProps("/wiki"),
      className: "flex items-center gap-2.5 px-3 py-2 text-[13px] font-medium text-foreground/80 transition-colors hover:bg-accent/50 hover:text-foreground",
      style: { textDecoration: "none" },
      children: [
        /* @__PURE__ */ jsx("span", { "aria-hidden": "true", className: "shrink-0", children: /* @__PURE__ */ jsx(BookOpenIcon, {}) }),
        /* @__PURE__ */ jsx("span", { className: "flex-1 truncate", children: "Wiki" })
      ]
    }
  );
}
function SettingsPage({ context }) {
  const isMobile = useIsMobileLayout();
  return /* @__PURE__ */ jsx("main", { style: { padding: isMobile ? 16 : 24, maxWidth: isMobile ? "none" : 1040, minWidth: 0, fontFamily: fontStack, color: tokens.fg }, children: /* @__PURE__ */ jsx(SettingsBody, { context }) });
}
var SECTIONS = [
  { key: "browse", label: "Wiki", Icon: BookOpenIcon, description: "Open wiki pages and raw sources from the sidebar." },
  { key: "query", label: "Ask", Icon: MessageSquareTextIcon, description: "Ask the Wiki Maintainer agent a cited question against the local wiki." },
  { key: "ingest", label: "Add Content", Icon: PlusCircleIcon, description: "Capture a new source into the active space and queue an ingest operation." },
  { key: "lint", label: "Lint", Icon: ListChecksIcon, description: "Run structural checks for orphan pages, missing backlinks, and stale provenance." },
  { key: "history", label: "History", Icon: HistoryIcon, description: "Inspect recent LLM Wiki operation issues." },
  { key: "settings", label: "Settings", Icon: SlidersHorizontalIcon, description: "Folder, agent, project, and routine configuration scoped to this company." }
];
var TOP_TOOL_KEYS = /* @__PURE__ */ new Set(["query", "ingest"]);
var BOTTOM_TOOL_KEYS = /* @__PURE__ */ new Set(["history", "settings"]);
var TOP_TOOL_SECTIONS = SECTIONS.filter((section) => TOP_TOOL_KEYS.has(section.key));
var BOTTOM_TOOL_SECTIONS = SECTIONS.filter((section) => BOTTOM_TOOL_KEYS.has(section.key));
var SECTION_KEYS = new Set(SECTIONS.map((s) => s.key));
var LEGACY_SECTION_ALIASES = {
  operations: "history"
};
function isSectionKey(value) {
  return typeof value === "string" && SECTION_KEYS.has(value);
}
function normalizeSectionKey(value) {
  if (typeof value !== "string") return null;
  return LEGACY_SECTION_ALIASES[value] ?? (isSectionKey(value) ? value : null);
}
function readSectionFromSearch(search) {
  const params = new URLSearchParams(search);
  const raw = params.get("section");
  return normalizeSectionKey(raw) ?? "browse";
}
function decodeRouteSegment(segment) {
  try {
    return decodeURIComponent(segment);
  } catch {
    return segment;
  }
}
function readWikiRouteSegments(pathname) {
  const segments = pathname.split("/").filter(Boolean);
  const wikiIndex = segments.findIndex((segment) => decodeRouteSegment(segment).toLowerCase() === "wiki");
  if (wikiIndex === -1) return [];
  return segments.slice(wikiIndex + 1).map(decodeRouteSegment);
}
function readWikiSpaceContext(pathname) {
  const segments = readWikiRouteSegments(pathname);
  if (segments[0] === "spaces" && typeof segments[1] === "string" && segments[1].length > 0) {
    return { spaceSlug: segments[1], rest: segments.slice(2) };
  }
  return { spaceSlug: DEFAULT_SPACE_SLUG, rest: segments };
}
function readActiveSpaceSlugFromLocation(pathname) {
  return readWikiSpaceContext(pathname).spaceSlug;
}
function readSectionFromLocation(pathname, search) {
  const [firstSegment] = readWikiSpaceContext(pathname).rest;
  if (firstSegment === "page") return "browse";
  return normalizeSectionKey(firstSegment) ?? readSectionFromSearch(search);
}
function readSettingsSectionFromLocation(pathname) {
  const [firstSegment, secondSegment] = readWikiSpaceContext(pathname).rest;
  if (firstSegment !== "settings") return "root";
  if (secondSegment === "maintainer" || secondSegment === "project") return "root";
  return SETTINGS_SECTIONS.some((section) => section.key === secondSegment) ? secondSegment : "root";
}
function readSettingsSpaceSlugFromLocation(pathname) {
  const segs = readWikiSpaceContext(pathname).rest;
  if (segs[0] !== "settings" || segs[1] !== "spaces") return null;
  const slug = segs[2];
  return typeof slug === "string" && slug.length > 0 ? slug : null;
}
function buildSpacePrefix(spaceSlug) {
  return spaceSlug && spaceSlug !== DEFAULT_SPACE_SLUG ? `/wiki/spaces/${encodeURIComponent(spaceSlug)}` : `/wiki`;
}
function buildSectionHref(section, spaceSlug = DEFAULT_SPACE_SLUG) {
  const prefix = buildSpacePrefix(spaceSlug);
  return section === "browse" ? prefix : `${prefix}/${section}`;
}
function buildSettingsSectionHref(settingsSection, spaceSlug = DEFAULT_SPACE_SLUG, slug) {
  const prefix = buildSpacePrefix(spaceSlug);
  if (settingsSection === "spaces" && slug) {
    return `${prefix}/settings/spaces/${encodeURIComponent(slug)}`;
  }
  if (settingsSection === "root") return `${prefix}/settings`;
  return `${prefix}/settings/${settingsSection}`;
}
function readSelectedTreePathFromSearch(search) {
  const params = new URLSearchParams(search);
  const raw = params.get("page")?.trim();
  return raw || null;
}
function readSelectedTreePathFromLocation(pathname, search) {
  const [firstSegment, ...rest] = readWikiSpaceContext(pathname).rest;
  if (firstSegment === "page") {
    return treePathFromRouteSegments(rest);
  }
  return readSelectedTreePathFromSearch(search);
}
function buildPageHref(treePath, spaceSlug = DEFAULT_SPACE_SLUG) {
  const prefix = buildSpacePrefix(spaceSlug);
  const encodedPath = routeSegmentsFromTreePath(treePath).map((segment) => encodeURIComponent(segment)).join("/");
  return encodedPath ? `${prefix}/page/${encodedPath}` : prefix;
}
function wikiSidebarNavigationState(treePath) {
  return { [WIKI_SIDEBAR_NAV_STATE_KEY]: treePath };
}
function readSidebarSelectedPathFromNavigationState(state) {
  if (!state || typeof state !== "object") return null;
  const value = state[WIKI_SIDEBAR_NAV_STATE_KEY];
  return typeof value === "string" && value.trim() ? value : null;
}
function routeSidebarExpandedStorageKey(companyId) {
  return `${ROUTE_SIDEBAR_EXPANDED_STORAGE_PREFIX}:${companyId ?? "global"}`;
}
function readRouteSidebarExpandedPaths(storageKey) {
  if (typeof window === "undefined") return new Set(DEFAULT_ROUTE_SIDEBAR_EXPANDED_PATHS);
  try {
    const raw = window.localStorage.getItem(storageKey);
    if (raw === null) return new Set(DEFAULT_ROUTE_SIDEBAR_EXPANDED_PATHS);
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return new Set(DEFAULT_ROUTE_SIDEBAR_EXPANDED_PATHS);
    return new Set(parsed.filter((value) => typeof value === "string" && value.trim().length > 0));
  } catch {
    return new Set(DEFAULT_ROUTE_SIDEBAR_EXPANDED_PATHS);
  }
}
function writeRouteSidebarExpandedPaths(storageKey, paths) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(storageKey, JSON.stringify([...paths].sort()));
  } catch {
  }
}
var ROOT_WIKI_LINK_PAGES = /* @__PURE__ */ new Set(["WIKI.md", "AGENTS.md", "IDEA.md", "index.md", "log.md"]);
function splitWikiLinkTarget(target) {
  const trimmed = target.trim();
  if (!trimmed || /^[a-z][a-z\d+.-]*:/i.test(trimmed) || trimmed.startsWith("//")) return null;
  const hashIndex = trimmed.indexOf("#");
  const rawPath = (hashIndex >= 0 ? trimmed.slice(0, hashIndex) : trimmed).trim().replace(/^\/+/, "");
  if (!rawPath || rawPath.includes("\\") || rawPath.split("/").some((segment) => !segment || segment === "." || segment === "..")) {
    return null;
  }
  const fragment = hashIndex >= 0 ? trimmed.slice(hashIndex + 1).trim() || null : null;
  return { path: rawPath, fragment };
}
function withMarkdownExtension(path) {
  return path.toLowerCase().endsWith(".md") ? path : `${path}.md`;
}
function normalizeWikiLinkPagePath(target) {
  const parsed = splitWikiLinkTarget(target);
  if (!parsed) return null;
  let path = withMarkdownExtension(parsed.path);
  if (!path.startsWith("wiki/") && !path.startsWith("raw/") && !ROOT_WIKI_LINK_PAGES.has(path)) {
    path = `wiki/${path}`;
  }
  return { path, fragment: parsed.fragment };
}
function buildWikiLinkHref(target, resolveHref) {
  const normalized = normalizeWikiLinkPagePath(target);
  if (!normalized) return null;
  const href = resolveHref(buildPageHref(normalized.path));
  return normalized.fragment ? `${href}#${encodeURIComponent(normalized.fragment)}` : href;
}
function routeSegmentsFromTreePath(treePath) {
  return treePath.split("/").filter(Boolean);
}
function treePathFromRouteSegments(segments) {
  if (segments.length === 0) return null;
  const [firstSegment, ...rest] = segments;
  if (firstSegment === "templates") {
    const templatePath = rest.join("/").trim();
    return templatePath || null;
  }
  const routePath = segments.join("/").trim();
  return routePath || null;
}
function firstSelectableTreePath(data) {
  const firstPage = data?.pages.find((p) => p.path !== "wiki/index.md" && p.path !== "index.md") ?? data?.pages[0] ?? null;
  if (firstPage) return firstPage.path;
  const firstSource = data?.sources[0] ?? null;
  if (firstSource) return firstSource.rawPath;
  return TEMPLATE_PATHS[0] ?? null;
}
function contentPathFromTreePath(treePath) {
  return treePath;
}
function isEditableWikiPagePath(path) {
  return path === "WIKI.md" || path === "AGENTS.md" || path === "IDEA.md" || path === "index.md" || path === "log.md" || path.startsWith("wiki/");
}
function WikiPage({ context }) {
  const { pathname, search } = useHostLocation();
  const isMobile = useIsMobileLayout();
  const section = useMemo(() => readSectionFromLocation(pathname, search), [pathname, search]);
  const settingsSection = useMemo(() => readSettingsSectionFromLocation(pathname), [pathname]);
  const activeSpaceSlug = useMemo(() => readActiveSpaceSlugFromLocation(pathname), [pathname]);
  const settingsSpaceSlug = useMemo(() => readSettingsSpaceSlugFromLocation(pathname), [pathname]);
  const overview = useOverview(context.companyId);
  const [isDragActive, setIsDragActive] = useState(false);
  const [stagedFiles, setStagedFiles] = useState([]);
  const [isIngestModalOpen, setIsIngestModalOpen] = useState(false);
  const resetDragState = useCallback(() => {
    setIsDragActive(false);
  }, []);
  const stageFiles = useCallback((files) => {
    if (files.length === 0) return;
    const now = Date.now();
    setStagedFiles((current) => [
      ...current,
      ...files.map((file, index) => ({
        id: `${now}-${index}-${file.name}-${file.size}-${file.lastModified}`,
        file
      }))
    ]);
    setIsIngestModalOpen(true);
  }, []);
  const handleDragEnter = useCallback((event) => {
    if (!isFileDrag(event)) return;
    event.preventDefault();
    event.stopPropagation();
    setIsDragActive(true);
  }, []);
  const handleDragOver = useCallback((event) => {
    if (!isFileDrag(event)) return;
    event.preventDefault();
    event.stopPropagation();
    event.dataTransfer.dropEffect = "copy";
    setIsDragActive(true);
  }, []);
  const handleDragLeave = useCallback((event) => {
    if (!isFileDrag(event)) return;
    event.preventDefault();
    event.stopPropagation();
    const relatedTarget = event.relatedTarget;
    if (relatedTarget instanceof Node && event.currentTarget.contains(relatedTarget)) return;
    resetDragState();
  }, [resetDragState]);
  const handleDrop = useCallback((event) => {
    if (!isFileDrag(event)) return;
    event.preventDefault();
    event.stopPropagation();
    resetDragState();
    stageFiles(Array.from(event.dataTransfer.files ?? []));
  }, [resetDragState, stageFiles]);
  useEffect(() => {
    if (!isDragActive) return;
    const handleWindowDragLeave = (event) => {
      const leftViewport = event.clientX <= 0 || event.clientY <= 0 || event.clientX >= window.innerWidth || event.clientY >= window.innerHeight;
      if (leftViewport) resetDragState();
    };
    const handleVisibilityChange = () => {
      if (document.visibilityState !== "visible") resetDragState();
    };
    window.addEventListener("dragend", resetDragState);
    window.addEventListener("drop", resetDragState);
    window.addEventListener("blur", resetDragState);
    window.addEventListener("dragleave", handleWindowDragLeave);
    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => {
      window.removeEventListener("dragend", resetDragState);
      window.removeEventListener("drop", resetDragState);
      window.removeEventListener("blur", resetDragState);
      window.removeEventListener("dragleave", handleWindowDragLeave);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [isDragActive, resetDragState]);
  if (!context.companyId) {
    return /* @__PURE__ */ jsx("main", { style: { ...shellStyle, height: isMobile ? "auto" : "100%", minHeight: isMobile ? "auto" : 600 }, children: "Choose a company to open the LLM Wiki." });
  }
  return /* @__PURE__ */ jsxs(
    "main",
    {
      style: { ...shellStyle, position: "relative", height: isMobile ? "auto" : "100%", minHeight: isMobile ? "auto" : 600 },
      onDragEnter: handleDragEnter,
      onDragOver: handleDragOver,
      onDragLeave: handleDragLeave,
      onDrop: handleDrop,
      children: [
        /* @__PURE__ */ jsx("style", { children: `@keyframes pcWikiPulse { 0%,100% { opacity:1 } 50% { opacity:0.45 } }` }),
        /* @__PURE__ */ jsx("section", { style: { flex: 1, minHeight: isMobile ? "auto" : 0, overflow: isMobile ? "visible" : "hidden", display: "flex" }, children: overview.error ? /* @__PURE__ */ jsx("div", { style: { padding: 24, flex: 1 }, children: /* @__PURE__ */ jsxs(Callout, { tone: "danger", children: [
          "LLM Wiki bridge error: ",
          overview.error.message
        ] }) }) : !overview.data ? /* @__PURE__ */ jsx("div", { style: { padding: 24, flex: 1, color: tokens.muted, fontSize: 13 }, children: "Loading wiki\u2026" }) : !overview.data.folder.healthy ? /* @__PURE__ */ jsx(UnconfiguredFolder, { context, folder: overview.data.folder, refresh: overview.refresh }) : section === "browse" ? /* @__PURE__ */ jsx(BrowseTab, { context }) : section === "ingest" ? /* @__PURE__ */ jsx(IngestTab, { context, refreshOverview: overview.refresh }) : section === "query" ? /* @__PURE__ */ jsx(QueryTab, { context, overview: overview.data }) : section === "lint" ? /* @__PURE__ */ jsx(SettingsTab, { context, initialSection: "lint" }) : section === "history" ? /* @__PURE__ */ jsx(HistoryTab, { context, overview: overview.data }) : /* @__PURE__ */ jsx(SettingsTab, { context, initialSection: settingsSection }) }),
        isDragActive ? /* @__PURE__ */ jsx(WikiPageDropOverlay, { onClose: resetDragState }) : null,
        isIngestModalOpen ? /* @__PURE__ */ jsx(
          IngestFilesModal,
          {
            companyId: context.companyId,
            files: stagedFiles,
            initialSpaceSlug: activeSpaceSlug,
            onAddFiles: stageFiles,
            onRemoveFile: (id) => setStagedFiles((current) => current.filter((item) => item.id !== id)),
            onClose: () => {
              setIsIngestModalOpen(false);
              setStagedFiles([]);
            },
            onIngested: () => {
              overview.refresh();
              if (typeof window !== "undefined") {
                window.dispatchEvent(new CustomEvent("pc-wiki-ingest-queued"));
              }
            }
          }
        ) : null
      ]
    }
  );
}
function isFileDrag(event) {
  return Array.from(event.dataTransfer?.types ?? []).includes("Files");
}
function WikiPageDropOverlay({ onClose }) {
  return /* @__PURE__ */ jsxs(
    "div",
    {
      "data-testid": "llm-wiki-page-drop-overlay",
      style: {
        position: "fixed",
        inset: 0,
        zIndex: 1e3,
        pointerEvents: "auto",
        display: "grid",
        placeItems: "center",
        padding: 24,
        background: "color-mix(in oklab, var(--background, oklch(0.145 0 0)) 72%, transparent)",
        backdropFilter: "blur(3px)"
      },
      children: [
        /* @__PURE__ */ jsx(
          "button",
          {
            type: "button",
            "aria-label": "Close ingest drop overlay",
            title: "Close",
            onClick: (event) => {
              event.preventDefault();
              event.stopPropagation();
              onClose();
            },
            style: {
              position: "absolute",
              top: 16,
              right: 16,
              width: 34,
              height: 34,
              borderRadius: 8,
              border: `1px solid ${tokens.border}`,
              background: "color-mix(in oklab, var(--card, oklch(0.205 0 0)) 90%, transparent)",
              color: tokens.fg,
              display: "inline-grid",
              placeItems: "center",
              cursor: "pointer",
              boxShadow: "0 10px 30px rgba(0,0,0,0.28)"
            },
            children: /* @__PURE__ */ jsx(XIcon, { size: 16 })
          }
        ),
        /* @__PURE__ */ jsxs("div", { style: {
          width: "min(520px, 100%)",
          borderRadius: 8,
          border: `1.5px dashed ${tokens.pluginBorder}`,
          background: "color-mix(in oklab, var(--card, oklch(0.205 0 0)) 92%, transparent)",
          color: tokens.fg,
          padding: "28px 24px",
          textAlign: "center",
          boxShadow: "0 20px 60px rgba(0,0,0,0.35)"
        }, children: [
          /* @__PURE__ */ jsx("div", { style: { display: "inline-flex", alignItems: "center", justifyContent: "center", width: 44, height: 44, borderRadius: 8, background: tokens.pluginBg, color: tokens.pluginFg, marginBottom: 12 }, children: /* @__PURE__ */ jsx(DownloadCloudIcon, { size: 24 }) }),
          /* @__PURE__ */ jsx("div", { style: { fontSize: 18, fontWeight: 650, marginBottom: 6 }, children: "Drop to ingest into LLM Wiki" }),
          /* @__PURE__ */ jsx(Tiny, { children: "Files will be staged for review before the wiki maintainer queues ingest operations." })
        ] })
      ]
    }
  );
}
function IngestFilesModal({
  companyId,
  files,
  onAddFiles,
  onRemoveFile,
  onClose,
  onIngested,
  initialSpaceSlug
}) {
  const ingest = usePluginAction("ingest-source");
  const toast = usePluginToast();
  const spacesQuery = useSpaces(companyId);
  const spaces = useMemo(() => {
    const list = spacesQuery.data?.spaces ?? [];
    return [...list].sort(compareSpaces);
  }, [spacesQuery.data]);
  const inputRef = useRef(null);
  const [busy, setBusy] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const [targetSpaceSlug, setTargetSpaceSlug] = useState(initialSpaceSlug || DEFAULT_SPACE_SLUG);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const targetSpace = useMemo(() => spaces.find((s) => s.slug === targetSpaceSlug) ?? null, [spaces, targetSpaceSlug]);
  const requestClose = useCallback(() => {
    if (!busy) onClose();
  }, [busy, onClose]);
  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key !== "Escape" || busy) return;
      event.preventDefault();
      onClose();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [busy, onClose]);
  async function confirm() {
    if (busy || files.length === 0) return;
    setBusy(true);
    setErrorMsg(null);
    try {
      for (const item of files) {
        const contents = await item.file.text();
        const result = await ingest({
          companyId,
          spaceSlug: targetSpaceSlug,
          sourceType: "file",
          title: item.file.name,
          url: null,
          contents,
          metadata: {
            fileName: item.file.name,
            fileSize: item.file.size,
            fileType: item.file.type || null,
            lastModified: item.file.lastModified
          }
        });
        await uploadIssueAttachmentFile({
          companyId,
          issueId: readIngestOperationIssueId(result),
          file: item.file
        });
      }
      const count = files.length;
      const spaceLabel = targetSpace?.displayName ?? targetSpaceSlug;
      toast({ tone: "success", title: `Files queued for ingest into ${spaceLabel}`, body: `${count} ${count === 1 ? "file" : "files"} captured into raw sources and attached to ingest tasks.` });
      onIngested();
      onClose();
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      setErrorMsg(message);
      toast({ tone: "error", title: "File ingest failed", body: message });
    } finally {
      setBusy(false);
    }
  }
  return /* @__PURE__ */ jsxs(
    "div",
    {
      role: "dialog",
      "aria-modal": "true",
      "aria-labelledby": "llm-wiki-ingest-modal-title",
      "data-testid": "llm-wiki-ingest-modal",
      onMouseDown: (event) => {
        if (event.currentTarget === event.target) requestClose();
      },
      style: {
        position: "fixed",
        inset: 0,
        zIndex: 1010,
        display: "grid",
        placeItems: "center",
        padding: 18,
        background: "rgba(0,0,0,0.52)"
      },
      children: [
        /* @__PURE__ */ jsxs("div", { style: {
          width: "min(680px, 100%)",
          maxHeight: "min(720px, calc(100vh - 36px))",
          overflow: "auto",
          background: tokens.card,
          color: tokens.fg,
          border: `1px solid ${tokens.border}`,
          borderRadius: 8,
          boxShadow: "0 24px 80px rgba(0,0,0,0.45)"
        }, children: [
          /* @__PURE__ */ jsxs("div", { style: { padding: "16px 18px", borderBottom: `1px solid ${tokens.border}`, display: "flex", gap: 12, alignItems: "flex-start" }, children: [
            /* @__PURE__ */ jsx("div", { style: { width: 34, height: 34, borderRadius: 8, background: tokens.pluginBg, color: tokens.pluginFg, display: "grid", placeItems: "center", flexShrink: 0 }, children: /* @__PURE__ */ jsx(DownloadCloudIcon, { size: 18 }) }),
            /* @__PURE__ */ jsxs("div", { style: { flex: 1, minWidth: 0 }, children: [
              /* @__PURE__ */ jsxs("h2", { id: "llm-wiki-ingest-modal-title", style: { margin: 0, fontSize: 16, fontWeight: 650 }, children: [
                "Ingest files into ",
                targetSpace?.displayName ?? targetSpaceSlug
              ] }),
              /* @__PURE__ */ jsx(Tiny, { style: { marginTop: 4 }, children: "Review the staged files, switch the destination space if needed, then queue them as LLM Wiki ingest operations. This is manual file ingest - Paperclip-derived distillation always routes to the default space regardless of the destination picked here." })
            ] }),
            /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: requestClose, disabled: busy, title: "Close ingest modal", children: "Close" })
          ] }),
          /* @__PURE__ */ jsxs("div", { style: { padding: 18, display: "grid", gap: 14 }, children: [
            /* @__PURE__ */ jsx(
              SpacePicker,
              {
                spaces,
                activeSpaceSlug: targetSpaceSlug,
                loading: spacesQuery.loading,
                error: spacesQuery.error?.message ?? null,
                isOpen: pickerOpen,
                onToggle: () => setPickerOpen((v) => !v),
                onClose: () => setPickerOpen(false),
                onSelect: (slug) => {
                  setPickerOpen(false);
                  setTargetSpaceSlug(slug);
                },
                onCreate: () => {
                  setPickerOpen(false);
                  setCreateOpen(true);
                }
              }
            ),
            /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }, children: [
              /* @__PURE__ */ jsxs(Badge, { tone: "running", children: [
                files.length,
                " staged"
              ] }),
              /* @__PURE__ */ jsx(Button, { size: "sm", onClick: () => inputRef.current?.click(), disabled: busy, children: "Add files" }),
              /* @__PURE__ */ jsx(
                "input",
                {
                  ref: inputRef,
                  type: "file",
                  multiple: true,
                  style: { display: "none" },
                  onChange: (event) => {
                    onAddFiles(Array.from(event.currentTarget.files ?? []));
                    event.currentTarget.value = "";
                  }
                }
              )
            ] }),
            /* @__PURE__ */ jsx("div", { style: { border: `1px solid ${tokens.border}`, borderRadius: 8, overflow: "hidden" }, children: files.length === 0 ? /* @__PURE__ */ jsx("div", { style: { padding: 16 }, children: /* @__PURE__ */ jsx(Tiny, { children: "No files staged." }) }) : files.map((item) => /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 10, alignItems: "center", padding: "10px 12px", borderBottom: `1px solid ${tokens.border}`, minWidth: 0 }, children: [
              /* @__PURE__ */ jsxs("div", { style: { flex: "1 1 auto", minWidth: 0 }, children: [
                /* @__PURE__ */ jsx("strong", { style: { display: "block", fontSize: 13, overflowWrap: "anywhere" }, children: item.file.name }),
                /* @__PURE__ */ jsxs(Tiny, { children: [
                  formatFileSize(item.file.size),
                  item.file.type ? ` \xB7 ${item.file.type}` : ""
                ] })
              ] }),
              /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: () => onRemoveFile(item.id), disabled: busy, children: "Remove" })
            ] }, item.id)) }),
            errorMsg ? /* @__PURE__ */ jsx(Callout, { tone: "danger", children: errorMsg }) : null,
            /* @__PURE__ */ jsxs(Callout, { children: [
              "Confirming captures each file into ",
              /* @__PURE__ */ jsxs(Mono, { children: [
                targetSpaceSlug,
                "/raw/"
              ] }),
              ", attaches the original file to the ingest task, and initiates a task for the Wiki Maintainer to process."
            ] }),
            /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 8, justifyContent: "flex-end", flexWrap: "wrap" }, children: [
              /* @__PURE__ */ jsx(Button, { variant: "ghost", onClick: requestClose, disabled: busy, children: "Cancel" }),
              /* @__PURE__ */ jsxs(Button, { variant: "primary", onClick: confirm, disabled: files.length === 0, loading: busy, children: [
                "Capture & ingest into ",
                targetSpace?.displayName ?? targetSpaceSlug
              ] })
            ] })
          ] })
        ] }),
        createOpen ? /* @__PURE__ */ jsx(
          CreateSpaceModal,
          {
            companyId,
            existingSlugs: new Set(spaces.map((s) => s.slug)),
            onClose: () => setCreateOpen(false),
            onCreated: (space) => {
              setCreateOpen(false);
              spacesQuery.refresh();
              setTargetSpaceSlug(space.slug);
            }
          }
        ) : null
      ]
    }
  );
}
function formatFileSize(bytes) {
  if (!Number.isFinite(bytes) || bytes < 0) return "Unknown size";
  if (bytes < 1024) return `${bytes} B`;
  const kib = bytes / 1024;
  if (kib < 1024) return `${kib.toFixed(kib >= 10 ? 0 : 1)} KB`;
  const mib = kib / 1024;
  return `${mib.toFixed(mib >= 10 ? 0 : 1)} MB`;
}
function slugify(input) {
  return input.toLowerCase().normalize("NFKD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "").slice(0, 40);
}
var SLUG_PATTERN = /^[a-z0-9](?:[a-z0-9-]{0,38}[a-z0-9])?$/;
function CreateSpaceModal({
  companyId,
  existingSlugs,
  onClose,
  onCreated
}) {
  const create = usePluginAction("create-space");
  const toast = usePluginToast();
  const [displayName, setDisplayName] = useState("");
  const [slug, setSlug] = useState("");
  const [slugDirty, setSlugDirty] = useState(false);
  const [folderMode, setFolderMode] = useState("managed_subfolder");
  const [accessScope, setAccessScope] = useState("shared");
  const [busy, setBusy] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const isMobile = useIsMobileLayout();
  useEffect(() => {
    const handler = (event) => {
      if (event.key === "Escape" && !busy) onClose();
    };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [busy, onClose]);
  const effectiveSlug = slug.trim() || (slugDirty ? "" : slugify(displayName));
  const slugError = (() => {
    if (!effectiveSlug) return null;
    if (effectiveSlug === DEFAULT_SPACE_SLUG) return "Slug 'default' is reserved.";
    if (!SLUG_PATTERN.test(effectiveSlug)) return "Use 2-40 chars: lowercase letters, numbers, hyphens.";
    if (existingSlugs.has(effectiveSlug)) return "A space with this slug already exists.";
    return null;
  })();
  const canSubmit = displayName.trim().length > 0 && effectiveSlug.length > 0 && !slugError && !busy;
  async function submit() {
    if (!canSubmit) return;
    setBusy(true);
    setErrorMsg(null);
    try {
      const result = await create({
        companyId,
        slug: effectiveSlug,
        displayName: displayName.trim(),
        folderMode,
        accessScope
      });
      toast({ tone: "success", title: "Space created", body: `${result.space.displayName} is ready for ingest.` });
      onCreated(result.space);
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      setErrorMsg(msg);
      toast({ tone: "error", title: "Could not create space", body: msg });
    } finally {
      setBusy(false);
    }
  }
  const previewName = displayName.trim() || "your-space";
  return /* @__PURE__ */ jsx(
    "div",
    {
      role: "dialog",
      "aria-modal": "true",
      "aria-labelledby": "create-space-modal-title",
      style: {
        position: "fixed",
        inset: 0,
        zIndex: 200,
        display: "grid",
        placeItems: isMobile ? "end" : "center",
        padding: isMobile ? 0 : 18,
        background: "rgba(0,0,0,0.52)"
      },
      onClick: (event) => {
        if (event.target === event.currentTarget && !busy) onClose();
      },
      children: /* @__PURE__ */ jsxs(
        "div",
        {
          style: {
            width: isMobile ? "100%" : "min(560px, 100%)",
            maxHeight: isMobile ? "92vh" : "min(760px, calc(100vh - 36px))",
            overflow: "auto",
            background: tokens.card,
            color: tokens.fg,
            border: `1px solid ${tokens.border}`,
            borderRadius: isMobile ? "12px 12px 0 0" : 8,
            boxShadow: "0 24px 80px rgba(0,0,0,0.45)",
            fontFamily: fontStack
          },
          children: [
            /* @__PURE__ */ jsxs("div", { style: { padding: "16px 20px", borderBottom: `1px solid ${tokens.border}`, display: "flex", alignItems: "flex-start", gap: 12 }, children: [
              /* @__PURE__ */ jsx("div", { style: { width: 34, height: 34, borderRadius: 8, background: tokens.pluginBg, color: tokens.pluginFg, display: "grid", placeItems: "center", flexShrink: 0 }, children: /* @__PURE__ */ jsx(FolderIcon, { size: 18 }) }),
              /* @__PURE__ */ jsxs("div", { style: { flex: 1, minWidth: 0 }, children: [
                /* @__PURE__ */ jsx("h2", { id: "create-space-modal-title", style: { margin: 0, fontSize: 17, fontWeight: 650 }, children: "Create a shared space" }),
                /* @__PURE__ */ jsx(Tiny, { style: { marginTop: 4 }, children: "Spaces partition wiki pages, sources, and manual ingest into separate slug-prefixed folders under the wiki root. Paperclip distillation and event capture always write into the default space and skip new spaces created here - per-space Paperclip routing is a later phase." })
              ] }),
              /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: onClose, disabled: busy, title: "Close", children: "Close" })
            ] }),
            /* @__PURE__ */ jsxs("div", { style: { padding: 20, display: "grid", gap: 16 }, children: [
              /* @__PURE__ */ jsx(FormField, { label: "Display name", children: /* @__PURE__ */ jsx(
                TextInput,
                {
                  value: displayName,
                  autoFocus: true,
                  onChange: (event) => {
                    setDisplayName(event.target.value);
                    if (!slugDirty) setSlug("");
                  },
                  placeholder: "Team research",
                  maxLength: 120
                }
              ) }),
              /* @__PURE__ */ jsx(
                FormField,
                {
                  label: "Slug",
                  help: slugError ?? `Stored as the URL segment and the on-disk folder. Defaults to ${slugify(displayName) || "auto-derived from display name"}.`,
                  tone: slugError ? "danger" : "muted",
                  children: /* @__PURE__ */ jsx(
                    TextInput,
                    {
                      value: slug || (slugDirty ? "" : slugify(displayName)),
                      onChange: (event) => {
                        setSlugDirty(true);
                        setSlug(event.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ""));
                      },
                      placeholder: "team-research",
                      style: { fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace" }
                    }
                  )
                }
              ),
              /* @__PURE__ */ jsx(FormField, { label: "Type", children: /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 6, flexWrap: "wrap" }, children: [
                /* @__PURE__ */ jsx(SegmentedOption, { selected: true, label: "Folder", onClick: () => void 0 }),
                /* @__PURE__ */ jsx(SegmentedOption, { disabled: true, label: "Cloud", suffix: "Coming soon", onClick: () => void 0 })
              ] }) }),
              /* @__PURE__ */ jsx(FormField, { label: "Folder source", help: "New managed folders create a slug-scoped subfolder under your wiki root. Existing folders must already live under the same wiki root.", children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
                /* @__PURE__ */ jsx(
                  FolderModeRow,
                  {
                    checked: folderMode === "managed_subfolder",
                    onSelect: () => setFolderMode("managed_subfolder"),
                    label: "New managed folder",
                    help: `Creates spaces/${previewName.replace(/\s+/g, "-").toLowerCase()}/ under the configured wiki root with the standard skeleton.`
                  }
                ),
                /* @__PURE__ */ jsx(
                  FolderModeRow,
                  {
                    checked: folderMode === "existing_local_folder",
                    onSelect: () => setFolderMode("existing_local_folder"),
                    label: "Existing folder under wiki root",
                    help: "Re-use a sub-folder you've already created inside the wiki root. The folder path is stored in the space settings."
                  }
                ),
                /* @__PURE__ */ jsx(
                  FolderModeRow,
                  {
                    disabled: true,
                    checked: false,
                    onSelect: () => void 0,
                    label: "Existing absolute path",
                    help: "Pending host capability \u2014 security review (PAP-3640) gates this until host-managed dynamic local-folder bindings ship.",
                    suffix: "Disabled"
                  }
                )
              ] }) }),
              /* @__PURE__ */ jsx(FormField, { label: "Access scope", help: "Access scope is metadata only. It does not currently enforce who can read or write the space, and it does not change which Paperclip sources reach the space.", children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gridTemplateColumns: isMobile ? "1fr" : "1fr 1fr 1fr", gap: 8 }, children: [
                /* @__PURE__ */ jsx(
                  ScopeTile,
                  {
                    selected: accessScope === "shared",
                    onSelect: () => setAccessScope("shared"),
                    label: "Shared",
                    help: "Visible to everyone in this company."
                  }
                ),
                /* @__PURE__ */ jsx(
                  ScopeTile,
                  {
                    selected: accessScope === "personal",
                    onSelect: () => setAccessScope("personal"),
                    label: "Personal",
                    help: "Future scope \u2014 stored only.",
                    tag: "Future"
                  }
                ),
                /* @__PURE__ */ jsx(
                  ScopeTile,
                  {
                    selected: accessScope === "team",
                    onSelect: () => setAccessScope("team"),
                    label: "Team",
                    help: "Future scope \u2014 stored only.",
                    tag: "Future"
                  }
                )
              ] }) }),
              errorMsg ? /* @__PURE__ */ jsx(Callout, { tone: "danger", children: errorMsg }) : null
            ] }),
            /* @__PURE__ */ jsxs("div", { style: {
              padding: "12px 20px",
              borderTop: `1px solid ${tokens.border}`,
              display: "flex",
              gap: 8,
              justifyContent: isMobile ? "stretch" : "flex-end",
              flexWrap: "wrap",
              position: isMobile ? "sticky" : "static",
              bottom: 0,
              background: tokens.card
            }, children: [
              /* @__PURE__ */ jsx(Button, { variant: "ghost", onClick: onClose, disabled: busy, style: { flex: isMobile ? 1 : void 0 }, children: "Cancel" }),
              /* @__PURE__ */ jsx(Button, { variant: "primary", onClick: submit, disabled: !canSubmit, loading: busy, style: { flex: isMobile ? 1 : void 0 }, children: "Create space" })
            ] })
          ]
        }
      )
    }
  );
}
function FormField({ label, help, tone = "muted", children }) {
  return /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 6 }, children: [
    /* @__PURE__ */ jsx("label", { style: { fontSize: 12, fontWeight: 600, color: tokens.fg }, children: label }),
    children,
    help ? /* @__PURE__ */ jsx("span", { style: { fontSize: 11, color: tone === "danger" ? "oklch(0.78 0.18 25)" : tokens.muted, lineHeight: 1.4 }, children: help }) : null
  ] });
}
function SegmentedOption({ label, selected, disabled, suffix, onClick }) {
  return /* @__PURE__ */ jsxs(
    "button",
    {
      type: "button",
      onClick: disabled ? void 0 : onClick,
      disabled,
      "aria-pressed": selected,
      "aria-disabled": disabled,
      style: {
        padding: "8px 14px",
        borderRadius: 6,
        border: `1px solid ${selected ? tokens.border : tokens.border}`,
        background: selected ? tokens.accent : "transparent",
        color: disabled ? tokens.muted : tokens.fg,
        fontSize: 13,
        fontWeight: 600,
        cursor: disabled ? "not-allowed" : "pointer",
        opacity: disabled ? 0.6 : 1,
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        fontFamily: fontStack
      },
      children: [
        /* @__PURE__ */ jsx("span", { children: label }),
        suffix ? /* @__PURE__ */ jsx("span", { style: {
          fontSize: 10,
          fontWeight: 500,
          padding: "1px 6px",
          borderRadius: 3,
          border: `1px dashed ${tokens.border}`,
          color: tokens.muted
        }, children: suffix }) : null
      ]
    }
  );
}
function FolderModeRow({ checked, onSelect, label, help, disabled, suffix }) {
  return /* @__PURE__ */ jsxs(
    "button",
    {
      type: "button",
      onClick: disabled ? void 0 : onSelect,
      disabled,
      style: {
        display: "flex",
        alignItems: "flex-start",
        gap: 10,
        padding: "10px 12px",
        borderRadius: 8,
        border: `1px solid ${checked ? tokens.fg : tokens.border}`,
        background: "transparent",
        color: tokens.fg,
        textAlign: "left",
        cursor: disabled ? "not-allowed" : "pointer",
        opacity: disabled ? 0.55 : 1,
        fontFamily: fontStack
      },
      children: [
        /* @__PURE__ */ jsx(
          "span",
          {
            "aria-hidden": "true",
            style: {
              width: 14,
              height: 14,
              borderRadius: 7,
              border: `2px solid ${checked ? tokens.fg : tokens.muted}`,
              flexShrink: 0,
              marginTop: 2,
              background: checked ? tokens.fg : "transparent"
            }
          }
        ),
        /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 2, flex: 1, minWidth: 0 }, children: [
          /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "center", gap: 6 }, children: [
            /* @__PURE__ */ jsx("span", { style: { fontSize: 13, fontWeight: 600 }, children: label }),
            suffix ? /* @__PURE__ */ jsx(Badge, { tone: "default", style: { fontSize: 10 }, children: suffix }) : null
          ] }),
          /* @__PURE__ */ jsx("span", { style: { fontSize: 11, color: tokens.muted, lineHeight: 1.4 }, children: help })
        ] })
      ]
    }
  );
}
function ScopeTile({ selected, onSelect, label, help, tag }) {
  return /* @__PURE__ */ jsxs(
    "button",
    {
      type: "button",
      onClick: onSelect,
      style: {
        padding: "10px 12px",
        borderRadius: 8,
        border: `1px solid ${selected ? tokens.fg : tokens.border}`,
        background: selected ? tokens.accent : "transparent",
        color: tokens.fg,
        textAlign: "left",
        display: "grid",
        gap: 4,
        cursor: "pointer",
        fontFamily: fontStack
      },
      children: [
        /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "center", gap: 6 }, children: [
          /* @__PURE__ */ jsx("span", { style: { fontSize: 13, fontWeight: 600 }, children: label }),
          tag ? /* @__PURE__ */ jsx(Badge, { tone: "default", style: { fontSize: 10 }, children: tag }) : null
        ] }),
        /* @__PURE__ */ jsx("span", { style: { fontSize: 11, color: tokens.muted, lineHeight: 1.4 }, children: help })
      ]
    }
  );
}
function compareSpaces(a, b) {
  if (a.slug === DEFAULT_SPACE_SLUG && b.slug !== DEFAULT_SPACE_SLUG) return -1;
  if (b.slug === DEFAULT_SPACE_SLUG && a.slug !== DEFAULT_SPACE_SLUG) return 1;
  return a.displayName.localeCompare(b.displayName, void 0, { sensitivity: "base" });
}
function activeWikiSpaces(spaces) {
  return spaces.filter((space) => space.status !== "archived");
}
function spaceTreeKey(spaceSlug, path) {
  return `${spaceSlug}::${path}`;
}
function SpacePageContentWarmup({ companyId, path, spaceSlug }) {
  usePageContent(companyId, path, spaceSlug);
  return null;
}
function SpacePagesWarmup({ companyId, spaceSlug }) {
  const pages = usePages(companyId, { includeRaw: true, spaceSlug });
  const selectedTreePath = firstSelectableTreePath(pages.data);
  const selected = contentPathFromTreePath(selectedTreePath);
  return selected ? /* @__PURE__ */ jsx(SpacePageContentWarmup, { companyId, path: selected, spaceSlug }) : null;
}
function WikiRouteSidebar({ context }) {
  const hostNavigation = useHostNavigation();
  const { pathname, search, state } = useHostLocation();
  const activeSection = useMemo(() => readSectionFromLocation(pathname, search), [pathname, search]);
  const activeSpaceSlug = useMemo(() => readActiveSpaceSlugFromLocation(pathname), [pathname]);
  const companyName = context.companyPrefix ?? "Company";
  const spacesQuery = useSpaces(context.companyId);
  const spaces = useMemo(() => {
    const list = spacesQuery.data?.spaces ?? [];
    if (list.length === 0) return list;
    return activeWikiSpaces(list).sort(compareSpaces);
  }, [spacesQuery.data]);
  const pages = usePages(context.companyId, { includeRaw: true, spaceSlug: activeSpaceSlug });
  const activeSpaceNodes = useMemo(
    () => buildBrowseTree(pages.data?.pages ?? [], pages.data?.sources ?? []),
    [pages.data]
  );
  const warmupSpaces = useMemo(() => {
    const active = spaces.find((space) => space.slug === activeSpaceSlug);
    const rest = spaces.filter((space) => space.slug !== activeSpaceSlug);
    return (active ? [active, ...rest] : rest).slice(0, WIKI_SPACE_PREFETCH_LIMIT);
  }, [spaces, activeSpaceSlug]);
  const storageKey = useMemo(() => routeSidebarExpandedStorageKey(context.companyId), [context.companyId]);
  const [expandedRaw, setExpandedRaw] = useState(() => readRouteSidebarExpandedPaths(storageKey));
  const [selectedTreePath, setSelectedTreePath] = useState(null);
  const [spaceCollapse, setSpaceCollapse] = useState(/* @__PURE__ */ new Set());
  const [createOpen, setCreateOpen] = useState(false);
  const [openMenuFor, setOpenMenuFor] = useState(null);
  useEffect(() => {
    if (activeSection !== "browse") return;
    const sidebarSelectedPath = readSidebarSelectedPathFromNavigationState(state);
    if (sidebarSelectedPath === null) return;
    setSelectedTreePath(sidebarSelectedPath);
  }, [activeSection, state]);
  useEffect(() => {
    setExpandedRaw(readRouteSidebarExpandedPaths(storageKey));
  }, [storageKey]);
  useEffect(() => {
    writeRouteSidebarExpandedPaths(storageKey, expandedRaw);
  }, [expandedRaw, storageKey]);
  useEffect(() => {
    const ancestors = expandedAncestors(selectedTreePath);
    if (ancestors.length === 0) return;
    const slug = activeSpaceSlug;
    setExpandedRaw((current) => {
      const next = new Set(current);
      let changed = false;
      for (const ancestor of ancestors) {
        const key = spaceTreeKey(slug, ancestor);
        if (next.has(key)) continue;
        next.add(key);
        changed = true;
      }
      return changed ? next : current;
    });
  }, [selectedTreePath, activeSpaceSlug]);
  const expandedForActiveSpace = useMemo(() => {
    const next = /* @__PURE__ */ new Set();
    const prefix = `${activeSpaceSlug}::`;
    for (const entry of expandedRaw) {
      if (entry.startsWith(prefix)) {
        next.add(entry.slice(prefix.length));
      } else if (!entry.includes("::") && activeSpaceSlug === DEFAULT_SPACE_SLUG) {
        next.add(entry);
      }
    }
    return next;
  }, [expandedRaw, activeSpaceSlug]);
  const handleToggleDir = (dirPath) => {
    const key = spaceTreeKey(activeSpaceSlug, dirPath);
    setExpandedRaw((current) => {
      const next = new Set(current);
      if (next.has(key)) {
        next.delete(key);
        if (activeSpaceSlug === DEFAULT_SPACE_SLUG) next.delete(dirPath);
      } else if (activeSpaceSlug === DEFAULT_SPACE_SLUG && next.has(dirPath)) {
        next.delete(dirPath);
      } else {
        next.add(key);
      }
      return next;
    });
  };
  const toggleSpaceCollapse = (slug) => {
    setSpaceCollapse((current) => {
      const next = new Set(current);
      if (next.has(slug)) next.delete(slug);
      else next.add(slug);
      return next;
    });
  };
  const renderToolLink = ({ key, label, Icon }) => {
    const isLegacyLintSettingsActive = key === "settings" && activeSection === "lint";
    const isActive = key === activeSection || isLegacyLintSettingsActive;
    return /* @__PURE__ */ jsxs(
      "a",
      {
        ...hostNavigation.linkProps(buildSectionHref(key, activeSpaceSlug)),
        "aria-current": isActive ? "page" : void 0,
        className: [
          "flex items-center gap-2.5 rounded-md px-2 py-1.5 text-[13px] font-medium transition-colors",
          isActive && !isLegacyLintSettingsActive ? "bg-accent text-foreground" : isActive ? "text-foreground" : "text-foreground/80 hover:bg-accent/50 hover:text-foreground"
        ].join(" "),
        style: { textDecoration: "none" },
        children: [
          /* @__PURE__ */ jsx("span", { "aria-hidden": "true", className: "shrink-0", children: /* @__PURE__ */ jsx(Icon, {}) }),
          /* @__PURE__ */ jsx("span", { className: "flex-1 truncate", children: label })
        ]
      },
      key
    );
  };
  return /* @__PURE__ */ jsxs("aside", { className: "w-full h-full min-h-0 border-r border-border bg-background flex flex-col", children: [
    warmupSpaces.map((space) => /* @__PURE__ */ jsx(SpacePagesWarmup, { companyId: context.companyId, spaceSlug: space.slug }, space.slug)),
    /* @__PURE__ */ jsx("div", { className: "flex flex-col gap-1 px-3 py-3 shrink-0", children: /* @__PURE__ */ jsxs(
      "a",
      {
        ...hostNavigation.linkProps("/dashboard"),
        className: "flex items-center gap-1.5 rounded-md px-2 py-1 text-xs text-muted-foreground transition-colors hover:bg-accent/50 hover:text-foreground",
        style: { textDecoration: "none" },
        children: [
          /* @__PURE__ */ jsx("span", { "aria-hidden": "true", className: "shrink-0", children: /* @__PURE__ */ jsx(ChevronLeftIcon, { size: 14 }) }),
          /* @__PURE__ */ jsx("span", { className: "truncate", children: companyName })
        ]
      }
    ) }),
    /* @__PURE__ */ jsxs("div", { className: "flex-1 min-h-0 overflow-y-auto border-t border-border px-3 py-3", children: [
      /* @__PURE__ */ jsx("nav", { "aria-label": "Wiki primary", className: "mb-3", children: /* @__PURE__ */ jsx("div", { className: "flex flex-col gap-0.5", children: TOP_TOOL_SECTIONS.map(renderToolLink) }) }),
      /* @__PURE__ */ jsxs("div", { className: "mb-1 flex items-center gap-1 px-2 text-[11px] font-semibold uppercase tracking-normal text-muted-foreground", style: { height: 24 }, children: [
        /* @__PURE__ */ jsx(
          "span",
          {
            className: "flex-1 truncate",
            title: "Destination spaces. Browsing and manual ingest happen in the active space; Paperclip distillation always writes into the default space in Phase 1.",
            children: "Shared Spaces"
          }
        ),
        /* @__PURE__ */ jsx(
          "button",
          {
            type: "button",
            "aria-label": "Create space",
            title: "Create space",
            onClick: () => setCreateOpen(true),
            style: {
              width: 22,
              height: 22,
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              border: "none",
              background: "transparent",
              color: tokens.muted,
              borderRadius: 4,
              cursor: "pointer"
            },
            children: /* @__PURE__ */ jsx(PlusIcon, { size: 14 })
          }
        )
      ] }),
      spacesQuery.error ? /* @__PURE__ */ jsxs("div", { style: { padding: "6px 8px", fontSize: 11, color: tokens.statusBlocked }, children: [
        "Failed to load spaces: ",
        spacesQuery.error.message
      ] }) : null,
      spaces.length === 0 && spacesQuery.loading ? /* @__PURE__ */ jsx(Tiny, { style: { padding: "6px 8px" }, children: "Loading spaces\u2026" }) : null,
      /* @__PURE__ */ jsx("div", { style: { display: "flex", flexDirection: "column" }, children: spaces.map((space) => {
        const isActiveSpace = space.slug === activeSpaceSlug;
        const collapsed = spaceCollapse.has(space.slug);
        const showTree = isActiveSpace && !collapsed;
        return /* @__PURE__ */ jsxs("div", { style: { position: "relative" }, children: [
          /* @__PURE__ */ jsx(
            SpaceRow,
            {
              space,
              active: isActiveSpace,
              expanded: showTree,
              hostNavigation,
              onToggleCollapse: () => {
                if (!isActiveSpace) {
                  setSpaceCollapse((current) => {
                    if (!current.has(space.slug)) return current;
                    const next = new Set(current);
                    next.delete(space.slug);
                    return next;
                  });
                  hostNavigation.navigate(buildSectionHref("browse", space.slug));
                } else {
                  toggleSpaceCollapse(space.slug);
                }
              },
              onMenuToggle: () => setOpenMenuFor((curr) => curr === space.slug ? null : space.slug),
              menuOpen: openMenuFor === space.slug,
              onMenuClose: () => setOpenMenuFor(null),
              onArchived: (slug) => {
                spacesQuery.refresh();
                setOpenMenuFor(null);
                setSpaceCollapse((current) => {
                  if (!current.has(slug)) return current;
                  const next = new Set(current);
                  next.delete(slug);
                  return next;
                });
                if (activeSpaceSlug === slug) {
                  hostNavigation.navigate(buildSectionHref("browse", DEFAULT_SPACE_SLUG));
                }
              },
              activeSpaceSlug,
              companyId: context.companyId
            }
          ),
          showTree ? /* @__PURE__ */ jsx("div", { style: { paddingLeft: 18, marginTop: 2, marginBottom: 6 }, children: /* @__PURE__ */ jsx(
            FileTree,
            {
              nodes: activeSpaceNodes,
              selectedFile: selectedTreePath,
              expandedPaths: expandedForActiveSpace,
              onSelectFile: (path) => {
                setSelectedTreePath(path);
                hostNavigation.navigate(buildPageHref(path, space.slug), { state: wikiSidebarNavigationState(path) });
              },
              onToggleDir: handleToggleDir,
              wrapLabels: false,
              loading: pages.loading,
              error: pages.error ? { message: pages.error.message } : null,
              empty: { title: "No pages yet", description: "Add content to populate this space." },
              ariaLabel: `Wiki pages in ${space.displayName}`
            }
          ) }) : null
        ] }, space.slug);
      }) })
    ] }),
    /* @__PURE__ */ jsx(
      "nav",
      {
        "aria-label": "Wiki secondary",
        className: "shrink-0 border-t border-border px-3 py-3",
        children: /* @__PURE__ */ jsx("div", { className: "flex flex-col gap-0.5", children: BOTTOM_TOOL_SECTIONS.map(renderToolLink) })
      }
    ),
    createOpen && context.companyId ? /* @__PURE__ */ jsx(
      CreateSpaceModal,
      {
        companyId: context.companyId,
        existingSlugs: new Set(spaces.map((s) => s.slug)),
        onClose: () => setCreateOpen(false),
        onCreated: (space) => {
          setCreateOpen(false);
          spacesQuery.refresh();
          hostNavigation.navigate(buildSectionHref("browse", space.slug));
        }
      }
    ) : null
  ] });
}
function SpaceRow({
  space,
  active,
  expanded,
  hostNavigation,
  onToggleCollapse,
  onMenuToggle,
  menuOpen,
  onMenuClose,
  onArchived,
  activeSpaceSlug,
  companyId
}) {
  const [hover, setHover] = useState(false);
  const isDefault = space.slug === DEFAULT_SPACE_SLUG;
  return /* @__PURE__ */ jsxs(
    "div",
    {
      role: "button",
      tabIndex: 0,
      "aria-expanded": expanded,
      "aria-label": `${expanded ? "Collapse" : active ? "Expand" : "Open"} ${space.displayName} space`,
      onMouseEnter: () => setHover(true),
      onMouseLeave: () => setHover(false),
      style: {
        display: "flex",
        alignItems: "center",
        gap: 6,
        padding: "6px 8px",
        borderRadius: 6,
        cursor: "pointer",
        background: active ? tokens.accent : "transparent",
        position: "relative"
      },
      onClick: onToggleCollapse,
      onKeyDown: (event) => {
        if (event.target !== event.currentTarget) return;
        if (event.key !== "Enter" && event.key !== " ") return;
        event.preventDefault();
        onToggleCollapse();
      },
      children: [
        /* @__PURE__ */ jsx("span", { "aria-hidden": "true", style: { color: tokens.muted, display: "flex", flexShrink: 0, transform: expanded ? "rotate(0)" : "rotate(0)" }, children: expanded ? /* @__PURE__ */ jsx(ChevronDownIcon, { size: 14 }) : /* @__PURE__ */ jsx(ChevronRightIcon, { size: 14 }) }),
        /* @__PURE__ */ jsx("span", { "aria-hidden": "true", style: { color: tokens.muted, display: "flex", flexShrink: 0 }, children: /* @__PURE__ */ jsx(FolderIcon, { size: 16 }) }),
        /* @__PURE__ */ jsx("span", { style: { flex: 1, fontSize: 13, fontWeight: 600, color: tokens.fg, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }, children: space.displayName }),
        space.accessScope === "personal" ? /* @__PURE__ */ jsx(Badge, { tone: "default", style: { height: 18, padding: "0 6px", fontSize: 10 }, children: "personal" }) : space.accessScope === "team" ? /* @__PURE__ */ jsx(Badge, { tone: "default", style: { height: 18, padding: "0 6px", fontSize: 10 }, children: "team" }) : null,
        /* @__PURE__ */ jsx(
          "button",
          {
            type: "button",
            "aria-label": `${space.displayName} space menu`,
            title: "Space menu",
            onClick: (event) => {
              event.stopPropagation();
              onMenuToggle();
            },
            style: {
              opacity: hover || menuOpen ? 1 : 0,
              transition: "opacity 80ms ease",
              width: 22,
              height: 22,
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              border: "none",
              background: "transparent",
              color: tokens.fg,
              borderRadius: 4,
              cursor: "pointer"
            },
            children: /* @__PURE__ */ jsx(MoreHorizontalIcon, { size: 14 })
          }
        ),
        menuOpen ? /* @__PURE__ */ jsx(
          SpaceRowMenu,
          {
            space,
            isDefault,
            hostNavigation,
            activeSpaceSlug,
            companyId,
            onClose: onMenuClose,
            onArchived
          }
        ) : null
      ]
    }
  );
}
function SpaceRowMenu({
  space,
  isDefault,
  hostNavigation,
  activeSpaceSlug,
  companyId,
  onClose,
  onArchived
}) {
  const ref = useRef(null);
  const archive = usePluginAction("archive-space");
  const bootstrap = usePluginAction("bootstrap-space");
  const toast = usePluginToast();
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    const handler = (event) => {
      if (!ref.current) return;
      if (event.target instanceof Node && ref.current.contains(event.target)) return;
      onClose();
    };
    const keyHandler = (event) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("mousedown", handler);
    document.addEventListener("keydown", keyHandler);
    return () => {
      document.removeEventListener("mousedown", handler);
      document.removeEventListener("keydown", keyHandler);
    };
  }, [onClose]);
  const handleArchive = async () => {
    if (!companyId || isDefault || busy) return;
    if (typeof window !== "undefined" && !window.confirm(`Archive ${space.displayName}? Pages remain on disk; you can restore later through the plugin API or by un-archiving from the database.`)) {
      return;
    }
    setBusy(true);
    try {
      await archive({ companyId, spaceSlug: space.slug });
      toast({ tone: "success", title: "Space archived", body: `${space.displayName} hidden from sidebar.` });
      onArchived(space.slug);
    } catch (err) {
      toast({ tone: "error", title: "Archive failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setBusy(false);
    }
  };
  const handleRefresh = async () => {
    if (!companyId || busy) return;
    setBusy(true);
    try {
      await bootstrap({ companyId, spaceSlug: space.slug });
      toast({ tone: "success", title: "Space refreshed", body: `${space.displayName} index re-bootstrapped.` });
      onClose();
    } catch (err) {
      toast({ tone: "error", title: "Refresh failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setBusy(false);
    }
  };
  return /* @__PURE__ */ jsxs(
    "div",
    {
      ref,
      role: "menu",
      onClick: (event) => event.stopPropagation(),
      style: {
        position: "absolute",
        top: "calc(100% + 4px)",
        right: 0,
        zIndex: 30,
        minWidth: 220,
        background: tokens.card,
        border: `1px solid ${tokens.border}`,
        borderRadius: 8,
        boxShadow: "0 12px 36px rgba(0,0,0,0.45)",
        padding: 4
      },
      children: [
        /* @__PURE__ */ jsx(
          SpaceMenuItem,
          {
            label: "Edit space",
            Icon: PencilIcon,
            onClick: () => {
              onClose();
              hostNavigation.navigate(buildSettingsSectionHref("spaces", activeSpaceSlug, space.slug));
            }
          }
        ),
        /* @__PURE__ */ jsx(
          SpaceMenuItem,
          {
            label: "Refresh index",
            Icon: RefreshIcon,
            onClick: handleRefresh,
            disabled: busy
          }
        ),
        /* @__PURE__ */ jsx(
          SpaceMenuItem,
          {
            label: "Open ingest",
            Icon: PlusCircleIcon,
            onClick: () => {
              onClose();
              hostNavigation.navigate(buildSectionHref("ingest", space.slug));
            }
          }
        ),
        /* @__PURE__ */ jsx(SpaceMenuDivider, {}),
        /* @__PURE__ */ jsx(
          SpaceMenuItem,
          {
            label: isDefault ? "Archive space (default cannot be archived)" : "Archive space\u2026",
            Icon: ArchiveIcon,
            onClick: handleArchive,
            disabled: isDefault || busy,
            destructive: true
          }
        )
      ]
    }
  );
}
function SpaceMenuItem({
  label,
  Icon,
  onClick,
  disabled,
  destructive
}) {
  return /* @__PURE__ */ jsxs(
    "button",
    {
      type: "button",
      role: "menuitem",
      onClick,
      disabled,
      style: {
        width: "100%",
        display: "flex",
        alignItems: "center",
        gap: 10,
        padding: "7px 10px",
        background: "transparent",
        border: "none",
        cursor: disabled ? "not-allowed" : "pointer",
        color: destructive ? "oklch(0.7 0.2 25)" : tokens.fg,
        opacity: disabled ? 0.5 : 1,
        fontSize: 13,
        textAlign: "left",
        borderRadius: 4,
        fontFamily: fontStack
      },
      onMouseEnter: (event) => {
        if (disabled) return;
        event.currentTarget.style.background = tokens.accent;
      },
      onMouseLeave: (event) => {
        event.currentTarget.style.background = "transparent";
      },
      children: [
        /* @__PURE__ */ jsx("span", { "aria-hidden": "true", style: { display: "flex", color: destructive ? "oklch(0.7 0.2 25)" : tokens.muted }, children: /* @__PURE__ */ jsx(Icon, { size: 14 }) }),
        /* @__PURE__ */ jsx("span", { style: { flex: 1 }, children: label })
      ]
    }
  );
}
function SpaceMenuDivider() {
  return /* @__PURE__ */ jsx("div", { style: { height: 1, background: tokens.border, margin: "4px 0" } });
}
var shellStyle = {
  display: "flex",
  flexDirection: "column",
  height: "100%",
  minHeight: 600,
  background: tokens.bg,
  color: tokens.fg,
  fontFamily: fontStack,
  fontSize: 14
};
function UnconfiguredFolder({ context, folder, refresh }) {
  const bootstrap = usePluginAction("bootstrap-root");
  const toast = usePluginToast();
  const isMobile = useIsMobileLayout();
  const [path, setPath] = useState(folder?.path ?? "");
  const [busy, setBusy] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const configuredButUnhealthy = Boolean(folder?.configured);
  useEffect(() => {
    setPath(folder?.path ?? "");
  }, [folder?.path]);
  async function submit() {
    if (!context.companyId || !path.trim()) return;
    setBusy(true);
    setErrorMsg(null);
    try {
      const result = await bootstrap({ companyId: context.companyId, path: path.trim() });
      const written = result.writtenFiles ?? [];
      toast({ tone: "success", title: "Wiki root configured", body: written.length ? `Created ${written.length} bootstrap file(s).` : "Existing files preserved." });
      refresh();
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      setErrorMsg(message);
      toast({ tone: "error", title: "Could not configure wiki root", body: message });
    } finally {
      setBusy(false);
    }
  }
  return /* @__PURE__ */ jsx("div", { style: { flex: 1, padding: isMobile ? 16 : 28, display: "grid", placeItems: "start", overflow: isMobile ? "visible" : "auto", minWidth: 0 }, children: /* @__PURE__ */ jsxs(Card, { style: { maxWidth: 720, width: "100%" }, children: [
    /* @__PURE__ */ jsx(CardHeader, { title: configuredButUnhealthy ? "Repair wiki root folder" : "Choose a wiki root folder" }),
    /* @__PURE__ */ jsxs(CardBody, { children: [
      /* @__PURE__ */ jsxs(Tiny, { style: { marginBottom: 12 }, children: [
        configuredButUnhealthy ? "The configured wiki root is not ready. Update the path or repair it to recreate required baseline files." : "Pick an absolute path on this machine. The plugin creates ",
        !configuredButUnhealthy ? /* @__PURE__ */ jsxs(Fragment, { children: [
          /* @__PURE__ */ jsx(Mono, { children: "raw/" }),
          ", ",
          /* @__PURE__ */ jsx(Mono, { children: "wiki/" }),
          ", ",
          /* @__PURE__ */ jsx(Mono, { children: "AGENTS.md" }),
          ", ",
          /* @__PURE__ */ jsx(Mono, { children: "IDEA.md" }),
          ", ",
          /* @__PURE__ */ jsx(Mono, { children: "wiki/index.md" }),
          ", and ",
          /* @__PURE__ */ jsx(Mono, { children: "wiki/log.md" }),
          " if they don't already exist."
        ] }) : null
      ] }),
      folder?.problems?.length ? /* @__PURE__ */ jsx(Callout, { tone: "danger", children: /* @__PURE__ */ jsx("div", { style: { display: "grid", gap: 6 }, children: folder.problems.map((problem, index) => /* @__PURE__ */ jsxs("div", { children: [
        problem.message,
        problem.path ? /* @__PURE__ */ jsxs(Fragment, { children: [
          " ",
          /* @__PURE__ */ jsx(Mono, { children: problem.path })
        ] }) : null
      ] }, `${problem.code}:${problem.path ?? index}`)) }) }) : null,
      /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 12 }, children: [
        folder ? /* @__PURE__ */ jsx(FolderHealthChecklist, { folder }) : null,
        /* @__PURE__ */ jsx(
          FolderPathPicker,
          {
            value: path,
            onChange: setPath,
            onApply: submit,
            applyLabel: configuredButUnhealthy ? "Repair & bootstrap" : "Configure & bootstrap",
            busy,
            disabled: !path.trim()
          }
        ),
        /* @__PURE__ */ jsx("div", { style: { display: "flex", gap: 8, flexWrap: "wrap" }, children: /* @__PURE__ */ jsx(Button, { variant: "ghost", onClick: () => refresh(), children: "I already configured it" }) }),
        errorMsg ? /* @__PURE__ */ jsx(Callout, { tone: "danger", children: errorMsg }) : null
      ] })
    ] })
  ] }) });
}
var TEMPLATE_PATHS = ["AGENTS.md", "IDEA.md"];
var BASELINE_DIRECTORIES = ["raw", "wiki", "wiki/sources", "wiki/projects", "wiki/entities", "wiki/concepts", "wiki/synthesis"];
var BASELINE_FILES = [...TEMPLATE_PATHS, "wiki/index.md", "wiki/log.md"];
var BASELINE_TREE_ORDER = /* @__PURE__ */ new Map([
  ["AGENTS.md", 0],
  ["IDEA.md", 1],
  ["raw", 2],
  ["wiki", 3],
  ["wiki/index.md", 0],
  ["wiki/log.md", 1],
  ["wiki/sources", 2],
  ["wiki/projects", 3],
  ["wiki/entities", 4],
  ["wiki/concepts", 5],
  ["wiki/synthesis", 6]
]);
function basename(path) {
  return path.split("/").pop() ?? path;
}
function dirname(path) {
  const idx = path.lastIndexOf("/");
  return idx === -1 ? "" : path.slice(0, idx);
}
function ensureDir(roots, dirPath) {
  const segments = dirPath.split("/").filter(Boolean);
  let parentChildren = roots;
  let currentPath = "";
  let currentNode = null;
  for (const segment of segments) {
    currentPath = currentPath ? `${currentPath}/${segment}` : segment;
    let next = parentChildren.find((c) => c.kind === "dir" && c.path === currentPath);
    if (!next) {
      next = { name: segment, path: currentPath, kind: "dir", children: [] };
      parentChildren.push(next);
    }
    parentChildren = next.children;
    currentNode = next;
  }
  if (!currentNode) {
    throw new Error(`ensureDir called with empty path`);
  }
  return currentNode;
}
function pageDisplayName(path, title) {
  const trimmed = title?.trim();
  return trimmed ? trimmed : basename(path);
}
function treeDisplayName(path, title) {
  return BASELINE_FILES.includes(path) ? basename(path) : pageDisplayName(path, title);
}
function buildBrowseTree(pages, sources) {
  const roots = [];
  const seenPaths = /* @__PURE__ */ new Set();
  for (const dirPath of BASELINE_DIRECTORIES) {
    ensureDir(roots, dirPath);
  }
  if (sources.length > 0) {
    const rawDir = ensureDir(roots, "raw");
    for (const source of sources) {
      seenPaths.add(source.rawPath);
      const node = {
        name: pageDisplayName(source.rawPath, source.title),
        path: source.rawPath,
        kind: "file",
        children: []
      };
      rawDir.children.push(node);
    }
  }
  for (const page of pages) {
    seenPaths.add(page.path);
    const parentDir = dirname(page.path);
    const file = {
      name: treeDisplayName(page.path, page.title),
      path: page.path,
      kind: "file",
      children: []
    };
    if (parentDir) {
      ensureDir(roots, parentDir).children.push(file);
    } else {
      roots.push(file);
    }
  }
  for (const path of BASELINE_FILES) {
    if (seenPaths.has(path)) continue;
    const parentDir = dirname(path);
    const node = {
      name: basename(path),
      path,
      kind: "file",
      children: [],
      action: path
    };
    seenPaths.add(path);
    if (parentDir) {
      ensureDir(roots, parentDir).children.push(node);
      continue;
    }
    roots.push({
      ...node,
      name: path
    });
  }
  function sortNodes(nodes) {
    nodes.sort((a, b) => {
      const orderA = BASELINE_TREE_ORDER.get(a.path);
      const orderB = BASELINE_TREE_ORDER.get(b.path);
      if (orderA != null || orderB != null) return (orderA ?? Number.MAX_SAFE_INTEGER) - (orderB ?? Number.MAX_SAFE_INTEGER);
      if (a.kind !== b.kind) return a.kind === "dir" ? -1 : 1;
      return a.name.localeCompare(b.name);
    });
    for (const node of nodes) {
      if (node.kind === "dir") sortNodes(node.children);
    }
  }
  sortNodes(roots);
  return roots;
}
function expandedAncestors(path) {
  if (!path) return [];
  const out = [];
  const segments = path.split("/").filter(Boolean);
  let current = "";
  for (let i = 0; i < segments.length - 1; i++) {
    current = current ? `${current}/${segments[i]}` : segments[i];
    out.push(current);
  }
  return out;
}
function BrowseTab({ context }) {
  const { pathname, search } = useHostLocation();
  const activeSpaceSlug = useMemo(() => readActiveSpaceSlugFromLocation(pathname), [pathname]);
  const pages = usePages(context.companyId, { includeRaw: true, spaceSlug: activeSpaceSlug });
  const isMobile = useIsMobileLayout();
  const selectedTreePath = readSelectedTreePathFromLocation(pathname, search) ?? firstSelectableTreePath(pages.data);
  const selected = contentPathFromTreePath(selectedTreePath);
  return /* @__PURE__ */ jsx("div", { style: { flex: 1, minWidth: 0, overflow: isMobile ? "visible" : "auto" }, children: pages.loading && !selected ? /* @__PURE__ */ jsx("div", { style: { padding: isMobile ? 16 : 28, color: tokens.muted, fontSize: 13 }, children: "Loading pages\u2026" }) : pages.error && !selected ? /* @__PURE__ */ jsx("div", { style: { padding: isMobile ? 16 : 28 }, children: /* @__PURE__ */ jsxs(Callout, { tone: "danger", children: [
    "Failed to load pages: ",
    pages.error.message
  ] }) }) : /* @__PURE__ */ jsx(PageDetail, { context, path: selected, spaceSlug: activeSpaceSlug }) });
}
function PageDetail({ context, path, spaceSlug }) {
  const content = usePageContent(context.companyId, path, spaceSlug ?? null);
  const writePage = usePluginAction("write-page");
  const toast = usePluginToast();
  const hostNavigation = useHostNavigation();
  const isMobile = useIsMobileLayout();
  const markdownBodyRef = useRef(null);
  const [editing, setEditing] = useState(false);
  const [savedHash, setSavedHash] = useState(null);
  const [provenanceOpen, setProvenanceOpen] = useState(false);
  const [tocOpen, setTocOpen] = useState(true);
  const [activeTocId, setActiveTocId] = useState(null);
  const parsedMarkdown = useMemo(() => parseWikiMarkdown(content.data?.contents ?? ""), [content.data?.contents]);
  const tocHeadings = useMemo(() => extractWikiTocHeadings(parsedMarkdown.body), [parsedMarkdown.body]);
  useEffect(() => {
    setEditing(false);
    setSavedHash(null);
    setTocOpen(true);
    setActiveTocId(null);
  }, [path]);
  useEffect(() => {
    if (!content.data || editing) return;
    const root = markdownBodyRef.current;
    if (!root) return;
    const renderedHeadings = Array.from(root.querySelectorAll("h2, h3, h4"));
    tocHeadings.forEach((heading, index) => {
      const element = renderedHeadings[index];
      if (element instanceof HTMLElement) {
        element.id = heading.id;
      }
    });
  }, [content.data, editing, tocHeadings]);
  useEffect(() => {
    if (!content.data || editing || tocHeadings.length === 0) return;
    const root = markdownBodyRef.current;
    if (!root) return;
    const scrollParent = findScrollableAncestor(root);
    const updateActiveHeading = () => {
      const containerTop = scrollParent instanceof HTMLElement ? scrollParent.getBoundingClientRect().top : 0;
      const activationY = containerTop + 96;
      let activeId = tocHeadings[0]?.id ?? null;
      for (const heading of tocHeadings) {
        const element = root.ownerDocument.getElementById(heading.id);
        if (!(element instanceof HTMLElement)) continue;
        if (!root.contains(element)) continue;
        if (element.getBoundingClientRect().top <= activationY) {
          activeId = heading.id;
        } else {
          break;
        }
      }
      setActiveTocId(activeId);
    };
    updateActiveHeading();
    scrollParent.addEventListener("scroll", updateActiveHeading, { passive: true });
    window.addEventListener("resize", updateActiveHeading);
    return () => {
      scrollParent.removeEventListener("scroll", updateActiveHeading);
      window.removeEventListener("resize", updateActiveHeading);
    };
  }, [content.data, editing, tocHeadings]);
  const editable = path ? isEditableWikiPagePath(path) : false;
  const resolveWikiLinkHref = useCallback(
    (target) => buildWikiLinkHref(target, hostNavigation.resolveHref),
    [hostNavigation.resolveHref]
  );
  const handleTocClick = useCallback((event, id) => {
    const root = markdownBodyRef.current;
    const target = root?.ownerDocument.getElementById(id);
    if (!(target instanceof HTMLElement)) return;
    if (root && !root.contains(target)) return;
    event.preventDefault();
    setActiveTocId(id);
    target.scrollIntoView({ block: "start", behavior: "smooth" });
    if (typeof window !== "undefined") {
      window.history.replaceState(window.history.state, "", `${window.location.pathname}${window.location.search}#${id}`);
    }
  }, []);
  const savePageContents = useCallback(async (nextContents) => {
    if (!context.companyId || !content.data || !editable || !path) return;
    const result = await writePage({
      companyId: context.companyId,
      wikiId: content.data.wikiId,
      spaceSlug: spaceSlug ?? null,
      path,
      contents: nextContents,
      expectedHash: savedHash ?? content.data.hash,
      summary: `Edited ${path} from the LLM Wiki page`
    });
    if (typeof result.hash === "string") setSavedHash(result.hash);
  }, [context.companyId, content.data, editable, path, savedHash, writePage, spaceSlug]);
  if (!path) return /* @__PURE__ */ jsx("div", { style: { padding: isMobile ? 16 : 28, color: tokens.muted, fontSize: 13 }, children: "Pick a page from the tree." });
  if (content.loading) return /* @__PURE__ */ jsxs("div", { style: { padding: isMobile ? 16 : 28, color: tokens.muted, fontSize: 13 }, children: [
    "Loading ",
    path,
    "\u2026"
  ] });
  if (content.error && path.startsWith("raw/")) {
    return /* @__PURE__ */ jsxs("div", { style: { padding: isMobile ? 16 : 28, display: "grid", gap: 12 }, children: [
      /* @__PURE__ */ jsxs(Callout, { tone: "warn", children: [
        "The captured source ",
        /* @__PURE__ */ jsx(Mono, { children: path }),
        " is indexed but no longer exists in the configured wiki folder. Refresh the wiki or re-ingest the source to restore it."
      ] }),
      /* @__PURE__ */ jsx(Tiny, { children: content.error.message })
    ] });
  }
  if (content.error) return /* @__PURE__ */ jsx("div", { style: { padding: isMobile ? 16 : 28 }, children: /* @__PURE__ */ jsxs(Callout, { tone: "danger", children: [
    "Failed to read ",
    path,
    ": ",
    content.error.message
  ] }) });
  if (!content.data) return /* @__PURE__ */ jsxs("div", { style: { padding: isMobile ? 16 : 28, color: tokens.muted, fontSize: 13 }, children: [
    "No content for ",
    path,
    "."
  ] });
  const { contents, title, sourceRefs, updatedAt, hash } = content.data;
  const visibleFrontmatter = parsedMarkdown.frontmatter.filter((property) => property.key.toLowerCase() !== "title");
  const displayTitle = BASELINE_FILES.includes(path) ? basename(path) : title ?? basename(path);
  const folderPath = dirname(path);
  const isDistilledProjectPage = path.startsWith("wiki/projects/");
  const showToc = !editing && tocHeadings.length > 0;
  const displaySourceRefs = sourceRefs.map((ref, index) => ({
    id: sourceRefIdentity(ref, index),
    label: formatSourceRef(ref, index),
    hasDisplayText: typeof ref === "string" || readSourceRefField(ref, "title") !== null
  })).filter((ref) => ref.hasDisplayText);
  return /* @__PURE__ */ jsxs("article", { style: { padding: isMobile ? "16px" : "24px 28px", display: "grid", gap: isMobile ? 12 : 14, minWidth: 0 }, children: [
    /* @__PURE__ */ jsxs("header", { style: { display: "grid", gap: 8, minWidth: 0 }, children: [
      /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "flex-start", gap: 12, flexWrap: "wrap", minWidth: 0 }, children: [
        /* @__PURE__ */ jsxs("div", { style: { flex: "1 1 260px", minWidth: 0 }, children: [
          folderPath ? /* @__PURE__ */ jsx(Tiny, { style: { display: "block", marginBottom: 6 }, children: /* @__PURE__ */ jsx(Mono, { children: folderPath }) }) : null,
          /* @__PURE__ */ jsx("h1", { style: { margin: 0, fontSize: isMobile ? 20 : 22, overflowWrap: "anywhere" }, children: displayTitle })
        ] }),
        /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 6, alignItems: "center" }, children: [
          isDistilledProjectPage ? /* @__PURE__ */ jsxs(Button, { size: "sm", variant: "ghost", onClick: () => setProvenanceOpen(true), title: "Show source provenance and freshness", children: [
            /* @__PURE__ */ jsx(InfoIcon, { size: 12 }),
            " Provenance"
          ] }) : null,
          editable && !editing ? /* @__PURE__ */ jsx(Button, { size: "sm", onClick: () => {
            setSavedHash(hash);
            setEditing(true);
          }, children: "Edit page" }) : editable ? /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: () => {
            setEditing(false);
            content.refresh();
          }, children: "Done" }) : /* @__PURE__ */ jsx(Badge, { children: "read-only source" })
        ] })
      ] }),
      /* @__PURE__ */ jsxs(Tiny, { children: [
        "Updated ",
        updatedAt ? formatTime(updatedAt) : "\u2014"
      ] })
    ] }),
    isDistilledProjectPage && path ? /* @__PURE__ */ jsx(FreshnessChip, { companyId: context.companyId, pagePath: path, companyPrefix: context.companyPrefix ?? null }) : null,
    editing ? /* @__PURE__ */ jsx(Fragment, { children: /* @__PURE__ */ jsx(
      AutosaveMarkdownEditor,
      {
        resetKey: `${path}:${hash}`,
        value: contents,
        placeholder: `Edit ${path}`,
        minHeight: isMobile ? 260 : 420,
        onSave: async (nextContents) => {
          await savePageContents(nextContents);
          toast({ tone: "success", title: `${path} saved` });
        }
      },
      `${path}:${hash}`
    ) }) : /* @__PURE__ */ jsxs(
      "div",
      {
        "data-testid": "llm-wiki-page-content-layout",
        style: {
          display: "grid",
          gridTemplateColumns: showToc && !isMobile ? `minmax(0, 1fr) ${tocOpen ? "minmax(180px, 240px)" : "36px"}` : "minmax(0, 1fr)",
          gap: showToc && !isMobile ? tocOpen ? 24 : 10 : 0,
          alignItems: "start",
          minWidth: 0
        },
        children: [
          /* @__PURE__ */ jsx("div", { style: { display: "grid", gap: isMobile ? 12 : 14, minWidth: 0 }, children: /* @__PURE__ */ jsxs("div", { ref: markdownBodyRef, style: { minWidth: 0, fontSize: 13, lineHeight: 1.65 }, children: [
            /* @__PURE__ */ jsx(FrontmatterProperties, { properties: visibleFrontmatter }),
            /* @__PURE__ */ jsx(
              MarkdownBlock,
              {
                content: parsedMarkdown.body,
                enableWikiLinks: true,
                resolveWikiLinkHref
              }
            ),
            displaySourceRefs.length > 0 ? /* @__PURE__ */ jsxs("section", { "aria-label": "Paperclip source refs", style: { marginTop: 16, display: "grid", gap: 6 }, children: [
              /* @__PURE__ */ jsx(Tiny, { style: { fontWeight: 650 }, children: "Paperclip source refs" }),
              /* @__PURE__ */ jsx("ul", { style: { margin: 0, paddingLeft: 18, color: tokens.muted, fontSize: 12, lineHeight: 1.5 }, children: displaySourceRefs.map((ref) => /* @__PURE__ */ jsx("li", { children: ref.label }, ref.id)) })
            ] }) : null
          ] }) }),
          showToc ? /* @__PURE__ */ jsx(
            OnThisPagePane,
            {
              headings: tocHeadings,
              activeHeadingId: activeTocId,
              open: tocOpen,
              onToggle: () => setTocOpen((current) => !current),
              onHeadingClick: handleTocClick,
              mobile: isMobile
            }
          ) : null
        ]
      }
    ),
    provenanceOpen && path ? /* @__PURE__ */ jsx(
      ProvenanceDrawer,
      {
        companyId: context.companyId,
        pagePath: path,
        onClose: () => setProvenanceOpen(false)
      }
    ) : null
  ] });
}
function findScrollableAncestor(element) {
  let current = element.parentElement;
  while (current && current !== document.body && current !== document.documentElement) {
    const style = window.getComputedStyle(current);
    const overflowY = style.overflowY;
    if ((overflowY === "auto" || overflowY === "scroll") && current.scrollHeight > current.clientHeight) {
      return current;
    }
    current = current.parentElement;
  }
  return window;
}
function OnThisPagePane({
  headings,
  activeHeadingId,
  open,
  onToggle,
  onHeadingClick,
  mobile
}) {
  const contentId = "llm-wiki-on-this-page";
  const currentHeadingId = activeHeadingId ?? headings[0]?.id ?? null;
  const shellRef = useRef(null);
  const [fixedFrame, setFixedFrame] = useState(null);
  useEffect(() => {
    if (mobile) {
      setFixedFrame(null);
      return;
    }
    const shell = shellRef.current;
    if (!shell) return;
    let animationFrame = 0;
    const updateFrame = () => {
      cancelAnimationFrame(animationFrame);
      animationFrame = requestAnimationFrame(() => {
        const rect = shell.getBoundingClientRect();
        const top = Math.max(WIKI_TOC_STICKY_TOP, rect.top);
        setFixedFrame((current) => {
          if (current && Math.abs(current.left - rect.left) < 0.5 && Math.abs(current.top - top) < 0.5 && Math.abs(current.width - rect.width) < 0.5) {
            return current;
          }
          return { left: rect.left, top, width: rect.width };
        });
      });
    };
    updateFrame();
    const resizeObserver = typeof ResizeObserver === "undefined" ? null : new ResizeObserver(updateFrame);
    resizeObserver?.observe(shell);
    window.addEventListener("resize", updateFrame);
    window.addEventListener("scroll", updateFrame, true);
    return () => {
      cancelAnimationFrame(animationFrame);
      resizeObserver?.disconnect();
      window.removeEventListener("resize", updateFrame);
      window.removeEventListener("scroll", updateFrame, true);
    };
  }, [mobile, open]);
  const paneStyle = mobile ? {} : fixedFrame ? {
    position: "fixed",
    top: fixedFrame.top,
    left: fixedFrame.left,
    width: fixedFrame.width,
    maxHeight: `calc(100vh - ${fixedFrame.top + 16}px)`,
    overflowY: "auto",
    zIndex: 2
  } : {
    position: "sticky",
    top: WIKI_TOC_STICKY_TOP
  };
  return /* @__PURE__ */ jsx(
    "aside",
    {
      ref: shellRef,
      "aria-label": "On this page",
      style: {
        order: mobile ? -1 : 0,
        minWidth: 0,
        minHeight: mobile ? void 0 : open ? 120 : 24,
        alignSelf: "start"
      },
      children: /* @__PURE__ */ jsxs("div", { style: {
        ...paneStyle,
        borderLeft: open ? `1px solid ${tokens.border}` : 0,
        paddingLeft: open ? 10 : 0
      }, children: [
        /* @__PURE__ */ jsxs(
          "button",
          {
            type: "button",
            "aria-label": open ? "Collapse on this page" : "Expand on this page",
            "aria-expanded": open,
            "aria-controls": contentId,
            onClick: onToggle,
            style: {
              display: "flex",
              alignItems: "center",
              justifyContent: open ? "space-between" : "center",
              gap: 10,
              width: "100%",
              border: 0,
              background: "transparent",
              color: tokens.fg,
              padding: open ? "0 0 8px" : "0",
              fontFamily: fontStack,
              fontSize: 12,
              fontWeight: 650,
              cursor: "pointer",
              textAlign: "left"
            },
            children: [
              open ? /* @__PURE__ */ jsx("span", { children: "On this page" }) : null,
              /* @__PURE__ */ jsx("span", { "aria-hidden": "true", style: { color: tokens.muted, transform: open ? "rotate(90deg)" : "rotate(0deg)", transition: "transform 120ms ease" }, children: /* @__PURE__ */ jsx(ChevronLeftIcon, { size: 13 }) })
            ]
          }
        ),
        open ? /* @__PURE__ */ jsx("nav", { id: contentId, style: { display: "grid", gap: 2 }, children: headings.map((heading) => {
          const active = heading.id === currentHeadingId;
          return /* @__PURE__ */ jsx(
            "a",
            {
              href: `#${heading.id}`,
              "aria-current": active ? "location" : void 0,
              onClick: (event) => onHeadingClick(event, heading.id),
              style: {
                display: "block",
                padding: `3px 0 3px ${Math.max(0, heading.level - 2) * 12}px`,
                color: active ? tokens.fg : tokens.muted,
                fontSize: 12,
                fontWeight: active ? 700 : 450,
                lineHeight: 1.35,
                textDecoration: "none",
                overflowWrap: "anywhere"
              },
              children: heading.text
            },
            heading.id
          );
        }) }) : null
      ] })
    }
  );
}
function FreshnessChip({ companyId, pagePath, companyPrefix }) {
  const provenance = useDistillationProvenance(companyId, pagePath);
  const binding = provenance.data?.binding ?? null;
  const cursor = provenance.data?.cursor ?? null;
  if (provenance.loading && !provenance.data) {
    return /* @__PURE__ */ jsx(FreshnessChipShell, { tone: "info", icon: /* @__PURE__ */ jsx(ClockIcon, { size: 14 }), children: "Checking distillation cursor\u2026" });
  }
  if (!binding) return null;
  const lastEnd = binding.lastRunSourceWindowEnd ?? binding.lastRunCompletedAt;
  const status = binding.lastRunStatus ?? "unknown";
  const sourceCount = (() => {
    const meta = binding.metadata;
    const refs = Array.isArray(meta.sourceRefs) ? meta.sourceRefs.length : null;
    return refs;
  })();
  const isStale = (() => {
    if (status === "failed" || status === "refused_cost_cap") return true;
    if (!lastEnd) return true;
    const diff = Date.now() - Date.parse(lastEnd);
    return Number.isFinite(diff) ? diff > 72 * 60 * 60 * 1e3 : true;
  })();
  const isFailed = status === "failed" || status === "refused_cost_cap";
  const isRunning = status === "running";
  const tone = isFailed ? "danger" : isStale ? "warn" : isRunning ? "running" : "info";
  const projectLink = cursor && cursor.projectId && companyPrefix ? `/${companyPrefix}/projects/${cursor.projectId}` : null;
  return /* @__PURE__ */ jsxs(
    FreshnessChipShell,
    {
      tone,
      icon: isFailed ? /* @__PURE__ */ jsx(AlertTriangleIcon, { size: 14 }) : isRunning ? /* @__PURE__ */ jsx(ActivityIcon, { size: 14 }) : /* @__PURE__ */ jsx(ClockIcon, { size: 14 }),
      children: [
        /* @__PURE__ */ jsxs("span", { children: [
          /* @__PURE__ */ jsxs("strong", { children: [
            "Current as of ",
            lastEnd ? formatTimestamp(lastEnd) : "\u2014",
            "."
          ] }),
          sourceCount ? ` ${sourceCount} sources in this window. ` : " ",
          "Cursor at ",
          /* @__PURE__ */ jsx(Mono, { children: lastEnd ?? "\u2014" }),
          "."
        ] }),
        projectLink ? /* @__PURE__ */ jsx("a", { href: projectLink, style: { marginLeft: 8, color: "inherit", textDecoration: "underline" }, children: "Open Paperclip for live state \u2192" }) : null
      ]
    }
  );
}
function FreshnessChipShell({ tone, icon, children }) {
  const palette = tone === "danger" ? { bg: "oklch(0.22 0.06 25)", fg: "oklch(0.85 0.12 25)", border: "oklch(0.45 0.12 25)" } : tone === "warn" ? { bg: "oklch(0.22 0.06 70)", fg: "oklch(0.85 0.1 70)", border: "oklch(0.45 0.12 70)" } : tone === "running" ? { bg: "oklch(0.22 0.06 200)", fg: "oklch(0.85 0.11 200)", border: "oklch(0.45 0.12 200)" } : tokens.callout;
  return /* @__PURE__ */ jsxs("div", { style: {
    display: "inline-flex",
    alignItems: "center",
    gap: 8,
    padding: "8px 12px",
    borderRadius: 999,
    background: palette.bg,
    color: palette.fg,
    border: `1px solid ${palette.border}`,
    fontSize: 12.5,
    lineHeight: 1.5,
    flexWrap: "wrap"
  }, children: [
    icon,
    /* @__PURE__ */ jsx("div", { style: { display: "flex", alignItems: "center", gap: 4, flexWrap: "wrap" }, children })
  ] });
}
function readSourceRefField(ref, field) {
  const value = ref[field];
  return typeof value === "string" && value.trim().length > 0 ? value : null;
}
function sourceRefIdentity(ref, fallbackIndex) {
  if (typeof ref === "string") return ref;
  const issueIdentifier = readSourceRefField(ref, "issueIdentifier");
  const issueId = readSourceRefField(ref, "issueId");
  const commentId = readSourceRefField(ref, "commentId");
  const documentKey = readSourceRefField(ref, "documentKey");
  const documentId = readSourceRefField(ref, "documentId");
  const kind = readSourceRefField(ref, "kind");
  const parts = [kind, issueIdentifier ?? issueId, commentId, documentKey ?? documentId].filter(Boolean);
  return parts.length > 0 ? parts.join(":") : `source-ref-${fallbackIndex}`;
}
function formatSourceRef(ref, fallbackIndex) {
  if (typeof ref === "string") return ref;
  const kind = readSourceRefField(ref, "kind");
  const issue = readSourceRefField(ref, "issueIdentifier") ?? readSourceRefField(ref, "issueId");
  const title = readSourceRefField(ref, "title");
  const commentId = readSourceRefField(ref, "commentId");
  const documentKey = readSourceRefField(ref, "documentKey");
  const documentId = readSourceRefField(ref, "documentId");
  const primary = issue ?? sourceRefIdentity(ref, fallbackIndex);
  const suffix = kind === "comment" && commentId ? ` comment ${commentId.slice(0, 8)}` : kind === "document" && (documentKey || documentId) ? ` document ${documentKey ?? documentId?.slice(0, 8)}` : kind ? ` ${kind}` : "";
  return title ? `${primary}${suffix} - ${title}` : `${primary}${suffix}`;
}
function ProvenanceDrawer({ companyId, pagePath, onClose }) {
  const isMobile = useIsMobileLayout();
  const provenance = useDistillationProvenance(companyId, pagePath);
  const data = provenance.data;
  const binding = data?.binding ?? null;
  const cursor = data?.cursor ?? null;
  const snapshot = data?.snapshot ?? null;
  const runs = data?.runs ?? [];
  return /* @__PURE__ */ jsx(
    "div",
    {
      onClick: onClose,
      style: {
        position: "fixed",
        inset: 0,
        background: "oklch(0 0 0 / 0.55)",
        zIndex: 50,
        display: "flex",
        justifyContent: isMobile ? "stretch" : "flex-end",
        alignItems: isMobile ? "flex-end" : "stretch"
      },
      children: /* @__PURE__ */ jsxs(
        "div",
        {
          onClick: (event) => event.stopPropagation(),
          style: {
            background: tokens.bg,
            borderLeft: isMobile ? void 0 : `1px solid ${tokens.border}`,
            borderTop: isMobile ? `1px solid ${tokens.border}` : void 0,
            width: isMobile ? "100%" : 420,
            maxWidth: "100%",
            maxHeight: isMobile ? "85vh" : "100vh",
            display: "flex",
            flexDirection: "column"
          },
          children: [
            /* @__PURE__ */ jsxs("div", { style: { padding: "14px 18px", borderBottom: `1px solid ${tokens.border}`, display: "flex", alignItems: "center", gap: 10 }, children: [
              /* @__PURE__ */ jsx(InfoIcon, { size: 16 }),
              /* @__PURE__ */ jsx("strong", { style: { fontSize: 14, flex: 1 }, children: "Provenance" }),
              /* @__PURE__ */ jsx(Button, { variant: "ghost", size: "sm", onClick: onClose, children: /* @__PURE__ */ jsx(XIcon, { size: 14 }) })
            ] }),
            /* @__PURE__ */ jsxs("div", { style: { overflow: "auto", flex: 1, padding: 18, display: "flex", flexDirection: "column", gap: 14 }, children: [
              provenance.loading && !data ? /* @__PURE__ */ jsx(Tiny, { children: "Loading provenance\u2026" }) : null,
              !binding && !provenance.loading ? /* @__PURE__ */ jsx(Callout, { children: "This page is not currently bound to a distillation cursor. It may be hand-authored or pre-distillation." }) : null,
              binding ? /* @__PURE__ */ jsx(Card, { children: /* @__PURE__ */ jsxs(CardBody, { padding: 14, children: [
                /* @__PURE__ */ jsx(PropRow, { label: "Page path", value: /* @__PURE__ */ jsx(Mono, { children: binding.pagePath }) }),
                /* @__PURE__ */ jsx(PropRow, { label: "Source hash", value: /* @__PURE__ */ jsxs(Mono, { children: [
                  binding.lastRunSourceHash?.slice(0, 16) ?? "\u2014",
                  "\u2026"
                ] }) }),
                /* @__PURE__ */ jsx(PropRow, { label: "Cursor end", value: /* @__PURE__ */ jsx(Mono, { children: binding.lastRunSourceWindowEnd ? formatTimestamp(binding.lastRunSourceWindowEnd) : "\u2014" }) }),
                /* @__PURE__ */ jsx(PropRow, { label: "Last run status", value: /* @__PURE__ */ jsx(Badge, { tone: runStatusTone(binding.lastRunStatus ?? ""), children: binding.lastRunStatus ?? "\u2014" }) }),
                /* @__PURE__ */ jsx(PropRow, { label: "Project", value: binding.projectName ?? "\u2014" }),
                /* @__PURE__ */ jsx(PropRow, { label: "Updated", value: formatTimestamp(binding.updatedAt) })
              ] }) }) : null,
              cursor ? /* @__PURE__ */ jsxs(Card, { children: [
                /* @__PURE__ */ jsx(CardHeader, { title: "Cursor" }),
                /* @__PURE__ */ jsxs(CardBody, { padding: 14, children: [
                  /* @__PURE__ */ jsx(PropRow, { label: "Scope", value: cursor.sourceScope }),
                  /* @__PURE__ */ jsx(PropRow, { label: "Pending events", value: String(cursor.pendingEventCount) }),
                  /* @__PURE__ */ jsx(PropRow, { label: "Last observed", value: formatTimestamp(cursor.lastObservedAt) }),
                  /* @__PURE__ */ jsx(PropRow, { label: "Last processed", value: formatTimestamp(cursor.lastProcessedAt) })
                ] })
              ] }) : null,
              snapshot ? /* @__PURE__ */ jsxs(Card, { children: [
                /* @__PURE__ */ jsx(CardHeader, { title: "Sources in this window" }),
                /* @__PURE__ */ jsxs(CardBody, { padding: 14, children: [
                  /* @__PURE__ */ jsxs(Tiny, { style: { marginBottom: 8 }, children: [
                    snapshot.sourceRefs.length,
                    " ref",
                    snapshot.sourceRefs.length === 1 ? "" : "s",
                    snapshot.clipped ? " \xB7 clipped" : ""
                  ] }),
                  /* @__PURE__ */ jsx("ul", { style: { margin: 0, paddingLeft: 18, fontSize: 12, color: tokens.muted, lineHeight: 1.5 }, children: snapshot.sourceRefs.slice(0, 8).map((ref, index) => {
                    const obj = typeof ref === "object" && ref ? ref : null;
                    const id = obj && typeof obj.id === "string" ? obj.id : typeof ref === "string" ? ref : `ref-${index}`;
                    const kind = obj && typeof obj.kind === "string" ? obj.kind : null;
                    const title = obj && typeof obj.title === "string" ? obj.title : null;
                    return /* @__PURE__ */ jsxs("li", { style: { marginBottom: 4 }, children: [
                      /* @__PURE__ */ jsx(Mono, { children: id }),
                      kind ? ` \xB7 ${kind}` : "",
                      title ? ` \xB7 ${title}` : ""
                    ] }, `${id}-${index}`);
                  }) }),
                  snapshot.sourceRefs.length > 8 ? /* @__PURE__ */ jsx(Tiny, { style: { marginTop: 6 }, children: `+${snapshot.sourceRefs.length - 8} more` }) : null
                ] })
              ] }) : null,
              runs.length > 0 ? /* @__PURE__ */ jsxs(Card, { children: [
                /* @__PURE__ */ jsx(CardHeader, { title: "Operations affecting this page" }),
                /* @__PURE__ */ jsx(CardBody, { padding: 0, children: /* @__PURE__ */ jsx("ul", { style: { margin: 0, padding: 0, listStyle: "none" }, children: runs.slice(0, 8).map((run) => /* @__PURE__ */ jsxs("li", { style: { padding: "10px 14px", borderBottom: `1px solid ${tokens.border}`, display: "flex", justifyContent: "space-between", alignItems: "center", gap: 8, flexWrap: "wrap" }, children: [
                  /* @__PURE__ */ jsx(Mono, { style: { fontSize: 12 }, children: run.operationIssueIdentifier ?? `op-${run.id.slice(0, 6)}` }),
                  /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }, children: [
                    /* @__PURE__ */ jsx(Tiny, { children: formatTimestamp(run.updatedAt) }),
                    /* @__PURE__ */ jsx(Mono, { style: { fontSize: 11 }, children: formatCostCents(run.costCents) }),
                    /* @__PURE__ */ jsx(Badge, { tone: runStatusTone(run.status), children: runStatusLabel(run.status) })
                  ] })
                ] }, run.id)) }) })
              ] }) : null
            ] })
          ]
        }
      )
    }
  );
}
function FrontmatterProperties({ properties }) {
  if (properties.length === 0) return null;
  return /* @__PURE__ */ jsxs(
    "details",
    {
      open: true,
      style: {
        marginBottom: 20,
        paddingBottom: 16,
        borderBottom: `1px solid ${tokens.border}`
      },
      children: [
        /* @__PURE__ */ jsx(
          "summary",
          {
            style: {
              cursor: "pointer",
              color: tokens.fg,
              fontSize: 13,
              fontWeight: 650,
              listStylePosition: "outside",
              marginBottom: 10
            },
            children: "Properties"
          }
        ),
        /* @__PURE__ */ jsx("dl", { style: { display: "grid", gap: 8, margin: 0, maxWidth: 720 }, children: properties.map((property) => /* @__PURE__ */ jsxs(
          "div",
          {
            style: {
              display: "grid",
              gridTemplateColumns: "minmax(96px, 0.32fr) minmax(0, 1fr)",
              gap: 12,
              alignItems: "baseline",
              minWidth: 0
            },
            children: [
              /* @__PURE__ */ jsx("dt", { style: { color: tokens.muted, fontSize: 12, minWidth: 0, overflowWrap: "anywhere" }, children: property.key }),
              /* @__PURE__ */ jsx("dd", { style: { margin: 0, minWidth: 0 }, children: /* @__PURE__ */ jsx(FrontmatterValue, { value: property.value }) })
            ]
          },
          property.key
        )) })
      ]
    }
  );
}
function FrontmatterValue({ value }) {
  if (Array.isArray(value)) {
    return /* @__PURE__ */ jsx("span", { style: { display: "flex", flexWrap: "wrap", gap: 6, minWidth: 0 }, children: value.map((item) => /* @__PURE__ */ jsx(
      "span",
      {
        style: {
          display: "inline-flex",
          alignItems: "center",
          minWidth: 0,
          maxWidth: "100%",
          padding: "1px 7px",
          borderRadius: 999,
          background: "var(--secondary, oklch(0.269 0 0))",
          color: tokens.fg,
          fontSize: 12,
          lineHeight: 1.5,
          overflowWrap: "anywhere"
        },
        children: item
      },
      item
    )) });
  }
  return /* @__PURE__ */ jsx("span", { style: { color: tokens.fg, fontSize: 13, overflowWrap: "anywhere" }, children: value });
}
function Row({ primary, secondary, right }) {
  return /* @__PURE__ */ jsxs("div", { style: {
    display: "flex",
    alignItems: "center",
    flexWrap: "wrap",
    gap: 12,
    padding: "8px 12px",
    borderBottom: `1px solid ${tokens.border}`,
    fontSize: 13,
    minWidth: 0
  }, children: [
    /* @__PURE__ */ jsx("div", { style: { flex: "1 1 220px", minWidth: 0, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "normal", overflowWrap: "anywhere" }, children: primary }),
    secondary ? /* @__PURE__ */ jsx("div", { style: { flex: "0 1 auto", minWidth: 0, color: tokens.muted, fontSize: 12, overflowWrap: "anywhere" }, children: secondary }) : null,
    right ? /* @__PURE__ */ jsx("div", { style: { marginLeft: "auto", minWidth: 0, display: "flex", gap: 6, alignItems: "center", justifyContent: "flex-end", flexWrap: "wrap" }, children: right }) : null
  ] });
}
function formatTime(iso) {
  if (!iso) return "\u2014";
  try {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return iso;
    const diffMs = Date.now() - date.getTime();
    const minutes = Math.floor(diffMs / 6e4);
    if (minutes < 1) return "just now";
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 30) return `${days}d ago`;
    return date.toLocaleDateString();
  } catch {
    return iso;
  }
}
function IngestTab({ context, refreshOverview }) {
  const { pathname } = useHostLocation();
  const activeSpaceSlug = useMemo(() => readActiveSpaceSlugFromLocation(pathname), [pathname]);
  const spacesQuery = useSpaces(context.companyId);
  const spaces = useMemo(() => {
    const list = spacesQuery.data?.spaces ?? [];
    return activeWikiSpaces(list).sort(compareSpaces);
  }, [spacesQuery.data]);
  const ingest = usePluginAction("ingest-source");
  const toast = usePluginToast();
  const hostNavigation = useHostNavigation();
  const isMobile = useIsMobileLayout();
  const [url, setUrl] = useState("");
  const [pasted, setPasted] = useState("");
  const [title, setTitle] = useState("");
  const [busy, setBusy] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const [spaceMenuOpen, setSpaceMenuOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const activeSpace = useMemo(() => {
    return spaces.find((space) => space.slug === activeSpaceSlug) ?? spaces.find((space) => space.slug === DEFAULT_SPACE_SLUG) ?? null;
  }, [spaces, activeSpaceSlug]);
  const canSubmit = !!context.companyId && (pasted.trim().length > 0 || url.trim().length > 0) && !busy;
  useEffect(() => {
    const refresh = () => refreshOverview();
    window.addEventListener("pc-wiki-ingest-queued", refresh);
    return () => window.removeEventListener("pc-wiki-ingest-queued", refresh);
  }, [refreshOverview]);
  async function submit() {
    if (!context.companyId) return;
    setBusy(true);
    setErrorMsg(null);
    try {
      let contents = pasted.trim();
      let sourceType = "text";
      let resolvedTitle = title.trim();
      if (url.trim()) {
        sourceType = "url";
        contents = pasted.trim() || `Captured URL: ${url.trim()}

_Plugin needs to fetch the URL \u2014 placeholder body for the alpha._`;
        resolvedTitle = resolvedTitle || url.trim();
      } else {
        resolvedTitle = resolvedTitle || pasted.split("\n", 1)[0]?.slice(0, 80) || "Pasted source";
      }
      await ingest({
        companyId: context.companyId,
        spaceSlug: activeSpaceSlug,
        sourceType,
        url: url.trim() || null,
        title: resolvedTitle,
        contents
      });
      const spaceLabel2 = activeSpace?.displayName ?? activeSpaceSlug;
      toast({ tone: "success", title: `Source captured into ${spaceLabel2}`, body: `Operation issue created. Check History to inspect.` });
      setUrl("");
      setPasted("");
      setTitle("");
      refreshOverview();
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      setErrorMsg(message);
      toast({ tone: "error", title: "Ingest failed", body: message });
    } finally {
      setBusy(false);
    }
  }
  const spaceLabel = activeSpace?.displayName ?? activeSpaceSlug;
  return /* @__PURE__ */ jsxs("div", { style: { flex: 1, minHeight: isMobile ? "auto" : 0, minWidth: 0, overflow: isMobile ? "visible" : "auto" }, children: [
    /* @__PURE__ */ jsxs("div", { style: {
      padding: isMobile ? "16px" : "24px 28px",
      maxWidth: 920,
      minWidth: 0
    }, children: [
      /* @__PURE__ */ jsx("div", { style: { marginBottom: 4, fontSize: 11, fontWeight: 600, letterSpacing: "0.04em", textTransform: "uppercase", color: tokens.muted }, children: "Add Content" }),
      /* @__PURE__ */ jsxs("h2", { style: { margin: "0 0 6px", fontSize: 18, fontWeight: 650 }, children: [
        "Capture into ",
        /* @__PURE__ */ jsx("span", { style: { color: tokens.fg }, children: spaceLabel })
      ] }),
      /* @__PURE__ */ jsxs(Tiny, { style: { marginBottom: 18 }, children: [
        "Each capture queues an ingest operation scoped to ",
        /* @__PURE__ */ jsx(Mono, { children: activeSpaceSlug }),
        ". Files land in that space's ",
        /* @__PURE__ */ jsx(Mono, { children: "raw/" }),
        " folder and the Wiki Maintainer proposes a patch."
      ] }),
      /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 14, marginBottom: 18 }, children: [
        /* @__PURE__ */ jsx(
          SpacePicker,
          {
            spaces,
            activeSpaceSlug,
            loading: spacesQuery.loading,
            error: spacesQuery.error?.message ?? null,
            isOpen: spaceMenuOpen,
            onToggle: () => setSpaceMenuOpen((v) => !v),
            onClose: () => setSpaceMenuOpen(false),
            onSelect: (slug) => {
              setSpaceMenuOpen(false);
              hostNavigation.navigate(buildSectionHref("ingest", slug));
            },
            onCreate: () => {
              setSpaceMenuOpen(false);
              setCreateOpen(true);
            }
          }
        ),
        /* @__PURE__ */ jsxs("div", { children: [
          /* @__PURE__ */ jsx("label", { style: { fontSize: 11, color: tokens.muted, display: "block", marginBottom: 6 }, children: "Drop files anywhere on this page" }),
          /* @__PURE__ */ jsxs("div", { style: {
            minHeight: isMobile ? 180 : 230,
            border: `1.5px dashed ${tokens.pluginBorder}`,
            borderRadius: 8,
            padding: isMobile ? 20 : 30,
            display: "flex",
            flexDirection: "column",
            justifyContent: "center",
            alignItems: "center",
            gap: 10,
            textAlign: "center",
            color: tokens.muted,
            background: tokens.pluginBg
          }, children: [
            /* @__PURE__ */ jsx("div", { style: { display: "inline-flex", alignItems: "center", justifyContent: "center", width: 46, height: 46, borderRadius: 8, background: tokens.card, color: tokens.pluginFg, border: `1px solid ${tokens.pluginBorder}` }, children: /* @__PURE__ */ jsx(DownloadCloudIcon, { size: 24 }) }),
            /* @__PURE__ */ jsx("div", { style: { fontSize: 16, fontWeight: 650, color: tokens.fg }, children: "Drop source files here" }),
            /* @__PURE__ */ jsx(Tiny, { children: "Review staged files before queueing maintainer tasks." })
          ] })
        ] }),
        /* @__PURE__ */ jsxs("div", { "data-testid": "llm-wiki-ingest-manual-separator", "aria-hidden": "true", style: { display: "flex", alignItems: "center", gap: 12, color: tokens.muted, fontSize: 11, fontWeight: 650, textTransform: "uppercase", letterSpacing: "0.04em" }, children: [
          /* @__PURE__ */ jsx("span", { style: { height: 1, flex: 1, background: tokens.border } }),
          /* @__PURE__ */ jsx("span", { children: "or" }),
          /* @__PURE__ */ jsx("span", { style: { height: 1, flex: 1, background: tokens.border } })
        ] }),
        /* @__PURE__ */ jsxs("div", { children: [
          /* @__PURE__ */ jsx("label", { style: { fontSize: 11, color: tokens.muted, display: "block", marginBottom: 4 }, children: "Source title (optional)" }),
          /* @__PURE__ */ jsx(TextInput, { value: title, onChange: (e) => setTitle(e.target.value), placeholder: "e.g. Karpathy LLM Wiki gist" })
        ] }),
        /* @__PURE__ */ jsxs("div", { children: [
          /* @__PURE__ */ jsx("label", { style: { fontSize: 11, color: tokens.muted, display: "block", marginBottom: 4 }, children: "URL" }),
          /* @__PURE__ */ jsx(TextInput, { value: url, onChange: (e) => setUrl(e.target.value), placeholder: "https://example.com/article" })
        ] }),
        /* @__PURE__ */ jsxs("div", { children: [
          /* @__PURE__ */ jsx("label", { style: { fontSize: 11, color: tokens.muted, display: "block", marginBottom: 4 }, children: "Paste markdown / text" }),
          /* @__PURE__ */ jsx(TextArea, { value: pasted, onChange: (e) => setPasted(e.target.value), placeholder: "Paste source content\u2026", rows: 8 })
        ] })
      ] }),
      /* @__PURE__ */ jsx("div", { style: { display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }, children: /* @__PURE__ */ jsx(Button, { variant: "primary", onClick: submit, disabled: !canSubmit, loading: busy, children: "+ Capture & ingest" }) }),
      errorMsg ? /* @__PURE__ */ jsx("div", { style: { marginTop: 14 }, children: /* @__PURE__ */ jsx(Callout, { tone: "danger", children: errorMsg }) }) : null
    ] }),
    createOpen && context.companyId ? /* @__PURE__ */ jsx(
      CreateSpaceModal,
      {
        companyId: context.companyId,
        existingSlugs: new Set(spaces.map((s) => s.slug)),
        onClose: () => setCreateOpen(false),
        onCreated: (space) => {
          setCreateOpen(false);
          spacesQuery.refresh();
          hostNavigation.navigate(buildSectionHref("ingest", space.slug));
        }
      }
    ) : null
  ] });
}
function SpacePicker({
  spaces,
  activeSpaceSlug,
  loading,
  error,
  isOpen,
  onToggle,
  onClose,
  onSelect,
  onCreate
}) {
  const ref = useRef(null);
  const active = spaces.find((s) => s.slug === activeSpaceSlug);
  useEffect(() => {
    if (!isOpen) return;
    const handler = (event) => {
      if (!ref.current) return;
      if (event.target instanceof Node && ref.current.contains(event.target)) return;
      onClose();
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [isOpen, onClose]);
  return /* @__PURE__ */ jsxs("div", { ref, style: { position: "relative" }, children: [
    /* @__PURE__ */ jsx("label", { style: { fontSize: 11, color: tokens.muted, display: "block", marginBottom: 4 }, children: "Space" }),
    /* @__PURE__ */ jsxs(
      "button",
      {
        type: "button",
        onClick: onToggle,
        "aria-haspopup": "listbox",
        "aria-expanded": isOpen,
        style: {
          width: "100%",
          display: "flex",
          alignItems: "center",
          gap: 8,
          background: "oklch(0.2 0 0)",
          border: `1px solid ${tokens.border}`,
          borderRadius: 6,
          padding: "8px 10px",
          color: tokens.fg,
          fontFamily: fontStack,
          fontSize: 13,
          cursor: "pointer",
          textAlign: "left"
        },
        children: [
          /* @__PURE__ */ jsx(FolderIcon, { size: 14 }),
          /* @__PURE__ */ jsx("span", { style: { flex: 1, fontWeight: 600 }, children: active?.displayName ?? activeSpaceSlug }),
          active ? /* @__PURE__ */ jsx(Badge, { tone: "default", style: { fontSize: 10, padding: "0 6px" }, children: active.accessScope }) : null,
          /* @__PURE__ */ jsx(ChevronDownIcon, { size: 14 })
        ]
      }
    ),
    /* @__PURE__ */ jsx("div", { style: { marginTop: 6, fontSize: 11, color: tokens.muted, lineHeight: 1.4 }, children: "Defaults to the space you opened. Switching now re-routes the page so deep links carry the destination." }),
    isOpen ? /* @__PURE__ */ jsxs(
      "div",
      {
        role: "listbox",
        style: {
          position: "absolute",
          top: "calc(100% + 4px)",
          left: 0,
          right: 0,
          zIndex: 25,
          background: tokens.card,
          border: `1px solid ${tokens.border}`,
          borderRadius: 8,
          boxShadow: "0 16px 40px rgba(0,0,0,0.45)",
          padding: 4,
          maxHeight: 320,
          overflowY: "auto"
        },
        children: [
          error ? /* @__PURE__ */ jsx("div", { style: { padding: 10, fontSize: 12, color: tokens.statusBlocked }, children: error }) : null,
          loading ? /* @__PURE__ */ jsx("div", { style: { padding: 10 }, children: /* @__PURE__ */ jsx(Tiny, { children: "Loading spaces\u2026" }) }) : null,
          spaces.map((space) => /* @__PURE__ */ jsxs(
            "button",
            {
              type: "button",
              role: "option",
              "aria-selected": space.slug === activeSpaceSlug,
              onClick: () => onSelect(space.slug),
              style: {
                width: "100%",
                display: "flex",
                alignItems: "center",
                gap: 8,
                padding: "8px 10px",
                background: space.slug === activeSpaceSlug ? tokens.accent : "transparent",
                border: "none",
                borderRadius: 6,
                color: tokens.fg,
                fontSize: 13,
                fontFamily: fontStack,
                cursor: "pointer",
                textAlign: "left"
              },
              children: [
                /* @__PURE__ */ jsx(FolderIcon, { size: 14 }),
                /* @__PURE__ */ jsx("span", { style: { flex: 1, fontWeight: 600 }, children: space.displayName }),
                /* @__PURE__ */ jsx("span", { style: { fontSize: 10, color: tokens.muted, fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace" }, children: space.slug })
              ]
            },
            space.slug
          )),
          /* @__PURE__ */ jsx(SpaceMenuDivider, {}),
          /* @__PURE__ */ jsxs(
            "button",
            {
              type: "button",
              onClick: onCreate,
              style: {
                width: "100%",
                display: "flex",
                alignItems: "center",
                gap: 8,
                padding: "8px 10px",
                background: "transparent",
                border: "none",
                borderRadius: 6,
                color: "oklch(0.78 0.13 250)",
                fontSize: 13,
                fontFamily: fontStack,
                cursor: "pointer",
                textAlign: "left",
                fontWeight: 600
              },
              children: [
                /* @__PURE__ */ jsx(PlusIcon, { size: 14 }),
                /* @__PURE__ */ jsx("span", { children: "New shared space\u2026" })
              ]
            }
          )
        ]
      }
    ) : null
  ] });
}
function statusTone(status) {
  if (status === "done") return "done";
  if (status === "running" || status === "in_progress") return "running";
  if (status === "blocked" || status === "failed") return "failed";
  if (status === "queued" || status === "todo") return "todo";
  if (status === "paused") return "paused";
  return "default";
}
function QueryTab({ context, overview }) {
  const startQuery = usePluginAction("start-query");
  const fileAsPage = usePluginAction("file-as-page");
  const toast = usePluginToast();
  const { pathname } = useHostLocation();
  const activeSpaceSlug = useMemo(() => readActiveSpaceSlugFromLocation(pathname), [pathname]);
  const isMobile = useIsMobileLayout();
  const [thread, setThread] = useState([]);
  const [prompt, setPrompt] = useState("");
  const [busy, setBusy] = useState(false);
  const [filePath, setFilePath] = useState("wiki/concepts/new-page.md");
  const [fileBody, setFileBody] = useState("");
  const [filing, setFiling] = useState(null);
  const fileSource = useMemo(() => {
    for (let i = thread.length - 1; i >= 0; i -= 1) {
      const entry = thread[i];
      if (entry.answer.trim()) return entry;
    }
    return null;
  }, [thread]);
  const activeEntry = useMemo(() => {
    for (let i = thread.length - 1; i >= 0; i -= 1) {
      const entry = thread[i];
      if (entry.status === "running" || entry.status === "queued") return entry;
    }
    return null;
  }, [thread]);
  const stream = usePluginStream(activeEntry?.channel ?? "llm-wiki:idle", {
    companyId: context.companyId ?? void 0
  });
  useEffect(() => {
    if (!activeEntry || !stream.lastEvent) return;
    const event = stream.lastEvent;
    setThread((prev) => prev.map((entry) => {
      if (entry.id !== activeEntry.id) return entry;
      if (event.type === "agent.event" && event.eventType === "chunk" && event.message && event.stream !== "stderr") {
        return { ...entry, answer: entry.answer + event.message, status: "running" };
      }
      if (event.type === "query.done") {
        return { ...entry, status: "done", answer: event.answer ?? entry.answer };
      }
      if (event.type === "query.error") {
        return { ...entry, status: "error", errorMessage: event.message ?? "agent session error" };
      }
      return entry;
    }));
    if (event.type === "query.done" && event.answer && !fileBody.trim()) {
      setFileBody(event.answer);
    }
  }, [fileBody, stream.lastEvent, activeEntry?.id]);
  async function send() {
    if (!context.companyId || !prompt.trim()) return;
    setBusy(true);
    const entryId = `q-${Date.now()}`;
    setThread((prev) => [...prev, {
      id: entryId,
      prompt: prompt.trim(),
      operationId: null,
      querySessionId: null,
      hiddenIssueIdentifier: null,
      channel: null,
      status: "queued",
      createdAt: (/* @__PURE__ */ new Date()).toISOString(),
      answer: ""
    }]);
    try {
      const res = await startQuery({ companyId: context.companyId, spaceSlug: activeSpaceSlug, question: prompt.trim() });
      const result = res;
      setThread((prev) => prev.map(
        (entry) => entry.id === entryId ? {
          ...entry,
          operationId: result.operationId,
          querySessionId: result.querySessionId ?? result.operationId,
          hiddenIssueIdentifier: result.issue?.identifier ?? null,
          channel: result.channel ?? `llm-wiki:query:${result.operationId}`,
          status: "running"
        } : entry
      ));
      setPrompt("");
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      setThread((prev) => prev.map(
        (entry) => entry.id === entryId ? { ...entry, status: "error", errorMessage: message } : entry
      ));
      toast({ tone: "error", title: "Ask failed", body: message });
    } finally {
      setBusy(false);
    }
  }
  async function fileAnswer(entry) {
    const source = entry ?? fileSource;
    const answer = fileBody.trim() || source?.answer.trim() || "";
    if (!context.companyId || !filePath.trim() || !answer) return;
    setFiling(source?.id ?? "manual");
    try {
      await fileAsPage({
        companyId: context.companyId,
        wikiId: overview.wikiId,
        spaceSlug: activeSpaceSlug,
        path: filePath.trim(),
        question: source?.prompt,
        answer,
        querySessionId: source?.querySessionId
      });
      toast({ tone: "success", title: "Answer filed", body: `Wrote ${filePath.trim()} and recorded a file-as-page task.` });
      setFileBody("");
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      toast({ tone: "error", title: "Could not file answer", body: message });
    } finally {
      setFiling(null);
    }
  }
  return /* @__PURE__ */ jsxs("div", { style: { display: "flex", flexDirection: isMobile ? "column" : "row", flex: 1, minHeight: isMobile ? "auto" : 0, minWidth: 0 }, children: [
    /* @__PURE__ */ jsxs("div", { style: { flex: 1, padding: isMobile ? "16px" : "24px 28px", overflow: isMobile ? "visible" : "auto", minWidth: 0 }, children: [
      thread.length === 0 ? /* @__PURE__ */ jsx(Callout, { children: "Ask the wiki anything. Each question initiates a task assigned to the Wiki Maintainer. The answer streams below; you can promote useful answers into a wiki page." }) : null,
      /* @__PURE__ */ jsx("div", { style: { display: "grid", gap: 22, marginTop: 18 }, children: thread.map((entry) => /* @__PURE__ */ jsxs("div", { children: [
        /* @__PURE__ */ jsxs(Tiny, { style: { marginBottom: 4 }, children: [
          "You \xB7 ",
          formatTime(entry.createdAt)
        ] }),
        /* @__PURE__ */ jsx("div", { style: { background: tokens.card, border: `1px solid ${tokens.border}`, padding: "10px 12px", borderRadius: 8, fontSize: 13 }, children: entry.prompt }),
        /* @__PURE__ */ jsxs(Tiny, { style: { marginTop: 8 }, children: [
          "Wiki Maintainer \xB7 ",
          entry.status,
          entry.hiddenIssueIdentifier ? /* @__PURE__ */ jsxs(Fragment, { children: [
            " \xB7 ",
            /* @__PURE__ */ jsx(Mono, { children: entry.hiddenIssueIdentifier })
          ] }) : null
        ] }),
        entry.status === "error" ? /* @__PURE__ */ jsx("div", { style: { marginTop: 6 }, children: /* @__PURE__ */ jsx(Callout, { tone: "danger", children: entry.errorMessage }) }) : /* @__PURE__ */ jsx("pre", { style: {
          margin: "8px 0 0",
          whiteSpace: "pre-wrap",
          wordBreak: "break-word",
          fontFamily: "ui-sans-serif, system-ui, sans-serif",
          fontSize: 13,
          lineHeight: 1.65,
          color: tokens.fg
        }, children: entry.answer || (entry.status === "running" ? "Streaming\u2026" : "") }),
        entry.answer.trim() && entry.status === "done" ? /* @__PURE__ */ jsxs("div", { style: { marginTop: 10, border: `1px dashed ${tokens.border}`, borderRadius: 8, padding: "10px 12px" }, children: [
          /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }, children: [
            /* @__PURE__ */ jsx("strong", { style: { fontSize: 13 }, children: "\u{1F4D1} File this answer as a wiki page?" }),
            /* @__PURE__ */ jsxs(Tiny, { style: { marginLeft: isMobile ? 0 : "auto", width: isMobile ? "100%" : void 0 }, children: [
              "Path: ",
              /* @__PURE__ */ jsx(Mono, { children: filePath })
            ] })
          ] }),
          /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 6, marginTop: 8, alignItems: "center", flexWrap: "wrap" }, children: [
            /* @__PURE__ */ jsx(TextInput, { value: filePath, onChange: (e) => setFilePath(e.target.value), style: { maxWidth: isMobile ? "none" : 360 } }),
            /* @__PURE__ */ jsx(Button, { size: "sm", variant: "primary", onClick: () => fileAnswer(entry), disabled: !filePath.trim(), loading: filing === entry.id, children: "Accept & file" })
          ] })
        ] }) : null
      ] }, entry.id)) }),
      /* @__PURE__ */ jsxs("div", { style: { borderTop: `1px solid ${tokens.border}`, paddingTop: 14, marginTop: 22 }, children: [
        /* @__PURE__ */ jsx(TextArea, { value: prompt, onChange: (e) => setPrompt(e.target.value), placeholder: "Ask the wiki\u2026", rows: 3 }),
        /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 6, marginTop: 8, alignItems: "center", flexWrap: "wrap" }, children: [
          /* @__PURE__ */ jsx(Button, { variant: "primary", size: "sm", onClick: send, disabled: !prompt.trim(), loading: busy, children: "Send (\u2318\u21B5)" }),
          /* @__PURE__ */ jsx(Badge, { children: "Cite: wiki + raw" }),
          /* @__PURE__ */ jsx(Badge, { children: "Max steps: 6" }),
          /* @__PURE__ */ jsx(Tiny, { style: { marginLeft: "auto" }, children: "Streamed via agent session \xB7 maintainer task" })
        ] })
      ] })
    ] }),
    /* @__PURE__ */ jsxs("aside", { style: {
      width: isMobile ? "auto" : 320,
      borderLeft: isMobile ? "none" : `1px solid ${tokens.border}`,
      borderTop: isMobile ? `1px solid ${tokens.border}` : "none",
      padding: isMobile ? "16px" : "18px 20px",
      overflow: isMobile ? "visible" : "auto",
      minWidth: 0
    }, children: [
      /* @__PURE__ */ jsx(Tiny, { style: { marginBottom: 8 }, children: "SESSION" }),
      /* @__PURE__ */ jsx(PropRow, { label: "Wiki", value: overview.wikiId }),
      /* @__PURE__ */ jsx(PropRow, { label: "Project", value: overview.managedProject.details?.name ?? overview.managedProject.status }),
      /* @__PURE__ */ jsx(PropRow, { label: "Agent", value: overview.managedAgent.details?.name ?? overview.managedAgent.status }),
      /* @__PURE__ */ jsx(PropRow, { label: "Operations", value: overview.operationCount }),
      /* @__PURE__ */ jsx(PropRow, { label: "Stream", value: stream.connected ? "live" : stream.connecting ? "connecting\u2026" : "idle" }),
      /* @__PURE__ */ jsx(Divider, {}),
      /* @__PURE__ */ jsx(Tiny, { style: { marginBottom: 8 }, children: "ASK PROMPT" }),
      /* @__PURE__ */ jsx("pre", { style: { margin: 0, whiteSpace: "pre-wrap", fontFamily: "ui-monospace, monospace", fontSize: 12, color: tokens.muted }, children: overview.prompts.query })
    ] })
  ] });
}
function SettingsLintPanel({ context }) {
  const overview = useOverview(context.companyId);
  if (overview.error) {
    return /* @__PURE__ */ jsx(SettingsPanel, { title: "Lint", badge: /* @__PURE__ */ jsx(HiddenOpBadge, {}), description: "Run structural checks for orphan pages, missing backlinks, and stale provenance.", children: /* @__PURE__ */ jsxs(Callout, { tone: "danger", children: [
      "LLM Wiki bridge error: ",
      overview.error.message
    ] }) });
  }
  if (!overview.data) {
    return /* @__PURE__ */ jsx(SettingsPanel, { title: "Lint", badge: /* @__PURE__ */ jsx(HiddenOpBadge, {}), description: "Run structural checks for orphan pages, missing backlinks, and stale provenance.", children: /* @__PURE__ */ jsx(Tiny, { children: "Loading lint controls\u2026" }) });
  }
  return /* @__PURE__ */ jsx(
    SettingsPanel,
    {
      title: "Lint",
      badge: /* @__PURE__ */ jsx(HiddenOpBadge, {}),
      description: "Run structural checks for orphan pages, missing backlinks, and stale provenance.",
      children: /* @__PURE__ */ jsx(LintPanelContent, { context, overview: overview.data, refreshOverview: overview.refresh, showHeading: false })
    }
  );
}
function LintPanelContent({
  context,
  overview,
  refreshOverview,
  showHeading = true
}) {
  const create = usePluginAction("create-operation");
  const { pathname } = useHostLocation();
  const activeSpaceSlug = useMemo(() => readActiveSpaceSlugFromLocation(pathname), [pathname]);
  const operations = useOperations(context.companyId, { operationType: "lint", spaceSlug: activeSpaceSlug });
  const toast = usePluginToast();
  const isMobile = useIsMobileLayout();
  const [busy, setBusy] = useState(false);
  async function runLint() {
    if (!context.companyId) return;
    setBusy(true);
    try {
      await create({
        companyId: context.companyId,
        spaceSlug: activeSpaceSlug,
        operationType: "lint",
        title: `Run LLM Wiki lint \xB7 ${activeSpaceSlug}`,
        prompt: overview.prompts.lint
      });
      toast({ tone: "success", title: "Lint queued", body: "Lint runs as a Wiki Maintainer task. Findings will appear here once the run completes." });
      operations.refresh();
      refreshOverview();
    } catch (err) {
      toast({ tone: "error", title: "Could not run lint", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setBusy(false);
    }
  }
  const recent = operations.data?.operations ?? [];
  const latestDone = recent.find((op) => op.status === "done");
  const findings = Array.isArray(latestDone?.warnings) ? latestDone.warnings : [];
  const counts = aggregateLintFindings(findings);
  return /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: isMobile ? 14 : 18, minWidth: 0 }, children: [
    /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }, children: [
      showHeading ? /* @__PURE__ */ jsx("h2", { style: { margin: 0, fontSize: 16, fontWeight: 600 }, children: "Lint" }) : null,
      /* @__PURE__ */ jsxs(Badge, { style: unfilledSurfaceStyle, children: [
        recent.length,
        " run",
        recent.length === 1 ? "" : "s"
      ] }),
      /* @__PURE__ */ jsx(Button, { variant: "primary", size: "sm", onClick: runLint, loading: busy, style: { marginLeft: isMobile ? 0 : "auto" }, children: "\u25B6 Run lint now" })
    ] }),
    /* @__PURE__ */ jsxs("div", { style: { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))", gap: 12 }, children: [
      /* @__PURE__ */ jsx(StatCard, { label: "Findings", value: String(counts.total), hint: latestDone ? `last run ${formatTime(latestDone.createdAt)}` : "no runs yet" }),
      /* @__PURE__ */ jsx(StatCard, { label: "Critical", value: String(counts.critical), hint: "contradictions / conflict", tone: counts.critical > 0 ? "danger" : void 0 }),
      /* @__PURE__ */ jsx(StatCard, { label: "Orphans", value: String(counts.orphan), hint: "no inbound backlinks" }),
      /* @__PURE__ */ jsx(StatCard, { label: "Stale", value: String(counts.stale), hint: "provenance > 30d" }),
      /* @__PURE__ */ jsx(StatCard, { label: "Index drift", value: String(counts.index), hint: "wiki/index.md / wiki/log.md" })
    ] }),
    /* @__PURE__ */ jsxs(Card, { style: unfilledSurfaceStyle, children: [
      /* @__PURE__ */ jsx(CardHeader, { title: "Findings", badges: /* @__PURE__ */ jsx(HiddenOpBadge, {}), right: /* @__PURE__ */ jsx(Tiny, { children: "Lint runs as a Wiki Maintainer task. Critical findings can optionally open visible follow-up issues \u2014 toggle in Settings \u2192 Lint policy." }) }),
      /* @__PURE__ */ jsx(CardBody, { padding: 0, children: findings.length === 0 ? /* @__PURE__ */ jsx("div", { style: { padding: 16, color: tokens.muted, fontSize: 13 }, children: latestDone ? "Latest lint run reported no findings." : "No completed lint runs yet. Use \u25B6 Run lint now to start one." }) : findings.map((f, idx) => {
        const severityTone = f.severity === "critical" ? "failed" : f.severity === "orphan" ? "paused" : "default";
        return /* @__PURE__ */ jsx(
          Row,
          {
            primary: /* @__PURE__ */ jsxs("span", { children: [
              /* @__PURE__ */ jsx(Badge, { tone: severityTone, style: severityTone === "default" ? unfilledSurfaceStyle : void 0, children: String(f.severity ?? "info") }),
              /* @__PURE__ */ jsx("span", { style: { marginLeft: 8 }, children: String(f.message ?? f.title ?? "(no description)") })
            ] }),
            secondary: f.path ? /* @__PURE__ */ jsx(Mono, { children: String(f.path) }) : null
          },
          idx
        );
      }) })
    ] }),
    /* @__PURE__ */ jsxs(Card, { style: unfilledSurfaceStyle, children: [
      /* @__PURE__ */ jsx(CardHeader, { title: "Recent lint runs" }),
      /* @__PURE__ */ jsx(CardBody, { padding: 0, children: recent.length === 0 ? /* @__PURE__ */ jsx("div", { style: { padding: 12, color: tokens.muted, fontSize: 13 }, children: "No lint runs yet." }) : recent.map((op) => /* @__PURE__ */ jsx(
        Row,
        {
          primary: /* @__PURE__ */ jsxs(Fragment, { children: [
            /* @__PURE__ */ jsxs(Mono, { children: [
              "op-",
              op.id.slice(0, 6)
            ] }),
            " ",
            op.hiddenIssueTitle ?? "Wiki lint"
          ] }),
          secondary: formatTime(op.createdAt),
          right: /* @__PURE__ */ jsx(Badge, { tone: statusTone(op.status), children: op.status })
        },
        op.id
      )) })
    ] })
  ] });
}
function StatCard({ label, value, hint, tone }) {
  const palette = tone === "danger" ? { color: "oklch(0.7 0.2 25)" } : tone === "warn" ? { color: "oklch(0.85 0.1 70)" } : { color: tokens.fg };
  return /* @__PURE__ */ jsxs(Card, { style: { ...unfilledSurfaceStyle, padding: 14 }, children: [
    /* @__PURE__ */ jsx(Tiny, { children: label.toUpperCase() }),
    /* @__PURE__ */ jsx("div", { style: { fontSize: 22, fontWeight: 700, marginTop: 4, ...palette }, children: value }),
    hint ? /* @__PURE__ */ jsx(Tiny, { style: { marginTop: 2 }, children: hint }) : null
  ] });
}
function aggregateLintFindings(findings) {
  const counts = { total: findings.length, critical: 0, orphan: 0, stale: 0, index: 0 };
  for (const f of findings) {
    const sev = String(f.severity ?? "");
    if (sev === "critical") counts.critical += 1;
    else if (sev === "orphan") counts.orphan += 1;
    else if (sev === "stale") counts.stale += 1;
    else if (sev === "index") counts.index += 1;
  }
  return counts;
}
function formatCostCents(cents) {
  if (!Number.isFinite(cents) || cents <= 0) return "$0.00";
  return `$${(cents / 100).toFixed(2)}`;
}
function formatTimestamp(value) {
  if (!value) return "\u2014";
  const ms = Date.parse(value);
  if (!Number.isFinite(ms)) return "\u2014";
  return new Date(ms).toLocaleString(void 0, { month: "short", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}
function runStatusTone(status) {
  if (status === "running") return "running";
  if (status === "succeeded" || status === "completed" || status === "done") return "done";
  if (status === "failed" || status === "refused_cost_cap") return "failed";
  if (status === "review_required") return "in_review";
  if (status === "paused") return "paused";
  if (status === "source_ready" || status === "queued") return "queued";
  return "default";
}
function runStatusLabel(status) {
  switch (status) {
    case "review_required":
      return "review required";
    case "refused_cost_cap":
      return "cost capped";
    case "source_ready":
      return "source ready";
    default:
      return status.replace(/_/g, " ");
  }
}
function HistoryTab({ context, overview }) {
  const isMobile = useIsMobileLayout();
  const projectId = overview.managedProject.projectId;
  const originKindPrefix = `plugin:${PLUGIN_ID}:operation`;
  if (!context.companyId) {
    return /* @__PURE__ */ jsx("div", { style: { padding: isMobile ? 16 : 24, flex: 1 }, children: /* @__PURE__ */ jsx(Callout, { children: "Choose a company to view LLM Wiki history." }) });
  }
  if (!projectId) {
    return /* @__PURE__ */ jsx("div", { style: { padding: isMobile ? 16 : 24, flex: 1 }, children: /* @__PURE__ */ jsx(Callout, { tone: "warn", children: "The LLM Wiki operations project is not resolved yet. Reconcile the managed project in Settings, then history will show its issues here." }) });
  }
  return /* @__PURE__ */ jsx("div", { style: { flex: 1, minHeight: isMobile ? "auto" : 0, overflow: isMobile ? "visible" : "auto", padding: isMobile ? 12 : 0, minWidth: 0 }, children: /* @__PURE__ */ jsx(
    PluginIssuesList,
    {
      companyId: context.companyId,
      projectId,
      filters: { originKindPrefix },
      viewStateKey: "paperclip:llm-wiki-history-issues-view",
      searchWithinLoadedIssues: true
    }
  ) });
}
function SettingsTab({ context, initialSection = "root" }) {
  const isMobile = useIsMobileLayout();
  return /* @__PURE__ */ jsx("div", { style: { flex: 1, minHeight: isMobile ? "auto" : 0, overflow: isMobile ? "visible" : "auto", padding: isMobile ? "16px" : "24px 28px", minWidth: 0 }, children: /* @__PURE__ */ jsx(SettingsBody, { context, initialSection }) });
}
var ROUTINE_FALLBACKS = {
  "cursor-window-processing": { title: "Process LLM Wiki updates", cron: "0 */6 * * *" },
  "nightly-wiki-lint": { title: "Run LLM Wiki lint", cron: "0 3 * * *" },
  "index-refresh": { title: "Refresh LLM Wiki index", cron: "0 * * * *" }
};
var MANAGED_SKILL_LABELS = {
  "wiki-maintainer": "LLM Wiki Maintainer",
  "wiki-ingest": "Wiki Ingest",
  "wiki-query": "Wiki Query",
  "wiki-lint": "Wiki Lint",
  "paperclip-distill": "Paperclip Distill",
  "index-refresh": "Index Refresh"
};
function routineFallbackFor(routine) {
  const key = routine.resourceKey?.split(":").pop() ?? "";
  return ROUTINE_FALLBACKS[key] ?? { title: routine.resourceKey ?? "Managed routine", cron: "\u2014" };
}
function managedRoutineStatus(routine) {
  return routine.routine?.status ?? routine.details?.status ?? (routine.routineId ? "paused" : "missing");
}
function routineResourceKey(routine) {
  return routine.resourceKey?.split(":").pop() ?? "";
}
function managedAgentIsReady(resource) {
  return resource.source === "managed" && Boolean(resource.agentId);
}
function managedProjectIsReady(resource) {
  return resource.source === "managed" && Boolean(resource.projectId);
}
function managedSkillIsReady(resource) {
  return resource.status !== "missing" && Boolean(resource.skillId);
}
function managedResourceKey(resourceKey) {
  return resourceKey?.split(":").pop() ?? "";
}
function skillLabel(resource) {
  const declaredLabel = MANAGED_SKILL_LABELS[managedResourceKey(resource.resourceKey)];
  return declaredLabel ?? resource.details?.name ?? resource.skill?.name ?? "Managed skill";
}
function buildAgentHealthItems(managedAgent) {
  const agentName = managedAgent.details?.name ?? "Wiki Maintainer";
  return [{
    label: agentName,
    ok: managedAgentIsReady(managedAgent) && !managedAgent.defaultDrift?.changedFiles.length,
    detail: managedAgent.source === "managed" ? managedAgent.defaultDrift?.changedFiles.length ? `The Wiki Maintainer instructions differ from the plugin default: ${managedAgent.defaultDrift.changedFiles.join(", ")}.` : "The plugin-managed Wiki Maintainer exists with current default instructions." : "The settings page is using a selected maintainer instead of the plugin-managed Wiki Maintainer."
  }];
}
function buildProjectHealthItems(managedProject) {
  const projectName = managedProject.details?.name ?? "LLM Wiki";
  return [{
    label: projectName,
    ok: managedProjectIsReady(managedProject),
    detail: managedProject.source === "managed" ? "The plugin-managed LLM Wiki project exists." : "The settings page is using a selected project instead of the plugin-managed LLM Wiki project."
  }];
}
function buildSkillHealthItems(skills) {
  if (skills.length === 0) {
    return [{
      label: "Managed skill",
      ok: false,
      detail: "No plugin-managed skills are installed in the company skill library."
    }];
  }
  return skills.map((skill) => ({
    label: skillLabel(skill),
    ok: managedSkillIsReady(skill) && !skill.defaultDrift?.changedFiles.length,
    detail: managedSkillIsReady(skill) ? skill.defaultDrift?.changedFiles.length ? `${skillLabel(skill)} differs from the plugin default: ${skill.defaultDrift.changedFiles.join(", ")}.` : `${skillLabel(skill)} is installed in the company skill library.` : `${skillLabel(skill)} is not installed in the company skill library.`
  }));
}
function buildRoutineHealthItems(routines, managedAgent, managedProject) {
  const routineByKey = new Map(routines.map((routine) => [routineResourceKey(routine), routine]));
  const expectedAgentId = managedAgent.source === "managed" ? managedAgent.agentId ?? null : null;
  const expectedProjectId = managedProject.source === "managed" ? managedProject.projectId ?? null : null;
  const items = [];
  for (const [key, fallback] of Object.entries(ROUTINE_FALLBACKS)) {
    const routine = routineByKey.get(key);
    const routineAgentId = routine?.routine?.assigneeAgentId ?? null;
    const routineProjectId = routine?.routine?.projectId ?? null;
    const missingRefs = routine?.missingRefs ?? [];
    const missing = !routine?.routineId || !routine.routine;
    const wrongAgent = Boolean(expectedAgentId && routineAgentId && routineAgentId !== expectedAgentId);
    const wrongProject = Boolean(expectedProjectId && routineProjectId && routineProjectId !== expectedProjectId);
    const missingAgent = Boolean(expectedAgentId && !routineAgentId);
    const missingProject = Boolean(expectedProjectId && !routineProjectId);
    const blockedByManagedResources = !expectedAgentId || !expectedProjectId;
    const ok = Boolean(routine && !missing && missingRefs.length === 0 && !wrongAgent && !wrongProject && !missingAgent && !missingProject && !blockedByManagedResources);
    let detail = `${fallback.title} is installed with the Wiki Maintainer and LLM Wiki project.`;
    if (missing) {
      detail = `${fallback.title} is not installed.`;
    } else if (missingRefs.length > 0) {
      detail = `${fallback.title} cannot resolve ${missingRefs.map((ref) => `${ref.resourceKind}:${ref.resourceKey}`).join(", ")}.`;
    } else if (blockedByManagedResources) {
      detail = `${fallback.title} cannot be validated until the managed agent and project are restored.`;
    } else if (wrongAgent || missingAgent) {
      detail = `${fallback.title} is not assigned to the Wiki Maintainer.`;
    } else if (wrongProject || missingProject) {
      detail = `${fallback.title} is not attached to the LLM Wiki project.`;
    }
    items.push({ label: fallback.title, ok, detail });
  }
  return items;
}
function RoutineHealthChecklist({ items }) {
  return /* @__PURE__ */ jsx(
    ManagedResourceHealthChecklist,
    {
      items,
      ariaLabel: "Wiki routines health checklist",
      heading: "Routine health"
    }
  );
}
function ManagedResourceHealthChecklist({
  items,
  ariaLabel,
  heading
}) {
  return /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, "aria-label": ariaLabel, children: [
    /* @__PURE__ */ jsx("div", { style: { fontSize: 12, fontWeight: 650, color: tokens.muted }, children: heading }),
    /* @__PURE__ */ jsxs("div", { role: "list", style: { position: "relative", display: "grid", gap: 0, padding: "2px 0" }, children: [
      items.length > 1 ? /* @__PURE__ */ jsx(
        "span",
        {
          "aria-hidden": true,
          style: {
            position: "absolute",
            left: 8,
            top: 12,
            bottom: 12,
            width: 1,
            background: "oklch(0.38 0.09 145)"
          }
        }
      ) : null,
      items.map((item) => /* @__PURE__ */ jsxs(
        "div",
        {
          role: "listitem",
          title: item.detail,
          style: {
            display: "grid",
            gridTemplateColumns: "18px minmax(0, 1fr)",
            alignItems: "center",
            gap: 10,
            padding: "7px 0",
            minWidth: 0
          },
          children: [
            /* @__PURE__ */ jsx("span", { style: { display: "inline-flex", justifyContent: "center", position: "relative", zIndex: 1, background: tokens.bg }, children: /* @__PURE__ */ jsx(StatusIcon, { status: item.ok ? "done" : "blocked" }) }),
            /* @__PURE__ */ jsx("span", { style: { fontSize: 13, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", color: item.ok ? tokens.fg : "oklch(0.85 0.1 70)" }, children: item.label })
          ]
        },
        item.label
      ))
    ] })
  ] });
}
function SkillHealthChecklist({ items }) {
  return /* @__PURE__ */ jsx(
    ManagedResourceHealthChecklist,
    {
      items,
      ariaLabel: "Wiki skills health checklist",
      heading: "Skill health"
    }
  );
}
var SETTINGS_SECTIONS = [
  { key: "root", label: "Setup", description: "" },
  { key: "spaces", label: "Spaces", description: "Destination spaces - folders, slugs, and folder health. Per-space Paperclip indexing is not configurable yet." },
  { key: "distillation", label: "Distillation", description: "Paperclip -> default space. Cursors, caps, and routines for the company-wide distillation pipeline." },
  { key: "routines", label: "Managed Routines", description: "Scheduled wiki maintenance." },
  { key: "lint", label: "Lint", description: "Run checks and review wiki health findings." },
  { key: "events", label: "Ingestion Settings", description: "Paperclip event capture into the default space (issues, comments, documents)." }
];
function SettingsSectionButton({
  section,
  active,
  onSelect
}) {
  return /* @__PURE__ */ jsxs(
    "button",
    {
      type: "button",
      "aria-current": active ? "page" : void 0,
      onClick: onSelect,
      style: {
        width: "100%",
        border: `1px solid ${active ? tokens.border : "transparent"}`,
        borderRadius: 6,
        background: "transparent",
        color: active ? tokens.fg : tokens.muted,
        cursor: "pointer",
        display: "grid",
        gap: 2,
        padding: "8px 10px",
        textAlign: "left",
        fontFamily: fontStack
      },
      children: [
        /* @__PURE__ */ jsx("span", { style: { fontSize: 13, fontWeight: 600, lineHeight: 1.3 }, children: section.label }),
        section.description ? /* @__PURE__ */ jsx("span", { style: { fontSize: 11, lineHeight: 1.35, overflowWrap: "anywhere" }, children: section.description }) : null
      ]
    }
  );
}
function SettingsPanel({
  title,
  badge,
  description,
  children
}) {
  return /* @__PURE__ */ jsxs("section", { style: { display: "grid", gap: 14, minWidth: 0 }, children: [
    /* @__PURE__ */ jsxs("header", { style: { display: "grid", gap: 6, minWidth: 0 }, children: [
      /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap", minWidth: 0 }, children: [
        /* @__PURE__ */ jsx("h2", { style: { margin: 0, fontSize: 17, fontWeight: 650, overflowWrap: "anywhere" }, children: title }),
        badge
      ] }),
      description ? /* @__PURE__ */ jsx(Tiny, { children: description }) : null
    ] }),
    /* @__PURE__ */ jsx("div", { style: { minWidth: 0 }, children })
  ] });
}
function SetupSection({ title, children, separated = false }) {
  return /* @__PURE__ */ jsxs("section", { style: { display: "grid", gap: 12, minWidth: 0, paddingTop: separated ? 22 : 0, borderTop: separated ? `1px solid ${tokens.border}` : "none" }, children: [
    /* @__PURE__ */ jsx("h2", { style: { margin: 0, fontSize: 16, fontWeight: 650 }, children: title }),
    /* @__PURE__ */ jsx("div", { style: { minWidth: 0 }, children })
  ] });
}
var PATH_PLATFORM_LABELS = {
  mac: "macOS",
  windows: "Windows",
  linux: "Linux"
};
var PATH_PLATFORM_STEPS = {
  mac: [
    "Open Finder and navigate to the folder.",
    "Control-click the folder.",
    "Hold Option, choose Copy as Pathname, then paste it here."
  ],
  windows: [
    "Open File Explorer and navigate to the folder.",
    "Click the address bar to reveal the full path.",
    "Copy the path and paste it here."
  ],
  linux: [
    "Open a terminal in the directory.",
    "Run pwd to print the full path.",
    "Copy the output and paste it here."
  ]
};
function detectPathPlatform() {
  if (typeof navigator === "undefined") return "mac";
  const agent = navigator.userAgent.toLowerCase();
  if (agent.includes("win")) return "windows";
  if (agent.includes("linux")) return "linux";
  return "mac";
}
function PathInstructionsDialog({ open, onClose }) {
  const [platform, setPlatform] = useState(detectPathPlatform);
  useEffect(() => {
    if (!open) return;
    function handleKeyDown(event) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose, open]);
  if (!open) return null;
  return /* @__PURE__ */ jsx(
    "div",
    {
      role: "presentation",
      onClick: onClose,
      style: {
        position: "fixed",
        inset: 0,
        zIndex: 50,
        background: "rgba(0, 0, 0, 0.48)",
        display: "grid",
        placeItems: "center",
        padding: 18
      },
      children: /* @__PURE__ */ jsxs(
        "div",
        {
          role: "dialog",
          "aria-modal": "true",
          "aria-labelledby": "wiki-path-help-title",
          onClick: (event) => event.stopPropagation(),
          style: {
            width: "min(460px, 100%)",
            border: `1px solid ${tokens.border}`,
            borderRadius: 8,
            background: tokens.card,
            color: tokens.fg,
            boxShadow: "0 24px 70px rgba(0, 0, 0, 0.45)",
            padding: 18,
            display: "grid",
            gap: 14
          },
          children: [
            /* @__PURE__ */ jsxs("div", { style: { display: "flex", justifyContent: "space-between", gap: 12, alignItems: "start" }, children: [
              /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 4 }, children: [
                /* @__PURE__ */ jsx("h3", { id: "wiki-path-help-title", style: { margin: 0, fontSize: 15, fontWeight: 650 }, children: "Get a full folder path" }),
                /* @__PURE__ */ jsxs(Tiny, { children: [
                  "Paste an absolute path such as ",
                  /* @__PURE__ */ jsx(Mono, { children: "/Users/you/company-wiki" }),
                  "."
                ] })
              ] }),
              /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: onClose, title: "Close path help", children: "Close" })
            ] }),
            /* @__PURE__ */ jsx("div", { style: { display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 4, border: `1px solid ${tokens.border}`, borderRadius: 7, padding: 3 }, children: Object.keys(PATH_PLATFORM_LABELS).map((key) => /* @__PURE__ */ jsx(
              "button",
              {
                type: "button",
                onClick: () => setPlatform(key),
                style: {
                  border: 0,
                  borderRadius: 5,
                  background: key === platform ? tokens.accent : "transparent",
                  color: key === platform ? tokens.fg : tokens.muted,
                  padding: "6px 8px",
                  cursor: "pointer",
                  fontSize: 12,
                  fontFamily: fontStack
                },
                children: PATH_PLATFORM_LABELS[key]
              },
              key
            )) }),
            /* @__PURE__ */ jsx("ol", { style: { margin: 0, paddingLeft: 20, display: "grid", gap: 8, fontSize: 13, lineHeight: 1.45 }, children: PATH_PLATFORM_STEPS[platform].map((step) => /* @__PURE__ */ jsx("li", { children: step }, step)) })
          ]
        }
      )
    }
  );
}
function FolderPathPicker({
  value,
  onChange,
  onApply,
  applyLabel,
  busy,
  disabled,
  onRefresh
}) {
  const [helpOpen, setHelpOpen] = useState(false);
  return /* @__PURE__ */ jsxs("div", { style: {
    border: `1px solid ${tokens.border}`,
    borderRadius: 8,
    background: "oklch(0.18 0 0)",
    overflow: "hidden",
    minWidth: 0
  }, children: [
    /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "center", gap: 10, padding: "10px 12px", borderBottom: `1px solid ${tokens.border}` }, children: [
      /* @__PURE__ */ jsx("span", { "aria-hidden": true, style: {
        width: 28,
        height: 28,
        borderRadius: 7,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        background: tokens.accent,
        color: tokens.pluginFg,
        flexShrink: 0
      }, children: /* @__PURE__ */ jsx(FolderOpenIcon, {}) }),
      /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 2, minWidth: 0 }, children: [
        /* @__PURE__ */ jsx("span", { style: { fontSize: 13, fontWeight: 650 }, children: "Local wiki folder" }),
        /* @__PURE__ */ jsx(Tiny, { children: "Absolute path on this machine" })
      ] })
    ] }),
    /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 8, padding: 12, alignItems: "center", flexWrap: "wrap" }, children: [
      /* @__PURE__ */ jsx(
        TextInput,
        {
          value,
          onChange: (event) => onChange(event.target.value),
          placeholder: "/absolute/path/to/wiki-root",
          style: { flex: "1 1 320px", fontFamily: "ui-monospace, SFMono-Regular, monospace" }
        }
      ),
      /* @__PURE__ */ jsxs(Button, { size: "sm", onClick: () => setHelpOpen(true), children: [
        /* @__PURE__ */ jsx(FolderOpenIcon, { size: 13 }),
        " Choose"
      ] }),
      /* @__PURE__ */ jsx(Button, { variant: "primary", size: "sm", onClick: onApply, loading: busy, disabled: disabled || !value.trim(), children: applyLabel }),
      onRefresh ? /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: onRefresh, children: "Run health check" }) : null
    ] }),
    /* @__PURE__ */ jsx(PathInstructionsDialog, { open: helpOpen, onClose: () => setHelpOpen(false) })
  ] });
}
function folderHealthItems(folder) {
  return [
    { label: "Path configured", ok: folder.configured },
    { label: "Readable", ok: folder.readable },
    { label: folder.access === "readWrite" ? "Writable" : "Read-only access", ok: folder.access === "read" || folder.writable },
    { label: "Baseline files", ok: folder.missingFiles.length === 0 },
    { label: "Wiki folders", ok: folder.missingDirectories.length === 0 }
  ];
}
function FolderHealthChecklist({ folder }) {
  const items = folderHealthItems(folder);
  return /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, "aria-label": "Wiki root health checklist", children: [
    /* @__PURE__ */ jsx("div", { style: { fontSize: 12, fontWeight: 650, color: tokens.muted }, children: "Health check" }),
    /* @__PURE__ */ jsxs("div", { role: "list", style: { position: "relative", display: "grid", gap: 0, padding: "2px 0" }, children: [
      items.length > 1 ? /* @__PURE__ */ jsx(
        "span",
        {
          "aria-hidden": true,
          style: {
            position: "absolute",
            left: 8,
            top: 12,
            bottom: 12,
            width: 1,
            background: "oklch(0.38 0.09 145)"
          }
        }
      ) : null,
      items.map((item) => /* @__PURE__ */ jsxs(
        "div",
        {
          role: "listitem",
          style: {
            display: "grid",
            gridTemplateColumns: "18px minmax(0, 1fr)",
            alignItems: "center",
            gap: 10,
            padding: "7px 0",
            minWidth: 0
          },
          children: [
            /* @__PURE__ */ jsx("span", { style: { display: "inline-flex", justifyContent: "center", position: "relative", zIndex: 1, background: tokens.bg }, children: /* @__PURE__ */ jsx(StatusIcon, { status: item.ok ? "done" : "blocked" }) }),
            /* @__PURE__ */ jsx("span", { style: { fontSize: 13, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", color: item.ok ? tokens.fg : "oklch(0.85 0.1 70)" }, children: item.label })
          ]
        },
        item.label
      ))
    ] })
  ] });
}
function projectStatusLabel(status) {
  if (!status) return "unknown";
  return status.replace(/_/g, " ");
}
function adapterTypeLabel(adapterType) {
  if (!adapterType) return "unknown adapter";
  return adapterType.replace(/_/g, " ");
}
function DistillationSettingsPanel({ context, settings }) {
  const overview = useDistillationOverview(context.companyId);
  const distillNow = usePluginAction("distill-paperclip-now");
  const enableActiveProjects = usePluginAction("enable-paperclip-distillation-active-projects");
  const queueBackfill = usePluginAction("backfill-paperclip-distillation");
  const toast = usePluginToast();
  const isMobile = useIsMobileLayout();
  const [busy, setBusy] = useState(null);
  const data = overview.data;
  const cursors = data?.cursors ?? [];
  const counts = data?.counts ?? { cursors: 0, runningRuns: 0, failedRuns24h: 0, reviewRequired: 0 };
  const isConfigured = cursors.length > 0;
  const autoApplyRestriction = settings.distillationPolicy?.autoApplyRestriction ?? null;
  const [useCheapPath, setUseCheapPath] = useState(true);
  const projectsCovered = useMemo(() => {
    const set = /* @__PURE__ */ new Set();
    for (const cursor of cursors) {
      if (cursor.projectId) set.add(cursor.projectId);
    }
    return set.size;
  }, [cursors]);
  async function runDistillNow() {
    if (!context.companyId) return;
    if (cursors.length === 0) {
      toast({ tone: "warn", title: "Distill now needs at least one cursor" });
      return;
    }
    setBusy("distill-now");
    try {
      await distillNow({
        companyId: context.companyId,
        useCheapModelProfile: useCheapPath,
        idempotencyKey: `manual:company:${Date.now()}`
      });
      toast({
        tone: "success",
        title: "Distill now queued",
        body: "Wiki Maintainer will scan changed projects in the company and write into the default wiki space."
      });
      overview.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Distill now failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setBusy(null);
    }
  }
  async function enableForActiveProjects() {
    if (!context.companyId) return;
    setBusy("enable-active-projects");
    try {
      const result = await enableActiveProjects({ companyId: context.companyId, limit: 3 });
      const count = result.selectedProjects?.length ?? 0;
      toast({
        tone: count > 0 ? "success" : "warn",
        title: count > 0 ? "Distillation enabled" : "No active projects found",
        body: count > 0 ? `${count} active project${count === 1 ? "" : "s"} added to the distillation cursor set.` : "Create or resume a project, then enable distillation again."
      });
      overview.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Enable distillation failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setBusy(null);
    }
  }
  async function runBackfill() {
    if (!context.companyId || cursors.length === 0) return;
    const target = cursors.find((cursor) => cursor.projectId) ?? cursors[0];
    if (!target.projectId && !target.rootIssueId) {
      toast({ tone: "warn", title: "Backfill needs a project or root issue scope" });
      return;
    }
    setBusy("backfill");
    try {
      await queueBackfill({
        companyId: context.companyId,
        projectId: target.projectId ?? void 0,
        rootIssueId: target.rootIssueId ?? void 0,
        useCheapModelProfile: useCheapPath
      });
      toast({ tone: "success", title: "Backfill queued", body: target.projectName ?? target.rootIssueIdentifier ?? "Selected scope" });
      overview.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Backfill failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setBusy(null);
    }
  }
  if (overview.loading && !data) {
    return /* @__PURE__ */ jsx(Tiny, { children: "Loading distillation overview\u2026" });
  }
  if (!isConfigured) {
    return /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 16, maxWidth: 720 }, children: [
      /* @__PURE__ */ jsx(Card, { children: /* @__PURE__ */ jsx(CardBody, { padding: isMobile ? 18 : 26, children: /* @__PURE__ */ jsxs("div", { style: { display: "flex", flexDirection: "column", alignItems: "flex-start", gap: 12 }, children: [
        /* @__PURE__ */ jsx(SparklesIcon, { size: 36 }),
        /* @__PURE__ */ jsxs("div", { children: [
          /* @__PURE__ */ jsx("h3", { style: { margin: 0, fontSize: 17, fontWeight: 650 }, children: "Distillation is off" }),
          /* @__PURE__ */ jsxs(Tiny, { style: { marginTop: 6, fontSize: 13, color: tokens.fg, lineHeight: 1.55, maxWidth: 540 }, children: [
            "When enabled, the Wiki Maintainer reads Paperclip issues, comments, and documents for this company and keeps ",
            /* @__PURE__ */ jsx(Mono, { children: "wiki/projects/<slug>/standup.md" }),
            " plus ",
            /* @__PURE__ */ jsx(Mono, { children: "wiki/projects/<slug>/index.md" }),
            " pages in the",
            /* @__PURE__ */ jsx("strong", { children: " default wiki space" }),
            ". Pages stay marked stale until a cursor window succeeds - they never imply live state."
          ] }),
          /* @__PURE__ */ jsx(Tiny, { style: { marginTop: 6, fontSize: 13, color: tokens.fg, lineHeight: 1.55, maxWidth: 540 }, children: "Other spaces do not receive Paperclip-derived pages yet. They stay on manual and raw-file ingest until per-space Paperclip ingestion profiles ship." })
        ] }),
        /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 8, flexWrap: "wrap" }, children: [
          /* @__PURE__ */ jsxs(Button, { variant: "primary", size: "md", onClick: enableForActiveProjects, loading: busy === "enable-active-projects", children: [
            /* @__PURE__ */ jsx(SparklesIcon, { size: 14 }),
            " Enable for active projects"
          ] }),
          /* @__PURE__ */ jsx(Button, { variant: "default", size: "md", onClick: () => overview.refresh(), children: "Configure manually" })
        ] }),
        /* @__PURE__ */ jsx(Tiny, { children: "Suggested defaults: 3 active projects in the default space \xB7 all-section auto-apply where allowed \xB7 routines paused for 24h." })
      ] }) }) }),
      /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 12, gridTemplateColumns: isMobile ? "1fr" : "1fr 1fr" }, children: [
        /* @__PURE__ */ jsxs(Card, { children: [
          /* @__PURE__ */ jsx(CardHeader, { title: "What gets created" }),
          /* @__PURE__ */ jsx(CardBody, { padding: 14, children: /* @__PURE__ */ jsxs("ul", { style: { margin: 0, paddingLeft: 18, fontSize: 12.5, color: tokens.muted, lineHeight: 1.5 }, children: [
            /* @__PURE__ */ jsxs("li", { children: [
              "Project overviews at ",
              /* @__PURE__ */ jsx(Mono, { children: "wiki/projects/<slug>/index.md" })
            ] }),
            /* @__PURE__ */ jsxs("li", { children: [
              "Executive standups at ",
              /* @__PURE__ */ jsx(Mono, { children: "wiki/projects/<slug>/standup.md" })
            ] }),
            /* @__PURE__ */ jsxs("li", { children: [
              "Decisions and history under ",
              /* @__PURE__ */ jsx(Mono, { children: "wiki/projects/<slug>/" })
            ] }),
            /* @__PURE__ */ jsxs("li", { children: [
              "Source bundles cached under ",
              /* @__PURE__ */ jsx(Mono, { children: "raw/distill/" })
            ] })
          ] }) })
        ] }),
        /* @__PURE__ */ jsxs(Card, { children: [
          /* @__PURE__ */ jsx(CardHeader, { title: "What it never does" }),
          /* @__PURE__ */ jsx(CardBody, { padding: 14, children: /* @__PURE__ */ jsxs("ul", { style: { margin: 0, paddingLeft: 18, fontSize: 12.5, color: tokens.muted, lineHeight: 1.5 }, children: [
            /* @__PURE__ */ jsx("li", { children: "Read across companies \u2014 strict per-company isolation." }),
            /* @__PURE__ */ jsx("li", { children: "Re-distill its own plugin operation issues." }),
            /* @__PURE__ */ jsx("li", { children: "Auto-apply patches when source hashes drift." })
          ] }) })
        ] })
      ] })
    ] });
  }
  return /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 16, minWidth: 0 }, children: [
    autoApplyRestriction ? /* @__PURE__ */ jsxs(Callout, { tone: "warn", children: [
      autoApplyRestriction,
      " The plugin ignores auto-apply requests from config and manual distill actions on this instance."
    ] }) : null,
    /* @__PURE__ */ jsxs(Callout, { children: [
      /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }, children: [
        /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "center", gap: 10 }, children: [
          /* @__PURE__ */ jsx(InfoIcon, { size: 16 }),
          /* @__PURE__ */ jsxs("strong", { style: { fontSize: 13 }, children: [
            "Active for ",
            projectsCovered,
            " project",
            projectsCovered === 1 ? "" : "s",
            " \xB7 ",
            counts.cursors,
            " cursor",
            counts.cursors === 1 ? "" : "s",
            " catching up \xB7 default space"
          ] })
        ] }),
        /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 8 }, children: [
          /* @__PURE__ */ jsxs(Button, { variant: "ghost", size: "sm", onClick: () => overview.refresh(), children: [
            /* @__PURE__ */ jsx(RefreshIcon, { size: 12 }),
            " Refresh"
          ] }),
          /* @__PURE__ */ jsxs(Button, { variant: "primary", size: "sm", onClick: runDistillNow, loading: busy === "distill-now", children: [
            /* @__PURE__ */ jsx(SparklesIcon, { size: 12 }),
            " Distill now"
          ] })
        ] })
      ] }),
      /* @__PURE__ */ jsx(Tiny, { style: { marginTop: 6 }, children: "Distillation runs on the assigned Wiki Maintainer agent and writes only into the default space. Use the cheap path option when the agent exposes a cheap model profile." })
    ] }),
    /* @__PURE__ */ jsxs(Card, { children: [
      /* @__PURE__ */ jsx(CardHeader, { title: "Source filters" }),
      /* @__PURE__ */ jsx(CardBody, { padding: 14, children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gridTemplateColumns: isMobile ? "1fr" : "1fr 1fr", gap: 16 }, children: [
        /* @__PURE__ */ jsxs("fieldset", { style: { border: 0, padding: 0, margin: 0, display: "grid", gap: 6 }, children: [
          /* @__PURE__ */ jsx("legend", { style: { fontSize: 12, color: tokens.muted, marginBottom: 4 }, children: "Issue scope" }),
          /* @__PURE__ */ jsx(CheckboxRow, { label: "Active projects", defaultChecked: true, help: "Cursors are created for projects with recent activity." }),
          /* @__PURE__ */ jsx(CheckboxRow, { label: "Root issues marked distillable", defaultChecked: true }),
          /* @__PURE__ */ jsx(CheckboxRow, { label: "All company issues", help: "May create large source windows." }),
          /* @__PURE__ */ jsx(Tiny, { children: "These filters narrow the Paperclip source scope. The destination is always the default wiki space in Phase 1." }),
          /* @__PURE__ */ jsx(Tiny, { children: "Plugin-operation issues are always excluded to prevent feedback loops." })
        ] }),
        /* @__PURE__ */ jsxs("fieldset", { style: { border: 0, padding: 0, margin: 0, display: "grid", gap: 6 }, children: [
          /* @__PURE__ */ jsx("legend", { style: { fontSize: 12, color: tokens.muted, marginBottom: 4 }, children: "Source kinds" }),
          /* @__PURE__ */ jsx(CheckboxRow, { label: "Issue title + description", defaultChecked: true, locked: true }),
          /* @__PURE__ */ jsx(CheckboxRow, { label: "Comments (ranked, clipped)", defaultChecked: true }),
          /* @__PURE__ */ jsx(CheckboxRow, { label: "Documents (plan, spec, report)", defaultChecked: true }),
          /* @__PURE__ */ jsx(CheckboxRow, { label: "Work products / attachments", suffix: "coming soon" }),
          /* @__PURE__ */ jsx(Tiny, { children: "Heartbeats and hidden documents are never included." })
        ] })
      ] }) })
    ] }),
    /* @__PURE__ */ jsxs(Card, { children: [
      /* @__PURE__ */ jsx(CardHeader, { title: "Cursor windows" }),
      /* @__PURE__ */ jsx(CardBody, { padding: 14, children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gridTemplateColumns: isMobile ? "1fr" : "1fr 1fr", gap: 16 }, children: [
        /* @__PURE__ */ jsx(SettingField, { label: "Max source characters per project", hint: "Source bundles above this size are clipped and the page warns 'source clipped'.", children: /* @__PURE__ */ jsx(TextInput, { defaultValue: "48000" }) }),
        /* @__PURE__ */ jsx(SettingField, { label: "Min source age before processing", hint: "Debounces a hot project so a flurry of comments collapses into one cursor window.", children: /* @__PURE__ */ jsx(SelectInput, { defaultValue: "15", options: [["5", "5 min"], ["15", "15 min"], ["30", "30 min"], ["60", "1 hour"]] }) }),
        /* @__PURE__ */ jsx(SettingField, { label: "Max cursor windows per routine run", hint: "Routine runs that hit this cap split the remainder into the next routine fire.", children: /* @__PURE__ */ jsx(TextInput, { defaultValue: "6" }) }),
        /* @__PURE__ */ jsx(SettingField, { label: "Stale window threshold", hint: "After this, project pages render a 'Stale' badge until a successful run advances the cursor.", children: /* @__PURE__ */ jsx(SelectInput, { defaultValue: "72", options: [["24", "24 h"], ["48", "48 h"], ["72", "72 h"], ["168", "7 days"]] }) })
      ] }) })
    ] }),
    /* @__PURE__ */ jsxs(Card, { children: [
      /* @__PURE__ */ jsx(CardHeader, { title: "Agent execution" }),
      /* @__PURE__ */ jsx(CardBody, { padding: 14, children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gridTemplateColumns: isMobile ? "1fr" : "1fr 1fr", gap: 16 }, children: [
        /* @__PURE__ */ jsx(SettingField, { label: "Assigned maintainer", hint: "Model selection comes from the agent adapter and its runtime config. The plugin does not choose Claude/Codex/Gemini models here.", children: /* @__PURE__ */ jsx("div", { style: { minHeight: 34, display: "flex", alignItems: "center", border: `1px solid ${tokens.border}`, borderRadius: 6, padding: "6px 10px", fontSize: 13 }, children: settings.managedAgent.details ? `${settings.managedAgent.details.name} \xB7 ${adapterTypeLabel(settings.managedAgent.details.adapterType)}` : "No maintainer agent resolved" }) }),
        /* @__PURE__ */ jsx(SettingField, { label: "Cheap path", hint: "When enabled, manual distill and backfill operation issues request assigneeAdapterOverrides.modelProfile = cheap.", children: /* @__PURE__ */ jsx(
          CheckboxRow,
          {
            label: "Request the assigned agent's cheap model profile for distillation tasks",
            checked: useCheapPath,
            onChange: setUseCheapPath
          }
        ) })
      ] }) })
    ] }),
    /* @__PURE__ */ jsxs(Card, { children: [
      /* @__PURE__ */ jsx(CardHeader, { title: "Auto-apply policy" }),
      /* @__PURE__ */ jsx(CardBody, { padding: 14, children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
        /* @__PURE__ */ jsx(RadioRow, { name: "autoapply", value: "never", label: "Never \u2014 every patch goes to review-required." }),
        /* @__PURE__ */ jsx(RadioRow, { name: "autoapply", value: "status", label: "Executive-status sections only \u2014 standup, current direction, and risks." }),
        /* @__PURE__ */ jsx(RadioRow, { name: "autoapply", value: "all", label: "All sections \u2014 apply when source hash matches and confidence \u2265 0.8 (default).", defaultChecked: true }),
        /* @__PURE__ */ jsx(Tiny, { children: "Stale-hash collisions always fall through to review, regardless of policy." })
      ] }) })
    ] }),
    /* @__PURE__ */ jsxs(Card, { children: [
      /* @__PURE__ */ jsx(
        CardHeader,
        {
          title: "Backfill",
          right: /* @__PURE__ */ jsx(Button, { variant: "default", size: "sm", onClick: runBackfill, loading: busy === "backfill", children: "Queue backfill" })
        }
      ),
      /* @__PURE__ */ jsxs(CardBody, { padding: 14, children: [
        /* @__PURE__ */ jsx(Tiny, { style: { marginBottom: 6 }, children: "Backfills replay a bounded source window for a single scope so newly-enabled projects can catch up to fresh state." }),
        /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8, fontSize: 13 }, children: [
          /* @__PURE__ */ jsx(PropRow, { label: "Default scope", value: cursors.find((c) => c.projectName)?.projectName ?? cursors[0]?.scopeKey ?? "\u2014" }),
          /* @__PURE__ */ jsx(PropRow, { label: "Cursors active", value: String(counts.cursors) }),
          /* @__PURE__ */ jsx(PropRow, { label: "Runs in flight", value: String(counts.runningRuns) }),
          /* @__PURE__ */ jsx(PropRow, { label: "Failed (24h)", value: String(counts.failedRuns24h) }),
          /* @__PURE__ */ jsx(PropRow, { label: "Review queue", value: String(counts.reviewRequired) })
        ] })
      ] })
    ] }),
    /* @__PURE__ */ jsxs(Tiny, { children: [
      "Active cursors:\xA0",
      cursors.slice(0, 6).map((cursor, idx) => /* @__PURE__ */ jsxs("span", { children: [
        idx > 0 ? " \xB7 " : "",
        cursor.projectName ?? cursor.rootIssueIdentifier ?? cursor.scopeKey
      ] }, cursor.id)),
      cursors.length > 6 ? /* @__PURE__ */ jsx("span", { children: ` +${cursors.length - 6} more` }) : null
    ] })
  ] });
}
function SettingField({ label, hint, children }) {
  return /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 6, minWidth: 0 }, children: [
    /* @__PURE__ */ jsx("label", { style: { fontSize: 12, color: tokens.muted }, children: label }),
    children,
    hint ? /* @__PURE__ */ jsx(Tiny, { children: hint }) : null
  ] });
}
function CheckboxRow({
  label,
  help,
  defaultChecked,
  checked,
  onChange,
  locked,
  suffix
}) {
  return /* @__PURE__ */ jsxs("label", { style: { display: "flex", alignItems: "flex-start", gap: 8, fontSize: 13 }, children: [
    /* @__PURE__ */ jsx(
      "input",
      {
        type: "checkbox",
        defaultChecked: checked === void 0 ? defaultChecked : void 0,
        checked,
        disabled: locked,
        onChange: (event) => onChange?.(event.currentTarget.checked)
      }
    ),
    /* @__PURE__ */ jsxs("span", { children: [
      label,
      suffix ? /* @__PURE__ */ jsxs("span", { style: { marginLeft: 6, fontSize: 11, color: tokens.muted }, children: [
        "(",
        suffix,
        ")"
      ] }) : null,
      help ? /* @__PURE__ */ jsx(Tiny, { style: { display: "block", marginTop: 2 }, children: help }) : null
    ] })
  ] });
}
function RadioRow({ name, value, label, defaultChecked }) {
  return /* @__PURE__ */ jsxs("label", { style: { display: "flex", alignItems: "flex-start", gap: 8, fontSize: 13 }, children: [
    /* @__PURE__ */ jsx("input", { type: "radio", name, value, defaultChecked }),
    /* @__PURE__ */ jsx("span", { children: label })
  ] });
}
function SelectInput({ defaultValue, options }) {
  return /* @__PURE__ */ jsx(
    "select",
    {
      defaultValue,
      style: {
        background: "oklch(0.2 0 0)",
        color: tokens.fg,
        border: `1px solid ${tokens.border}`,
        borderRadius: 6,
        padding: "6px 10px",
        fontSize: 13
      },
      children: options.map(([value, label]) => /* @__PURE__ */ jsx("option", { value, children: label }, value))
    }
  );
}
function SettingsBody({ context, initialSection = "root" }) {
  const settings = useSettings(context.companyId);
  const hostNavigation = useHostNavigation();
  const { pathname } = useHostLocation();
  const isMobile = useIsMobileLayout();
  const bootstrap = usePluginAction("bootstrap-root");
  const updateEventIngestion = usePluginAction("update-event-ingestion-settings");
  const resetAgent = usePluginAction("reset-managed-agent");
  const resetProject = usePluginAction("reset-managed-project");
  const resetRoutine = usePluginAction("reset-managed-routine");
  const reconcileAgent = usePluginAction("reconcile-managed-agent");
  const reconcileProject = usePluginAction("reconcile-managed-project");
  const selectAgent = usePluginAction("select-managed-agent");
  const selectProject = usePluginAction("select-managed-project");
  const resetSkills = usePluginAction("reset-managed-skills");
  const reconcileRoutines = usePluginAction("reconcile-managed-routines");
  const updateRoutineStatus = usePluginAction("update-managed-routine-status");
  const runManagedRoutine = usePluginAction("run-managed-routine");
  const toast = usePluginToast();
  const [folderPath, setFolderPath] = useState("");
  const [folderBusy, setFolderBusy] = useState(false);
  const [agentBusy, setAgentBusy] = useState(false);
  const [projectBusy, setProjectBusy] = useState(false);
  const [selectedAgentId, setSelectedAgentId] = useState("");
  const [selectedProjectId, setSelectedProjectId] = useState("");
  const [routineBusyKey, setRoutineBusyKey] = useState(null);
  const [routineRepairBusy, setRoutineRepairBusy] = useState(false);
  const [skillBusy, setSkillBusy] = useState(false);
  const [allRepairBusy, setAllRepairBusy] = useState(false);
  const [eventPolicy, setEventPolicy] = useState(null);
  const [eventPolicyBusy, setEventPolicyBusy] = useState(false);
  const [activeSettingsSection, setActiveSettingsSection] = useState(initialSection);
  useEffect(() => {
    if (settings.data?.folder.path && !folderPath) setFolderPath(settings.data.folder.path);
  }, [settings.data?.folder.path, folderPath]);
  useEffect(() => {
    if (settings.data?.managedAgent.agentId) setSelectedAgentId(settings.data.managedAgent.agentId);
  }, [settings.data?.managedAgent.agentId]);
  useEffect(() => {
    if (settings.data?.managedProject.projectId) setSelectedProjectId(settings.data.managedProject.projectId);
  }, [settings.data?.managedProject.projectId]);
  useEffect(() => {
    if (settings.data?.eventIngestion && eventPolicy === null) setEventPolicy(settings.data.eventIngestion);
  }, [settings.data?.eventIngestion, eventPolicy]);
  useEffect(() => {
    setActiveSettingsSection(initialSection);
  }, [initialSection]);
  if (!context.companyId) return /* @__PURE__ */ jsx(Callout, { children: "Choose a company to view LLM Wiki settings." });
  if (settings.loading) return /* @__PURE__ */ jsx(Tiny, { children: "Loading settings\u2026" });
  if (settings.error) return /* @__PURE__ */ jsx(Callout, { tone: "danger", children: settings.error.message });
  if (!settings.data) return /* @__PURE__ */ jsx(Tiny, { children: "No settings available." });
  const data = settings.data;
  const maintainerFallbackAgent = data.managedAgent.agentId ? {
    id: data.managedAgent.agentId,
    name: data.managedAgent.details?.name ?? "Wiki Maintainer",
    status: data.managedAgent.details?.status ?? data.managedAgent.status,
    adapterType: data.managedAgent.details?.adapterType ?? null,
    icon: data.managedAgent.details?.icon ?? "book-open",
    urlKey: data.managedAgent.details?.urlKey ?? null
  } : null;
  const maintainerAgentOptions = maintainerFallbackAgent && !data.agentOptions.some((agent) => agent.id === maintainerFallbackAgent.id) ? [maintainerFallbackAgent, ...data.agentOptions] : data.agentOptions;
  const effectiveSelectedAgentId = selectedAgentId || data.managedAgent.agentId || "";
  const currentMaintainerAgent = maintainerAgentOptions.find((agent) => agent.id === effectiveSelectedAgentId) ?? maintainerFallbackAgent;
  const savedCustomMaintainer = data.managedAgent.source === "selected";
  const selectingDifferentMaintainer = Boolean(
    data.managedAgent.source === "managed" && data.managedAgent.agentId && effectiveSelectedAgentId && effectiveSelectedAgentId !== data.managedAgent.agentId
  );
  const showMaintainerWarning = savedCustomMaintainer || selectingDifferentMaintainer;
  const maintainerPendingApproval = currentMaintainerAgent?.status === "pending_approval" || data.managedAgent.details?.status === "pending_approval";
  const agentLink = currentMaintainerAgent?.id ? `/agents/${currentMaintainerAgent.id}` : null;
  const projectLink = data.managedProject.projectId ? `/projects/${data.managedProject.projectId}` : null;
  const managedRoutines = data.managedRoutines ?? (data.managedRoutine ? [data.managedRoutine] : []);
  const agentHealthItems = buildAgentHealthItems(data.managedAgent);
  const agentHealthWarnings = agentHealthItems.filter((item) => !item.ok);
  const routineHealthItems = buildRoutineHealthItems(managedRoutines, data.managedAgent, data.managedProject);
  const routineHealthWarnings = routineHealthItems.filter((item) => !item.ok);
  const projectHealthItems = buildProjectHealthItems(data.managedProject);
  const projectHealthWarnings = projectHealthItems.filter((item) => !item.ok);
  const managedSkills = data.managedSkills ?? [];
  const skillHealthItems = buildSkillHealthItems(managedSkills);
  const skillHealthWarnings = skillHealthItems.filter((item) => !item.ok);
  const configurationErrors = [
    ...!data.folder.healthy ? ["Wiki root folder"] : [],
    ...agentHealthWarnings.length > 0 ? ["Managed agents"] : [],
    ...skillHealthWarnings.length > 0 ? ["Managed skills"] : [],
    ...projectHealthWarnings.length > 0 ? ["Managed projects"] : [],
    ...routineHealthWarnings.length > 0 ? ["Managed routines"] : []
  ];
  const hasConfigurationErrors = configurationErrors.length > 0;
  const projectFallbackOption = data.managedProject.projectId ? {
    id: data.managedProject.projectId,
    name: data.managedProject.details?.name ?? "Current project",
    status: data.managedProject.details?.status ?? data.managedProject.status,
    color: data.managedProject.details?.color ?? null
  } : null;
  const projectOptions = projectFallbackOption && !data.projectOptions.some((project) => project.id === projectFallbackOption.id) ? [projectFallbackOption, ...data.projectOptions] : data.projectOptions;
  const effectiveSelectedProjectId = selectedProjectId || data.managedProject.projectId || "";
  const currentProjectOption = projectOptions.find((project) => project.id === effectiveSelectedProjectId) ?? projectFallbackOption;
  const currentEventPolicy = eventPolicy ?? data.eventIngestion;
  const managedRoutineItems = managedRoutines.map((routine) => {
    const fallback = routineFallbackFor(routine);
    const key = routine.resourceKey ?? routine.routineId ?? fallback.title;
    const status = managedRoutineStatus(routine);
    const assigneeAgentId = routine.routine?.assigneeAgentId ?? routine.details?.assigneeAgentId ?? null;
    return {
      key,
      title: routine.routine?.title ?? routine.details?.title ?? fallback.title,
      status: status === "missing" || status === "missing_refs" ? "paused" : status,
      routineId: routine.routineId ?? routine.routine?.id ?? null,
      href: routine.routineId ? `/routines/${routine.routineId}` : null,
      resourceKey: routine.resourceKey ?? null,
      projectId: routine.routine?.projectId ?? null,
      assigneeAgentId,
      cronExpression: routine.details?.cronExpression ?? fallback.cron,
      lastRunAt: routine.routine?.lastTriggeredAt ?? routine.details?.lastRunAt ?? null,
      managedByPluginDisplayName: routine.routine?.managedByPlugin?.pluginDisplayName ?? "LLM Wiki",
      missingRefs: routine.missingRefs?.map((ref) => ({
        resourceKind: ref.resourceKind,
        resourceKey: ref.resourceKey
      })),
      defaultDrift: routine.defaultDrift ? {
        changedFields: routine.defaultDrift.changedFields,
        defaultTitle: routine.defaultDrift.defaultTitle ?? null,
        defaultDescription: routine.defaultDrift.defaultDescription ?? null
      } : null
    };
  });
  const routineDefaultDriftItems = managedRoutineItems.filter((routine) => routine.defaultDrift?.changedFields.length);
  const agentDefaultDrift = data.managedAgent.defaultDrift;
  const activeSpaceSlug = readActiveSpaceSlugFromLocation(pathname);
  function routineBusyKeyFor(prefix) {
    const marker = `${prefix}:`;
    return routineBusyKey?.startsWith(marker) ? routineBusyKey.slice(marker.length) : null;
  }
  async function changeFolder() {
    if (!context.companyId || !folderPath.trim()) return;
    setFolderBusy(true);
    try {
      await bootstrap({ companyId: context.companyId, path: folderPath.trim() });
      toast({ tone: "success", title: "Folder updated" });
      settings.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Folder update failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setFolderBusy(false);
    }
  }
  async function chooseAgent() {
    const agentId = selectedAgentId || data.managedAgent.agentId;
    if (!context.companyId || !agentId) return;
    setAgentBusy(true);
    try {
      await selectAgent({ companyId: context.companyId, agentId });
      toast({ tone: "success", title: "Maintainer agent selected" });
      settings.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Agent selection failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setAgentBusy(false);
    }
  }
  async function chooseProject() {
    const projectId = effectiveSelectedProjectId;
    if (!context.companyId || !projectId) return;
    setProjectBusy(true);
    try {
      await selectProject({ companyId: context.companyId, projectId });
      toast({ tone: "success", title: "Project selected" });
      settings.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Project selection failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setProjectBusy(false);
    }
  }
  async function saveEventPolicy() {
    if (!context.companyId || !eventPolicy) return;
    setEventPolicyBusy(true);
    try {
      const next = await updateEventIngestion({ companyId: context.companyId, ...eventPolicy });
      setEventPolicy(next);
      toast({ tone: "success", title: "Event ingestion controls saved" });
      settings.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Could not save event controls", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setEventPolicyBusy(false);
    }
  }
  async function repairManagedRoutines() {
    if (!context.companyId) return;
    setRoutineRepairBusy(true);
    try {
      await reconcileRoutines({ companyId: context.companyId });
      toast({ tone: "success", title: "Routines fixed" });
      settings.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Routine repair failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setRoutineRepairBusy(false);
    }
  }
  async function resyncManagedSkills() {
    if (!context.companyId) return;
    setSkillBusy(true);
    try {
      await resetSkills({ companyId: context.companyId });
      toast({ tone: "success", title: "Skills synced" });
      settings.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Skill sync failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setSkillBusy(false);
    }
  }
  async function fixAllConfigurationErrors() {
    if (!context.companyId || !hasConfigurationErrors) return;
    const confirmed = typeof window === "undefined" || window.confirm(
      "Fix all detected LLM Wiki configuration errors? This may recreate missing wiki baseline files and restore plugin-managed agents, projects, routines, and skills to their current defaults."
    );
    if (!confirmed) return;
    setAllRepairBusy(true);
    try {
      if (!data.folder.healthy) {
        const path = folderPath.trim() || data.folder.path?.trim() || "";
        if (!path && !data.folder.configured) {
          throw new Error("Choose a wiki root folder path before fixing all configuration errors.");
        }
        await bootstrap(path ? { companyId: context.companyId, path } : { companyId: context.companyId });
      }
      if (skillHealthWarnings.length > 0) {
        await resetSkills({ companyId: context.companyId });
      }
      const shouldResetAgent = data.managedAgent.source !== "managed" || !managedAgentIsReady(data.managedAgent) || Boolean(data.managedAgent.defaultDrift?.changedFiles.length);
      const shouldResetProject = data.managedProject.source !== "managed" || !managedProjectIsReady(data.managedProject);
      if (shouldResetAgent) {
        await resetAgent({ companyId: context.companyId });
      } else if (routineHealthWarnings.length > 0) {
        await reconcileAgent({ companyId: context.companyId });
      }
      if (shouldResetProject) {
        await resetProject({ companyId: context.companyId });
      } else if (routineHealthWarnings.length > 0) {
        await reconcileProject({ companyId: context.companyId });
      }
      if (routineHealthWarnings.length > 0) {
        await reconcileRoutines({ companyId: context.companyId });
      }
      toast({ tone: "success", title: "Configuration errors fixed" });
      settings.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Fix all failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setAllRepairBusy(false);
    }
  }
  async function toggleManagedRoutine(routine, enabled) {
    if (!context.companyId || !routine.resourceKey) return;
    if (!enabled && !routine.assigneeAgentId) {
      toast({ tone: "warn", title: "Default agent required", body: "Set a default maintainer before enabling this routine." });
      return;
    }
    setRoutineBusyKey(`status:${routine.key}`);
    try {
      await updateRoutineStatus({ companyId: context.companyId, routineKey: routine.resourceKey, status: enabled ? "paused" : "active" });
      toast({ tone: "success", title: enabled ? "Routine paused" : "Routine enabled" });
      settings.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Routine update failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setRoutineBusyKey(null);
    }
  }
  async function runManagedRoutineNow(routine) {
    if (!context.companyId || !routine.resourceKey) return;
    const assigneeAgentId = routine.assigneeAgentId ?? data.managedAgent.agentId ?? null;
    const projectId = routine.projectId ?? data.managedProject.projectId ?? null;
    if (!assigneeAgentId) {
      toast({ tone: "warn", title: "Default agent required", body: "Set a default maintainer before running this routine." });
      return;
    }
    setRoutineBusyKey(`run:${routine.key}`);
    try {
      await runManagedRoutine({
        companyId: context.companyId,
        routineKey: routine.resourceKey,
        assigneeAgentId,
        projectId
      });
      toast({ tone: "success", title: "Routine run started" });
      settings.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Routine run failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setRoutineBusyKey(null);
    }
  }
  async function resetManagedRoutineToDefaults(routine) {
    if (!context.companyId || !routine.resourceKey) return;
    const changedFields = routine.defaultDrift?.changedFields ?? [];
    const fieldList = changedFields.length > 0 ? changedFields.join(", ") : "managed defaults";
    const confirmed = typeof window === "undefined" || window.confirm(
      `Update "${routine.title}" to the current LLM Wiki plugin defaults? This replaces ${fieldList}. Cancel to keep the current custom routine text.`
    );
    if (!confirmed) return;
    const assigneeAgentId = routine.assigneeAgentId ?? data.managedAgent.agentId ?? null;
    const projectId = routine.projectId ?? data.managedProject.projectId ?? null;
    setRoutineBusyKey(`reset:${routine.key}`);
    try {
      await resetRoutine({
        companyId: context.companyId,
        routineKey: routine.resourceKey,
        assigneeAgentId,
        projectId
      });
      toast({ tone: "success", title: "Routine defaults updated" });
      settings.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Routine reset failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setRoutineBusyKey(null);
    }
  }
  async function resetManagedAgentToDefaults() {
    if (!context.companyId) return;
    const changedFiles = agentDefaultDrift?.changedFiles ?? [];
    const fileList = changedFiles.length > 0 ? changedFiles.join(", ") : "managed instructions and defaults";
    const confirmed = typeof window === "undefined" || window.confirm(
      `Update the Wiki Maintainer to the current LLM Wiki plugin defaults? This replaces ${fileList}. Cancel to keep the current custom instructions.`
    );
    if (!confirmed) return;
    setAgentBusy(true);
    try {
      await resetAgent({ companyId: context.companyId });
      toast({ tone: "success", title: "Agent reset to plugin defaults" });
      settings.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Reset failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setAgentBusy(false);
    }
  }
  const activeSettingsConfig = SETTINGS_SECTIONS.find((section) => section.key === activeSettingsSection) ?? SETTINGS_SECTIONS[0];
  return /* @__PURE__ */ jsxs("div", { style: {
    display: "flex",
    flexDirection: isMobile ? "column" : "row",
    gap: isMobile ? 16 : 24,
    maxWidth: isMobile ? "none" : 1040,
    minWidth: 0
  }, children: [
    /* @__PURE__ */ jsx("aside", { style: {
      width: isMobile ? "auto" : 230,
      flexShrink: 0,
      borderRight: isMobile ? "none" : `1px solid ${tokens.border}`,
      borderBottom: isMobile ? `1px solid ${tokens.border}` : "none",
      paddingRight: isMobile ? 0 : 16,
      paddingBottom: isMobile ? 12 : 0
    }, children: /* @__PURE__ */ jsx("nav", { "aria-label": "LLM Wiki settings sections", style: {
      display: "flex",
      flexDirection: isMobile ? "row" : "column",
      gap: 4,
      overflowX: isMobile ? "auto" : "visible",
      paddingBottom: isMobile ? 2 : 0
    }, children: SETTINGS_SECTIONS.map((section) => /* @__PURE__ */ jsx("div", { style: { minWidth: isMobile ? 190 : 0 }, children: /* @__PURE__ */ jsx(
      SettingsSectionButton,
      {
        section,
        active: activeSettingsSection === section.key,
        onSelect: () => {
          setActiveSettingsSection(section.key);
          hostNavigation.navigate(buildSettingsSectionHref(section.key, activeSpaceSlug));
        }
      }
    ) }, section.key)) }) }),
    /* @__PURE__ */ jsxs("div", { style: { flex: 1, minWidth: 0 }, children: [
      hasConfigurationErrors ? /* @__PURE__ */ jsx("div", { style: { marginBottom: 18 }, children: /* @__PURE__ */ jsx(Callout, { tone: "warn", children: /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }, children: [
        /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 4, minWidth: 0 }, children: [
          /* @__PURE__ */ jsx("strong", { children: "configuration errors detected, fix them all?" }),
          /* @__PURE__ */ jsxs(Tiny, { children: [
            configurationErrors.join(", "),
            " ",
            configurationErrors.length === 1 ? "needs" : "need",
            " attention."
          ] })
        ] }),
        /* @__PURE__ */ jsx(Button, { size: "sm", variant: "primary", onClick: fixAllConfigurationErrors, loading: allRepairBusy, children: "Fix them all" })
      ] }) }) }) : null,
      activeSettingsSection === "root" ? /* @__PURE__ */ jsxs("section", { style: { display: "grid", gap: 22, minWidth: 0 }, children: [
        /* @__PURE__ */ jsx("h1", { style: { margin: 0, fontSize: isMobile ? 20 : 22, fontWeight: 700 }, children: "Setup" }),
        /* @__PURE__ */ jsx(SetupSection, { title: "Base Folder", children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 12 }, children: [
          /* @__PURE__ */ jsx(
            FolderPathPicker,
            {
              value: folderPath,
              onChange: setFolderPath,
              onApply: changeFolder,
              applyLabel: "Apply path",
              busy: folderBusy,
              disabled: !folderPath.trim(),
              onRefresh: () => settings.refresh()
            }
          ),
          /* @__PURE__ */ jsx(FolderHealthChecklist, { folder: data.folder }),
          data.folder.problems.length > 0 ? /* @__PURE__ */ jsxs(Callout, { tone: "warn", children: [
            data.folder.problems.length,
            " folder issue(s): ",
            data.folder.problems.map((p) => p.message).join("; ")
          ] }) : null
        ] }) }),
        /* @__PURE__ */ jsx(SetupSection, { title: "Managed Agents", separated: true, children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 14, maxWidth: isMobile ? "none" : 620, minWidth: 0 }, children: [
          /* @__PURE__ */ jsx(
            ManagedResourceHealthChecklist,
            {
              items: agentHealthItems,
              ariaLabel: "Wiki agents health checklist",
              heading: "Agent health"
            }
          ),
          agentHealthWarnings.length > 0 ? /* @__PURE__ */ jsx(Callout, { tone: "warn", children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
            /* @__PURE__ */ jsxs("div", { children: [
              agentHealthWarnings.length,
              " agent issue(s) need attention."
            ] }),
            /* @__PURE__ */ jsx("ul", { style: { margin: 0, paddingLeft: 18 }, children: agentHealthWarnings.map((item) => /* @__PURE__ */ jsx("li", { children: item.detail }, item.label)) })
          ] }) }) : null,
          /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
            /* @__PURE__ */ jsx("label", { style: { fontSize: 12, color: tokens.muted }, children: "Maintainer" }),
            /* @__PURE__ */ jsx(
              "fieldset",
              {
                disabled: agentBusy,
                style: { border: 0, margin: 0, minWidth: 0, padding: 0 },
                children: /* @__PURE__ */ jsx(
                  AssigneePicker,
                  {
                    companyId: context.companyId,
                    value: effectiveSelectedAgentId ? `agent:${effectiveSelectedAgentId}` : "",
                    includeUsers: false,
                    placeholder: "Select maintainer",
                    noneLabel: "No maintainer",
                    searchPlaceholder: "Search agents...",
                    emptyMessage: "No agents found.",
                    onChange: (_value, selection) => {
                      setSelectedAgentId(selection.assigneeAgentId ?? "");
                    }
                  }
                )
              }
            ),
            /* @__PURE__ */ jsxs(Tiny, { children: [
              "Adapter: ",
              adapterTypeLabel(currentMaintainerAgent?.adapterType ?? data.managedAgent.details?.adapterType ?? null)
            ] }),
            maintainerPendingApproval ? /* @__PURE__ */ jsx(Callout, { tone: "warn", children: "The Wiki Maintainer is pending approval. Approve the agent before relying on wiki ingest, query, lint, or scheduled maintenance tasks." }) : null,
            showMaintainerWarning ? /* @__PURE__ */ jsx(Callout, { tone: "warn", children: "This is not the Paperclip-provided Wiki Maintainer. Plugin operations and routines may miss the recommended wiki role, tools, and default instructions." }) : null,
            agentDefaultDrift?.changedFiles.length ? /* @__PURE__ */ jsxs(Callout, { tone: "warn", children: [
              "Wiki Maintainer instruction defaults changed: ",
              agentDefaultDrift.changedFiles.join(", "),
              ". Reset only if you want to replace current custom instructions with the plugin template."
            ] }) : null,
            /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 8, flexWrap: "wrap" }, children: [
              /* @__PURE__ */ jsx(
                Button,
                {
                  size: "sm",
                  variant: "primary",
                  onClick: chooseAgent,
                  loading: agentBusy,
                  disabled: !effectiveSelectedAgentId || effectiveSelectedAgentId === data.managedAgent.agentId,
                  children: "Save maintainer"
                }
              ),
              agentLink ? /* @__PURE__ */ jsx(Button, { size: "sm", onClick: () => hostNavigation.navigate(agentLink), children: "Open agent \u2197" }) : null,
              /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: async () => {
                if (!context.companyId) return;
                setAgentBusy(true);
                try {
                  await reconcileAgent({ companyId: context.companyId });
                  toast({ tone: "success", title: "Agent reconciled" });
                  settings.refresh();
                } catch (err) {
                  toast({ tone: "error", title: "Reconcile failed", body: err instanceof Error ? err.message : String(err) });
                } finally {
                  setAgentBusy(false);
                }
              }, loading: agentBusy, children: "Repair" }),
              /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: resetManagedAgentToDefaults, loading: agentBusy, children: "Reset to defaults" })
            ] })
          ] })
        ] }) }),
        /* @__PURE__ */ jsx(SetupSection, { title: "Managed Skills", separated: true, children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 12, maxWidth: isMobile ? "none" : 620, minWidth: 0 }, children: [
          /* @__PURE__ */ jsx(SkillHealthChecklist, { items: skillHealthItems }),
          skillHealthWarnings.length > 0 ? /* @__PURE__ */ jsx(Callout, { tone: "warn", children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
            /* @__PURE__ */ jsxs("div", { children: [
              skillHealthWarnings.length,
              " skill issue(s) need attention."
            ] }),
            /* @__PURE__ */ jsx("ul", { style: { margin: 0, paddingLeft: 18 }, children: skillHealthWarnings.map((item) => /* @__PURE__ */ jsx("li", { children: item.detail }, item.label)) }),
            /* @__PURE__ */ jsx("div", { children: /* @__PURE__ */ jsx(Button, { size: "sm", variant: "primary", onClick: resyncManagedSkills, loading: skillBusy, children: "Re-sync skills" }) })
          ] }) }) : /* @__PURE__ */ jsx(Callout, { children: /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }, children: [
            /* @__PURE__ */ jsx("span", { children: "LLM Wiki skills are installed in the company skill library." }),
            /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: resyncManagedSkills, loading: skillBusy, children: "Re-sync skills" })
          ] }) })
        ] }) }),
        /* @__PURE__ */ jsx(SetupSection, { title: "Managed Projects", separated: true, children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 10, maxWidth: isMobile ? "none" : 620, minWidth: 0 }, children: [
          /* @__PURE__ */ jsx(
            ManagedResourceHealthChecklist,
            {
              items: projectHealthItems,
              ariaLabel: "Wiki projects health checklist",
              heading: "Project health"
            }
          ),
          projectHealthWarnings.length > 0 ? /* @__PURE__ */ jsx(Callout, { tone: "warn", children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
            /* @__PURE__ */ jsxs("div", { children: [
              projectHealthWarnings.length,
              " project issue(s) need attention."
            ] }),
            /* @__PURE__ */ jsx("ul", { style: { margin: 0, paddingLeft: 18 }, children: projectHealthWarnings.map((item) => /* @__PURE__ */ jsx("li", { children: item.detail }, item.label)) })
          ] }) }) : null,
          /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
            /* @__PURE__ */ jsx("label", { style: { fontSize: 12, color: tokens.muted }, children: "Use existing project" }),
            /* @__PURE__ */ jsx(
              "fieldset",
              {
                disabled: projectBusy,
                style: { border: 0, margin: 0, minWidth: 0, padding: 0 },
                children: /* @__PURE__ */ jsx(
                  ProjectPicker,
                  {
                    companyId: context.companyId,
                    value: effectiveSelectedProjectId,
                    includeArchived: true,
                    placeholder: "Project",
                    noneLabel: "No project",
                    searchPlaceholder: "Search projects...",
                    emptyMessage: "No projects found.",
                    onChange: setSelectedProjectId
                  }
                )
              }
            ),
            /* @__PURE__ */ jsxs(Tiny, { children: [
              "Status: ",
              projectStatusLabel(currentProjectOption?.status ?? data.managedProject.details?.status ?? data.managedProject.status)
            ] }),
            /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 8, flexWrap: "wrap" }, children: [
              /* @__PURE__ */ jsx(Button, { size: "sm", variant: "primary", onClick: chooseProject, loading: projectBusy, disabled: !effectiveSelectedProjectId, children: "Save project" }),
              projectLink ? /* @__PURE__ */ jsx(Button, { size: "sm", onClick: () => hostNavigation.navigate(projectLink), children: "Open project \u2197" }) : null,
              /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: async () => {
                if (!context.companyId) return;
                setProjectBusy(true);
                try {
                  await reconcileProject({ companyId: context.companyId });
                  toast({ tone: "success", title: "Project reconciled" });
                  settings.refresh();
                } catch (err) {
                  toast({ tone: "error", title: "Reconcile failed", body: err instanceof Error ? err.message : String(err) });
                } finally {
                  setProjectBusy(false);
                }
              }, loading: projectBusy, children: "Repair / reconcile" }),
              /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: async () => {
                if (!context.companyId) return;
                setProjectBusy(true);
                try {
                  await resetProject({ companyId: context.companyId });
                  toast({ tone: "success", title: "Project reset to plugin defaults" });
                  settings.refresh();
                } catch (err) {
                  toast({ tone: "error", title: "Reset failed", body: err instanceof Error ? err.message : String(err) });
                } finally {
                  setProjectBusy(false);
                }
              }, loading: projectBusy, children: "\u21BA Reset to plugin defaults" })
            ] })
          ] })
        ] }) }),
        /* @__PURE__ */ jsx(SetupSection, { title: "Managed Routines", separated: true, children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 12, maxWidth: isMobile ? "none" : 620, minWidth: 0 }, children: [
          /* @__PURE__ */ jsx(RoutineHealthChecklist, { items: routineHealthItems }),
          routineHealthWarnings.length > 0 ? /* @__PURE__ */ jsx(Callout, { tone: "warn", children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
            /* @__PURE__ */ jsxs("div", { children: [
              routineHealthWarnings.length,
              " routine issue(s) need attention."
            ] }),
            /* @__PURE__ */ jsx("ul", { style: { margin: 0, paddingLeft: 18 }, children: routineHealthWarnings.map((item) => /* @__PURE__ */ jsx("li", { children: item.detail }, item.label)) }),
            /* @__PURE__ */ jsx("div", { children: /* @__PURE__ */ jsx(Button, { size: "sm", variant: "primary", onClick: repairManagedRoutines, loading: routineRepairBusy, children: "Fix routines" }) })
          ] }) }) : routineDefaultDriftItems.length > 0 ? /* @__PURE__ */ jsx(Callout, { tone: "warn", children: /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }, children: [
            /* @__PURE__ */ jsxs("span", { children: [
              routineDefaultDriftItems.length,
              " routine default update",
              routineDefaultDriftItems.length === 1 ? "" : "s",
              " available."
            ] }),
            /* @__PURE__ */ jsx(Button, { size: "sm", variant: "primary", onClick: () => {
              setActiveSettingsSection("routines");
              hostNavigation.navigate(buildSettingsSectionHref("routines", activeSpaceSlug));
            }, children: "Review defaults" })
          ] }) }) : /* @__PURE__ */ jsx(Tiny, { children: "Managed routines are installed with the Wiki Maintainer and LLM Wiki project." })
        ] }) })
      ] }) : activeSettingsSection === "distillation" ? /* @__PURE__ */ jsx(
        SettingsPanel,
        {
          title: "Distillation",
          badge: /* @__PURE__ */ jsx(Badge, { tone: "default", children: "Default space only" }),
          description: "Read Paperclip issues, comments, and documents for this company and write project pages into the default wiki space. Assets/attachments and work products stay metadata-only in Phase 5 and are excluded from source-text extraction. Other spaces cannot be selected as a destination yet - that lands with per-space Paperclip ingestion profiles.",
          children: /* @__PURE__ */ jsx(DistillationSettingsPanel, { context, settings: data })
        }
      ) : activeSettingsSection === "routines" ? /* @__PURE__ */ jsx(SettingsPanel, { title: "Managed Routines", description: activeSettingsConfig.description, children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 12 }, children: [
        routineDefaultDriftItems.length > 0 ? /* @__PURE__ */ jsx(Callout, { tone: "warn", children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 6 }, children: [
          /* @__PURE__ */ jsx("strong", { children: "Routine defaults changed." }),
          /* @__PURE__ */ jsx("span", { children: "Review rows marked with changed defaults. Reset a row to update it to the current LLM Wiki instructions, or leave it unchanged to keep custom routine text." })
        ] }) }) : null,
        routineHealthWarnings.length > 0 ? /* @__PURE__ */ jsx(Callout, { tone: "warn", children: /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }, children: [
          /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 6, minWidth: 0 }, children: [
            /* @__PURE__ */ jsx("strong", { children: "Routine setup needs repair." }),
            /* @__PURE__ */ jsx("ul", { style: { margin: 0, paddingLeft: 18 }, children: routineHealthWarnings.map((item) => /* @__PURE__ */ jsx("li", { children: item.detail }, item.label)) })
          ] }),
          /* @__PURE__ */ jsx(Button, { size: "sm", variant: "primary", onClick: repairManagedRoutines, loading: routineRepairBusy, children: "Fix routines" })
        ] }) }) : /* @__PURE__ */ jsx(Callout, { children: /* @__PURE__ */ jsxs("div", { style: { display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }, children: [
          /* @__PURE__ */ jsx("span", { children: "Managed routines are installed with the Wiki Maintainer and LLM Wiki project." }),
          /* @__PURE__ */ jsx(Button, { size: "sm", disabled: true, children: "Routines valid" })
        ] }) }),
        /* @__PURE__ */ jsx(
          PluginManagedRoutinesList,
          {
            routines: managedRoutineItems,
            agents: maintainerAgentOptions,
            projects: projectOptions,
            pluginDisplayName: "LLM Wiki",
            runningRoutineKey: routineBusyKeyFor("run"),
            statusMutationRoutineKey: routineBusyKeyFor("status"),
            resettingRoutineKey: routineBusyKeyFor("reset"),
            onRunNow: runManagedRoutineNow,
            onToggleEnabled: toggleManagedRoutine,
            onReset: resetManagedRoutineToDefaults
          }
        )
      ] }) }) : activeSettingsSection === "lint" ? /* @__PURE__ */ jsx(SettingsLintPanel, { context }) : activeSettingsSection === "spaces" ? /* @__PURE__ */ jsx(SpacesSettingsPanel, { context, description: activeSettingsConfig.description }) : activeSettingsSection === "events" ? /* @__PURE__ */ jsxs(
        SettingsPanel,
        {
          title: "Paperclip event ingestion",
          badge: /* @__PURE__ */ jsx(Badge, { tone: currentEventPolicy.enabled ? "running" : "default", children: currentEventPolicy.enabled ? "enabled" : "off by default" }),
          description: activeSettingsConfig.description,
          children: [
            /* @__PURE__ */ jsx(Tiny, { style: { marginBottom: 10 }, children: "Company-scoped Paperclip events can advance default-space cursors. Enable only the first-party text sources this wiki should observe for default-space distillation." }),
            /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 10 }, children: [
              /* @__PURE__ */ jsxs("label", { style: { display: "flex", gap: 8, alignItems: "center", fontSize: 13 }, children: [
                /* @__PURE__ */ jsx(
                  "input",
                  {
                    type: "checkbox",
                    checked: currentEventPolicy.enabled,
                    onChange: (event) => setEventPolicy({ ...currentEventPolicy, enabled: event.currentTarget.checked })
                  }
                ),
                "Enable event ingestion for this company"
              ] }),
              /* @__PURE__ */ jsx("div", { style: { display: "grid", gap: 8, paddingLeft: isMobile ? 0 : 22 }, children: [
                ["issues", "Issues", "Capture issue title and description when issue events fire."],
                ["comments", "Comments", "Capture comment body when comment-created events fire."],
                ["documents", "Documents", "Capture document body when document-created or document-updated events fire."]
              ].map(([key, label, help]) => /* @__PURE__ */ jsxs("label", { style: { display: "grid", gridTemplateColumns: "auto 1fr", columnGap: 8, rowGap: 2, alignItems: "start", fontSize: 13 }, children: [
                /* @__PURE__ */ jsx(
                  "input",
                  {
                    type: "checkbox",
                    checked: currentEventPolicy.sources[key],
                    onChange: (event) => setEventPolicy({
                      ...currentEventPolicy,
                      sources: { ...currentEventPolicy.sources, [key]: event.currentTarget.checked }
                    })
                  }
                ),
                /* @__PURE__ */ jsxs("span", { children: [
                  label,
                  /* @__PURE__ */ jsx(Tiny, { style: { display: "block" }, children: help })
                ] })
              ] }, key)) }),
              /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 6, maxWidth: isMobile ? "none" : 220 }, children: [
                /* @__PURE__ */ jsx("label", { style: { fontSize: 12, color: tokens.muted }, children: "Max characters per captured event" }),
                /* @__PURE__ */ jsx(
                  TextInput,
                  {
                    value: String(currentEventPolicy.maxCharacters),
                    onChange: (event) => {
                      const parsed = Number(event.currentTarget.value);
                      setEventPolicy({ ...currentEventPolicy, maxCharacters: Number.isFinite(parsed) ? parsed : currentEventPolicy.maxCharacters });
                    }
                  }
                )
              ] }),
              /* @__PURE__ */ jsx(Callout, { tone: "warn", children: "Event ingestion records selected Paperclip issue, comment, and document activity for the default wiki space. Assets/attachments and work products are excluded here: Phase 5 allows metadata-only references later, not blob reads or linked-content fetches. It never reads across companies or creates non-default space cursors." }),
              /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 8, flexWrap: "wrap" }, children: [
                /* @__PURE__ */ jsx(Button, { size: "sm", variant: "primary", onClick: saveEventPolicy, loading: eventPolicyBusy, children: "Save controls" }),
                /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: () => setEventPolicy(data.eventIngestion), children: "Revert" })
              ] })
            ] })
          ]
        }
      ) : null
    ] })
  ] });
}
function SpacesSettingsPanel({ context, description }) {
  const { pathname } = useHostLocation();
  const hostNavigation = useHostNavigation();
  const activeSpaceSlug = useMemo(() => readActiveSpaceSlugFromLocation(pathname), [pathname]);
  const editingSlug = useMemo(() => readSettingsSpaceSlugFromLocation(pathname), [pathname]);
  const isMobile = useIsMobileLayout();
  const spacesQuery = useSpaces(context.companyId);
  const spaces = useMemo(() => {
    const list = spacesQuery.data?.spaces ?? [];
    return activeWikiSpaces(list).sort(compareSpaces);
  }, [spacesQuery.data]);
  const [createOpen, setCreateOpen] = useState(false);
  const focusedSpace = useMemo(() => {
    if (editingSlug) return spaces.find((s) => s.slug === editingSlug) ?? null;
    return spaces.find((s) => s.slug === activeSpaceSlug) ?? spaces.find((s) => s.slug === DEFAULT_SPACE_SLUG) ?? spaces[0] ?? null;
  }, [editingSlug, spaces, activeSpaceSlug]);
  return /* @__PURE__ */ jsxs(SettingsPanel, { title: "Shared spaces", description, children: [
    /* @__PURE__ */ jsxs("div", { style: { display: "grid", gridTemplateColumns: isMobile ? "1fr" : "220px 1fr", gap: 18, alignItems: "flex-start" }, children: [
      /* @__PURE__ */ jsxs("aside", { style: { display: "grid", gap: 4, minWidth: 0 }, children: [
        spaces.map((space) => {
          const active = focusedSpace?.slug === space.slug;
          return /* @__PURE__ */ jsxs(
            "button",
            {
              type: "button",
              onClick: () => hostNavigation.navigate(buildSettingsSectionHref("spaces", activeSpaceSlug, space.slug)),
              style: {
                textAlign: "left",
                background: active ? tokens.accent : "transparent",
                border: `1px solid ${active ? tokens.border : "transparent"}`,
                borderRadius: 6,
                padding: "8px 10px",
                color: tokens.fg,
                cursor: "pointer",
                fontFamily: fontStack,
                display: "grid",
                gap: 2
              },
              children: [
                /* @__PURE__ */ jsx("span", { style: { fontSize: 13, fontWeight: 600 }, children: space.displayName }),
                /* @__PURE__ */ jsx("span", { style: { fontSize: 11, color: tokens.muted, fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace" }, children: space.slug })
              ]
            },
            space.slug
          );
        }),
        /* @__PURE__ */ jsxs(
          "button",
          {
            type: "button",
            onClick: () => setCreateOpen(true),
            style: {
              textAlign: "left",
              background: "transparent",
              border: `1px dashed ${tokens.border}`,
              borderRadius: 6,
              padding: "8px 10px",
              color: "oklch(0.78 0.13 250)",
              cursor: "pointer",
              fontFamily: fontStack,
              display: "flex",
              alignItems: "center",
              gap: 8,
              fontSize: 13,
              fontWeight: 600,
              marginTop: 6
            },
            children: [
              /* @__PURE__ */ jsx(PlusIcon, { size: 14 }),
              "Add space\u2026"
            ]
          }
        )
      ] }),
      /* @__PURE__ */ jsxs("div", { style: { minWidth: 0, display: "grid", gap: 14 }, children: [
        spacesQuery.loading && spaces.length === 0 ? /* @__PURE__ */ jsx(Tiny, { children: "Loading spaces\u2026" }) : null,
        spacesQuery.error ? /* @__PURE__ */ jsxs(Callout, { tone: "danger", children: [
          "Failed to load spaces: ",
          spacesQuery.error.message
        ] }) : null,
        focusedSpace ? /* @__PURE__ */ jsx(
          SpaceEditCard,
          {
            space: focusedSpace,
            companyId: context.companyId,
            isOnlySpace: spaces.length === 1,
            refresh: spacesQuery.refresh,
            onArchived: () => {
              hostNavigation.navigate(buildSettingsSectionHref("spaces", activeSpaceSlug));
            }
          }
        ) : /* @__PURE__ */ jsx(Callout, { children: "Pick a space from the list, or create one with the \u201CAdd space\u2026\u201D button." })
      ] })
    ] }),
    createOpen && context.companyId ? /* @__PURE__ */ jsx(
      CreateSpaceModal,
      {
        companyId: context.companyId,
        existingSlugs: new Set(spaces.map((s) => s.slug)),
        onClose: () => setCreateOpen(false),
        onCreated: (space) => {
          setCreateOpen(false);
          spacesQuery.refresh();
          hostNavigation.navigate(buildSettingsSectionHref("spaces", activeSpaceSlug, space.slug));
        }
      }
    ) : null
  ] });
}
function SpaceEditCard({
  space,
  companyId,
  isOnlySpace,
  refresh,
  onArchived
}) {
  const updateSpace = usePluginAction("update-space");
  const archiveSpace = usePluginAction("archive-space");
  const bootstrapSpace = usePluginAction("bootstrap-space");
  const folderStatusQuery = useSpaceFolderStatus(companyId, space.slug);
  const toast = usePluginToast();
  const isDefault = space.slug === DEFAULT_SPACE_SLUG;
  const [displayName, setDisplayName] = useState(space.displayName);
  const [busy, setBusy] = useState(false);
  const [folderBusy, setFolderBusy] = useState(false);
  const [archiveBusy, setArchiveBusy] = useState(false);
  useEffect(() => {
    setDisplayName(space.displayName);
  }, [space.slug, space.displayName]);
  const folder = folderStatusQuery.data?.folder ?? null;
  const relativeRoot = folderStatusQuery.data?.relativeRoot ?? "";
  const settingsRecord = space.settings ?? {};
  const folderModeLabel = space.folderMode === "managed_subfolder" ? "New managed folder (under wiki root)" : space.folderMode === "existing_local_folder" ? "Existing folder under wiki root" : space.folderMode;
  async function saveName() {
    if (!companyId || displayName.trim().length === 0 || displayName.trim() === space.displayName) return;
    setBusy(true);
    try {
      await updateSpace({ companyId, spaceSlug: space.slug, displayName: displayName.trim() });
      toast({ tone: "success", title: "Display name updated" });
      refresh();
    } catch (err) {
      toast({ tone: "error", title: "Update failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setBusy(false);
    }
  }
  async function recreateBaseline() {
    if (!companyId || folderBusy) return;
    setFolderBusy(true);
    try {
      await bootstrapSpace({ companyId, spaceSlug: space.slug });
      toast({ tone: "success", title: "Baseline restored", body: `Re-created the standard skeleton for ${space.displayName}.` });
      folderStatusQuery.refresh();
    } catch (err) {
      toast({ tone: "error", title: "Bootstrap failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setFolderBusy(false);
    }
  }
  async function archive() {
    if (!companyId || isDefault || isOnlySpace || archiveBusy) return;
    if (typeof window !== "undefined" && !window.confirm(`Archive ${space.displayName}? Pages stay on disk; you can restore later through the plugin API or by un-archiving from the database.`)) {
      return;
    }
    setArchiveBusy(true);
    try {
      await archiveSpace({ companyId, spaceSlug: space.slug });
      toast({ tone: "success", title: "Space archived", body: `${space.displayName} hidden from the sidebar.` });
      refresh();
      onArchived();
    } catch (err) {
      toast({ tone: "error", title: "Archive failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setArchiveBusy(false);
    }
  }
  return /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 12 }, children: [
    /* @__PURE__ */ jsxs(Card, { children: [
      /* @__PURE__ */ jsx(
        CardHeader,
        {
          title: /* @__PURE__ */ jsxs("span", { style: { display: "flex", alignItems: "center", gap: 8 }, children: [
            /* @__PURE__ */ jsx(FolderIcon, { size: 16 }),
            /* @__PURE__ */ jsx("span", { children: space.displayName }),
            /* @__PURE__ */ jsx(Badge, { tone: "default", style: { fontSize: 10 }, children: space.slug }),
            /* @__PURE__ */ jsx(Badge, { tone: space.status === "active" ? "running" : "default", style: { fontSize: 10 }, children: space.status })
          ] }),
          right: /* @__PURE__ */ jsxs("span", { style: { fontSize: 11, color: tokens.muted }, children: [
            space.spaceType,
            " \xB7 ",
            space.accessScope
          ] })
        }
      ),
      /* @__PURE__ */ jsx(CardBody, { children: /* @__PURE__ */ jsxs(Tiny, { children: [
        "Stored under ",
        /* @__PURE__ */ jsx(Mono, { children: relativeRoot || (isDefault ? "(wiki root)" : `spaces/${space.slug}/`) }),
        " within the configured wiki root."
      ] }) })
    ] }),
    /* @__PURE__ */ jsxs(Card, { children: [
      /* @__PURE__ */ jsx(CardHeader, { title: "Identity" }),
      /* @__PURE__ */ jsx(CardBody, { children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 12 }, children: [
        /* @__PURE__ */ jsx(FormField, { label: "Display name", children: /* @__PURE__ */ jsx(TextInput, { value: displayName, onChange: (event) => setDisplayName(event.target.value), maxLength: 120 }) }),
        /* @__PURE__ */ jsx(FormField, { label: "Slug", help: "Slug is locked once a space has indexed pages. Contact platform team to migrate.", children: /* @__PURE__ */ jsx(TextInput, { value: space.slug, disabled: true, style: { fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace", opacity: 0.7 } }) }),
        /* @__PURE__ */ jsx(FormField, { label: "Type", help: "(Cloud connectors coming soon)", children: /* @__PURE__ */ jsx(TextInput, { value: space.spaceType === "managed" ? "Folder" : space.spaceType, disabled: true, style: { opacity: 0.7 } }) }),
        /* @__PURE__ */ jsx("div", { children: /* @__PURE__ */ jsx(Button, { variant: "primary", size: "sm", onClick: saveName, disabled: busy || displayName.trim() === space.displayName, loading: busy, children: "Save name" }) })
      ] }) })
    ] }),
    /* @__PURE__ */ jsxs(Card, { children: [
      /* @__PURE__ */ jsx(CardHeader, { title: "Folder source & health" }),
      /* @__PURE__ */ jsx(CardBody, { children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 12 }, children: [
        /* @__PURE__ */ jsx(FormField, { label: "Mode", children: /* @__PURE__ */ jsx(TextInput, { value: folderModeLabel, disabled: true, style: { opacity: 0.7 } }) }),
        /* @__PURE__ */ jsx(FormField, { label: "Path", children: /* @__PURE__ */ jsx(TextInput, { value: folder?.path ?? folder?.realPath ?? relativeRoot ?? "(unconfigured)", disabled: true, style: { fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace", opacity: 0.7 } }) }),
        folderStatusQuery.loading ? /* @__PURE__ */ jsx(Tiny, { children: "Loading folder status\u2026" }) : null,
        folderStatusQuery.error ? /* @__PURE__ */ jsx(Callout, { tone: "danger", children: folderStatusQuery.error.message }) : null,
        folder ? /* @__PURE__ */ jsx(SpaceFolderHealthChecklist, { folder }) : null,
        /* @__PURE__ */ jsx("div", { children: /* @__PURE__ */ jsx(Button, { size: "sm", onClick: recreateBaseline, loading: folderBusy, children: "Recreate baseline" }) })
      ] }) })
    ] }),
    /* @__PURE__ */ jsx(PaperclipIngestionSpaceCard, { companyId, space, refresh }),
    /* @__PURE__ */ jsxs(Card, { style: { opacity: 0.56 }, children: [
      /* @__PURE__ */ jsx(CardHeader, { title: "Access" }),
      /* @__PURE__ */ jsx(CardBody, { children: /* @__PURE__ */ jsxs("div", { "aria-disabled": "true", style: { display: "grid", gap: 10 }, children: [
        /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 6, flexWrap: "wrap" }, children: [
          /* @__PURE__ */ jsx(Badge, { tone: "default", style: { fontSize: 10 }, children: space.accessScope }),
          /* @__PURE__ */ jsx(Badge, { tone: "default", style: { fontSize: 10 }, children: "Coming soon" })
        ] }),
        /* @__PURE__ */ jsxs(Tiny, { children: [
          "Access scope is stored as metadata only. ",
          /* @__PURE__ */ jsx(Mono, { children: "shared" }),
          ", ",
          /* @__PURE__ */ jsx(Mono, { children: "team" }),
          ", and",
          " ",
          /* @__PURE__ */ jsx(Mono, { children: "personal" }),
          " are saved on the space record but do not currently enforce read/write permissions, and they do not change which Paperclip sources reach this space."
        ] }),
        /* @__PURE__ */ jsx(FormField, { label: "Owner user id", children: /* @__PURE__ */ jsx(TextInput, { value: settingsRecord.ownerUserHint ?? space.ownerUserId ?? "", disabled: true, style: { fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace" } }) }),
        /* @__PURE__ */ jsx(FormField, { label: "Owner team key", children: /* @__PURE__ */ jsx(TextInput, { value: space.teamKey ?? "", disabled: true, style: { fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace" } }) })
      ] }) })
    ] }),
    /* @__PURE__ */ jsxs(Card, { style: { borderColor: "oklch(0.5 0.18 25)" }, children: [
      /* @__PURE__ */ jsx(
        CardHeader,
        {
          title: /* @__PURE__ */ jsx("span", { style: { color: "oklch(0.78 0.18 25)" }, children: "Danger zone" })
        }
      ),
      /* @__PURE__ */ jsx(CardBody, { children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
        /* @__PURE__ */ jsx(Tiny, { children: isDefault ? "The default space cannot be archived because new operations and tools fall back to it." : isOnlySpace ? "This is the only space in the company. Create another before archiving this one." : "Archiving hides the space from the sidebar and pauses scheduled lint/index. Pages remain on disk." }),
        /* @__PURE__ */ jsx("div", { children: /* @__PURE__ */ jsx(
          Button,
          {
            variant: "destructive",
            size: "sm",
            onClick: archive,
            disabled: isDefault || isOnlySpace || archiveBusy,
            loading: archiveBusy,
            title: isDefault ? "Default space cannot be archived" : isOnlySpace ? "At least one space must remain" : void 0,
            children: "Archive this space"
          }
        ) })
      ] }) })
    ] })
  ] });
}
function paperclipIngestionStateBadge(data) {
  if (!data) return { tone: "default", label: "Loading" };
  if (data.effectiveState === "policy_blocked") return { tone: "blocked", label: "Locked" };
  if (data.effectiveState === "pending_approval") return { tone: "queued", label: "Pending approval" };
  if (data.effectiveState === "enabled_no_scopes") return { tone: "failed", label: "Misconfigured" };
  if (data.effectiveState === "enabled") return { tone: "running", label: `On \xB7 ${data.profile.sourceScopes.length} source${data.profile.sourceScopes.length === 1 ? "" : "s"}` };
  return { tone: "default", label: data.historicalPageCount > 0 ? `Off \xB7 ${data.historicalPageCount} historical pages` : "Off" };
}
function PaperclipIngestionSpaceCard({ companyId, space, refresh }) {
  const profileQuery = usePaperclipIngestionProfile(companyId, space.slug);
  const updateProfile = usePluginAction("update-paperclip-ingestion-profile");
  const toast = usePluginToast();
  const data = profileQuery.data ?? null;
  const [draft, setDraft] = useState(null);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    setDraft(data?.profile ?? null);
  }, [data?.space.slug, data?.profile]);
  const badge = paperclipIngestionStateBadge(data);
  const locked = data?.effectiveState === "policy_blocked";
  const sourceScope = draft?.sourceScopes[0];
  const activeProjectLimit = sourceScope?.kind === "active_projects" ? sourceScope.limit : 3;
  const canSave = Boolean(companyId && draft && !busy && !locked);
  const emptyScopes = Boolean(draft?.enabled && draft.sourceScopes.length === 0);
  function patchDraft(patch) {
    setDraft((current) => current ? { ...current, ...patch } : current);
  }
  function setSourceKind(key, value) {
    setDraft((current) => current ? { ...current, sourceKinds: { ...current.sourceKinds, [key]: value } } : current);
  }
  function setActiveProjectsLimit(value) {
    setDraft((current) => current ? {
      ...current,
      sourceScopes: [{ kind: "active_projects", limit: Math.max(1, Math.floor(value || 1)) }]
    } : current);
  }
  async function save() {
    if (!companyId || !draft || locked) return;
    setBusy(true);
    try {
      await updateProfile({ companyId, spaceSlug: space.slug, profile: draft });
      toast({ tone: "success", title: "Paperclip ingestion profile saved", body: `${space.displayName} will use the selected Paperclip sources.` });
      profileQuery.refresh();
      refresh();
    } catch (err) {
      toast({ tone: "error", title: "Profile save failed", body: err instanceof Error ? err.message : String(err) });
    } finally {
      setBusy(false);
    }
  }
  return /* @__PURE__ */ jsxs(Card, { children: [
    /* @__PURE__ */ jsx(
      CardHeader,
      {
        title: /* @__PURE__ */ jsxs("span", { children: [
          "Paperclip \u2192 ",
          space.displayName
        ] }),
        right: /* @__PURE__ */ jsx(Badge, { tone: badge.tone, style: { fontSize: 10 }, children: badge.label })
      }
    ),
    /* @__PURE__ */ jsx(CardBody, { children: /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 12 }, children: [
      profileQuery.loading && !data ? /* @__PURE__ */ jsx(Tiny, { children: "Loading Paperclip ingestion profile\u2026" }) : null,
      profileQuery.error ? /* @__PURE__ */ jsx(Callout, { tone: "danger", children: profileQuery.error.message }) : null,
      locked ? /* @__PURE__ */ jsx(Callout, { tone: "warn", children: "Locked \u2014 host permissions pending. Paperclip ingestion stays disabled on team and personal spaces until LLM Wiki enforces read/write permissions for non-shared spaces." }) : null,
      data && data.historicalPageCount > 0 && data.effectiveState === "disabled" ? /* @__PURE__ */ jsxs(Callout, { children: [
        "Off \xB7 ",
        data.historicalPageCount,
        " historical Paperclip page",
        data.historicalPageCount === 1 ? "" : "s",
        " still in this space. Disabling stops new observations but does not delete prior wiki pages."
      ] }) : null,
      data && data.overlapCount > 0 ? /* @__PURE__ */ jsxs(Callout, { children: [
        data.overlapCount,
        " source overlap",
        data.overlapCount === 1 ? "" : "s",
        " with another enabled space. Duplicate destinations are allowed, but they are explicit."
      ] }) : null,
      emptyScopes ? /* @__PURE__ */ jsx(Callout, { tone: "warn", children: "Pick at least one source scope before saving." }) : null,
      draft ? /* @__PURE__ */ jsxs(Fragment, { children: [
        /* @__PURE__ */ jsxs("label", { style: { display: "flex", gap: 8, alignItems: "flex-start", fontSize: 13 }, children: [
          /* @__PURE__ */ jsx(
            "input",
            {
              type: "checkbox",
              checked: draft.enabled,
              disabled: locked || busy,
              onChange: (event) => patchDraft({
                enabled: event.currentTarget.checked,
                sourceScopes: event.currentTarget.checked && draft.sourceScopes.length === 0 ? [{ kind: "active_projects", limit: activeProjectLimit }] : draft.sourceScopes
              })
            }
          ),
          /* @__PURE__ */ jsxs("span", { children: [
            "Enable Paperclip ingestion for this destination space",
            /* @__PURE__ */ jsxs(Tiny, { style: { display: "block" }, children: [
              "Future Paperclip issue, comment, and document events can advance cursors in ",
              space.displayName,
              ". Existing pages are preserved when this is turned off."
            ] })
          ] })
        ] }),
        /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
          /* @__PURE__ */ jsx("strong", { style: { fontSize: 13 }, children: "Source scope" }),
          /* @__PURE__ */ jsxs("label", { style: { display: "grid", gap: 6, maxWidth: 260 }, children: [
            /* @__PURE__ */ jsx(Tiny, { children: "Recently active projects (auto)" }),
            /* @__PURE__ */ jsx(
              TextInput,
              {
                value: String(activeProjectLimit),
                disabled: locked || busy,
                onChange: (event) => setActiveProjectsLimit(Number(event.currentTarget.value))
              }
            )
          ] }),
          /* @__PURE__ */ jsx(Tiny, { children: "Specific projects, issue trees, and company-wide ingestion use the same profile API; this first editor keeps the default auto-scope path visible and capped." })
        ] }),
        /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
          /* @__PURE__ */ jsx("strong", { style: { fontSize: 13 }, children: "Source kinds" }),
          [
            ["issues", "Issues"],
            ["comments", "Comments"],
            ["documents", "Documents"]
          ].map(([key, label]) => /* @__PURE__ */ jsxs("label", { style: { display: "flex", gap: 8, alignItems: "center", fontSize: 13 }, children: [
            /* @__PURE__ */ jsx(
              "input",
              {
                type: "checkbox",
                checked: draft.sourceKinds[key],
                disabled: locked || busy,
                onChange: (event) => setSourceKind(key, event.currentTarget.checked)
              }
            ),
            label
          ] }, key)),
          /* @__PURE__ */ jsx(Tiny, { children: "Attachments \u2014 locked, metadata only; no file contents. Future extraction needs separate review." }),
          /* @__PURE__ */ jsx(Tiny, { children: "Work products \u2014 locked, metadata only; no artifact contents." })
        ] }),
        /* @__PURE__ */ jsxs("div", { style: { display: "grid", gap: 8 }, children: [
          /* @__PURE__ */ jsx("strong", { style: { fontSize: 13 }, children: "Caps" }),
          /* @__PURE__ */ jsxs(Tiny, { children: [
            "Defaults: ",
            draft.cursor.maxWindowCharacters.toLocaleString(),
            " chars/window \xB7 ",
            draft.cursor.maxCharactersPerSource.toLocaleString(),
            " chars/source."
          ] })
        ] }),
        /* @__PURE__ */ jsxs("div", { style: { display: "flex", gap: 8, flexWrap: "wrap" }, children: [
          /* @__PURE__ */ jsx(Button, { size: "sm", variant: "primary", onClick: save, loading: busy, disabled: !canSave || emptyScopes, children: "Save Paperclip profile" }),
          /* @__PURE__ */ jsx(Button, { size: "sm", variant: "ghost", onClick: () => setDraft(data?.profile ?? null), disabled: busy, children: "Revert" })
        ] })
      ] }) : null
    ] }) })
  ] });
}
function SpaceFolderHealthChecklist({ folder }) {
  const items = [
    { key: "readable", label: "Folder readable", ok: folder.readable },
    { key: "writable", label: "Folder writable", ok: folder.writable },
    ...folder.requiredDirectories.map((dir) => ({
      key: `dir-${dir}`,
      label: `${dir}/ present`,
      ok: !folder.missingDirectories.includes(dir)
    })),
    ...folder.requiredFiles.map((file) => ({
      key: `file-${file}`,
      label: `${file} present`,
      ok: !folder.missingFiles.includes(file)
    }))
  ];
  return /* @__PURE__ */ jsxs("div", { role: "list", "aria-label": "Space folder health checklist", style: { position: "relative", display: "grid", gap: 0, padding: "2px 0" }, children: [
    items.length > 1 ? /* @__PURE__ */ jsx(
      "span",
      {
        "aria-hidden": true,
        style: {
          position: "absolute",
          left: 8,
          top: 12,
          bottom: 12,
          width: 1,
          background: "oklch(0.38 0.09 145)"
        }
      }
    ) : null,
    items.map((item) => /* @__PURE__ */ jsxs(
      "div",
      {
        role: "listitem",
        style: {
          display: "grid",
          gridTemplateColumns: "18px minmax(0, 1fr)",
          alignItems: "center",
          gap: 10,
          padding: "7px 0",
          minWidth: 0
        },
        children: [
          /* @__PURE__ */ jsx("span", { style: { display: "inline-flex", justifyContent: "center", position: "relative", zIndex: 1, background: tokens.bg }, children: /* @__PURE__ */ jsx(StatusIcon, { status: item.ok ? "done" : "blocked" }) }),
          /* @__PURE__ */ jsx("span", { style: { fontSize: 13, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", color: item.ok ? tokens.fg : "oklch(0.85 0.1 70)" }, children: item.label })
        ]
      },
      item.key
    ))
  ] });
}
export {
  SettingsPage,
  SidebarLink,
  WikiPage,
  WikiRouteSidebar
};
//# sourceMappingURL=index.js.map
