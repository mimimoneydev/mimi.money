package com.money.mimi.fragments.home;


import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.GestureDetectorCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.money.mimi.R;
import com.money.mimi.adapters.recyclerView.messages.ConversationsAdapter;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.notifications.NotificationsManager;
import com.money.mimi.interfaces.LoadingData;
import com.money.mimi.models.groups.GroupsModel;
import com.money.mimi.models.messages.ConversationsModel;
import com.money.mimi.models.messages.MessagesModel;
import com.money.mimi.models.users.Pusher;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.presenters.messages.ConversationsPresenter;
import com.money.mimi.services.MainService;
import com.money.mimi.ui.CustomProgressView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

import static com.money.mimi.app.AppConstants.EVENT_BUS_ACTION_MODE_FINISHED;
import static com.money.mimi.app.AppConstants.EVENT_BUS_CONTACTS_PERMISSION;
import static com.money.mimi.app.AppConstants.EVENT_BUS_CREATE_NEW_GROUP;
import static com.money.mimi.app.AppConstants.EVENT_BUS_DELETE_CONVERSATION_ITEM;
import static com.money.mimi.app.AppConstants.EVENT_BUS_EXIT_NEW_GROUP;
import static com.money.mimi.app.AppConstants.EVENT_BUS_IMAGE_GROUP_UPDATED;
import static com.money.mimi.app.AppConstants.EVENT_BUS_ITEM_IS_ACTIVATED;
import static com.money.mimi.app.AppConstants.EVENT_BUS_MESSAGE_COUNTER;
import static com.money.mimi.app.AppConstants.EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_CONVERSATIONS;
import static com.money.mimi.app.AppConstants.EVENT_BUS_MESSAGE_IS_READ;
import static com.money.mimi.app.AppConstants.EVENT_BUS_MESSAGE_IS_SEEN_FOR_CONVERSATIONS;
import static com.money.mimi.app.AppConstants.EVENT_BUS_NEW_MESSAGE_CONVERSATION_NEW_ROW;
import static com.money.mimi.app.AppConstants.EVENT_BUS_NEW_MESSAGE_CONVERSATION_OLD_ROW;
import static com.money.mimi.app.AppConstants.EVENT_BUS_NEW_MESSAGE_IS_SENT_FOR_CONVERSATIONS;
import static com.money.mimi.app.AppConstants.EVENT_UPDATE_CONVERSATION_OLD_ROW;

