package com.wms.youtubeuploader.sdk.task;

import android.app.Activity;
import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Videos.Insert;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.common.collect.Lists;
import com.wms.youtubeuploader.sdk.handler.HandlerMessage;
import com.wms.youtubeuploader.sdk.handler.UploadProgressHandler;
import com.wms.youtubeuploader.sdk.util.DialogUtil;
import com.wms.youtubeuploader.sdk.util.MessageUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class YouTubeUploadTask extends AsyncTask<Void, Void, Void> {

	private static final String VIDEO_FILE_FORMAT = "video/*";

	public static final String scope = "oauth2:https://www.googleapis.com/auth/youtube";
    private static final List<String> scopes = Lists.newArrayList(YouTubeScopes.YOUTUBE);

	private Activity activity = null;
	private String videoFileName = null;
	private String appName = "";
	private String title = "";
	private String description = "";
	private String selectedGoogleAccount = "";
	private UploadProgressHandler uploadProgressHandler = null;

	private HttpTransport transport = AndroidHttp.newCompatibleTransport();
	private JsonFactory jsonFactory = new GsonFactory();
	
	private MediaHttpUploader uploader = null;

	private Video youtubeVideo = null;

	private String errorMessage = null;

	public YouTubeUploadTask(Activity activity, String videoFileName, String appName, String title, String description, String selectedGoogleAccount, UploadProgressHandler uploadProgressHandler) {
		this.activity = activity;
		this.videoFileName = videoFileName;
		this.appName = appName;
		this.title = title;
		this.description = description;
		this.selectedGoogleAccount = selectedGoogleAccount;
		this.uploadProgressHandler = uploadProgressHandler;
		this.uploadProgressHandler.setTask(this);
	}
	
    /**
     * Prepare upload. Just leaves execute to be run in AsyncTask.
     *
     * @return video insert
     */
    public Insert prepareUpload() {
        try {
        	File videoFile = new File(videoFileName);
        	
            // Add extra information to the video before uploading
            Video videoObjectDefiningMetadata = new Video();

            // Set the video to public (default)
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus("public");
            videoObjectDefiningMetadata.setStatus(status);

            // Set metadata with the VideoSnippet object
            VideoSnippet snippet = new VideoSnippet();

            // Video title and description
            snippet.setTitle(title);
            snippet.setDescription(description);

            // Set keywords
            List<String> tags = new ArrayList<String>();
            tags.add(appName);
            snippet.setTags(tags);

            // Set completed snippet to the video object
            videoObjectDefiningMetadata.setSnippet(snippet);

            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT, new BufferedInputStream(new FileInputStream(videoFile)));
            mediaContent.setLength(videoFile.length());

	        GoogleAccountCredential credential = buildGoogleAccountCredential();
	        YouTube youtube = new YouTube.Builder(transport, jsonFactory, credential).setApplicationName(appName).build();

            Insert videoInsert = youtube.videos().insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);

            uploader = videoInsert.getMediaHttpUploader();

            /*
             * Sets whether direct media upload is enabled or disabled. True = whole media content is
             * uploaded in a single request. False (default) = resumable media upload protocol to upload
             * in data chunks.
             */
            uploader.setDirectUploadEnabled(false);
	        MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            MessageUtil.sendHandlerMessage(uploadProgressHandler, HandlerMessage.VIDEO_UPLOAD_INITIATION_STARTED);
                            break;
                        case INITIATION_COMPLETE:
                            break;
                        case MEDIA_IN_PROGRESS:
                            MessageUtil.sendHandlerMessage(uploadProgressHandler, HandlerMessage.VIDEO_UPLOAD_PROGRESS_UPDATE);
                            break;
                        case MEDIA_COMPLETE:
	                        MessageUtil.sendHandlerMessage(uploadProgressHandler, HandlerMessage.VIDEO_UPLOAD_PROGRESS_UPDATE);
                            break;
                        case NOT_STARTED:
                            break;
                    }
                }
            };
            uploader.setProgressListener(progressListener);
            // Set chunk size. See http://stackoverflow.com/questions/13580109/check-progress-for-upload-download-google-drive-api-for-android-or-java
            uploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE * 2);

            return videoInsert;
        } 
        catch (FileNotFoundException e) {
            MessageUtil.sendHandlerMessage(uploadProgressHandler, HandlerMessage.VIDEO_UPLOAD_FAILED);
	        errorMessage = "File Not Found: " + e.getMessage();
            return null;
        } 
        catch (IOException e) {
	        MessageUtil.sendHandlerMessage(uploadProgressHandler, HandlerMessage.VIDEO_UPLOAD_FAILED);
	        errorMessage = "IOException: " + e.getMessage();
            return null;
        } 
        catch (Exception e) {
	        MessageUtil.sendHandlerMessage(uploadProgressHandler, HandlerMessage.VIDEO_UPLOAD_FAILED);
        	errorMessage = e.getMessage();
            return null;
		}
    }
    
    /**
     * Build the credential to authorize the installed application to access user's protected data.
     */
    private GoogleAccountCredential buildGoogleAccountCredential() throws Exception {
	    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(activity, scopes);
        credential.setBackOff(new ExponentialBackOff());
        credential.setSelectedAccountName(selectedGoogleAccount);
        return credential;
    }

	@Override
	protected Void doInBackground(Void... params) {

		Insert videoInsert = prepareUpload();
		if(videoInsert != null) {
			try {
				youtubeVideo = videoInsert.execute();
			}
			catch (IOException e) {
				MessageUtil.sendHandlerMessage(uploadProgressHandler, HandlerMessage.VIDEO_UPLOAD_FAILED);
			}
		}
		return null;
		
	}

    @Override
    protected void onPostExecute(Void result) {
	    if(errorMessage != null) {
		    DialogUtil.showExceptionAlertDialog(activity, "Exception", errorMessage);
	    }
	    else if(youtubeVideo != null) {
		    MessageUtil.sendHandlerMessage(uploadProgressHandler, HandlerMessage.VIDEO_UPLOAD_COMPLETED);
	    }
    }

    public Video getUploadedVideo() {
    	return youtubeVideo;
    }
    
    public MediaHttpUploader getUploader() {
    	return uploader;
    }
    
}
