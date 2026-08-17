package com.money.mimi.api;


import com.money.mimi.app.EndPoints;
import com.money.mimi.models.BackupModel;
import com.money.mimi.models.auth.JoinModelResponse;
import com.money.mimi.models.NetworkModel;
import com.money.mimi.models.SettingsResponse;
import com.money.mimi.models.calls.CallSaverModel;
import com.money.mimi.models.messages.UpdateMessageModel;
import com.money.mimi.models.users.contacts.BlockResponse;
import com.money.mimi.models.users.contacts.ContactsModel;
import com.money.mimi.models.users.contacts.ProfileResponse;
import com.money.mimi.models.users.contacts.SyncContacts;
import com.money.mimi.models.users.status.EditStatus;
import com.money.mimi.models.users.status.StatusModel;
import com.money.mimi.models.users.status.StatusResponse;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by Abderrahim El imame on 02/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public interface APIContact {

    /**
     * method to  syncing all contacts
     *
     * @param listString this is parameter for  contacts method
     * @return this is return value
     */
    @POST(EndPoints.SEND_CONTACTS)
    Observable<List<ContactsModel>> contacts(@Body SyncContacts listString);

    /**
     * method to get contact info
     *
     * @param userID this is parameter for  contact method
     * @return this is return value
     */
    @GET(EndPoints.GET_CONTACT)
    Observable<ContactsModel> contact(@Query("userID") int userID);


    /**
     * method to get  user  status
     *
     * @return this is return value
     */
    @GET(EndPoints.GET_STATUS)
    Observable<List<StatusModel>> status();

    /**
     * method to delete user status
     *
     * @param statusId this is parameter for  delete status method
     * @return this is return value
     */
    @DELETE(EndPoints.DELETE_STATUS)
    Observable<StatusResponse> deleteStatus(@Query("statusId") int statusId);


    /**
     * method to delete all user status
     *
     * @return this is return value
     */
    @DELETE(EndPoints.DELETE_ALL_STATUS)
    Observable<StatusResponse> deleteAllStatus();

    /**
     * method to update user status
     *
     * @param statusID this is parameter for  update status method
     * @return this is return value
     */
    @PUT(EndPoints.UPDATE_STATUS)
    Observable<StatusResponse> updateStatus(@Query("statusID") int statusID);

    /**
     * method to edit user status
     *
     * @param editStatus this is parameter for  editStatus method
     * @return this is return value
     */
    @POST(EndPoints.EDIT_STATUS)
    Observable<StatusResponse> editStatus(@Body EditStatus editStatus);

    /**
     * method to edit username
     *
     * @param editStatus this is parameter for  editUsername method
     * @return this is return value
     */
    @POST(EndPoints.EDIT_NAME)
    Observable<StatusResponse> editUsername(@Body EditStatus editStatus);

    /**
     * method to edit group name
     *
     * @param editStatus this is parameter for  editGroupName method
     * @return this is return value
     */
    @POST(EndPoints.EDIT_GROUP_NAME)
    Observable<StatusResponse> editGroupName(@Body EditStatus editStatus);

    /**
     * method to edit user image
     *
     * @param image this is parameter for  uploadImage method
     * @return this is return value
     */
    @Multipart
    @POST(EndPoints.UPLOAD_PROFILE_IMAGE)
    Observable<ProfileResponse> uploadImage(@Part("image\"; filename=\"profileImage.jpg\" ") RequestBody image);


    @POST(EndPoints.SAVE_EMITTED_CALL)
    Observable<BlockResponse> saveEmittedCall(@Body CallSaverModel callSaverModel);

    @POST(EndPoints.SAVE_ACCEPTED_CALL)
    Observable<BlockResponse> saveAcceptedCall(@Body CallSaverModel callSaverModel);

    @POST(EndPoints.SAVE_RECEIVED_CALL)
    Observable<BlockResponse> saveReceivedCall(@Body CallSaverModel callSaverModel);

    /**
     * Block user
     *
     * @param userId This is the  parameter to follow method
     * @return return value
     */
    @GET(EndPoints.BLOCK_USER)
    Observable<BlockResponse> block(@Query("userId") int userId);

    /**
     * UnBlock user
     *
     * @param userId This is the  parameter to unFollow method
     * @return return value
     */
    @GET(EndPoints.UN_BLOCK_USER)
    Observable<BlockResponse> unBlock(@Query("userId") int userId);

    /**
     * method userHasBackup
     *
     * @param hasBackup this is  parameter for  uploadMessageBackup method
     * @return this is return value
     */
    @FormUrlEncoded
    @POST(EndPoints.HAS_BACKUP)
    Observable<BackupModel> userHasBackup(@Field("hasBackup") String hasBackup);

    /**
     * method to delete account
     *
     * @param phone this is parameter for  uploadImage method
     * @return this is return value
     */
    @FormUrlEncoded
    @POST(EndPoints.DELETE_ACCOUNT)
    Observable<JoinModelResponse> deleteAccount(@Field("walletAddress") String phone, @Field("country") String country);


    /**
     * method to verify the user code
     *
     * @param code this is parameter for verifyUser method
     * @return this is what method will return
     */
    @FormUrlEncoded
    @POST(EndPoints.DELETE_ACCOUNT_CONFIRMATION)
    Observable<StatusResponse> deleteAccountConfirmation(@Field("code") String code);

    /**
     * method to get video ads info
     *
     * @return this is return value
     */
    @GET(EndPoints.GET_APPLICATION_SETTINGS)
    Observable<SettingsResponse> getAppSettings();

    /**
     * method to get app privacy and terms
     *
     * @return this is return value
     */
    @GET(EndPoints.GET_APPLICATION_PRIVACY)
    Observable<StatusResponse> getPrivacyTerms();


    /**
     * method to check network state
     *
     * @return this is return value
     */
    @GET(EndPoints.CHECK_NETWORK)
    Observable<NetworkModel> checkNetwork();

    @POST(EndPoints.SEND_MESSAGE)
    Observable<StatusResponse> sendMessage(@Body UpdateMessageModel updateMessageModel);


    @POST(EndPoints.SEND_GROUP_MESSAGE)
    Observable<StatusResponse> sendGroupMessage(@Body UpdateMessageModel updateMessageModel);

    @FormUrlEncoded
    @POST(EndPoints.UPDATE_FCM_TOKEN)
    Observable<StatusResponse> updateFcmToken(@Field("registeredId") String fcmToken);
}
