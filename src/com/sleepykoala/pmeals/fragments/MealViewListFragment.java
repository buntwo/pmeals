package com.sleepykoala.pmeals.fragments;

import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTLOC;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTNUM;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTQUERY;
import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_ISREFRESH;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONIDS;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONNAME;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONNUMBER;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALEXISTS;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAME;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAMES;
import static com.sleepykoala.pmeals.data.C.STRING_CLOSED;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
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
import android.util.SparseArray;
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
import com.sleepykoala.pmeals.activities.ViewByLocation;
import com.sleepykoala.pmeals.adapters.MealViewListAdapter;
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
import com.sleepykoala.pmeals.data.PMealsDatabase;
import com.sleepykoala.pmeals.data.PMealsPreferenceManager;
import com.sleepykoala.pmeals.services.MenuDownloaderService;

public class MealViewListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	
	//private static final String TAG = "MealViewListFragment";
	
	private static final IntentFilter sIntentFilter;
	// create intent filter
	static {
		sIntentFilter = new IntentFilter();
		sIntentFilter.addAction(C.ACTION_TIME_CHANGED);
		sIntentFilter.addAction(C.ACTION_NEW_MEAL);
		sIntentFilter.addAction(C.ACTION_NEW_DAY);
		sIntentFilter.addAction(C.ACTION_REFRESHSTART);
		sIntentFilter.addAction(C.ACTION_REFRESHDONE);
		sIntentFilter.addAction(C.ACTION_REFRESHFAILED);
		sIntentFilter.addAction(C.ACTION_DATEFORMATTOGGLED);
	}
	private final BroadcastReceiver bCastReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(C.ACTION_TIME_CHANGED)) { // update time
				mAdapter.timeChanged();
			} else if (action.equals(C.ACTION_NEW_MEAL)) {
				mAdapter.newMeal();
			} else if (action.equals(C.ACTION_NEW_DAY)) {
				mAdapter.newDay();
			} else if (action.equals(C.ACTION_REFRESHSTART) &&
					intent.getExtras().getString(EXTRA_DATE).equals(mDate)) {
				// start animation
				mRefreshIcon.startAnimation(mRotation);
				mRefreshIcon.setVisibility(View.VISIBLE);
			} else if (action.equals(C.ACTION_REFRESHDONE)) {
				if (!isRefreshing()) {
					mRefreshIcon.clearAnimation();
					mRefreshIcon.setVisibility(View.GONE);
				}
			} else if (action.equals(C.ACTION_REFRESHFAILED) &&
					intent.getExtras().getString(C.EXTRA_DATE).equals(mDate)) {
				synchronized (dlFailedLock) {
					if (!failedToastDisplayed.get(mDate)) {
						Toast.makeText(getActivity(), "Refresh failed", Toast.LENGTH_SHORT).show();
						failedToastDisplayed.put(mDate, true);
					}
				}
			} else if (action.equals(C.ACTION_DATEFORMATTOGGLED)) {
				mAdapter.notifyDataSetChanged();
			}
		}
	};

	private static String[] projection = {
		PMealsDatabase.ITEMNAME,
		PMealsDatabase.ITEMTYPE,
		PMealsDatabase.ITEMERROR,
		PMealsDatabase.ITEMVEGAN,
		PMealsDatabase.ITEMVEGETARIAN,
		PMealsDatabase.ITEMPORK,
		PMealsDatabase.ITEMNUTS,
		PMealsDatabase.ITEMEFRIENDLY,
	};

	private MealViewListAdapter mAdapter;

	private static final Object dlFailedLock = new Object();
	private static HashMap<String, Boolean> failedToastDisplayed = new HashMap<String, Boolean>();
	private ArrayList<Bundle> loaderArgs;
	private String mDate;
	private String mealName;
	private ArrayList<Location> mLocsToShow;
	private SparseArray<DatedMealTime> mMealArr;
	
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
		
		MealTimeProvider mTP = MealTimeProviderFactory.newMealTimeProvider();
		LocationProvider lP = LocationProviderFactory.newLocationProvider();
		
		// get arguments
		Bundle args = getArguments();
		ArrayList<Integer> locIDs = args.getIntegerArrayList(EXTRA_LOCATIONIDS);
		mDate = args.getString(EXTRA_DATE);
		mealName = args.getString(EXTRA_MEALNAME);
		
		// build locsToShow
		mLocsToShow = new ArrayList<Location>();
		for (int id : locIDs)
			mLocsToShow.add(lP.getById(id));
		
		// loop until we find non-null one
		int size = mLocsToShow.size();
		DatedMealTime originalMeal = null;
		for (int i = 0; i < size; ++i) {
			originalMeal = mTP.constructMeal(mealName, mDate, mLocsToShow.get(i).type);
			if (originalMeal != null)
				break;
		}
		int lastNewMealType = originalMeal.type;
		// create meal array
		mMealArr = new SparseArray<DatedMealTime>();
		mMealArr.put(lastNewMealType, originalMeal);
		for (Location l : mLocsToShow) {
			if (l.type != lastNewMealType) {
				mMealArr.put(l.type, mTP.swapType(originalMeal, l.type));
				lastNewMealType = l.type;
			}
		}
		
		mAdapter = new MealViewListAdapter(getActivity(), mMealArr, mLocsToShow);
		setListAdapter(mAdapter);
		
		// cache loader args
		loaderArgs = new ArrayList<Bundle>(mLocsToShow.size());
		for (Location l : mLocsToShow) {
			DatedMealTime meal = mMealArr.get(l.type);
			// set arguments
			Bundle loaderArg = new Bundle();
			if (meal == null) { // meal does not exist
				loaderArg.putBoolean(EXTRA_MEALEXISTS, false);
			} else {
				loaderArg.putBoolean(EXTRA_MEALEXISTS, true);
				loaderArg.putInt(EXTRA_LOCATIONID, l.ID);
				loaderArg.putString(EXTRA_DATE, meal.date.toString());
				loaderArg.putString(EXTRA_MEALNAME, meal.mealName);
			}
			loaderArgs.add(loaderArg);
		}
		
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
	
	// launch ViewByLocation if a location is clicked
	@Override
	public void onListItemClick(ListView l, View v, int pos, long id) {
		if (id >= 0) { // location name
			Intent intent = new Intent(getActivity(), ViewByLocation.class);
			intent.putExtra(EXTRA_LOCATIONID, (int) id);
			intent.putExtra(EXTRA_DATE, mDate);
			startActivity(intent);
		} else if (pos == 0) { // toggle date
			mAdapter.toggleDateFormat();
			Intent toggleDate = new Intent();
			toggleDate.setAction(C.ACTION_DATEFORMATTOGGLED);
			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(toggleDate);
		}
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
		// register receiver
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(bCastReceiver, sIntentFilter);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mRefreshIcon.clearAnimation();
		// unregister receiver
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(bCastReceiver);
	}
	
	@Override
	public void onActivityResult(int reqCode, int resCode, Intent data) {
		if (resCode != Activity.RESULT_OK)
			return;
		
		Toast.makeText(getActivity(), "Alert created", Toast.LENGTH_SHORT).show();
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
    			// send broadcast
    			Intent refreshStart = new Intent();
    			refreshStart.setAction(C.ACTION_REFRESHSTART);
    			refreshStart.putExtra(EXTRA_DATE, mDate);
    			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(refreshStart);
    			// start animation
    			mRefreshIcon.startAnimation(mRotation);
    			mRefreshIcon.setVisibility(View.VISIBLE);
    			// send refresh request to service
    			// sets service refresh status
    			refreshLocations();
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
    		MenuInflater inflater = getActivity().getMenuInflater();
    		inflater.inflate(R.menu.context_menuitem, menu);
    	}
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	String locName;
    	String itemName = ((FoodItem) getListAdapter().getItem(info.position)).itemName;
    	switch (item.getItemId()) {
    	case R.id.share:
    		locName = ((MealViewListAdapter) getListAdapter()).getLocation(info.position).nickname;
    		Intent share = new Intent(Intent.ACTION_SEND);
    		share.setType("text/plain");
    		share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    		share.putExtra(Intent.EXTRA_SUBJECT, "Entree at " + locName);
    		StringBuilder body = new StringBuilder();
    		Date dateShown = new Date(mDate);
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
    		add.putExtra(EXTRA_ALERTLOC, ((MealViewListAdapter)
    				getListAdapter()).getLocation(info.position).ID);
    		add.putExtra(EXTRA_MEALNAME, mealName);

    		startActivityForResult(add, 0);
    		return true;
    	default:
    		return super.onContextItemSelected(item);
    	}
    }
    
    //----------------------------------------------------------------------------------------------------------
    
    private boolean isRefreshing() {
    	for (Location l : mLocsToShow) {
    		if (mMealArr.get(l.type) == null) // since we do not request refreshes of nonexistent meals
    			continue;
    		if (MenuProvider.isRefreshing(String.valueOf(l.ID), mDate))
    			return true;
    	}
    	return false;
    }
    
    // send refresh requests to service
    private void refreshLocations() {
    	MealTimeProvider mTP = MealTimeProviderFactory.newMealTimeProvider();
    	failedToastDisplayed.put(mDate, false);
    	for (Location l : mLocsToShow) {
    		DatedMealTime meal = mMealArr.get(l.type);
    		if (meal != null) {
    			MenuProvider.startRefresh(String.valueOf(l.ID), mDate);
    			Intent dlService = new Intent(getActivity(), MenuDownloaderService.class);
    			dlService.putExtra(EXTRA_LOCATIONID, String.valueOf(l.ID));
    			dlService.putExtra(EXTRA_LOCATIONNAME, l.locName);
    			dlService.putExtra(EXTRA_LOCATIONNUMBER, l.locNum);
    			dlService.putExtra(EXTRA_DATE, mDate);
    			dlService.putExtra(EXTRA_ISREFRESH, true);
    			dlService.putExtra(EXTRA_MEALNAMES, mTP.getDaysMealNames(l.type, meal.date.weekDay));
    			
    			getActivity().startService(dlService);
    		}
    	}
    }
    
    //---------------------------------------------LOADER CALLBACKS---------------------------------------------
    
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String select;
		String[] selectArgs;
		if (args.getBoolean(EXTRA_MEALEXISTS)) {
			select = "((" + PMealsDatabase.LOCATIONID + "=?) and ("
					+ PMealsDatabase.DATE + "=?) and (" + PMealsDatabase.MEALNAME + "=?))";
			selectArgs = new String[] { String.valueOf(args.getInt(EXTRA_LOCATIONID)),
					args.getString(EXTRA_DATE),
					args.getString(EXTRA_MEALNAME)
			};
		} else {
			select = "((" + PMealsDatabase.ITEMNAME + "=?) and (" + 
					PMealsDatabase.ITEMERROR + "=?))";
			// need the second string for locking purposes in the content provider
			selectArgs = new String[] { STRING_CLOSED, "1" };
		}
		
		return new CursorLoader(getActivity(), MenuProvider.CONTENT_URI,
				projection, select, selectArgs, null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.swapCursor(cursor, loader.getId());
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null, loader.getId());
	}
	
    //----------------------------------------------------------------------------------------------------------
}
