package com.boardgamegeek.sorter;

import java.text.DecimalFormat;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class MyRatingSorter extends CollectionSorter {
	private static final String DEFAULT_VALUE = "?";
	private DecimalFormat mDisplayFormat = new DecimalFormat("0.0");

	public MyRatingSorter(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.RATING, true);
        mDescriptionId = R.string.menu_collection_sort_myrating;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_MY_RATING;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.RATING };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		return getInfo(cursor, null);
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getInfo(cursor, mDisplayFormat);
	}

	private String getInfo(Cursor cursor, DecimalFormat format) {
		return getDoubleAsString(cursor, Collection.RATING, DEFAULT_VALUE, true, format);
	}
}
