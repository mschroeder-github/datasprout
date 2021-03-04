
package de.dfki.sds.datasprout.excelgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Explains how the workbook is generated.
 */
public class ExcelGeneratorWorkbookConfig {
    
    private List<ExcelGeneratorSheetConfig> sheetConfigs;
    private String fileName;

    public ExcelGeneratorWorkbookConfig() {
        sheetConfigs = new ArrayList<>();
    }
    
    public List<ExcelGeneratorSheetConfig> getSheetConfigs() {
        return sheetConfigs;
    }

    public void setSheetConfigs(List<ExcelGeneratorSheetConfig> sheetConfigs) {
        this.sheetConfigs = sheetConfigs;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
}
