package blakmilk.lemmus;

import java.util.ArrayList;
import java.util.Random;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainGame extends Activity {
	theGame G;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		G = new theGame(this, getAssets());
		setContentView(G);
	}
	
	@Override
    public void onBackPressed() {
		G.bufferPaused = !G.bufferPaused;
    }

	public boolean onPrepareOptionsMenu(Menu menu) {
		G.bufferShowRoster = !G.bufferShowRoster;
		return false;
	}
} //end MainGame

class theGame extends SurfaceView implements SurfaceHolder.Callback {
   
	Bitmap selected, pause, zoom, roster, rosterBgd, blankButton, tiles, background;
	Bitmap[] spriteSheet, controls, doods, walking, left, camera;
	ArrayList<AnimatedDoodad> doodads = new ArrayList<AnimatedDoodad>();
	Lemming[] Lem = new Lemming[10];
	Pic other;
	
	byte[] Level;
	byte selRobot = 0, Z = 0, bufferZ = 0;
	int WIDTH, HEIGHT;
	int[] tileW, tileH;
	int[] screenTilesX, screenTilesY, maxZenith;
	int levelTilesX, levelTilesY, tileOffset;
	int zenith = 0, bufferZenith = 0;
	float[] move, gravity;
	float startX, startY, moveX, moveY;
	boolean paused = false, bufferPaused = false, showRoster = false, bufferShowRoster = false;
	
	Rect[][] rRosterDst, rTilesDst;
	Rect[] rRosterBgdDst, rMenuDst, rCamDst, rControlsDst;
	Rect[] rFlatSrc, r2SquareSrc, rTilesSrc, rRobotNumSrc;
	Rect rRosterBgdSrc, rSelectedDst, rWalkingSrc, rOffscreen;
	Rect rScreen, rLemSrc, rDoodadSrc, rDoodadDst, rBgdSrc;
	
	GameThread nthread;
	Typeface astrolyt;
	Random rand = new Random();
	Matrix Neo = new Matrix();
	Paint textPaint = new Paint();
	Paint pausePaint = new Paint();
	String[] phrasesSelected, phrasesCommanded, phrasesSmoking;
	short[] reds, greens, blues;
	final static int SELECTED = 0, COMMANDED = 1, SMOKING = 2;
	
	public theGame(Context context, AssetManager A) {
		super(context);
	    getHolder().addCallback(this);
        setFocusable(true);	
		astrolyt = Typeface.createFromAsset(A, "ASTROLYT.TTF");
		miscSetup();
	}

	//called when created, giving screen size
	 @Override
    public void onSizeChanged (int w, int h, int oldw, int oldh) {
		//landscape
	    WIDTH = Math.max(w, h);
	    HEIGHT = Math.min(w, h);
	    
	    Levels lvls = new Levels();
	    levelTilesX = lvls.getLevelTilesX(1);
	    levelTilesY = lvls.getLevelTilesY(1);
	    Level = new byte[levelTilesX * levelTilesY];
	    Level = lvls.getLevel(1);
	    
	    Neo.setScale(-1, 1);
	    spriteSetup();
	    guiSetup();
	    
	    //start thread AFTER loading
	    nthread = new GameThread(getHolder(), this);
        nthread.setRunning(true);
        nthread.start();
        
        super.onSizeChanged(w, h, oldw, oldh);
	}
	 
	byte s = 0;
	@Override
    public synchronized boolean onTouchEvent(MotionEvent me) {
		moveX = me.getX();
		moveY = me.getY();
		
		if(checkMenuPresses())
			return false;
		if (showRoster && rosterSelect())
			return false;
		
		//paused screen
		if (paused) {
			if (Within(other, (int) me.getX(), (int) me.getY()))
				((Activity) getContext()).finish();
		}
		//other click events
		else {
			s = scrollCamera();
			if (s == 1)
				return true;
			else if (s == 2)
				return false;
			
			//initial touch
			if (me.getAction() == MotionEvent.ACTION_DOWN) {
				startX = me.getX();
				startY = me.getY();

				//check for control changes
				if (checkControlPresses())
					return false;
				
				//click/choose new robot
				for (byte l = 0; l < Lem.length; l++) {
					if (withinRect(Lem[l].rLDst, me.getX(), me.getY()) && l != selRobot) {
						selRobot = l;
						setPhrase(selRobot, SELECTED);
						//centerScreen(Lem[selRobot].Tile);
						return false;
					}
				}
				
			} //end action_down
			else if (me.getAction() == MotionEvent.ACTION_MOVE) {
				dragCamera();
			} //end action_move
			else if (me.getAction() == MotionEvent.ACTION_UP) {
				startX = moveX;
				startY = moveY;
				return false;
			} //end action_up
		} //end !paused
        return true;
    } //end OnTouchEvent
	
