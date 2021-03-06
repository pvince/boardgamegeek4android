package com.boardgamegeek.util;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Constants;

/**
 * Methods to aid in presenting information in a consistent manner.
 */
public class PresentationUtils {
	private PresentationUtils() {
	}

	public static CharSequence describePastTimeSpan(long time, String defaultValue) {
		if (time == 0) {
			return defaultValue;
		}
		return DateUtils.getRelativeTimeSpanString(time);
	}

	public static CharSequence describePastTimeSpan(long time, String defaultValue, String prefix) {
		if (time == 0) {
			return defaultValue;
		}
		return prefix + " " + DateUtils.getRelativeTimeSpanString(time);
	}

	/**
	 * Given the year, return a string interpretation.
	 */
	public static String describeYear(Context context, int year) {
		if (year > 0) {
			return context.getString(R.string.year_positive, year);
		} else if (year == Constants.YEAR_UNKNOWN) {
			return context.getString(R.string.year_zero, year);
		} else {
			return context.getString(R.string.year_negative, -year);
		}
	}

	/**
	 * Describe the priority of the wishlist.
	 */
	public static String describeWishlist(Context context, int priority) {
		if (priority < 0 || priority > 5) {
			return context.getString(R.string.wishlist);
		}
		return context.getResources().getStringArray(R.array.wishlist_priority)[priority];
	}

	/**
	 * Build a displayable full name from the first and last name.
	 */
	public static String buildFullName(String firstName, String lastName) {
		if (TextUtils.isEmpty(firstName) && TextUtils.isEmpty(lastName)) {
			return "";
		} else if (TextUtils.isEmpty(firstName)) {
			return lastName.trim();
		} else if (TextUtils.isEmpty(lastName)) {
			return firstName.trim();
		} else {
			return firstName.trim() + " " + lastName.trim();
		}
	}
}
