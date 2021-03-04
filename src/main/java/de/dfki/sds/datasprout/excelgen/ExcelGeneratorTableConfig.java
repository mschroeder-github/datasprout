
package de.dfki.sds.datasprout.excelgen;

import java.awt.Point;
import java.util.function.Consumer;

/**
 * Explains how the table is generated.
 */
public class ExcelGeneratorTableConfig {
    
    //decide where the table should start in the sheet
    private Point offset;
    
    //use a sparql query to get the main resources that will be listed in the table
    //here you can find, filter and sort them like you want
    //just return one resource in the SELECT header
    private String sparqlQuery;
    
    //use this to draw all cells that are not dependent to the data
    private Consumer<ExcelGeneratorTableDrawer> staticCellDrawer;
    
    //use this to draw all lines that are dependent to the data
    //each line a new resource is used
    private Consumer<ExcelGeneratorTableDrawer> dynamicLineDrawer;

    public String getSparqlQuery() {
        return sparqlQuery;
    }

    public void setSparqlQuery(String sparqlQuery) {
        this.sparqlQuery = sparqlQuery;
    }

    
    public Point getOffset() {
        if(offset == null)
            return new Point(0,0);
        
        return offset;
    }

    public void setOffset(Point offset) {
        this.offset = offset;
    }

    public Consumer<ExcelGeneratorTableDrawer> getStaticCellDrawer() {
        return staticCellDrawer;
    }

    public void setStaticCellDrawer(Consumer<ExcelGeneratorTableDrawer> staticCellDrawer) {
        this.staticCellDrawer = staticCellDrawer;
    }

    public Consumer<ExcelGeneratorTableDrawer> getDynamicLineDrawer() {
        return dynamicLineDrawer;
    }

    public void setDynamicLineDrawer(Consumer<ExcelGeneratorTableDrawer> dynamicLineDrawer) {
        this.dynamicLineDrawer = dynamicLineDrawer;
    }
    
}
