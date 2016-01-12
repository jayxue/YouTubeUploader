package com.wms.youtubeuploader.sdk.util;

import android.content.Context;
import android.os.Environment;

public class FileUtil {

	/**
	 * Return a path of external storage + package name like /mnt/sdcard/com.company.app
	 *
	 * @param context
	 * @return path of the external storage
	 */
	public static String getAppExternalStoragePath(Context context) {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + context.getApplicationContext().getPackageName();
	}

}
