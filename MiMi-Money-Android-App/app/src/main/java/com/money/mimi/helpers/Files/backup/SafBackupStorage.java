package com.money.mimi.helpers.Files.backup;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.money.mimi.app.AppConstants;
import com.money.mimi.models.BackupDriveModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Storage Access Framework based backup I/O. */
public final class SafBackupStorage {
    private static final String MIME_TYPE = "application/octet-stream";

    private SafBackupStorage() {
    }

    @Nullable
    public static DocumentFile getFolder(@NonNull Context context, @Nullable String folderUri) {
        if (folderUri == null || folderUri.isEmpty() || !folderUri.startsWith("content://")) {
            return null;
        }
        DocumentFile folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri));
        return folder != null && folder.exists() && folder.isDirectory() ? folder : null;
    }

    @NonNull
    public static List<BackupDriveModel> listBackups(@NonNull DocumentFile folder) {
        String baseName = AppConstants.EXPORT_REALM_FILE_NAME;
        int extensionIndex = baseName.lastIndexOf('.');
        String prefix = extensionIndex > 0 ? baseName.substring(0, extensionIndex) : baseName;
        DocumentFile[] children = folder.listFiles();
        Arrays.sort(children, new Comparator<DocumentFile>() {
            @Override
            public int compare(DocumentFile first, DocumentFile second) {
                return Long.compare(second.lastModified(), first.lastModified());
            }
        });

        List<BackupDriveModel> backups = new ArrayList<>();
        for (DocumentFile child : children) {
            String name = child.getName();
            if (child.isFile() && name != null
                    && (name.equals(baseName) || (name.startsWith(prefix + "_") && name.endsWith(".realm")))) {
                backups.add(new BackupDriveModel(child.getUri(), new Date(child.lastModified()), child.length()));
            }
        }
        return backups;
    }

    @NonNull
    public static Uri writeBackup(@NonNull Context context, @NonNull DocumentFile folder,
                                  @NonNull File source) throws IOException {
        String baseName = AppConstants.EXPORT_REALM_FILE_NAME;
        int extensionIndex = baseName.lastIndexOf('.');
        String stem = extensionIndex > 0 ? baseName.substring(0, extensionIndex) : baseName;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        DocumentFile destination = folder.createFile(MIME_TYPE, stem + "_" + timestamp + ".realm");
        if (destination == null) {
            throw new IOException("The selected folder did not create the backup file");
        }
        try (InputStream input = new FileInputStream(source);
             OutputStream output = context.getContentResolver().openOutputStream(destination.getUri(), "w")) {
            if (output == null) {
                throw new IOException("The selected provider did not open the backup file");
            }
            copy(input, output);
        }
        return destination.getUri();
    }

    public static void copyToLocal(@NonNull Context context, @NonNull Uri source,
                                   @NonNull File destination) throws IOException {
        try (InputStream input = context.getContentResolver().openInputStream(source);
             OutputStream output = new FileOutputStream(destination)) {
            if (input == null) {
                throw new IOException("The selected provider did not open the backup file");
            }
            copy(input, output);
        }
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        output.flush();
    }
}