	@Override
    public void onDraw(Canvas c) {
		//this bit keeps your zenith within bounds when zooming out
		//(if you're in the bottom right corner and zoom out,
		//your zenith is too far down)
		if (bufferZ == 0 && Z == 1) {
			while (bufferZenith > levelTilesX * (levelTilesY - screenTilesY[0] + 1))
				bufferZenith -= levelTilesX;
			while (bufferZenith % levelTilesX > levelTilesX - screenTilesX[0])
				bufferZenith--;
		}		
		Z = bufferZ;
		showRoster = bufferShowRoster;
		paused = bufferPaused;
		zenith = bufferZenith;

		//draw bgd img==============================================
		rBgdSrc.left = (zenith % 100) * tileW[Z];
		rBgdSrc.top = (zenith / 100) * tileH[Z];
		rBgdSrc.right = rBgdSrc.left + screenTilesX[Z] * tileW[Z];
		rBgdSrc.bottom = rBgdSrc.top + screenTilesY[Z] * tileH[Z];
		
		c.drawBitmap(background, rBgdSrc, rScreen, null);
		
		//draw tile background==================================================
		for (int i = 0; i < screenTilesY[Z]; i++) {
			for (int p = 0; p < screenTilesX[Z]; p++) {
				if (Level[zenith + (levelTilesX * i) + p] > 3)
					c.drawBitmap(tiles, rTilesSrc[Level[zenith + (levelTilesX * i) + p] - 4], rTilesDst[Z][i * screenTilesX[Z] + p], null);
			}
		}
		
		//draw animated doodads========================================================
//		for (int d = 0; d < doodads.size(); d++) {
//			if (isOnScreen(doodads.get(d).Tile)) {
//				tileOffset = findTileOffset(doodads.get(d).Tile);
//				rDoodadSrc.left = tileW * doodads.get(d).Frame;
//				rDoodadSrc.right = rDoodadSrc.left + tileW;
//				
//				rDoodadDst.left = tPos[tileOffset].x;
//				rDoodadDst.top = tPos[tileOffset].y;
//				rDoodadDst.right = rDoodadDst.left + tileW;
//				rDoodadDst.bottom = rDoodadDst.top + tileH;
//				c.drawBitmap(doods[doodads.get(d).SS], rDoodadSrc, rDoodadDst, null);	
//			}
//			if(!paused)
//				doodads.get(d).update();
//		}
		
		//draw/update Lemmings=============================================
		for (byte l = 0; l < Lem.length; l++) {
			if (isOnScreen(Lem[l].Tile) && Lem[l].visible) {
				tileOffset = findTileOffset(Lem[l].Tile);
				Lem[l].rLDst = setDestRect(Lem[l], tileOffset);
				
				if(Lem[l].action == "right")
					c.drawBitmap(walking[Lem[l].Frame], rWalkingSrc, Lem[l].rLDst, null);
				else 
					c.drawBitmap(left[Lem[l].Frame], rWalkingSrc, Lem[l].rLDst, null);	
				
				if (l == selRobot) {
					rSelectedDst = Lem[l].rLDst;
					rSelectedDst.bottom = rSelectedDst.top;
					rSelectedDst.top -= tileH[Z] / 2;
					c.drawBitmap(selected, rFlatSrc[0], rSelectedDst, null);
				}
			}
			else {
				//lemming is off screen, so negate its Rect so you can't click on it still
				Lem[l].rLDst = rOffscreen;
			}
			if(!paused)
				updateLemming(Lem[l]);
		}
		
		//Lemming speech======================================================
		if (Lem[selRobot].talkingTimer > 0) {
			textPaint.setColor(getTextColor(selRobot));
			c.drawText(Lem[selRobot].phrase, Lem[selRobot].rLDst.right, Lem[selRobot].rLDst.top + tileH[0] / 2, textPaint);
		}
		
		//draw menu buttons or roster=========================================================
		if (!showRoster) {
			for (byte b = 0; b < 6; b++) {
				//button 0 = robot selection
				if (b == 0) {
					c.drawBitmap(controls[b], rRobotNumSrc[selRobot], rControlsDst[b], null);
				}
				else {
					//selected button
					if (Lem[selRobot].selButton == b)
						c.drawBitmap(controls[b], r2SquareSrc[1], rControlsDst[b], null);
					else
						c.drawBitmap(controls[b], r2SquareSrc[0], rControlsDst[b], null);
				}
				
			}
			//camera buttons=========================================================
			for (byte a = 0; a < 4; a++)
				c.drawBitmap(camera[a], r2SquareSrc[0], rCamDst[a], null);
		}
		else {
			//roster=============================================================
			for (byte r = 0; r < 10; r++) {
				//bgd
				c.drawBitmap(rosterBgd, rRosterBgdSrc, rRosterBgdDst[r], null);
				//robot number
				c.drawBitmap(controls[0], rRobotNumSrc[r], rRosterDst[r][0], null);
				//action
				c.drawBitmap(controls[Lem[r].selButton], r2SquareSrc[1], rRosterDst[r][1], null);
				//spot for hp and smoking life
				c.drawBitmap(blankButton, r2SquareSrc[0], rRosterDst[r][2], null);
				//tech
				c.drawBitmap(controls[1], r2SquareSrc[0], rRosterDst[r][3], null);
			}
		}

		//paused?=====================================================================
		if (paused) {
			c.drawRect(rScreen, pausePaint);
			c.drawBitmap(other.B, other.X, other.Y, null);
		}	
		
		//small menu buttons==========================================================
		if (paused) 
			c.drawBitmap(pause, rFlatSrc[1], rMenuDst[0], null);
		else
			c.drawBitmap(pause, rFlatSrc[0], rMenuDst[0], null);
		
		c.drawBitmap(zoom, rFlatSrc[Z], rMenuDst[1], null);
		
		if (showRoster) 
			c.drawBitmap(roster, rFlatSrc[1], rMenuDst[2], null);
		else
			c.drawBitmap(roster, rFlatSrc[0], rMenuDst[2], null);
		c.drawText(String.valueOf(s), 300, 300, textPaint);
	} //end onDraw
	
