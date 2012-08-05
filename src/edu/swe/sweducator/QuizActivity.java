package edu.swe.sweducator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class QuizActivity extends Activity {

	private static final String LOG_TAG = "QuizActivity";
	private static final String IS_CACHED = "isCached";
	private static final String ID_OF_ANSWER = "idOfAnswer";
	private static final String PREFS_NAME = "edu.swe.quiz.prefsfile";

	private DatabaseHelper dbh;
	private SQLiteDatabase db;

	private List<Word> quizWordList;
	private int idOfAnswer;
	private boolean isCached = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.quiz);
				
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		isCached = settings.getBoolean(IS_CACHED, false);
		idOfAnswer = settings.getInt(ID_OF_ANSWER, -1);
		Log.d(LOG_TAG, "isCached: " + isCached + " idOfAnswer: " + idOfAnswer);
		generateAndDisplayQuiz();
	}

	private void generateAndDisplayQuiz() {
		try {
			Cursor cursor;
			if (dbh == null) {
				dbh = new DatabaseHelper(this);
			}
			if (db == null || db.isOpen() == false) {
				db = dbh.getWritableDatabase();
			}
	        if (isCached == true) { //if screen flip, get words from secondary table
	        	cursor = db.rawQuery("SELECT * FROM wordsTable WHERE _id IN (SELECT word_id FROM savedQuizTable)", null);
	        } else { //get words from primary table
				cursor = db.rawQuery("SELECT * FROM wordsTable ORDER BY RANDOM() LIMIT 4", null);
	        }
	    	if (cursor != null && cursor.getCount() == 4) {
	    		if (cursor.moveToFirst()) {
	    			//store words in list
	    			quizWordList = new ArrayList<Word>(4);
	    			do {
	    				int wordId = cursor.getInt(cursor.getColumnIndex("_id"));
	    				String wordStr = cursor.getString(cursor.getColumnIndex("word"));
	    				String defStr = cursor.getString(cursor.getColumnIndex("definition"));
	    				quizWordList.add(new Word(wordId, wordStr, defStr));
	    			} while (cursor.moveToNext());
	    		} else {
	    			Log.e(LOG_TAG, "Error moving cursor to first row");
	    		}
	    	} else {
	    		Log.e(LOG_TAG, "Error, wrong amount of words in cursor:" + cursor.getCount());
	    	}
	    	startManagingCursor(cursor);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//choose the quiz word
    	int randInt = -1;
		if (!isCached) { //randomizes index and answer
			randInt = new Random(System.currentTimeMillis()).nextInt(4);
			idOfAnswer = quizWordList.get(randInt).getId();
			saveCurrentQuiz();
		} else { //retrieves cached index and answer
			assert(idOfAnswer != -1);
			assert(quizWordList.size() == 4);
			for (int i = 0; i < quizWordList.size(); i++) {
				if (quizWordList.get(i).getId() == idOfAnswer) {
					randInt = i;
				}
			}
			assert(randInt != -1);
		}
		Log.d(LOG_TAG, "Index of answer: " + randInt + " Id of answer: " + idOfAnswer);
		
		setupGUI(randInt);
		closeAllDBConnections();
	}

	private void closeAllDBConnections() {
		if (db != null && db.isOpen()) {
			db.close();
		}
		if (dbh != null) {
			dbh.close();
		}
	}
	
	@Override
	protected void onStop() {
		closeAllDBConnections();
		isCached = false;
		//TODO reset quiz score
		super.onStop();
	}
	
	@Override
	protected void onResume() {
		if (isCached) {
			SQLiteDatabase writeDB = dbh.getWritableDatabase();
			Cursor cursor = writeDB.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.savedQuizTable, null);
			cursor.moveToFirst();
			int count = cursor.getInt(0);
			if (count != 4) {
				cleanupCache(writeDB);
			}
			cursor.close();
			writeDB.close();
		}
		super.onResume();
	}

	private void cleanupCache(SQLiteDatabase writeDB) {
		Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
		editor.putBoolean(IS_CACHED, false);
		editor.putInt(ID_OF_ANSWER, -1);
		editor.commit();
		
		isCached = false;
		writeDB.delete(DatabaseHelper.savedQuizTable, null, null);
	}

	/**
	 * Called to setup the GUI when four words/defs and one correct answer have been chosed.
	 */
	private void setupGUI(int correctAnswerIndex) {
		TextView quizView = (TextView) findViewById(R.id.quizWordView);
		quizView.setText(quizWordList.get(correctAnswerIndex).getWord());
		int idArray[] = new int[] {R.id.defView0, R.id.defView1, R.id.defView2, R.id.defView3};
		for (int i = 0; i < idArray.length; i++) {
			Button defView = (Button) findViewById(idArray[i]);
			defView.setText(quizWordList.get(i).getDefinition());
			defView.setBackgroundResource(R.drawable.btn_recolored);
			int id = quizWordList.get(i).getId();
			defView.setOnClickListener(new OnQuizAnswerListener(id));
		}
	}
	
	/**
	 * Saves the current quiz words and correct answer to ensure quizzes survive screen flips.
	 */
	private void saveCurrentQuiz() {
		Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
		editor.putBoolean(IS_CACHED, true);
		editor.putInt(ID_OF_ANSWER, idOfAnswer);
		editor.commit();
		
		int deletedRows = db.delete(DatabaseHelper.savedQuizTable, "1", null);
		Log.d(LOG_TAG, "Rows deleted from savedQuizTable: " + deletedRows);
		ContentValues values;
		for (Word word : quizWordList) {
			values = new ContentValues(1);
			values.put("word_id", word.getId());
			db.insert(DatabaseHelper.savedQuizTable, null, values);
		}
	}
	
	/**
	 * To be called after quiz answer to get a new set of quiz words.
	 */
	private void generateNewQuiz() {
		Log.v(LOG_TAG, "New quiz!");
		
		isCached = false;
		Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
		editor.putBoolean(IS_CACHED, false);
		editor.putInt(ID_OF_ANSWER, 0);
		editor.commit();
		
		generateAndDisplayQuiz();
	}

	private class OnQuizAnswerListener implements OnClickListener {
		private final int id;

		public OnQuizAnswerListener(int id) {
			this.id = id;
		}

		@Override
		public void onClick(View view) {
			if (id == idOfAnswer) { //TODO increase quiz score
				new AsyncColorSwitcher((Button) view, Color.GREEN).execute((Object[])null);
			} else {
				new AsyncColorSwitcher((Button) view, Color.RED).execute((Object[])null);
			}
		}
		
		private class AsyncColorSwitcher extends AsyncTask<Object,Integer,Boolean> {
			
			private final Button view;
			private final int color;

			public AsyncColorSwitcher(Button view, int color) {
				this.view = view;
				this.color = color;
			}
			
			@Override
			protected void onPreExecute() {
				view.setBackgroundColor(color);
				//TODO color wrong answer red, correct answer green
			}
			
			@Override
			protected Boolean doInBackground(Object... arg0) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return true;
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				generateNewQuiz();
			}
			
		}
	}
}
