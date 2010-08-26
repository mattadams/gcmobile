/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.radicaldynamic.turboform.activities;

import com.radicaldynamic.turboform.R;

import com.radicaldynamic.turboform.database.FileDbAdapter;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Responsible for displaying and deleting all the valid forms in the forms directory.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class FormManagerList extends ListActivity {

    private AlertDialog mAlertDialog;
    private Button mActionButton;
    private Button mGetButton;

    private SimpleCursorAdapter mInstances;
    private ArrayList<Long> mSelected = new ArrayList<Long>();
    private boolean mRestored = false;
    private final String SELECTED = "selected";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form_manage_list);
        mGetButton = (Button) findViewById(R.id.get_button);
        mGetButton.setText(getString(R.string.get_forms));
        mGetButton.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), FormDownloadList.class);
                startActivity(i);
            }
        });
        
        mActionButton = (Button) findViewById(R.id.delete_button);
        mActionButton.setText(getString(R.string.delete_file));
        mActionButton.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {

                if (mSelected.size() > 0) {
                    createDeleteDialog();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.noselect_error,
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
        // buildView takes place in resume
    }


    private void refreshView() {
        // get all mInstances that match the status.
        FileDbAdapter fda = new FileDbAdapter();
        fda.open();
        fda.addOrphanForms();
        Cursor c = fda.fetchFilesByType(FileDbAdapter.TYPE_FORM, null);
        startManagingCursor(c);

        String[] data = new String[] {
                FileDbAdapter.KEY_DISPLAY, FileDbAdapter.KEY_META
        };
        int[] view = new int[] {
                R.id.text1, R.id.text2
        };

        // render total instance view
        mInstances =
            new SimpleCursorAdapter(this, R.layout.two_item_multiple_choice, c, data, view);
        setListAdapter(mInstances);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        getListView().setItemsCanFocus(false);
        mActionButton.setEnabled(!(mSelected.size() == 0));

        // cleanup
        fda.close();

        // if current activity is being reinitialized due to changing
        // orientation
        // restore all check marks for ones selected
        if (mRestored) {
            ListView ls = getListView();
            for (long id : mSelected) {
                for (int pos = 0; pos < ls.getCount(); pos++) {
                    if (id == ls.getItemIdAtPosition(pos)) {
                        ls.setItemChecked(pos, true);
                        break;
                    }
                }

            }
            mRestored = false;
        }
    }


    /**
     * Create the file delete dialog
     */
    private void createDeleteDialog() {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setTitle(getString(R.string.delete_file));
        mAlertDialog.setMessage(getString(R.string.delete_confirm, mSelected.size()));
        DialogInterface.OnClickListener dialogYesNoListener =
            new DialogInterface.OnClickListener() {
                @Override
				public void onClick(DialogInterface dialog, int i) {
                    switch (i) {
                        case DialogInterface.BUTTON1: // delete and
                            deleteSelectedFiles();
                            refreshData();
                            break;
                        case DialogInterface.BUTTON2: // do nothing
                            break;
                    }
                }

            };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.delete_yes), dialogYesNoListener);
        mAlertDialog.setButton2(getString(R.string.delete_no), dialogYesNoListener);
        mAlertDialog.show();
    }


    private void refreshData() {
        if (mInstances != null) {
            mInstances.getCursor().requery();
        }
        if (!mRestored) {
            mSelected.clear();
        }
        refreshView();
    }


    /**
     * Deletes the selected files.First from the database then from the file system
     */
    private void deleteSelectedFiles() {
        FileDbAdapter fda = new FileDbAdapter();
        fda.open();

        // delete removes the file from the database first
        int deleted = 0;
        for (int i = 0; i < mSelected.size(); i++) {
            if (fda.deleteFile(mSelected.get(i))) {
                deleted++;
            }
        }

        // remove the actual files and close db
        fda.removeOrphanForms();
        fda.removeOrphanInstances(this);
        fda.close();

        if (deleted > 0) {
            // all deletes were successful
            Toast.makeText(getApplicationContext(), getString(R.string.file_deleted_ok, deleted),
                Toast.LENGTH_SHORT).show();
            refreshData();
            if (mInstances.isEmpty()) {
                finish();
            }
        } else {
            // had some failures
            Toast.makeText(
                getApplicationContext(),
                getString(R.string.file_deleted_error, mSelected.size() - deleted + " of "
                        + mSelected.size()), Toast.LENGTH_LONG).show();
        }

    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        // get row id from db
        Cursor c = (Cursor) getListAdapter().getItem(position);
        long k = c.getLong(c.getColumnIndex(FileDbAdapter.KEY_ID));

        // add/remove from selected list
        if (mSelected.contains(k))
            mSelected.remove(k);
        else
            mSelected.add(k);

        mActionButton.setEnabled(!(mSelected.size() == 0));

    }


    @Override
    protected void onPause() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        super.onPause();
    }


    @Override
    protected void onResume() {
        // update the list (for returning from the remote manager)
        refreshData();
        super.onResume();
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        long[] selectedArray = savedInstanceState.getLongArray(SELECTED);
        for (int i = 0; i < selectedArray.length; i++)
            mSelected.add(selectedArray[i]);
        mRestored = true;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        long[] selectedArray = new long[mSelected.size()];
        for (int i = 0; i < mSelected.size(); i++)
            selectedArray[i] = mSelected.get(i);
        outState.putLongArray(SELECTED, selectedArray);
    }
}
