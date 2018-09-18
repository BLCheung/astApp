package com.ast.zbl.util;

import android.content.Context;
import android.widget.Toast;

/**
 * @author Created by BLCheung
 * @email 925306022@qq.com
 * @date Created on 2018/9/6 14:49
 */
public class ToastUtil {
    private static Toast toast;

    public static void showToast(Context context, String content) {
        if (toast == null) {
            toast = Toast.makeText(context, content, Toast.LENGTH_SHORT);
        } else {
            toast.setText(content);
        }
        toast.show();
    }
}
