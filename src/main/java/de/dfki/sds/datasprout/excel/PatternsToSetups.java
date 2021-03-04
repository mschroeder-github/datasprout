package de.dfki.sds.datasprout.excel;

import de.dfki.sds.datasprout.Setup;
import de.dfki.sds.datasprout.utils.ListUtility;
import de.dfki.sds.datasprout.utils.SemanticUtility;
import de.dfki.sds.datasprout.utils.SetUtility;
import de.dfki.sds.rdf2rdb.RdfsAnalyzer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

/**
 * You decide what
 * <a href="http://www.dfki.uni-kl.de/~mschroeder/a-pattern-language-for-spreadsheets">patterns</a>
 * you would like to have and setups will be generated from an RDF model.
 */
public class PatternsToSetups {

    private boolean numericInformationAsText;
    private boolean acronymsOrSymbols;
    private boolean multipleSurfaceForms;
    private boolean propertyValueAsColor;
    private boolean partialFormattingIndicatesRelations;
    private boolean outdatedIsFormatted;
    private boolean multipleEntitiesInOneCell;
    private boolean intraCellAdditionalInformation;
    private boolean multipleTypesInATable;

    //settings for formats
    private Locale locale;
    private List booleanNativeDataFormats;
    private List booleanTrueSymbols;
    private List booleanFalseSymbols;
    private List numericNativeDataFormats;
    private List dateTimeDataFormats;
    private List dateTimeStringFormats;
    private List dateDataFormats;
    private List dateStringFormats;
    private List mergeCellDelimiters;

    private List labelProperties;
    private List acronymProperties;
    private List partialLabelProperties;
    
    //use this for 
    private List outdatedProperties;

    private double multipleTypesInATablePropertyOverlapThreshold = 0.4;
    private int multipleTypesInATableCount = 3;
    
    private StringBuilder colorCodes;
    
    private RdfsAnalyzer rdfsAnalyzer;

    public PatternsToSetups(Locale locale) {
        this.locale = locale;
        initFormats();
    }

    private void initFormats() {
        booleanNativeDataFormats = new ArrayList<>();
        booleanTrueSymbols = new ArrayList<>();
        booleanFalseSymbols = new ArrayList<>();
        numericNativeDataFormats = new ArrayList<>(Arrays.asList(""));
        dateTimeDataFormats = new ArrayList<>();
        dateTimeStringFormats = new ArrayList<>();
        dateDataFormats = new ArrayList<>();
        dateStringFormats = new ArrayList<>();
        mergeCellDelimiters = new ArrayList<>(Arrays.asList("\n"));
        labelProperties = new ArrayList<>(Arrays.asList(RDFS.label));
        acronymProperties = new ArrayList<>();
        partialLabelProperties = new ArrayList<>();
        outdatedProperties = new ArrayList<>();
        colorCodes = new StringBuilder();

        if (locale == Locale.ENGLISH) {
            booleanNativeDataFormats.add("\"Yes\";;\"No\";");
            booleanTrueSymbols.addAll(Arrays.asList("✓")); //, "OK", "true", "yes", "x"
            booleanFalseSymbols.addAll(Arrays.asList("")); //, "-", "false", "no", "not"
            dateTimeDataFormats.add("MM/DD/YYYY HH:mm");
            dateTimeStringFormats.add("MM/dd/yyyy HH:mm:ss");
            dateTimeStringFormats.add("yyyy-MM-dd HH:mm:ss");
            dateTimeStringFormats.add("HH:mm:ss yyyy-MM-dd");
            dateTimeStringFormats.add("yyyyMMdd-HHmmss");
            dateDataFormats.add("MM/DD/YYYY");
            dateStringFormats.add("MM/dd/yyyy");
            dateStringFormats.add("yyyy-MM-dd");
            dateStringFormats.add("yyyyMMdd");

        } else if (locale == Locale.GERMAN) {
            booleanNativeDataFormats.add("\"Ja\";;\"Nein\";");
            booleanTrueSymbols.addAll(Arrays.asList("✓")); //, "OK", "wahr", "ja"
            booleanFalseSymbols.addAll(Arrays.asList("")); //, "-", "falsch", "nein", "nicht"
            dateTimeDataFormats.add("DD.MM.YYYY HH:mm");
            dateTimeStringFormats.add("dd.MM.yyyy HH:mm:ss");
            dateTimeStringFormats.add("yyyy-MM-dd HH:mm:ss");
            dateTimeStringFormats.add("HH:mm:ss yyyy-MM-dd");
            dateTimeStringFormats.add("yyyyMMdd-HHmmss");
            dateDataFormats.add("DD.MM.YYYY");
            dateStringFormats.add("dd.MM.yyyy");
            dateStringFormats.add("yyyy-MM-dd");
            dateStringFormats.add("yyyyMMdd");

        } else {
            throw new RuntimeException(locale + " locale not supported");
        }
    }

