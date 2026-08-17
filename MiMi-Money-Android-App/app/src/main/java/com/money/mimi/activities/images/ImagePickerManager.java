package com.money.mimi.activities.images;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.money.mimi.R;
import com.money.mimi.helpers.MediaPicker;

/**
 * Created by Abderrahim El imame on 1/11/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class ImagePickerManager extends PickerManager {

    public ImagePickerManager(Activity activity) {
        super(activity);
    }

    protected void sendToExternalApp() {
        Intent intent = MediaPicker.createImageIntent();
        activity.startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
    }

    @Override
    public void setUri(Uri uri) {
        mProcessingPhotoUri = uri;
    }

}
