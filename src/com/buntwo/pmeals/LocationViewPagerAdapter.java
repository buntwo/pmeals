package com.buntwo.pmeals;

import static com.buntwo.pmeals.data.C.EXTRA_DATE;
import static com.buntwo.pmeals.data.C.EXTRA_LOCATIONID;
import static com.buntwo.pmeals.data.C.VBL_NUMLISTS_AFTER;
import static com.buntwo.pmeals.data.C.VBL_NUMLISTS_BEFORE;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.buntwo.pmeals.data.Date;
import com.buntwo.pmeals.data.Location;
import com.buntwo.pmeals.data.MealTimeProvider;
import com.buntwo.pmeals.data.MealTimeProviderFactory;

public class LocationViewPagerAdapter extends FragmentPagerAdapter {
	
	private final Location mLoc;
	private MealTimeProvider mTP;
	private final int mPagerId;
	
	private ArrayList<Date> dates;
	
	public LocationViewPagerAdapter(Location l, FragmentManager fm) {
		super(fm);
		mLoc = l;
		mPagerId = mLoc.hashCode();
		
		// should already be initialized
		mTP = MealTimeProviderFactory.newMealTimeProvider();
		
		// add dates
		dates = new ArrayList<Date>();
		Date today = mTP.getCurrentMeal(mLoc.type).date;
		dates.add(today);
		Date yesterday = today;
		for (int i = 0; i < VBL_NUMLISTS_BEFORE; ++i) {
			yesterday = new Date(yesterday);
			--yesterday.monthDay;
			yesterday.normalize(true);
			dates.add(0, yesterday);
		}
		Date tmrw = today;
		for (int i = 0; i < VBL_NUMLISTS_AFTER; ++i) {
			tmrw = new Date(tmrw);
			++tmrw.monthDay;
			tmrw.normalize(true);
			dates.add(tmrw);
		}
	}
	
	// return the index of today in the our list
	// if not found, return -1
	// well it just returns the number of lists before today, and since it's
	// 0-indexed, this works
	public int getTodayIndex() {
		return VBL_NUMLISTS_BEFORE;
	}
	
	public Date getDate(int pos) {
		return dates.get(pos);
	}
	
	@Override
	public int getCount() {
		return dates.size();
	}
	
	@Override
	public Fragment getItem(int pos) {
		LocationViewListFragment lf = new LocationViewListFragment();
		Bundle args = new Bundle();
		args.putInt(EXTRA_LOCATIONID, mLoc.ID);
		args.putString(EXTRA_DATE, dates.get(pos).toString());
		lf.setArguments(args);
		
		return lf;
	}
	
	@Override
	public long getItemId(int position) {
		return mPagerId*100 + position;
	}

}
