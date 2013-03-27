package com.sleepykoala.pmeals.adapters;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.PMealsDB;

public class SearchResultsListAdapter extends BaseAdapter {
	
	//private static final String DATEFORMAT = "EEE, MMM d, yyyy";
	
	// double array, first index is type
	private ArrayList<String[]> data;
	private LayoutInflater mInflater;
	
	public SearchResultsListAdapter(Context context, Cursor c) {
		processCursor(c);
		mInflater = ((Activity) context).getLayoutInflater();
	}

	public void swapCursor(Cursor c) {
		processCursor(c);
		notifyDataSetChanged();
	}
	
	private void processCursor(Cursor c) {
		data = new ArrayList<String[]>();
		if (c == null) {
			return;
		} else if (c.getCount() == 0) {
			data.add(new String[]{"1", "No Results", "", ""});
			return;	
		}
		c.moveToFirst();
		Date curDate = new Date("01/01/1900");
		while (!c.isAfterLast()) {
			Date date = new Date(getDate(c));
			if (!date.equals(curDate)) {
				data.add(new String[]{"0", date.toStringPretty(true, false)});
				curDate = date;
			}
			String[] result = new String[]{"1", getItemName(c), getMealName(c),
					getLocName(c), getDate(c), String.valueOf(getLocID(c))};
			if (!isDiningHall(result[2]))
				result[2] = "";
			data.add(result);
			c.moveToNext();
		}
		return;
	}
	
	public int getCount() {
		return data.size();
	}

	public Object getItem(int position) {
		return null;
	}

	// return location ID of position
	public long getItemId(int position) {
		return 0;
	}
	
	// get location ID of position
	public int getLocId(int pos) {
		return Integer.valueOf(data.get(pos)[5]);
	}
	
	public int getViewTypeCount() {
		return 2;
	}
	
	// 0 - date
	// 1 - search result
	public int getItemViewType(int pos) {
		return Integer.valueOf(data.get(pos)[0]);
	}
	
	public String getDateString(int position) {
		return data.get(position)[4];
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		String[] result = data.get(position);
		int type = Integer.valueOf(result[0]);
		if (type == 1) { // menu item
			ResultHolder holder;
			if (convertView != null)
				holder = (ResultHolder) convertView.getTag();
			else {
				convertView = mInflater.inflate(R.layout.searchresult, parent, false);
				holder = new ResultHolder();
				holder.itemName = (TextView) convertView.findViewById(R.id.text1);
				holder.mealName = (TextView) convertView.findViewById(R.id.text2);
				holder.locationName = (TextView) convertView.findViewById(R.id.text3);
				convertView.setTag(holder);
			}
			holder.itemName.setText(result[1]);
			holder.mealName.setText(result[2]);
			holder.locationName.setText(result[3]);
		} else if (type == 0) { // date
			DateHolder holder;
			if (convertView != null)
				holder = (DateHolder) convertView.getTag();
			else {
				convertView = mInflater.inflate(R.layout.searchresult_date, parent, false);
				holder = new DateHolder();
				holder.date = (TextView) convertView.findViewById(R.id.result_date);
				convertView.setTag(holder);
			}
			holder.date.setText(result[1]);
		}

		return convertView;
	}
	
	/**
	 * Given a meal name, check if it is Breakfast, Lunch, or Dinner.
	 * This occurs exactly when the location is a dining hall, hence
	 * the method name.
	 * 
	 * @param mealName Meal name from location
	 * @return If name is Breakfast, Lunch, or Dinner
	 */
	private boolean isDiningHall(String mealName) {
		return (mealName.equals("Breakfast") || mealName.equals("Lunch")
				|| mealName.equals("Dinner"));
	}
	
	//---------------------------------------------CURSOR GETTERS-------------------------------------
	
	private String getItemName(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(PMealsDB.ITEMNAME));
	}
	
	private String getMealName(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(PMealsDB.MEALNAME));
	}
	
	private String getLocName(Cursor c) {
		// should be initialized
		try {
			return LocationProviderFactory.newLocationProvider().getById(getLocID(c)).nickname;
		} catch (NumberFormatException e) {
			return "";
		}
	}
	
	private int getLocID(Cursor c) {
		return Integer.valueOf(c.getString(c.getColumnIndexOrThrow(PMealsDB.LOCATIONID)));
	}
	
	private String getDate(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(PMealsDB.DATE));
	}
	
	//---------------------------------------------STATIC HOLDER CLASSES--------------------------------
	
	private static class DateHolder {
		TextView date;
	}
	
	private static class ResultHolder {
		TextView itemName;
		TextView mealName;
		TextView locationName;
	}

}
