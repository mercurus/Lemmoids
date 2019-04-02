package blakmilk.lemmus;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class Splash extends Activity {

	//used in MainMenu to open with the splash screen on restarts
	public static boolean restarted = false; 
	boolean running = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		Display display = getWindowManager().getDefaultDisplay(); 
//		int w = display.getWidth();
//		int h = display.getHeight();
//		Pic splash = new Pic(getResources(), R.drawable.splash, w, h);
		setContentView(R.layout.splash);
		ImageView imgSplash = (ImageView)findViewById(R.id.ivSplash);
		//imgSplash.setImageBitmap(splash.B);
		
        imgSplash.setOnClickListener(new OnClickListener(){
        	public void onClick(View v)	{
        		running = false;
        		startActivity(new Intent(Splash.this, MainMenu.class));
        		finish();
        	}
        });
        
        Thread splashTimer = new Thread() {
        	public void run() {
        		try {
        			int splashTimer = 0;
        			while (splashTimer < 500) {
        				sleep(100);
        				splashTimer += 100;
        			}
        			if (running) {
            			startActivity(new Intent(Splash.this, MainMenu.class));
            			finish();
        			}
        		}
        		catch (InterruptedException e) {
        			e.printStackTrace();
        		}
        	}
        };
        splashTimer.start();
	} //end onCreate
	
} //end class
