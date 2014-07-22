package hu.danieleff.android.kanji_quiz;

import hu.danieleff.android.kanji_quiz.Question.Type;
import hu.danieleff.kanji_quiz_debug.R;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WordListActivity extends ActionActivity implements OnItemClickListener {
	
	private ListView listView;
	private Quiz quiz;
	private List<Word> words;
	private int quizFileId;
	private WordListAdapter wordList;
	private int start;
	private int end;
	private int current;
	private CheckBox checkbox;
	private boolean[] typesChecked;
	private int hideWords;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		App.get(this);
        setContentView(R.layout.activity_word_list);
        checkbox = (CheckBox) findViewById(R.id.checkbox);

        checkbox.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("hide_words_max", false));
		checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setWords();
			}
		});
        
        quizFileId = getIntent().getIntExtra(QuizListActivity.EXTRA_QUIZFILE_ID, 0);
        
        start = getIntent().getIntExtra(QuizListActivity.EXTRA_QUIZFILE_START_ID, 0);
        end = getIntent().getIntExtra(QuizListActivity.EXTRA_QUIZFILE_END_ID, 0);
        
        quiz = App.files.get(quizFileId).quiz;
        getTypesChecked();
        
        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        
        setTitle(quiz.name);
        
        wordList = new WordListAdapter(this, 0);
        listView.setAdapter(wordList);
        
	}

	@Override
	protected void onPause() {
		super.onPause();
		Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
		edit.putBoolean("hide_words_max", checkbox.isChecked());
		edit.commit();
	}
	
	private void getTypesChecked() {
		typesChecked = getTypesChecked(quiz.kanjiList);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete_score:
			Builder builder = new AlertDialog.Builder(this);
			View view = getLayoutInflater().inflate(R.layout.dialog_input, null);
			View okButton = view.findViewById(R.id.button1);
			View cancelButton = view.findViewById(R.id.button2);
			
			final EditText startEditText = (EditText) view.findViewById(R.id.editText1);
			final EditText endEditText = (EditText) view.findViewById(R.id.editText2);
			
			startEditText.setText(""+(start+1));
			endEditText.setText(""+end);
			final int max = quiz.words.size();
			
			builder.setTitle("Delete score in the word range:");
			builder.setView(view);
			
			final AlertDialog dialog = builder.create();
			okButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					int start=0;
					int end=0;
					try {
						start = Integer.parseInt(startEditText.getText().toString())-1;
						end = Integer.parseInt(endEditText.getText().toString());
					} catch (NumberFormatException e) {
						
					}
					int startMin=0;
					int startMax=max;
					if (start<startMin || start>startMax) {
						Toast.makeText(WordListActivity.this, "Start must be between "+(startMin+1)+" and "+startMax, Toast.LENGTH_SHORT).show();
						return;
					}
					int endMin=start+1;
					int endMax=max;
					if (end<endMin || end>endMax) {
						Toast.makeText(WordListActivity.this, "End must be between "+endMin+" and "+endMax, Toast.LENGTH_SHORT).show();
						return;
					}
					
					App.db.getWritableDatabase().beginTransaction();
					try {
						for(int i = start;i<end;i++) {
							Word word = quiz.words.get(i);
							word.setScore(Type.TRANS1_WORD, (byte)0, 0, quiz.id);
							word.setScore(Type.WORD_TRANS1, (byte)0, 0, quiz.id);
							if (word.trans2!=null) {
								word.setScore(Type.WORD_TRANS2, (byte)0, 0, quiz.id);
								word.setScore(Type.TRANS2_WORD, (byte)0, 0, quiz.id);
							}
							if (word.trans2!=null && word.trans1!=null) {
								word.setScore(Type.TRANS1_TRANS2, (byte)0, 0, quiz.id);
								word.setScore(Type.TRANS2_TRANS1, (byte)0, 0, quiz.id);
							}
						}
						App.db.getWritableDatabase().setTransactionSuccessful();
					} finally {
						App.db.getWritableDatabase().endTransaction();
					}
					
					wordList.notifyDataSetChanged();
					setProgress(quiz, getTypesChecked(quiz.kanjiList));
					
					dialog.dismiss();
				}
			});
			cancelButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			
			dialog.show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		getTypesChecked();
		setWords();
		setProgress(quiz, getTypesChecked(quiz.kanjiList));
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		App.put();
	}
	
	private void setWords() {
		words=new ArrayList<Word>();
		int counter=start+1;
		hideWords=0;
		for (Word w : quiz.words.subList(start, end)) {
			boolean hide=w.hasFullScore(typesChecked);
			if (hide) hideWords++;
			if (!checkbox.isChecked() || !hide) {
				words.add(w);
				w.counter=counter;
			}
			counter++;
		}
		if (hideWords==0) {
			checkbox.setText("Hide words with max score");
		} else {
			checkbox.setText(Html.fromHtml("Hide words with max score (<font color=\"green\"><b>"+hideWords+"</b></font>)"));
		}
		wordList.notifyDataSetChanged();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
	}
	
	public void onTakeQuiz(View view) {
		Intent intent = new Intent(this, QuizActivity.class);
		intent.putExtra(QuizListActivity.EXTRA_QUIZFILE_ID, quizFileId);
		intent.putExtra(QuizListActivity.EXTRA_QUIZFILE_START_ID, start);
		intent.putExtra(QuizListActivity.EXTRA_QUIZFILE_END_ID, end);
		startActivity(intent);
		overridePendingTransition(R.anim.left, R.anim.right);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
		current = pos;
		Builder builder = new AlertDialog.Builder(this);
		
		View layout = getLayoutInflater().inflate(R.layout.dialog, null);
		final TextView textView = (TextView) layout.findViewById(R.id.textView);
		final TextView textView2 = (TextView) layout.findViewById(R.id.textView2);
		
		Button prev = (Button) layout.findViewById(R.id.previousButton);
		Button next = (Button) layout.findViewById(R.id.nextButton);
		final Button delete = (Button) layout.findViewById(R.id.deleteButton);
		
		builder.setView(layout);
		builder.setPositiveButton(android.R.string.ok, null);
		final AlertDialog alertDialog = builder.create();
		
		prev.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				current--;
				if (current<0) current=words.size()-1;
				setMessage(alertDialog, delete, textView, textView2);
			}
		});
		next.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				current++;
				if (current>=words.size()) current=0;
				setMessage(alertDialog, delete, textView, textView2);
			}
		});
		delete.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Word word = words.get(current);
				word.setScore(Type.TRANS1_WORD, (byte)0, 0, quiz.id);
				word.setScore(Type.WORD_TRANS1, (byte)0, 0, quiz.id);
				if (word.trans2!=null) {
					word.setScore(Type.WORD_TRANS2, (byte)0, 0, quiz.id);
					word.setScore(Type.TRANS2_WORD, (byte)0, 0, quiz.id);
				}
				if (word.trans2!=null && word.trans1!=null) {
					word.setScore(Type.TRANS1_TRANS2, (byte)0, 0, quiz.id);
					word.setScore(Type.TRANS2_TRANS1, (byte)0, 0, quiz.id);
				}
				setMessage(alertDialog, delete, textView, textView2);
			}
		});
		setMessage(alertDialog, delete, textView, textView2);
		alertDialog.setOnDismissListener(new OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				wordList.notifyDataSetChanged();
			}
		});
		alertDialog.show();
	}

	private void setMessage(AlertDialog dialog, Button delete, TextView textView, TextView textView2) {
		Word word = words.get(current);
		
		delete.setEnabled(word.getTotalScore(typesChecked)>0);
		
		String message="";
		String message2="";
		if (word.trans1!=null && word.trans2!=null) {
			message+="Reading: <b>"+word.trans1+"</b>";
			message+="<br>Meaning: <b>"+word.trans2+"</b>";
		} else if (word.trans1!=null) {
			message+="Meaning: <b>"+word.trans1+"</b>";
		} else {
			message+="Meaning: <b>"+word.trans2+"</b>";
		}
		
		message2="Kanji->Kana score: <b>"+word.getScore(Type.WORD_TRANS1)+" / "+Quiz.maxScorePerQuestion+"</b><br>";
		message2+="Kana->Kanji score: <b>"+word.getScore(Type.TRANS1_WORD)+" / "+Quiz.maxScorePerQuestion+"</b><br>";
		if (word.trans2!=null) {
			message2+="Kanji->English score: <b>"+word.getScore(Type.WORD_TRANS2)+" / "+Quiz.maxScorePerQuestion+"</b><br>";
			message2+="English->Kanji score: <b>"+word.getScore(Type.TRANS2_WORD)+" / "+Quiz.maxScorePerQuestion+"</b><br>";
		}
		if (word.trans1!=null && word.trans2!=null) {
			message2+="Kana->English score: <b>"+word.getScore(Type.TRANS1_TRANS2)+" / "+Quiz.maxScorePerQuestion+"</b><br>";
			message2+="English->Kanascore: <b>"+word.getScore(Type.TRANS2_TRANS1)+" / "+Quiz.maxScorePerQuestion+"</b><br>";
		}
		
		dialog.setTitle(word.word + " ("+(current+1)+"/"+words.size()+")");
		textView.setText(Html.fromHtml(message));
		textView2.setText(Html.fromHtml(message2));
	}
	
	class WordListAdapter extends ArrayAdapter<Word> {
		public WordListAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}
		@Override
    	public int getCount() {
			if (words==null) return 0;
    		return words.size();
    	}
    	@Override
    	public Word getItem(int position) {
    		if (words==null) return null;
    		return words.get(position);
    	}
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		LinearLayout ret = (LinearLayout) convertView;
    		if (ret==null) {
    			ret=(LinearLayout) getLayoutInflater().inflate(R.layout.word_row, null);
    		}
    		Word item = getItem(position);
    		TextView idTextView = (TextView) ret.findViewById(R.id.idTextView);
    		TextView scoreTextView = (TextView) ret.findViewById(R.id.scoreTextView);
    		TextView textView1 = (TextView) ret.findViewById(R.id.textView1);
    		TextView textView2 = (TextView) ret.findViewById(R.id.textView2);
    		TextView textView3 = (TextView) ret.findViewById(R.id.textView3);
    		
    		//int index=position+start+1;
    		idTextView.setText(""+item.counter);
    		scoreTextView.setText(""+item.getTotalScore(typesChecked));
    		if (item.trans1!=null || !quiz.kanjiList) {
	    		textView1.setText(item.word);
	    		textView2.setText(item.trans1);
	    		textView3.setText(item.trans2);
    		} else {
    			textView1.setText(null);
	    		textView2.setText(item.word);
	    		textView3.setText(item.trans2);
    		}
    		
    		int alpha = 255 - item.getTotalScore(typesChecked) * 4 * (item.trans2!=null?1:2);
    		textView1.setTextColor(textView1.getTextColors().withAlpha(alpha));
    		textView2.setTextColor(textView2.getTextColors().withAlpha(alpha));
    		textView3.setTextColor(textView3.getTextColors().withAlpha(alpha));
    		idTextView.setTextColor(idTextView.getTextColors().withAlpha(alpha));    		
    		scoreTextView.setTextColor(scoreTextView.getTextColors().withAlpha(alpha));    		
    		
    		return ret;
    	}
	}
}
