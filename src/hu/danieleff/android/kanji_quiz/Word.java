package hu.danieleff.android.kanji_quiz;

import hu.danieleff.android.kanji_quiz.Question.Type;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Word {
	public final String word;
	public final String trans1;
	public final String trans2;
	public int counter;

	//byte[6] = wordTrans1Score, wordTrans2Score, trans1WordScore, trans2WordScore, trans1Trans2Score, trans2Trans1Score
	private byte[] scores;

	public Word(String word, String trans1, String trans2) {
		this.word = word;
		this.trans1 = trans1;
		this.trans2 = trans2;
	}

	public int getMaxScore(boolean[] typesChecked) {		
		int ret=0;
		for(int i=0;i<6;i++) {
			if (typesChecked[i] && canHaveScore(i)) {
				ret+=Quiz.maxScorePerQuestion;
			}
		}
		return ret;
	}

	public int getTotalScore(boolean[] typesChecked) {		
		int ret=0;
		for(int i=0;i<6;i++) {
			if (typesChecked[i]) ret+=scores[i];
		}
		return ret;
	}
	
	public byte getScore(Type type) {
		return scores[type.id];
	}

	public void setScore(byte[] scores) {
		this.scores=scores;
	}
	
	public void setScore(Type type, byte newScore, int mult, String saveToDbQuizId) {
		if (scores[type.id]==newScore) return;
		scores[type.id]=newScore;
		if (saveToDbQuizId!=null) {
			save(saveToDbQuizId, type.columnName, newScore, mult);
		}
	}

	
	private void save(String saveToDbQuizId, String column, int score, int mult) {
		SQLiteDatabase db = App.db.getWritableDatabase();
		Cursor q = db.rawQuery("SELECT id FROM quiz WHERE id=? AND word=?", new String[]{saveToDbQuizId, word});
		try {
			if (q.moveToFirst()) {
				db.execSQL("UPDATE quiz SET "+column+"="+score+", "+column+"Mult="+mult+" WHERE id=? AND word=?", new String[]{saveToDbQuizId, word});
			} else {
				db.execSQL("INSERT INTO quiz (id, word, "+column+", "+column+"Mult) VALUES ('"+saveToDbQuizId+"', '"+word+"', "+score+", "+mult+")");
			}
		} finally {
			q.close();
		}
	}

	
	private boolean canHaveScore(int i) {
		if (trans1!=null && i==0) return true;
		if (trans2!=null && i==1) return true;
		
		if (trans1!=null && i==2) return true;
		if (trans2!=null && i==3) return true;
		
		if (trans1!=null && trans2!=null && i==4) return true;
		if (trans1!=null && trans2!=null && i==5) return true;
		
		return false;
	}
	
	public boolean hasFullScore(boolean[] typesChecked) {
		if (typesChecked[0] && trans1!=null && scores[0]<Quiz.maxScorePerQuestion) return false;
		if (typesChecked[1] && trans2!=null && scores[1]<Quiz.maxScorePerQuestion) return false;
		
		if (typesChecked[2] && trans1!=null && scores[2]<Quiz.maxScorePerQuestion) return false;
		if (typesChecked[3] && trans2!=null && scores[3]<Quiz.maxScorePerQuestion) return false;
		
		if (typesChecked[4] && trans1!=null && trans2!=null && scores[4]<Quiz.maxScorePerQuestion) return false;
		if (typesChecked[5] && trans1!=null && trans2!=null && scores[5]<Quiz.maxScorePerQuestion) return false;
		
		return true;
	}
}