package com.boardgamegeek.sorter;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class YearPublishedAscendingSorter extends YearPublishedSorter {
	public YearPublishedAscendingSorter(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.YEAR_PUBLISHED, false);
		mSubDescriptionId = R.string.oldest;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_YEAR_PUBLISHED_ASC;
	}
}
