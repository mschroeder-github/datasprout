
package de.dfki.sds.datasprout;

import de.dfki.sds.datasprout.excel.ClassConfig;
import de.dfki.sds.datasprout.excel.ExcelSproutOptions;
import de.dfki.sds.datasprout.excel.ExcelTable;
import de.dfki.sds.datasprout.excel.IdCounter;
import de.dfki.sds.datasprout.excel.Patterns;
import de.dfki.sds.datasprout.excel.PropertyConfig;
import de.dfki.sds.datasprout.excel.TableGenerator;
import de.dfki.sds.datasprout.excel.WorkbookCreator;
import de.dfki.sds.datasprout.utils.Dataset;
import de.dfki.sds.datasprout.vocab.FOAF;
import de.dfki.sds.datasprout.vocab.GL;
import de.dfki.sds.rdf2rdb.RdfsAnalyzer;
import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * 
 */
public class MainPipeline {

    public static void main(String[] args) throws Exception {
        dataSproutPipeline(args);
        guidelineManualSetup();
    }
    
    private static void dataSproutPipeline(String[] args) throws IOException {
        DataSproutPipeline pipeline = new DataSproutPipeline();
        
        pipeline.getDatasets().add(new Dataset("GL", new File("dataset/GL.ttl")));
        pipeline.getDatasets().add(new Dataset("BSBM", new File("dataset/BSBM.ttl")));
        pipeline.getDatasets().add(new Dataset("LUBM", new File("dataset/LUBM.ttl")));
        pipeline.getDatasets().add(new Dataset("SP2B", new File("dataset/SP2B.ttl")));
        
        pipeline.getModeOptions().put("Clean", options -> { });
        
        String prefix = "SinglePattern_";
        pipeline.getModeOptions().put(prefix + "NumericInformationAsText", options -> { options.getPatternsToSetups().setNumericInformationAsText(true); });
        pipeline.getModeOptions().put(prefix + "AcronymsOrSymbols", options -> { options.getPatternsToSetups().setAcronymsOrSymbols(true); });
        pipeline.getModeOptions().put(prefix + "MultipleSurfaceForms", options -> { options.getPatternsToSetups().setMultipleSurfaceForms(true); });
        pipeline.getModeOptions().put(prefix + "PropertyValueAsColor", options -> { options.getPatternsToSetups().setPropertyValueAsColor(true); });
        pipeline.getModeOptions().put(prefix + "IntraCellAdditionalInformation", options -> { options.getPatternsToSetups().setIntraCellAdditionalInformation(true); });
        pipeline.getModeOptions().put(prefix + "IntraCellAdditionalInformation_PartialFormattingIndicatesRelations", options -> { 
            options.getPatternsToSetups().setIntraCellAdditionalInformation(true);
            options.getPatternsToSetups().setPartialFormattingIndicatesRelations(true);
        });
        pipeline.getModeOptions().put(prefix + "OutdatedIsFormatted", options -> { options.getPatternsToSetups().setOutdatedIsFormatted(true); });
        pipeline.getModeOptions().put(prefix + "MultipleTypesInATable", options -> { options.getPatternsToSetups().setMultipleTypesInATable(true); });
        
        
        //all
        pipeline.getModeOptions().put("All", options -> { 
                options.getPatternsToSetups().setNumericInformationAsText(true);
                options.getPatternsToSetups().setAcronymsOrSymbols(true);
                options.getPatternsToSetups().setMultipleSurfaceForms(true);
                options.getPatternsToSetups().setPropertyValueAsColor(true);
                options.getPatternsToSetups().setIntraCellAdditionalInformation(true);
                options.getPatternsToSetups().setPartialFormattingIndicatesRelations(true);
                options.getPatternsToSetups().setOutdatedIsFormatted(true);
                options.getPatternsToSetups().setMultipleTypesInATable(true);
        });
        pipeline.getModeOptions().put("All_ProvenanceAsCellComment", options -> { 
                options.getPatternsToSetups().setNumericInformationAsText(true);
                options.getPatternsToSetups().setAcronymsOrSymbols(true);
                options.getPatternsToSetups().setMultipleSurfaceForms(true);
                options.getPatternsToSetups().setPropertyValueAsColor(true);
                options.getPatternsToSetups().setIntraCellAdditionalInformation(true);
                options.getPatternsToSetups().setPartialFormattingIndicatesRelations(true);
                options.getPatternsToSetups().setOutdatedIsFormatted(true);
                options.getPatternsToSetups().setMultipleTypesInATable(true);
                
                options.setProvenanceAsCellComment(true);
        });
        
        pipeline.run();
    }
    
