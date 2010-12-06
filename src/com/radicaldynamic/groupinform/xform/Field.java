package com.radicaldynamic.groupinform.xform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import com.mycila.xmltool.XMLTag;
import com.radicaldynamic.groupinform.application.Collect;

import android.util.Log;

public class Field
{
    private static String t = "Field: ";
    
    // Any attributes found on this element
    private Map<String, String> attributes = new HashMap<String, String>();
    // Any children (other fields) of this one, e.g., groups and repeats (or items for a select or select1 field)
    private ArrayList<Field> children = new ArrayList<Field>();    
    
    private String type;                        // The XML element name of this field  (e.g., group, input, etc.)
    private String location;                    // The XML element location of this node (e.g., *[2]/*[1])
    private String xpath;                       // Value of the "ref" or "nodeset" attribute (if any)
    
    private Field parent;                       // The parent of this field item (null if at top of form hierarchy)
    
    private String itemValue = "";              // Any value assigned to this node (if it is an item)
    private boolean itemDefault = false;        // Whether this value should be preselected
    
    private FieldText label = new FieldText();  // Any label assigned to this field
    private FieldText hint = new FieldText();   // Any hint assigned to this field 
    
    private Bind bind = new Bind();
    private Instance instance = new Instance();
    
    private boolean active   = false;           // Used to determine which field is "active" in form builder navigation               
    private boolean empty    = false;           // This is an "empty" or new field and requires further initialization
    private boolean newField = false;           // Whether this field is new and should be added to the (control) field state list
    private boolean saved    = false;           // Whether changes to a field that has been loaded into the field editor were saved
    
    /* 
     * For fields instantiated by the form builder
     */
    public Field()
    {
        empty = true;
        newField = true;
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
                String xpath = tag.getAttribute(s);
                
                // If this reference is not to an itext translation then it must be to an instance/bind
                // FIXME: does this even happen?
                if (!Pattern.matches("^jr:.*", xpath)) {
                    // If the reference is not literal then make it so
                    if (!Pattern.matches("^/.*", xpath)) {
                        String newRef = "/" + instanceRoot + "/" + xpath;
                        
                        /*
                         * This logic exists to support repeated elements.  We must ensure that their refs
                         * are literal if binds and other items are to be properly associated with them.
                         * 
                         * FIXME: this assumes that the immediate parent of repeated items is a <repeat> field
                         */
                        if (parent != null) {
                            if (parent.getType().equals("repeat")) {
                                newRef = parent.getXPath() + "/" + xpath;
                            }
                        }
                        
                        xpath = newRef;                        
                    }

                    Iterator<Bind> it = binds.iterator();                    
                    
                    while (it.hasNext()) {
                        Bind b = it.next();

                        // If a bind with a nodeset identical to this ref exists, associate it with this field
                        if (b.getXPath().equals(xpath)) {
                            Log.v(Collect.LOGTAG, t + "bind with nodeset " + b.getXPath() + " associated to field at " + getLocation());                            
                            setBind(b);
                            
                            // Not all binds will have an associated type but our code (may) expect them to
                            if (b.getType() == null) {
//                                if (getType().equals("input"))
//                                    b.setType("string");
//                                else 
//                                    b.setType(getType());
                                
                                Log.w(Collect.LOGTAG, t + "bind for " + b.getXPath() + " missing an explicit type");
                            }
                        }
                    }
                }
                
                setXPath(xpath);
            }
        }
    }

    public Map<String, String> getAttributes() { return attributes; }

    public ArrayList<Field> getChildren() { return children; }
    
    /* 
     * If this field is a group, containing exactly one field which is a repeat then return it.
     * Else, return null.  This should only be used on fields that are known to be repeated groups.
     */
    public Field getRepeat()
    {
        if (type.equals("group")
                && children.size() == 1 
                && children.get(0).getType().equals("repeat"))
            return children.get(0);
        else
            return null;
    }

    public void setLabel(String label)
    {
        Log.v(Collect.LOGTAG, t + "setting label for " + type + " at " + location);
        this.label = new FieldText(label);
    }

    // If you want a human readable textual string then you need to perform .toString() on the result
    public FieldText getLabel()
    {
        return label;
    }
    
    public void setHint(String hint)
    {
        Log.v(Collect.LOGTAG, t + "setting hint for " + type + " at " + location);
        this.hint = new FieldText(hint);
    }

    // If you want a human readable textual string then you need to perform .toString() on the result
    public FieldText getHint()
    {
        return hint;
    }
    
    public void setType(String type) { this.type = type; }
    public String getType() { return type; }

    public void setLocation(String location) { this.location = location; }
    public String getLocation() { return location; }

    public void setItemValue(String itemValue) { this.itemValue = itemValue; }
    public String getItemValue() { return itemValue; }
    
    public void setItemDefault(boolean itemDefault) { this.itemDefault = itemDefault; }
    public boolean isItemDefault() { return itemDefault; }
    
    public void setXPath(String xpath) { this.xpath = xpath; }
    public String getXPath() { return xpath; }

    public void setBind(Bind bind) { this.bind = bind; }
    public Bind getBind() { return bind; }

    public void setActive(boolean active)
    {
        Log.v(Collect.LOGTAG, t + "setting field " + getLabel() + " active state to " + active);
        this.active = active;
    }

    public boolean isActive()
    {
        return active;
    }
    
    public void setParent(Field parent) { this.parent = parent; }
    public Field getParent() { return parent; }
    
    public void setInstance(Instance instance) { this.instance = instance; }
    public Instance getInstance() { return instance; }

    public void setEmpty(boolean empty) { this.empty = empty; }
    public boolean isEmpty() { return empty; }

    public void setSaved(boolean saved) { this.saved = saved; }
    public boolean isSaved() { return saved; }

    public void setNewField(boolean newField) { this.newField = newField; }
    public boolean isNewField() { return newField; }    
    
    /*
     * Returns true if the field it has been passed is a repeated group, otherwise false
     */
    public static boolean isRepeatedGroup(Field f)
    {
        if (f != null
                && f.getType().equals("group") 
                && f.getChildren().size() == 1 
                && f.getChildren().get(0).getType().equals("repeat"))
            return true;
        else
            return false;        
    }
    
    /*
     * Creates a suitable instance field name from the label.  This should only be used on new fields.
     * 
     * TODO: the user should probably be aware if this method returns false
     */
    public static String makeFieldName(FieldText label)
    {
        String instanceFieldName = label.toString().replaceAll("\\s", "").replaceAll("[^a-zA-Z0-9]", "");
        
        // Just in case the label did not have anything in it from which to generate a sane field name
        if (instanceFieldName.length() == 0) {
            Log.i(Collect.LOGTAG, t 
                    + "unable to construct field name from getLabel().toString() of " 
                    + label.toString());
            
            // Get rid of - characters (not valid in XML tag names)
            instanceFieldName = UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", "");
        }
        
        return instanceFieldName;
    }
}
