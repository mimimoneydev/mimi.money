package com.money.mimi.adapters.recyclerView.groups;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.money.mimi.R;
import com.money.mimi.api.APIGroups;
import com.money.mimi.api.APIService;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.models.groups.GroupResponse;
import com.money.mimi.models.groups.MembersGroupModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.TextDrawable;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import org.greenrobot.eventbus.EventBus;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Created by Abderrahim El imame on 11/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class AddNewMembersToGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Activity mActivity;
    private RealmList<ContactsModel> mContactsModels;
    private LayoutInflater mInflater;
    private Realm realm;
    private int groupID;
    private APIService mApiService;
    private String SearchQuery;
    private MemoryCache memoryCache;

    public AddNewMembersToGroupAdapter(Activity mActivity, RealmList<ContactsModel> mContactsModels, int groupID, APIService mApiService) {
        this.mActivity = mActivity;
        this.mContactsModels = mContactsModels;
        mInflater = LayoutInflater.from(mActivity);
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
        this.groupID = groupID;
        this.mApiService = mApiService;
        this.memoryCache = new MemoryCache();
    }

    public void setContacts(RealmList<ContactsModel> mContactsModels) {
        this.mContactsModels = mContactsModels;
        notifyDataSetChanged();
    }


    public RealmList<ContactsModel> getContacts() {
        return mContactsModels;
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
        int arraySize = mContactsModels.size();
        for (int i = arraySize - 1; i >= 0; i--) {
            final ContactsModel model = mContactsModels.get(i);
            if (!newModels.contains(model)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<ContactsModel> newModels) {
        int arraySize = newModels.size();
        for (int i = 0; i < arraySize; i++) {
            final ContactsModel model = newModels.get(i);
            if (!mContactsModels.contains(model)) {
                addItem(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<ContactsModel> newModels) {
        int arraySize = newModels.size();
        for (int toPosition = arraySize - 1; toPosition >= 0; toPosition--) {
            final ContactsModel model = newModels.get(toPosition);
            final int fromPosition = mContactsModels.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private ContactsModel removeItem(int position) {
        final ContactsModel model = mContactsModels.remove(position);
        notifyItemRemoved(position);
        return model;
    }

    private void addItem(int position, ContactsModel model) {
        mContactsModels.add(position, model);
        notifyItemInserted(position);
    }

    private void moveItem(int fromPosition, int toPosition) {
        final ContactsModel model = mContactsModels.remove(fromPosition);
        mContactsModels.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
    }
    //Methods for search end

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.row_add_members_group, parent, false);
        return new ContactsViewHolder(view);

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final ContactsViewHolder contactsViewHolder = (ContactsViewHolder) holder;
        final ContactsModel contactsModel = this.mContactsModels.get(position);
        try {
            if (contactsViewHolder.checkIfMemberExist(contactsModel.getId(), groupID)) {
                contactsViewHolder.itemView.setEnabled(false);
                contactsViewHolder.username.setTextColor(mActivity.getResources().getColor(R.color.colorGray2));
            } else {
                contactsViewHolder.itemView.setEnabled(true);
                contactsViewHolder.username.setTextColor(mActivity.getResources().getColor(R.color.colorBlack));
            }
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
                contactsViewHolder.setStatus(contactsModel.getStatus());
            }

            contactsViewHolder.setUserImage(contactsModel.getImage(), contactsModel.getId(),username);

        } catch (Exception e) {
            AppHelper.LogCat("Exception" + e.getMessage());
        }

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        if (mContactsModels != null) {
            return mContactsModels.size();
        } else {
            return 0;
        }
    }


    class ContactsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.user_image)
        ImageView userImage;

        @BindView(R.id.username)
        TextView username;

        @BindView(R.id.status)
        EmojiconTextView status;

        @BindView(R.id.select_icon)
        LinearLayout selectIcon;

        ContactsViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            status.setSelected(true);
            itemView.setOnClickListener(this);
            setTypeFaces();
        }


        private void setTypeFaces() {
            if (AppConstants.ENABLE_FONTS_TYPES) {
                status.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                username.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
            }
        }

        boolean checkIfMemberExist(int memberID, int groupID) {
            RealmQuery<MembersGroupModel> query = realm.where(MembersGroupModel.class)
                    .equalTo("userId", memberID)
                    .equalTo("groupID", groupID)
                    .equalTo("isLeft", false);
            return query.count() != 0;
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


        void setStatus(String Status) {
            String statu = UtilsString.unescapeJava(Status);
            status.setText(statu);
        }

        @Override
        public void onClick(View view) {
            ContactsModel membersGroupModel = mContactsModels.get(getAdapterPosition());
            String theName;
            String name = UtilsPhone.getContactName(membersGroupModel.getWalletAddress());
            if (name != null) {
                theName = name;
            } else {
                theName = membersGroupModel.getWalletAddress();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setMessage(mActivity.getString(R.string.add_to_group) + theName + mActivity.getString(R.string.member_to_group))
                    .setPositiveButton(mActivity.getString(R.string.add_new_member), (dialog, which) -> {
                        AddMembersToGroup(membersGroupModel.getId());
                    }).setNegativeButton(mActivity.getString(R.string.cancel), null).show();
        }


        private void AddMembersToGroup(int id) {
            APIGroups mApiGroups = mApiService.RootService(APIGroups.class, EndPoints.BACKEND_BASE_URL);
            Call<GroupResponse> CreateGroupCall = mApiGroups.addMembers(groupID, id);
            AppHelper.showDialog(mActivity, mActivity.getString(R.string.adding_member));
            CreateGroupCall.enqueue(new Callback<GroupResponse>() {
                @Override
                public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                    if (response.isSuccessful()) {
                        AppHelper.hideDialog();
                        if (response.body().isSuccess()) {
                            AppHelper.Snackbar(mActivity, mActivity.findViewById(R.id.ParentLayoutAddNewMembers), response.body().getMessage(), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_CREATE_GROUP));
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_ADD_MEMBER, groupID));
                            mActivity.finish();

                        } else {
                            AppHelper.Snackbar(mActivity, mActivity.findViewById(R.id.ParentLayoutAddNewMembers), response.body().getMessage(), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
                        }
                    } else {
                        AppHelper.hideDialog();
                        AppHelper.Snackbar(mActivity, mActivity.findViewById(R.id.ParentLayoutAddNewMembers), response.message(), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);

                    }
                }

                @Override
                public void onFailure(Call<GroupResponse> call, Throwable t) {
                    AppHelper.hideDialog();
                    AppHelper.Snackbar(mActivity, mActivity.findViewById(R.id.ParentLayoutAddNewMembers), mActivity.getString(R.string.failed_to_add_member_to_group), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
                }
            });


        }
    }


}

