package com.radicaldynamic.groupinform.adapters;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinitionDocument;
import com.radicaldynamic.groupinform.documents.FormInstanceDocument;

public class BrowserListAdapter extends ArrayAdapter<FormDefinitionDocument>
{       
    private Context mContext;
    private ArrayList<FormDefinitionDocument> mItems;
    private HashMap<String, HashMap<String, String>> mTallies;
    private Spinner mSpinner;

    public BrowserListAdapter(Context context, int textViewResourceId, ArrayList<FormDefinitionDocument> items, HashMap<String, HashMap<String, String>> tallies, Spinner spinner) {
        super(context, textViewResourceId, items);
        mContext = context;
        mItems = items;           
        mTallies = tallies;
        mSpinner = spinner;
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
            ImageView fi = (ImageView) v.findViewById(R.id.icon);
            TextView tt = (TextView) v.findViewById(R.id.toptext);
            TextView bt = (TextView) v.findViewById(R.id.bottomtext);
            
            if (tt != null) {
                tt.setText(f.getName());
            }
            
            if (bt != null) {
                String tallies    = "";
                String draft      = null;
                String complete   = null;
                
                switch (mSpinner.getSelectedItemPosition()) {
                case 0:
                    fi.setImageDrawable(Collect.getInstance().getResources().getDrawable(R.drawable.to_do_list));
                    
                case 1:
                    if (mSpinner.getSelectedItemPosition() == 1)
                        fi.setImageDrawable(Collect.getInstance().getResources().getDrawable(R.drawable.to_do_list_edit));
                    
                    if (mTallies.containsKey(f.getId())) {
                        draft    = mTallies.get(f.getId()).get(FormInstanceDocument.Status.draft.toString());
                        complete = mTallies.get(f.getId()).get(FormInstanceDocument.Status.complete.toString());
                    }
                    
                    if (draft == null) 
                        draft = "0";
                    
                    if (complete == null)
                        complete = "0";
                    
                    tallies = draft + " draft(s), " + complete + " complete";
                    break;
                    
                case 2:
                    draft = mTallies.get(f.getId()).get(FormInstanceDocument.Status.draft.toString());
                    
                    if (draft == null)
                        draft = "0";
                    
                    fi.setImageDrawable(Collect.getInstance().getResources().getDrawable(R.drawable.to_do_list_checked1));
                    tallies = draft + " drafts";
                    
                    if (mTallies.get(f.getId()).get(FormInstanceDocument.Status.draft.toString()).equals("1"))
                        tallies = tallies.substring(0, tallies.length() - 1);
                    break;
                    
                case 3:
                    complete = mTallies.get(f.getId()).get(FormInstanceDocument.Status.complete.toString());
                    
                    if (complete == null)
                        complete = "0";
                    
                    fi.setImageDrawable(Collect.getInstance().getResources().getDrawable(R.drawable.to_do_list_checked3));
                    tallies = complete + " completed forms";
                    
                    if (mTallies.get(f.getId()).get(FormInstanceDocument.Status.complete.toString()).equals("1"))
                        tallies = tallies.substring(0, tallies.length() - 1);
                    break;
                }                
                
                bt.setText(tallies);               
            }
        }

        return v;
    }
}