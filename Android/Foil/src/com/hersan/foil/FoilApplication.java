package com.hersan.foil;

import android.app.Application;

import twitter4j.Twitter;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import com.facebook.android.Facebook;
import com.facebook.android.AsyncFacebookRunner;


public class FoilApplication extends Application{

	// from twitter app
	protected static final String TT_CONSUMER_KEY = "xeZuSvcmEIE5XtlJgPtFg";
	protected static final String TT_CONSUMER_SECRET = "LJekmTiql6wJcb8kswmSKpjlkHZ7gHkUEqWizrKmKM";
	protected static final String TT_CALLBACK_URL = "callback://foil-twitter/";

	// for twitter access
	// these should be set once and kept throughout applications life-cycle...
	protected Twitter ttTwitter = null;
	protected AccessToken ttAccessToken = null;
	protected RequestToken ttRequestToken = null;

	// from facebook app
	//protected static final String FB_APP_ID = "370668442971954";
	protected static final String FB_APP_ID = "224988764274012";

	// for facebook access
    protected Facebook fbFacebook = null;
   	protected AsyncFacebookRunner fbAsyncRunner = null;
	protected String fbAccessToken = null;
	protected long fbExpires = -1;
	
	// for communicating between activities
	protected static final int RESULT_ERROR = 234;
	protected static final int RESULT_QUIT = 456;
	protected static final int RESULT_BACK = 678;
}
