package com.hersan.foil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Math;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

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
		System.out.println("!!!!!!: onCreate");

		// house-keeping
		super.onCreate(savedInstanceState);
	}

	/*
	 * Sets up all the buttons when app is first launched and also when we come back from twitter/facebook login
	 */
	private void onResumeCreateHelper(){
		// DEBUG
		System.out.println("!!!!!!: onResumeCreateHelper");

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
				if(theStringMessage.equals("")){
					finish();
				}
				else {
					// TODO : start new activity here
					//
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
		System.out.println("!!!!!: onResume");

		super.onResume();

		// draws the buttons every time we resume
		this.onResumeCreateHelper();
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
		System.out.println("!!!!!: onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
	}

	/* Perform any final cleanup before an activity is destroyed. This gets called when someone 
	 * calls finish() on Activity. This method is usually implemented to free resources. 
	 */
	@Override
	public void onDestroy(){
		// DEBUG
		System.out.println("!!!!!: onDestroy!!!");

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
