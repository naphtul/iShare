package il.co.gilead.ishare;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class DealsArrayAdapter extends ArrayAdapter<Deal> implements Filterable {
	private final Activity context;
	private ArrayList<Deal> dealsList, backupDealsList;
	public static ArrayList<Deal> externalList;
	private FriendsFilter friendsFilter;

	static class ViewHolder {
		public TextView text, textA, textB, textC;
		public ImageView image;
	}

	public DealsArrayAdapter(Activity context, ArrayList<Deal> dealsList) {
		super(context, R.layout.adapter_browse_deals, dealsList);
		this.context = context;
		this.dealsList = dealsList;
		this.backupDealsList = dealsList;
		externalList = dealsList;
		getFilter();
	}
	
	//How many items are in the data set represented by this Adapter.
	@Override
	public int getCount() {
	    return dealsList.size();
	}

	//Get the data item associated with the specified position in the data set.
	@Override
	public Deal getItem(int position) {
	    return dealsList.get(position);
	}

	//Get the row id associated with the specified position in the list.
	@Override
	public long getItemId(int position) {
	    return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.adapter_browse_deals, null);
			
			ViewHolder viewHolder = new ViewHolder();
//			viewHolder.text = (TextView) rowView.findViewById(R.id.label);
			viewHolder.textA = (TextView) rowView.findViewById(R.id.line_a);
			viewHolder.textB = (TextView) rowView.findViewById(R.id.line_b);
			viewHolder.textC = (TextView) rowView.findViewById(R.id.line_c);
			viewHolder.image = (ImageView) rowView.findViewById(R.id.thumb);
			rowView.setTag(viewHolder);
		}

		ViewHolder holder = (ViewHolder) rowView.getTag();
		holder.textA.setText(dealsList.get(position).getDealDescription());
		holder.textB.setText(dealsList.get(position).getBusinessName());
		holder.textC.setText(dealsList.get(position).getDistance().toString()+
				context.getString(R.string.distance_unit));
		Integer p = dealsList.get(position).getPictureId();
		String pictureUrl = context.getString(R.string.baseServerName) + 
				"retrieve_picture.php?dimensions=300&file="+p;
		loadImage(context, pictureUrl, holder.image);

		return rowView;
	}
	
	//Returns a filter that can be used to constrain data with a filtering pattern.
	@Override
	public Filter getFilter() {
	    if(friendsFilter==null) {
	        friendsFilter = new FriendsFilter();
	    }
	    return friendsFilter;
	}

	private void loadImage(Context context, String imageUrl, ImageView iv){
		/*
		Picasso.Builder builder = new Picasso.Builder(context);
		builder.downloader(new UrlConnectionDownloader(context) {
			@Override
			protected HttpURLConnection openConnection(Uri path) throws IOException{
				HttpURLConnection c = super.openConnection(path);
				c.setRequestProperty("userid", MainActivity.userid);
				c.setRequestProperty("sso", MainActivity.sso);
				c.setRequestProperty("email", MainActivity.email);
				return c;
			}
		});
		Picasso picasso = builder.build();
		picasso.load(imageUrl).resize(100,100).centerCrop().into(iv);
		*/
		Glide.with(context)
		.load(imageUrl)
//		.override(100,100)
		.centerCrop()
		.into(iv);
	}


	private class FriendsFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (constraint == "1") {
				ArrayList<Deal> filteredList = new ArrayList<Deal>();
				for (int i=0; i<dealsList.size(); i++){
					if (MainActivity.friendsList.contains(dealsList.get(i).getDealProviderId())){
						filteredList.add(dealsList.get(i));
					}
				}
				results.count = filteredList.size();
				results.values = filteredList;
			} else {
				results.count = backupDealsList.size();
				results.values = backupDealsList;
			}
			return results;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			// TODO Auto-generated method stub
			dealsList = (ArrayList<Deal>) results.values;
			externalList = (ArrayList<Deal>) results.values;
			notifyDataSetChanged();
		}
		
	}
}