package com.sleepykoala.pmeals;

import static com.sleepykoala.pmeals.data.C.COLOR_ERROR_ITEM;
import static com.sleepykoala.pmeals.data.C.COLOR_REGULAR_ITEM;
import static com.sleepykoala.pmeals.data.C.STRING_DOWNLOADING;
import static com.sleepykoala.pmeals.data.C.STRING_LOADINGDATA;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.DatedMealTime;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.PMealsDatabase;

// shows meal for one day for one location
public class LocationViewListAdapter extends BaseAdapter {

	private ArrayList<Cursor> data;
	private ArrayList<DatedMealTime> mealsToShow;
	
	private final LayoutInflater mInflater;
	
	private Date today;
	private Date dateShowing; // the date we are showing
	
	private final boolean isEmpty;
	private static boolean isDetailedDate = false;
	private static final String DATEFORMAT = "EEEE, MMM d, yyyy";
	
	public LocationViewListAdapter(Context context, ArrayList<DatedMealTime> daysMeals, Date d) {
		mealsToShow = daysMeals;
		mInflater = ((Activity) context).getLayoutInflater();
		
		// init data and ID array
		data = new ArrayList<Cursor>();
		int size = daysMeals.size();
		if (size == 0) {
			data.add(null);
			isEmpty = true;
		} else {
			for (int i = 0; i < size; ++i)
				data.add(null);
			isEmpty = false;
		}
		
		// set dates
		today = new Date();
		dateShowing = d;
	}
	
	// is it okay to start a refresh?
	// only if all cursors are nonzero
	// returns nonsense if we are already refreshing! adapter does not store refreshing state
	public boolean okayToRefresh() {
		if (isEmpty)
			return false;
		for (Cursor c : data) {
			if (c == null || c.getCount() == 0)
				return false;
		}
		return true;
	}
	
	public void toggleDateFormat() {
		isDetailedDate ^= true;
	}
	
	//------------------------------------------------BROADCAST RECEIVER CALLS---------------------------------------
	
	// called by broadcast receiver when we enter a new day
	public void newDay() {
		today = new Date();
		notifyDataSetChanged();
	}
	
	// swap in the cursor at the given ID
	public void swapCursor(Cursor c, int ID) {
		data.set(ID, c);
		notifyDataSetChanged();
	}
	
	//----------------------------------------------------------------------------------------------------------------
	
	public int getCount() {
		if (isEmpty) {
			return 2;
		} else {
			int count = 1;
			for (Cursor menu : data)
				count += 1 + (menu == null || menu.getCount() == 0 ? 1 : menu.getCount());
			return count;
		}
	}

	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getViewTypeCount() {
		return 4;
	}

