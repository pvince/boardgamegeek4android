package com.boardgamegeek.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.PreferencesUtils;

import timber.log.Timber;

/**
 * Syncs the user's collection in brief mode, one collection status at a time.
 */
public class SyncCollectionComplete extends SyncTask {
	private static final String STATUS_PLAYED = "played";

	public SyncCollectionComplete(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, SyncResult syncResult) {
		Timber.i("Syncing full collection list...");
		boolean success = true;
		try {
			CollectionPersister persister = new CollectionPersister(mContext);

			List<String> statuses = new ArrayList<>(Arrays.asList(PreferencesUtils.getSyncStatuses(mContext)));
			if (statuses.remove(STATUS_PLAYED)) {
				statuses.add(0, STATUS_PLAYED);
			}

			for (int i = 0; i < statuses.size(); i++) {
				if (isCancelled()) {
					success = false;
					break;
				}

				String status = statuses.get(i);
				if (TextUtils.isEmpty(status)) {
					Timber.i("...skipping blank status");
					continue;
				}
				Timber.i("...syncing status [" + status + "]");
				showNotification(String.format("Syncing %s collection items", status));

				Map<String, String> options = new HashMap<>();
				options.put(status, "1");
				for (int j = 0; j < i; j++) {
					options.put(statuses.get(j), "0");
				}

				requestAndPersist(account.name, persister, options, syncResult);

				showNotification(String.format("Syncing %s collection accessories", status));
				options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
				requestAndPersist(account.name, persister, options, syncResult);
			}

			if (success) {
				Timber.i("...deleting old collection entries");
				// Delete all collection items that weren't updated in the sync above
				int count = mContext.getContentResolver().delete(Collection.CONTENT_URI,
					Collection.UPDATED_LIST + "<?", new String[] { String.valueOf(persister.getTimeStamp()) });
				Timber.i("...deleted " + count + " old collection entries");
				// TODO: delete games as well?!
				// TODO: delete thumbnail images associated with this list (both collection and game)

				Authenticator.putLong(mContext, SyncService.TIMESTAMP_COLLECTION_COMPLETE, persister.getTimeStamp());
				Authenticator.putLong(mContext, SyncService.TIMESTAMP_COLLECTION_PARTIAL, persister.getTimeStamp());
			}
		} finally {
			Timber.i("...complete!");
		}
	}

	private void requestAndPersist(String username, CollectionPersister persister, Map<String, String> options,
								   SyncResult syncResult) {
		CollectionResponse response = getCollectionResponse(mService, username, options);
		if (response.items != null && response.items.size() > 0) {
			int rows = persister.save(response.items);
			syncResult.stats.numEntries += response.items.size();
			Timber.i("...saved " + rows + " records for " + response.items.size() + " collection items");
		} else {
			Timber.i("...no collection items to save");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_full;
	}
}