    public List<Setup> generate(Model model, int numberOfWorkbooks, Random rnd) {
        rdfsAnalyzer = new RdfsAnalyzer();
        rdfsAnalyzer.analyze(model);

        //renderings
        List booleanRendering = new ArrayList<>(Arrays.asList(Patterns.BooleanRendering.Native));
        List numericRendering = new ArrayList<>(Arrays.asList(Patterns.NumericRendering.Native));
        List numberStringFormats = new ArrayList<>();
        List dateTimeRendering = new ArrayList<>(Arrays.asList(Patterns.DateTimeRendering.Numeric));
        List dateRendering = new ArrayList<>(Arrays.asList(Patterns.DateRendering.Numeric));
        List emptyCellRendering = new ArrayList<>(Arrays.asList(Patterns.EmptyCellRendering.Native));
        List labelPickStrategy = new ArrayList<>(Arrays.asList(Patterns.LabelPickStrategy.Longest));

        //multiple entities in one cell
        List mergeCellStrategy = new ArrayList<>(Arrays.asList(Patterns.MergeCellStrategy.Delimiter));

        //get classes that have instances and create class configs for them
        List<ClassConfig> classConfigs = new ArrayList<>();
        Map<Resource, Set<Resource>> type2insts = rdfsAnalyzer.getType2instances();
        for (Map.Entry<Resource, Set<Resource>> e : type2insts.entrySet()) {
            //if there are instances
            if (!e.getValue().isEmpty()) {
                classConfigs.add(new ClassConfig(e.getKey()).setLabel(SemanticUtility.getLocalName(e.getKey().getURI())));
            }
        }

        //======================================================================
        //spreadsheet patterns
        //multiple entities in one cell is the default case
        //if we would like to prevent it, we have to change the model so that
        //multi-edges become complex objects
        //TODO this leads to a wrong model: have to check later, make a special method for it?
        //easier solution would be: just remove the multi-edges so that a single edge is left
        /*
        if (!multipleEntitiesInOneCell) {

            Model duplicateStmtModel = ModelFactory.createDefaultModel();

            //cleanup
            for (RdfsAnalyzer.MultiCardinalityProperty mcp : rdfsAnalyzer.getMultiCardProperties()) {

                //System.out.println(mcp);
                //the property becomes a class
                Resource multiCardClass = model.createResource(
                        "uuid:" + UUID.randomUUID().toString()
                );
                model.add(multiCardClass, RDF.type, RDFS.Class);
                model.add(multiCardClass, RDFS.label, mcp.getName());

                Property domainProperty = model.createProperty(
                        "uuid:" + UUID.randomUUID().toString()
                );
                model.add(domainProperty, RDFS.label, (mcp.getDomain() != null ? SemanticUtility.getLocalName(mcp.getDomain().getURI()) : "Domain"));

                Property rangeProperty = model.createProperty(
                        "uuid:" + UUID.randomUUID().toString()
                );
                model.add(rangeProperty, RDFS.label, (mcp.getRange() != null ? SemanticUtility.getLocalName(mcp.getRange().getURI()) : "Range"));

                Model toBeRemoved = ModelFactory.createDefaultModel();
                for (Statement stmt : SemanticUtility.iterable(model.listStatements(null, mcp.getProperty(), (RDFNode) null))) {

                    if (duplicateStmtModel.contains(stmt)) {
                        continue;
                    }

                    Resource inst = model.createResource("uuid:" + UUID.randomUUID().toString());
                    model.add(inst, RDF.type, multiCardClass);
                    model.add(inst, domainProperty, stmt.getSubject());
                    model.add(inst, rangeProperty, stmt.getObject());

                    toBeRemoved.add(stmt);
                    duplicateStmtModel.add(stmt);
                }

                if (!toBeRemoved.isEmpty()) {
                    classConfigs.add(new ClassConfig(multiCardClass).setLabel(mcp.getName()));
                    //remove the multi edges in original graph
                    model.remove(toBeRemoved);
                }
            }

            //re-analyze
            rdfsAnalyzer = new RdfsAnalyzer();
            rdfsAnalyzer.analyze(model);
        }*/

        if (numericInformationAsText) {
            //also allow symbols and strings
            booleanRendering.add(Patterns.BooleanRendering.Symbol);
            numericRendering.add(Patterns.NumericRendering.String);
            numberStringFormats.addAll(Arrays.asList(Patterns.NumberStringFormats.StringValueOf));
            dateRendering.add(Patterns.DateRendering.String);
            dateTimeRendering.add(Patterns.DateTimeRendering.String);
        }

        if (acronymsOrSymbols) {
            //you can now pick also shortest
            labelPickStrategy.add(Patterns.LabelPickStrategy.Shortest);
            //special acronym properties are also allowed now
            labelProperties.addAll(acronymProperties);
        }

        //Multiple Surface Forms
        Map<String, Object> multipleSurfaceFormsConfig = new HashMap<>();
        if (multipleSurfaceForms) {
            for (ClassConfig cc : classConfigs) {
                Set<Property> props = new HashSet<>(rdfsAnalyzer.getType2prop().get(cc.getSingleClass()));
                Set<Property> partialLabelPropertiesSet = new HashSet<>(partialLabelProperties);

                //when there are enough partial labels
                props.retainAll(partialLabelPropertiesSet);
                if (props.size() > 1) {

                    List<List<Property>> subsets = SetUtility.subsetsAsList(new ArrayList<>(props));
                    subsets.removeIf(ss -> ss.size() <= 1);

                    List<List<Property>> listOfPropertyList = new ArrayList<>();
                    for (List<Property> subset : subsets) {
                        for (List<Property> permutation : ListUtility.generatePerm(subset)) {
                            listOfPropertyList.add(permutation);
                        }
                    }

                    //TODO maybe later you define identifying label properties (like "Smith") and additional information properties (like "Mr.")
                    multipleSurfaceFormsConfig.put(cc.getSingleClass().getURI() + "." + Setup.LABEL_PROPERTIES, listOfPropertyList);
                }
            }
        }

        //Property Value as Color
        //find good candidates for properties and their object setting
        //* \<Property-URI\>.\<RDFNode\>.BackgroundColor
        //* \<Property-URI\>.\<RDFNode\>.ForegroundColor
        Map<Resource, Map<Property, Set<RDFNode>>> c2p2oPropValAsColor = new HashMap<>();
        if (propertyValueAsColor) {
            for (ClassConfig classConfig : classConfigs) {
                Set<Resource> instances = rdfsAnalyzer.getType2instances().get(classConfig.getSingleClass());
                Set<Property> properties = rdfsAnalyzer.getType2prop().get(classConfig.getSingleClass());

                //a threshold to stop when the split of the instances raises too high
                int maxSplit = 3;

                //for each property
                for (Property p : properties) {

                    if (p.equals(RDF.type)) {
                        continue;
                    }

                    Map<RDFNode, Set<Resource>> o2inst = new HashMap<>();

                    boolean disjunct = true;

                    //check for each inst
                    for (Resource inst : instances) {
                        for (Statement stmt : SemanticUtility.iterable(rdfsAnalyzer.getModel().listStatements(inst, p, (RDFNode) null))) {

                            //maybe we should not allow literals here
                            if (stmt.getObject().isLiteral()) {
                                disjunct = true;
                                break;
                            }

                            o2inst.computeIfAbsent(stmt.getObject(), o -> new HashSet<>()).add(inst);

                            //if the inst is also in other sets other than the current object it is also not
                            //a good candidate
                            for (Entry<RDFNode, Set<Resource>> e : o2inst.entrySet()) {
                                if (e.getKey().equals(stmt.getObject())) {
                                    continue;
                                }

                                if (e.getValue().contains(inst)) {
                                    disjunct = false;
                                    break;
                                }
                            }

                            if (o2inst.size() > maxSplit || !disjunct) {
                                break;
                            }
                        }

                        if (o2inst.size() > maxSplit || !disjunct) {
                            break;
                        }
                    }

                    if (o2inst.size() > 1 && o2inst.size() <= maxSplit && disjunct) {
                        //if it was well distributed => a good candidate
                        Map<Property, Set<RDFNode>> map = c2p2oPropValAsColor.computeIfAbsent(classConfig.getSingleClass(), c -> new HashMap<>());
                        map.put(p, o2inst.keySet());
                    }
                }
            }
        }

        //Multiple Types in a Table: good class candidates
        List<Entry<Set<ClassConfig>, Double>> propertyOverlapsInClasses = null;
        if (multipleTypesInATable) {
            //Set<ClassConfig> taken = new HashSet<>();
            
            Map<Set<ClassConfig>, Double> classes2overlap = new HashMap<>();
            
            for (ClassConfig left : classConfigs) {
                for (ClassConfig right : classConfigs) {
                    if(left == right) {
                        continue;
                    }
                    
                    Set<ClassConfig> subset = new HashSet<>(Arrays.asList(left, right));
                    //prevent dublicates
                    if(classes2overlap.containsKey(subset)) {
                        continue;
                    }
                    
                    Set<Property> leftProps = new HashSet<>(rdfsAnalyzer.getType2prop().get(left.getSingleClass()));
                    Set<Property> rightProps = new HashSet<>(rdfsAnalyzer.getType2prop().get(right.getSingleClass()));
                
                    Set<Property> allProps = new HashSet<>();
                    allProps.addAll(leftProps);
                    allProps.addAll(rightProps);
                    
                    leftProps.retainAll(rightProps);
                    
                    double overlapping = leftProps.size() / (double) allProps.size();
                    
                    if(overlapping > multipleTypesInATablePropertyOverlapThreshold) {
                        classes2overlap.put(subset, overlapping);
                    }
                }
            }
            
            propertyOverlapsInClasses = new ArrayList<>(classes2overlap.entrySet());
            propertyOverlapsInClasses.sort((a,b) -> Double.compare(b.getValue(), a.getValue()));
        }
        

        //======================================================================
        List<Setup> setups = new ArrayList<>();

        for (int workbookIndex = 0; workbookIndex < numberOfWorkbooks; workbookIndex++) {

            //make a copy so that we can change them
            List<ClassConfig> tmpClassConfigs = new ArrayList<>(classConfigs);
            
            //if multiple types in a table we have to change the classConfigs list by
            //merging some of them
            if(multipleTypesInATable) {
                List<Entry<Set<ClassConfig>, Double>> tmpPropertyOverlapsInClasses = 
                        new ArrayList<>(propertyOverlapsInClasses);
                
                //prevent duplicates
                Set<ClassConfig> usedClassConfigs = new HashSet<>();
                
                for(int i = 0; i < Math.min(tmpPropertyOverlapsInClasses.size(), multipleTypesInATableCount); i++) {
                    
                    Entry<Set<ClassConfig>, Double> e = randomlyRemove(tmpPropertyOverlapsInClasses, rnd);
                    
                    //duplicate
                    boolean duplicate = false;
                    for(ClassConfig cc : e.getKey()) {
                        if(usedClassConfigs.contains(cc)) {
                            duplicate = true;
                            break;
                        }
                    }
                    
                    if(duplicate) {
                        i--;
                        continue;
                    }
                    
                    //remove the single ones
                    List<Resource> classes = new ArrayList<>();
                    for(ClassConfig cc : e.getKey()) {
                        tmpClassConfigs.remove(cc);
                        classes.add(cc.getSingleClass());
                    }
                    
                    //add the multi one
                    tmpClassConfigs.add(new ClassConfig(classes).setLabel(getLabelFor(classes, model, rnd, " + ")));
                    
                    //mark as used
                    usedClassConfigs.addAll(e.getKey());
                }
            }
            
            
            for (ClassConfig classConfig : tmpClassConfigs) {

                Setup setup = new Setup();
                setup.put(Setup.CLASSES, classConfig);
                setup.put(Setup.RANDOM, rnd);
                setup.put("header", true);
                setup.put("headerBackgroundColor", Color.lightGray);
                setup.put("locale", locale);
                //add rdfAnalyzer to setup because we could change the model for each workbook
                setup.put(Setup.RDFS_ANALYZER, rdfsAnalyzer);
                if(classConfig.getClasses().size() > 1) {
                    setup.put("instanceRandomOrder", true);
                }

                setup.put("BooleanRendering", booleanRendering);
                setup.put("BooleanNativeDataFormats", booleanNativeDataFormats);
                setup.put("BooleanTrueSymbols", booleanTrueSymbols);
                setup.put("BooleanFalseSymbols", booleanFalseSymbols);
                setup.put("NumericRendering", numericRendering);
                setup.put("NumericNativeDataFormats", numericNativeDataFormats);
                setup.put("NumberStringFormats", numberStringFormats);
                setup.put("DateTimeRendering", dateTimeRendering);
                setup.put("DateTimeDataFormats", dateTimeDataFormats);
                setup.put("DateTimeStringFormats", dateTimeStringFormats);
                setup.put("DateRendering", dateRendering);
                setup.put("DateDataFormats", dateDataFormats);
                setup.put("DateStringFormats", dateStringFormats);
                setup.put("EmptyCellRendering", emptyCellRendering);
                setup.put("MergeCellStrategy", mergeCellStrategy);
                setup.put("LabelProperties", labelProperties);
                setup.put("LabelPickStrategy", labelPickStrategy);
                setup.put("MergeCellDelimiters", mergeCellDelimiters);
                
                //add default property configs (in case intraCellAdditionalInformation is not activated)
                addDefaultPropertyConfigs(classConfig, setup);

                setup.putAll(multipleSurfaceFormsConfig);
                
                //Intra-Cell Additional Information: PropertyConfig pairs or even three
                if(intraCellAdditionalInformation) {
                    //adds properties to the setup
                    intraCellAdditionalInformation(classConfig, setup, rnd);
                }

                //Partial Formatting Indicates Relations
                //find good candidates for properties, you have to put properties together with PropertyConfig
                //\<Property-URI\>.PartialFormattingIndicatesRelations
                if (partialFormattingIndicatesRelations) {
                    List<PropertyConfig> properties = setup.getOrThrow(Setup.PROPERTIES, List.class);
                    for(PropertyConfig pConfig : properties) {
                        if(!pConfig.isPartialFormattingNeeded())
                            continue;
                        
                        //TODO make configurable
                        List<String> formattings = new ArrayList<>(Arrays.asList(
                                "<b>|</b>", "<i>|</i>", "<u>|</u>",
                                "<font color='#ff0000'>|</font>",
                                "<font color='#008000'>|</font>",
                                "<font color='#0000ff'>|</font>"
                        ));
                        
                        //one stays unformatted
                        for(int i = 0; i < pConfig.getProperties().size() - 1; i++) {
                            String selectedFormatting = randomlyRemove(formattings, rnd);
                            setup.put(pConfig.getProperties().get(i).getURI() + ".PartialFormattingIndicatesRelations", selectedFormatting);
                        }
                    }
                }
                //Outdated is Formatted => may define outdatedProperties
                if (outdatedIsFormatted) {
                    //TODO discuss when should it be applied: only if multiple entries in one cell occur or always?
                    for(Object p : outdatedProperties) {
                        setup.put(((Property)p).getURI() + ".PartialFormattingIndicatesRelations", "<strike>|</strike>");
                    }
                }
                
                if (propertyValueAsColor) {
                    Map<String, List<Color>> ground2colors = new HashMap<>();
                    ground2colors.put("BackgroundColor", randomlySortedColorList(0.3f, 0.93f, rnd));
                    ground2colors.put("ForegroundColor", randomlySortedColorList(0.9f, 0.5f, rnd));
                    
                    for (Resource cls : classConfig.getClasses()) {
                        Map<Property, Set<RDFNode>> p2os = c2p2oPropValAsColor.get(cls);
                        if (p2os != null) {
                            List<Entry<Property, Set<RDFNode>>> entries = new ArrayList<>(p2os.entrySet());
                            Collections.shuffle(entries, rnd);
                            //* \<Property-URI\>.\<RDFNode\>.BackgroundColor
                            //* \<Property-URI\>.\<RDFNode\>.ForegroundColor
                            List<String> grounds = Arrays.asList("BackgroundColor", "ForegroundColor");
                            Collections.shuffle(grounds, rnd);
                            //one for foreground, one for background
                            for(int j = 0; j < Math.min(grounds.size(), entries.size()); j++) {
                                Entry<Property, Set<RDFNode>> entry = entries.get(j);
                                List<RDFNode> objects = new ArrayList<>(entry.getValue());
                                for(int k = 0; k < objects.size(); k++) {
                                    String key = entry.getKey().getURI() + "." + 
                                              objects.get(k).toString() + "." + 
                                              grounds.get(j);
                                    Color color = randomlyRemove(ground2colors.get(grounds.get(j)), rnd);
                                    setup.put(key, color);
                                    
                                    colorCodes.append(cls + " | " + key + " => " + color + "\n");
                                }
                                
                                //do not remove the property statements because now it is color coded
                                //you just correct the property configs
                                List<PropertyConfig> pConfigs = setup.getOrThrow(Setup.PROPERTIES, List.class);
                                List<PropertyConfig> pConfigsToBeRemoved = new ArrayList<>();
                                for(PropertyConfig pConfig : pConfigs) {
                                    //if the property config has the property that is now color coded
                                    if(pConfig.getProperties().contains(entry.getKey())) {
                                        pConfig.getProperties().remove(entry.getKey());
                                        
                                        //if empty remove whole object
                                        if(pConfig.getProperties().isEmpty()) {
                                            pConfigsToBeRemoved.add(pConfig);
                                        } else {
                                            pConfig.setLabel(getLabelFor(SemanticUtility.toResourceList(pConfig.getProperties()), model, rnd));
                                        }
                                    }
                                }
                                
                                pConfigs.removeAll(pConfigsToBeRemoved);
                            }
                        }
                    }
                }

                setups.add(setup);
            }
            
            colorCodes.append("\n");
        }
        

        return setups;
    }
    