	// 0 - location name
	// 1 - loading
	// 2 - menu item
	// 3 - date
	public int getItemViewType(int pos) {
		if (pos == 0) {
			return 3;
		} else {
			int counter = 1;
			if (isEmpty)
				return data.get(0) == null ? 1 : 2;
			else {
				for (Cursor c : data) {
					if (pos == counter)
						return 0;
					boolean loaded = c != null && c.getCount() != 0;
					counter += (loaded) ? c.getCount() : 1;
					if (pos <= counter)
						return (loaded) ? 2 : 1;
					++counter;
				}
				return IGNORE_ITEM_VIEW_TYPE; // should never get here...
			}
		}
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		int counter = 1;
		DatedMealTime meal = null;
		int itemType = 2;
		Cursor menu = null;
		if (position == 0) {
			itemType = 3;
		} else {
			if (isEmpty) {
				menu = data.get(0);
				itemType = menu == null ? 1 : 2;
			} else {
				for (int mealPos = 0; mealPos < mealsToShow.size(); ++mealPos) {
					meal = mealsToShow.get(mealPos);
					if (position == counter) {
						itemType = 0;
						break;
					}
					menu = data.get(mealPos);
					boolean loaded = menu != null && menu.getCount() != 0;
					counter += (loaded) ? menu.getCount() : 1;
					if (position <= counter) {
						itemType = (loaded) ? 2 : 1;
						break;
					}
					++counter;
				}
				// if type is menu item (2), counter now points to last menu item's position in that location
			}
		}
		
		if (itemType == 0) { // meal name
			MealNameHolder holder;
			if (convertView != null) {
				holder = (MealNameHolder) convertView.getTag();
			} else {
				convertView = (LinearLayout) mInflater.inflate(R.layout.menu_sectiontitle_nonclickable, parent, false);
				holder = new MealNameHolder();
				holder.name = (TextView) convertView.findViewById(R.id.sectiontitle);
				holder.extra = (TextView) convertView.findViewById(R.id.sectionextra);
				convertView.setTag(holder);
			}
			holder.name.setText(meal.mealName);
			// set meal times
			StringBuilder sb = new StringBuilder();
			sb.append(MealTimeProvider.getFormattedTime(meal.startTime));
			sb.append(" - ");
			sb.append(MealTimeProvider.getFormattedTime(meal.endTime));
			holder.extra.setText(sb);
		} else if (itemType == 1) { // loading
			LoadingHolder holder;
			if (convertView != null) {
				holder = (LoadingHolder) convertView.getTag();
			} else {
				convertView = (LinearLayout) mInflater.inflate(R.layout.menu_loading, parent, false);
				holder = new LoadingHolder();
				holder.text = (TextView) convertView.findViewById(R.id.loading_text);
				convertView.setTag(holder);
			}
			if (menu == null) // waiting for initial cursor
				holder.text.setText(STRING_LOADINGDATA);
			else // cursor has size 0, so the service is downloading the menu
				holder.text.setText(STRING_DOWNLOADING);
		} else if (itemType == 2) { // menu item
			MenuItemHolder holder;
			if (convertView != null) {
				holder = (MenuItemHolder) convertView.getTag();
			} else {
				convertView = (LinearLayout) mInflater.inflate(R.layout.menu_item, parent, false);
				holder = new MenuItemHolder();
				holder.item = (TextView) convertView.findViewById(R.id.itemname);
				holder.vegan_vegetarian = (ImageView) convertView.findViewById(R.id.vegan_vegetarian);
				holder.pork = (ImageView) convertView.findViewById(R.id.pork);
				holder.nuts = (ImageView) convertView.findViewById(R.id.nuts);
				convertView.setTag(holder);
			}
			// move cursor to the item we want
			menu.moveToPosition(menu.getCount() - counter + position - 1);
			holder.item.setText(getItemName(menu));
			if (getItemError(menu)) {
				holder.item.setTextColor(COLOR_ERROR_ITEM);
				holder.item.setGravity(Gravity.RIGHT);
			} else {
				holder.item.setTextColor(COLOR_REGULAR_ITEM);
				holder.item.setGravity(Gravity.LEFT);
			}
			// set food info indicators
			boolean[] info = getFoodInfo(menu);
			if (info[0])
				holder.vegan_vegetarian.setImageDrawable(ViewByMeal.vegan);
			else if (info[1])
				holder.vegan_vegetarian.setImageDrawable(ViewByMeal.vegetarian);
			else
				holder.vegan_vegetarian.setImageDrawable(null);
			if (info[2])
				holder.pork.setImageDrawable(ViewByMeal.pork);
			else
				holder.pork.setImageDrawable(null);
			if (info[3])
				holder.nuts.setImageDrawable(ViewByMeal.nuts);
			else
				holder.nuts.setImageDrawable(null);
		} else if (itemType == 3) { // date
			DateHolder holder;
			if (convertView != null) {
				holder = (DateHolder) convertView.getTag();
			} else {
				convertView = (LinearLayout) mInflater.inflate(R.layout.menu_header, parent, false);
				holder = new DateHolder();
				holder.date = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(holder);
			}		// set header text
			CharSequence text;
			if (!isDetailedDate) {
				if (today.equals(dateShowing))
					text = "Today";
				else if (today.isTomorrow(dateShowing))
					text = "Tomorrow";
				else if (today.isYesterday(dateShowing))
					text = "Yesterday";
				else
					text = DateFormat.format("EEEE", dateShowing.toMillis(true));
			} else {
				text = DateFormat.format(DATEFORMAT, dateShowing.toMillis(true));
			}
			holder.date.setText(text);
		}
		
		return convertView;
	}

	// get the name of the food item the cursor is pointing at
	private String getItemName(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(PMealsDatabase.ITEMNAME));
	}

	// get the error status of the food item the cursor is pointing at
	private boolean getItemError(Cursor c) {
		return c.getInt(c.getColumnIndexOrThrow(PMealsDatabase.ITEMERROR)) == 1 ? true : false;
	}

	/* food info getter
 	 * array is { isVegan, isVegetarian, hasPork, hasNuts, isEFriendly }
	 */
	private boolean[] getFoodInfo(Cursor c) {
		return new boolean[]{ c.getInt(c.getColumnIndexOrThrow(PMealsDatabase.ITEMVEGAN)) == 1 ? true : false,
				c.getInt(c.getColumnIndexOrThrow(PMealsDatabase.ITEMVEGETARIAN)) == 1 ? true : false,
				c.getInt(c.getColumnIndexOrThrow(PMealsDatabase.ITEMPORK)) == 1 ? true : false,
				c.getInt(c.getColumnIndexOrThrow(PMealsDatabase.ITEMNUTS)) == 1 ? true : false,
				c.getInt(c.getColumnIndexOrThrow(PMealsDatabase.ITEMEFRIENDLY)) == 1 ? true : false
		};
	}
	
	// --------------------------------------STATIC HOLDER CLASSES----------------------------------------
	
	static class DateHolder {
		TextView date;
	}
	
	static class MealNameHolder {
		TextView name;
		TextView extra;
	}
	
	static class MenuItemHolder {
		TextView item;
		ImageView vegan_vegetarian;
		ImageView pork;
		ImageView nuts;
	}
	
	private static class LoadingHolder {
		TextView text;
	}
}
