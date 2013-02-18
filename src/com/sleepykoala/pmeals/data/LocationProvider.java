package com.sleepykoala.pmeals.data;

import java.util.ArrayList;


// parses locations.xml, provides locations
public class LocationProvider {
	
	//private static final String DEBUG_TAG = "LocationProvider";
	
	// double array, top level is type, second level is locations
	private ArrayList<ArrayList<Location>> locations;

	public LocationProvider(ArrayList<ArrayList<Location>> aLocations) {
		locations = aLocations;
	}
	
	// return Location ArrayList for the given type(s)
	public ArrayList<Location> getLocationsForType(int...type) {
		ArrayList<Location> locs = new ArrayList<Location>();
		for (int i : type)
			locs.addAll(locations.get(i));
		return locs;
	}
	
	// return ArrayList of id's for given type(s)
	public ArrayList<Integer> getIDsForType(int...type) {
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int i : type) {
			for (Location l : locations.get(i))
				ids.add(l.ID);
		}
		return ids;
	}
	
	// return location with given ID
	// null if not found
	public Location getById(int id) {
		for (ArrayList<Location> arr : locations) {
			for (Location l : arr) {
				if (l.ID == id)
					return l;
			}
		}
		return null;
	}
	
	// return an arraylist of info about all locations on file
	// used in dropdown lists
	// type = 0, name
	//		= 1, nickname
	public ArrayList<String> getInfoArray(int type) {
		ArrayList<String> info = new ArrayList<String>();
		for (ArrayList<Location> arr : locations) {
			for (Location l : arr) {
				switch (type) {
				case 0:
					info.add(l.locName);
					break;
				case 1:
					info.add(l.nickname);
					break;
				}
			}
		}
		return info;
	}
	
	// return arraylist of non-dining hall locations
	public ArrayList<Location> getNonDiningHalls() {
		ArrayList<Location> ret = new ArrayList<Location>();
		for (int i = 3; i < locations.size(); ++i)
			ret.addAll(locations.get(i));
		return ret;
	}
	
	// return location with given index
	// null if not found
	public Location getByIndex(int pos) {
		int counter = 0;
		for (ArrayList<Location> arr : locations) {
			for (Location l : arr) {
				if (counter == pos)
					return l;
				++counter;
			}
		}
		return null;
	}
	
	// return index of location
	// -1 if not found
	public int getIndex(Location loc) {
		int counter = 0;
		for (ArrayList<Location> arr : locations) {
			for (Location l : arr) {
				if (loc.equals(l))
					return counter;
				++counter;
			}
		}
		return -1;
	}
	
	// translate a location ID to its index
	// right now it's the same
	public static int idToIndex(int ID) {
		return ID;
	}
	
	// is the given location a dining hall?
	// a dining hall is a location that has different menus depending on
	// what meal it is
	public static boolean isDiningHall(Location l) {
		return l.type <= 2;
	}
}
