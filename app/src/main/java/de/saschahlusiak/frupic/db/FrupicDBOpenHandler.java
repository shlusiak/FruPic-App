package de.saschahlusiak.frupic.db;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class FrupicDBOpenHandler extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "frupic.db";
	private static final int DATABASE_VERSION = 2;
	private static final String tag = FrupicDBOpenHandler.class.getSimpleName();

	public FrupicDBOpenHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(FrupicDB.CREATE_TABLE);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		try {
			if (oldVersion < 2) {
				db.execSQL(FrupicDB.UPGRADE_FROM_1);
			}
		}catch (Exception e) {
			e.printStackTrace();
			db.execSQL(FrupicDB.DROP_TABLE);
			onCreate(db);
		}
	}

}
