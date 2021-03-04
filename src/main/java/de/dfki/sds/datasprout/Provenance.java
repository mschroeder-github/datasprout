
package de.dfki.sds.datasprout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;

/**
 * Stores what statements were involved and what patterns were used.
 */
public class Provenance {
    
    //the used pattern
    //key is pattern name and value is the actual value
    private Map<String, Object> usedPatterns;
    
    //the statements that can be found
    private List<Statement> statements;
    
    public Provenance() {
        statements = new ArrayList<>();
        usedPatterns = new HashMap<>();
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public Map<String, Object> getUsedPatterns() {
        return usedPatterns;
    }
    
    public Model getModel() {
        Model m = ModelFactory.createDefaultModel();
        m.add(statements);
        return m;
    }
    
}
