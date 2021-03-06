package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.CollectionStatusChangedEvent;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.RetryableException;
import com.boardgamegeek.model.CollectionItem;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.Data;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.RandomUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import timber.log.Timber;

public class BuddyCollectionFragment extends StickyHeaderListFragment implements
	LoaderManager.LoaderCallbacks<BuddyCollectionFragment.BuddyCollectionData> {
	private static final int BUDDY_GAMES_LOADER_ID = 1;
	private static final String STATE_STATUS_VALUE = "buddy_collection_status_value";
	private static final String STATE_STATUS_LABEL = "buddy_collection_status_entry";
	private static final int MAX_RETRIES = 5;
	private static final int RETRY_BACKOFF = 100;

	private BuddyCollectionAdapter mAdapter;
	private SubMenu mSubMenu;
	private String mName;
	private String mStatusValue;
	private String mStatusLabel;
	private String[] mStatusValues;
	private String[] mStatusEntries;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mName = intent.getStringExtra(ActivityUtils.KEY_BUDDY_NAME);

		if (TextUtils.isEmpty(mName)) {
			Timber.w("Missing buddy name.");
			return;
		}

		mStatusEntries = getResources().getStringArray(R.array.pref_sync_status_entries);
		mStatusValues = getResources().getStringArray(R.array.pref_sync_status_values);

		setHasOptionsMenu(true);
		if (savedInstanceState == null) {
			mStatusValue = mStatusValues[0];
			mStatusLabel = mStatusEntries[0];
		} else {
			mStatusValue = savedInstanceState.getString(STATE_STATUS_VALUE);
			mStatusLabel = savedInstanceState.getString(STATE_STATUS_LABEL);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_buddy_collection));
		reload();
	}

	@Override
	public void onListItemClick(View convertView, int position, long id) {
		super.onListItemClick(convertView, position, id);
		int gameId = (int) convertView.getTag(R.id.id);
		String gameName = (String) convertView.getTag(R.id.game_name);
		ActivityUtils.launchGame(getActivity(), gameId, gameName);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(STATE_STATUS_VALUE, mStatusValue);
		outState.putString(STATE_STATUS_LABEL, mStatusLabel);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.buddy_collection, menu);
		MenuItem mi = menu.findItem(R.id.menu_collection_status);
		if (mi != null) {
			mSubMenu = mi.getSubMenu();
			if (mSubMenu != null) {
				for (int i = 0; i < mStatusEntries.length; i++) {
					mSubMenu.add(1, Menu.FIRST + i, i, mStatusEntries[i]);
				}
				mSubMenu.setGroupCheckable(1, true, true);
			}
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem mi = menu.findItem(R.id.menu_collection_random_game);
		if (mi != null) {
			if (mAdapter != null && mAdapter.getCount() > 0) {
				mi.setVisible(true);
			} else {
				mi.setVisible(false);
			}
		}
		// check the proper submenu item
		if (mSubMenu != null) {
			for (int i = 0; i < mSubMenu.size(); i++) {
				MenuItem smi = mSubMenu.getItem(i);
				if (smi.getTitle().equals(mStatusLabel)) {
					smi.setChecked(true);
					break;
				}
			}
		}
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		String status = "";
		int i = id - Menu.FIRST;
		if (i >= 0 && i < mStatusValues.length) {
			status = mStatusValues[i];
		} else if (id == R.id.menu_collection_random_game) {
			CollectionItem ci = mAdapter.getItem(RandomUtils.getRandom().nextInt(mAdapter.getCount()));
			ActivityUtils.launchGame(getActivity(), ci.gameId, ci.gameName());
			return true;
		}

		if (!TextUtils.isEmpty(status) && !status.equals(mStatusValue)) {
			mStatusValue = status;
			mStatusLabel = mStatusEntries[i];

			reload();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void reload() {
		EventBus.getDefault().postSticky(new CollectionStatusChangedEvent(mStatusLabel));
		if (mAdapter != null) {
			mAdapter.clear();
		}
		getActivity().supportInvalidateOptionsMenu();
		setListShown(false);
		getLoaderManager().restartLoader(BUDDY_GAMES_LOADER_ID, null, this);
	}

	@Override
	public Loader<BuddyCollectionData> onCreateLoader(int id, Bundle data) {
		return new BuddyGamesLoader(getActivity(), mName, mStatusValue);
	}

	@Override
	public void onLoadFinished(Loader<BuddyCollectionData> loader, BuddyCollectionData data) {
		if (getActivity() == null) {
			return;
		}

		List<CollectionItem> list = new ArrayList<>();
		if (data != null) {
			list = data.list();
		}

		if (mAdapter == null) {
			mAdapter = new BuddyCollectionAdapter(getActivity(), list);
			setListAdapter(mAdapter);
		} else {
			mAdapter.setCollection(list);
		}
		mAdapter.notifyDataSetChanged();
		getActivity().supportInvalidateOptionsMenu();

		if (data == null) {
			setEmptyText(getString(R.string.empty_buddy_collection));
		} else if (data.hasError()) {
			setEmptyText(data.getErrorMessage());
		} else {
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
			restoreScrollState();
		}
	}

	@Override
	public void onLoaderReset(Loader<BuddyCollectionData> loader) {
	}

	private static class BuddyGamesLoader extends BggLoader<BuddyCollectionData> {
		private BggService mService;
		private String mUsername;
		private Map<String, String> mOptions;

		public BuddyGamesLoader(Context context, String username, String status) {
			super(context);
			mService = Adapter.create();
			mUsername = username;
			mOptions = new HashMap<>();
			mOptions.put(status, "1");
			mOptions.put(BggService.COLLECTION_QUERY_KEY_BRIEF, "1");
		}

		@Override
		public BuddyCollectionData loadInBackground() {
			BuddyCollectionData collection = null;
			int retries = 0;
			while (true) {
				try {
					collection = new BuddyCollectionData(mService.collection(mUsername, mOptions));
					break;
				} catch (Exception e) {
					if (e.getCause() instanceof RetryableException) {
						retries++;
						if (retries > MAX_RETRIES) {
							break;
						}
						try {
							Timber.i("...retrying #" + retries);
							Thread.sleep(retries * retries * RETRY_BACKOFF);
						} catch (InterruptedException e1) {
							Timber.i("Interrupted while sleeping before retry " + retries);
							break;
						}
					} else {
						collection = new BuddyCollectionData(e);
					}
				}
			}
			return collection;
		}
	}

	static class BuddyCollectionData extends Data<CollectionItem> {
		private CollectionResponse mResponse;

		public BuddyCollectionData(CollectionResponse response) {
			mResponse = response;
		}

		public BuddyCollectionData(Exception e) {
			super(e);
		}

		@Override
		public List<CollectionItem> list() {
			if (mResponse == null || mResponse.items == null) {
				return new ArrayList<>();
			}
			return mResponse.items;
		}
	}

	public static class BuddyCollectionAdapter extends ArrayAdapter<CollectionItem> implements StickyListHeadersAdapter {
		private List<CollectionItem> mBuddyCollection;
		private LayoutInflater mInflater;

		public BuddyCollectionAdapter(Activity activity, List<CollectionItem> collection) {
			super(activity, R.layout.row_text_2, collection);
			mInflater = activity.getLayoutInflater();
			setCollection(collection);
		}

		public void setCollection(List<CollectionItem> games) {
			mBuddyCollection = games;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mBuddyCollection.size();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			BuddyGameViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_text_2, parent, false);
				holder = new BuddyGameViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (BuddyGameViewHolder) convertView.getTag();
			}

			CollectionItem game;
			try {
				game = mBuddyCollection.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (game != null) {
				holder.title.setText(game.gameName());
				holder.text.setText(String.valueOf(game.gameId));

				convertView.setTag(R.id.id, game.gameId);
				convertView.setTag(R.id.game_name, game.gameName());
			}
			return convertView;
		}

		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent) {
			HeaderViewHolder holder;
			if (convertView == null) {
				holder = new HeaderViewHolder();
				convertView = mInflater.inflate(R.layout.row_header, parent, false);
				holder.text = (TextView) convertView.findViewById(android.R.id.title);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			holder.text.setText(getHeaderText(position));
			return convertView;
		}

		@Override
		public long getHeaderId(int position) {
			return getHeaderText(position).charAt(0);
		}

		private String getHeaderText(int position) {
			CollectionItem game = mBuddyCollection.get(position);
			if (game != null) {
				return game.gameSortName().substring(0, 1);
			}
			return "-";
		}

		class BuddyGameViewHolder {
			public TextView title;
			public TextView text;

			public BuddyGameViewHolder(View view) {
				title = (TextView) view.findViewById(android.R.id.title);
				text = (TextView) view.findViewById(android.R.id.text1);
			}
		}

		class HeaderViewHolder {
			TextView text;
		}
	}
}
