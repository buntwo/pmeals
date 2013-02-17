package com.sleepykoala.pmeals.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.text.format.Time;

public class MealTimeProviderFactory {
	
	private static boolean isInitialized = false;
	
	// XML tags
    private static final String TAG_DAY = "day";
    private static final String TAG_TYPE = "type";
    private static final String TAG_MEAL = "meal";
    
    // data
	// each top level entry is like week, which is an array of 7 days
	private static ArrayList<ArrayList<MealTime>[]> mealTimes;
	
	private static final Object lock = new Object();
	
	// not instantiatable
	private MealTimeProviderFactory() {};

	public static MealTimeProvider newMealTimeProvider() {
		if (!isInitialized)
			throw new IllegalStateException("Not initialized");
		return new MealTimeProvider(mealTimes);
	}

	@SuppressWarnings("unchecked")
	public static void initialize(InputStream mealTimesXML) {
		if (!isInitialized) {
			synchronized (lock) {
				if (!isInitialized) { // this DCL SHOULD work, since boolean write/reads
									  // are atomic
					// parse the XML file
					ArrayList<MealTime>[] newWeek = null;

					XmlPullParser p = null;
					try {
						p = XmlPullParserFactory.newInstance().newPullParser();
						p.setInput(mealTimesXML, null);
						int type = -1;
						int eventType = p.getEventType();
						while (eventType != XmlPullParser.END_DOCUMENT) {
							switch (eventType) {
							case XmlPullParser.START_DOCUMENT:
								mealTimes = new ArrayList<ArrayList<MealTime>[]>();
								break;
							case XmlPullParser.START_TAG:
								switch (p.getDepth()) {
								case 2: // new week
									newWeek = new ArrayList[7];
									mealTimes.add(newWeek);
									break;
								case 3: // type or day
									String tagName = p.getName();
									if (tagName.equals(TAG_TYPE)) {
										type = Integer.parseInt(p.nextText());
									} else if (tagName.equals(TAG_DAY)) {
										int dayNum = Integer.valueOf(p.getAttributeValue(0));
										// after this, parser should be at </day>
										ArrayList<MealTime> day = processDay(type, p);
										newWeek[dayNum] = day;
									}
									break;
								}
								break;
							}
							eventType = p.next();
						}
					} catch (XmlPullParserException e) {
						throw new RuntimeException("XmlPullParserException");
					} catch (IOException e) {
						throw new RuntimeException("IOException");
					} finally { // release parser resources
						if (p != null) {
							try {
								p.setInput(null);
							} catch (XmlPullParserException e) {} // should never happen
						}
					}

					// initialized!
					isInitialized = true;
				}
			}
		}
	}
	
	//---------------------------------------HELPER METHODS-------------------------------------
	
	// process a day's worth of meals in the XML file
	// parser starts at the day's start tag
	private static ArrayList<MealTime> processDay(int type, XmlPullParser p) throws XmlPullParserException, IOException {
		ArrayList<MealTime> day = new ArrayList<MealTime>();
		String tagName = p.getName();
		
		int eventType = p.next();
		while (!((eventType == XmlPullParser.END_TAG) && p.getName().equals(tagName))) {
			if (eventType == XmlPullParser.START_TAG && p.getName().equals(TAG_MEAL))
				day.add(processMeal(type, p));
			eventType = p.next();
		}
		return day;
	}
	
	// expects next tags to be <start> and <end>, in that order
	private static MealTime processMeal(int type, XmlPullParser p) throws XmlPullParserException, IOException {
		String mealName = p.getAttributeValue(0);
		while (p.next() != XmlPullParser.START_TAG); // <start>
		String time = p.nextText(); // start time
		Time tm = new Time();
		tm.setToNow();
		tm.hour = Integer.parseInt(time.substring(0,2));
		tm.minute = Integer.parseInt(time.substring(3));
		long start = tm.toMillis(false);
		
		while (p.next() != XmlPullParser.START_TAG); // <end>
		time = p.nextText(); // end time
		tm.hour = Integer.parseInt(time.substring(0,2));
		tm.minute = Integer.parseInt(time.substring(3));
		long end = tm.toMillis(false);
		if (end < start) {
			++tm.monthDay;
			end = tm.toMillis(true);
		}
		
		return new MealTime(mealName, start, end, type);
	}
}
