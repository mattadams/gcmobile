package com.radicaldynamic.groupinform.xform;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
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
    
    // For select or select1 fields (also see "children" attribute)
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
        final String tt = t + "Field(): ";

        if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, tt + "created new " + tag.getCurrentTagName() + " field at " + tag.getCurrentTagLocation());
        
        setType(tag.getCurrentTagName());
        setLocation(tag.getCurrentTagLocation());
        
        if (parent != null) {
            setParent(parent);
        }
        
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
            if (s.equals(XForm.Attribute.REFERENCE) || s.equals(XForm.Attribute.NODESET) || s.equals(XForm.Attribute.BIND)) {
                String xpath = tag.getAttribute(s);
                
                // If this reference is not to an itext translation then it must be to an instance/bind
                if (Pattern.matches("^jr:.*", xpath)) {
                    // FIXME: is this sanity check required?
                } else {
                    // If the reference is not literal then make it so
                    if (!Pattern.matches("^/.*", xpath)) {
                        String ref = determineXPath(parent, instanceRoot, xpath);

                        if (ref.length() > 0) {
                            if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, tt + "changed non-literal XPath from " + xpath + " to " + ref);
                            xpath = ref;
                        }
                    }

                    /*
                     * Iterate through the known list of binds and form a relationship 
                     * with those that have the same XPath as the current field.
                     */
                    Iterator<Bind> it = binds.iterator();                    
                    
                    while (it.hasNext()) {
                        Bind b = it.next();

                        // If a bind with a nodeset identical to this ref exists, associate it with this field
                        if (b.getXPath().equals(xpath)) {
                            if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, t + "bind with nodeset " + b.getXPath() + " associated to field at " + getLocation());                            
                            setBind(b);
                            
                            // Not all binds will have an associated type but our code (may) expect them to
                            if (b.getType() == null) {
                                if (Collect.Log.WARN) Log.w(Collect.LOGTAG, t + "bind for " + b.getXPath() + " missing an explicit type (setting to string if input)");
                                
                                if (getType().equals("input"))
                                    b.setType("string");
                            }

                            // No point in looking further, right?
                            break;
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
        if (type.equals("group") && children.size() == 1 && children.get(0).getType().equals("repeat")) {
            return children.get(0);
        } else {
            return null;
        }
    }

    public void setLabel(String label)
    {
        if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, t + "setting label for " + type + " at " + location);
        this.label = new FieldText(label);
    }

    // If you want a human readable textual string then you need to perform .toString() on the result
    public FieldText getLabel()
    {
        return label;
    }
    
    public void setHint(String hint)
    {
        if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, t + "setting hint for " + type + " at " + location);
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
    public boolean hasXPath() { if (xpath != null && xpath.length() > 0) return true; else return false; }

    public void setBind(Bind bind) { this.bind = bind; }
    public Bind getBind() { return bind; }

    public void setActive(boolean active) { this.active = active; }
    public boolean isActive() { return active; }
    
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
     * Traverse the tree upwards until an XPath can be determined 
     */
    private String determineXPath(Field parent, String instanceRoot, String xpath)
    {
        if (parent == null) {
            xpath = File.separator + instanceRoot + File.separator + xpath;
        } else {
            if (Field.isRepeatedGroup(parent)) {
                xpath = parent.getRepeat().getXPath() + File.separator + xpath;
            } else {
                xpath = determineXPath(parent.getParent(), instanceRoot, xpath);
            }
        }

        return xpath;
    }

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
     */
    public static String makeFieldName(FieldText label)
    {
        final StringTokenizer st = new StringTokenizer(label.toString(), " ", true);
        final StringBuilder sb = new StringBuilder();
        String name = "";
         
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            token = String.format("%s%s", Character.toUpperCase(token.charAt(0)), token.substring(1));
            sb.append(token);
        }
        
        name = sb.toString().replaceAll("\\s", "").replaceAll("[^a-zA-Z0-9]", "");
        
        // Just in case the label did not have anything in it from which to generate a sane field name
        if (name.length() == 0) {
            if (Collect.Log.WARN) Log.w(Collect.LOGTAG, t 
                    + "unable to construct field name from getLabel().toString() of " 
                    + label.toString());
            
            // Get rid of - characters (not valid in XML tag names)
            name = UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", "");
        }
        
        return name;
    }
}
