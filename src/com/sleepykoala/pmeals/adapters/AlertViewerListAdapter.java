package com.sleepykoala.pmeals.adapters;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;

public class AlertViewerListAdapter extends BaseAdapter {
	
	//private static final String TAG = "AlertViewerListAdapter";
	
	// parallel arrays
	private final ArrayList<Location> locs;
	private final ArrayList<Integer> itemsPerLoc;
	private final String mealName;
	private final ArrayList<String> dates;
	private final int numLocs;
	private final LayoutInflater mInflater;
	private final int ptonOrange;
	private final Date today;
	//
	private final ArrayList<String> itemNames;
	
	public AlertViewerListAdapter(Activity act, ArrayList<Location> aLocs,
			ArrayList<Integer> aItemsPerLoc, ArrayList<String> aItemNames,
			String aMealName, ArrayList<String> aDates) {
		mInflater = act.getLayoutInflater();
		locs = aLocs;
		itemsPerLoc = aItemsPerLoc;
		itemNames = aItemNames;
		numLocs = locs.size();
		mealName = aMealName;
		dates = aDates;
		ptonOrange = act.getResources().getColor(R.color.PrincetonOrange);
		today = new Date();
	}

	public int getCount() {
		int count = 0;
		for (int i : itemsPerLoc)
			count += i + 1;
		
		return count;
	}

	// get the location ID (Integer) the position belongs to
	public Object getItem(int position) {
		for (int i = 0; i < numLocs; ++i) {
			int numItems = itemsPerLoc.get(i);
			if (position - numItems - 1 < 0)
				return locs.get(i).ID;
			position -= numItems + 1;
		}
		return null;
	}

	public long getItemId(int position) {
		return 0;
	}

	public int getViewTypeCount() {
		return 2;
	}
	
	// 0 - loc name
	// 1 - alert item
	public int getItemViewType(int pos) {
		for (int i = 0; i < numLocs; ++i) {
			int numItems = itemsPerLoc.get(i);
			if (pos - numItems - 1 == -numItems - 1)
				return 0;
			else if (pos - numItems - 1 < 0)
				return 1;
			else
				pos -= numItems + 1;
		}
		return IGNORE_ITEM_VIEW_TYPE; // should never get here...
	}
	
	public View getView(int pos, View convertView, ViewGroup parent) {
		int type = -1;
		int itemIndex = 0;
		int locIndex;
		for (locIndex = 0; locIndex < numLocs; ++locIndex) {
			int numItems = itemsPerLoc.get(locIndex);
			if (pos - numItems - 1 == -numItems - 1) {
				type = 0;
				break;
			} else if (pos - numItems - 1 < 0) {
				type = 1;
				itemIndex += pos - 1;
				break;
			} else {
				pos -= numItems + 1;
				itemIndex += numItems;
			}
		}
		if (type == 0) { // location name
			LocationNameHolder holder;
			if (convertView != null)
				holder = (LocationNameHolder) convertView.getTag();
			else {
				convertView = mInflater.inflate(R.layout.menu_sectiontitle, parent, false);
				holder = new LocationNameHolder();
				holder.name = (TextView) convertView.findViewById(R.id.sectiontitle);
				holder.date = (TextView) convertView.findViewById(R.id.sectionextra);
				holder.date.setTextColor(ptonOrange);
				convertView.setTag(holder);
			}
			holder.name.setText(locs.get(locIndex).nickname);
			holder.date.setText(today.equals(new Date(dates.get(locIndex))) ? "Today" : "Tomorrow");
		} else if (type == 1) { // alert result
			ResultHolder holder;
			if (convertView != null)
				holder = (ResultHolder) convertView.getTag();
			else {
				convertView = mInflater.inflate(R.layout.searchresult, parent, false);
				convertView.findViewById(R.id.text3).setVisibility(View.GONE);
				holder = new ResultHolder();
				holder.itemName = (TextView) convertView.findViewById(R.id.text1);
				holder.mealName = (TextView) convertView.findViewById(R.id.text2);
				convertView.setTag(holder);
			}
			holder.itemName.setText(itemNames.get(itemIndex));
			if (LocationProvider.isDiningHall(locs.get(locIndex))) {
				holder.mealName.setText(mealName);
				holder.mealName.setVisibility(View.VISIBLE);
			} else {
				holder.mealName.setText("");
				holder.mealName.setVisibility(View.GONE);
			}
		}
		
		return convertView;
	}
	
	/**
	 * Given the locationId of a location this adapter is showing, return the
	 * date (as a string) that this location's meals are on
	 * 
	 * @param locId Location ID
	 * @return Date string of this location's meals, or null
	 */
	public String getDate(int locId) {
		for (int i = 0; i < numLocs; ++i) {
			if (locs.get(i).ID == locId)
				return dates.get(i);
		}
		return null;
	}

	//---------------------------------------------STATIC HOLDER CLASSES--------------------------------
	
	private static class LocationNameHolder {
		TextView name;
		TextView date;
	}
	
	private static class ResultHolder {
		TextView itemName;
		TextView mealName;
	}

}
