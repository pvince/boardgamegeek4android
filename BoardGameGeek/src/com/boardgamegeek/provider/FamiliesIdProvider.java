package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Families;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class FamiliesIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int familyId = Families.getFamilyId(uri);
		return new SelectionBuilder().table(Tables.FAMILIES).whereEquals(Families.FAMILY_ID, familyId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(BggContract.PATH_FAMILIES);
	}

	@Override
	protected String getType(Uri uri) {
		return Families.CONTENT_ITEM_TYPE;
	}
}
