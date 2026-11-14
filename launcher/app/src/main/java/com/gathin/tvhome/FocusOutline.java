package com.gathin.tvhome;
/** Accept historical setup switches and native string choices without rewriting saved preferences. */
final class FocusOutline {
    static String read(Object value){if(value instanceof Boolean)return (Boolean)value?"double":"rim";return value instanceof String&&valid((String)value)?(String)value:"";}
    static boolean valid(String value){return "".equals(value)||"rim".equals(value)||"double".equals(value);}
    private FocusOutline(){}
}
