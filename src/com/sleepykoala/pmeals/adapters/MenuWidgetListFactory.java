package com.sleepykoala.pmeals.adapters;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.contentproviders.MenuProvider;
import com.sleepykoala.pmeals.data.PMealsDatabase;

public class MenuWidgetListFactory implements RemoteViewsService.RemoteViewsFactory {
	
	//private static final String TAG = "MenuWidgetListFactory";
	
	private final int locID;
	private final String mealName;
	private final String date;
	private ContentResolver cr;
	private Cursor menu;
	private final String packageName;

	public MenuWidgetListFactory(Context context, int locID, String mealName, String date) {
		this.locID = locID;
		this.date = date;
		this.mealName = mealName;
		cr = context.getContentResolver();
		packageName = context.getPackageName();
	}

	public int getCount() {
		return menu.getCount() + 1;
	}
	
	public long getItemId(int position) {
		return position;
	}
	
	public RemoteViews getLoadingView() {
		return new RemoteViews(packageName, R.layout.widget_loading);
	}

	public RemoteViews getViewAt(int position) {
		RemoteViews views;
		
		if (position == 0) { // meal name
			views = new RemoteViews(packageName, R.layout.widget_meal_name);
			views.setTextViewText(R.id.widget_mealname, mealName);
		} else { // menu item
			views = new RemoteViews(packageName, R.layout.widget_menu_item);
			menu.moveToPosition(position - 1);
			views.setTextViewText(R.id.widget_itemname, getItemName(menu));
			// set food info indicators
			boolean[] info = getFoodInfo(menu);
			if (info[0])
				views.setImageViewResource(R.id.widget_vegan_vegetarian, R.drawable.vegan);
			else if (info[1])
				views.setImageViewResource(R.id.widget_vegan_vegetarian, R.drawable.vegetarian);
			else
				views.setImageViewResource(R.id.widget_vegan_vegetarian, R.drawable.foodinfo_outline);
			if (info[2])
				views.setImageViewResource(R.id.widget_pork, R.drawable.pork);
			else
				views.setImageViewResource(R.id.widget_pork, R.drawable.foodinfo_outline);
			if (info[3])
				views.setImageViewResource(R.id.widget_nuts, R.drawable.nuts);
			else
				views.setImageViewResource(R.id.widget_nuts, R.drawable.foodinfo_outline);
		}

		return views;
	}

	public int getViewTypeCount() {
		return 2;
	}

	public boolean hasStableIds() {
		return true;
	}

	public void onCreate() {
	}

	private void getMenu() {
		String[] projection = { PMealsDatabase.ITEMNAME, PMealsDatabase.ITEMERROR,
				PMealsDatabase.ITEMVEGAN,
				PMealsDatabase.ITEMVEGETARIAN,
				PMealsDatabase.ITEMPORK,
				PMealsDatabase.ITEMNUTS,
				PMealsDatabase.ITEMEFRIENDLY,
		};
		String select = "((" + PMealsDatabase.LOCATIONID + "=?) and ("
				+ PMealsDatabase.DATE + "=?) and (" + PMealsDatabase.MEALNAME + "=?))";
		String[] selectArgs = new String[]{ String.valueOf(locID),
				date,
				mealName
		};
		menu = cr.query(MenuProvider.CONTENT_URI, projection, select, selectArgs, null);
	}

	public void onDataSetChanged() {
		getMenu();
	}

	public void onDestroy() {
		try {
			menu.close();
		} catch (RuntimeException e) { }
	}

	//--------------------------------------------------CURSOR GETTERS------------------------------------

	// get the name of the food item the cursor is pointing at
	private String getItemName(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(PMealsDatabase.ITEMNAME));
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

}
