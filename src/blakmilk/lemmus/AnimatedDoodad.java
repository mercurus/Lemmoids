package blakmilk.lemmus;

public class AnimatedDoodad {

	int SS, Tile, Frame = 0, frameTimer = 0;
	int[] timer;
	
	public AnimatedDoodad(int s, int t) {
		SS = s;
		Tile = t;
		if (SS == 0) {  //circle light
			timer = new int[2];
			timer[0] = 70;
			timer[1] = 70;
		}
	}
	
	public void update() {
		frameTimer++;
		if (frameTimer > timer[Frame]) {
			Frame++;
			frameTimer = 0;
		}
		if (Frame >= timer.length)
			Frame = 0;
	}

}
