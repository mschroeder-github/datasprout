
package de.dfki.sds.datasprout.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * the CSVW Namespace Vocabulary Terms and Term definitions used for creating 
 * Metadata descriptions for Tabular Data.
 */
public class CSVW {

    public static final String NS = "http://www.w3.org/ns/csvw#";
    
    public static final Resource Cell = ResourceFactory.createResource(NS + "Cell");
    
    public static final Property aboutUrl = ResourceFactory.createProperty(NS + "aboutUrl");
    
    //TODO put also the other vocabulary in it, visit https://www.w3.org/ns/csvw
}
