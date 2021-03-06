package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

public class ProducerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler mHandler = new Handler();
	private Runnable mUpdaterRunnable = null;
	private static final int AGE_IN_DAYS_TO_REFRESH = 30;
	private Uri mUri;
	private int mToken;

	@SuppressWarnings("unused") @InjectView(R.id.id) TextView mId;
	@SuppressWarnings("unused") @InjectView(R.id.name) TextView mName;
	@SuppressWarnings("unused") @InjectView(R.id.description) TextView mDescription;
	@SuppressWarnings("unused") @InjectView(R.id.updated) TextView mUpdated;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mUri = intent.getData();

		if (Designers.isDesignerUri(mUri)) {
			mToken = DesignerQuery._TOKEN;
		} else if (Artists.isArtistUri(mUri)) {
			mToken = ArtistQuery._TOKEN;
		} else if (Publishers.isPublisherUri(mUri)) {
			mToken = PublisherQuery._TOKEN;
		}
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_producer, container, false);
		ButterKnife.inject(this, rootView);
		getLoaderManager().restartLoader(mToken, null, this);
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mUpdaterRunnable != null) {
			mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}


	@Override
	public void onPause() {
		super.onPause();
		if (mUpdaterRunnable != null) {
			mHandler.removeCallbacks(mUpdaterRunnable);
		}
	}

	@Override
	@DebugLog
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case DesignerQuery._TOKEN:
				loader = new CursorLoader(getActivity(), mUri, DesignerQuery.PROJECTION, null, null, null);
				break;
			case ArtistQuery._TOKEN:
				loader = new CursorLoader(getActivity(), mUri, ArtistQuery.PROJECTION, null, null, null);
				break;
			case PublisherQuery._TOKEN:
				loader = new CursorLoader(getActivity(), mUri, PublisherQuery.PROJECTION, null, null, null);
				break;
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == mToken) {
			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			int id = cursor.getInt(Query.ID);
			String name = cursor.getString(Query.NAME);
			String description = cursor.getString(Query.DESCRIPTION);
			long updated = cursor.getLong(Query.UPDATED);

			mId.setText(String.format(getString(R.string.id_list_text), id));
			mName.setText(name);
			UIUtils.setTextMaybeHtml(mDescription, description);
			mUpdated.setTag(updated);

			updateTimeBasedUi();
			if (mUpdaterRunnable != null) {
				mHandler.removeCallbacks(mUpdaterRunnable);
			}
			mUpdaterRunnable = new Runnable() {
				@Override
				public void run() {
					updateTimeBasedUi();
					mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
				}
			};
			mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);

			if (updated == 0 || DateTimeUtils.howManyDaysOld(updated) > AGE_IN_DAYS_TO_REFRESH) {
				UpdateService.start(getActivity(), mToken, id);
			}
		} else {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void updateTimeBasedUi() {
		if (mUpdated != null) {
			long updated = (long) mUpdated.getTag();
			mUpdated.setText(PresentationUtils.describePastTimeSpan(updated, getResources().getString(R.string.text_unknown)));
		}
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private interface Query {
		int ID = 0;
		int NAME = 1;
		int DESCRIPTION = 2;
		int UPDATED = 3;
	}

	private interface DesignerQuery extends Query {
		int _TOKEN = UpdateService.SYNC_TYPE_DESIGNER;
		String[] PROJECTION = { Designers.DESIGNER_ID, Designers.DESIGNER_NAME, Designers.DESIGNER_DESCRIPTION,
			Designers.UPDATED };
	}

	private interface ArtistQuery extends Query {
		int _TOKEN = UpdateService.SYNC_TYPE_ARTIST;
		String[] PROJECTION = { Artists.ARTIST_ID, Artists.ARTIST_NAME, Artists.ARTIST_DESCRIPTION, Artists.UPDATED };
	}

	private interface PublisherQuery extends Query {
		int _TOKEN = UpdateService.SYNC_TYPE_PUBLISHER;
		String[] PROJECTION = { Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME, Publishers.PUBLISHER_DESCRIPTION,
			Publishers.UPDATED };
	}
}