    private static void guidelineManualSetup() throws IOException {
        ExcelSproutOptions options = new ExcelSproutOptions();
        options.setWriteExpectedModel(true);
        options.setWriteProvenanceModel(true);
        options.setWriteProvenanceCSV(true);
        options.setWriteGenerationSummaryJson(true);

        Model model = ModelFactory.createDefaultModel().read(new FileReader("dataset/GL.ttl"), null, "TTL");
        System.out.println(model.size() + " a-box statements");

        Model glOntoModel = ModelFactory.createDefaultModel().read("http://www.dfki.uni-kl.de/~mschroeder/ld/gl", "TTL");
        System.out.println(glOntoModel.size() + " t-box statements");
        model.add(glOntoModel);
        
        options.setPrefixMapping(model);
        
        //setup = table
        List<Setup> setups = new ArrayList<>();

        Resource medicalGuideline = model.listSubjectsWithProperty(RDFS.label, "Medical Guideline").toList().get(0);
        Resource certainMailingList = model.listSubjectsWithProperty(RDF.type, GL.MailingList).toList().get(0);
        Resource stateDeprecated = model.listSubjectsWithProperty(RDFS.label, "Deprecated").toList().get(0);
        //Resource researchAndDevelopment = model.listSubjectsWithProperty(RDFS.label, "Research and Development").toList().get(0);
        
        Setup glSetup = new Setup();
        glSetup.put(Setup.CLASSES, new ClassConfig(GL.Guideline).setLabel("Medical Guidelines"));
        glSetup.put(Setup.PROPERTIES, Arrays.asList(
                new PropertyConfig(GL.hasCategory),
                new PropertyConfig(Arrays.asList(GL.hasId, RDFS.label)).setLabel("Guideline ID"),
                new PropertyConfig(GL.hasTitle),
                new PropertyConfig(GL.hasDepartment),
                new PropertyConfig(Arrays.asList(GL.hasEditor, GL.hasEditorResponsible, GL.wasFormerEditor)).setUri("java:datasprout.editor").setLabel("Editor / Respons."),
                new PropertyConfig(Arrays.asList(GL.validFrom, GL.plannedValidFrom, GL.hasNote)).setUri("java:datasprout.valid"),
                new PropertyConfig(GL.lastModifiedDate),
                new PropertyConfig(GL.invalidFrom),
                new PropertyConfig(GL.hasSecurityNeed),
                new PropertyConfig(GL.inMailingList, certainMailingList).setLabel("Certain Mailing List?").setUri("java:datasprout.certainMailingList")
        ));
        glSetup.put(Setup.RANDOM, new Random(1234));
        glSetup.put("header", true);
        glSetup.put("headerBackgroundColor", new Color(252, 213, 181));
        glSetup.put(Setup.LABEL_PROPERTIES, Arrays.asList(RDFS.label));
        glSetup.put("offset", new Point(1,1));
        
        glSetup.put("DateRendering", Arrays.asList(Patterns.DateRendering.String, Patterns.DateRendering.Numeric));
        glSetup.put("DateDataFormats", Arrays.asList("DD.MM.YYYY"));
        glSetup.put("DateStringFormats", Arrays.asList("dd.MM.yyyy"));
        
        glSetup.put(GL.hasDepartment + "." + Setup.LABEL_PROPERTIES, Arrays.asList(GL.hasAbbreviation));
        
        BiFunction<Resource, Model, Boolean> filter = (r, m) -> m.contains(r, GL.hasKind, medicalGuideline);
        glSetup.put("instanceFilter", filter);

        glSetup.put("EmptyCellRendering", Arrays.asList(Patterns.EmptyCellRendering.Native));
        glSetup.put("MergeCellStrategy", Arrays.asList(Patterns.MergeCellStrategy.Delimiter));
        glSetup.put("MergeCellDelimiters", Arrays.asList("/ "));

        glSetup.put("java:datasprout.certainMailingList.BooleanRendering", Arrays.asList(Patterns.BooleanRendering.Symbol));
        glSetup.put("java:datasprout.certainMailingList.BooleanTrueSymbols", Arrays.asList("x"));
        glSetup.put("java:datasprout.certainMailingList.BooleanFalseSymbols", Arrays.asList(""));

        glSetup.put("java:datasprout.editor" + "." + "MergeCellDelimiters", Arrays.asList("\n", ", "), 0.5, 0.5);

        //class based Setup.LABEL_PROPERTIES with property subsets (will be concatinated) and distribution
        glSetup.put(FOAF.Person + "." + Setup.LABEL_PROPERTIES, Arrays.asList(
                Arrays.asList(FOAF.lastName, FOAF.firstName),
                Arrays.asList(FOAF.firstName, FOAF.lastName),
                Arrays.asList(FOAF.lastName),
                Arrays.asList(FOAF.lastName, GL.worksAt)
        ));
        glSetup.putDistribution(FOAF.Person.getURI() + "." + Setup.LABEL_PROPERTIES, 0.5, 0.15, 0.3, 0.05);

        //pattern: Property Value as Color
        glSetup.put(GL.hasState + "." + stateDeprecated + ".BackgroundColor", new Color(217, 217, 217));
        glSetup.put(GL.isRecent + "." + "true^^http://www.w3.org/2001/XMLSchema#boolean" + ".ForegroundColor", new Color(255, 0, 0));

        //glSetup.put(GL.hasEditorResponsible + ".PartialFormattingIndicatesRelations", "<b><font color='#0000ff'>|</font></b>");
        glSetup.put(GL.plannedValidFrom + ".PartialFormattingIndicatesRelations", "<font color='#ff4848'>|</font>");
        glSetup.put(GL.hasEditorResponsible + ".PartialFormattingIndicatesRelations", "<b>|</b>");
        glSetup.put(GL.wasFormerEditor + ".PartialFormattingIndicatesRelations", "<strike>|</strike>");

        glSetup.put("instanceRandomOrder", true);

        //pattern: Multiple Types in a Table
        glSetup.put("MultipleTypesInATable.ChildProperty", GL.hasAttachment);
        //TODO sort children: Comparator<Resource>

        glSetup.put(Setup.RDFS_ANALYZER, new RdfsAnalyzer().analyze(model)); //TODO still necessary?

        setups.add(glSetup);

        IdCounter idCounter = new IdCounter();
        
        //generate tables from the setups
        TableGenerator generator = new TableGenerator();
        List<ExcelTable> tables = generator.generateList(setups, idCounter, options);
        System.out.println(tables.size() + " tables");

        File genFolder = new File("gen/GL/Manual");

        //generate workbooks
        WorkbookCreator workbookCreator = new WorkbookCreator();
        workbookCreator.create(genFolder, tables, options);
    }
    
}
