package com.wms.youtubeuploader.sdk.activity;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.common.AccountPicker;
import com.wms.youtubeuploader.sdk.R;
import com.wms.youtubeuploader.sdk.dialog.ConfirmUploadVideoDialogBuilder;
import com.wms.youtubeuploader.sdk.handler.FetchTokenHandler;
import com.wms.youtubeuploader.sdk.handler.UploadProgressHandler;
import com.wms.youtubeuploader.sdk.listener.ImageButtonBackgroundSelector;
import com.wms.youtubeuploader.sdk.task.FetchYouTubeTokenTask;
import com.wms.youtubeuploader.sdk.util.DialogUtil;
import com.wms.youtubeuploader.sdk.util.FileUtil;
import com.wms.youtubeuploader.sdk.task.YouTubeUploadTask;

import java.io.File;
import java.util.UUID;

/**
 * In order to upload videos to YouTube, you need to create a Google API project in Google Developers Console (https://console.developers.google.com). In the API project, under
 * APIs & Auth and Credentials, create two OAuth client IDs for each individual app, one for dev version and one for release version. Enter package name and SHA1 code for it.
 * You'll need to wait for 15 minutes or longer until the ID takes effect.
 */
public class UploadVideoActivity extends Activity {
	private ProgressBar progressBarUploadVideo = null;
	private EditText editTextVideoTitle = null;
	private EditText editTextVideoDescription = null;
	private TextView textViewFilePath = null;
	private TextView textViewVideoUrl = null;
	private TextView textViewProgress = null;
	private ImageButton imageButtonTakeVideo = null;
	private ImageButton imageButtonGallery = null;
	private ImageButton imageButtonUploadVideo = null;
	private VideoView videoViewPreview = null;

	private String videoFileName = "";

	private String selectedGoogleAccount;

	private FetchTokenHandler fetchTokenHandler = null;
	private UploadProgressHandler uploadProgressHandler = null;

