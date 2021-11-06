package il.co.gilead.ishare;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class Splash extends Activity {
	public static FusedLocationService fusedLocationService;
	private static String TAG = Splash.class.getName();
	private static long SLEEP_TIME = 2;    // Sleep time in seconds

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);    // Removes title bar
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);    // Removes notification bar

		setContentView(R.layout.activity_splash);

        //show error dialog if GoolglePlayServices not available
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }

        fusedLocationService = new FusedLocationService(this);

//		Start timer and launch main activity
		IntentLauncher launcher = new IntentLauncher();
		launcher.start();
	}

	private class IntentLauncher extends Thread {
		
		/**
		* Sleep for some time and then start the main activity.
		*/
		@Override
		public void run() {
			try {
				// Sleeping
				Thread.sleep(SLEEP_TIME*1000);
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}

			// Find out if we came from a deep link or else from the main flow.
	        final Intent intent = getIntent();
	        Integer categoryId = 0;
	        Integer sharedDealId = 0;
            if (intent.getData() != null){
            	categoryId = Integer.parseInt(intent.getData().getQueryParameters("cat").get(0));
            	sharedDealId = Integer.parseInt(intent.getData().getQueryParameters("id").get(0));
            }
            if (sharedDealId > 0){
            	Intent deepLink = new Intent(getApplicationContext(), DealDetailActivity.class);
            	deepLink.putExtra("sharedDealId", sharedDealId);
            	deepLink.putExtra("categoryId", categoryId);
            	startActivity(deepLink);
            	finish();
            } else {
            // Start main activity
				Intent mainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(mainActivityIntent);
				finish();
            }
		}
	}
	
    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }
    
}
