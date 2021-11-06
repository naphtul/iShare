package il.co.gilead.ishare;

import il.co.gilead.ishare.browse.util.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.acra.ACRA;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;

public class BrowseDeals extends Activity {
	private DealsArrayAdapter adapter;
	private ArrayList<Deal> dealsList;
	private String response;
	private CheckBox chkFriendsDeals;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browse_deals);
		ListView listView = (ListView) findViewById(R.id.listview);
		String url = "";
		Bundle extras = getIntent().getExtras();
		int selectedCategory = 0;
		int proximity = 0;
		String keyword = "";
		String discount = "";
		if (extras != null){
			selectedCategory = extras.getInt("category", 0);
			proximity = extras.getInt("proximity", 0);
			keyword = extras.getString("keyword", "");
			discount = extras.getString("discount", "");
		}
        if (extras.getParcelableArrayList("dealsList") != null){
        	dealsList = extras.getParcelableArrayList("dealsList");
        } else {
	//		Double lat = 0.0, lng = 0.0;
			if (FusedLocationService.lat == 0.0 & FusedLocationService.lng == 0.0){
				try {
					Location location = Splash.fusedLocationService.getLocation();
					FusedLocationService.lat = location.getLatitude();
					FusedLocationService.lng = location.getLongitude();
				} catch (NullPointerException e) {
					e.printStackTrace();
					ACRA.getErrorReporter().putCustomData("FusedLocationService.lat", FusedLocationService.lat.toString());
					ACRA.getErrorReporter().putCustomData("FusedLocationService.lng", FusedLocationService.lng.toString());
					ACRA.getErrorReporter().handleException(null);				
				}
			}
	//		String fbFriends = extras.getString("fbFriends");
	//		Boolean friendsFlag = extras.getBoolean("friendsFlag");
	//		if (!friendsFlag)
	//			fbFriends = "";
			try {
				url = getString(R.string.baseServerName) + "browse_deals.php?category="
						+ selectedCategory + "&proximity=" + proximity + "&lat="
						+ FusedLocationService.lat + "&long=" + FusedLocationService.lng
						+ "&discount=" + discount
	//					+ "&fbfriends=" + URLEncoder.encode(fbFriends, "utf-8")
						+ "&keyword=" + URLEncoder.encode(keyword, "utf-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
				ACRA.getErrorReporter().putCustomData("FusedLocationService.lat", FusedLocationService.lat.toString());
				ACRA.getErrorReporter().putCustomData("FusedLocationService.lng", FusedLocationService.lng.toString());
				ACRA.getErrorReporter().putCustomData("url", url);
				ACRA.getErrorReporter().putCustomData("keyword", keyword);
				ACRA.getErrorReporter().handleException(null);
			}
			try {
				response = (String) new GetHTTPResponse(getApplicationContext())
				.execute(url).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
	/*		
			try {
				response = getDealsList(url);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	*/
			dealsList = Utils.createDealsList(response);
        }
		adapter = new DealsArrayAdapter(this, dealsList);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
		        Intent i = new Intent(getApplicationContext(), DealDetailActivity.class);
		        i.putExtra("selectedDeal", position);
//		        i.putExtra("dealsListAsString", response);
		        i.putParcelableArrayListExtra("dealsList", DealsArrayAdapter.externalList);
		        startActivity(i);
			}
		});
//		setListAdapter(adapter);
		chkFriendsDeals = (CheckBox) findViewById(R.id.chkFriendsDeals);
		chkFriendsDeals.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked)
					adapter.getFilter().filter("1");
				else
					adapter.getFilter().filter("0");
			}
		});
	}
/*
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent(getApplicationContext(), DealDetailActivity.class);
        i.putExtra("SelectedDeal", position);
        i.putExtra("dealsListAsString", response);
        startActivity(i);
    }
*/
	
	private String getDealsList(String url) throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
			.url(url)
			.build();
		Response response = client.newCall(request).execute();
		return response.body().string();
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
//            	onBackPressed();
//                NavUtils.navigateUpFromSameTask(this);
//            	Intent i = new Intent();
//    			i = new Intent(this, BrowseDeals.class);
//            	Intent upIntent = new Intent(this, NavUtils.getParentActivityName(this));
//                NavUtils.navigateUpTo(this, NavUtils.getParentActivityIntent(this));
            	Intent i = new Intent(this, MainActivity.class);
//    			i = new Intent(this, BrowseDeals.class);
//    			i.putExtra("category", categoryId);
    			startActivity(i);
                return true;
//            case R.id.action_search:
//                Toast.makeText(
//                        this, R.string.search, Toast.LENGTH_SHORT).show();
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.browse_deals_menu, menu);
        
//        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
//        searchView.setIconifiedByDefault(false);
//        searchView.
        return super.onCreateOptionsMenu(menu);
    }

}