package edu.swe.sweducator;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String LOG_TAG = "MainActivity";
	private ListView wordlistView;
	private SimpleCursorAdapter adapter;
	private WordListViewBinder viewBinder;
	private DatabaseHelper dbh = null;
	private SQLiteDatabase db = null;
	private SoundManager sm;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        try {
        	//parse xml data
        	if (dbh == null) {
        		dbh = new DatabaseHelper(this, 2);
        	}
        	if (db == null) {
        		db = dbh.getWritableDatabase();
        	}
        	if (isAllDataLoaded(db) == false) {
        		int deletedRows = db.delete(DatabaseHelper.wordsTable, null, null);
        		Log.d(LOG_TAG, "Rows deleted: " + deletedRows);
        		new XMLParser(this.getAssets(), db).parseData();
        	} //TODO parse data in async task
        	
        	//display in list
        	wordlistView = (ListView) findViewById(R.id.listView1);
        	final String[] columns = new String[] {"_id","word","definition"};
        	Cursor cursor = db.query(DatabaseHelper.wordsTable, columns, null, null, null, null, null);
        	String[] from = {"word","definition"};
        	int[] to = {R.id.wordLabel, R.id.defLabel};
        	adapter = new SimpleCursorAdapter(this, R.layout.list_item, cursor, from, to);
        	adapter.setViewBinder(viewBinder);
        	wordlistView.setAdapter(adapter);
        	wordlistView.setOnItemClickListener(new OnWordClickListener());
        	wordlistView.setOnItemLongClickListener(new OnItemLongClickListener() {
        		@Override
				public boolean onItemLongClick(AdapterView<?> av, View view, int pos, long id) {
					Intent quizIntent = new Intent(MainActivity.this, QuizActivity.class);
        			startActivity(quizIntent);
					return true;
				}
        	});
        	
        	startManagingCursor(cursor);
        	
//        	sm = new SoundManager(getAssets());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
//		if (!adapter.getCursor().isClosed()) {
//			adapter.getCursor().close();
//		}
//		if (db != null && db.isOpen()) {
//			db.close();
//		}
//		if (dbh != null) {
//			dbh.close();
//		}
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		Log.e(LOG_TAG, "in onRestart()");
		CursorAdapter adapter2 = (CursorAdapter) wordlistView.getAdapter();
		Cursor cursor2 = adapter2.getCursor();
		adapter2.changeCursor(cursor2);
		adapter2.notifyDataSetChanged();
		
		Log.e(LOG_TAG, "Adapter count: " + adapter2.getCount());
		Log.e(LOG_TAG, "Cursor count: " + cursor2.getCount());
	}
	
	private boolean isAllDataLoaded(SQLiteDatabase db) {
		boolean result = false;
		final String SQL_STATEMENT = "SELECT COUNT(*) FROM " + DatabaseHelper.wordsTable;
		Cursor cursor = db.rawQuery(SQL_STATEMENT, null);
		int count;
		cursor.moveToFirst();
		count = cursor.getInt(0);
		if (count >= 841) {
			result = true;
		}
		cursor.close();
		Log.d(LOG_TAG, "Rows found: " + count);
		return result;
	}
	
	private class OnWordClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> av, View view, int pos, long id) {
			TextView wordTextView = (TextView) view.findViewById(R.id.wordLabel);
			String word = wordTextView.getText().toString();
			try {
				SQLiteDatabase db = dbh.getReadableDatabase();
				Cursor cursor = db.query(DatabaseHelper.wordsTable, 
						new String[] {"_id","soundfile"}, "word = ?", 
						new String[] {word}, null, null, null);
				if (cursor.moveToFirst() != false) {
					String soundfile = cursor.getString(cursor.getColumnIndex("soundfile"));
//					new AsyncSoundPlayer(soundfile).execute((Object[])null);
					Log.d(LOG_TAG, "Word clicked, soundfile: " + soundfile);
				} else {
					Log.e(LOG_TAG, "No soundfile found for word " + word);
				}
				cursor.close();
				db.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				
			}
		}
	}
	
	private class AsyncSoundPlayer extends AsyncTask<Object,Integer,Boolean> {
		private final String fileName;
		
		public AsyncSoundPlayer(String fileName) {
			this.fileName = fileName;
		}
		
		@Override
		protected void onPreExecute() {
			Log.d(LOG_TAG,"Pre playback of " + fileName);
		}
		
		@Override
		protected Boolean doInBackground(Object... params) {
			//TODO make reusable by sending filename as a parameter
			sm.playSoundFileByName(fileName);
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			Log.d(LOG_TAG,"Post playback of " + fileName);
		}
	}
	
}