package il.co.gilead.ishare;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.acra.ACRA;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;
import com.svenkapudija.imageresizer.ImageResizer;
import com.svenkapudija.imageresizer.operations.ImageRotation;

public class ShareDeal extends Activity implements ImageChooserListener {
	private static final int REQUEST_TAKE_PICTURE = 1;
	protected static final String TAG = "iShare";
	private int serverResponseCode = 0;
	private TextView messageText;
	private EditText myAddress, dealDescription, businessName, discount,
			expiration;
	private Button shareDealButton, selectImageButton;
	private ProgressDialog dialog = null;
	private ImageChooserManager imageChooserManager;
	private int chooserType;
	private String upLoadServerUri, uploadFilePath, mCurrentPhotoPath,
				uploadFileName, filePath = ""; 
	private Spinner categoriesSpinner;
//	private Integer category;
	private Location location;
	private Double lat = 0.0, lon = 0.0;
	private DatePickerDialog expirationDialog;
	private SimpleDateFormat dateFormatter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_deal);
        
		shareDealButton = (Button) findViewById(R.id.button_share_deal);
		selectImageButton = (Button) findViewById(R.id.selectImageButton);
		messageText = (TextView) findViewById(R.id.messageText);
		myAddress = (EditText) findViewById(R.id.address);
		dealDescription = (EditText) findViewById(R.id.deal_description);
		businessName = (EditText) findViewById(R.id.business_name);
		categoriesSpinner = (Spinner) findViewById(R.id.categories_spinner);
		discount = (EditText) findViewById(R.id.edtDiscount);
		expiration = (EditText) findViewById(R.id.edtExpiration);
		expiration.setFocusable(false);
		
		if (savedInstanceState == null){
			location = Splash.fusedLocationService.getLocation();
			myAddress.setText(getMyLocationAddress(location));
		}else{
			myAddress.setText(savedInstanceState.getString("addressText"));
			dealDescription.setText(savedInstanceState.getString("dealDescriptionText"));
			businessName.setText(savedInstanceState.getString("businessNameText"));
			filePath = savedInstanceState.getString("filePath");
			mCurrentPhotoPath = savedInstanceState.getString("mCurrentPhotoPath"); 
			lat = savedInstanceState.getDouble("lat");
			lon = savedInstanceState.getDouble("lon");
			discount.setText(savedInstanceState.getString("discount"));
		}

		removeAllFromCategories();
		
		// PHP script path
		upLoadServerUri = getString(R.string.baseServerName)+"share_deal.php";

		dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		selectImageButton.setOnClickListener (new OnClickListener() {

			@Override
			public void onClick(View v) {
				getImage(ChooserType.REQUEST_PICK_PICTURE);
			}
		});

		shareDealButton.setOnClickListener(new OnClickListener() {            
			@Override
			public void onClick(View v) {
				if (dealDescription.getText().length() == 0 || myAddress.getText().length() == 0){
					Log.e(TAG, "No data was filled in either Deal description, Business name or address.");
					messageText.setText(getString(R.string.mandatory_fields_missing));
					return;
				}
				if (filePath == null || filePath.isEmpty()){
					Log.e(TAG, "No valid picture was selected.");
					messageText.setText(getString(R.string.picture_not_selected));
					return;
				}
				lockScreenOrientation();
				messageText.setText(getString(R.string.sharing_started));
				showAddressConfirmationDialog();
			}
		});
		
		expiration.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				expirationDialog.show();
			}
		});
        
        Calendar newCalendar = Calendar.getInstance();
        newCalendar.add(Calendar.MONTH, 6);
        expiration.setText(dateFormatter.format(newCalendar.getTime()));
        expirationDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

        	@Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.add(Calendar.MONTH, 6);
                newDate.set(year, monthOfYear, dayOfMonth);
                expiration.setText(dateFormatter.format(newDate.getTime()));
            }
 
        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

		
		int selectedCategory = getIntent().getIntExtra("category", 0);
		if (selectedCategory == 0)
			selectedCategory = 1;
		categoriesSpinner.setSelection(selectedCategory);
	}

	private void removeAllFromCategories() {
		SpinnerAdapter sa = (SpinnerAdapter) categoriesSpinner.getAdapter();
		List<String> list = new ArrayList<String>();
		for (int i=0; i<sa.getCount(); i++){
			list.add((String) sa.getItem(i));
		}
		// Populate the spinner using a customized ArrayAdapter that hides the first (dummy) entry
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_spinner_item, list) {
		    @Override
		    public View getDropDownView(int position, View convertView, android.view.ViewGroup parent)
		    {
		        View v = null;

		        // If this is the initial dummy entry, make it hidden
		        if (position == 0) {
		            TextView tv = new TextView(getContext());
		            tv.setHeight(0);
		            tv.setVisibility(View.GONE);
		            v = tv;
		        }
		        else {
		            // Pass convertView as null to prevent reuse of special case views
		            v = super.getDropDownView(position, null, parent);
		        }

		        // Hide scroll bar because it appears sometimes unnecessarily, this does not prevent scrolling 
		        parent.setVerticalScrollBarEnabled(false);
		        return v;
		    }
		};

		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		categoriesSpinner.setAdapter(dataAdapter);
	}

	private void showAddressConfirmationDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		Add the Edit Text
		final EditText input = new EditText(this);
		input.setText(myAddress.getText().toString());
		input.selectAll();
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		input.setLayoutParams(lp);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		input.setFocusable(true);
		builder.setView(input);
		builder.setTitle(getString(R.string.verify_address_title));
		builder.setMessage(getString(R.string.verify_address));

