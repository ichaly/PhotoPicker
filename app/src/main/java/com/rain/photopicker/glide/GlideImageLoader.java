package com.rain.photopicker.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.rain.library.loader.ImageLoader;
import com.rain.photopicker.R;

/**
 * Describe :GlideImageLoader
 * Email:baossrain99@163.com
 * Created by Rain on 17-5-3.
 */

public class GlideImageLoader implements ImageLoader {

    @Override
    public void displayImage(Context context, String originalImagePath, String thumbnailsImagePath, final ImageView imageView, boolean resize, boolean loadThumbnailsImage) {
        RequestOptions options = new RequestOptions().error(R.mipmap.image_error).placeholder(R.mipmap.image_placeholder);
        if (resize) {
            options = options.centerCrop();
        }
        RequestBuilder<Drawable> load;
        if (loadThumbnailsImage && !TextUtils.isEmpty(thumbnailsImagePath)) {
            load = Glide.with(context).load(originalImagePath).thumbnail(Glide.with(context).load(thumbnailsImagePath).apply(options));
        } else {
            load = Glide.with(context).load(originalImagePath);
        }
        load.apply(options).transition(DrawableTransitionOptions.withCrossFade()).into(imageView);
    }

    @Override
    public void displayImage(Context context, String originalImagePath, ImageView imageView, boolean resize) {
        displayImage(context, originalImagePath, null, imageView, resize, false);
    }

    @Override
    public void clearMemoryCache() {

    }
}
/*
 *   ┏┓　　　┏┓
 * ┏┛┻━━━┛┻┓
 * ┃　　　　　　　┃
 * ┃　　　━　　　┃
 * ┃　┳┛　┗┳　┃
 * ┃　　　　　　　┃
 * ┃　　　┻　　　┃
 * ┃　　　　　　　┃
 * ┗━┓　　　┏━┛
 *     ┃　　　┃
 *     ┃　　　┃
 *     ┃　　　┗━━━┓
 *     ┃　　　　　　　┣┓
 *     ┃　　　　　　　┏┛
 *     ┗┓┓┏━┳┓┏┛
 *       ┃┫┫　┃┫┫
 *       ┗┻┛　┗┻┛
 *        神兽保佑
 *        代码无BUG!
 */