package il.co.gilead.ishare;

import android.os.Parcel;
import android.os.Parcelable;

public class Deal implements Parcelable {
	private int dealId;
	private String dealDescription;
	private String dealAddress;
	private String businessName;
	private Integer pictureId;
	private Float dealRating;
	private Integer dealCategory;
	private String dealProviderId;
	private Float distance;
	
	/**
	 * A Deal object
	 * 
	 * @param dealId
	 * @param dealDescription
	 * @param dealAddress
	 * @param businessName
	 * @param pictureId
	 * @param dealRating
	 * @param dealCategory
	 * @param dealProviderId
	 * @param distance
	 */
	public Deal(int dealId, String dealDescription, String dealAddress, 
			String businessName, Integer pictureId, Float dealRating,
			int selectedCategory, String dealProviderId, Float distance){
		super();
		this.dealId = dealId;
		this.dealDescription = dealDescription;
		this.dealAddress = dealAddress;
		this.businessName = businessName;
		this.pictureId = pictureId;
		this.dealRating = dealRating;
		this.dealCategory = selectedCategory;
		this.dealProviderId = dealProviderId;
		this.distance = distance;
	}
	
	public Deal(Parcel in){
		readFromParcel(in);
	}

	public int getDealId() {
		return dealId;
	}
	
	public void setDealId(int dealId) {
		this.dealId = dealId;
	}
	
	public String getDealDescription() {
		return dealDescription;
	}
	
	public void setDealDescription(String dealDescription) {
		this.dealDescription = dealDescription;
	}
	
	public String getDealAddress() {
		return dealAddress;
	}
	
	public void setDealAddress(String dealAddress) {
		this.dealAddress = dealAddress;
	}
	
	public String getBusinessName() {
		return businessName;
	}
	
	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}
	
	public Integer getPictureId() {
		return pictureId;
	}
	
	public void setPictureId(Integer pictureId) {
		this.pictureId = pictureId;
	}
	
	public Float getDealRating() {
		return dealRating;
	}
	
	public void setDealRating(Float dealRating) {
		this.dealRating = dealRating;
	}
	
	public int getDealCategory() {
		return dealCategory;
	}
	
	public void setDealCategory(int category) {
		this.dealCategory = category;
	}
	
	public String getDealProviderId() {
		return dealProviderId;
	}
	
	public void setDealProviderId(String providerId) {
		this.dealProviderId = providerId;
	}
	
	public Float getDistance() {
		return distance;
	}
	
	public void setDistance(Float distance) {
		this.distance = distance;
	}
	
	@Override
	public String toString() {
		return "{\"id\":"+dealId+",\"address\":\""+dealAddress+"\",\"business\":\""+businessName+
				"\",\"description\":\""+dealDescription+"\",\"pictureId\":\""+pictureId.toString()+
				"\",\"dealRating\":"+dealRating.toString()+"\",\"dealCategory\":"+dealCategory.toString()+
				"\",\"dealProviderId\":"+dealProviderId.toString()+"\",\"distance\":"+distance.toString()+"}";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(dealId);
		dest.writeString(dealDescription);
		dest.writeString(dealAddress);
		dest.writeString(businessName);
		dest.writeInt(pictureId);
		dest.writeFloat(dealRating);
		dest.writeInt(dealCategory);
		dest.writeString(dealProviderId);
		dest.writeFloat(distance);
	}

	private void readFromParcel(Parcel in) {
		dealId = in.readInt();
		dealDescription = in.readString();
		dealAddress = in.readString();
		businessName = in.readString();
		pictureId = in.readInt();
		dealRating = in.readFloat();
		dealCategory = in.readInt();
		dealProviderId = in.readString();
		distance = in.readFloat();
	}
	
	public static final Parcelable.Creator<Deal> CREATOR =
			new Parcelable.Creator<Deal>() {

				@Override
				public Deal createFromParcel(Parcel source) {
					return new Deal(source);
				}

				@Override
				public Deal[] newArray(int size) {
					return new Deal[size];
				}
			};
}
