package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Families;
import com.boardgamegeek.provider.BggDatabase.GamesFamilies;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdFamiliesIdProvider extends BaseProvider {
	GamesIdFamiliesProvider mProvider = new GamesIdFamiliesProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long familyId = ContentUris.parseId(uri);
		return mProvider.buildSimpleSelection(uri).whereEquals(GamesFamilies.FAMILY_ID, familyId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(mProvider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return Families.CONTENT_ITEM_TYPE;
	}
}
