package com.radicaldynamic.groupinform.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinition;
import com.radicaldynamic.groupinform.documents.FormInstance;

public class BrowserLongListAdapter extends ArrayAdapter<FormInstance> 
{
    private Map<String, FormDefinition> mFormDefinitions = new HashMap<String, FormDefinition>();

    public BrowserLongListAdapter(Context context, int textViewResourceId, ArrayList<FormInstance> instances, ArrayList<FormDefinition> definitions) 
    {
        super(context, textViewResourceId, instances);
        
        for (int i = 0; i < definitions.size(); i++) {
            mFormDefinitions.put(definitions.get(i).getId(), definitions.get(i));
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;        

        if (v == null) {            
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.browser_list_item, null);
        } 

        FormInstance i = getItem(position);
        
        if (i == null)
            return v;
        
        ImageView icon = (ImageView) v.findViewById(R.id.icon);
        
        TextView topText = (TextView) v.findViewById(R.id.toptext);
        TextView bottomText = (TextView) v.findViewById(R.id.bottomtext);
        
        if (i.getStatus().equals(FormInstance.Status.draft)) 
            icon.setImageDrawable(Collect.getInstance().getResources().getDrawable(R.drawable.to_do_list_checked1));
        else if (i.getStatus().equals(FormInstance.Status.complete))
            icon.setImageDrawable(Collect.getInstance().getResources().getDrawable(R.drawable.to_do_list_checked3));

        topText.setText(mFormDefinitions.get(i.getFormId()).getName());
        
        if (i.getName() == null)
            bottomText.setText("");
        else
            bottomText.setText(i.getName());
        
        return v;
    }
}