	void updateLemming(Lemming L) {
//		if (L.walk++ < 2) 
//			return;
//		L.walk = 0;
		
		if(L.frameCounter++ >= 2)
		{
			L.frameCounter = 0;
			L.Frame++;
			if(L.Frame >= 19)
				L.Frame = 0;
		}
		if (L.talkingTimer > 0)
			L.talkingTimer--;
		
		if (L.smoking) {
			if (L.smokingTimer-- == 0) {
				L.smoking = false;
				if (L.action == "right")
					L.selButton = 5;
				else
					L.selButton = 3;
			}
			return;
		}
		
		//fall
		if (Level[L.Tile + levelTilesX] == 0) {
			if (L.gravityTick++ == 8) {
				L.gravityTick = 0;
				L.Tile += levelTilesX;
			}
		} //end fall
		else if (L.action == "right") {
			//ascend ramp
			if(Level[L.Tile + 1] < 3) {
				L.moveVTick--;
				if (L.moveHTick++ == 45) {
					L.moveVTick = L.moveHTick = 0;
					L.Tile -= levelTilesX - 1;
				}
			}
			//descend ramp
			else if(Level[L.Tile + levelTilesX] < 3 && Level[L.Tile + levelTilesX + 1] == 0) {
				L.moveVTick++;
				if (L.moveHTick++ == 45) {
					L.moveVTick = L.moveHTick = 0;
					L.Tile += levelTilesX + 1;
				}
			}
			//move
			else if (Level[L.Tile + 1] == 0) {
				if (L.moveHTick++ == 45) {
					L.moveHTick = 0;
					L.Tile++;
				}
			}
			//switch direction
			else if (Level[L.Tile + 1] > 2) {
				//L.moveHTick = L.moveVTick = 0;
				L.action = "left";
				L.selButton = 3;
			}
		} //end right
		else if (L.action == "left") {
			//ascend ramp
			if(Level[L.Tile - 1] < 3) {
				L.moveVTick--;
				if (L.moveHTick-- == -45) {
					L.moveVTick = L.moveHTick = 0;
					L.Tile -= levelTilesX + 1;
				}
			}
			//descend ramp
			else if(Level[L.Tile + levelTilesX] < 3 && Level[L.Tile + levelTilesX - 1] == 0) {
				L.moveVTick++;
				if (L.moveHTick-- == -45) {
					L.moveVTick = L.moveHTick = 0;
					L.Tile += levelTilesX - 1;
				}
			}
			//move
			else if (Level[L.Tile - 1] == 0) {
				if (L.moveHTick-- == -45) {
					L.moveHTick = 0;
					L.Tile--;
				}
			}
			//switch actionection
			else if (Level[L.Tile - 1] > 2) {
				//L.VX = 0;
				L.action = "right";
				L.selButton = 5;
			}
		} //end left	
	} //end updateLemming
	
