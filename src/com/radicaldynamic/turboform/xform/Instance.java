package com.radicaldynamic.turboform.xform;

import java.util.ArrayList;

import android.util.Log;

import com.mycila.xmltool.XMLTag;
import com.radicaldynamic.turboform.application.Collect;

public class Instance
{
    private static final String t = "Instance: ";
    
    private ArrayList<Instance> children = new ArrayList<Instance>();
    
    private String location;                // The XML element location of this node (e.g., *[2]/*[1])
    private String xpath;                   // The XPath to this element (same as "ref" or "nodeset" for controls)
    private String name;                    // The element name
    private String defaultValue;            // Any defaultValue assigned to this element
    
    public Instance(XMLTag instance)
    {
        Log.v(Collect.LOGTAG, t + "creating new instance");
    }

    public ArrayList<Instance> getChildren()
    {
        return children;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getLocation()
    {
        return location;
    }

    public void setXpath(String xpath)
    {
        this.xpath = xpath;
    }

    public String getXpath()
    {
        return xpath;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }
}
