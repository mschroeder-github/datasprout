package de.dfki.sds.datasprout.utils;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import org.json.JSONObject;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class JsonUtility {
    
    public static void forceLinkedHashMap(JSONObject json) {
        try {
            Field map = json.getClass().getDeclaredField("map");
            map.setAccessible(true);
            map.set(json, new LinkedHashMap<>());
            map.setAccessible(false);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
