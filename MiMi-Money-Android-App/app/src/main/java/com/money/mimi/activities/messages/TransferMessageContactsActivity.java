package com.money.mimi.activities.messages;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.view.inputmethod.InputMethodManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.money.mimi.R;
import com.money.mimi.adapters.recyclerView.messages.TransferMessageContactsAdapter;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.adapters.others.TextWatcherAdapter;
import com.money.mimi.fragments.home.SaveWalletContactDialogFragment;
import com.money.mimi.helpers.PermissionHandler;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.presenters.users.SelectContactsPresenter;
import com.money.mimi.ui.RecyclerViewFastScroller;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Case;
import io.realm.Realm;

/**
 * Created by abderrahimelimame on 6/9/16.
 * Email : abderrahim.elimame@gmail.com
 */

public class TransferMessageContactsActivity extends AppCompatActivity {

    @BindView(R.id.ContactsList)
    RecyclerView ContactsList;
    @BindView(R.id.fastscroller)

    RecyclerViewFastScroller fastScroller;

    // Custom search view (matches chat select contact UI)
    @BindView(R.id.app_bar_search_view)
    android.widget.LinearLayout appBarSearchView;
    @BindView(R.id.close_btn_search_view)
    android.widget.ImageView closeBtnSearch;
    @BindView(R.id.search_input)
    com.google.android.material.textfield.TextInputEditText searchInput;
    @BindView(R.id.clear_btn_search_view)
    android.widget.ImageView clearBtnSearch;
    @BindView(R.id.add_btn_search_view)
    android.widget.ImageView addBtnSearch;
    @BindView(R.id.scan_btn_search_view)
    android.widget.ImageView scanBtnSearch;
    private List<ContactsModel> mContactsModelList;
    private TransferMessageContactsAdapter mTransferMessageContactsAdapter;
    private SelectContactsPresenter mContactsPresenter;
    private ArrayList<String> messageCopied = new ArrayList<>();
    private ArrayList<String> filePathList = new ArrayList<>();
    private String filePath;
    private boolean forCall;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        ButterKnife.bind(this);
        if (getIntent().getExtras() != null) {
            if (getIntent().hasExtra("messageCopied")) {
                messageCopied = getIntent().getExtras().getStringArrayList("messageCopied");
            }
            forCall = getIntent().getBooleanExtra("forCall", false);

            Intent intent = getIntent();
            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null) {
                    String cachePath = FilesManager.getPath(this, uri);
                    filePath = FilesManager.persistFile(this, cachePath, FilesManager.FILE_TYPE_IMAGE);
                }
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (text != null) {
                    messageCopied.add(text);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
                ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (uris != null) {
                    for (Uri uri : uris) {
                        String cachePath = FilesManager.getPath(this, uri);
                        filePathList.add(FilesManager.persistFile(this, cachePath, FilesManager.FILE_TYPE_IMAGE));
                    }
                }
            }

        }
        initializeView();
        mContactsPresenter = new SelectContactsPresenter(this);
        mContactsPresenter.onCreate();

        if (forCall) {
            android.view.View toolbar = findViewById(R.id.app_bar);
            if (toolbar != null) toolbar.setVisibility(android.view.View.GONE);
            if (appBarSearchView != null) appBarSearchView.setVisibility(android.view.View.VISIBLE);
            if (addBtnSearch != null) addBtnSearch.setVisibility(android.view.View.GONE);
            if (scanBtnSearch != null) scanBtnSearch.setVisibility(android.view.View.GONE);
            initializerSearchView();
        }

    }

    private void initializerSearchView() {
        closeBtnSearch.setOnClickListener(v -> {
            finish();
            AnimationsUtil.setSlideOutAnimation(this);
        });
        clearBtnSearch.setOnClickListener(v -> {
            if (searchInput.getText() != null) {
                searchInput.setText("");
            }
            mContactsPresenter.onCreate();
        });
        addBtnSearch.setOnClickListener(v -> showAddContactDialog());
        scanBtnSearch.setOnClickListener(v -> startWalletQrScan());

        final android.content.Context context = this;
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager inputManager = (InputMethodManager) context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (inputManager != null) {
                    inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });
        searchInput.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                clearBtnSearch.setVisibility(android.view.View.GONE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Search(s.toString().trim());
                clearBtnSearch.setVisibility(android.view.View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    clearBtnSearch.setVisibility(android.view.View.GONE);
                    mContactsPresenter.onCreate();
                }
            }
        });
    }

    /**
     * method to initialize the view
     */
    private void initializeView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.title_select_contacts));

        }
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (filePathList != null && filePathList.size() != 0) {
            mTransferMessageContactsAdapter = new TransferMessageContactsAdapter(this, mContactsModelList, filePathList, true);
        } else if (filePath != null) {
            mTransferMessageContactsAdapter = new TransferMessageContactsAdapter(this, mContactsModelList, filePath);
        } else if (messageCopied != null && messageCopied.size() != 0) {
            mTransferMessageContactsAdapter = new TransferMessageContactsAdapter(this, mContactsModelList, messageCopied);
        } else if (forCall) {
            mTransferMessageContactsAdapter = new TransferMessageContactsAdapter(this, mContactsModelList, true);
        }

        ContactsList.setLayoutManager(mLinearLayoutManager);
        ContactsList.setAdapter(mTransferMessageContactsAdapter);
        // set recyclerView to fastScroller
        fastScroller.setRecyclerView(ContactsList);
        fastScroller.setViewsToUse(R.layout.contacts_fragment_fast_scroller, R.id.fastscroller_bubble, R.id.fastscroller_handle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (forCall) {
            // Using custom search view; no toolbar menu.
            return false;
        }

        getMenuInflater().inflate(R.menu.search_menu, menu);
        // Set up SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search_contacts).getActionView();
        searchView.setIconified(true);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(mQueryTextListener);
        searchView.setQueryHint(getString(R.string.search_hint));
        return super.onCreateOptionsMenu(menu);
    }

    private void showAddContactDialog() {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Enter contact wallet address");
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        et.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(42)});
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
            if (searchInput != null) {
                searchInput.setText(address);
                searchInput.setSelection(address.length());
            }
        });
        dialog.show(getSupportFragmentManager(), "SaveWalletContactDialog");
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
                String trimmed = contents.trim();
                if (trimmed.matches("^0x[a-fA-F0-9]{40}$")) {
                    showSaveWalletContactDialog(trimmed);
                } else {
                    Toast.makeText(this, "Invalid EVM address", Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

     private void ensureSavedWalletContact(String walletAddress, String firstName, String lastName, String category) {
         saveWalletContactToRealm(walletAddress, firstName, lastName, category);
     }

       private void saveWalletContactToRealm(String walletAddress, String firstName, String lastName, String category) {
           try {
               Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
               realm.executeTransactionAsync(r -> {
                   int walletId = -(int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                   if (walletId >= 0) walletId = walletId - 1;
                   ContactsModel existing = r.where(ContactsModel.class)
                           .equalTo("walletAddress", walletAddress)
                           .findFirst();
                   if (existing != null) {
                       existing.setUsername(firstName + " " + lastName);
                       existing.setWalletAddressTmp(walletAddress);
                       existing.setFirstName(firstName);
                       existing.setLastName(lastName);
                       existing.setCategory(category);
                       existing.setExist(true);
                       existing.setLinked(true);
                       existing.setActivate(true);
                   } else {
                       ContactsModel contact = r.createObject(ContactsModel.class, walletId);
                       contact.setUsername(firstName + " " + lastName);
                       contact.setWalletAddress(walletAddress);
                       contact.setWalletAddressTmp(walletAddress);
                       contact.setFirstName(firstName);
                       contact.setLastName(lastName);
                       contact.setCategory(category);
                       contact.setExist(true);
                       contact.setLinked(true);
                       contact.setActivate(true);
                   }
               }, () -> {
                   syncWalletContactWithBackend(walletAddress);
                   if (mContactsPresenter != null) {
                       mContactsPresenter.onCreate();
                   }
               });
               if (!realm.isClosed()) realm.close();
           } catch (Exception e) {
               AppHelper.LogCat("Failed to save wallet contact to Realm: " + e);
           }
       }

    private void syncWalletContactWithBackend(String walletAddress) {
        ContactsModel contactModel = new ContactsModel();
        contactModel.setWalletAddress(walletAddress);
        contactModel.setWalletAddressTmp(walletAddress);
        List<ContactsModel> contacts = new java.util.ArrayList<>();
        contacts.add(contactModel);
        com.money.mimi.api.APIHelper.initialApiUsersContacts().updateContacts(contacts)
            .subscribe(contactsModelList -> {
                AppHelper.LogCat("Wallet contact synced with backend: " + walletAddress);
                if (mContactsPresenter != null) {
                    mContactsPresenter.onCreate();
                }
            }, throwable -> {
                AppHelper.LogCat("Failed to sync wallet contact with backend: " + throwable.getMessage());
            });
    }

      @Override
      public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
          super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      }

    private SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String s) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            Search(s.trim());
            return true;
        }
    };

    /**
     * method to start searching
     *
     * @param string this is parameter for Search method
     */
    public void Search(String string) {
        mTransferMessageContactsAdapter.setString(string);
        List<ContactsModel> filteredModelList = FilterList(string);
        if (filteredModelList.size() != 0) {
            mTransferMessageContactsAdapter.setContacts(filteredModelList);
        }
    }

    /**
     * method to filter the list of contacts
     *
     * @param query this is parameter for FilterList method
     * @return this is what method will return
     */
    private List<ContactsModel> FilterList(String query) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();

        List<ContactsModel> contactsModels = realm.where(ContactsModel.class)
                .equalTo("Linked", true)
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
        realm.close();
        return contactsModels;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            AnimationsUtil.setSlideOutAnimation(this);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * method to show linked contacts
     *
     * @param contactsModels this is parameter for ShowContacts method
     */
    public void ShowContacts(List<ContactsModel> contactsModels) {
        mContactsModelList = contactsModels;
        if (getSupportActionBar() != null)
            getSupportActionBar().setSubtitle("" + PreferenceManager.getContactSize(this) + getString(R.string.of) +mContactsModelList.size() );
        mTransferMessageContactsAdapter.setContacts(mContactsModelList);
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
