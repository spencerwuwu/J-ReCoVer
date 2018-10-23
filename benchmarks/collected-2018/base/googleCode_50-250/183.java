// https://searchcode.com/api/result/7299494/

package com.ader.ui;

/**
 * DaisyBrowser enables the user to pick the book they want to read.
 * 
 * It is used to navigate through a folder structure on the device, starting
 * with the /sdcard/ For now we assume always exists, we need to make the code
 * both more robust e.g. in case the sdcard is busy or unavailable, and more
 * flexible e.g. the user may want to store the books elsewhere.
 * 
 * This code is getting kinda creaky and is due for revamping. As we're
 * actively integrating BookValidator I can accept the current code for now
 * since some of the current hacks should be able to be removed during the
 * integration work.
 */
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ader.R;
import com.ader.utilities.DaisyBookUtils;
import com.ader.utilities.Logging;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;

public class DaisyBrowser extends ListActivity {
    private File currentDirectory = new File("/sdcard/");
    private List<String> files;
    private static final String TAG = "DaisyBrowser";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logging.logInfo(TAG, "onCreate");
        setContentView(R.layout.results_list);
        generateBrowserData();
    }


    @Override
    protected void onListItemClick(android.widget.ListView l,
            android.view.View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String item = files.get(position);

        File daisyPath = new File(currentDirectory, item);
		if (DaisyBookUtils.folderContainsDaisy2_02Book(daisyPath)) {
            Intent i = new Intent(this, DaisyReader.class);

            i.putExtra("daisyPath", daisyPath.getAbsolutePath() + "/");
            i.putExtra("daisyNccFile", DaisyBookUtils.getNccFileName(daisyPath));
            startActivity(i);
            return;
        }

        if (item.equals(this.getString(R.string.up_1_level))) {
            currentDirectory = new File(currentDirectory.getParent());
            generateBrowserData();
            return;
        }

        File temp = daisyPath;
        if (temp.isDirectory()) {
            currentDirectory = temp;
            generateBrowserData();
        }
    }

	void generateBrowserData() {

		// TODO(jharty): Check if currentDirectory maps to ExternalStorageDirectory
		Logging.logInfo(TAG, "External Storage is: " + Environment.getExternalStorageDirectory());
		// TODO(jharty): remove this hack once I've debugged the interaction
		// It probably needs to move to a more general FileIO class that'd be
		// used by the rest of the application. That way we can reduce
		// duplication of code e.g. with DaisyBookFinder and make the 
		// application more robust against events such as the sdcard becoming
		// unavailable while the application is in use.
		String storagestate = Environment.getExternalStorageState();
		if (!storagestate.equals(Environment.MEDIA_MOUNTED) ) {
		  AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		  alertDialog.setTitle(R.string.sdcard_title);
		  alertDialog.setMessage(this.getString(R.string.sdcard_mounted));
		  alertDialog.setButton(this.getString(R.string.close_instructions), 
				  new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
		      finish();
		      return;
		  } });
		  alertDialog.show();
		}

        FilenameFilter dirFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        };
        
        String[] listOfFiles = currentDirectory
                .list(dirFilter);
        if (listOfFiles != null) {
        	files = new ArrayList<String>(Arrays.asList(listOfFiles));
        	Collections.sort(files, String.CASE_INSENSITIVE_ORDER);
        	if (!currentDirectory.getParent().equals("/")) {
        		files.add(this.getString(R.string.up_1_level));
        	}
        } else {
        	files = new ArrayList<String>();
        }

        setListAdapter(new ArrayAdapter<String>(this,
        		R.layout.listrow, R.id.textview, files));
        return;

    }
}

