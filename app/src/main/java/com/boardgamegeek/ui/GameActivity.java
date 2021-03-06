package com.boardgamegeek.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.UpdateCompleteEvent;
import com.boardgamegeek.events.UpdateEvent;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.ui.GameFragment.GameInfo;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ShortcutUtils;

public class GameActivity extends SimpleSinglePaneActivity implements GameFragment.Callbacks {
	private static final int REQUEST_EDIT_PLAY = 1;
	private int mGameId;
	private String mGameName;
	private String mThumbnailUrl;
	private String mImageUrl;
	private boolean mCustomPlayerSort;
	private boolean mSyncing = false;
	private Menu mOptionsMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mGameId = Games.getGameId(getIntent().getData());
		changeName(getIntent().getStringExtra(ActivityUtils.KEY_GAME_NAME));

		new Handler().post(new Runnable() {
			@Override
			public void run() {
				ContentValues values = new ContentValues();
				values.put(Games.LAST_VIEWED, System.currentTimeMillis());
				getContentResolver().update(getIntent().getData(), values, null, null);
			}
		});
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GameFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.game;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mOptionsMenu = menu;
		updateRefreshStatus();
		menu.findItem(R.id.menu_log_play).setVisible(PreferencesUtils.showLogPlay(this));
		menu.findItem(R.id.menu_log_play_quick).setVisible(PreferencesUtils.showQuickLogPlay(this));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent upIntent = new Intent(this, HotnessActivity.class);
				if (Authenticator.isSignedIn(this)) {
					upIntent = new Intent(this, CollectionActivity.class);
				}
				if (shouldUpRecreateTask()) {
					TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
				} else {
					NavUtils.navigateUpTo(this, upIntent);
				}
				return true;
			case R.id.menu_language_poll:
				Bundle arguments = new Bundle(2);
				arguments.putInt(ActivityUtils.KEY_GAME_ID, mGameId);
				arguments.putString(ActivityUtils.KEY_TYPE, "language_dependence");
				DialogUtils.launchDialog(getFragment(), new PollFragment(), "poll-dialog", arguments);
				return true;
			case R.id.menu_share:
				ActivityUtils.shareGame(this, mGameId, mGameName);
				return true;
			case R.id.menu_shortcut:
				ShortcutUtils.createShortcut(this, mGameId, mGameName, mThumbnailUrl);
				return true;
			case R.id.menu_log_play:
				Intent intent = ActivityUtils.createEditPlayIntent(this, 0, mGameId, mGameName, mThumbnailUrl,
					mImageUrl);
				intent.putExtra(LogPlayActivity.KEY_CUSTOM_PLAYER_SORT, mCustomPlayerSort);
				startActivityForResult(intent, REQUEST_EDIT_PLAY);
				return true;
			case R.id.menu_log_play_quick:
				Toast.makeText(this, R.string.msg_logging_play, Toast.LENGTH_SHORT).show();
				ActivityUtils.logQuickPlay(this, mGameId, mGameName);
				return true;
			case R.id.menu_link_bgg:
				ActivityUtils.linkBgg(this, mGameId);
				return true;
			case R.id.menu_link_bg_prices:
				ActivityUtils.linkBgPrices(this, mGameName);
				return true;
			case R.id.menu_link_amazon:
				ActivityUtils.linkAmazon(this, mGameName);
				return true;
			case R.id.menu_link_ebay:
				ActivityUtils.linkEbay(this, mGameName);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean shouldUpRecreateTask() {
		return getIntent().getBooleanExtra(ActivityUtils.KEY_FROM_SHORTCUT, false);
	}

	@Override
	public void onGameInfoChanged(GameInfo gameInfo) {
		changeName(gameInfo.gameName);
		changeSubtype(gameInfo.subtype);
		mThumbnailUrl = gameInfo.thumbnailUrl;
		mImageUrl = gameInfo.imageUrl;
		mCustomPlayerSort = gameInfo.customPlayerSort;
	}

	private void changeName(String gameName) {
		mGameName = gameName;
		if (!TextUtils.isEmpty(gameName)) {
			getIntent().putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
			getSupportActionBar().setTitle(gameName);
		}
	}

	private void changeSubtype(String subtype) {
		if (subtype == null) {
			return;
		}
		int resId = R.string.title_game;
		switch (subtype) {
			case BggService.THING_SUBTYPE_BOARDGAME:
				resId = R.string.title_board_game;
				break;
			case BggService.THING_SUBTYPE_BOARDGAME_EXPANSION:
				resId = R.string.title_board_game_expansion;
				break;
			case BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY:
				resId = R.string.title_board_game_accessory;
				break;
		}
		getSupportActionBar().setSubtitle(getString(resId));
	}

	public void onEventMainThread(UpdateEvent event) {
		mSyncing =
			event.type == UpdateService.SYNC_TYPE_GAME ||
			event.type == UpdateService.SYNC_TYPE_GAME_COLLECTION ||
			event.type == UpdateService.SYNC_TYPE_GAME_PLAYS;
		updateRefreshStatus();
	}

	public void onEventMainThread(UpdateCompleteEvent event) {
		mSyncing = false;
		updateRefreshStatus();
	}

	private void updateRefreshStatus() {
		if (mOptionsMenu == null) {
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (mSyncing) {
				MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_indeterminate_progress);
			} else {
				MenuItemCompat.setActionView(refreshItem, null);
			}
		}
	}
}
