package com.radicaldynamic.groupinform.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.logic.FormGroup;

public class GroupListAdapter extends ArrayAdapter<FormGroup>
{       
    private Context mContext;
    private ArrayList<FormGroup> mItems;
    
    public GroupListAdapter(Context context, int textViewResourceId, ArrayList<FormGroup> items) {
        super(context, textViewResourceId, items);
        mContext = context;
        mItems = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;        

        if (v == null) {            
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.group_list_item, null);
        } 

        FormGroup f = mItems.get(position);

        if (f != null) {
            TextView tt = (TextView) v.findViewById(R.id.toptext);
            TextView bt = (TextView) v.findViewById(R.id.bottomtext);

            if (tt != null) {
                tt.setText(f.getName());
            }

            if (bt != null) {
                bt.setText(f.getDescription());
            }

        }

        return v;
    }
}