	//====================================================================================================
	//                                       GUI BUTTON PRESSES
	//====================================================================================================
	
	boolean checkControlPresses () {
		for (byte b = 0; b < 6; b++) {
			if (withinRect(rControlsDst[b], moveX, moveY)) {
				//change robot selection
				if (b == 0) {
					selRobot++;
					if (selRobot == 10)
						selRobot = 0;
					setPhrase(selRobot, SELECTED);
					centerScreen(Lem[selRobot].Tile);
					return true;
				}
				//other menu buttons
				Lem[selRobot].selButton = b;
				if (b == 3) {
					Lem[selRobot].action = "left";
					setPhrase(selRobot, COMMANDED);
				}
				else if (b == 5) {
					Lem[selRobot].action = "right";
					setPhrase(selRobot, COMMANDED);
				}
				else if (b == 4) {
					Lem[selRobot].smoking = true;
					Lem[selRobot].smokingTimer = 99;
					setPhrase(selRobot, SMOKING);
				}
				
				return true;
			} //end buttonPress
		} //end for b
		return false;
	} //end checkControlPresses
	
	byte scrollCamera() {
		//returns 1 to keep scrolling,
		//2 to stop, and 0 for no presses
		bufferZenith = zenith;
		int camFromZeroX = bufferZenith % levelTilesX;
		int camFromZeroY = bufferZenith / levelTilesX;
		if(withinRect(rCamDst[0], moveX, moveY) && camFromZeroY > 0) {
			bufferZenith -= levelTilesX;
			if (bufferZenith / levelTilesX > 0)
				return 1;
			else
				return 2;
		}
		else if(withinRect(rCamDst[1], moveX, moveY) && camFromZeroX < levelTilesX - screenTilesX[Z]) { 
			bufferZenith++;
			if (bufferZenith % levelTilesX < levelTilesX - screenTilesX[Z])
				return 1;
			else
				return 2;
		}
		else if(withinRect(rCamDst[2], moveX, moveY) && camFromZeroY < levelTilesY - screenTilesY[Z]) { 
			bufferZenith += levelTilesX;
			if (bufferZenith / levelTilesX < levelTilesY - screenTilesY[Z])
				return 1;
			else
				return 2;
		}
		else if(withinRect(rCamDst[3], moveX, moveY) && camFromZeroX > 0) {
			bufferZenith--;
			if (bufferZenith % levelTilesX > 0)
				return 1;
			else
				return 2;
		}
		return 0;
	}
	
	void dragCamera() {
		bufferZenith = zenith;
		int fromZeroX = bufferZenith % 100;
		int fromZeroY = (bufferZenith - fromZeroX) / 100;
		//int nextLineStart = zenith + (levelTilesX - fromZeroX);
		
		if (startX - moveX < -tileW[Z] && fromZeroX > 0) {
			bufferZenith--;
			startX += tileW[Z];
		}
		if (moveX - startX < -tileW[Z] && fromZeroX < levelTilesX - screenTilesX[Z]) {
			bufferZenith++;
			startX -= tileW[Z];
		}
		if (startY - moveY < -tileH[Z] && fromZeroY > 0) {
			bufferZenith -= levelTilesX;
			startY += tileH[Z];
		}
		if (moveY - startY < -tileH[Z] && fromZeroY < levelTilesY - screenTilesY[Z]) {
			bufferZenith += levelTilesX;
			startY -= tileH[Z];
		}
	}
	
	boolean rosterSelect() {
		for (byte r = 0; r < 10; r++) {
			if (withinRect(rRosterBgdDst[r], moveX, moveY)) {
				selRobot = r;
				setPhrase(selRobot, SELECTED);
				centerScreen(Lem[selRobot].Tile);
				bufferShowRoster = false;
				return true;
			}
		}
		return false;
	}
	
