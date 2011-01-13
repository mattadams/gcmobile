package com.radicaldynamic.groupinform.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.logic.AccountGroup;

public class AccountGroupListAdapter extends ArrayAdapter<AccountGroup>
{       
    private Context mContext;
    private ArrayList<AccountGroup> mItems;
    
    public AccountGroupListAdapter(Context context, int textViewResourceId, ArrayList<AccountGroup> items) {
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

        AccountGroup f = mItems.get(position);

        if (f != null) {
            TextView tt = (TextView) v.findViewById(R.id.toptext);
            TextView bt = (TextView) v.findViewById(R.id.bottomtext);

            if (tt != null) {
                tt.setText("[" + f.getVisibility().toUpperCase() + "] " + f.getName());
            }

            if (bt != null) {
                bt.setText(f.getDescription());
            }

        }

        return v;
    }
}