    private void addDefaultPropertyConfigs(ClassConfig classConfig, Setup setup) {
        //collect all properties 
        Set<Property> allProperties = new HashSet<>();
        for(Resource cls : classConfig.getClasses()) {
            allProperties.addAll(rdfsAnalyzer.getType2prop().get(cls));
        }
        
        allProperties.remove(RDF.type);
        
        List<PropertyConfig> pConfigs = new ArrayList<>();
        for(Property p : allProperties) {
            pConfigs.add(new PropertyConfig(p));
        }
        
        setup.put(Setup.PROPERTIES, pConfigs);
    }
    
    private void intraCellAdditionalInformation(ClassConfig classConfig, Setup setup, Random rnd) {
        //collect all properties 
        Set<Property> allProperties = new HashSet<>();
        for(Resource cls : classConfig.getClasses()) {
            allProperties.addAll(rdfsAnalyzer.getType2prop().get(cls));
        }

        allProperties.remove(RDF.type);

        //merge properties based on range information
        //this way, for example, 
        Map<Resource, List<Property>> range2properties = new HashMap<>();
        List<Property> properties = new ArrayList<>(allProperties);
        for(Property prop : properties) {
            for(Resource range : rdfsAnalyzer.getPropertyRanges().get(prop)) {
                range2properties.computeIfAbsent(range, l -> new ArrayList<>()).add(prop);
            }
        }

        //to prevent duplicates
        Set<Property> usedProperties = new HashSet<>();
        Set<Property> remainingProperties = new HashSet<>(allProperties);
        
        //create property configs for the multi
        List<PropertyConfig> propertyConfigs = new ArrayList<>();
        for(Entry<Resource, List<Property>> e : range2properties.entrySet()) {
            
            //we remove the already used ones
            e.getValue().removeAll(usedProperties);

            //if multi, we pick only some
            if(e.getValue().size() > 1) {
                
                //shuffel and pick only some of them
                Collections.shuffle(e.getValue(), rnd);
                
                //2 + (0 or 1)
                int max = Math.min(e.getValue().size(), 2 + rnd.nextInt(2));
                List<Property> picked = new ArrayList<>();
                for(int i = 0; i < max; i++) {
                    Property prop = e.getValue().get(i);
                    
                    //remove them so they are taken
                    //we will add the remaining then
                    remainingProperties.remove(prop);
                    //we add them here to check for duplicates
                    usedProperties.add(prop);
                    
                    picked.add(prop);
                }
                
                //create a conf for the picked one (multi)
                PropertyConfig conf = new PropertyConfig(picked);
                propertyConfigs.add(conf);
                
                //if multi give it a uri and a label
                conf.setUri("uuid:" + UUID.randomUUID().toString());
                conf.setLabel(getLabelFor(SemanticUtility.toResourceList(conf.getProperties()), rdfsAnalyzer.getModel(), rnd));
                //its needed because they have same range type
                conf.setPartialFormattingNeeded(true);
            }
        }
        
        //put a string together with another type (no partial formatting needed)
        List<Property> stringProperties = range2properties.get(XSD.xstring);
        if(stringProperties != null) {
            stringProperties.removeAll(usedProperties);
            if(stringProperties.size() > 1) {
                Map<Resource, List<Property>> r2ps = new HashMap<>(range2properties);
                r2ps.remove(XSD.xstring);
                
                Property selected = randomlySelect(stringProperties, rnd);
                usedProperties.add(selected);
                remainingProperties.remove(selected);

                for(Entry<Resource, List<Property>> e : r2ps.entrySet()) {
                    
                    //search for antoher one
                    e.getValue().removeAll(usedProperties);
                    if(e.getValue().isEmpty()) {
                       continue; 
                    }
                    
                    Property another = randomlySelect(e.getValue(), rnd);
                    usedProperties.add(another);
                    remainingProperties.remove(another);

                    //create a conf for the picked one (multi)
                    PropertyConfig conf = new PropertyConfig(new ArrayList<>(Arrays.asList(selected, another)));
                    propertyConfigs.add(conf);

                    //if multi give it a uri and a label
                    conf.setUri("uuid:" + UUID.randomUUID().toString());
                    conf.setLabel(getLabelFor(SemanticUtility.toResourceList(conf.getProperties()), rdfsAnalyzer.getModel(), rnd));
                    
                    break;
                }
            }
        }
        
        //remaining ones, can only be single
        for(Property remaining : remainingProperties) {
            propertyConfigs.add(new PropertyConfig(remaining));
        }
        
        setup.put(Setup.PROPERTIES, propertyConfigs);
    }
    
