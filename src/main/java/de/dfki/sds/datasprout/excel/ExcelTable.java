
package de.dfki.sds.datasprout.excel;

import de.dfki.sds.datasprout.Provenance;
import de.dfki.sds.datasprout.Setup;
import de.dfki.sds.hephaistos.storage.excel.ExcelCell;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * 
 */
public class ExcelTable {
    
    private Map<ExcelCell, Provenance> cellProvMap;
    
    private Setup setup;

    private ExcelCell[][] data;
    
    private IdCounter idCounter;
    
    public ExcelTable() {
        cellProvMap = new HashMap<>();
    }

    public ExcelCell[][] getData() {
        return data;
    }

    public void setData(ExcelCell[][] data) {
        this.data = data;
    }

    public Setup getSetup() {
        return setup;
    }

    public void setSetup(Setup setup) {
        this.setup = setup;
    }
    
    public String toCSV() throws IOException {
        StringWriter sw = new StringWriter();
        CSVPrinter p = CSVFormat.DEFAULT.print(sw);
        
        for(int i = 0; i < data.length; i++) {
            for(int j = 0; j < data[i].length; j++) {
            
                ExcelCell cell = data[i][j];
                
                
                if(cell == null) {
                    p.print("");
                } else {
                    
                    
                    if(cell.getCellType() == null) {
                        throw new RuntimeException("type is null in (" + j + "," + i + ")");
                    }
                    
                    switch(cell.getCellType()) {
                        case "string": p.print(cell.getValueString()); break;
                        case "numeric": p.print(cell.getValueNumeric()); break;
                        case "boolean": p.print(cell.getValueBoolean()); break;
                        default: throw new RuntimeException(cell.getCellType() + " type not implemented");
                    }
                }
            }
            
            p.println();
        }
        
        return sw.toString();
    }
    
    public void addStatement(ExcelCell cell, Resource s, Property p, RDFNode o) {
        cellProvMap.computeIfAbsent(cell, c -> new Provenance()).getStatements().add(ResourceFactory.createStatement(s, p, o));
    }
    
    public void addStatements(ExcelCell cell, Resource s, Property p, List<RDFNode> os) {
        for(RDFNode o : os) {
            addStatement(cell, s, p, o);
        }
    }
    
    public void putUsedPattern(ExcelCell cell, String patternName, Object patternValue) {
        cellProvMap.computeIfAbsent(cell, c -> new Provenance()).getUsedPatterns().put(patternName, patternValue);
    }

    public Map<ExcelCell, Provenance> getCellProvMap() {
        return cellProvMap;
    }

    @Override
    public String toString() {
        return "ExcelCell{" + "(" + data[0].length + ", " + data.length + ") " + setup + "}";
    }

    public void setIdCounter(IdCounter idCounter) {
        this.idCounter = idCounter;
    }
    
    public int getAndIncCellId() {
        return idCounter.getAndIncId();
    }
}
