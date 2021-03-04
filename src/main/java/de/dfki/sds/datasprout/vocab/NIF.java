
package de.dfki.sds.datasprout.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * 
 */
public class NIF {

    public static final String NS = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";
    
    public static final Resource Context = ResourceFactory.createResource(NS + "Context");
    public static final Resource String = ResourceFactory.createResource(NS + "String");
    public static final Resource Phrase = ResourceFactory.createResource(NS + "Phrase");
    public static final Resource RFC5147String = ResourceFactory.createResource(NS + "RFC5147String");
    
    public static final Property anchorOf = ResourceFactory.createProperty(NS + "anchorOf");
    public static final Property beginIndex = ResourceFactory.createProperty(NS + "beginIndex");
    public static final Property endIndex = ResourceFactory.createProperty(NS + "endIndex");
    public static final Property referenceContext = ResourceFactory.createProperty(NS + "referenceContext");
    public static final Property isString = ResourceFactory.createProperty(NS + "isString");
    
    
}
