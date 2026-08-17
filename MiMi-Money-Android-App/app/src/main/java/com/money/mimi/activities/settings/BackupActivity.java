package com.money.mimi.activities.settings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.money.mimi.R;
import com.money.mimi.activities.main.welcome.SplashScreenActivity;
import com.money.mimi.adapters.recyclerView.BackupAdapter;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.api.APIHelper;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.helpers.Files.backup.RealmBackupRestore;
import com.money.mimi.helpers.Files.backup.SafBackupStorage;
import com.money.mimi.helpers.PreferenceManager;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;

/** Creates and restores database backups through Android's Storage Access Framework. */
public class BackupActivity extends AppCompatActivity {
    private static final int REQUEST_SELECT_FOLDER_AND_BACKUP = 2;
    private static final int REQUEST_SELECT_FOLDER = 4;

    @BindView(R.id.backup_msg) TextView backupMessage;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.backup_drive_button_backup) Button backupButton;
    @BindView(R.id.backup_drive_button_folder) LinearLayout selectFolderButton;
    @BindView(R.id.backupRecyclerView) RecyclerView backupRecyclerView;
    @BindView(R.id.backup_drive_textview_folder) TextView folderTextView;

    private final CompositeDisposable disposables = new CompositeDisposable();
    private String backupFolderUri = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        ButterKnife.bind(this);
        setupToolbar();
        setTypeFaces();
        backupButton.setOnClickListener(v -> {
            DocumentFile folder = getSelectedFolder();
            if (folder == null) {
                openFolderPicker(REQUEST_SELECT_FOLDER_AND_BACKUP);
            } else {
                uploadBackup(folder);
            }
        });
        selectFolderButton.setOnClickListener(v -> openFolderPicker(REQUEST_SELECT_FOLDER));
        setDetails();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.chat_backup));
        }
    }

    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            backupMessage.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            folderTextView.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }

    private DocumentFile getSelectedFolder() {
        return SafBackupStorage.getFolder(this, backupFolderUri);
    }

    private void setDetails() {
        backupFolderUri = PreferenceManager.getBackupFolder(this);
        DocumentFile folder = getSelectedFolder();
        if (folder == null) {
            folderTextView.setText(R.string.backup_drive_folder_not_set);
            backupButton.setVisibility(View.VISIBLE);
            return;
        }
        folderTextView.setText(folder.getName() == null ? folder.getUri().toString() : folder.getName());
        backupButton.setVisibility(View.VISIBLE);
        loadBackups(folder);
    }

    private void loadBackups(DocumentFile folder) {
        new Thread(() -> {
            try {
                List<com.money.mimi.models.BackupDriveModel> backups = SafBackupStorage.listBackups(folder);
                runOnUiThread(() -> {
                    backupRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                    BackupAdapter adapter = new BackupAdapter(this, true);
                    backupRecyclerView.setAdapter(adapter);
                    adapter.setBackupDriveModelList(backups);
                });
            } catch (Exception e) {
                logError(e, "Unable to list SAF backups");
                runOnUiThread(this::showErrorDialog);
            }
        }).start();
    }

    private void openFolderPicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    private void saveSelectedFolder(Intent data) {
        Uri uri = data.getData();
        if (uri == null) return;
        try {
            persistFolderPermission(uri, data.getFlags());
        } catch (SecurityException e) {
            logError(e, "Provider did not grant persistent folder access");
            showErrorDialog();
            return;
        }
        backupFolderUri = uri.toString();
        PreferenceManager.saveBackupFolder(this, backupFolderUri);
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

    private void uploadBackup(DocumentFile folder) {
        AppHelper.showDialog(this, getString(R.string.please_wait_a_moment));
        new Thread(() -> {
            try {
                File localBackup = RealmBackupRestore.backup(this);
                SafBackupStorage.writeBackup(this, folder, localBackup);
                runOnUiThread(() -> {
                    AppHelper.hideDialog();
                    showSuccessDialog();
                    setDetails();
                    disposables.add(APIHelper.initialApiUsersContacts().userHasBackup("true")
                            .subscribe(ignored -> { }, throwable -> { }));
                });
            } catch (Exception e) {
                logError(e, "Unable to write SAF backup");
                runOnUiThread(() -> {
                    AppHelper.hideDialog();
                    showErrorDialog();
                });
            }
        }).start();
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
                showRestartDialog();
            });
        }).start();
    }

    private void showRestartDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(R.string.you_need_to_restart_the_application);
        alert.setPositiveButton(R.string.ok, (dialog, which) -> {
            Intent restart = new Intent(this, SplashScreenActivity.class);
            PendingIntent pending = PendingIntent.getActivity(this, 123456, restart,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            manager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pending);
            System.exit(0);
        });
        alert.setCancelable(false);
        alert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_SELECT_FOLDER_AND_BACKUP || requestCode == REQUEST_SELECT_FOLDER)
                && resultCode == RESULT_OK && data != null) {
            saveSelectedFolder(data);
            DocumentFile folder = getSelectedFolder();
            setDetails();
            if (requestCode == REQUEST_SELECT_FOLDER_AND_BACKUP && folder != null) {
                uploadBackup(folder);
            }
        }
    }

    private void showSuccessDialog() {
        Toast.makeText(this, R.string.activity_backup_drive_success, Toast.LENGTH_SHORT).show();
    }

    private void showErrorDialog() {
        Toast.makeText(this, R.string.activity_backup_drive_failed, Toast.LENGTH_SHORT).show();
    }

    private void logError(Exception e, String message) {
        AppHelper.LogCat(message + ": " + e.getMessage());
    }

    @Override
    protected void onDestroy() {
        disposables.clear();
        super.onDestroy();
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
