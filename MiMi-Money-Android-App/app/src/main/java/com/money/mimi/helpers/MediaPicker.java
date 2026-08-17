package com.money.mimi.helpers;

import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;

/**
 * Creates permission-free system picker intents for user-selected media.
 *
 * Android 13+ uses the privacy-preserving Photo Picker. Older supported Android
 * versions use the Storage Access Framework, which grants access only to the
 * document selected by the user.
 */
public final class MediaPicker {

    private MediaPicker() {
    }

    public static Intent createImageIntent() {
        return createVisualMediaIntent("image/*");
    }

    public static Intent createVideoIntent() {
        return createVisualMediaIntent("video/*");
    }

    public static Intent createDocumentIntent(String mimeType) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return intent;
    }

    private static Intent createVisualMediaIntent(String mimeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            intent.setType(mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return intent;
        }
        return createDocumentIntent(mimeType);
    }
}
