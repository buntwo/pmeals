package com.buntwo.pmeals.service;

import static com.buntwo.pmeals.data.C.ACTION_REFRESHFAILED;
import static com.buntwo.pmeals.data.C.EXTRA_DATE;
import static com.buntwo.pmeals.data.C.EXTRA_ISREFRESH;
import static com.buntwo.pmeals.data.C.EXTRA_LOCATIONID;
import static com.buntwo.pmeals.data.C.EXTRA_LOCATIONNAME;
import static com.buntwo.pmeals.data.C.EXTRA_LOCATIONNUMBER;
import static com.buntwo.pmeals.data.C.EXTRA_MEALNAMES;
import static com.buntwo.pmeals.data.C.STRING_DOWNLOADFAILED;
import static com.buntwo.pmeals.data.C.STRING_NODATA;
import static com.buntwo.pmeals.data.PMealsDatabase.DATE;
import static com.buntwo.pmeals.data.PMealsDatabase.ITEMERROR;
import static com.buntwo.pmeals.data.PMealsDatabase.ITEMNAME;
import static com.buntwo.pmeals.data.PMealsDatabase.LOCATIONID;
import static com.buntwo.pmeals.data.PMealsDatabase.MEALNAME;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.buntwo.pmeals.contentprovider.MenuProvider;
import com.buntwo.pmeals.data.C;
import com.buntwo.pmeals.data.FoodItem;

// actual downloading class
// downloads and writes to the content resolver
public class MenuDownloader implements Runnable {
	
	private static final String TAG = "MenuDownloader";

	// base URI's
	private static final String MENU1_BASEURI = "http://facilities.princeton.edu/dining/_Foodpro/menuSamp.asp";
	private static final String MENU2_BASEURI = "http://facilities.princeton.edu/dining/_Foodpro/pickMenu.asp";

	/*
	 *  Field names used for building the .asp request
	 */
	private static final String FIELD_LOCNUM = "locationNum";
	private static final String FIELD_MYACTION = "myaction";
	private static final String FIELD_ACTIONREAD = "read";
	private static final String FIELD_LOCNAME = "locationName";
	private static final String FIELD_DATE = "dtdate";
	private static final String FIELD_SCHOOLNAME = "sname";
	private static final String FIELD_NAFLAG = "naFlag";
	private static final String FIELD_MEALNAME = "mealName";
	// Princeton dining services name
	private static final String PRINCETON_DINING = "Princeton University Dining Services";

	/*
	 * Regular expressions
	 */
	// Matched in group()
	// Pattern.MULTILINE must be on!!
	private static final String REGEX_MEALNAME = "(?<=menusampmeals\">).*$";		// URI1
	// Matched in group(1)
	private static final String REGEX_MENU1ITEM = "menusamprecipes.*>(.*)(?=</a>)";	// URI1
	// Matched in group(1)
	private static final String REGEX_MENU2ITEM = "<a[^#>]*?>([^<]*?)</a>";			// URI2

	private final WeakReference<Context> mContext;
	
	private final String mLocId;
	private final ArrayList<String> mMealNames;
	private final String mLocNum;
	private final String mLocName;
	private final String mDate;
	private final boolean mIsRefresh;

	public MenuDownloader(Bundle args, Context cxt) {
		mLocId = args.getString(EXTRA_LOCATIONID);
		mMealNames = args.getStringArrayList(EXTRA_MEALNAMES);
		mLocNum = args.getString(EXTRA_LOCATIONNUMBER);
		mLocName = args.getString(EXTRA_LOCATIONNAME);
		mDate = args.getString(EXTRA_DATE);
		mIsRefresh = args.getBoolean(EXTRA_ISREFRESH);
		mContext = new WeakReference<Context>(cxt);
	}

