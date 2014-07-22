package hu.danieleff.android.kanji_quiz;

import hu.danieleff.kanji_quiz_debug.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

public class ActionActivity extends Activity {
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	}
	
	protected boolean[] getTypesChecked(boolean kanjiList) {
		boolean[] ret=new boolean[6];
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		ret[0]=prefs.getBoolean("word_trans1", true);
		ret[1]=prefs.getBoolean("word_trans2", true);
		ret[2]=prefs.getBoolean("trans1_word", true);
		ret[3]=prefs.getBoolean("trans2_word", true);		
		ret[4]=prefs.getBoolean("trans1_trans2", false);
		ret[5]=prefs.getBoolean("trans2_trans1", false);
		if (!kanjiList) {
			ret[0]=prefs.getBoolean("trans1_word_kana", true);
			ret[2]=prefs.getBoolean("word_trans1_kana", true);
		}
		/*for(int i=0;i<6;i++) {
			Log.e("", ""+i+" "+ret[i]);
		}*/
		return ret;
	}
	
	public void setTitle(String title) {
		TextView titleView = (TextView) findViewById(R.id.titleTextView);
		titleView.setText(title);
	}
	
	public void setProgress(Quiz quiz, boolean[] typeschecked) {
		TextView title2View = (TextView) findViewById(R.id.title2TextView);
		setProgress(title2View, quiz, typeschecked);
	}
	
	public void setProgress(TextView textView, Quiz quiz, boolean[] typesChecked) {
		if (quiz==null) {
			textView.setText("");
		} else {
			int total=0;
			int progress=0;
			for (Word w : quiz.words) {
				total+=w.getMaxScore(typesChecked);
				progress+=w.getTotalScore(typesChecked);
			}
			textView.setText(progress+" / "+total);
		}
	}
	
	public void onHelp(View view) {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Help");
		String message="";
		
		message+="<b>Simple Kanji Quiz</b>";
		message+="<p><b>Simple:</b> Only a few kana-readings and english-meanings for each Kanji.</p>";
		message+="<p><b>Kanji:</b> Actually contains Hiragana/Katakana too, but mainly JLPT N5-N1, and Grade 1-6 Kanji.</p>";
		message+="<p><b>Quiz:</b> Contains only continuous multi-choice quiz per Kanji list.</p>";
		message+="<p>You can set the question types in the <font color='red'>settings menu</font></p>";
		message+="<p>You can reduce the word count by <font color='red'>long clicking</font> on the list on main page</p>";
		message+="<p>Scoring is used to track successful answers, words with 10/10 score will not be asked again, " +
				"new words will be added to the questions instead.</p>";
		
		builder.setMessage(Html.fromHtml(message));
		builder.setPositiveButton(android.R.string.ok, null);
		builder.show();
	}
	
	public void onSettings(View view) {
		startActivity(new Intent(this, PrefsActivity.class));
	}
	
}