	boolean checkMenuPresses() {
		bufferZ = Z;
		bufferShowRoster = showRoster;
		bufferPaused = paused;
		//pause button
		if (withinRect(rMenuDst[0], moveX, moveY)) {
			bufferPaused = !bufferPaused;
			return true;
		}
		//zoom button
		if (withinRect(rMenuDst[1], moveX, moveY)) {
			if (bufferZ == 1)
				bufferZ = 0;
			else
				bufferZ = 1;
			return true;
		}
		//roster button
		if (withinRect(rMenuDst[2], moveX, moveY)) {
			bufferShowRoster = !bufferShowRoster;
			return true;
		}
		return false;
	}
	
	//====================================================================================================
	//                                        HELPER FUNCTIONS
	//====================================================================================================
	
	Rect setDestRect(Lemming L, int T) {
		Rect R = new Rect(rTilesDst[Z][T]);
		R.top = (int) (R.top - tileH[Z] + L.gravityTick * gravity[Z] + L.moveVTick * move[Z]);
		R.left += (int) (L.moveHTick * move[Z]);
		R.right = R.left + tileW[Z];
		R.bottom = R.top + tileH[Z] * 2;
		return R;
	}
	
	int findTileOffset(int fedTile) {
		int yOffset = 0, xOffset = 0;
		for (int a = 1; a < screenTilesY[Z]; a++) {
			if (fedTile - levelTilesX * a < zenith)
				break;
			yOffset++;
		}
		for (int z = 1; z < screenTilesX[Z]; z++) {
			if (fedTile - levelTilesX * yOffset - z < zenith)
				break;
			xOffset++;
		}
		return xOffset + yOffset * screenTilesX[Z];
	}
	
	boolean isOnScreen(int fedTile) {
		for (int s = 0; s < screenTilesY[Z]; s++) {
			if (fedTile >= zenith + levelTilesX * s &&
				fedTile < zenith + levelTilesX * s + screenTilesX[Z])
				return true;
		}
		return false;
	}
	
	void centerScreen(int fedTile) {
		byte xx = (byte) (fedTile % 100 - screenTilesX[Z] / 2);
		byte yy = (byte) (fedTile / 100 - screenTilesY[Z] / 2);
		byte xOffset = 0, yOffset = 0;
		
		//find offsets
		if (xx < 0)
			xOffset = 0;
		else if (xx > levelTilesX - screenTilesX[Z])
			xOffset = (byte) (levelTilesX - screenTilesX[Z]);
		else
			xOffset = xx;
		
		if (yy < 0)
			yOffset = 0;
		else if (yy > levelTilesY - screenTilesY[Z])
			yOffset = (byte) (levelTilesY - screenTilesY[Z]);
		else
			yOffset = yy;
		
		//set screen
		bufferZenith = xOffset + yOffset * levelTilesX;
	}
	
	boolean Within(Pic P, int x, int y) {
		if (x >= P.X && x <= P.X + P.B.getWidth() &&
			y >= P.Y && y <= P.Y + P.B.getHeight())
			return true;
		return false;
	}
	
	boolean withinRect (Rect R, float x, float y) {
		if (x >= R.left && x <= R.right &&
			y >= R.top && y <= R.bottom)
			return true;
		return false;
	}
	
	int getTextColor (byte c) {
		return Color.rgb(reds[c], greens[c], blues[c]);
	}
	
	void setPhrase(byte dude, int which) {
		Lem[dude].talkingTimer = 100;
		switch (which) {
			case SELECTED:
				Lem[selRobot].phrase = phrasesSelected[rand.nextInt(phrasesSelected.length)];
				break;
			case COMMANDED:
				Lem[selRobot].phrase = phrasesCommanded[rand.nextInt(phrasesCommanded.length)];
				break;	
			case SMOKING:
				Lem[selRobot].phrase = phrasesSmoking[rand.nextInt(phrasesSmoking.length)];
				break;
		}
	}
	
	//====================================================================================================
	//                                           INTIALIZATION
	//====================================================================================================
	
