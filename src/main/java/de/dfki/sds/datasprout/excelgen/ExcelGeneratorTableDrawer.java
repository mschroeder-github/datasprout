package de.dfki.sds.datasprout.excelgen;

import de.dfki.sds.datasprout.Provenance;
import de.dfki.sds.datasprout.excel.ExcelSproutOptions;
import de.dfki.sds.datasprout.excel.ExcelTable;
import de.dfki.sds.datasprout.excelgen.FontStyle.FontStyleParseResult;
import de.dfki.sds.datasprout.utils.SemanticUtility;
import de.dfki.sds.hephaistos.storage.excel.ExcelCell;
import java.awt.Point;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.apache.commons.csv.CSVPrinter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.PrefixMapping;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Main interface to write something in a cell.
 */
public class ExcelGeneratorTableDrawer {

    private String workbookFileName;
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private Point offset;
    private Model abox;
    private Model provenanceModel;
    private Model expectedModel;
    private CSVPrinter provenanceCSV;


    //the resource for the line
    /*package*/ Resource resource;
    /*package*/ int lineIndex;

    private CreationHelper creationHelper;

    private short defaultFontHeightInPoints = 10;
    private String defaultFontName = "Liberation Sans";
    
    //used in rdfProvenance
    private Map<Statement, Resource> stmt2res = new HashMap<>();
    
    //IllegalStateException: The maximum number of Cell Styles was exceeded. 
    //You can define up to 64000 style in a .xlsx Workbook
    //if we call too often workbook.createCellStyle() in getCell method, we reach a limit
    //actually we have to reuse the styles
    //the hash is a string that contains relevant information of styles that could be used
    private Map<String, CellStyle> hash2cellStyle;

    /*package*/ ExcelGeneratorTableDrawer(String workbookFileName, XSSFWorkbook workbook, XSSFSheet sheet, Point offset, Model abox, 
            //maybe deprecated:
            Model provenanceModel, CSVPrinter provenanceCSV, Model expectedModel) {
        this.workbookFileName = workbookFileName;
        this.workbook = workbook;
        this.sheet = sheet;
        this.offset = offset;
        this.abox = abox;
        this.hash2cellStyle = new HashMap<>();
        
        creationHelper = workbook.getCreationHelper();
        
        
        //deprecated
        this.expectedModel = expectedModel;
        this.provenanceModel = provenanceModel;
        /*
        try {
            this.provenanceCSV = provenanceCSV;
            //this.provenanceCSV = CSVFormat.DEFAULT.print(new FileWriter(provenanceCSV));
            csvProvenanceHeader();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        */
    }

    public void text(String text, int x, int y) {
        if (text != null) {
            getCell(x, y).setCellValue(text);
        }
    }

    public void text(String text, FontStyle style, int x, int y) {
        if (text != null) {
            Cell cell = getCell(x, y);
            cell.setCellValue(text);

            CellStyle cellStyle = workbook.createCellStyle();

            XSSFFont font = workbook.createFont();
            style.applyTo(font);
            cellStyle.setFont(font);

            cell.setCellStyle(cellStyle);
        }
    }

    public void text(String text, int x, int y, Collection<Statement> provenance) {
        if (text != null) {
            Cell cell = getCell(x, y);
            cell.setCellValue(text);

            for (Statement stmt : provenance) {
                //wasDerivedFrom(stmt, cell);
            }
        }
    }

    public void richtext(String text, List<FontStyle> styles, int x, int y, Collection<Statement> provenance) {
        if (text != null) {
            Cell cell = getCell(x, y);

            RichTextString rts = new XSSFRichTextString(text);

            for (FontStyle fsit : styles) {
                XSSFFont font = workbook.createFont();
                font.setFontHeightInPoints(defaultFontHeightInPoints);
                font.setFontName(defaultFontName);
                fsit.applyTo(font);
                rts.applyFont(fsit.getStartIndex(), fsit.getEndIndex(), font);
            }

            cell.setCellValue(rts);

            for (Statement stmt : provenance) {
                //wasDerivedFrom(stmt, cell);
            }
        }
    }

