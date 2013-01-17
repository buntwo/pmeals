package com.sleepykoala.pmeals;

import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.VBL_NUMLISTS_AFTER;
import static com.sleepykoala.pmeals.data.C.VBL_NUMLISTS_BEFORE;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.Location;

public class LocationViewPagerAdapter extends FragmentPagerAdapter {
	
	private final Location mLoc;
	private final int mPagerId;
	
	private ArrayList<Date> dates;
	
	public LocationViewPagerAdapter(Location l, Date centerDate, FragmentManager fm) {
		super(fm);
		mLoc = l;
		mPagerId =  l.hashCode() + centerDate.hashCode();
		
		// add dates
		dates = new ArrayList<Date>();
		dates.add(centerDate);
		Date yesterday = centerDate;
		for (int i = 0; i < VBL_NUMLISTS_BEFORE; ++i) {
			yesterday = new Date(yesterday);
			--yesterday.monthDay;
			yesterday.normalize(true);
			dates.add(0, yesterday);
		}
		Date tmrw = centerDate;
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
	public int getMealIndex(Date dt) {
		for (int i = 0; i < dates.size(); ++i) {
			if (dates.get(i).equals(dt))
				return i;
		}
		return -1;
	}
	
	// returns the middle index
	public int getMiddleIndex() {
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
