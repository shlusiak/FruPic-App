package de.saschahlusiak.frupic.db;

import de.saschahlusiak.frupic.model.Frupic;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class FrupicDB {
	Context context;
	SQLiteDatabase db;
	FrupicDBOpenHandler dbHelper;
	private static final String tag = FrupicDB.class.getSimpleName();


	public static final String TABLE = "frupics";
	public static final String ID_ID = "_id";					/* 0 */
	public static final String FULLURL_ID = "fullurl";			/* 1 */
	public static final String THUMBURL_ID = "thumburl";		/* 2 */
	public static final String DATE_ID = "date";				/* 3 */
	public static final String USERNAME_ID = "username";		/* 4 */
	public static final String FLAGS_ID = "flags";				/* 5 */

	/* NEVER CHANGE THE INDEX OF COLUMNS FOR BACKWARDS COMPATIBILITY */
	public static final int ID_INDEX = 0;
	public static final int FULLURL_INDEX = 1;
	public static final int THUMBURL_INDEX = 2;
	public static final int DATE_INDEX = 3;
	public static final int USERNAME_INDEX = 4;
	public static final int FLAGS_INDEX = 5;
	
	
	/* WARNING: The IDs are used in Cursors to query the colums, for compatibility they should NEVER be changed.
	 * Make sure to ONLY append colums. */
	public static final String CREATE_TABLE = "CREATE TABLE " + TABLE + " (" +
			ID_ID + " INTEGER PRIMARY KEY, " + 		/* 0 */
			FULLURL_ID + " TEXT, " +		/* 1 */
			THUMBURL_ID + " TEXTL, " +		/* 2 */
			DATE_ID + " TEXTL, " +			/* 3 */
			USERNAME_ID + " TEXT, " +		/* 4 */
			FLAGS_ID + " INTEGER);";				/* 5 */
	
	public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE + ";";

	public FrupicDB(Context context) {
		this.context = context;
	}

	public boolean open() {
		db = null;
		dbHelper = new FrupicDBOpenHandler(context);
		try {
			db = dbHelper.getWritableDatabase();
			db.setLockingEnabled(true);
		} catch (Exception e) {
			return false;
		}
		return (db != null);
	}

	public void close() {
		dbHelper.close();
	}

	public boolean addFrupic(Frupic frupic) {
		final String fields[] = { ID_ID };
		Cursor c = db.query(TABLE, fields, ID_ID + "=" + frupic.getId(), null, null, null, null);
		if (c.getCount() <= 0) {
			c.close();
			ContentValues values = new ContentValues();

			values.put(ID_ID, frupic.getId());
			values.put(FULLURL_ID, frupic.getFullUrl());
			values.put(THUMBURL_ID, frupic.getThumbUrl());
			values.put(DATE_ID, frupic.getDate());
			values.put(USERNAME_ID, frupic.getUsername());
			values.put(FLAGS_ID, frupic.getFlags());

			db.insert(TABLE, null, values);
			return true;
		}
		c.close();
		return false;
	}
	
	public boolean addFrupics(Frupic frupics[]) {
		db.beginTransaction();
		for (Frupic frupic: frupics) {
			addFrupic(frupic);
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		return true;
	}

	public boolean clearAll(boolean includeFavourite) {
		try {
			return db.delete(TABLE, includeFavourite ? null : ("NOT " + FLAGS_ID + "=" + Frupic.FLAG_FAV), null) > 0;
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean setFlags(Frupic frupic) {
		ContentValues values = new ContentValues();
		
		values.put(FLAGS_ID, frupic.getFlags());
		db.update(TABLE, values, ID_ID + "=" + frupic.getId(), null);
		return true;
	}
	
	public boolean markAllSeen() {
		ContentValues values = new ContentValues();

		values.put(FLAGS_ID, 0);

		db.update(TABLE, values, FLAGS_ID + "=" + Frupic.FLAG_NEW, null);
		return true;
	}

	public Cursor getFrupics(String username) {
		String where;
		
		if (username != null)
			where = USERNAME_ID + "=" + username;
		else
			where = null;
		
		Cursor c = db.query(TABLE, null, where, null, null, null, ID_ID + " DESC", null);
		if (c.getCount() <= 0) {
			c.close();
			return null;
		}
		
		return c;
	}
	
	public Cursor getFavFrupics() {
		String where;
		
		where = FLAGS_ID + "=" + Frupic.FLAG_FAV;
		
		Cursor c = db.query(TABLE, null, where, null, null, null, ID_ID + " DESC", null);
		if (c.getCount() <= 0) {
			c.close();
			return null;
		}
		
		return c;
	}
	
}
