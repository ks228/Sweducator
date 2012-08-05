package edu.swe.sweducator;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Xml;

public class XMLParser {
	
	private static final String LOG_TAG = "XMLParser";
	private InputStream is;
	private SQLiteDatabase db;
	
	public XMLParser(AssetManager assets, SQLiteDatabase db) throws IOException {
		this.db = db;
		this.is = assets.open("swe_dict.xml");
	}
	
	private XmlPullParser setupParser() throws XmlPullParserException,IOException {
		//Gamla inställningar
		XmlPullParser xpp;
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setValidating(false);
		factory.setFeature(Xml.FEATURE_RELAXED, true);
		factory.setNamespaceAware(true);
		xpp = factory.newPullParser();
		
		xpp.setInput(is, "UTF8");
		return xpp;
	}
	
	private void insertWordIntoDB(String orig, String filepath, String type, String def) throws SQLException {
		ContentValues values = new ContentValues(3);
		values.put("word", orig);
		values.put("soundfile", filepath);
		//values.put("type", type);
		values.put("definition", def);
		long result = db.insertOrThrow(DatabaseHelper.wordsTable, null, values);
		if (result == -1) {
			Log.e(LOG_TAG, "Database insert of " + orig + " failed");
		}
	}
	
	public void parseData() {
		try {
			XmlPullParser xpp = setupParser();
			int tag = xpp.next();
			String tagName = "";
			String orig = "", filepath = "", type = "", def = "";
			while (tag != XmlPullParser.END_DOCUMENT) {
				if (tag == XmlPullParser.START_TAG) {
					tagName = xpp.getName();
					if ("word".equals(tagName)) {
						orig = xpp.getAttributeValue(null, "orig");
						filepath = xpp.getAttributeValue(null, "soundfile");
						//type = xpp.getAttributeValue(null, "type");
						def = xpp.nextText();
						if (orig.length() > 0 && def.length() > 0) {
							insertWordIntoDB(orig, filepath, type, def);
							//Log.d(LOG_TAG, orig);
						}
					}
				} else if (tag == XmlPullParser.END_TAG) {
//					tagName = xpp.getName();
//					if ("".equals(tagName)) {
//						
//					}
				}
				tag = xpp.next();
			}
			Log.d(LOG_TAG, "Klar med parseing");
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
