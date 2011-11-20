package com.radicaldynamic.groupinform.tasks;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.supercsv.cellprocessor.constraint.StrMinMax;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.listeners.SelectFieldImportListener;

//public class SelectFieldImportTask extends AsyncTask<Params, Progress, Result> {
public class SelectFieldImportTask extends AsyncTask<Void, Void, ArrayList<List<String>>> 
{
    private static final String t = "SelectFieldImportTask: ";
    
    private SelectFieldImportListener mStateListener;

    private boolean mImportClearList = false;
    private String mImportFilePath;
    private boolean mImportSkipFirstLine = false;
    private int mImportMode = -1;
    private boolean mImportSuccessful = false;
    
    static final CellProcessor[] processor = new CellProcessor[] {
        new StrMinMax(1, 128),
        new StrMinMax(1, 128),
    };

    @Override
    protected ArrayList<List<String>> doInBackground(Void... params) 
    {
        ArrayList<List<String>> importedData = new ArrayList<List<String>>();
        List<String> line;
        
        try {
            ICsvListReader inFile = new CsvListReader(new FileReader(mImportFilePath), CsvPreference.EXCEL_PREFERENCE);
            
            // If the user doesn't want to import the first line, skip it and discard
            if (mImportSkipFirstLine) {
                inFile.getCSVHeader(true);
            }

            switch (mImportMode) {
            case SelectFieldImportListener.MODE_PREVIEW:
                while ((line = inFile.read(processor)) != null) {
                    importedData.add(line);
                    
                    // Only read a few lines in
                    if (inFile.getLineNumber() > 4)
                        break;
                }
                
                break;
            case SelectFieldImportListener.MODE_IMPORT:
                while ((line = inFile.read(processor)) != null) {
                    importedData.add(line);
                }
                
                break;
            }
            
            inFile.close();
            
            mImportSuccessful = true;
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "error while reading CSV file for import: " + e.toString());
            e.printStackTrace();
        }

        return importedData;
    }    

    @Override
    protected void onPostExecute(ArrayList<List<String>> importedData)
    {   
        synchronized (this) {
            if (mStateListener != null) {
                Bundle b = new Bundle();                
                b.putBoolean(SelectFieldImportListener.CLEAR_LIST, mImportClearList);
                b.putBoolean(SelectFieldImportListener.SUCCESSFUL, mImportSuccessful);
                b.putInt(SelectFieldImportListener.MODE, mImportMode);                
                mStateListener.importTaskFinished(b, importedData);
            }
        }
    }
    
    public void setImportClearList(boolean b)
    {
        mImportClearList = b;
    }
    
    public void setImportSkipFirstLine(boolean b)
    {
        mImportSkipFirstLine = b;
    }
    
    public void setImportFilePath(String pathToFile)
    {
        mImportFilePath = pathToFile;
    }
    
    public void setImportMode(int m)
    {
        mImportMode = m;
    }
    
    public void setListener(SelectFieldImportListener sl) 
    {
        synchronized (this) {
            mStateListener = sl;
        }
    }
}

