package com.money.mimi.adapters.recyclerView.messages;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.money.mimi.R;
import com.money.mimi.activities.settings.PreferenceSettingsManager;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.api.APIService;
import com.money.mimi.api.FilesDownloadService;
import com.money.mimi.api.FilesUploadService;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.DownloadFilesHelper;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.helpers.Files.SafeFileGuard;
import com.money.mimi.helpers.Files.UploadFilesHelper;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.PermissionHandler;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.helpers.UtilsTime;
import com.money.mimi.helpers.images.BlurTransformationPicasso;
import com.money.mimi.interfaces.AudioCallbacks;
import com.money.mimi.interfaces.DownloadCallbacks;
import com.money.mimi.interfaces.UploadCallbacks;
import com.money.mimi.models.messages.FilesResponse;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.TextDrawable;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import io.realm.Realm;
import io.realm.RealmList;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */

@SuppressLint("StaticFieldLeak")
public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private RealmList<MessagesModel> mMessagesModel;
    private static final int INCOMING_MESSAGES = 0;
    private static final int OUTGOING_MESSAGES = 1;
    private static final int LEFT_MESSAGES = 2;
    private Realm realm;
    private MediaPlayer mMediaPlayer;
    private Handler mHandler;
    private String SearchQuery;
    private SparseBooleanArray selectedItems;
    private boolean isStatusUpdated = false;
    private boolean isActivated = false;
    private MemoryCache memoryCache;
    private SparseArray<Drawable> thumbnailRequestMap = new SparseArray<>();
    private APIService mApiService;
    private Activity activeActivity;
    private final Set<Integer> autoDownloadQueue = new HashSet<>();

    public MessagesAdapter(Realm realm) {
        this.mMessagesModel = new RealmList<>();
        this.realm = realm;
        this.mHandler = new Handler();
        this.mMediaPlayer = new MediaPlayer();
        this.selectedItems = new SparseBooleanArray();
        this.memoryCache = new MemoryCache();
        mApiService = new APIService(WhatsCloneApplication.getInstance());
    }

    public void setMessages(RealmList<MessagesModel> messagesModelList) {
        this.mMessagesModel = messagesModelList;
        notifyDataSetChanged();
    }

    public void addMessage(MessagesModel messagesModel) {
        for (MessagesModel existing : mMessagesModel) {
            if (existing.getId() == messagesModel.getId()) {
                return;
            }
        }
        this.mMessagesModel.add(messagesModel);
        notifyItemInserted(mMessagesModel.size() - 1);
    }


    //Methods for search start
    public void setString(String SearchQuery) {
        this.SearchQuery = SearchQuery;
        notifyDataSetChanged();
    }

    public void animateTo(List<MessagesModel> models) {
        applyAndAnimateRemovals(models);
        applyAndAnimateAdditions(models);
        applyAndAnimateMovedItems(models);
    }

    private void applyAndAnimateRemovals(List<MessagesModel> newModels) {
        int arraySize = mMessagesModel.size();
        for (int i = arraySize - 1; i >= 0; i--) {
            final MessagesModel model = mMessagesModel.get(i);
            if (!newModels.contains(model)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<MessagesModel> newModels) {
        int arraySize = newModels.size();
        for (int i = 0; i < arraySize; i++) {
            final MessagesModel model = newModels.get(i);
            if (!mMessagesModel.contains(model)) {
                addItem(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<MessagesModel> newModels) {
        int arraySize = newModels.size();
        for (int toPosition = arraySize - 1; toPosition >= 0; toPosition--) {
            final MessagesModel model = newModels.get(toPosition);
            final int fromPosition = mMessagesModel.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private MessagesModel removeItem(int position) {
        final MessagesModel model = mMessagesModel.remove(position);
        notifyItemRemoved(position);
        return model;
    }

    private void addItem(int position, MessagesModel model) {
        mMessagesModel.add(position, model);
        notifyItemInserted(position);
    }

    private void moveItem(int fromPosition, int toPosition) {
        final MessagesModel model = mMessagesModel.remove(fromPosition);
        mMessagesModel.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
    }
    //Methods for search end

    @Override
    public int getItemViewType(int position) {
        try {
            MessagesModel messagesModel = mMessagesModel.get(position);


            if (messagesModel.getSenderID() == PreferenceManager.getID(WhatsCloneApplication.getInstance())) {
                if (messagesModel.getMessage().equals(AppConstants.CREATE_GROUP) || messagesModel.getMessage().equals(AppConstants.LEFT_GROUP)) {
                    return LEFT_MESSAGES;
                } else {
                    return OUTGOING_MESSAGES;

                }
            } else {
                if (messagesModel.getMessage().equals(AppConstants.CREATE_GROUP) || messagesModel.getMessage().equals(AppConstants.LEFT_GROUP)) {
                    return LEFT_MESSAGES;
                } else {
                    return INCOMING_MESSAGES;
                }

            }

        } catch (Exception e) {
            AppHelper.LogCat("kdoub rminin Exception" + e.getMessage());
            return 0;
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context mActivity = parent.getContext();
        View view;
        if (viewType == INCOMING_MESSAGES) {
            view = LayoutInflater.from(mActivity).inflate(R.layout.bubble_left, parent, false);
            return new MessagesViewHolder(view);
        } else if (viewType == OUTGOING_MESSAGES) {
            view = LayoutInflater.from(mActivity).inflate(R.layout.bubble_right, parent, false);
            return new MessagesViewHolder(view);
        } else {
            view = LayoutInflater.from(mActivity).inflate(R.layout.created_group_view, parent, false);
            return new MessagesViewHolder(view);
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof MessagesViewHolder) {
            MessagesViewHolder mMessagesViewHolder = (MessagesViewHolder) holder;
            Context mActivity = holder.itemView.getContext();
            if (mActivity instanceof Activity) {
                activeActivity = (Activity) mActivity;
            }
            MessagesModel messagesModel = this.mMessagesModel.get(position);
            Linkify.addLinks(mMessagesViewHolder.message, Linkify.ALL);
            if (messagesModel.isValid()) {

                ContactsModel SenderInfo = realm.where(ContactsModel.class).equalTo("id", messagesModel.getSenderID()).findFirst();
                ContactsModel RecipientInfo = realm.where(ContactsModel.class).equalTo("id", messagesModel.getRecipientID()).findFirst();
                boolean hasRemoteFile = messagesModel.getSenderID() != PreferenceManager.getID(mActivity)
                        && ((messagesModel.getImageFile() != null && !messagesModel.getImageFile().equals("null"))
                        || (messagesModel.getVideoFile() != null && !messagesModel.getVideoFile().equals("null"))
                        || (messagesModel.getAudioFile() != null && !messagesModel.getAudioFile().equals("null"))
                        || (messagesModel.getDocumentFile() != null && !messagesModel.getDocumentFile().equals("null")));

          /*      if (messagesModel.getMessage() != null) {
                    if (UtilsString.checkForUrls(messagesModel.getMessage())) {
                        AppHelper.LogCat("checkForUrls is true");
                        String url = UtilsString.getUrl(messagesModel.getMessage());
                        if (url != null)
                            if (!url.startsWith("http://")) {
                                if (!url.startsWith("https://")) {
                                    url = (new StringBuilder()).append("http://").append(url).toString();
                                }
                            }
                        mMessagesViewHolder.textCrawler.makePreview(mMessagesViewHolder.linkPreviewCallback, url);
                    } else {
                        AppHelper.LogCat("checkForUrls is false");
                        mMessagesViewHolder.linkPreview.setVisibility(View.GONE);
                    }
                }*/
                boolean isGroup;
                if (messagesModel.isGroup()) {
                    isGroup = true;
                    try {
                        if (messagesModel.getSenderID() != PreferenceManager.getID(mActivity)) {


                            String name = UtilsPhone.getContactName(messagesModel.getWalletAddress());
                            if (name != null) {
                                ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
                                // generate random color
                                int color = generator.getColor(name);
                                mMessagesViewHolder.showSenderName();
                                mMessagesViewHolder.setSenderName(name);
                                mMessagesViewHolder.setSenderColor(color);
                            } else {
                                ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
                                // generate random color
                                int color = generator.getColor(messagesModel.getWalletAddress());
                                mMessagesViewHolder.showSenderName();
                                mMessagesViewHolder.setSenderName(messagesModel.getWalletAddress());
                                mMessagesViewHolder.setSenderColor(color);
                            }

                            //  }


                        }
                    } catch (Exception e) {
                        AppHelper.LogCat("Group username is null" + e.getMessage());
                    }
                } else {
                    isGroup = false;
                    mMessagesViewHolder.hideSenderName();
                }

                String message = UtilsString.unescapeJava(messagesModel.getMessage());
                switch (messagesModel.getMessage()) {
                    case AppConstants.CREATE_GROUP:
                        if (isGroup) {
                            if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                                mMessagesViewHolder.message.setText(mActivity.getString(R.string.you_created_this_group), TextView.BufferType.NORMAL);
                                mMessagesViewHolder.message.setTextSize(PreferenceSettingsManager.getMessage_font_size(mActivity));
                            } else {
                                String name = UtilsPhone.getContactName(messagesModel.getWalletAddress());
                                if (name != null) {
                                    mMessagesViewHolder.message.setText("" + name + mActivity.getString(R.string.he_created_this_group), TextView.BufferType.NORMAL);
                                    mMessagesViewHolder.message.setTextSize(PreferenceSettingsManager.getMessage_font_size(mActivity));
                                } else {
                                    mMessagesViewHolder.message.setText("" + messagesModel.getWalletAddress() + mActivity.getString(R.string.he_created_this_group), TextView.BufferType.NORMAL);
                                    mMessagesViewHolder.message.setTextSize(PreferenceSettingsManager.getMessage_font_size(mActivity));
                                }

                            }

                        }

                        break;
                    case AppConstants.LEFT_GROUP:

                        if (isGroup) {
                            if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                                mMessagesViewHolder.message.setText(mActivity.getString(R.string.you_left), TextView.BufferType.NORMAL);
                                mMessagesViewHolder.message.setTextSize(PreferenceSettingsManager.getMessage_font_size(mActivity));
                            } else {
                                String name = UtilsPhone.getContactName(messagesModel.getWalletAddress());
                                if (name != null) {
                                    mMessagesViewHolder.message.setText("" + name + mActivity.getString(R.string.he_left), TextView.BufferType.NORMAL);
                                    mMessagesViewHolder.message.setTextSize(PreferenceSettingsManager.getMessage_font_size(mActivity));
                                } else {
                                    mMessagesViewHolder.message.setText("" + messagesModel.getWalletAddress() + mActivity.getString(R.string.he_left), TextView.BufferType.NORMAL);
                                    mMessagesViewHolder.message.setTextSize(PreferenceSettingsManager.getMessage_font_size(mActivity));
                                }


                            }


                        }

                        break;
                    default:
                        SpannableString Message = SpannableString.valueOf(message);
                        if (SearchQuery != null) {
                            int index = TextUtils.indexOf(message.toLowerCase(), SearchQuery.toLowerCase());
                            if (index >= 0) {
                                Message.setSpan(new ForegroundColorSpan(AppHelper.getColor(mActivity, R.color.colorSpanSearch)), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                                Message.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                            }
                            mMessagesViewHolder.message.setText(Message, TextView.BufferType.SPANNABLE);
                            mMessagesViewHolder.message.setTextSize(PreferenceSettingsManager.getMessage_font_size(mActivity));
                        } else {
                            mMessagesViewHolder.message.setText(message, TextView.BufferType.NORMAL);
                            mMessagesViewHolder.message.setTextSize(PreferenceSettingsManager.getMessage_font_size(mActivity));
                        }

                        break;
                }


                if (messagesModel.isFileUpload()) {

                    if (messagesModel.getImageFile() != null && !messagesModel.getImageFile().equals("null")) {
                        mMessagesViewHolder.imageLayout.setVisibility(View.VISIBLE);
                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.showImageFile();
                            mMessagesViewHolder.setImageFile(messagesModel);
                        } else {
                            mMessagesViewHolder.showImageFile();
                            mMessagesViewHolder.setImageFile(messagesModel);
                        }

                    } else {

                        mMessagesViewHolder.imageLayout.setVisibility(View.GONE);
                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.mProgressUploadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadImageInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelUploadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.retryUploadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.hideImageFile();
                        } else {
                            mMessagesViewHolder.mProgressDownloadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadImageInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelDownloadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.downloadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.hideImageFile();
                        }
                    }

                    if (messagesModel.getVideoFile() != null && !messagesModel.getVideoFile().equals("null")
                            && messagesModel.getVideoThumbnailFile() != null && !messagesModel.getVideoThumbnailFile().equals("null")) {
                        mMessagesViewHolder.videoLayout.setVisibility(View.VISIBLE);
                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.showVideoThumbnailFile();
                            mMessagesViewHolder.setVideoThumbnailFile(messagesModel);
                            mMessagesViewHolder.setVideoTotalDuration(messagesModel);
                        } else {
                            mMessagesViewHolder.showVideoThumbnailFile();
                            mMessagesViewHolder.setVideoThumbnailFile(messagesModel);
                            mMessagesViewHolder.setVideoTotalDuration(messagesModel);
                        }

                    } else {
                        mMessagesViewHolder.videoLayout.setVisibility(View.GONE);
                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.mProgressUploadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadVideoInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelUploadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.retryUploadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.hideVideoThumbnailFile();
                        } else {
                            mMessagesViewHolder.mProgressDownloadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadVideoInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelDownloadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.downloadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.hideVideoThumbnailFile();
                        }
                    }


                    if (messagesModel.getAudioFile() != null && !messagesModel.getAudioFile().equals("null")) {
                        mMessagesViewHolder.audioLayout.setVisibility(View.VISIBLE);
                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            if (SenderInfo != null)
                                mMessagesViewHolder.setUserAudioImage(SenderInfo.getImage(), SenderInfo.getId());
                            else
                                mMessagesViewHolder.setUnregistredUserAudioImage();
                            mMessagesViewHolder.setAudioTotalDurationAudio(messagesModel);
                            mMessagesViewHolder.mProgressUploadAudioInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelUploadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.retryUploadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.playBtnAudio.setVisibility(View.VISIBLE);
                            mMessagesViewHolder.audioSeekBar.setEnabled(true);

                        } else {

                            if (SenderInfo != null)
                                mMessagesViewHolder.setUserAudioImage(SenderInfo.getImage(), SenderInfo.getId());
                            else
                                mMessagesViewHolder.setUnregistredUserAudioImage();
                            mMessagesViewHolder.setAudioTotalDurationAudio(messagesModel);
                            if (messagesModel.isFileDownLoad()) {
                                mMessagesViewHolder.retryDownloadAudio.setVisibility(View.GONE);
                                mMessagesViewHolder.mProgressDownloadAudio.setVisibility(View.GONE);
                                mMessagesViewHolder.mProgressDownloadAudioInitial.setVisibility(View.GONE);
                                mMessagesViewHolder.cancelDownloadAudio.setVisibility(View.GONE);
                                mMessagesViewHolder.playBtnAudio.setVisibility(View.VISIBLE);
                                mMessagesViewHolder.audioSeekBar.setEnabled(true);
                            } else {
                                mMessagesViewHolder.retryDownloadAudio.setVisibility(View.VISIBLE);
                                mMessagesViewHolder.mProgressDownloadAudio.setVisibility(View.GONE);
                                mMessagesViewHolder.mProgressDownloadAudioInitial.setVisibility(View.GONE);
                                mMessagesViewHolder.cancelDownloadAudio.setVisibility(View.GONE);
                                mMessagesViewHolder.playBtnAudio.setVisibility(View.GONE);
                                mMessagesViewHolder.audioSeekBar.setEnabled(false);
                            }
                        }

                    } else {
                        mMessagesViewHolder.audioLayout.setVisibility(View.GONE);
                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.mProgressUploadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadAudioInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelUploadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.retryUploadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.playBtnAudio.setVisibility(View.GONE);
                        } else {
                            mMessagesViewHolder.mProgressDownloadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadAudioInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelDownloadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.retryDownloadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.playBtnAudio.setVisibility(View.GONE);

                        }
                    }
                    if (messagesModel.getDocumentFile() != null && !messagesModel.getDocumentFile().equals("null")) {
                        mMessagesViewHolder.documentLayout.setVisibility(View.VISIBLE);
                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.setDocumentTitle(messagesModel);
                            mMessagesViewHolder.mProgressUploadDocumentInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadDocument.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelUploadDocument.setVisibility(View.GONE);
                            mMessagesViewHolder.retryUploadDocument.setVisibility(View.GONE);

                        } else {
                            mMessagesViewHolder.setDocumentTitle(messagesModel);
                            if (messagesModel.isFileDownLoad()) {
                                mMessagesViewHolder.retryDownloadDocument.setVisibility(View.GONE);
                                mMessagesViewHolder.mProgressDownloadDocument.setVisibility(View.GONE);
                                mMessagesViewHolder.mProgressDownloadDocumentInitial.setVisibility(View.GONE);
                                mMessagesViewHolder.cancelDownloadDocument.setVisibility(View.GONE);
                                mMessagesViewHolder.documentImage.setVisibility(View.VISIBLE);
                            } else {
                                mMessagesViewHolder.retryDownloadDocument.setVisibility(View.VISIBLE);
                                mMessagesViewHolder.mProgressDownloadDocument.setVisibility(View.GONE);
                                mMessagesViewHolder.mProgressDownloadDocumentInitial.setVisibility(View.GONE);
                                mMessagesViewHolder.cancelDownloadDocument.setVisibility(View.GONE);
                            }
                        }

                    } else {

                        mMessagesViewHolder.documentLayout.setVisibility(View.GONE);
                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.mProgressUploadDocument.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadDocumentInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelUploadDocument.setVisibility(View.GONE);
                            mMessagesViewHolder.retryUploadDocument.setVisibility(View.GONE);
                        } else {
                            mMessagesViewHolder.mProgressDownloadDocument.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadDocumentInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelDownloadDocument.setVisibility(View.GONE);
                            mMessagesViewHolder.retryDownloadDocument.setVisibility(View.GONE);
                        }
                    }
                } else {
                    if (messagesModel.getImageFile() != null && !messagesModel.getImageFile().equals("null")) {

                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.imageLayout.setVisibility(View.VISIBLE);
                            mMessagesViewHolder.showImageFile();
                            mMessagesViewHolder.setImageFileOffline(messagesModel);
                            mMessagesViewHolder.mProgressUploadImageInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelUploadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.retryUploadImage.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(() -> mMessagesViewHolder.retryUploadImage.performClick(), 200);
                        } else {
                            mMessagesViewHolder.imageLayout.setVisibility(View.VISIBLE);
                            mMessagesViewHolder.showImageFile();
                            mMessagesViewHolder.setImageFileOffline(messagesModel);

                        }
                    } else {
                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.imageLayout.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadImageInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelUploadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.retryUploadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.hideImageFile();
                        } else {
                            mMessagesViewHolder.imageLayout.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadImageInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelDownloadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.downloadImage.setVisibility(View.GONE);
                            mMessagesViewHolder.hideImageFile();
                        }
                    }


                    if (messagesModel.getVideoFile() != null && !messagesModel.getVideoFile().equals("null")
                            && messagesModel.getVideoThumbnailFile() != null && !messagesModel.getVideoThumbnailFile().equals("null")) {

                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.videoLayout.setVisibility(View.VISIBLE);
                            mMessagesViewHolder.showVideoThumbnailFile();
                            mMessagesViewHolder.setVideoThumbnailFileOffline(messagesModel);
                            mMessagesViewHolder.setVideoTotalDuration(messagesModel);
                            mMessagesViewHolder.mProgressUploadVideoInitial.setVisibility(View.VISIBLE);
                            mMessagesViewHolder.cancelUploadVideo.setVisibility(View.VISIBLE);
                            mMessagesViewHolder.mProgressUploadVideoInitial.setIndeterminate(true);
                            new Handler().postDelayed(() -> mMessagesViewHolder.retryUploadVideo.performClick(), 200);
                        } else {
                            mMessagesViewHolder.videoLayout.setVisibility(View.VISIBLE);
                            mMessagesViewHolder.showVideoThumbnailFile();
                            mMessagesViewHolder.setVideoThumbnailFileOffline(messagesModel);

                        }
                    } else {
                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.videoLayout.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadVideoInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelUploadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.retryUploadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.setVideoTotalDuration(messagesModel);
                            mMessagesViewHolder.hideVideoThumbnailFile();
                        } else {
                            mMessagesViewHolder.videoLayout.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadVideoInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelDownloadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.downloadVideo.setVisibility(View.GONE);
                            mMessagesViewHolder.hideVideoThumbnailFile();
                        }
                    }
                    if (messagesModel.getAudioFile() != null && !messagesModel.getAudioFile().equals("null")) {

                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.audioLayout.setVisibility(View.VISIBLE);
                            if (SenderInfo != null)
                                mMessagesViewHolder.setUserAudioImage(SenderInfo.getImage(), SenderInfo.getId());
                            else
                                mMessagesViewHolder.setUnregistredUserAudioImage();
                            mMessagesViewHolder.setAudioTotalDurationAudio(messagesModel);
                            mMessagesViewHolder.retryUploadAudio.setVisibility(View.VISIBLE);
                            mMessagesViewHolder.audioSeekBar.setEnabled(false);
                            new Handler().postDelayed(() -> mMessagesViewHolder.retryUploadAudioButton.performClick(), 200);
                        } else {
                            mMessagesViewHolder.audioLayout.setVisibility(View.VISIBLE);
                            mMessagesViewHolder.setUserAudioImage(RecipientInfo.getImage(), RecipientInfo.getId());
                        }
                    } else {

                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.audioLayout.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadAudioInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelUploadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.retryUploadAudio.setVisibility(View.GONE);
                        } else {
                            mMessagesViewHolder.audioLayout.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadAudioInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelDownloadAudio.setVisibility(View.GONE);
                            mMessagesViewHolder.retryDownloadAudio.setVisibility(View.GONE);
                        }
                    }
                    if (messagesModel.getDocumentFile() != null && !messagesModel.getDocumentFile().equals("null")) {

                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.documentLayout.setVisibility(View.VISIBLE);
                            mMessagesViewHolder.setDocumentTitle(messagesModel);
                            mMessagesViewHolder.retryUploadDocument.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(() -> mMessagesViewHolder.retryUploadDocumentButton.performClick(), 200);
                        } else {
                            mMessagesViewHolder.documentLayout.setVisibility(View.VISIBLE);
                            mMessagesViewHolder.setDocumentTitle(messagesModel);
                        }
                    } else {


                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            mMessagesViewHolder.documentLayout.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadDocument.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressUploadDocumentInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelUploadDocument.setVisibility(View.GONE);
                            mMessagesViewHolder.retryUploadDocument.setVisibility(View.GONE);
                        } else {
                            mMessagesViewHolder.mProgressDownloadDocument.setVisibility(View.GONE);
                            mMessagesViewHolder.mProgressDownloadDocumentInitial.setVisibility(View.GONE);
                            mMessagesViewHolder.cancelDownloadDocument.setVisibility(View.GONE);
                            mMessagesViewHolder.retryDownloadDocument.setVisibility(View.GONE);
                        }
                    }

                }

                try {

                    mMessagesViewHolder.setDate(messagesModel.getDate());
                    if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                        mMessagesViewHolder.showSent(messagesModel.getStatus());
                    } else {
                        mMessagesViewHolder.hideSent();
                    }
                } catch (Exception e) {
                    AppHelper.LogCat("Exception time " + e.getMessage());
                }

                if (hasRemoteFile && !messagesModel.isFileDownLoad()) {
                    queueAutoDownload(messagesModel, mMessagesViewHolder);
                }

            }
        }


        holder.itemView.setActivated(selectedItems.get(position, false));
    }


    @Override
    public int getItemCount() {
        return mMessagesModel != null ? mMessagesModel.size() : 0;
    }


    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
        } else {
            selectedItems.put(pos, true);
            if (!isActivated)
                isActivated = true;
        }
        notifyItemChanged(pos);
    }

    public void clearSelections() {
        selectedItems.clear();
        if (isActivated)
            isActivated = false;
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selectedItems.size());
        int arraySize = selectedItems.size();
        for (int i = 0; i < arraySize; i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }


    public MessagesModel getItem(int position) {
        return mMessagesModel.get(position);
    }


    public void removeMessageItem(int position) {
        if (position != 0) {
            try {
                mMessagesModel.remove(position);
                notifyItemRemoved(position);
            } catch (Exception e) {
                AppHelper.LogCat(e);
            }
        }
    }

    public void updateStatusMessageItem(int messageId) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        int arraySize = mMessagesModel.size();
        for (int i = 0; i < arraySize; i++) {
            MessagesModel model = mMessagesModel.get(i);
            if (model.isValid() && messageId == model.getId()) {
                MessagesModel messagesModel = realm.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                changeItemAtPosition(i, messagesModel);
                break;
            }


        }
        realm.close();
    }


    private void changeItemAtPosition(int position, MessagesModel messagesModel) {
        mMessagesModel.set(position, messagesModel);
        notifyItemChanged(position);
        notifyItemRangeChanged(position, mMessagesModel.size());
        isStatusUpdated = true;
    }

    private boolean hasMediaStorageAccess() {
        return activeActivity != null;
    }

    private void requestMediaStorageAccess() {
        // Downloads are written to app-specific storage and need no media permission.
    }

    private void queueAutoDownload(MessagesModel messagesModel, MessagesViewHolder holder) {
        if (messagesModel == null || messagesModel.isFileDownLoad() || autoDownloadQueue.contains(messagesModel.getId())) {
            return;
        }
        if (!hasMediaStorageAccess()) {
            requestMediaStorageAccess();
            return;
        }
        autoDownloadQueue.add(messagesModel.getId());
        new Handler().postDelayed(() -> holder.downloadFile(messagesModel), 300);
    }


    public class MessagesViewHolder extends RecyclerView.ViewHolder implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

        private Activity mActivity;
        @BindView(R.id.message_text)
        EmojiconTextView message;
        @BindView(R.id.date_message)
        TextView date;
        @BindView(R.id.sender_name)
        TextView senderName;
        @BindView(R.id.status_messages)
        ImageView statusMessages;

        //var for links

        @BindView(R.id.link_preview)
        LinearLayout linkPreview;

        @BindView(R.id.image_preview)
        ImageView imagePreview;

        @BindView(R.id.title_link)
        TextView titleLink;

        @BindView(R.id.url)
        TextView urlLink;

        @BindView(R.id.description)
        TextView descriptionLink;


        //var for  images
        @BindView(R.id.image_layout)
        FrameLayout imageLayout;
        @BindView(R.id.image_file)
        ImageView imageFile;
        @BindView(R.id.progress_bar_upload_image)
        ProgressBar mProgressUploadImage;
        @BindView(R.id.progress_bar_upload_image_init)
        ProgressBar mProgressUploadImageInitial;
        @BindView(R.id.cancel_upload_image)
        ImageButton cancelUploadImage;
        @BindView(R.id.retry_upload_image)
        LinearLayout retryUploadImage;
        @BindView(R.id.progress_bar_download_image)
        ProgressBar mProgressDownloadImage;
        @BindView(R.id.progress_bar_download_image_init)
        ProgressBar mProgressDownloadImageInitial;
        @BindView(R.id.cancel_download_image)
        ImageButton cancelDownloadImage;
        @BindView(R.id.download_image)
        LinearLayout downloadImage;
        @BindView(R.id.file_size_image)
        TextView fileSizeImage;

        //var for upload videos
        @BindView(R.id.video_layout)
        FrameLayout videoLayout;
        @BindView(R.id.video_thumbnail)
        ImageView videoThumbnailFile;
        @BindView(R.id.play_btn_video)
        ImageButton playBtnVideo;
        @BindView(R.id.progress_bar_upload_video)
        ProgressBar mProgressUploadVideo;
        @BindView(R.id.progress_bar_upload_video_init)
        ProgressBar mProgressUploadVideoInitial;
        @BindView(R.id.cancel_upload_video)
        ImageButton cancelUploadVideo;
        @BindView(R.id.retry_upload_video)
        LinearLayout retryUploadVideo;
        @BindView(R.id.progress_bar_download_video)
        ProgressBar mProgressDownloadVideo;
        @BindView(R.id.progress_bar_download_video_init)
        ProgressBar mProgressDownloadVideoInitial;
        @BindView(R.id.cancel_download_video)
        ImageButton cancelDownloadVideo;
        @BindView(R.id.download_video)
        LinearLayout downloadVideo;
        @BindView(R.id.file_size_video)
        TextView fileSizeVideo;

        @BindView(R.id.video_total_duration)
        TextView videoTotalDuration;

        //var for audio
        @BindView(R.id.audio_layout)
        LinearLayout audioLayout;
        @BindView(R.id.audio_user_image)
        ImageView userAudioImage;
        @BindView(R.id.progress_bar_upload_audio)
        ProgressBar mProgressUploadAudio;
        @BindView(R.id.progress_bar_upload_audio_init)
        ProgressBar mProgressUploadAudioInitial;
        @BindView(R.id.cancel_upload_audio)
        ImageButton cancelUploadAudio;
        @BindView(R.id.retry_upload_audio)
        LinearLayout retryUploadAudio;

        @BindView(R.id.retry_upload_audio_button)
        ImageButton retryUploadAudioButton;
        @BindView(R.id.progress_bar_download_audio)
        ProgressBar mProgressDownloadAudio;
        @BindView(R.id.progress_bar_download_audio_init)
        ProgressBar mProgressDownloadAudioInitial;
        @BindView(R.id.cancel_download_audio)
        ImageButton cancelDownloadAudio;
        @BindView(R.id.retry_download_audio)
        LinearLayout retryDownloadAudio;
        @BindView(R.id.retry_download_audio_button)
        ImageButton retryDownloadAudioButton;
        @BindView(R.id.play_btn_audio)
        ImageButton playBtnAudio;
        @BindView(R.id.pause_btn_audio)
        ImageButton pauseBtnAudio;
        @BindView(R.id.audio_progress_bar)
        SeekBar audioSeekBar;
        @BindView(R.id.audio_current_duration)
        TextView audioCurrentDurationAudio;
        @BindView(R.id.audio_total_duration)
        TextView audioTotalDurationAudio;

        //for documents
        @BindView(R.id.document_layout)
        LinearLayout documentLayout;
        @BindView(R.id.progress_bar_upload_document)
        ProgressBar mProgressUploadDocument;
        @BindView(R.id.progress_bar_upload_document_init)
        ProgressBar mProgressUploadDocumentInitial;
        @BindView(R.id.cancel_upload_document)
        ImageButton cancelUploadDocument;
        @BindView(R.id.retry_upload_document)
        LinearLayout retryUploadDocument;
        @BindView(R.id.retry_upload_document_button)
        ImageButton retryUploadDocumentButton;
        @BindView(R.id.progress_bar_download_document)
        ProgressBar mProgressDownloadDocument;
        @BindView(R.id.progress_bar_download_document_init)
        ProgressBar mProgressDownloadDocumentInitial;
        @BindView(R.id.cancel_download_document)
        ImageButton cancelDownloadDocument;
        @BindView(R.id.retry_download_document)
        LinearLayout retryDownloadDocument;
        @BindView(R.id.retry_download_document_button)
        ImageButton retryDownloadDocumentButton;
        @BindView(R.id.document_title)
        TextView documentTitle;
        @BindView(R.id.document_image)
        ImageButton documentImage;

        @BindView(R.id.document_size)
        TextView fileSizeDocument;

        UploadCallbacks mUploadCallbacks;
        DownloadCallbacks mDownloadCallbacks;
        AudioCallbacks mAudioCallbacks;
        UploadFilesHelper mUploadFilesHelper;