	public void run() {
		Context cxt = mContext.get();
		
		// this line does all the network requests
		ArrayList<ArrayList<FoodItem>> menus = getDaysMenus(mMealNames, mLocNum, mLocName, mDate);
		
		MenuDownloaderService.startNextTask();

		final Uri CONTENT_URI = MenuProvider.CONTENT_URI;
		ContentResolver cr = cxt.getContentResolver();
		// if we are refreshing and download did not fail, delete all menu items for that day
		// we assume that if the first menu is a download failed, all of them are
		// if download failed, remind users with a toast
		if (mIsRefresh) {
			if (!menus.get(0).get(0).itemName.equals(STRING_DOWNLOADFAILED)) { // download did not fail
				cr.delete(CONTENT_URI, LOCATIONID + "=? and " +
						DATE + "=?", new String[]{mLocId, mDate});
				for (int i = 0; i < mMealNames.size(); ++i) {
					String mealName = mMealNames.get(i);
					ArrayList<FoodItem> menu = menus.get(i);
					int size = menu.size();
					ContentValues[] mealData = new ContentValues[size];
					for (int j = 0; j < size; ++j) {
						FoodItem f = menu.get(j);
						ContentValues data = new ContentValues();
						data.put(LOCATIONID, mLocId);
						data.put(DATE, mDate);
						data.put(MEALNAME, mealName);
						data.put(ITEMNAME, f.itemName);
						data.put(ITEMERROR, f.error); // converted to 1 = true, 0 = false
						mealData[j] = data;
					}
					cr.bulkInsert(CONTENT_URI, mealData);
				}
			} else { // send broadcast indicating failure
				Intent failure = new Intent();
				failure.setAction(ACTION_REFRESHFAILED);
				failure.putExtra(EXTRA_LOCATIONID, mLocId);
				failure.putExtra(EXTRA_DATE, mDate);
				LocalBroadcastManager.getInstance(cxt).sendBroadcast(failure);
			}
			// failed or not, we have to clear refresh status, and send broadcast
			MenuProvider.doneRefreshing(mLocId, mDate);
			Intent refreshDone = new Intent();
			refreshDone.setAction(C.ACTION_REFRESHDONE);
			LocalBroadcastManager.getInstance(cxt).sendBroadcast(refreshDone);
		} else {
			for (int i = 0; i < mMealNames.size(); ++i) {
				String mealName = mMealNames.get(i);
				ArrayList<FoodItem> menu = menus.get(i);
				int size = menu.size();
				ContentValues[] mealData = new ContentValues[size];
				for (int j = 0; j < size; ++j) {
					FoodItem f = menu.get(j);
					ContentValues data = new ContentValues();
					data.put(LOCATIONID, mLocId);
					data.put(DATE, mDate);
					data.put(MEALNAME, mealName);
					data.put(ITEMNAME, f.itemName);
					data.put(ITEMERROR, f.error); // converted to 1=true, 0=false
					mealData[j] = data;
				}
				cr.bulkInsert(CONTENT_URI, mealData);
			}
			// put in finished downloading status
			MenuProvider.doneDownloading(mLocId, mDate);
		}
	}

	//----------------------------------------------------HELPER METHODS-----------------------------------------
	private ArrayList<ArrayList<FoodItem>> getDaysMenus(ArrayList<String> aMealNames,
			String aLocNum, String aLocName, String aDate) {
		
		int size = aMealNames.size();
		ArrayList<ArrayList<FoodItem>> daysMenus = new ArrayList<ArrayList<FoodItem>>(size); // parallel array of menus
		// (parallel to mealNames)
		// get this day's MealTime
		for (int i = 0; i < size; ++i) { // populate with download failed's
			ArrayList<FoodItem> menu = new ArrayList<FoodItem>(1);
			menu.add(new FoodItem(STRING_DOWNLOADFAILED, true));
			daysMenus.add(menu);
		}

		// get menuSamp.asp info
		String URI1 = buildMenu1URI(aLocNum, aLocName, aDate);
		String htmlData = getHtmlData(URI1);

		// if URI1 fails, assume all URI2's will fail too
		if (htmlData == null)
			return daysMenus;
		// use HashMap because webpage could have the meals out of order...
		// actually probably not...think about using ArrayList instead?? TODO
		HashMap<String, ArrayList<FoodItem>> mealItems1 = parseMenu1(htmlData);

		// get URI2 data, and aggregate the two
		for (int i = 0; i < size; ++i) {
			String mealName = aMealNames.get(i);
			//for (DatedMealTime d : daysMenus.keySet()) {
			// expected that the meals URI1 returns is a subset of the ones in the local schedule
			// if URI1 does not have this meal, assume URI2 won't either

			if (!mealItems1.containsKey(mealName)) {
				daysMenus.get(i).set(0, new FoodItem(STRING_NODATA, true));
				continue;
			}

			// use LinkedHashSet to remove duplicates
			LinkedHashSet<FoodItem> meal = new LinkedHashSet<FoodItem>();
			// add in first set
			meal.addAll(mealItems1.get(mealName));
			String URI2 = buildMenu2URI(aLocNum, aLocName, aDate, mealName);

			htmlData = getHtmlData(URI2);

			if (htmlData == null)
				return daysMenus;
			// add in second set
			meal.addAll(parseMenu2(htmlData));
			
			// if it's empty, then add nodata entry
			if (meal.isEmpty())
				meal.add(new FoodItem(STRING_NODATA, true));
			
			// add the menu
			daysMenus.set(i, new ArrayList<FoodItem>(meal));
		}

		return daysMenus;
	}

