package hu.danieleff.android.kanji_quiz;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Question {

	public final Word word;
	
	public final Type type;
	
	public Word[] answers;
	
	public Question(Word word, Type type) {
		this.word = word;
		this.type = type;
	}
	
	public String getQuestionText() {
		if (type==Type.WORD_TRANS1) return word.word;
		if (type==Type.WORD_TRANS2) return word.word;
		if (type==Type.TRANS1_WORD) return word.trans1;
		if (type==Type.TRANS2_WORD) return word.trans2;
		
		if (type==Type.TRANS1_TRANS2) return word.trans1;
		if (type==Type.TRANS2_TRANS1) return word.trans2;
		return null;
	}
	
	public String getAnswerText(int index) {
		if (answers[index]==null) return null;
		if (type==Type.WORD_TRANS1) return answers[index].trans1;
		if (type==Type.WORD_TRANS2) return answers[index].trans2;
		if (type==Type.TRANS1_WORD) return answers[index].word;
		if (type==Type.TRANS2_WORD) return answers[index].word;
		
		if (type==Type.TRANS1_TRANS2) return answers[index].trans2;
		if (type==Type.TRANS2_TRANS1) return answers[index].trans1;
		return null;
	}
	
	public int getScore() {
		return word.getScore(type);
	}

	public int missing(int maxScorePerQuestion) {
		return Math.max(0, maxScorePerQuestion - getScore());
	}

	public void saveAnswerToDb(String quizId, byte maxScorePerQuestion, boolean good) {
		byte old = word.getScore(type);
		//int debug=old;
		int mult=getMult(quizId);
		if (good) {
			old+=1 * mult;
			mult*=2;
		} else {
			mult=1;
			old-=1;
		}
		
		if (old<0) old=0;
		if (old>maxScorePerQuestion) old=maxScorePerQuestion;
		
		//Log.e("", word.word+" "+debug+" ="+(old-debug)+"> "+old);
		
		word.setScore(type, old, mult, quizId);
	}

	private int getMult(String quizId) {
		SQLiteDatabase db = App.db.getReadableDatabase();
		Cursor q = db.rawQuery("SELECT "+type.columnName+"Mult FROM quiz WHERE id=? AND word=?", new String[]{quizId, word.word});
		try {
			if (!q.moveToFirst()) return 2;
			int ret = q.getInt(0);
			if (ret<1) ret=2;
			return ret;
		} finally {
			q.close();
		}
	}



	enum Type {
		WORD_TRANS1(0, "wordTrans1Score"),
		WORD_TRANS2(1, "wordTrans2Score"),
		TRANS1_WORD(2, "trans1WordScore"),
		TRANS2_WORD(3, "trans2WordScore"),
		
		TRANS1_TRANS2(4, "trans1Trans2Score"),
		TRANS2_TRANS1(5, "trans2Trans1Score"),
		;
		final String columnName;
		final int id;

		Type(int id, String columnName) {
			this.id = id;
			this.columnName = columnName;
			
		}
	}
	
}
