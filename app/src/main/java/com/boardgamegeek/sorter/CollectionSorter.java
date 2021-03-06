package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Collection;

public abstract class CollectionSorter extends Sorter {
	protected int mSubDescriptionId;

	public CollectionSorter(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		String description = super.getDescription();
		if (mSubDescriptionId > 0) {
			description += " - " + mContext.getString(mSubDescriptionId);
		}
		return description;
	}

	@Override
	protected String getDefaultSort() {
		return Collection.DEFAULT_SORT;
	}

	/**
	 * Gets the text to display on each row.
	 */
	public String getDisplayInfo(Cursor cursor) {
		return getHeaderText(cursor);
	}
}
