package com.radicaldynamic.turboform.xform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import com.mycila.xmltool.XMLTag;
import com.radicaldynamic.turboform.application.Collect;

import android.util.Log;

public class Control
{
    private String t = "Control: ";
    
    // Any attributes found on this element
    public Map<String, String> attributes = new HashMap<String, String>();
    // Any children (other controls) of this one, e.g., groups and repeats (or items for a select or select1 control)
    public ArrayList<Control> children = new ArrayList<Control>();  
    
    private String type;                        // The XML element name of this control  (e.g., group, input, etc.)
    private String location;                    // The XML element location of this node (e.g., *[2]/*[1])
    private String ref;                         // Value of the "ref" attribute (if any)
    
    private String itemValue;                   // Any value assigned to this node (if it is an item)
    private String defaultValue;                // A default value assigned to this field (will be stored in the instance XML)
    
    private ControlText label;                  // Any label assigned to this control
    private ControlText hint;                   // Any hint assigned to this control
    
    private Bind bind;
    
    private boolean hidden = false;             // Whether or not this is a hidden control
    private boolean repeated = false;           // Whether this is a repeated control (e.g., a child of a <repeat> element.
                                                // This has a bearing on how the resulting XML will be output.
    
    /*
     * For controls instantiated from entries in <instance>
     * 
     * FIXME: entries in the instance are not guaranteed to be recreated at the same "level"
     */
    public Control(XMLTag tag, ArrayList<Bind> binds, String instancePath)
    {
        Log.v(Collect.LOGTAG, t + "created new \"" + tag.getCurrentTagName() + "\" hidden control from instance, having an instancePath of " + instancePath);

        setDefaultValue(tag.getInnerText());
        setHidden(true);        
        setLabel(tag.getCurrentTagName());
        setRef(instancePath);
        setType("input");

        // If a bind with a nodeset identical to this ref exists, bind it to this control (hahaha)
        Iterator<Bind> it = binds.iterator();                    
        
        while (it.hasNext()) {
            Bind b = it.next();
            
            if (b.getNodeset().equals(getRef())) {
                setBind(b);
                Log.v(Collect.LOGTAG, t + "bind with nodeset " + b.getNodeset() + " bound to hidden control");
            }
        }      
    }
    
    // For controls instantiated from entries in <h:body>
    public Control(XMLTag tag, Control parent, String instanceRoot, ArrayList<Bind> binds)
    {
        Log.v(Collect.LOGTAG, t + "created new " + tag.getCurrentTagName() + " control at " + tag.getCurrentTagLocation());
        
        setType(tag.getCurrentTagName());
        setLocation(tag.getCurrentTagLocation());
        
        // Read in attributes (includes "ref" to instance data output)
        for (String s : tag.getAttributeNames()) {
            attributes.put(s, tag.getAttribute(s));
            
            /*
             * Special handling for the ref attribute to make it easy to access.
             * 
             * Note that repeat elements have a nodeset attribute rather than a ref
             * but that both are essentially equivalent.  We need to remember this 
             * when it comes time to write out the XML. 
             */
            if (s.equals("ref") || s.equals("nodeset")) {
                String ref = tag.getAttribute(s);
                
                // If this reference is not to an itext translation then it must be to an instance/bind
                if (!Pattern.matches("^jr:.*", ref)) {
                    // If the reference is not literal then make it so
                    if (!Pattern.matches("^/.*", ref)) {
                        String newRef = "/" + instanceRoot + "/" + ref;
                        
                        /*
                         * This logic exists to support repeated elements.  We must ensure that their refs
                         * are literal if binds and other items are to be properly associated with them.
                         * 
                         * FIXME: this assumes that the immediate parent of a repeated items is a <repeat> control
                         */
                        if (parent != null) {
                            if (parent.getType().equals("repeat")) {
                                setRepeated(true);
                                newRef = parent.getRef() + "/" + ref;
                            }
                        }
                        
                        ref = newRef;                        
                    }
                    
                    // If a bind with a nodeset identical to this ref exists, bind it to this control (hahaha)
                    Iterator<Bind> it = binds.iterator();                    
                    
                    while (it.hasNext()) {
                        Bind b = it.next();
                        
                        // Log.v(Collect.LOGTAG, t + "testing bind " + b.getNodeset() + " for a match with " + ref);
                        
                        if (b.getNodeset().equals(ref)) {
                            setBind(b);
                            Log.v(Collect.LOGTAG, t + "bind with nodeset " + b.getNodeset() + " bound to this control at " + getLocation());
                        }
                    }
                }
                
                setRef(ref);
            }
        }
    }

    public void setLabel(String label)
    {
        Log.v(Collect.LOGTAG, t + "setting label " + label + " for " + type + " at " + location);
        this.label = new ControlText(label);
    }

    public String getLabel()
    {
        return label.toString();
    }

    public void setHint(String hint)
    {
        Log.v(Collect.LOGTAG, t + "setting hint " + hint + " for " + type + " at " + location);
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

    public void setItemValue(String itemValue)
    {
        this.itemValue = itemValue;
    }

    public String getItemValue()
    {
        return itemValue;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public void setRef(String ref)
    {
        this.ref = ref;
    }

    public String getRef()
    {
        return ref;
    }

    public void setBind(Bind bind)
    {
        this.bind = bind;
    }

    public Bind getBind()
    {
        return bind;
    }

    public void setRepeated(boolean repeated)
    {
        this.repeated = repeated;
    }

    public boolean isRepeated()
    {
        return repeated;
    }
}