/**
 * Created by Abderrahim El imame  on 20/01/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class ConversationsFragment extends Fragment implements LoadingData, RecyclerView.OnItemTouchListener, ActionMode.Callback {

    @BindView(R.id.ConversationsList)
    RecyclerView ConversationList;
    @BindView(R.id.empty)
    LinearLayout emptyConversations;

    @BindView(R.id.swipeConversations)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @BindView(R.id.progress_bar_load)
    CustomProgressView progressBarLoad;

    private ConversationsAdapter mConversationsAdapter;
    private ConversationsPresenter mConversationsPresenter;
    private GestureDetectorCompat gestureDetector;
    private ActionMode actionMode;


    private View mView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_conversations, container, false);
        ButterKnife.bind(this, mView);
        initializerView();
        mConversationsPresenter = new ConversationsPresenter(this);
        mConversationsPresenter.onCreate();
        return mView;
    }

    /**
     * method to initialize the view
     */
    private void initializerView() {
        setHasOptionsMenu(true);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mConversationsAdapter = new ConversationsAdapter(ConversationList);
        mConversationsAdapter.setSupportAgentEnabled(true);
        mConversationsAdapter.setAdContext(getActivity());
        ConversationList.setLayoutManager(mLinearLayoutManager);
        ConversationList.setAdapter(mConversationsAdapter);
        ConversationList.setItemAnimator(new DefaultItemAnimator());
        ConversationList.getItemAnimator().setChangeDuration(0);
        ConversationList.getItemAnimator().setMoveDuration(0);
        ConversationList.getItemAnimator().setAddDuration(0);
        ConversationList.getItemAnimator().setRemoveDuration(0);

        //fix slow recyclerview start
        ConversationList.setHasFixedSize(true);
        ConversationList.setItemViewCacheSize(10);
        ConversationList.setDrawingCacheEnabled(true);
        ConversationList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        ///fix slow recyclerview end
        ConversationList.addOnItemTouchListener(this);
        gestureDetector = new GestureDetectorCompat(getActivity(), new RecyclerViewBenOnGestureListener());
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorAccent, R.color.colorGreenLight);
        mSwipeRefreshLayout.setOnRefreshListener(() -> mConversationsPresenter.onRefresh());

    }


    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }


    /**
     * method to toggle the selection
     *
     * @param position
     */
    private void ToggleSelection(int position) {
        mConversationsAdapter.toggleSelection(position);
        String title = String.format("%s selected", mConversationsAdapter.getSelectedItemCount());
        actionMode.setTitle(title);


    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.select_conversation_menu, menu);
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_ACTION_MODE_STARTED));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {


        switch (item.getItemId()) {
            case R.id.delete_conversations:

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());


                builder.setMessage(R.string.alert_message_delete_conversation);

                builder.setPositiveButton(R.string.Yes, (dialog, whichButton) -> {
                    int arraySize = mConversationsAdapter.getSelectedItems().size();
                    Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                    AppHelper.LogCat("start delete " + arraySize);
                    if (arraySize != 0) {

                        AppHelper.showDialog(getActivity(), getString(R.string.deleting_chat));

                        for (int x = 0; x < arraySize; x++) {
                            int currentPosition = mConversationsAdapter.getSelectedItems().get(x);
                            try {
                                ConversationsModel conversationsModel = mConversationsAdapter.getItem(currentPosition);
                                if (conversationsModel == null) {
                                    continue;
                                }
                                int selectedConversationId = conversationsModel.getId();
                                int conversationID = getConversationId(conversationsModel.getRecipientID(), PreferenceManager.getID(getActivity()), realm);
                                realm.executeTransactionAsync(realm1 -> {
                                    RealmResults<MessagesModel> messagesModel1 = realm1.where(MessagesModel.class).equalTo("conversationID", conversationID).findAll();
                                    messagesModel1.deleteAllFromRealm();
                                }, () -> {
                                    AppHelper.LogCat("Message Deleted  successfully  ConversationsFragment");
                                    mConversationsAdapter.DeleteConversationItem(selectedConversationId);
                                    realm.executeTransactionAsync(realm1 -> {
                                        ConversationsModel conversationsModel1 = realm1.where(ConversationsModel.class).equalTo("id", conversationID).findFirst();
                                        conversationsModel1.deleteFromRealm();
                                    }, () -> {
                                        AppHelper.LogCat("Conversation deleted successfully ConversationsFragment");
                                        EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_COUNTER));
                                        EventBus.getDefault().post(new Pusher(EVENT_BUS_DELETE_CONVERSATION_ITEM, conversationID));
                                        NotificationsManager.SetupBadger(getActivity());
                                    }, error -> {
                                        AppHelper.LogCat("Delete conversation failed  ConversationsFragment" + error.getMessage());

                                    });
                                }, error -> {
                                    AppHelper.LogCat("Delete message failed ConversationsFragment" + error.getMessage());

                                });

                            } catch (Exception e) {
                                AppHelper.LogCat(e);
                            }
                        }
                        AppHelper.LogCat("finish delete");
                        AppHelper.hideDialog();
                    } else {
                        AppHelper.CustomToast(getActivity(), "Delete conversation failed  ");
                    }
                    if (actionMode != null) {
                        mConversationsAdapter.clearSelections();
                        actionMode.finish();
                        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
                    }
                    realm.close();
                });


                builder.setNegativeButton(R.string.No, (dialog, whichButton) -> {

                });

                builder.show();
                return true;
            default:
                return false;
        }
    }

    /**
     * method to get a conversation id
     *
     * @param recipientId this is the first parameter for getConversationId method
     * @param senderId    this is the second parameter for getConversationId method
     * @param realm       this is the thirded parameter for getConversationId method
     * @return conversation id
     */
    private int getConversationId(int recipientId, int senderId, Realm realm) {
        ConversationsModel conversationsModelNew = realm.where(ConversationsModel.class)
                .beginGroup()
                .equalTo("RecipientID", recipientId)
                .or()
                .equalTo("RecipientID", senderId)
                .endGroup()
                .findFirst();
        return conversationsModelNew != null ? conversationsModelNew.getId() : 0;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.actionMode = null;
        mConversationsAdapter.clearSelections();
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_ACTION_MODE_DESTROYED));
    }

    public void sendGroupMessage(GroupsModel groupsModel, MessagesModel messagesModel) {
        Realm realmCreateGroup = WhatsCloneApplication.getRealmDatabaseInstance();
        ContactsModel contactsModel = realmCreateGroup.where(ContactsModel.class).equalTo("id", PreferenceManager.getID(getActivity())).findFirst();
        new Handler().postDelayed(() -> MainService.sendMessagesGroup(getActivity(), contactsModel, groupsModel, messagesModel), 500);
        realmCreateGroup.close();
    }

    public void sendGroupMessage(GroupsModel groupsModel, int conversationID) {
        new Handler().postDelayed(() -> {
            Realm realmCreateGroup = WhatsCloneApplication.getRealmDatabaseInstance();
            ContactsModel contactsModel = realmCreateGroup.where(ContactsModel.class).equalTo("id", PreferenceManager.getID(getActivity())).findFirst();
            MessagesModel messagesModel = realmCreateGroup.where(MessagesModel.class).notEqualTo("id", 0).equalTo("conversationID", conversationID).equalTo("isGroup", true).findFirst();
            MainService.sendMessagesGroup(getActivity(), contactsModel, groupsModel, messagesModel);
            realmCreateGroup.close();
        }, 500);

    }


    private class RecyclerViewBenOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }

        public void onLongPress(MotionEvent e) {
            try {
                View view = ConversationList.findChildViewUnder(e.getX(), e.getY());
                if (view == null) return;
                int currentPosition = ConversationList.getChildAdapterPosition(view);
                ConversationsModel conversationsModel = mConversationsAdapter.getItem(currentPosition);
                if (conversationsModel == null || !conversationsModel.isValid()) return;
                if (!conversationsModel.isGroup()) {
                    if (actionMode != null) {
                        return;
                    }
                    actionMode = getActivity().startActionMode(ConversationsFragment.this);
                    ToggleSelection(currentPosition);
                }
                super.onLongPress(e);
            } catch (Exception e1) {
                AppHelper.LogCat(" onLongPress " + e1.getMessage());
            }


        }

    }

    public void onProgressShow() {
        if (!isAdded() || progressBarLoad == null) return;
        progressBarLoad.setVisibility(View.VISIBLE);
        progressBarLoad.setColor(AppHelper.getColor(getActivity(), R.color.colorPrimaryDark));
    }

    public void onProgressHide() {
        if (progressBarLoad == null) return;
        progressBarLoad.setVisibility(View.GONE);
    }

    /**
     * method to show conversation list
     *
     * @param conversationsModels this is parameter for  ShowConversation  method
     */
    public void UpdateConversation(List<ConversationsModel> conversationsModels) {
        if (!isAdded() || ConversationList == null || emptyConversations == null || mConversationsAdapter == null) {
            return;
        }

        ConversationList.setVisibility(View.VISIBLE);
        emptyConversations.setVisibility(View.GONE);
        RealmList<ConversationsModel> conversationsModels1 = new RealmList<>();
        conversationsModels1.addAll(conversationsModels);
        mConversationsAdapter.setConversations(conversationsModels1);
    }

    public void refreshInlineAds() {
        if (mConversationsAdapter != null) {
            if (actionMode != null) {
                actionMode.finish();
            }
            mConversationsAdapter.refreshAds();
        }
    }

    @Override
    public void onDestroyView() {
        if (mConversationsAdapter != null) {
            mConversationsAdapter.releaseAds();
        }
        super.onDestroyView();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mConversationsPresenter != null)
            mConversationsPresenter.onDestroy();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onShowLoading() {
        if (mSwipeRefreshLayout == null) return;
        if (!mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void onHideLoading() {
        if (mSwipeRefreshLayout == null) return;
        if (mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onErrorLoading(Throwable throwable) {
        AppHelper.LogCat(throwable);
        if (mSwipeRefreshLayout == null) return;
        if (mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * method to add a new message to list messages
     *
     * @param conversationId this is the parameter for addConversationEventMainThread
     */

    private void addConversationEventMainThread(int conversationId) {
        mConversationsAdapter.addConversationItem(conversationId);
        ConversationList.setVisibility(View.VISIBLE);
        emptyConversations.setVisibility(View.GONE);
        ConversationList.scrollToPosition(0);
    }

    /**
     * method of EventBus
     *
     * @param pusher this is parameter of onEventMainThread method
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Pusher pusher) {
        int messageId = pusher.getMessageId();
        switch (pusher.getAction()) {

            case EVENT_BUS_ITEM_IS_ACTIVATED:
                int idx = ConversationList.getChildAdapterPosition(pusher.getView());
                if (actionMode != null) {
                    ToggleSelection(idx);
                    return;
                }

                break;
            case EVENT_BUS_NEW_MESSAGE_CONVERSATION_NEW_ROW:
                new Handler().postDelayed(() -> addConversationEventMainThread(pusher.getConversationId()), 500);
                break;
/*
            case EVENT_BUS_NEW_MESSAGE_GROUP_CONVERSATION_NEW_ROW:
                mConversationsPresenter.getGroupInfo(pusher.getGroupID());
                break;*/
            case EVENT_BUS_NEW_MESSAGE_CONVERSATION_OLD_ROW:
                new Handler().postDelayed(() -> mConversationsAdapter.updateConversationItem(pusher.getConversationId()), 500);
                break;
            case AppConstants.EVENT_BUS_UPDATE_USER_STATE:
                if (pusher.getData().equals(AppConstants.EVENT_BUS_USER_IS_ONLINE))
                    mConversationsAdapter.updateItem(pusher.getUserID(), true);
                else if (pusher.getData().equals(AppConstants.EVENT_BUS_USER_IS_OFFLINE))
                    mConversationsAdapter.updateItem(pusher.getUserID(), false);
                break;
            case EVENT_BUS_MESSAGE_IS_READ:
            case EVENT_UPDATE_CONVERSATION_OLD_ROW:
            case EVENT_BUS_NEW_MESSAGE_IS_SENT_FOR_CONVERSATIONS:
            case EVENT_BUS_MESSAGE_IS_SEEN_FOR_CONVERSATIONS:
            case EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_CONVERSATIONS:
                new Handler().postDelayed(() -> mConversationsAdapter.updateStatusConversationItem(pusher.getConversationId()), 500);
                break;
            case EVENT_BUS_DELETE_CONVERSATION_ITEM:
                mConversationsAdapter.DeleteConversationItem(pusher.getConversationId());
                showEmptyView();
                break;
            case EVENT_BUS_CREATE_NEW_GROUP:
                mConversationsPresenter.getGroupInfo(pusher.getGroupID(), pusher.getConversationId());
                break;
            case EVENT_BUS_EXIT_NEW_GROUP:
                MessagesModel messagesModel2 = pusher.getMessagesModel();
                mConversationsPresenter.getGroupInfo(pusher.getGroupID(), messagesModel2);
                break;
            case EVENT_BUS_CONTACTS_PERMISSION:
                mConversationsPresenter.onRefresh();
                break;
            case EVENT_BUS_IMAGE_GROUP_UPDATED:
                mConversationsPresenter.getGroupInfo(pusher.getGroupID());
                break;
            case EVENT_BUS_ACTION_MODE_FINISHED:
                if (actionMode != null) {
                    mConversationsAdapter.clearSelections();
                    actionMode.finish();
                    ((AppCompatActivity) getActivity()).getSupportActionBar().show();
                }
                break;

        }
    }

    private void showEmptyView() {
        if (mConversationsAdapter.getItemCount() == 0) {
            ConversationList.setVisibility(View.GONE);
            emptyConversations.setVisibility(View.VISIBLE);
        }
    }

}
