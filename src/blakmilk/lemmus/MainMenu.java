package blakmilk.lemmus;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainMenu extends Activity {

	theMenu M;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		M = new theMenu(this);
		setContentView(M);
	}

	@Override
    public void onBackPressed() {
		M.thread.setRunning(false);
        finish();
        return;
    }

} //end MainMenu

class theMenu extends SurfaceView implements SurfaceHolder.Callback {
    
	GameThread thread;
	Pic bgd, start, levels, other;
	int WIDTH, HEIGHT;
	
	public theMenu(Context context) {
		super(context);
	    getHolder().addCallback(this);
        setFocusable(true);	
	}

	 @Override
    public void onSizeChanged (int w, int h, int oldw, int oldh) {
		//landscape
	    WIDTH = Math.max(w, h);
	    HEIGHT = Math.min(w, h);
	    setScale();
        super.onSizeChanged(w, h, oldw, oldh);
	}
	 
	void setScale() {
		bgd = new Pic(getResources(), R.drawable.menubgd, WIDTH, HEIGHT);
		bgd.X = bgd.Y = 0;
		start = new Pic(getResources(), R.drawable.menustart, WIDTH / 2, HEIGHT / 8);
		start.X = WIDTH / 4;
		start.Y = (int) (HEIGHT * .4);
		levels = new Pic(getResources(), R.drawable.menulevel, WIDTH / 2, HEIGHT / 8);
		levels.X = WIDTH / 4;
		levels.Y = (int) (HEIGHT * .6);
		other = new Pic(getResources(), R.drawable.menuother, WIDTH / 2, HEIGHT / 8);
		other.X = WIDTH / 4;
		other.Y = (int) (HEIGHT * .8);
	}
	
	@Override
    public synchronized boolean onTouchEvent(MotionEvent me) {
		start.X = (int)me.getX();
		if (Within(other, (int)me.getX(), (int)me.getY())) {
			thread.setRunning(false);
			((Activity) getContext()).moveTaskToBack(true);
			((Activity) getContext()).finish();
		}
		else if (Within(levels, (int)me.getX(), (int)me.getY())) {
			thread.setRunning(false);
			((Activity) getContext()).startActivity(new Intent(getContext(), MainGame.class)
			.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
			//thread.setRunning(false);
		}
        return true;
    }
	
	boolean Within(Pic P, int x, int y) {
		if (x >= P.X && x <= P.X + P.B.getWidth() &&
			y >= P.Y && y <= P.Y + P.B.getHeight())
			return true;
		return false;
	}
	
	@Override
    public void onDraw(Canvas c) {

        c.drawBitmap(bgd.B, bgd.X, bgd.Y, null);
        c.drawBitmap(start.B, start.X, start.Y, null);
        c.drawBitmap(levels.B, levels.X, levels.Y, null);
        c.drawBitmap(other.B, other.X, other.Y, null);
        
        start.X++;
        //thread.update();
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		thread = new GameThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		thread.setRunning(false);
	}

} //end theMenu
