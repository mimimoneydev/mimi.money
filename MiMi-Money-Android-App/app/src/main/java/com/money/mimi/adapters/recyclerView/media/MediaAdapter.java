package com.money.mimi.adapters.recyclerView.media;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.money.mimi.R;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.models.messages.MessagesModel;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Abderrahim El imame on 11/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class MediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Activity mActivity;
    private List<MessagesModel> mMessagesModel;
    private LayoutInflater mInflater;
    private MemoryCache memoryCache;

    public MediaAdapter(Activity mActivity) {
        this.mActivity = mActivity;
        mInflater = LayoutInflater.from(mActivity);
        this.memoryCache = new MemoryCache();
    }

    public void setMessages(List<MessagesModel> mMessagesList) {
        this.mMessagesModel = mMessagesList;
        notifyDataSetChanged();
    }


    public List<MessagesModel> getMessages() {
        return mMessagesModel;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.row_media, parent, false);
        return new MediaProfileViewHolder(view);

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final MediaProfileViewHolder mediaProfileViewHolder = (MediaProfileViewHolder) holder;
        final MessagesModel messagesModel = this.mMessagesModel.get(position);
        try {
            if (messagesModel.getImageFile() != null && !messagesModel.getImageFile().equals("null")) {
                mediaProfileViewHolder.imageFile.setVisibility(View.VISIBLE);
                mediaProfileViewHolder.setImage(messagesModel);
            } else {
                mediaProfileViewHolder.imageFile.setVisibility(View.GONE);
            }

            if (messagesModel.getAudioFile() != null && !messagesModel.getAudioFile().equals("null")) {
                mediaProfileViewHolder.mediaAudio.setVisibility(View.VISIBLE);
            } else {
                mediaProfileViewHolder.mediaAudio.setVisibility(View.GONE);
            }

            if (messagesModel.getVideoFile() != null && !messagesModel.getVideoFile().equals("null")) {
                mediaProfileViewHolder.mediaVideo.setVisibility(View.VISIBLE);
                mediaProfileViewHolder.setMediaVideoThumbnail(messagesModel);
            } else {
                mediaProfileViewHolder.mediaVideo.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            AppHelper.LogCat("" + e.getMessage());
        }

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        if (mMessagesModel != null) {
            return mMessagesModel.size();
        } else {
            return 0;
        }
    }

    public class MediaProfileViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.media_image)
        ImageView imageFile;
        @BindView(R.id.media_audio)
        ImageView mediaAudio;
        @BindView(R.id.media_video_thumbnail)
        ImageView mediaVideoThumbnail;
        @BindView(R.id.media_video)
        FrameLayout mediaVideo;
        @BindView(R.id.play_btn_video)
        ImageButton playVideo;


        MediaProfileViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            imageFile.setOnClickListener(this);
            mediaVideo.setOnClickListener(this);
            mediaAudio.setOnClickListener(this);
            playVideo.setOnClickListener(this);

        }


        @SuppressLint("StaticFieldLeak")
        void setImage(MessagesModel messagesModel) {
            int messageId = messagesModel.getId();
            String imageUrl = messagesModel.getImageFile();
            if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {

                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        return ImageLoader.GetCachedBitmapImage(memoryCache, imageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_AFTER);
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        if (bitmap != null) {
                            imageFile.setImageBitmap(bitmap);
                        } else {

                            if (FilesManager.isFileImagesSentExists(mActivity, FilesManager.getImage(imageUrl))) {
                                Glide.with(mActivity)
                                        .load( FilesManager.getFileImageSent(mActivity, imageUrl))
                                        .asBitmap()
                                        .centerCrop()
                                        .placeholder(R.drawable.bg_rect_image_holder)
                                        .error(R.drawable.bg_rect_image_holder)
                                        .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                                        .into(imageFile);
                            } else {
                                BitmapImageViewTarget target = new BitmapImageViewTarget(imageFile) {
                                    @Override
                                    public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                                        super.onResourceReady(bitmap, anim);
                                        imageFile.setImageBitmap(bitmap);
                                    }

                                    @Override
                                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                        super.onLoadFailed(e, errorDrawable);
                                        imageFile.setImageDrawable(errorDrawable);
                                    }

                                    @Override
                                    public void onLoadStarted(Drawable placeholder) {
                                        super.onLoadStarted(placeholder);
                                        imageFile.setImageDrawable(placeholder);
                                    }
                                };
                                Glide.with(mActivity.getApplicationContext())
                                        .load(EndPoints.MESSAGE_IMAGE_URL + imageUrl)
                                        .asBitmap()
                                        .centerCrop()
                                        .placeholder(R.drawable.bg_rect_image_holder)
                                        .error(R.drawable.bg_rect_image_holder)
                                        .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                                        .into(target);
                            }
                        }
                    }
                }.execute();

            } else {

                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        return ImageLoader.GetCachedBitmapImage(memoryCache, imageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_AFTER);
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        if (bitmap != null) {
                            imageFile.setImageBitmap(bitmap);
                        } else {

                            if (FilesManager.isFileImagesSentExists(mActivity, FilesManager.getImage(imageUrl))) {
                                Glide.with(mActivity)
                                        .load(FilesManager.getFileImageSent(mActivity, imageUrl))
                                        .asBitmap()
                                        .centerCrop()
                                        .placeholder(R.drawable.bg_rect_image_holder)
                                        .error(R.drawable.bg_rect_image_holder)
                                        .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                                        .into(imageFile);


                            } else {
                                BitmapImageViewTarget target = new BitmapImageViewTarget(imageFile) {
                                    @Override
                                    public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                                        super.onResourceReady(bitmap, anim);
                                        imageFile.setImageBitmap(bitmap);
                                    }

                                    @Override
                                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                        super.onLoadFailed(e, errorDrawable);
                                        imageFile.setImageDrawable(errorDrawable);
                                    }

                                    @Override
                                    public void onLoadStarted(Drawable placeholder) {
                                        super.onLoadStarted(placeholder);
                                        imageFile.setImageDrawable(placeholder);
                                    }
                                };
                                Glide.with(mActivity)
                                        .load(EndPoints.MESSAGE_IMAGE_URL + imageUrl)
                                        .asBitmap()
                                        .centerCrop()
                                        .placeholder(R.drawable.bg_rect_image_holder)
                                        .error(R.drawable.bg_rect_image_holder)
                                        .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                                        .into(target);
                            }
                        }
                    }
                }.execute();
            }


        }


        @SuppressLint("StaticFieldLeak")
        void setMediaVideoThumbnail(MessagesModel messagesModel) {

            int messageId = messagesModel.getId();
            String imageUrl = messagesModel.getVideoThumbnailFile();
            if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {

                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        return ImageLoader.GetCachedBitmapImage(memoryCache, imageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_AFTER);
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        if (bitmap != null) {
                            mediaVideoThumbnail.setImageBitmap(bitmap);
                        } else {

                            BitmapImageViewTarget target = new BitmapImageViewTarget(mediaVideoThumbnail) {
                                @Override
                                public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                                    super.onResourceReady(bitmap, anim);
                                    mediaVideoThumbnail.setImageBitmap(bitmap);
                                    FilesManager.downloadMediaFile(mActivity, bitmap, imageUrl, AppConstants.SENT_IMAGE);

                                }

                                @Override
                                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                    super.onLoadFailed(e, errorDrawable);
                                    mediaVideoThumbnail.setImageDrawable(errorDrawable);
                                }

                                @Override
                                public void onLoadStarted(Drawable placeholder) {
                                    super.onLoadStarted(placeholder);
                                    mediaVideoThumbnail.setImageDrawable(placeholder);
                                }
                            };
                            Glide.with(mActivity)
                                    .load(EndPoints.MESSAGE_VIDEO_THUMBNAIL_URL + imageUrl)
                                    .asBitmap()
                                    .centerCrop()
                                    .placeholder(R.drawable.bg_rect_image_holder)
                                    .error(R.drawable.bg_rect_image_holder)
                                    .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                                    .into(target);
                        }
                    }
                }.execute();
            } else {

                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        return ImageLoader.GetCachedBitmapImage(memoryCache, imageUrl, mActivity, messageId, AppConstants.USER, AppConstants.ROW_MESSAGES_AFTER);
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        if (bitmap != null) {
                            mediaVideoThumbnail.setImageBitmap(bitmap);
                        } else {

                            if (FilesManager.isFileImagesExists(mActivity, FilesManager.getImage(imageUrl))) {
                                Glide.with(mActivity)
                                        .load(FilesManager.getFileImage(mActivity, imageUrl))
                                        .asBitmap()
                                        .centerCrop()
                                        .placeholder(R.drawable.bg_rect_image_holder)
                                        .error(R.drawable.bg_rect_image_holder)
                                        .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                                        .into(mediaVideoThumbnail);

                            } else {
                                BitmapImageViewTarget target = new BitmapImageViewTarget(mediaVideoThumbnail) {
                                    @Override
                                    public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                                        super.onResourceReady(bitmap, anim);
                                        mediaVideoThumbnail.setImageBitmap(bitmap);
                                        FilesManager.downloadMediaFile(mActivity, bitmap, imageUrl, AppConstants.RECEIVED_IMAGE);

                                    }

                                    @Override
                                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                        super.onLoadFailed(e, errorDrawable);
                                        mediaVideoThumbnail.setImageDrawable(errorDrawable);
                                    }

                                    @Override
                                    public void onLoadStarted(Drawable placeholder) {
                                        super.onLoadStarted(placeholder);
                                        mediaVideoThumbnail.setImageDrawable(placeholder);
                                    }
                                };
                                Glide.with(mActivity)
                                        .load(EndPoints.MESSAGE_VIDEO_THUMBNAIL_URL + imageUrl)
                                        .asBitmap()
                                        .centerCrop()
                                        .placeholder(R.drawable.bg_rect_image_holder)
                                        .error(R.drawable.bg_rect_image_holder)
                                        .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                                        .into(target);
                            }
                        }
                    }
                }.execute();


            }

        }


        @Override
        public void onClick(View view) {
            MessagesModel messagesModel = mMessagesModel.get(getAdapterPosition());
            switch (view.getId()) {
                case R.id.media_audio:
                    playingAudio(messagesModel);
                    break;

                case R.id.media_video:
                    playingVideo(messagesModel);
                    break;
                case R.id.play_btn_video:
                    playingVideo(messagesModel);
                    break;

                case R.id.media_image:
                    showImage(messagesModel);
                    break;
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

    private void showImage(MessagesModel messagesModel) {
        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {

            if (FilesManager.isFileImagesSentExists(mActivity, FilesManager.getImage(messagesModel.getImageFile()))) {
                AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.SENT_IMAGE, messagesModel.getImageFile());
            } else {
                if (messagesModel.getImageFile() != null)
                    AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.SENT_IMAGE_FROM_SERVER, messagesModel.getImageFile());
            }
        } else {

            if (FilesManager.isFileImagesExists(mActivity, FilesManager.getImage(messagesModel.getImageFile()))) {
                AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.RECEIVED_IMAGE, messagesModel.getImageFile());

            } else {
                if (messagesModel.getImageFile() != null)
                    AppHelper.LaunchImagePreviewActivity(mActivity, AppConstants.RECEIVED_IMAGE_FROM_SERVER, messagesModel.getImageFile());
            }
        }
    }

    private void playingAudio(MessagesModel messagesModel) {
        String audioFile = messagesModel.getAudioFile();

        if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {

            if (FilesManager.isFileAudiosSentExists(mActivity, FilesManager.getAudio(audioFile))) {
                File fileAudio = FilesManager.getFileAudioSent(mActivity, audioFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri data = FilesManager.getFile(fileAudio);
                intent.setDataAndType(data, "audio/*");
                try {
                    mActivity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    AppHelper.CustomToast(mActivity, mActivity.getString(R.string.no_app_to_play_audio));
                }

            } else {
                AppHelper.CustomToast(mActivity, mActivity.getString(R.string.this_audio_is_not_exist));
            }
        } else {

            if (FilesManager.isFileAudioExists(mActivity, FilesManager.getAudio(audioFile))) {
                File fileAudio = FilesManager.getFileAudio(mActivity, audioFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri data = FilesManager.getFile(fileAudio);
                intent.setDataAndType(data, "audio/*");
                try {
                    mActivity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    AppHelper.CustomToast(mActivity, mActivity.getString(R.string.no_app_to_play_audio));
                }

            } else {
                AppHelper.CustomToast(mActivity, mActivity.getString(R.string.this_audio_is_not_exist));
            }
        }

    }

}