    public RdfsAnalyzer getRdfsAnalyzer() {
        return rdfsAnalyzer;
    }

    public boolean isNumericInformationAsText() {
        return numericInformationAsText;
    }

    public PatternsToSetups setNumericInformationAsText(boolean numericInformationAsText) {
        this.numericInformationAsText = numericInformationAsText;
        return this;
    }

    public boolean isAcronymsOrSymbols() {
        return acronymsOrSymbols;
    }

    public PatternsToSetups setAcronymsOrSymbols(boolean acronymsOrSymbols) {
        this.acronymsOrSymbols = acronymsOrSymbols;
        return this;
    }

    public boolean isMultipleSurfaceForms() {
        return multipleSurfaceForms;
    }

    public PatternsToSetups setMultipleSurfaceForms(boolean multipleSurfaceForms) {
        this.multipleSurfaceForms = multipleSurfaceForms;
        return this;
    }

    public boolean isPropertyValueAsColor() {
        return propertyValueAsColor;
    }

    public PatternsToSetups setPropertyValueAsColor(boolean propertyValueAsColor) {
        this.propertyValueAsColor = propertyValueAsColor;
        return this;
    }

    public boolean isPartialFormattingIndicatesRelations() {
        return partialFormattingIndicatesRelations;
    }

    public PatternsToSetups setPartialFormattingIndicatesRelations(boolean partialFormattingIndicatesRelations) {
        this.partialFormattingIndicatesRelations = partialFormattingIndicatesRelations;
        return this;
    }

