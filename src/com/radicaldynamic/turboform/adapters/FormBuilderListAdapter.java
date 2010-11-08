package com.radicaldynamic.turboform.adapters;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
        ArrayList<String> details = new ArrayList<String>();

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.form_builder_row2, parent, false);
        }
        
        // If these objects are not reset to suitable defaults they might be reused with undesired side effects
        TextView labelView = (TextView) row.findViewById(R.id.label);
        TextView detailView = (TextView) row.findViewById(R.id.details);
        ImageView controlTypeView = (ImageView) row.findViewById(R.id.control_type);
        
        /*
         * Shorten label to an appropriate length
         * 
         * TODO: this might not be suitable on different devices, resolutions, orientations etc.
         */
        if (control.getLabel().length() > 30)
            labelView.setText(control.getLabel().substring(0, 27) + "...");
        else               
            labelView.setText(control.getLabel());

        /*
         * Customize the row according to per-control specifics
         */
        
        if (control.getType().equals("group")) {
            controlTypeView.setImageDrawable(getDrawable(R.drawable.element_group));
            details.add("Contains " + control.children.size() + " elements");
            
        } else if (control.getType().equals("input")) {
            Drawable icon = getDrawable(R.drawable.element_string);
            
            try {
                String specificType = control.getBind().getType();
                
                if (specificType.equals("barcode"))     icon = getDrawable(R.drawable.element_barcode);     else
                if (specificType.equals("date"))        icon = getDrawable(R.drawable.element_calendar);    else
                if (specificType.equals("decimal"))     icon = getDrawable(R.drawable.element_number);      else
                if (specificType.equals("geopoint"))    icon = getDrawable(R.drawable.element_location);    else
                if (specificType.equals("int"))         icon = getDrawable(R.drawable.element_number);
            } catch (NullPointerException e){
                // TODO: is this really a problem?    
            } finally {
                controlTypeView.setImageDrawable(icon);
            }
            
        } else if (control.getType().equals("repeat")) { 
            controlTypeView.setImageDrawable(getDrawable(R.drawable.element_group));
            details.add("Repeated");
            
        } else if (control.getType().equals("select")) {
            controlTypeView.setImageDrawable(getDrawable(R.drawable.element_selectmulti));
            details.add(control.children.size() + " items");
            
        } else if (control.getType().equals("select1")) {
            controlTypeView.setImageDrawable(getDrawable(R.drawable.element_selectsingle));
            details.add(control.children.size() + " items");
            
        } else if (control.getType().equals("trigger")) {
            controlTypeView.setImageDrawable(getDrawable(R.drawable.element_noicon));
            details.add("Trigger");
            
        } else if (control.getType().equals("upload")) {
            String mediaType = control.attributes.get("mediatype");
            mediaType = mediaType.substring(0, 1).toUpperCase() + mediaType.substring(1, 5) + " media";
                        
            controlTypeView.setImageDrawable(getDrawable(R.drawable.element_media));
            details.add(mediaType);
        }
        
        try {
            if (control.getBind().isRequired())
                details.add("Required");
            
            if (control.isHidden())
                details.add("Hidden form data");
            
        } catch (NullPointerException e) {
           // TODO: is this really a problem?
        }
        
        // Build details line
        Iterator<String> it = details.iterator();
        
        // Suitable default
        String detailText = "";
        
        while (it.hasNext()) {
            String d = it.next();
            
            if (detailText.length() > 0)
                detailText = detailText + ", " + d;
            else 
                detailText = d;
        }
        
        detailView.setText(detailText);        
        
        return (row);
    }
    
    private Drawable getDrawable(int image)
    {
        return mContext.getResources().getDrawable(image);
    }
}