	private YouTubeUploadTask youtubeUploadTask = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.upload_video);

		editTextVideoTitle = (EditText) findViewById(R.id.editTextTitle);
		editTextVideoDescription = (EditText) findViewById(R.id.editTextDescription);

		textViewFilePath = (TextView) findViewById(R.id.textViewFilePath);
		textViewVideoUrl = (TextView) findViewById(R.id.textViewVideoUrl);
		textViewProgress = (TextView) findViewById(R.id.textViewProgress);

		videoViewPreview = (VideoView) findViewById(R.id.videoViewPreview);

		imageButtonUploadVideo = (ImageButton) findViewById(R.id.imageButtonUploadVideo);
		imageButtonUploadVideo.setOnClickListener(new ImageButtonUploadVideoOnClickListener());
		imageButtonUploadVideo.setOnTouchListener(new ImageButtonBackgroundSelector());
		imageButtonUploadVideo.setEnabled(false);

		imageButtonTakeVideo = (ImageButton) findViewById(R.id.imageButtonTakeVideo);
		imageButtonTakeVideo.setOnClickListener(new ImageButtonTakeVideoOnClickListener());
		imageButtonTakeVideo.setOnTouchListener(new ImageButtonBackgroundSelector());

		imageButtonGallery = (ImageButton) findViewById(R.id.imageButtonGallery);
		imageButtonGallery.setOnClickListener(new ImageButtonGalleryOnClickListener());
		imageButtonGallery.setOnTouchListener(new ImageButtonBackgroundSelector());

		progressBarUploadVideo = (ProgressBar) findViewById(R.id.progressBarUploadVideo);

		uploadProgressHandler = new UploadProgressHandler(this);
		fetchTokenHandler = new FetchTokenHandler(this);

		// Do not show the soft keyboard
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode,Intent data) {
		if (requestCode == IntentRequestCode.TAKE_VIDEO && resultCode == RESULT_OK) {
			// videoFileName has been prepared for taking video
			File file = new File(videoFileName);
			// In Android 2.2, the file may not be created, therefore we need to check the returned URI.
			if (!file.exists()) {
				if (data.getData() != null) {
					videoFileName = getRealPathFromURI(data.getData());
				}
				else {
					Toast.makeText(this, getString(R.string.videoNotAvailable), Toast.LENGTH_LONG).show();
					return;
				}
			}
			onVideoReady();
		}
		else if (requestCode == IntentRequestCode.PICK_UP_VIDEO && resultCode == RESULT_OK) {
			Uri selectedVideo = data.getData();
			String[] filePathColumn = { MediaStore.Video.Media.DATA };
			Cursor cursor = getContentResolver().query(selectedVideo, filePathColumn, null, null, null);
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String filePath = cursor.getString(columnIndex);
			cursor.close();
			File file = new File(filePath);
			videoFileName = file.getAbsolutePath();
			onVideoReady();
		}
		else if (requestCode ==  IntentRequestCode.REQUEST_ACCOUNT_PICKER &&resultCode == Activity.RESULT_OK) {
			if(data != null	&& data.getExtras() != null) {
				String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					selectedGoogleAccount = accountName;
					fetchToken();
				}
				else {
					DialogUtil.showExceptionAlertDialog(this, getString(R.string.googleAccountNotSelected), getString(R.string.googleAccountNotSupported));
				}
			}
		}
		else if(requestCode == IntentRequestCode.REQUEST_AUTHORIZATION && resultCode == Activity.RESULT_OK) {
			// Account has been chosen and permissions have been granted. You can upload video
			Toast.makeText(this, getString(R.string.appAuthorized), Toast.LENGTH_LONG).show();
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private class ImageButtonUploadVideoOnClickListener implements ImageButton.OnClickListener {

		@Override
		public void onClick(View v) {
			// Title must be provided for a YouTube video
			if(editTextVideoTitle.getText().toString().trim().isEmpty()) {
				DialogUtil.showDialog(UploadVideoActivity.this, getString(R.string.enterVideoTitle));
				return;
			}

			new ConfirmUploadVideoDialogBuilder(UploadVideoActivity.this, uploadProgressHandler).create().show();
		}

	}

	private File getTempVideoFile() {
		// It will return a file path like: /mnt/sdcard/com.company.app
		videoFileName = FileUtil.getAppExternalStoragePath(this);
		File file = new File(videoFileName);
		if (!file.exists()) {
			// Create the folder if it does not exist
			file.mkdir();
		}

		// Generate a UUID as file name
		videoFileName += "/" + UUID.randomUUID().toString() + ".3gp";

		file = new File(videoFileName);
		return file;
	}

	private class ImageButtonTakeVideoOnClickListener implements ImageButton.OnClickListener {

		@Override
		public void onClick(View v) {
			startTakeVideo();
		}

	}

	private class ImageButtonGalleryOnClickListener implements ImageButton.OnClickListener {

		@Override
		public void onClick(View v) {
			startPickVideo();
		}

	}

	private void startTakeVideo() {
		resetProgress();

		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getTempVideoFile()));
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
		intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 300);
		startActivityForResult(intent, IntentRequestCode.TAKE_VIDEO);
	}

	private void startPickVideo() {
		resetProgress();

		Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
		try {
			startActivityForResult(intent, IntentRequestCode.PICK_UP_VIDEO);
		}
		catch (ActivityNotFoundException e) {
			 // On Andriod 2.2, the above method may cause exception due to not finding an activity to handle the intent. Use the method below instead.
			Intent mediaChooser = new Intent(Intent.ACTION_GET_CONTENT);
			mediaChooser.setType("video/*");
			startActivityForResult(mediaChooser, IntentRequestCode.PICK_UP_VIDEO);
		}
		catch (SecurityException e) {
			// When picking up videos, there may be an exception:
			//  java.lang.SecurityException:
			//      Permission Denial:
			//      starting Intent { act=android.intent.action.PICK
			//      dat=content://media/external/video/media
			//      cmp=com.android.music/.VideoBrowserActivity } from ProcessRecord
			// Try another way to start the intent
			intent = new Intent(Intent.ACTION_PICK, null);
			intent.setType("video/*");
			try {
				startActivityForResult(intent, IntentRequestCode.PICK_UP_VIDEO);
			} catch (Exception ex) {
				DialogUtil.showExceptionAlertDialog(UploadVideoActivity.this, getString(R.string.cannotPickUpVideo), getString(R.string.notSupportedOnDevice));
			}
		}
	}

	@SuppressWarnings("deprecation")
	private String getRealPathFromURI(Uri contentUri) {
		String[] projection = { MediaStore.Video.Media.DATA };
		Cursor cursor = managedQuery(contentUri, projection, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	private void onVideoReady() {
		MediaController mediaController = new MediaController(this);
		videoViewPreview.setVisibility(View.VISIBLE);
		videoViewPreview.setVideoPath(videoFileName);
		videoViewPreview.setMediaController(mediaController);
		videoViewPreview.requestFocus();
		videoViewPreview.start();
		videoViewPreview.pause();

		imageButtonUploadVideo.setEnabled(true);
		imageButtonUploadVideo.setImageResource(R.drawable.upload);

		textViewFilePath.setText(videoFileName);

		editTextVideoTitle.setText("");
		editTextVideoDescription.setText("");
		textViewVideoUrl.setText(getString(R.string.noUrlYet));

		Toast.makeText(this, R.string.pressVideoToPreview, Toast.LENGTH_LONG).show();
	}

	private void resetProgress() {
		progressBarUploadVideo.setProgress(0);
		textViewProgress.setText(" 00%");
	}


	private void fetchToken() {
		new FetchYouTubeTokenTask(this, selectedGoogleAccount, fetchTokenHandler).execute();
	}

	public void preventUploadingSameVideo() {
		imageButtonUploadVideo.setEnabled(false);
		imageButtonUploadVideo.setImageResource(R.drawable.upload_disabled);
	}

	/**
	 * Pick up a Google account from the device. See http://developer.android.com/google/auth/http-auth.html.
	 */
	public void chooseAccount()
	{
		String[] accountTypes = new String[]{"com.google"};
		Intent intent = AccountPicker.newChooseAccountIntent(null, null, accountTypes, false, null, null, null, null);
		startActivityForResult(intent, IntentRequestCode.REQUEST_ACCOUNT_PICKER);
	}

	/**
	 * Uploads user selected video to the user's YouTube account using OAuth2 for authentication.
	 */
	public void uploadYouTubeVideo() {
		youtubeUploadTask = new YouTubeUploadTask(this, videoFileName, getString(R.string.app_name), editTextVideoTitle.getText().toString(),
				editTextVideoDescription.getText().toString(), selectedGoogleAccount, uploadProgressHandler);
		youtubeUploadTask.execute();
	}

	public TextView getTextViewProgress() {
		return textViewProgress;
	}

	public ProgressBar getProgressBarUploadVideo() {
		return progressBarUploadVideo;
	}

	public TextView getTextViewVideoUrl() {
		return textViewVideoUrl;
	}

	public void setSelectedGoogleAccount(String account) {
		selectedGoogleAccount = account;
	}
}
