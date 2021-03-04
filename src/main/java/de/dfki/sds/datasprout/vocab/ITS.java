
package de.dfki.sds.datasprout.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * 
 */
public class ITS {

    public static final String NS = "http://www.w3.org/2005/11/its/rdf#";
    
    public static final Property taIdentRef = ResourceFactory.createProperty(NS + "taIdentRef");
    
}
