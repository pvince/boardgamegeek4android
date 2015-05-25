package com.boardgamegeek.provider;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.TableBuilder;
import com.boardgamegeek.util.TableBuilder.COLUMN_TYPE;
import com.boardgamegeek.util.TableBuilder.CONFLICT_RESOLUTION;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

public class BggDatabase extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "bgg.db";

	// NOTE: carefully update onUpgrade() when bumping database versions to make sure user data is saved.
	private static final int VER_INITIAL = 1;
	private static final int VER_WISHLIST_PRIORITY = 2;
	private static final int VER_GAME_COLORS = 3;
	private static final int VER_EXPANSIONS = 4;
	private static final int VER_VARIOUS = 5;
	private static final int VER_PLAYS = 6;
	private static final int VER_PLAY_NICKNAME = 7;
	private static final int VER_PLAY_SYNC_STATUS = 8;
	private static final int VER_COLLECTION_VIEWS = 9;
	private static final int VER_COLLECTION_VIEWS_SORT = 10;
	private static final int VER_CASCADING_DELETE = 11;
	private static final int VER_IMAGE_CACHE = 12;
	private static final int VER_GAMES_UPDATED_PLAYS = 13;
	private static final int VER_COLLECTION = 14;
	private static final int VER_GAME_COLLECTION_CONFLICT = 15;
	private static final int VER_PLAYS_START_TIME = 16;
	private static final int VER_PLAYS_PLAYER_COUNT = 17;
	private static final int VER_GAMES_SUBTYPE = 18;
	private static final int VER_COLLECTION_ID_NULLABLE = 19;
	private static final int VER_GAME_CUSTOM_PLAYER_SORT = 20;
	private static final int VER_BUDDY_FLAG = 21;
	private static final int VER_GAME_RANK = 22;
	private static final int VER_BUDDY_SYNC_HASH_CODE = 23;
	private static final int VER_PLAY_SYNC_HASH_CODE = 24;
	private static final int VER_PLAYER_COLORS = 25;
	private static final int DATABASE_VERSION = VER_PLAYER_COLORS;

	private Context mContext;

	public interface GamesDesigners {
		String GAME_ID = Games.GAME_ID;
		String DESIGNER_ID = Designers.DESIGNER_ID;
	}

	public interface GamesArtists {
		String GAME_ID = Games.GAME_ID;
		String ARTIST_ID = Artists.ARTIST_ID;
	}

	public interface GamesPublishers {
		String GAME_ID = Games.GAME_ID;
		String PUBLISHER_ID = Publishers.PUBLISHER_ID;
	}

	public interface GamesMechanics {
		String GAME_ID = Games.GAME_ID;
		String MECHANIC_ID = Mechanics.MECHANIC_ID;
	}

	public interface GamesCategories {
		String GAME_ID = Games.GAME_ID;
		String CATEGORY_ID = Categories.CATEGORY_ID;
	}

	interface Tables {
		String DESIGNERS = "designers";
		String ARTISTS = "artists";
		String PUBLISHERS = "publishers";
		String MECHANICS = "mechanics";
		String CATEGORIES = "categories";
		String GAMES = "games";
		String GAME_RANKS = "game_ranks";
		String GAMES_DESIGNERS = "games_designers";
		String GAMES_ARTISTS = "games_artists";
		String GAMES_PUBLISHERS = "games_publishers";
		String GAMES_MECHANICS = "games_mechanics";
		String GAMES_CATEGORIES = "games_categories";
		String GAMES_EXPANSIONS = "games_expansions";
		String COLLECTION = "collection";
		String BUDDIES = "buddies";
		String GAME_POLLS = "game_polls";
		String GAME_POLL_RESULTS = "game_poll_results";
		String GAME_POLL_RESULTS_RESULT = "game_poll_results_result";
		String GAME_COLORS = "game_colors";
		String PLAYS = "plays";
		String PLAY_ITEMS = "play_items";
		String PLAY_PLAYERS = "play_players";
		String COLLECTION_VIEWS = "collection_filters";
		String COLLECTION_VIEW_FILTERS = "collection_filters_details";
		String PLAYER_COLORS = "player_colors";

		String GAMES_JOIN_COLLECTION = createJoin(GAMES, COLLECTION, Games.GAME_ID);
		String GAMES_DESIGNERS_JOIN_DESIGNERS = createJoin(GAMES_DESIGNERS, DESIGNERS, Designers.DESIGNER_ID);
		String GAMES_ARTISTS_JOIN_ARTISTS = createJoin(GAMES_ARTISTS, ARTISTS, Artists.ARTIST_ID);
		String GAMES_PUBLISHERS_JOIN_PUBLISHERS = createJoin(GAMES_PUBLISHERS, PUBLISHERS, Publishers.PUBLISHER_ID);
		String GAMES_MECHANICS_JOIN_MECHANICS = createJoin(GAMES_MECHANICS, MECHANICS, Mechanics.MECHANIC_ID);
		String GAMES_CATEGORIES_JOIN_CATEGORIES = createJoin(GAMES_CATEGORIES, CATEGORIES, Categories.CATEGORY_ID);
		String GAMES_EXPANSIONS_JOIN_EXPANSIONS = createJoin(GAMES_EXPANSIONS, GAMES, GamesExpansions.EXPANSION_ID,
			Games.GAME_ID);
		String POLLS_JOIN_POLL_RESULTS = createJoin(GAME_POLLS, GAME_POLL_RESULTS, GamePolls._ID,
			GamePollResults.POLL_ID);
		String POLL_RESULTS_JOIN_POLL_RESULTS_RESULT = createJoin(GAME_POLL_RESULTS, GAME_POLL_RESULTS_RESULT,
			GamePollResults._ID, GamePollResultsResult.POLL_RESULTS_ID);
		String COLLECTION_JOIN_GAMES = createJoin(COLLECTION, GAMES, Collection.GAME_ID);
		String PLAY_ITEMS_JOIN_PLAYS = createJoin(PLAY_ITEMS, PLAYS, Plays.PLAY_ID);
		String PLAY_ITEMS_JOIN_PLAYS_JOIN_GAMES = Tables.PLAY_ITEMS
			+ createJoinSuffix(PLAY_ITEMS, PLAYS, Plays.PLAY_ID)
			+ createJoinSuffix(PLAY_ITEMS, GAMES, PlayItems.OBJECT_ID, Games.GAME_ID);
		String PLAY_PLAYERS_JOIN_PLAYS = createJoin(PLAY_PLAYERS, PLAYS, Plays.PLAY_ID);
		String PLAY_PLAYERS_JOIN_PLAYS_JOIN_ITEMS = Tables.PLAY_PLAYERS
			+ createJoinSuffix(PLAY_PLAYERS, PLAYS, Plays.PLAY_ID)
			+ createJoinSuffix(PLAY_PLAYERS, PLAY_ITEMS, Plays.PLAY_ID)
			+ createJoinSuffix(PLAY_ITEMS, GAMES, PlayItems.OBJECT_ID, Games.GAME_ID);
		String PLAY_ITEMS_JOIN_PLAYS_JOIN_PLAYERS = Tables.PLAY_ITEMS
			+ createJoinSuffix(PLAY_ITEMS, PLAYS, PlayItems.PLAY_ID)
			+ createJoinSuffix(PLAY_ITEMS, PLAY_PLAYERS, PlayItems.PLAY_ID)
			+ createJoinSuffix(PLAY_ITEMS, GAMES, PlayItems.OBJECT_ID, Games.GAME_ID);
		String COLLECTION_VIEW_FILTERS_JOIN_COLLECTION_VIEWS = createJoin(COLLECTION_VIEWS, COLLECTION_VIEW_FILTERS,
			CollectionViews._ID, CollectionViewFilters.VIEW_ID);
		String POLLS_RESULTS_RESULT_JOIN_POLLS_RESULTS_JOIN_POLLS = createJoin(GAME_POLL_RESULTS_RESULT,
			GAME_POLL_RESULTS, GamePollResultsResult.POLL_RESULTS_ID, GamePollResults._ID)
			+ createJoinSuffix(Tables.GAME_POLL_RESULTS, Tables.GAME_POLLS, GamePollResults.POLL_ID, GamePolls._ID);
	}

	private static String createJoin(String table1, String table2, String column) {
		return table1 + createJoinSuffix(table1, table2, column, column);
	}

	private static String createJoin(String table1, String table2, String column1, String column2) {
		return table1 + createJoinSuffix(table1, table2, column1, column2);
	}

	private static String createJoinSuffix(String table1, String table2, String column) {
		return createJoinSuffix(table1, table2, column, column);
	}

	private static String createJoinSuffix(String table1, String table2, String column1, String column2) {
		return " LEFT OUTER JOIN " + table2 + " ON " + table1 + "." + column1 + "=" + table2 + "." + column2;
	}

	public BggDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		if (!db.isReadOnly()) {
			db.execSQL("PRAGMA foreign_keys=ON;");
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		buildDesignersTable().create(db);
		buildArtistsTable().create(db);
		buildPublishersTable().create(db);
		buildMechanicsTable().create(db);
		buildCategoriesTable().create(db);

		buildGamesTable().create(db);
		buildGameRanksTable().create(db);
		buildGamesDesignersTable().create(db);
		buildGamesArtistsTable().create(db);
		buildGamesPublishersTable().create(db);
		buildGamesMechanicsTable().create(db);
		buildGamesCategoriesTable().create(db);
		buildGameExpansionsTable().create(db);
		buildGamePollsTable().create(db);
		buildGamePollResultsTable().create(db);
		buildGamePollResultsResultTable().create(db);
		buildGameColorsTable().create(db);

		buildPlaysTable().create(db);
		buildPlayItemsTable().create(db);
		buildPlayPlayersTable().create(db);

		buildCollectionTable().create(db);

		buildBuddiesTable().create(db);
		buildPlayerColorsTable().create(db);

		buildCollectionViewsTable().create(db);
		buildCollectionViewFiltersTable().create(db);
	}

	private TableBuilder buildDesignersTable() {
		return new TableBuilder().setTable(Tables.DESIGNERS).useDefaultPrimaryKey()
			.addColumn(Designers.UPDATED, COLUMN_TYPE.INTEGER)
			.addColumn(Designers.DESIGNER_ID, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(Designers.DESIGNER_NAME, COLUMN_TYPE.TEXT, true)
			.addColumn(Designers.DESIGNER_DESCRIPTION, COLUMN_TYPE.TEXT);
	}

	private TableBuilder buildArtistsTable() {
		return new TableBuilder().setTable(Tables.ARTISTS).useDefaultPrimaryKey()
			.addColumn(Artists.UPDATED, COLUMN_TYPE.INTEGER)
			.addColumn(Artists.ARTIST_ID, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(Artists.ARTIST_NAME, COLUMN_TYPE.TEXT, true)
			.addColumn(Artists.ARTIST_DESCRIPTION, COLUMN_TYPE.TEXT);
	}

	private TableBuilder buildPublishersTable() {
		return new TableBuilder().setTable(Tables.PUBLISHERS).useDefaultPrimaryKey()
			.addColumn(Publishers.UPDATED, COLUMN_TYPE.INTEGER)
			.addColumn(Publishers.PUBLISHER_ID, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(Publishers.PUBLISHER_NAME, COLUMN_TYPE.TEXT, true)
			.addColumn(Publishers.PUBLISHER_DESCRIPTION, COLUMN_TYPE.TEXT);
	}

	private TableBuilder buildMechanicsTable() {
		return new TableBuilder().setTable(Tables.MECHANICS).useDefaultPrimaryKey()
			.addColumn(Mechanics.MECHANIC_ID, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(Mechanics.MECHANIC_NAME, COLUMN_TYPE.TEXT, true);
	}

	private TableBuilder buildCategoriesTable() {
		return new TableBuilder().setTable(Tables.CATEGORIES).useDefaultPrimaryKey()
			.addColumn(Categories.CATEGORY_ID, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(Categories.CATEGORY_NAME, COLUMN_TYPE.TEXT, true);
	}

	private TableBuilder buildGamesTable() {
		return new TableBuilder().setTable(Tables.GAMES).useDefaultPrimaryKey()
			.addColumn(Games.UPDATED, COLUMN_TYPE.INTEGER).addColumn(Games.UPDATED_LIST, COLUMN_TYPE.INTEGER, true)
			.addColumn(Games.GAME_ID, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(Games.GAME_NAME, COLUMN_TYPE.TEXT, true).addColumn(Games.GAME_SORT_NAME, COLUMN_TYPE.TEXT, true)
			.addColumn(Games.YEAR_PUBLISHED, COLUMN_TYPE.INTEGER).addColumn(Games.IMAGE_URL, COLUMN_TYPE.TEXT)
			.addColumn(Games.THUMBNAIL_URL, COLUMN_TYPE.TEXT).addColumn(Games.MIN_PLAYERS, COLUMN_TYPE.INTEGER)
			.addColumn(Games.MAX_PLAYERS, COLUMN_TYPE.INTEGER).addColumn(Games.PLAYING_TIME, COLUMN_TYPE.INTEGER)
			.addColumn(Games.NUM_PLAYS, COLUMN_TYPE.INTEGER, true, 0).addColumn(Games.MINIMUM_AGE, COLUMN_TYPE.INTEGER)
			.addColumn(Games.DESCRIPTION, COLUMN_TYPE.TEXT).addColumn(Games.SUBTYPE, COLUMN_TYPE.TEXT)
			.addColumn(Games.STATS_USERS_RATED, COLUMN_TYPE.INTEGER).addColumn(Games.STATS_AVERAGE, COLUMN_TYPE.REAL)
			.addColumn(Games.STATS_BAYES_AVERAGE, COLUMN_TYPE.REAL)
			.addColumn(Games.STATS_STANDARD_DEVIATION, COLUMN_TYPE.REAL)
			.addColumn(Games.STATS_MEDIAN, COLUMN_TYPE.INTEGER)
			.addColumn(Games.STATS_NUMBER_OWNED, COLUMN_TYPE.INTEGER)
			.addColumn(Games.STATS_NUMBER_TRADING, COLUMN_TYPE.INTEGER)
			.addColumn(Games.STATS_NUMBER_WANTING, COLUMN_TYPE.INTEGER)
			.addColumn(Games.STATS_NUMBER_WISHING, COLUMN_TYPE.INTEGER)
			.addColumn(Games.STATS_NUMBER_COMMENTS, COLUMN_TYPE.INTEGER)
			.addColumn(Games.STATS_NUMBER_WEIGHTS, COLUMN_TYPE.INTEGER)
			.addColumn(Games.STATS_AVERAGE_WEIGHT, COLUMN_TYPE.REAL).addColumn(Games.LAST_VIEWED, COLUMN_TYPE.INTEGER)
			.addColumn(Games.STARRED, COLUMN_TYPE.INTEGER).addColumn(Games.UPDATED_PLAYS, COLUMN_TYPE.INTEGER)
			.addColumn(Games.CUSTOM_PLAYER_SORT, COLUMN_TYPE.INTEGER).addColumn(Games.GAME_RANK, COLUMN_TYPE.INTEGER)
			.setConflictResolution(CONFLICT_RESOLUTION.ABORT);
	}

	private TableBuilder buildGameRanksTable() {
		return new TableBuilder().setTable(Tables.GAME_RANKS).useDefaultPrimaryKey()
			.addColumn(GameRanks.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID, true)
			.addColumn(GameRanks.GAME_RANK_ID, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(GameRanks.GAME_RANK_TYPE, COLUMN_TYPE.TEXT, true)
			.addColumn(GameRanks.GAME_RANK_NAME, COLUMN_TYPE.TEXT, true)
			.addColumn(GameRanks.GAME_RANK_FRIENDLY_NAME, COLUMN_TYPE.TEXT, true)
			.addColumn(GameRanks.GAME_RANK_VALUE, COLUMN_TYPE.INTEGER, true)
			.addColumn(GameRanks.GAME_RANK_BAYES_AVERAGE, COLUMN_TYPE.REAL, true)
			.setConflictResolution(CONFLICT_RESOLUTION.REPLACE);
	}

	private TableBuilder buildGamesDesignersTable() {
		return new TableBuilder()
			.setTable(Tables.GAMES_DESIGNERS)
			.useDefaultPrimaryKey()
			.addColumn(GamesDesigners.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID, true)
			.addColumn(GamesDesigners.DESIGNER_ID, COLUMN_TYPE.INTEGER, true, true, Tables.DESIGNERS,
				Designers.DESIGNER_ID);
	}

	private TableBuilder buildGamesArtistsTable() {
		return new TableBuilder().setTable(Tables.GAMES_ARTISTS).useDefaultPrimaryKey()
			.addColumn(GamesArtists.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID, true)
			.addColumn(GamesArtists.ARTIST_ID, COLUMN_TYPE.INTEGER, true, true, Tables.ARTISTS, Artists.ARTIST_ID);
	}

	private TableBuilder buildGamesPublishersTable() {
		return new TableBuilder()
			.setTable(Tables.GAMES_PUBLISHERS)
			.useDefaultPrimaryKey()
			.addColumn(GamesPublishers.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID, true)
			.addColumn(GamesPublishers.PUBLISHER_ID, COLUMN_TYPE.INTEGER, true, true, Tables.PUBLISHERS,
				Publishers.PUBLISHER_ID);
	}

	private TableBuilder buildGamesMechanicsTable() {
		return new TableBuilder()
			.setTable(Tables.GAMES_MECHANICS)
			.useDefaultPrimaryKey()
			.addColumn(GamesMechanics.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID, true)
			.addColumn(GamesMechanics.MECHANIC_ID, COLUMN_TYPE.INTEGER, true, true, Tables.MECHANICS,
				Mechanics.MECHANIC_ID);
	}

	private TableBuilder buildGamesCategoriesTable() {
		return new TableBuilder()
			.setTable(Tables.GAMES_CATEGORIES)
			.useDefaultPrimaryKey()
			.addColumn(GamesCategories.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID, true)
			.addColumn(GamesCategories.CATEGORY_ID, COLUMN_TYPE.INTEGER, true, true, Tables.CATEGORIES,
				Categories.CATEGORY_ID);
	}

	private TableBuilder buildCollectionTable() {
		return new TableBuilder().setTable(Tables.COLLECTION).useDefaultPrimaryKey()
			.addColumn(Collection.UPDATED, COLUMN_TYPE.INTEGER).addColumn(Collection.UPDATED_LIST, COLUMN_TYPE.INTEGER)
			.addColumn(Collection.GAME_ID, COLUMN_TYPE.INTEGER, true, false, Tables.GAMES, Games.GAME_ID, true)
			.addColumn(Collection.COLLECTION_ID, COLUMN_TYPE.INTEGER)
			.addColumn(Collection.COLLECTION_NAME, COLUMN_TYPE.TEXT, true)
			.addColumn(Collection.COLLECTION_SORT_NAME, COLUMN_TYPE.TEXT, true)
			.addColumn(Collection.STATUS_OWN, COLUMN_TYPE.INTEGER, true, 0)
			.addColumn(Collection.STATUS_PREVIOUSLY_OWNED, COLUMN_TYPE.INTEGER, true, 0)
			.addColumn(Collection.STATUS_FOR_TRADE, COLUMN_TYPE.INTEGER, true, 0)
			.addColumn(Collection.STATUS_WANT, COLUMN_TYPE.INTEGER, true, 0)
			.addColumn(Collection.STATUS_WANT_TO_PLAY, COLUMN_TYPE.INTEGER, true, 0)
			.addColumn(Collection.STATUS_WANT_TO_BUY, COLUMN_TYPE.INTEGER, true, 0)
			.addColumn(Collection.STATUS_WISHLIST_PRIORITY, COLUMN_TYPE.INTEGER)
			.addColumn(Collection.STATUS_WISHLIST, COLUMN_TYPE.INTEGER, true, 0)
			.addColumn(Collection.STATUS_PREORDERED, COLUMN_TYPE.INTEGER, true, 0)
			.addColumn(Collection.COMMENT, COLUMN_TYPE.TEXT).addColumn(Collection.LAST_MODIFIED, COLUMN_TYPE.INTEGER)
			.addColumn(Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY, COLUMN_TYPE.TEXT)
			.addColumn(Collection.PRIVATE_INFO_PRICE_PAID, COLUMN_TYPE.REAL)
			.addColumn(Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, COLUMN_TYPE.TEXT)
			.addColumn(Collection.PRIVATE_INFO_CURRENT_VALUE, COLUMN_TYPE.REAL)
			.addColumn(Collection.PRIVATE_INFO_QUANTITY, COLUMN_TYPE.INTEGER)
			.addColumn(Collection.PRIVATE_INFO_ACQUISITION_DATE, COLUMN_TYPE.TEXT)
			.addColumn(Collection.PRIVATE_INFO_ACQUIRED_FROM, COLUMN_TYPE.TEXT)
			.addColumn(Collection.PRIVATE_INFO_COMMENT, COLUMN_TYPE.TEXT)
			.addColumn(Collection.CONDITION, COLUMN_TYPE.TEXT).addColumn(Collection.HASPARTS_LIST, COLUMN_TYPE.TEXT)
			.addColumn(Collection.WANTPARTS_LIST, COLUMN_TYPE.TEXT)
			.addColumn(Collection.WISHLIST_COMMENT, COLUMN_TYPE.TEXT)
			.addColumn(Collection.COLLECTION_YEAR_PUBLISHED, COLUMN_TYPE.INTEGER)
			.addColumn(Collection.RATING, COLUMN_TYPE.REAL)
			.addColumn(Collection.COLLECTION_THUMBNAIL_URL, COLUMN_TYPE.TEXT)
			.addColumn(Collection.COLLECTION_IMAGE_URL, COLUMN_TYPE.TEXT)
			.setConflictResolution(CONFLICT_RESOLUTION.ABORT);
	}

	private TableBuilder buildBuddiesTable() {
		return new TableBuilder().setTable(Tables.BUDDIES).useDefaultPrimaryKey()
			.addColumn(Buddies.UPDATED, COLUMN_TYPE.INTEGER)
			.addColumn(Buddies.UPDATED_LIST, COLUMN_TYPE.INTEGER, true)
			.addColumn(Buddies.BUDDY_ID, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(Buddies.BUDDY_NAME, COLUMN_TYPE.TEXT, true)
			.addColumn(Buddies.BUDDY_FIRSTNAME, COLUMN_TYPE.TEXT)
			.addColumn(Buddies.BUDDY_LASTNAME, COLUMN_TYPE.TEXT)
			.addColumn(Buddies.AVATAR_URL, COLUMN_TYPE.TEXT)
			.addColumn(Buddies.PLAY_NICKNAME, COLUMN_TYPE.TEXT)
			.addColumn(Buddies.BUDDY_FLAG, COLUMN_TYPE.INTEGER)
			.addColumn(Buddies.SYNC_HASH_CODE, COLUMN_TYPE.INTEGER);
	}

	private TableBuilder buildGamePollsTable() {
		return new TableBuilder().setTable(Tables.GAME_POLLS).useDefaultPrimaryKey()
			.addColumn(Games.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID, true)
			.addColumn(GamePolls.POLL_NAME, COLUMN_TYPE.TEXT, true, true)
			.addColumn(GamePolls.POLL_TITLE, COLUMN_TYPE.TEXT, true)
			.addColumn(GamePolls.POLL_TOTAL_VOTES, COLUMN_TYPE.INTEGER, true);
	}

	private TableBuilder buildGamePollResultsTable() {
		return new TableBuilder()
			.setTable(Tables.GAME_POLL_RESULTS)
			.useDefaultPrimaryKey()
			.addColumn(GamePollResults.POLL_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAME_POLLS, GamePolls._ID, true)
			.addColumn(GamePollResults.POLL_RESULTS_KEY, COLUMN_TYPE.TEXT, true, true)
			.addColumn(GamePollResults.POLL_RESULTS_PLAYERS, COLUMN_TYPE.TEXT)
			.addColumn(GamePollResults.POLL_RESULTS_SORT_INDEX, COLUMN_TYPE.INTEGER, true);
	}

	private TableBuilder buildGamePollResultsResultTable() {
		return new TableBuilder()
			.setTable(Tables.GAME_POLL_RESULTS_RESULT)
			.useDefaultPrimaryKey()
			.addColumn(GamePollResultsResult.POLL_RESULTS_ID, COLUMN_TYPE.INTEGER, true, true,
				Tables.GAME_POLL_RESULTS, GamePollResults._ID, true)
			.addColumn(GamePollResultsResult.POLL_RESULTS_RESULT_KEY, COLUMN_TYPE.TEXT, true, true)
			.addColumn(GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL, COLUMN_TYPE.INTEGER)
			.addColumn(GamePollResultsResult.POLL_RESULTS_RESULT_VALUE, COLUMN_TYPE.TEXT, true)
			.addColumn(GamePollResultsResult.POLL_RESULTS_RESULT_VOTES, COLUMN_TYPE.INTEGER, true)
			.addColumn(GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX, COLUMN_TYPE.INTEGER, true);
	}

	private TableBuilder buildGameColorsTable() {
		return new TableBuilder().setTable(Tables.GAME_COLORS).useDefaultPrimaryKey()
			.addColumn(Games.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID, true)
			.addColumn(GameColors.COLOR, COLUMN_TYPE.TEXT, true, true);
	}

	private TableBuilder buildGameExpansionsTable() {
		return new TableBuilder().setTable(Tables.GAMES_EXPANSIONS).useDefaultPrimaryKey()
			.addColumn(Games.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID, true)
			.addColumn(GamesExpansions.EXPANSION_ID, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(GamesExpansions.EXPANSION_NAME, COLUMN_TYPE.TEXT, true)
			.addColumn(GamesExpansions.INBOUND, COLUMN_TYPE.INTEGER);
	}

	private TableBuilder buildPlaysTable() {
		return new TableBuilder().setTable(Tables.PLAYS).useDefaultPrimaryKey()
			.addColumn(Plays.UPDATED_LIST, COLUMN_TYPE.INTEGER, true)
			.addColumn(Plays.PLAY_ID, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(Plays.DATE, COLUMN_TYPE.TEXT, true)
			.addColumn(Plays.QUANTITY, COLUMN_TYPE.INTEGER, true)
			.addColumn(Plays.LENGTH, COLUMN_TYPE.INTEGER, true)
			.addColumn(Plays.INCOMPLETE, COLUMN_TYPE.INTEGER, true)
			.addColumn(Plays.NO_WIN_STATS, COLUMN_TYPE.INTEGER, true)
			.addColumn(Plays.LOCATION, COLUMN_TYPE.TEXT)
			.addColumn(Plays.COMMENTS, COLUMN_TYPE.TEXT)
			.addColumn(Plays.SYNC_STATUS, COLUMN_TYPE.INTEGER)
			.addColumn(Plays.START_TIME, COLUMN_TYPE.INTEGER)
			.addColumn(Plays.PLAYER_COUNT, COLUMN_TYPE.INTEGER)
			.addColumn(Plays.UPDATED, COLUMN_TYPE.INTEGER)
			.addColumn(Plays.SYNC_HASH_CODE, COLUMN_TYPE.INTEGER);
	}

	private TableBuilder buildPlayItemsTable() {
		return new TableBuilder().setTable(Tables.PLAY_ITEMS).useDefaultPrimaryKey()
			.addColumn(Plays.PLAY_ID, COLUMN_TYPE.INTEGER, true, true, Tables.PLAYS, Plays.PLAY_ID, true)
			.addColumn(PlayItems.OBJECT_ID, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(PlayItems.NAME, COLUMN_TYPE.TEXT, true);
	}

	private TableBuilder buildPlayPlayersTable() {
		return new TableBuilder().setTable(Tables.PLAY_PLAYERS).useDefaultPrimaryKey()
			.addColumn(Plays.PLAY_ID, COLUMN_TYPE.INTEGER, true, false, Tables.PLAYS, Plays.PLAY_ID, true)
			.addColumn(PlayPlayers.USER_NAME, COLUMN_TYPE.TEXT).addColumn(PlayPlayers.USER_ID, COLUMN_TYPE.INTEGER)
			.addColumn(PlayPlayers.NAME, COLUMN_TYPE.TEXT).addColumn(PlayPlayers.START_POSITION, COLUMN_TYPE.TEXT)
			.addColumn(PlayPlayers.COLOR, COLUMN_TYPE.TEXT).addColumn(PlayPlayers.SCORE, COLUMN_TYPE.TEXT)
			.addColumn(PlayPlayers.NEW, COLUMN_TYPE.INTEGER).addColumn(PlayPlayers.RATING, COLUMN_TYPE.REAL)
			.addColumn(PlayPlayers.WIN, COLUMN_TYPE.INTEGER);
	}

	private TableBuilder buildCollectionViewsTable() {
		return new TableBuilder().setTable(Tables.COLLECTION_VIEWS).useDefaultPrimaryKey()
			.addColumn(CollectionViews.NAME, COLUMN_TYPE.TEXT).addColumn(CollectionViews.STARRED, COLUMN_TYPE.INTEGER)
			.addColumn(CollectionViews.SORT_TYPE, COLUMN_TYPE.INTEGER);
	}

	private TableBuilder buildCollectionViewFiltersTable() {
		return new TableBuilder()
			.setTable(Tables.COLLECTION_VIEW_FILTERS)
			.useDefaultPrimaryKey()
			.addColumn(CollectionViewFilters.VIEW_ID, COLUMN_TYPE.INTEGER, true, false, Tables.COLLECTION_VIEWS,
				CollectionViews._ID, true).addColumn(CollectionViewFilters.TYPE, COLUMN_TYPE.INTEGER)
			.addColumn(CollectionViewFilters.DATA, COLUMN_TYPE.TEXT);
	}

	private TableBuilder buildPlayerColorsTable() {
		return new TableBuilder().setTable(Tables.PLAYER_COLORS)
			.setConflictResolution(CONFLICT_RESOLUTION.REPLACE)
			.useDefaultPrimaryKey()
			.addColumn(PlayerColors.PLAYER_TYPE, COLUMN_TYPE.INTEGER, true, true)
			.addColumn(PlayerColors.PLAYER_NAME, COLUMN_TYPE.TEXT, true, true)
			.addColumn(PlayerColors.PLAYER_COLOR, COLUMN_TYPE.TEXT, true, true)
			.addColumn(PlayerColors.PLAYER_COLOR_SORT_ORDER, COLUMN_TYPE.INTEGER, true);
	}

	@SuppressWarnings("UnusedAssignment")
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Timber.d("Upgrading database from " + oldVersion + " to " + newVersion);

		// NOTE: This switch statement is designed to handle cascading database
		// updates, starting at the current version and falling through to all
		// future upgrade cases. Only use "break;" when you want to drop and
		// recreate the entire database.
		int version = oldVersion;

		switch (version) {
			case VER_INITIAL:
				addColumn(db, Tables.COLLECTION, Collection.STATUS_WISHLIST_PRIORITY, COLUMN_TYPE.INTEGER);
				version = VER_WISHLIST_PRIORITY;
			case VER_WISHLIST_PRIORITY:
				buildGameColorsTable().create(db);
				version = VER_GAME_COLORS;
			case VER_GAME_COLORS:
				buildGameExpansionsTable().create(db);
				version = VER_EXPANSIONS;
			case VER_EXPANSIONS:
				addColumn(db, Tables.COLLECTION, Collection.LAST_MODIFIED, COLUMN_TYPE.INTEGER);
				addColumn(db, Tables.GAMES, Games.LAST_VIEWED, COLUMN_TYPE.INTEGER);
				addColumn(db, Tables.GAMES, Games.STARRED, COLUMN_TYPE.INTEGER);
				version = VER_VARIOUS;
			case VER_VARIOUS:
				buildPlaysTable().create(db);
				buildPlayItemsTable().create(db);
				buildPlayPlayersTable().create(db);
				version = VER_PLAYS;
			case VER_PLAYS:
				addColumn(db, Tables.BUDDIES, Buddies.PLAY_NICKNAME, COLUMN_TYPE.TEXT);
				version = VER_PLAY_NICKNAME;
			case VER_PLAY_NICKNAME:
				addColumn(db, Tables.PLAYS, Plays.SYNC_STATUS, COLUMN_TYPE.INTEGER);
				addColumn(db, Tables.PLAYS, Plays.UPDATED, COLUMN_TYPE.INTEGER);
				version = VER_PLAY_SYNC_STATUS;
			case VER_PLAY_SYNC_STATUS:
				buildCollectionViewsTable().create(db);
				buildCollectionViewFiltersTable().create(db);
				version = VER_COLLECTION_VIEWS;
			case VER_COLLECTION_VIEWS:
				addColumn(db, Tables.COLLECTION_VIEWS, CollectionViews.SORT_TYPE, COLUMN_TYPE.INTEGER);
				version = VER_COLLECTION_VIEWS_SORT;
			case VER_COLLECTION_VIEWS_SORT:
				buildGameRanksTable().replace(db);
				buildGamesDesignersTable().replace(db);
				buildGamesArtistsTable().replace(db);
				buildGamesPublishersTable().replace(db);
				buildGamesMechanicsTable().replace(db);
				buildGamesCategoriesTable().replace(db);
				buildGameExpansionsTable().replace(db);
				buildGamePollsTable().replace(db);
				buildGamePollResultsTable().replace(db);
				buildGamePollResultsResultTable().replace(db);
				buildGameColorsTable().replace(db);
				buildPlayItemsTable().replace(db);
				buildPlayPlayersTable().replace(db);
				buildCollectionViewFiltersTable().replace(db);
				version = VER_CASCADING_DELETE;
			case VER_CASCADING_DELETE:
				// remove the old cache directory
				try {
					File oldCacheDirectory = new File(Environment.getExternalStorageDirectory(),
						BggContract.CONTENT_AUTHORITY);
					FileUtils.deleteContents(oldCacheDirectory);
					boolean success = oldCacheDirectory.delete();
					if (!success) {
						Timber.i("Unable to delete old cache directory");
					}
				} catch (IOException e) {
					Timber.e(e, "Error clearing the cache");
				}
				version = VER_IMAGE_CACHE;
			case VER_IMAGE_CACHE:
				addColumn(db, Tables.GAMES, Games.UPDATED_PLAYS, COLUMN_TYPE.INTEGER);
				version = VER_GAMES_UPDATED_PLAYS;
			case VER_GAMES_UPDATED_PLAYS:
				addColumn(db, Tables.COLLECTION, Collection.CONDITION, COLUMN_TYPE.TEXT);
				addColumn(db, Tables.COLLECTION, Collection.HASPARTS_LIST, COLUMN_TYPE.TEXT);
				addColumn(db, Tables.COLLECTION, Collection.WANTPARTS_LIST, COLUMN_TYPE.TEXT);
				addColumn(db, Tables.COLLECTION, Collection.WISHLIST_COMMENT, COLUMN_TYPE.TEXT);
				addColumn(db, Tables.COLLECTION, Collection.COLLECTION_YEAR_PUBLISHED, COLUMN_TYPE.INTEGER);
				addColumn(db, Tables.COLLECTION, Collection.RATING, COLUMN_TYPE.REAL);
				addColumn(db, Tables.COLLECTION, Collection.COLLECTION_THUMBNAIL_URL, COLUMN_TYPE.TEXT);
				addColumn(db, Tables.COLLECTION, Collection.COLLECTION_IMAGE_URL, COLUMN_TYPE.TEXT);
				buildCollectionTable().replace(db);
				version = VER_COLLECTION;
			case VER_COLLECTION:
				addColumn(db, Tables.GAMES, Games.SUBTYPE, COLUMN_TYPE.TEXT);
				addColumn(db, Tables.GAMES, Games.CUSTOM_PLAYER_SORT, COLUMN_TYPE.INTEGER);
				addColumn(db, Tables.GAMES, Games.GAME_RANK, COLUMN_TYPE.INTEGER);
				buildGamesTable().replace(db);
				dropTable(db, Tables.COLLECTION);
				buildCollectionTable().create(db);
				SyncService.clearCollection(mContext);
				SyncService.sync(mContext, SyncService.FLAG_SYNC_COLLECTION);
				version = VER_GAME_COLLECTION_CONFLICT;
			case VER_GAME_COLLECTION_CONFLICT:
				addColumn(db, Tables.PLAYS, Plays.START_TIME, COLUMN_TYPE.INTEGER);
				version = VER_PLAYS_START_TIME;
			case VER_PLAYS_START_TIME:
				addColumn(db, Tables.PLAYS, Plays.PLAYER_COUNT, COLUMN_TYPE.INTEGER);
				db.execSQL("UPDATE " + Tables.PLAYS + " SET " + Plays.PLAYER_COUNT + "= (SELECT COUNT("
					+ PlayPlayers.USER_ID + ")" + " FROM " + Tables.PLAY_PLAYERS + " WHERE " + Tables.PLAYS + "."
					+ Plays.PLAY_ID + "=" + Tables.PLAY_PLAYERS + "." + PlayPlayers.PLAY_ID + ")");
				version = VER_PLAYS_PLAYER_COUNT;
			case VER_PLAYS_PLAYER_COUNT:
				addColumn(db, Tables.GAMES, Games.SUBTYPE, COLUMN_TYPE.TEXT);
				version = VER_GAMES_SUBTYPE;
			case VER_GAMES_SUBTYPE:
				buildCollectionTable().replace(db);
				version = VER_COLLECTION_ID_NULLABLE;
			case VER_COLLECTION_ID_NULLABLE:
				addColumn(db, Tables.GAMES, Games.CUSTOM_PLAYER_SORT, COLUMN_TYPE.INTEGER);
				version = VER_GAME_CUSTOM_PLAYER_SORT;
			case VER_GAME_CUSTOM_PLAYER_SORT:
				addColumn(db, Tables.BUDDIES, Buddies.BUDDY_FLAG, COLUMN_TYPE.INTEGER);
				version = VER_BUDDY_FLAG;
			case VER_BUDDY_FLAG:
				addColumn(db, Tables.GAMES, Games.GAME_RANK, COLUMN_TYPE.INTEGER);
				version = VER_GAME_RANK;
			case VER_GAME_RANK:
				addColumn(db, Tables.BUDDIES, Buddies.SYNC_HASH_CODE, COLUMN_TYPE.INTEGER);
				version = VER_BUDDY_SYNC_HASH_CODE;
			case VER_BUDDY_SYNC_HASH_CODE:
				addColumn(db, Tables.PLAYS, Plays.SYNC_HASH_CODE, COLUMN_TYPE.INTEGER);
				version = VER_PLAY_SYNC_HASH_CODE;
			case VER_PLAY_SYNC_HASH_CODE:
				buildPlayerColorsTable().create(db);
				version = VER_PLAYER_COLORS;
		}

		if (version != DATABASE_VERSION) {
			Timber.w("Destroying old data during upgrade");

			dropTable(db, Tables.DESIGNERS);
			dropTable(db, Tables.ARTISTS);
			dropTable(db, Tables.PUBLISHERS);
			dropTable(db, Tables.MECHANICS);
			dropTable(db, Tables.CATEGORIES);
			dropTable(db, Tables.GAMES);
			dropTable(db, Tables.GAME_RANKS);
			dropTable(db, Tables.GAMES_DESIGNERS);
			dropTable(db, Tables.GAMES_ARTISTS);
			dropTable(db, Tables.GAMES_PUBLISHERS);
			dropTable(db, Tables.GAMES_MECHANICS);
			dropTable(db, Tables.GAMES_CATEGORIES);
			dropTable(db, Tables.GAMES_EXPANSIONS);
			dropTable(db, Tables.COLLECTION);
			dropTable(db, Tables.BUDDIES);
			dropTable(db, Tables.GAME_POLLS);
			dropTable(db, Tables.GAME_POLL_RESULTS);
			dropTable(db, Tables.GAME_POLL_RESULTS_RESULT);
			dropTable(db, Tables.GAME_COLORS);
			dropTable(db, Tables.PLAYS);
			dropTable(db, Tables.PLAY_ITEMS);
			dropTable(db, Tables.PLAY_PLAYERS);
			dropTable(db, Tables.COLLECTION_VIEWS);
			dropTable(db, Tables.COLLECTION_VIEW_FILTERS);
			dropTable(db, Tables.PLAYER_COLORS);

			onCreate(db);
		}
	}

	private void dropTable(SQLiteDatabase db, String tableName) {
		db.execSQL("DROP TABLE IF EXISTS " + tableName);
	}

	private void addColumn(SQLiteDatabase db, String table, String column, COLUMN_TYPE type) {
		try {
			db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
		} catch (SQLException e) {
			Timber.w("Probably just trying to add an existing column.\n" + e.toString());
		}
	}
}
