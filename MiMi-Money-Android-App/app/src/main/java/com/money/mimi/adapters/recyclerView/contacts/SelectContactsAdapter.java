package com.money.mimi.adapters.recyclerView.contacts;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
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
import com.money.mimi.activities.groups.AddMembersToGroupActivity;
import com.money.mimi.activities.messages.MessagesActivity;
import com.money.mimi.activities.profile.ProfilePreviewActivity;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.RateHelper;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.RecyclerViewFastScroller;
import com.money.mimi.ui.TextDrawable;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import io.realm.RealmList;
import jp.wasabeef.glide.transformations.CropCircleTransformation;


/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class SelectContactsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements RecyclerViewFastScroller.BubbleTextGetter {
    private final Activity mActivity;
    private RealmList<ContactsModel> mContactsModel;

    private static final int TYPE_ITEM = 1;
    private static final int TYPE_HEADER = 2;
    private MemoryCache memoryCache;
    private String SearchQuery;

    public void setContacts(RealmList<ContactsModel> contactsModelList) {
        this.mContactsModel = contactsModelList;
        notifyDataSetChanged();
    }

    public SelectContactsAdapter(@NonNull Activity mActivity) {
        this.mActivity = mActivity;
        this.mContactsModel = new RealmList<>();
        this.memoryCache = new MemoryCache();
    }

    //Methods for search start
    public void setString(String SearchQuery) {
        this.SearchQuery = SearchQuery;
        notifyDataSetChanged();
    }

    public void animateTo(List<ContactsModel> models) {
        applyAndAnimateRemovals(models);
        applyAndAnimateAdditions(models);
        applyAndAnimateMovedItems(models);
    }

    private void applyAndAnimateRemovals(List<ContactsModel> newModels) {
        int arraySize = mContactsModel.size();
        for (int i = arraySize - 1; i >= 0; i--) {
            final ContactsModel model = mContactsModel.get(i);
            if (!newModels.contains(model)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<ContactsModel> newModels) {
        int arraySize = newModels.size();
        for (int i = 0; i < arraySize; i++) {
            final ContactsModel model = newModels.get(i);
            if (!mContactsModel.contains(model)) {
                addItem(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<ContactsModel> newModels) {
        int arraySize = newModels.size();
        for (int toPosition = arraySize - 1; toPosition >= 0; toPosition--) {
            final ContactsModel model = newModels.get(toPosition);
            final int fromPosition = mContactsModel.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private ContactsModel removeItem(int position) {
        final ContactsModel model = mContactsModel.remove(position);
        notifyItemRemoved(position);
        return model;
    }

    private void addItem(int position, ContactsModel model) {
        mContactsModel.add(position, model);
        notifyItemInserted(position);
    }

    private void moveItem(int fromPosition, int toPosition) {
        final ContactsModel model = mContactsModel.remove(fromPosition);
        mContactsModel.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
    }

    //Methods for search end
    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        } else {
            return TYPE_ITEM;
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {

            View itemView = LayoutInflater.from(mActivity).inflate(R.layout.header_contacts, parent, false);
            return new ContactsHeaderViewHolder(itemView);
        } else {

            View itemView = LayoutInflater.from(mActivity).inflate(R.layout.row_contacts, parent, false);
            return new ContactsViewHolder(itemView);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ContactsViewHolder) {
            ContactsViewHolder contactsViewHolder = (ContactsViewHolder) holder;
            ContactsModel contactsModel = this.mContactsModel.get(position - 1);
            try {
                String Username;

                if (contactsModel.getUsername() != null) {
                    Username = contactsModel.getUsername();
                } else {

                    String name = UtilsPhone.getContactName(contactsModel.getWalletAddress());
                    if (name != null) {
                        Username = name;
                    } else {
                        Username = contactsModel.getWalletAddress();
                    }
                }
                SpannableString recipientUsername = SpannableString.valueOf(Username);
                if (SearchQuery == null) {
                    contactsViewHolder.username.setText(recipientUsername, TextView.BufferType.NORMAL);
                } else {
                    int index = TextUtils.indexOf(Username.toLowerCase(), SearchQuery.toLowerCase());
                    if (index >= 0) {
                        recipientUsername.setSpan(new ForegroundColorSpan(AppHelper.getColor(mActivity, R.color.colorSpanSearch)), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        recipientUsername.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    }

                    contactsViewHolder.username.setText(recipientUsername, TextView.BufferType.SPANNABLE);
                }


                if (contactsModel.getStatus() != null) {
                    contactsViewHolder.setStatus(contactsModel.getStatus());
                } else {
                    contactsViewHolder.setStatus(contactsModel.getWalletAddress());
                }

                if (contactsModel.isLinked()) {
                    contactsViewHolder.hideInviteButton();
                } else {
                    contactsViewHolder.showInviteButton();
                }
                contactsViewHolder.setUserImage(contactsModel.getImage(), contactsModel.getId(),Username);

            } catch (Exception e) {
                AppHelper.LogCat("" + e.getMessage());
            }
            contactsViewHolder.setOnClickListener(view -> {
                if (view.getId() == R.id.user_image) {
                    RateHelper.significantEvent(mActivity);
                    if (AppHelper.isAndroid5()) {
                        if (contactsModel.isLinked()) {
                            Intent mIntent = new Intent(mActivity, ProfilePreviewActivity.class);
                            mIntent.putExtra("userID", contactsModel.getId());
                            mIntent.putExtra("isGroup", false);
                            mActivity.startActivity(mIntent);
                        }
                    } else {
                        if (contactsModel.isLinked()) {
                            Intent mIntent = new Intent(mActivity, ProfilePreviewActivity.class);
                            mIntent.putExtra("userID", contactsModel.getId());
                            mIntent.putExtra("isGroup", false);
                            mActivity.startActivity(mIntent);
                            AnimationsUtil.setSlideInAnimation(mActivity);
                        }
                    }

                } else {
                    RateHelper.significantEvent(mActivity);
                    if (contactsModel.isLinked() && contactsModel.isActivate()) {
                        Intent messagingIntent = new Intent(mActivity, MessagesActivity.class);
                        messagingIntent.putExtra("conversationID", 0);
                        messagingIntent.putExtra("recipientID", contactsModel.getId());
                        messagingIntent.putExtra("isGroup", false);
                        mActivity.startActivity(messagingIntent);
                        mActivity.finish();
                        AnimationsUtil.setSlideInAnimation(mActivity);
                    } else if (contactsModel.getWalletAddress() != null && contactsModel.getWalletAddress().trim().matches("^0x[a-fA-F0-9]{40}$")) {
                        Intent messagingIntent = new Intent(mActivity, MessagesActivity.class);
                        messagingIntent.putExtra("conversationID", 0);
                        messagingIntent.putExtra("recipientID", contactsModel.getId());
                        messagingIntent.putExtra("isGroup", false);
                        mActivity.startActivity(messagingIntent);
                        mActivity.finish();
                        AnimationsUtil.setSlideInAnimation(mActivity);
                    } else {
                        String number = contactsModel.getWalletAddress();
                        contactsViewHolder.setShareApp(mActivity.getString(R.string.invitation_from) + " " + number);
                    }
                }

            });
        }

    }


    @Override
    public int getItemCount() {
        return mContactsModel.size() > 0 ? mContactsModel.size() + 1 : 1;
    }

    @Override
    public String getTextToShowInBubble(int pos) {
        try {
            if (mContactsModel.size() > pos) {
                if (mContactsModel.get(pos).getUsername() != null) {
                    return Character.toString(mContactsModel.get(pos).getUsername().charAt(0));
                } else {
                    String name = UtilsPhone.getContactName(mContactsModel.get(pos).getWalletAddress());
                    if (name != null) {
                        return Character.toString(name.charAt(0));
                    } else {
                        return Character.toString(mContactsModel.get(pos).getWalletAddress().charAt(0));
                    }
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            AppHelper.LogCat(e.getMessage());
            return e.getMessage();
        }

      /*  try {
            return mContactsModel.size() > pos ? Character.toString(mContactsModel.get(pos).getUsername().charAt(0)) : null;
        } catch (Exception e) {
            AppHelper.LogCat(e.getMessage());
            return e.getMessage();
        }*/
    }


    public class ContactsHeaderViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.imageHeader)
        AppCompatImageView imageHeader;

        ContactsHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(v -> {
                mActivity.startActivity(new Intent(mActivity, AddMembersToGroupActivity.class));
                mActivity.finish();
            });
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

        ContactsViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            status.setSelected(true);
            setTypeFaces();
        }

        private void setTypeFaces() {
            if (AppConstants.ENABLE_FONTS_TYPES) {
                status.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                username.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
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
                                ImageLoader.DownloadImage(memoryCache, EndPoints.ROWS_IMAGE_URL + ImageUrl, ImageUrl, mActivity, recipientId, AppConstants.USER, AppConstants.ROW_PROFILE);

                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                super.onLoadFailed(e, errorDrawable);
                                if (ImageUrl != null) {
                                    Bitmap bitmap = null;
                                    try {
                                        bitmap = MediaStore.Images.Media.getBitmap(mActivity.getContentResolver(), Uri.parse(ImageUrl));
                                    } catch (IOException ex) {
                                        // AppHelper.LogCat(e.getMessage());
                                    }
                                    if (bitmap != null) {
                                        ImageLoader.SetBitmapImage(bitmap, userImage);
                                    } else {
                                        userImage.setImageDrawable(errorDrawable);
                                    }
                                } else {
                                    userImage.setImageDrawable(errorDrawable);
                                }
                            }

                            @Override
                            public void onLoadStarted(Drawable placeholder) {
                                super.onLoadStarted(placeholder);
                                userImage.setImageDrawable(placeholder);
                            }
                        };
                        Glide.with(mActivity)
                                .load(EndPoints.ROWS_IMAGE_URL + ImageUrl)
                                .asBitmap()
                                .centerCrop()
                                .transform(new CropCircleTransformation(mActivity))
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

        void setUsername(String phone) {
            String name = UtilsPhone.getContactName(phone);
            if (name != null) {
                username.setText(name);
            } else {
                username.setText(phone);
            }

        }

        void setStatus(String Status) {
            String user = UtilsString.unescapeJava(Status);
            status.setText(user);
        }


        void setOnClickListener(View.OnClickListener listener) {
            itemView.setOnClickListener(listener);
            userImage.setOnClickListener(listener);
        }


        void setShareApp(String subject) {

            //   Uri imageUri = Uri.parse("android.resource://" + getPackageName() + "/mipmap/" + "ic_launcher");
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            shareIntent.putExtra(Intent.EXTRA_TEXT, AppConstants.INVITE_MESSAGE_SMS + String.format(mActivity.getString(R.string.rate_helper_google_play_url), mActivity.getPackageName()));
            // shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.setType("text/*");
            mActivity.startActivity(Intent.createChooser(shareIntent, mActivity.getString(R.string.shareItem)));
        }

    }


}
