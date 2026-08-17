package com.money.mimi.helpers.Files;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.money.mimi.helpers.AppHelper;
import com.money.mimi.interfaces.DownloadCallbacks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import io.realm.internal.IOException;
import okhttp3.ResponseBody;

import static com.money.mimi.helpers.Files.FilesManager.getFileAudio;
import static com.money.mimi.helpers.Files.FilesManager.getFileDocument;
import static com.money.mimi.helpers.Files.FilesManager.getFileImage;
import static com.money.mimi.helpers.Files.FilesManager.getFileVideo;
import static com.money.mimi.helpers.Files.FilesManager.isFileAudioExists;
import static com.money.mimi.helpers.Files.FilesManager.isFileDocumentsExists;
import static com.money.mimi.helpers.Files.FilesManager.isFileImagesExists;
import static com.money.mimi.helpers.Files.FilesManager.isFileVideosExists;

/**
 * Created by Abderrahim El imame on 7/28/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/bencherif_el
 */

public class DownloadFilesHelper {

    private DownloadCallbacks mDownloadCallbacks;
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private String type;
    private String Identifier;
    private ResponseBody mFile;

    public DownloadFilesHelper(final ResponseBody mFile, String Identifier, String type, final DownloadCallbacks mDownloadCallbacks) {
        this.mFile = mFile;
        this.Identifier = Identifier;
        this.type = type;
        this.mDownloadCallbacks = mDownloadCallbacks;

    }


    public boolean writeResponseBodyToDisk(Activity mActivity) {
        try {
            try {
                if (isFileImagesExists(mActivity, Identifier)) {
                    getFileImage(mActivity, Identifier).delete();
                } else if (isFileVideosExists(mActivity, Identifier)) {
                    getFileVideo(mActivity, Identifier).delete();
                } else if (isFileAudioExists(mActivity, Identifier)) {
                    getFileAudio(mActivity, Identifier).delete();
                } else if (isFileDocumentsExists(mActivity, Identifier)) {
                    getFileDocument(mActivity, Identifier).delete();
                }
            } catch (Exception ignored) {
            }

            File downloadedFile = null;
            String detectedExtension = "";
            if (mFile.contentType() != null) {
                detectedExtension = FilesManager.getExtensionFromMimeType(mFile.contentType().toString());
            }
            if ("document".equals(type)) {
                String contentType = mFile.contentType() != null ? mFile.contentType().toString() : null;
                if (!SafeFileGuard.isSafeServerIdentifier(Identifier)
                        || !SafeFileGuard.isSafeContentType(contentType)
                        || SafeFileGuard.hasBlockedExtension(detectedExtension)) {
                    return false;
                }
            }
            switch (type) {
                case "image":
                    downloadedFile = new File(FilesManager.getFileImagesPath(mActivity, Identifier));
                    break;
                case "video":
                    downloadedFile = new File(FilesManager.getFileVideoPath(mActivity, Identifier));
                    break;
                case "audio":
                    if (!detectedExtension.isEmpty()) {
                        downloadedFile = new File(FilesManager.getFileAudioPathWithExtension(mActivity, Identifier, detectedExtension));
                    } else {
                        downloadedFile = new File(FilesManager.getFileAudioPath(mActivity, Identifier));
                    }
                    break;
                case "document":
                    if (!detectedExtension.isEmpty()) {
                        downloadedFile = new File(FilesManager.getFileDocumentsPathWithExtension(mActivity, Identifier, detectedExtension));
                    } else {
                        downloadedFile = new File(FilesManager.getFileDocumentsPath(mActivity, Identifier));
                    }
                    break;
            }

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[DEFAULT_BUFFER_SIZE];

                long fileSize = mFile.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = mFile.byteStream();
                try {
                    outputStream = new FileOutputStream(downloadedFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }
                Handler handler = new Handler(Looper.getMainLooper());
                while (true) {
                    int read = 0;
                    try {
                        read = inputStream.read(fileReader);
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }

                    if (read == -1) {
                        break;
                    }

                    try {
                        outputStream.write(fileReader, 0, read);
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }

                    fileSizeDownloaded += read;
                    // update progress on UI thread
                    handler.post(new Updater(fileSizeDownloaded, fileSize));
                    AppHelper.LogCat("file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                try {
                    outputStream.flush();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }

                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    private class Updater implements Runnable {
        private long mUploaded;
        private long mTotal;

        Updater(long uploaded, long total) {
            mUploaded = uploaded;
            mTotal = total;
        }


        @Override
        public void run() {
            if (mTotal > 0) {
                mDownloadCallbacks.onUpdate((int) (100 * mUploaded / mTotal), type);
            }
        }
    }

}
