package com.sleepykoala.pmeals.contentproviders;

import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_ISREFRESH;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONNAME;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONNUMBER;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAMES;
import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;
import static com.sleepykoala.pmeals.data.C.MEALTIMESXML;

import java.io.IOException;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.MealTimeProviderFactory;
import com.sleepykoala.pmeals.data.PMealsDatabase;
import com.sleepykoala.pmeals.services.MenuDownloaderService;

public class MenuProvider extends ContentProvider {
	
	//private static final String TAG = "MenuProvider";
	
	private MealTimeProvider mTP;
	private LocationProvider lP;
	
	// database
	private PMealsDatabase mDB;
	
	// downloading status map
	// id + date (concatenated strings) -> boolean
	private static HashMap<String, Boolean> sDownloadStatuses = new HashMap<String, Boolean>();
	private static HashMap<String, Boolean> sRefreshStatuses = new HashMap<String, Boolean>();
	private static HashMap<String, Object> sLocks = new HashMap<String, Object>();
	private static final Object sMasterLock = new Object();

	// constants
	private static final String AUTHORITY = "com.sleepykoala.pmeals.contentproviders.MenuProvider";
	public static final int MENU = 10;
	public static final int FOODITEM_ID = 20;
	
	private static final String BASE_PATH = "meals";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + 
			"/" + BASE_PATH);
	
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + 
			"/pmeals-fooditem";
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE +
			"/pmeals-menu";
	
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sUriMatcher.addURI(AUTHORITY, BASE_PATH, MENU);
		sUriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", FOODITEM_ID);
	}
	
	@Override
	public boolean onCreate() {
		mDB = new PMealsDatabase(getContext());
        // get meal time provider
		try {
			MealTimeProviderFactory.initialize(getContext().getAssets().open(MEALTIMESXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + MEALTIMESXML + "!!");
		}
		mTP = MealTimeProviderFactory.newMealTimeProvider();
		
        // get location provider
        try {
        	LocationProviderFactory.initialize(getContext().getAssets().open(LOCATIONSXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + LOCATIONSXML + "!!");
		}
        lP = LocationProviderFactory.newLocationProvider();
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sUriMatcher.match(uri);
	    SQLiteDatabase sqlDB = mDB.getWritableDatabase();
	    int rowsDeleted = 0;
	    
	    switch (uriType) {
	    case MENU:
	      rowsDeleted = sqlDB.delete(PMealsDatabase.TABLE_MEALS, selection, selectionArgs);
	      break;
	    case FOODITEM_ID:
	      String id = uri.getLastPathSegment();
	      if (TextUtils.isEmpty(selection)) {
	        rowsDeleted = sqlDB.delete(PMealsDatabase.TABLE_MEALS,
	            PMealsDatabase._ID + "=" + id,
	            null);
	      } else {
	        rowsDeleted = sqlDB.delete(PMealsDatabase.TABLE_MEALS,
	            PMealsDatabase._ID + "=" + id +
	            " and " + selection,
	            selectionArgs);
	      }
	      break;
	    default:
	      throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsDeleted;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sUriMatcher.match(uri);
		long id = 0;
		switch (uriType) {
		case MENU:
			id = mDB.getWritableDatabase().insert(PMealsDatabase.TABLE_MEALS, null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return Uri.parse(BASE_PATH + "/" + id);
	}

	// NOTE: if selectionArgs has size 3, it must be in following order:
	// { locId, date, mealName }
	// else, performs a regular query
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		
		queryBuilder.setTables(PMealsDatabase.TABLE_MEALS);
		
		int uriType = sUriMatcher.match(uri);
		switch (uriType) {
		case MENU:
			break;
		case FOODITEM_ID:
			// add ID to query
			queryBuilder.appendWhere(PMealsDatabase._ID + "=" + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		Cursor cursor;
		if (selectionArgs.length == 3) {
			String id = selectionArgs[0];
			String date = selectionArgs[1];
			// create a lock for this location+date, if needed
			// this DCL *SHOULD* work, as HashMap is thread-safe
			// eg, reads and writes are atomic wrt each other
			if (sLocks.get(id + date) == null) {
				synchronized (sMasterLock) {
					if (sLocks.get(id + date) == null)
						sLocks.put(id + date, new Object());
				}
			}

			// synchronized block
			// each day+location combo gets its own lock
			synchronized (sLocks.get(id + date)) {
				cursor = queryBuilder.query(mDB.getReadableDatabase(), projection,
						selection, selectionArgs, null, null, sortOrder);
				cursor.setNotificationUri(getContext().getContentResolver(), uri);

				// not downloaded yet; download!!
				if (cursor.getCount() == 0) {
					// request download only if not already downloading
					// need this because a request for one meal forces request for all meals that day!
					if (!isDownloading(id, date)) {
						Location l = lP.getById(Integer.parseInt(id));
						// put arguments
						Intent dlService = new Intent();
						dlService.putExtra(EXTRA_LOCATIONID, id);
						dlService.putExtra(EXTRA_LOCATIONNAME, l.locName);
						dlService.putExtra(EXTRA_LOCATIONNUMBER, l.locNum);
						dlService.putExtra(EXTRA_DATE, date);
						dlService.putExtra(EXTRA_ISREFRESH, false);
						// get meal names
						Date dt = new Date(date);
						dlService.putExtra(EXTRA_MEALNAMES, mTP.getDaysMealNames(l.type, dt.weekDay));

						dlService.setClass(getContext(), MenuDownloaderService.class);
						getContext().startService(dlService);

						startDownload(id, date);
					}
				}
			}
		} else {
			cursor = queryBuilder.query(mDB.getReadableDatabase(), projection, selection,
					selectionArgs, null, null, sortOrder);
			//cursor.setNotificationUri(getContext().getContentResolver(), uri);
		}

		return cursor;
	}

	//-----------------------------------------------DOWNLOADING STATUS SETTER/GETTERS-----------------------------------
	
	// called by service when it finishes downloading
	// synchronized on the id+date combination
	public static void doneDownloading(String id, String date) {
		synchronized (sLocks.get(id + date)) {
			sDownloadStatuses.put(id + date, false);
		}
	}
	
	public static void doneRefreshing(String id, String date) {
		sDownloadStatuses.put(id + date, false);
		sRefreshStatuses.put(id + date, false);
	}
	
	public static void startDownload(String id, String date) {
		sDownloadStatuses.put(id + date, true);
	}
	
	public static void startRefresh(String id, String date) {
		sDownloadStatuses.put(id + date, true);
		sRefreshStatuses.put(id + date, true);
	}
	
	public static boolean isDownloading(String id, String date) {
		Boolean isDownloading = sDownloadStatuses.get(id + date);
		return isDownloading == null ? false : isDownloading;
	}
	
	public static boolean isRefreshing(String id, String date) {
		Boolean isRefreshing = sRefreshStatuses.get(id + date);
		return isRefreshing == null ? false : isRefreshing;
	}
	
	//-------------------------------------------------------------------------------------------------------------------

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int uriType = sUriMatcher.match(uri);
	    SQLiteDatabase sqlDB = mDB.getWritableDatabase();
	    int rowsUpdated = 0;
	    
	    switch (uriType) {
	    case MENU:
	      rowsUpdated = sqlDB.update(PMealsDatabase.TABLE_MEALS, 
	          values, selection, selectionArgs);
	      break;
	    case FOODITEM_ID:
	      String id = uri.getLastPathSegment();
	      if (TextUtils.isEmpty(selection)) {
	        rowsUpdated = sqlDB.update(PMealsDatabase.TABLE_MEALS, 
	            values, PMealsDatabase._ID + "=" + id, 
	            null);
	      } else {
	        rowsUpdated = sqlDB.update(PMealsDatabase.TABLE_MEALS, 
	            values, PMealsDatabase._ID + "=" + id + " and " +
	            selection, selectionArgs);
	      }
	      break;
	    default:
	      throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsUpdated;
	}

}
