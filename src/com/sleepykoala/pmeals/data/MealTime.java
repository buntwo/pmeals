package com.sleepykoala.pmeals.data;

import android.text.format.Time;

public class MealTime {
	
	/* here, startTime and endTime are only to be used for reading
	 * times, not dates
	 */
	public long startTime;
	public long endTime;
	public final String mealName;
	public final int type;
	
	// Constructor
	// name is meal name, as reported on the XML feed
	// two longs, milliseconds since UTC epoch, for start and end times
	// first element is hour, second is minute.
	public MealTime(String name, long start, long end, int type) {
		startTime = start;
		endTime = end;
		mealName = name;
		this.type = type;
	}
	
	// just set name and type
	// used by DatedMealTime
	protected MealTime(String name, int type) {
		mealName = name;
		this.type = type;
	}
	
	// copy constructor
	public MealTime(MealTime mt) {
		startTime = mt.startTime;
		endTime = mt.endTime;
		mealName = mt.mealName;
		type = mt.type;
	}
	
	// given start and end time (in ms since epoch) and a Time/Date,
	// set the start/end times so the start time is in the given time's date
	public void setProperTimes(long start, long end, Time tm) {
		Time tm_ = new Time(tm);
		tm_.allDay = false;
		tm_.set(start);
		tm_.month = tm.month;
		tm_.monthDay = tm.monthDay;
		tm_.year = tm.year;
		startTime = tm_.toMillis(true);
		endTime = startTime + (end - start);
	}
	
	// after startTime/endTime have already been set,
	// set the start/end times so the start time is in the given time's date
	public void setProperTimes(Time tm) {
		Time tm_ = new Time(tm);
		long diff = endTime - startTime;
		tm_.allDay = false;
		tm_.set(startTime);
		tm_.month = tm.month;
		tm_.monthDay = tm.monthDay;
		tm_.year = tm.year;
		startTime = tm_.toMillis(true);
		endTime = startTime + diff;
	}
}
