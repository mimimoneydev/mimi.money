package com.money.mimi.api;

import com.money.mimi.app.EndPoints;
import com.money.mimi.models.messages.FilesResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface FilesUploadService {

    @Multipart
    @POST(EndPoints.UPLOAD_MESSAGES_IMAGE)
    Call<FilesResponse> uploadMessageImage(@Part MultipartBody.Part image);

    @Multipart
    @POST(EndPoints.UPLOAD_MESSAGES_VIDEO)
    Call<FilesResponse> uploadMessageVideo(@Part MultipartBody.Part video,
                                           @Part("thumbnail\"; filename=\"messageVideoThumbnail.jpg\" ") RequestBody thumbnail);

    @Multipart
    @POST(EndPoints.UPLOAD_MESSAGES_AUDIO)
    Call<FilesResponse> uploadMessageAudio(@Part MultipartBody.Part audio);

    @Multipart
    @POST(EndPoints.UPLOAD_MESSAGES_DOCUMENT)
    Call<FilesResponse> uploadMessageDocument(@Part MultipartBody.Part document);

}
