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
import com.radicaldynamic.groupinform.documents.FormDefinitionDocument;

public class UploaderListAdapter extends ArrayAdapter<FormDefinitionDocument>
{       
    private Context mContext;
    private ArrayList<FormDefinitionDocument> mItems;
    private Map<String, List<String>> mInstanceTallies;    

    public UploaderListAdapter(Context context, int textViewResourceId, ArrayList<FormDefinitionDocument> items, Map<String, List<String>> instanceTallies) {
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

        FormDefinitionDocument f = mItems.get(position);

        if (f != null) {
            TextView tt = (TextView) v.findViewById(R.id.text1);
            TextView bt = (TextView) v.findViewById(R.id.text2);

            if (tt != null) {
                tt.setText(f.getName());
            }

            // TODO: ODK 1.1.6 or 484 and newer display additional information about failed/partially submitted attempts
            // as well as where submissions will be sent to ... consider also displaying that information
            if (!mInstanceTallies.isEmpty()) {          
                if (bt != null) {
                    bt.setText(mInstanceTallies.get(f.getId()).size() + " " + mContext.getText(R.string.tf_forms_ready_to_upload));
                }
            }
        }

        return v;
    }
}