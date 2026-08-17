package com.money.mimi.activities.popups;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.money.mimi.R;
import com.money.mimi.activities.images.PickerBuilder;
import com.money.mimi.animations.AnimationsUtil;
import com.money.mimi.app.AppConstants;
import com.money.mimi.helpers.AppHelper;
import com.money.mimi.helpers.Files.FilesManager;
import com.money.mimi.helpers.Files.cache.ImageLoader;
import com.money.mimi.helpers.Files.cache.MemoryCache;
import com.money.mimi.helpers.PreferenceManager;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Abderrahim El imame on 1/15/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class WallpaperSelector extends Activity {


    @BindView(R.id.defaultBtnTxt)
    TextView defaultBtnTxt;
    @BindView(R.id.galleryBtnText)
    TextView galleryBtnText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_bottom_sheet_wallpaper);
        ButterKnife.bind(this);
        setTypeFaces();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.defaultBtn)
    public void setDefaultBtn() {
        PreferenceManager.setWallpaper(this, null);
        finish();
        AnimationsUtil.setSlideOutAnimation(this);
    }

    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            galleryBtnText.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            defaultBtnTxt.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.galleryBtn)
    public void setGalleryBtn() {
        new PickerBuilder(this, PickerBuilder.SELECT_FROM_GALLERY)
                    .setOnImageReceivedListener(imageUri -> {
                        Intent data = new Intent();
                        data.setData(imageUri);
                        String fileImagePath;
                        AppHelper.LogCat("new image SELECT_FROM_GALLERY" + imageUri);

                        fileImagePath = FilesManager.getPath(this.getApplicationContext(), data.getData());
                        if (fileImagePath != null) {
                            fileImagePath = FilesManager.persistFile(this.getApplicationContext(), fileImagePath, FilesManager.FILE_TYPE_IMAGE);
                        }
                        File file;
                        if (fileImagePath != null) {
                            file = new File(fileImagePath);
                            MemoryCache memoryCache = new MemoryCache();
                            //get filename from path
                            String filename = fileImagePath.substring(fileImagePath.lastIndexOf("/") + 1);
                            //remove extension
                            if (filename.indexOf(".") > 0)
                                filename = filename.substring(0, filename.lastIndexOf("."));

                            PreferenceManager.setWallpaper(this, filename);
                            ImageLoader.DownloadOfflineImage(memoryCache, file, filename, this, PreferenceManager.getID(this), AppConstants.USER, AppConstants.ROW_WALLPAPER);
                            AppHelper.CustomToast(this, getString(R.string.wallpaper_is_set));
                        }
                        finish();
                        AnimationsUtil.setSlideOutAnimation(this);
                    })
                    .setImageName(this.getString(R.string.app_name))
                    .setImageFolderName(this.getString(R.string.app_name))
                    .setCropScreenColor(R.color.colorPrimary)
                    .withTimeStamp(false)
                    .start();
    }
}