	// given location number, location name, date, constructs URI that has the day's menu items
	// this is the menuSamp.asp page
	private String buildMenu1URI(String locNum, String locName, String date) {

		Uri.Builder b = Uri.parse(MENU1_BASEURI).buildUpon();
		// add args
		b.appendQueryParameter(FIELD_MYACTION, FIELD_ACTIONREAD);
		b.appendQueryParameter(FIELD_LOCNUM, locNum);
		b.appendQueryParameter(FIELD_LOCNAME, locName);
		b.appendQueryParameter(FIELD_DATE, date);
		b.appendQueryParameter(FIELD_SCHOOLNAME, PRINCETON_DINING);
		b.appendQueryParameter(FIELD_NAFLAG, "1");

		return b.build().toString();
	}

	// given HTML data from menuSamp.asp request, returns HashMap of meals and items
	// Meal name (pulled from data) -> items, as ArrayList<MenuItem>
	// DatedMealTime (name pulled from data, date given in constructor) -> items, as ArrayList<MenuItem>
	private HashMap<String, ArrayList<FoodItem>> parseMenu1(String htmlData) {

		// meal name matcher
		Matcher mealNameM = Pattern.compile(REGEX_MEALNAME, Pattern.MULTILINE).matcher(htmlData);
		ArrayList<String> mealNames = new ArrayList<String>();
		ArrayList<Integer> mealNamePositions = new ArrayList<Integer>();
		while (mealNameM.find()) {
			mealNames.add(mealNameM.group());
			mealNamePositions.add(mealNameM.start());
		}
		mealNamePositions.add(Integer.MAX_VALUE);

		// process mealLocations; if an entry is 0, make it equal to the next one
		// go backward, in case we have more than one 0 entry
		for (int i = mealNamePositions.size() - 2; i >= 0; --i) {
			if (mealNamePositions.get(i) == 0)
				mealNamePositions.set(i, mealNamePositions.get(i+1));
		}

		// meal item matcher
		// matched in group(1)
		Matcher itemNameM = Pattern.compile(REGEX_MENU1ITEM).matcher(htmlData);
		// store location -> meal item
		LinkedHashMap<Integer, String> itemNames = new LinkedHashMap<Integer, String>();
		while (itemNameM.find())
			itemNames.put(itemNameM.start(), itemNameM.group(1).replaceAll("&nbsp;", ""));

		// populate meals
		HashMap<String, ArrayList<FoodItem>> meals = new HashMap<String, ArrayList<FoodItem>>();

		// populate with empty ArrayLists
		for (String name : mealNames)
			meals.put(name, new ArrayList<FoodItem>());

		int mealNum = 1;
		// i is the character position of the match
		for (int i : itemNames.keySet()) {
			while (i > mealNamePositions.get(mealNum))
				++mealNum;
			FoodItem item = new FoodItem(itemNames.get(i), false);
			meals.get(mealNames.get(mealNum-1)).add(item);
		}
		
		return meals;
	}

	// given location number, name, date, and meal, all as strings, constructs URI that has
	// that meal's items
	// this is the pickMenu.asp page
	private String buildMenu2URI(String locNum, String locName, String date, String meal) {

		Uri.Builder b = Uri.parse(MENU2_BASEURI).buildUpon();
		// add args
		b.appendQueryParameter(FIELD_LOCNUM, locNum);
		b.appendQueryParameter(FIELD_LOCNAME, locName);
		b.appendQueryParameter(FIELD_DATE, date);
		b.appendQueryParameter(FIELD_MEALNAME, meal);
		b.appendQueryParameter(FIELD_SCHOOLNAME, PRINCETON_DINING);

		return b.build().toString();
	}

	// given HTML data from a pickMenu.asp request, returns the menu items
	// pulls all text in between <a> </a> tags
	// uses regex :(
	private ArrayList<FoodItem> parseMenu2(String htmlData) {
		Matcher m = Pattern.compile(REGEX_MENU2ITEM).matcher(htmlData); // match stuff between tags
		ArrayList<FoodItem> items = new ArrayList<FoodItem>();
		while (m.find()) {
			FoodItem item = new FoodItem(m.group(1).replaceAll("&nbsp;", ""), false);
			items.add(item);
		}
		return items;
	}

	// gets html data from an url
	// returns null if any error occurrs
	private String getHtmlData(String url) {
		HttpURLConnection conn = null;
		InputStream is = null;

		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			//conn.setRequestProperty("Connection", "close");

			// check for valid server response
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				Log.w(TAG, "Http server error " + conn.getResponseCode()
						+ ": " + conn.getResponseMessage());
				return null;
			}

			// pull content
			is = conn.getInputStream();
			InputStreamReader in = new InputStreamReader(new BufferedInputStream(is));
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			int charRead;
			while ((charRead = in.read()) != -1)
				content.write(charRead);

			return new String(content.toByteArray());

		} catch (IOException e) {
			return null;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					return null;
				}
			}
		}
	}

} 
