
package de.dfki.sds.datasprout;

import de.dfki.sds.datasprout.excel.ExcelSproutOptions;
import de.dfki.sds.datasprout.excel.ExcelTable;
import de.dfki.sds.datasprout.excel.IdCounter;
import de.dfki.sds.datasprout.excel.PatternsToSetups;
import de.dfki.sds.datasprout.excel.TableGenerator;
import de.dfki.sds.datasprout.excel.WorkbookCreator;
import de.dfki.sds.datasprout.vocab.FOAF;
import de.dfki.sds.datasprout.vocab.GL;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.json.JSONObject;
import org.zeroturnaround.zip.ZipUtil;
import spark.Request;
import spark.Response;
import spark.Spark;

/**
 * 
 */
public class DataSproutServer {

    
    private static final String ROOT_PATH = "/de/dfki/sds/datasprout";
    
    //Data Sprout
    private int port = (int)'D' * 100 + (int)'S';
    
    private File folder;
    
    private Map<String, String> kg2resource;
    
    public DataSproutServer(String[] args) {
        folder = new File("gen");
        folder.mkdir();
        
        kg2resource = new HashMap<>();
        kg2resource.put("GL", ROOT_PATH + "/web/kg/GL.ttl");
        kg2resource.put("BSBM", ROOT_PATH + "/web/kg/BSBM.ttl");
        kg2resource.put("LUBM", ROOT_PATH + "/web/kg/LUBM.ttl");
        kg2resource.put("SP2B", ROOT_PATH + "/web/kg/SP2B.ttl");
    }
    
