package de.dfki.sds.datasprout;

import de.dfki.sds.datasprout.excel.ExcelSproutOptions;
import de.dfki.sds.datasprout.excel.ExcelTable;
import de.dfki.sds.datasprout.excel.IdCounter;
import de.dfki.sds.datasprout.excel.PatternsToSetups;
import de.dfki.sds.datasprout.excel.TableGenerator;
import de.dfki.sds.datasprout.excel.WorkbookCreator;
import de.dfki.sds.datasprout.utils.Dataset;
import de.dfki.sds.datasprout.vocab.FOAF;
import de.dfki.sds.datasprout.vocab.GL;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.zeroturnaround.zip.commons.FileUtils;

/**
 *
 */
public class DataSproutPipeline {

    private List<Dataset> datasets;
    private File genFolder;
    private Map<String, Consumer<ExcelSproutOptions>> modeOptions;

    public DataSproutPipeline() {
        datasets = new ArrayList<>();
        genFolder = new File("gen");
        modeOptions = new HashMap<>();
    }

    private void defaultSettings(ExcelSproutOptions options, String mode) {
        options.setWriteProvenanceCSV(true);
        options.setWriteProvenanceModel(true);
        options.setWriteExpectedModel(true);
        options.setWriteGenerationSummaryJson(true);
        options.setNumberOfWorkbooks(1);
        options.setRandomSeed(0);
        
        options.getPatternsToSetups().getLabelProperties().add(FOAF.name);
        options.getPatternsToSetups().getLabelProperties().add(DCTerms.title);
        options.getPatternsToSetups().getLabelProperties().add(GL.hasId);
        options.getPatternsToSetups().getLabelProperties().add(ResourceFactory.createProperty("http://purl.org/dc/elements/1.1/title"));
        options.getPatternsToSetups().getLabelProperties().add(ResourceFactory.createProperty("http://example.org#name"));
        options.getPatternsToSetups().getLabelProperties().add(ResourceFactory.createProperty("http://localhost/vocabulary/bench/booktitle"));
        
        options.getPatternsToSetups().getAcronymProperties().add(FOAF.firstName);
        options.getPatternsToSetups().getAcronymProperties().add(FOAF.lastName);
        options.getPatternsToSetups().getAcronymProperties().add(GL.hasAbbreviation);
        
        options.getPatternsToSetups().getPartialLabelProperties().add(FOAF.firstName);
        options.getPatternsToSetups().getPartialLabelProperties().add(FOAF.lastName);
        options.getPatternsToSetups().getPartialLabelProperties().add(GL.worksAt);
        options.getPatternsToSetups().getPartialLabelProperties().add(FOAF.homepage);
        options.getPatternsToSetups().getPartialLabelProperties().add(DCTerms.issued);
        options.getPatternsToSetups().getPartialLabelProperties().add(ResourceFactory.createProperty("http://example.org#emailAddress"));
        options.getPatternsToSetups().getPartialLabelProperties().add(ResourceFactory.createProperty("http://example.org#name"));
        options.getPatternsToSetups().getPartialLabelProperties().add(ResourceFactory.createProperty("http://localhost/vocabulary/bench/booktitle"));
        
        options.getPatternsToSetups().getOutdatedProperties().add(GL.wasFormerEditor);
        
        if(!mode.equals("Clean")) {
            options.getPatternsToSetups().getMergeCellDelimiters().addAll(Arrays.asList(", ", " ", " + ", " & "));
            
            if(options.getPatternsToSetups().getLocale() == Locale.ENGLISH) {
                options.getPatternsToSetups().getBooleanTrueSymbols().addAll(Arrays.asList("OK", "true", "yes", "x"));
                options.getPatternsToSetups().getBooleanFalseSymbols().addAll(Arrays.asList("-", "false", "no", "not"));
            } else if(options.getPatternsToSetups().getLocale() == Locale.GERMAN) {
                options.getPatternsToSetups().getBooleanTrueSymbols().addAll(Arrays.asList("OK", "wahr", "ja", "x"));
                options.getPatternsToSetups().getBooleanFalseSymbols().addAll(Arrays.asList("-", "falsch", "nein", "nicht"));
            }
        }
    }

    public void run() throws IOException {
        ModelFactory.createDefaultModel();

        FileUtils.deleteQuietly(genFolder);

        TableGenerator tableGenerator = new TableGenerator();
        tableGenerator.getDataTypeMap().put("http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/USD", value -> Double.parseDouble(value));

        WorkbookCreator workbookCreator = new WorkbookCreator();

        for (Dataset dataset : datasets) {
            
            IdCounter idCounter = new IdCounter();

            File datasetFolder = new File(genFolder, dataset.getName());

            System.out.println("==============");
            System.out.println("loading " + dataset.getName());
            Model model = ModelFactory.createDefaultModel().read(new FileReader(dataset.getFile()), null, "TTL");
            System.out.println(model.size() + " statements");

            for (Entry<String, Consumer<ExcelSproutOptions>> modeOption : modeOptions.entrySet()) {

                ExcelSproutOptions options = new ExcelSproutOptions();
                options.setPrefixMapping(model);
                

                String mode = modeOption.getKey();
                Consumer<ExcelSproutOptions> modeOptionSetter = modeOption.getValue();

                System.out.println("--------------");
                System.out.println("Mode: " + mode);

                File datasetModeFolder = new File(datasetFolder, mode);

                PatternsToSetups patternsToSetups = new PatternsToSetups(Locale.ENGLISH);
                patternsToSetups.setMultipleEntitiesInOneCell(true);
                options.setPatternsToSetups(patternsToSetups);

                defaultSettings(options, mode);

                //changes per mode
                modeOptionSetter.accept(options);
                
                options.getGenerationSummary().put("dataset", dataset.getName());
                options.getGenerationSummary().put("statements", model.size());
                options.getGenerationSummary().put("mode", mode);
                options.getGenerationSummary().put("date", LocalDate.now().toString());
                options.getGenerationSummary().put("numberOfWorkbooks", options.getNumberOfWorkbooks());
                options.getGenerationSummary().put("randomSeed", options.getRandomSeed());
                options.getGenerationSummary().put("locale", patternsToSetups.getLocale().toString());

                //generate --------------------
                //from patterns to setups for tables
                List<Setup> setups = patternsToSetups.generate(model, options.getNumberOfWorkbooks(), new Random(options.getRandomSeed()));

                //from setups to tables
                List<ExcelTable> tables = tableGenerator.generateList(setups, idCounter, options);
                System.out.println(tables.size() + " tables");
                options.getGenerationSummary().put("tables", tables.size());

                //from tables to workbooks
                workbookCreator.create(datasetModeFolder, tables, options);
            }
        }
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public Map<String, Consumer<ExcelSproutOptions>> getModeOptions() {
        return modeOptions;
    }

}
