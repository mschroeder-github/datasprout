
package de.dfki.sds.datasprout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

/**
 * One setup creates one data entity (for excel it's a table).
 */
public class Setup extends LinkedHashMap<String, Object> {
    
    public static final String CLASSES = "classes";
    public static final String PROPERTIES = "properties";
    public static final String LABEL_PROPERTIES = "LabelProperties";
    public static final String RANDOM = "random";
    public static final String INSTANCE_FILTER = "instanceFilter";
    public static final String RDFS_ANALYZER = "rdfsAnalyzer";

    private Map<String, EnumeratedDistribution> distribution;
    
    public Setup() {
        distribution = new HashMap<>();
    }

    public Setup(Map<? extends String, ? extends Object> map) {
        super(map);
        distribution = new HashMap<>();
    }

    public Object getSingle(String key) {
        Object v = get(key);
        if(v instanceof List) {
            List l = (List)v;
            if(l.size() != 1) {
                throw new RuntimeException("getSingle(" + key + ") but list has size " + l.size());
            }
            return l.get(0);
        }
        return v;
    }
    
    public <T> T getOrThrow(String key, Class<T> type) {
        if(!containsKey(key)) {
            throw new RuntimeException(key + " not found");
        }
        Object value = get(key);
        return (T) value;
    }
    
    public <T> T getSingleOrThrow(String key, Class<T> type) {
        if(!containsKey(key)) {
            throw new RuntimeException(key + " not found");
        }
        return (T) getSingle(key);
    }

    @Override
    public Object put(String key, Object value) {
        Object obj = super.put(key, value);
        
        if(value instanceof List) {
            List list = (List) value;
            if(!list.isEmpty()) {
                double[] dArray = new double[list.size()];
                for(int i = 0; i < dArray.length; i++) {
                    dArray[i] = 1.0 / dArray.length;
                }
                putDistribution(key, dArray);
            }
        }
        
        return obj;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        for(Entry e : m.entrySet()) {
            this.put((String) e.getKey(), e.getValue());
        }
    }
    
    
    
    
    public void put(String key, Object value, double... distribution) {
        super.put(key, value);
        putDistribution(key, distribution);
    }
    
    public void putDistribution(String key, double... distribution) {
        
        List values = (List) get(key);
        
        if(values.size() != distribution.length) {
            throw new RuntimeException("there are " + values.size() + " values but " + distribution.length + " distribution values");
        }
        
        List<Pair<Object, Double>> pmf = new ArrayList<>();
        for(int i = 0; i < distribution.length; i++) {
            pmf.add(new Pair(values.get(i), distribution[i]));
        }
        EnumeratedDistribution ed = new EnumeratedDistribution(pmf);
                
        this.distribution.put(key, ed);
    }
    
    public Object getByDistribution(String key) {
        
        //if single just return one
        Object v = get(key);
        if(v instanceof List) {
            List l = (List)v;
            if(l.size() == 1) {
                return l.get(0);
            }
        } else {
            return v;
        }
        
        //not single, need distribution
        if(!distribution.containsKey(key)) {
            throw new RuntimeException("for " + key + " is no distribution defined, use putDistribution");
        }
        
        EnumeratedDistribution ed = distribution.get(key);
        return ed.sample();
    }
    
    /*
    @Override
    public String toString() {
        String str = super.toString();
        return StringUtility.makeWhitespaceVisible(str);
    }
    */
}
