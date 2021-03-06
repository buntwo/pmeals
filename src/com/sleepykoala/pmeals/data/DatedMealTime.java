package com.sleepykoala.pmeals.data;

import android.text.format.Time;

public class DatedMealTime extends MealTime {

	public final Date date;
	
	// time is set to be allDay
	public DatedMealTime(String name, long start, long end, int type, Time tm) {
		super(name, type);
		this.date = new Date(tm);
		setProperTimes(start, end, tm);
	}
	
	// construct new meal with same date but different name
	public DatedMealTime(DatedMealTime meal, String newName) {
		super(newName, meal.startTime, meal.endTime, meal.type);
		this.date = new Date(meal.date);
	}
	
	// copy-ish constructor
	public DatedMealTime(MealTime mt, Time tm) {
		super(mt.mealName, mt.type);
		this.date = new Date(tm);
		setProperTimes(mt.startTime, mt.endTime, tm);
	}
	
	// copy constructor
	public DatedMealTime(DatedMealTime dmt) {
		this(dmt.mealName, dmt.startTime, dmt.endTime, dmt.type, dmt.date);
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof DatedMealTime))
			return false;
		DatedMealTime d = (DatedMealTime) obj;
		return date.equals(d.date) &&
				type == d.type &&
				mealName.equals(d.mealName);
	}
	
	public int hashCode() {
		int code = 17;
		
		code = code * 31 + mealName.hashCode();
		code = code * 31 + type;
		code = code * 31 + date.hashCode();
		
		return code;
	}
}