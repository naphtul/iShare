package il.co.gilead.ishare;

import il.co.gilead.ishare.browse.util.ImageCache;
import il.co.gilead.ishare.browse.util.ImageFetcher;
import il.co.gilead.ishare.browse.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.acra.ACRA;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;

public class DealDetailActivity extends FragmentActivity implements OnClickListener {
    private static final String IMAGE_CACHE_DIR = "images";
    private ImagePagerAdapter mAdapter;
    private ImageFetcher mImageFetcher;
    private ViewPager mPager;
    private ArrayList<Deal> dealsList = new ArrayList<Deal>();
    private int categoryId = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Utils.enableStrictMode();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_pager);

        // Find out if we came from a deep link or through the normal flow.
        Integer dealPositionInDealsList = 0;
        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        if (extras.getParcelableArrayList("dealsList") != null){
        	dealsList = intent.getParcelableArrayListExtra("dealsList");
        	dealPositionInDealsList = extras.getInt("selectedDeal", 0);
        	categoryId = dealsList.get(dealPositionInDealsList).getDealCategory();
        } else {
        	Integer sharedDealId = extras.getInt("sharedDealId");
        	categoryId = extras.getInt("categoryId");
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
    		final String url=getString(R.string.baseServerName)
    				+ "browse_deals.php?category=" + categoryId 
    				+ "&lat=" + FusedLocationService.lat 
    				+ "&long=" + FusedLocationService.lng
    				;
    		String response = "";
//			try {
//				response = (String) new GetHTTPResponse(getApplicationContext())
//				.execute(url).get();
//			} catch (InterruptedException | ExecutionException e) {
//				e.printStackTrace();
//			}
    		try {
				response = getDealsList(url);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

    		dealsList = Utils.createDealsList(response);

        	for (int i=0; i<dealsList.size(); i++){
        		if (sharedDealId.equals(dealsList.get(i).getDealId())){
        			dealPositionInDealsList = i;
        			break;
        		}
        	}
        }
//        if (dealsList.get(dealPositionInDealsList) != null)
//        	categoryId = dealsList.get(dealPositionInDealsList).getDealCategory();

        // Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;

        // For this sample we'll use half of the longest width to resize our images. As the
        // image scaling ensures the image is larger than this, we should be left with a
        // resolution that is appropriate for both portrait and landscape. For best image quality
        // we shouldn't divide by 2, but this will use more memory and require a larger memory
        // cache.
        final int longest = (height > width ? height : width) / 2;

        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(this, longest);
        mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
        mImageFetcher.setImageFadeIn(false);

        // Set up ViewPager and backing adapter
        mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), dealsList.size());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setPageMargin((int) getResources().getDimension(R.dimen.image_detail_pager_margin));
        mPager.setOffscreenPageLimit(2);

        // Set up activity to go full screen
        getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);

        // Enable some additional newer visibility and ActionBar features to create a more
        // immersive photo viewing experience
        if (Utils.hasHoneycomb()) {
            final ActionBar actionBar = getActionBar();

            // Hide title text and set home as up
            if (actionBar != null){
	            actionBar.setDisplayShowTitleEnabled(false);
	            actionBar.setDisplayHomeAsUpEnabled(true);
            }

            // Hide and show the ActionBar as the visibility changes
            mPager.setOnSystemUiVisibilityChangeListener(
                    new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int vis) {
                            if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                                if (actionBar != null)
                                	actionBar.hide();
                            } else {
                                if (actionBar != null)
                                	actionBar.show();
                            }
                        }
                    });

            // Start low profile mode and hide ActionBar
            mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            if (actionBar != null)
            	actionBar.hide();
        }
        
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
        	final List<String> id = intent.getData().getQueryParameters("id");
        	if (id.size() > 0) {
        		for (int i=0; i < dealsList.size(); i++)
        			if (dealsList.get(i).getDealId() == Integer.parseInt(id.get(0))){
        				mPager.setCurrentItem(i);
        				break;
        			}
        	}
        }

        // Set the current item based on the extra passed in to this activity
        if (dealPositionInDealsList != -1) {
            mPager.setCurrentItem(dealPositionInDealsList);
        }
    }

	private String getDealsList(String url) throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
			.url(url)
			.build();
		Response response = client.newCall(request).execute();
		return response.body().string();
	}

	@Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	if (categoryId != 0){
//            		Intent upIntent = NavUtils.getParentActivityIntent(this);
//            		upIntent.putExtra("category", categoryId);
//            		NavUtils.navigateUpTo(this, upIntent);
	            	Intent i = new Intent();
	    			i = new Intent(this, BrowseDeals.class);
	    			i.putExtra("category", categoryId);
	    			i.putParcelableArrayListExtra("dealsList", dealsList);
	    			startActivity(i);
            	} else {
//            		onBackPressed();
            		NavUtils.navigateUpFromSameTask(this);
            	}
                return true;
//            case R.id.clear_cache:
//                mImageFetcher.clearCache();
//                Toast.makeText(
//                        this, R.string.clear_cache_complete_toast,Toast.LENGTH_SHORT).show();
//                return true;
//            case R.id.menu_item_share:

//            	startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share_with_friends)));
//            	return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.deal_details_menu, menu);

        return true;
    }

    /**
     * Called by the ViewPager child fragments to load images via the one ImageFetcher
     */
    public ImageFetcher getImageFetcher() {
        return mImageFetcher;
    }

    /**
     * The main adapter that backs the ViewPager. A subclass of FragmentStatePagerAdapter as there
     * could be a large number of items in the ViewPager and we don't want to retain them all in
     * memory at once but create/destroy them on the fly.
     */
    private class ImagePagerAdapter extends FragmentStatePagerAdapter {
        private final int mSize;

        public ImagePagerAdapter(FragmentManager fm, int size) {
            super(fm);
            mSize = size;
        }

        @Override
        public int getCount() {
            return mSize;
        }

        @Override
        public Fragment getItem(int position) {
//        	if (mDealID == 0)
//        		return DealDetailFragment.newInstance(mDealID);
//        	else
        		return DealDetailFragment.newInstance(dealsList.get(position));
        }
    }

    /**
     * Set on the ImageView in the ViewPager children fragments, to enable/disable low profile mode
     * when the ImageView is touched.
     */
    @TargetApi(11)
    @Override
    public void onClick(View v) {
        final int vis = mPager.getSystemUiVisibility();
        if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
            mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }
}
