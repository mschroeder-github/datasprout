package de.dfki.sds.datasprout.utils;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class StringUtility {
    
    public static String makeWhitespaceVisible(String s) {
        if(s == null)
            return null;
        
        if(s.isEmpty())
            return "";
        
        return s.replace(" ", "␣").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r");
    }
    
    public static String toProperCase(String s) {
        if (s == null) {
            return null;
        }

        if (s.length() == 0) {
            return s;
        }

        if (s.length() == 1) {
            return s.toUpperCase();
        }

        return s.substring(0, 1).toUpperCase()
                + s.substring(1).toLowerCase();
    }
    
    public static String toAcronym(String str) {
        return str.replaceAll("\\B.|\\P{L}", "").toUpperCase();
    }
}
