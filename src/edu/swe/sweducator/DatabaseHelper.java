package edu.swe.sweducator;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
	protected static final String DBNAME = "sweducatordb";
	protected static final int DBVERSION = 2;

	public static final String wordsTable = "wordsTable";
	public static final String savedQuizTable = "savedQuizTable";

	public DatabaseHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, null, version);
	}

	public DatabaseHelper(Context context, int version) {
		this(context, DBNAME, null, version);
	}

	public DatabaseHelper(Context context) {
		this(context, DBNAME, null, DBVERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE wordsTable(" +
				"_id integer primary key," +
				"word text," +
				"definition text," +
				"soundfile text);");
		db.execSQL("CREATE TABLE savedQuizTable(" +
				"_id integer primary key," +
				"word_id integer);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
		if (oldVer == 1 && newVer == 2) {
			db.execSQL("CREATE TABLE savedQuizTable(" +
					"_id integer primary key," +
					"word_id integer);");
		}
	}

}