    public void start() {
        Spark.port(port);
        
        Spark.exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
            response.body(exception.getMessage());
        });
        
        Spark.staticFiles.location(ROOT_PATH + "/web");
        
        Spark.before((req, res) -> {
            String path = req.pathInfo();
            if (!path.equals("/") && path.endsWith("/")) {
                res.redirect(path.substring(0, path.length() - 1));
            }
        });
        
        Spark.awaitInitialization();
        System.out.println("server running at localhost:" + port);
        
        Spark.get("/sprawl", this::getSprawl);
    }
    
    private void excel(File genFolder, Model model, Request req) throws IOException {
        
        //from params to options
        ExcelSproutOptions options = excelOptions(req);
        options.setPrefixMapping(model);
        
        List<Setup> setups = options.getPatternsToSetups().generate(
                model,
                options.getNumberOfWorkbooks(), 
                new Random(options.getRandomSeed())
        );
        
        IdCounter idCounter = new IdCounter();
        
        //generate tables from the setups
        TableGenerator generator = new TableGenerator();
        generator.getDataTypeMap().put("http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/USD", value -> Double.parseDouble(value));
        List<ExcelTable> tables = generator.generateList(setups, idCounter, options);
        
        //generate workbooks
        WorkbookCreator workbookCreator = new WorkbookCreator();
        workbookCreator.create(genFolder, tables, options);
    }
    
    //from request to options
    private ExcelSproutOptions excelOptions(Request req) {
        ExcelSproutOptions options = new ExcelSproutOptions();
        
        options.setRandomSeed(Long.parseLong(req.queryParams("randomSeed")));
        options.setNumberOfWorkbooks(Integer.parseInt(req.queryParams("numberOfWorkbooks")));
        
        options.setWriteExpectedModel(Boolean.parseBoolean(req.queryParams("writeExpectedModel")));
        options.setWriteProvenanceModel(Boolean.parseBoolean(req.queryParams("writeProvenanceModel")));
        options.setWriteProvenanceCSV(Boolean.parseBoolean(req.queryParams("writeProvenanceCSV")));
        options.setWriteGenerationSummaryJson(Boolean.parseBoolean(req.queryParams("writeGenerationSummaryJson")));
        options.setProvenanceAsCellComment(Boolean.parseBoolean(req.queryParams("provenanceAsCellComment")));
        
        String patternsJson = req.queryParams("patterns");
        JSONObject patterns = new JSONObject(patternsJson);
        
        Locale locale = Locale.forLanguageTag(req.queryParamOrDefault("lang", "en"));
        PatternsToSetups patternsToSetups = new PatternsToSetups(locale);
        options.setPatternsToSetups(patternsToSetups);
        
        patternsToSetups.setNumericInformationAsText(patterns.getBoolean("Numeric Information as Text"));
        patternsToSetups.setAcronymsOrSymbols(patterns.getBoolean("Acronyms or Symbols"));
        patternsToSetups.setMultipleSurfaceForms(patterns.getBoolean("Multiple Surface Forms"));
        patternsToSetups.setPropertyValueAsColor(patterns.getBoolean("Property Value as Color"));
        patternsToSetups.setPartialFormattingIndicatesRelations(patterns.getBoolean("Partial Formatting Indicates Relations"));
        patternsToSetups.setOutdatedIsFormatted(patterns.getBoolean("Outdated is Formatted"));
        patternsToSetups.setMultipleEntitiesInOneCell(true);
        patternsToSetups.setIntraCellAdditionalInformation(patterns.getBoolean("Intra-Cell Additional Information"));
        patternsToSetups.setMultipleTypesInATable(patterns.getBoolean("Multiple Types in a Table"));
        
        //more details

        patternsToSetups.getLabelProperties().add(FOAF.name);
        patternsToSetups.getLabelProperties().add(DCTerms.title);
        patternsToSetups.getLabelProperties().add(ResourceFactory.createProperty("http://purl.org/dc/elements/1.1/title"));
        patternsToSetups.getLabelProperties().add(ResourceFactory.createProperty("http://example.org#name"));
        patternsToSetups.getLabelProperties().add(ResourceFactory.createProperty("http://localhost/vocabulary/bench/booktitle"));
        
        patternsToSetups.getAcronymProperties().add(FOAF.firstName);
        patternsToSetups.getAcronymProperties().add(FOAF.lastName);
        
        patternsToSetups.getPartialLabelProperties().add(FOAF.firstName);
        patternsToSetups.getPartialLabelProperties().add(FOAF.lastName);
        patternsToSetups.getPartialLabelProperties().add(FOAF.homepage);
        patternsToSetups.getPartialLabelProperties().add(DCTerms.issued);
        patternsToSetups.getPartialLabelProperties().add(ResourceFactory.createProperty("http://example.org#emailAddress"));
        patternsToSetups.getPartialLabelProperties().add(ResourceFactory.createProperty("http://example.org#name"));
        patternsToSetups.getPartialLabelProperties().add(ResourceFactory.createProperty("http://localhost/vocabulary/bench/booktitle"));
        
        options.getPatternsToSetups().getOutdatedProperties().add(GL.wasFormerEditor);
        
        patternsToSetups.getMergeCellDelimiters().addAll(Arrays.asList(", ", " ", " + ", " & "));
            
        if(patternsToSetups.getLocale() == Locale.ENGLISH) {
            patternsToSetups.getBooleanTrueSymbols().addAll(Arrays.asList("OK", "true", "yes", "x"));
            patternsToSetups.getBooleanFalseSymbols().addAll(Arrays.asList("-", "false", "no", "not"));
        } else if(patternsToSetups.getLocale() == Locale.GERMAN) {
            patternsToSetups.getBooleanTrueSymbols().addAll(Arrays.asList("OK", "wahr", "ja", "x"));
            patternsToSetups.getBooleanFalseSymbols().addAll(Arrays.asList("-", "falsch", "nein", "nicht"));
        }
        
        return options;
    }
    
    private Object getSprawl(Request req, Response resp) throws Exception {
        
        try {
            //zip filename
            LocalDateTime ldt = LocalDateTime.now();
            String time = ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

            //params
            String kg = req.queryParams("kg");
            String mode = req.queryParams("mode");
            if(kg == null) {
                return getError("kg parameter is not set", req, resp);
            }
            if(mode == null) {
                return getError("mode parameter is not set", req, resp);
            }
            
            //get model
            if(!kg2resource.containsKey(kg)) {
                throw new RuntimeException("a knowledge graph named " + kg + " was not found");
            }
            InputStream kgStream = DataSproutServer.class.getResourceAsStream(kg2resource.get(kg));
            Model model = ModelFactory.createDefaultModel().read(kgStream, null, "TTL");
            
            //generate it
            File genFolder = new File(folder, time);
            genFolder.mkdir();
            switch(mode) {
                case "excel": 
                    excel(genFolder, model, req);
                    break;
                    
                default: 
                    throw new RuntimeException("mode " + mode + " is unknown");
            }
            
            System.gc();
            
            //pack zip
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipUtil.pack(genFolder, baos);

            //send
            String zipFilename = "datasprout-sprawl-" + kg + "-" + mode + "-" + time + ".zip";
            
            resp.type("application/zip");
            resp.header("content-disposition", "attachment;filename=" + zipFilename);
            return baos.toByteArray();
            
        } catch(Exception e) {
            e.printStackTrace();
            String stacktrace = ExceptionUtils.getStackTrace(e);
            return getError("Exception:\n" + stacktrace, req, resp);
        } 
    }
    
    private Object getError(String msg, Request req, Response resp) throws Exception {
        String errorHtml = IOUtils.toString(DataSproutServer.class.getResourceAsStream(ROOT_PATH + "/web/error.html"), "UTF-8");
        errorHtml = errorHtml.replace("${msg}", msg);
        
        String emailbodyparam = "&body=" + URLEncoder.encode(msg, "UTF-8").replace("+", "%20");
        errorHtml = errorHtml.replace("${emailbodyparam}", emailbodyparam);
        
        return errorHtml;
    }
    
}
