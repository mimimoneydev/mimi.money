package com.money.mimi.models.groups;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class GroupsModel extends RealmObject {
    @PrimaryKey
    private int id;
    private String CreatedDate;
    private int Status;
    private String GroupName;
    private String GroupImage;
    private int CreatorID;
    private String Creator;
    private boolean isAdmin;
    private boolean isDeleted;
    private boolean isLeft;
    private RealmList<MembersGroupModel> Members;

    public GroupsModel() {
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public boolean isLeft() {
        return isLeft;
    }

    public void setLeft(boolean left) {
        isLeft = left;
    }

    public RealmList<MembersGroupModel> getMembers() {
        return Members;
    }

    public void setMembers(RealmList<MembersGroupModel> members) {
        Members = members;
    }

    public int getCreatorID() {
        return CreatorID;
    }

    public void setCreatorID(int creatorID) {
        CreatorID = creatorID;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getCreator() {
        return Creator;
    }

    public void setCreator(String creator) {
        Creator = creator;
    }

    public String getGroupName() {
        return GroupName;
    }

    public void setGroupName(String groupName) {
        GroupName = groupName;
    }

    public String getGroupImage() {
        return GroupImage;
    }

    public void setGroupImage(String groupImage) {
        GroupImage = groupImage;
    }


    public String getCreatedDate() {
        return CreatedDate;
    }

    public void setCreatedDate(String createdDate) {
        CreatedDate = createdDate;
    }

    public int getStatus() {
        return Status;
    }

    public void setStatus(int status) {
        Status = status;
    }

}
