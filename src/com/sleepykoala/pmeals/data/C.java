package com.sleepykoala.pmeals.data;

// constants
public final class C {
	
	// cannot be instantiated
	private C() { }
	
	// menu provider handler codes
	public static final int CODE_REQUEST = 0;
	public static final int CODE_DATAAVAILABLE = 1;
	public static final int CODE_DOWNLOADFAILED = 2;
	
	// menu provider arguments
	// arg1
	public static final int ARG_FORCEREFRESH = 1;
	
	// requester handler codes
	public static final int CODE_REQUESTER_RECVDATA = 0;
	public static final int CODE_REQUESTER_DOWNLOADFAILED = 1;
	public static final int CODE_REQUESTER_REFRESH = 2;
	public static final int CODE_REQUESTER_REFRESHALL = 3;
	public static final int CODE_REQUESTER_UPDATETEXT = 4;
	
	// error FoodItem strings
	public static final String STRING_DOWNLOADFAILED = "Download failed";
	public static final String STRING_CLOSED = "Closed";
	public static final String STRING_NODATA = "No data";
	public static final String STRING_NOMEALSTODAY = "Closed today";
	
	// loading item strings (in ListView)
	public static final String STRING_LOADINGDATA = "Loading menu data";
	public static final String STRING_DOWNLOADING = "Downloading";
	
	// file names
    public static final String MEALTIMESXML = "mealTimes.xml";
    public static final String LOCATIONSXML = "locations.xml";
    
    // time constants
    public static final int MINUTES_END_ALERT = 30;
    public static final int MINUTES_START_ALERT = 10;
    public static final long ALERT_FADEIN_TIME = 500;
    public static final int ONEHOUR_RADIUS = 10; 	// within this many minutes of a meal starting/ending in 1 hour,
    												// infobar will show "about one hour"
    
	// number of lists in ViewPagers
	public static final int VBM_NUMLISTS_BEFORE = 2;
	public static final int VBM_NUMLISTS_AFTER = 2;
	public static final int VBL_NUMLISTS_BEFORE = 2;
	public static final int VBL_NUMLISTS_AFTER = 2;
	
	// extra flags
	public static final String EXTRA_LOCATIONID = "com.sleepykoala.pmeals.LOCATIONID";
	public static final String EXTRA_LOCATIONNUMBER = "com.sleepykoala.pmeals.LOCATIONNUMBER";
	public static final String EXTRA_LOCATIONNAME = "com.sleepykoala.pmeals.LOCATIONNAME";
	public static final String EXTRA_MEALNAMES = "com.sleepykoala.pmeals.MEALNAMES";
	public static final String EXTRA_DATE = "com.sleepykoala.pmeals.DATE";
	public static final String EXTRA_ISREFRESH = "com.sleepykoala.pmeals.ISREFRESH";
	public static final String EXTRA_TYPE = "com.sleepykoala.pmeals.TYPE";
	// NOTE: this is for an arraylist of ID's
	// the similarly named one above is for a single ID
	public static final String EXTRA_LOCATIONIDS = "com.sleepykoala.pmeals.LOCATIONIDS";
	// NOTE: similarly named one above
	public static final String EXTRA_MEALNAME = "com.sleepykoala.pmeals.MEALNAME";
	public static final String EXTRA_ISCURRENTMEAL = "com.sleepykoala.pmeals.ISCURRENTMEAL";
	public static final String EXTRA_MEALEXISTS = "com.sleepykoala.pmeals.MEALEXISTS";
	// alert extras
	public static final String EXTRA_ALERTNUM = "com.sleepykoala.pmeals.ALERTNUM";
	public static final String EXTRA_ALERTNUMS = "com.sleepykoala.pmeals.ALERTNUMS";
	public static final String EXTRA_ALERTSTATUS = "com.sleepykoala.pmeals.ALERTSTATUS";
	public static final String EXTRA_ALERTQUERY = "com.sleepykoala.pmeals.ALERTQUERY";
	public static final String EXTRA_ALERTREPEAT = "com.sleepykoala.pmeals.ALERTREPEAT";
	public static final String EXTRA_ALERTHOUR = "com.sleepykoala.pmeals.ALERTHOUR";
	public static final String EXTRA_ALERTMINUTE = "com.sleepykoala.pmeals.ALERTMINUTE";
	public static final String EXTRA_ALERTTIME = "com.sleepykoala.pmeals.ALERTTIME";
    
