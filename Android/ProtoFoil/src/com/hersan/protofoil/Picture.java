package com.hersan.protofoil;

import processing.core.PApplet;
import processing.core.PImage;


/* from Processing :
 *		PImage : tpi
 *			resize()
 *			width + height
 *		PImage : uImage
 *			createImage()
 */

public class Picture extends Word {

  private String fileName;

  public Picture(String fn, PApplet pa) {
    fileName = fn;
    myProcessing = pa;
    this.createUImage();
    this.createDImage();
  }

  private void createUImage() {
    PImage tpi = myProcessing.loadImage(fileName);
    tpi.resize(0,150);
    // temporary sizing
    uImage = myProcessing.createImage(1, 1, PApplet.ARGB);
    // crop+copy
    Picture.cropImage(tpi, uImage);
    //clean up
    tpi.delete();
  }
}

