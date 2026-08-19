export function printLmstudioStreamEvent(line, debug) {
    if (!debug)
        return;
    const trimmed = line.trim();
    if (trimmed)
        process.stdout.write(trimmed + "\n");
}
//# sourceMappingURL=format-event.js.map