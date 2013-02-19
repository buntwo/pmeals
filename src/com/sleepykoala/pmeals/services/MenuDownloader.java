package com.sleepykoala.pmeals.services;

import static com.sleepykoala.pmeals.data.C.ACTION_REFRESHFAILED;
import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_ISREFRESH;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONNAME;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONNUMBER;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAMES;
import static com.sleepykoala.pmeals.data.C.STRING_DOWNLOADFAILED;
import static com.sleepykoala.pmeals.data.C.STRING_NODATA;
import static com.sleepykoala.pmeals.data.PMealsDatabase.DATE;
import static com.sleepykoala.pmeals.data.PMealsDatabase.ITEMEFRIENDLY;
import static com.sleepykoala.pmeals.data.PMealsDatabase.ITEMERROR;
import static com.sleepykoala.pmeals.data.PMealsDatabase.ITEMNAME;
import static com.sleepykoala.pmeals.data.PMealsDatabase.ITEMNUTS;
import static com.sleepykoala.pmeals.data.PMealsDatabase.ITEMPORK;
import static com.sleepykoala.pmeals.data.PMealsDatabase.ITEMVEGAN;
import static com.sleepykoala.pmeals.data.PMealsDatabase.ITEMVEGETARIAN;
import static com.sleepykoala.pmeals.data.PMealsDatabase.LOCATIONID;
import static com.sleepykoala.pmeals.data.PMealsDatabase.MEALNAME;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.sleepykoala.pmeals.contentproviders.MenuProvider;
import com.sleepykoala.pmeals.data.C;
import com.sleepykoala.pmeals.data.FoodItem;

// actual downloading class
// downloads and writes to the content resolver
public class MenuDownloader implements Runnable {
	
	//private static final String TAG = "MenuDownloader";

	// base URI's
	private static final String MENU1_BASEURI = "http://facilities.princeton.edu/dining/_Foodpro/menu.asp?";

	// XML charset
	private static final String CHARSET = "ISO-8859-1";
	
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
	// Princeton dining services name
	private static final String PRINCETON_DINING = "Princeton University Dining Services";

