export function parseLmstudioStdoutLine(line, ts) {
    const trimmed = line.trim();
    if (!trimmed)
        return [];
    return [{ kind: "assistant", ts, text: trimmed }];
}
//# sourceMappingURL=parse-stdout.js.map