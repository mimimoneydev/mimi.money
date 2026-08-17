package com.money.mimi.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.money.mimi.R;
import com.money.mimi.activities.groups.AddMembersToGroupActivity;
import com.money.mimi.adapters.others.TextWatcherAdapter;
import com.money.mimi.adapters.recyclerView.contacts.ContactsAdapter;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.app.AppConstants;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.fragments.home.SaveWalletContactDialogFragment;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.interfaces.LoadingData;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.presenters.users.ContactsPresenter;
import com.money.mimi.ui.RecyclerViewFastScroller;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmList;

/**
 * Created by abderrahimelimame on 6/9/16.
 * Email : abderrahim.elimame@gmail.com
 */

public class NewConversationContactsActivity extends AppCompatActivity implements LoadingData {

    private static final Pattern ETH_ADDRESS_PATTERN = Pattern.compile("0x[a-fA-F0-9]{40}");

    @BindView(R.id.ContactsList)
    RecyclerView ContactsList;
    @BindView(R.id.fastscroller)
    RecyclerViewFastScroller fastScroller;
    @BindView(R.id.app_bar)
    Toolbar toolbar;
    @BindView(R.id.empty)
    LinearLayout emptyContacts;
    private ContactsAdapter mSelectContactsAdapter;
    private ContactsPresenter mContactsPresenter;

    @BindView(R.id.toolbar_progress_bar)
    ProgressBar toolbarProgressBar;

    @BindView(R.id.close_btn_search_view)
    ImageView closeBtn;
    @BindView(R.id.search_input)
    TextInputEditText searchInput;
    @BindView(R.id.clear_btn_search_view)
    ImageView clearBtn;
    @BindView(R.id.add_btn_search_view)
    ImageView addBtn;
    @BindView(R.id.scan_btn_search_view)
    ImageView scanBtn;
    @BindView(R.id.app_bar_search_view)
    View searchView;

