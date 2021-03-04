
package de.dfki.sds.datasprout.excelgen;

import de.dfki.sds.datasprout.utils.SemanticUtility;
import java.awt.Point;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 
 */
public class ExcelGenerator {

    //input abox.ttl
    
    //output workbook.xlsx + abox.ttl
    
    public ExcelGeneratorResult generate(Model abox, ExcelGeneratorWorkbookConfig... configs) {
        
        ExcelGeneratorResult result = new ExcelGeneratorResult();
        
        /*
        CSVPrinter provenanceCSV;
        try {
            provenanceCSV = CSVFormat.DEFAULT.print(new FileWriter(result.getProvenanceCSV()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        */
        
        for(ExcelGeneratorWorkbookConfig workbookConfig : configs) {
            
            XSSFWorkbook workbook = new XSSFWorkbook();
            
            for(ExcelGeneratorSheetConfig sheetConfig : workbookConfig.getSheetConfigs()) {
                
                XSSFSheet sheet = workbook.createSheet(sheetConfig.getSheetName());
                
                for(ExcelGeneratorTableConfig tableConfig : sheetConfig.getTableConfigs()) {
                    
                    Point offset = tableConfig.getOffset();
                    
                    ExcelGeneratorTableDrawer drawer = new ExcelGeneratorTableDrawer(workbookConfig.getFileName(), workbook, sheet, offset, abox, 
                            //deprecated:
                            null, null, null);
                    //fixed cells like headers
                    if(tableConfig.getStaticCellDrawer() != null) {
                        tableConfig.getStaticCellDrawer().accept(drawer);
                    }
                    
                    String sparqlQuery = tableConfig.getSparqlQuery();
                    if(sparqlQuery != null) {
                        
                        //start points of the table
                        List<Resource> startPoints = SemanticUtility.sparqlr(abox, SemanticUtility.prefixes(abox) + "\n" + sparqlQuery);
                        
                        if(tableConfig.getDynamicLineDrawer() != null) {
                            drawer.lineIndex = 0;
                            for(Resource startPoint : startPoints) {
                                drawer.resource = startPoint;
                                tableConfig.getDynamicLineDrawer().accept(drawer);

                                drawer.lineIndex++;
                            }
                        }
                    }
                    
                }
            }
            
            result.getWorkbooks().add(workbook);
            result.getFileNames().add(workbookConfig.getFileName());
        }
        
        /* deprecated
        try {
            provenanceCSV.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        */
        
        return result;
    }
    
    
    
}
