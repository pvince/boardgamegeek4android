package com.boardgamegeek.service;

import java.util.List;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ResolverUtils;

import timber.log.Timber;

/**
 * Removes games that aren't in the collection and haven't been viewed in 72 hours.
 */
public class SyncGamesRemove extends SyncTask {
	private static final int HOURS_OLD = 72;
	private static final String STATUS_PLAYED = "played";

	public SyncGamesRemove(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, SyncResult syncResult) {
		Timber.i("Removing games not in the collection...");
		try {
			long hoursAgo = DateTimeUtils.hoursAgo(HOURS_OLD);

			String date = DateUtils.formatDateTime(mContext, hoursAgo, DateUtils.FORMAT_SHOW_DATE
				| DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME);
			Timber.i("...not viewed since " + date);

			ContentResolver resolver = mContext.getContentResolver();
			String selection = "collection." + Collection.GAME_ID + " IS NULL AND games." + Games.LAST_VIEWED + "<?";
			if (PreferencesUtils.isSyncStatus(mContext, STATUS_PLAYED)) {
				selection += " AND games." + Games.NUM_PLAYS + "=0";
			}
			List<Integer> gameIds = ResolverUtils.queryInts(resolver, Games.CONTENT_URI, Games.GAME_ID, selection,
				new String[] { String.valueOf(hoursAgo) }, "games." + Games.UPDATED);
			if (gameIds.size() > 0) {
				Timber.i("...found " + gameIds.size() + " games to delete");
				showNotification("Deleting " + gameIds.size() + " games from your collection");

				int count = 0;
				// NOTE: We're deleting one at a time, because a batch doesn't perform the game/collection join
				for (Integer gameId : gameIds) {
					Timber.i("...deleting game ID=" + gameId);
					count += resolver.delete(Games.buildGameUri(gameId), null, null);
				}
				syncResult.stats.numDeletes += count;
				Timber.i("...deleted " + count + " games");
			} else {
				Timber.i("...no games need deleting");
			}
		} finally {
			Timber.i("...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_missing;
	}
}
