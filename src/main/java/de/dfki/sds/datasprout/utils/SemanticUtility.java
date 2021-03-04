package de.dfki.sds.datasprout.utils;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 *
 */
public class SemanticUtility {

    public static <T> Iterable<T> iterable(ExtendedIterator<T> extIter) {
        return () -> {
            return extIter;
        };
    }

    public static List<QuerySolution> sparql(Model model, String query) {
        StringBuilder prefixes = new StringBuilder();

        for (Map.Entry<String, String> e : model.getNsPrefixMap().entrySet()) {
            prefixes.append("PREFIX ").append(e.getKey()).append(":").append(" <").append(e.getValue()).append(">");
            prefixes.append("\n");
        }

        QueryExecution exec = QueryExecutionFactory.create(prefixes.toString() + query, model);
        return ResultSetFormatter.toList(exec.execSelect());
    }

    public static List<Resource> sparqlr(Model model, String query) {
        List<Resource> result = new ArrayList<>();
        List<QuerySolution> l = sparql(model, query);
        for (QuerySolution qs : l) {
            result.add(qs.getResource(qs.varNames().next()));
        }
        return result;
    }

    public static Iterable<Statement> list(Model model, Resource s, Property p, RDFNode o) {
        return iterable(model.listStatements(s, p, o));
    }

    //can be null
    public static Literal literal(Model model, Resource s, Property p) {
        try {
            return model.getRequiredProperty(s, p).getLiteral();
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Literal literalOrEmptyString(Model model, Resource s, Property p) {
        try {
            return model.getRequiredProperty(s, p).getLiteral();
        } catch (Exception e) {
            return model.createLiteral("");
        }
    }
    
    public static Literal longestLiteral(Model model, Resource s, Property p) {
        List<Statement> stmts = model.listStatements(s, p, (RDFNode)null).toList();
        if(stmts.isEmpty())
            return null;
        
        Statement longest = stmts.get(0);
        for(Statement stmt : stmts) {
            if(stmt.getObject().isLiteral()) {
                if(longest.getObject().asLiteral().getString().length() < 
                   stmt.getObject().asLiteral().getString().length()) {
                    
                    longest = stmt;
                }
            }
        }
        
        return longest.getLiteral();
    }
    
    public static Statement statement(Model model, Resource s, Property p) {
        List<Statement> stmts = model.listStatements(s, p, (RDFNode) null).toList();
        if(stmts.isEmpty())
            return null;
        return stmts.get(0);
    }

    public static String describe(Model model, Resource s) {
        Model m = model.query(new SimpleSelector(s, null, (RDFNode) null));
        return toTTL(m);
    }
    
    public static String toTTL(Model model) {
        return toTTL(model, true);
    }
    
    public static String toTTL(Model model, boolean withPrefixHeader) {
        StringWriter sw = new StringWriter();
        model.write(sw, "TTL");
        String code = sw.toString().trim();
        
        if(!withPrefixHeader) {
            //remove @prefix
            while(code.startsWith("@prefix")) {
                int i = code.indexOf("\n");
                if(i == -1)
                    break;
                code = code.substring(i+1);
            }
            code = code.trim();
        }
        
        return code;
    }
    
    public static RDFNode rdfnode(Model model, Resource s, Property p) {
        try {
            return model.getRequiredProperty(s, p).getObject();
        } catch (Exception e) {
            return null;
        }
    }
    
    public static List<RDFNode> rdfnodes(Model model, Resource s, Property p) {
        try {
            return model.listObjectsOfProperty(s, p).toList();
        } catch (Exception e) {
            return null;
        }
    }
    
    public static List<Resource> objects(Model model, Resource s, Property p) {
        List<Resource> l = new ArrayList<>();
        for(RDFNode node : rdfnodes(model, s, p)) {
            if(node.isResource()) {
                l.add(node.asResource());
            }
        }
        return l;
    }
    
    //can be null
    public static Resource resource(Model model, Resource s, Property p) {
        try {
            return model.getRequiredProperty(s, p).getResource();
        } catch (Exception e) {
            return null;
        }
    }

    public static String prefixes(Model model) {
        StringBuilder prefixes = new StringBuilder();
        for (Map.Entry<String, String> e : model.getNsPrefixMap().entrySet()) {
            prefixes.append("PREFIX ").append(e.getKey()).append(":").append(" <").append(e.getValue()).append(">");
            prefixes.append("\n");
        }
        return prefixes.toString();
    }
    
    public static List<Resource> toResourceList(List<Property> ps) {
        List<Resource> resList = new ArrayList<>();
        for(Property p : ps) {
            resList.add(p.asResource());
        }
        return resList;
    }
    
    /**
     * Returns http://.../[localname] or http://...#[localname] of URI.
     * @param uri
     * @return
     */
    public static String getLocalName(String uri) {
        int a = uri.lastIndexOf("/");
        int b = uri.lastIndexOf("#");
        int m = Math.max(a, b);
        if(m == -1)
            return null;
        
        return uri.substring(m+1, uri.length());
    }
    
    public static Literal toXsdDate(LocalDate ld) {
        return ResourceFactory.createTypedLiteral(ld.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), XSDDatatype.XSDdate);
    }
    
    public static Literal toXsdDateTime(LocalDateTime ldt) {
        return ResourceFactory.createTypedLiteral(ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), XSDDatatype.XSDdateTime);
    }
    
}
