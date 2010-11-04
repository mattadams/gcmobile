package com.radicaldynamic.turboform.xform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.mycila.xmltool.XMLTag;
import com.radicaldynamic.turboform.application.Collect;

import android.util.Log;

public class Control
{
    // Any attributes found on this element
    public Map<String, String> attributes = new HashMap<String, String>();
    // Any children (other controls) of this one, e.g., groups and repeats (or items for a select or select1 control)
    public ArrayList<Control> children = new ArrayList<Control>();  
    
    private String type;                        // The element name of this control  (e.g., group, input, etc.)
    private String location;                    // The element location of this node (e.g., *[2]/*[1])
    private String value;                       // Any value assigned to this node (if it is an item)
    private ControlText label;                  // Any label assigned to this control
    private ControlText hint;                   // Any hint assigned to this control
    private boolean hidden = false;             // Whether or not this is a hidden control
    
    public Control(XMLTag tag)
    {
        Log.v(Collect.LOGTAG, "Created new " + tag.getCurrentTagName() + " control at " + tag.getCurrentTagLocation());
        
        setType(tag.getCurrentTagName());
        setLocation(tag.getCurrentTagLocation());
        
        // Read in attributes (includes "ref" to instance data output)
        for (String s : tag.getAttributeNames()) {
            attributes.put(s, tag.getAttribute(s));
        }
    }

    public void setLabel(String label)
    {
        Log.v(Collect.LOGTAG, "Setting label " + label + " for " + type + " at " + location);
        this.label = new ControlText(label);
    }

    public String getLabel()
    {
        return label.toString();
    }

    public void setHint(String hint)
    {
        Log.v(Collect.LOGTAG, "Setting hint " + hint + " for " + type + " at " + location);
        this.hint = new ControlText(hint);
    }

    public String getHint()
    {
        return hint.toString();
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getLocation()
    {
        return location;
    }

    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }
}
