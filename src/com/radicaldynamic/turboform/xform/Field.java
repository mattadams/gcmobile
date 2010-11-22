package com.radicaldynamic.turboform.xform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import com.mycila.xmltool.XMLTag;
import com.radicaldynamic.turboform.application.Collect;

import android.util.Log;

public class Field
{
    private String t = "Field: ";
    
    // Any attributes found on this element
    public Map<String, String> attributes = new HashMap<String, String>();
    // Any children (other fields) of this one, e.g., groups and repeats (or items for a select or select1 field)
    public ArrayList<Field> children = new ArrayList<Field>();    
    
    private String type;                        // The XML element name of this field  (e.g., group, input, etc.)
    private String location;                    // The XML element location of this node (e.g., *[2]/*[1])
    private String ref;                         // Value of the "ref" attribute (if any)
    
    private Field parent;
    private String itemValue;                   // Any value assigned to this node (if it is an item)
    
    private FieldText label;                    // Any label assigned to this field
    private FieldText hint;                     // Any hint assigned to this field   
    
    private Bind bind = new Bind();
    private Instance instance = new Instance();
    
    private boolean active   = false;           // Used to determine which field is "active" in form builder navigation
    private boolean repeated = false;           // Whether this is a repeated field (e.g., a child of a <repeat> element.
                                                // This has a bearing on how the resulting XML will be output.
    
    /* 
     * For fields instantiated by the form builder
     */
    public Field()
    {        
    }
    
    // For fields instantiated from entries in <h:body>
    public Field(XMLTag tag, ArrayList<Bind> binds, String instanceRoot, Field parent)
    {
        Log.v(Collect.LOGTAG, t + "created new " + tag.getCurrentTagName() + " field at " + tag.getCurrentTagLocation());
        
        setType(tag.getCurrentTagName());
        setLocation(tag.getCurrentTagLocation());
        
        if (parent != null)
            setParent(parent);
        
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
                // FIXME: does this even happen?
                if (!Pattern.matches("^jr:.*", ref)) {
                    // If the reference is not literal then make it so
                    if (!Pattern.matches("^/.*", ref)) {
                        String newRef = "/" + instanceRoot + "/" + ref;
                        
                        /*
                         * This logic exists to support repeated elements.  We must ensure that their refs
                         * are literal if binds and other items are to be properly associated with them.
                         * 
                         * FIXME: this assumes that the immediate parent of repeated items is a <repeat> field
                         */
                        if (parent != null) {
                            if (parent.getType().equals("repeat")) {
                                setRepeated(true);
                                newRef = parent.getRef() + "/" + ref;
                            }
                        }
                        
                        ref = newRef;                        
                    }

                    Iterator<Bind> it = binds.iterator();                    
                    
                    while (it.hasNext()) {
                        Bind b = it.next();

                        // If a bind with a nodeset identical to this ref exists, associate it with this field
                        if (b.getNodeset().equals(ref)) {
                            setBind(b);
                            Log.v(Collect.LOGTAG, t + "bind with nodeset " + b.getNodeset() + " bound to this field at " + getLocation());
                        }
                    }
                }
                
                setRef(ref);
            }
        }
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    public ArrayList<Field> getChildren()
    {
        return children;
    }

    public void setLabel(String label)
    {
        Log.v(Collect.LOGTAG, t + "setting label for " + type + " at " + location);
        this.label = new FieldText(label);
    }

    /*
     * FIXME
     * We should really be able to return a label for anything that is going to be
     * displayed but include this failsafe here just in case so things don't crash
     * elsewhere if the label is null.
     */
    public String getLabel()
    {
        if (label == null) {
            Log.w(Collect.LOGTAG, t + "label unavailable for field");
            return "";
        } else
            return label.toString();
    }

    public void setHint(String hint)
    {
        Log.v(Collect.LOGTAG, t + "setting hint for " + type + " at " + location);
        this.hint = new FieldText(hint);
    }

    public String getHint()
    {
        if (hint == null) {
            Log.w(Collect.LOGTAG, t + "hint unavailable for field");
            return "";
        } else 
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

    public void setItemValue(String itemValue)
    {
        this.itemValue = itemValue;
    }

    public String getItemValue()
    {
        return itemValue;
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

    public void setActive(boolean active)
    {
        Log.v(Collect.LOGTAG, t + "setting field " + getLabel() + " active state to " + active);
        this.active = active;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setRepeated(boolean repeated)
    {
        this.repeated = repeated;
    }

    public boolean isRepeated()
    {
        return repeated;
    }

    public void setParent(Field parent)
    {
        this.parent = parent;
    }

    public Field getParent()
    {
        return parent;
    }
    
    public void setInstance(Instance instance)
    {
        this.instance = instance;
    }

    public Instance getInstance()
    {
        return instance;
    }
}