    public void localDate(LocalDate localDate, String format, int x, int y, Statement provenance) {
        if (localDate != null) {
            Cell cell = getCell(x, y);
            cell.setCellValue(localDate);

            CellStyle style = workbook.createCellStyle();
            short fmt = creationHelper.createDataFormat().getFormat(format);
            style.setDataFormat(fmt);
            cell.setCellStyle(style);

            //wasDerivedFrom(provenance, cell);
        }
    }

    //overwrites with cell.setCellStyle the style
    private void dataFormat(Cell cell, String format) {
        short fmt = creationHelper.createDataFormat().getFormat(format);

        //set
        //CellStyle style = workbook.createCellStyle();
        //style.setDataFormat(fmt);
        //cell.setCellStyle(style);

        //use available
        //this does not work if it was not created by workbook.createCellStyle()
        //we do this in the getCell method now 2021-02-18
        cell.getCellStyle().setDataFormat(fmt);
    }
    
    //from https://stackoverflow.com/a/53059793
    public void comment(Cell cell, String author, String commentText) {
        ClientAnchor anchor = creationHelper.createClientAnchor();

        anchor.setCol1(cell.getColumnIndex() + 1); //the box of the comment starts at this given column...
        anchor.setCol2(cell.getColumnIndex() + 10); //...and ends at that given column
        anchor.setRow1(cell.getRowIndex() + 1); //one row below the cell...
        anchor.setRow2(cell.getRowIndex() + 7); //...and 4 rows high

        Drawing drawing = sheet.createDrawingPatriarch();
        Comment comment = drawing.createCellComment(anchor);
        comment.setString(creationHelper.createRichTextString(commentText));
        comment.setAuthor(author);

        cell.setCellComment(comment);
    }
    
    public void literal(Statement literalStatement, int x, int y) {
        if (literalStatement != null) {
            Cell cell = getCell(x, y);
            cell.setCellValue(literalStatement.getObject().asLiteral().getValue().toString());

            //wasDerivedFrom(literalStatement, cell);
        }
    }

    //==========================================================================
    //the ExcelTable case (not free drawing)
    
    //use from storage
    public void excelcell(ExcelCell excelCell, int x, int y, Provenance provenance, ExcelSproutOptions options) {
        //empty
        if (excelCell == null) {
            return;
        }

        Cell cell = getCell(x, y, excelCell);

        switch (excelCell.getCellType()) {
            case "string":
                if(excelCell.getValueRichText() != null) {
                    
                    FontStyleParseResult result = FontStyle.parse(excelCell.getValueRichText());
                    
                    RichTextString rts = new XSSFRichTextString(result.getText());

                    for (FontStyle fsit : result.getStyles()) {
                        XSSFFont font = workbook.createFont();
                        //TODO we may pass this also
                        font.setFontHeightInPoints(defaultFontHeightInPoints);
                        font.setFontName(defaultFontName);
                        fsit.applyTo(font);
                        rts.applyFont(fsit.getStartIndex(), fsit.getEndIndex(), font);
                    }

                    cell.setCellValue(rts);
                    
                } else {
                    //plain text
                    cell.setCellValue(excelCell.getValueString());
                }
                break;

            case "numeric":
                cell.setCellValue(excelCell.getValueNumeric());
                if (excelCell.getDataFormat() != null) {
                    dataFormat(cell, excelCell.getDataFormat());
                }
                break;

            case "boolean":
                cell.setCellValue(excelCell.getValueBoolean());
                if (excelCell.getDataFormat() != null) {
                    dataFormat(cell, excelCell.getDataFormat());
                }
                break;

            default:
                throw new RuntimeException(excelCell.getCellType() + " not implemented yet");
        }
        
        if(excelCell.getBackgroundColor() != null) {
            IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
            XSSFColor color = new XSSFColor(excelCell.getBackgroundColor(), colorMap);
            cell.getCellStyle().setFillPattern(FillPatternType.SOLID_FOREGROUND);
            ((XSSFCellStyle) cell.getCellStyle()).setFillForegroundColor(color);
        }
        
        if(excelCell.getFontColor() != null) {
            //IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
            //XSSFColor color = new XSSFColor(excelCell.getFontColor(), colorMap);
            
            FontStyle fs = new FontStyle();
            fs.setColor(excelCell.getFontColor());
            
            XSSFFont font = workbook.createFont();
            fs.applyTo(font);
            
            XSSFCellStyle xssfCellStyle = (XSSFCellStyle) cell.getCellStyle();
            xssfCellStyle.setFont(font);
        }
        
        if(excelCell.getRotation() != 0) {
            ((XSSFCellStyle) cell.getCellStyle()).setRotation((short) excelCell.getRotation());
        }

        //if(provenance == null) {
        //    throw new RuntimeException("provenance is null");
        //}
        
        //store the provenance statements in TTL as a cell comment
        if (options.isProvenanceAsCellComment()) {
            //use prefixed
            //but do not put it in the comment here because of loading time
            Model model = provenance.getModel();
            model.setNsPrefixes(options.getPrefixMapping());
            model.setNsPrefixes(PrefixMapping.Standard);
            comment(cell, "Provenance", SemanticUtility.toTTL(model, false));
        }
    }

