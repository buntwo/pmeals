package com.sleepykoala.pmeals;

import static com.sleepykoala.pmeals.data.C.COLOR_ERROR_ITEM;
import static com.sleepykoala.pmeals.data.C.COLOR_REGULAR_ITEM;
import static com.sleepykoala.pmeals.data.C.END_ALERT_COLOR_NONMAIN;
import static com.sleepykoala.pmeals.data.C.MEAL_PASSED_COLOR_NONMAIN;
import static com.sleepykoala.pmeals.data.C.MINUTES_END_ALERT;
import static com.sleepykoala.pmeals.data.C.MINUTES_START_ALERT;
import static com.sleepykoala.pmeals.data.C.NO_ALERT_COLOR_NONMAIN;
import static com.sleepykoala.pmeals.data.C.ONEHOUR_RADIUS;
import static com.sleepykoala.pmeals.data.C.START_ALERT_COLOR_NONMAIN;
import static com.sleepykoala.pmeals.data.C.STRING_DOWNLOADING;
import static com.sleepykoala.pmeals.data.C.STRING_LOADINGDATA;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.util.SparseArray;
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
import com.sleepykoala.pmeals.data.FoodItem;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.MealTimeProviderFactory;
import com.sleepykoala.pmeals.data.PMealsDatabase;

public class MealViewListAdapter extends BaseAdapter {
	
	//private final String TAG = "MenuItemListAdapter";
	
	private ArrayList<Cursor> data; // parallel array of the data
	private ArrayList<Location> locsToShow; // list of locations to show, in display order
	
	// map: location type -> meal
	private SparseArray<DatedMealTime> mealsToShow;
	
	private final LayoutInflater mInflater;
	
	private final int mainType;
	private int mealStatus;
	
	private Date today;
	private boolean isCurrentMeal; // are we showing the current meal?
	private static boolean isDetailedDate = false;
	private static final String DATEFORMAT = "EEE, MMM d, yyyy";
	
	// CONSTRUCTOR
	// messenger is the a messenger of the MenuProvider class
	public MealViewListAdapter(Context context, SparseArray<DatedMealTime> dmtMap, ArrayList<Location> locs) {
		mealsToShow = dmtMap;
		locsToShow = locs;
		mInflater = ((Activity) context).getLayoutInflater();
		
		mainType = locsToShow.get(0).type;
		isCurrentMeal = MealTimeProviderFactory.newMealTimeProvider().isCurrentMeal(mealsToShow.get(mainType), mainType);
		
		// set initial data and id
		int size = locsToShow.size();
		data = new ArrayList<Cursor>(size);
		for (int i = 0; i < size; ++i)
			data.add(null);
		
		// set date
		today = new Date();
	}
	
	// swap in the cursor at the given ID
	public void swapCursor(Cursor c, int ID) {
		data.set(ID, c);
		notifyDataSetChanged();
	}
	
