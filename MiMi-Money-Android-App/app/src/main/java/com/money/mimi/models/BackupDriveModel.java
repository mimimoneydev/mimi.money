package com.money.mimi.models;

import android.net.Uri;

import java.util.Date;

/**
 * Created by Abderrahim El imame on 6/17/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class BackupDriveModel {
    private Uri uri;
    private Date modifiedDate;
    private long backupSize;

    public BackupDriveModel(Uri uri, Date modifiedDate, long backupSize) {
        this.uri = uri;
        this.modifiedDate = modifiedDate;
        this.backupSize = backupSize;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public long getBackupSize() {
        return backupSize;
    }

    public void setBackupSize(long backupSize) {
        this.backupSize = backupSize;
    }
}
