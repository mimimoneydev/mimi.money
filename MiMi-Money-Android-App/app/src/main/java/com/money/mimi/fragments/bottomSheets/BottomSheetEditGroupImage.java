package com.money.mimi.fragments.bottomSheets;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.money.mimi.R;
import com.money.mimi.activities.images.PickerBuilder;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.helpers.PermissionHandler;
import com.money.mimi.helpers.MediaPicker;
import com.money.mimi.presenters.groups.EditGroupPresenter;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.app.Activity.RESULT_OK;

/**
 * Created by abderrahimelimame on 6/9/16.
 * Email : abderrahim.elimame@gmail.com
 */

public class BottomSheetEditGroupImage extends BottomSheetDialogFragment {

    private View mView;
    @BindView(R.id.cameraBtn)
    FrameLayout cameraBtn;
    @BindView(R.id.galleryBtn)
    FrameLayout galleryBtn;
    private EditGroupPresenter mEditGroupPresenter;
    private Uri mProcessingPhotoUri;

    @Override
    public void onStart() {
        super.onStart();


    }

    private void setGalleryBtn() {
        if (getActivity() == null) {
            return;
        }
        dismiss();
        Intent intent = MediaPicker.createImageIntent();
        getActivity().startActivityForResult(intent, AppConstants.SELECT_PROFILE_PICTURE);
    }

    protected void sendToExternalApp() {
        if (getActivity() == null) {
            return;
        }
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mProcessingPhotoUri = FilesManager.getImageFile(getActivity());
        EditGroupPresenter.setLastCameraImageUri(mProcessingPhotoUri);
        if (mProcessingPhotoUri != null)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mProcessingPhotoUri);
        getActivity().startActivityForResult(cameraIntent, AppConstants.SELECT_PROFILE_CAMERA);
    }


    private void setCameraBtn() {
        if (PermissionHandler.checkPermission(getActivity(), Manifest.permission.CAMERA)) {
            AppHelper.LogCat("camera permission already granted.");
            if (AppHelper.isAndroid6()) {
                dismiss();
                sendToExternalApp();
            } else {
                dismiss();
                new PickerBuilder(getActivity(), PickerBuilder.SELECT_FROM_CAMERA)
                        .setOnImageReceivedListener(imageUri -> {
                            AppHelper.LogCat("new image SELECT_FROM_CAMERA " + imageUri);
                            Intent data = new Intent();
                            data.setData(imageUri);
                            mEditGroupPresenter.onActivityResult(AppConstants.SELECT_PROFILE_CAMERA, RESULT_OK, data);

                        })
                        .setImageName(getActivity().getString(R.string.app_name))
                        .setImageFolderName(getActivity().getString(R.string.app_name))
                        .setCropScreenColor(R.color.colorPrimary)
                        .withTimeStamp(false)
                        .setOnPermissionRefusedListener(() -> {
                            PermissionHandler.requestPermission(getActivity(), Manifest.permission.CAMERA);
                        })
                        .start();
            }
        } else {
            AppHelper.LogCat("Please request camera  permission.");
            PermissionHandler.requestPermission(getActivity(), Manifest.permission.CAMERA);
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.content_bottom_sheet, container, false);
        ButterKnife.bind(this, mView);
        mEditGroupPresenter = new EditGroupPresenter(this);
        galleryBtn.setOnClickListener(v -> setGalleryBtn());
        cameraBtn.setOnClickListener(v -> setCameraBtn());
        return mView;
    }

    @Override
    public void onViewCreated(View contentView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(contentView, savedInstanceState);
        initView();
    }

    public void initView() {

    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
    }

}
