package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.Families;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesFamiliesIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long id = ContentUris.parseId(uri);
		return new SelectionBuilder().table(Tables.GAMES_FAMILIES).whereEquals(BaseColumns._ID, id);
	}

	@Override
	protected String getPath() {
		return "games/families/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Families.CONTENT_ITEM_TYPE;
	}
}
