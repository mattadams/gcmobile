package com.radicaldynamic.turboform.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.xform.Field;

public class FormBuilderSelectItemListAdapter extends ArrayAdapter<Field>
{
    private ArrayList<Field> mFields;
    private Context mContext;
    
    public FormBuilderSelectItemListAdapter(Context context, ArrayList<Field> fieldList) {
        super(context, R.layout.fb_item_row, fieldList);
        mFields = fieldList;
        mContext = context;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        Field field = mFields.get(position);

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.fb_item_row, parent, false);
        }
        
        // If these objects are not reset to suitable defaults they might be reused with undesired side effects
        TextView labelView = (TextView) row.findViewById(R.id.label);
        TextView detailView = (TextView) row.findViewById(R.id.details);
        ToggleButton preselected = (ToggleButton) row.findViewById(R.id.preselected);
        
        /*
         * Shorten label to an appropriate length
         * 
         * TODO: this might not be suitable on different devices, resolutions, orientations etc.
         */
        if (field.getLabel().toString().length() > 30)
            labelView.setText(field.getLabel().toString().substring(0, 27) + "...");
        else               
            labelView.setText(field.getLabel().toString());
      
        // Item value (as will be saved in the instance output)
        if (field.getItemValue() == null || field.getItemValue().length() == 0)
            detailView.setText("Value not set!");
        else 
            detailView.setText(field.getItemValue());
        
        // Preselected indicator
        preselected.setChecked(field.isItemDefault());
        
        return (row);
    }
}