	/* Tag names for XML parsing
	 */
	private static final String TAG_MEAL = "meal";
	private static final String TAG_ENTREE = "entree";
	private static final String TAG_NAME = "name";
	private static final String TAG_VEGAN = "vegan";
	private static final String TAG_VEGETARIAN = "vegetarian";
	private static final String TAG_PORK = "pork";
	private static final String TAG_NUTS = "nuts";
	private static final String TAG_EFRIENDLY = "earth_friendly";
	private static final String ATTRIBUTE_TYPE = "type";

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
						data.put(ITEMERROR, f.error); // converted to 1 = true, 0 = false, in db
						data.put(ITEMVEGAN, f.foodInfo[0]);
						data.put(ITEMVEGETARIAN, f.foodInfo[1]);
						data.put(ITEMPORK, f.foodInfo[2]);
						data.put(ITEMNUTS, f.foodInfo[3]);
						data.put(ITEMEFRIENDLY, f.foodInfo[4]);
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
					data.put(ITEMVEGAN, f.foodInfo[0]);
					data.put(ITEMVEGETARIAN, f.foodInfo[1]);
					data.put(ITEMPORK, f.foodInfo[2]);
					data.put(ITEMNUTS, f.foodInfo[3]);
					data.put(ITEMEFRIENDLY, f.foodInfo[4]);
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
			menu.add(new FoodItem(STRING_DOWNLOADFAILED, true, "", new boolean[5]));
			daysMenus.add(menu);
		}

		// get XML feed info
		String URI1 = buildMenu1URI(aLocNum, aLocName, aDate);
		String htmlData = getHtmlData(URI1);

		if (htmlData == null)
			return daysMenus;
		// use HashMap because webpage could have the meals out of order...
		// actually probably not...think about using ArrayList instead?? TODO
		htmlData = cleanData(htmlData);
		HashMap<String, ArrayList<FoodItem>> mealItems1 = parseMenu1(htmlData);
		
		if (mealItems1 == null)
			return daysMenus;

		for (int i = 0; i < size; ++i) {
			String mealName = aMealNames.get(i);
			if (!mealItems1.containsKey(mealName)) {
				daysMenus.get(i).set(0, new FoodItem(STRING_NODATA, true, "", new boolean[5]));
				continue;
			}
			
			ArrayList<FoodItem> meal = mealItems1.get(mealName);
			
			// if it's empty, then add nodata entry
			if (meal.isEmpty())
				meal.add(new FoodItem(STRING_NODATA, true, "", new boolean[5]));
			
			// add the menu
			daysMenus.set(i, meal);
		}

		return daysMenus;
	}
	
	// replace diacritical characters with non-accented ones
	// and for some reason e' -> y' lol
	private String cleanData(String s) {
		return s.replace('ý', 'e');
	}

	// given location number, location name, date, constructs URI that has the day's menu items
	// this is the XML feed
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
	// returns null on download error
	private HashMap<String, ArrayList<FoodItem>> parseMenu1(String htmlData) {
		HashMap<String, ArrayList<FoodItem>> meals = new HashMap<String, ArrayList<FoodItem>>();

		XmlPullParser p = null;
		try {
			p = XmlPullParserFactory.newInstance().newPullParser();
			p.setInput(new StringReader(htmlData));
			
			int eventType = p.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					String tagName = p.getName();
					if (tagName.equals(TAG_MEAL)) {
						// assumes that there is 1 attribute in the meal tag, 
						// which is the name of the meal
						String mealName = p.getAttributeValue(0);
						meals.put(mealName, processMeal(p));
					}
					break;
					
				}
				eventType = p.next();
			}
		} catch (XmlPullParserException e) {
			// assume that this means the xml file is malformed, eg, not my error
			return null;
			//throw new RuntimeException("XmlPullParseException");
		} catch (IOException e) {
			throw new RuntimeException("IOException");
		} finally { // release parser resources
			if (p != null) {
				try {
					p.setInput(null);
				} catch (XmlPullParserException e) {} // should never happen
			}
		}
		
		return meals;
	}
	
	private ArrayList<FoodItem> processMeal(XmlPullParser p) throws XmlPullParserException, IOException {
		ArrayList<FoodItem> meal = new ArrayList<FoodItem>();
		
		int eventType = p.next();
		while (!((eventType == XmlPullParser.END_TAG) && p.getName().equals(TAG_MEAL))) {
			if (eventType == XmlPullParser.START_TAG)
				if (p.getName().equals(TAG_ENTREE))
					meal.add(processEntree(p));
			eventType = p.next();
		}
		
		return meal;
	}
	
	private FoodItem processEntree(XmlPullParser p) throws XmlPullParserException, IOException {
		String name = null;
		// params is { isVegan, isVegetarian, hasPork, hasNuts, isEFriendly }
		boolean[] params = new boolean[5];
		
		String type;
		// get entree type attribute
		try {
			if (p.getAttributeName(0).equals(ATTRIBUTE_TYPE))
				type = p.getAttributeValue(0);
			else
				type = "";
		} catch (IndexOutOfBoundsException e) {
			type = "";
		}
		int eventType = p.next();
		while (!((eventType == XmlPullParser.END_TAG) && p.getName().equals(TAG_ENTREE))) {
			if (eventType == XmlPullParser.START_TAG) {
				if (p.getName().equals(TAG_NAME))
					name = p.nextText();
				else if (p.getName().equals(TAG_VEGAN))
					params[0] = (p.nextText().equals("y")) ? true : false;
				else if (p.getName().equals(TAG_VEGETARIAN))
					params[1] = (p.nextText().equals("y")) ? true : false;
				else if (p.getName().equals(TAG_PORK))
					params[2] = (p.nextText().equals("y")) ? true : false;
				else if (p.getName().equals(TAG_NUTS))
					params[3] = (p.nextText().equals("y")) ? true : false;
				else if (p.getName().equals(TAG_EFRIENDLY))
					params[4] = (p.nextText().equals("y")) ? true : false;
			}
			eventType = p.next();
		}
		
		return new FoodItem(name, false, type, params);
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
				/*Log.w(TAG, "Http server error " + conn.getResponseCode()
						+ ": " + conn.getResponseMessage());
						*/
				return null;
			}

			// pull content
			is = conn.getInputStream();
			InputStreamReader in = new InputStreamReader(new BufferedInputStream(is));
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			int charRead;
			while ((charRead = in.read()) != -1)
				content.write(charRead);

			return new String(content.toByteArray(), CHARSET);

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