    //uses excelcell and draws the cells
    public void exceltable(ExcelTable table, ExcelSproutOptions options) {
        for (int i = 0; i < table.getData().length; i++) {
            for (int j = 0; j < table.getData()[i].length; j++) {

                ExcelCell cell = table.getData()[i][j];

                Provenance prov = table.getCellProvMap().get(cell);

                excelcell(cell, j, i, prov, options);
            }
        }
    }

    //getter and stuff =========================================================
    
    private Cell getCell(int x, int y) {
        return getCell(x, y, null);
    }
    
    private Cell getCell(int x, int y, ExcelCell excelCell) {
        x = x + offset.x;
        y = y + offset.y;

        Row row = sheet.getRow(y);
        if (row == null) {
            row = sheet.createRow(y);
        }

        Cell cell = row.getCell(x);
        if (cell == null) {
            cell = row.createCell(x);
            
            //create for each cell a new style object
            //because then we can use the available always
            //reuse if style will be equal based on excelCell
            cell.setCellStyle(getOrCreateStyle(excelCell));
        }
        
        return cell;
    }

    //excelCell can be null
    private CellStyle getOrCreateStyle(ExcelCell excelCell) {
        //if null always create a new one
        if(excelCell == null) {
            return workbook.createCellStyle();
        }
        
        StringJoiner sj = new StringJoiner("-");
        sj.add(String.valueOf(excelCell.getFontColor()));
        sj.add(String.valueOf(excelCell.getFontName()));
        sj.add(String.valueOf(excelCell.getFontSize()));
        sj.add(String.valueOf(excelCell.getBackgroundColor()));
        sj.add(String.valueOf(excelCell.getForegroundColor()));
        sj.add(excelCell.getBorderBottom());
        sj.add(excelCell.getBorderLeft());
        sj.add(excelCell.getBorderRight());
        sj.add(excelCell.getBorderTop());
        sj.add(excelCell.getDataFormat());
        String hash = sj.toString();
        
        CellStyle cs = hash2cellStyle.get(hash);
        //reuse already created one
        if(cs != null) {
            return cs;
        }
        
        //create new
        cs = workbook.createCellStyle();
        //store for next time
        hash2cellStyle.put(hash, cs);
        
        return cs;
    }
    
    public Model getABox() {
        return abox;
    }

    public Resource getResource() {
        return resource;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    //if you want to jump over a line
    public void incLineIndex() {
        lineIndex++;
    }

    public String address(int col, int row) {
        return new CellAddress(row + offset.y, col + offset.x).formatAsString();
    }
    
}
