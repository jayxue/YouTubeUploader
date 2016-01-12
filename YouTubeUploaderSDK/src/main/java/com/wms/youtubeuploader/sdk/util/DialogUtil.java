package com.wms.youtubeuploader.sdk.util;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

import com.wms.youtubeuploader.sdk.R;

public class DialogUtil {

	public static ProgressDialog showWaitingProgressDialog(Context context, int style, String message, boolean cancelable) {
		ProgressDialog progressDialog = new ProgressDialog(context);
		progressDialog.setProgressStyle(style);
		progressDialog.setMessage(message);
		progressDialog.setCancelable(cancelable);
		try {
			// Avoid window leaking exception after existing an activity
			progressDialog.show();
		}
		catch(Exception e) {
			
		}
		return progressDialog;
	}

	public static void showExceptionAlertDialog(Context context, String title, String message) {
		Builder exceptionAlertDialogBuilder = new Builder(context);
		exceptionAlertDialogBuilder.setTitle(title).setMessage(message).setCancelable(true).setNeutralButton(context.getString(R.string.ok), null);
		AlertDialog alert = exceptionAlertDialogBuilder.create();
		try {
			// Avoid window leaking exception after existing an activity
			alert.show();
		}
		catch(Exception e) {
			
		}			
	}

	public static void showDialog(Context context, String msg) {
		Builder builder = new Builder(context);
		EditText msgEditText = new EditText(context);
		msgEditText.setText(msg);
		msgEditText.setFocusable(false);
		builder.setCancelable(false).setView(msgEditText).setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		try {
			// Avoid window leaking exception after existing an activity
			alert.show();
		}	
		catch(Exception e) {
			
		}
	}

}