    @BindView(R.id.main_view)
    LinearLayout MainView;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_conversation);
        ButterKnife.bind(this);

        searchInput.setFocusable(true);
        initializerSearchView(searchInput, clearBtn);
        initializerView();
        setTypeFaces();
        mContactsPresenter = new ContactsPresenter(this);
        mContactsPresenter.onCreate();


        // Show search view by default (match Calls search in light mode)
        launcherSearchView();
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        }


    }

    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            searchInput.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }

    /**
     * method to initialize the view
     */
    private void initializerView() {

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.title_select_contacts));

        }
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mSelectContactsAdapter = new ContactsAdapter();
        ContactsList.setLayoutManager(mLinearLayoutManager);
        ContactsList.setAdapter(mSelectContactsAdapter);

        //fix slow recyclerview start
        ContactsList.setHasFixedSize(true);
        ContactsList.setItemViewCacheSize(10);
        ContactsList.setDrawingCacheEnabled(true);
        ContactsList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        // set recyclerView to fastScroller
        fastScroller.setRecyclerView(ContactsList);
        fastScroller.setViewsToUse(R.layout.contacts_fragment_fast_scroller, R.id.fastscroller_bubble, R.id.fastscroller_handle);

        closeBtn.setOnClickListener(v -> closeSearchView());
        clearBtn.setOnClickListener(v -> clearSearchView());

        if (addBtn != null) {
            addBtn.setOnClickListener(v -> showAddContactDialog());
        }
        if (scanBtn != null) {
            scanBtn.setOnClickListener(v -> startWalletQrScan());
        }
    }

    private void showAddContactDialog() {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Enter contact wallet address");
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        et.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(42), (source, start, end, dest, dstart, dend) -> {
            if (source == null) return null;
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                boolean ok = (c >= '0' && c <= '9')
                        || (c >= 'a' && c <= 'f')
                        || (c >= 'A' && c <= 'F')
                        || c == 'x' || c == 'X'
                        || c == '0';
                if (!ok) {
                    return "";
                }
            }
            return null;
        }});
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Enter contact wallet address")
                .setView(et)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String address = et.getText() == null ? "" : et.getText().toString().trim();
                    if (address.isEmpty()) return;
                    if (!address.matches("^0x[a-fA-F0-9]{40}$")) {
                        et.setError("Invalid EVM address");
                        return;
                    }
                    showSaveWalletContactDialog(address);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSaveWalletContactDialog(String walletAddress) {
        SaveWalletContactDialogFragment dialog = SaveWalletContactDialogFragment.newInstance(walletAddress);
        dialog.setOnSaveWalletContactListener((firstName, lastName, address, category) -> {
            ensureSavedWalletContact(address, firstName, lastName, category);
            searchInput.setText("");
        });
        dialog.show(getSupportFragmentManager(), "SaveWalletContactDialog");
    }

    private void ensureSavedWalletContact(String walletAddress, String firstName, String lastName, String category) {
        saveWalletContactToRealm(walletAddress, firstName, lastName, category);
    }

       private void saveWalletContactToRealm(String walletAddress, String firstName, String lastName, String category) {
           String normalizedAddress = normalizeWalletAddress(walletAddress);
           if (normalizedAddress == null) {
               AppHelper.LogCat("Failed to save wallet contact: invalid wallet " + walletAddress);
               return;
           }
           try {
               Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
               realm.executeTransaction(r -> upsertWalletContact(r, normalizedAddress, firstName, lastName, category));
               if (!realm.isClosed()) realm.close();

               AppHelper.LogCat("Wallet contact saved locally, refreshing contacts list: " + normalizedAddress);
               refreshContactsFromLocalRealm();
               android.widget.Toast.makeText(this, "Contact saved", android.widget.Toast.LENGTH_SHORT).show();
               syncWalletContactWithBackend(normalizedAddress, firstName, lastName, category);
           } catch (Exception e) {
               AppHelper.LogCat("Failed to save wallet contact to Realm: " + e);
           }
       }

     private void upsertWalletContact(Realm realm, String walletAddress, String firstName, String lastName, String category) {
         String displayName = buildWalletContactName(walletAddress, firstName, lastName);
         ContactsModel existing = realm.where(ContactsModel.class)
                 .equalTo("walletAddress", walletAddress, Case.INSENSITIVE)
                 .findFirst();
         if (existing == null) {
             existing = realm.where(ContactsModel.class)
                     .equalTo("walletAddressTmp", walletAddress, Case.INSENSITIVE)
                     .findFirst();
         }
         if (existing != null) {
             AppHelper.LogCat("Wallet contact already exists, updating locally: " + walletAddress);
             existing.setUsername(displayName);
             existing.setWalletAddress(walletAddress);
             existing.setWalletAddressTmp(walletAddress);
             existing.setFirstName(firstName);
             existing.setLastName(lastName);
             existing.setCategory(category);
             existing.setExist(true);
             existing.setLinked(true);
             existing.setActivate(true);
             return;
         }

         int walletId = createLocalWalletContactId(realm);
         AppHelper.LogCat("Creating local wallet contact: " + walletAddress + " id=" + walletId);
         ContactsModel contact = realm.createObject(ContactsModel.class, walletId);
         contact.setUsername(displayName);
         contact.setWalletAddress(walletAddress);
         contact.setWalletAddressTmp(walletAddress);
         contact.setFirstName(firstName);
         contact.setLastName(lastName);
         contact.setCategory(category);
         contact.setExist(true);
         contact.setLinked(true);
         contact.setActivate(true);
     }

     private int createLocalWalletContactId(Realm realm) {
         int walletId = -(int) (System.currentTimeMillis() % Integer.MAX_VALUE);
         if (walletId >= 0) walletId = -1;
         while (realm.where(ContactsModel.class).equalTo("id", walletId).findFirst() != null) {
             walletId--;
         }
         return walletId;
     }

     private String buildWalletContactName(String walletAddress, String firstName, String lastName) {
         String first = firstName == null ? "" : firstName.trim();
         String last = lastName == null ? "" : lastName.trim();
         String fullName = (first + " " + last).trim();
         return fullName.isEmpty() ? walletAddress : fullName;
     }

     private String normalizeWalletAddress(String walletAddress) {
         if (walletAddress == null) return null;
         Matcher matcher = ETH_ADDRESS_PATTERN.matcher(walletAddress.trim());
         if (!matcher.find()) return null;
         return matcher.group(0);
     }

     private void refreshContactsFromLocalRealm() {
         Realm realm = null;
         try {
             realm = WhatsCloneApplication.getRealmDatabaseInstance();
             List<ContactsModel> contacts = realm.copyFromRealm(realm.where(ContactsModel.class)
                     .notEqualTo("id", PreferenceManager.getID(this))
                     .equalTo("Exist", true)
                     .findAll()
                     .sort("Activate", io.realm.Sort.DESCENDING)
                     .sort("Linked", io.realm.Sort.DESCENDING)
                     .sort("username", io.realm.Sort.ASCENDING));
             ShowContacts(contacts);
             ContactsList.scrollToPosition(0);
         } catch (Exception e) {
             AppHelper.LogCat("Failed to refresh contacts after local wallet save: " + e);
             if (mContactsPresenter != null) {
                 mContactsPresenter.getContacts();
             }
         } finally {
             if (realm != null && !realm.isClosed()) realm.close();
         }
     }

     private void syncWalletContactWithBackend(String walletAddress, String firstName, String lastName, String category) {
            try {
                Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                realm.executeTransaction(r -> {
                    ContactsModel existing = r.where(ContactsModel.class)
                            .equalTo("walletAddress", walletAddress, Case.INSENSITIVE)
                            .findFirst();
                    if (existing != null) {
                        existing.setExist(true);
                        existing.setLinked(true);
                        existing.setActivate(true);
                        AppHelper.LogCat("syncWalletContact: ensured wallet " + walletAddress + " exists in Realm");
                    } else {
                        AppHelper.LogCat("syncWalletContact: wallet " + walletAddress + " NOT found in Realm!");
                    }
                });
                if (!realm.isClosed()) realm.close();
            } catch (Exception e) {
                AppHelper.LogCat("syncWalletContact: failed to verify wallet in Realm: " + e);
            }

            ContactsModel contactModel = new ContactsModel();
            contactModel.setWalletAddress(walletAddress);
            contactModel.setWalletAddressTmp(walletAddress);
            contactModel.setUsername(buildWalletContactName(walletAddress, firstName, lastName));
            contactModel.setFirstName(firstName);
            contactModel.setLastName(lastName);
            contactModel.setCategory(category);
            contactModel.setExist(true);
            contactModel.setLinked(true);
            contactModel.setActivate(true);
            List<ContactsModel> contacts = new java.util.ArrayList<>();
            contacts.add(contactModel);

            try {
                Realm allRealm = WhatsCloneApplication.getRealmDatabaseInstance();
                io.realm.RealmResults<ContactsModel> allWalletContacts = allRealm.where(ContactsModel.class)
                        .equalTo("Exist", true)
                        .beginGroup()
                        .beginsWith("walletAddress", "0x", io.realm.Case.INSENSITIVE)
                        .or()
                        .beginsWith("walletAddressTmp", "0x", io.realm.Case.INSENSITIVE)
                        .endGroup()
                        .findAll();
                for (ContactsModel wc : allWalletContacts) {
                    if (wc.getWalletAddress() == null
                            || !wc.getWalletAddress().trim().matches("^0x[a-fA-F0-9]{40}$")) {
                        continue;
                    }
                    ContactsModel syncModel = new ContactsModel();
                    syncModel.setWalletAddress(wc.getWalletAddress());
                    syncModel.setWalletAddressTmp(wc.getWalletAddressTmp());
                    if (!walletAddress.equalsIgnoreCase(wc.getWalletAddress())) {
                        contacts.add(syncModel);
                    }
                }
                if (!allRealm.isClosed()) allRealm.close();
            } catch (Exception e) {
                AppHelper.LogCat("syncWalletContact: failed to collect all wallet contacts: " + e);
            }

            com.money.mimi.api.APIHelper.initialApiUsersContacts().updateContacts(contacts)
                .subscribe(contactsModelList -> {
                    AppHelper.LogCat("Wallet contact synced with backend: " + walletAddress + " server returned " + contactsModelList.size() + " contacts");

                    try {
                        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                        realm.executeTransaction(r -> {
                            ContactsModel existing = r.where(ContactsModel.class)
                                    .equalTo("walletAddress", walletAddress, Case.INSENSITIVE)
                                    .findFirst();
                            if (existing != null) {
                                existing.setExist(true);
                                existing.setLinked(true);
                                existing.setActivate(true);
                                AppHelper.LogCat("syncWalletContact: post-sync verified wallet " + walletAddress + " Linked=" + existing.isLinked() + " Activate=" + existing.isActivate());
                            } else {
                                AppHelper.LogCat("syncWalletContact: post-sync wallet " + walletAddress + " was DELETED by server sync! Re-creating...");
                                int walletId = -(int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                                if (walletId >= 0) walletId = walletId - 1;
                                ContactsModel contact = r.createObject(ContactsModel.class, walletId);
                                contact.setWalletAddress(walletAddress);
                                contact.setWalletAddressTmp(walletAddress);
                                contact.setUsername(walletAddress);
                                contact.setExist(true);
                                contact.setLinked(true);
                                contact.setActivate(true);
                            }
                        });
                        if (!realm.isClosed()) realm.close();
                    } catch (Exception e) {
                        AppHelper.LogCat("syncWalletContact: post-sync verification failed: " + e);
                    }

                    if (mContactsPresenter != null) {
                        mContactsPresenter.getContacts();
                    }
                }, throwable -> {
                    AppHelper.LogCat("Failed to sync wallet contact with backend: " + throwable.getMessage());
                    if (mContactsPresenter != null) {
                        mContactsPresenter.getContacts();
                    }
                });
        }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startWalletQrScan() {
        com.google.zxing.integration.android.IntentIntegrator integrator =
                new com.google.zxing.integration.android.IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(com.google.zxing.integration.android.IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan QR");
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.setCaptureActivity(com.money.mimi.wallet.PortraitCaptureActivity.class);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        com.google.zxing.integration.android.IntentResult scanResult =
                com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            String contents = scanResult.getContents();
            if (contents != null) {
                String address = extractWalletAddress(contents);
                if (address != null) {
                    showSaveWalletContactDialog(address);
                } else {
                    String trimmed = contents.trim();
                    searchInput.setText(trimmed);
                    searchInput.setSelection(trimmed.length());
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String extractWalletAddress(String raw) {
        if (raw == null) return null;
        String text = raw.trim();
        String lower = text.toLowerCase();
        if (lower.startsWith("ethereum:")) {
            String s = text.substring(text.indexOf(':') + 1);
            while (s.startsWith("/")) s = s.substring(1);
            int idx0x = s.toLowerCase().indexOf("0x");
            if (idx0x >= 0) s = s.substring(idx0x);
            int q = s.indexOf('?');
            if (q > 0) s = s.substring(0, q);
            int at = s.indexOf('@');
            if (at > 0) s = s.substring(0, at);
            Matcher m = ETH_ADDRESS_PATTERN.matcher(s);
            if (m.find()) return m.group(0);
        }
        Matcher m2 = ETH_ADDRESS_PATTERN.matcher(text);
        if (m2.find()) return m2.group(0);
        return null;
    }

    /**
     * method to initialize the search view
     */
    public void initializerSearchView(TextInputEditText searchInput, ImageView clearSearchBtn) {

        final Context context = this;
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }

        });
        searchInput.addTextChangedListener(new TextWatcherAdapter() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                clearSearchBtn.setVisibility(View.GONE);
            }

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSelectContactsAdapter.setString(s.toString());
                Search(s.toString().trim());
                clearSearchBtn.setVisibility(View.VISIBLE);
            }

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    clearSearchBtn.setVisibility(View.GONE);
                    mContactsPresenter.getContacts();
                }
            }
        });

    }


    @SuppressWarnings("unused")
    @OnClick(R.id.new_group)
    public void newGroup() {
        startActivity(new Intent(this, AddMembersToGroupActivity.class));
        finish();
        AnimationsUtil.setSlideInAnimation(this);
    }

    /**
     * method to close the searchview with animation
     */
    @SuppressWarnings("unused")
    @OnClick(R.id.close_btn_search_view)
    public void closeSearchView() {
        final Animation animation = AnimationUtils.loadAnimation(this, R.anim.scale_for_button_animtion_exit);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                searchView.setVisibility(View.GONE);
                toolbar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        searchView.startAnimation(animation);
    }

    private void launcherSearchView() {
        final Animation animation = AnimationUtils.loadAnimation(this, R.anim.scale_for_button_animtion_enter);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                searchView.setVisibility(View.VISIBLE);
                toolbar.setVisibility(View.GONE);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        searchView.startAnimation(animation);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_contacts:
                launcherSearchView();
                break;
            case R.id.refresh_contacts:
                mContactsPresenter.onRefresh();
                break;
            case android.R.id.home:
                finish();
                AnimationsUtil.setSlideOutAnimation(this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * method to show contacts list
     *
     * @param contactsModels this is parameter for ShowContacts  method
     */
    public void ShowContacts(List<ContactsModel> contactsModels) {

        AppHelper.LogCat("ShowContacts called with " + contactsModels.size() + " contacts");
        int walletCount = 0;
        for (ContactsModel c : contactsModels) {
            if (c.getWalletAddress() != null && c.getWalletAddress().trim().matches("^0x[a-fA-F0-9]{40}$")) walletCount++;
        }
        AppHelper.LogCat("ShowContacts: " + walletCount + " wallet contacts found");

        if (getSupportActionBar() != null)
            getSupportActionBar().setSubtitle("" + PreferenceManager.getContactSize(this) + getResources().getString(R.string.of) +contactsModels.size() );
        if (contactsModels.size() != 0) {
            fastScroller.setVisibility(View.VISIBLE);
            ContactsList.setVisibility(View.VISIBLE);
            emptyContacts.setVisibility(View.GONE);
            RealmList<ContactsModel> usersModelRealmList = new RealmList<>();
            usersModelRealmList.addAll(contactsModels);
            mSelectContactsAdapter.setContacts(usersModelRealmList);
            /*
            mContactsModelList = usersModelRealmList;
            mSelectContactsAdapter.setContacts(usersModelRealmList);*/
        } else {
            fastScroller.setVisibility(View.GONE);
            ContactsList.setVisibility(View.GONE);
            emptyContacts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainView.setVisibility(View.GONE);

    }

    @Override
    protected void onPause() {
        super.onPause();
        MainView.setVisibility(View.VISIBLE);
    }




    @Override
    public void onShowLoading() {
        toolbarProgressBar.setVisibility(View.VISIBLE);
        toolbarProgressBar.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(this, R.color.colorWhite), PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void onHideLoading() {
        toolbarProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onErrorLoading(Throwable throwable) {
        AppHelper.LogCat("Contacts Fragment " + throwable.getMessage());

        toolbarProgressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contacts_menu, menu);
        return true;
    }

    /**
     * method to clear/reset the search view
     */
    public void clearSearchView() {
        if (searchInput.getText() != null) {
            searchInput.setText("");
            mContactsPresenter.getContacts();
            ContactsList.setVisibility(View.VISIBLE);
            emptyContacts.setVisibility(View.GONE);
        }
    }

    /**
     * method to start searching
     *
     * @param string this  is parameter for Search method
     */
    public void Search(String string) {

        List<ContactsModel> filteredModelList;
        filteredModelList = FilterList(string);
        if (filteredModelList.size() != 0) {
            ContactsList.setVisibility(View.VISIBLE);
            emptyContacts.setVisibility(View.GONE);
            mSelectContactsAdapter.animateTo(filteredModelList);
            ContactsList.scrollToPosition(0);
        } else {
            ContactsList.setVisibility(View.GONE);
            emptyContacts.setVisibility(View.VISIBLE);
        }
    }

    /**
     * method to filter the list of contacts
     *
     * @param query this parameter for FilterList  method
     * @return this for what method will return
     */
    private List<ContactsModel> FilterList(String query) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<ContactsModel> contactsModelList = new ArrayList<>();

        List<ContactsModel> usersModels = realm.where(ContactsModel.class)
                .equalTo("Exist", true)
                .notEqualTo("id", PreferenceManager.getID(this))
                .beginGroup()
                .contains("walletAddress", query, Case.INSENSITIVE)
                .or()
                .contains("username", query, Case.INSENSITIVE)
                .or()
                .contains("firstName", query, Case.INSENSITIVE)
                .or()
                .contains("lastName", query, Case.INSENSITIVE)
                .or()
                .contains("category", query, Case.INSENSITIVE)
                .endGroup()
                .findAll();
        contactsModelList.addAll(usersModels);

        if (!realm.isClosed())
            realm.close();
        return contactsModelList;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContactsPresenter.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationsUtil.setSlideOutAnimation(this);
    }


}
