
package de.dfki.sds.datasprout.excelgen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 
 */
public class ExcelGeneratorResult { 
    
    private List<XSSFWorkbook> workbooks;
    private List<String> fileNames;
    
    
    public ExcelGeneratorResult() {
        workbooks = new ArrayList<>();
        fileNames = new ArrayList<>();
    }
    
    public List<XSSFWorkbook> getWorkbooks() {
        return workbooks;
    }

    public void setWorkbooks(List<XSSFWorkbook> workbooks) {
        this.workbooks = workbooks;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }
    
    public void saveExcel(File folder) {
        folder.mkdirs();
        int index = 0;
        for(XSSFWorkbook workbook : workbooks) {
            File wbFile = new File(folder, fileNames.get(index));
            try(FileOutputStream fos = new FileOutputStream(wbFile)) {
                workbook.write(fos);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
            index++;
        }
    }
    
}
