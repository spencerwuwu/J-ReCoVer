// https://searchcode.com/api/result/76047337/

package in.co.praveenkumar.iitblit;

import in.co.praveenkumar.iitblit.networking.QuesDownloader;
import in.co.praveenkumar.iitblit.networking.ScoresDownloader;
import in.co.praveenkumar.iitblit.networking.SumbitAnswers;
import in.co.praveenkumar.iitblit.tools.JsonDecoder;
import in.co.praveenkumar.iitblit.tools.StringList;
import in.co.praveenkumar.iitblit.tools.scoreSorter;
import in.co.praveenkumar.litiitb.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuizzingActivity extends FragmentActivity {
	public static View[] sectionRootView = new View[5];
	public static LinearLayout progMsgLL;
	public static TextView progMsgTV;
	private final String DEBUG_TAG = "IITBLit.MainActivity";
	public static Database db;
	public final static String COLOR_RED = "#FF0000";
	public final static String COLOR_GREEN = "#12a962";
	public final static String COLOR_YELLOW = "#55FFF200";
	public final static String COLOR_BLUE = "#43b1d6";
	private static Button submitBtn;
	private static Context context;

	// For answer color evaluation on submit
	private static Boolean[][] response = new Boolean[4][4];

	// For questions availability evaluation. Not all questions in each section
	// will be there. So, accordingly adjust visibility per section.
	private static Boolean[][] qStatus = new Boolean[4][4];

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.quiz_main);
		context = this;

		// For answers temporary storage
		db = new Database(getApplicationContext());

		// Set current question count from db before views setup
		getQsCountToBoolArray();

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// For caching more than just 2 pages
		mViewPager.setOffscreenPageLimit(5);

		// Set progress message views
		progMsgLL = (LinearLayout) findViewById(R.id.progress_msg_layout);
		progMsgTV = (TextView) findViewById(R.id.progress_msg);

		// Update with already existing questions on device
		UIupdater.questionsUIUpdate();
		UIupdater.scoresUIUpdate();

		// Start a background thread that downloads files in the bg
		// The newly obtained will be pushed to UI thread after each image
		// download
		syncWithServer syncer = new syncWithServer();
		syncer.execute();

		// Setting up onClickListeners....
		Button refreshButton = (Button) findViewById(R.id.refresh_button);
		refreshButton.setOnClickListener(refreshButtonListener);

		submitBtn = (Button) findViewById(R.id.submit_button);
		submitBtn.setOnClickListener(submitButtonListener);

	}

	// To avoid going back to landing page. Open Launcher. App in bg.
	@Override
	public void onBackPressed() {
		Intent l = new Intent(this, MainActivity.class);
		// To close app when finish() is called in the main activity
		l.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		l.putExtra("EXIT", true);
		startActivityForResult(l, 11);
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_about:
			Intent j = new Intent(this, MenuClickHandler.class);
			j.putExtra("menu_item", 1);
			startActivityForResult(j, 11);
			break;
		case R.id.menu_help:
			Intent k = new Intent(this, MenuClickHandler.class);
			k.putExtra("menu_item", 2);
			startActivityForResult(k, 11);
			break;
		case R.id.menu_tutorial:
			Intent l = new Intent(this, Tutorial.class);
			startActivityForResult(l, 11);
			break;
		}
		return true;
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = new DummySectionFragment();
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			// Show 5 total pages.
			return 5;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);
			case 3:
				return getString(R.string.title_section4).toUpperCase(l);
			case 4:
				return getString(R.string.title_section5).toUpperCase(l);
			}
			return null;
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private final String DEBUG_TAG = "IITBLit.DummySectionFragment";
		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			int sectionNum = getArguments().getInt(ARG_SECTION_NUMBER);
			View quizView = inflater.inflate(R.layout.quiz_section, container,
					false);
			View fHighScoresView = inflater.inflate(R.layout.final_high_scores,
					container, false);
			View rootView = quizView;
			switch (sectionNum) {
			case 1:
				Log.d(DEBUG_TAG, "Creating section 1");
				sectionRootView[0] = rootView;
				UIupdater.questionsUIUpdate();
				UIupdater.scoresUIUpdate();
				UIupdater.ansUIUpdate(response);
				setUpAnswerFields();
				UIupdater.quesVisibUpdate();
				UIupdater.setSubmitBtn(2);
				break;
			case 2:
				Log.d(DEBUG_TAG, "Creating section 2");
				sectionRootView[1] = rootView;
				UIupdater.questionsUIUpdate();
				UIupdater.scoresUIUpdate();
				UIupdater.ansUIUpdate(response);
				setUpAnswerFields();
				UIupdater.quesVisibUpdate();
				UIupdater.setSubmitBtn(2);
				break;
			case 3:
				Log.d(DEBUG_TAG, "Creating section 3");
				sectionRootView[2] = rootView;
				UIupdater.questionsUIUpdate();
				UIupdater.scoresUIUpdate();
				UIupdater.ansUIUpdate(response);
				setUpAnswerFields();
				UIupdater.quesVisibUpdate();
				UIupdater.setSubmitBtn(2);
				break;
			case 4:
				Log.d(DEBUG_TAG, "Creating section 4");
				sectionRootView[3] = rootView;
				UIupdater.questionsUIUpdate();
				UIupdater.scoresUIUpdate();
				UIupdater.ansUIUpdate(response);
				setUpAnswerFields();
				UIupdater.quesVisibUpdate();
				UIupdater.setSubmitBtn(2);
				break;
			case 5:
				Log.d(DEBUG_TAG, "Creating section 5");
				rootView = fHighScoresView;
				sectionRootView[4] = rootView;
				UIupdater.scoresUIUpdate();
				UIupdater.setSubmitBtn(2);
				break;
			}
			return rootView;
		}
	}

	public static class addListenerOnTextChange implements TextWatcher {
		// private Context mContext;
		EditText mEdittextview;

		public addListenerOnTextChange(EditText edittextview) {
			super();
			// this.mContext = context;
			this.mEdittextview = edittextview;
		}

		@Override
		public void afterTextChanged(Editable s) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// Get data for all fields and store in settings.
			// It's redundant to check for all fields though
			for (int catNum = 0; catNum < 4; catNum++) {
				if (sectionRootView[catNum] != null) {
					EditText ans1View = (EditText) sectionRootView[catNum]
							.findViewById(R.id.ans1);
					EditText ans2View = (EditText) sectionRootView[catNum]
							.findViewById(R.id.ans2);
					EditText ans3View = (EditText) sectionRootView[catNum]
							.findViewById(R.id.ans3);
					EditText ans4View = (EditText) sectionRootView[catNum]
							.findViewById(R.id.ans4);

					// Save answers in local db
					if (!ans1View.getText().toString().contentEquals(""))
						db.saveAnswer("c" + catNum + "q0", ans1View.getText()
								.toString());
					if (!ans2View.getText().toString().contentEquals(""))
						db.saveAnswer("c" + catNum + "q1", ans2View.getText()
								.toString());
					if (!ans3View.getText().toString().contentEquals(""))
						db.saveAnswer("c" + catNum + "q2", ans3View.getText()
								.toString());
					if (!ans4View.getText().toString().contentEquals(""))
						db.saveAnswer("c" + catNum + "q3", ans4View.getText()
								.toString());

				}
			}
			// End of method
		}
		// End of class
	}

	public static class UIupdater {
		private final static String DEBUG_TAG = "IITBLit.UIUpdater";

		public UIupdater() {
			// Constructor does nothing
			Log.d(DEBUG_TAG, "UIupdater initiated!");
		}

		public static void questionsUIUpdate() {
			for (int catNum = 0; catNum < 4; catNum++) {
				for (int quesNum = 0; quesNum < 4; quesNum++) {
					final File imgFile = new File(
							Environment.getExternalStorageDirectory(),
							"/IITBLit/" + "Cat" + catNum + "Ques" + quesNum
									+ ".jpg");
					if (imgFile.exists() && sectionRootView[catNum] != null) {
						Bitmap myBitmap = decodeImage(imgFile);
						// Log.d(DEBUG_TAG, "Updating UI for Cat : " + catNum
						// + " Ques : " + quesNum);
						switch (quesNum) {
						case 0:
							ImageView ques1View = (ImageView) sectionRootView[catNum]
									.findViewById(R.id.q1ImgView);
							ques1View.setImageBitmap(myBitmap);
							ques1View.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									openImage(imgFile);
								}
							});
							break;
						case 1:
							ImageView ques2View = (ImageView) sectionRootView[catNum]
									.findViewById(R.id.q2ImgView);
							ques2View.setImageBitmap(myBitmap);
							ques2View.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									openImage(imgFile);
								}
							});
							break;
						case 2:
							ImageView ques3View = (ImageView) sectionRootView[catNum]
									.findViewById(R.id.q3ImgView);
							ques3View.setImageBitmap(myBitmap);
							ques3View.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									openImage(imgFile);
								}
							});
							break;
						case 3:
							ImageView ques4View = (ImageView) sectionRootView[catNum]
									.findViewById(R.id.q4ImgView);
							ques4View.setImageBitmap(myBitmap);
							ques4View.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									openImage(imgFile);
								}
							});
							break;

						}
					}
				}
			} // End of loop code
		}

		public static void scoresUIUpdate() {
			ArrayList<StringList> uNames = new ArrayList<StringList>();
			ArrayList<StringList> sScores = new ArrayList<StringList>();
			JsonDecoder jd = new JsonDecoder();

			// Get scores from JsonDecoder
			uNames = jd.getUnames();
			sScores = jd.getSscores();

			if (uNames == null || sScores == null)
				return;

			// TextViews for each section
			TextView[] scoreNamesView = new TextView[5];
			TextView[] scoreValuesView = new TextView[5];

			for (int catNum = 0; catNum < 5; catNum++) {
				// Adding StringList per section to ArrayList
				uNames.add(new StringList());
				sScores.add(new StringList());

				// Get score and user name views
				if (sectionRootView[catNum] != null) {
					scoreNamesView[catNum] = (TextView) sectionRootView[catNum]
							.findViewById(R.id.score_names);
					scoreValuesView[catNum] = (TextView) sectionRootView[catNum]
							.findViewById(R.id.score_values);
				}
			}

			// Strings to hold uNames and scores per section as line break
			// separated strings.
			String[] uNamesStrng = new String[5];
			String[] sScoresStrng = new String[5];

			// Sorting scores
			for (int catNum = 0; catNum < 5; catNum++) {
				scoreSorter ss = new scoreSorter(uNames.get(catNum),
						sScores.get(catNum));
				ss.sort();
				uNames.set(catNum, (StringList) ss.getSorteduNames());
				sScores.set(catNum, (StringList) ss.getSortedScores());
			}

			// Building scores and names string per section
			for (int catNum = 0; catNum < 5; catNum++) {
				uNamesStrng[catNum] = "";
				sScoresStrng[catNum] = "";
				for (int j = 0; j < uNames.get(catNum).size(); j++) {
					uNamesStrng[catNum] = uNamesStrng[catNum]
							+ uNames.get(catNum).get(j) + "\n";
					sScoresStrng[catNum] = sScoresStrng[catNum]
							+ sScores.get(catNum).get(j) + "\n";
				}
			}

			// Setting scores and names to views
			for (int catNum = 0; catNum < 5; catNum++) {
				if (sectionRootView[catNum] != null) {
					scoreNamesView[catNum].setText(uNamesStrng[catNum]);
					scoreValuesView[catNum].setText(sScoresStrng[catNum]);
				}
			}

		}

		public static void ansUIUpdate(Boolean resp[][]) {
			// Save evaluated responses for future answer color state
			response = resp;

			// NULL checking is done only for one field.
			// If one is set all will be set.
			if (resp[0][0] != null) {
				EditText[] ans1EV = new EditText[4];
				EditText[] ans2EV = new EditText[4];
				EditText[] ans3EV = new EditText[4];
				EditText[] ans4EV = new EditText[4];

				for (int cat = 0; cat < 4; cat++) {
					if (sectionRootView[cat] != null) {
						ans1EV[cat] = (EditText) sectionRootView[cat]
								.findViewById(R.id.ans1);
						ans2EV[cat] = (EditText) sectionRootView[cat]
								.findViewById(R.id.ans2);
						ans3EV[cat] = (EditText) sectionRootView[cat]
								.findViewById(R.id.ans3);
						ans4EV[cat] = (EditText) sectionRootView[cat]
								.findViewById(R.id.ans4);

						// Setting colors
						if (!resp[cat][0])
							ans1EV[cat].setTextColor(Color
									.parseColor(COLOR_RED));
						else
							ans1EV[cat].setTextColor(Color
									.parseColor(COLOR_GREEN));

						if (!resp[cat][1])
							ans2EV[cat].setTextColor(Color
									.parseColor(COLOR_RED));
						else
							ans2EV[cat].setTextColor(Color
									.parseColor(COLOR_GREEN));

						if (!resp[cat][2])
							ans3EV[cat].setTextColor(Color
									.parseColor(COLOR_RED));
						else
							ans3EV[cat].setTextColor(Color
									.parseColor(COLOR_GREEN));

						if (!resp[cat][3])
							ans4EV[cat].setTextColor(Color
									.parseColor(COLOR_RED));
						else
							ans4EV[cat].setTextColor(Color
									.parseColor(COLOR_GREEN));

					}
				}
			}// End of null ptr checking condition.
		} // End of ansUIUpdate

		public static void quesVisibUpdate() {
			Boolean resp[][] = new Boolean[4][4];
			resp = qStatus;

			// NULL checking is done only for one field.
			// If one is set all will be set.
			if (resp[0][0] != null) {
				LinearLayout[] q1LL = new LinearLayout[4];
				LinearLayout[] q2LL = new LinearLayout[4];
				LinearLayout[] q3LL = new LinearLayout[4];
				LinearLayout[] q4LL = new LinearLayout[4];

				for (int cat = 0; cat < 4; cat++) {
					if (sectionRootView[cat] != null) {
						q1LL[cat] = (LinearLayout) sectionRootView[cat]
								.findViewById(R.id.ques1Card);
						q2LL[cat] = (LinearLayout) sectionRootView[cat]
								.findViewById(R.id.ques2Card);
						q3LL[cat] = (LinearLayout) sectionRootView[cat]
								.findViewById(R.id.ques3Card);
						q4LL[cat] = (LinearLayout) sectionRootView[cat]
								.findViewById(R.id.ques4Card);

						// Setting Visibility
						if (!resp[cat][0])
							q1LL[cat].setVisibility(LinearLayout.GONE);
						else
							q1LL[cat].setVisibility(LinearLayout.VISIBLE);

						if (!resp[cat][1])
							q2LL[cat].setVisibility(LinearLayout.GONE);
						else
							q2LL[cat].setVisibility(LinearLayout.VISIBLE);

						if (!resp[cat][2])
							q3LL[cat].setVisibility(LinearLayout.GONE);
						else
							q3LL[cat].setVisibility(LinearLayout.VISIBLE);

						if (!resp[cat][3])
							q4LL[cat].setVisibility(LinearLayout.GONE);
						else
							q4LL[cat].setVisibility(LinearLayout.VISIBLE);
					}
				}

			}// End of null ptr checking
		}// End of quesVisibUpdate

		public static void setSubmitBtn(int val) {
			// 0 = Success; 1 = fail; 2 = reset
			switch (val) {
			case 0:
				submitBtn.setText("Success ! Resubmit ?");
				submitBtn.setClickable(true);
				break;

			case 1:
				submitBtn.setText("Failed ! Retry ?");
				submitBtn.setClickable(true);
				break;
			case 2:
				if (!submitBtn.getText().toString()
						.contentEquals("Submitting..")) {
					submitBtn.setText("Submit");
					submitBtn.setClickable(true);
				}
				break;
			}

		}
	} // End of UI updation class

	// This class does all the network activity
	private class syncWithServer extends AsyncTask<Integer, Integer, Boolean> {
		Boolean qDownStatus = false;
		Boolean sDownStatus = false;
		Boolean qAvailDownStatus = false;
		int successCount = 0;

		@Override
		protected Boolean doInBackground(Integer... quesParam) {
			Log.d(DEBUG_TAG, "Async thread started");

			// New questions downloader
			QuesDownloader qDowner = new QuesDownloader();

			// Questions availability
			publishProgress(0);
			qAvailDownStatus = qDowner.downQStatus();
			if (qDowner.getAvailQStatusStrng() != null) {
				qStatus = qDowner.getAvailQStatus();
				db.saveAnswer("quesCount", qDowner.getAvailQStatusStrng());
			}

			// Download questions
			publishProgress(1);
			qDownStatus = qDowner.downQues();

			// Download scores
			publishProgress(2);
			ScoresDownloader sDowner = new ScoresDownloader();
			sDownStatus = sDowner.downloadScores();

			publishProgress(3);
			return true;
		}

		// For publishing progress from outside this class
		// public void doProgress(int value) {
		// publishProgress(value);
		// }

		protected void onProgressUpdate(Integer... progress) {

			switch (progress[0]) {
			case 0:
				progMsgLL.setVisibility(LinearLayout.VISIBLE);
				progMsgTV.setText("Fetching questions count...");
				progMsgLL.setBackgroundColor(Color.parseColor(COLOR_YELLOW));
				break;
			case 1:
				if (qAvailDownStatus) {
					progMsgLL.setBackgroundColor(Color.parseColor(COLOR_GREEN));
					progMsgTV.setText("Success !");
					successCount++;
				} else {
					progMsgLL.setBackgroundColor(Color.parseColor(COLOR_RED));
					progMsgTV.setText("Failed !");
				}
				progMsgTV.setText("Fetching questions...");
				progMsgLL.setBackgroundColor(Color.parseColor(COLOR_YELLOW));
				break;
			case 2:
				if (qDownStatus) {
					progMsgLL.setBackgroundColor(Color.parseColor(COLOR_GREEN));
					progMsgTV.setText("Success !");
					successCount++;
				} else {
					progMsgLL.setBackgroundColor(Color.parseColor(COLOR_RED));
					progMsgTV.setText("Failed !");
				}
				progMsgTV.setText("Fetching scores...");
				progMsgLL.setBackgroundColor(Color.parseColor(COLOR_YELLOW));
				break;
			case 3:
				if (sDownStatus) {
					progMsgLL.setBackgroundColor(Color.parseColor(COLOR_GREEN));
					progMsgTV.setText("Success !");
					successCount++;
				} else {
					progMsgLL.setBackgroundColor(Color.parseColor(COLOR_RED));
					progMsgTV.setText("Failed !");
				}
				progMsgTV.setText(successCount + " Hit. " + (3 - successCount)
						+ " Fail !");
				progMsgLL.setBackgroundColor(Color.parseColor(COLOR_BLUE));
				break;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			Log.d(DEBUG_TAG, "UI update requested");
			if (qAvailDownStatus)
				UIupdater.quesVisibUpdate();

			if (qDownStatus)
				UIupdater.questionsUIUpdate();
			else
				System.out.println("Failed to update questions");

			if (sDownStatus)
				UIupdater.scoresUIUpdate();
			else
				System.out.println("Failed to update scores");

			progMsgLL.setVisibility(LinearLayout.GONE);
		}
	}

	// Onclick listeners
	private OnClickListener refreshButtonListener = new OnClickListener() {
		public void onClick(View v) {
			syncWithServer syncer = new syncWithServer();
			syncer.execute();
		}
	};

	private OnClickListener submitButtonListener = new OnClickListener() {
		public void onClick(View v) {
			submitBtn.setText("Submitting..");
			submitBtn.setClickable(false);
			String[][] answers = new String[4][4];
			for (int cat = 0; cat < 4; cat++) {
				for (int q = 0; q < 4; q++) {
					answers[cat][q] = db.getAnswer("c" + cat + "q" + q);
				}
			}

			// Submit to a webpage now
			SumbitAnswers sa = new SumbitAnswers(answers, db.getLDAP());
			sa.sync();
		}
	};

	// OnTextChange Listeners for each EditText. This can't be set for all
	// EditTexts untill all sectionRootViews are
	// available Also, they could be removed without notice on changing
	// sections.
	// So, this is set along with section settings. Any redrawing of sections
	// resets these
	public static void setUpAnswerFields() {
		for (int catNum = 0; catNum < 4; catNum++) {
			if (sectionRootView[catNum] != null) {
				EditText ans1View = (EditText) sectionRootView[catNum]
						.findViewById(R.id.ans1);
				EditText ans2View = (EditText) sectionRootView[catNum]
						.findViewById(R.id.ans2);
				EditText ans3View = (EditText) sectionRootView[catNum]
						.findViewById(R.id.ans3);
				EditText ans4View = (EditText) sectionRootView[catNum]
						.findViewById(R.id.ans4);

				// Set values to values from db
				if (!db.getAnswer("c" + catNum + "q0").contentEquals(""))
					ans1View.setText(db.getAnswer("c" + catNum + "q0"));
				if (!db.getAnswer("c" + catNum + "q1").contentEquals(""))
					ans2View.setText(db.getAnswer("c" + catNum + "q1"));
				if (!db.getAnswer("c" + catNum + "q2").contentEquals(""))
					ans3View.setText(db.getAnswer("c" + catNum + "q2"));
				if (!db.getAnswer("c" + catNum + "q3").contentEquals(""))
					ans4View.setText(db.getAnswer("c" + catNum + "q3"));

				// Setting up on change listeners
				ans1View.addTextChangedListener(new addListenerOnTextChange(
						ans1View));
				ans2View.addTextChangedListener(new addListenerOnTextChange(
						ans2View));
				ans3View.addTextChangedListener(new addListenerOnTextChange(
						ans3View));
				ans4View.addTextChangedListener(new addListenerOnTextChange(
						ans4View));

			}
		}
	}

	// For offline qs count
	private void getQsCountToBoolArray() {
		String nQStrng = db.getAnswer("quesCount");
		if (!nQStrng.contentEquals("") && nQStrng.length() == 16) {
			char[] resp = nQStrng.toCharArray();
			for (int c = 0; c < 4; c++) {
				for (int q = 0; q < 4; q++) {
					if (resp[c * 4 + q] == '1')
						qStatus[c][q] = true;
					else
						qStatus[c][q] = false;
				}
			}
		}
	}// End of function

	// decodes image and scales it to reduce memory consumption
	private static Bitmap decodeImage(File f) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// The new size we want to scale to
			final int REQUIRED_SIZE = 70;

			// Find the correct scale value. It should be the power of 2.
			int scale = 1;
			while (o.outWidth / scale / 2 >= REQUIRED_SIZE
					&& o.outHeight / scale / 2 >= REQUIRED_SIZE)
				scale *= 2;

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	private static void openImage(File f) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse("file://" + f.getAbsolutePath()),
				"image/*");
		context.startActivity(intent);
	}

}

