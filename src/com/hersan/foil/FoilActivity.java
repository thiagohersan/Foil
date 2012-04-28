package com.hersan.foil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Math;
import java.text.SimpleDateFormat;
import java.util.Calendar;
//import java.util.Iterator;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONObject;
import org.json.JSONArray;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import processing.core.PImage;

import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.StatusUpdate;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.DialogError;
import com.facebook.android.Util;

import com.hersan.protofoil.Message;
import com.hersan.protofoil.TApplet;


/* from Processing :
 * 		PImage : tmpImg
 * 			width + height+pixels
 *		PImage : toSave
 *			resize()
 *			width + height+pixels
 */

public class FoilActivity extends TApplet {

	///////////////
	// constants and variables
	////////////////

	// initial size of final image
	private static final int INITW = 600;
	private static final int INITH = 400;

	// dialog ids
	private static final int TEXTINPUTDIALOG = 0;
	private static final int SHAREDIALOG = 1;

	// some state variables
	//private boolean showingDialog = false;

	// text message. or leave blank for prompt
	private String theStringMessage;// = "Is this too long enough for you! Answer me please! thank you!";

	// surface stuff
	private ImageView imageSurface;
	private Button regenButton, saveButton, quitButton;
	private Button emailButton, tweetButton, faceButton;
	private Button backButton, shareButton;

	// private of privates
	private Message myMessage = null;
	private Bitmap toShow = null;
	private Uri imgUri = null;

	////////////////////
	//
	// activity flow methods
	//
	////////////////////

