package com.radicaldynamic.groupinform.adapters;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.documents.FormDefinitionDocument;
import com.radicaldynamic.groupinform.documents.FormInstanceDocument;

public class MyFormsListAdapter extends ArrayAdapter<FormDefinitionDocument>
{       
    private Context mContext;
    private ArrayList<FormDefinitionDocument> mItems;
    private HashMap<String, HashMap<String, String>> mInstanceTalliesByStatus;
    
    public MyFormsListAdapter(Context context, int textViewResourceId, ArrayList<FormDefinitionDocument> items, HashMap<String, HashMap<String, String>> instanceTalliesByStatus, Spinner spinner) {
        super(context, textViewResourceId, items);
        mContext = context;
        mItems = items;           
        mInstanceTalliesByStatus = instanceTalliesByStatus;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;        

        if (v == null) {            
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.browser_list_item, null);
        } 

        FormDefinitionDocument f = mItems.get(position);

        if (f != null) {
            TextView tt = (TextView) v.findViewById(R.id.toptext);
            TextView bt = (TextView) v.findViewById(R.id.bottomtext);

            if (tt != null) {
                tt.setText(f.getName());
            }

            if (bt != null) {
                String tallies = "";
                String draft = null;
                String complete = null;
                
                if (mInstanceTalliesByStatus.containsKey(f.getId())) {
                    draft = mInstanceTalliesByStatus.get(f.getId()).get(FormInstanceDocument.Status.draft.toString());
                    complete = mInstanceTalliesByStatus.get(f.getId()).get(FormInstanceDocument.Status.complete.toString());
                }
                
                if (draft == null) 
                    draft = "0";
                
                if (complete == null)
                    complete = "0";
                
                tallies = draft + " draft(s), " + complete + " complete";
                
                bt.setText(tallies);
            }

        }

        return v;
    }
}