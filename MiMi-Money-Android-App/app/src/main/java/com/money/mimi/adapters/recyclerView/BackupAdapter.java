package com.money.mimi.adapters.recyclerView;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.net.Uri;
import com.money.mimi.R;
import com.money.mimi.activities.main.PreMainActivity;
import com.money.mimi.activities.settings.BackupActivity;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.backup.FormatDateTime;
import com.money.mimi.models.BackupDriveModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by Abderrahim El imame on 28/04/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class BackupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Activity mActivity;
    private List<BackupDriveModel> backupDriveModelList;
    private FormatDateTime formatDateTime;
    private boolean forBackupActivity;

    public void setBackupDriveModelList(List<BackupDriveModel> statusModelList) {
        this.backupDriveModelList = statusModelList;
        notifyDataSetChanged();
    }


    public BackupAdapter(@NonNull Activity mActivity, boolean forBackupActivity) {
        this.mActivity = mActivity;
        formatDateTime = new FormatDateTime(mActivity);
        this.forBackupActivity = forBackupActivity;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mActivity).inflate(R.layout.row_backup, parent, false);
        return new BackupViewHolder(itemView);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BackupViewHolder backupViewHolder = (BackupViewHolder) holder;
        BackupDriveModel backupDriveModel = backupDriveModelList.get(position);
        String modified = formatDateTime.formatDate(backupDriveModel.getModifiedDate());
        String size = Formatter.formatFileSize(mActivity, backupDriveModel.getBackupSize());

        backupViewHolder.modifiedTextView.setText(modified);
        backupViewHolder.typeTextView.setText(size);

    }

    @Override
    public int getItemCount() {
        if (backupDriveModelList != null)
            return backupDriveModelList.size();
        else
            return 0;
    }

    public class BackupViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_history_time)
        TextView modifiedTextView;
        @BindView(R.id.item_history_type)
        TextView typeTextView;

        BackupViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            setTypeFaces();
            itemView.setOnClickListener(v -> {
                BackupDriveModel backupDriveModel = backupDriveModelList.get(getAdapterPosition());
                final Uri backupUri = backupDriveModel.getUri();
                final String modified = formatDateTime.formatDate(backupDriveModel.getModifiedDate());
                final String size = Formatter.formatFileSize(mActivity, backupDriveModel.getBackupSize());

                // Show custom dialog
                final Dialog dialog = new Dialog(mActivity);
                dialog.setContentView(R.layout.dialog_backup_restore);
                AppCompatTextView createdTextView = (AppCompatTextView) dialog.findViewById(R.id.dialog_backup_restore_created);
                TextView sizeTextView = (TextView) dialog.findViewById(R.id.dialog_backup_restore_size);
                LinearLayout restoreButton = (LinearLayout) dialog.findViewById(R.id.dialog_backup_restore_button_restore);
                LinearLayout cancelButton = (LinearLayout) dialog.findViewById(R.id.dialog_backup_restore_button_cancel);

                createdTextView.setText(modified);
                sizeTextView.setText(size);

                restoreButton.setOnClickListener(v1 -> {

                    if (forBackupActivity) {
                        ((BackupActivity) mActivity).restoreBackup(backupUri);
                        dialog.dismiss();
                    } else {
                        ((PreMainActivity) mActivity).restoreBackup(backupUri);
                        dialog.dismiss();
                    }
                });

                cancelButton.setOnClickListener(v12 -> {
                    dialog.dismiss();
                });

                dialog.show();
            });
        }


        private void setTypeFaces() {
            if (AppConstants.ENABLE_FONTS_TYPES) {
                modifiedTextView.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                typeTextView.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
            }
        }


    }


}
