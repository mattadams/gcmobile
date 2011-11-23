package com.radicaldynamic.groupinform.adapters;

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

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.xform.Field;
import com.radicaldynamic.groupinform.xform.XForm;

public class FormBuilderFieldListAdapter extends ArrayAdapter<Field>
{
    private ArrayList<Field> mFields;
    private Context mContext;
    
    public FormBuilderFieldListAdapter(Context context, ArrayList<Field> fieldList) {
        super(context, R.layout.fb_field_row, fieldList);
        mFields = fieldList;
        mContext = context;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        Field field = mFields.get(position);
        ArrayList<String> details = new ArrayList<String>();

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.fb_field_row, parent, false);
        }
        
        // If these objects are not reset to suitable defaults they might be reused with undesired side effects
        TextView labelView = (TextView) row.findViewById(R.id.label);
        TextView detailView = (TextView) row.findViewById(R.id.details);
        ImageView fieldTypeView = (ImageView) row.findViewById(R.id.field_type);
        
        /*
         * Shorten label to an appropriate length
         * 
         * TODO: this might not be suitable on different devices, resolutions, orientations etc.
         */
        if (field.getLabel().toString().length() > 30) {
            labelView.setText(field.getLabel().toString().substring(0, 27) + "...");
        } else {
            if (field.getLabel().toString().length() == 0)
                labelView.setText("Label missing!");
            else
                labelView.setText(field.getLabel().toString());
        }

        /*
         * Customize the row according to per-field specifics
         */
        if (field.getType().equals("group")) {
            fieldTypeView.setImageDrawable(getDrawable(R.drawable.element_group));            
            
            // Special logic to hide the complexity of repeated elements
            if (Field.isRepeatedGroup(field)) {
                if (field.getRepeat().getChildren().size() == 1)
                    details.add("Repeats " + field.getRepeat().getChildren().size() + " field");
                else
                    details.add("Repeats " + field.getRepeat().getChildren().size() + " fields");
            } else {
                if (field.getAttributes().containsKey(XForm.Attribute.APPEARANCE) &&
                        field.getAttributes().get(XForm.Attribute.APPEARANCE).equals(XForm.Value.FIELD_LIST))
                {
                    details.add("Displays " + field.getChildren().size() + " field(s) on one screen");
                } else {
                    if (field.getChildren().size() == 1)
                        details.add("Contains " + field.getChildren().size() + " field");
                    else
                        details.add("Contains " + field.getChildren().size() + " fields");
                }
            }            
        } else if (field.getType().equals("input")) {
            Drawable icon = getDrawable(R.drawable.element_string);
            
            try {
                String specificType = field.getBind().getType();
                
                if (specificType.equals("barcode")) {
                    icon = getDrawable(R.drawable.element_barcode);
                } else if (specificType.equals("date")) {
                    icon = getDrawable(R.drawable.element_calendar);
                    details.add("Pick date");
                } else if (specificType.equals("dateTime")) {
                    icon = getDrawable(R.drawable.element_calendar);
                    details.add("Pick date & time");
                } else if (specificType.equals("decimal")) {
                    icon = getDrawable(R.drawable.element_number);
                } else if (specificType.equals("geopoint")) {
                    icon = getDrawable(R.drawable.element_location);
                    
                    if (field.getAttributes().containsKey(XForm.Attribute.APPEARANCE) 
                            && field.getAttributes().get(XForm.Attribute.APPEARANCE).equals(XForm.Value.MAP))
                        details.add("With map");
                } else if (specificType.equals("int")) {
                    icon = getDrawable(R.drawable.element_number);
                } else if (specificType.equals("time")) {
                    icon = getDrawable(R.drawable.element_calendar);
                    details.add("Pick time");
                }
            } catch (NullPointerException e){
                // TODO: is this really a problem?    
            } finally {
                fieldTypeView.setImageDrawable(icon);
            }
            
        } else if (field.getType().equals("select")) {
            fieldTypeView.setImageDrawable(getDrawable(R.drawable.element_selectmulti));
            details.add(field.getChildren().size() + " items");
            
        } else if (field.getType().equals("select1")) {
            fieldTypeView.setImageDrawable(getDrawable(R.drawable.element_selectsingle));
            details.add(field.getChildren().size() + " items");
            
        } else if (field.getType().equals("trigger")) {
            fieldTypeView.setImageDrawable(getDrawable(R.drawable.element_noicon));
            details.add("Trigger");
            
        } else if (field.getType().equals("upload")) {
            if (field.getAttributes().get(XForm.Attribute.MEDIA_TYPE).contains("draw")) {
                String appearance = field.getAttributes().get(XForm.Attribute.APPEARANCE);
                
                fieldTypeView.setImageDrawable(getDrawable(R.drawable.element_draw));
                
                if (appearance == null) {
                    details.add("Sketch");
                } else {
                    details.add(appearance.substring(0, 1).toUpperCase() + appearance.substring(1));
                }
            } else {
                String mediaType = field.getAttributes().get(XForm.Attribute.MEDIA_TYPE);
                mediaType = mediaType.substring(0, 1).toUpperCase() + mediaType.substring(1, mediaType.indexOf("/")) + " media";
                fieldTypeView.setImageDrawable(getDrawable(R.drawable.element_media));
                details.add(mediaType);
            }
        }
        
        try {
            if (field.getBind().isRequired()) {
                details.add("Required");
            }
        } catch (NullPointerException e) {
           // TODO: is this really a problem?
        }
        
        // Build details line
        Iterator<String> it = details.iterator();
        
        // Suitable default
        String detailText = "";
        
        while (it.hasNext()) {
            String d = it.next();
            
            if (detailText.length() > 0) {
                detailText = detailText + ", " + d;
            } else {
                detailText = d;
            }
        }
        
        detailView.setText(detailText);        
        
        return (row);
    }
    
    private Drawable getDrawable(int image)
    {
        return mContext.getResources().getDrawable(image);
    }
}