	// is it okay to start a refresh?
	// only if all cursors are nonzero
	// returns nonsense if we are already refreshing! adapter does not store refreshing state
	public boolean okayToRefresh() {
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
	
	// called by broadcast receiver when time changes
	public void timeChanged() {
		mealStatus = MealTimeProvider.currentMealStatus(mealsToShow.get(mainType));
		notifyDataSetChanged();
	}
	
	// called by broadcast receiver when we enter a new meal
	public void newMeal() {
		mealStatus = MealTimeProvider.currentMealStatus(mealsToShow.get(mainType));
		isCurrentMeal = MealTimeProviderFactory.newMealTimeProvider().isCurrentMeal(mealsToShow.get(mainType), mainType);
		notifyDataSetChanged();
	}
	
	// called by broadcast receiver when we enter a new day
	public void newDay() {
		today = new Date();
		notifyDataSetChanged();
	}
	
	//----------------------------------------------------------------------------------------------------------------
	
	public int getCount() {
		int count = 1;
		for (Cursor c : data) {
			count += 1 + (c == null || c.getCount() == 0 ? 1 : c.getCount());
		}
		return count;
	}

	public Object getItem(int pos) {
		int counter = 1;
		int itemType = 0;
		Location loc = null;
		Cursor menu = null;
		for (int locPos = 0; locPos < locsToShow.size(); ++locPos) {
			loc = locsToShow.get(locPos);
			menu = data.get(locPos);
			if (pos == counter) {
				itemType = 0;
				break;
			}
			boolean loaded = menu != null && menu.getCount()!= 0;
			counter += (loaded) ? menu.getCount() : 1;
			if (pos <= counter) {
				itemType = (loaded) ? 2 : 1;
				break;
			}
			++counter;
		}
		// if type is menu item (2), counter now points to last menu item's position in that location
		
		if (itemType == 0)
			return loc;
		else if (itemType == 2) {
			menu.moveToPosition(menu.getCount() - counter + pos - 1);
			return inflateItem(menu);
		}
		
		return null;
	}

	// -1 = title
	// -2 = menu item
	// -3 = loading
	// -4 = unknown
	// if pos points to a location, returns its ID
	public long getItemId(int pos) {
		if (pos == 0) {
			return -1;
		} else {
			int counter = 1;
			for (int locPos = 0; locPos < locsToShow.size(); ++locPos) {
				Cursor c = data.get(locPos);
				if (pos == counter)
					return locsToShow.get(locPos).ID;
				boolean loaded = c != null && c.getCount() != 0;
				counter += (loaded) ? c.getCount() : 1;
				if (pos <= counter)
					return (loaded) ? -2 : -3;
				++counter;
			}
		}
		return -4;
	}
	
	public int getViewTypeCount() {
		return 4;
	}
	
	// 0 - location name
	// 1 - loading
	// 2 - menu item
	// 3 - meal info
	public int getItemViewType(int pos) {
		if (pos == 0) {
			return 3;
		} else {
			int counter = 1;
			for (Cursor c : data) {
				if (pos == counter)
					return 0;
				boolean loaded = c != null && c.getCount() != 0;
				counter += (loaded) ? c.getCount() : 1;
				if (pos <= counter)
					return (loaded) ? 2 : 1;
				++counter;
			}
		}
		return IGNORE_ITEM_VIEW_TYPE; // should never get here...
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		int counter = 1;
		Location loc = null;
		int itemType = 0;
		Cursor menu = null;
		if (position == 0) {
			itemType = 3;
		} else {
			for (int locPos = 0; locPos < locsToShow.size(); ++locPos) {
				loc = locsToShow.get(locPos);
				if (position == counter) {
					itemType = 0;
					break;
				}
				menu = data.get(locPos);
				boolean loaded = menu != null && menu.getCount() != 0;
				counter += (loaded) ? menu.getCount() : 1;
				if (position <= counter) {
					itemType = (loaded) ? 2 : 1;
					break;
				}
				++counter;
			}
		}
		// if type is menu item (2), counter now points to last menu item's position in that location

		if (itemType == 0) { // location name
			LocationNameHolder holder;
			if (convertView != null) {
				holder = (LocationNameHolder) convertView.getTag();
			} else {
				convertView = (LinearLayout) mInflater.inflate(R.layout.menu_sectiontitle, parent, false);
				holder = new LocationNameHolder();
				holder.name = (TextView) convertView.findViewById(R.id.sectiontitle);
				holder.extra = (TextView) convertView.findViewById(R.id.sectionextra);
				convertView.setTag(holder);
			}
			// hard coded location info!! (nickname);
			holder.name.setText(loc.nickname);
			if (loc.type != mainType) {
				DatedMealTime meal = mealsToShow.get(loc.type);
				if (meal != null) {
					// build mealinfo text
					final StringBuilder newTitleText = new StringBuilder(); 
					mealStatus = MealTimeProvider.currentMealStatus(meal);
					boolean inMeal = (mealStatus < 1) ? false : true;
					int[] timeTo;

					if (mealStatus == 0) { // meal already happened
						timeTo = MealTimeProvider.getTimeUntilMeal(meal, false);
						newTitleText.append("ended ");
						if ((-timeTo[0] == 1 && -timeTo[1] <= ONEHOUR_RADIUS) ||
								timeTo[0] == 0 && 60 + timeTo[1] <= ONEHOUR_RADIUS) {
							if (timeTo[1] != 0)
								newTitleText.append("about ");
							newTitleText.append("an hour ago");
						} else if (timeTo[0] == 0) {
							if (timeTo[1] == 0)
								newTitleText.append(" just now");
							else {
								newTitleText.append(-timeTo[1]);
								newTitleText.append(" minute");
								if (timeTo[1] != 1) // plural
									newTitleText.append("s");
								newTitleText.append(" ago");
							}
						} else {
							newTitleText.append("at ");
							int[] time;
							time = meal.endTime;
							boolean inAM = time[0] < 12;
							newTitleText.append(String.format("%d:%02d%s", inAM ? time[0] : time[0]-12,
									time[1], inAM ? "am" : "pm"));
						}
					} else {
						timeTo = MealTimeProvider.getTimeUntilMeal(meal, !inMeal);
						newTitleText.append((inMeal) ? "ends " : "starts ");
						if ((timeTo[0] == 1 && timeTo[1] <= ONEHOUR_RADIUS) ||
								timeTo[0] == 0 && 60 - timeTo[1] <= ONEHOUR_RADIUS) {
							newTitleText.append("in ");
							if (timeTo[1] != 0)
								newTitleText.append("about ");
							newTitleText.append("an hour");
						} else if (timeTo[0] == 0) {
							newTitleText.append("in ");
							newTitleText.append(timeTo[1]);
							newTitleText.append(" minute");
							if (timeTo[1] != 1) // plural
								newTitleText.append("s");
						} else {
							newTitleText.append("at ");
							int[] time;
							if (inMeal)
								time = meal.endTime;
							else
								time = meal.startTime;
							newTitleText.append(MealTimeProvider.getFormattedTime(time));
						}
						if ((new Date()).isTomorrow(meal.date))
							// next meal is tomorrow
							newTitleText.append(" tomorrow");
					}
					// get text color
					int textColor;
					// get new color
					if (mealStatus == 0) {
						textColor = MEAL_PASSED_COLOR_NONMAIN;
					} else {
						if (timeTo[0] == 0) {
							if (inMeal && timeTo[1] <= MINUTES_END_ALERT)
								textColor = END_ALERT_COLOR_NONMAIN;
							else if (!inMeal && timeTo[1] <= MINUTES_START_ALERT)
								textColor = START_ALERT_COLOR_NONMAIN;
							else
								textColor = NO_ALERT_COLOR_NONMAIN;
						} else 
							textColor = NO_ALERT_COLOR_NONMAIN;
					}
					holder.extra.setText(newTitleText);
					holder.extra.setTextColor(textColor);
				}
			} else {
				holder.extra.setText("");
			}
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
			MealInfoHolder holder;
			if (convertView != null) {
				holder = (MealInfoHolder) convertView.getTag();
			} else {
				convertView = (LinearLayout) mInflater.inflate(R.layout.menu_header, parent, false);
				holder = new MealInfoHolder();
				holder.info = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(holder);
			}
			// set header text
			StringBuilder text = new StringBuilder();
			DatedMealTime mainMeal = mealsToShow.get(mainType);

			if (!isDetailedDate) {
				if (isCurrentMeal) {
					if (mealStatus == -1) // before meal
						text.append("Next");
					else // in a meal; current meal cannot have happened already
						text.append("Current");
					text.append(" meal: ");
					text.append(mainMeal.mealName);
				} else {
					if (today.equals(mainMeal.date)) {
						text.append("Today's ");
					} else if (today.isTomorrow(mainMeal.date)) {
						text.append("Tomorrow's ");
					} else if (today.isYesterday(mainMeal.date)) {
						text.append("Yesterday's ");
					} else {
						text.append(DateFormat.format("EEEE", mainMeal.date.toMillis(true)));
						text.append("'s ");
					}
					text.append(mainMeal.mealName.toLowerCase());
				}
			} else {
				text.append(mainMeal.mealName);
				text.append(" on ");
				text.append(DateFormat.format(DATEFORMAT, mainMeal.date.toMillis(true)));
			}
			
			holder.info.setText(text);
		}

		return convertView;
	}
	
	// get a FoodItem from cursor's current position
	private FoodItem inflateItem(Cursor c) {
		boolean[] params = { c.getInt(c.getColumnIndex(PMealsDatabase.ITEMVEGAN)) == 1 ? true : false,
				c.getInt(c.getColumnIndex(PMealsDatabase.ITEMVEGETARIAN)) == 1 ? true : false,
				c.getInt(c.getColumnIndex(PMealsDatabase.ITEMPORK)) == 1 ? true : false,
				c.getInt(c.getColumnIndex(PMealsDatabase.ITEMNUTS)) == 1 ? true : false,
				c.getInt(c.getColumnIndex(PMealsDatabase.ITEMEFRIENDLY)) == 1 ? true : false
		};
		return new FoodItem(c.getString(c.getColumnIndex(PMealsDatabase.ITEMNAME)),
				c.getInt(c.getColumnIndex(PMealsDatabase.ITEMERROR)) == 1 ? true : false,
				params
				);
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
	
	private static class MealInfoHolder {
		TextView info;
	}
	
	private static class LocationNameHolder {
		TextView name;
		TextView extra;
	}
	
	private static class MenuItemHolder {
		TextView item;
		ImageView vegan_vegetarian;
		ImageView pork;
		ImageView nuts;
	}
	
	private static class LoadingHolder {
		TextView text;
	}

}
