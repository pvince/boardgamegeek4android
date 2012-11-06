package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;

public class PlaysFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(PlaysFragment.class);
	private static final int MENU_PLAY_EDIT = Menu.FIRST;
	private PlayAdapter mAdapter;
	private Uri mUri;
	private int mGameId;
	private boolean mAutoSyncTriggered;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setSelector(android.R.color.transparent);
		listView.setCacheColorHint(Color.WHITE);
		listView.setFastScrollEnabled(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(getListView());
		setEmptyText(getString(R.string.empty_plays));
		setListShown(false);

		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		if (uri != null && Games.isGameUri(uri)) {
			mGameId = Games.getGameId(uri);
			mUri = Games.buildPlaysUri(mGameId);
			getLoaderManager().restartLoader(GameQuery._TOKEN, getArguments(), this);
		} else {
			mGameId = BggContract.INVALID_ID;
			mUri = Plays.CONTENT_URI;
			if (DateTimeUtils.howManyHoursOld(BggApplication.getInstance().getLastPlaysSync()) > 4) {
				BggApplication.getInstance().putLastPlaysSync();
				triggerRefresh();
			}
		}

		getLoaderManager().restartLoader(PlaysQuery._TOKEN, getArguments(), this);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		launchPlay(cursor);
	}

	private void launchPlay(Cursor cursor) {
		int playId = cursor.getInt(PlaysQuery.PLAY_ID);
		Uri playUri = Plays.buildPlayUri(playId);
		Intent i = new Intent(Intent.ACTION_VIEW, playUri);
		i.putExtra(PlayActivity.KEY_GAME_ID, cursor.getInt(PlaysQuery.GAME_ID));
		i.putExtra(PlayActivity.KEY_GAME_NAME, cursor.getString(PlaysQuery.GAME_NAME));
		startActivity(i);
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			if (mAutoSyncTriggered) {
				Toast.makeText(getActivity(), R.string.msg_refresh_recent, Toast.LENGTH_LONG).show();
			} else {
				triggerRefresh();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			LOGE(TAG, "bad menuInfo", e);
			return;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return;
		}
		final String gameName = cursor.getString(PlaysQuery.GAME_NAME);

		menu.setHeaderTitle(gameName);
		menu.add(0, MENU_PLAY_EDIT, 0, R.string.menu_edit);
		// MenuItem mi = menu.add(0, MENU_PLAY_DELETE, 0, R.string.menu_delete);
		// mi.setEnabled(mLogInHelper.checkCookies());
		// TODO: add Send and Share menu items
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			LOGE(TAG, "bad menuInfo", e);
			return false;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return false;
		}

		switch (item.getItemId()) {
			case MENU_PLAY_EDIT: {
				ActivityUtils.logPlay(getActivity(), cursor.getInt(PlaysQuery.PLAY_ID),
					cursor.getInt(PlaysQuery.GAME_ID), cursor.getString(PlaysQuery.GAME_NAME));
				return true;
			}
			// case MENU_PLAY_DELETE: {
			// final int playId = cursor.getInt(Query.PLAY_ID);
			// AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// builder.setTitle(R.string.are_you_sure_title).setMessage(R.string.are_you_sure_delete_play)
			// .setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			// public void onClick(DialogInterface dialog, int id) {
			// ActivityUtils.deletePlay(PlaysActivity.this, mLogInHelper.getCookieStore(), playId);
			// }
			// }).setNegativeButton(R.string.no, null);
			// builder.create().show();
			// return true;
			// }
		}
		return false;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == PlaysQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), mUri, PlaysQuery.PROJECTION, null, null, null);
			loader.setUpdateThrottle(2000);
		} else if (id == GameQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), Games.buildGameUri(mGameId), GameQuery.PROJECTION, null, null,
				null);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new PlayAdapter(getActivity());
			if (Games.isGameUri(mUri)) {
				mAdapter.setRowResId(R.layout.row_play_game);
			}
			setListAdapter(mAdapter);
		}

		int token = loader.getId();
		if (token == PlaysQuery._TOKEN) {
			mAdapter.changeCursor(cursor);
		} else if (token == GameQuery._TOKEN) {
			if (!mAutoSyncTriggered && cursor != null && cursor.moveToFirst()) {
				mAutoSyncTriggered = true;
				long updated = cursor.getLong(GameQuery.UPDATED_PLAYS);
				if (updated == 0 || DateTimeUtils.howManyDaysOld(updated) > 2) {
					triggerRefresh();
				}
			}
		} else {
			LOGD(TAG, "Query complete, Not Actionable: " + token);
			cursor.close();
		}

		if (token != GameQuery._TOKEN) {
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == PlaysQuery._TOKEN) {
			mAdapter.changeCursor(null);
		}
	}

	public void triggerRefresh() {
		if (mGameId == BggContract.INVALID_ID) {
			SyncService.start(getActivity(), null, SyncService.SYNC_TYPE_PLAYS);
		} else {
			SyncService.start(getActivity(), null, SyncService.SYNC_TYPE_GAME_PLAYS, mGameId);
		}
	}

	private class PlayAdapter extends CursorAdapter {
		private LayoutInflater mInflater;
		private int mRowResId = R.layout.row_play;

		public PlayAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();
		}

		public void setRowResId(int resId) {
			mRowResId = resId;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(mRowResId, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.date.setText(cursor.getString(PlaysQuery.DATE));
			holder.name.setText(cursor.getString(PlaysQuery.GAME_NAME));
			holder.location.setText(cursor.getString(PlaysQuery.LOCATION));
			if (cursor.getInt(PlaysQuery.SYNC_STATUS) != Play.SYNC_STATUS_SYNCED) {
				view.setBackgroundResource(R.color.background_light);
			} else {
				view.setBackgroundResource(R.color.background);
			}
		}
	}

	static class ViewHolder {
		TextView name;
		TextView date;
		TextView location;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.list_name);
			date = (TextView) view.findViewById(R.id.list_date);
			location = (TextView) view.findViewById(R.id.list_location);
		}
	}

	private interface GameQuery {
		int _TOKEN = 0x22;
		String[] PROJECTION = { Games.UPDATED_PLAYS };
		int UPDATED_PLAYS = 0;
	}

	private interface PlaysQuery {
		int _TOKEN = 0x21;
		String[] PROJECTION = { BaseColumns._ID, Plays.PLAY_ID, Plays.DATE, PlayItems.NAME, PlayItems.OBJECT_ID,
			Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH, Plays.SYNC_STATUS, };
		int PLAY_ID = 1;
		int DATE = 2;
		int GAME_NAME = 3;
		int GAME_ID = 4;
		int LOCATION = 5;
		int SYNC_STATUS = 8;
	}
}