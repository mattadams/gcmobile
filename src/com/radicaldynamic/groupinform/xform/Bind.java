package com.radicaldynamic.groupinform.xform;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import android.util.Log;

import com.mycila.xmltool.XMLTag;
import com.radicaldynamic.groupinform.application.Collect;

public class Bind
{
    private String t = "Bind: ";
    
    // All unhandled attributes taken from the bind
    public Map<String, String> attributes = new HashMap<String, String>();
    
    // Indicates whether or not this bind contains attributes that we do not handle
    private boolean hasUnhandledAttribute;
    
    // Important fields that we need easy access to (none of these should be null after parsing the XForm)
    private String xpath;                           // Value of the "nodeset" attribute (same as "ref" or "nodeset" in fields)
    private String type;
    private boolean required = false;
    private boolean readonly = false;
    
    // Other common bind attributes that we handle directly
    private String preload;
    private String preloadParams;    
    private String constraint;
    private String constraintMsg;
    private String relevant;
    private String calculate;
    
    /*
     * Used for instantiating binds created by the form builder and to ensure
     * that default binds are available to newly created fields
     */
    public Bind()
    {
        
    }
    
    public Bind(XMLTag tag, String instanceRoot)
    {
        // Read in attributes (includes "ref" to instance data output)
        for (String s : tag.getAttributeNames()) {            
            // Special handling for certain attributes
            if (s.equals("nodeset")) {
                String nodeset = tag.getAttribute(s);

                // If the nodeset is not literal then make it so
                if (!Pattern.matches("^/.*", nodeset)) {
                    nodeset = "/" + instanceRoot + "/" + nodeset;
                }

                setXPath(nodeset);         
            } else if (s.equals("type")) {
                // KoBo Forms Designer outputs something like xsd:type but that doesn't help us.  Workaround ensues.
                String t = tag.getAttribute(s);
                
                if (t.contains(":")) {
                    t = t.substring(t.indexOf(":") + 1);
                }
            
                setType(t);
            } else if (s.equals("required") && tag.getAttribute(s).equals("true()"))
                setRequired(true);
            else if (s.equals("readonly") && tag.getAttribute(s).equals("true()"))
                setReadonly(true);
            else if (s.equals("jr:preload"))
                setPreload(tag.getAttribute(s));
            else if (s.equals("jr:preloadParams"))
                setPreloadParams(tag.getAttribute(s));
            else if (s.equals("constraint")) 
                setConstraint(tag.getAttribute(s));
            else if (s.equals("jr:constraintMsg"))
                setConstraintMsg(tag.getAttribute(s));
            else if (s.equals("relevant"))
                setRelevant(tag.getAttribute(s));
            else if (s.equals("calculate"))
                setCalculate(tag.getAttribute(s));
            else {
                attributes.put(s, tag.getAttribute(s));
                setHasUnhandledAttribute(true);
            }                
        }
        
        Log.v(Collect.LOGTAG, t + "created new bind for " + getXPath());
    }
    
    public Map<String, String> getAttributes()
    {
        return attributes;
    }
    
    public void setXPath(String xpath)
    {
        this.xpath = xpath;
    }
    
    public String getXPath()
    {
        return xpath;
    }

    public void setType(String type)
    {   
        this.type = type;
    }

    public String getType()
    {
        /*
         * Also see field/bind association bit in the Field class constructor where we 
         * attempt to ensure that all binds have the minimum number of expected attributes
         * 
         * We do this here because a bind may not have a type nor a control field that will
         * ensure that one is set.
         */          
//        if (type == null)
//            return "string";
//        else 
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
    
    public String getRequired()
    {
        if (isRequired())
            return "true()";
        else
            return "false()";
    }

    public void setReadonly(boolean readonly)
    {
        this.readonly = readonly;
    }

    public boolean isReadonly()
    {
        return readonly;
    }
    
    public String getReadonly()
    {
        if (isReadonly())
            return "true()";
        else
            return "false()";
    }

    public void setHasUnhandledAttribute(boolean hasUnhandledAttribute)
    {
        this.hasUnhandledAttribute = hasUnhandledAttribute;
    }

    public boolean hasUnhandledAttribute()
    {
        return hasUnhandledAttribute;
    }

    public void setPreload(String preload)
    {
        this.preload = preload;
    }

    public String getPreload()
    {
        return preload;
    }

    public void setPreloadParams(String preloadParams)
    {
        this.preloadParams = preloadParams;
    }

    public String getPreloadParams()
    {
        return preloadParams;
    }

    public void setConstraint(String constraint)
    {
        this.constraint = constraint;
    }

    public String getConstraint()
    {
        return constraint;
    }

    public void setConstraintMsg(String constraintMsg)
    {
        this.constraintMsg = constraintMsg;
    }

    public String getConstraintMsg()
    {
        return constraintMsg;
    }

    public void setRelevant(String relevant)
    {
        this.relevant = relevant;
    }

    public String getRelevant()
    {
        return relevant;
    }

    public void setCalculate(String calculate)
    {
        this.calculate = calculate;
    }

    public String getCalculate()
    {
        return calculate;
    }
}
