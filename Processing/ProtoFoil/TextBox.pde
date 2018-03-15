public class TextBox {
  //////////////////
  static final int SMALLFONT  = 20;

  // text box dimension
  static final int   NUMLINES = 4;
  static final float BOXWIDTH = 0.8;  // percent of width

  private PFont myFont = null;
  private float boxX, boxW, textY;

  private String theMessage = "";

  public TextBox() {

    // use courier here too
    int minFontNameLen = 1024;
    String myFontName = null;
    for (int i=0; i<PFont.list().length; i++) {
      if ((PFont.list()[i].contains("ourier") == true)&&(PFont.list()[i].length() < minFontNameLen)) {
        myFontName = PFont.list()[i];
        minFontNameLen = PFont.list()[i].length();
      }
    }
    myFontName = (myFontName == null)?(PFont.list()[0]):(myFontName);

    myFont = createFont(myFontName, SMALLFONT, true);

    // some constant stuff
    boxX = (1-BOXWIDTH)/2*width;
    boxW = BOXWIDTH*width;
    textY = height/4;
  }

  // draw a text input box and a send button
  public void draw() {
    background(0);

    // intro text
    textFont(myFont, SMALLFONT);
    fill(255);
    text("Enter message and hit enter.", boxX, textY/2, boxW-4, SMALLFONT+5);

    // text box
    stroke(111);
    fill(111);
    rect(boxX, textY, boxW, NUMLINES*SMALLFONT);

    stroke(111);
    fill(255);
    rect(boxX-1, textY-1, boxW-1, NUMLINES*SMALLFONT-1);

    // the message
    textAlign(LEFT, TOP);
    fill(0);
    //text(theMessage.toUpperCase(), boxX+2, textY+1, boxW-4, SMALLFONT*(NUMLINES-1));
    text(theMessage, boxX+2, textY+1, boxW-4, SMALLFONT*(NUMLINES-1));
  }

  public void clearMessage() {
    theMessage = "";
  }

  public String getMessage() {
    return theMessage;
  }

  void keyReleased(char mykey) {
    // on backspace, clear last char
    if (mykey == BACKSPACE) {
      if (theMessage.length() > 0) {
        theMessage = theMessage.substring(0, theMessage.length()-1);
      }
    }
    // on delete, clear string
    else if (mykey == DELETE) {
      theMessage = new String("");
    }
    // on key, add key to string
    else {
      if (theMessage.length() < 140) {
        theMessage = theMessage.toString()+(String.valueOf(mykey));
      }
    }
  }
};

