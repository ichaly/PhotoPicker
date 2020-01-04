package com.rain.library.weidget;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.rain.library.R;

/**
 * @author:duyu
 * @org :   www.yudu233.com
 * @email : yudu233@gmail.com
 * @date :  2019/8/6 18:10
 * @filename : LoadingDialog
 * @describe :
 */
public class LoadingDialog extends AlertDialog {
    public LoadingDialog(@NonNull Context context) {
        super(context, R.style.PhoAppTheme_AppTheme_AlertDialog);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_loading);
    }
}
