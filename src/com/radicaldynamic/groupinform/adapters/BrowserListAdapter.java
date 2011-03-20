package com.radicaldynamic.groupinform.adapters;

import java.util.ArrayList;
import java.util.Map;

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

public class BrowserListAdapter extends ArrayAdapter<FormDefinitionDocument>
{       
    private Context mContext;
    private ArrayList<FormDefinitionDocument> mItems;
    private Map<String, String> mInstanceTallies;
    private Spinner mSpinner;

    public BrowserListAdapter(Context context, int textViewResourceId, ArrayList<FormDefinitionDocument> items, Map<String, String> instanceTallies, Spinner spinner) {
        super(context, textViewResourceId, items);
        mContext = context;
        mItems = items;           
        mInstanceTallies = instanceTallies;
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

            if (!mInstanceTallies.isEmpty()) {           
                if (bt != null) {
                    String descriptor = mSpinner.getSelectedItem().toString().toLowerCase();
                    
                    switch (mSpinner.getSelectedItemPosition()) {
                    // Show all forms (in folder)  
                    case 0: 
                        fi.setImageDrawable(Collect.getInstance().getResources().getDrawable(R.drawable.ic_menu_agenda_new));
                        break;
    
                        // Show all draft forms
                    case 1:
                        fi.setImageDrawable(Collect.getInstance().getResources().getDrawable(R.drawable.ic_menu_agenda_draft));
                        break;
                        
                    // Show all completed forms
                    case 2:
                        fi.setImageDrawable(Collect.getInstance().getResources().getDrawable(R.drawable.ic_menu_agenda_complete));
                        break;
                    
                    default:
                        fi.setImageDrawable(Collect.getInstance().getResources().getDrawable(R.drawable.ic_menu_agenda_new));
                    }

                    // FIXME: correct plural words (this only works in very simple circumstances using English)
                    if (mInstanceTallies.get(f.getId()).equals("1")) {                           
                        descriptor = descriptor.substring(0, descriptor.length() - 1);
                    }

                    bt.setText(mInstanceTallies.get(f.getId()) + " " + descriptor);
                }
            }
        }

        return v;
    }
}