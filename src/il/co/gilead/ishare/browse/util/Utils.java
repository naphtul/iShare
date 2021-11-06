/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package il.co.gilead.ishare.browse.util;

import il.co.gilead.ishare.Deal;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.StrictMode;

/**
 * Class containing some static utility methods.
 */
public class Utils {
    private Utils() {};

    @TargetApi(11)
    public static void enableStrictMode() {
        if (Utils.hasGingerbread()) {
            StrictMode.ThreadPolicy.Builder threadPolicyBuilder =
                    new StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyLog();
            StrictMode.VmPolicy.Builder vmPolicyBuilder =
                    new StrictMode.VmPolicy.Builder()
                            .detectAll()
                            .penaltyLog();

            if (Utils.hasHoneycomb()) {
                threadPolicyBuilder.penaltyFlashScreen();
            }
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }

    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }
    
	public static ArrayList<Deal> createDealsList(String response) {
		ArrayList<Deal> list = new ArrayList<Deal>();
		
		if (response == null || response == "" || 
				response.startsWith("Exception") ||
				response.startsWith("{\"pictures\":[]}"))
			return list;
		else
			try {
				JSONArray jsonResponse = new JSONArray(response);
				for (int i=0; i<jsonResponse.length(); i++){
					JSONObject jsonDeal = jsonResponse.getJSONObject(i);
					int dealId = jsonDeal.getInt("id");
					String dealDesc = jsonDeal.getString("description");
					String dealAddr = jsonDeal.getString("address");
					String dealBusiness = jsonDeal.getString("business");
					Integer picId = jsonDeal.getInt("pictureid");
					Float dealRating = (float) jsonDeal.getDouble("avg_rating");
					Integer categoryId = jsonDeal.getInt("categoryid");
					String providerId = jsonDeal.getString("providerid");
					Float distance = (float) jsonDeal.getDouble("distance");
					Deal deal = new Deal(dealId, dealDesc, dealAddr, dealBusiness, picId,
							dealRating, categoryId, providerId, distance);
					list.add(deal);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

		return list;
	}

}
