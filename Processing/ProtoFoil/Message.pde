public class Message {

  private final static int SUCCESS = 0;
  private final static int FAIL = 1;

  private ArrayList<Word> words;
  private PImage theImage;

  // constructor
  //   create array of words
  public Message(String msg) {
    // fill array with words from msg
    MessageHelper(msg);
    // gen graphic
    this.genImage();
  }

  // constructor helper to fill array with words from msg
  private void MessageHelper(String msg) {
    words = new ArrayList<Word>();

    // fill array
    String[] tWords = msg.split(" ");
    for (int i=0; i<tWords.length; i++) {
      if (tWords[i].length() > 0) {
        words.add(new Word(tWords[i]));
      }
    }
  }

  private void genImage() {
    int w = INITW;
    int h = INITH;
    int result = placeWords(w, h);

    while (result == FAIL) {
      w += int(0.2f*w);
      h += int(0.2f*h);
      result = placeWords(w, h);
    }
  }

  private int placeWords(int width_, int height_) {
    // init graphic
    PGraphics theGraphic = createGraphics(width_, height_, JAVA2D);
    theGraphic.beginDraw();
    theGraphic.background(255);

    // keep track of max dimensions
    float maxX = 0;
    float maxY = 0;

    // current offsets for placing
    float xoff = 0;
    float yoff = 0;

    // for every word w
    for (int i=0; i<words.size(); i++) {
      Word w = words.get(i);
      PImage ti = w.getDImage();

      // if word doesn't fit in this line
      if ((xoff+ti.width)>theGraphic.width) {
        // reset xoffset
        xoff = 0;
        // bump up yoffset
        yoff = maxY+2;
      }

      // check if out of bounds in the y direction
      if ((yoff+ti.height) > theGraphic.height) {
        theGraphic.endDraw();
        theGraphic.dispose();
        return FAIL;
      }

      // offsets should be good: place word
      theGraphic.image(ti, xoff, yoff);

      // bump up xoffset
      xoff += ti.width+2;

      // update max values
      if (xoff > maxX) maxX = xoff;
      if ((yoff+ti.height)>maxY) maxY = yoff+ti.height;
    }
    // should have image
    theGraphic.endDraw();

    // create, size and copy onto theImage
    theImage = createImage(int(maxX+2), int(maxY+2), RGB);

    PImage t = theGraphic.get();
    theImage.copy(t, 0, 0, int(maxX+2), int(maxY+2), 0, 0, theImage.width, theImage.height);

    // clean up
    theGraphic.dispose();

    // success
    return SUCCESS;
  }

  private void genTestImage() {
    // init graphic
    PGraphics theGraphic = createGraphics(width, height, JAVA2D);
    theGraphic.beginDraw();
    theGraphic.background(200);

    float yoff = 0;
    for (int i=0; i<words.size()&&i<4; i++) {
      Word w = words.get(i);

      // show undistorted
      theGraphic.image(w.getUImage(), 0, yoff);
      yoff += w.getUImage().height+5;

      // show distorted
      theGraphic.image(w.getDImage(), 0, yoff);
      yoff += w.getDImage().height+5;
    }
    theGraphic.endDraw();
    // get image from graphic
    theImage = theGraphic.get();
  }

  // get Image
  public PImage getImage() {
    return theImage;
  }

  // access words
  public Word getWord(int i) {
    if ((i>-1)&&(i<words.size())) {
      return words.get(i);
    }
    else {
      return words.get(0);
    }
  }
}

