////////////////////////
//
//
///////////////////////

// some initial size guesses for the final image.
//   this affects the aspect ratio.
private final int INITW = 770;
private final int INITH = 330;

private final int STATE_ASK = 0;
private final int STATE_SHOW = 1;


int myState;
String theStringMessage = "";
PImage tt;
PFont mainFont = null;
TextBox myTextBox;
Message m;

void setup() {
  size(900, 450);
  background(220);
  smooth();

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

  mainFont = createFont(myFontName, 20);
  myTextBox = new TextBox();

  myState = STATE_ASK;
}


private String prepend0(int i) {
  if (i<10) return (new String("0"+String.valueOf(i)));
  else return String.valueOf(i);
}


void draw() {
  if (myState == STATE_ASK) {
    background(0);
    myTextBox.draw();
  }
  else if (myState == STATE_SHOW) {
    background(220);
    fill(0);
    textFont(mainFont);
    text("'R' = reset phrase; 'S' = save image; spacebar = regenerate image", 0, 0, width, 24);
    image(tt, 0, 40);
  }
}


////////////////////
void keyReleased() {
  // on ask screen, only deal with non-coded keys
  //   things that can be displayed
  if ((myState == STATE_ASK) && (key != CODED)) {
    // on enter, send message and clear box
    if ((key == ENTER) || (key == RETURN)) {
      theStringMessage = myTextBox.getMessage();
      m = new Message(theStringMessage);
      tt = m.getImage();
      //myTextBox.clearMessage();
      myState = STATE_SHOW;
    }
    else {
      /* pass forward to text box */
      myTextBox.keyReleased(key);
    }
  }

  // on show screen, save/reset/regen
  else if ((myState == STATE_SHOW)) {
    // save
    if ((m != null)&&((key == 's') || (key == 'S'))) {
      PImage ti = createImage(tt.width, tt.height, ARGB);
      ti.copy(tt, 0, 0, tt.width, tt.height, 0, 0, ti.width, ti.height);
      ti.resize(min(400, ti.width), 0);
      PGraphics tg = createGraphics(ti.width, ti.height, JAVA2D);
      tg.beginDraw();
      tg.background(255);
      tg.smooth();
      tg.image(ti, 0, 0);
      tg.endDraw();
      tg.save(dataPath(year()+prepend0(month())+prepend0(day())+prepend0(hour())+prepend0(minute())+prepend0(second())+".png"));
    }

    // reset
    else if ((key == 'r')||(key == 'R')) {
      myState = STATE_ASK;
    }

    // regen
    else if (key == ' ') {
      m = new Message(theStringMessage);
      tt = m.getImage();
    }
  }
}

void mouseReleased() {
  if (myState == STATE_SHOW) {
    if (mouseButton == RIGHT) {
      myState = STATE_ASK;
    }
    else {
      Message m = new Message(theStringMessage);
      tt = m.getImage();
    }
  }

  else if (myState == STATE_ASK) {
    // nothing??
  }
}

