package com.boardgamegeek.service;

import android.content.Context;
import android.text.TextUtils;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.provider.BggContract;

import timber.log.Timber;

public class SyncBuddy extends UpdateTask {
	private String mName;

	public SyncBuddy(String name) {
		mName = name;
	}

	@Override
	public String getDescription() {
		if (TextUtils.isEmpty(mName)) {
			return "update an unknown buddy";
		}
		return "update buddy " + mName;
	}

	@Override
	public void execute(Context context) {
		BggService service = Adapter.create();
		User user = service.user(mName);

		if (user == null || user.getId() == 0 || user.getId() == BggContract.INVALID_ID) {
			Timber.i("Invalid user: " + mName);
			return;
		}
		BuddyPersister persister = new BuddyPersister(context);
		persister.save(user);
		Timber.i("Synced Buddy " + mName);
	}
}
