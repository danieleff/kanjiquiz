package hu.danieleff.android.kanji_quiz;

import hu.danieleff.android.kanji_quiz.Question.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Quiz {

	public final String id;
	
	public final String name;
	
	public boolean kanjiList;
	
	public final List<Word> words;

	private Random random;
	
	public static int maxScorePerQuestionList=200;
	
	public static byte maxScorePerQuestion=10;

	public final String filename;

	private int total;
	
	public Quiz(String filename, String id, String name, List<Word> words) {
		this.filename = filename;
		this.id = id;
		this.name = name;
		this.words = words;
		random = new Random(System.currentTimeMillis());
	}
	
	public static Quiz load(String filename, boolean kanjiList, InputStream in) {
		List<Word> words=new ArrayList<Word>();
		String id=null;
		String name=null;
		int total=0;
        try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			
			while((line=reader.readLine())!=null) {
				if ("".equals(line)) break;
				String[] split = line.split("=");
				if ("id".equals(split[0].trim())) {
					id=split[1].trim();
				}
				if ("name".equals(split[0].trim())) {
					name=split[1].trim();
				}
			}
			
			while((line=reader.readLine())!=null) {
				int first = line.indexOf('|');
				int second = line.indexOf('|', first+1);
				Word item=null;
				
				if (second!=-1) {
					kanjiList=true;
					item = new Word(line.substring(0, first),
							line.substring(first+1, second),
							line.substring(second+1, line.length())
							);
					total+=60;
				} else {
					if (kanjiList) {
						item = new Word(line.substring(0, first),
								null,
								line.substring(first+1, line.length())
								);	
					} else {
						item = new Word(line.substring(0, first),
								line.substring(first+1, line.length()),
								null
								);
					}
					total+=20;
				}
				words.add(item);
			}
			in.close();
		} catch (IOException e) {
			Log.e("", e.getMessage(), e);
		}
        
        Quiz ret = new Quiz(filename, id, name, words);
        ret.kanjiList=kanjiList;
        ret.total=total;
        loadDb(ret);
        
        return ret;
	}

	private static void loadDb(Quiz ret) {
		Map<String, Map<String, Object>> words2 = App.db.getWords();
		Map<String, Object> map = words2.get(ret.id);
		if (map==null) {
			map=new HashMap<String, Object>();
			words2.put(ret.id, map);
		}
		for (Word w : ret.words) {
			byte[] b=(byte[]) map.get(w.word);
			if (b==null) {
				b=new byte[6];
				map.put(w.word, b);
			}
			w.setScore(b);
		}
	}

	public Question createQuestion(Context context, int start, int end) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean wordTrans1 = prefs.getBoolean("word_trans1", true);
		boolean wordTrans2 = prefs.getBoolean("word_trans2", true);
		boolean trans1Word = prefs.getBoolean("trans1_word", true);
		boolean trans2Word = prefs.getBoolean("trans2_word", true);
		
		boolean trans1Trans2 = prefs.getBoolean("trans1_trans2", false);
		boolean trans2Trans1 = prefs.getBoolean("trans2_trans1", false);
		
		if (!kanjiList) {
			trans1Word = prefs.getBoolean("trans1_word_kana", true);
			wordTrans1 = prefs.getBoolean("word_trans1_kana", true);
		}
		
		if (!wordTrans1 && !wordTrans2 && !trans2Word && !trans2Word && !trans1Trans2 && !trans2Trans1) {
			wordTrans1=true;
		}
		if (!kanjiList && !wordTrans1 && !trans1Word) {
			wordTrans1=true;
		}
		
		int totalMax;
		if (kanjiList) {
			totalMax=maxScorePerQuestionList*4;
		} else {
			totalMax=maxScorePerQuestionList*2;			
		}
		int current=0;
		List<Question> questions=new ArrayList<Question>();
		for(int i=start;i<end;i++) {
			Word w=words.get(i);
			
			Question q;
			if (trans1Word && w.trans1!=null) {
				q = new Question(w, Type.TRANS1_WORD);
				current+=add(questions, q);
				if (current>totalMax) break;
			}
			
			if (trans2Word && w.trans2!=null) {
				q = new Question(w, Type.TRANS2_WORD);
				current+=add(questions, q);
				if (current>totalMax) break;
			}
			
			if (wordTrans1 && w.trans1!=null) {
				q = new Question(w, Type.WORD_TRANS1);
				current+=add(questions, q);
				if (current>totalMax) break;
			}
			
			if (wordTrans2 && w.trans2!=null) {
				q = new Question(w, Type.WORD_TRANS2);
				current+=add(questions, q);
				if (current>totalMax) break;
			}
			
			if (trans1Trans2 && w.trans1!=null && w.trans2!=null) {
				q = new Question(w, Type.TRANS1_TRANS2);
				current+=add(questions, q);
				if (current>totalMax) break;
			}
			if (trans2Trans1 && w.trans1!=null && w.trans2!=null) {
				q = new Question(w, Type.TRANS2_TRANS1);
				current+=add(questions, q);
				if (current>totalMax) break;
			}
		}
		if (questions.size()==0) {
			return null;
		}
		
		Question ret=getRandom(questions, current);		
		ret.answers=new Word[4];
		ret.answers[0]=ret.word;
		
		Set<Word> uniqueWords=new HashSet<Word>();
		for (Question question : questions) {
			boolean ok=false;
			if ((ret.type==Type.TRANS1_WORD || ret.type==Type.WORD_TRANS1) && question.word.trans1!=null)
				ok=true;
			if ((ret.type==Type.TRANS2_WORD || ret.type==Type.WORD_TRANS2) && question.word.trans2!=null)
				ok=true;
			
			if ((ret.type==Type.TRANS1_TRANS2 || ret.type==Type.TRANS2_TRANS1))
				ok=true;
			
			if (ok)
				uniqueWords.add(question.word);
		}
		uniqueWords.remove(ret.word);
		ArrayList<Word> wordList = new ArrayList<Word>(uniqueWords);
		Collections.shuffle(wordList);
		if (wordList.size()>0) {
			ret.answers[1]=wordList.get(0);
		} else {
			ret.answers[1]=null;
		}
		if (wordList.size()>1) {
			ret.answers[2]=wordList.get(1);
		} else {
			ret.answers[2]=null;
		}
		if (wordList.size()>2) {
			ret.answers[3]=wordList.get(2);
		} else {
			ret.answers[3]=null;
		}
		Collections.shuffle(Arrays.asList(ret.answers));
		
		return ret;
	}
	
	private Question getRandom(List<Question> questions, int max) {
		int current=0;
		int index = random.nextInt(max);
		for (Question question : questions) {
			current+=question.missing(maxScorePerQuestion);
			if (current>=index) {
				questions.remove(question);
				return question; 
			}
		}
		return questions.get(0);
	}
	
	private int add(List<Question> questions, Question q) {
		if (q.missing(maxScorePerQuestion)>0) {
			questions.add(q);
			return q.missing(maxScorePerQuestion);
		}
		return 0;
	}

	public int getMaxScore() {
		return total;
	}
}