    public boolean isOutdatedIsFormatted() {
        return outdatedIsFormatted;
    }

    public PatternsToSetups setOutdatedIsFormatted(boolean outdatedIsFormatted) {
        this.outdatedIsFormatted = outdatedIsFormatted;
        return this;
    }

    public boolean isMultipleEntitiesInOneCell() {
        return multipleEntitiesInOneCell;
    }

    public PatternsToSetups setMultipleEntitiesInOneCell(boolean multipleEntitiesInOneCell) {
        this.multipleEntitiesInOneCell = multipleEntitiesInOneCell;
        return this;
    }

    public boolean isIntraCellAdditionalInformation() {
        return intraCellAdditionalInformation;
    }

    public PatternsToSetups setIntraCellAdditionalInformation(boolean intraCellAdditionalInformation) {
        this.intraCellAdditionalInformation = intraCellAdditionalInformation;
        return this;
    }

    public boolean isMultipleTypesInATable() {
        return multipleTypesInATable;
    }

    public PatternsToSetups setMultipleTypesInATable(boolean multipleTypesInATable) {
        this.multipleTypesInATable = multipleTypesInATable;
        return this;
    }

    public String getColorCodes() {
        return colorCodes.toString();
    }
    
    //==========================
    public Locale getLocale() {
        return locale;
    }

