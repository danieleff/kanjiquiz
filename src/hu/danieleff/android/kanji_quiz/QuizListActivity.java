package hu.danieleff.android.kanji_quiz;

import hu.danieleff.android.kanji_quiz.App.QuizFile;
import hu.danieleff.kanji_quiz_debug.R;

import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class QuizListActivity extends ActionActivity implements OnItemClickListener, OnItemLongClickListener {
    private ListView listView;
    public static final String EXTRA_QUIZFILE_ID = "quiz_file_id";
    public static final String EXTRA_QUIZFILE_START_ID = "quiz_start";
    public static final String EXTRA_QUIZFILE_END_ID = "quiz_end";

    private ArrayAdapter<QuizFile> arrayAdapter;
	private LoadTask loadTask;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.get(this);
        setContentView(R.layout.activity_quiz_list);
        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        arrayAdapter = new ArrayAdapter<QuizFile>(this, android.R.layout.simple_list_item_1) {
        	@Override
        	public int getCount() {
        		return App.files.size();
        	}
        	@Override
        	public QuizFile getItem(int position) {
        		return App.files.get(position);
        	}
        	@Override
        	public boolean areAllItemsEnabled() {
        		return false;
        	}
        	@Override
        	public boolean isEnabled(int position) {
        		return App.files.get(position).quiz!=null;
        	}
        	public View getView(int position, View convertView, android.view.ViewGroup parent) {
        		RelativeLayout ret=(RelativeLayout) convertView;
        		if (ret==null) {
        			ret=(RelativeLayout) getLayoutInflater().inflate(R.layout.quiz_list_row, null);
        		}
        		TextView textView1 = (TextView) ret.findViewById(R.id.textView1);
        		TextView textView2 = (TextView) ret.findViewById(R.id.textView2);
        		QuizFile item = getItem(position);
        		
        		if (item.quiz==null) {
        			if (item.error!=null) {
        				textView1.setText(item.error);
        			} else {
        				textView1.setText("Loading...");
        			}
        		} else {
        			textView1.setText(item.quiz.name+" ("+item.quiz.words.size()+")"/*+" "+item.quiz.words.get(0).word+".."+item.quiz.words.get(item.quiz.words.size()-1).word*/);
        		}
        		if (item.quiz!=null) {
        			setProgress(textView2, item.quiz, getTypesChecked(item.quiz.kanjiList));
        		} else {
        			setProgress(textView2, item.quiz, null);
        		}
        		return ret;
        	};
        };
        listView.setAdapter(arrayAdapter);
        
        loadTask = new LoadTask();
        loadTask.execute();
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	arrayAdapter.notifyDataSetChanged();
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		loadTask.cancel(true);
		App.put();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
		int size = App.files.get(pos).quiz.words.size();
		if (size>200) {
			onItemLongClick(arg0, arg1, pos, arg3);
		} else {
			start(pos, 0, App.files.get(pos).quiz.words.size());			
		}
	}

	private void start(final int pos, int start, int end) {
		Intent intent = new Intent(QuizListActivity.this, WordListActivity.class);
		intent.putExtra(EXTRA_QUIZFILE_ID, pos);
		intent.putExtra(EXTRA_QUIZFILE_START_ID, start);
		intent.putExtra(EXTRA_QUIZFILE_END_ID, end);
		startActivity(intent);
		overridePendingTransition(R.anim.left, R.anim.right);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int pos,
			long arg3) {
		Builder builder = new AlertDialog.Builder(this);
		View view = getLayoutInflater().inflate(R.layout.dialog_input, null);
		View okButton = view.findViewById(R.id.button1);
		View cancelButton = view.findViewById(R.id.button2);
		
		final EditText startEditText = (EditText) view.findViewById(R.id.editText1);
		final EditText endEditText = (EditText) view.findViewById(R.id.editText2);
		
		final Quiz quiz = App.files.get(pos).quiz;
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		startEditText.setText(""+prefs.getInt("start_"+quiz.filename, 1));
		final int max = Math.min(quiz.words.size(), prefs.getInt("end_"+quiz.filename, quiz.words.size()));
		
		endEditText.setText(""+max);
		
		final int realmax=quiz.words.size();
		
		builder.setTitle("Select the word range to view");
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
				int startMax=realmax-20;
				if (start<startMin || start>startMax) {
					Toast.makeText(QuizListActivity.this, "Start must be between "+(startMin+1)+" and "+startMax, Toast.LENGTH_SHORT).show();
					return;
				}
				int endMin=start+20;
				int endMax=realmax;
				if (end<endMin || end>endMax) {
					Toast.makeText(QuizListActivity.this, "End must be between "+endMin+" and "+endMax, Toast.LENGTH_SHORT).show();
					return;
				}
				
				Editor edit = prefs.edit();
				edit.putInt("start_"+quiz.filename, start+1);
				edit.putInt("end_"+quiz.filename, end);
				edit.commit();
				
				start(pos, start, end);
				
				dialog.dismiss();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		
		dialog.show();
		return false;
	}
	
	class LoadTask extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... arg0) {
			for (QuizFile f : App.files) {
				try {
					InputStream in = getAssets().open(f.filename);
					f.quiz = Quiz.load(f.filename, f.hasTrans2, in);
				} catch (IOException e) {
					f.error=e.getMessage();
					e.printStackTrace();
				}
				publishProgress();
				if (isCancelled()) return null;
			}
			return null;
		}
		@Override
		protected void onProgressUpdate(Void... values) {
			arrayAdapter.notifyDataSetChanged();
		}
	}
	
}