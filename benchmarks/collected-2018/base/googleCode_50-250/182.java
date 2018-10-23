// https://searchcode.com/api/result/7299490/

package com.ader.ui;

/**
 * DaisyBookFinder automatically searches for suitable books on the sdcard.
 * 
 * This is a first cut of the implementation as it's slow, uncommunicative,
 * and calls BookValidator that currently assumes the books are in /sdcard/ on
 * the device.
 */
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ader.DaisyBook;
import com.ader.InvalidDaisyStructureException;
import com.ader.OldDaisyBookImplementation;
import com.ader.R;
import com.ader.io.BookValidator;
import com.ader.utilities.DaisyBookUtils;
import com.ader.utilities.Logging;

public class DaisyBookFinder extends ListActivity {
	private List<String> books;
	private static final String TAG = "DaisyBookFinder";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Logging.logInfo(TAG, "onCreate");
        setContentView(R.layout.results_list);
                
        BookValidator validator = new BookValidator();
        String rootfolder = Preferences.getRootfolder(getBaseContext());
		Logging.logInfo(TAG, "The root folder to search is: " + rootfolder);
        validator.findBooks(rootfolder);
        books = validator.getBookList();
        populateList();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String item = books.get(position);
		
		Intent i = new Intent(this, DaisyReader.class);
		i.putExtra("daisyPath", item + "/");
		i.putExtra("daisyNccFile", DaisyBookUtils.getNccFileName(new File(item)));
		startActivity(i);
		return;
	}
	
	void populateList() {
		// TODO(jharty): Check if currentDirectory maps to ExternalStorageDirectory
		Logging.logInfo(TAG, "External Storage is: " + Environment.getExternalStorageDirectory());
		// TODO(jharty): remove this hack once I've debugged the interaction
		// It probably needs to move to a more general FileIO class that'd be
		// used by the rest of the application. That way we can reduce
		// duplication of code e.g. with DaisyBrowser and make the 
		// application more robust against events such as the sdcard becoming
		// unavailable while the application is in use.
		String storagestate = Environment.getExternalStorageState();

		if (!storagestate.equals(Environment.MEDIA_MOUNTED) ) {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle(R.string.sdcard_title);
			alertDialog.setMessage(getString(R.string.sdcard_mounted));
			alertDialog.setButton(getString(R.string.close_instructions), 
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
					return;
				} });
			alertDialog.show();  
		}

		// build the Book list
		// TODO 20111202 (damienkallison) Refactor loading into load time.
		List<DaisyBook> bookList = new ArrayList<DaisyBook>();
		for (String path : books) {
			try {
				DaisyBook book = new OldDaisyBookImplementation();
				book.openFromFile(path + File.separator + 
						DaisyBookUtils.getNccFileName(new File(path)));
				bookList.add(book);
			} catch (InvalidDaisyStructureException e) {
				Logging.logSevereWarning(TAG, "Invalid daisy structure " +
						"exception reading: " + path, e);
			} catch (IOException e) {
				Logging.logSevereWarning(TAG, "Skipping book due to IO " +
						"exception reading: " + path, e);
			} catch (RuntimeException e) {
				Logging.logSevereWarning(TAG, 
						"Skipping book due to runtime exception reading: " + path, e);
			}
		}
		
		// TODO (jharty): format the list of books more attractively.
		setListAdapter(new ArrayAdapter<DaisyBook>(this, R.layout.listrow,
				R.id.textview, bookList));
		getListView().setTextFilterEnabled(true);
	}
}

