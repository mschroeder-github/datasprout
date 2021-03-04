
package de.dfki.sds.datasprout.excelgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Explains how the sheet is generated.
 */
public class ExcelGeneratorSheetConfig {
    
    private String sheetName;

    private List<ExcelGeneratorTableConfig> tableConfigs;
    
    public ExcelGeneratorSheetConfig() {
        tableConfigs = new ArrayList<>();
    }
    
    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public List<ExcelGeneratorTableConfig> getTableConfigs() {
        return tableConfigs;
    }

    public void setTableConfigs(List<ExcelGeneratorTableConfig> tableConfigs) {
        this.tableConfigs = tableConfigs;
    }

}
