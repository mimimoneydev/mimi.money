package com.money.mimi.models.users.contacts;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class ContactsModel extends RealmObject {
    @PrimaryKey
    private int id;
    private int contactID;
    private String username;
    private String walletAddress;
    private String walletAddressTmp;
    private boolean Linked;
    private boolean Activate;
    private boolean Exist;
    private String image;
    private String status;
    private String status_date;
    private String socketId;
    private String firstName;
    private String lastName;
    private String category;

    @Ignore
    private boolean online;

    public ContactsModel() {

    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getSocketId() {
        return socketId;
    }

    public void setSocketId(String socketId) {
        this.socketId = socketId;
    }


    public String getWalletAddressTmp() {
        return walletAddressTmp;
    }

    public void setWalletAddressTmp(String walletAddressTmp) {
        this.walletAddressTmp = walletAddressTmp;
    }


    public boolean isActivate() {
        return Activate;
    }

    public void setActivate(boolean activate) {
        Activate = activate;
    }

    public boolean isExist() {
        return Exist;
    }

    public void setExist(boolean exist) {
        Exist = exist;
    }


    public int getContactID() {
        return contactID;
    }

    public void setContactID(int contactID) {
        this.contactID = contactID;
    }


    public String getStatus_date() {
        return status_date;
    }

    public void setStatus_date(String status_date) {
        this.status_date = status_date;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public boolean isLinked() {
        return Linked;
    }

    public void setLinked(boolean linked) {
        Linked = linked;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
