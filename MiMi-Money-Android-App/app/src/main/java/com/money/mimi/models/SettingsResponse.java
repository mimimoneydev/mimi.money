package com.money.mimi.models;

/**
 * Created by Abderrahim El imame on 03/05/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class SettingsResponse {
    private boolean adsVideoStatus;
    private boolean adsBannerStatus;
    private boolean adsInterstitialStatus;
    private String unitBannerID;
    private String unitVideoID;
    private String unitInterstitialID;
    private String appID;
    private int appVersion;
    private boolean adsWalletBannerStatus;
    private String unitWalletBannerID;
    private boolean adsMoneyBannerStatus;
    private String unitMoneyBannerID;
    private boolean adsSpaceBannerStatus;
    private String unitSpaceBannerID;

    public boolean isAdsVideoStatus() {
        return adsVideoStatus;
    }

    public void setAdsVideoStatus(boolean adsVideoStatus) {
        this.adsVideoStatus = adsVideoStatus;
    }

    public boolean isAdsBannerStatus() {
        return adsBannerStatus;
    }

    public void setAdsBannerStatus(boolean adsBannerStatus) {
        this.adsBannerStatus = adsBannerStatus;
    }

    public boolean isAdsInterstitialStatus() {
        return adsInterstitialStatus;
    }

    public void setAdsInterstitialStatus(boolean adsInterstitialStatus) {
        this.adsInterstitialStatus = adsInterstitialStatus;
    }

    public String getUnitBannerID() {
        return unitBannerID;
    }

    public void setUnitBannerID(String unitBannerID) {
        this.unitBannerID = unitBannerID;
    }

    public String getUnitVideoID() {
        return unitVideoID;
    }

    public void setUnitVideoID(String unitVideoID) {
        this.unitVideoID = unitVideoID;
    }

    public String getUnitInterstitialID() {
        return unitInterstitialID;
    }

    public void setUnitInterstitialID(String unitInterstitialID) {
        this.unitInterstitialID = unitInterstitialID;
    }

    public String getAppID() {
        return appID;
    }

    public void setAppID(String appID) {
        this.appID = appID;
    }

    public int getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(int appVersion) {
        this.appVersion = appVersion;
    }

    public boolean isAdsWalletBannerStatus() {
        return adsWalletBannerStatus;
    }

    public void setAdsWalletBannerStatus(boolean adsWalletBannerStatus) {
        this.adsWalletBannerStatus = adsWalletBannerStatus;
    }

    public String getUnitWalletBannerID() {
        return unitWalletBannerID;
    }

    public void setUnitWalletBannerID(String unitWalletBannerID) {
        this.unitWalletBannerID = unitWalletBannerID;
    }

    public boolean isAdsMoneyBannerStatus() {
        return adsMoneyBannerStatus;
    }

    public void setAdsMoneyBannerStatus(boolean adsMoneyBannerStatus) {
        this.adsMoneyBannerStatus = adsMoneyBannerStatus;
    }

    public String getUnitMoneyBannerID() {
        return unitMoneyBannerID;
    }

    public void setUnitMoneyBannerID(String unitMoneyBannerID) {
        this.unitMoneyBannerID = unitMoneyBannerID;
    }

    public boolean isAdsSpaceBannerStatus() {
        return adsSpaceBannerStatus;
    }

    public void setAdsSpaceBannerStatus(boolean adsSpaceBannerStatus) {
        this.adsSpaceBannerStatus = adsSpaceBannerStatus;
    }

    public String getUnitSpaceBannerID() {
        return unitSpaceBannerID;
    }

    public void setUnitSpaceBannerID(String unitSpaceBannerID) {
        this.unitSpaceBannerID = unitSpaceBannerID;
    }
}
