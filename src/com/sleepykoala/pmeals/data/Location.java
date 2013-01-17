package com.sleepykoala.pmeals.data;

/*
 * Represents a dining location
 * locName is what is sent to the server
 * nickname is what is displayed
 * ID uniquely identifies the location
 */

public class Location {
	
	public final String locNum;
	public final String locName;
	public final String nickname;
	public final int ID;
	public final int type;
	
	public Location(String locNum, String locName, String nickName,
					int type, int ID) {
		this.locNum = locNum;
		this.locName = locName;
		this.nickname = nickName;
		this.type = type;
		this.ID = ID;
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Location))
			return false;
		Location l = (Location) obj;
		return ID == l.ID && type == l.type;
	}
	
	public int hashCode() {
		int code = 17;
		
		code = 31 * code + ID;
		code = 31 * code + type;
		
		return code;
	}
}
