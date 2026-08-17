package com.money.mimi.activities.main;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.money.mimi.R;
import com.money.mimi.adapters.recyclerView.BackupAdapter;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.helpers.Files.backup.RealmBackupRestore;
import com.money.mimi.helpers.Files.backup.SafBackupStorage;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.models.BackupDriveModel;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/** Lets a returning user select and restore a SAF database backup before entering the app. */
public class PreMainActivity extends AppCompatActivity {
    private static final int REQUEST_SELECT_FOLDER = 4;

    @BindView(R.id.toolbar) LinearLayout toolbar;
    @BindView(R.id.skip_backup) AppCompatTextView skipBackupBtn;
    @BindView(R.id.backup_drive_button_folder) LinearLayout selectFolderButton;
    @BindView(R.id.backupRecyclerView) RecyclerView backupRecyclerView;
    @BindView(R.id.backup_drive_textview_folder) TextView folderTextView;

    private String backupFolderUri = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_main);
        ButterKnife.bind(this);
        if (AppConstants.ENABLE_FONTS_TYPES) {
            folderTextView.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
        skipBackupBtn.setOnClickListener(v -> skipBackup());
        selectFolderButton.setOnClickListener(v -> openFolderPicker());
        setDetails();
    }

    private DocumentFile getSelectedFolder() {
        return SafBackupStorage.getFolder(this, backupFolderUri);
    }

    private void setDetails() {
        backupFolderUri = PreferenceManager.getBackupFolder(this);
        DocumentFile folder = getSelectedFolder();
        if (folder == null) {
            folderTextView.setText(R.string.backup_drive_folder_not_set);
            return;
        }
        folderTextView.setText(folder.getName() == null ? folder.getUri().toString() : folder.getName());
        loadBackups(folder);
    }

    private void loadBackups(DocumentFile folder) {
        new Thread(() -> {
            try {
                List<BackupDriveModel> backups = SafBackupStorage.listBackups(folder);
                runOnUiThread(() -> {
                    backupRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                    BackupAdapter adapter = new BackupAdapter(this, false);
                    backupRecyclerView.setAdapter(adapter);
                    adapter.setBackupDriveModelList(backups);
                });
            } catch (Exception e) {
                logError(e, "Unable to list SAF backups");
                runOnUiThread(this::showErrorDialog);
            }
        }).start();
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_SELECT_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SELECT_FOLDER || resultCode != RESULT_OK || data == null
                || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        try {
            persistFolderPermission(uri, data.getFlags());
            PreferenceManager.saveBackupFolder(this, uri.toString());
            setDetails();
        } catch (SecurityException e) {
            logError(e, "Provider did not grant persistent folder access");
            showErrorDialog();
        }
    }

    private void persistFolderPermission(Uri uri, int grantedFlags) {
        boolean canRead = (grantedFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0;
        boolean canWrite = (grantedFlags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0;
        if (canRead && canWrite) {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else if (canRead) {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (canWrite) {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            throw new SecurityException("Provider granted no persistent folder access");
        }
    }

    public void restoreBackup(Uri backupUri) {
        AppHelper.showDialog(this, getString(R.string.please_wait_a_moment));
        new Thread(() -> {
            boolean restored = false;
            try {
                File localFile = new File(FilesManager.getImagesCachePath(this), AppConstants.EXPORT_REALM_FILE_NAME);
                SafBackupStorage.copyToLocal(this, backupUri, localFile);
                restored = RealmBackupRestore.restore(this);
            } catch (Exception e) {
                logError(e, "Unable to restore SAF backup");
            }
            boolean restoreSucceeded = restored;
            runOnUiThread(() -> {
                AppHelper.hideDialog();
                if (!restoreSucceeded) {
                    showErrorDialog();
                    return;
                }
                Toast.makeText(this, R.string.activity_backup_drive_message_restart, Toast.LENGTH_LONG).show();
                showRestoreCompleteDialog();
            });
        }).start();
    }

    private void showRestoreCompleteDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(getString(R.string.restore_is_done));
        alert.setPositiveButton(R.string.next, (dialog, which) -> {
            AppHelper.showDialog(this, getString(R.string.please_wait_a_moment), false);
            new Handler().postDelayed(() -> {
                PreferenceManager.setHasBackup(this, false);
                AppHelper.hideDialog();
                if (PreferenceManager.getToken(this) != null) {
                    AppHelper.startMainService(this);
                }
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }, 1000);
        });
        alert.setCancelable(false);
        alert.show();
    }

    public void skipBackup() {
        PreferenceManager.setHasBackup(this, false);
        if (PreferenceManager.getToken(this) != null) {
            AppHelper.startMainService(this);
        }
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showErrorDialog() {
        Toast.makeText(this, R.string.activity_backup_drive_failed, Toast.LENGTH_SHORT).show();
    }

    private void logError(Exception e, String message) {
        AppHelper.LogCat(message + ": " + e.getMessage());
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            AnimationsUtil.setSlideOutAnimation(this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationsUtil.setSlideOutAnimation(this);
    }
}
