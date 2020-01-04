package com.rain.photopicker;

import android.content.Context;
import android.os.Environment;
import com.rain.library.PhotoPickOptions;

import java.io.File;

/**
 * @author:duyu
 * @org :   www.yudu233.com
 * @email : yudu233@gmail.com
 * @date :  2019/5/10 14:44
 * @filename : PhotoPickOptionsConfig
 * @describe :
 */
public class PhotoPickOptionsConfig {

    public static PhotoPickOptions getPhotoPickOptions(Context context) {
        PhotoPickOptions options = new PhotoPickOptions();
        options.filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "eWorld/";
        options.photoPickAuthority = context.getPackageName() + ".provider";
        return options;
    }

}
