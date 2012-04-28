package com.hersan.foil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.facebook.android.AsyncFacebookRunner;

public class FoilActivityInput extends Activity {

	///////////////
	// constants and variables
	////////////////
	
	// text message. or leave blank for prompt
	private String theStringMessage;// = "Is this too long enough for you! Answer me please! thank you!";

	// surface stuff
	private EditText textInput;
	private Button nextButton, quitButton;

	////////////////////
	//
	// activity flow methods
	//
	////////////////////

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// DEBUG
		System.out.println("!!!!!!: Input - onCreate");

		// house-keeping
		super.onCreate(savedInstanceState);
		
		// create buttons, etc, set content view
		this.onResumeCreateHelper();
	}

	/*
	 * Sets up all the buttons when app is first launched and also when we come back from twitter/facebook login
	 */
	private void onResumeCreateHelper(){
		// DEBUG
		System.out.println("!!!!!!: Input - onResumeCreateHelper");
		
		// this should be in an xml, but this way it's consistent with the main FoilActivity
	    // Take up as much area as possible
	    requestWindowFeature(Window.FEATURE_NO_TITLE);


		setContentView(com.hersan.foil.R.layout.input);

		// text input
		textInput = (EditText) findViewById(com.hersan.foil.R.id.inputdialogtext);
		
		// set up buttons!
		quitButton = (Button) findViewById(com.hersan.foil.R.id.inputquitbutton);
		nextButton = (Button) findViewById(com.hersan.foil.R.id.nextbutton);

		// next button : generate image
		nextButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				theStringMessage = textInput.getText().toString();
				if(!theStringMessage.equals("")){
					Intent myIntent = new Intent(FoilActivityInput.this, FoilActivity.class);
					myIntent.putExtra("THE_STRING_MESSAGE", theStringMessage);
					FoilActivityInput.this.startActivity(myIntent);
				}
			}
		});

		// quit button. Just Quit!
		quitButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
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
		System.out.println("!!!!!: Input - onResume");

		super.onResume();

		// draws the buttons every time we resume
		//this.onResumeCreateHelper();
	}

	/* Instead of a new instance of the activity being started, 
	 * this is called on the existing instance with the Intent that was used to re-launch the activity.
	 */
	@Override
	public void onNewIntent(Intent intent) {
		// Check result. If quitting, call quit
		if(intent.getIntExtra("THE_RESULT", 0) == FoilApplication.RESULT_ERROR){
			// error in the Foil activity, quit
			System.out.println("!!!!!: Input - onNewIntent = ERROR");
			finish();
		}
		else if(intent.getIntExtra("THE_RESULT", 0) == FoilApplication.RESULT_QUIT){
			// quitting the Foil activity, quit
			System.out.println("!!!!!: Input - onNewIntent = QUIT");
			finish();
		}
		else if(intent.getIntExtra("THE_RESULT", 0) == FoilApplication.RESULT_BACK){
			// just getting back..... do nothing.
			System.out.println("!!!!!: Input - onNewIntent = BACK");
		}
	}

	/* Perform any final cleanup before an activity is destroyed. This gets called when someone 
	 * calls finish() on Activity. This method is usually implemented to free resources. 
	 */
	@Override
	public void onDestroy(){
		// DEBUG
		System.out.println("!!!!!: Input - onDestroy !!!");
		super.onDestroy();

		FoilApplication myApp = (FoilApplication)getApplication();

		// logout of twitter
		if(myApp.ttTwitter != null){
			// destroy twitter : I can't find a "logout" method -> manual logout
			myApp.ttTwitter = null;
			myApp.ttAccessToken = null;
			myApp.ttRequestToken = null;
		}
		// logout of facebook
		if(myApp.fbFacebook != null){
			// if no async facebook, make a new instance
			if(myApp.fbAsyncRunner == null){
				myApp.fbAsyncRunner = new AsyncFacebookRunner(myApp.fbFacebook);
			}
			// should have an async runner here.
			myApp.fbAsyncRunner.logout(this, new BaseRequestListener());
			myApp.fbAsyncRunner = null;
			myApp.fbFacebook = null;
			myApp.fbAccessToken = null;
			myApp.fbExpires = -1;
		}
	}
}
