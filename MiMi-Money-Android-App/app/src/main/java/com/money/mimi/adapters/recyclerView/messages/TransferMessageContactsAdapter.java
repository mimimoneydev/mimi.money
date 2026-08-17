package com.money.mimi.adapters.recyclerView.messages;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.money.mimi.R;
import com.money.mimi.activities.messages.MessagesActivity;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.helpers.call.CallManager;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.RecyclerViewFastScroller;
import com.money.mimi.ui.TextDrawable;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import androidx.appcompat.app.AlertDialog;
import jp.wasabeef.glide.transformations.CropCircleTransformation;


/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class TransferMessageContactsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements RecyclerViewFastScroller.BubbleTextGetter {
    protected final Activity mActivity;
    private List<ContactsModel> mContactsModel;
    private String SearchQuery;
    private ArrayList<String> filePathList;
    private ArrayList<String> messageCopied;
    private String filePath;
    private boolean forCall = false;
    private MemoryCache memoryCache;

    public TransferMessageContactsAdapter(@NonNull Activity mActivity, List<ContactsModel> mContactsModel, ArrayList<String> messageCopied) {
        this.mActivity = mActivity;
        this.mContactsModel = mContactsModel;
        this.messageCopied = messageCopied;
        this.filePathList = messageCopied;
        this.memoryCache = new MemoryCache();
    }

    public TransferMessageContactsAdapter(@NonNull Activity mActivity, List<ContactsModel> mContactsModel, boolean forCall) {
        this.mActivity = mActivity;
        this.mContactsModel = mContactsModel;
        this.forCall = forCall;
        this.memoryCache = new MemoryCache();
    }

    public TransferMessageContactsAdapter(@NonNull Activity mActivity, List<ContactsModel> mContactsModel, ArrayList<String> filePathList, boolean forFiles) {
        this.mActivity = mActivity;
        this.mContactsModel = mContactsModel;
        this.filePathList = filePathList;
        this.memoryCache = new MemoryCache();
    }

    public TransferMessageContactsAdapter(@NonNull Activity mActivity, List<ContactsModel> mContactsModel, String filePath) {
        this.mActivity = mActivity;
        this.mContactsModel = mContactsModel;
        this.filePath = filePath;
        this.memoryCache = new MemoryCache();
    }


    public void setContacts(List<ContactsModel> contactsModelList) {
        this.mContactsModel = contactsModelList;
        notifyDataSetChanged();
    }

    //Methods for search start
    public void setString(String SearchQuery) {
        this.SearchQuery = SearchQuery;
        notifyDataSetChanged();
    }

    //Methods for search end

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mActivity).inflate(R.layout.row_contacts, parent, false);
        return new ContactsViewHolder(itemView);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final ContactsViewHolder contactsViewHolder = (ContactsViewHolder) holder;
        final ContactsModel contactsModel = this.mContactsModel.get(position);
        try {
            contactsViewHolder.setUsername(contactsModel.getUsername(), contactsModel.getWalletAddress());

            String username;
            if (contactsModel.getUsername() != null) {
                username = contactsModel.getUsername();
            } else {
                String name = UtilsPhone.getContactName(contactsModel.getWalletAddress());
                if (name != null) {
                    username = name;
                } else {
                    username = contactsModel.getWalletAddress();
                }

            }

            SpannableString recipientUsername = SpannableString.valueOf(username);
            if (SearchQuery == null) {
                contactsViewHolder.username.setText(recipientUsername, TextView.BufferType.NORMAL);
            } else {
                int index = TextUtils.indexOf(username.toLowerCase(), SearchQuery.toLowerCase());
                if (index >= 0) {
                    recipientUsername.setSpan(new ForegroundColorSpan(AppHelper.getColor(mActivity, R.color.colorSpanSearch)), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    recipientUsername.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }

                contactsViewHolder.username.setText(recipientUsername, TextView.BufferType.SPANNABLE);
            }
            if (contactsModel.getStatus() != null) {
                String status = UtilsString.unescapeJava(contactsModel.getStatus());
                if (status.length() > 18)
                    contactsViewHolder.setStatus(status.substring(0, 18) + "... " + "");

                else
                    contactsViewHolder.setStatus(status);

            } else {
                contactsViewHolder.setStatus(contactsModel.getWalletAddress());
            }

            if (contactsModel.isLinked()) {
                contactsViewHolder.hideInviteButton();
            } else {
                contactsViewHolder.showInviteButton();
            }

            contactsViewHolder.setUserImage(contactsModel.getImage(), contactsModel.getId(),username);

            if (forCall) {
                contactsViewHolder.showVideoButton();
            } else {
                contactsViewHolder.hideVideoButton();
            }
            contactsViewHolder.setOnClickListener(view -> {
                if (view.getId() == R.id.CallVideoBtn) {
                    CallManager.callContact(mActivity, false, true, contactsModel.getId());
                } else if (view.getId() == R.id.CallBtn) {
                    CallManager.callContact(mActivity, false, false, contactsModel.getId());
                } else {

                    if (messageCopied != null && messageCopied.size() != 0) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                        builder.setMessage(mActivity.getString(R.string.transfer_message_to) + " " + username)
                                .setPositiveButton(mActivity.getString(R.string.Yes), (dialog, which) -> {
                                    Intent messagingIntent = new Intent(mActivity, MessagesActivity.class);
                                    messagingIntent.putExtra("conversationID", 0);
                                    messagingIntent.putExtra("recipientID", contactsModel.getId());
                                    messagingIntent.putExtra("isGroup", false);
                                    messagingIntent.putExtra("messageCopied", messageCopied);
                                    mActivity.startActivity(messagingIntent);
                                    mActivity.finish();
                                }).setNegativeButton(mActivity.getString(R.string.cancel), null).show();

                    } else if (filePathList != null && filePathList.size() != 0) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                        builder.setMessage(mActivity.getString(R.string.transfer_message_to) + " " + username)
                                .setPositiveButton(mActivity.getString(R.string.Yes), (dialog, which) -> {
                                    Intent messagingIntent = new Intent(mActivity, MessagesActivity.class);
                                    messagingIntent.putExtra("conversationID", 0);
                                    messagingIntent.putExtra("recipientID", contactsModel.getId());
                                    messagingIntent.putExtra("isGroup", false);
                                    messagingIntent.putExtra("filePathList", filePathList);
                                    mActivity.startActivity(messagingIntent);
                                    mActivity.finish();
                                }).setNegativeButton(mActivity.getString(R.string.cancel), null).show();

                    } else if (filePath != null) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                        builder.setMessage(mActivity.getString(R.string.transfer_message_to) + " " + username)
                                .setPositiveButton(mActivity.getString(R.string.Yes), (dialog, which) -> {
                                    Intent messagingIntent = new Intent(mActivity, MessagesActivity.class);
                                    messagingIntent.putExtra("conversationID", 0);
                                    messagingIntent.putExtra("recipientID", contactsModel.getId());
                                    messagingIntent.putExtra("isGroup", false);
                                    messagingIntent.putExtra("filePath", filePath);
                                    mActivity.startActivity(messagingIntent);
                                    mActivity.finish();
                                }).setNegativeButton(mActivity.getString(R.string.cancel), null).show();

                    }
                }
            });

        } catch (Exception e) {
            AppHelper.LogCat("contacts adapters " + e.getMessage());
        }

    }


    @Override
    public int getItemCount() {
        if (mContactsModel != null) return mContactsModel.size();
        return 0;
    }

    @Override
    public String getTextToShowInBubble(int pos) {
        try {
            return mContactsModel.size() > pos ? Character.toString(mContactsModel.get(pos).getUsername().charAt(0)) : null;
        } catch (Exception e) {
            AppHelper.LogCat(e.getMessage());
            return e.getMessage();
        }

    }

    public class ContactsViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.user_image)
        ImageView userImage;
        @BindView(R.id.username)
        TextView username;
        @BindView(R.id.status)
        EmojiconTextView status;
        @BindView(R.id.invite)
        TextView invite;

        @BindView(R.id.CallVideoBtn)
        AppCompatImageView CallVideoBtn;

        @BindView(R.id.CallBtn)
        AppCompatImageView CallBtn;

        ContactsViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            setTypeFaces();
        }

        private void setTypeFaces() {
            if (AppConstants.ENABLE_FONTS_TYPES) {
                username.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                status.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                invite.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
            }
        }

        TextDrawable textDrawable(String name) {
            if (name == null || name.trim().isEmpty()) {
                name = mActivity.getString(R.string.app_name);
            }
            ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
            // generate random color
            int color = generator.getColor(name);
            String c = String.valueOf(name.toUpperCase().charAt(0));
            return TextDrawable.builder().buildRound(c, color);


        }

        @SuppressLint("StaticFieldLeak")
        void setUserImage(String ImageUrl, int recipientId, String username) {
            TextDrawable drawable = textDrawable(username);

            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, mActivity, recipientId, AppConstants.USER, AppConstants.ROW_PROFILE);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    if (bitmap != null) {
                        ImageLoader.SetBitmapImage(bitmap, userImage);
                    } else {


                        BitmapImageViewTarget target = new BitmapImageViewTarget(userImage) {
                            @Override
                            public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                                super.onResourceReady(bitmap, anim);
                                userImage.setImageBitmap(bitmap);

                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                super.onLoadFailed(e, errorDrawable);
                                userImage.setImageDrawable(errorDrawable);
                            }

                            @Override
                            public void onLoadStarted(Drawable placeHolderDrawable) {
                                super.onLoadStarted(placeHolderDrawable);
                                userImage.setImageDrawable(placeHolderDrawable);
                            }
                        };
                        Glide.with(mActivity.getApplicationContext())
                                .load(EndPoints.ROWS_IMAGE_URL + ImageUrl)
                                .asBitmap()
                                .centerCrop()
                                .transform(new CropCircleTransformation(mActivity.getApplicationContext()))
                                .placeholder(drawable)
                                .error(drawable)
                                .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                                .into(target);
                    }
                }
            }.execute();

        }


        void hideInviteButton() {
            invite.setVisibility(View.GONE);
        }

        void showInviteButton() {
            invite.setVisibility(View.VISIBLE);
        }

        void showVideoButton() {
            CallVideoBtn.setVisibility(View.VISIBLE);
            CallBtn.setVisibility(View.VISIBLE);
        }

        void hideVideoButton() {
            CallVideoBtn.setVisibility(View.GONE);
            CallBtn.setVisibility(View.GONE);
        }

        void setUsername(String Username, String phone) {
            if (Username != null) {
                username.setText(Username);
            } else {
                String name = UtilsPhone.getContactName(phone);
                if (name != null) {
                    username.setText(name);
                } else {
                    username.setText(phone);
                }

            }

        }

        void setStatus(String Status) {
            status.setText(Status);
        }


        void setOnClickListener(View.OnClickListener listener) {
            itemView.setOnClickListener(listener);
            userImage.setOnClickListener(listener);
            CallVideoBtn.setOnClickListener(listener);
            CallBtn.setOnClickListener(listener);
        }


    }


}
