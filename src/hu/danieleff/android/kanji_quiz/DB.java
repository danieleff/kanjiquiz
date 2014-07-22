package hu.danieleff.android.kanji_quiz;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DB extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 2;
	
	private static final String DATABASE_NAME = "quiz";
	
	public DB(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public Map<String, Map<String, Object>> words;
	
	public synchronized Map<String, Map<String, Object>> getWords() {
		if (words==null) {
			words=new HashMap<String, Map<String,Object>>();
			
			SQLiteDatabase db = App.db.getReadableDatabase();
			Cursor q = db.rawQuery("SELECT id, word, wordTrans1Score, wordTrans2Score, trans1WordScore, trans2WordScore, trans1Trans2Score, trans2Trans1Score FROM quiz", null);
			try {
				if (q.moveToFirst()) {
					do {
						String id = q.getString(0);
						String word = q.getString(1);
						Map<String, Object> map = words.get(id);
						if (map==null) {
							map=new HashMap<String, Object>();
							words.put(id, map);
						}
						map.put(word, new byte[]{
								(byte) q.getInt(2),
								(byte) q.getInt(3),
								(byte) q.getInt(4),
								(byte) q.getInt(5),
								(byte) q.getInt(6),
								(byte) q.getInt(7),
								});
					} while (q.moveToNext());
				}
			} finally {
				q.close();
			}
		}
		return words;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE quiz (" + 
				"id TEXT NOT NULL" + 
				",word TEXT NOT NULL " + 
				",wordTrans1Score INTEGER" +
				",wordTrans2Score INTEGER" +
				",trans1WordScore INTEGER" +
				",trans2WordScore INTEGER" +
				
				",trans1Trans2Score INTEGER" +
				",trans2Trans1Score INTEGER" +
				
				",wordTrans1ScoreMult INTEGER" +
				",wordTrans2ScoreMult INTEGER" +
				",trans1WordScoreMult INTEGER" +
				",trans2WordScoreMult INTEGER" +
				
				",trans1Trans2ScoreMult INTEGER" +
				",trans2Trans1ScoreMult INTEGER" +
				")");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion<2) {
			db.execSQL("ALTER TABLE quiz ADD COLUMN trans1Trans2Score INTEGER");
			db.execSQL("ALTER TABLE quiz ADD COLUMN trans2Trans1Score INTEGER");
			db.execSQL("ALTER TABLE quiz ADD COLUMN trans1Trans2ScoreMult INTEGER");
			db.execSQL("ALTER TABLE quiz ADD COLUMN trans2Trans1ScoreMult INTEGER");
		}
	}

}
