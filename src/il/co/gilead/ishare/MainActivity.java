package il.co.gilead.ishare;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.UserInfoChangedCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.plus.People.LoadPeopleResult;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;

public class MainActivity extends Activity implements OnClickListener,
ConnectionCallbacks, OnConnectionFailedListener, ResultCallback<LoadPeopleResult> {

	private LoginButton loginBtn;
	private UiLifecycleHelper uiHelper;
	private static final List<String> PERMISSIONS = Arrays.asList("email", "user_friends");
	private Session fb_sess;
	private static final int RC_SIGN_IN = 0;
	// Profile pic image size in pixels
	private static final int PROFILE_PIC_SIZE = 100;
	protected static final String TAG = "iShare";
	// Google client to interact with Google API
	private GoogleApiClient mGoogleApiClient;
	/**
	 * A flag indicating that a PendingIntent is in progress and prevents us
	 * from starting further intents.
	 */
	private boolean mIntentInProgress;
	private boolean mSignInClicked;
	private ConnectionResult mConnectionResult;
	private SignInButton btnSignIn;
	private Button btnSignOut, btnUploadToServer, btnBrowsePictures,
			btnShowHideSearchOptions;
	private Spinner categoriesSpinner, proximitySpinner;
	private ImageView imgProfilePic;
	private TextView txtName, txtEmail;
	private LinearLayout llProfileLayout, llSignedIn, llSearchOptions;
	public static String userid, sso, email;
	private String personPhotoUrl;
	private EditText keyword, discount;
	private Intent intent;
//	private String fbFriends = "";
//	private CheckBox chkFriendsDeals;
	public static List<String> friendsList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		uiHelper = new UiLifecycleHelper(this, statusCallback);
		uiHelper.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);

		getAlbumDir();
		friendsList = new ArrayList<String>();
		
		// General button and such - initialization
		llSignedIn = (LinearLayout) findViewById(R.id.llSignedIn);
		llSearchOptions = (LinearLayout) findViewById(R.id.ll_search_options);
		btnUploadToServer = (Button) findViewById(R.id.button_share_deal);
		btnBrowsePictures = (Button) findViewById(R.id.button_browse_deals);
		categoriesSpinner = (Spinner) findViewById(R.id.categories_spinner);
		proximitySpinner = (Spinner) findViewById(R.id.proximity_spinner);
		btnShowHideSearchOptions = (Button) findViewById(R.id.button_show_search_options);
		keyword = (EditText) findViewById(R.id.edtKeyword);
		discount = (EditText) findViewById(R.id.edtDiscount);
//		chkFriendsDeals = (CheckBox) findViewById(R.id.chkFriendsDeals);

		if (savedInstanceState != null){
			keyword.setText(savedInstanceState.getString("keyword"));
			discount.setText(savedInstanceState.getString("discount"));
//			fbFriends = savedInstanceState.getString("fbFriends");
		}

		// Google Sign In
		btnSignIn = (SignInButton) findViewById(R.id.btn_sign_in);
		btnSignOut = (Button) findViewById(R.id.btn_sign_out);
//		btnRevokeAccess = (Button) findViewById(R.id.btn_revoke_access);
		imgProfilePic = (ImageView) findViewById(R.id.imgProfilePic);
		txtEmail = (TextView) findViewById(R.id.txtEmail);
		txtName = (TextView) findViewById(R.id.txtName);
		llProfileLayout = (LinearLayout) findViewById(R.id.llProfile);

		// Facebook Login
		loginBtn = (LoginButton) findViewById(R.id.fb_login_button);
		loginBtn.setReadPermissions(PERMISSIONS);
		loginBtn.setUserInfoChangedCallback(new UserInfoChangedCallback() {
			@Override
			public void onUserInfoFetched(GraphUser user) {
				if (user != null) {
					llProfileLayout.setVisibility(View.VISIBLE);
					txtName.setText("Hello " + user.getName());
					email = user.getProperty("email").toString();
					txtEmail.setText(email);
					sso = "FB";
					userid = user.getId();
					personPhotoUrl = "https://graph.facebook.com/"+ userid + "/picture?height="+
							PROFILE_PIC_SIZE+"&width="+PROFILE_PIC_SIZE;
					Glide.with(getApplicationContext())
					.load(personPhotoUrl)
					.into(imgProfilePic);
//					Picasso.with(getApplicationContext()).load(personPhotoUrl).into(imgProfilePic);
					btnSignIn.setVisibility(View.GONE);
					llSignedIn.setVisibility(View.VISIBLE);
					getFBFriendList();
				} else {
					txtName.setText(getString(R.string.not_logged_in_fb));
					btnSignIn.setVisibility(View.VISIBLE);
				}
			}
		});

		// Button click listeners
		btnSignIn.setOnClickListener(this);
		btnSignOut.setOnClickListener(this);
