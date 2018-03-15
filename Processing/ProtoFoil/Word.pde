public class Word {

  //final private static String WHICHFONT = "Courier";
  final private static int FONTSIZE = 72;
  private String theCourierFont = null;

  private String theWord;
  protected PImage uImage, dImage;

  private void setFont() {
    // get the Courier font that has no Bold or Italic
    int minFontNameLen = 1024;
    for (int i=0; i<PFont.list().length; i++) {
      /*
      if ((PFont.list()[i].contains("ourier") == true)&&(PFont.list()[i].matches(".*(([bB]old)|([iI]t[aá]lic)|([nN]egrito)).*") == false)) {
       println(PFont.list()[i]);
       theCourierFont = PFont.list()[i];
       }
       */
      if ((PFont.list()[i].contains("ourier") == true)&&(PFont.list()[i].length() < minFontNameLen)) {
        theCourierFont = PFont.list()[i];
        minFontNameLen = PFont.list()[i].length();
      }
    }
    theCourierFont = (theCourierFont == null)?(PFont.list()[0]):(theCourierFont);
  }

  public Word(String w) {
    theWord = w;
    this.setFont();
    this.createUImage();
    this.createDImage();
  }

  public Word() {
    this("foock!");
  }

  public PImage getUImage() {
    return uImage;
  }

  public PImage getDImage() {
    return dImage;
  }

  // create the undistorted image.
  //   get word, write it to an graphic, measure word, add line, crop.
  private void createUImage() {
    PGraphics pg = createGraphics(theWord.length()*FONTSIZE, FONTSIZE+20, JAVA2D);
    PFont theFont = createFont(theCourierFont, FONTSIZE);

    // write word on graphic
    pg.beginDraw();
    pg.background(255);
    pg.smooth();
    pg.fill(0);
    pg.textFont(theFont);
    pg.text(theWord, 0, 0, pg.width, pg.height);
    pg.endDraw();

    // get image from graphic
    PImage tpi = pg.get();
    // temporary sizing
    uImage = createImage(1, 1, ARGB);
    // crop+copy
    this.cropImage(tpi, uImage);

    // clear old drawing,
    // draw some random lines through the cropped image
    pg.beginDraw();
    pg.background(255);
    pg.image(uImage, 0, 0);
    pg.stroke(0);
    pg.strokeWeight(3);
    pg.noFill();
    pg.line(random(5, uImage.width/5f), random(5, uImage.height-5), random(4f*uImage.width/5f, uImage.width-5), random(5, uImage.height-5));
    pg.endDraw();

    // copy final version with line
    tpi = pg.get();
    uImage.copy(tpi, 0, 0, uImage.width, uImage.height, 0, 0, uImage.width, uImage.height);

    // clean up
    pg.dispose();
  }

  // given src and dst images,
  //    find the boundaries on src,
  //    resize dst,
  //    copy the right pixels from src to dst
  protected void cropImage(PImage src, PImage dst) {

    // find boundaries
    int minX = 1024;
    int maxX = 0;
    int minY = 1024;
    int maxY = 0;

    src.loadPixels();
    // get pixel boundaries
    for (int i=0; i<src.height; i++) {
      for (int j=0; j<src.width; j++) {
        color c = src.pixels[i*src.width+j];
        // find non-white pixels
        if ((red(c) < 250) && (green(c) < 250) && (blue(c) < 250)) {
          if (i<minY) minY = i;
          if (i>maxY) maxY = i;
          if (j<minX) minX = j;
          if (j>maxX) maxX = j;
        }
      }
    }

    // copy it onto dst
    dst.resize(maxX-minX+10, maxY-minY+10);
    dst.copy(src, minX-5, minY-5, maxX-minX+10, maxY-minY+10, 0, 0, dst.width, dst.height);
  }

  // create the distorted image
  protected void createDImage() {
    // temp image to help with the distortions
    //  both tpi and dImage are sized according to uImage
    PImage tp0 = createImage(uImage.width, uImage.height, ARGB);
    PImage tp1 = createImage(uImage.width, uImage.height, ARGB);
    // temporary size
    dImage = createImage(uImage.width, uImage.height, ARGB);

    //tp0.copy(uImage, 0, 0, uImage.width, uImage.height, 0, 0, uImage.width, uImage.height);

    // barrel eye
    //this.fisheye(uImage, tp, uImage.width/3, uImage.height/2, 0.001);

    // horizontal shear
    this.horizontalShear(uImage, tp0);

    // keystone
    this.keyTransform(tp0, tp1);

    // wave shear
    this.waveShear(tp1, tp0);

    // final blur
    tp0.filter(BLUR, 1.6);

    // crop and copy
    this.cropImage(tp0, dImage);
  }

  ///////////////
  // perspective transform (keystone)
  //   from : http://xenia.media.mit.edu/~cwren/interpolator/
  //   and  : http://www.bluebit.gr/matrix-calculator/linear_equations.aspx
  //
  private void keyTransform(PImage src, PImage dst) {
    // transform parameters
    // how much to keystone
    float w0 = random(5, 20);
    float w1 = 100.0-w0;

    // squeeze top or bottom
    int squeezeTop = (int)random(0, 2);

    // pick transform parameters based on amount and location of squeeze
    float a =  (squeezeTop == 1)?(100.0/(w1-w0)):(100.0/(w1+w0));
    float b =  (squeezeTop == 1)?(0+w0/(w1-w0)):(0-w0/(w1+w0));
    float c =  (squeezeTop == 1)?(-b):(0);
    float d =  0.0;
    float e =  (squeezeTop == 1)?((w1+w0)/(w1-w0)):((w1-w0)/(w1+w0));
    float f =  0.0;
    float g =  0.0;
    float h =  2*b;

    // image shouldn't be bigger or smaller than src
    dst.resize(src.width+10, src.height+20);

    src.loadPixels();
    dst.loadPixels();

    // iterate on dst pixels..
    for (int i=0; i<dst.height; i++) {
      for (int j=0; j<dst.width; j++) {
        // pixels in percent
        float xdf = float(j)/float(dst.width);
        float ydf = float(i)/float(dst.height);

        // percent points as well
        float xuf = (a*xdf + b*ydf + c)/(g*xdf + h*ydf + 1);
        float yuf = (d*xdf + e*ydf + f)/(g*xdf + h*ydf + 1);

        // turn back to pixels
        int xu = int(xuf*src.width);
        int yu = int(yuf*src.height);

        if ((xu>0) && (xu<src.width) && (yu>0) && (yu<src.height)) {
          dst.pixels[i*dst.width+j] = src.pixels[yu*src.width+xu];
        }
        else {
          dst.pixels[i*dst.width+j] = color(255);
        }
      }
    }
  }


  //////////////
  // affine transform (shear)
  //   from : http://homepages.inf.ed.ac.uk/rbf/HIPR2/affine.htm
  //
  private void horizontalShear(PImage src, PImage dst) {

    // shear amount
    float w = random(0.3, 0.7);
    w *= (random(-1, 1) > 0)?1:-1;

    // resize according to src image (which could be bigger/smaller) and shear amount
    dst.resize(int(src.width+src.height*abs(w)), src.height+10);

    src.loadPixels();
    dst.loadPixels();

    // iterate on dst pixels..
    for (int i=0; i<dst.height; i++) {
      for (int j=0; j<dst.width; j++) {
        // linear shearing
        int xu = int(j+i*w);
        int yu = i;

        // center word horizontally
        if (w>0) {
          xu -= int(dst.height*abs(w));
        }

        // check boundaries
        if ((xu>0) && (xu<src.width) && (yu>0) && (yu<src.height)) {
          dst.pixels[i*dst.width+j] = src.pixels[yu*src.width+xu];
        }
        else {
          dst.pixels[i*dst.width+j] = color(255);
        }
      }
    }
  }

  // wave shear
  //   simple, efficient and elegant way of vertically shifting pixels
  //
  private void waveShear(PImage src, PImage dst) {

    // number of waves/half-cycles in the shearing [1,4]
    float nn = random(1, 4);
    // max amplitude of shearing
    float ampY = random(4, 10);
    // pick a kind...
    //  k<1 = constant frequency
    //  k<2 = increasing frequency
    //  k<3 = decreasing frequency
    int k = (int)random(0, 3);

    // resize according to src image (which could be bigger/smaller) and shear amount
    dst.resize(src.width, int(src.height+2*ampY+20));

    src.loadPixels();
    dst.loadPixels();

    // iterate on dst pixels..
    for (int j=0; j<dst.width; j++) {
      for (int i=0; i<dst.height; i++) {

        // sinusoidal shearing
        float yuf = 0;
        if (k<1) {
          // constant frequency
          yuf = i-ampY*sin(PI*j*nn/dst.width);
        }
        else if (k<2) {
          // increasing frequency
          yuf = i-ampY*sin(PI*(constrain(nn-1, 1, nn)*0.01*float(j+dst.width/2))*(j)/dst.width);
        }
        else if (k<3) {
          //decreasing frequency
          yuf = i-ampY*sin(PI*(constrain(nn-2, 1, nn)*0.01*float((dst.width-j)+dst.width/2))*(dst.width-j)/dst.width);
        }

        // center vertically 
        int yu = int(yuf-ampY/2);
        int xu = j;

        // check boundaries
        if ((yu>0) && (yu<src.height)) {
          dst.pixels[i*dst.width+j] = src.pixels[yu*src.width+xu];
        }
        else {
          dst.pixels[i*dst.width+j] = color(255);
        }
      }
    }
  }

  /////////////////////
  // swirl ditort??
  //    from : http://stackoverflow.com/questions/225548/resources-for-image-distortion-algorithms
  //
  //    a = amount of rotation
  //    b = size of effect
  //
  //    angle = a*exp(-(x*x+y*y)/(b*b))
  //    u = cos(angle)*x + sin(angle)*y
  //    v = -sin(angle)*x + cos(angle)*y
  //
  private void swirl(PImage src, PImage dst, int xc, int yc) {
    float a = 0.01; // w
    float b = 66;

    src.loadPixels();
    dst.loadPixels();

    // iterate on dst pixels..
    for (int i=0; i<dst.height; i++) {      
      for (int j=0; j<dst.width; j++) {

        float rsq = (j-xc)*(j-xc) + (i-yc)*(i-yc);

        if (rsq < 200) {
          float angle = a*rsq/b;
          float xuf = cos(angle)*(j-xc) + sin(angle)*(i-yc);
          float yuf = -sin(angle)*(j-xc) + cos(angle)*(i-yc);

          int xu = int(xc+xuf);
          int yu = int(yc+yuf);

          dst.pixels[i*dst.width+j] = src.pixels[yu*src.width+xu];
        }
        else {
          dst.pixels[i*dst.width+j] = src.pixels[i*src.width+j];
        }
      }
    }
  }


  /////////////////////
  // fisheye distortion
  //  from : http://www.jasonwaltman.com/thesis/filter-fisheye.html
  //  w is some magic number [0.001, 0.1]
  //
  private void fisheye(PImage src, PImage dst, int xc, int yc, float w) {

    // resize according to src image (which could be bigger/smaller) and shear amount
    // !!!! ToBeImplemented

    src.loadPixels();
    dst.loadPixels();

    // iterate on dst pixels..
    for (int i=0; i<dst.height; i++) {      
      for (int j=0; j<dst.width; j++) {

        float rsq = (j-xc)*(j-xc) + (i-yc)*(i-yc);

        // do transform
        if (rsq < 256) {
          float R = min(dst.height, dst.width)/2;
          float s = R/log(w*R+1);

          float r = sqrt(rsq);
          float a = atan2((i-yc), (j-xc));

          //r = s * log(1+w*r);
          r = (exp(r/s)-1)/w;

          float xuf = r*cos(a);
          float yuf = r*sin(a);

          int xu = int(xc+xuf);
          int yu = int(yc+yuf);

          dst.pixels[i*dst.width+j] = src.pixels[yu*src.width+xu];
        }
        else {
          dst.pixels[i*dst.width+j] = color(0, 0, 0, 0);
          dst.pixels[i*dst.width+j] = src.pixels[i*src.width+j];
        }
      }
    }

    src.updatePixels();
    dst.updatePixels();
  }
}

