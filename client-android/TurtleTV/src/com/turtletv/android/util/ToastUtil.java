package com.turtletv.android.util;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {
	public static void toast(Context ctx, String info) {
		Toast.makeText(ctx, info, Toast.LENGTH_SHORT).show();
	}
}
