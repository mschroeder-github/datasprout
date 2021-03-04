
package de.dfki.sds.datasprout.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * 
 */
public class FOAF {
    
    public static final String NS = "http://xmlns.com/foaf/0.1/";
    
    public static final Resource Person = ResourceFactory.createResource(NS + "Person");
    
    public static final Property name = ResourceFactory.createProperty(NS + "name");
    public static final Property lastName = ResourceFactory.createProperty(NS + "lastName");
    public static final Property homepage = ResourceFactory.createProperty(NS + "homepage");
    public static final Property firstName = ResourceFactory.createProperty(NS + "firstName");
    public static final Property icqChatID = ResourceFactory.createProperty(NS + "icqChatID");
    
    
}
