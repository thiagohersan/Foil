package com.hersan.protofoil;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import processing.core.PImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Math;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/* from Processing :
 * 		PImage : tmpImg
 * 			width + height+pixels
 *		PImage : toSave
 *			resize()
 *			width + height+pixels
 */

public class ProtoFoilActivity extends TApplet {

	///////////////
	// constants and variables
	////////////////

	// initial size of final image
	private static final int INITW = 600;
	private static final int INITH = 400;

	// dialog ids
	private static final int TEXTINPUTDIALOG = 0;

	// some state variables
	private boolean showingDialog = false;

	// text message. or leave blank for prompt
	private String theStringMessage;// = "Is this too long enough for you! Answer me please! thank you!";
	// an image path
	private String thePicturePath = "";

	// surface stuff
	private ImageView imageSurface;
	private Button saveButton, regenButton, quitButton;

	// private of privates
	private Message myMessage = null;
	private Bitmap toShow = null;

	////////////////////
	// activity flow
	////////////////////

	/* for creating dialogs that are managed by the activity.  */
	@Override
	protected Dialog onCreateDialog(final int id) {
		if(id == TEXTINPUTDIALOG){
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			// build view group dynamically
			// from : http://developer.android.com/guide/topics/ui/dialogs.html#CustomDialog
			LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);

			final View vg = inflater.inflate(com.hersan.protofoil.R.layout.dialog,
					(ViewGroup) findViewById(com.hersan.protofoil.R.id.dialog_root));

			final EditText textInput = (EditText) vg.findViewById(com.hersan.protofoil.R.id.dialogtext);

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
		return null;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// house-keeping
		super.onCreate(savedInstanceState);
		setContentView(com.hersan.protofoil.R.layout.main);
		final Context c = this;

		// set up surface to show image
		imageSurface = (ImageView) findViewById(com.hersan.protofoil.R.id.textimageview);

		// set up buttons!
		saveButton = (Button) findViewById(com.hersan.protofoil.R.id.savebutton);
		regenButton = (Button) findViewById(com.hersan.protofoil.R.id.regenbutton);
		quitButton = (Button) findViewById(com.hersan.protofoil.R.id.quitbutton);

		// save button : if image has been generated, save it.
		saveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// save generated image to file
				if((myMessage != null) && (toShow != null)) saveImage();
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
					Toast.makeText(c, "Regenerating Image", Toast.LENGTH_SHORT ).show();
					//genImageFromText();
				}
			}
		});

		// quit button. Just Quit!
		quitButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		System.out.println("!!!!!!: onCreate");
	}

	/* Called when the activity will start interacting with the user. 
	 * At this point your activity is at the top of the activity stack, 
	 * with user input going to it.  */
	@Override
	public void onResume() {
		super.onResume();
		// if message not set above, prompt user for message
		if((theStringMessage == null) || (theStringMessage.equals(""))) {
			showingDialog = true;
			showDialog(TEXTINPUTDIALOG);
		}
		// onResume can happen a few times. Only do stuff if the message/image is not set
		//   this is mostly for debugging, when the message is set up statically above
		else if((myMessage == null) || (toShow == null)){			
			genImageFromText();
		}

		// DEBUG
		System.out.println("!!!!!: onResume");

	}

	/* Called by the system when the device configuration changes while your activity is running. */
	@Override
	public void onConfigurationChanged (Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	////////////////////////////
	// own methods for generating text, saving image, etc
	///////////////////////////

	/* generate the image from the text 
	 * only called when we have a valid theStringMessage (not null and not empty) */
	private void genImageFromText() {
		// have valid string, create message
		System.out.println("!!!!!: "+theStringMessage);
		// turn off dialog 
		if(showingDialog == true){
			dismissDialog(TEXTINPUTDIALOG);
			showingDialog = false;
		}

		// Message constructor should be able to deal with null or empty string for thePicturePath
		myMessage = new Message(theStringMessage, thePicturePath, this, INITW, INITH);
		// original sized image
		PImage tmpImg = myMessage.getImage();
		// not first time
		if(toShow != null){
			toShow.recycle();
			toShow = null;
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


}
