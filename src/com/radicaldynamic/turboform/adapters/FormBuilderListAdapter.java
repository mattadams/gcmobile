package com.radicaldynamic.turboform.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.xform.Control;

public class FormBuilderListAdapter extends ArrayAdapter<Control>
{
    private ArrayList<Control> mControls;
    private Context mContext;
    
    public FormBuilderListAdapter(Context context, ArrayList<Control> controlList) {
        super(context, R.layout.form_builder_row2, controlList);
        mControls = controlList;
        mContext = context;    
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        Control control = mControls.get(position);

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.form_builder_row2, parent, false);
        }
        
        TextView label = (TextView) row.findViewById(R.id.label);
        TextView details = (TextView) row.findViewById(R.id.details);
        ImageView controlType = (ImageView) row.findViewById(R.id.control_type);
        
        label.setText(control.getLabel());
       
        if (control.getType().equals("group")) {
            // No need to set drawable (this is the default)            
            if (control.children.isEmpty()) 
                details.setText("Empty group (no controls)");
            else
                details.setText("Group has controls");                  
        } else if (control.getType().equals("input")) {
            controlType.setImageDrawable(mContext.getResources().getDrawable(R.drawable.element_string));
        } else if (control.getType().equals("repeat")) {
            
        } else if (control.getType().equals("select")) {
            controlType.setImageDrawable(mContext.getResources().getDrawable(R.drawable.element_selectmulti));
        } else if (control.getType().equals("select1")) {
            controlType.setImageDrawable(mContext.getResources().getDrawable(R.drawable.element_selectsingle));
        } else if (control.getType().equals("trigger")) {
            
        } else if (control.getType().equals("upload")) {
            controlType.setImageDrawable(mContext.getResources().getDrawable(R.drawable.element_media));
        }
        
        return (row);
    }
}
