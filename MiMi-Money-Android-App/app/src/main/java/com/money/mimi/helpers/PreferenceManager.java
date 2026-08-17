package com.money.mimi.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.money.mimi.helpers.security.WalletSecretStorage;
import com.google.gson.Gson;
import com.money.mimi.models.groups.MembersGroupModel;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import com.money.mimi.wallet.TokenInfo;
import com.money.mimi.wallet.NftCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by Abderrahim El imame on 20/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class PreferenceManager {


    private static SharedPreferences mSharedPreferences;
    private static final String KEY_USER_PREF = "KEY_USER_PREFERENCES";


    private static final String KEY_MEMBERS_SELECTED = "KEY_MEMBERS_SELECTED";
    private static final String KEY_IS_WAITING_FOR_SMS = "KEY_IS_WAITING_FOR_SMS";
    private static final String KEY_MOBILE_NUMBER = "KEY_MOBILE_NUMBER";
    private static final String KEY_LAST_BACKUP = "KEY_LAST_BACKUP";
    private static final String KEY_VERSION_APP = "KEY_VERSION_APP";
    private static final String KEY_NEW_USER = "KEY_NEW_USER";
    private static final String KEY_WALLPAPER_USER = "KEY_WALLPAPER_USER";
    private static final String KEY_LANGUAGE = "KEY_LANGUAGE";
    private static final String KEY_APP_IS_OUT_DATE = "KEY_APP_IS_OUT_DATE";
    private static final String KEY_NEED_MORE_INFO = "KEY_NEED_MORE_INFO";
    private static final String BACKUP_FOLDER_KEY = "BACKUP_FOLDER_KEY";
    private static final String HAS_BACKUP = "HAS_BACKUP";

    private static final String KEY_WALLET_ADDRESS = "KEY_WALLET_ADDRESS";
    private static final String KEY_WALLET_MNEMONIC = "KEY_WALLET_MNEMONIC";
    private static final String KEY_WALLET_PASSWORD = "KEY_WALLET_PASSWORD";
    private static final String KEY_WALLET_PIN = "KEY_WALLET_PIN";
    private static final String KEY_WALLET_TOKENS = "KEY_WALLET_TOKENS";
    private static final String KEY_WALLET_NFTS = "KEY_WALLET_NFTS";
    private static final String KEY_WALLET_SELECTED_NETWORK = "KEY_WALLET_SELECTED_NETWORK";
    private static final String KEY_WALLET_TESTNET_ENABLED = "KEY_WALLET_TESTNET_ENABLED";
    private static final String KEY_WALLET_CUSTOM_NETWORKS = "KEY_WALLET_CUSTOM_NETWORKS";

    private static final String KEY_SECURITY_BIOMETRIC_ENABLED = "KEY_SECURITY_BIOMETRIC_ENABLED";
    private static final String KEY_REQUIRE_AUTH_FOR_TX = "KEY_REQUIRE_AUTH_FOR_TX";
    private static final String KEY_DARK_THEME_ENABLED = "KEY_DARK_THEME_ENABLED";
    private static final String KEY_SOCKET_SERVER_URL = "KEY_SOCKET_SERVER_URL";


    public static boolean setHasBackup(Context mContext, boolean hasBackup) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(HAS_BACKUP, hasBackup);
        return editor.commit();
    }


    public static boolean isHasBackup(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean(HAS_BACKUP, false);
    }

    public static boolean saveBackupFolder(Context mContext, String folderPath) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(BACKUP_FOLDER_KEY, folderPath);
        return editor.commit();
    }


    public static String getBackupFolder(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString(BACKUP_FOLDER_KEY, "");
    }

    /**
     * method to set Language
     *
     * @param lang     this is the first parameter for setLanguage  method
     * @param mContext this is the second parameter for setLanguage  method
     * @return return value
     */
    public static boolean setLanguage(Context mContext, String lang) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(KEY_LANGUAGE, lang);
        return editor.commit();
    }

    /**
     * method to get Language
     *
     * @return return value
     */
    public static String getLanguage(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString(KEY_LANGUAGE, "");
    }

    /**
     * method to set wallpaper
     *
     * @param wallpaper this is the first parameter for setWallpaper  method
     * @param mContext  this is the second parameter for setWallpaper  method
     * @return return value
     */
    public static boolean setWallpaper(Context mContext, String wallpaper) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(KEY_WALLPAPER_USER, wallpaper);
        return editor.commit();
    }

    /**
     * method to get wallpaper
     *
     * @return return value
     */
    public static String getWallpaper(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString(KEY_WALLPAPER_USER, null);
    }

    /**
     * method to set token
     *
     * @param token    this is the first parameter for setToken  method
     * @param mContext this is the second parameter for setToken  method
     * @return return value
     */
    public static boolean setToken(Context mContext, String token) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("token", token);
        return editor.commit();
    }

    /**
     * method to get token
     *
     * @return return value
     */
    public static String getToken(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("token", null);
    }


    /**
     * method to setID
     *
     * @param ID this is the first parameter for setID  method
     * @return return value
     */
    public static boolean setID(Context mContext, int ID) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt("id", ID);
        return editor.commit();
    }

    /**
     * method to getID
     *
     * @return return value
     */
    public static int getID(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getInt("id", 0);
    }


    /**
     * method to getWalletAddress
     *
     * @return return value
     */
    public static String getWalletAddress(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("walletAddress", null);
    }

    /**
     * method to setWalletAddress
     *
     * @param walletAddress this is the first parameter for setWalletAddress method
     * @return return value
     */
    public static boolean setWalletAddress(Context mContext, String walletAddress) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("walletAddress", walletAddress);
        return editor.commit();
    }

    /**
     * method to setSocketID
     *
     * @param ID this is the first parameter for setID  method
     * @return return value
     */
    public static boolean setSocketID(Context mContext, String ID) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("socketId", ID);
        return editor.commit();
    }

    /**
     * method to getID
     *
     * @return return value
     */
    public static String getSocketID(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("socketId", null);
    }

    /**
     * method to set contacts size
     *
     * @param size this is the first parameter for setContactSize  method
     * @return return value
     */
    public static boolean setContactSize(Context mContext, int size) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt("size", size);
        return editor.commit();
    }

    /**
     * method to get contacts size
     *
     * @return return value
     */
    public static int getContactSize(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getInt("size", 0);

    }


    /**
     * method to save new members to group
     *
     * @param membersGroupModels this is the second parameter for saveMembers  method
     */
    private static void saveMembers(Context mContext, List<MembersGroupModel> membersGroupModels) {
        //SharedPreferences settings;
        // SharedPreferences.Editor editor;

        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        Gson gson = new Gson();
        String jsonMembers = gson.toJson(membersGroupModels);

        editor.putString(KEY_MEMBERS_SELECTED, jsonMembers);

        editor.apply();
    }

    /**
     * method to add member
     *
     * @param membersGroupModel this is the second parameter for addMember  method
     */
    public static void addMember(Context mContext, MembersGroupModel membersGroupModel) {
        if (membersGroupModel == null) {
            return;
        }
        List<MembersGroupModel> membersGroupModelArrayList = getMembers(mContext);
        if (membersGroupModelArrayList == null)
            membersGroupModelArrayList = new ArrayList<MembersGroupModel>();
        Iterator<MembersGroupModel> iterator = membersGroupModelArrayList.iterator();
        while (iterator.hasNext()) {
            MembersGroupModel selectedMember = iterator.next();
            if (selectedMember != null && selectedMember.getUserId() == membersGroupModel.getUserId()) {
                iterator.remove();
            }
        }
        membersGroupModelArrayList.add(membersGroupModel);
        saveMembers(mContext, membersGroupModelArrayList);
    }

    /**
     * method to remove member
     *
     * @param membersGroupModel this is the second parameter for removeMember  method
     */
    public static void removeMember(Context mContext, MembersGroupModel membersGroupModel) {
        ArrayList<MembersGroupModel> membersGroupModelArrayList = getMembers(mContext);
        if (membersGroupModelArrayList != null) {
            Iterator<MembersGroupModel> iterator = membersGroupModelArrayList.iterator();
            while (iterator.hasNext()) {
                MembersGroupModel selectedMember = iterator.next();
                if (selectedMember == null || (membersGroupModel != null && selectedMember.getUserId() == membersGroupModel.getUserId())) {
                    iterator.remove();
                }
            }
            saveMembers(mContext, membersGroupModelArrayList);
        }
    }

    /**
     * method to clear members
     */
    public static void clearMembers(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(KEY_MEMBERS_SELECTED, null);
        editor.apply();
    }

    /**
     * method to get all members
     *
     * @return return value
     */
    public static ArrayList<MembersGroupModel> getMembers(Context mContext) {
        try {
            List<MembersGroupModel> membersGroupModels;
            mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
            if (mSharedPreferences.contains(KEY_MEMBERS_SELECTED)) {
                String jsonMembers = mSharedPreferences.getString(KEY_MEMBERS_SELECTED, null);
                if (jsonMembers == null || jsonMembers.trim().isEmpty()) {
                    return new ArrayList<>();
                }
                Gson gson = new Gson();
                MembersGroupModel[] membersItems = gson.fromJson(jsonMembers, MembersGroupModel[].class);
                if (membersItems == null) {
                    return new ArrayList<>();
                }
                membersGroupModels = Arrays.asList(membersItems);
                return new ArrayList<>(membersGroupModels);
            } else {
                return null;
            }

        } catch (Exception e) {
            AppHelper.LogCat("getMembers Exception " + e.getMessage());
            return null;
        }
    }


    /**
     * method to setUnitInterstitialAdID
     *
     * @param UnitId this is the first parameter for setUnitInterstitialAdID  method
     * @return return value
     */
    public static boolean setUnitInterstitialAdID(Context mContext, String UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("InterstitialUnitId", UnitId);
        return editor.commit();
    }

    /**
     * method to getUnitInterstitialAdID
     *
     * @return return value
     */
    public static String getUnitInterstitialAdID(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("InterstitialUnitId", null);
    }

    /**
     * method to setShowInterstitialAds
     *
     * @param UnitId this is the first parameter for setShowInterstitialAds  method
     * @return return value
     */
    public static boolean setShowInterstitialAds(Context mContext, Boolean UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean("ShowInterstitialAds", UnitId);
        return editor.commit();
    }

    /**
     * method to ShowInterstitialrAds
     *
     * @return return value
     */
    public static boolean ShowInterstitialrAds(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean("ShowInterstitialAds", false);
    }

    /**
     * method to setUnitBannerAdsID
     *
     * @param UnitId this is the first parameter for setUnitBannerAdsID  method
     * @return return value
     */
    public static boolean setUnitBannerAdsID(Context mContext, String UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("BannerUnitId", UnitId);
        return editor.commit();
    }

    /**
     * method to getUnitBannerAdsID
     *
     * @return return value
     */
    public static String getUnitBannerAdsID(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("BannerUnitId", null);
    }


    /**
     * method to setShowBannerAds
     *
     * @param UnitId this is the first parameter for setShowBannerAds  method
     * @return return value
     */
    public static boolean setShowBannerAds(Context mContext, Boolean UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean("ShowBannerAds", UnitId);
        return editor.commit();
    }

    /**
     * method to ShowBannerAds
     *
     * @return return value
     */
    public static boolean ShowBannerAds(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean("ShowBannerAds", false);
    }


    public static boolean setUnitWalletBannerAdsID(Context mContext, String UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("WalletBannerUnitId", UnitId);
        return editor.commit();
    }

    public static String getUnitWalletBannerAdsID(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("WalletBannerUnitId", null);
    }

    public static boolean setShowWalletBannerAds(Context mContext, Boolean UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean("ShowWalletBannerAds", UnitId);
        return editor.commit();
    }

    public static boolean ShowWalletBannerAds(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean("ShowWalletBannerAds", false);
    }

    public static boolean setUnitMoneyBannerAdsID(Context mContext, String UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("MoneyBannerUnitId", UnitId);
        return editor.commit();
    }

    public static String getUnitMoneyBannerAdsID(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("MoneyBannerUnitId", null);
    }

    public static boolean setShowMoneyBannerAds(Context mContext, Boolean UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean("ShowMoneyBannerAds", UnitId);
        return editor.commit();
    }

    public static boolean ShowMoneyBannerAds(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean("ShowMoneyBannerAds", false);
    }

    public static boolean setUnitSpaceBannerAdsID(Context mContext, String UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("SpaceBannerUnitId", UnitId);
        return editor.commit();
    }

    public static String getUnitSpaceBannerAdsID(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("SpaceBannerUnitId", null);
    }

    public static boolean setShowSpaceBannerAds(Context mContext, Boolean UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean("ShowSpaceBannerAds", UnitId);
        return editor.commit();
    }

    public static boolean ShowSpaceBannerAds(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean("ShowSpaceBannerAds", false);
    }


    /**
     * method to setUnitVideoAdsID
     *
     * @param UnitId this is the first parameter for setUnitVideoAdsID  method
     * @return return value
     */
    public static boolean setUnitVideoAdsID(Context mContext, String UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("VideoUnitId", UnitId);
        return editor.commit();
    }

    /**
     * method to getUnitVideoAdsID
     *
     * @return return value
     */
    public static String getUnitVideoAdsID(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("VideoUnitId", null);
    }

    /**
     * method to setAppVideoAdsID
     *
     * @param UnitId this is the first parameter for setAppVideoAdsID  method
     * @return return value
     */
    public static boolean setAppVideoAdsID(Context mContext, String UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("VideoAppId", UnitId);
        return editor.commit();
    }

    /**
     * method to getAppVideoAdsID
     *
     * @return return value
     */
    public static String getAppVideoAdsID(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("VideoAppId", null);
    }


    /**
     * method to setShowVideoAds
     *
     * @param UnitId this is the first parameter for setShowVideoAds  method
     * @return return value
     */
    public static boolean setShowVideoAds(Context mContext, Boolean UnitId) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean("ShowVideoAds", UnitId);
        return editor.commit();
    }

    /**
     * method to ShowVideoAds
     *
     * @return return value
     */
    public static boolean ShowVideoAds(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean("ShowVideoAds", false);
    }

    /**
     * method to set var as the info aren't incomplete
     *
     * @param isNew this is parameter for setIsNewUser  method
     */
    public static boolean setIsNeedInfo(Context mContext, Boolean isNew) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(KEY_NEED_MORE_INFO, isNew);
        return editor.commit();
    }

    /**
     * method to check if user is provide more info
     *
     * @return return value
     */
    public static boolean isNeedProvideInfo(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean(KEY_NEED_MORE_INFO, false);
    }

    /**
     * method to set user waiting for SMS (code verification)
     *
     * @param isWaiting this is parameter for setIsWaitingForSms  method
     */
    public static boolean setIsWaitingForSms(Context mContext, Boolean isWaiting) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(KEY_IS_WAITING_FOR_SMS, isWaiting);
        return editor.commit();
    }

    /**
     * method to check if user is waiting for SMS
     *
     * @return return value
     */
    public static boolean isWaitingForSms(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean(KEY_IS_WAITING_FOR_SMS, false);
    }

    /**
     * method to set mobile phone
     *
     * @param mobileNumber this is parameter for setMobileNumber  method
     */
    public static boolean setMobileNumber(Context mContext, String mobileNumber) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(KEY_MOBILE_NUMBER, mobileNumber);
        return editor.commit();
    }

    /**
     * method to get mobile phone
     *
     * @return return value
     */
    // Wallet storage helpers
    public static boolean setWalletMnemonic(Context mContext, String mnemonic) {
        return setSecureWalletValue(mContext, KEY_WALLET_MNEMONIC, mnemonic);
    }

    public static String getWalletMnemonic(Context mContext) {
        return getSecureWalletValue(mContext, KEY_WALLET_MNEMONIC);
    }

    public static boolean setWalletPassword(Context mContext, String password) {
        return setSecureWalletValue(mContext, KEY_WALLET_PASSWORD, password);
    }

    public static String getWalletPassword(Context mContext) {
        return getSecureWalletValue(mContext, KEY_WALLET_PASSWORD);
    }


    public static boolean setWalletPin(Context mContext, String pin) {
        return setSecureWalletValue(mContext, KEY_WALLET_PIN, pin);
    }

    public static String getWalletPin(Context mContext) {
        return getSecureWalletValue(mContext, KEY_WALLET_PIN);
    }

    public static void migrateLegacyWalletSecrets(Context mContext) {
        migrateSecureWalletValue(mContext, KEY_WALLET_MNEMONIC);
        migrateSecureWalletValue(mContext, KEY_WALLET_PASSWORD);
        migrateSecureWalletValue(mContext, KEY_WALLET_PIN);
    }

    private static boolean setSecureWalletValue(Context mContext, String key, String value) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        if (value == null) {
            editor.remove(key);
            return editor.commit();
        }

        String encryptedValue = WalletSecretStorage.encrypt(mContext, value);
        if (encryptedValue == null) return false;

        editor.putString(key, encryptedValue);
        return editor.commit();
    }

    private static String getSecureWalletValue(Context mContext, String key) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        String storedValue = mSharedPreferences.getString(key, null);
        if (storedValue == null) return null;

        if (WalletSecretStorage.isEncryptedValue(storedValue)) {
            return WalletSecretStorage.decrypt(mContext, storedValue);
        }

        String encryptedValue = WalletSecretStorage.encrypt(mContext, storedValue);
        if (encryptedValue != null) {
            mSharedPreferences.edit().putString(key, encryptedValue).commit();
        }
        return storedValue;
    }

    private static void migrateSecureWalletValue(Context mContext, String key) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        String storedValue = mSharedPreferences.getString(key, null);
        if (storedValue == null || WalletSecretStorage.isEncryptedValue(storedValue)) return;

        String encryptedValue = WalletSecretStorage.encrypt(mContext, storedValue);
        if (encryptedValue != null) {
            mSharedPreferences.edit().putString(key, encryptedValue).commit();
        }
    }

    public static boolean setWalletSelectedNetworkKey(Context mContext, String networkKey) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(KEY_WALLET_SELECTED_NETWORK, networkKey);
        return editor.commit();
    }

    public static String getWalletSelectedNetworkKey(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString(KEY_WALLET_SELECTED_NETWORK, "ethereum");
    }

    public static boolean setWalletTestnetEnabled(Context mContext, boolean enabled) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(KEY_WALLET_TESTNET_ENABLED, enabled);
        return editor.commit();
    }

    public static boolean isWalletTestnetEnabled(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean(KEY_WALLET_TESTNET_ENABLED, false);
    }

    public static boolean setWalletCustomNetworksJson(Context mContext, String json) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(KEY_WALLET_CUSTOM_NETWORKS, json);
        return editor.commit();
    }

    public static String getWalletCustomNetworksJson(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString(KEY_WALLET_CUSTOM_NETWORKS, null);
    }

    // Security preferences
    public static boolean setSecurityBiometricEnabled(Context mContext, boolean enabled) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(KEY_SECURITY_BIOMETRIC_ENABLED, enabled);
        return editor.commit();
    }

    public static boolean isSecurityBiometricEnabled(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean(KEY_SECURITY_BIOMETRIC_ENABLED, false);
    }

    public static boolean setRequireAuthForTransactions(Context mContext, boolean required) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(KEY_REQUIRE_AUTH_FOR_TX, required);
        return editor.commit();
    }

    public static boolean isRequireAuthForTransactions(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean(KEY_REQUIRE_AUTH_FOR_TX, false);
    }

    // Theme preferences
    public static boolean setDarkThemeEnabled(Context mContext, boolean enabled) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(KEY_DARK_THEME_ENABLED, enabled);
        return editor.commit();
    }

    public static boolean isDarkThemeEnabled(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean(KEY_DARK_THEME_ENABLED, false);
    }

    // Wallet tokens (ERC-20) storage
    public static boolean setWalletTokens(Context mContext, List<TokenInfo> tokens) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        Gson gson = new Gson();
        editor.putString(getWalletAssetScopedKey(mContext, KEY_WALLET_TOKENS), gson.toJson(tokens));
        return editor.commit();
    }

    public static ArrayList<TokenInfo> getWalletTokens(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        String json = mSharedPreferences.getString(getWalletAssetScopedKey(mContext, KEY_WALLET_TOKENS), null);
        if (json == null) return new ArrayList<>();
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<TokenInfo>>(){}.getType();
        ArrayList<TokenInfo> list = gson.fromJson(json, listType);
        return list == null ? new ArrayList<>() : list;
    }

    // Wallet NFTs (ERC-721) storage
    public static boolean setWalletNfts(Context mContext, List<NftCollection> nfts) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        Gson gson = new Gson();
        editor.putString(getWalletAssetScopedKey(mContext, KEY_WALLET_NFTS), gson.toJson(nfts));
        return editor.commit();
    }

    public static ArrayList<NftCollection> getWalletNfts(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        String json = mSharedPreferences.getString(getWalletAssetScopedKey(mContext, KEY_WALLET_NFTS), null);
        if (json == null) return new ArrayList<>();
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<NftCollection>>(){}.getType();
        ArrayList<NftCollection> list = gson.fromJson(json, listType);
        return list == null ? new ArrayList<>() : list;
    }

    private static String getWalletAssetScopedKey(Context mContext, String baseKey) {
        String networkKey = getWalletSelectedNetworkKey(mContext);
        if (networkKey == null || networkKey.trim().isEmpty()) {
            networkKey = "ethereum";
        }
        String environment = "mainnet";
        String normalized = networkKey.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9_]+", "_");
        return baseKey + "_" + environment + "_" + normalized;
    }

    public static String getMobileNumber(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString(KEY_MOBILE_NUMBER, null);
    }


    /**
     * method to set last backup
     *
     * @param hasBackup this is parameter for setLastBackup  method
     */
    public static boolean setLastBackup(Context mContext, String hasBackup) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(KEY_LAST_BACKUP, hasBackup);
        return editor.commit();
    }

    /**
     * method to get last backup
     *
     * @return return value
     */
    public static String lastBackup(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString(KEY_LAST_BACKUP, null);
    }

    /**
     * method to set var as the user is new on the app
     *
     * @param isNew this is parameter for setIsNewUser  method
     */
    public static boolean setIsNewUser(Context mContext, Boolean isNew) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(KEY_NEW_USER, isNew);
        return editor.commit();
    }

    /**
     * method to check if user is new here the app
     *
     * @return return value
     */
    public static boolean isNewUser(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean(KEY_NEW_USER, false);
    }


    /**
     * method to set last backup
     *
     * @param version this is parameter for setLastBackup  method
     */
    public static boolean setVersionApp(Context mContext, int version) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(KEY_VERSION_APP, version);
        return editor.commit();
    }

    /**
     * method to get last backup
     *
     * @return return value
     */
    public static int getVersionApp(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getInt(KEY_VERSION_APP, 0);
    }

    /**
     * method to set the app is out date
     *
     * @param isNew this is parameter for setIsOutDate  method
     */
    public static boolean setIsOutDate(Context mContext, Boolean isNew) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(KEY_APP_IS_OUT_DATE, isNew);
        return editor.commit();
    }

    /**
     * method to check if the app is out date
     *
     * @return return value
     */
    public static boolean isOutDate(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean(KEY_APP_IS_OUT_DATE, false);
    }

    /**
     * method to check if the app is out date
     *
     * @return return value
     */
    public static void clearPreferences(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public static boolean isSampleDataSeeded(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getBoolean("SampleDataSeeded", false);
    }

    public static boolean setSampleDataSeeded(Context mContext, boolean seeded) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean("SampleDataSeeded", seeded);
        return editor.commit();
    }

    public static boolean setSocketServerUrl(Context mContext, String url) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(KEY_SOCKET_SERVER_URL, url);
        return editor.commit();
    }

    public static String getSocketServerUrl(Context mContext) {
        mSharedPreferences = mContext.getSharedPreferences(KEY_USER_PREF, Context.MODE_PRIVATE);
        return mSharedPreferences.getString(KEY_SOCKET_SERVER_URL, null);
    }
}
