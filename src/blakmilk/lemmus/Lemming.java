package blakmilk.lemmus;

import android.graphics.Rect;

public class Lemming {
	
	int Tile, Frame = 0, frameCounter = 0;
	int walk = 0;
	String action = "right", phrase = "";
	boolean visible = true, smoking = false;
	byte selButton = 5, smokingTimer = 1, talkingTimer = 0;
	byte moveHTick = 0, moveVTick = 0, gravityTick = 0;
	Rect rLDst = new Rect();
	
	public Lemming(int t) {
		Tile = t;
	}
}
