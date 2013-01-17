package com.buntwo.pmeals;

import static com.buntwo.pmeals.data.C.EXTRA_DATE;
import static com.buntwo.pmeals.data.C.EXTRA_ISREFRESH;
import static com.buntwo.pmeals.data.C.EXTRA_LOCATIONID;
import static com.buntwo.pmeals.data.C.EXTRA_LOCATIONNAME;
import static com.buntwo.pmeals.data.C.EXTRA_LOCATIONNUMBER;
import static com.buntwo.pmeals.data.C.EXTRA_MEALEXISTS;
import static com.buntwo.pmeals.data.C.EXTRA_MEALNAME;
import static com.buntwo.pmeals.data.C.EXTRA_MEALNAMES;
import static com.buntwo.pmeals.data.C.STRING_NOMEALSTODAY;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.buntwo.pmeals.contentprovider.MenuProvider;
import com.buntwo.pmeals.data.C;
import com.buntwo.pmeals.data.Date;
import com.buntwo.pmeals.data.DatedMealTime;
import com.buntwo.pmeals.data.Location;
import com.buntwo.pmeals.data.LocationProvider;
import com.buntwo.pmeals.data.LocationProviderFactory;
import com.buntwo.pmeals.data.MealTimeProvider;
import com.buntwo.pmeals.data.MealTimeProviderFactory;
import com.buntwo.pmeals.data.PMealsDatabase;
import com.buntwo.pmeals.service.MenuDownloaderService;

public class LocationViewListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	
	//private static final String TAG = "LocationViewListFragment";
	
	private static final IntentFilter sIntentFilter;
	// create intent filter
	static {
		sIntentFilter = new IntentFilter();
		sIntentFilter.addAction(C.ACTION_NEW_DAY);
		sIntentFilter.addAction(C.ACTION_REFRESHDONE);
		sIntentFilter.addAction(C.ACTION_REFRESHFAILED);
		sIntentFilter.addAction(C.ACTION_DATEFORMATTOGGLED);
	}
	private final BroadcastReceiver timeChangedReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(C.ACTION_NEW_DAY)) {
				((LocationViewListAdapter) getListAdapter()).newDay();
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
	
	private LocationViewListAdapter mAdapter;
	
	private ArrayList<Bundle> loaderArgs;
	private String mDate;
	private Location mLoc;
	private ArrayList<String> mDaysMealNames;
	
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
		mAdapter = new LocationViewListAdapter(getActivity(), daysMeals, dt);
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
		String[] projection = { PMealsDatabase.ITEMNAME, PMealsDatabase.ITEMERROR,
				PMealsDatabase.ITEMVEGAN,
				PMealsDatabase.ITEMVEGETARIAN,
				PMealsDatabase.ITEMPORK,
				PMealsDatabase.ITEMNUTS,
				PMealsDatabase.ITEMEFRIENDLY,
		};

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
			selectArgs = new String[] { STRING_NOMEALSTODAY, "1" };
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

}