    public List getBooleanNativeDataFormats() {
        return booleanNativeDataFormats;
    }

    public List getBooleanTrueSymbols() {
        return booleanTrueSymbols;
    }

    public List getBooleanFalseSymbols() {
        return booleanFalseSymbols;
    }

    public List getNumericNativeDataFormats() {
        return numericNativeDataFormats;
    }

    public List getDateTimeDataFormats() {
        return dateTimeDataFormats;
    }

    public List getDateTimeStringFormats() {
        return dateTimeStringFormats;
    }

    public List getDateDataFormats() {
        return dateDataFormats;
    }

    public List getDateStringFormats() {
        return dateStringFormats;
    }

    public List getMergeCellDelimiters() {
        return mergeCellDelimiters;
    }

    public List getAcronymProperties() {
        return acronymProperties;
    }

    public List getLabelProperties() {
        return labelProperties;
    }

    /**
     * Use this to describe, for example, that Person has partial labels first
     * name and last name. This is used to perform the pattern "Multiple Surface
     * Forms".
     *
     * @return
     */
    public List getPartialLabelProperties() {
        return partialLabelProperties;
    }

    public List getOutdatedProperties() {
        return outdatedProperties;
    }
    
    //==========================================================================
    
    private List<Color> randomlySortedColorList(float s, float b, Random rnd) {
        List<Color> colors = new ArrayList<>(Arrays.asList(
                Color.getHSBColor(0.1f,s,b),
                Color.getHSBColor(0.2f,s,b),
                Color.getHSBColor(0.3f,s,b),
                Color.getHSBColor(0.4f,s,b),
                Color.getHSBColor(0.5f,s,b),
                Color.getHSBColor(0.6f,s,b),
                Color.getHSBColor(0.7f,s,b),
                Color.getHSBColor(0.8f,s,b),
                Color.getHSBColor(0.9f,s,b),
                Color.getHSBColor(1.0f,s,b),
                s <= 0.5 ? Color.white : Color.black
        ));
        Collections.shuffle(colors, rnd);
        return colors;
    }

