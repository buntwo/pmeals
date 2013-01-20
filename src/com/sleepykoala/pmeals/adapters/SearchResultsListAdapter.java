package com.sleepykoala.pmeals.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.PMealsDatabase;

public class SearchResultsListAdapter extends BaseAdapter {
	
	private Cursor results;
	private LayoutInflater mInflater;
	
	public SearchResultsListAdapter(Context context, Cursor c) {
		results = c;
		mInflater = ((Activity) context).getLayoutInflater();
	}

	public void swapCursor(Cursor c) {
		results = c;
		notifyDataSetChanged();
	}
	
	public int getCount() {
		if (results != null)
			return (results.getCount() == 0) ? 1 : results.getCount();
		else
			return 0;
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ResultHolder holder;
		if (convertView != null)
			holder = (ResultHolder) convertView.getTag();
		else {
			convertView = mInflater.inflate(R.layout.searchresult, parent, false);
			holder = new ResultHolder();
			holder.itemName = (TextView) convertView.findViewById(R.id.text1);
			holder.mealName = (TextView) convertView.findViewById(R.id.text2);
			holder.locationName = (TextView) convertView.findViewById(R.id.text3);
			holder.date = (TextView) convertView.findViewById(R.id.text4);
			convertView.setTag(holder);
		}
		if (results.getCount() == 0) {
			holder.itemName.setText("No Results");
			holder.mealName.setText("");
			holder.locationName.setText("");
			holder.date.setText("");
		} else {
			results.moveToPosition(position);
			holder.itemName.setText(getItemName(results));
			holder.mealName.setText(getMealName(results));
			holder.locationName.setText(getLocName(results));
			holder.date.setText(getDate(results));
		}
		
		return convertView;
	}
	
	//---------------------------------------------CURSOR GETTERS-------------------------------------
	
	private String getItemName(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(PMealsDatabase.ITEMNAME));
	}
	
	private String getMealName(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(PMealsDatabase.MEALNAME));
	}
	
	private String getLocName(Cursor c) {
		// should be initialized
		try {
			return LocationProviderFactory.newLocationProvider().getById(
					Integer.valueOf(c.getString(c.getColumnIndexOrThrow(PMealsDatabase.LOCATIONID)))).nickname;
		} catch (NumberFormatException e) {
			return "";
		}
	}
	
	private String getDate(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(PMealsDatabase.DATE));
	}
	
	//---------------------------------------------STATIC HOLDER CLASS--------------------------------
	
	private static class ResultHolder {
		TextView itemName;
		TextView mealName;
		TextView locationName;
		TextView date;
	}

}
