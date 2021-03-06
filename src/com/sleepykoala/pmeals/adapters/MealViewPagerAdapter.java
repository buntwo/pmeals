package com.sleepykoala.pmeals.adapters;

import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_FRAGMENTNUM;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONIDS;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAME;
import static com.sleepykoala.pmeals.data.C.PAGES_TO_PRELOAD;
import static com.sleepykoala.pmeals.data.C.TOTAL_PAGES;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.sleepykoala.pmeals.data.DatedMealTime;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.MealTimeProviderFactory;
import com.sleepykoala.pmeals.fragments.MealViewListFragment;

public class MealViewPagerAdapter extends FragmentStatePagerAdapter {
	
	//private static final String TAG = "MealViewPagerAdapter";
	
	private ArrayList<Integer> locIDsToShow;
	
	private DatedMealTime[] meals;
	private final MealTimeProvider mTP;
	private final int mealType;
	private static final int MIDDLE = TOTAL_PAGES / 2;
	
		
	public MealViewPagerAdapter(ArrayList<Integer> locIDs, DatedMealTime centerMeal,
			int mainType, FragmentManager fm) {
		super(fm);
		
		locIDsToShow = locIDs;
		meals = new DatedMealTime[TOTAL_PAGES];
		
		// should be initialized already
		mTP = MealTimeProviderFactory.newMealTimeProvider();
		
		mealType = mainType;
		meals[MIDDLE] = centerMeal;
		
		// add later and previous ones
		DatedMealTime prevMeal = centerMeal;
		DatedMealTime nextMeal = centerMeal;
		for (int i = 1; i <= PAGES_TO_PRELOAD; ++i) {
			prevMeal = mTP.getPreviousMeal(mealType, prevMeal);
			meals[MIDDLE - i] = prevMeal;
			nextMeal = mTP.getNextMeal(mealType, nextMeal);
			meals[MIDDLE + i] = nextMeal;
		}
	}
	
	// return the index of the given meal in the our list
	// if not found, return -1
	// start searching from middle, since usually this is called on
	// the current meal
	public int findMealIndex(DatedMealTime dmt) {
		if (dmt.equals(meals[MIDDLE]))
			return MIDDLE;
		for (int i = 1; i <= MIDDLE; ++i) {
			if (dmt.equals(meals[MIDDLE + i]))
				return MIDDLE + i;
			if (dmt.equals(meals[MIDDLE - i]))
				return MIDDLE - i;
		}
		return -1;
	}
	
	// returns the middle index
	public int getMiddleIndex() {
		return MIDDLE;
		
	}

	/**
	 * Get the meal at the specified position, loading them if needed.
	 * 
	 * @param pos Position of meal to retrieve
	 * @return DatedMealTime
	 */
	public DatedMealTime getMeal(int pos) {
		DatedMealTime meal = meals[pos];
		if (meal == null) {
			if (pos > MIDDLE) {
				int lastNotNull;
				for (lastNotNull = pos - 1; lastNotNull >= MIDDLE; --lastNotNull)
					if (meals[lastNotNull] != null)
						break;
				for (int j = lastNotNull; j < pos; ++j)
					meals[j + 1] = mTP.getNextMeal(mealType, meals[j]);
			} else {
				int lastNotNull;
				for (lastNotNull = pos + 1; lastNotNull <= MIDDLE; ++lastNotNull)
					if (meals[lastNotNull] != null)
						break;
				for (int j = lastNotNull; j > pos; --j)
					meals[j - 1] = mTP.getPreviousMeal(mealType, meals[j]);
			}
			meal = meals[pos];
		}
		return meal;
	}
	
	@Override
	public Fragment getItem(int pos) {
		MealViewListFragment lf = new MealViewListFragment();
		Bundle args = new Bundle();
		DatedMealTime meal = getMeal(pos);
		args.putIntegerArrayList(EXTRA_LOCATIONIDS, locIDsToShow);
		args.putString(EXTRA_DATE, meal.date.toString());
		args.putString(EXTRA_MEALNAME, meal.mealName);
		args.putInt(EXTRA_FRAGMENTNUM, pos);
		lf.setArguments(args);
		
		// load next/prev meals if needed
		if (pos < TOTAL_PAGES - PAGES_TO_PRELOAD && meals[pos + PAGES_TO_PRELOAD] == null)
			getMeal(pos + PAGES_TO_PRELOAD);
		if (pos > PAGES_TO_PRELOAD && meals[pos - PAGES_TO_PRELOAD] == null)
			getMeal(pos - PAGES_TO_PRELOAD);
		
		return lf;
	}
	
	@Override
	public int getCount() {
		return TOTAL_PAGES;
	}

	public void newLocs(ArrayList<Integer> locIDs) {
		locIDsToShow = locIDs;
		
	}
	
}
