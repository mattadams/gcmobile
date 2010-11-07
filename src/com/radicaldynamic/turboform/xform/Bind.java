package com.radicaldynamic.turboform.xform;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import android.util.Log;

import com.mycila.xmltool.XMLTag;
import com.radicaldynamic.turboform.application.Collect;

public class Bind
{
    private String t = "Bind: ";
    
    // All attributes taken from the bind
    public Map<String, String> attributes = new HashMap<String, String>();
    
    // Indicates whether or not this bind contains attributes that we do not handle
    private boolean hasUnhandledAttribute;
    
    // Important fields that we need easy access to
    private String nodeset;
    private String type;
    private boolean required;
    
    public Bind(XMLTag tag, String instanceRoot)
    {
        // Read in attributes (includes "ref" to instance data output)
        for (String s : tag.getAttributeNames()) {
            attributes.put(s, tag.getAttribute(s));

            // Special handling for certain attributes
            if (s.equals("nodeset")) {
                String nodeset = tag.getAttribute(s);

                // If the nodeset is not literal then make it so
                if (!Pattern.matches("^/.*", nodeset)) {
                    nodeset = "/" + instanceRoot + "/" + nodeset;
                }

                setNodeset(nodeset);         
            } else if (s.equals("type"))
                setType(tag.getAttribute(s));
            else if (s.equals("required"))
                setRequired(true);
            else 
                setHasUnhandledAttribute(true);
        }
        
        Log.v(Collect.LOGTAG, t + "created new bind for " + getNodeset());
    }
    
    public void setNodeset(String nodeset)
    {
        this.nodeset = nodeset;
    }
    
    public String getNodeset()
    {
        return nodeset;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    public void setRequired(boolean required)
    {
        this.required = required;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setHasUnhandledAttribute(boolean hasUnhandledAttribute)
    {
        this.hasUnhandledAttribute = hasUnhandledAttribute;
    }

    public boolean isHasUnhandledAttribute()
    {
        return hasUnhandledAttribute;
    }
}
