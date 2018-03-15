/*

  Copyright (c) 2004-10 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
 */

package com.hersan.protofoil;


import android.content.*;
import android.util.*;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import processing.core.PApplet;

public class TApplet extends PApplet {

	static final boolean DEBUG = false;

	/** Post events to the main thread that created the Activity */
	Handler handler;

	////////////////////////////////!!!!!!
	/* tgh: my version of onCreate.
	 * less window stuff set up here, leave that for main activity 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (DEBUG) println("onCreateT() happening here: " + Thread.currentThread().getName());

		Window window = getWindow();

		/*
		// Take up as much area as possible
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    	WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

    	// This does the actual full screen work
    	window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    	WindowManager.LayoutParams.FLAG_FULLSCREEN);
		 */

		// requestWindowFeature(Window.FEATURE_NO_TITLE); // ??
		// From : http://developer.android.com/reference/android/view/Window.html#requestFeature(int)
		//        You can not turn off a feature once it is requested.
		// Oh well. No title.

		window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
		window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		displayWidth = dm.widthPixels;
		displayHeight = dm.heightPixels;
		if (DEBUG) println("display metrics: " + dm);

		int sw = sketchWidth();
		int sh = sketchHeight();

		// tgh: don't need a big PGraphic
		//   short-circuit the sw and sh
		sw = sh = 1;

		if (sketchRenderer().equals(P2D)) {
			surfaceView = new SketchSurfaceView2D(this, sw, sh);
		} else if (sketchRenderer().equals(P3D)) {
			surfaceView = new SketchSurfaceView3D(this, sw, sh);
		}
		g = ((SketchSurfaceView) surfaceView).getGraphics();

		if (sw == displayWidth && sh == displayHeight) {
			// If using the full screen, don't embed inside other layouts
			window.setContentView(surfaceView);      
		} else {
			// If not using full screen, setup awkward view-inside-a-view so that
			// the sketch can be centered on screen. (If anyone has a more efficient
			// way to do this, please file an issue on Google Code, otherwise you
			// can keep your "talentless hack" comments to yourself. Ahem.)
			RelativeLayout overallLayout = new RelativeLayout(this);
			RelativeLayout.LayoutParams lp =
				new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
			lp.addRule(RelativeLayout.CENTER_IN_PARENT);

			LinearLayout layout = new LinearLayout(this);
			// tgh : again, don't allocate anything "big"
			//layout.addView(surfaceView, sketchWidth(), sketchHeight());
			layout.addView(surfaceView, sw, sh);
			overallLayout.addView(layout, lp);
			window.setContentView(overallLayout);
		}


		// code below here formerly from init()

		finished = false; // just for clarity

		// this will be cleared by draw() if it is not overridden
		looping = true;
		redraw = true;  // draw this guy once
		firstMotion = true;

		// these need to be inited before setup
		sizeMethods = new RegisteredMethods();
		preMethods = new RegisteredMethods();
		drawMethods = new RegisteredMethods();
		postMethods = new RegisteredMethods();
		mouseEventMethods = new RegisteredMethods();
		keyEventMethods = new RegisteredMethods();
		disposeMethods = new RegisteredMethods();

		Context context = getApplicationContext();
		sketchPath = context.getFilesDir().getAbsolutePath();

		handler = new Handler();
		start();
	}
	
}

