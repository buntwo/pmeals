package com.buntwo.pmeals.data;

public class FoodItem {

	public final String itemName;
	public final boolean error;
	
	public FoodItem(String s, boolean err) {
		itemName = s;
		error = err;
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof FoodItem))
			return false;
		FoodItem mi = (FoodItem) obj;
		return itemName.equals(mi.itemName) && error == mi.error;
	}
	
	public int hashCode() {
		int code = 17;
		
		code = code * 31 + itemName.hashCode();
		code = code * 31 + (error ? 1 : 0);
		
		return code;
	}
}
