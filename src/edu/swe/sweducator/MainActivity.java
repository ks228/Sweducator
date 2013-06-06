package edu.swe.sweducator;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String LOG_TAG = "MainActivity";
	private ListView wordlistView;
	private SimpleCursorAdapter adapter;
	private WordListViewBinder viewBinder;
	private DatabaseHelper dbh = null;
	private SQLiteDatabase db = null;
	private SoundManager soundManager;
	
	private static final String PREFS_NAME = "edu.swe.sweducator.preferences";
	private static final String LIST_POS = "listPos";
	private static final String LENGTH_FROM_TOP = "lengthFromTop";
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        soundManager = new SoundManager(getAssets());
        
        try {
        	if (dbh == null) {
        		dbh = new DatabaseHelper(this, 2);
        	}
        	if (db == null) {
        		db = dbh.getWritableDatabase();
        	}
        	if (isAllDataLoaded(db) == false) {
        		new AsyncParser().execute((Object[])null);
        	}
        	
        	displayWordList();
        	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void displayWordList() {
		//display in list
		wordlistView = (ListView) findViewById(R.id.listView1);
		wordlistView.setFastScrollEnabled(true);
		final String[] columns = new String[] {"_id","word","definition"};
		Cursor cursor = db.query(DatabaseHelper.wordsTable, columns, null, null, null, null, null);
		String[] from = {"word", "definition"};
		int[] to = {R.id.wordLabel, R.id.defLabel};
		adapter = new SimpleCursorAdapter(this, R.layout.list_item, cursor, from, to);
		adapter.setViewBinder(viewBinder);
		wordlistView.setAdapter(adapter);
		wordlistView.setOnItemClickListener(new OnWordClickListener());
		wordlistView.setOnItemLongClickListener(new OnItemLongClickListener() { //TODO replace with menu/action bar
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View view, int pos, long id) {
				Intent quizIntent = new Intent(MainActivity.this, QuizActivity.class);
				startActivity(quizIntent);
				return true;
			}
		});
		startManagingCursor(cursor);
	}

	private void parseData() throws IOException {
		int deletedRows = db.delete(DatabaseHelper.wordsTable, null, null);
		Log.d(LOG_TAG, "Rows deleted: " + deletedRows);
		new XMLParser(this.getAssets(), db).parseData();
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.mainmenu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.quiz_menu_item: {
	            Intent intent = new Intent(MainActivity.this, QuizActivity.class);
	            startActivity(intent);
	            return true;
	        }
	        case R.id.about_menu_item: {
	        	Intent intent = new Intent(MainActivity.this, AboutActivity.class);
	        	startActivity(intent);
	        	return true;
	        }
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private class OnWordClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> av, View view, int pos, long id) {
			TextView wordTextView = (TextView) view.findViewById(R.id.wordLabel);
			String word = wordTextView.getText().toString();
			SQLiteDatabase db = null;
			Cursor cursor = null;
			try {
				db = dbh.getReadableDatabase();
				cursor = db.query(DatabaseHelper.wordsTable, 
						new String[] {"_id","soundfile"}, "word = ?", 
						new String[] {word}, null, null, null);
				if (cursor.moveToFirst() != false) {
					String soundfile = cursor.getString(cursor.getColumnIndex("soundfile"));
					new AsyncSoundPlayer().execute(soundfile);
					Log.d(LOG_TAG, "Word clicked, soundfile: " + soundfile);
				} else {
					Log.e(LOG_TAG, "No soundfile found for word " + word);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {	
				cursor.close();
				DatabaseHelper.safelyCloseDB(db);
			}
		}
	}
	
	@Override
	protected void onResume() { //requery for list and restore list position
		dbh = new DatabaseHelper(this);
		SQLiteDatabase db = dbh.getReadableDatabase();
		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_WORLD_READABLE);
		
		wordlistView = (ListView) findViewById(R.id.listView1);
		Cursor cursor = db.query(DatabaseHelper.wordsTable, new String[] {"_id","word","definition"}, null, null, null, null, null);
		refreshList(cursor);
		
		startManagingCursor(cursor);
		
		int listPos = prefs.getInt(LIST_POS, 0);
		int lengthFromTop = prefs.getInt(LENGTH_FROM_TOP, 0);
		wordlistView.setSelectionFromTop(listPos, lengthFromTop);
		if (listPos != 0) {
			Log.d(LOG_TAG, "Restoring list position to index " + listPos);
		}
		
		super.onResume();
	}
	
	private void refreshList(Cursor cursor) {
		if (cursor.getCount() <= 0) {
			wordlistView.setVisibility(View.GONE);
			new TextView(this).setText("Error loading dictionary, please reinstall app");
		} else {
			String[] from = {"word", "definition"};
			int[] to = {R.id.wordLabel, R.id.defLabel};
			adapter = new SimpleCursorAdapter(this, R.layout.list_item, cursor, from, to);
			adapter.setViewBinder(viewBinder);
			wordlistView.setAdapter(adapter);
		}
	}
	
	@Override
	protected void onPause() { //save position in list on pause
		if (wordlistView != null) {
			int position = wordlistView.getFirstVisiblePosition();
			View v = wordlistView.getChildAt(0);
			int lengthFromTop = (v == null) ? 0 : v.getTop();
			SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_WORLD_WRITEABLE);
			Editor edit = prefs.edit();
			edit.putInt(LIST_POS, position);
			edit.putInt(LENGTH_FROM_TOP, lengthFromTop);
			edit.commit();
		}
		
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		DatabaseHelper.safelyCloseDB(db);
		DatabaseHelper.safelyCloseDBHelper(dbh);
		super.onStop();
	}
	
	private class AsyncSoundPlayer extends AsyncTask<Object,Integer,Boolean> {	
		@Override
		protected Boolean doInBackground(Object... params) {
			String fileName = (String) params[0];
			return soundManager.playSoundFile("audio/" + fileName);
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (!result) {
				Toast.makeText(MainActivity.this, 
						"Sound not available yet, " +
						"will come in a future update!", 
						Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private class AsyncParser extends AsyncTask<Object,Integer,Boolean> {
		private ProgressDialog dialog;
		
		public AsyncParser() {
			dialog = new ProgressDialog(MainActivity.this);
		}
		
		@Override
		protected void onPreExecute() {
			this.dialog.setMessage("Installing dictionary, do not interrupt...");
			this.dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			this.dialog.show();
		}
		
		@Override
		protected Boolean doInBackground(Object... params) {
			try {
				parseData();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (dialog.isShowing()) {
	            dialog.dismiss();
	        }
			
			displayWordList();
			ListView listview = (ListView) findViewById(R.id.listView1);
			listview.setVisibility(View.VISIBLE);
		}
		
	}
	
}