	/* for creating dialogs that are managed by the activity.  */
	@Override
	protected Dialog onCreateDialog(final int id) {
		if(id == TEXTINPUTDIALOG){
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			// build view group dynamically
			// from : http://developer.android.com/guide/topics/ui/dialogs.html#CustomDialog
			LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);

			final View vg = inflater.inflate(com.hersan.foil.R.layout.dialog,
					(ViewGroup) findViewById(com.hersan.foil.R.id.dialog_root));

			final EditText textInput = (EditText) vg.findViewById(com.hersan.foil.R.id.dialogtext);

			alert.setView(vg);

			// on positive button
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					theStringMessage = textInput.getText().toString();
					if(theStringMessage.equals("")){
						finish();
					}
					else {
						genImageFromText();
						showImageFromText();
						textInput.setText("");
					}
				}
			});
			// on negative button
			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					finish();
				}
			});

			return alert.create();
		}
		////////
		else if(id == SHAREDIALOG){
			System.out.println("!!! from SHAREDIALOG");
			final AlertDialog.Builder alert = new AlertDialog.Builder(this);

			// build view group dynamically
			// from : http://developer.android.com/guide/topics/ui/dialogs.html#CustomDialog
			final LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);

			final View vg = inflater.inflate(com.hersan.foil.R.layout.sharedialog,
					(ViewGroup) findViewById(com.hersan.foil.R.id.share_root));

			alert.setView(vg);

			emailButton = (Button) vg.findViewById(com.hersan.foil.R.id.emailbutton);
			tweetButton = (Button) vg.findViewById(com.hersan.foil.R.id.tweetbutton);
			faceButton = (Button) vg.findViewById(com.hersan.foil.R.id.facebutton);

			// email button : if image has been generated, email it.
			emailButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// save generated image to file
					if((myMessage != null) && (toShow != null)) sendEmail(); //saveImage(); //
					dismissDialog(SHAREDIALOG);
				}
			});

			// tweet button. Jesus...
			//  using thread to get smooth click look
			tweetButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					new Thread(new Runnable() {
						public void run(){
							// tested this on a network without internet
							//     (works when internet is present, and when there's no network)
							if(FoilActivity.checkConnectionTo("http://www.twitter.com") == true){
								postToTwitter();
							}
							else{ 
								runOnUiThread(new Runnable() {
									public void run() { 
										Toast.makeText(FoilActivity.this, "Couldn't connect to Twitter. Check internet connection", Toast.LENGTH_SHORT ).show();
									}
								});
							}
						}
					}).start();
					dismissDialog(SHAREDIALOG);
					Toast.makeText(FoilActivity.this, "Posting to Twitter....", Toast.LENGTH_SHORT ).show();
				}
			});

			// facebook button
			faceButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					new Thread(new Runnable() {
						public void run(){
							// tested this on a network without internet
							//     (works when internet is present, and when there's no network)
							if(FoilActivity.checkConnectionTo("http://www.facebook.com") == true){
								postToFacebook();
							}
							else{ 
								runOnUiThread(new Runnable() {
									public void run() { 
										Toast.makeText(FoilActivity.this, "Couldn't connect to Facebook. Check internet connection", Toast.LENGTH_SHORT ).show();
									}
								});
							}
						}
					}).start();
					dismissDialog(SHAREDIALOG);
					Toast.makeText(FoilActivity.this, "Posting on Facebook....", Toast.LENGTH_SHORT ).show();
				}
			});

			// done creating dialog
			return alert.create();
		}

		return null;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// DEBUG
		System.out.println("!!!!!!: Foil - onCreate");

		// house-keeping
		super.onCreate(savedInstanceState);

		// get text message from input activity
		theStringMessage = this.getIntent().getStringExtra("THE_STRING_MESSAGE");
	}

	/*
	 * Sets up all the buttons when app is first launched and also when we come back from twitter/facebook login
	 */
	private void onResumeCreateHelper(){
		// DEBUG
		System.out.println("!!!!!!: Foil - onResumeCreateHelper");

		setContentView(com.hersan.foil.R.layout.main);

		// set up surface to show image
		imageSurface = (ImageView) findViewById(com.hersan.foil.R.id.textimageview);

		// set up buttons!
		backButton = (Button) findViewById(com.hersan.foil.R.id.backbutton);
		shareButton = (Button) findViewById(com.hersan.foil.R.id.sharebutton);
		regenButton = (Button) findViewById(com.hersan.foil.R.id.regenbutton);
		saveButton = (Button) findViewById(com.hersan.foil.R.id.savebutton);
		quitButton = (Button) findViewById(com.hersan.foil.R.id.quitbutton);

		// quit button. Just Quit!
		backButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Set code for returning to input activity
				Intent myIntent = new Intent(FoilActivity.this, FoilActivityInput.class);
				myIntent.putExtra("THE_RESULT", FoilApplication.RESULT_BACK);
				FoilActivity.this.startActivity(myIntent);
				finish();
			}
		});

		// quit button. Just Quit!
		shareButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(SHAREDIALOG);
			}
		});

		// re-gen button : re-generate image
		regenButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// re-gen image from text in a thread
				// from : http://developer.android.com/resources/articles/painless-threading.html
				if((theStringMessage != null) && (!theStringMessage.equals(""))){
					// the thread to re-generate the image.
					new Thread(new Runnable() {
						public void run(){
							genImageFromText();
							// but can't update image view from thread
							imageSurface.post(new Runnable(){
								// imageSurface.setImageBitmap(toShow);
								public void run(){showImageFromText();}
							});
						}
					}).start();
					Toast.makeText(FoilActivity.this, "Regenerating Image", Toast.LENGTH_SHORT ).show();
				}
			}
		});

		// save button : if image has been generated, save it.
		saveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// save generated image to file
				if((myMessage != null) && (toShow != null)) saveImage(); //sendEmail();
			}
		});

		// quit button. Return to other activity
		quitButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Set quit code for returning to input activity
				Intent myIntent = new Intent(FoilActivity.this, FoilActivityInput.class);
				myIntent.putExtra("THE_RESULT", FoilApplication.RESULT_QUIT);
				FoilActivity.this.startActivity(myIntent);
				finish();
			}
		});

	}


	/* Called when the activity will start interacting with the user. 
	 * At this point your activity is at the top of the activity stack, 
	 * with user input going to it.  */
	@Override
	public void onResume() {
		// DEBUG
		System.out.println("!!!!!: Foil - onResume");

		super.onResume();

		// draws the buttons every time we resume
		this.onResumeCreateHelper();
		// if there is an image to show
		if(toShow != null){
			this.showImageFromText();
		}

		// if message is not set, prompt user for message
		//   this should never happen here...
		if((theStringMessage == null) || (theStringMessage.equals(""))) {
			// Set error code and return to input activity
			Intent myIntent = new Intent(FoilActivity.this, FoilActivityInput.class);
			myIntent.putExtra("THE_RESULT", FoilApplication.RESULT_ERROR);
			FoilActivity.this.startActivity(myIntent);
			finish();
		}

		// onResume can happen a few times. Only do stuff if the message or image is not set.
		//   This handles the case where message is set, but there's no image. 
		//   Should only happen during first time this activity runs with a new theStringMessage
		else if((myMessage == null) || (toShow == null)){			
			this.genImageFromText();
			this.showImageFromText();
		}

	}


	/* Instead of a new instance of the activity being started, 
	 * this is called on the existing instance with the Intent that was used to re-launch the activity.
	 * 
	 *   from : http://blog.blundell-apps.com/sending-a-tweet/
	 *   from : https://github.com/robhinds/AndroidTwitterDemo/
	 *   
	 *   Basically this gets the callback from the twitter webview, 
	 *      the sets the twitter variables for the application and calls postToTwitter() again
	 */
	@Override
	public void onNewIntent(Intent intent) {
		// DEBUG
		System.out.println("!!!!!: Foil - onNewIntent");

		Uri uri = intent.getData();

		// we got a BROWSABLE intent from a webview, with "callback://" scheme.
		// check to make sure it's the callback from the twitter server
		if((uri != null) && (uri.toString().startsWith(FoilApplication.TT_CALLBACK_URL))){
			String oauth_verifier = uri.getQueryParameter("oauth_verifier");
			// if we got a verifier back... keep logging in
			if(oauth_verifier != null){
				FoilApplication myApp = (FoilApplication)getApplication();
				try{
					// save the accessToken we just got back
					myApp.ttAccessToken = myApp.ttTwitter.getOAuthAccessToken(myApp.ttRequestToken, oauth_verifier);
					// set it on the current twitter
					myApp.ttTwitter.setOAuthAccessToken(myApp.ttAccessToken);
					// continue posting to twitter
					this.postToTwitter();
				}
				catch(TwitterException e){
					System.out.println("!!!: Error getting access token in onNewIntent");
				}
			}
		}
	}

	/* Called when an activity you launched exits.
	 * You will receive this call immediately before onResume() when your activity is re-starting.
	 * 
	 * Only used to set up the callback authorization from facebook instance
	 * 
	 */ 
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// DEBUG
		System.out.println("!!!!!: Foil - onActivityResult");

		super.onActivityResult(requestCode, resultCode, data);

		// set up facebook callbacks
		FoilApplication myApp = (FoilApplication)getApplication();
		myApp.fbFacebook.authorizeCallback(requestCode, resultCode, data);
	}

	/* Perform any final cleanup before an activity is destroyed. This gets called when someone 
	 * calls finish() on Activity. This method is usually implemented to free resources. 
	 */
	@Override
	public void onDestroy(){
		// DEBUG
		System.out.println("!!!!!: Foil - onDestroy !!!");
		super.onDestroy();
	}

	////////////////////////////
	//
	// own methods for generating text, saving image, etc
	//
	///////////////////////////


	/* generate the image from the text 
	 * only called when we have a valid theStringMessage (not null and not empty)
	 */
	private void genImageFromText() {
		// have valid string, create message
		System.out.println("!!!!! genImageFromText: "+theStringMessage);

		// Message constructor should be able to deal with null or empty string for thePicturePath
		myMessage = new Message(theStringMessage, this, INITW, INITH);
		// original sized image
		PImage tmpImg = myMessage.getImage();
		// not first time
		if(toShow != null){
			toShow.recycle();
			toShow = null;
			// every newly generated image has a new URI
			imgUri = null;
		}
		// create bitap from pixel array in PImage
		toShow = Bitmap.createBitmap(tmpImg.pixels, tmpImg.width, tmpImg.height, Bitmap.Config.ARGB_8888); 

		// DEBUG
		System.out.println("!!! toShow : "+tmpImg.width+" x "+tmpImg.height);

		// clean up?
		tmpImg.delete();
	}

	/* to show image */
	private void showImageFromText() {
		// display image
		imageSurface.setImageBitmap(toShow);
	}

	/* save an image using PImage.getImage() (or PImage.getBitmap()) and date and time... */
	private void saveImage(){
		boolean sawError = false;
		// grab an image from the message
		PImage toSave = myMessage.getImage();
		// resize
		toSave.resize(Math.min(400, toSave.width), 0);

		// DEBUG
		System.out.println("!!! toSave : "+toSave.width+" x "+toSave.height);

		// path to sdcard
		File imgDir  = new File(Environment.getExternalStorageDirectory()+File.separator+"Foil"+File.separator);
		File imgFile = null;

		// setting up date strings
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat sdfD = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat sdfT = new SimpleDateFormat("HHmmss");

		// get bitmap
		Bitmap bm = Bitmap.createBitmap(toSave.pixels, toSave.width, toSave.height, Bitmap.Config.ARGB_8888);
		// clean up
		toSave.delete();

		try {
			String date = new String(sdfD.format(calendar.getTime()));
			String time = new String(sdfT.format(calendar.getTime()));

			String fname = new String("Foil_"+date+"_"+time+".jpg");
			imgDir.mkdirs();
			imgFile = new File(imgDir,fname);

			// write into media folder
			ContentValues v = new ContentValues();
			long dateTaken = calendar.getTimeInMillis()/1000;
			v.put(MediaStore.Images.Media.TITLE, fname);
			v.put(MediaStore.Images.Media.PICASA_ID, fname);
			v.put(MediaStore.Images.Media.DISPLAY_NAME, "Foil");
			v.put(MediaStore.Images.Media.DESCRIPTION, "Foil");
			v.put(MediaStore.Images.Media.DATE_ADDED, dateTaken);
			v.put(MediaStore.Images.Media.DATE_TAKEN, dateTaken);
			v.put(MediaStore.Images.Media.DATE_MODIFIED, dateTaken);
			v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
			v.put(MediaStore.Images.Media.ORIENTATION, 0);
			v.put(MediaStore.Images.Media.DATA, imgFile.getAbsolutePath());

			Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
			OutputStream outStream = getContentResolver().openOutputStream(uri);

			bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
			outStream.flush();
			outStream.close();
			bm.recycle();

			// cache uri
			imgUri = uri;
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			sawError = true;
		}
		catch (IOException e) {
			e.printStackTrace();
			sawError = true;
		}

		// some feedback on saving files
		if(sawError){
			Toast.makeText(this, "Error While Saving Image", Toast.LENGTH_SHORT ).show();
		}
		else{
			Toast.makeText(this, "Saved Image", Toast.LENGTH_SHORT ).show();
		}
	} // saveImage()


	/* deal with Twitter:
	 *   from : http://blog.blundell-apps.com/sending-a-tweet/
	 *   from : https://github.com/robhinds/AndroidTwitterDemo/
	 *   
	 *   This is used in a 2-pass method :
	 *     First time this is called it sets up twitter authorization
	 *     Subsequent calls just send info/data to twitter
	 */
	private void postToTwitter(){
		FoilApplication myApp = (FoilApplication)getApplication();

		// if not authorized, get authorization and access token...
		if((myApp.ttTwitter == null) || (myApp.ttAccessToken == null) || (myApp.ttRequestToken == null)){
			// set a new twitter
			myApp.ttTwitter = new TwitterFactory().getInstance();
			// tell it which app we want
			myApp.ttTwitter.setOAuthConsumer(FoilApplication.TT_CONSUMER_KEY, FoilApplication.TT_CONSUMER_SECRET);
			try{
				// request a token
				myApp.ttRequestToken = myApp.ttTwitter.getOAuthRequestToken(FoilApplication.TT_CALLBACK_URL);
				runOnUiThread(new Runnable() {
					public void run() { 
						// Open Web Site
						FoilApplication localMyApp = (FoilApplication)getApplication();
						final WebView twitterSite = new WebView(FoilActivity.this);
						twitterSite.loadUrl(localMyApp.ttRequestToken.getAuthenticationURL());
						twitterSite.requestFocus();
						setContentView(twitterSite); }
				});
				//setContentView(twitterSite);
			}
			catch(TwitterException e){
				System.out.println("!!!!! somethings wrong in postToTwitter"); 
				e.printStackTrace();
			}

		}
		// if authorized and string/image is valid... post a tweet
		else if((theStringMessage != null) && (toShow != null)){
			try{
				// idea from : http://www.mokasocial.com/2011/07/writing-an-android-twitter-client-with-image-upload-using-twitter4j/
				// but learned about statusUpdate class from reading code : 
				// https://github.com/yusuke/twitter4j/blob/master/twitter4j-media-support/src/main/java/twitter4j/media/TwitterUpload.java

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				toShow.compress(Bitmap.CompressFormat.JPEG, 100, baos);
				//myApp.ttTwitter.updateStatus(theStringMessage);
				myApp.ttTwitter.updateStatus(new StatusUpdate("").media("",new ByteArrayInputStream(baos.toByteArray())));
				baos.close();
				runOnUiThread(new Runnable() {
					public void run() { 
						Toast.makeText(FoilActivity.this, "Image Posted on Twitter", Toast.LENGTH_SHORT ).show();
					}
				});
			}
			catch(TwitterException e){
				System.out.println("!!! Something wrong with Twitter updateStatus: Twitter Exception");
			}
			catch(Exception e){
				System.out.println("!!! Something wrong with Twitter updateStatus.");
			}
		}
	}

	/* deal with Facebook:
	 *   get access token if it doesn't exist, then post to wall
	 *   from : https://developers.facebook.com/docs/mobile/android/build/
	 *   
	 *   Similar to postToTwitter: Logs in and sets up the facebook instance when it's first called
	 *                             then, just posts to facebook on subsequent calls
	 *     
	 */
	private void postToFacebook(){
		// DEBUG
		System.out.println("!!!!!!: postToFacebook");

		final FoilApplication myApp = (FoilApplication)getApplication();

		// make sure we have a facebook instance
		if(myApp.fbFacebook == null){
			myApp.fbFacebook = new Facebook(FoilApplication.FB_APP_ID);
			myApp.fbAsyncRunner = new AsyncFacebookRunner(myApp.fbFacebook);
		}

		// ??
		//myApp.fbFacebook.extendAccessTokenIfNeeded(this, null);

		// if no accessToken or expiration were ever set, or if the session has expired, get authorization
		if((myApp.fbAccessToken == null) || (myApp.fbExpires == -1) || (myApp.fbFacebook.isSessionValid() == false)){
			// from : https://developers.facebook.com/docs/mobile/android/build/#perms
			// from : https://developers.facebook.com/docs/reference/api/permissions

			// either uses the installed facebook app to do the login
			//   or creates and displays a webview for logging in (and hopefully deal with everything internally)
			myApp.fbFacebook.authorize(this, new String[] { "publish_stream", "photo_upload", "user_photos" }, new DialogListener() {
				@Override
				public void onComplete(Bundle values) {
					// this runs independent of SSO and onActivityResult().
					// Should work for both SSO and WebView style of log in

					// DEBUG
					System.out.println("!!!!!: fb.authorize.onCreate() done!");

					// set the application level facebook variables here, and call postToFacebook again.
					myApp.fbAccessToken = myApp.fbFacebook.getAccessToken();
					myApp.fbExpires = myApp.fbFacebook.getAccessExpires();

					FoilActivity.this.postToFacebook();
				}
				@Override
				public void onFacebookError(FacebookError e) {
					System.out.println("!!!!!: fb.authorize.onFaceError");
					e.printStackTrace();
				}
				@Override
				public void onError(DialogError e) {
					System.out.println("!!!!!: fb.authorize.onError");
				}
				@Override
				public void onCancel() {
					System.out.println("!!!!!: fb.authorize.onCancel");
				}
			});

		}
		// if authorized and string/image is valid... post to wall...
		else if((theStringMessage != null) && (toShow != null)){
			// from : https://github.com/facebook/facebook-android-sdk/blob/master/examples/Hackbook/src/com/facebook/android/Hackbook.java
			// from : http://stackoverflow.com/questions/5788436/android-photo-upload-to-facebook-using-graph-api
			// from : http://stackoverflow.com/questions/8662460/android-app-how-to-upload-photo-from-sdcard-to-facebook-wall

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			toShow.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			final byte[] bArr = baos.toByteArray();
			try { baos.close(); }
			catch(Exception e) { System.out.println("!!!! Something wrong when closing the bytearray in postToFacebook"); }
			//Bundle params = new Bundle();
			//params.putByteArray("photo", bArr);
			//params.putString("message", "");
			//params.putString("message", theStringMessage);

			// get all albums
			myApp.fbAsyncRunner.request("me/albums", new Bundle(), "GET", new BaseRequestListener(){
				@Override
				public void onComplete(final String response, final Object state) { 
					System.out.println("!!! response from album GET: "+response);
					try{
						// get data object
						JSONObject jObj = Util.parseJson(response);
						JSONArray dataArr = jObj.getJSONArray("data");
						// look for wall album 
						for(int i=0; i<dataArr.length(); i++){
							jObj = dataArr.getJSONObject(i);
							Bundle byteBundle = new Bundle();
							byteBundle.putByteArray("photo", bArr);
							// my wall
							if(jObj.getString("type").equalsIgnoreCase("wall")){
								System.out.println("!!! found my wall");
								String wallID = jObj.getString("id");
								// add to my wall
								myApp.fbAsyncRunner.request(wallID+"/photos", byteBundle, "POST", new BaseRequestListener(), null);
								//break;
							}
							// secret wall album for posting to friends' wall
							else if((jObj.getString("type").equalsIgnoreCase("friends_walls"))&&(false)){
								// TODO: friend picker dialog
								System.out.println("!!! found friends_walls");
								String wallID = jObj.getString("id");
								Bundle friendBundle = new Bundle();
								friendBundle.putByteArray("photo", bArr);
								// hard coded Bana Na Gringuita 's ID
								friendBundle.putString("target_id", "1568649239");

								// add to friend wall
								myApp.fbAsyncRunner.request(wallID+"/photos", friendBundle, "POST", new BaseRequestListener(){
									// Turning this off.
									// Use onComplete here to POST a Post request to a friend's wall
									//@Override
									public void onComplete__(final String response, final Object state) { 
										try {
											JSONObject jObj = Util.parseJson(response);
											final String myPhotoID = jObj.getString("id");
											myApp.fbAsyncRunner.request(myPhotoID, new Bundle(), "GET", new BaseRequestListener(){
												@Override
												public void onComplete(final String response, final Object state) { 
													try{
														System.out.println("!!! from photo GET: "+myPhotoID);
														JSONObject jObj = Util.parseJson(response);
														final String myPhotoLink = jObj.getString("link");
														FoilActivity.this.postToFriendsFacebook(myPhotoID);
													}
													catch(Exception e){}
												}
											}, null);
										}
										catch(Exception e){}
									}
								}, null);								
							}
						}
					}
					catch(Exception e){	}
				}
			},null);


			////////////////////////////////////////////////////////////////////////
			//
			/////////////////////////

			runOnUiThread(new Runnable() {
				public void run() { 
					Toast.makeText(FoilActivity.this, "Image Posted on Facebook", Toast.LENGTH_SHORT ).show();
				}
			});
		}
	}

	private void postToFriendsFacebook(final String photoID){
		final FoilApplication myApp = (FoilApplication)getApplication();
		myApp.fbAsyncRunner.request("me/friends", new Bundle(), "GET", new BaseRequestListener(){
			@Override
			public void onComplete(final String response, final Object state) { 
				System.out.println("!!! response from friends GET: "+response);
				try{
					// get data object
					JSONObject jObj = Util.parseJson(response);
					JSONArray dataArr = jObj.getJSONArray("data");
					// look for wall album
					for(int i=0; i<dataArr.length(); i++){
						jObj = dataArr.getJSONObject(i);
						if((jObj.getString("name").contains("ringuita")) && (jObj.getString("name").contains("Bana"))){
							System.out.println("!!!!! found Banana!");
							String friendID = jObj.getString("id");
							Bundle postBundle = new Bundle();
							//postBundle.putString("link", photoLink);
							//postBundle.putString("message", "...");
							//postBundle.putString("name", "");
							//postBundle.putString("caption", " ");
							if(photoID.equals("") == false){
								System.out.println("!!! has ID ");
								postBundle.putString("object_attachment", photoID);
							}
							// add to wall
							myApp.fbAsyncRunner.request(friendID+"/feed", postBundle, "POST", new BaseRequestListener(){
								@Override
								public void onComplete(final String response, final Object state){
									System.out.println("!!! response from friend POST: "+response);
								}
							}, null);
							break;
						}
					}
				}
				catch(Exception e){	}
			}
		},null);

	}

	private void sendEmail(){
		// should save image before sending it
		if(imgUri == null){
			this.saveImage();
		}
		// should be set now
		if(imgUri == null){
			// something went wrong
			runOnUiThread(new Runnable() {
				public void run() { 
					Toast.makeText(FoilActivity.this, "Couldn't save image", Toast.LENGTH_SHORT ).show();
				}
			});
			return;
		}
		// otherwise, prepare intent
		// from : http://mobile.tutsplus.com/tutorials/android/android-email-intent/
		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		emailIntent.setType("image/jpeg");
		emailIntent.putExtra(android.content.Intent.EXTRA_STREAM, imgUri);
		startActivity(Intent.createChooser(emailIntent, "Choose your email program: "));
	}

	/*
	 * Check connectivity to a given server.
	 * Mostly to see if we can access twitter/facebook
	 * 
	 * from : http://stackoverflow.com/questions/4723964/json-fetch-url-content-for-android/4724491#4724491
	 * maybe : http://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-timeouts
	 */
	static private boolean checkConnectionTo(String uri){
		try{
			// whoa. 1337.
			//   get a new client to execute a get request with the uri
			// TODO : test request timeout

			final HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is established. In millis
			HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
			// Set the default socket timeout (SO_TIMEOUT) in millis
			HttpConnectionParams.setSoTimeout(httpParameters, 3000);

			final HttpResponse myResponse = (new DefaultHttpClient(httpParameters)).execute(new HttpGet(uri));
			if(myResponse.getEntity() == null){
				return false;
			}
		}
		catch(Exception e){
			return false;
		}

		return true;
	}
}
