package com.boardgamegeek.ui;

import android.support.v4.app.Fragment;

import com.boardgamegeek.R;

public class GeekListsActivity extends TopLevelSinglePaneActivity {
	@Override
	protected Fragment onCreatePane() {
		return new GeekListsFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_geeklists;
	}
}
