package com.radicaldynamic.turboform.adapters;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.documents.FormDocument;
import com.radicaldynamic.turboform.documents.InstanceDocument;

public class LocalFormListAdapter extends ArrayAdapter<FormDocument>
{       
    private Context mContext;
    private ArrayList<FormDocument> mItems;
    private HashMap<String, HashMap<String, String>> mInstanceTalliesByStatus;
    
    public LocalFormListAdapter(Context context, int textViewResourceId, ArrayList<FormDocument> items, HashMap<String, HashMap<String, String>> instanceTalliesByStatus, Spinner spinner) {
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
            v = vi.inflate(R.layout.main_browser_list_item, null);
        } 

        FormDocument f = mItems.get(position);

        if (f != null) {
            TextView tt = (TextView) v.findViewById(R.id.toptext);
            TextView bt = (TextView) v.findViewById(R.id.bottomtext);

            if (tt != null) {
                tt.setText(f.getName());
            }

            if (bt != null) {
                String tallies = "";
                String incomplete = null;
                String complete = null;
                
                if (mInstanceTalliesByStatus.containsKey(f.getId())) {
                    incomplete = mInstanceTalliesByStatus.get(f.getId()).get(InstanceDocument.Status.incomplete.toString());
                    complete = mInstanceTalliesByStatus.get(f.getId()).get(InstanceDocument.Status.complete.toString());
                }
                
                if (incomplete == null) 
                    incomplete = "0";
                
                if (complete == null)
                    complete = "0";
                
                tallies = incomplete + " incomplete, " + complete + " complete";
                
                bt.setText(tallies);
            }

        }

        return v;
    }
}