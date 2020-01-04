package com.rain.library.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.rain.library.BaseActivity;
import com.rain.library.PhotoPick;
import com.rain.library.PhotoPickOptions;
import com.rain.library.R;
import com.rain.library.bean.MediaData;
import com.rain.library.bean.PhotoPreviewBean;
import com.rain.library.controller.PhotoPickConfig;
import com.rain.library.controller.PhotoPreviewConfig;
import com.rain.library.impl.CommonResult;
import com.rain.library.impl.PhotoSelectCallback;
import com.rain.library.observer.UpdateUIObserver;
import com.rain.library.utils.MimeType;
import com.rain.library.utils.Rlog;
import com.rain.library.utils.UtilsHelper;
import com.rain.library.weidget.HackyViewPager;
import com.rain.library.weidget.LoadingDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.ListIterator;


/**
 * Describe :仿微信图片预览
 * Email:baossrain99@163.com
 * Created by Rain on 17-5-3.
 */
public class PhotoPreviewActivity extends BaseActivity implements OnPhotoTapListener {

    private static final String TAG = "PhotoPreviewActivity";

    private ArrayList<MediaData> photos;            //全部图片集合
    private ArrayList<MediaData> selectPhotosInfo;  //选中的图片集合信息

    private CheckBox checkbox;
    private RadioButton radioButton;
    private int pos;                    //当前位置
    private int maxPickSize;            //最大选择个数
    private boolean isChecked = false;  //是否已选定
    private boolean originalPicture;    //是否选择的是原图
    private PhotoSelectCallback callback;

    private static final int MAX_SCALE = 3;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        loadingDialog = new LoadingDialog(this);
        Bundle bundle = getIntent().getBundleExtra(PhotoPreviewConfig.EXTRA_BUNDLE);
        if (bundle == null) {
            throw new NullPointerException("bundle is null,please init it");
        }
        PhotoPreviewBean bean = bundle.getParcelable(PhotoPreviewConfig.EXTRA_BEAN);
        if (bean == null) {
            finish();
            return;
        }
        photos = PhotoPreviewConfig.getPhotos();
        if (photos == null || photos.isEmpty()) {
            finish();
            return;
        }

        originalPicture = bean.isOriginalPicture();
        maxPickSize = bean.getMaxPickSize();
        selectPhotosInfo = bean.getSelectPhotosInfo();
        callback = PhotoPickConfig.getInstance().getCallback();
        setContentView(R.layout.activity_photo_select);

        radioButton = (RadioButton) findViewById(R.id.radioButton);
        checkbox = (CheckBox) findViewById(R.id.checkbox);
        HackyViewPager viewPager = (HackyViewPager) findViewById(R.id.pager);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(PhotoPick.getToolbarBackGround());
        toolbar.setTitle((bean.getPosition() + 1) + "/" + photos.size());
        toolbar.setNavigationIcon(PhotoPickOptions.DEFAULT.backIcon);
        setSupportActionBar(toolbar);

