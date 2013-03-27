package com.sleepykoala.pmeals.fragments;

import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTLOC;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTNUM;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTQUERY;
import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_FRAGMENTNUM;
import static com.sleepykoala.pmeals.data.C.EXTRA_ISREFRESH;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONNAME;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONNUMBER;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALEXISTS;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAME;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAMES;
import static com.sleepykoala.pmeals.data.C.STRING_CLOSED;

import java.util.ArrayList;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.activities.MealSearcher;
import com.sleepykoala.pmeals.activities.SetupNewAlert;
import com.sleepykoala.pmeals.adapters.LocationViewListAdapter;
import com.sleepykoala.pmeals.contentproviders.MenuProvider;
import com.sleepykoala.pmeals.data.C;
import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.DatedMealTime;
import com.sleepykoala.pmeals.data.FoodItem;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.MealTimeProviderFactory;
import com.sleepykoala.pmeals.data.PMealsDB;
import com.sleepykoala.pmeals.data.PMealsPreferenceManager;
import com.sleepykoala.pmeals.services.MenuDownloaderService;

public class LocationViewListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	
	//private static final String TAG = "LocationViewListFragment";
	
	private static final IntentFilter sIntentFilter;
	// create intent filter
	static {
		sIntentFilter = new IntentFilter();
		sIntentFilter.addAction(C.ACTION_NEW_DAY);
		sIntentFilter.addAction(C.ACTION_NEW_MEAL);
		sIntentFilter.addAction(C.ACTION_REFRESHDONE);
		sIntentFilter.addAction(C.ACTION_REFRESHFAILED);
		sIntentFilter.addAction(C.ACTION_DATEFORMATTOGGLED);
	}
	private final BroadcastReceiver timeChangedReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(C.ACTION_NEW_DAY)) {
				mAdapter.newDay();
			} else if (action.equals(C.ACTION_NEW_MEAL)) {
				mAdapter.notifyDataSetChanged();
			} else if (action.equals(C.ACTION_REFRESHDONE)) {
				if (!isRefreshing()) {
					mRefreshIcon.clearAnimation();
					mRefreshIcon.setVisibility(View.GONE);
				}
			} else if (action.equals(C.ACTION_REFRESHFAILED)) {
				Bundle extras = intent.getExtras();
				if (extras.getString(C.EXTRA_DATE).equals(mDate) && 
						extras.getString(C.EXTRA_LOCATIONID).equals(String.valueOf(mLoc.ID)))
					Toast.makeText(getActivity(), "Refresh failed", Toast.LENGTH_SHORT).show();
			} else if (action.equals(C.ACTION_DATEFORMATTOGGLED)) {
				mAdapter.notifyDataSetChanged();
			}
		}
	};

	private static String[] projection = {
		PMealsDB.ITEMNAME,
		PMealsDB.ITEMTYPE,
		PMealsDB.ITEMERROR,
		PMealsDB.ITEMVEGAN,
		PMealsDB.ITEMVEGETARIAN,
		PMealsDB.ITEMPORK,
		PMealsDB.ITEMNUTS,
		PMealsDB.ITEMEFRIENDLY,
	};

	private LocationViewListAdapter mAdapter;

	private ArrayList<Bundle> loaderArgs;
	private String mDate;
	private Location mLoc;
	private ArrayList<String> mDaysMealNames;
	private int fragmentNum;
	
	// cached resources
	Animation mRotation;
	ImageView mRefreshIcon;
	
	
	//-----------------------------------------------OVERRIDES-----------------------------------------
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// has options menu
		setHasOptionsMenu(true);
		
		// cache resources
		mRotation = AnimationUtils.loadAnimation(getActivity(), R.anim.refresh_rotate);
		
		// should be initialized already
		MealTimeProvider mTP = MealTimeProviderFactory.newMealTimeProvider();
		LocationProvider lP = LocationProviderFactory.newLocationProvider();
		
		// get arguments
		Bundle args = getArguments();
		int locID = args.getInt(EXTRA_LOCATIONID);
		mDate = args.getString(EXTRA_DATE);
		fragmentNum = args.getInt(EXTRA_FRAGMENTNUM);
		
		// inflate objects
		mLoc = lP.getById(locID);
		Date dt = new Date(mDate);
		
		// get day's meals
		ArrayList<DatedMealTime> daysMeals = mTP.getDaysMeals(mLoc.type, dt);
		
		// cache days meal names
		mDaysMealNames = new ArrayList<String>(daysMeals.size());
		for (DatedMealTime dmt : daysMeals)
			mDaysMealNames.add(dmt.mealName);
		
		// set adapter
		mAdapter = new LocationViewListAdapter(getActivity(), daysMeals, dt, LocationProvider.isDiningHall(mLoc));
		setListAdapter(mAdapter);
		
		// cache loader args
		if (daysMeals.size() != 0) {
			int size = daysMeals.size();
			loaderArgs = new ArrayList<Bundle>(size);
			for (int i = 0; i < size; ++i) {
				Bundle loaderArg = new Bundle();
				loaderArg.putBoolean(EXTRA_MEALEXISTS, true);
				loaderArg.putInt(EXTRA_LOCATIONID, locID);
				loaderArg.putString(EXTRA_DATE, mDate);
				loaderArg.putString(EXTRA_MEALNAME, daysMeals.get(i).mealName);
				
				loaderArgs.add(loaderArg);
			}
		} else {
			loaderArgs = new ArrayList<Bundle>(1);
			Bundle loaderArg = new Bundle();
			loaderArg.putBoolean(EXTRA_MEALEXISTS, false);
			loaderArgs.add(loaderArg);
		}
		// add locnote arg
		Bundle loaderArg = new Bundle();
		loaderArg.putInt(EXTRA_LOCATIONID, locID);
		loaderArg.putString(EXTRA_DATE, mDate);
		loaderArgs.add(loaderArg);
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.listfragment_menu, container, false);
		mRefreshIcon = (ImageView) view.findViewById(R.id.refreshicon);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		// register listview for context menu
		registerForContextMenu(getListView());
		
		// load cursors
		LoaderManager lM = getLoaderManager();
		for (int i = 0; i < loaderArgs.size(); ++i)
			lM.initLoader(i, loaderArgs.get(i), this);
	}
	
	@Override
	public void onListItemClick (ListView l, View v, int pos, long id) {
		if (pos == 0) { // change date type to detailed
			mAdapter.toggleDateFormat();
			Intent toggleDate = new Intent();
			toggleDate.setAction(C.ACTION_DATEFORMATTOGGLED);
			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(toggleDate);
		} else if (pos == 1 && id == -5)
			mAdapter.toggleNoteExpanded();
	}
	
	@Override
	public void onResume() {
		super.onResume();
        if (isRefreshing()) {
        	mRefreshIcon.startAnimation(mRotation);
        	mRefreshIcon.setVisibility(View.VISIBLE);
        } else {
        	mRefreshIcon.setVisibility(View.GONE);
        }
		// register local receiver
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(timeChangedReceiver, sIntentFilter);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mRefreshIcon.clearAnimation();
		// unregister receiver
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(timeChangedReceiver);
	}
	
	//-------------------------------------------------OPTIONS MENU STUFF-----------------------------------------

	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_menu, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.refresh:
    		if (!isRefreshing() && mAdapter.okayToRefresh()) {
    			// start animation
    			mRefreshIcon.startAnimation(mRotation);
    			mRefreshIcon.setVisibility(View.VISIBLE);
    			// send refresh request to service
    			// sets service refresh status
    			refreshDay();
    		}
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
    	super.onCreateContextMenu(menu, v, info);
    	
    	if (((AdapterContextMenuInfo) info).id == -2) {
    		menu.add(fragmentNum, R.id.share, 0, R.string.share);
    		menu.add(fragmentNum, R.id.copy, 1, R.string.copy);
    		menu.add(fragmentNum, R.id.searchmeals, 2, R.string.search_meals);
    		menu.add(fragmentNum, R.id.searchonline, 3, R.string.search_online);
    		menu.add(fragmentNum, R.id.makealert, 4, R.string.make_alert);
    	}
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	if (item.getGroupId() != fragmentNum)
    		return false;
    	
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	String locName, mealName;
    	String itemName = ((FoodItem) getListAdapter().getItem(info.position)).itemName;
    	switch (item.getItemId()) {
    	case R.id.share:
    		locName = mLoc.nickname;
    		mealName = ((LocationViewListAdapter) getListAdapter()).getMeal(info.position).mealName;
    		Intent share = new Intent(Intent.ACTION_SEND);
    		share.setType("text/plain");
    		share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    		share.putExtra(Intent.EXTRA_SUBJECT, "Entree at " + locName);
    		Date dateShown = new Date(mDate);
    		StringBuilder body = new StringBuilder();
    		body.append(locName).append(" h").append(dateShown.after(new Date()) ? "as" : "ad" )
    			.append(" ").append(itemName).append(" for ") .append(mealName.toLowerCase())
    			.append(" ").append((new Date(mDate)).toStringPretty(false, true))
	    		.append("!");
    		share.putExtra(Intent.EXTRA_TEXT, body.toString());
    		startActivity(Intent.createChooser(share, "Share this entree..."));
    		return true;
    	case R.id.copy:
    		ClipboardManager cbm = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
    		cbm.setPrimaryClip(ClipData.newPlainText("pmeals item", itemName));
    		Toast.makeText(getActivity(), "Copied: " +itemName, Toast.LENGTH_SHORT).show();
    		return true;
    	case R.id.searchmeals:
    		Intent searchIntent = new Intent(getActivity(), MealSearcher.class);
    		searchIntent.setAction(Intent.ACTION_SEARCH);
    		searchIntent.putExtra(SearchManager.QUERY, itemName);
    		startActivity(searchIntent);
    		return true;
    	case R.id.searchonline:
    		Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
    		search.putExtra(SearchManager.QUERY, itemName);
    		search.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    		
    		startActivity(search);
    		return true;
    	case R.id.makealert:
    		Intent add = new Intent(getActivity(), SetupNewAlert.class);
    		add.putExtra(EXTRA_ALERTNUM, PMealsPreferenceManager.getNumAlerts() + 1);
    		add.putExtra(EXTRA_ALERTQUERY, itemName);
    		add.putExtra(EXTRA_ALERTLOC, mLoc.ID);
    		add.putExtra(EXTRA_MEALNAME, ((LocationViewListAdapter) getListAdapter()).getMeal(info.position).mealName);

    		startActivity(add);
    		return true;
    	default:
    		return super.onContextItemSelected(item);
    	}
    }
    
    //----------------------------------------------------------------------------------------------------------
    
    private boolean isRefreshing() {
    	return MenuProvider.isRefreshing(String.valueOf(mLoc.ID), mDate);
    }

    private void refreshDay() {
    	MenuProvider.startRefresh(String.valueOf(mLoc.ID), mDate);
    	Intent dlService = new Intent();
    	dlService.putExtra(EXTRA_LOCATIONID, String.valueOf(mLoc.ID));
    	dlService.putExtra(EXTRA_LOCATIONNAME, mLoc.locName);
    	dlService.putExtra(EXTRA_LOCATIONNUMBER, mLoc.locNum);
    	dlService.putExtra(EXTRA_DATE, mDate);
    	dlService.putExtra(EXTRA_ISREFRESH, true);
    	dlService.putExtra(EXTRA_MEALNAMES, mDaysMealNames);

    	dlService.setClass(getActivity(), MenuDownloaderService.class);
    	getActivity().startService(dlService);
    }

    //---------------------------------------------------LOADER CALLBACKS---------------------------------------

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    	String select;
    	String[] selectArgs;
    	if (id < loaderArgs.size() - 1) {
    		if (args.getBoolean(EXTRA_MEALEXISTS)) {
    			select = "((" + PMealsDB.LOCATIONID + "=?) and ("
    					+ PMealsDB.DATE + "=?) and (" + PMealsDB.MEALNAME + "=?))";
    			selectArgs = new String[] { String.valueOf(args.getInt(EXTRA_LOCATIONID)),
    					args.getString(EXTRA_DATE),
    					args.getString(EXTRA_MEALNAME)
    			};
    		} else {
    			select = "((" + PMealsDB.ITEMNAME + "=?) and (" + 
    					PMealsDB.ITEMERROR + "=?))";
    			// need the second string for locking purposes in the content provider
    			selectArgs = new String[] { STRING_CLOSED, "1" };
    		}

    		return new CursorLoader(getActivity(), MenuProvider.MEALS_URI,
    				projection, select, selectArgs, null);
		} else { // locnote
			select = "((" + PMealsDB.LOCATIONID + "=?) and ("
					+ PMealsDB.DATE + "=?))";
			selectArgs = new String[] { String.valueOf(args.getInt(EXTRA_LOCATIONID)),
					args.getString(EXTRA_DATE),
			};
			return new CursorLoader(getActivity(), MenuProvider.LOCNOTES_URI,
					new String[]{ PMealsDB.NOTE }, select, selectArgs, null);
    	}
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		int id = loader.getId();
		if (id < loaderArgs.size() - 1)
			mAdapter.swapCursor(cursor, id);
		else {
			if (cursor.getCount() != 0) {
				cursor.moveToFirst();
				mAdapter.setNote(cursor.getString(cursor.getColumnIndexOrThrow(PMealsDB.NOTE)));
			} else
				mAdapter.setNote(null);
		}
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		int id = loader.getId();
		if (id < loaderArgs.size() - 1)
			mAdapter.swapCursor(null, id);
	}

}
