package com.buntwo.pmeals.data;

import static com.buntwo.pmeals.data.C.MEAL_NAMES;

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
    private static final String TAG_WEEKDAY = "weekDay";
    private static final String TAG_SATURDAY = "saturday";
    private static final String TAG_SUNDAY = "sunday";
    private static final String TAG_TYPE  = "type";
    
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
								case 3: // type, weekDay, saturday, or sunday
									String tagName = p.getName();
									if (tagName.equals(TAG_TYPE)) {
										type = Integer.parseInt(p.nextText());
									} else if (tagName.equals(TAG_WEEKDAY)) {
										// after this, parser should be at </weekDay>
										ArrayList<MealTime> weekDay = processDay(type, p);
										// add to weekdays
										for (int i = Time.MONDAY; i <= Time.FRIDAY; ++i)
											newWeek[i] = weekDay;
									} else if (tagName.equals(TAG_SATURDAY)) {
										newWeek[Time.SATURDAY] = processDay(type, p);
									} else if (tagName.equals(TAG_SUNDAY)) {
										newWeek[Time.SUNDAY] = processDay(type, p);
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
			if (eventType == XmlPullParser.START_TAG) {
				for (int i = 0; i < MEAL_NAMES.length; ++i) { // match meal name
					if (p.getName().equalsIgnoreCase(MEAL_NAMES[i])) {
						day.add(processMeal(i, type, p));
						break;
					}
				}
			}
			eventType = p.next();
		}
		return day;
	}
	
	// expects next tags to be <start> and <end>, in that order
	private static MealTime processMeal(int mealIndex, int type, XmlPullParser p) throws XmlPullParserException, IOException {
		int[] start = new int[2];
		int[] end = new int[2];
		while (p.next() != XmlPullParser.START_TAG); // <start>
		String time = p.nextText(); // start time
		start[0] = Integer.parseInt(time.substring(0,2));
		start[1] = Integer.parseInt(time.substring(3));
		while (p.next() != XmlPullParser.START_TAG); // <end>
		time = p.nextText(); // end time
		end[0] = Integer.parseInt(time.substring(0,2));
		end[1] = Integer.parseInt(time.substring(3));
		return new MealTime(mealIndex, start, end, type);
	}
}