//		Add the buttons
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
//				User clicked OK button
				myAddress.setText(input.getText().toString().trim());
//				dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//				dialog.dismiss();
				new UploadDeal().execute();
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
//				User cancelled the dialog
			}
		});

		// Create the AlertDialog
		AlertDialog dialog = builder.create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();
	}

	@Override
	public void onDestroy() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
 		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putInt("category", categoriesSpinner.getSelectedItemPosition());
		editor.commit();
		super.onDestroy();
	}

	private void getImage(int choice) {
		chooserType = choice;
		imageChooserManager = new ImageChooserManager(this, choice, false);
		imageChooserManager.setImageChooserListener(this);
		try {
			imageChooserManager.choose();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	public int uploadFile(String sourceFileUri) {
		if (sourceFileUri == null){
			Log.e("uploadFile", "Source picture doesn't exist.");
			runOnUiThread(new Runnable() {
				public void run() {
					messageText.setText(getString(R.string.source_not_exist));
				}
			});
			return 0;
		}

//		if (categoriesSpinner.getSelectedItem() != null)
//			category = categoriesSpinner.getSelectedItemId();
//		else
//			category = 9;

		String fileName = sourceFileUri;
		File selectedPicture = new File(fileName);
		ExifInterface exif;
		try {
			exif = new ExifInterface(fileName);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			ImageResizer.saveToFile(
					ImageResizer.resize(selectedPicture, 640, 480),
					selectedPicture);
			switch(orientation) {
			case ExifInterface.ORIENTATION_ROTATE_270:
				ImageResizer.saveToFile(
						ImageResizer.rotate(selectedPicture, ImageRotation.CW_270),
						selectedPicture);
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				ImageResizer.saveToFile(
						ImageResizer.rotate(selectedPicture, ImageRotation.CW_180),
						selectedPicture);
				break;
			case ExifInterface.ORIENTATION_ROTATE_90:
				ImageResizer.saveToFile(
						ImageResizer.rotate(selectedPicture, ImageRotation.CW_90),
						selectedPicture);
				break;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		HttpURLConnection conn = null;
		DataOutputStream dos = null;  
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024; 
		File sourceFile = new File(sourceFileUri); 

		if (!sourceFile.isFile()) {

			if (dialog != null)
				dialog.dismiss(); 

			Log.e("uploadFile", "Source picture doesn't exist: "
					+uploadFilePath + "" + uploadFileName);

			runOnUiThread(new Runnable() {
				public void run() {
					messageText.setText(getString(R.string.source_not_exist)
							+ " " +uploadFilePath + "" + uploadFileName);
				}
			}); 

			return 0;

		}
		else
		{
			try { 

				// open a URL connection to the Servlet
				FileInputStream fileInputStream = new FileInputStream(sourceFile);
				URL url = new URL(upLoadServerUri);

				// Open a HTTP  connection to  the URL
				conn = (HttpURLConnection) url.openConnection(); 
				conn.setDoInput(true); // Allow Inputs
				conn.setDoOutput(true); // Allow Outputs
				conn.setUseCaches(false); // Don't use a Cached Copy
				conn.setRequestMethod("POST");
				conn.setRequestProperty("userid", MainActivity.userid);
				conn.setRequestProperty("sso", MainActivity.sso);
				conn.setRequestProperty("email", MainActivity.email);
				conn.setRequestProperty("Connection", "Keep-Alive");
				conn.setRequestProperty("ENCTYPE", "multipart/form-data");
				conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
				conn.setRequestProperty("uploaded_file", fileName); 

				dos = new DataOutputStream(conn.getOutputStream());

				dos.writeBytes(twoHyphens + boundary + lineEnd); 
				dos.writeBytes("Content-Disposition: form-data; name=\"169f863f-1abe-444d-9852-8454abff478d\"; filename=\""
						+ fileName + "\"" + lineEnd);

				dos.writeBytes(lineEnd);

				// create a buffer of  maximum size
				bytesAvailable = fileInputStream.available(); 

				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				buffer = new byte[bufferSize];

				// read file and write it into form...
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);  

				while (bytesRead > 0) {

					dos.write(buffer, 0, bufferSize);
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);   

				}

				// send multipart form data necessary after file data...
				dos.writeBytes(lineEnd);
				dos.writeBytes(twoHyphens + boundary + lineEnd);
				dos.writeBytes("Content-Disposition: form-data; name=\"data\"" + lineEnd + lineEnd);
				int discnt = 0;
				if (discount.getText().length() > 0)
					try {
						discnt = Integer.parseInt(discount.getText().toString());
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				JSONObject data = new JSONObject();
				if (location != null){
					lat = location.getLatitude();
					lon = location.getLongitude();
				}
				try {
					data.put("business", businessName.getText().toString());
					data.put("description", dealDescription.getText().toString());
					data.put("category", categoriesSpinner.getSelectedItemId());
					data.put("address", myAddress.getText().toString());
					data.put("latitude", lat);
					data.put("longitude", lon);
					data.put("discount", discnt);
					data.put("expiration", expiration.getText().toString());
				} catch (JSONException e) {
					e.printStackTrace();
				}
				dos.writeBytes(URLEncoder.encode(data.toString(4), "utf-8"));
				dos.writeBytes(lineEnd + twoHyphens + boundary + twoHyphens + lineEnd);

				// Responses from the server (code and message)
				serverResponseCode = conn.getResponseCode();
				String serverResponseMessage = conn.getResponseMessage();

				Log.i(TAG, "HTTP Response is : " 
						+ serverResponseMessage + ": " + serverResponseCode);

				if(serverResponseCode == 200 && conn.getContentLength() == 0){
					runOnUiThread(new Runnable() {
						public void run() {
							messageText.setText(getString(R.string.sharing_complete));
						}
					});                
				}

				//close the streams //
				fileInputStream.close();
				dos.flush();
				dos.close();

			} catch (MalformedURLException ex) {

				if (dialog != null)
					dialog.dismiss();  
				ex.printStackTrace();

				runOnUiThread(new Runnable() {
					public void run() {
						messageText.setText(getString(R.string.sharing_complete_errors));
					}
				});

				Log.e("Upload file to server", "error: " + ex.getMessage(), ex);  
			} catch (Exception e) {

				if (dialog != null)
					dialog.dismiss();  
				e.printStackTrace();

				runOnUiThread(new Runnable() {
					public void run() {
						messageText.setText(getString(R.string.sharing_complete_errors));
					}
				});
				Log.e("Upload file to server Exception", "Exception : " 
						+ e.getMessage(), e);  
			}
			if (dialog != null)
				dialog.dismiss();
			unlockScreenOrientation();
			return serverResponseCode; 

		} // End else block 
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != MainActivity.RESULT_OK) return;
		switch (requestCode) {
		case ChooserType.REQUEST_PICK_PICTURE:
			if (imageChooserManager == null) {
				reinitializeImageChooser();
			}
			imageChooserManager.submit(requestCode, data);
			break;
		case REQUEST_TAKE_PICTURE:
			filePath = mCurrentPhotoPath;
			break;
		}
	}

	private File createImageFile() throws IOException {
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
	    String imageFileName = getString(R.string.album_name)+"_" + timeStamp + "_";
	    File storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
	            + "/Pictures/"+getString(R.string.album_name)+"/");
	    File image = File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */
	        storageDir      /* directory */
	    );

	    mCurrentPhotoPath = image.getAbsolutePath();
	    return image;
	}

	public void dispatchTakePictureIntent(View v) {
	    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    // Ensure that there's a camera activity to handle the intent
	    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
	        // Create the File where the photo should go
	        File photoFile = null;
	        try {
	            photoFile = createImageFile();
	        } catch (IOException ex) {
	            // Error occurred while creating the File
	        }
	        // Continue only if the File was successfully created
	        if (photoFile != null) {
	            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
	                    Uri.fromFile(photoFile));
	            startActivityForResult(takePictureIntent, REQUEST_TAKE_PICTURE);
	        }
	    }
	}
	
	@Override
	public void onImageChosen(final ChosenImage image) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (image != null) {
					filePath = image.getFilePathOriginal();
					uploadFileName = filePath.substring(filePath.lastIndexOf("/")+1);
					uploadFilePath = filePath.substring(0, filePath.lastIndexOf("/")+1);
					messageText.setText(getString(R.string.picture_selected)+" "+uploadFileName);
				}
			}
		});
	}

	@Override
	public void onError(String reason) {
		ACRA.getErrorReporter().putCustomData("reason", reason);
		ACRA.getErrorReporter().handleException(null);
	}

	public String getMyLocationAddress(Location location) {
		String addressText = getString(R.string.no_location);
		Geocoder geocoder = new Geocoder(this);
		try {
//			Place your latitude and longitude
			List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if(addresses != null) {
				Address fetchedAddress = addresses.get(0);
				StringBuilder strAddress = new StringBuilder();
				for(int i=0; i<fetchedAddress.getMaxAddressLineIndex(); i++) {
					strAddress.append(fetchedAddress.getAddressLine(i)).append(" ");
				}
				addressText = strAddress.toString().trim();
			}
		} catch (IOException e) {
			e.printStackTrace();
//			Toast.makeText(getApplicationContext(),"Could not get address..!", Toast.LENGTH_LONG).show();
		} catch (NullPointerException e) {
			e.printStackTrace();
//			Toast.makeText(getApplicationContext(),"Could not get address..!", Toast.LENGTH_LONG).show();
		}
		return addressText;
	}

	private void lockScreenOrientation() {
	    int currentOrientation = getResources().getConfiguration().orientation;
	    if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    } else {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	    }
	}
	 
	private void unlockScreenOrientation() {
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}

	// Should be called if for some reason the ImageChooserManager is null (Due
	// to destroying of activity for low memory situations)
	private void reinitializeImageChooser() {
		imageChooserManager = new ImageChooserManager(this, chooserType, false);
		imageChooserManager.setImageChooserListener(this);
		imageChooserManager.reinitialize(filePath);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		savedState.putString("addressText", myAddress.getText().toString());
		savedState.putString("dealDescriptionText", dealDescription.getText().toString());
		savedState.putString("businessNameText", businessName.getText().toString());
		savedState.putString("filePath", filePath);
		savedState.putString("mCurrentPhotoPath", mCurrentPhotoPath);
		savedState.putDouble("lat", lat);
		savedState.putDouble("lon", lon);
		savedState.putString("discount", discount.getText().toString());
	}

	private class UploadDeal extends AsyncTask<Void, Void, Integer>{

		@Override
		protected void onPreExecute() {
			dialog = ProgressDialog.show(ShareDeal.this, "", getString(R.string.sharing_deal), true);
		}

		@Override
		protected Integer doInBackground(Void... params) {
			return uploadFile(filePath);
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (dialog != null)
				dialog.dismiss();
			if (result == 200)
				messageText.setText(getString(R.string.sharing_complete));
			else
				messageText.setText(getString(R.string.sharing_complete_errors));
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		}
	}
}