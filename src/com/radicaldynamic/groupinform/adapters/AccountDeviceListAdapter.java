package com.radicaldynamic.groupinform.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.logic.AccountDevice;

public class AccountDeviceListAdapter extends ArrayAdapter<AccountDevice>
{       
    private Context mContext;
    private ArrayList<AccountDevice> mItems;
    
    public AccountDeviceListAdapter(Context context, int textViewResourceId, ArrayList<AccountDevice> items) {
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
            v = vi.inflate(R.layout.device_list_item, null);
        } 

        AccountDevice f = mItems.get(position);

        if (f != null) {
            TextView tt = (TextView) v.findViewById(R.id.toptext);
            TextView bt = (TextView) v.findViewById(R.id.bottomtext);

            if (tt != null) {
                if (f.getAlias() == null || f.getAlias().equals("null") || f.getAlias().length() == 0)
                    tt.setText(mContext.getString(R.string.tf_alias_not_assigned));
                else 
                    tt.setText(f.getAlias());
            }

            if (bt != null) {
                if (f.getEmail() == null || f.getEmail().equals("null"))
                    tt.setText(mContext.getString(R.string.tf_email_not_available));
                else
                    bt.setText(f.getEmail());
            }

        }

        return v;
    }
}