//		btnRevokeAccess.setOnClickListener(this);
		btnUploadToServer.setOnClickListener(this);
		btnBrowsePictures.setOnClickListener(this);
		btnShowHideSearchOptions.setOnClickListener(this);	

		mGoogleApiClient = new GoogleApiClient.Builder(this)
		.addConnectionCallbacks(this)
		.addOnConnectionFailedListener(this).addApi(Plus.API)
		.addScope(Plus.SCOPE_PLUS_LOGIN).build();
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		int selectedCategory = sharedPrefs.getInt("category", 0);
		categoriesSpinner.setSelection(selectedCategory);
		int selectedProximity = sharedPrefs.getInt("proximity", 0);
		proximitySpinner.setSelection(selectedProximity);
		
//		Disable the Glide cache - ONLY FOR DEBUGGING!
//		new GlideBuilder(getApplicationContext())
//			.setDiskCache(DiskLruCacheWrapper.get(Glide.getPhotoCacheDir(getApplicationContext()), 0));
	}

	protected void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
	}

	protected void onStop() {
		super.onStop();
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	/**
	  Method to resolve any signin errors
	  */
	private void resolveSignInError() {
		if (mConnectionResult.hasResolution()) {
			try {
				mIntentInProgress = true;
				mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
			} catch (SendIntentException e) {
				mIntentInProgress = false;
				mGoogleApiClient.connect();
			}
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (!result.hasResolution()) {
			GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this,
					0).show();
			return;
		}

		if (!mIntentInProgress) {
			// Store the ConnectionResult for later usage
			mConnectionResult = result;

			if (mSignInClicked) {
				// The user has already clicked 'sign-in' so we attempt to
				// resolve all
				// errors until the user is signed in, or they cancel.
				resolveSignInError();
			}
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int responseCode,
			Intent intent) {
		super.onActivityResult(requestCode, responseCode, intent);
		uiHelper.onActivityResult(requestCode, responseCode, intent);

		if (requestCode == RC_SIGN_IN) {
			if (responseCode != RESULT_OK) {
				mSignInClicked = false;
			}

			mIntentInProgress = false;

			if (!mGoogleApiClient.isConnecting()) {
				mGoogleApiClient.connect();
			}
		}
	}

	@Override
	public void onConnected(Bundle arg0) {
		mSignInClicked = false;

		// Get user's information
		getProfileInformation();

		// Update the UI after signin
		updateUI(true);
		if (friendsList.size() > 0) return;
		Plus.PeopleApi.loadVisible(mGoogleApiClient, null).setResultCallback(this);
	}

	/**
	 * Updating the UI, showing/hiding buttons and profile layout
	 * */
	private void updateUI(boolean isSignedIn) {
		if (isSignedIn) {
			btnSignIn.setVisibility(View.GONE);
			btnSignOut.setVisibility(View.VISIBLE);
//			btnRevokeAccess.setVisibility(View.VISIBLE);
			llSignedIn.setVisibility(View.VISIBLE);
			llProfileLayout.setVisibility(View.VISIBLE);
			loginBtn.setVisibility(View.GONE);
		} else {
			btnSignIn.setVisibility(View.VISIBLE);
			btnSignOut.setVisibility(View.GONE);
//			btnRevokeAccess.setVisibility(View.GONE);
			llSignedIn.setVisibility(View.GONE);
			llProfileLayout.setVisibility(View.GONE);
			loginBtn.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Fetching user's information name, email, profile pic
	 * */
	private void getProfileInformation() {
		try {
			if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
				Person currentPerson = Plus.PeopleApi
						.getCurrentPerson(mGoogleApiClient);
				String personName = currentPerson.getDisplayName();
				personPhotoUrl = currentPerson.getImage().getUrl();
				email = Plus.AccountApi.getAccountName(mGoogleApiClient);
				sso = "G+";
				userid = email;
				
				// TODO: Close the buffer??
				userid = currentPerson.getId();
				

				Log.d(getString(R.string.app_name), "Name: " + personName + ", email: " + email
						+ ", Image: " + personPhotoUrl);

				txtName.setText(personName);
				txtEmail.setText(email);

				// by default the profile url gives 50x50 px image only
				// we can replace the value with whatever dimension we want by
				// replacing sz=X
				personPhotoUrl = personPhotoUrl.substring(0,
						personPhotoUrl.length() - 2)
						+ PROFILE_PIC_SIZE;
				Log.d(getString(R.string.app_name), "personPhotoUrl: "+personPhotoUrl);
				Glide.with(getApplicationContext())
				.load(personPhotoUrl)
				.into(imgProfilePic);
//				Picasso.with(getApplicationContext()).load(personPhotoUrl).into(imgProfilePic);
			} else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.person_info_null), Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		mGoogleApiClient.connect();
		updateUI(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Button on click listener
	 * */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_sign_in:
			// Signin button clicked
			signInWithGplus();
			break;
		case R.id.btn_sign_out:
			// Signout button clicked
			signOutFromGplus();
			break;
//		case R.id.btn_revoke_access:
			// Revoke access button clicked
//			revokeGplusAccess();
//			break;
		case R.id.button_share_deal:
			// Share Deal button clicked
			intent = new Intent(this, ShareDeal.class);
			intent.putExtra("category", categoriesSpinner.getSelectedItemPosition());
			startActivity(intent);
			break;
		case R.id.button_browse_deals:
			// Browse Deals button clicked
			int proximity;
			try {
				proximity = Integer.parseInt(proximitySpinner.getSelectedItem().toString());
			} catch (NumberFormatException e) {
				proximity = 0;
			}
			intent = new Intent(this, BrowseDeals.class);
			intent.putExtra("category", categoriesSpinner.getSelectedItemPosition());
			intent.putExtra("proximity", proximity);
			intent.putExtra("keyword", keyword.getText().toString());
			intent.putExtra("discount", discount.getText().toString());
//			intent.putExtra("fbFriends", fbFriends);
//			intent.putExtra("friendsFlag", chkFriendsDeals.isChecked());
			startActivity(intent);
			break;
		case R.id.button_show_search_options:
			toggleOptionsVisibility();
			break;
		}
	}

	private void getFBFriendList() {
		friendsList.clear();
		Session session = Session.getActiveSession();
//		if (fbFriends.length() > 0)
//			return;
		/* make the API call */
		new Request(
		    session,
		    "/me/friends",
		    null,
		    HttpMethod.GET,
		    new Request.Callback() {
		        public void onCompleted(Response response) {
		        	if (response.getGraphObject() != null) {
			            JSONObject fbResponse = response.getGraphObject().getInnerJSONObject();
			            try {
				            JSONArray fbFriendsList = (JSONArray) fbResponse.get("data");
				            for (int i=0; i<fbFriendsList.length(); i++){
				            	JSONObject friendObject = fbFriendsList.getJSONObject(i);
//				            	fbFriends += "'" + friendObject.get("id").toString() + "',";
				            	friendsList.add(friendObject.get("id").toString());
			            }
						} catch (JSONException e) {
							e.printStackTrace();
						}
//			            fbFriends = fbFriends.substring(0, fbFriends.length()-1);
		        	}
		        }
		    }
		).executeAsync();		
	}

	private void toggleOptionsVisibility() {
		int visibility = llSearchOptions.getVisibility();
		if (visibility == View.VISIBLE){
			llSearchOptions.setVisibility(View.GONE);
			btnShowHideSearchOptions.setText(R.string.show_search_options);
		}
		if (visibility == View.GONE){
			llSearchOptions.setVisibility(View.VISIBLE);
			btnShowHideSearchOptions.setText(R.string.hide_search_options);
		}
	}

	/**
	 * Sign-in into google
	 * */
	private void signInWithGplus() {
		if (!mGoogleApiClient.isConnecting()) {
			mSignInClicked = true;
			resolveSignInError();
		}
	}

	/**
	 * Sign-out from google
	 * */
	private void signOutFromGplus() {
		if (mGoogleApiClient.isConnected()) {
			Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
			mGoogleApiClient.disconnect();
			mGoogleApiClient.connect();
			updateUI(false);
			userid = "";
			sso = "";
			email = "";
			imgProfilePic.setImageResource(android.R.color.transparent);
			friendsList.clear();
		}
	}

	/**
	 * Revoking access from google
	 * */
	/*
	private void revokeGplusAccess() {
		if (mGoogleApiClient.isConnected()) {
			Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
			Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
			.setResultCallback(new ResultCallback<Status>() {
				@Override
				public void onResult(Status arg0) {
					Log.d(getString(R.string.app_name), "User access revoked!");
					mGoogleApiClient.connect();
					updateUI(false);
				}

			});
		}
	}
*/
	private Session.StatusCallback statusCallback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			onSessionStateChange(session, state, exception);
			if (state.isOpened()) {
				Log.d(getString(R.string.app_name), "Facebook session opened");
				
			} else if (state.isClosed()) {
				Log.d(getString(R.string.app_name), "Facebook session closed");
				txtName.setText("");
				llProfileLayout.setVisibility(View.GONE);
				llSignedIn.setVisibility(View.GONE);
				userid = "";
				sso = "";
				email = "";
				if (fb_sess != null)
					fb_sess.close();
				friendsList.clear();
			}
		}
	};

	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
	    if (state.isOpened()) {
	        Log.i(TAG, "Logged in...");
	    } else if (state.isClosed()) {
	    Log.i(TAG, "Logged out...");
	  }
	}

	private File getAlbumDir() {
		File storageDir = null;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
					+ "/Pictures/"+getString(R.string.album_name)+"/");
			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.d(getString(R.string.app_name), "failed to create directory");
						return getFilesDir();
					}
				}
			}
		} else {
			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}
		return storageDir;
	}

	@Override
	public void onResume() {
		super.onResume();
		Session session = Session.getActiveSession();
		if (session != null && (session.isOpened() || session.isClosed()) ) {
			onSessionStateChange(session, session.getState(), null);
		}
		uiHelper.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
 		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putInt("category", categoriesSpinner.getSelectedItemPosition());
		editor.putInt("proximity", proximitySpinner.getSelectedItemPosition());
		editor.commit();
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		uiHelper.onSaveInstanceState(savedState);
		savedState.putString("keyword", keyword.getText().toString());
		savedState.putString("discount", discount.getText().toString());
//		savedState.putString("fbFriends", fbFriends);
	}

	@Override
	public void onResult(LoadPeopleResult peopleData) {
		if (peopleData.getStatus().getStatusCode() == CommonStatusCodes.SUCCESS) {
			PersonBuffer personBuffer = peopleData.getPersonBuffer();
			try {
				int count = personBuffer.getCount();
				for (int i = 0; i < count; i++) {
//					Log.d(TAG, "Display name: " + personBuffer.get(i).getDisplayName() +
//						" ID: " + personBuffer.get(i).getId() );
					friendsList.add(personBuffer.get(i).getId());
				}
			} finally {
				personBuffer.close();
			}
		} else {
			Log.e(TAG, "Error requesting visible circles: " + peopleData.getStatus());
		}
	}
}