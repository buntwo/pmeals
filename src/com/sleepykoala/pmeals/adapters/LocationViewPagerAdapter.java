package com.sleepykoala.pmeals.adapters;

import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_FRAGMENTNUM;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.PAGES_TO_LOAD;
import static com.sleepykoala.pmeals.data.C.TOTAL_PAGES;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.fragments.LocationViewListFragment;

public class LocationViewPagerAdapter extends FragmentStatePagerAdapter {
	
	private final Location mLoc;
	
	private Date[] dates;
	private static final int MIDDLE = TOTAL_PAGES / 2;
	
	public LocationViewPagerAdapter(Location l, Date centerDate, FragmentManager fm) {
		super(fm);
		mLoc = l;
		
		// add dates
		dates = new Date[TOTAL_PAGES];
		dates[MIDDLE] = centerDate;
		// load prev/next dates
		Date yesterday = centerDate;
		Date tmrw = centerDate;
		for (int i = 1; i <= PAGES_TO_LOAD; ++i) {
			yesterday = getYesterday(yesterday);
			dates[MIDDLE - i] = yesterday;
			tmrw = getTomorrow(tmrw);
			dates[MIDDLE + i] = tmrw;
		}
	}
	
	// return the index of today in the our list
	// if not found, return -1
	// well it just returns the number of lists before today, and since it's
	// 0-indexed, this works
	public int getDateIndex(Date dt) {
		if (dt.equals(dates[MIDDLE]))
			return MIDDLE;
		for (int i = 1; i <= MIDDLE; ++i) {
			if (dt.equals(dates[MIDDLE + i]))
				return MIDDLE + i;
			if (dt.equals(dates[MIDDLE - i]))
				return MIDDLE - i;
		}
		return -1;
	}
	
	// returns the middle index
	public int getMiddleIndex() {
		return MIDDLE;
	}
	
	public Date getDate(int pos) {
		return dates[pos];
	}
	
	@Override
	public int getCount() {
		return TOTAL_PAGES;
	}
	
	@Override
	public Fragment getItem(int pos) {
		LocationViewListFragment lf = new LocationViewListFragment();
		Bundle args = new Bundle();
		args.putInt(EXTRA_LOCATIONID, mLoc.ID);
		Date date = dates[pos];
		
		if (date == null) {
			if (pos > MIDDLE) {
				int lastNotNull;
				for (lastNotNull = pos - 1; lastNotNull >= MIDDLE; --lastNotNull)
					if (dates[lastNotNull] != null)
						break;
				for (int j = lastNotNull; j < pos; ++j)
					dates[j + 1] = getTomorrow(dates[j]);
			} else {
				int lastNotNull;
				for (lastNotNull = pos + 1; lastNotNull <= MIDDLE; ++lastNotNull)
					if (dates[lastNotNull] != null)
						break;
				for (int j = lastNotNull; j > pos; --j)
					dates[j - 1] = getYesterday(dates[j]);
			}
			date = dates[pos];
		}
		args.putString(EXTRA_DATE, date.toString());
		args.putInt(EXTRA_FRAGMENTNUM, pos);
		lf.setArguments(args);
		
		// load next/prev dates if needed
		if (pos < TOTAL_PAGES - PAGES_TO_LOAD && dates[pos + PAGES_TO_LOAD] == null)
			dates[pos + PAGES_TO_LOAD] = getTomorrow(dates[pos + PAGES_TO_LOAD - 1]);
		if (pos > PAGES_TO_LOAD && dates[pos - PAGES_TO_LOAD] == null)
			dates[pos - PAGES_TO_LOAD] = getYesterday(dates[pos - PAGES_TO_LOAD + 1]);
		
		return lf;
	}
	
	private Date getTomorrow(Date d) {
		Date ret = new Date(d);
		++ret.monthDay;
		ret.normalize(true);
		
		return ret;
	}
	
	private Date getYesterday(Date d) {
		Date ret = new Date(d);
		--ret.monthDay;
		ret.normalize(true);
		
		return ret;
	}

}
