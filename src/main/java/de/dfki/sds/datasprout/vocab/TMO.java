
package de.dfki.sds.datasprout.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * 
 */
public class TMO {

    public static final String NS = "http://www.semanticdesktop.org/ontologies/2008/05/20/tmo#";
    
    public static final Resource Task = ResourceFactory.createResource(NS + "Task");
  
    //Task -> 
    public static final Property taskId = ResourceFactory.createProperty(NS + "taskId");
    public static final Property taskName = ResourceFactory.createProperty(NS + "taskName");
    public static final Property taskDescription = ResourceFactory.createProperty(NS + "taskDescription");
    
    public static final Property contextTask = ResourceFactory.createProperty(NS + "contextTask");
    
    //defined by me
    //Task -> Project
    public static final Property relatedProject = ResourceFactory.createProperty(NS + "relatedProject");
    public static final Property relatedTask = ResourceFactory.createProperty(NS + "relatedTask");
    
    public static final Property dueDate = ResourceFactory.createProperty(NS + "dueDate");
    public static final Property actualStartTime = ResourceFactory.createProperty(NS + "actualStartTime"); //set when worked on
    public static final Property actualEndTime = ResourceFactory.createProperty(NS + "actualEndTime");
    public static final Property targetStartTime = ResourceFactory.createProperty(NS + "targetStartTime");
    public static final Property targetEndTime = ResourceFactory.createProperty(NS + "targetEndTime"); //where is the diffence between targetEndTime and dueDate
    
    //PersonInvolvement
    public static final Resource PersonInvolvement = ResourceFactory.createResource(NS + "PersonInvolvement");
    public static final Property involvedPersonTask = ResourceFactory.createProperty(NS + "involvedPersonTask");
    public static final Property involvedPersons = ResourceFactory.createProperty(NS + "involvedPersons"); //inverse of involvedPersonTask
    public static final Property involvedPersonRole = ResourceFactory.createProperty(NS + "involvedPersonRole");
    public static final Property involvedPerson = ResourceFactory.createProperty(NS + "involvedPerson");
    
    //state
    public static final Resource TaskState = ResourceFactory.createResource(NS + "TaskState");
    public static final Property taskState = ResourceFactory.createProperty(NS + "taskState");
    
    public static final Resource TMO_Instance_TaskState_New = ResourceFactory.createResource(NS + "TMO_Instance_TaskState_New"); //when created
    public static final Resource TMO_Instance_TaskState_Running = ResourceFactory.createResource(NS + "TMO_Instance_TaskState_Running"); //when worked on
    public static final Resource TMO_Instance_TaskState_Suspended = ResourceFactory.createResource(NS + "TMO_Instance_TaskState_Suspended"); //when paused
    public static final Resource TMO_Instance_TaskState_Completed = ResourceFactory.createResource(NS + "TMO_Instance_TaskState_Completed"); //when finished (positive)
    public static final Resource TMO_Instance_TaskState_Terminated = ResourceFactory.createResource(NS + "TMO_Instance_TaskState_Terminated"); //when finished (negative)
    public static final Resource TMO_Instance_TaskState_Deleted = ResourceFactory.createResource(NS + "TMO_Instance_TaskState_Deleted");
    public static final Resource TMO_Instance_TaskState_Archived = ResourceFactory.createResource(NS + "TMO_Instance_TaskState_Archived");
    public static final Resource TMO_Instance_TaskState_Finalized = ResourceFactory.createResource(NS + "TMO_Instance_TaskState_Finalized");
    
    
}
