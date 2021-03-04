
package de.dfki.sds.datasprout.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * 
 */
public class OA {

    public static final String NS = "http://www.w3.org/ns/oa#";
    
    public static final Resource Annotation = ResourceFactory.createResource(NS + "Annotation");
    public static final Resource SpecificResource = ResourceFactory.createResource(NS + "SpecificResource");
    
    public static final Property hasTarget = ResourceFactory.createProperty(NS + "hasTarget");
    public static final Property hasBody = ResourceFactory.createProperty(NS + "hasBody");
    public static final Property hasSource = ResourceFactory.createProperty(NS + "hasSource");
    
    
}
