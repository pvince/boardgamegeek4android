package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class WishlistPrioritySorter extends CollectionSorter {
	String[] mPriorityText;

	public WishlistPrioritySorter(Context context) {
		super(context);
		mPriorityText = mContext.getResources().getStringArray(R.array.wishlist_priority);
		mOrderByClause = getClause(Collection.STATUS_WISHLIST_PRIORITY, false);
		mDescriptionId = R.string.menu_collection_sort_wishlist_priority;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_WISHLIST_PRIORITY;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.STATUS_WISHLIST_PRIORITY };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		int level = getInt(cursor, Collection.STATUS_WISHLIST_PRIORITY);
		if (level >= mPriorityText.length) {
			level = 0;
		}
		return mPriorityText[level];
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getIntAsString(cursor, Collection.STATUS_WISHLIST_PRIORITY, "?", true) + " - " + getHeaderText(cursor);
	}
}
