
package de.dfki.sds.datasprout.excel;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.rdf.model.Resource;

/**
 * A class config for a certain table.
 * A table can be configured with multiple classes.
 */
public class ClassConfig {

    private List<Resource> classes;

    private String label;
    
    private ClassConfig() {
        this.classes = new ArrayList<>();
    }
    
    public ClassConfig(Resource cls) {
        this();
        this.classes.add(cls);
    }
    
    public ClassConfig(List<Resource> classes) {
        this();
        this.classes.addAll(classes);
    }

    public Resource getSingleClass() {
        return classes.get(0);
    }
    
    public List<Resource> getClasses() {
        return classes;
    }

    public String getLabel() {
        return label;
    }

    public ClassConfig setLabel(String label) {
        this.label = label;
        return this;
    }
    
    public boolean hasLabel() {
        return label != null;
    }

    @Override
    public String toString() {
        return "ClassConfig{" + classes + ", label=" + label + "}";
    }
    
    
    
}
