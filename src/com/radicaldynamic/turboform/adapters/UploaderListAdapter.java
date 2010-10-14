package com.radicaldynamic.turboform.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.documents.FormDocument;

public class UploaderListAdapter extends ArrayAdapter<FormDocument>
{       
    private Context mContext;
    private ArrayList<FormDocument> mItems;
    private Map<String, List<String>> mInstanceTallies;    

    public UploaderListAdapter(Context context, int textViewResourceId, ArrayList<FormDocument> items, Map<String, List<String>> instanceTallies) {
        super(context, textViewResourceId, items);
        mContext = context;
        mItems = items;           
        mInstanceTallies = instanceTallies;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;        

        if (v == null) {            
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.two_item_multiple_choice, null);
        } 

        FormDocument f = mItems.get(position);

        if (f != null) {
            TextView tt = (TextView) v.findViewById(R.id.text1);
            TextView bt = (TextView) v.findViewById(R.id.text2);

            if (tt != null) {
                tt.setText(f.getName());
            }

            if (!mInstanceTallies.isEmpty()) {          
                if (bt != null) {
                    bt.setText(mInstanceTallies.get(f.getId()).size() + " " + mContext.getText(R.string.tf_forms_ready_to_upload));
                }
            }
        }

        return v;
    }
}