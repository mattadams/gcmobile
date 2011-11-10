package com.radicaldynamic.groupinform.xform;

/*
 * This class should contain all of the string fragments that could be
 * encountered in an XForm file (attributes, attribute values, etc)
 */
public class XForm
{
    public static class Attribute
    {
        public static final String APPEARANCE = "appearance";
        public static final String ID = "id";
        public static final String JR_TEMPLATE = "jr:template";
        public static final String LANGUAGE = "lang";
        public static final String NODESET = "nodeset";
        public static final String MEDIA_TYPE = "mediatype";
        public static final String REFERENCE = "ref";
        public static final String XML_NAMESPACE = "xmlns";
    }

    public static class Element
    {

    }

    public static class Value
    {
        public static final String AUTOCOMPLETE = "autocomplete";
        public static final String COMPACT = "compact";
        public static final String COMPACT_QUICK = "compact quick";
        public static final String FIELD_LIST = "field-list";
        public static final String LABEL = "label";
        public static final String LIST = "list";
        public static final String LIST_NOLABEL = "list-nolabel";
        public static final String MAP = "maps";
        public static final String MINIMAL = "minimal";
        public static final String QUICK = "quick";        
    }
}
