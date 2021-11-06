package il.co.gilead.ishare;

import java.util.concurrent.ExecutionException;

import il.co.gilead.ishare.BuyPicture;
import il.co.gilead.ishare.GetHTTPResponse;
import il.co.gilead.ishare.R;
import il.co.gilead.ishare.browse.util.ImageFetcher;
import il.co.gilead.ishare.browse.util.ImageWorker;
import il.co.gilead.ishare.browse.util.Utils;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.ShareActionProvider;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;

/**
 * This fragment will populate the children of the ViewPager from {@link ImageDetailActivity}.
 */
public class DealDetailFragment extends Fragment {
    private Float mRating, mDistance;
	private String mDealDesc, mDealAddress, mBusinessName;
    private Integer dealId, pictureId, mDealCategory;
    private String baseUrl = "http://omw-naphtul.rhcloud.com/retrieve_picture.php?dimensions=1024&file=";
    private ImageView mImageView;
    private ImageFetcher mImageFetcher;
    private Context context;
    private TextView dealDetails;
    private Button navTo;
    private RatingBar ratingBar;
    private ShareActionProvider mShareActionProvider;
    
    /**
     * Factory method to generate a new instance of the fragment given an image number.
     *
     * @param deal The deal object to load
     * @param ratings 
     * @return A new instance of ImageDetailFragment with imageNum extras
     */
    public static DealDetailFragment newInstance(Deal deal) {
        final DealDetailFragment f = new DealDetailFragment();

        final Bundle args = new Bundle();
        args.putInt("DealID", deal.getDealId());
        args.putInt("PictureID", deal.getPictureId());
        args.putString("DealDescription", deal.getDealDescription());
        args.putString("BusinessName", deal.getBusinessName());
        args.putString("DealAddress", deal.getDealAddress());
        args.putFloat("DealRating", deal.getDealRating());
        args.putInt("DealCategory", deal.getDealCategory());
        args.putFloat("distance", deal.getDistance());
        f.setArguments(args);

        return f;
    }

    /**
     * Empty constructor as per the Fragment documentation
     */
    public DealDetailFragment() {}

    /**
     * Populate image using a url from extras, use the convenience factory method
     * {@link ImageDetailFragment#newInstance(Deal)} to create this fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        dealId = getArguments() != null ? getArguments().getInt("DealID") : null;
        pictureId = getArguments() != null ? getArguments().getInt("PictureID") : null;
        mDealDesc = getArguments() != null ? getArguments().getString("DealDescription") : null;
        mBusinessName = getArguments() != null ? getArguments().getString("BusinessName") : null;
        mDealAddress = getArguments() != null ? getArguments().getString("DealAddress") : null;
        mRating = getArguments() != null ? getArguments().getFloat("DealRating") : null;
        mDealCategory = getArguments() != null ? getArguments().getInt("DealCategory") : null;
        mDistance = getArguments() != null ? getArguments().getFloat("distance") : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate and locate the main ImageView
        final View v = inflater.inflate(R.layout.detail_fragment, container, false);
        context = v.getContext();
        dealDetails = (TextView) v.findViewById(R.id.textView);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        navTo = (Button) v.findViewById(R.id.navigateTo);
        ratingBar = (RatingBar) v.findViewById(R.id.ratingBar);
        navTo.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Uri addr = Uri.parse("geo:0,0?q="+Uri.encode(mDealAddress));
				showMap(addr);
			}
		});
        registerForContextMenu(mImageView);
        ratingBar.setRating(mRating);
        ratingBar.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
			
			@Override
			public void onRatingChanged(RatingBar ratingBar, float rating,
					boolean fromUser) {
				rateDeal(rating);
				// TODO Auto-generated method stub
				
			}
		});
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Use the parent activity to load the image asynchronously into the ImageView (so a single
        // cache can be used over all pages in the ViewPager
        if (DealDetailActivity.class.isInstance(getActivity())) {
            mImageFetcher = ((DealDetailActivity) getActivity()).getImageFetcher();
            mImageFetcher.loadImage(baseUrl+pictureId.toString(), mImageView);
            dealDetails.setText(
            		mDealDesc+"\r\n"
            		+mDealAddress+"\r\n"
            		+mBusinessName+"\r\n"
            		+getString(R.string.average_rating)+" "+mRating.toString()+"\r\n"
            		+mDistance+getString(R.string.distance_unit)+"\r\n"
            		);
        }

        // Pass clicks on the ImageView to the parent activity to handle
        if (OnClickListener.class.isInstance(getActivity()) && Utils.hasHoneycomb()) {
            mImageView.setOnClickListener((OnClickListener) getActivity());
        }
    }

	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(R.string.rate);
		menu.add(1, Menu.NONE, 1, "*");
		menu.add(1, Menu.NONE, 2, "**");
		menu.add(1, Menu.NONE, 3, "***");
		menu.add(1, Menu.NONE, 4, "****");
		menu.add(1, Menu.NONE, 5, "*****");
//		menu.add(1, Menu.NONE, 6, "Buy");
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		if (item.getGroupId() == 1){		
			switch (item.getOrder()) {
			case 1:
				rateDeal(1);
				return true;
			case 2:
				rateDeal(2);
				return true;
			case 3:
				rateDeal(3);
				return true;
			case 4:
				rateDeal(4);
				return true;
			case 5:
				rateDeal(5);
				return true;
            case 6:
            	Intent intent = new Intent(this.getActivity(), BuyPicture.class);
            	intent.putExtra("pictureid", pictureId);
            	intent.putExtra("provider", "PP");
            	startActivity(intent);
            	return true;
			}
		    return super.onContextItemSelected(item);
		}
		return false;
	}

	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	Intent sendIntent = new Intent();
    	sendIntent.setAction(Intent.ACTION_SEND);
    	String shareText = "iShare My Way\r\nhttp://omw-naphtul.rhcloud.com/ishare.html?id=" +
    			dealId + "&cat=" + mDealCategory;
    	sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
    	sendIntent.setType("text/plain");
        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
    	mShareActionProvider.setShareIntent(sendIntent);

        super.onCreateOptionsMenu(menu, inflater);
    }

    private String rateDeal(float i) {
		String url = getString(R.string.baseServerName)+"rate_deal.php";
		url += "?deal_id="+dealId;
		url += "&rating="+i;
		String response = null;

		try {
			response = (String) new GetHTTPResponse(context)
			.execute(url).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		return response;
		// TODO Display the user with the response to the rating action (successful or not).
		
	}

	@Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
            // Cancel any pending image work
            ImageWorker.cancelWork(mImageView);
            mImageView.setImageDrawable(null);
        }
    }
	
	public void showMap(Uri geoLocation) {
	    Intent intent = new Intent(Intent.ACTION_VIEW, geoLocation);
	    intent.setData(geoLocation);
	    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
	        startActivity(intent);
	    }
	}
}
