package com.hersan.foil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.FacebookError;


public class BaseRequestListener implements RequestListener {

	public void onComplete(final String response, final Object state) { }

	public void onFacebookError(FacebookError e, final Object state) { }

    public void onFileNotFoundException(FileNotFoundException e, final Object state) { }

    public void onIOException(IOException e, final Object state) { }

    public void onMalformedURLException(MalformedURLException e, final Object state) { }

}