package com.sleepykoala.pmeals.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class LocationProviderFactory {
	
	// Xml parser tags
	private static final String TAG_LOCATION = "location";
	private static final String TAG_NAME = "name";
	private static final String TAG_NICKNAME = "nickname";
	private static final String TAG_LOCNUM = "num";
	private static final String TAG_TYPE = "type";
	private static final String TAG_ID = "id";
	
	// top level: type
	// second level: list of locations
	private static ArrayList<ArrayList<Location>> locations;

	private static boolean isInitialized = false;

	private static final Object lock = new Object();

	// not instantiatable
	private LocationProviderFactory() {};

	public static LocationProvider newLocationProvider() {
		if (!isInitialized)
			throw new IllegalStateException("Not initialized");
		return new LocationProvider(locations);
	}

	public static void initialize(InputStream locationsXML) {
		if (!isInitialized) {
			synchronized (lock) {
				if (!isInitialized) { // this DCL SHOULD work, since boolean write/reads
									  // are atomic
					XmlPullParser p = null;
					try {
						ArrayList<Location> currentType = null;
						p = XmlPullParserFactory.newInstance().newPullParser();
						p.setInput(locationsXML, null);
						int eventType = p.getEventType();
						while (eventType != XmlPullParser.END_DOCUMENT) {
							switch (eventType) {
							case XmlPullParser.START_DOCUMENT:
								locations = new ArrayList<ArrayList<Location>>();
								break;
							case XmlPullParser.START_TAG:
								if (p.getName().equals(TAG_LOCATION)) {
									Location newLoc = parseNewLocation(p);
									if (locations.size() != newLoc.type + 1) { // new type
										currentType = new ArrayList<Location>();
										locations.add(currentType);
									}
									currentType.add(newLoc);
								}
							}
							eventType = p.next();
						}
					} catch (XmlPullParserException e) {
						throw new RuntimeException("XmlPullParseException");
					} catch (IOException e) {
						throw new RuntimeException("IOException");
					} finally {
						if (p != null) {
							try {
								p.setInput(null);
							} catch (XmlPullParserException e) { } // should never happen
						}
					}

					// we are initialized!!
					isInitialized = true;
				}
			}
		}
	}
	
	//------------------------------------HELPER METHODS-----------------------------
	
	// given a parser at a <location> tag, parse and return the Location
	private static Location parseNewLocation(XmlPullParser p) throws XmlPullParserException, IOException {
		String tagName = p.getName();
		String name = "", locNum = "", nickname = "";
		int type = -1;
		int ID = -1;
		
		int eventType = p.next();
		while (!((eventType == XmlPullParser.END_TAG) && p.getName().equals(tagName))) {
			String fieldName = p.getName();
			if (eventType == XmlPullParser.START_TAG) {
				if (fieldName.equals(TAG_NAME))
					name = p.nextText();
				else if (fieldName.equals(TAG_NICKNAME))
					nickname = p.nextText();
				else if (fieldName.equals(TAG_LOCNUM))
					locNum = p.nextText();
				else if (fieldName.equals(TAG_TYPE))
					type = Integer.parseInt(p.nextText());
				else if (fieldName.equals(TAG_ID))
					ID = Integer.parseInt(p.nextText());
			}
			eventType = p.next();
		}
		if (name.equals("") || locNum.equals("") || (type == -1)) // malformed XML file
			throw new RuntimeException("Bad XML file!");
		
		return new Location(locNum, name, nickname, type, ID);
	}
	
}
