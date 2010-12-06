package com.radicaldynamic.groupinform.xform;

import java.util.ArrayList;
import java.util.Iterator;

import android.util.Log;

import com.radicaldynamic.groupinform.application.Collect;

public class Instance
{
    private static final String t = "Instance: ";
    
    private ArrayList<Instance> children = new ArrayList<Instance>();
    
    private Bind bind           = new Bind();       // Binds should be used here vs going through getField() as hidden
                                                    // instance fields will not have associated fields but are likely
                                                    // to have binds.
    
    private Field field         = null;             // The field to which this instance is associated
    private Instance parent     = null;             // The parent to this instance (if it is nested in the linked list)
        
    private String location     = null;             // The XML element location of this node (e.g., *[2]/*[1])
    private String xpath        = null;             // The XPath to this element (same as "ref" or "nodeset" for fields)
    private String defaultValue = null;             // Any defaultValue assigned to this element
    
    private boolean active      = false;            // Used to determine which field is "active" in form builder navigation
    private boolean hidden      = false;            // This is a hidden instance (it has no associated field)
    
    /*
     * Used for instantiating instances created by the form builder and to ensure
     * that default binds are available to newly created fields
     */
    public Instance()
    {
        this.defaultValue = "";
    }
    
    public Instance(String instancePath, String defaultValue, String location, ArrayList<Bind> binds)
    {
        Log.v(Collect.LOGTAG, t + "creating new instance with XPath " + instancePath + " and a default value of \"" + defaultValue + "\"");
        
        this.xpath          = instancePath;
        this.defaultValue   = defaultValue;
        this.location       = location;
        
        Iterator<Bind> it = binds.iterator();                    
        
        while (it.hasNext()) {
            Bind b = it.next();

            // If a bind with a nodeset identical to this ref exists, associate it with this field
            if (b.getXPath().equals(instancePath)) {
                setBind(b);
                Log.v(Collect.LOGTAG, t + "bind with nodeset " + b.getXPath() + " bound to this instance at " + instancePath);
            }
        }
    }

    public ArrayList<Instance> getChildren()
    {
        return children;
    }
    
    public void setBind(Bind bind)
    {
        this.bind = bind;
    }

    public Bind getBind()
    {
        return bind;
    }

    public void setField(Field field)
    {
        this.field = field;
    }

    public Field getField()
    {
        return field;
    }

    public void setParent(Instance parent)
    {
        this.parent = parent;
    }

    public Instance getParent()
    {
        return parent;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getLocation()
    {
        return location;
    }

    public void setXPath(String xpath)
    {
        this.xpath = xpath;
    }

    public String getXPath()
    {
        return xpath;
    }

    /*
     * Default values may include entities that need to be encoded, similarly to
     * labels and hints, so we take care of that here 
     */
    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = FieldText.encodeXMLEntities(defaultValue);
    }

    public String getDefaultValue()
    {
        return FieldText.decodeXMLEntities(defaultValue);
    }
    
    public String getName()
    {
        return xpath.substring(xpath.lastIndexOf("/") + 1, xpath.length());
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    public boolean isHidden()
    {
        return hidden;
    }
}
