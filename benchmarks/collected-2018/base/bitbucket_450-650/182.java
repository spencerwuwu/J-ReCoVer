// https://searchcode.com/api/result/102641176/

package au.edu.nsw.schools.normanhurb_h.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class LoginDialog extends DialogFragment {
	LoginDialog mContext = this;
	private static final String SERVER_URL = "http://timetable.ravrahn.net/";
	private static final String TIMETABLE_URL = SERVER_URL + "api/";
	private static final String STUDENTS_URL = SERVER_URL + "students/";
	private static final String TEACHERS_URL = SERVER_URL + "teachers/";
	private static final String STUDENT_CODE = "613354";
	private static final String TEACHER_CODE = "528491";
	ProgressDialog timetableDownloadProgress;
	ProgressDialog isValidProgress;
	int errorInt = 0;
	String studentSuffix = "@education.nsw.gov.au";
	String teacherSuffix = "@det.nsw.edu.au";
	boolean setToStudent = true;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		Bundle args = getArguments();
		boolean initialLogin = args.getBoolean("initialLogin", true);
		if (initialLogin) {
			builder.setTitle("Log In");
		} else {
			builder.setTitle("Change User");
		}

		LinearLayout ll = new LinearLayout(getActivity());
		ll.setPadding(dp(8), dp(4), dp(8), dp(4));
		ll.setOrientation(LinearLayout.VERTICAL);

		LinearLayout emailLl = new LinearLayout(getActivity());

		// Add the text box for the DET email
		final EditText userName = new EditText(getActivity());
		userName.setHint("DET Email");
		userName.setTextColor(0xFF000000);
		int emailType = InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
		userName.setInputType(emailType);
		userName.setTextSize(14);
		emailLl.addView(userName);

		final TextView emailSuffix = new TextView(getActivity());
		emailSuffix.setText(studentSuffix);
		if (android.os.Build.VERSION.SDK_INT < 11) {
			emailSuffix.setTextColor(0xFFFFFFFF);
		}
		emailLl.addView(emailSuffix);

		ll.addView(emailLl);

		// Add the spinner for choosing student or teacher
		Spinner studentTeacher = new Spinner(getActivity());
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				getActivity(), R.array.student_teacher,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		studentTeacher.setAdapter(adapter);
		studentTeacher.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				switch (pos) {
				case 0: // Student
					emailSuffix.setText(studentSuffix);
					setToStudent = true;
					break;
				case 1: // Teacher
					emailSuffix.setText(teacherSuffix);
					setToStudent = false;
				}
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}

		});
		ll.addView(studentTeacher);

		// Add the text box for the validation code
		final EditText validate = new EditText(getActivity());
		validate.setHint("Validation Code");
		validate.setInputType(InputType.TYPE_CLASS_NUMBER);
		ll.addView(validate);

		// Add the button to get the timetable, which starts the IsValidChecker
		// when pressed
		Button getTimetable = new Button(getActivity());
		getTimetable.setTag(userName);
		getTimetable.setText("Get Timetable");
		getTimetable.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				String userNameText = userName.getText().toString();
				String validationCode = validate.getText().toString();

				IsValidChecker task = new IsValidChecker();
				task.execute(userNameText, validationCode);
			}

		});
		ll.addView(getTimetable);

		builder.setView(ll);

		return builder.create();
	}

	/**
	 * Checks if a username and validation code is valid, and if it is, runs
	 * GetDataThreaded for the username
	 * 
	 * @author ravrahn
	 * 
	 */
	public class IsValidChecker extends AsyncTask<String, Void, Boolean> {
		String userName;
		String validationCode;
		boolean codeCorrect = false;
		boolean userNameValid = false;

		/**
		 * Runs before execution of doInBackground() in the UI thread
		 */
		@Override
		protected void onPreExecute() {
			// Show a progress dialog to allow user to know what is happening
			isValidProgress = ProgressDialog.show(getActivity(), "",
					"Checking your details...", true);
			
			lockOrientation();
		}

		/**
		 * Runs in a worker thread to reduce lag on UI thread
		 */
		@Override
		protected Boolean doInBackground(String... params) {
			userName = params[0];
			validationCode = params[1];
			String[] userNameList;
			String code; // The code to check against

			// Get list of either students or teachers, depending on what is set
			userNameList = getUserList(setToStudent);
			

			// Use appropriate validation codes for teacher and student
			code = getValidationCode(setToStudent);

			if (code.equals(validationCode)) {
				codeCorrect = true;
			} else {
				return false;
			}

			// Check if the userName is in the userNameList
			if (!userName.equals("")) {
				for (int i = 0; i < userNameList.length; i++) {
					if (userNameList[i].equals(userName)) {
						userNameValid = true;
					}
				}
			}

			if (userNameValid && codeCorrect) {
				return true;
			} else {
				return false;
			}
		}

		/**
		 * Runs on completion of doInBackground() in the UI thread
		 */
		@Override
		protected void onPostExecute(Boolean result) {
			// Close progress dialog opened in onPreExecute()
			isValidProgress.dismiss();

			switch (errorInt) {
			// If there's an error, notify the user through a toast notification
			// and exit the app
			case 1:
				Toast.makeText(getActivity(),
						"Download failed - try again later", Toast.LENGTH_SHORT)
						.show();
				getActivity().finish();
				break;
			case 2:
				Toast.makeText(getActivity(), "Download failed",
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
				break;
			case 3:
				Toast.makeText(getActivity(),
						"Download failed - check your internet connection",
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
				break;
			default: // If there's no error
				// If username and code are valid, get the data, otherwise, tell
				// the user what is not valid
				if (result) {
					new GetDataThreaded().execute(userName);
				} else {
					unlockOrientation();
					if (!codeCorrect) {
						Toast.makeText(getActivity(), "Invalid Code",
								Toast.LENGTH_SHORT).show();
					}
					if (!userNameValid) {
						Toast.makeText(getActivity(), "Invalid Email",
								Toast.LENGTH_SHORT).show();
					}
				}

			}

		}

	}

	private class GetDataThreaded extends AsyncTask<String, Void, Integer> {

		@Override
		protected void onPreExecute() {
			// Show a progress dialog to let the user know what is happening
			timetableDownloadProgress = ProgressDialog.show(getActivity(), "",
					"Downloading your timetable...", true);
			lockOrientation();
		}

		@Override
		protected Integer doInBackground(String... userNameArray) {
			String userName = userNameArray[0];

			getData(userName);

			// Save the human-readable name to the SharedPreferences
			SharedPreferences preferences = PreferenceManager
					.getDefaultSharedPreferences(mContext.getActivity());
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(PreferenceStore.USERNAME,
					getReadableName(userName));
			editor.commit();

			return 1;
		}

		@Override
		protected void onPostExecute(Integer result) {
			unlockOrientation();
			switch (errorInt) {
			// If there's an error, notify the user through a toast notification
			// and exit the app
			case 1:
				Toast.makeText(getActivity(),
						"Download failed - try again later", Toast.LENGTH_SHORT)
						.show();
				getActivity().finish();
				break;
			case 2:
				Toast.makeText(getActivity(), "Download failed",
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
				break;
			case 3:
				Toast.makeText(getActivity(),
						"Download failed - check your internet connection",
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
				break;
			default: // If there's no error
				// Start the timetable activity
				Intent toTabs = new Intent(mContext.getActivity(),
						TabController.class);
				startActivity(toTabs);

				// Exit the dialog and activity
				((DialogFragment) mContext).dismiss();
				mContext.getActivity().finish();
				timetableDownloadProgress.dismiss();

			}

		}

	}

	/**
	 * Simple method that returns the validation code for either teacher or
	 * student
	 * 
	 * @param student
	 *            Whether to get student code or teacher code
	 * @return The requested code, as a string
	 */
	private String getValidationCode(boolean student) {
		String code = null;
		if (student) {
			code = STUDENT_CODE;
		} else {
			code = TEACHER_CODE;
		}
		return code;
	}

	/**
	 * Gets a list of users, either students or teachers, in a stringArray
	 * 
	 * @param getStudents
	 *            If true, get students, if false, get teachers
	 * @return A string array of usernames
	 */
	private String[] getUserList(boolean getStudents) {
		String[] userList = null;
		String url;
		if (getStudents) {
			url = STUDENTS_URL;
		} else {
			url = TEACHERS_URL;
		}
		try {
			JSONArray studentArray = new JSONArray(getStringFromWeb(url));
			userList = new String[studentArray.length()];
			for (int i = 0; i < studentArray.length(); i++) {
				JSONObject user = studentArray.getJSONObject(i);
				userList[i] = user.getString("username");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (userList == null) {
			userList = new String[1];
			userList[0] = "";
		}
		
		return userList;
	}

	/**
	 * Get the human-readable name from the database The username is known to be
	 * valid, so no check is needed
	 * 
	 * @param userName
	 *            The DET ID for the user
	 * @return The first and last names of the user
	 */
	private String getReadableName(String userName) {
		String name = "";

		String listFromWeb;

		if (setToStudent) {
			listFromWeb = getStringFromWeb(STUDENTS_URL);
		} else {
			listFromWeb = getStringFromWeb(TEACHERS_URL);
		}

		try {
			JSONArray studentArray = new JSONArray(listFromWeb);
			for (int i = 0; i < studentArray.length(); i++) {
				JSONObject student = studentArray.getJSONObject(i);
				if (student.getString("username").equals(userName)) {
					name = student.getString("first_name") + " "
							+ student.getString("last_name");
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return name;

	}

	/**
	 * Gets the timetable data and puts it in the database
	 * 
	 * @param userName
	 *            The username to get the data for
	 */
	private void getData(String userName) {

		// Wipe all data - only one timetable is wanted.
		// Backup not required, all data originally came from the cloud
		ContentResolver cr = getActivity().getContentResolver();
		cr.delete(TimetableProvider.PERIODS_URI, null, null);

		// Perform a HTTP GET for the timetable data
		String mTimetableData = getStringFromWeb(TIMETABLE_URL + userName);
		try {
			// Convert raw String into a JSON Array
			JSONArray jsonArray = new JSONArray(mTimetableData);

			// Put all the data into the database, period by period
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				getTimetableFields(jsonObject).putInDb(getActivity());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Parses a JSON object into a Period object
	 * 
	 * @param jsonObject
	 *            The JSON object to parse
	 * @return A Period object parsed from the JSON object
	 */
	public Period getTimetableFields(JSONObject jsonObject) {
		Period period = new Period();

		try {
			period.setId(jsonObject.getJSONObject("timeslot").getString("name"));

			String day = jsonObject.getJSONObject("timeslot").getString("day");

			// Translate the day into an integer
			if (day.equals("M")) {
				period.setDayInt(0);
			} else if (day.equals("T")) {
				period.setDayInt(1);
			} else if (day.equals("W")) {
				period.setDayInt(2);
			} else if (day.equals("Th")) {
				period.setDayInt(3);
			} else if (day.equals("F")) {
				period.setDayInt(4);
			}

			period.setName(jsonObject.getJSONObject("subject")
					.getString("name"));

			period.setRoom(jsonObject.getString("room"));

			period.setTeacher(jsonObject.getString("teacher"));

			period.setTime(parseTimeString(jsonObject.getJSONObject("timeslot")
					.getString("start")), parseTimeString(jsonObject
					.getJSONObject("timeslot").getString("end")));

			period.setBreak(jsonObject.getJSONObject("subject").getBoolean(
					"isbreak"));

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return period;
	}

	/**
	 * Performs a HTTP GET on the timetable server to get a String. Must be
	 * executed in a separate thread.
	 * 
	 * @param url
	 *            The URL to perform a GET on
	 * @return The string retrieved from the URL, or null
	 */
	public String getStringFromWeb(String url) {

		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				Log.e(LoginDialog.class.toString(), "Failed to download file");
				errorInt = 1; // So user can be notified in the UI thread

			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			errorInt = 2; // So the user can be notified in the UI thread

		} catch (IOException e) {
			e.printStackTrace();
			errorInt = 3; // So the user can be notified in the UI thread
		}
		return builder.toString();
	}

	/**
	 * Translates a django string into the integer format the SQLite database
	 * uses (minutes from midnight)
	 * 
	 * @param timeString
	 *            The time as a django string
	 * @return The time as an integer in minutes from midnight
	 */
	public int parseTimeString(String timeString) {
		String[] timeStringSplit = timeString.split(":");
		int hours = Integer.parseInt(timeStringSplit[0]);
		int minutes = Integer.parseInt(timeStringSplit[1]);
		int time = hours * 60 + minutes;
		return time;
	}

	/**
	 * Converts density-independent pixels (dp) to absolute pixels, for scaling
	 * functionality
	 * 
	 * @param amount
	 *            The amount in dp
	 * @return The amount in pixels
	 */
	private int dp(int amount) {
		final float scale = getResources().getDisplayMetrics().density;
		final int dp = (int) (amount * scale + 0.5f);
		return dp;
	}

	/**
	 * Prevents the screen from auto-rotating
	 */
	private void lockOrientation() {
		int currentOrientation = getResources().getConfiguration().orientation;
		if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			   getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
			}
			else {
			   getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
			}
	}

	/**
	 * Allow the screen to auto-rotate
	 */
	private void unlockOrientation() {
		getActivity().setRequestedOrientation(
				ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

}