        //照片滚动监听，更改ToolBar数据
        viewPager.addOnPageChangeListener(onPageChangeListener);
        //选中
        checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectPhotosInfo == null) {
                    selectPhotosInfo = new ArrayList<>();
                }
                if (!checkbox.isChecked()) {
                    selectPhotosInfo.remove(photos.get(pos));
                    updateMenuItemTitle();
                    UpdateUIObserver.getInstance().sendUpdateUIMessage(pos, photos.get(pos), checkbox.isChecked());
                } else {
                    //判断是否同一类型文件
                    String mimeType = selectPhotosInfo.size() > 0 ? selectPhotosInfo.get(0).getImageType() : "";
                    if (!TextUtils.isEmpty(mimeType)) {
                        boolean toEqual = MimeType.mimeToEqual(mimeType, photos.get(pos).getImageType());
                        if (!toEqual) {
                            PhotoPick.toast(R.string.tips_rule);
                            checkbox.setChecked(false);
                            return;
                        }
                    }

                    if (selectPhotosInfo.size() == maxPickSize && checkbox.isChecked()) {
                        checkbox.setChecked(false);
                        PhotoPick.toast(getString(R.string.tips_max_num, maxPickSize));
                        return;
                    }
                    selectPhotosInfo.add(photos.get(pos));
                    updateMenuItemTitle();
                    UpdateUIObserver.getInstance().sendUpdateUIMessage(pos, photos.get(pos), checkbox.isChecked());
                }
            }
        });

        //原图
        if (originalPicture) {
            radioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isChecked) {
                        radioButton.setChecked(false);
                        isChecked = false;
                        radioButton.setText(getString(R.string.original_image));
                    } else {
                        radioButton.setChecked(true);
                        isChecked = true;
                        radioButton.setText(getString(R.string.image_size, UtilsHelper.formatFileSize(photos.get(pos).getOriginalSize())));
                        if (!checkbox.isChecked()) {
                            checkbox.setChecked(true);
                            selectPhotosInfo.add(photos.get(pos));
                            updateMenuItemTitle();
                            UpdateUIObserver.getInstance().sendUpdateUIMessage(pos, photos.get(pos), checkbox.isChecked());
                        }
                    }
                }
            });
        } else {
            radioButton.setVisibility(View.GONE);
        }

        viewPager.setAdapter(new ImagePagerAdapter());
        viewPager.setCurrentItem(bean.getPosition());
        if (bean.getPosition() == 0) {
            onPageChangeListener.onPageSelected(bean.getPosition());
        }
    }

    ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            pos = position;
            toolbar.setTitle(position + 1 + "/" + photos.size());

            if (isChecked(position)) {
                checkbox.setChecked(true);
            } else {
                checkbox.setChecked(false);
            }
            if (!originalPicture) {
                radioButton.setVisibility(View.GONE);
                return;
            }

            if (originalPicture && radioButton.isChecked()) {
                radioButton.setText(getString(R.string.image_size, UtilsHelper.formatFileSize(photos.get(pos).getOriginalSize())));
            } else {
                radioButton.setText(getString(R.string.original_image));
            }
            if (MimeType.isPictureType(photos.get(position).getImageType()) == MimeType.ofVideo()) {
                radioButton.setVisibility(View.GONE);
            } else {
                radioButton.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };


    /**
     * 判断当前图片是否选中
     *
     * @param position
     * @return
     */
    private boolean isChecked(int position) {
        if (selectPhotosInfo == null || selectPhotosInfo.size() == 0) {
            return false;
        } else {
            for (MediaData mediaData : selectPhotosInfo) {
                if (mediaData.getOriginalPath().equals(photos.get(position).getOriginalPath())) {
                    return true;
                }
            }
            return false;
        }
    }

    private void updateMenuItemTitle() {
        if (selectPhotosInfo.isEmpty()) {
            menuItem.setTitle(R.string.send);
        } else {
            menuItem.setTitle(getString(R.string.sends, selectPhotosInfo.size(), maxPickSize));
        }
    }

    private MenuItem menuItem;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_send, menu);
        menuItem = menu.findItem(R.id.send);
        updateMenuItemTitle();
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (PhotoPick.isTimeEnabled()) {
            if (item.getItemId() == R.id.send) {
                if (!selectPhotosInfo.isEmpty()) {
                    if (PhotoPickConfig.getInstance().isStartCompression() && !isChecked && !MimeType.isVideo(selectPhotosInfo.get(0).getImageType())) {
                        if (loadingDialog != null) {
                            loadingDialog.show();
                        }
                        checkImages();
                        PhotoPick.startCompression(PhotoPreviewActivity.this, selectPhotosInfo, compressResult);
                    } else {
                        sendImage();
                    }
                }
            } else {
                backTo();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendImage() {
        if (callback != null) {
            callback.selectResult(selectPhotosInfo);
            setResult(RESULT_OK, new Intent());
        } else {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra(PhotoPickConfig.EXTRA_SELECT_PHOTOS, selectPhotosInfo);
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    private void checkImages() {
        //检测图片是否存在/损坏
        ListIterator<MediaData> iterator = selectPhotosInfo.listIterator();
        while (iterator.hasNext()) {
            String mediaPath;
            MediaData media = iterator.next();
            if (media.isClip()) {
                mediaPath = media.getClipImagePath();
            } else if (media.isCamera()) {
                mediaPath = media.getCameraImagePath();
            } else if (media.isCompressed()) {
                mediaPath = media.getCompressionPath();
            } else {
                mediaPath = media.getOriginalPath();
            }

            if (!new File(mediaPath).exists()) {
                Rlog.e("文件不存在" + selectPhotosInfo.size());
                iterator.remove();
            }

        }
    }

    private int index = 0;

    private CommonResult<File> compressResult = new CommonResult<File>() {
        @Override
        public void onSuccess(File file, boolean success) {

            if (success && file.exists()) {
                Rlog.e("Rain", "Luban compression success:" + file.getAbsolutePath() + " ; image length = " + file.length());
                MediaData photo = selectPhotosInfo.get(index);
                photo.setCompressed(true);
                photo.setCompressionPath(file.getAbsolutePath());
                index++;

                if (index > 0 && index == selectPhotosInfo.size()) {
                    Rlog.e("Rain", "all select image compression success!");
                    if (loadingDialog != null) {
                        loadingDialog.dismiss();
                    }
                    Intent intent = new Intent();
                    if (callback != null) {
                        callback.selectResult(selectPhotosInfo);
                    } else {
                        intent.putParcelableArrayListExtra(PhotoPickConfig.EXTRA_SELECT_PHOTOS, selectPhotosInfo);
                    }
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            } else {
                MediaData photo = selectPhotosInfo.get(index);
                photo.setCompressed(true);
                photo.setCompressionPath(photo.getOriginalPath());
                index++;
            }
        }
    };


    private void backTo() {
        if (isChecked) {
            PhotoPickConfig.getInstance().setStartCompression(false);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        backTo();
        super.onBackPressed();
    }

    private boolean toolBarStatus = true;

    //隐藏ToolBar
    private void hideViews() {
        toolBarStatus = false;
        toolbar.animate().translationY(-toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(2));
    }

    //显示ToolBar
    private void showViews() {
        toolBarStatus = true;
        toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2));
    }

    //单击图片时操作
    @Override
    public void onPhotoTap(ImageView view, float x, float y) {
        if (MimeType.isPictureType(photos.get(pos).getImageType()) == MimeType.ofVideo()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String type = "video/*";
            Uri uri = FileProvider.getUriForFile(this, PhotoPickOptions.DEFAULT.photoPickAuthority, new File(photos.get(pos).getOriginalPath()));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setDataAndType(uri, type);
            startActivity(intent);
        }
        finish();
    }

    private class ImagePagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return photos.size();
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {
            View view;
            View longView = LayoutInflater.from(PhotoPreviewActivity.this).inflate(R.layout.item_photo_preview_long, container, false);
            View simpleView = LayoutInflater.from(PhotoPreviewActivity.this).inflate(R.layout.item_photo_preview, container, false);

            String originalImagePath = photos.get(position).getOriginalPath();
            int imageWidth = photos.get(position).getImageWidth() == 0 ? UtilsHelper.getScreenWidth(PhotoPreviewActivity.this) :
                    photos.get(position).getImageWidth();
            int imageHeight = photos.get(position).getImageHeight() == 0 ? UtilsHelper.getScreenHeight(PhotoPreviewActivity.this) :
                    photos.get(position).getImageHeight();
            if (imageHeight / imageWidth > MAX_SCALE) {
                //加载长截图
                view = longView;
                SubsamplingScaleImageView imageView = longView.findViewById(R.id.iv_media_image);
                float scale = UtilsHelper.getImageScale(PhotoPreviewActivity.this, originalImagePath);
                imageView.setImage(ImageSource.uri(originalImagePath),
                        new ImageViewState(scale, new PointF(0, 0), 0));
            } else {
                view = simpleView;
                PhotoView imageView = (PhotoView) simpleView.findViewById(R.id.iv_media_image);
                imageView.setOnPhotoTapListener(PhotoPreviewActivity.this);
                PhotoPickConfig.getInstance().getImageLoader().displayImage(PhotoPreviewActivity.this, originalImagePath, imageView, false);
            }
            if (MimeType.isVideo(photos.get(position).getImageType())) {
                simpleView.findViewById(R.id.imv_play).setVisibility(View.VISIBLE);
            }
            container.addView(view, 0);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.image_pager_exit_animation);
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