	void miscSetup() {
        textPaint.setStyle(Paint.Style.STROKE);
	    textPaint.setTypeface(astrolyt);
	    textPaint.setTextSize(17);
	    
	    pausePaint.setColor(Color.BLACK);
		pausePaint.setStyle(Paint.Style.FILL);
		pausePaint.setAlpha(150);
		
		//text colors for robot speech (used by/with getTextColor)
		reds = new short[]   {212, 221,  94, 255,  58, 255, 111, 103, 205, 175};
		greens = new short[] {104, 221, 255,  99,  65, 249,  15, 205,  52, 212};
		blues = new short[]  {255,   5, 188,  61, 255, 170, 127,  53,  57, 255};
		
		//flavor text
		phrasesSelected = new String[]
			{"Hello",
			"Hi!",
			"Orders?",
			"Whatchya need?",
			"Input required",
			"Command me",
			"*whistling*",
			"eh?",
			"hrm?",
			"I'm thirsty",
			"What's up boss?"};
		
		phrasesCommanded = new String[]
			{"You got it",
			"Okay",
			"If I have to...",
			"Can't make me",
			"Right on",
			"beep",
			"boop",
			"Alrighty then",
			"Sounds fun",
			"10100111001",
			"shibby",
			"Gotchya boss"};
		
		phrasesSmoking = new String[]
			{"You tryna kill me?",
			"But I'll die",
			"Oh dear",
			"Oofda",
			"Where's my lighter?",
			"My lungs!",
			"*cough*",
			"ow",
			"I'm trying to quit though",
			"So much for cold turkey",
			"Smoke break!",
			"Ay dios mio"};
				
	}
	
	void spriteSetup() {
		//declare for 2 zoom levels
		tileW = new int[2];
		tileH = new int[2];
		screenTilesX = new int[2];
		screenTilesY = new int[2];
		move = new float[2];
		gravity = new float[2];
		rTilesDst = new Rect[2][];

		// W <= 480 give small sprites
		if (WIDTH <= 480) {
			tileW[0] = tileH[0] = 16;
			tileW[1] = tileH[1] = 32;
			move[0] = .35f;
			move[1] = .7f;
			gravity[0] = 2;
			gravity[1] = 4;
		}
		else {
			tileW[0] = tileH[0] = 32;
			tileW[1] = tileH[1] = 64;
			move[0] = .7f;
			move[1] = 1.4f;
			gravity[0] = 4;
			gravity[1] = 8;
		}
	
		//load tiles======================================
		tiles = BitmapFactory.decodeResource(getResources(), R.drawable.ss_bgd);
		tiles = Bitmap.createScaledBitmap(tiles, tileW[1] * 8, tileH[1], false);

		rTilesSrc = new Rect[8];
		for (byte t = 0; t < 8; t++) 
			rTilesSrc[t] = new Rect(tileW[1] * t, 0, tileW[1] * (t + 1), tileH[1]);
		
		//load backgound======================================
		background = BitmapFactory.decodeResource(getResources(), R.drawable.lvl1);
		background = Bitmap.createScaledBitmap(background, tileW[1] * levelTilesX, tileH[1] * levelTilesY, false);
		
		rBgdSrc = new Rect();
		
		//doodads====================================================================
//		doods = new Bitmap[1];
//		doods[0] = BitmapFactory.decodeResource(getResources(), R.drawable.d_circlelight);
//		
//		doodads.add(new AnimatedDoodad(0, 113));
//		doodads.add(new AnimatedDoodad(0, 213));
//		doodads.add(new AnimatedDoodad(0, 313));
		
		//set screen size/tiles for both zoom levels===========================================
		for (byte z = 0; z < 2; z++) {
			screenTilesX[z] = (int) (WIDTH / tileW[z]);
			screenTilesY[z] = (int) (HEIGHT / tileH[z]);
			//for those goofy resolutions
			if (WIDTH % tileW[z] != 0)
				screenTilesX[z]++;
			if (HEIGHT % tileH[z] != 0)
				screenTilesY[z]++;
			
			rTilesDst[z] = new Rect[screenTilesX[z] * screenTilesY[z]];

			for (int yy = 0; yy < screenTilesY[z]; yy++) {
				for (int xx = 0; xx < screenTilesX[z]; xx++) {
					rTilesDst[z][yy * screenTilesX[z] + xx] = new Rect(tileW[z] * xx, tileH[z] * yy, tileW[z] * xx + tileW[z], tileH[z] * yy + tileH[z]);
				}
			}
		} //end for z
		
		walking = new Bitmap[19];
		walking[0] = BitmapFactory.decodeResource(getResources(), R.drawable.f1);
		walking[1] = BitmapFactory.decodeResource(getResources(), R.drawable.f2);
		walking[2] = BitmapFactory.decodeResource(getResources(), R.drawable.f3);
		walking[3] = BitmapFactory.decodeResource(getResources(), R.drawable.f4);
		walking[4] = BitmapFactory.decodeResource(getResources(), R.drawable.f5);
		walking[5] = BitmapFactory.decodeResource(getResources(), R.drawable.f6);
		walking[6] = BitmapFactory.decodeResource(getResources(), R.drawable.f7);
		walking[7] = BitmapFactory.decodeResource(getResources(), R.drawable.f8);
		walking[8] = BitmapFactory.decodeResource(getResources(), R.drawable.f9);
		walking[9] = BitmapFactory.decodeResource(getResources(), R.drawable.f10);
		walking[10] = BitmapFactory.decodeResource(getResources(), R.drawable.f11);
		walking[11] = BitmapFactory.decodeResource(getResources(), R.drawable.f12);
		walking[12] = BitmapFactory.decodeResource(getResources(), R.drawable.f13);
		walking[13] = BitmapFactory.decodeResource(getResources(), R.drawable.f14);
		walking[14] = BitmapFactory.decodeResource(getResources(), R.drawable.f15);
		walking[15] = BitmapFactory.decodeResource(getResources(), R.drawable.f16);
		walking[16] = BitmapFactory.decodeResource(getResources(), R.drawable.f17);
		walking[17] = BitmapFactory.decodeResource(getResources(), R.drawable.f18);
		walking[18] = BitmapFactory.decodeResource(getResources(), R.drawable.f19);
		
		for (int w = 0; w < 19; w++) 
			walking[w] = Bitmap.createScaledBitmap(walking[w], tileW[1], tileH[1] * 2, false);
		
		
		left = new Bitmap[19];
		for (int w = 0; w < 19; w++) 
			left[w] = Bitmap.createBitmap(walking[w], 0, 0, tileW[1], tileH[1] * 2, Neo, false);
		
		rWalkingSrc = new Rect(0, 0, tileW[1], tileH[1] * 2);
		rOffscreen = new Rect(0, -tileH[0], 0, 0);
		
		for(int l = 0; l < Lem.length; l++) {
			Lem[l] = new Lemming(l + 4);
			Lem[l].Frame = rand.nextInt(19);
		}
	} //end spriteSetup
	
