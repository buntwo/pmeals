package com.buntwo.pmeals.data;

public class FoodItem {

	public final String itemName;
	public final boolean error;
	public final boolean[] foodInfo;
	
	// params is { isVegan, isVegetarian, hasPork, hasNuts, isEFriendly }
	public FoodItem(String s, boolean err, boolean[] params) {
		itemName = s;
		error = err;
		foodInfo = new boolean[5];
		for (int i = 0; i < 5; ++i)
			foodInfo[i] = params[i];
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
