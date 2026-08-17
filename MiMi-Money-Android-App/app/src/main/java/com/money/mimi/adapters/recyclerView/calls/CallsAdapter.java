package com.money.mimi.adapters.recyclerView.calls;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.money.mimi.R;
import com.money.mimi.activities.call.CallDetailsActivity;
import com.money.mimi.activities.profile.ProfilePreviewActivity;
import com.money.mimi.activities.settings.PreferenceSettingsManager;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.EndPoints;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.AdMobHelper;
import com.money.mimi.helpers.InlineAdPositionHelper;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.UtilsTime;
import com.money.mimi.helpers.call.CallManager;
import com.money.mimi.models.calls.CallsModel;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.ui.ColorGenerator;
import com.money.mimi.ui.TextDrawable;

import org.joda.time.DateTime;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by Abderrahim El imame on 12/3/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class CallsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_AD = 1;

    private RealmList<CallsModel> callsModelList;
    private MemoryCache memoryCache;
    private RecyclerView callList;
    private String SearchQuery;
    private Context adContext;
    private final Set<AdView> activeAdViews = new HashSet<>();


    public CallsAdapter(RecyclerView callList) {
        this.callList = callList;
        this.memoryCache = new MemoryCache();
        this.callsModelList = new RealmList<>();
    }

    public CallsAdapter() {
        this.memoryCache = new MemoryCache();
        this.callsModelList = new RealmList<>();
    }

    public void setAdContext(Context context) {
        this.adContext = context;
    }

    private boolean shouldShowAds() {
        return adContext != null && PreferenceManager.ShowBannerAds(adContext)
                && AdMobHelper.bannerId(
                        adContext, PreferenceManager.getUnitBannerAdsID(adContext)) != null;
    }

    private int getRealPosition(int adapterPosition) {
        return InlineAdPositionHelper.toDataPosition(adapterPosition, shouldShowAds());
    }

    private boolean isAdPosition(int adapterPosition) {
        return InlineAdPositionHelper.isAdPosition(adapterPosition, shouldShowAds());
    }

    public void setCalls(RealmList<CallsModel> callsModelList) {
        this.callsModelList = callsModelList;
        notifyDataSetChanged();
    }


    //Methods for search start
    public void setString(String SearchQuery) {
        this.SearchQuery = TextUtils.isEmpty(SearchQuery) ? null : SearchQuery;
        notifyDataSetChanged();
    }

    public void animateTo(List<CallsModel> models) {
        applyAndAnimateRemovals(models);
        applyAndAnimateAdditions(models);
        applyAndAnimateMovedItems(models);
    }

    private void applyAndAnimateRemovals(List<CallsModel> newModels) {
        int arraySize = callsModelList.size();
        for (int i = arraySize - 1; i >= 0; i--) {
            final CallsModel model = callsModelList.get(i);
            if (!newModels.contains(model)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<CallsModel> newModels) {
        int arraySize = newModels.size();
        for (int i = 0; i < arraySize; i++) {
            final CallsModel model = newModels.get(i);
            if (!callsModelList.contains(model)) {
                addItem(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<CallsModel> newModels) {
        int arraySize = newModels.size();
        for (int toPosition = arraySize - 1; toPosition >= 0; toPosition--) {
            final CallsModel model = newModels.get(toPosition);
            final int fromPosition = callsModelList.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private CallsModel removeItem(int position) {
        final CallsModel model = callsModelList.remove(position);
        notifyDataSetChanged();
        return model;
    }

    private void addItem(int position, CallsModel model) {
        callsModelList.add(position, model);
        notifyDataSetChanged();
    }

    private void moveItem(int fromPosition, int toPosition) {
        final CallsModel model = callsModelList.remove(fromPosition);
        callsModelList.add(toPosition, model);
        notifyItemMoved(
                InlineAdPositionHelper.toAdapterPosition(fromPosition, shouldShowAds()),
                InlineAdPositionHelper.toAdapterPosition(toPosition, shouldShowAds()));
    }
    //Methods for search end


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_AD) {
            View adView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_ad_banner, parent, false);
            return new AdViewHolder(adView);
        }
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_calls, parent, false);
        return new CallsViewHolder(itemView);
    }

    @Override
    public int getItemViewType(int position) {
        if (isAdPosition(position)) {
            return TYPE_AD;
        }
        return TYPE_ITEM;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

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

        final CallsViewHolder callsViewHolder = (CallsViewHolder) holder;
        int realPosition = getRealPosition(position);
        if (callsModelList == null || realPosition < 0 || realPosition >= callsModelList.size()) {
            return;
        }
        final CallsModel callsModel = callsModelList.get(realPosition);
        Context context = callsViewHolder.itemView.getContext();
        if (!(context instanceof Activity) || callsModel == null || !callsModel.isValid()) {
            return;
        }
        Activity mActivity = (Activity) context;
        try {
            String Username;
            ContactsModel contactsModel = callsModel.getContactsModel();
            if (contactsModel != null && contactsModel.getUsername() != null && !contactsModel.getUsername().isEmpty()) {
                Username = contactsModel.getUsername();
            } else if (callsModel.getWalletAddress() != null && !callsModel.getWalletAddress().isEmpty()) {
                Username = callsModel.getWalletAddress();
            } else {
                Username = "Unknown";
            }

            SpannableString Message = SpannableString.valueOf(Username);
            if (SearchQuery != null) {
                int index = TextUtils.indexOf(Username.toLowerCase(), SearchQuery.toLowerCase());
                if (index >= 0) {
                    Message.setSpan(new ForegroundColorSpan(AppHelper.getColor(mActivity, R.color.colorSpanSearch)), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    Message.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), index, index + SearchQuery.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                callsViewHolder.username.setText(Message, TextView.BufferType.SPANNABLE);
                callsViewHolder.username.setTextSize(PreferenceSettingsManager.getMessage_font_size(mActivity));
            } else {
                callsViewHolder.username.setText(Username, TextView.BufferType.NORMAL);
                callsViewHolder.username.setTextSize(PreferenceSettingsManager.getMessage_font_size(mActivity));
            }


            if (callsModel.isReceived()) {
                callsViewHolder.showIcon();
            } else {
                callsViewHolder.hideIcon();
            }
            String callType = callsModel.getType();
            if (AppConstants.VIDEO_CALL.equals(callType)) {
                callsViewHolder.showVideoButton();
            } else {
                callsViewHolder.hideVideoButton();

            }
            String contactImage = contactsModel != null ? contactsModel.getImage() : null;
            int contactId = contactsModel != null ? contactsModel.getId() : 0;
            callsViewHolder.setUserImage(contactImage, contactId, Username);

            if (callsModel.getDate() != null) {
                callsViewHolder.setCallDate(callsModel.getDate());
            } else {
                callsViewHolder.CallDate.setText("");
            }

            if (callsModel.getCounter() != 0 && callsModel.getCounter() > 1)
                callsViewHolder.setCallCounter(callsModel.getCounter());
            else
                callsViewHolder.counterCall.setVisibility(View.GONE);


        callsViewHolder.setOnClickListener(v -> {
            switch (v.getId()) {
                case R.id.CallVideoBtn:
                    if (callsModel.isReceived())
                        CallManager.callContact(mActivity, false, true, callsModel.getFrom());
                    else
                        CallManager.callContact(mActivity, false, true, callsModel.getTo());
                    break;
                case R.id.CallBtn:
                    if (callsModel.isReceived())
                        CallManager.callContact(mActivity, false, false, callsModel.getFrom());
                    else
                        CallManager.callContact(mActivity, false, false, callsModel.getTo());
                    break;
                case R.id.user_image:
                    if (AppHelper.isAndroid5()) {
                        if (contactsModel != null && contactsModel.isLinked() && contactsModel.isActivate()) {
                            Intent mIntent = new Intent(mActivity, ProfilePreviewActivity.class);
                            mIntent.putExtra("userID", contactsModel.getId());
                            mIntent.putExtra("isGroup", false);
                            mActivity.startActivity(mIntent);
                        }
                    } else {
                        if (contactsModel != null && contactsModel.isLinked() && contactsModel.isActivate()) {
                            Intent mIntent = new Intent(mActivity, ProfilePreviewActivity.class);
                            mIntent.putExtra("userID", contactsModel.getId());
                            mActivity.startActivity(mIntent);
                            mActivity.overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);
                        }
                    }

                    break;
                default:
                    if (contactsModel == null) {
                        break;
                    }
                    Intent mIntent = new Intent(mActivity, CallDetailsActivity.class);
                    mIntent.putExtra("userID", contactsModel.getId());
                    mIntent.putExtra("callID", callsModel.getId());
                    mActivity.startActivity(mIntent);
                    AnimationsUtil.setSlideInAnimation(mActivity);
                    break;

            }
        });

        } catch (Exception e) {
            AppHelper.LogCat(e.getMessage());
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
        int dataSize = callsModelList == null ? 0 : callsModelList.size();
        return InlineAdPositionHelper.getItemCount(dataSize, shouldShowAds());
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


    public CallsModel getItem(int position) {
        if (callsModelList == null || position < 0 || position >= getItemCount() || isAdPosition(position)) {
            return null;
        }
        int realPosition = getRealPosition(position);
        if (realPosition < 0 || realPosition >= callsModelList.size()) {
            return null;
        }
        return callsModelList.get(realPosition);
    }


    /**
     * method to check if a  call exist
     *
     * @param callId this is the first parameter for  checkIfCallExist method
     * @param realm  this is the second parameter for  checkIfCallExist  method
     * @return return value
     */
    private boolean checkIfCallExist(int callId, Realm realm) {
        RealmQuery<CallsModel> query = realm.where(CallsModel.class).equalTo("id", callId);
        return query.count() != 0;

    }

    public void addCallItem(int callId) {
        Realm realm = null;
        try {
            realm = WhatsCloneApplication.getRealmDatabaseInstance();
            CallsModel callsModel = realm.where(CallsModel.class).equalTo("id", callId).findFirst();
            if (callsModel != null && !isCallExistInList(callsModel.getId())) {
                addCallItem(0, realm.copyFromRealm(callsModel));
            }

        } catch (Exception e) {
            AppHelper.LogCat("addCallItem Exception" + e);
        } finally {
            if (realm != null && !realm.isClosed()) {
                realm.close();
            }
        }
    }

    private void addCallItem(int position, CallsModel callsModel) {
        try {
            this.callsModelList.add(position, callsModel);
            notifyDataSetChanged();
        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
    }

    private boolean isCallExistInList(int callId) {
        int arraySize = callsModelList.size();
        boolean exist = false;
        for (int i = 0; i < arraySize; i++) {
            CallsModel model = callsModelList.get(i);
            if (model != null && model.isValid() && callId == model.getId()) {
                exist = true;
                break;
            }
        }
        return exist;
    }


    public void updateCallItem(int callId) {
        Realm realm = null;
        try {
            realm = WhatsCloneApplication.getRealmDatabaseInstance();
            int arraySize = callsModelList.size();
            for (int i = 0; i < arraySize; i++) {
                CallsModel model = callsModelList.get(i);
                if (model != null && model.isValid() && callId == model.getId()) {
                    CallsModel callsModel = realm.where(CallsModel.class).equalTo("id", callId).findFirst();
                    if (callsModel == null) {
                        break;
                    }
                    changeItemAtPosition(i, realm.copyFromRealm(callsModel));
                    if (i != 0) {
                        MoveItemToPosition(i, 0);
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

    private void changeItemAtPosition(int position, CallsModel callsModel) {
        callsModelList.set(position, callsModel);
        notifyItemChanged(InlineAdPositionHelper.toAdapterPosition(position, shouldShowAds()));
    }

    private void MoveItemToPosition(int fromPosition, int toPosition) {
        CallsModel model = callsModelList.remove(fromPosition);
        callsModelList.add(toPosition, model);
        notifyItemMoved(
                InlineAdPositionHelper.toAdapterPosition(fromPosition, shouldShowAds()),
                InlineAdPositionHelper.toAdapterPosition(toPosition, shouldShowAds()));
        if (callList != null) {
            callList.scrollToPosition(
                    InlineAdPositionHelper.toAdapterPosition(toPosition, shouldShowAds()));
        }
    }

    public void removeCallItem(int position) {
        try {
            callsModelList.remove(position);
            notifyDataSetChanged();
        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
    }

    public void DeleteCallItem(int callID) {
        try {
            int arraySize = callsModelList.size();
            for (int i = 0; i < arraySize; i++) {
                CallsModel model = callsModelList.get(i);
                if (model.isValid()) {
                    if (callID == model.getId()) {
                        removeCallItem(i);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
    }

    class AdViewHolder extends RecyclerView.ViewHolder {
        AdViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class CallsViewHolder extends RecyclerView.ViewHolder {

        Context mActivity;
        @BindView(R.id.user_image)
        ImageView userImage;
        @BindView(R.id.username)
        TextView username;
        @BindView(R.id.CallVideoBtn)
        AppCompatImageView CallVideoBtn;
        @BindView(R.id.CallBtn)
        AppCompatImageView CallBtn;
        @BindView(R.id.icon_made)
        AppCompatImageView IconMade;
        @BindView(R.id.icon_received)
        AppCompatImageView IconReceived;
        @BindView(R.id.date_call)
        TextView CallDate;
        @BindView(R.id.counter_call)
        TextView counterCall;

        public CallsViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mActivity = itemView.getContext();
            setTypeFaces();
        }


        private void setTypeFaces() {
            if (AppConstants.ENABLE_FONTS_TYPES) {
                counterCall.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                CallDate.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
                username.setTypeface(AppHelper.setTypeFace(mActivity, "Futura"));
            }
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

        void hideIcon() {
            IconMade.setVisibility(View.VISIBLE);
            IconReceived.setVisibility(View.GONE);
        }

        void showIcon() {
            IconMade.setVisibility(View.GONE);
            IconReceived.setVisibility(View.VISIBLE);
        }

        void showVideoButton() {
            CallVideoBtn.setVisibility(View.VISIBLE);
            CallBtn.setVisibility(View.GONE);
        }

        void hideVideoButton() {
            CallVideoBtn.setVisibility(View.GONE);
            CallBtn.setVisibility(View.VISIBLE);
        }

        @SuppressLint("StaticFieldLeak")
        void setCallDate(String date) {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    DateTime messageDate = UtilsTime.getCorrectDate(params[0]);
                    return UtilsTime.convertDateToString(mActivity, messageDate);
                }

                @Override
                protected void onPostExecute(String date) {
                    super.onPostExecute(date);
                    CallDate.setText(date);
                }
            }.execute(date);

        }

        void setCallCounter(int counter) {
            counterCall.setVisibility(View.VISIBLE);
            counterCall.setText(String.format("(%d)", counter));
        }


        void setOnClickListener(View.OnClickListener listener) {
            itemView.setOnClickListener(listener);
            userImage.setOnClickListener(listener);
            CallVideoBtn.setOnClickListener(listener);
            CallBtn.setOnClickListener(listener);
        }


    }
}
