
package de.dfki.sds.datasprout.excel;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

/**
 * A property config for a certain column.
 */
public class PropertyConfig {

    private List<Property> properties;
    private List<RDFNode> objects;
    
    private boolean distinctObjects;
    
    //we set this to true so that we know when multiple properties are used
    //List<Property> properties
    //we have to apply partial formatting to it to distinguish them
    private boolean partialFormattingNeeded;
    
    //if this label is filled, it will be used for column name
    private String label;
    
    //use the uri to refer to it in the pattern key
    private String uri;

    private PropertyConfig() {
        this.properties = new ArrayList<>();
        this.objects = new ArrayList<>();
        distinctObjects = true;
    }
    
    public PropertyConfig(Property property) {
        this();
        properties.add(property);
    }
    
    public PropertyConfig(Property property, RDFNode object) {
        this();
        properties.add(property);
        objects.add(object);
    }

    public PropertyConfig(List<Property> properties) {
        this();
        this.properties = properties;
    }
    
    public String getLabel() {
        return label;
    }

    public PropertyConfig setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * If the config has no manual entered URI and only one property, we
     * return the URI of the property.
     * @return 
     */
    public String getUri() {
        if(uri == null && properties.size() == 1) {
            return properties.get(0).getURI();
        }
        
        if(uri == null) {
            throw new RuntimeException("set the uri in the PropertyConfig");
        }
            
        
        return uri;
    }

    public PropertyConfig setUri(String uri) {
        this.uri = uri;
        return this;
    }
    
    public boolean hasUri() {
        return uri != null;
    }
    
    public RDFNode getObject() {
        return objects.get(0);
    }

    public List<RDFNode> getObjects() {
        return objects;
    }
    
    public Property getProperty() {
        return properties.get(0);
    }

    public List<Property> getProperties() {
        return properties;
    }
    
    public boolean hasObject() {
        return !objects.isEmpty();
    }
    
    public boolean hasLabel() {
        return label != null;
    }
    
    public boolean isMulti() {
        return properties.size() > 1;
    }

    public boolean isDistinctObjects() {
        return distinctObjects;
    }

    public PropertyConfig setDistinctObjects(boolean distinctObjects) {
        this.distinctObjects = distinctObjects;
        return this;
    }

    public boolean isPartialFormattingNeeded() {
        return partialFormattingNeeded;
    }

    public PropertyConfig setPartialFormattingNeeded(boolean partialFormattingNeeded) {
        this.partialFormattingNeeded = partialFormattingNeeded;
        return this;
    }

    @Override
    public String toString() {
        return "PropertyConfig{" + properties + ", label=" + label + "}";
    }
    
    
    
}
