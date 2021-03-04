
package de.dfki.sds.datasprout.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * 
 */
public class PIMO {

    public static final String NS = "http://www.semanticdesktop.org/ontologies/2007/11/01/pimo#";
    
    public static final Resource Project = ResourceFactory.createResource(NS + "Project");
    public static final Resource Person = ResourceFactory.createResource(NS + "Person"); 
    
    public static final Property dtstart = ResourceFactory.createProperty(NS + "dtstart");
    public static final Property dtend = ResourceFactory.createProperty(NS + "dtend");
    
    
    public static final Property memberOf = ResourceFactory.createProperty(NS + "memberOf");
    
    //defined by me
    //to have a boolean value
    public static final Property isResearchProject = ResourceFactory.createProperty(NS + "isResearchProject");
    
    //defined by me
    public static final Property acronym = ResourceFactory.createProperty(NS + "acronym");
    
    //from CoMem
    //Person -> Project
    public static final Property does = ResourceFactory.createProperty(NS + "does");
    public static final Property isDoneBy = ResourceFactory.createProperty(NS + "isDoneBy");
    
    //defined by me
    //Person -> Project
    public static final Property did = ResourceFactory.createProperty(NS + "did");
    public static final Property wasDoneBy = ResourceFactory.createProperty(NS + "wasDoneBy");
    
    //from CoMem
    //Person -> Project
    public static final Property manages = ResourceFactory.createProperty(NS + "manages");
    public static final Property isManagedBy = ResourceFactory.createProperty(NS + "isManagedBy");
    
    //defined by me
    //Person -> Project
    public static final Property managed = ResourceFactory.createProperty(NS + "managed");
    public static final Property wasManagedBy = ResourceFactory.createProperty(NS + "wasManagedBy");
    
    //Person -> Meeting
    public static final Property attends = ResourceFactory.createProperty(NS + "attends");
    public static final Property attendee = ResourceFactory.createProperty(NS + "attendee");
    
    //from CoMem
    //Person -> Meeting
    public static final Property isOrganizedBy = ResourceFactory.createProperty(NS + "isOrganizedBy");
    public static final Property organizes = ResourceFactory.createProperty(NS + "organizes");
    
    //Thing -> Tag
    public static final Property hasTag = ResourceFactory.createProperty(NS + "hasTag");
    public static final Property isTagFor = ResourceFactory.createProperty(NS + "isTagFor");
    
    
}
