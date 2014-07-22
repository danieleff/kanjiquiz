package hu.danieleff.android.kanji_quiz;

import hu.danieleff.android.kanji_quiz.Question.Type;
import hu.danieleff.kanji_quiz_debug.R;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class QuizActivity extends ActionActivity {
	private Quiz quiz;
	private Button[] buttons=new Button[4];
	private Question question;
	private TextView questionTextView;
	private ColorStateList defColors;
	private ColorStateList defColors2;
	private Handler handler=new Handler();
	private View table;
	private int start;
	private int end;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.get(this);
        
        setContentView(R.layout.activity_quiz);
        
        questionTextView = (TextView) findViewById(R.id.textView1);
        buttons[0]=(Button) findViewById(R.id.button1);
        buttons[1]=(Button) findViewById(R.id.button2);
        buttons[2]=(Button) findViewById(R.id.button3);
        buttons[3]=(Button) findViewById(R.id.button4);
        table = findViewById(R.id.tableLayout1);
		
        defColors = buttons[0].getTextColors();
        defColors2 = ((TextView) table.findViewById(R.id.textView1)).getTextColors();
        
        int intExtra = getIntent().getIntExtra(QuizListActivity.EXTRA_QUIZFILE_ID, 0);
        start = getIntent().getIntExtra(QuizListActivity.EXTRA_QUIZFILE_START_ID, 0);
        end = getIntent().getIntExtra(QuizListActivity.EXTRA_QUIZFILE_END_ID, 0);
        quiz = App.files.get(intExtra).quiz;
        setTitle(quiz.name);
        
        newQuestion();
        table.setVisibility(View.GONE);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("hint_shown", false)) {
        	prefs.edit().putBoolean("hint_shown", true).commit();
        	Toast.makeText(this, "Set question types in settings menu", Toast.LENGTH_LONG).show();
        }
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		App.put();
	}
	
	private void newQuestion() {
		question=quiz.createQuestion(this, start, end);
		if (question!=null) {
			init();
		} else {
			Toast.makeText(this, "No more questions in this quiz", Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	
	private void init() {
		questionTextView.setText(question.getQuestionText());
		questionTextView.setTextSize(10+getFontSize(questionTextView.getText()));
		
		buttons[0].setText(question.getAnswerText(0));
		buttons[1].setText(question.getAnswerText(1));
		buttons[2].setText(question.getAnswerText(2));
		buttons[3].setText(question.getAnswerText(3));
		for (Button b : buttons) {
			CharSequence text = b.getText();
			b.setTextColor(defColors);
			b.setEnabled(text.length()!=0);
			
			b.setTextSize(getFontSize(text));
		}
		
		setProgress(quiz, getTypesChecked(quiz.kanjiList));
	}
	
	private int getFontSize(CharSequence charSequence) {
		int size = 35 - charSequence.length() * 2;
		if (size>25) size=25;
		if (size<15) size=15;
		return size;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
	}
	
	public void onAnswer1(View view) {
		onAnswer(0);
	}
	public void onAnswer2(View view) {
		onAnswer(1);
	}
	public void onAnswer3(View view) {
		onAnswer(2);
	}
	public void onAnswer4(View view) {
		onAnswer(3);
	}

	private void onAnswer(int index) {
		table.setVisibility(View.GONE);
		
		for (Button b : buttons) {
			b.setEnabled(false);
		}
		
		boolean ok=false;
		if (question.word.word.equals(question.answers[index].word)) {
			ok=true;
		}
		if ((question.type==Type.TRANS1_WORD || question.type==Type.WORD_TRANS1)
				&& question.word.trans1.equals(question.answers[index].trans1)
				) {
			ok=true;
		}
		if ((question.type==Type.TRANS2_WORD || question.type==Type.WORD_TRANS2)
				&& question.word.trans2.equals(question.answers[index].trans2)
				) {
			ok=true;
		}
		if ((question.type==Type.TRANS1_TRANS2 || question.type==Type.TRANS2_TRANS1)
				&& question.word.trans1.equals(question.answers[index].trans1)
				) {
			ok=true;
		}
		
		if (ok) {
			question.saveAnswerToDb(quiz.id, Quiz.maxScorePerQuestion, true);
			buttons[index].setTextColor(Color.GREEN);
			
			handler.postDelayed(new Runnable() {
				public void run() {
					newQuestion();
				}
			}, (int)(0.5 * 1000));
			
		} else {
			question.saveAnswerToDb(quiz.id, Quiz.maxScorePerQuestion, false);
			buttons[index].setTextColor(Color.RED);
			for (int j=0;j<4;j++) {
				if (question.answers[j]!=null) {
					if (question.word.word.equals(question.answers[j].word)) {
						buttons[j].setTextColor(Color.GREEN);
					}
				}
			}
			
			handler.postDelayed(new Runnable() {
				public void run() {
					newQuestion();
				}
			}, (int)(1 * 1000));
			
			table.setVisibility(View.VISIBLE);
			TextView textView1 = (TextView) table.findViewById(R.id.textView1);
			TextView textView2 = (TextView) table.findViewById(R.id.textView2);
			TextView textView3 = (TextView) table.findViewById(R.id.textView3);
			TextView textView4 = (TextView) table.findViewById(R.id.textView4);
			TextView textView5 = (TextView) table.findViewById(R.id.textView5);
			TextView textView6 = (TextView) table.findViewById(R.id.textView6);
			
			textView1.setTextColor(defColors2);
			textView2.setTextColor(defColors2);
			textView3.setTextColor(defColors2);
			textView4.setTextColor(defColors2);
			textView5.setTextColor(defColors2);
			textView6.setTextColor(defColors2);
			
			textView1.setText(question.word.word);
			textView2.setText(question.word.trans1);
			textView3.setText(question.word.trans2);
			
			textView4.setText(question.answers[index].word);
			textView5.setText(question.answers[index].trans1);
			textView6.setText(question.answers[index].trans2);
			
			if (question.type==Type.WORD_TRANS1) {
				textView1.setTextColor(Color.GREEN);
				textView2.setTextColor(Color.GREEN);
				textView5.setTextColor(Color.RED);
			}
			if (question.type==Type.WORD_TRANS2) {
				textView1.setTextColor(Color.GREEN);
				textView3.setTextColor(Color.GREEN);
				textView6.setTextColor(Color.RED);
			}
			if (question.type==Type.TRANS1_WORD) {
				textView2.setTextColor(Color.GREEN);
				textView1.setTextColor(Color.GREEN);
				textView4.setTextColor(Color.RED);
			}
			if (question.type==Type.TRANS2_WORD) {
				textView3.setTextColor(Color.GREEN);
				textView1.setTextColor(Color.GREEN);
				textView4.setTextColor(Color.RED);
			}
			if (question.type==Type.TRANS1_TRANS2) {
				textView2.setTextColor(Color.GREEN);
				textView3.setTextColor(Color.GREEN);
				textView6.setTextColor(Color.RED);
			}
			if (question.type==Type.TRANS2_TRANS1) {
				textView3.setTextColor(Color.GREEN);
				textView2.setTextColor(Color.GREEN);
				textView5.setTextColor(Color.RED);
			}
		}
		
	}

}