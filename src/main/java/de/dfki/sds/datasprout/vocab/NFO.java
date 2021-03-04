
package de.dfki.sds.datasprout.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * 
 */
public class NFO {

    public static final String NS = "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#";
    
    public static final Resource Filesystem = ResourceFactory.createResource(NS + "Filesystem"); //subClassOf DataContainer
    public static final Resource Folder = ResourceFactory.createResource(NS + "Folder"); //subClassOf DataContainer
    public static final Resource FileDataObject = ResourceFactory.createResource(NS + "FileDataObject");
    
    public static final Property fileName = ResourceFactory.createProperty(NS + "fileName");
    /**
     * Models the containment relations between Files and Folders (or CompressedFiles).
     * DataObject -> DataContainer
     */
    public static final Property belongsToContainer = ResourceFactory.createProperty(NS + "belongsToContainer");
    
    
}
