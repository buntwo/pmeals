package com.buntwo.pmeals.data;

public class FoodItem {

	public final String itemName;
	public final boolean error;
	public final boolean isVegan;
	public final boolean isVegetarian;
	public final boolean hasPork;
	public final boolean hasNuts;
	public final boolean isEFriendly;
	
	// params is { isVegan, isVegetarian, hasPork, hasNuts, isEFriendly }
	public FoodItem(String s, boolean err, boolean[] params) {
		itemName = s;
		error = err;
		this.isVegan = params[0];
		this.isVegetarian = params[1];
		this.hasPork = params[2];
		this.hasNuts = params[3];
		this.isEFriendly = params[4];
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
