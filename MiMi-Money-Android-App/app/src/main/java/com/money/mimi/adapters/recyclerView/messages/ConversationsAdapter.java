package com.money.mimi.adapters.recyclerView.messages;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.money.mimi.R;
import com.money.mimi.activities.messages.MessagesActivity;
import com.money.mimi.activities.profile.ProfilePreviewActivity;
import com.money.mimi.activities.support.SupportAgentActivity;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.api.APIHelper;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.AdMobHelper;
import com.money.mimi.helpers.InlineAdPositionHelper;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.RateHelper;
import com.money.mimi.helpers.UtilsPhone;
import com.money.mimi.helpers.UtilsString;
import com.money.mimi.helpers.UtilsTime;
import com.money.mimi.helpers.images.ImageCompressionAsyncTask;
import com.money.mimi.models.groups.GroupsModel;
import com.money.mimi.models.groups.MembersGroupModel;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.TextDrawable;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import static com.money.mimi.app.AppConstants.EVENT_BUS_ITEM_IS_ACTIVATED;


/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
@SuppressLint("StaticFieldLeak")
public class ConversationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_AD = 1;
    private static final int TYPE_SUPPORT = 2;
    private static final long SUPPORT_STABLE_ID = Long.MIN_VALUE;

    private RealmList<ConversationsModel> mConversations;
    private Realm realm;
    private String SearchQuery;
    private SparseBooleanArray selectedItems;
    private boolean isActivated = false;
    private RecyclerView conversationList;
    private MemoryCache memoryCache;
    private Context adContext;
    private boolean supportAgentEnabled;
    private final Set<AdView> activeAdViews = new HashSet<>();
    private final Set<Integer> pendingGroupCreations = new HashSet<>();

    public ConversationsAdapter() {
        this.mConversations = new RealmList<>();
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
        this.selectedItems = new SparseBooleanArray();
        this.memoryCache = new MemoryCache();
        setHasStableIds(true);
    }

    public ConversationsAdapter(RecyclerView conversationList) {
        this.mConversations = new RealmList<>();
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
        this.selectedItems = new SparseBooleanArray();
        this.conversationList = conversationList;
        this.memoryCache = new MemoryCache();
        setHasStableIds(true);
    }

    public void setAdContext(Context context) {
        this.adContext = context;
    }

    public void setSupportAgentEnabled(boolean enabled) {
        if (supportAgentEnabled == enabled) return;
        supportAgentEnabled = enabled;
        notifyDataSetChanged();
    }

    private boolean shouldShowAds() {
        return adContext != null && PreferenceManager.ShowBannerAds(adContext)
                && AdMobHelper.bannerId(
                        adContext, PreferenceManager.getUnitBannerAdsID(adContext)) != null;
    }

    private int getLeadingItemCount() {
        return supportAgentEnabled ? 1 : 0;
    }

    private int getRealPosition(int adapterPosition) {
        return InlineAdPositionHelper.toDataPositionAfterLeadingItems(
                adapterPosition, shouldShowAds(), getLeadingItemCount());
    }

    private boolean isAdPosition(int adapterPosition) {
        return InlineAdPositionHelper.isAdPositionAfterLeadingItems(
                adapterPosition, shouldShowAds(), getLeadingItemCount());
    }

    private boolean isSupportPosition(int adapterPosition) {
        return supportAgentEnabled && adapterPosition == 0;
    }

    private int toAdapterPosition(int dataPosition) {
        return InlineAdPositionHelper.toAdapterPositionAfterLeadingItems(
                dataPosition, shouldShowAds(), getLeadingItemCount());
    }


    public void setConversations(RealmList<ConversationsModel> conversationsModelList) {
        this.mConversations = conversationsModelList;
        notifyDataSetChanged();
    }

    /**
     * method to connect to the chat sever by socket
     */

    //Methods for search start
    public void setString(String SearchQuery) {
        this.SearchQuery = SearchQuery;
        notifyDataSetChanged();
    }

    public void animateTo(List<ConversationsModel> models) {
        applyAndAnimateRemovals(models);
        applyAndAnimateAdditions(models);
        applyAndAnimateMovedItems(models);
    }

    private void applyAndAnimateRemovals(List<ConversationsModel> newModels) {
        int arraySize = mConversations.size();
        for (int i = arraySize - 1; i >= 0; i--) {
            final ConversationsModel model = mConversations.get(i);
            if (!newModels.contains(model)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<ConversationsModel> newModels) {
        int arraySize = newModels.size();
        for (int i = 0; i < arraySize; i++) {
            final ConversationsModel model = newModels.get(i);
            if (!mConversations.contains(model)) {
                addItem(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<ConversationsModel> newModels) {
        int arraySize = newModels.size();
        for (int toPosition = arraySize - 1; toPosition >= 0; toPosition--) {
            final ConversationsModel model = newModels.get(toPosition);
            final int fromPosition = mConversations.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private ConversationsModel removeItem(int position) {
        final ConversationsModel model = mConversations.remove(position);
        notifyDataSetChanged();
        return model;
    }

    private void addItem(int position, ConversationsModel model) {
        mConversations.add(position, model);
        notifyDataSetChanged();
    }

    private void moveItem(int fromPosition, int toPosition) {
        final ConversationsModel model = mConversations.remove(fromPosition);
        mConversations.add(toPosition, model);
        notifyItemMoved(
                toAdapterPosition(fromPosition),
                toAdapterPosition(toPosition));
    }
    //Methods for search end

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_SUPPORT) {
            View supportView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_support_agent, parent, false);
            return new SupportViewHolder(supportView);
        }
        if (viewType == TYPE_AD) {
            View adView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_ad_banner, parent, false);
            return new AdViewHolder(adView);
        }
        Context context = parent.getContext();
        View itemView = LayoutInflater.from(context).inflate(R.layout.row_conversation, parent, false);
        return new ConversationViewHolder(itemView);
    }

    @Override
    public int getItemViewType(int position) {
        if (isSupportPosition(position)) {
            return TYPE_SUPPORT;
        }
        if (isAdPosition(position)) {
            return TYPE_AD;
        }
        return TYPE_ITEM;
    }

    @Override
    public long getItemId(int position) {
        if (isSupportPosition(position)) {
            return SUPPORT_STABLE_ID;
        }
        if (isAdPosition(position)) {
            return Long.MIN_VALUE + position;
        }
        int realPosition = getRealPosition(position);
        if (mConversations == null || realPosition < 0 || realPosition >= mConversations.size()) {
            return RecyclerView.NO_ID;
        }
        ConversationsModel model = mConversations.get(realPosition);
        return model == null ? RecyclerView.NO_ID : model.getId();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("RecyclerView") // position is converted to a model immediately and is not captured.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof SupportViewHolder) {
            holder.itemView.setOnClickListener(view -> {
                Context context = view.getContext();
                context.startActivity(new Intent(context, SupportAgentActivity.class));
            });
            return;
        }

        if (holder instanceof AdViewHolder) {
            AdViewHolder adViewHolder = (AdViewHolder) holder;
            LinearLayout adContainer = (LinearLayout) adViewHolder.itemView;
            if (adContainer.getChildCount() == 0 && adContext != null) {
                AdView adView = new AdView(adContext);
                adView.setAdSize(AdSize.BANNER);
                String adUnitId = AdMobHelper.bannerId(adContext, PreferenceManager.getUnitBannerAdsID(adContext));
                if (adUnitId == null) return;
                adView.setAdUnitId(adUnitId);
                AdRequest adRequest = new AdRequest.Builder()
                        
                        .build();
                adView.loadAd(adRequest);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                adContainer.addView(adView, params);
                activeAdViews.add(adView);
            }
            return;
        }

        ConversationViewHolder conversationViewHolder = (ConversationViewHolder) holder;
        Activity mActivity = (Activity) conversationViewHolder.itemView.getContext();
        int realPosition = getRealPosition(position);
        if (realPosition < 0 || realPosition >= mConversations.size()) {
            return;
        }
        ConversationsModel conversationsModel = mConversations.get(realPosition);
        if (conversationsModel == null || !conversationsModel.isValid()) {
            return;
        }

        MessagesModel messagesModel = null;
        try {
            RealmList<MessagesModel> messagesList = conversationsModel.getMessages();
            messagesModel = (messagesList != null && !messagesList.isEmpty()) ? messagesList.last() : null;
        } catch (IllegalStateException e) {
            AppHelper.LogCat("Invalid conversation messages list: " + e.getMessage());
            return;
        }

        if (conversationsModel.isGroup()) {

            if (conversationsModel.getRecipientUsername() != null) {
                String groupName = UtilsString.unescapeJava(conversationsModel.getRecipientUsername());
                conversationViewHolder.setUsername(groupName);
                SpannableString recipientUsername = SpannableString.valueOf(groupName);
                if (SearchQuery == null) {
                    conversationViewHolder.username.setText(recipientUsername, TextView.BufferType.NORMAL);
                } else {
                    int index = TextUtils.indexOf(groupName.toLowerCase(), SearchQuery.toLowerCase());
                    if (index >= 0) {
                        recipientUsername.setSpan(new ForegroundColorSpan(AppHelper.getColor(mActivity, R.color.colorSpanSearch)), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        recipientUsername.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    }

                    conversationViewHolder.username.setText(recipientUsername, TextView.BufferType.SPANNABLE);
                }
            }


            conversationViewHolder.isOffline();

            if (!conversationsModel.getCreatedOnline()) {
                conversationViewHolder.username.setTextColor(mActivity.getResources().getColor(R.color.colorGray2));

            } else {
                conversationViewHolder.username.setTextColor(mActivity.getResources().getColor(R.color.colorBlack));
            }
            if (messagesModel != null) {
                if (messagesModel.getImageFile() != null && !messagesModel.getImageFile().equals("null")) {
                    conversationViewHolder.lastMessage.setVisibility(View.GONE);
                    conversationViewHolder.setTypeFile("image");
                } else if (messagesModel.getVideoFile() != null && !messagesModel.getVideoFile().equals("null")) {
                    conversationViewHolder.lastMessage.setVisibility(View.GONE);
                    conversationViewHolder.setTypeFile("video");
                } else if (messagesModel.getAudioFile() != null && !messagesModel.getAudioFile().equals("null")) {
                    conversationViewHolder.lastMessage.setVisibility(View.GONE);
                    conversationViewHolder.setTypeFile("audio");
                } else if (messagesModel.getDocumentFile() != null && !messagesModel.getDocumentFile().equals("null")) {
                    conversationViewHolder.lastMessage.setVisibility(View.GONE);
                    conversationViewHolder.setTypeFile("document");
                } else {

                    conversationViewHolder.isFile.setVisibility(View.GONE);
                    conversationViewHolder.FileContent.setVisibility(View.GONE);
                    conversationViewHolder.lastMessage.setVisibility(View.VISIBLE);
                    switch (messagesModel.getMessage()) {
                        case AppConstants.CREATE_GROUP:
                            if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                                if (!conversationsModel.getCreatedOnline()) {
                                    conversationViewHolder.setLastMessage(mActivity.getString(R.string.tap_to_create_group));
                                } else {
                                    conversationViewHolder.setLastMessage(mActivity.getString(R.string.you_created_this_group));
                                }

                            } else {
                                String name = UtilsPhone.getContactName(messagesModel.getWalletAddress());
                                if (name != null) {
                                    conversationViewHolder.setLastMessage("" + name + mActivity.getString(R.string.he_created_this_group));
                                } else {
                                    conversationViewHolder.setLastMessage("" + messagesModel.getWalletAddress() + mActivity.getString(R.string.he_created_this_group));
                                }
                            }


                            break;
                        case AppConstants.LEFT_GROUP:
                            if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                                conversationViewHolder.setLastMessage(mActivity.getString(R.string.you_left));
                            } else {
                                String name = UtilsPhone.getContactName(messagesModel.getWalletAddress());
                                if (name != null) {
                                    conversationViewHolder.setLastMessage("" + name + mActivity.getString(R.string.he_left));
                                } else {
                                    conversationViewHolder.setLastMessage("" + messagesModel.getWalletAddress() + mActivity.getString(R.string.he_left));
                                }


                            }


                            break;
                        default:

                            conversationViewHolder.isFile.setVisibility(View.GONE);
                            conversationViewHolder.FileContent.setVisibility(View.GONE);
                            conversationViewHolder.lastMessage.setVisibility(View.VISIBLE);
                            if (conversationsModel.getLastMessage() != null)
                                conversationViewHolder.setLastMessage(conversationsModel.getLastMessage());
                            else
                                conversationViewHolder.setLastMessage(messagesModel.getMessage());
                            break;
                    }
                }


                if (messagesModel.getDate() != null) {
                    conversationViewHolder.setMessageDate(messagesModel.getDate());
                }
            } else {
                conversationViewHolder.isFile.setVisibility(View.GONE);
                conversationViewHolder.FileContent.setVisibility(View.GONE);
                conversationViewHolder.lastMessage.setVisibility(View.GONE);
            }

            if (conversationsModel.getCreatedOnline()) {
                conversationViewHolder.setGroupImage(conversationsModel.getRecipientImage(), conversationsModel.getGroupID(), conversationsModel.getRecipientUsername());
            } else {
                conversationViewHolder.setGroupImageOffline(conversationsModel.getRecipientImage(), conversationsModel.getRecipientUsername());
            }

            if (messagesModel != null) {
                if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                    conversationViewHolder.showSent(messagesModel.getStatus());
                } else {
                    conversationViewHolder.hideSent();
                }
            } else {
                conversationViewHolder.hideSent();
            }
            if (conversationsModel.getStatus() == AppConstants.IS_WAITING && !conversationsModel.getUnreadMessageCounter().equals("0")) {
                conversationViewHolder.ChangeStatusUnread();
                conversationViewHolder.showCounter();
                conversationViewHolder.setCounter(conversationsModel.getUnreadMessageCounter());

            } else {
                conversationViewHolder.ChangeStatusRead();
                conversationViewHolder.hideCounter();

            }

        } else {
            String username;
            if (conversationsModel.getRecipientUsername() != null && !conversationsModel.getRecipientUsername().equals("null")) {
                username = conversationsModel.getRecipientUsername();
            } else {
                String name = UtilsPhone.getContactName(conversationsModel.getRecipientPhone());
                if (name != null) {
                    username = name;
                } else {
                    username = conversationsModel.getRecipientPhone();
                }

            }
            conversationViewHolder.setUserImage(conversationsModel.getRecipientImage(), conversationsModel.getRecipientID(), username);
            SpannableString recipientUsername = SpannableString.valueOf(username);
            if (SearchQuery == null) {
                conversationViewHolder.username.setText(recipientUsername, TextView.BufferType.NORMAL);
            } else {
                int index = TextUtils.indexOf(username.toLowerCase(), SearchQuery.toLowerCase());
                if (index >= 0) {
                    recipientUsername.setSpan(new ForegroundColorSpan(AppHelper.getColor(mActivity, R.color.colorSpanSearch)), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    recipientUsername.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }

                conversationViewHolder.username.setText(recipientUsername, TextView.BufferType.SPANNABLE);
            }


            if (!conversationsModel.getCreatedOnline()) {
                conversationViewHolder.username.setTextColor(mActivity.getResources().getColor(R.color.colorBlack));
            } else {
                conversationViewHolder.username.setTextColor(mActivity.getResources().getColor(R.color.colorBlack));
                if (messagesModel != null) {
                    if (messagesModel.getImageFile() != null && !messagesModel.getImageFile().equals("null")) {
                        conversationViewHolder.lastMessage.setVisibility(View.GONE);
                        conversationViewHolder.setTypeFile("image");
                    } else if (messagesModel.getVideoFile() != null && !messagesModel.getVideoFile().equals("null")) {
                        conversationViewHolder.lastMessage.setVisibility(View.GONE);
                        conversationViewHolder.setTypeFile("video");
                    } else if (messagesModel.getAudioFile() != null && !messagesModel.getAudioFile().equals("null")) {
                        conversationViewHolder.lastMessage.setVisibility(View.GONE);
                        conversationViewHolder.setTypeFile("audio");
                    } else if (messagesModel.getDocumentFile() != null && !messagesModel.getDocumentFile().equals("null")) {
                        conversationViewHolder.lastMessage.setVisibility(View.GONE);
                        conversationViewHolder.setTypeFile("document");
                    } else {
                        conversationViewHolder.isFile.setVisibility(View.GONE);
                        conversationViewHolder.FileContent.setVisibility(View.GONE);
                        if (conversationsModel.getLastMessage() != null)
                            conversationViewHolder.setLastMessage(conversationsModel.getLastMessage());
                        else
                            conversationViewHolder.setLastMessage(messagesModel.getMessage());
                    }

                    if (messagesModel.getDate() != null) {
                        conversationViewHolder.setMessageDate(messagesModel.getDate());
                    } else {
                        conversationViewHolder.setMessageDate(conversationsModel.getMessageDate());
                    }
                } else {
                    conversationViewHolder.isFile.setVisibility(View.GONE);
                    conversationViewHolder.FileContent.setVisibility(View.GONE);
                    conversationViewHolder.lastMessage.setVisibility(View.GONE);
                    conversationViewHolder.setMessageDate(conversationsModel.getMessageDate());
                }
            }


            if (messagesModel != null) {
                if (messagesModel.getSenderID() == PreferenceManager.getID(mActivity)) {
                    conversationViewHolder.showSent(messagesModel.getStatus());
                } else {
                    conversationViewHolder.hideSent();
                }
            } else {
                conversationViewHolder.hideSent();
            }


            if (conversationsModel.getStatus() == AppConstants.IS_WAITING && !conversationsModel.getUnreadMessageCounter().equals("0")) {
                conversationViewHolder.ChangeStatusUnread();
                conversationViewHolder.showCounter();
                conversationViewHolder.setCounter(conversationsModel.getUnreadMessageCounter());

            } else {
                conversationViewHolder.ChangeStatusRead();
                conversationViewHolder.hideCounter();

            }


        }

        conversationViewHolder.setOnClickListener(view -> {
            if (!isActivated) {
                if (conversationsModel.isValid())
                    if (conversationsModel.isGroup()) {
                        if (!conversationsModel.getCreatedOnline()) {
                            try {
                                if (!pendingGroupCreations.add(conversationsModel.getId())) {
                                    return;
                                }

                                int currentUserId = PreferenceManager.getID(mActivity);
                                if (currentUserId <= 0) {
                                    pendingGroupCreations.remove(conversationsModel.getId());
                                    AppHelper.Snackbar(mActivity, mActivity.findViewById(R.id.main_activity), mActivity.getString(R.string.create_group_failed), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
                                    return;
                                }

                                StringBuilder ids = new StringBuilder();
                                Set<Integer> uniqueMemberIds = new LinkedHashSet<>();
                                List<MembersGroupModel> localMembers = realm.where(MembersGroupModel.class)
                                        .equalTo("groupID", conversationsModel.getId())
                                        .equalTo("Deleted", false)
                                        .equalTo("isLeft", false)
                                        .findAll();
                                if (localMembers != null && !localMembers.isEmpty()) {
                                    for (MembersGroupModel selectedMember : localMembers) {
                                        if (selectedMember != null && selectedMember.getUserId() > 0) {
                                            uniqueMemberIds.add(selectedMember.getUserId());
                                        }
                                    }
                                }
                                uniqueMemberIds.add(currentUserId);
                                for (Integer userId : uniqueMemberIds) {
                                    ids.append(userId);
                                    ids.append(",");
                                }
                                String id = UtilsString.removelastString(ids.toString());
                                if (id == null || id.trim().isEmpty()) {
                                    pendingGroupCreations.remove(conversationsModel.getId());
                                    AppHelper.Snackbar(mActivity, mActivity.findViewById(R.id.main_activity), mActivity.getString(R.string.create_group_failed), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
                                    return;
                                }
                                List<MembersGroupModel> localMembersBackup = realm.copyFromRealm(localMembers);
                                if (view.getId() != R.id.user_image) {
                                    openGroupConversation(view.getContext(), conversationsModel.getId(), conversationsModel.getGroupID(), conversationsModel.getRecipientID());
                                }
                                // create RequestBody instance from file
                                RequestBody requestIds =
                                        RequestBody.create(MediaType.parse("multipart/form-data"), id);
                                conversationViewHolder.getProgressBarGroup();

                                ImageCompressionAsyncTask imageCompression = new ImageCompressionAsyncTask() {
                                    @Override
                                    protected void onPostExecute(byte[] imageBytes) {
                                        // image here is compressed & ready to be sent to the server
                                        // create RequestBody instance from file
                                        RequestBody requestFile;
                                        if (imageBytes == null)
                                            requestFile = RequestBody.create(MediaType.parse("image*//*"), new byte[0]);
                                        else
                                            requestFile = RequestBody.create(MediaType.parse("image*//*"), imageBytes);


                                        RequestBody requestName = RequestBody.create(MediaType.parse("multipart/form-data"), conversationsModel.getRecipientUsername());
                                        String notificationKey = "group_" + PreferenceManager.getID(mActivity) + "_" + conversationsModel.getId();
                                        RequestBody requestNotificationKey = RequestBody.create(MediaType.parse("multipart/form-data"), notificationKey);

                                        APIHelper.initializeApiGroups().createGroup(PreferenceManager.getID(mActivity), requestName, requestFile, requestIds, requestNotificationKey, conversationsModel.getMessageDate()).subscribe(groupResponse -> {
                                            if (groupResponse.isSuccess()) {
                                                conversationViewHolder.setProgressBarGroup();
                                                Realm realm = null;
                                                try {
                                                    realm = WhatsCloneApplication.getRealmDatabaseInstance();
                                                    realm.executeTransaction(realm1 -> {
                                                        ConversationsModel conversationsModel1 = realm1.where(ConversationsModel.class).equalTo("id", conversationsModel.getId()).findFirst();
                                                        if (conversationsModel1 == null) {
                                                            throw new IllegalStateException("Conversation disappeared before group creation could be saved");
                                                        }
                                                        conversationsModel1.setCreatedOnline(true);
                                                        conversationsModel1.setGroupID(groupResponse.getGroupID());
                                                        conversationsModel1.setRecipientImage(groupResponse.getGroupImage());
                                                        realm1.copyToRealmOrUpdate(conversationsModel1);


                                                        MessagesModel messagesModel1 = realm1.where(MessagesModel.class).equalTo("conversationID", conversationsModel.getId()).findFirst();
                                                        if (messagesModel1 != null) {
                                                            messagesModel1.setGroup(true);
                                                            messagesModel1.setGroupID(groupResponse.getGroupID());
                                                            realm1.copyToRealmOrUpdate(messagesModel1);
                                                        }

                                                        RealmList<MembersGroupModel> members = groupResponse.getMembersGroupModels();
                                                        if (members == null || members.isEmpty()) {
                                                            members = buildLocalGroupMembers(groupResponse.getGroupID(), localMembersBackup);
                                                        }

                                                        GroupsModel groupsModel = new GroupsModel();
                                                        groupsModel.setId(groupResponse.getGroupID());
                                                        groupsModel.setMembers(members);
                                                        if (groupResponse.getGroupImage() != null)
                                                            groupsModel.setGroupImage(groupResponse.getGroupImage());
                                                        else
                                                            groupsModel.setGroupImage("null");
                                                        groupsModel.setGroupName(conversationsModel.getRecipientUsername());
                                                        groupsModel.setCreatorID(PreferenceManager.getID(mActivity));
                                                        realm1.copyToRealmOrUpdate(groupsModel);
                                                    });
                                                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_MESSAGE_CONVERSATION_OLD_ROW, conversationsModel.getId()));
                                                    AppHelper.LogCat("group id created 2 e " + groupResponse.getGroupID());
                                                    new Handler().postDelayed(() -> {
                                                        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_CREATE_NEW_GROUP, groupResponse.getGroupID(), conversationsModel.getId()));
                                                    }, 1000);
                                                    PreferenceManager.clearMembers(mActivity);
                                                    AppHelper.Snackbar(mActivity, mActivity.findViewById(R.id.main_activity), groupResponse.getMessage(), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);
                                                    AppHelper.CustomToast(mActivity, groupResponse.getMessage());
                                                } catch (Exception e) {
                                                    AppHelper.LogCat("Create group local save failed: " + e.getMessage());
                                                } finally {
                                                    pendingGroupCreations.remove(conversationsModel.getId());
                                                    if (realm != null && !realm.isClosed()) {
                                                        realm.close();
                                                    }
                                                }
                                            } else {
                                                conversationViewHolder.setProgressBarGroup();
                                                pendingGroupCreations.remove(conversationsModel.getId());
                                                AppHelper.LogCat("Create group server rejected: " + groupResponse.getMessage());
                                            }
                                        }, throwable -> {
                                            conversationViewHolder.setProgressBarGroup();
                                            pendingGroupCreations.remove(conversationsModel.getId());
                                            AppHelper.LogCat("Create group upload failed: " + throwable.getMessage());
                                        });
                                    }
                                };
                                imageCompression.execute(conversationsModel.getRecipientImage());
                            } catch (Exception e) {
                                pendingGroupCreations.remove(conversationsModel.getId());
                                AppHelper.LogCat("execption  ids " + e.getMessage());
                            }
                        } else {
                            if (view.getId() == R.id.user_image) {
                                if (AppHelper.isAndroid5()) {
                                    Intent mIntent = new Intent(mActivity, ProfilePreviewActivity.class);
                                    mIntent.putExtra("conversationID", conversationsModel.getId());
                                    mIntent.putExtra("groupID", conversationsModel.getGroupID());
                                    mIntent.putExtra("isGroup", conversationsModel.isGroup());
                                    mIntent.putExtra("userId", conversationsModel.getRecipientID());
                                    mActivity.startActivity(mIntent);
                                } else {
                                    Intent mIntent = new Intent(mActivity, ProfilePreviewActivity.class);
                                    mIntent.putExtra("conversationID", conversationsModel.getId());
                                    mIntent.putExtra("groupID", conversationsModel.getGroupID());
                                    mIntent.putExtra("isGroup", conversationsModel.isGroup());
                                    mIntent.putExtra("userId", conversationsModel.getRecipientID());
                                    mActivity.startActivity(mIntent);
                                    mActivity.overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);
                                }
                            } else {

                                RateHelper.significantEvent(mActivity);
                                Intent messagingIntent = new Intent(mActivity, MessagesActivity.class);
                                messagingIntent.putExtra("conversationID", conversationsModel.getId());
                                messagingIntent.putExtra("groupID", conversationsModel.getGroupID());
                                messagingIntent.putExtra("isGroup", true);
                                messagingIntent.putExtra("recipientID", conversationsModel.getRecipientID());
                                mActivity.startActivity(messagingIntent);
                                AnimationsUtil.setSlideInAnimation(mActivity);

                            }
                        }

                    } else {
                        if (view.getId() == R.id.user_image) {

                            if (AppHelper.isAndroid5()) {
                                Intent mIntent = new Intent(mActivity, ProfilePreviewActivity.class);
                                mIntent.putExtra("userID", conversationsModel.getRecipientID());
                                mIntent.putExtra("isGroup", false);
                                mActivity.startActivity(mIntent);
                            } else {
                                Intent mIntent = new Intent(mActivity, ProfilePreviewActivity.class);
                                mIntent.putExtra("userID", conversationsModel.getRecipientID());
                                mIntent.putExtra("isGroup", false);
                                mActivity.startActivity(mIntent);
                                mActivity.overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);
                            }
                        } else {
                            RateHelper.significantEvent(mActivity);
                            Intent messagingIntent = new Intent(mActivity, MessagesActivity.class);
                            messagingIntent.putExtra("conversationID", conversationsModel.getId());
                            messagingIntent.putExtra("recipientID", conversationsModel.getRecipientID());
                            messagingIntent.putExtra("isGroup", false);
                            mActivity.startActivity(messagingIntent);
                            AnimationsUtil.setSlideInAnimation(mActivity);
                        }
                    }
            } else {
                if (conversationsModel.isGroup()) {
                    AppHelper.LogCat("This is a group you cannot delete this conversation now");
                } else {
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_ITEM_IS_ACTIVATED, view));
                }

            }


        });

        holder.itemView.setActivated(selectedItems.get(position, false));

        if (holder.itemView.isActivated()) {

            final Animation animation = AnimationUtils.loadAnimation(mActivity, R.anim.scale_for_button_animtion_enter);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    conversationViewHolder.selectIcon.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            conversationViewHolder.selectIcon.startAnimation(animation);
        } else {


            final Animation animation = AnimationUtils.loadAnimation(mActivity, R.anim.scale_for_button_animtion_exit);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    conversationViewHolder.selectIcon.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            conversationViewHolder.selectIcon.startAnimation(animation);
        }


    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof AdViewHolder) {
            LinearLayout container = (LinearLayout) holder.itemView;
            if (container.getChildCount() > 0 && container.getChildAt(0) instanceof AdView) {
                AdView adView = (AdView) container.getChildAt(0);
                activeAdViews.remove(adView);
                adView.destroy();
                container.removeAllViews();
            }
        }
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        int dataSize = mConversations == null ? 0 : mConversations.size();
        return InlineAdPositionHelper.getItemCountWithLeadingItems(
                dataSize, shouldShowAds(), getLeadingItemCount());
    }

    public void refreshAds() {
        notifyDataSetChanged();
    }

    public void releaseAds() {
        for (AdView adView : new HashSet<>(activeAdViews)) {
            ViewParent parent = adView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(adView);
            }
            adView.destroy();
        }
        activeAdViews.clear();
        adContext = null;
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


    public ConversationsModel getItem(int position) {
        if (mConversations == null || position < 0 || position >= getItemCount()
                || isSupportPosition(position) || isAdPosition(position)) {
            return null;
        }
        int realPosition = getRealPosition(position);
        if (realPosition < 0 || realPosition >= mConversations.size()) {
            return null;
        }
        return mConversations.get(realPosition);
    }

    /**
     * method to check if a  conversation exist
     *
     * @param conversationId this is the first parameter for  checkIfGroupConversationExist method
     * @param realm          this is the second parameter for  checkIfGroupConversationExist  method
     * @return return value
     */
    private boolean checkIfConversationExist(int conversationId, Realm realm) {
        RealmQuery<ConversationsModel> query = realm.where(ConversationsModel.class).equalTo("id", conversationId);
        return query.count() != 0;

    }

    public void addConversationItem(int conversationId) {
        Realm realm = null;
        try {
            realm = WhatsCloneApplication.getRealmDatabaseInstance();
            ConversationsModel conversationsModel = realm.where(ConversationsModel.class).equalTo("id", conversationId).findFirst();
            if (conversationsModel == null || !conversationsModel.isValid()) {
                return;
            }
            ConversationsModel detachedConversation = realm.copyFromRealm(conversationsModel);
            if (!isConversationExistInList(conversationsModel.getId())) {
                addConversationItem(0, detachedConversation);
            } else {
                return;
            }

        } catch (Exception e) {
            AppHelper.LogCat(e);
        } finally {
            if (realm != null && !realm.isClosed()) {
                realm.close();
            }
        }
    }

    private boolean isConversationExistInList(int conversationId) {
        int arraySize = mConversations.size();
        boolean conversationExist = false;
        for (int i = 0; i < arraySize; i++) {
            ConversationsModel model = mConversations.get(i);
            if (conversationId == model.getId()) {
                conversationExist = true;
                break;
            }
        }
        return conversationExist;
    }

    private void addConversationItem(int position, ConversationsModel conversationsModel) {
        // if (position != 0) {
        try {
            this.mConversations.add(position, conversationsModel);
            notifyDataSetChanged();
        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
        // }
    }

    public void removeConversationItem(int position) {
        //if (position != 0) {
        try {
            mConversations.remove(position);
            notifyDataSetChanged();
        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
        //  }
    }

    public void DeleteConversationItem(int ConversationID) {
        try {
            int arraySize = mConversations.size();
            for (int i = 0; i < arraySize; i++) {
                ConversationsModel model = mConversations.get(i);
                if (model.isValid()) {
                    if (ConversationID == model.getId()) {
                        removeConversationItem(i);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
    }

    public void updateStatusConversationItem(int ConversationID) {
        Realm realm = null;
        try {
            realm = WhatsCloneApplication.getRealmDatabaseInstance();
            int arraySize = mConversations.size();
            for (int i = 0; i < arraySize; i++) {
                ConversationsModel model = mConversations.get(i);
                try {
                    if (ConversationID == model.getId()) {
                        ConversationsModel conversationsModel = realm.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                        if (conversationsModel != null && conversationsModel.isValid()) {
                            changeItemAtPosition(i, realm.copyFromRealm(conversationsModel));
                        }
                        break;
                    }
                } catch (Exception e) {
                    AppHelper.LogCat(e);
                }


            }
        } catch (Exception e) {
            AppHelper.LogCat(e);
        } finally {
            if (realm != null && !realm.isClosed()) {
                realm.close();
            }
        }
    }

    public void updateConversationItem(int ConversationID) {
        Realm realm = null;
        try {
            realm = WhatsCloneApplication.getRealmDatabaseInstance();
            int arraySize = mConversations.size();
            for (int i = 0; i < arraySize; i++) {
                ConversationsModel model = mConversations.get(i);
                if (ConversationID == model.getId()) {
                    ConversationsModel conversationsModel = realm.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                    if (conversationsModel != null && conversationsModel.isValid()) {
                        changeItemAtPosition(i, realm.copyFromRealm(conversationsModel));
                    }
                    if (i != 0)
                        MoveItemToPosition(i, 0);
                    break;
                }

            }
        } catch (Exception e) {
            AppHelper.LogCat(e);
        } finally {
            if (realm != null && !realm.isClosed()) {
                realm.close();
            }
        }
    }

    private void changeItemAtPosition(int position, ConversationsModel conversationsModel) {
        mConversations.set(position, conversationsModel);
        notifyItemChanged(toAdapterPosition(position));
    }

    private void MoveItemToPosition(int fromPosition, int toPosition) {
        ConversationsModel model = mConversations.remove(fromPosition);
        mConversations.add(toPosition, model);
        notifyItemMoved(
                toAdapterPosition(fromPosition),
                toAdapterPosition(toPosition));
        if (conversationList != null) {
            conversationList.scrollToPosition(toAdapterPosition(toPosition));
        }
    }

    public static int getConversationId(int recipientId, int senderId, Realm realm) {
        ConversationsModel conversationsModelNew = realm.where(ConversationsModel.class)
                .beginGroup()
                .equalTo("RecipientID", recipientId)
                .or()
                .equalTo("RecipientID", senderId)
                .endGroup()
                .findFirst();
        return conversationsModelNew != null ? conversationsModelNew.getId() : 0;
    }

    public void updateItem(int userId, boolean isOnline) {
        Realm realm = null;
        try {
            realm = WhatsCloneApplication.getRealmDatabaseInstance();
            int conversationId = getConversationId(userId, PreferenceManager.getID(WhatsCloneApplication.getInstance()), realm);

            int arraySize = mConversations.size();
            for (int i = 0; i < arraySize; i++) {
                ConversationsModel model = mConversations.get(i);
                if (conversationId == model.getId()) {
                    ConversationsModel conversationsModel = realm.where(ConversationsModel.class).equalTo("id", conversationId).findFirst();
                    if (conversationsModel != null && conversationsModel.isValid()) {
                        conversationsModel.setOnline(isOnline);
                        changeItemAtPosition(i, realm.copyFromRealm(conversationsModel));
                    }
                    break;
                }
            }
        } catch (Exception e) {
            AppHelper.LogCat(e);
        } finally {
            if (realm != null && !realm.isClosed()) {
                realm.close();
            }
        }
    }

    private RealmList<MembersGroupModel> buildLocalGroupMembers(int serverGroupId, List<MembersGroupModel> localMembers) {
        RealmList<MembersGroupModel> members = new RealmList<>();
        Set<Integer> copiedUserIds = new HashSet<>();
        if (localMembers == null) return members;

        for (MembersGroupModel localMember : localMembers) {
            if (localMember == null || localMember.getUserId() <= 0 || !copiedUserIds.add(localMember.getUserId())) {
                continue;
            }
            MembersGroupModel member = new MembersGroupModel();
            member.setId(getLocalMemberId(serverGroupId, localMember.getUserId()));
            member.setGroupID(serverGroupId);
            member.setUserId(localMember.getUserId());
            member.setUsername(localMember.getUsername());
            member.setWalletAddress(localMember.getWalletAddress());
            member.setLinked(localMember.isLinked());
            member.setImage(localMember.getImage());
            member.setStatus(localMember.getStatus());
            member.setStatus_date(localMember.getStatus_date());
            member.setRole(localMember.getRole());
            member.setLeft(localMember.isLeft());
            member.setAdmin(localMember.isAdmin());
            member.setDeleted(localMember.isDeleted());
            members.add(member);
        }

        return members;
    }

    private int getLocalMemberId(int groupId, int userId) {
        return -Math.abs((groupId * 1000) + userId);
    }

    private void openGroupConversation(Context context, int conversationId, int groupId, int recipientId) {
        RateHelper.significantEvent(context);
        Intent messagingIntent = new Intent(context, MessagesActivity.class);
        messagingIntent.putExtra("conversationID", conversationId);
        messagingIntent.putExtra("groupID", groupId);
        messagingIntent.putExtra("isGroup", true);
        messagingIntent.putExtra("recipientID", recipientId);
        context.startActivity(messagingIntent);
        if (context instanceof Activity) {
            AnimationsUtil.setSlideInAnimation((Activity) context);
        }
    }


    static class SupportViewHolder extends RecyclerView.ViewHolder {
        SupportViewHolder(View itemView) {
            super(itemView);
        }
    }

    class AdViewHolder extends RecyclerView.ViewHolder {
        AdViewHolder(View itemView) {
            super(itemView);
        }
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {

        Context context;
        @BindView(R.id.user_image)
        ImageView userImage;

        @BindView(R.id.online_indicator)
        View onlineIndicator;

        @BindView(R.id.username)
        EmojiconTextView username;

        @BindView(R.id.last_message)
        EmojiconTextView lastMessage;

        @BindView(R.id.counter)
        TextView counter;

        @BindView(R.id.date_message)
        TextView messageDate;

        @BindView(R.id.status_messages)
        ImageView status_messages;
        @BindView(R.id.file_types)
        ImageView isFile;
        @BindView(R.id.file_types_text)
        TextView FileContent;

        @BindView(R.id.create_group_pro_bar)
        ProgressBar progressBarGroup;

        @BindView(R.id.conversation_row)
        LinearLayout ConversationRow;


        @BindView(R.id.select_icon)
        LinearLayout selectIcon;

        ConversationViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            username.setSelected(true);
            context = itemView.getContext();
            setTypeFaces();
        }

        private void setTypeFaces() {
            if (AppConstants.ENABLE_FONTS_TYPES) {
                username.setTypeface(AppHelper.setTypeFace(context, "Futura"));
                lastMessage.setTypeface(AppHelper.setTypeFace(context, "Futura"));
                counter.setTypeface(AppHelper.setTypeFace(context, "Futura"));
                messageDate.setTypeface(AppHelper.setTypeFace(context, "Futura"));
                FileContent.setTypeface(AppHelper.setTypeFace(context, "Futura"));

            }
        }

        void getProgressBarGroup() {
            progressBarGroup.setVisibility(View.VISIBLE);
        }

        void setProgressBarGroup() {
            progressBarGroup.setVisibility(View.GONE);
        }

        @SuppressLint("SetTextI18n")
        void setTypeFile(String type) {
            isFile.setVisibility(View.VISIBLE);
            FileContent.setVisibility(View.VISIBLE);
            switch (type) {
                case "image":
                    isFile.setImageResource(R.drawable.ic_photo_camera_gray_24dp);
                    FileContent.setText("Image");
                    break;
                case "video":
                    isFile.setImageResource(R.drawable.ic_videocam_gray_24dp);
                    FileContent.setText("Video");
                    break;
                case "audio":
                    isFile.setImageResource(R.drawable.ic_headset_gray_24dp);
                    FileContent.setText("Audio");
                    break;
                case "document":
                    isFile.setImageResource(R.drawable.ic_document_file_gray_24dp);
                    FileContent.setText("Document");
                    break;
            }

        }

        void isOnline() {
            onlineIndicator.setVisibility(View.VISIBLE);
        }

        void isOffline() {
            onlineIndicator.setVisibility(View.GONE);
        }

        void setGroupImageOffline(String ImageUrl, String name) {
            TextDrawable drawable = textDrawable(name);
            Glide.with(context.getApplicationContext())
                    .load(ImageUrl)
                    .asBitmap()
                    .centerCrop()
                    .transform(new CropCircleTransformation(context.getApplicationContext()))
                    .placeholder(drawable)
                    .error(drawable)
                    .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                    .into(userImage);
        }

        void setGroupImage(String ImageUrl, int groupId, String name) {
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, context, groupId, AppConstants.GROUP, AppConstants.ROW_PROFILE);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    if (bitmap != null) {
                        ImageLoader.SetBitmapImage(bitmap, userImage);
                    } else {
                        TextDrawable drawable = textDrawable(name);
                        BitmapImageViewTarget target = new BitmapImageViewTarget(userImage) {
                            @Override
                            public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                                super.onResourceReady(bitmap, anim);
                                userImage.setImageBitmap(bitmap);
                                ImageLoader.DownloadImage(memoryCache, EndPoints.ROWS_IMAGE_URL + ImageUrl, ImageUrl, context, groupId, AppConstants.GROUP, AppConstants.ROW_PROFILE);

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
                        Glide.with(context.getApplicationContext())
                                .load(EndPoints.ROWS_IMAGE_URL + ImageUrl)
                                .asBitmap()
                                .centerCrop()
                                .transform(new CropCircleTransformation(context.getApplicationContext()))
                                .placeholder(drawable)
                                .error(drawable)
                                .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                                .into(target);
                    }
                }
            }.execute();


        }

        TextDrawable textDrawable(String name) {
            if (name == null || name.trim().isEmpty()) {
                name = context.getApplicationContext().getString(R.string.app_name);
            }
            ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
            // generate random color
            int color = generator.getColor(name);
            String c = String.valueOf(name.toUpperCase().charAt(0));
            return TextDrawable.builder().buildRound(c, color);


        }

        void setUserImage(String ImageUrl, int recipientId, String name) {

            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    return ImageLoader.GetCachedBitmapImage(memoryCache, ImageUrl, context, recipientId, AppConstants.USER, AppConstants.ROW_PROFILE);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    if (bitmap != null) {
                        ImageLoader.SetBitmapImage(bitmap, userImage);
                    } else {
                        TextDrawable drawable = textDrawable(name);

                        BitmapImageViewTarget target = new BitmapImageViewTarget(userImage) {
                            @Override
                            public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                                super.onResourceReady(bitmap, anim);
                                userImage.setImageBitmap(bitmap);
                                ImageLoader.DownloadImage(memoryCache, EndPoints.ROWS_IMAGE_URL + ImageUrl, ImageUrl, context, recipientId, AppConstants.USER, AppConstants.ROW_PROFILE);

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
                        Glide.with(context.getApplicationContext())
                                .load(EndPoints.ROWS_IMAGE_URL + ImageUrl)
                                .asBitmap()
                                .centerCrop()
                                .transform(new CropCircleTransformation(context.getApplicationContext()))
                                .placeholder(drawable)
                                .error(drawable)
                                // .signature(new StringSignature(System.currentTimeMillis()+""))
                                .override(AppConstants.ROWS_IMAGE_SIZE, AppConstants.ROWS_IMAGE_SIZE)
                                .into(target);
                    }
                }
            }.execute();
        }

        void setUsername(String user) {
            username.setText(user);

        }

        void setLastMessage(String LastMessage) {
            lastMessage.setVisibility(View.VISIBLE);
            lastMessage.setTextColor(AppHelper.getColor(context, R.color.colorGray2));
            String last = UtilsString.unescapeJava(LastMessage);
            if (last.length() > 18)
                lastMessage.setText(String.format("%s... ", last.substring(0, 18)));
            else
                lastMessage.setText(last);

        }

        void setMessageDate(String MessageDate) {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    return UtilsTime.convertDateToString(context, UtilsTime.getCorrectDate(params[0]));
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    messageDate.setText(result);
                }
            }.execute(MessageDate);

        }

        void hideSent() {
            status_messages.setVisibility(View.GONE);
        }

        void showSent(int status) {
            status_messages.setVisibility(View.VISIBLE);
            switch (status) {
                case AppConstants.IS_WAITING:
                    status_messages.setImageResource(R.drawable.ic_access_time_gray_24dp);
                    break;
                case AppConstants.IS_SENT:
                    status_messages.setImageResource(R.drawable.ic_done_gray_24dp);
                    break;
                case AppConstants.IS_DELIVERED:
                    status_messages.setImageResource(R.drawable.ic_done_all_gray_24dp);
                    break;
                case AppConstants.IS_SEEN:
                    status_messages.setImageResource(R.drawable.ic_done_all_blue_24dp);
                    break;

            }

        }

        void setCounter(String Counter) {
            counter.setText(Counter.toUpperCase());
        }

        void hideCounter() {
            counter.setVisibility(View.GONE);
        }


        void showCounter() {
            counter.setVisibility(View.VISIBLE);
        }

        void ChangeStatusUnread() {
            messageDate.setTypeface(null, Typeface.BOLD);
            username.setTypeface(null, Typeface.BOLD);
            if (AppConstants.ENABLE_FONTS_TYPES)
                username.setTypeface(AppHelper.setTypeFace(context, "Futura"));
            messageDate.setTextColor(ContextCompat.getColor(context, R.color.colorAccentSecondary));
        }

        void ChangeStatusRead() {
            messageDate.setTypeface(null, Typeface.NORMAL);
            username.setTypeface(null, Typeface.BOLD);
            if (AppConstants.ENABLE_FONTS_TYPES)
                username.setTypeface(AppHelper.setTypeFace(context, "Futura"));
            messageDate.setTextColor(ContextCompat.getColor(context, R.color.colorGray2));
            username.setTextColor(ContextCompat.getColor(context, R.color.colorBlack));
        }

        void setOnClickListener(View.OnClickListener listener) {
            itemView.setOnClickListener(listener);
            userImage.setOnClickListener(listener);
        }

    }

}
