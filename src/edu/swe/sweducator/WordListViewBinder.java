package edu.swe.sweducator;

import android.database.Cursor;
import android.view.View;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter;

public class WordListViewBinder implements SimpleCursorAdapter.ViewBinder {

	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		if (columnIndex == 1) {
			TextView word = (TextView) view;
			word.setText(cursor.getString(cursor.getColumnIndex("word")));
			return true;
		} else if (columnIndex == 2) {
			TextView def = (TextView) view;
			def.setText(cursor.getString(cursor.getColumnIndex("definition")));
			return true;
		}
		return false;
	}

}
