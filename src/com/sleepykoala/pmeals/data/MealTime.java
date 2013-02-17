package com.sleepykoala.pmeals.data;

public class MealTime {
	
	public final int[] startTime;
	public final int[] endTime;
	public final String mealName;
	public final int type;
	
	// Constructor
	// takes int, which is index of name of meal in MEAL_NAMES
	// two two-int arrays, representing start and end times of this meal.
	// first element is hour, second is minute.
	public MealTime(String name, int[] start, int[] end, int type) {
		startTime = new int[2];
		startTime[0] = start[0];
		startTime[1] = start[1];
		endTime = new int[2];
		endTime[0] = end[0];
		endTime[1] = end[1];
		mealName = name;
		this.type = type;
	}
	
	// copy constructor
	public MealTime(MealTime mt) {
		startTime = new int[2];
		startTime[0] = mt.startTime[0];
		startTime[1] = mt.startTime[1];
		endTime = new int[2];
		endTime[0] = mt.endTime[0];
		endTime[1] = mt.endTime[1];
		mealName = mt.mealName;
		type = mt.type;
	}
}
