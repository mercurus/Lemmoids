package blakmilk.lemmus;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread{

	long now;
    int framesCount = 0;
    int framesCountAvg = 0;
    long framesTimer = 0;
    long timeNow;
    long timePrev = 0;
    long timePrevFrame = 0;
    long timeDelta;
    int w = 23; //16 = about 60 fps
    
    private Canvas c;
    private SurfaceHolder surfaceHolder;
    private theMenu MENU;
    private theGame GAME;
    private boolean run = false;

    public GameThread(SurfaceHolder s, theMenu M) {
        this.surfaceHolder = s;
        this.MENU = M;
    }
    
    public GameThread(SurfaceHolder s, theGame G) {
        this.surfaceHolder = s;
        this.GAME = G;
    }

    public void setRunning(boolean run) {
        this.run = run;
    }

    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }
    
    public void updateFPS() {
    	now = System.currentTimeMillis();
        framesCount++;
        if(now-framesTimer>1000) {
                framesTimer=now;
                framesCountAvg=framesCount;
                framesCount=0;
        }
    }

    @Override
    public void run() {
        while (run) {
            c = null;

            //limit frame rate to max 60fps
            timeNow = System.currentTimeMillis();
            timeDelta = timeNow - timePrevFrame;
            if ( timeDelta < w) {
                try {
                    Thread.sleep(w - timeDelta);
                } catch(InterruptedException e) { }
            }
            timePrevFrame = System.currentTimeMillis();

            try {
                c = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                   //call methods to draw and process next fame
                	if (GAME != null)
                		GAME.onDraw(c);
                	else if (MENU != null)
                		MENU.onDraw(c);
                }
            } catch(NullPointerException e) {
            	//cry
            } finally {
                if (c != null)
                    surfaceHolder.unlockCanvasAndPost(c);
            }
        }
    }
} //end GameThread

