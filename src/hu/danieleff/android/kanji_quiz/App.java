package hu.danieleff.android.kanji_quiz;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class App {

	private static int refcount;
	
	public static DB db;
	
	public static List<QuizFile> files;	
	
	public synchronized static void get(Context context) {
		if (refcount==0) {
			db=new DB(context);
			files=new ArrayList<QuizFile>();
			
			files.add(new QuizFile("hiragana.quiz", false));
			files.add(new QuizFile("katakana.quiz", false));
			
			files.add(new QuizFile("jlpt_5.quiz"));			
			files.add(new QuizFile("jlpt_5_v.quiz"));
			
	        files.add(new QuizFile("jlpt_4.quiz"));
	        files.add(new QuizFile("jlpt_4_v.quiz"));
	        
	        files.add(new QuizFile("jlpt_3.quiz"));
	        files.add(new QuizFile("jlpt_3_v.quiz"));
	        
	        files.add(new QuizFile("jlpt_2.quiz"));
	        //files.add(new QuizFile("jlpt_2_v.quiz"));
	        
	        files.add(new QuizFile("jlpt_1.quiz"));
	        //files.add(new QuizFile("jlpt_1_v.quiz"));
	        
	        files.add(new QuizFile("grade_1.quiz"));
	        files.add(new QuizFile("grade_2.quiz"));
	        files.add(new QuizFile("grade_3.quiz"));
	        files.add(new QuizFile("grade_4.quiz"));
	        files.add(new QuizFile("grade_5.quiz"));
	        files.add(new QuizFile("grade_6.quiz"));
	        
	        //files.add(new QuizFile("grade_8.quiz"));
	        //files.add(new QuizFile("grade_9.quiz"));
	        //files.add(new QuizFile("grade_10.quiz"));
		}
		refcount++;
	}
	
	public synchronized static void put() {
		refcount--;
		if (refcount<=0) {
			files.clear();
			db.close();
		}
	}
	
	public static class QuizFile {
		public String filename;
		public String error;
		public Quiz quiz;
		public final boolean hasTrans2;
		public QuizFile(String filename) {
			this.filename = filename;
			hasTrans2=true;
		}
		public QuizFile(String filename, boolean hasTrans2) {
			this.filename = filename;
			this.hasTrans2 = hasTrans2;
			
		}
	}
	
}
