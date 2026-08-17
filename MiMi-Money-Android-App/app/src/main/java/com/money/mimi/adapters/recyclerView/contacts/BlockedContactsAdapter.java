package com.money.mimi.adapters.recyclerView.contacts;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.money.mimi.R;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.models.users.contacts.UsersBlockModel;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.RecyclerViewFastScroller;
import com.money.mimi.ui.TextDrawable;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import io.realm.Realm;
import jp.wasabeef.glide.transformations.CropCircleTransformation;


/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class BlockedContactsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements RecyclerViewFastScroller.BubbleTextGetter {
    private final Activity mActivity;
    private List<UsersBlockModel> mContactsModel;
    private MemoryCache memoryCache;
    private int userId;

    public void setContacts(List<UsersBlockModel> contactsModelList) {
        this.mContactsModel = contactsModelList;
        notifyDataSetChanged();
    }

    public BlockedContactsAdapter(@NonNull Activity mActivity, List<UsersBlockModel> mContactsModel) {
        this.mActivity = mActivity;
        this.mContactsModel = mContactsModel;
        this.memoryCache = new MemoryCache();
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mActivity).inflate(R.layout.row_contacts, parent, false);
        return new ContactsViewHolder(itemView);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ContactsViewHolder) {
            final ContactsViewHolder contactsViewHolder = (ContactsViewHolder) holder;
            final UsersBlockModel usersBlockModel = this.mContactsModel.get(position);
            try {
                String username;
                if (usersBlockModel.getContactsModel().getUsername() != null) {
                    username = usersBlockModel.getContactsModel().getUsername();
                } else {
                    String name = UtilsPhone.getContactName(usersBlockModel.getContactsModel().getWalletAddress());
                    if (name != null) {
                        username = name;
                    } else {
                        username = usersBlockModel.getContactsModel().getWalletAddress();
                    }

                }
                contactsViewHolder.setUsername(username);


                if (usersBlockModel.getContactsModel().getStatus() != null) {
                    contactsViewHolder.setStatus(usersBlockModel.getContactsModel().getStatus());
                } else {
                    contactsViewHolder.setStatus(usersBlockModel.getContactsModel().getWalletAddress());
                }

                if (usersBlockModel.getContactsModel().isLinked()) {
                    contactsViewHolder.hideInviteButton();
                } else {
                    contactsViewHolder.showInviteButton();
                }
                contactsViewHolder.setUserImage(usersBlockModel.getContactsModel().getImage(), usersBlockModel.getId(), username);

            } catch (Exception e) {
                AppHelper.LogCat("" + e.getMessage());
            }

        }

    }


    @Override
    public int getItemCount() {
        return mContactsModel.size() > 0 ? mContactsModel.size() : 0;
    }

    @Override
    public String getTextToShowInBubble(int pos) {
        try {
            return mContactsModel.size() > pos ? Character.toString(mContactsModel.get(pos).getContactsModel().getUsername().charAt(0)) : null;
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

        ContactsViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            status.setSelected(true);
            setTypeFaces();
            itemView.setOnClickListener(view -> {
                UsersBlockModel usersBlockModel = mContactsModel.get(getAdapterPosition());
                userId = usersBlockModel.getContactsModel().getId();
                //delete popup
                Realm realmUnblock = WhatsCloneApplication.getRealmDatabaseInstance();
                AlertDialog.Builder builderUnblock = new AlertDialog.Builder(mActivity);
                builderUnblock.setMessage(R.string.unblock_user_make_sure);
                builderUnblock.setPositiveButton(R.string.Yes, (dialog, whichButton) -> {
                    realmUnblock.executeTransactionAsync(realm1 -> {
                        UsersBlockModel usersBlockModel2 = realm1.where(UsersBlockModel.class).equalTo("contactsModel.id", userId).findFirst();
                        usersBlockModel2.deleteFromRealm();
                    }, () -> {
                        AppHelper.LogCat("unBlock user success");
                        notifyDataSetChanged();
                    }, error -> {
                        AppHelper.LogCat("unBlock user" + error.getMessage());

                    });


                });

                builderUnblock.setNegativeButton(R.string.No, (dialog, whichButton) -> {

                });

                builderUnblock.show();
                if (!realmUnblock.isClosed())
                    realmUnblock.close();
            });
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
        void setUserImage(String ImageUrl, int recipientId, String name) {
            TextDrawable drawable = textDrawable(name);
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

        void setUsername(String phone) {
            username.setText(phone);
        }

        void setStatus(String Status) {
            String user = UtilsString.unescapeJava(Status);
            status.setText(user);
        }


    }
}