/*
        LinkPreviewCallback linkPreviewCallback;
        TextCrawler textCrawler;*/

        MessagesViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            senderName.setSelected(true);
            mActivity = (Activity) itemView.getContext();
            setTypeFaces();
            //for image upload
            setupProgressBarUploadImage();
            //for video upload
            setupProgressBarUploadVideo();
            //for audio upload
            setupProgressBarUploadAudio();
            //for document upload
            setupProgressBarUploadDocument();

            cancelDownloadImage.setOnClickListener(this);
            downloadImage.setOnClickListener(this);
            cancelUploadImage.setOnClickListener(this);
            retryUploadImage.setOnClickListener(this);
            imageLayout.setOnClickListener(this);

            cancelDownloadVideo.setOnClickListener(this);
            downloadVideo.setOnClickListener(this);
            cancelUploadVideo.setOnClickListener(this);
            retryUploadVideo.setOnClickListener(this);
            videoLayout.setOnClickListener(this);
            playBtnVideo.setOnClickListener(this);

            cancelDownloadAudio.setOnClickListener(this);
            retryDownloadAudioButton.setOnClickListener(this);
            cancelUploadAudio.setOnClickListener(this);
            retryUploadAudioButton.setOnClickListener(this);
            audioSeekBar.setOnSeekBarChangeListener(this);
            playBtnAudio.setOnClickListener(this);
            pauseBtnAudio.setOnClickListener(this);

            cancelDownloadDocument.setOnClickListener(this);
            retryDownloadDocumentButton.setOnClickListener(this);
            cancelUploadDocument.setOnClickListener(this);
            retryUploadDocumentButton.setOnClickListener(this);
            documentTitle.setOnClickListener(this);
            itemView.setOnClickListener(view -> {

               /* if (messagesModel.isGroup()) {
                    AppHelper.LogCat("This is a group you cannot delete there message now");
                } else {*/
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_ITEM_IS_ACTIVATED_MESSAGES, view));
                //}
            });

            mUploadCallbacks = new UploadCallbacks() {
                @Override
                public void onUpdate(int percentage, String type) {
                    switch (type) {
                        case "image":
                            mProgressUploadImage.setVisibility(View.VISIBLE);
                            cancelUploadImage.setVisibility(View.VISIBLE);
                            mProgressUploadImageInitial.setVisibility(View.GONE);
                            mProgressUploadImage.setIndeterminate(false);
                            mProgressUploadImage.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
                            mProgressUploadImage.setProgress(percentage);
                            break;
                        case "video":
                            mProgressUploadVideo.setVisibility(View.VISIBLE);
                            cancelUploadVideo.setVisibility(View.VISIBLE);
                            mProgressUploadVideoInitial.setVisibility(View.GONE);
                            mProgressUploadVideo.setIndeterminate(false);
                            mProgressUploadVideo.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
                            mProgressUploadVideo.setProgress(percentage);
                            break;
                        case "audio":
                            mProgressUploadAudio.setVisibility(View.VISIBLE);
                            cancelUploadAudio.setVisibility(View.VISIBLE);
                            mProgressUploadAudioInitial.setVisibility(View.GONE);
                            mProgressUploadAudio.setIndeterminate(false);
                            mProgressUploadAudio.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
                            mProgressUploadAudio.setProgress(percentage);
                            break;
                        case "document":
                            mProgressUploadDocument.setVisibility(View.VISIBLE);
                            cancelUploadDocument.setVisibility(View.VISIBLE);
                            mProgressUploadDocumentInitial.setVisibility(View.GONE);
                            mProgressUploadDocument.setIndeterminate(false);
                            mProgressUploadDocument.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);

                            break;
                    }
                }

                @Override
                public void onError(String type) {
                    AppHelper.LogCat("on error " + type);
                    switch (type) {
                        case "image":
                            mProgressUploadImage.setVisibility(View.GONE);
                            mProgressUploadImageInitial.setVisibility(View.GONE);
                            cancelUploadImage.setVisibility(View.GONE);
                            retryUploadImage.setVisibility(View.VISIBLE);
                            break;
                        case "video":
                            mProgressUploadVideo.setVisibility(View.GONE);
                            mProgressUploadVideoInitial.setVisibility(View.GONE);
                            cancelUploadVideo.setVisibility(View.GONE);
                            retryUploadVideo.setVisibility(View.VISIBLE);
                            break;
                        case "audio":
                            mProgressUploadAudio.setVisibility(View.GONE);
                            mProgressUploadAudioInitial.setVisibility(View.GONE);
                            cancelUploadAudio.setVisibility(View.GONE);
                            playBtnAudio.setVisibility(View.GONE);
                            pauseBtnAudio.setVisibility(View.GONE);
                            audioSeekBar.setEnabled(false);
                            retryUploadAudio.setVisibility(View.VISIBLE);
                            break;
                        case "document":
                            mProgressUploadDocument.setVisibility(View.GONE);
                            mProgressUploadDocumentInitial.setVisibility(View.GONE);
                            cancelUploadDocument.setVisibility(View.GONE);
                            documentImage.setVisibility(View.GONE);
                            retryUploadDocument.setVisibility(View.VISIBLE);
                            break;
                    }
                }

                @Override
                public void onFinish(String type, MessagesModel messagesModel) {
                    switch (type) {
                        case "image":
                            mProgressUploadImage.setVisibility(View.GONE);
                            mProgressUploadImageInitial.setVisibility(View.GONE);
                            cancelUploadImage.setVisibility(View.GONE);
                            retryUploadImage.setVisibility(View.GONE);
                            updateStatusMessageItem(messagesModel.getId());
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_UPLOAD_MESSAGE_FILES, messagesModel));
                            break;
                        case "video":
                            mProgressUploadVideo.setVisibility(View.GONE);
                            mProgressUploadVideoInitial.setVisibility(View.GONE);
                            cancelUploadVideo.setVisibility(View.GONE);
                            retryUploadVideo.setVisibility(View.GONE);
                            setVideoThumbnailFile(messagesModel);
                            updateStatusMessageItem(messagesModel.getId());
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_UPLOAD_MESSAGE_FILES, messagesModel));
                            break;
                        case "audio":
                            mProgressUploadAudio.setVisibility(View.GONE);
                            mProgressUploadAudioInitial.setVisibility(View.GONE);
                            cancelUploadAudio.setVisibility(View.GONE);
                            retryUploadAudio.setVisibility(View.GONE);
                            playBtnAudio.setVisibility(View.VISIBLE);
                            audioSeekBar.setEnabled(true);
                            setAudioTotalDurationAudio(messagesModel);
                            updateStatusMessageItem(messagesModel.getId());
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_UPLOAD_MESSAGE_FILES, messagesModel));
                            break;
                        case "document":
                            mProgressUploadDocument.setVisibility(View.GONE);
                            mProgressUploadDocumentInitial.setVisibility(View.GONE);
                            cancelUploadDocument.setVisibility(View.GONE);
                            retryUploadDocument.setVisibility(View.GONE);
                            documentImage.setVisibility(View.VISIBLE);
                            updateStatusMessageItem(messagesModel.getId());
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_UPLOAD_MESSAGE_FILES, messagesModel));
                            break;
                    }
                }

            };
            mDownloadCallbacks = new DownloadCallbacks() {
                @Override
                public void onUpdate(int percentage, String type) {
                    switch (type) {
                        case "image":
                            mProgressDownloadImageInitial.setVisibility(View.GONE);
                            mProgressDownloadImage.setVisibility(View.VISIBLE);
                            cancelDownloadImage.setVisibility(View.VISIBLE);
                            mProgressDownloadImage.setIndeterminate(false);
                            mProgressDownloadImage.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
                            mProgressDownloadImage.setProgress(percentage);
                            break;
                        case "video":
                            mProgressDownloadVideoInitial.setVisibility(View.GONE);
                            mProgressDownloadVideo.setVisibility(View.VISIBLE);
                            cancelDownloadVideo.setVisibility(View.VISIBLE);
                            mProgressDownloadVideo.setIndeterminate(false);
                            mProgressDownloadVideo.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
                            mProgressDownloadVideo.setProgress(percentage);
                            break;
                        case "audio":
                            mProgressDownloadAudioInitial.setVisibility(View.GONE);
                            mProgressDownloadAudio.setVisibility(View.VISIBLE);
                            cancelDownloadAudio.setVisibility(View.VISIBLE);
                            mProgressDownloadAudio.setIndeterminate(false);
                            mProgressDownloadAudio.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
                            mProgressDownloadAudio.setProgress(percentage);
                            break;
                        case "document":
                            mProgressDownloadDocumentInitial.setVisibility(View.GONE);
                            mProgressDownloadDocument.setVisibility(View.VISIBLE);
                            cancelDownloadDocument.setVisibility(View.VISIBLE);
                            mProgressDownloadDocument.setIndeterminate(false);
                            mProgressDownloadDocument.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
                            mProgressDownloadDocument.setProgress(percentage);
                            break;
                    }


                }

                @Override
                public void onError(String type) {
                    switch (type) {
                        case "image":
                            mProgressDownloadImage.setVisibility(View.GONE);
                            mProgressDownloadImageInitial.setVisibility(View.GONE);
                            cancelDownloadImage.setVisibility(View.GONE);
                            downloadImage.setVisibility(View.VISIBLE);
                            break;
                        case "video":
                            mProgressDownloadVideo.setVisibility(View.GONE);
                            mProgressDownloadVideoInitial.setVisibility(View.GONE);
                            cancelDownloadVideo.setVisibility(View.GONE);
                            downloadVideo.setVisibility(View.VISIBLE);
                            break;
                        case "audio":
                            mProgressDownloadAudio.setVisibility(View.GONE);
                            mProgressDownloadAudioInitial.setVisibility(View.GONE);
                            cancelDownloadAudio.setVisibility(View.GONE);
                            retryDownloadAudio.setVisibility(View.VISIBLE);
                            break;
                        case "document":
                            mProgressDownloadDocument.setVisibility(View.GONE);
                            mProgressDownloadDocumentInitial.setVisibility(View.GONE);
                            cancelDownloadDocument.setVisibility(View.GONE);
                            retryDownloadDocument.setVisibility(View.VISIBLE);
                            break;
                    }
                }

                @Override
                public void onFinish(String type, MessagesModel messagesModel) {
                    switch (type) {
                        case "image":
                            mProgressDownloadImage.setVisibility(View.GONE);
                            mProgressDownloadImageInitial.setVisibility(View.GONE);
                            cancelDownloadImage.setVisibility(View.GONE);
                            downloadImage.setVisibility(View.GONE);
                            showImageFile();
                            setImageFile(messagesModel);
                            updateStatusMessageItem(messagesModel.getId());
                            break;
                        case "video":
                            mProgressDownloadVideo.setVisibility(View.GONE);
                            mProgressDownloadVideoInitial.setVisibility(View.GONE);
                            cancelDownloadVideo.setVisibility(View.GONE);
                            downloadVideo.setVisibility(View.GONE);
                            showVideoThumbnailFile();
                            setVideoThumbnailFile(messagesModel);
                            setVideoTotalDuration(messagesModel);
                            updateStatusMessageItem(messagesModel.getId());
                            break;
                        case "audio":
                            mProgressDownloadAudio.setVisibility(View.GONE);
                            mProgressDownloadAudioInitial.setVisibility(View.GONE);
                            cancelDownloadAudio.setVisibility(View.GONE);
                            retryDownloadAudio.setVisibility(View.GONE);
                            cancelDownloadAudio.setVisibility(View.GONE);
                            playBtnAudio.setVisibility(View.VISIBLE);
                            audioSeekBar.setEnabled(true);
                            updateStatusMessageItem(messagesModel.getId());
                            break;
                        case "document":
                            mProgressDownloadDocument.setVisibility(View.GONE);
                            mProgressDownloadDocumentInitial.setVisibility(View.GONE);
                            cancelDownloadDocument.setVisibility(View.GONE);
                            retryDownloadDocument.setVisibility(View.GONE);
                            documentImage.setVisibility(View.VISIBLE);
                            updateStatusMessageItem(messagesModel.getId());
                            break;
                    }
                }
            };
            mAudioCallbacks = new AudioCallbacks() {
                @Override
                public void onUpdate(int percentage) {
                    audioSeekBar.setProgress(percentage);
                    if (percentage == 100)
                        mAudioCallbacks.onStop();
                }

                @Override
                public void onPause() {
                    AppHelper.LogCat("on pause audio");

                }

                @Override
                public void onStop() {
                    AppHelper.LogCat("on stop audio");
                    stopPlayingAudio();
                }

            };
            //for urls to be clickable
            Linkify.addLinks(message, Linkify.WEB_URLS);

           /* textCrawler = new TextCrawler();
            linkPreviewCallback = new LinkPreviewCallback() {
                @Override
                public void onPre() {
                    AppHelper.LogCat("onPre");
                }

                @Override
                public void onPos(SourceContent sourceContent, boolean b) {
                    try {
                        MessagesModel messagesModel = mMessagesModel.get(getAdapterPosition());
                        if (messagesModel.isValid()) {
                            if (messagesModel.getMessage() != null) {
                                if (UtilsString.checkForUrls(messagesModel.getMessage())) {

                                    String url = UtilsString.getUrl(messagesModel.getMessage());
                                    if (url != null)
                                        if (!url.startsWith("http://")) {
                                            if (!url.startsWith("https://")) {
                                                url = (new StringBuilder()).append("http://").append(url).toString();
                                            }
                                        }
                                    if (UtilsString.isValidUrl(url)) {
                                        linkPreview.setVisibility(View.VISIBLE);
                                    } else {
                                        linkPreview.setVisibility(View.GONE);
                                    }

                                    //  imagePreview

                                    titleLink.setText(sourceContent.getTitle());
                                    urlLink.setText(sourceContent.getUrl());

                                    if (sourceContent.getDescription().length() > 80) {
                                        descriptionLink.setText(sourceContent.getDescription().substring(0, 80) + "... " + "");
                                    } else {
                                        descriptionLink.setText(sourceContent.getDescription());
                                    }


                                    if (sourceContent.getImages().size() > 0) {
                                        Target target = new BitmapImageViewTarget(imagePreview) {
                                            @Override
                                            public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                                                super.onResourceReady(bitmap, anim);
                                                imagePreview.setImageBitmap(bitmap);
                                            }

                                            @Override
                                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                                super.onLoadFailed(e, errorDrawable);
                                                imagePreview.setImageDrawable(errorDrawable);
                                            }

                                            @Override
                                            public void onLoadStarted(Drawable placeHolderDrawable) {
                                                super.onLoadStarted(placeHolderDrawable);
                                                imagePreview.setImageDrawable(placeHolderDrawable);
                                            }
                                        };

                                        WhatsCloneImageLoader.loadSimpleImage(context, sourceContent.getImages().get(0), target, null, AppConstants.MESSAGE_IMAGE_SIZE);

                                    }
                                } else {
                                    linkPreview.setVisibility(View.GONE);
                                }
                            }
                        }

                    } catch (Exception e) {
                        AppHelper.LogCat(e.getMessage());
                    }

                }


            };*/

        }


        private void setTypeFaces() {
            if (AppConstants.ENABLE_FONTS_TYPES) {
                fileSizeDocument.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                documentTitle.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                audioTotalDurationAudio.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                audioCurrentDurationAudio.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                videoTotalDuration.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                fileSizeVideo.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                fileSizeImage.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                descriptionLink.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                urlLink.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                titleLink.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                date.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                senderName.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                message.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
            }
        }

        void setAudioTotalDurationAudio(MessagesModel messagesModel) {
            String duration = messagesModel.getDuration();
            stopPlayingAudio();
            audioCurrentDurationAudio.setVisibility(View.GONE);
            if (duration == null || duration.isEmpty()) {
                audioTotalDurationAudio.setVisibility(View.GONE);
                return;
            }
            audioTotalDurationAudio.setVisibility(View.VISIBLE);
            try {
                long timeInMilliSecond = Long.parseLong(duration);
                mActivity.runOnUiThread(() -> setTotalTime(timeInMilliSecond));
            } catch (Exception e) {
                AppHelper.LogCat("Exception total duration " + e.getMessage());
                audioTotalDurationAudio.setVisibility(View.GONE);
            }
        }


        void setTotalTime(long totalTime) {
            audioTotalDurationAudio.setText(UtilsTime.getFileTime(totalTime));
        }

         void setVideoTotalDuration(MessagesModel messagesModel) {
             String duration = messagesModel.getDuration();
             if (duration == null || duration.isEmpty()) {
                 videoTotalDuration.setVisibility(View.GONE);
                 return;
             }
             videoTotalDuration.setVisibility(View.VISIBLE);
             try {
                 long timeInMilliSecond = Long.parseLong(duration);
                 setVideoTotalTime(timeInMilliSecond);
             } catch (Exception e) {
                 AppHelper.LogCat("Exception total duration " + e.getMessage());
                 videoTotalDuration.setVisibility(View.GONE);
             }
         }

        void setVideoTotalTime(long totalTime) {
            videoTotalDuration.setText(UtilsTime.getFileTime(totalTime));
        }

        void setImageFileOffline(MessagesModel messagesModel) {
            String ImageUrl = messagesModel.getImageFile();
            int messageId = messagesModel.getId();
            File file = new File(ImageUrl);

            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_BEFORE);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    if (bitmap != null) {
                        imageFile.setImageBitmap(bitmap);
                    } else {
                        thumbnailRequestMap.put(messagesModel.getId(), Drawable.createFromPath(ImageUrl));
                        Glide.with(mActivity)
                                .load(file)
                                .asBitmap()
                                .centerCrop()
                                .override(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                .into(imageFile);
                        ImageLoader.DownloadOfflineImage(memoryCache, file, ImageUrl, mActivity, messagesModel.getId(), AppConstants.USER, AppConstants.ROW_MESSAGES_BEFORE);
                    }
                }
            }.execute();

        }

        void setVideoThumbnailFileOffline(MessagesModel messagesModel) {
            AppHelper.LogCat("setVideoThumbnailFileOffline");
            String ImageUrl = messagesModel.getVideoThumbnailFile();
            int messageId = messagesModel.getId();
            File file = new File(ImageUrl);

            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_BEFORE);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    if (bitmap != null) {
                        imageFile.setImageBitmap(bitmap);
                    } else {
                        thumbnailRequestMap.put(messagesModel.getId(), Drawable.createFromPath(ImageUrl));
                        Glide.with(mActivity)
                                .load(file)
                                .asBitmap()
                                .centerCrop()
                                .override(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                .into(videoThumbnailFile);
                        ImageLoader.DownloadOfflineImage(memoryCache, file, ImageUrl, mActivity, messagesModel.getId(), AppConstants.USER, AppConstants.ROW_MESSAGES_BEFORE);
                    }
                }
            }.execute();


        }

        void setUnregistredUserAudioImage() {
            if (AppHelper.hasImage(userAudioImage)) return;
            TextDrawable drawable = textDrawable();
            Glide.with(mActivity.getApplicationContext())
                    .load(drawable)
                    .asBitmap()
                    .centerCrop()
                    .transform(new CropCircleTransformation(mActivity))
                    .placeholder(drawable)
                    .error(drawable)
                    .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                    .into(userAudioImage);
        }

        void setUserAudioImage(String ImageUrl, int recipientId) {
            if (AppHelper.hasImage(userAudioImage)) return;
            TextDrawable drawable = textDrawable();
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, mActivity, recipientId, AppConstants.USER, AppConstants.ROW_PROFILE);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    if (bitmap != null) {
                        ImageLoader.SetBitmapImage(bitmap, userAudioImage);
                    } else {
                        Target target = new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                userAudioImage.setImageBitmap(bitmap);
                                ImageLoader.DownloadImage(memoryCache, EndPoints.ROWS_IMAGE_URL + ImageUrl, ImageUrl, mActivity, recipientId, AppConstants.USER, AppConstants.ROW_PROFILE);

                            }

                            @Override
                            public void onBitmapFailed(Drawable errorDrawable) {
                                userAudioImage.setImageDrawable(errorDrawable);
                            }

                            @Override
                            public void onPrepareLoad(Drawable placeHolderDrawable) {
                                userAudioImage.setImageDrawable(placeHolderDrawable);
                            }
                        };
                        Picasso.with(mActivity.getApplicationContext())
                                .load(EndPoints.ROWS_IMAGE_URL + ImageUrl)
                                .centerCrop()
                                .placeholder(drawable)
                                .transform(new jp.wasabeef.picasso.transformations.CropCircleTransformation())
                                .error(drawable)
                                .resize(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                                .into(target);
                    }
                }
            }.execute();


        }


        void setDocumentTitle(MessagesModel messagesModel) {

            String documentFile = messagesModel.getDocumentFile();
            int senderId = messagesModel.getSenderID();
            boolean isDownLoad = messagesModel.isFileDownLoad();
            if (senderId == PreferenceManager.getID(mActivity)) {
                File file;
                if (FilesManager.isFileDocumentsSentExists(mActivity, FilesManager.getDocument(documentFile))) {
                    file = FilesManager.getFileDocumentSent(mActivity, documentFile);
                    String document_title = file.getName();
                    documentTitle.setText(document_title);
                    documentImage.setVisibility(View.VISIBLE);
                } else {
                    if (messagesModel.isFileUpload())
                        documentImage.setVisibility(View.VISIBLE);
                    FilesManager.downloadFilesToDevice(mActivity, EndPoints.MESSAGE_DOCUMENT_DOWNLOAD_URL + documentFile, documentFile, AppConstants.SENT_DOCUMENTS);
                    documentTitle.setText(R.string.document);
                }

            } else {
                if (isDownLoad) {
                    documentImage.setVisibility(View.VISIBLE);
                    File file;
                    if (FilesManager.isFileDocumentsExists(mActivity, FilesManager.getDocument(documentFile))) {
                        file = FilesManager.getFileDocument(mActivity, documentFile);
                        String document_title = file.getName();
                        documentTitle.setText(document_title);
                    } else {
                        documentTitle.setText(R.string.document);
                    }
                } else {
                    retryDownloadDocument.setVisibility(View.VISIBLE);
                    documentTitle.setText(R.string.document);
                    try {
                        getFileSize(messagesModel.getFileSize(), "document");
                    } catch (Exception e) {
                        AppHelper.LogCat("Exception of file size");
                    }

                }
            }

        }

        void setImageFile(MessagesModel messagesModel) {

            String ImageUrl = messagesModel.getImageFile();
            int senderId = messagesModel.getSenderID();
            int messageId = messagesModel.getId();
            boolean isDownLoad = messagesModel.isFileDownLoad();
            if (senderId == PreferenceManager.getID(mActivity)) {

                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_AFTER);
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        if (bitmap != null) {
                            imageFile.setImageBitmap(bitmap);
                            if (!FilesManager.isFileImagesSentExists(mActivity, FilesManager.getImage(ImageUrl))) {
                                FilesManager.downloadMediaFile(mActivity, bitmap, ImageUrl, AppConstants.SENT_IMAGE);
                            }
                        } else {
                            if (FilesManager.isFileImagesSentExists(mActivity, FilesManager.getImage(ImageUrl))) {
                                Target target = new Target() {
                                    @Override
                                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                        imageFile.setImageBitmap(bitmap);
                                    }

                                    @Override
                                    public void onBitmapFailed(Drawable errorDrawable) {
                                        imageFile.setImageDrawable(errorDrawable);
                                    }

                                    @Override
                                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                                        imageFile.setImageDrawable(placeHolderDrawable);
                                    }
                                };
                                if (thumbnailRequestMap.size() != 0 && thumbnailRequestMap.get(messagesModel.getId()) != null) {
                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(FilesManager.getFileImageSent(mActivity, ImageUrl))
                                            .centerCrop()
                                            .placeholder(thumbnailRequestMap.get(messagesModel.getId()))
                                            .error(thumbnailRequestMap.get(messagesModel.getId()))
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                } else {

                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(FilesManager.getFileImageSent(mActivity, ImageUrl))
                                            .centerCrop()
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                }
                            } else {
                                Target target = new Target() {
                                    @Override
                                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                        imageFile.setImageBitmap(bitmap);
                                        FilesManager.downloadMediaFile(mActivity, bitmap, ImageUrl, AppConstants.SENT_IMAGE);
                                        ImageLoader.DownloadImage(memoryCache, EndPoints.MESSAGE_IMAGE_URL + ImageUrl, ImageUrl, mActivity, messagesModel.getId(), AppConstants.USER, AppConstants.ROW_MESSAGES_AFTER);

                                    }

                                    @Override
                                    public void onBitmapFailed(Drawable errorDrawable) {
                                        imageFile.setImageDrawable(errorDrawable);
                                    }

                                    @Override
                                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                                        imageFile.setImageDrawable(placeHolderDrawable);
                                    }
                                };
                                if (thumbnailRequestMap.size() != 0 && thumbnailRequestMap.get(messagesModel.getId()) != null) {


                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(EndPoints.MESSAGE_IMAGE_URL + ImageUrl)
                                            .centerCrop()
                                            .placeholder(thumbnailRequestMap.get(messagesModel.getId()))
                                            .error(thumbnailRequestMap.get(messagesModel.getId()))
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                } else {
                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(EndPoints.MESSAGE_IMAGE_URL + ImageUrl)
                                            .centerCrop()
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                }
                            }
                        }

                    }
                }.execute();


            } else {
                if (isDownLoad) {


                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_AFTER);
                        }

                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            super.onPostExecute(bitmap);

                            if (bitmap != null) {
                                imageFile.setImageBitmap(bitmap);
                            } else {
                                if (FilesManager.isFileImagesExists(mActivity, FilesManager.getImage(ImageUrl))) {
                                    Target target = new Target() {
                                        @Override
                                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                            imageFile.setImageBitmap(bitmap);
                                        }

                                        @Override
                                        public void onBitmapFailed(Drawable errorDrawable) {
                                            imageFile.setImageDrawable(errorDrawable);
                                        }

                                        @Override
                                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                                            imageFile.setImageDrawable(placeHolderDrawable);
                                        }
                                    };

                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(FilesManager.getFileImage(mActivity, ImageUrl))
                                            .centerCrop()
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                } else {

                                    Target target = new Target() {
                                        @Override
                                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                            imageFile.setImageBitmap(bitmap);
                                            ImageLoader.DownloadImage(memoryCache, EndPoints.MESSAGE_IMAGE_URL + ImageUrl, ImageUrl, mActivity, messagesModel.getId(), AppConstants.USER, AppConstants.ROW_MESSAGES_AFTER);

                                        }

                                        @Override
                                        public void onBitmapFailed(Drawable errorDrawable) {
                                            imageFile.setImageDrawable(errorDrawable);
                                        }

                                        @Override
                                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                                            imageFile.setImageDrawable(placeHolderDrawable);
                                        }
                                    };

                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(EndPoints.MESSAGE_IMAGE_URL + ImageUrl)
                                            .centerCrop()
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                }
                            }
                        }
                    }.execute();

                } else {
                    downloadImage.setVisibility(View.VISIBLE);
                    getFileSize(messagesModel.getFileSize(), "image");


                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            Bitmap bitmap = ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_BEFORE);
                            return ImageLoader.BlurBitmap(bitmap, mActivity);
                        }

                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            super.onPostExecute(bitmap);
                            if (bitmap != null) {
                                imageFile.setImageBitmap(bitmap);
                            } else {

                                Target target = new Target() {
                                    @Override
                                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                        imageFile.setImageBitmap(bitmap);

                                    }

                                    @Override
                                    public void onBitmapFailed(Drawable errorDrawable) {
                                        imageFile.setImageDrawable(errorDrawable);
                                    }

                                    @Override
                                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                                        imageFile.setImageDrawable(placeHolderDrawable);
                                    }
                                };

                                Picasso.with(mActivity.getApplicationContext())
                                        .load(EndPoints.MESSAGE_HOLDER_IMAGE_URL + ImageUrl)
                                        .transform(new BlurTransformationPicasso(mActivity.getApplicationContext()))
                                        .centerCrop()
                                        .resize(AppConstants.PRE_MESSAGE_IMAGE_SIZE, AppConstants.PRE_MESSAGE_IMAGE_SIZE)
                                        .into(target);
                            }
                        }
                    }.execute();

                }
            }


        }


        void setVideoThumbnailFile(MessagesModel messagesModel) {
            String ImageUrl = messagesModel.getVideoThumbnailFile();
            String videoUrl = messagesModel.getVideoFile();
            int senderId = messagesModel.getSenderID();
            int messageId = messagesModel.getId();
            boolean isDownLoad = messagesModel.isFileDownLoad();
            if (senderId == PreferenceManager.getID(mActivity)) {
                AppHelper.LogCat("PreferenceManager ");
                if (messagesModel.isFileUpload()) {
                    playBtnVideo.setVisibility(View.VISIBLE);


                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_AFTER);
                        }

                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            super.onPostExecute(bitmap);
                            if (bitmap != null) {
                                videoThumbnailFile.setImageBitmap(bitmap);
                            } else {
                                if (FilesManager.isFileImagesSentExists(mActivity, FilesManager.getImage(ImageUrl))) {
                                    Target target = new Target() {
                                        @Override
                                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                            videoThumbnailFile.setImageBitmap(bitmap);
                                        }

                                        @Override
                                        public void onBitmapFailed(Drawable errorDrawable) {
                                            videoThumbnailFile.setImageDrawable(errorDrawable);
                                        }

                                        @Override
                                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                                            videoThumbnailFile.setImageDrawable(placeHolderDrawable);
                                        }
                                    };
                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(FilesManager.getFileImageSent(mActivity, ImageUrl))
                                            .centerCrop()
                                            .placeholder(thumbnailRequestMap.get(messagesModel.getId()))
                                            .error(thumbnailRequestMap.get(messagesModel.getId()))
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                } else {

                                    Target target = new Target() {
                                        @Override
                                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                            videoThumbnailFile.setImageBitmap(bitmap);
                                            ImageLoader.DownloadImage(memoryCache, EndPoints.MESSAGE_VIDEO_THUMBNAIL_URL + ImageUrl, ImageUrl, mActivity, messagesModel.getId(), AppConstants.USER, AppConstants.ROW_MESSAGES_AFTER);

                                        }

                                        @Override
                                        public void onBitmapFailed(Drawable errorDrawable) {
                                            videoThumbnailFile.setImageDrawable(errorDrawable);
                                        }

                                        @Override
                                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                                            videoThumbnailFile.setImageDrawable(placeHolderDrawable);
                                        }
                                    };
                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(EndPoints.MESSAGE_VIDEO_THUMBNAIL_URL + ImageUrl)
                                            .placeholder(thumbnailRequestMap.get(messagesModel.getId()))
                                            .error(thumbnailRequestMap.get(messagesModel.getId()))
                                            .centerCrop()
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                }
                            }
                        }
                    }.execute();

                } else {
                    playBtnVideo.setVisibility(View.GONE);


                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_BEFORE);
                        }

                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            super.onPostExecute(bitmap);
                            if (bitmap != null) {
                                videoThumbnailFile.setImageBitmap(bitmap);
                            } else {
                                if (FilesManager.isFileImagesSentExists(mActivity, FilesManager.getImage(ImageUrl))) {
                                    Target target = new Target() {
                                        @Override
                                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                            videoThumbnailFile.setImageBitmap(bitmap);
                                            ImageLoader.DownloadImage(memoryCache, EndPoints.MESSAGE_VIDEO_THUMBNAIL_URL + ImageUrl, ImageUrl, mActivity, messagesModel.getId(), AppConstants.USER, AppConstants.ROW_MESSAGES_BEFORE);

                                        }

                                        @Override
                                        public void onBitmapFailed(Drawable errorDrawable) {
                                            videoThumbnailFile.setImageDrawable(errorDrawable);
                                        }

                                        @Override
                                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                                            videoThumbnailFile.setImageDrawable(placeHolderDrawable);
                                        }
                                    };
                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(FilesManager.getFileImageSent(mActivity, ImageUrl))
                                            .centerCrop()
                                            .placeholder(thumbnailRequestMap.get(messagesModel.getId()))
                                            .error(thumbnailRequestMap.get(messagesModel.getId()))
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                } else {
                                    Target target = new Target() {
                                        @Override
                                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                            videoThumbnailFile.setImageBitmap(bitmap);
                                            ImageLoader.DownloadImage(memoryCache, EndPoints.MESSAGE_VIDEO_THUMBNAIL_URL + ImageUrl, ImageUrl, mActivity, messagesModel.getId(), AppConstants.USER, AppConstants.ROW_MESSAGES_BEFORE);

                                        }

                                        @Override
                                        public void onBitmapFailed(Drawable errorDrawable) {
                                            videoThumbnailFile.setImageDrawable(errorDrawable);
                                        }

                                        @Override
                                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                                            videoThumbnailFile.setImageDrawable(placeHolderDrawable);
                                        }
                                    };
                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(EndPoints.MESSAGE_VIDEO_THUMBNAIL_URL + ImageUrl)
                                            .placeholder(thumbnailRequestMap.get(messagesModel.getId()))
                                            .error(thumbnailRequestMap.get(messagesModel.getId()))
                                            .centerCrop()
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                }
                            }
                        }
                    }.execute();


                }

            } else {

                if (isDownLoad) {
                    AppHelper.LogCat("isDownLoad   ");


                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_AFTER);
                        }

                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            super.onPostExecute(bitmap);
                            if (bitmap != null) {
                                videoThumbnailFile.setImageBitmap(bitmap);
                            } else {
                                if (FilesManager.isFileImagesExists(mActivity, FilesManager.getImage(ImageUrl))) {
                                    playBtnVideo.setVisibility(View.VISIBLE);
                                    Target target = new Target() {
                                        @Override
                                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                            videoThumbnailFile.setImageBitmap(bitmap);
                                        }

                                        @Override
                                        public void onBitmapFailed(Drawable errorDrawable) {
                                            videoThumbnailFile.setImageDrawable(errorDrawable);
                                        }

                                        @Override
                                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                                            videoThumbnailFile.setImageDrawable(placeHolderDrawable);
                                        }
                                    };
                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(FilesManager.getFileImage(mActivity, ImageUrl))
                                            .centerCrop()
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                } else {
                                    playBtnVideo.setVisibility(View.VISIBLE);
                                    Target target = new Target() {
                                        @Override
                                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                            videoThumbnailFile.setImageBitmap(bitmap);
                                            FilesManager.downloadMediaFile(mActivity, bitmap, ImageUrl, AppConstants.RECEIVED_IMAGE);
                                        }

                                        @Override
                                        public void onBitmapFailed(Drawable errorDrawable) {
                                            videoThumbnailFile.setImageDrawable(errorDrawable);
                                        }

                                        @Override
                                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                                            videoThumbnailFile.setImageDrawable(placeHolderDrawable);
                                        }
                                    };
                                    Picasso.with(mActivity.getApplicationContext())
                                            .load(EndPoints.MESSAGE_VIDEO_THUMBNAIL_URL + ImageUrl)
                                            .centerCrop()
                                            .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                            .into(target);
                                }
                            }
                        }
                    }.execute();


                } else {

                    downloadVideo.setVisibility(View.VISIBLE);
                    playBtnVideo.setVisibility(View.GONE);
                    getFileSize(messagesModel.getFileSize(), "video");


                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            Bitmap bitmap = ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_BEFORE);
                            return ImageLoader.BlurBitmap(bitmap, mActivity);
                        }

                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            super.onPostExecute(bitmap);
                            if (bitmap != null) {
                                AppHelper.LogCat("blurredbitmap dfs  ");
                                videoThumbnailFile.setImageBitmap(bitmap);
                            } else {
                                AppHelper.LogCat("onResourceReady dfs  ");

                                Target target = new Target() {
                                    @Override
                                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                        videoThumbnailFile.setImageBitmap(bitmap);
                                        ImageLoader.DownloadImage(memoryCache, EndPoints.MESSAGE_VIDEO_THUMBNAIL_URL + ImageUrl, ImageUrl, mActivity, messagesModel.getId(), AppConstants.USER, AppConstants.ROW_MESSAGES_BEFORE);


                                    }

                                    @Override
                                    public void onBitmapFailed(Drawable errorDrawable) {
                                        videoThumbnailFile.setImageDrawable(errorDrawable);
                                    }

                                    @Override
                                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                                        videoThumbnailFile.setImageDrawable(placeHolderDrawable);
                                    }
                                };
                                Picasso.with(mActivity.getApplicationContext())
                                        .load(EndPoints.MESSAGE_VIDEO_THUMBNAIL_URL + ImageUrl)
                                        .transform(new BlurTransformationPicasso(mActivity.getApplicationContext()))
                                        .centerCrop()
                                        .resize(AppConstants.MESSAGE_IMAGE_SIZE, AppConstants.MESSAGE_IMAGE_SIZE)
                                        .into(target);
                            }
                        }
                    }.execute();

                }
            }


        }

        TextDrawable textDrawable() {
            String name = mActivity.getApplicationContext().getString(R.string.app_name);

            ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
            // generate random color
            int color = generator.getColor(name);
            String c = String.valueOf(name.toUpperCase().charAt(0));
            return TextDrawable.builder().buildRound(c, color);


        }

        void setDate(String Date) {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {

                    return UtilsTime.convertDateToString(mActivity, UtilsTime.getCorrectDate(params[0]));
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                /*    if (messagesModel.getSenderID() != PreferenceManager.getID(mActivity)) {
                        if (finalDate.equals(mActivity.getString(R.string.date_format_yesterday))) {
                            mMessagesViewHolder.date.setTextSize(10.0f);
                            mMessagesViewHolder.date.setPadding(0, 0, 6, 0);
                        } else {
                            mMessagesViewHolder.date.setPadding(0, 0, 16, 0);
                        }
                    }*///todo here padding

                    date.setText(result);

                }
            }.execute(Date);

        }

        private void setupProgressBarUploadImage() {
            mProgressUploadImageInitial.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
            mProgressUploadImage.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
        }

        private void setupProgressBarUploadVideo() {
            mProgressUploadVideoInitial.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
            mProgressUploadVideo.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
        }

        private void setupProgressBarUploadAudio() {
            mProgressUploadAudioInitial.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
            mProgressUploadAudio.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);

        }

        private void setupProgressBarUploadDocument() {
            mProgressUploadDocumentInitial.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
            mProgressUploadDocument.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
        }

        private void setupProgressBarDownloadImage() {
            mProgressDownloadImageInitial.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
            mProgressDownloadImage.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);

        }

        private void setupProgressBarDownloadVideo() {
            mProgressDownloadVideoInitial.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
            mProgressDownloadVideo.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);

        }

        private void setupProgressBarDownloadAudio() {
            mProgressDownloadAudioInitial.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
            mProgressDownloadAudio.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
        }

        private void setupProgressBarDownloadDocument() {
            mProgressDownloadDocumentInitial.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
            mProgressDownloadDocument.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(mActivity, R.color.colorGreenProgressBars), PorterDuff.Mode.SRC_IN);
        }

        void hideImageFile() {
            imageFile.setVisibility(View.GONE);
            mProgressUploadImage.setVisibility(View.GONE);
            mProgressUploadImageInitial.setVisibility(View.GONE);
        }

        void showImageFile() {
            imageFile.setVisibility(View.VISIBLE);

        }

        void hideVideoThumbnailFile() {
            videoThumbnailFile.setVisibility(View.GONE);
            mProgressUploadVideo.setVisibility(View.GONE);
            mProgressUploadVideoInitial.setVisibility(View.GONE);
        }

        void showVideoThumbnailFile() {
            videoThumbnailFile.setVisibility(View.VISIBLE);

        }


        void setSenderName(String SendName) {
            senderName.setText(SendName);
        }

        void setSenderColor(int Sendcolor) {
            senderName.setTextColor(Sendcolor);
        }

        void hideSenderName() {
            senderName.setVisibility(View.GONE);
        }

        void showSenderName() {
            senderName.setVisibility(View.VISIBLE);
        }

        void hideSent() {
            statusMessages.setVisibility(View.GONE);
        }

        void showSent(int status) {
            statusMessages.setVisibility(View.VISIBLE);
            switch (status) {
                case AppConstants.IS_WAITING:
                    statusMessages.setImageResource(R.drawable.ic_access_time_gray_24dp);
                    break;
                case AppConstants.IS_SENT:
                    if (isStatusUpdated) {
                        AppHelper.playSound(mActivity, "audio/message_is_sent.m4a");
                        //AnimationsUtil.rotationY(statusMessages);
                        isStatusUpdated = false;
                    }
                    statusMessages.setImageResource(R.drawable.ic_done_gray_24dp);

                    break;
                case AppConstants.IS_DELIVERED:
                    if (isStatusUpdated) {
                        //  AnimationsUtil.rotationY(statusMessages);
                        isStatusUpdated = false;
                    }
                    statusMessages.setImageResource(R.drawable.ic_done_all_gray_24dp);
                    break;
                case AppConstants.IS_SEEN:
                    if (isStatusUpdated) {
                        AnimationsUtil.rotationY(statusMessages);
                        isStatusUpdated = false;
                    }
                    statusMessages.setImageResource(R.drawable.ic_done_all_blue_24dp);
                    break;

            }

        }

        void getFileSize(String size, String type) {
            if (size == null || size.isEmpty()) return;
            try {
                long filesSize = Long.parseLong(size);
                switch (type) {
                    case "document":
                        fileSizeDocument.setVisibility(View.VISIBLE);
                        fileSizeDocument.setText(String.valueOf(FilesManager.getFileSize(filesSize)));
                        break;
                    case "video":
                        fileSizeVideo.setVisibility(View.VISIBLE);
                        fileSizeVideo.setText(String.valueOf(FilesManager.getFileSize(filesSize)));
                        break;
                    case "image":
                        fileSizeImage.setVisibility(View.VISIBLE);
                        fileSizeImage.setText(String.valueOf(FilesManager.getFileSize(filesSize)));
                        break;
                }
            } catch (Exception e) {
                AppHelper.LogCat(" MessagesAdapter " + e.getMessage());
            }


        }

        void downloadFile(MessagesModel messagesModel) {

            String fileUrl = null;
            String type = "null";
            String Identifier = null;

            if (messagesModel.getImageFile() != null && !messagesModel.getImageFile().equals("null")) {
                downloadImage.setVisibility(View.GONE);
                type = "image";
                fileUrl = EndPoints.MESSAGE_IMAGE_DOWNLOAD_URL + messagesModel.getImageFile();
                Identifier = messagesModel.getImageFile();
            } else if (messagesModel.getVideoFile() != null && !messagesModel.getVideoFile().equals("null")) {
                type = "video";
                fileUrl = EndPoints.MESSAGE_VIDEO_DOWNLOAD_URL + messagesModel.getVideoFile();
                Identifier = messagesModel.getVideoFile();
            } else if (messagesModel.getAudioFile() != null && !messagesModel.getAudioFile().equals("null")) {
                retryDownloadAudio.setVisibility(View.GONE);
                type = "audio";
                fileUrl = EndPoints.MESSAGE_AUDIO_DOWNLOAD_URL + messagesModel.getAudioFile();
                Identifier = messagesModel.getAudioFile();
            } else if (messagesModel.getDocumentFile() != null && !messagesModel.getDocumentFile().equals("null")) {
                retryDownloadDocument.setVisibility(View.GONE);
                type = "document";
                fileUrl = EndPoints.MESSAGE_DOCUMENT_DOWNLOAD_URL + messagesModel.getDocumentFile();
                Identifier = messagesModel.getDocumentFile();
            }

            if (fileUrl == null || Identifier == null) {
                autoDownloadQueue.remove(messagesModel.getId());
                return;
            }

            if ("document".equals(type) && !SafeFileGuard.isSafeServerIdentifier(Identifier)) {
                AppHelper.CustomToast(mActivity, SafeFileGuard.BLOCKED_FILE_MESSAGE);
                autoDownloadQueue.remove(messagesModel.getId());
                mDownloadCallbacks.onError(type);
                return;
            }

            final FilesDownloadService downloadService = mApiService.RootService(FilesDownloadService.class, EndPoints.BACKEND_BASE_URL);
            String finalFileUrl = fileUrl;
            String finalIdentifier = Identifier;
            Call<ResponseBody> downloadResponseCall = downloadService.downloadLargeFileSizeUrlSync(finalFileUrl);
            String finalType = type;
            int messageId = messagesModel.getId();
            new AsyncTask<Void, Long, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    downloadResponseCall.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                ResponseBody responseBody = response.body();
                                if (responseBody == null) {
                                    autoDownloadQueue.remove(messageId);
                                    mDownloadCallbacks.onError(finalType);
                                    return;
                                }
                                String contentType = responseBody.contentType() != null
                                        ? responseBody.contentType().toString()
                                        : null;
                                if ("document".equals(finalType) && !SafeFileGuard.isSafeContentType(contentType)) {
                                    AppHelper.CustomToast(mActivity, SafeFileGuard.BLOCKED_FILE_MESSAGE);
                                    autoDownloadQueue.remove(messageId);
                                    mDownloadCallbacks.onError(finalType);
                                    return;
                                }
                                AppHelper.LogCat("server contacted and has file");
                                DownloadFilesHelper downloadFilesHelper = new DownloadFilesHelper(responseBody, finalIdentifier, finalType, mDownloadCallbacks);
                                boolean writtenToDisk = downloadFilesHelper.writeResponseBodyToDisk(mActivity);
                                if (writtenToDisk) {
                                    int senderId = messagesModel.getSenderID();
                                    if (senderId != PreferenceManager.getID(mActivity)) {
                                        com.money.mimi.services.MainService.emitMessageDownloaded(mActivity, messageId, senderId);
                                    }
                                    new Handler().postDelayed(() -> {
                                        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                                        realm.executeTransactionAsync(realm1 -> {
                                            MessagesModel messagesModel1 = realm1.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                                            messagesModel1.setFileDownLoad(true);
                                            realm1.copyToRealmOrUpdate(messagesModel1);

                                        }, () -> {
                                            Realm realm2 = WhatsCloneApplication.getRealmDatabaseInstance();
                                            MessagesModel messagesModel1 = realm2.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                                            mDownloadCallbacks.onFinish(finalType, messagesModel1);
                                            autoDownloadQueue.remove(messageId);
                                            if (!realm2.isClosed()) realm2.close();
                                            if (!realm.isClosed()) realm.close();
                                        }, error -> {
                                            autoDownloadQueue.remove(messageId);
                                            mDownloadCallbacks.onError(finalType);
                                            if (!realm.isClosed()) realm.close();
                                        });

                                    }, 2000);

                                } else {
                                    autoDownloadQueue.remove(messageId);
                                    mDownloadCallbacks.onError(finalType);
                                }


                            } else {
                                AppHelper.LogCat("server contact failed");
                                autoDownloadQueue.remove(messageId);
                                mDownloadCallbacks.onError(finalType);
                            }

                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            AppHelper.LogCat("download is failed " + t.getMessage());
                            autoDownloadQueue.remove(messageId);
                            mDownloadCallbacks.onError(finalType);
                        }


                    });
                    return null;
                }
            }.execute();
            cancelDownloadImage.setOnClickListener(view -> {
                downloadResponseCall.cancel();
                cancelDownloadImage.setVisibility(View.GONE);
                downloadImage.setVisibility(View.VISIBLE);
                mProgressDownloadImage.setVisibility(View.GONE);
                mProgressDownloadImageInitial.setVisibility(View.GONE);
            });

            cancelDownloadAudio.setOnClickListener(view -> {
                downloadResponseCall.cancel();
                cancelDownloadAudio.setVisibility(View.GONE);
                downloadVideo.setVisibility(View.VISIBLE);
                mProgressDownloadVideo.setVisibility(View.GONE);
                mProgressDownloadVideoInitial.setVisibility(View.GONE);
            });

            cancelDownloadDocument.setOnClickListener(view -> {
                downloadResponseCall.cancel();
                cancelDownloadDocument.setVisibility(View.GONE);
                retryDownloadDocument.setVisibility(View.VISIBLE);
                mProgressDownloadDocument.setVisibility(View.GONE);
                mProgressDownloadDocumentInitial.setVisibility(View.GONE);
            });

            cancelDownloadVideo.setOnClickListener(view -> {
                downloadResponseCall.cancel();
                cancelDownloadVideo.setVisibility(View.GONE);
                downloadVideo.setVisibility(View.VISIBLE);
                mProgressDownloadVideo.setVisibility(View.GONE);
                mProgressDownloadVideoInitial.setVisibility(View.GONE);
            });

        }

        void uploadFile(MessagesModel messagesModel) {

            File file;

            try {
                String type = "null";
                String mimeType = null;
                String filePath = null;
                if (messagesModel.getImageFile() != null && !messagesModel.getImageFile().equals("null")) {
                    type = "image";
                    filePath = messagesModel.getImageFile();
                    mimeType = FilesManager.getMimeTypeFromPath(messagesModel.getImageFile());
                    if (mimeType == null) mimeType = "image/*";
                } else if (messagesModel.getVideoFile() != null && !messagesModel.getVideoFile().equals("null")) {
                    type = "video";
                    filePath = messagesModel.getVideoFile();
                    mimeType = FilesManager.getMimeTypeFromPath(messagesModel.getVideoFile());
                    if (mimeType == null) mimeType = "video/*";
                } else if (messagesModel.getAudioFile() != null && !messagesModel.getAudioFile().equals("null")) {
                    type = "audio";
                    filePath = messagesModel.getAudioFile();
                    mimeType = FilesManager.getMimeTypeFromPath(messagesModel.getAudioFile());
                    if (mimeType == null) mimeType = "audio/*";
                } else if (messagesModel.getDocumentFile() != null && !messagesModel.getDocumentFile().equals("null")) {
                    type = "document";
                    filePath = messagesModel.getDocumentFile();
                    mimeType = FilesManager.getMimeTypeFromPath(messagesModel.getDocumentFile());
                    if (mimeType == null) mimeType = "application/pdf";
                }
                if (filePath != null && new File(filePath).length() > AppConstants.MAX_UPLOAD_FILE_SIZE) {
                    AppHelper.CustomToast(mActivity, mActivity.getString(R.string.file_too_large, 50));
                    mUploadCallbacks.onError(type);
                    return;
                }
                switch (type) {
                    case "image":
                        file = new File(messagesModel.getImageFile());
                        mUploadFilesHelper = new UploadFilesHelper(file, mUploadCallbacks, mimeType, null, type);
                        uploadFileRequest(messagesModel, mUploadFilesHelper, "image", mimeType);
                        break;
                    case "video":
                        file = new File(messagesModel.getVideoFile());
                        mUploadFilesHelper = new UploadFilesHelper(file, mUploadCallbacks, mimeType, null, type);
                        uploadFileRequest(messagesModel, mUploadFilesHelper, "video", mimeType);
                        break;
                    case "audio":
                        file = new File(messagesModel.getAudioFile());
                        mUploadFilesHelper = new UploadFilesHelper(file, mUploadCallbacks, mimeType, null, type);
                        uploadFileRequest(messagesModel, mUploadFilesHelper, "audio", mimeType);
                        break;
                    case "document":
                        file = new File(messagesModel.getDocumentFile());
                        if (!SafeFileGuard.isSafeDocumentPath(messagesModel.getDocumentFile(), mimeType) || !SafeFileGuard.isSafeDocumentFile(file)) {
                            AppHelper.CustomToast(mActivity, SafeFileGuard.BLOCKED_FILE_MESSAGE);
                            mUploadCallbacks.onError(type);
                            return;
                        }
                        mUploadFilesHelper = new UploadFilesHelper(file, mUploadCallbacks, mimeType, null, type);
                        uploadFileRequest(messagesModel, mUploadFilesHelper, "document", mimeType);
                        break;

                }
            } catch (Exception e) {
                AppHelper.LogCat("failed to select a type file " + e.getMessage());
            }


        }


        void uploadFileRequest(MessagesModel messagesModel, UploadFilesHelper uploadFilesHelper, String type, String mimeType) {
            FilesUploadService filesUploadService = mApiService.RootService(FilesUploadService.class, EndPoints.BACKEND_BASE_URL);
            Call<FilesResponse> filesResponseCall;
            int messageId = messagesModel.getId();

            String fileName;
            switch (type) {
                case "image":
                    fileName = "messageImage" + FilesManager.getExtensionFromMimeType(mimeType);
                    if (fileName.equals("messageImage")) fileName = "messageImage.jpg";
                    break;
                case "video":
                    fileName = "messageVideo" + FilesManager.getExtensionFromMimeType(mimeType);
                    if (fileName.equals("messageVideo")) fileName = "messageVideo.mp4";
                    break;
                case "audio":
                    fileName = "messageAudio" + FilesManager.getExtensionFromMimeType(mimeType);
                    if (fileName.equals("messageAudio")) fileName = "messageAudio.mp3";
                    break;
                case "document":
                    fileName = "messageDocument" + FilesManager.getExtensionFromMimeType(mimeType);
                    if (fileName.equals("messageDocument")) fileName = "messageDocument.pdf";
                    break;
                default:
                    fileName = "file";
            }

            MultipartBody.Part filePart = MultipartBody.Part.createFormData(type, fileName, uploadFilesHelper);

            switch (type) {
                case "image":
                    filesResponseCall = filesUploadService.uploadMessageImage(filePart);
                    new AsyncTask<Void, Long, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            filesResponseCall.enqueue(new Callback<FilesResponse>() {
                                                          @Override
                                                          public void onResponse(Call<FilesResponse> call, Response<FilesResponse> response) {
                                                              if (response.isSuccessful()) {
                                                                  if (response.body().isSuccess()) {
                                                                      AppHelper.LogCat("url image " + response.body().getUrl());
                                                                       Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                                                                       realm.executeTransactionAsync(realm1 -> {
                                                                                   MessagesModel messagesModel1 = realm1.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                                                                                   messagesModel1.setFileUpload(true);
                                                                                   messagesModel1.setImageFile(response.body().getUrl());
                                                                                   realm1.copyToRealmOrUpdate(messagesModel1);
                                                                               }, () -> {


                                                                                   new Handler().postDelayed(() -> {
                                                                                       Realm realm2 = WhatsCloneApplication.getRealmDatabaseInstance();
                                                                                       MessagesModel messagesModel1 = realm2.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                                                                                       mUploadCallbacks.onFinish("image", messagesModel1);
                                                                                       if (!realm2.isClosed()) realm2.close();
                                                                                   }, 500);
                                                                                   AppHelper.LogCat("finish realm image");
                                                                                   if (!realm.isClosed()) realm.close();
                                                                               }
                                                                               , error -> {
                                                                                   AppHelper.LogCat("error realm image");
                                                                                   mUploadCallbacks.onError(type);
                                                                                   if (!realm.isClosed()) realm.close();
                                                                               });
                                                                  } else {
                                                                      AppHelper.LogCat("failed to upload image " + response.body().getUrl());
                                                                      mUploadCallbacks.onError(type);
                                                                  }
                                                              } else

                                                              {
                                                                  AppHelper.LogCat("failed to upload image  ");
                                                                  mUploadCallbacks.onError(type);
                                                              }
                                                          }

                                                          @Override
                                                          public void onFailure
                                                                  (Call<FilesResponse> call, Throwable t) {
                                                              AppHelper.LogCat("failed to upload image  " + t.getMessage());
                                                              mUploadCallbacks.onError(type);

                                                          }
                                                      }

                            );


                            return null;
                        }
                    }.execute();
                    cancelUploadImage.setOnClickListener(view -> {
                        filesResponseCall.cancel();
                        cancelUploadImage.setVisibility(View.GONE);
                        retryUploadImage.setVisibility(View.VISIBLE);
                        mProgressUploadImage.setVisibility(View.GONE);
                        mProgressUploadImageInitial.setVisibility(View.GONE);
                    });
                    break;

                case "video":
                    File thumbnailFile = messagesModel.getVideoThumbnailFile() != null && !messagesModel.getVideoThumbnailFile().equals("null") ? new File(messagesModel.getVideoThumbnailFile()) : null;
                    if (thumbnailFile != null && thumbnailFile.exists()) {
                        RequestBody thumbnail = RequestBody.create(MediaType.parse("image/*"), thumbnailFile);
                        filesResponseCall = filesUploadService.uploadMessageVideo(filePart, thumbnail);
                    } else {
                        AppHelper.LogCat("Video thumbnail is missing, uploading video without thumbnail");
                        RequestBody emptyThumbnail = RequestBody.create(MediaType.parse("image/*"), new byte[0]);
                        filesResponseCall = filesUploadService.uploadMessageVideo(filePart, emptyThumbnail);
                    }
                    new AsyncTask<Void, Long, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            filesResponseCall.enqueue(new Callback<FilesResponse>() {
                                                          @Override
                                                          public void onResponse(Call<FilesResponse> call, Response<FilesResponse> response) {
                                                              if (response.isSuccessful()) {
                                                                  if (response.body().isSuccess()) {
                                                                       Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                                                                       realm.executeTransactionAsync(realm1 -> {
                                                                                   MessagesModel messagesModel1 = realm1.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                                                                                   messagesModel1.setFileUpload(true);
                                                                                   messagesModel1.setVideoFile(response.body().getUrl());
                                                                                   messagesModel1.setVideoThumbnailFile(response.body().getVideoThumbnail());
                                                                                   realm1.copyToRealmOrUpdate(messagesModel1);
                                                                               }, () -> {
                                                                                   File file1 = new File(messagesModel.getVideoFile());
                                                                                   try {
                                                                                       FilesManager.copyFile(file1, FilesManager.getFileVideoSent(mActivity, response.body().getUrl()));
                                                                                   } catch (IOException e) {
                                                                                       e.printStackTrace();
                                                                                   }
                                                                                   new Handler().postDelayed(() -> {
                                                                                       Realm realm2 = WhatsCloneApplication.getRealmDatabaseInstance();
                                                                                       MessagesModel messagesModel1 = realm2.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                                                                                       mUploadCallbacks.onFinish("video", messagesModel1);
                                                                                       if (!realm2.isClosed()) realm2.close();
                                                                                   }, 500);
                                                                                   AppHelper.LogCat("finish realm video");
                                                                                   if (!realm.isClosed()) realm.close();
                                                                               }
                                                                               , error -> {
                                                                                   AppHelper.LogCat("error realm video " + error.getMessage());
                                                                                   mUploadCallbacks.onError(type);
                                                                                   if (!realm.isClosed()) realm.close();
                                                                               });
                                                                  } else {
                                                                      AppHelper.LogCat("failed to upload video " + response.body().getUrl());
                                                                      mUploadCallbacks.onError(type);
                                                                  }
                                                              } else {
                                                                  AppHelper.LogCat("failed to upload video  ");
                                                                  mUploadCallbacks.onError(type);
                                                              }
                                                          }

                                                          @Override
                                                          public void onFailure
                                                                  (Call<FilesResponse> call, Throwable t) {
                                                              AppHelper.LogCat("failed to upload video  " + t.getMessage());
                                                              mUploadCallbacks.onError(type);

                                                          }
                                                      }

                            );


                            return null;
                        }
                    }.execute();
                    cancelUploadVideo.setOnClickListener(view -> {
                        filesResponseCall.cancel();
                        cancelUploadVideo.setVisibility(View.GONE);
                        retryUploadVideo.setVisibility(View.VISIBLE);
                        mProgressUploadVideo.setVisibility(View.GONE);
                        mProgressUploadVideoInitial.setVisibility(View.GONE);
                    });
                    break;
                case "audio":
                    filesResponseCall = filesUploadService.uploadMessageAudio(filePart);
                    new AsyncTask<Void, Long, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            filesResponseCall.enqueue(new Callback<FilesResponse>() {
                                @Override
                                public void onResponse(Call<FilesResponse> call, Response<FilesResponse> response) {
                                    if (response.isSuccessful()) {
                                        if (response.body().isSuccess()) {
                                            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                                            realm.executeTransactionAsync(realm1 -> {
                                                        MessagesModel messagesModel1 = realm1.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                                                        messagesModel1.setFileUpload(true);
                                                        messagesModel1.setAudioFile(response.body().getUrl());
                                                        realm1.copyToRealmOrUpdate(messagesModel1);
                                                    }, () -> {

                                                         File file1 = new File(messagesModel.getAudioFile());
                                                         try {
                                                             FilesManager.copyFile(file1, FilesManager.getFileAudioSent(mActivity, response.body().getUrl()));
                                                         } catch (IOException e) {
                                                             e.printStackTrace();
                                                         }
                                                        new Handler().postDelayed(() -> {
                                                            Realm realm2 = WhatsCloneApplication.getRealmDatabaseInstance();
                                                            MessagesModel messagesModel1 = realm2.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                                                            mUploadCallbacks.onFinish("audio", messagesModel1);
                                                            if (!realm2.isClosed()) realm2.close();
                                                        }, 500);
                                                        AppHelper.LogCat("finish realm audio");
                                                        if (!realm.isClosed()) realm.close();
                                                    }
                                                    , error -> {
                                                        mUploadCallbacks.onError(type);
                                                        AppHelper.LogCat("error realm audio");
                                                        if (!realm.isClosed()) realm.close();
                                                    });
                                        } else {
                                            AppHelper.LogCat("failed to upload audio " + response.body().getUrl());
                                            AppHelper.CustomToast(mActivity, "Failed to upload audio");
                                            mUploadCallbacks.onError(type);
                                        }
                                    } else {
                                        AppHelper.LogCat("failed to upload audio  ");
                                        AppHelper.CustomToast(mActivity, "Failed to upload audio");
                                        mUploadCallbacks.onError(type);
                                    }
                                }

                                @Override
                                public void onFailure(Call<FilesResponse> call, Throwable t) {
                                    AppHelper.CustomToast(mActivity, "Failed to upload audio");
                                    AppHelper.LogCat("failed to upload audio  " + t.getMessage());
                                    mUploadCallbacks.onError(type);

                                }
                            });


                            return null;
                        }
                    }.execute();
                    cancelUploadAudio.setOnClickListener(view -> {
                        filesResponseCall.cancel();
                        cancelUploadAudio.setVisibility(View.GONE);
                        retryUploadAudio.setVisibility(View.VISIBLE);
                        mProgressUploadAudio.setVisibility(View.GONE);
                        mProgressUploadAudioInitial.setVisibility(View.GONE);
                    });
                    break;
                case "document":
                    filesResponseCall = filesUploadService.uploadMessageDocument(filePart);
                    new AsyncTask<Void, Long, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            filesResponseCall.enqueue(new Callback<FilesResponse>() {
                                @Override
                                public void onResponse(Call<FilesResponse> call, Response<FilesResponse> response) {
                                    if (response.isSuccessful()) {
                                        if (response.body().isSuccess()) {
                                            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                                            realm.executeTransactionAsync(realm1 -> {
                                                        MessagesModel messagesModel1 = realm1.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                                                        messagesModel1.setFileUpload(true);
                                                        messagesModel1.setDocumentFile(response.body().getUrl());
                                                        realm1.copyToRealmOrUpdate(messagesModel1);
                                                    }, () -> {
                                                        new Handler().postDelayed(() -> {
                                                            Realm realm2 = WhatsCloneApplication.getRealmDatabaseInstance();
                                                            MessagesModel messagesModel1 = realm2.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                                                            mUploadCallbacks.onFinish("document", messagesModel1);
                                                            if (!realm2.isClosed()) realm2.close();
                                                        }, 500);
                                                        AppHelper.LogCat("finish realm document");
                                                        if (!realm.isClosed()) realm.close();
                                                    }
                                                    , error -> {
                                                        mUploadCallbacks.onError(type);
                                                        AppHelper.LogCat("error realm document");
                                                        if (!realm.isClosed()) realm.close();
                                                    });
                                        } else {
                                            AppHelper.LogCat("failed to upload document isNotSuccess" + response.body().getUrl());
                                            mUploadCallbacks.onError(type);
                                            AppHelper.CustomToast(mActivity, "Failed to upload the document");
                                        }
                                    } else {
                                        AppHelper.LogCat("failed to upload document isNotSuccessful  ");
                                        AppHelper.CustomToast(mActivity, "Failed to upload the document");
                                        mUploadCallbacks.onError(type);
                                    }
                                }

                                @Override
                                public void onFailure
                                        (Call<FilesResponse> call, Throwable t) {
                                    AppHelper.CustomToast(mActivity, "Failed to upload the document");
                                    AppHelper.LogCat("failed to upload document Throwable " + t.getMessage());
                                    mUploadCallbacks.onError(type);

                                }
                            });


                            return null;
                        }
                    }.execute();
                    cancelUploadDocument.setOnClickListener(view -> {
                        filesResponseCall.cancel();
                        cancelUploadDocument.setVisibility(View.GONE);
                        retryUploadDocument.setVisibility(View.VISIBLE);
                        mProgressUploadDocument.setVisibility(View.GONE);
                        mProgressUploadDocumentInitial.setVisibility(View.GONE);
                    });
                    break;
            }


        }

        void stopPlayingAudio() {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    updateAudioProgressBar();
                    mMediaPlayer.stop();
                    mMediaPlayer.reset();
                    audioSeekBar.setProgress(0);
                    audioCurrentDurationAudio.setVisibility(View.GONE);
                    audioTotalDurationAudio.setVisibility(View.VISIBLE);
                    playBtnAudio.setVisibility(View.VISIBLE);
                    pauseBtnAudio.setVisibility(View.GONE);


                }

            }

        }

        void pausePlayingAudio() {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    updateAudioProgressBar();
                    mAudioCallbacks.onPause();
                }
            }
        }

        void playingAudio(MessagesModel messagesModel) {
            if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                updateAudioProgressBar();
                String AudioDataSource;
                if (mMediaPlayer != null) {


                    try {

                        if (FilesManager.isFileAudiosSentExists(mActivity, FilesManager.getAudio(messagesModel.getAudioFile()))) {
                            AudioDataSource = FilesManager.getFileAudiosSentPath(mActivity, messagesModel.getAudioFile());
                            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mMediaPlayer.setDataSource(AudioDataSource);
                            mMediaPlayer.prepare();
                            mMediaPlayer.setOnPreparedListener(MediaPlayer::start);

                        } else {

                            AudioDataSource = EndPoints.MESSAGE_AUDIO_URL + messagesModel.getAudioFile();
                            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mMediaPlayer.setDataSource(AudioDataSource);
                            mMediaPlayer.prepareAsync();
                            mMediaPlayer.setOnPreparedListener(MediaPlayer::start);

                        }

                    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                        e.printStackTrace();
                    }

                    mMediaPlayer.start();
                    audioTotalDurationAudio.setVisibility(View.GONE);
                    audioCurrentDurationAudio.setVisibility(View.VISIBLE);

                }
            } else {
                updateAudioProgressBar();
                String AudioDataSource;
                if (mMediaPlayer != null) {

                    try {
                        if (FilesManager.isFileAudioExists(mActivity, FilesManager.getAudio(messagesModel.getAudioFile()))) {
                            AudioDataSource = FilesManager.getFileAudioPath(mActivity, messagesModel.getAudioFile());
                            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mMediaPlayer.setDataSource(AudioDataSource);
                            mMediaPlayer.prepare();
                            mMediaPlayer.setOnPreparedListener(MediaPlayer::start);
                        } else {
                            AudioDataSource = EndPoints.MESSAGE_AUDIO_URL + messagesModel.getAudioFile();
                            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mMediaPlayer.setDataSource(AudioDataSource);
                            mMediaPlayer.prepareAsync();
                            mMediaPlayer.setOnPreparedListener(MediaPlayer::start);
                        }

                    } catch (Exception e) {
                        AppHelper.LogCat("IOException audio recipient " + e.getMessage());
                    }


                    mMediaPlayer.start();
                    audioTotalDurationAudio.setVisibility(View.GONE);
                    audioCurrentDurationAudio.setVisibility(View.VISIBLE);

                }
            }


        }

        void updateAudioProgressBar() {
            mHandler.postDelayed(mUpdateTimeTask, 100);
        }


        /**
         * Background Runnable thread
         */
        private Runnable mUpdateTimeTask = new Runnable() {
            public void run() {
                try {
                    if (mMediaPlayer.isPlaying()) {
                        long totalDuration = mMediaPlayer.getDuration();
                        long currentDuration = mMediaPlayer.getCurrentPosition();
                        audioCurrentDurationAudio.setText(UtilsTime.getFileTime(currentDuration));
                        int progress = (int) UtilsTime.getProgressPercentage(currentDuration, totalDuration);
                        mAudioCallbacks.onUpdate(progress);
                        mHandler.postDelayed(this, 100);
                    }
                } catch (Exception e) {
                    AppHelper.LogCat("Exception mUpdateTimeTask " + e.getMessage());
                }

            }
        };

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mHandler.removeCallbacks(mUpdateTimeTask);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int totalDuration = mMediaPlayer.getDuration();
            int currentPosition = (int) UtilsTime.progressToTimer(seekBar.getProgress(), totalDuration);
            mMediaPlayer.seekTo(currentPosition);
            updateAudioProgressBar();
        }

        @Override
        public void onClick(View view) {
            if (!isActivated) {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION || mMessagesModel == null || position >= mMessagesModel.size()) {
                    return;
                }
                MessagesModel messagesModel = mMessagesModel.get(position);
                switch (view.getId()) {
                    case R.id.pause_btn_audio:
                        playBtnAudio.setVisibility(View.VISIBLE);
                        pauseBtnAudio.setVisibility(View.GONE);
                        pausePlayingAudio();
                        break;
                    case R.id.play_btn_audio:
                        playBtnAudio.setVisibility(View.GONE);
                        pauseBtnAudio.setVisibility(View.VISIBLE);
                        stopPlayingAudio();
                        playingAudio(messagesModel);
                        break;
                    case R.id.video_layout:
                        playingVideo(messagesModel);
                        break;
                    case R.id.play_btn_video:
                        playingVideo(messagesModel);
                        break;
                    case R.id.image_layout:
                        showImage(messagesModel);
                        break;
                    case R.id.download_image:

                        if (hasMediaStorageAccess()) {
                            setupProgressBarDownloadImage();
                            mProgressDownloadImageInitial.setVisibility(View.VISIBLE);
                            cancelDownloadImage.setVisibility(View.VISIBLE);
                            downloadImage.setVisibility(View.GONE);
                            new Handler().postDelayed(() -> downloadFile(messagesModel), 2000);
                        } else {
                            AppHelper.LogCat("Please request Read storage data permission.");
                            requestMediaStorageAccess();
                        }
                        break;
                    case R.id.cancel_download_image:
                        cancelDownloadImage.setVisibility(View.GONE);
                        downloadImage.setVisibility(View.VISIBLE);
                        mProgressDownloadImage.setVisibility(View.GONE);
                        mProgressDownloadImageInitial.setVisibility(View.GONE);

                        break;
                    case R.id.cancel_upload_image:
                        cancelUploadImage.setVisibility(View.GONE);
                        retryUploadImage.setVisibility(View.VISIBLE);
                        mProgressUploadImage.setVisibility(View.GONE);
                        mProgressUploadImageInitial.setVisibility(View.GONE);
                        break;

                    case R.id.retry_upload_image:
                        if (hasMediaStorageAccess()) {
                            retryUploadImage.setVisibility(View.GONE);
                            setupProgressBarUploadImage();
                            mProgressUploadImageInitial.setVisibility(View.VISIBLE);
                            cancelUploadImage.setVisibility(View.VISIBLE);
                            mProgressUploadImageInitial.setIndeterminate(true);
                            new Handler().postDelayed(() -> uploadFile(messagesModel), 1000);
                        } else {
                            AppHelper.LogCat("Please request Read storage data permission.");
                            requestMediaStorageAccess();
                        }
                        break;


                    case R.id.download_video:

                        if (hasMediaStorageAccess()) {
                            setupProgressBarDownloadVideo();
                            mProgressDownloadVideoInitial.setVisibility(View.VISIBLE);
                            cancelDownloadVideo.setVisibility(View.VISIBLE);
                            downloadVideo.setVisibility(View.GONE);
                            new Handler().postDelayed(() -> downloadFile(messagesModel), 1000);
                        } else {
                            AppHelper.LogCat("Please request Read storage data permission.");
                            requestMediaStorageAccess();
                        }
                        break;
                    case R.id.cancel_download_video:
                        cancelDownloadVideo.setVisibility(View.GONE);
                        downloadVideo.setVisibility(View.VISIBLE);
                        mProgressDownloadVideo.setVisibility(View.GONE);
                        mProgressDownloadVideoInitial.setVisibility(View.GONE);

                        break;
                    case R.id.cancel_upload_video:
                        cancelUploadVideo.setVisibility(View.GONE);
                        retryUploadVideo.setVisibility(View.VISIBLE);
                        mProgressUploadVideo.setVisibility(View.GONE);
                        mProgressUploadVideoInitial.setVisibility(View.GONE);
                        break;
                    case R.id.retry_upload_video:

                        if (hasMediaStorageAccess()) {
                            retryUploadVideo.setVisibility(View.GONE);
                            setupProgressBarUploadVideo();
                            mProgressUploadVideoInitial.setVisibility(View.VISIBLE);
                            cancelUploadVideo.setVisibility(View.VISIBLE);
                            mProgressUploadVideoInitial.setIndeterminate(true);
                            new Handler().postDelayed(() -> uploadFile(messagesModel), 1000);
                        } else {
                            AppHelper.LogCat("Please request Read storage data permission.");
                            requestMediaStorageAccess();
                        }

                        break;
                    case R.id.cancel_download_audio:
                        cancelDownloadAudio.setVisibility(View.GONE);
                        retryDownloadAudio.setVisibility(View.VISIBLE);
                        mProgressDownloadAudio.setVisibility(View.GONE);
                        mProgressDownloadAudioInitial.setVisibility(View.GONE);
                        break;
                    case R.id.retry_download_audio_button:

                        if (hasMediaStorageAccess()) {
                            setupProgressBarDownloadAudio();
                            mProgressDownloadAudioInitial.setVisibility(View.VISIBLE);
                            cancelDownloadAudio.setVisibility(View.VISIBLE);
                            retryDownloadAudio.setVisibility(View.GONE);
                            new Handler().postDelayed(() -> downloadFile(messagesModel), 1000);
                        } else {
                            AppHelper.LogCat("Please request Read storage data permission.");
                            requestMediaStorageAccess();
                        }
                        break;
                    case R.id.cancel_download_document:
                        cancelDownloadDocument.setVisibility(View.GONE);
                        retryDownloadDocument.setVisibility(View.VISIBLE);
                        mProgressDownloadDocument.setVisibility(View.GONE);
                        mProgressDownloadDocumentInitial.setVisibility(View.GONE);
                        break;
                    case R.id.retry_download_document_button:

                        if (hasMediaStorageAccess()) {
                            setupProgressBarDownloadDocument();
                            mProgressDownloadDocumentInitial.setVisibility(View.VISIBLE);
                            cancelDownloadDocument.setVisibility(View.VISIBLE);
                            retryDownloadDocument.setVisibility(View.GONE);
                            new Handler().postDelayed(() -> downloadFile(messagesModel), 1000);
                        } else {
                            AppHelper.LogCat("Please request Read storage data permission.");
                            requestMediaStorageAccess();
                        }
                        break;

                    case R.id.cancel_upload_audio:
                        cancelUploadAudio.setVisibility(View.GONE);
                        retryUploadAudio.setVisibility(View.VISIBLE);
                        mProgressUploadAudio.setVisibility(View.GONE);
                        mProgressUploadAudioInitial.setVisibility(View.GONE);
                        break;
                    case R.id.retry_upload_audio_button:

                        if (hasMediaStorageAccess()) {
                            retryUploadAudio.setVisibility(View.GONE);
                            setupProgressBarUploadAudio();
                            mProgressUploadAudioInitial.setVisibility(View.VISIBLE);
                            cancelUploadAudio.setVisibility(View.VISIBLE);
                            mProgressUploadAudioInitial.setIndeterminate(true);
                            new Handler().postDelayed(() -> uploadFile(messagesModel), 1000);
                        } else {
                            AppHelper.LogCat("Please request Read storage data permission.");
                            requestMediaStorageAccess();
                        }
                        break;
                    case R.id.cancel_upload_document:
                        cancelUploadDocument.setVisibility(View.GONE);
                        retryUploadDocument.setVisibility(View.VISIBLE);
                        mProgressUploadDocument.setVisibility(View.GONE);
                        mProgressUploadDocumentInitial.setVisibility(View.GONE);
                        break;
                    case R.id.retry_upload_document_button:
                        if (hasMediaStorageAccess()) {
                            retryUploadDocument.setVisibility(View.GONE);
                            setupProgressBarUploadDocument();
                            mProgressUploadDocumentInitial.setVisibility(View.VISIBLE);
                            cancelUploadDocument.setVisibility(View.VISIBLE);
                            mProgressUploadDocumentInitial.setIndeterminate(true);
                            new Handler().postDelayed(() -> uploadFile(messagesModel), 1000);
                        } else {
                            AppHelper.LogCat("Please request Read storage data permission.");
                            requestMediaStorageAccess();
                        }
                        break;
                    case R.id.document_title:
                        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                            if (FilesManager.isFileDocumentsSentExists(mActivity, FilesManager.getDocument(messagesModel.getDocumentFile()))) {
                                openDocument(FilesManager.getFileDocumentSent(mActivity, messagesModel.getDocumentFile()));
                            } else {
                                File file = new File(EndPoints.MESSAGE_DOCUMENT_URL + messagesModel.getDocumentFile());
                                openDocument(file);
                            }
                        } else {
                            if (FilesManager.isFileDocumentsExists(mActivity, FilesManager.getDocument(messagesModel.getDocumentFile()))) {
                                openDocument(FilesManager.getFileDocument(mActivity, messagesModel.getDocumentFile()));
                            } else {
                                File file = new File(EndPoints.MESSAGE_DOCUMENT_URL + messagesModel.getDocumentFile());
                                openDocument(file);
                            }
                        }

                        break;
                }
            }
        }

        private void showImage(MessagesModel messagesModel) {
            String imageUrl = messagesModel.getImageFile();
            if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                if (messagesModel.isFileUpload()) {
                    if (FilesManager.isFileImagesSentExists(mActivity, FilesManager.getImage(imageUrl))) {
                        AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.SENT_IMAGE, imageUrl);

                    } else {
                        if (imageUrl != null)
                            AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.SENT_IMAGE_FROM_SERVER, imageUrl);
                    }

                } else {
                    if (messagesModel.isFileDownLoad()) {
                        if (FilesManager.isFileImagesSentExists(mActivity, FilesManager.getImage(imageUrl))) {
                            AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.SENT_IMAGE, imageUrl);

                        } else {
                            if (imageUrl != null)
                                AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.SENT_IMAGE_FROM_SERVER, imageUrl);
                        }
                    }
                }

            } else {
                if (messagesModel.isFileUpload()) {
                    if (FilesManager.isFileImagesExists(mActivity, FilesManager.getImage(imageUrl))) {
                        AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.RECEIVED_IMAGE, imageUrl);
                    } else {
                        if (imageUrl != null)
                            AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.RECEIVED_IMAGE_FROM_SERVER, imageUrl);
                    }

                } else {
                    if (messagesModel.isFileDownLoad()) {
                        if (FilesManager.isFileImagesExists(mActivity, FilesManager.getImage(imageUrl))) {
                            AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.RECEIVED_IMAGE, imageUrl);

                        } else {
                            if (imageUrl != null)
                                AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.RECEIVED_IMAGE_FROM_SERVER, imageUrl);
                        }
                    }
                }
            }
        }


        private void playingVideo(MessagesModel messagesModel) {
            String video = messagesModel.getVideoFile();

            if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {

                if (FilesManager.isFileVideosSentExists(mActivity, FilesManager.getVideo(video))) {
                    AppHelper.LaunchVideoPreviewActivity(mActivity, video, true);
                } else {
                    AppHelper.CustomToast(mActivity, mActivity.getString(R.string.this_video_is_not_exist));
                }
            } else {

                if (FilesManager.isFileVideosExists(mActivity, FilesManager.getVideo(video))) {
                    AppHelper.LaunchVideoPreviewActivity(mActivity, video, false);
                } else {
                    AppHelper.CustomToast(mActivity, mActivity.getString(R.string.this_video_is_not_exist));
                }
            }


        }

        void openDocument(File file) {
            if (SafeFileGuard.isSafeDocumentFile(file)) {
                Uri path = FilesManager.getFile(file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(path, "application/pdf");
                if (AppHelper.isAndroid7()) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                try {
                    mActivity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    AppHelper.CustomToast(mActivity, mActivity.getString(R.string.no_application_to_view_pdf));
                }
            }
        }

    }

    public void stopAudio() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
                mMediaPlayer.reset();
                mMediaPlayer.release();

            }
            mMediaPlayer = null;
        }

    }

}
