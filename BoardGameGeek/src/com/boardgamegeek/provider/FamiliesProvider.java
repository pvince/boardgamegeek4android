package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Families;
import com.boardgamegeek.provider.BggDatabase.Tables;

public class FamiliesProvider extends BasicProvider {

	@Override
	protected String getDefaultSortOrder() {
		return Families.DEFAULT_SORT;
	}

	@Override
	protected Integer getInsertedId(ContentValues values) {
		return values.getAsInteger(Families.FAMILY_ID);
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_FAMILIES;
	}

	@Override
	protected String getTable() {
		return Tables.FAMILIES;
	}

	@Override
	protected String getType(Uri uri) {
		return Families.CONTENT_TYPE;
	}
}