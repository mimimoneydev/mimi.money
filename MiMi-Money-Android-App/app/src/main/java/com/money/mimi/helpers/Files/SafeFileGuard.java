package com.money.mimi.helpers.Files;

import android.text.TextUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class SafeFileGuard {
    public static final String BLOCKED_FILE_MESSAGE = "This file type is blocked for safety";

    private static final Set<String> BLOCKED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".aab", ".dex", ".jar", ".js", ".mjs", ".sh", ".bash", ".zsh",
            ".bat", ".cmd", ".exe", ".msi", ".scr", ".com", ".pif",
            ".vbs", ".vbe", ".ps1", ".psm1", ".apkx"
    ));

    private static final Set<String> BLOCKED_MIME_TYPES = new HashSet<>(Arrays.asList(
            "application/javascript",
            "application/ecmascript",
            "application/java-archive",
            "application/x-dex",
            "application/x-sh",
            "application/x-msdownload",
            "application/x-msdos-program",
            "application/x-bat",
            "text/javascript",
            "text/ecmascript",
            "text/x-shellscript"
    ));

    private SafeFileGuard() {
    }

    public static boolean isSafeDocumentPath(String path, String mimeType) {
        if (TextUtils.isEmpty(path) || "null".equals(path)) return false;
        return !hasBlockedExtension(path) && !hasBlockedMimeType(mimeType);
    }

    public static boolean isSafeDocumentFile(File file) {
        return file != null && file.exists() && file.isFile() && !hasBlockedExtension(file.getName());
    }

    public static boolean isSafeServerIdentifier(String identifier) {
        if (TextUtils.isEmpty(identifier) || "null".equals(identifier)) return false;
        if (hasBlockedExtension(identifier)) return false;
        for (int i = 0; i < identifier.length(); i++) {
            if (Character.isISOControl(identifier.charAt(i))) return false;
        }
        return !identifier.contains("/") && !identifier.contains("\\") && !identifier.contains("..");
    }

    public static boolean isSafeContentType(String mimeType) {
        return !hasBlockedMimeType(mimeType) && !hasBlockedExtension(extensionFromMimeType(mimeType));
    }

    public static boolean hasBlockedExtension(String value) {
        String extension = extensionFromName(value);
        return !extension.isEmpty() && BLOCKED_EXTENSIONS.contains(extension);
    }

    public static boolean hasBlockedMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) return false;
        String normalized = mimeType.split(";", 2)[0].trim().toLowerCase(Locale.US);
        return BLOCKED_MIME_TYPES.contains(normalized);
    }

    private static String extensionFromName(String value) {
        if (TextUtils.isEmpty(value)) return "";
        String clean = value;
        int query = clean.indexOf('?');
        if (query >= 0) clean = clean.substring(0, query);
        int fragment = clean.indexOf('#');
        if (fragment >= 0) clean = clean.substring(0, fragment);
        clean = clean.replace('\\', '/');
        int slash = clean.lastIndexOf('/');
        if (slash >= 0) clean = clean.substring(slash + 1);
        int dot = clean.lastIndexOf('.');
        if (dot < 0 || dot == clean.length() - 1) return "";
        return clean.substring(dot).toLowerCase(Locale.US);
    }

    private static String extensionFromMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) return "";
        String normalized = mimeType.split(";", 2)[0].trim().toLowerCase(Locale.US);
        if ("application/java-archive".equals(normalized)) return ".jar";
        if ("application/javascript".equals(normalized) || "text/javascript".equals(normalized)) return ".js";
        if ("application/x-dex".equals(normalized)) return ".dex";
        if ("application/x-msdownload".equals(normalized) || "application/x-msdos-program".equals(normalized)) return ".exe";
        if ("application/x-sh".equals(normalized) || "text/x-shellscript".equals(normalized)) return ".sh";
        return "";
    }
}
