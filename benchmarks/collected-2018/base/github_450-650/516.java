// https://searchcode.com/api/result/111079874/

package fm.smart.r1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.nullwire.trace.ExceptionHandler;

import fm.smart.Lookup;
import fm.smart.Result;
import fm.smart.Transport;
import fm.smart.Utils;
import fm.smart.r1.provider.SearchSuggestionSampleProvider;

public class Main extends Activity implements View.OnClickListener {

	public static final String API_KEY = "7pvmc285fxnexgwhbfddzkjn";
	public static String default_study_goal_id = "279542";

	public static String search_lang = "ja";
	public static String result_lang = "en";

	private SmartFmMenus menus;
	private String query_string = null;
	public static ProgressDialog myProgressDialog;
	final private int VOICE_RECOGNITION_REQUEST_CODE = 0;
	public static List<String> languages = null;
	// not being persisted have to think carefully about that ...
	public static LinkedList<LanguagePair> recent_lang_pairs_stack = new LinkedList<LanguagePair>();
	static final String PREF_REF = "SMART_FM_PREFERENCES";
	static Lookup lookup = null;
	static final Transport transport = new AndroidTransport();

	public static void showErrorDialog(final String dialog_message,
			final Activity context) {
		((Activity) context).runOnUiThread(new Thread() {
			public void run() {
				final AlertDialog dialog = new AlertDialog.Builder(context)
						.create();
				dialog.setTitle("Network Failure");
				dialog.setMessage(dialog_message);
				dialog.setButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

					}
				});
				// TODO suggest to user to upload new sound?
				dialog.show();
			}
		});
	}

	private void persistRecentLanguagePairs() {
		SharedPreferences.Editor editor = getSharedPreferences(PREF_REF, 0)
				.edit();
		editor.putString("search_lang_1",
				recent_lang_pairs_stack.get(0).search_language_code);
		editor.putString("result_lang_1",
				recent_lang_pairs_stack.get(0).result_language_code);
		editor.putString("search_lang_2",
				recent_lang_pairs_stack.get(1).search_language_code);
		editor.putString("result_lang_2",
				recent_lang_pairs_stack.get(1).result_language_code);
		editor.putString("search_lang_3",
				recent_lang_pairs_stack.get(2).search_language_code);
		editor.putString("result_lang_3",
				recent_lang_pairs_stack.get(2).result_language_code);
		editor.commit();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("ACTIVITY-DEBUG", "onCreate");
		
		lookup = new Lookup(API_KEY, LoginActivity.username(this),
				LoginActivity.password(this));
		if (!LoginActivity.isNotLoggedIn(this)) {
			ItemListActivity.getDefaultStudyList(this);
		}
		ExceptionHandler.register(this);
		menus = new SmartFmMenus(this);
		this.setContentView(R.layout.main);

		final Intent queryIntent = getIntent();
		final String queryAction = queryIntent.getAction();
		Bundle extras = queryIntent.getExtras();
		String goalType = null;
		if (extras != null) {
			goalType = (String) extras.get("goal_type");
			query_string = (String) extras.get("query_string");
			if (!TextUtils.isEmpty(query_string)) {
				query_string = query_string.replaceAll("\\++", " ");
			}
		}
		if (goalType == null) {
			// hansleOauth(queryIntent);
		}
		GoalDownload goal_download = null;
		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			doSearchQuery(queryIntent, "onCreate()");
		}

		else if (goalType != null && goalType.equals("recent_goals")) {
			// Display an indeterminate Progress-Dialog
			myProgressDialog = ProgressDialog.show(Main.this, "Please wait...",
					"Downloading community goals...", true, true);
			if (goal_download == null) {
				goal_download = new GoalDownload(this, myProgressDialog, "") {
					public Result downloadCall() throws Exception {
						return lookup.recentGoals(transport);
					}
				};
			}
			goal_download.start();
		} else if ((goalType != null && goalType.equals("my_study_goals"))) {
			// Display an indeterminate Progress-Dialog
			myProgressDialog = ProgressDialog.show(Main.this, "Please wait...",
					"Downloading " + LoginActivity.username(this)
							+ "'s goals...", true, true);
			goal_download = new GoalDownload(this, myProgressDialog, "") {
				public Result downloadCall() throws Exception {
					return lookup.userStudyGoals(transport,
							LoginActivity.username(Main.this));
				}
			};
			goal_download.start();
		} else if ((goalType != null && goalType.equals("my_created_goals"))) {
			// Display an indeterminate Progress-Dialog
			myProgressDialog = ProgressDialog.show(
					Main.this,
					"Please wait...",
					"Downloading goals created by "
							+ LoginActivity.username(this) + "...", true, true);
			goal_download = new GoalDownload(this, myProgressDialog, "") {
				public Result downloadCall() throws Exception {
					return lookup.userGoals(transport,
							LoginActivity.username(Main.this));
				}
			};
			goal_download.start();
		}
		final ImageButton button = (ImageButton) findViewById(R.id.main_submit);
		button.setOnClickListener(this);
		// button.setEnabled(false);
		ImageButton vbutton = (ImageButton) findViewById(R.id.main_voice);
		final Intent intent = new Intent(
				"android.speech.action.RECOGNIZE_SPEECH");
		ComponentName cname = intent.resolveActivity(getPackageManager());
		if (cname != null) {
			Log.d("SPEECH-DEBUG", cname.toString());
		} else {
			Log.d("SPEECH-DEBUG", "NULL");
			vbutton.setEnabled(false);
		}
		// TODO some way to test if voice is available and disable
		// android.content.ActivityNotFoundException: No Activity found to
		// handle Intent { action=android.speech.action.RECOGNIZE_SPEECH (has
		// extras) }
		// test for presence of activities?
		vbutton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {

				// Create Intent

				// Settings
				intent.putExtra("android.speech.extra.LANGUAGE_MODEL",
						"free_form");
				intent.putExtra("android.speech.extra.PROMPT", "Speak now");
				button.setEnabled(true);// TODO ensure activate search button
				// .... or search on first match ...
				// Start the Recognition Activity
				startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);

			}
		});

		InputMethodManager inputManager = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
		List<InputMethodInfo> inputMethodInfoList = inputManager
				.getEnabledInputMethodList();
		Vector<String> imes = new Vector<String>();
		for (InputMethodInfo i : inputMethodInfoList) {
			imes.add(i.getServiceName());
			try {
				Log.d("INPUT-DEBUG", i.getServiceName());
				Log.d("INPUT-DEBUG", i.getPackageName());
				Log.d("INPUT-DEBUG", i.getSettingsActivity());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * large number of locales - not apparently related to what user had set
		 * up for languages they care about ... List<Locale> locales = new
		 * ArrayList(Arrays.asList(Locale.getAvailableLocales())); for (Locale l
		 * : locales) { Log.d("LOCALE-DEBUG", l.getCountry());
		 * Log.d("LOCALE-DEBUG", l.getLanguage()); Log.d("LOCALE-DEBUG",
		 * l.getISO3Language()); }
		 */

		final TextView lookup_legend_textView = (TextView) findViewById(R.id.main_lookup);
		final AutoCompleteTextView lookup_textView = (AutoCompleteTextView) findViewById(R.id.lookup);
		lookup_textView.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (v.equals(lookup_textView)) {
					if (!TextUtils.isEmpty(((AutoCompleteTextView) v).getText())) {
						button.setEnabled(true);
					}
					if (event != null) {
						onClick(v);
						Log.d("KEY-DEBUG", "enter key pressed");
						return true;
					}
				}
				return false;
			}
		});
		// lookup_textView.setOnKeyListener(new OnKeyListener() {
		//
		// public boolean onKey(View v, int keyCode, KeyEvent event) {
		// if (v.equals(lookup_textView)) {
		// if (!TextUtils
		// .isEmpty(((AutoCompleteTextView) v).getText())) {
		// button.setEnabled(true);
		// }
		// switch (keyCode) {
		// case KeyEvent.KEYCODE_ENTER:
		// if (event.getAction() == KeyEvent.ACTION_UP) {
		// onClick(v);
		// Log.d("KEY-DEBUG", "enter key pressed");
		// return true;
		// }
		// }
		// }
		// return false;
		// }
		// });
		lookup_textView.setSelectAllOnFocus(true);
		if (!TextUtils.isEmpty(query_string)) {
			lookup_textView.setText(query_string);
			button.setEnabled(true);
		}

		Log.d("LOCALE-DEFAULT-DEBUG", Locale.getDefault().getCountry());
		Log.d("LOCALE-DEFAULT-DEBUG", Locale.getDefault().getLanguage());
		Log.d("LOCALE-DEFAULT-DEBUG", Locale.getDefault().getISO3Language());

		PackageManager pm = getPackageManager();
		/*
		 * PackageInfo pi; try { pi = pm .getPackageInfo("fm.smart.r1",
		 * PackageManager.GET_ACTIVITIES); for (ActivityInfo a : pi.activities)
		 * { Log.d("PACKAGE-DEBUG", a.name); } pi =
		 * pm.getPackageInfo("android.speech", PackageManager.GET_ACTIVITIES);
		 * for (ActivityInfo a : pi.activities) { Log.d("PACKAGE-DEBUG",
		 * a.name); } } catch (NameNotFoundException e) { // TODO Auto-generated
		 * catch block e.printStackTrace(); }
		 */
		// languages = new Vector<String>(Utils.LANGUAGE_MAP.keySet());
		languages = Utils.POPULAR_LANGUAGES;
		Collections.sort(languages);

		ArrayAdapter<String> search_language_adapter = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item, languages);
		final Spinner search_language_spinner = (Spinner) findViewById(R.id.search_language);

		ArrayAdapter<String> result_language_adapter = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item, languages);
		final Spinner result_language_spinner = (Spinner) findViewById(R.id.result_language);

		search_language_spinner.setAdapter(search_language_adapter);
		result_language_spinner.setAdapter(result_language_adapter);

		search_language_spinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						lookup_legend_textView.setText("Search "
								+ languages.get(position) + " or "
								+ result_language_spinner.getSelectedItem()
								+ " words");

					}

					public void onNothingSelected(AdapterView<?> parent) {
					}

				});

		result_language_spinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						lookup_legend_textView.setText("Search "
								+ search_language_spinner.getSelectedItem()
								+ " or " + languages.get(position) + " words");

					}

					public void onNothingSelected(AdapterView<?> parent) {
					}

				});

		search_language_spinner.setSelection(languages
				.indexOf(Utils.INV_LANGUAGE_MAP.get(Main.search_lang)));
		result_language_spinner.setSelection(languages
				.indexOf(Utils.INV_LANGUAGE_MAP.get(Main.result_lang)));

		loadRecentLanguagePairs(imes);
		updateSpinner();

		// TODO
		// switch autocompletes to spinners on main languages we support,
		// and have a change listener update the lookup text
	}

	private void doSearchQuery(final Intent queryIntent, final String entryPoint) {
		menus.doSearchQuery(queryIntent, entryPoint, this);
	}

	public void updateSpinner() {
		final Spinner search_language_spinner = (Spinner) findViewById(R.id.search_language);
		final Spinner result_language_spinner = (Spinner) findViewById(R.id.result_language);

		String search_language = search_language_spinner.getSelectedItem()
				.toString();
		String result_language = result_language_spinner.getSelectedItem()
				.toString();
		String search_language_code = Utils.LANGUAGE_MAP.get(search_language);
		String result_language_code = Utils.LANGUAGE_MAP.get(result_language);
		if (!TextUtils.isEmpty(search_language_code)) {
			Main.search_lang = search_language_code;
		}
		if (!TextUtils.isEmpty(result_language_code)) {
			Main.result_lang = result_language_code;
		}
		LanguagePair pair = new LanguagePair(Main.search_lang, Main.result_lang);
		Log.d("DEBUG_SPINNER", "language pair: " + pair.toString());
		Log.d("DEBUG_SPINNER",
				"language pair: " + recent_lang_pairs_stack.get(0).toString());
		Log.d("DEBUG_SPINNER",
				"language pair: " + recent_lang_pairs_stack.get(1).toString());
		Log.d("DEBUG_SPINNER",
				"language pair: " + recent_lang_pairs_stack.get(2).toString());
		int position = recent_lang_pairs_stack.indexOf(pair);
		Log.d("DEBUG_SPINNER", "position: " + position);
		if (position == -1) {
			Log.d("DEBUG_SPINNER", "removing last pair!!!!");
			recent_lang_pairs_stack.removeLast();
			recent_lang_pairs_stack.addFirst(pair);
		} else {
			Log.d("DEBUG_SPINNER", "removing an existing pair!!!!");
			recent_lang_pairs_stack.remove(pair);
			recent_lang_pairs_stack.addFirst(pair);
		}
		persistRecentLanguagePairs();
		String[] spinner_list = new String[Main.recent_lang_pairs_stack.size()];
		for (int i = 0; i < Main.recent_lang_pairs_stack.size(); i++) {
			spinner_list[i] = Utils.INV_LANGUAGE_MAP
					.get(recent_lang_pairs_stack.get(i).search_language_code)
					+ ", "
					+ Utils.INV_LANGUAGE_MAP
							.get(recent_lang_pairs_stack.get(i).result_language_code);
			Log.d("SPINNER-DEBUG",
					recent_lang_pairs_stack.get(i).search_language_code
							+ ": "
							+ recent_lang_pairs_stack.get(i).result_language_code);
			Log.d("SPINNER-DEBUG", Integer.toString(i) + ": " + spinner_list[i]);
		}

		Spinner s1 = (Spinner) findViewById(R.id.recent_lang_pairs_spinner);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spinner_list);
		adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		s1.setAdapter(adapter);
		s1.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				search_language_spinner.setSelection(languages.indexOf(Utils.INV_LANGUAGE_MAP
						.get(recent_lang_pairs_stack.get(position).search_language_code)));
				result_language_spinner.setSelection(languages.indexOf(Utils.INV_LANGUAGE_MAP
						.get(recent_lang_pairs_stack.get(position).result_language_code)));
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}

		});

	}

	private void loadRecentLanguagePairs(Vector<String> imes) {
		Iterator<String> iter = imes.iterator();
		boolean chinese = false;
		boolean japanese = false;
		String ime = null;
		while (iter.hasNext()) {
			ime = iter.next().toLowerCase();
			if (ime.contains("pinyin") || ime.contains("chinese")) {
				chinese = true;
			}
			if (ime.contains("simeji") || ime.contains("japan")
					|| ime.contains("iwnn") || ime.contains("NTT")
					|| ime.contains("ntt")) {
				japanese = true;
			}
		}
		String default_lang = Locale.getDefault().getLanguage();
		String EN = "en";
		String ES = "es";
		String JA = "ja";
		String ZH = "zh-Hant";

		String[] default_1 = new String[2];
		String[] default_2 = new String[2];
		String[] default_3 = new String[2];

		default_1[0] = (default_lang.equals(EN) ? ES : EN);
		default_1[1] = default_lang;
		default_2[0] = default_lang;
		default_2[1] = (default_lang.equals(EN) ? ES : EN);
		default_3[0] = default_lang;
		default_3[1] = default_lang;

		if (chinese) {
			default_1[0] = (default_lang.equals(ZH) ? EN : ZH);
			default_1[1] = default_lang;
			default_2[0] = default_lang;
			default_2[1] = (default_lang.equals(ZH) ? EN : ZH);
		}
		if (japanese) {
			default_1[0] = (default_lang.equals(JA) ? EN : JA);
			default_1[1] = default_lang;
			default_2[0] = default_lang;
			default_2[1] = (default_lang.equals(JA) ? EN : JA);
		}

		LanguagePair one = new LanguagePair(getSharedPreferences(PREF_REF, 0)
				.getString("search_lang_1", default_1[0]),
				getSharedPreferences(PREF_REF, 0).getString("result_lang_1",
						default_1[1]));
		LanguagePair two = new LanguagePair(getSharedPreferences(PREF_REF, 0)
				.getString("search_lang_2", default_2[0]),
				getSharedPreferences(PREF_REF, 0).getString("result_lang_2",
						default_2[1]));
		LanguagePair three = new LanguagePair(getSharedPreferences(PREF_REF, 0)
				.getString("search_lang_3", default_3[0]),
				getSharedPreferences(PREF_REF, 0).getString("result_lang_3",
						default_3[1]));

		recent_lang_pairs_stack.clear();
		recent_lang_pairs_stack.add(one);
		recent_lang_pairs_stack.add(two);
		recent_lang_pairs_stack.add(three);
	}

	public void onClick(View view) {
		EditText lookupInput = (EditText) findViewById(R.id.lookup);
		String lookup = lookupInput.getText().toString();
		if (!TextUtils.isEmpty(lookup)) {

			updateSpinner();

			// TODO pop this new language search pair onto the language search
			// pair
			// stack
			// would be good to search stack and move item to top if already
			// present
			// in order
			// to avoid list filling up with multiple instances of same item
			// persistence could be an issue - simplest might be having 3/4
			// spaces
			// in preferences ...
			// but conflict there between what user want's to set as preference
			// and
			// what
			// is actually recent

			// TODO should we move language autocompletes over to spinners?

			// autocomplete
			// pros:
			// a) user can type a letter and get suggestions vs having to
			// navigate
			// long list
			//
			// cons
			// a) incorrect entry can lead to failure [i) can catch and ask user
			// to
			// select]
			// b) user can't see list of all possibilities [i) present in other
			// location ii) don't care?]
			// c) inconvenient to delete existing item

			// spinner
			// pros
			// a) user can see all possibilities
			// b) any item selected will necessarily be correct
			//
			// cons
			// a) list may be too long making navigation challenging [i) can put
			// freq at top of list ii) allow keypress to nav list (not supported
			// on
			// android)]
			// b)

			// other issue is for user setting a group of pairs they want as
			// defaults and also about making a list of recent pairs available
			// ...

			// TODO could replace this with making an intent and passing it to
			// SmartFmMenus with appropriate query string extra - would reduce
			// shared
			// code and could call own search method ...
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this, SearchSuggestionSampleProvider.AUTHORITY,
					SearchSuggestionSampleProvider.MODE);
			suggestions.saveRecentQuery(lookup, null);
			SmartFmMenus.loadItems(this, lookup, 1);
		}
	}

	// this forces the menu to be reloaded each time, checking for login status
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.d("DEBUG-MENU", "onPrepareOptionsMenu");
		super.onPrepareOptionsMenu(menu);
		return onCreateOptionsMenu(menu);
	}

	/**
	 * Handle the results from the recognition activity.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE
				&& resultCode == RESULT_OK) {
			// Fill the list view with the strings the recognizer thought it
			// could have heard
			ArrayList<String> matches = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			if (matches != null && matches.size() > 0) {
				Log.d("VOICE-DEBUG", matches.toString());
				ArrayAdapter<String> lookup_voice_adapter = new ArrayAdapter<String>(
						this, android.R.layout.simple_dropdown_item_1line,
						matches);
				AutoCompleteTextView lookup_textView = (AutoCompleteTextView) findViewById(R.id.lookup);
				lookup_textView.setAdapter(lookup_voice_adapter);
				lookup_textView.setText(matches.get(0));
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public void openMenu() {
		this.getWindow().openPanel(Window.FEATURE_OPTIONS_PANEL,
				new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU));
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		menus.onOptionsItemSelected(item, this);
		return super.onOptionsItemSelected(item);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		return menus.onCreateOptionsMenu(menu);
	}

	@Override
	public void onConfigurationChanged(Configuration config) {
		Log.d("ORIENTATION-DEBUG", "onConfigurationChanged");
		// Do nothing, this is to prevent the activity from being restarted when
		// the keyboard opens.
		super.onConfigurationChanged(config);
	}

	private final DialogInterface.OnClickListener mAboutListener = new DialogInterface.OnClickListener() {
		public void onClick(android.content.DialogInterface dialogInterface,
				int i) {
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(getString(R.string.smartfm_url)));
			startActivity(intent);
		}
	};

}
