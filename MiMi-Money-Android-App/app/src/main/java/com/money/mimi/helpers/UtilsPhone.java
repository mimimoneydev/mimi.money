package com.money.mimi.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import androidx.core.content.ContextCompat;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.money.mimi.app.WhatsCloneApplication;
import com.money.mimi.interfaces.ContactMobileNumbQuery;
import com.money.mimi.models.users.contacts.ContactsModel;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Created by Abderrahim El imame on 03/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class UtilsPhone {


    private static ArrayList<ContactsModel> mListContacts = new ArrayList<ContactsModel>();
    private static PhoneNumberUtil mPhoneUtil = PhoneNumberUtil.getInstance();
    private static String name = null;

    /**
     * method to retrieve all contacts from the book
     *
     * @return return value
     */
    public static ArrayList<ContactsModel> GetPhoneContacts() {
        mListContacts.clear();
        Context context = WhatsCloneApplication.getInstance().getApplicationContext();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            AppHelper.LogCat("READ_CONTACTS permission not granted, returning empty contacts list.");
            return mListContacts;
        }
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cur = contentResolver.query(ContactMobileNumbQuery.CONTENT_URI, ContactMobileNumbQuery.PROJECTION, ContactMobileNumbQuery.SELECTION, null, ContactMobileNumbQuery.SORT_ORDER);
        if (cur != null) {
            if (cur.getCount() > 0) {
                while (cur.moveToNext()) {
                    ContactsModel contactsModel = new ContactsModel();
                    String name = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String id = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID));
                    String image_uri = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));


                    //     AppHelper.LogCat("number phone --> " + phoneNumber);
                    if (name.contains("\\s+")) {
                        String[] nameArr = name.split("\\s+");
                        contactsModel.setUsername(nameArr[0] + nameArr[1]);
                        // AppHelper.LogCat("Fname --> " + nameArr[0]);
                        // AppHelper.LogCat("Lname --> " + nameArr[1]);
                    } else {
                        contactsModel.setUsername(name);
                        //AppHelper.LogCat("name" + name);
                    }
                    if (phoneNumber != null) {

                        String raw = phoneNumber.trim();
                        boolean isWallet = raw.matches("^0x[a-fA-F0-9]{40}$");
                        if (isWallet) {
                            contactsModel.setWalletAddressTmp(raw);
                            contactsModel.setWalletAddress(raw);
                            contactsModel.setContactID(Integer.parseInt(id));
                            contactsModel.setImage(image_uri);

                            int flag = 0;
                            int arraySize = mListContacts.size();
                            if (arraySize == 0) {
                                mListContacts.add(contactsModel);
                            }
                            //remove duplicate entries by exact match
                            for (int i = 0; i < arraySize; i++) {
                                if (!mListContacts.get(i).getWalletAddress().trim().equals(raw.trim())) {
                                    flag = 1;
                                } else {
                                    flag = 0;
                                    break;
                                }
                            }
                            if (flag == 1) {
                                mListContacts.add(contactsModel);
                            }
                        }

                    }
                }
                cur.close();
            }
        }
        return mListContacts;
    }

    /**
     * Check if number is valid
     *
     * @return boolean
     */
    @SuppressWarnings("unused")
    public static boolean isValid(String phone) {
        Phonenumber.PhoneNumber phoneNumber = getPhoneNumber(phone);
        return phoneNumber != null && mPhoneUtil.isValidNumber(phoneNumber);
    }

    /**
     * Get PhoneNumber object
     *
     * @return PhoneNumber | null on error
     */
    @SuppressWarnings("unused")
    public static Phonenumber.PhoneNumber getPhoneNumber(String phone) {
        final String DEFAULT_COUNTRY = Locale.getDefault().getCountry();
        try {
            return mPhoneUtil.parse(phone, DEFAULT_COUNTRY);
        } catch (NumberParseException ignored) {
            return null;
        }
    }

    /**
     * method to get contact ID
     *
     * @param mContext this is the first parameter for getContactID  method
     * @param phone    this is the second parameter for getContactID  method
     * @return return value
     */
    public static long getContactID(Activity mContext, String phone) {
        if (PermissionHandler.checkPermission(mContext, Manifest.permission.READ_CONTACTS)) {
            AppHelper.LogCat("Read contact data permission already granted.");
            // CONTENT_FILTER_URI allow to search contact by phone number
            Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
            // This query will return NAME and ID of contact, associated with phone //number.
            Cursor mcursor = mContext.getContentResolver().query(lookupUri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID}, null, null, null);
            //Now retrieve _ID from query result
            long idPhone = 0;
            try {
                if (mcursor != null) {
                    if (mcursor.moveToFirst()) {
                        idPhone = Long.parseLong(mcursor.getString(mcursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID)));
                    }
                }
            } finally {
                if (mcursor != null) mcursor.close();
            }
            return idPhone;
        } else {
            AppHelper.LogCat("Please request Read contact data permission.");
            PermissionHandler.requestPermission(mContext, Manifest.permission.READ_CONTACTS);
            return 0;
        }

    }


    /**
     * method to check for contact name
     *
     * @param phone this is the second parameter for getContactName  method
     * @return return value
     */
    @SuppressLint("StaticFieldLeak")
    public static String getContactName(String phone) {
        Context context = WhatsCloneApplication.getInstance().getApplicationContext();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        try {
            return new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    try {

                        // CONTENT_FILTER_URI allow to search contact by phone number
                        Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(params[0]));
                        // This query will return NAME and ID of contact, associated with phone //number.
                        Cursor mcursor = WhatsCloneApplication.getInstance().getApplicationContext().getContentResolver().query(lookupUri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID}, null, null, null);
                        //Now retrieve _ID from query result
                        String name = null;
                        try {
                            if (mcursor != null) {
                                if (mcursor.moveToFirst()) {
                                    name = mcursor.getString(mcursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                                }
                            }
                        } finally {
                            if (mcursor != null) mcursor.close();
                        }
                        return name;
                    } catch (Exception e) {
                        return e.getMessage();
                    }
                }

                @Override
                protected void onPostExecute(String username) {
                    super.onPostExecute(username);
                    // String name = UtilsPhone.getContactName(mActivity, conversationsModel.getRecipientPhone());
//                    AppHelper.LogCat("name " + username);
                    name = username;
                }
            }.execute(phone).get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }

    }

    /**
     * method to check if user contact exist
     *
     * @param phone this is the second parameter for checkIfContactExist  method
     * @return return value
     */
    public static boolean checkIfContactExist(Context mContext, String phone) {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        try {
            // CONTENT_FILTER_URI allow to search contact by phone number
            Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
            // This query will return NAME and ID of contact, associated with phone //number.
            Cursor mcursor = mContext.getApplicationContext().getContentResolver().query(lookupUri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID}, null, null, null);
            //Now retrieve _ID from query result
            String name = null;
            try {
                if (mcursor != null) {
                    if (mcursor.moveToFirst()) {
                        name = mcursor.getString(mcursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                    }
                }
            } finally {
                if (mcursor != null) mcursor.close();
            }

            return name != null;
        } catch (Exception e) {
            AppHelper.LogCat(e);
            return false;
        }
    }
}
