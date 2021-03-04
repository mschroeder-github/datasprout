
package de.dfki.sds.datasprout.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * 
 */
public class PROV {

    public static final String NS = "http://www.w3.org/ns/prov#";
    
    public static final Resource Entity = ResourceFactory.createResource(NS + "Entity");
    public static final Resource Activity = ResourceFactory.createResource(NS + "Activity");
    public static final Resource Agent = ResourceFactory.createResource(NS + "Agent");
    
    //Entity -> Entity
    public static final Property wasDerivedFrom = ResourceFactory.createProperty(NS + "wasDerivedFrom");
    
}
