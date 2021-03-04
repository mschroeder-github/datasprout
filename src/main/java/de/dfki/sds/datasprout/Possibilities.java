
package de.dfki.sds.datasprout;

import de.dfki.sds.datasprout.utils.SetUtility;
import de.dfki.sds.rdf2rdb.RdfsAnalyzer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Stack;
import java.util.function.Function;

/**
 * Space of all possibilties.
 * Just a key with a value (flat list).
 * In getSetups the flat lists are used to create subsets (sublists) based on 
 * minMaxMap size settings.
 */
public class Possibilities extends LinkedHashMap<String, List<Object>> {

    //parent relation in dependency tree
    private Map<String, String> dependsOn;
    
    //to decide the allowed subset sizes
    private Map<String, int[]> minMaxMap;
    
    //when fromModel is used this is filled
    //it also contains the original model
    private RdfsAnalyzer rdfsAnalyzer;
    
    public Possibilities() {
        dependsOn = new HashMap<>();
        minMaxMap = new HashMap<>();
    }
    
    public void put(String key, int min, int max, Function<Setup, List<Object>> f) {
        put(key, Arrays.asList(f));
        minMaxMap.put(key, new int[] { min, max });
    }
    
    //put a list of things like [A,B,C], 
    //the subset generates happens during the getSetups
    //so do not put here List in List; a flat list is correct
    public void put(String key, int min, int max, List objects) {
        put(key, objects);
        minMaxMap.put(key, new int[] { min, max });
    }
    
    public void dependsOn(String child, String parent) {
        dependsOn.put(child, parent);
    }
    
    public List<String> getTopologicalSortedKeys() {
        List<String> result = new ArrayList<>();
        List<String> roots = new ArrayList<>();
        List<String> keys = new ArrayList<>(keySet());
        
        //independent ones first
        for(String key : keys.toArray(new String[0])) {
            if(!dependsOn.containsKey(key)) {
                roots.add(key);
                keys.remove(key);
            }
        }
        roots.sort((a,b) -> a.compareTo(b));
        
        Map<String, List<String>> parent2children = new HashMap<>();
        for(Entry<String, String> child2parent : dependsOn.entrySet()) {
            parent2children.computeIfAbsent(child2parent.getValue(), c -> new ArrayList<>()).add(child2parent.getKey());
        }
        
        Stack<String> s = new Stack<>();
        s.addAll(roots);
        while(!s.isEmpty()) {
            String node = s.pop();
            
            result.add(node);
            
            List<String> children = parent2children.get(node);
            if(children != null) {
                for(String child : children) {
                    s.add(child);
                }
            }
        }
        
        return result;
    }
    
    public List<Setup> getSetups(int numberOfWorkbooks, Random rnd) {
        List<Setup> result = new ArrayList<>();
        
        List<Object> classes = this.get(Setup.CLASSES);
        
        List<String> keys = new ArrayList<>(this.keySet());
        keys.remove(Setup.CLASSES);
        keys.sort((a,b) -> a.compareTo(b));
        
        //each workbook will have all classes
        for(int i = 0; i < numberOfWorkbooks; i++) {
            
            //TODO later we should also allow two or more classes in one table
            for(Object cls : classes) {
                
                Setup setup = new Setup();
                setup.put(Setup.CLASSES, Arrays.asList(Arrays.asList(cls)));
                
                for(String key : keys) {
                    List values = this.get(key);
                    
                    if(values.size() > 1) {
                        setup.put(key, Arrays.asList(values.get(rnd.nextInt(values.size()))));
                    } else if(values.size() == 1) {
                        setup.put(key, Arrays.asList(values.get(0)));
                    }
                }
                
                result.add(setup);
            }
            
        }
        
        return result;
    }
    
    public List<Setup> getSetups() {
        List<Setup> result = new ArrayList<>();
        
        List<String> keys = getTopologicalSortedKeys();
        
        for(String key : keys) {
            
            List<Object> options = this.get(key);
            
            //init (first key)
            if(result.isEmpty()) {
                
                List<List<Object>> optionSubsets = subsetsOf(key, options);
                for(List<Object> option : optionSubsets) {
                    
                    Setup setup = new Setup();
                    setup.put(key, option);
                    result.add(setup);
                }
                
            } else {
                
                List<Setup> newOnes = new ArrayList<>();
                
                for(Setup setup : result) {
                    
                    //we allow option subsets so that multiple options are possible in a setup
                    List<List<Object>> optionSubsets;
                    
                    //is it a dependent key
                    if(dependsOn.containsKey(key)) {
                        //options contains the function
                        Function<Setup, List<List<Object>>> f = (Function<Setup, List<List<Object>>>) options.get(0);
                        
                        //the function has to return the subsets
                        optionSubsets = f.apply(setup);
                    } else {
                        optionSubsets = subsetsOf(key, options);
                    }
                    
                    
                    for(int i = 0; i < optionSubsets.size(); i++) {
                        //the options are sets
                        List<Object> option = optionSubsets.get(i);
                        
                        if(i == 0) {
                            //first one
                            setup.put(key, option);
                            
                        } else {
                            //more
                            Setup copy = new Setup(setup);
                            
                            copy.put(key, option);
                            
                            newOnes.add(copy);
                        }
                    }
                }
                
                result.addAll(newOnes);
            }
        }
        
        return result;
    }
    
    private List<List<Object>> subsetsOf(String key, List<Object> options) {
        List<List<Object>> optionSubsets = SetUtility.subsetsAsList(options);
        
        int[] minMax = minMaxMap.getOrDefault(key, new int[] {1,1});
        
        optionSubsets.removeIf(ss -> {
            
            //makes no sense to provide an empty set of options
            if(ss.isEmpty())
                return true;
            
            //cut away the unwanted set sizes
            if(ss.size() < minMax[0] || ss.size() > minMax[1])
                return true;
            
            return false;
        });
        
        return optionSubsets;
    }

    public void setRdfsAnalyzer(RdfsAnalyzer rdfsAnalyzer) {
        this.rdfsAnalyzer = rdfsAnalyzer;
    }
    
    public RdfsAnalyzer getRdfsAnalyzer() {
        return rdfsAnalyzer;
    }
    
    /*
    @Override
    public String toString() {
        String str = super.toString();
        return StringUtility.makeWhitespaceVisible(str);
    }
    */
    
}
