package com.money.mimi.helpers.Files;

import android.os.Handler;
import android.os.Looper;

import com.money.mimi.interfaces.UploadCallbacks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class UploadFilesHelper extends RequestBody {

    private File mFile;
    private byte[] arrBytes;
    private UploadCallbacks mUploadCallbacks;
    private String mimeType;
    private String mType;

    private static final int DEFAULT_BUFFER_SIZE = 65536;

    public UploadFilesHelper(final File mFile, final UploadCallbacks mUploadCallbacks, String mimeType, byte[] arrBytes, String mType) {
        this.mFile = mFile;
        this.mUploadCallbacks = mUploadCallbacks;
        this.mimeType = mimeType;
        this.mType = mType;
        this.arrBytes = arrBytes;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(mimeType);
    }

    @Override
    public long contentLength() throws IOException {
        if (mFile != null && arrBytes == null) {
            return mFile.length();
        } else if (arrBytes != null) {
            return arrBytes.length;
        }
        return 0;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Handler handler = new Handler(Looper.getMainLooper());

        if (mFile != null && arrBytes == null) {
            long fileLength = mFile.length();
            FileInputStream fileInputStream = new FileInputStream(mFile);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            long uploaded = 0;
            int lastProgress = -1;

            try {
                int read;
                while ((read = fileInputStream.read(buffer)) != -1) {
                    uploaded += read;
                    sink.write(buffer, 0, read);

                    int progress = fileLength > 0 ? (int) (100 * uploaded / fileLength) : 0;
                    if (progress != lastProgress) {
                        lastProgress = progress;
                        handler.post(new Updater(progress, mType));
                    }
                }
            } finally {
                fileInputStream.close();
            }
        } else if (arrBytes != null) {
            long fileLength = arrBytes.length;
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(arrBytes);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            long uploaded = 0;
            int lastProgress = -1;

            try {
                int read;
                while ((read = byteArrayInputStream.read(buffer)) != -1) {
                    uploaded += read;
                    sink.write(buffer, 0, read);

                    int progress = fileLength > 0 ? (int) (100 * uploaded / fileLength) : 0;
                    if (progress != lastProgress) {
                        lastProgress = progress;
                        handler.post(new Updater(progress, mType));
                    }
                }
            } finally {
                byteArrayInputStream.close();
            }
        }
    }

    private class Updater implements Runnable {
        private int mProgress;
        private String mType;

        Updater(int progress, String type) {
            mProgress = progress;
            mType = type;
        }

        @Override
        public void run() {
            mUploadCallbacks.onUpdate(mProgress, mType);
        }
    }
}
