package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Families;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.GamesFamilies;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdFamiliesProvider extends BaseProvider {
	private static final String TABLE = Tables.GAMES_FAMILIES;

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		final int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(Tables.GAMES_FAMILIES_JOIN_FAMILIES)
			.mapToTable(Families._ID, Tables.FAMILIES).mapToTable(Families.FAMILY_ID, Tables.FAMILIES)
			.whereEquals(Tables.GAMES_FAMILIES + "." + GamesFamilies.GAME_ID, gameId);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(TABLE).whereEquals(GamesFamilies.GAME_ID, gameId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Families.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/#/families";
	}

	@Override
	protected String getType(Uri uri) {
		return Families.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		values.put(GamesFamilies.GAME_ID, Games.getGameId(uri));
		long rowId = db.insertOrThrow(TABLE, null, values);
		return Games.buildFamilyUri(rowId);
	}
}
