# YouTubeUploader - Android library for eazily uploading videos to YouTube

An Android library that helps easily create apps with the functionality of uploading videos to YouTube.

![Demo Screenshot 1](https://github.com/jayxue/YouTubeUploader/tree/master/YouTubeUploaderSDK/src/main/res/raw/screenshot_1.png)
![Demo Screenshot 2](https://github.com/jayxue/YouTubeUploader/tree/master/YouTubeUploaderSDK/src/main/res/raw/screenshot_2.png)
![Demo Screenshot 3](https://github.com/jayxue/YouTubeUploader/tree/master/YouTubeUploaderSDK/src/main/res/raw/screenshot_3.png)
![Demo Screenshot 4](https://github.com/jayxue/YouTubeUploader/tree/master/YouTubeUploaderSDK/src/main/res/raw/screenshot_4.png)

Details
-------
This Android library facilitates developers to create Android applications with the functionality of uploading videos to YouTube.

The major features include:
* Shoot new videos or pick videos from gallery for uploading.
* Select Google account to upload videos to.
* Enter title and description for a video to upload.
* Ask user to confirm before starting uploading.
* Show progress of uploading.
* Display video URL after uploading is successfully completed.

Usage
-----

In order to utilize this library, you just need to do some configurations without writing any code.
* Import the YouTubeUploadSDK module into your Android Studio project. Add dependency to the module to your app project.
* In your app's ```AndroidManifest.xml```, make sure that you have the following permissions:
  * ```android.permission.INTERNET```
  * ```android.permission.WRITE_EXTERNAL_STORAGE```
  * ```android.permission.CAMERA```
  * ```android.permission.GET_ACCOUNTS```
* In your app's ```AndroidManifest.xml```, include the activity:
  * ```com.wms.youtubeuploader.sdk.activity.UploadVideoActivity```
* In your app's ```AndroidManifest.xml```, include meta-data for Google GMS:
		<meta-data
			android:name="com.google.android.gms.version"
			android:value="@integer/google_play_services_version" />

* In your app's ```res/values/strings.xml```,
  * Set ```app_name``` (name of your application).
* Replace your app's ic_launcher icons.
* In order to upload videos to YouTube, you need to create a Google API project in Google Developers Console (https://console.developers.google.com). In the API project' Dashboard, select "Enable APIs and get credentials like keys".
  In "Overview" tab, make sure you enable the "YouTube Data API v3" API. Then, in "Credentials" tab, create two Android OAuth 2.0 client IDs, with meaningful names, one for dev version and one for release version. Enter package name and SHA1 signing-certificate fingerprint for them.
  You'll need to wait for 15 minutes or longer until the ID takes effect. See http://stackoverflow.com/questions/18022444/youtube-api-3-upload-video-access-not-configured-android.
  For loading videos, we just need a key for Android applications that allows any application (note that loading videos is not a functionality of this library).
 
Of course you can modify any components of the library or add new components to customize your app's functionality.

Acknowledgement
---------------

This library utilizes multiple Google libraries.

Developer
---------
* Jay Xue <yxue24@gmail.com>, Waterloo Mobile Studio

License
-------

    Copyright 2015 Waterloo Mobile Studio

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
