package com.example.safesphere.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.example.safesphere.R;

public class LoaderUtil {
    private Dialog dialog;
    public LoaderUtil(Context context){
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.layout_progress_bar);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void stopLoader(){
        dialog.dismiss();
    }
    public void showLoader(){
        dialog.show();
    }
}
