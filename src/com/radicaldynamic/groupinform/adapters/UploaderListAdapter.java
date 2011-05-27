package com.radicaldynamic.groupinform.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.documents.FormDefinition;

public class UploaderListAdapter extends ArrayAdapter<FormDefinition>
{       
    private Context mContext;
    private ArrayList<FormDefinition> mItems;
    private Map<String, List<String>> mTallies;    

    public UploaderListAdapter(Context context, int textViewResourceId, ArrayList<FormDefinition> items, Map<String, List<String>> tallies) {
        super(context, textViewResourceId, items);
        mContext = context;
        mItems = items;           
        mTallies = tallies;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;        

        if (v == null) {            
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.two_item_multiple_choice, null);
        } 

        FormDefinition f = mItems.get(position);

        if (f != null) {
            TextView tt = (TextView) v.findViewById(R.id.text1);
            TextView bt = (TextView) v.findViewById(R.id.text2);

            if (tt != null) {
                tt.setText(f.getName());
            }

            // TODO: ODK 1.1.6 or 484 and newer display additional information about failed/partially submitted attempts
            // as well as where submissions will be sent to ... consider also displaying that information
            if (!mTallies.isEmpty()) {          
                if (bt != null) {
                    bt.setText(mTallies.get(f.getId()).size() + " " + mContext.getText(R.string.tf_forms_ready_to_upload));
                }
            }
        }

        return v;
    }
}