	// icon alpha constants
	public static final int ALPHA_ENABLED = 255;
	public static final int ALPHA_DISABLED = 77;
	
	// infobar colors
    public static final int MEAL_PASSED_COLOR = 0x222222;
    public static final int IN_MEAL_COLOR = 0x218211;
    public static final int BEFORE_MEAL_COLOR = 0x868686;
    public static final int START_ALERT_COLOR = 0xffbd00;
    public static final int END_ALERT_COLOR = 0xc60000;
    // listview non-main location colors
    public static final int MEAL_PASSED_COLOR_NONMAIN = 0xff555555;
    public static final int NO_ALERT_COLOR_NONMAIN = 0xffee7f2d;
    public static final int START_ALERT_COLOR_NONMAIN = 0xffffcc15;
    public static final int END_ALERT_COLOR_NONMAIN = 0xffff0b0b;
    public static final int COLOR_ERROR_ITEM = 0xff8a8a8a;
    public static final int COLOR_REGULAR_ITEM = 0xff000000;
    
    // intent actions
    public static final String ACTION_TIME_CHANGED = "com.sleepykoala.pmeals.action.TIME_CHANGED";
    public static final String ACTION_NEW_DAY = "com.sleepykoala.pmeals.action.NEW_DAY";
    public static final String ACTION_NEW_MEAL = "com.sleepykoala.pmeals.action.NEW_MEAL";
    public static final String ACTION_REFRESHSTART = "com.sleepykoala.pmeals.action.REFRESHSTART";
    public static final String ACTION_REFRESHDONE = "com.sleepykoala.pmeals.action.REFRESHDONE";
    public static final String ACTION_REFRESHFAILED = "com.sleepykoala.pmeals.action.REFRESHFAILED";
    public static final String ACTION_DATEFORMATTOGGLED = "com.sleepykoala.pmeals.action.DATEFORMATTOGGLED";
    public static final String ACTION_WIDGET_FORWARD = "com.sleepykoala.pmeals.action.WIDGETFORWARD";
    public static final String ACTION_WIDGET_BACKWARD = "com.sleepykoala.pmeals.action.WIDGETBACKWARD";
    public static final String ACTION_ABOUT = "com.sleepykoala.pmeals.action.ABOUT";
    // request codes
    public static final int REQCODE_REORDER = 1;
    
    // user's 12/24 hour status
    public static boolean IS24HOURFORMAT = false;
    
    // preference file name
	public static final String PREFSFILENAME = "PMealsPrefs";
    // preferences keys
    public static final String PREF_FIRSTTIME = "firsttime";
    public static final String PREF_LASTVER = "lastversion";
    public static final String PREF_LOCATIONORDER = "locorder";
    public static final String PREF_WIDGET_LOCID = "widget_locid";
    public static final String PREF_WIDGET_LOCNAME = "widget_locname";
    public static final String PREF_WIDGET_TYPE = "widget_type";
    public static final String PREF_STARTUPLOC = "startupShowId";
    public static final String PREF_NUMALERTS = "num_alerts";
    public static final String PREF_ALERTQUERY = "alert_query";
    public static final String PREF_ALERTON = "alert_on";
    public static final String PREF_ALERTREPEAT = "alert_repeat";
    public static final String PREF_ALERTHOUR = "alert_hour";
    public static final String PREF_ALERTMINUTE = "alert_minute";
    
    // feedback email
    public static final String FEEDBACK_EMAIL = "sleepykoala03@gmail.com";
}
