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
import com.radicaldynamic.turboform.xform.Instance;

public class FormBuilderInstanceListAdapter extends ArrayAdapter<Instance>
{
    private ArrayList<Instance> mInstances;
    private Context mContext;
    
    public FormBuilderInstanceListAdapter(Context context, ArrayList<Instance> instanceList) {
        super(context, R.layout.fb_field_row, instanceList);
        mInstances = instanceList;
        mContext = context;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        Instance instance = mInstances.get(position);
        ArrayList<String> details = new ArrayList<String>();

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.fb_field_row, parent, false);
        }
        
        // If these objects are not reset to suitable defaults they might be reused with undesired side effects
        TextView labelView = (TextView) row.findViewById(R.id.label);
        TextView detailView = (TextView) row.findViewById(R.id.details);
        ImageView fieldTypeView = (ImageView) row.findViewById(R.id.field_type);
        
        labelView.setText(instance.getName());
        
        if (instance.getChildren().isEmpty()) {
            fieldTypeView.setImageDrawable(getDrawable(R.drawable.element_noicon));
            
            if (instance.getField() == null)
                if (instance.getBind().getAttributes().containsKey("jr:preload"))
                    details.add("Hidden data (auto populated)");
                else
                    details.add("Hidden data");
            else 
                details.add("Data from \"" + instance.getField().getLabel() + "\"");
                
        } else {
            fieldTypeView.setImageDrawable(getDrawable(R.drawable.element_group));             
            details.add("Select to view repeated fields");
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
