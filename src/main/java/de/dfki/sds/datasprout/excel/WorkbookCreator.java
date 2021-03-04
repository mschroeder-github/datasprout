package de.dfki.sds.datasprout.excel;

import de.dfki.sds.datasprout.Provenance;
import de.dfki.sds.datasprout.Setup;
import de.dfki.sds.datasprout.excelgen.ExcelGenerator;
import de.dfki.sds.datasprout.excelgen.ExcelGeneratorResult;
import de.dfki.sds.datasprout.excelgen.ExcelGeneratorSheetConfig;
import de.dfki.sds.datasprout.excelgen.ExcelGeneratorTableConfig;
import de.dfki.sds.datasprout.excelgen.ExcelGeneratorWorkbookConfig;
import de.dfki.sds.datasprout.utils.JsonUtility;
import de.dfki.sds.datasprout.utils.SemanticUtility;
import de.dfki.sds.datasprout.vocab.CSVW;
import de.dfki.sds.datasprout.vocab.PROV;
import de.dfki.sds.datasprout.vocab.SS;
import de.dfki.sds.hephaistos.storage.excel.ExcelCell;
import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 */
public class WorkbookCreator {

    //creates per workbook a folder and puts there
    //the workbook (as messy data container)
    //expected.ttl (expected triples to extract from the workbook)
    //provenance.ttl (per cell the provenance to the title)
    public void create(File dstFolder, List<ExcelTable> tables, ExcelSproutOptions options) {

        Random rnd = new Random(options.getRandomSeed());

        //there are many tables
        //we have to decide: what table to what sheet, and what sheet to what workbook
        //we should not repeat tables about the same class, so there are possible groups
        //maybe a workbook should be full to have the best expected.ttl from the original model
        //here we calculate: what table to what workbook
        List<List<ExcelTable>> workbookClusters = getWorkbookClusters(tables, rnd);

        //now we have to decide which table will be in which sheet
        //the cleanest way is to have a table per sheet
        //TODO make this variable so more messy version are possible
        List<List<List<ExcelTable>>> workbookSheetTables = new ArrayList<>();
        for (List<ExcelTable> workbookTables : workbookClusters) {
            List<List<ExcelTable>> sheetTables = new ArrayList<>();
            for (ExcelTable tbl : workbookTables) {
                List<ExcelTable> sheet = Arrays.asList(tbl);
                sheetTables.add(sheet);
            }
            workbookSheetTables.add(sheetTables);
        }

        int maxDigits = String.valueOf(workbookSheetTables.size() - 1).length();

        Map<List<ExcelTable>, ExcelGeneratorSheetConfig> sheetConfigMap = new HashMap<>();

        //now we use the ExcelGenerator to generate the workbooks
        ExcelGenerator excelGenerator = new ExcelGenerator();
        for (int i = 0; i < workbookSheetTables.size(); i++) {

            List<List<ExcelTable>> sheets = workbookSheetTables.get(i);

            //create a config for this workbook
            ExcelGeneratorWorkbookConfig workbookConf = new ExcelGeneratorWorkbookConfig();
            //TODO configurable
            workbookConf.setFileName("workbook.xlsx");

            for (List<ExcelTable> sheet : sheets) {
                ExcelGeneratorSheetConfig sheetConf = new ExcelGeneratorSheetConfig();
                sheetConfigMap.put(sheet, sheetConf);

                StringBuilder sheetNameSB = new StringBuilder();

                //TODO a second table in the sheet means we maybe have to move the offset
                //      so that it will not overlap
                for (int k = 0; k < sheet.size(); k++) {
                    ExcelTable excelTable = sheet.get(k);

                    ExcelGeneratorTableConfig tableConf = new ExcelGeneratorTableConfig();

                    //TODO maybe make a getSingleOrDefault method
                    Point offset = (Point) excelTable.getSetup().getOrDefault("offset", new Point(0, 0));
                    tableConf.setOffset(offset);

                    //draw the ExcelCell matrix from ExcelTable
                    tableConf.setStaticCellDrawer(d -> {
                        //it uses the tableConf offset
                        d.exceltable(excelTable, options);
                    });

                    sheetConf.getTableConfigs().add(tableConf);

                    //TODO if only one table with one class: add provenance sheetname -> insts a class. (for all insts)
                    ClassConfig classConfig = excelTable.getSetup().getOrThrow("classes", ClassConfig.class);
                    if (classConfig.hasLabel()) {
                        sheetNameSB.append(classConfig.getLabel());
                    } else {
                        throw new RuntimeException("ClassConfig should give a label to name the sheet");
                    }

                    //in one sheet multiple tables could be existing
                    if (k != sheet.size() - 1) {
                        sheetNameSB.append(" & ");
                    }
                }

                //sheet name comes from table content
                sheetConf.setSheetName(sheetNameSB.toString());

                workbookConf.getSheetConfigs().add(sheetConf);

            }//per sheet

            ExcelGeneratorResult result = excelGenerator.generate(null, workbookConf);

            //System.out.println("save workbook " + i);
            //no extra folder when only one workbook
            File workbookFolder = workbookSheetTables.size() == 1 ? dstFolder : new File(dstFolder, String.format("%0" + maxDigits + "d", i));
            result.saveExcel(workbookFolder);

            //write provenance =================================================
            Model expectedModel = null;
            Model provenanceModel = null;
            CSVPrinter provenanceCSV = null;

            if (options.isWriteExpectedModel()) {
                expectedModel = ModelFactory.createDefaultModel();
                expectedModel.setNsPrefixes(options.getPrefixMapping());
                expectedModel.setNsPrefixes(PrefixMapping.Standard);
            }
            if (options.isWriteProvenanceModel()) {
                provenanceModel = ModelFactory.createDefaultModel();
                provenanceModel.setNsPrefixes(options.getPrefixMapping());
                provenanceModel.setNsPrefix("prov", PROV.NS);
                provenanceModel.setNsPrefix("csvw", CSVW.NS);
                provenanceModel.setNsPrefix("ss", SS.NS);
                provenanceModel.setNsPrefixes(PrefixMapping.Standard);
            }
            if (options.isWriteProvenanceCSV()) {
                try {
                    provenanceCSV = CSVFormat.DEFAULT.print(
                            new OutputStreamWriter(
                                    new GZIPOutputStream(
                                            new FileOutputStream(
                                                    new File(workbookFolder, "provenance.csv.gz")
                                            ))));
                    csvProvenanceHeader(provenanceCSV);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            //used in rdfProvenance for fast lookup reified statements
            Map<Statement, Resource> stmt2res = new HashMap<>();

            //need here sheetname for provenance
            for (List<ExcelTable> sheet : sheets) {

                ExcelGeneratorSheetConfig sheetConfig = sheetConfigMap.get(sheet);

                for (ExcelTable table : sheet) {

                    for (Entry<ExcelCell, Provenance> cell2prov : table.getCellProvMap().entrySet()) {

                        ExcelCell cell = cell2prov.getKey();
                        Provenance prov = cell2prov.getValue();

                        if (cell.getAddress() == null) {
                            //this was a temporary cell created for a merge 
                            //in TableGenerator putMultipleObjects method
                            continue;
                        }

                        if (prov.getStatements().isEmpty()) {
                            //no provenance information for this cell
                            continue;
                        }

                        if (options.isWriteExpectedModel()) {
                            expectedModel.add(prov.getStatements());
                        }
                        if (options.isWriteProvenanceModel()) {
                            rdfProvenance(cell, sheetConfig.getSheetName(), prov, stmt2res, provenanceModel);
                        }
                        if (options.isWriteProvenanceCSV()) {
                            csvProvenance(cell, sheetConfig.getSheetName(), prov, provenanceCSV, provenanceModel);
                        }
                    }
                }
            }

            //write to files
            if (options.isWriteExpectedModel()) {
                File file = new File(workbookFolder, "expected.ttl.gz");
                try (OutputStream os = file.getName().endsWith("gz") ? new GZIPOutputStream(new FileOutputStream(file)) : new FileOutputStream(file)) {
                    expectedModel.write(os, "TTL");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (options.isWriteProvenanceModel()) {
                File file = new File(workbookFolder, "provenance.ttl.gz");
                try (OutputStream os = file.getName().endsWith("gz") ? new GZIPOutputStream(new FileOutputStream(file)) : new FileOutputStream(file)) {
                    provenanceModel.write(os, "TTL");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (options.isWriteProvenanceCSV()) {
                try {
                    provenanceCSV.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            if (options.isWriteGenerationSummaryJson()) {

                //key is sheet name
                JSONObject perSheetPatternUsage = new JSONObject();
                JsonUtility.forceLinkedHashMap(perSheetPatternUsage);

                for (List<ExcelTable> sheet : sheets) {

                    ExcelGeneratorSheetConfig sheetConfig = sheetConfigMap.get(sheet);

                    //count how often
                    Map<String, Map<Object, Integer>> pattern2value2count = new HashMap<>();
                    for (ExcelTable tbl : sheet) {
                        for (Entry<ExcelCell, Provenance> entry : tbl.getCellProvMap().entrySet()) {

                            //skip the ones with no prov and no address (temp cells)
                            if (entry.getKey().getAddress() == null || entry.getValue().getStatements().isEmpty()) {
                                continue;
                            }

                            for (Entry<String, Object> e : entry.getValue().getUsedPatterns().entrySet()) {
                                
                                Object val = e.getValue();
                                if(val instanceof JSONArray) {
                                    //because json array hash is always different
                                    val = val.toString();
                                }
                                
                                Map<Object, Integer> value2count = pattern2value2count.computeIfAbsent(e.getKey(), k -> new HashMap<>());
                                value2count.put(val, value2count.getOrDefault(val, 0) + 1);
                            }
                        }
                    }

                    JSONObject patternUsage = new JSONObject();
                    JsonUtility.forceLinkedHashMap(patternUsage);

                    List<Entry<String, Map<Object, Integer>>> pattern2value2countList = new ArrayList<>(pattern2value2count.entrySet());
                    pattern2value2countList.sort((a,b) -> a.getKey().compareTo(b.getKey()));
                    
                    for (Entry<String, Map<Object, Integer>> pattern2value2countEntry : pattern2value2countList) {
                        JSONArray array = new JSONArray();

                        for (Entry<Object, Integer> e : pattern2value2countEntry.getValue().entrySet()) {
                            JSONObject v2c = new JSONObject();
                            JsonUtility.forceLinkedHashMap(v2c);
                            v2c.put("value", e.getKey());
                            v2c.put("count", e.getValue());
                            array.put(v2c);
                        }

                        patternUsage.put(pattern2value2countEntry.getKey(), array);
                    }

                    perSheetPatternUsage.put(sheetConfig.getSheetName(), patternUsage);
                }
                options.getGenerationSummary().put("patternUsagePerSheet", perSheetPatternUsage);

                File file = new File(workbookFolder, "summary.json");
                try {
                    FileUtils.writeStringToFile(file, options.getGenerationSummary().toString(2), StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

        }//per workbook
    }

    private List<List<ExcelTable>> getWorkbookClusters(List<ExcelTable> tables, Random rnd) {
        //collect them based on classes and all classes so that we can create workbooks with all classes and no duplicates
        Map<Resource, List<ExcelTable>> class2tables = new HashMap<>();
        for (ExcelTable tbl : tables) {
            ClassConfig classConfig = tbl.getSetup().getOrThrow(Setup.CLASSES, ClassConfig.class);
            //we allow multiple classes in one table
            for (Resource cls : classConfig.getClasses()) {
                class2tables.computeIfAbsent(cls, c -> new ArrayList<>()).add(tbl);
            }
        }

        Set<Resource> allClasses = new HashSet<>(class2tables.keySet());
        Set<ExcelTable> remainingTables = new HashSet<>(tables);

        //a list of workbook tables
        List<List<ExcelTable>> workbookClusters = new ArrayList<>();
        while (!remainingTables.isEmpty()) {

            List<ExcelTable> workbookCluster = new ArrayList<>();

            List<Resource> remainingClasses = new ArrayList<>(allClasses);

            //take all classes
            while (!remainingClasses.isEmpty()) {
                Resource cls = remainingClasses.get(0);

                List<ExcelTable> tbls = class2tables.get(cls);

                if (tbls.isEmpty()) {
                    remainingClasses.remove(cls);
                } else {
                    ExcelTable selected = tbls.get(rnd.nextInt(tbls.size()));

                    ClassConfig classConfig = selected.getSetup().getOrThrow(Setup.CLASSES, ClassConfig.class);
                    remainingClasses.removeAll(classConfig.getClasses());

                    workbookCluster.add(selected);

                    tbls.remove(selected);
                    remainingTables.remove(selected);
                }
            }

            if (!workbookCluster.isEmpty()) {
                workbookClusters.add(workbookCluster);
            }
        }

        return workbookClusters;
    }

    //-----------------------
    //provenance
    private void csvProvenanceHeader(CSVPrinter provenanceCSV) throws IOException {
        provenanceCSV.printRecord(
                "id",
                "sheet",
                "address",
                "type",
                "value",
                "statements"
        );
    }

    private void csvProvenance(ExcelCell excelCell, String sheetName, Provenance provenance, CSVPrinter provenanceCSV, PrefixMapping prefixMapping) {
        Model provModel = provenance.getModel();
        provModel.setNsPrefixes(prefixMapping);

        try {
            provenanceCSV.printRecord(
                    excelCell.getId(),
                    sheetName, //"sheet",
                    excelCell.getAddress(), //"addr" has both x,y
                    excelCell.getCellType(), //cellType
                    excelCell.getValue(),
                    //cell.getColumnIndex(), //"x",
                    //cell.getRowIndex(), //"y",
                    //excelCell.toJSON().toString(), //"content", too much
                    SemanticUtility.toTTL(provModel, false)//, //"statements",
            //provenance.getUsedPatterns().toString() //"patterns"
            );
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void rdfProvenance(ExcelCell excelCell, String sheetName, Provenance provenance, Map<Statement, Resource> stmt2res, Model provenanceModel) {
        Resource cellRes = cellToResource(excelCell, sheetName, provenanceModel);

        for (Statement stmt : provenance.getStatements()) {

            Resource refstmt = stmt2res.get(stmt);
            if (refstmt == null) {
                refstmt = provenanceModel.createResource();
                stmt2res.put(stmt, refstmt);

                provenanceModel.add(refstmt, RDF.type, RDF.Statement);
                provenanceModel.add(refstmt, RDF.type, PROV.Entity);
                provenanceModel.add(refstmt, RDF.subject, stmt.getSubject());
                provenanceModel.add(refstmt, RDF.predicate, stmt.getPredicate());
                provenanceModel.add(refstmt, RDF.object, stmt.getObject());
            }

            provenanceModel.add(refstmt, PROV.wasDerivedFrom, cellRes);
        }
    }

    private Resource cellToResource(ExcelCell cell, String sheetName, Model provenanceModel) {
        //String hash = StringUtility.md5(sheetName + cell.getAddress());

        //better use id
        Resource cellRes = provenanceModel.createResource("cell:" + cell.getId());

        provenanceModel.add(cellRes, RDF.type, CSVW.Cell);
        provenanceModel.add(cellRes, RDF.type, PROV.Entity);

        if (cell.getAddress() == null) {
            int a = 0;
        }

        provenanceModel.add(cellRes, SS.sheetName, sheetName);
        provenanceModel.add(cellRes, SS.address, cell.getAddress());

        return cellRes;
    }

}