    private <T> T randomlyRemove(List<T> l, Random rnd) {
        if(l == null || l.isEmpty()) {
            throw new RuntimeException("list is empty");
        }
        return l.remove(rnd.nextInt(l.size()));
    }
    
    private <T> T randomlySelect(List<T> l, Random rnd) {
        if(l == null || l.isEmpty()) {
            throw new RuntimeException("list is empty");
        }
        return l.get(rnd.nextInt(l.size()));
    }
    
    private String getLabelFor(List<Resource> resources, Model model, Random rnd) {
        return getLabelFor(resources, model, rnd, null);
    }
    
    private String getLabelFor(List<Resource> resources, Model model, Random rnd, String forcedDelimiter) {
        String delimiter;
        if(forcedDelimiter != null) {
            delimiter = forcedDelimiter;
        } else {
            delimiter = (String) mergeCellDelimiters.get(rnd.nextInt(mergeCellDelimiters.size()));
        }
        StringJoiner sj = new StringJoiner(delimiter);
        for(Resource res : resources) {
            Literal lit = SemanticUtility.literal(model, res, RDFS.label);
            if(lit != null) {
                sj.add(lit.getLexicalForm());
            } else {
                sj.add(SemanticUtility.getLocalName(res.getURI()));
            }
        }
        return sj.toString();
    }
    
    
}
