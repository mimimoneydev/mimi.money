package com.money.mimi.adapters.recyclerView;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.money.mimi.R;
import com.money.mimi.activities.popups.StatusDelete;
import com.money.mimi.activities.status.StatusActivity;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.models.users.status.StatusModel;
import com.money.mimi.presenters.users.StatusPresenter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;


/**
 * Created by Abderrahim El imame on 28/04/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class StatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Activity mActivity;
    private List<StatusModel> mStatusModel;
    private StatusPresenter statusPresenter;

    public void setStatus(List<StatusModel> statusModelList) {
        this.mStatusModel = statusModelList;
        notifyDataSetChanged();
    }


    public StatusAdapter(@NonNull StatusActivity mActivity) {
        this.mActivity = mActivity;
        statusPresenter = new StatusPresenter(mActivity);
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mActivity).inflate(R.layout.row_status, parent, false);
        return new StatusViewHolder(itemView);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        StatusViewHolder statusViewHolder = (StatusViewHolder) holder;
        StatusModel statusModel = mStatusModel.get(position);
        try {


            if (statusModel.getStatus() != null) {

                statusViewHolder.setStatus(statusModel.getStatus());
            }


        } catch (Exception e) {
            AppHelper.LogCat("" + e.getMessage());
        }
        statusViewHolder.setOnLongClickListener(v -> {
            Intent mIntent = new Intent(mActivity, StatusDelete.class);
            mIntent.putExtra("statusID", statusModel.getId());
            mActivity.startActivity(mIntent);
            return true;
        });
        statusViewHolder.setOnClickListener(v -> statusPresenter.UpdateCurrentStatus(statusModel.getStatus(), statusModel.getId()));

    }

    private void removeStatusItem(int position) {
        if (position != 0) {
            try {
                mStatusModel.remove(position);
                notifyItemRemoved(position);
            } catch (Exception e) {
                AppHelper.LogCat(e);
            }
        }
    }

    public void DeleteStatusItem(int StatusID) {
        try {
            int arraySize = mStatusModel.size();
            for (int i = 0; i < arraySize; i++) {
                StatusModel model = mStatusModel.get(i);
                if (model.isValid()) {
                    if (StatusID == model.getId()) {
                        removeStatusItem(i);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
    }

    @Override
    public int getItemCount() {
        if (mStatusModel != null)
            return mStatusModel.size();
        else
            return 0;
    }

    class StatusViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.status)
        EmojiconTextView status;

        StatusViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            status.setSelected(true);
            setTypeFaces();
        }


        private void setTypeFaces() {
            if (AppConstants.ENABLE_FONTS_TYPES) {
                status.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
            }
        }

        void setStatus(String Status) {
            String finalStatus = UtilsString.unescapeJava(Status);
                status.setText(finalStatus);
        }

        void setStatusColorCurrent() {
            status.setTextColor(mActivity.getResources().getColor(R.color.colorBlueLight));
        }

        void setStatusColor() {
            status.setTextColor(mActivity.getResources().getColor(R.color.colorBlack));
        }

        void setOnClickListener(View.OnClickListener listener) {
            itemView.setOnClickListener(listener);

        }

        void setOnLongClickListener(View.OnLongClickListener listener) {
            itemView.setOnLongClickListener(listener);

        }
    }


}
