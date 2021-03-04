
package de.dfki.sds.datasprout.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Spreadsheet Ontology.
 */
public class SS {

    public static final String NS = "http://www.dfki.uni-kl.de/~mschroeder/ld/ss#";
    
    public static final Resource Workbook = ResourceFactory.createResource(NS + "Workbook");
    
    public static final Property sheetName = ResourceFactory.createProperty(NS + "sheetName");
    
    public static final Property address = ResourceFactory.createProperty(NS + "address");
    
}
