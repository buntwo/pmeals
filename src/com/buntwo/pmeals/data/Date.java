package com.buntwo.pmeals.data;

import android.text.format.DateFormat;
import android.text.format.Time;

public class Date extends Time {

	// new date that points to today
	public Date() {
		super();
		setToNow();
		dateify();
	}
	
	// copy constructor
	public Date(Date d) {
		super(d);
		dateify();
	}
	
	// copy-ish constructor
	public Date(Time t) {
		super(t);
		dateify();
	}
	
	// get a date from a date string in MM/dd/yyyy form
	// hackish, lol
	public Date(String date) {
		// remember month is 0-indexed!!!!!!
		this(
		Integer.parseInt(date.substring(0, 2)) - 1, // month
		Integer.parseInt(date.substring(3, 5)),		// monthDay
		Integer.parseInt(date.substring(6))			// year
		);
	}
	
	// generate date from given data.
	// NOTE: month is from [0-11] !!
	public Date(int aMonth, int aMonthDay, int aYear) {
		month = aMonth;
		monthDay = aMonthDay;
		year = aYear;
		dateify();
	}
	
	//--------------------------------------PUBLIC METHODS-------------------------------------
			
	// is the given date tomorrow?
	public boolean isTomorrow(Date d) {
		Date d_ = new Date(d);
		--d_.monthDay;
		d_.normalize(true);
		return equals(d_);
	}
	
	// is the given date yesterday?
	public boolean isYesterday(Date d) {
		Date d_ = new Date(d);
		++d_.monthDay;
		d_.normalize(true);
		return equals(d_);
	}
	
	//------------------------------------------------------------------------------------------
	
	private void dateify() {
		allDay = true;
		hour = minute = second = 0;
		normalize(true);
	}
	
	public String toString() {
		return DateFormat.format("MM/dd/yyyy", toMillis(false)).toString();
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Date))
			return false;
		Date d = (Date) obj;
		return month == d.month &&
				monthDay == d.monthDay &&
				year ==  d.year &&
				weekDay == d.weekDay;
	}
	
	public int hashCode() {
		int code = 17;
		
		code = code * 31 + month;
		code = code * 31 + monthDay;
		code = code * 31 + year;
		code = code * 31 + weekDay;
		
		return code;
	}
}