	void guiSetup() {
		other = new Pic(getResources(), R.drawable.menuother, WIDTH / 2, HEIGHT / 8);
		other.X = WIDTH / 4;
		other.Y = (int) (HEIGHT * .8);
		
		//control buttons=====================================================================
		controls = new Bitmap[6];
		controls[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.b_numbers), tileW[1] * 10, tileH[1], false);
		controls[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.b_emptytech), tileW[1] * 2, tileH[1], false);
		controls[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.b_operate), tileW[1] * 3, tileH[1], false);
		controls[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.b_arrowleft), tileW[1] * 2, tileH[1], false);
		controls[4] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.b_smoking), tileW[1] * 2, tileH[1], false);
		controls[5] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.b_arrowright), tileW[1] * 2, tileH[1], false);
		
		r2SquareSrc = new Rect[3];
		for (byte q = 0; q < 3; q++)
			r2SquareSrc[q] = new Rect(tileW[1] * q, 0, tileW[1] * (q + 1), tileH[1]);
		
		rRobotNumSrc = new Rect[10];
		for (byte t = 0; t < 10; t++) 
			rRobotNumSrc[t] = new Rect(tileW[1] * t, 0, tileW[1] * (t + 1), tileH[1]);
		
		rControlsDst = new Rect[6];
		for (byte b = 0; b < 6; b++)
			rControlsDst[b] = new Rect(WIDTH / 2 - tileW[0] * 6 + tileW[1] * b, HEIGHT - tileH[1] - tileH[0] / 2, WIDTH / 2 - tileW[0] * 6 + tileW[1] * b + tileW[1], HEIGHT - tileH[0] / 2);
		
		//camera==================================================================================
		camera = new Bitmap[4];
		camera[0] = BitmapFactory.decodeResource(getResources(), R.drawable.cam_up);
		camera[1] = BitmapFactory.decodeResource(getResources(), R.drawable.cam_right);
		camera[2] = BitmapFactory.decodeResource(getResources(), R.drawable.cam_down);
		camera[3] = BitmapFactory.decodeResource(getResources(), R.drawable.cam_left);
		
		for (byte c = 0; c < 4; c++) 
			camera[c] = Bitmap.createScaledBitmap(camera[c], tileW[1], tileH[1], false);
	
		rCamDst = new Rect[4];
		rCamDst[0] = new Rect(tileW[1], 0, tileW[1] * 2, tileH[1]);
		rCamDst[1] = new Rect(tileW[1] * 2, tileH[1], tileW[1] * 3, tileH[1] * 2);
		rCamDst[2] = new Rect(tileW[1], tileH[1] * 2, tileW[1] * 2, tileH[1] * 3);
		rCamDst[3] = new Rect(0, tileH[1], tileW[1], tileH[1] * 2);
		
		//menu=========================================================================================
		zoom = BitmapFactory.decodeResource(getResources(), R.drawable.c_zoom);
		zoom = Bitmap.createScaledBitmap(zoom, tileW[1] * 2, tileH[0], false);
		
		pause = BitmapFactory.decodeResource(getResources(), R.drawable.c_pause);
		pause = Bitmap.createScaledBitmap(pause, tileW[1] * 2, tileH[0], false);
		
		roster = BitmapFactory.decodeResource(getResources(), R.drawable.c_roster);
		roster = Bitmap.createScaledBitmap(roster, tileW[1] * 2, tileH[0], false);
		
		selected = BitmapFactory.decodeResource(getResources(), R.drawable.selected);
		selected = Bitmap.createScaledBitmap(selected, tileW[1], tileH[0], false);
		
		rMenuDst = new Rect[3];
		for (byte c = 0; c < 3; c++)
			rMenuDst[c] = new Rect(WIDTH - tileW[1] * (c + 1), 0, WIDTH - tileW[1] * c, tileH[0]);
		
		//rFlatSrc is used by menu buttons and selected
		rFlatSrc = new Rect[2];
		rFlatSrc[0] = new Rect(0, 0, tileW[1], tileH[0]);
		rFlatSrc[1] = new Rect(tileW[1], 0, tileW[1] * 2, tileH[0]);
				
		//roster=======================================================================================
		rRosterDst = new Rect[10][4];
		int w = (WIDTH - (tileW[1] * 10)) / 2;
		int h = (HEIGHT / 2);
		for (byte row = 0; row < 2; row++) {
			for (byte col = 0; col < 5; col++) {
				rRosterDst[row * 5 + col][0] = new Rect(w + col * tileW[1] * 2, h - tileW[1] * 2, w + col * tileW[1] * 2 + tileW[1], h - tileW[1] * 2 + tileH[1]);
				rRosterDst[row * 5 + col][1] = new Rect(w + col * tileW[1] * 2 + tileW[1], h - tileW[1] * 2, w + col * tileW[1] * 2 + tileW[1] * 2, h - tileW[1] * 2 + tileH[1]);
				rRosterDst[row * 5 + col][2] = new Rect(w + col * tileW[1] * 2, h - tileW[1], w + col * tileW[1] * 2 + tileW[1], h);
				rRosterDst[row * 5 + col][3] = new Rect(w + col * tileW[1] * 2 + tileW[1], h - tileW[1], w + col * tileW[1] * 2 + tileW[1] * 2, h);
			}
			h += tileH[1] * 2;
		}
		
		blankButton = BitmapFactory.decodeResource(getResources(), R.drawable.b_blank);
		blankButton = Bitmap.createScaledBitmap(blankButton, tileW[1], tileH[1], false);
		
		rosterBgd = BitmapFactory.decodeResource(getResources(), R.drawable.b_rosterbgd);
		rosterBgd = Bitmap.createScaledBitmap(rosterBgd, tileW[1] * 2, tileH[1] * 2, false);
		
		rRosterBgdSrc = new Rect(0, 0, tileW[1] * 2, tileH[1] * 2);
		rRosterBgdDst = new Rect[10];
		for (byte r = 0; r < 10; r++) 
			rRosterBgdDst[r] = new Rect(rRosterDst[r][0].left, rRosterDst[r][0].top, rRosterDst[r][3].right, rRosterDst[r][3].bottom);
		
		//pause screen rectangle
		rScreen = new Rect(0, 0, WIDTH, HEIGHT);
		
//		System.out.println(screenTilesX + ", " + screenTilesY);
//		System.out.println(levelTilesX + ", " + levelTilesY);
	} //end guiSetup
	
	//default methods=========================================================
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		//thread initialization moved to after loading sprites
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		nthread.setRunning(false);	
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		//nada
	}

} //end theGame
