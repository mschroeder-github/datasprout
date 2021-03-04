package de.dfki.sds.datasprout.excel;

import de.dfki.sds.datasprout.Provenance;
import de.dfki.sds.datasprout.Setup;
import de.dfki.sds.datasprout.excel.Patterns.BooleanRendering;
import de.dfki.sds.datasprout.excel.Patterns.DateRendering;
import de.dfki.sds.datasprout.excel.Patterns.DateTimeRendering;
import de.dfki.sds.datasprout.excel.Patterns.EmptyCellRendering;
import de.dfki.sds.datasprout.excel.Patterns.LabelPickStrategy;
import de.dfki.sds.datasprout.excel.Patterns.MergeCellStrategy;
import de.dfki.sds.datasprout.excel.Patterns.NumberStringFormats;
import de.dfki.sds.datasprout.excel.Patterns.NumericRendering;
import de.dfki.sds.datasprout.utils.RomanNumber;
import de.dfki.sds.datasprout.utils.SemanticUtility;
import de.dfki.sds.hephaistos.storage.excel.ExcelCell;
import de.dfki.sds.rdf2rdb.RdfsAnalyzer;
import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.jena.datatypes.BaseDatatype.TypedValue;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellAddress;
import org.json.JSONArray;

/**
 *
 */
public class TableGenerator {

    //use this map to assign a data type uri to a conversion function 
    //that converts a lexical value to an object
    private Map<String, Function<String, Object>> dataTypeMap;
    
    public TableGenerator() {
        dataTypeMap = new HashMap<>();
    }

    public List<ExcelTable> generateList(List<Setup> setups, IdCounter idCounter, ExcelSproutOptions options) {
        List<ExcelTable> result = new ArrayList<>();
        for (Setup setup : setups) {
            result.add(generate(setup, setup.getOrThrow(Setup.RDFS_ANALYZER, RdfsAnalyzer.class), idCounter, options));
        }
        return result;
    }

    //based on the settings in the setup and the model we create a excel table
    public ExcelTable generate(Setup setup, RdfsAnalyzer rdfAnalyzer, IdCounter idCounter, ExcelSproutOptions options) {
        ExcelTable table = new ExcelTable();
        table.setIdCounter(idCounter);

        /*
        a) the setup selected classes, their instances should be rows
        a1) how to select instances?
        a2) how many?
        b) the setup selected instances, they will be rows
        
        c) the properties become columns
        c1) with header or without header
        
         */
        Model model = rdfAnalyzer.getModel();
        ClassConfig classConfig = setup.getOrThrow(Setup.CLASSES, ClassConfig.class);

        Random rnd = setup.getSingleOrThrow("random", Random.class);

        List<Resource> instances = new ArrayList<>();
        List<PropertyConfig> propertyConfigs = new ArrayList<>();
        for (Resource cls : classConfig.getClasses()) {
            //we allow subsets of classes so that a table can have instance of two or more classes
            Set<Resource> instancesOfClass = rdfAnalyzer.getType2instances().getOrDefault(cls, new HashSet<>());
            instances.addAll(instancesOfClass);

            for(Property p : rdfAnalyzer.getType2prop().getOrDefault(cls, new HashSet<>())) {

                if(p.equals(RDF.type)) {
                    //we just do not add it
                    continue;
                }

                propertyConfigs.add(new PropertyConfig(p));
            }
        }
        //TODO getSingleOrDefault
        boolean instanceRandomOrder = (boolean) setup.getOrDefault("instanceRandomOrder", false);
        if (instanceRandomOrder) {
            Collections.shuffle(instances, rnd);
        }
        //TODO order instances by certain property first

        //setup can select the properties
        if (setup.containsKey(Setup.PROPERTIES)) {
            propertyConfigs = (List<PropertyConfig>) setup.get(Setup.PROPERTIES);
        }

        if (setup.containsKey(Setup.INSTANCE_FILTER)) {
            BiFunction<Resource, Model, Boolean> f = (BiFunction<Resource, Model, Boolean>) setup.getSingle(Setup.INSTANCE_FILTER);
            instances.removeIf(r -> !f.apply(r, model));
        }

        boolean header = setup.getSingleOrThrow("header", Boolean.class);
        
        //Pattern: Multiple Types in a Table (special case: hierarchy)
        if(setup.containsKey("MultipleTypesInATable.ChildProperty")) {
            Property childProperty = setup.getOrThrow("MultipleTypesInATable.ChildProperty", Property.class);
            
            List<Resource> tmpInstances = new ArrayList<>();
            
            //for now we allow only one extra level
            for(Resource inst : instances) {
                tmpInstances.add(inst);
                
                List<Resource> children = SemanticUtility.objects(model, inst, childProperty);
                
                //TODO maybe order children here
                
                tmpInstances.addAll(children);
            }
            
            instances = tmpInstances;
            
            //TODO provenance statements for children
        }

        int h = (header ? 1 : 0) + instances.size();
        int w = propertyConfigs.size();

        //row x column
        ExcelCell[][] data = new ExcelCell[h][w];

        if (header) {
            for (int i = 0; i < w; i++) {
                PropertyConfig propertyConfig = propertyConfigs.get(i);
                
                String name;
                if(propertyConfig.hasLabel()) {
                    name = propertyConfig.getLabel();
                    
                } else {
                    Literal labelLit = SemanticUtility.literal(model, propertyConfig.getProperty(), RDFS.label);

                    if (labelLit != null) {
                        name = labelLit.getString();
                    } else {
                        name = SemanticUtility.getLocalName(propertyConfig.getProperty().getURI());
                    }
                }
                
                if(name == null) {
                    //this should not happen
                    //name = RandomStringUtils.randomAlphabetic(6);
                    throw new RuntimeException("property name is null");
                }

                ExcelCell cell = new ExcelCell();
                cell.setId(table.getAndIncCellId());
                cell.setCellType("string");
                cell.setValueString(name);
                //for provenance
                cell.setAddress(new CellAddress(0, i).formatAsString());
                cell.setColumn(i);

                Color bg = (Color) setup.getSingle("headerBackgroundColor");
                if (bg != null) {
                    cell.setBackgroundColor(bg);
                    table.putUsedPattern(cell, "headerBackgroundColor", bg);
                }

                data[0][i] = cell;
                
                //provenance
                for(Property p : propertyConfig.getProperties()) {
                    table.addStatement(cell, propertyConfig.getProperty(), RDF.type, RDF.Property);
                    if(!propertyConfig.isMulti()) {
                        table.addStatement(cell, propertyConfig.getProperty(), RDFS.label, ResourceFactory.createPlainLiteral(name));
                    }
                }
                if(propertyConfig.hasObject() && propertyConfig.getObject().isResource()) {
                    table.addStatement(cell, propertyConfig.getObject().asResource(), RDF.type, RDFS.Resource);
                }
                
                if(propertyConfig.isMulti()) {
                    JSONArray array = new JSONArray();
                    for(Property prop : propertyConfig.getProperties()) {
                        array.put(prop.getURI());
                    }
                    table.putUsedPattern(cell, "intra-CellAdditionalInformation", array);
                }
                
                //if multiple types in a table use the first header column to state that fact
                if(i == 0 && classConfig.getClasses().size() > 1) {
                    JSONArray array = new JSONArray();
                    for(Resource type : classConfig.getClasses()) {
                        array.put(type.getURI());
                    }
                    table.putUsedPattern(cell, "MultipleTypesInATable", array);
                }
            }
        }

        //TODO classes are split up in different tables
        //for each instance
        for (int j = 0; j < instances.size(); j++) {
            Resource instance = instances.get(j);

            int row = j + (header ? 1 : 0);
            
            //for each property
            for (int i = 0; i < w; i++) {
                
                int col = i;
                
                //sometimes multiple properties are merged to one cell
                //this can be configured in PropertyConfig
                List<Property> properties = propertyConfigs.get(i).getProperties();

                //all objects
                Map<Property, List<RDFNode>> property2objects = new HashMap<>();
                List<RDFNode> objects = new ArrayList<>();
                for(Property prop : properties) {
                    List<RDFNode> nodes = SemanticUtility.rdfnodes(model, instance, prop);
                    if(!nodes.isEmpty()) {
                        property2objects.put(prop, nodes);
                    }
                    objects.addAll(nodes);
                }

                ExcelCell cell = new ExcelCell();
                cell.setId(table.getAndIncCellId());
                //for provenance
                cell.setAddress(new CellAddress(row, col).formatAsString());
                cell.setRow(row);
                cell.setColumn(col);
                
                

                boolean add;

                //this instance has objects for this property
                if (!objects.isEmpty()) {

                    //one object
                    //means: one property => one object (simple)
                    //special case: the property config has an object, thus it is a question if this relation exists or not
                    if (objects.size() == 1 || propertyConfigs.get(i).hasObject()) {
                        add = putObject(instance, propertyConfigs.get(i), property2objects, rdfAnalyzer, cell, table, setup);
                        
                        //pattern: Partial Formatting Indicates Relations =======
                        if(add) {
                            String[] openCloseTags = null;
                            StringBuilder sb = new StringBuilder();
                            
                            //we try to find a propery that has a PartialFormattingIndicatesRelations key in setup
                            for(Entry<Property, List<RDFNode>> property2objectsEntry : property2objects.entrySet()) {
                                String key = property2objectsEntry.getKey().getURI() + ".PartialFormattingIndicatesRelations";
                                if(setup.containsKey(key)) {
                                    String formatting = setup.getOrThrow(key, String.class);
                                    
                                    table.putUsedPattern(cell, key, formatting);

                                    if(!formatting.contains("|")) {
                                        throw new RuntimeException("put a '|' in the formatting of " + key);
                                    }

                                    openCloseTags = formatting.split("\\|");
                                    break;
                                }
                            }
                            
                            //in case of rich text found
                            if(openCloseTags != null) {
                                sb.append(openCloseTags[0]);
                                
                                sb.append(toString(cell, propertyConfigs.get(i), table, setup).replace("<", "&lt;").replace(">", "&gt;"));
                                
                                sb.append(openCloseTags[1]);
                                
                                //update
                                cell.setCellType("string");
                                cell.setValueRichText(sb.toString());
                                cell.setValueString(null);
                                cell.setValueNumeric(0.0);
                                cell.setValueBoolean(false);
                            }
                        }

                    } else {
                        //maybe multiple properties and multiple objects
                        add = putMultipleObjects(instance, propertyConfigs.get(i), property2objects, rdfAnalyzer, cell, table, setup);
                    }

                } else {
                    //no object
                    add = putNoObject(instance, propertyConfigs.get(i), cell, table, setup);
                }

                if (add) {
                    data[row][col] = cell;
                }
            }
        }

        //pattern: Property Value as Color
        //for certain instances that have in a certain property a certain value, we color the lines (foreground=font-color or background)
        //<Property-URI>.<RDFNode>.ForegroundColor = new java.awt.Color()
        //<Property-URI>.<RDFNode>.BackgroundColor = new java.awt.Color()
        for (int j = 0; j < instances.size(); j++) {
            Resource instance = instances.get(j);
            
            String fgKeySelected = null;
            String bgKeySelected = null;
            Color foregroundColor = null;
            Color backgroundColor = null;
            for(Statement stmt : SemanticUtility.iterable(model.listStatements(instance, null, (RDFNode)null))) {
                String fgKey = stmt.getPredicate().getURI() + "." + stmt.getObject().toString() + ".ForegroundColor";
                String bgKey = stmt.getPredicate().getURI() + "." + stmt.getObject().toString() + ".BackgroundColor";
                if(setup.containsKey(fgKey)) {
                    foregroundColor = setup.getOrThrow(fgKey, Color.class);
                    fgKeySelected = fgKey;
                }
                if(setup.containsKey(bgKey)) {
                    backgroundColor = setup.getOrThrow(bgKey, Color.class);
                    bgKeySelected = bgKey;
                }
            }

            if(foregroundColor != null || backgroundColor != null) {
                //for each property
                for (int i = 0; i < w; i++) {
                    
                    int row = j + (header ? 1 : 0);
                    int col = i;
                    
                    ExcelCell cell = data[row][col];
                    
                    //if real empty make a cell that is string empty
                    //thus, you can attach the color
                    if(cell == null) {
                        cell = new ExcelCell();
                        cell.setCellType("string");
                        cell.setValueString("");
                        //for provenance
                        cell.setAddress(new CellAddress(row, col).formatAsString());
                        cell.setRow(row);
                        cell.setColumn(col);
                        
                        data[row][col] = cell;
                    }
                    
                    if(foregroundColor != null) {
                        cell.setFontColor(foregroundColor);
                        table.putUsedPattern(cell, fgKeySelected, foregroundColor);
                    }
                    if(backgroundColor != null) {
                        cell.setBackgroundColor(backgroundColor);
                        table.putUsedPattern(cell, bgKeySelected, backgroundColor);
                    }
                }
            }
        }
        
        //TODO later: transpose (header is left, columns become rows and rows become columns)
        table.setData(data);
        table.setSetup(setup);
        return table;
    }

    private boolean putObject(Resource s, PropertyConfig pConfig, Map<Property, List<RDFNode>> property2objects, RdfsAnalyzer rdfAnalyzer, ExcelCell cell, ExcelTable table, Setup setup) {
        Model model = rdfAnalyzer.getModel();
        
        Property p;
        RDFNode object;
        
        if(pConfig.hasObject()) {
            //special case that we actually have a boolean value here
            //because we check if the spo statement exists
            //in this case we assume that there is only one property
            
            boolean statementExists = false;
            
            List<RDFNode> nodes = property2objects.get(pConfig.getProperty());
            if(nodes != null) {
                statementExists = nodes.contains(pConfig.getObject());
            }
            
            p = pConfig.getProperty();
            
            //having a boolean literal now, the correct if case will be used
            object = ResourceFactory.createTypedLiteral(statementExists);
        } else {
        
            //should be only one entry here
            Entry<Property, List<RDFNode>> e = property2objects.entrySet().iterator().next();
        
            p = e.getKey();
            //should be only one object here
            object = e.getValue().get(0);
        }
        

        boolean objectIsResource = object.isResource();
        boolean objectIsLiteral = object.isLiteral();

        if (objectIsLiteral) {
            //type of literal

            Literal literal = object.asLiteral();
            String datatype = literal.getDatatypeURI();
            Object value = literal.getValue();
            
            if(value instanceof TypedValue) {
                TypedValue typedValue = (TypedValue) value;
                
                Function<String, Object> func = dataTypeMap.get(datatype);

                if(func == null) {
                    throw new RuntimeException(datatype + " no conversion function found");
                }
                
                value = func.apply(typedValue.lexicalValue);
            }

            //cell-type: boolean, numeric, richtext, string
            if (value instanceof String || datatype.equals(XSD.xstring.getURI())) {

                cell.setCellType("string");
                cell.setValueString((String) value);

                //TODO string literal: different ways to change the string
                //e.g. add spelling error, change case
                
                table.addStatement(cell, s, p, object);
                
                //we add the type information for label properties
                List labelProperties = (List) setup.get(Setup.LABEL_PROPERTIES);
                if(labelProperties != null) {
                    if(labelProperties.contains(p)) {
                        List<Resource> types = SemanticUtility.objects(model, s, RDF.type);
                        for(Resource type : types) {
                            table.addStatement(cell, s, RDF.type, type);
                        }
                    }
                }
                
                return true;

            } else if (value instanceof Number) {

                NumericRendering rend = selectPattern("NumericRendering", NumericRendering.class, pConfig, setup, cell, table);

                Number number = (Number) value;

                switch (rend) {
                    //as a number
                    case Native:
                        cell.setValueNumeric(number.doubleValue());
                        cell.setCellType("numeric");

                        String format = selectPattern("NumericNativeDataFormats", String.class, pConfig, setup, cell, table);
                        cell.setDataFormat(format);
                        break;

                    //as a string
                    case String:
                        String str = numberToString(number, pConfig, setup, cell, table);

                        cell.setValueString(str);
                        cell.setCellType("string");
                        break;
                }

                table.addStatement(cell, s, p, object);
                return true;

            } else if (value instanceof Boolean || datatype.equals(XSD.xboolean.getURI())) {

                //check what patterns are possible and select one
                BooleanRendering boolRender = selectPattern("BooleanRendering", BooleanRendering.class, pConfig, setup, cell, table);

                switch (boolRender) {

                    //value is stored "as is"
                    case Native:
                        cell.setValueBoolean((boolean) value);
                        cell.setCellType("boolean");

                        String format = selectPattern("BooleanNativeDataFormats", String.class, pConfig, setup, cell, table);
                        cell.setDataFormat(format);
                        break;

                    case Symbol:
                        String symbol = booleanToString((boolean) value, pConfig, setup, cell, table);

                        cell.setCellType("string");
                        cell.setValueString(symbol);
                        break;

                    case Numeric:
                        //select a number
                        double number;
                        if ((boolean) value) {
                            number = selectPattern("BooleanTrueNumbers", Double.class, pConfig, setup, cell, table);
                        } else {
                            number = selectPattern("BooleanFalseNumbers", Double.class, pConfig, setup, cell, table);
                        }
                        cell.setCellType("numeric");
                        cell.setValueNumeric(number);
                        break;
                }

                if(pConfig.hasObject()) {
                    //special case: the object is not the boolean, it is the statement that exists
                    table.addStatement(cell, s, p, pConfig.getObject());
                    table.putUsedPattern(cell, p.getURI() + ".AcronymsOrSymbols", pConfig.getObject().toString());
                } else {
                    table.addStatement(cell, s, p, object);
                }
                return true;

            } else if (value instanceof XSDDateTime) {
                //numeric date
                XSDDateTime dateTime = (XSDDateTime) value;

                if(datatype.equals(XSD.date.getURI())) {
                    DateRendering dateRendering = selectPattern("DateRendering", DateRendering.class, pConfig, setup, cell, table);
                    
                    switch (dateRendering) {
                        case Numeric: {

                            Date d = new Date(dateTime.getYears() - 1900, dateTime.getMonths() - 1, dateTime.getDays());
                            double dateValue = DateUtil.getExcelDate(d);

                            cell.setCellType("numeric");
                            cell.setValueNumeric(dateValue);

                            String format = selectPattern("DateDataFormats", String.class, pConfig, setup, cell, table);
                            cell.setDataFormat(format);
                            break;
                        }
                        case String: {
                            LocalDate ldt = LocalDate.of(
                                    dateTime.getYears(),
                                    dateTime.getMonths(),
                                    dateTime.getDays()
                            );
                            String format = selectPattern("DateStringFormats", String.class, pConfig, setup, cell, table);
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
                            String date = ldt.format(dtf);
                            cell.setCellType("string");
                            cell.setValueString(date);
                            break;
                        }
                    }
                    
                } else if(datatype.equals(XSD.dateTime.getURI())) {
                    DateTimeRendering dateTimeRendering = selectPattern("DateTimeRendering", DateTimeRendering.class, pConfig, setup, cell, table);
                    
                    switch (dateTimeRendering) {
                        case Numeric: {

                            Date d = new Date(dateTime.getYears() - 1900, dateTime.getMonths() - 1, dateTime.getDays());
                            double timeValue = DateUtil.convertTime(dateTime.getHours() + ":" + dateTime.getMinutes() + ":" + dateTime.getFullSeconds());
                            double dateValue = DateUtil.getExcelDate(d);

                            cell.setCellType("numeric");
                            cell.setValueNumeric(dateValue + timeValue);

                            String format = selectPattern("DateTimeDataFormats", String.class, pConfig, setup, cell, table);
                            cell.setDataFormat(format);
                            break;
                        }
                        case String: {
                            LocalDateTime ldt = LocalDateTime.of(
                                    dateTime.getYears(),
                                    dateTime.getMonths(),
                                    dateTime.getDays(),
                                    dateTime.getHours(),
                                    dateTime.getMinutes(),
                                    dateTime.getFullSeconds()
                            );
                            String format = selectPattern("DateTimeStringFormats", String.class, pConfig, setup, cell, table);
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
                            String date = ldt.format(dtf);
                            cell.setCellType("string");
                            cell.setValueString(date);
                            break;
                        }
                    }
                }

                table.addStatement(cell, s, p, object);
                return true;

            } else {
                throw new RuntimeException("not yet implemented what to do for " + value + " (" + datatype + ", " + value.getClass() + ")");
            }

        } else if (objectIsResource) {
            //how to refer to it
            Resource res = object.asResource();

            objectIsResource(res, pConfig, model, setup, cell, table, s, p, object);
            
            return true;
        }

        return false;
    }

    private void objectIsResource(Resource res, PropertyConfig pConfig, Model model, Setup setup, ExcelCell cell, ExcelTable table, Resource s, Property p, RDFNode object) {
        List<Resource> types = SemanticUtility.objects(model, res, RDF.type);

        //also type provenance
        for(Resource type : types) {
            table.addStatement(cell, res, RDF.type, type);
        }
        
        //type based property selection
        for(Resource type : types) {
            String key = type.getURI() + "." + Setup.LABEL_PROPERTIES;
            if(setup.containsKey(key)) {
                
                List<Property> propertyList = (List<Property>) setup.getByDistribution(key);
                
                StringJoiner sj = new StringJoiner(" ");
                
                for(Property labelProp : propertyList) {
                    
                    RDFNode label = SemanticUtility.rdfnode(model, res, labelProp);
                    
                    if(label != null) {
                        sj.add(label.asLiteral().getLexicalForm());
                        table.addStatement(cell, res, labelProp, label);
                    }
                }
                
                
                //put in cell
                cell.setCellType("string");
                cell.setValueString(sj.toString());
                
                JSONArray array = new JSONArray();
                for(Property prop : propertyList) {
                    array.put(prop.getURI());
                }
                
                //provenance
                table.putUsedPattern(cell, key, array);
                table.addStatement(cell, s, p, object);
                
                //return so that the below code is not executed
                return;
            }
        }
        
        //here is the case when we use just one of the LabelProperties
        
        //use label literal if possible to refer to it
        //what label do we use to refer to the resource
        int labelPropertyIterMax = (int) setup.getOrDefault("LabelPropertyIterMax", 100);
        int labelPropertyIter = 0;
        Property labelProperty;
        do {
            //we select one
            labelProperty = selectPattern(Setup.LABEL_PROPERTIES, Property.class, pConfig, setup, cell, table);

            //a safety net: we stop after max iterations
            labelPropertyIter++;

            //loop until we find a labelProperty that has this object
        } while (!model.contains(res, labelProperty, (RDFNode) null) && labelPropertyIter < labelPropertyIterMax);

        //the possible labels
        List<Literal> labels = new ArrayList<>();
        if (labelPropertyIter >= labelPropertyIterMax) {
            //TODO object ref: maybe use full URI and style to make a link in Excel?
            //TODO blank node without any label property, what to do?
            //fallback: use localname of the object
            String localname = SemanticUtility.getLocalName(res.getURI());
            
            if (localname == null) {
                localname = res.getURI();
                
                //String desc = SemanticUtility.describe(model, res);
                //if(!desc.trim().isEmpty()) {
                //    System.out.println("we have to use the full URI to refer to");
                //    System.out.println(desc);
                //}
            }
            
            labels.add(ResourceFactory.createPlainLiteral(localname));
            labelProperty = null;
            
        } else {
            //collect all the labels using the label property
            for (Statement stmt : SemanticUtility.iterable(model.listStatements(res, labelProperty, (RDFNode) null))) {
                if (stmt.getObject().isLiteral()) {
                    labels.add(stmt.getLiteral());
                }
            }
        }

        Literal label;
        if (labels.size() > 1) {
            //there could be many, so pick one with a strategy
            LabelPickStrategy labelPickStrategy = selectPattern("LabelPickStrategy", LabelPickStrategy.class, pConfig, setup, cell, table);

            switch (labelPickStrategy) {
                case Alphabetical:
                    labels.sort((a, b) -> {
                        return a.getLexicalForm().compareTo(b.getLexicalForm());
                    });
                    break;

                case Longest:
                case Shortest:
                    labels.sort((a, b) -> {
                        int cmp = Integer.compare(a.getLexicalForm().length(), b.getLexicalForm().length());
                        if (labelPickStrategy == LabelPickStrategy.Longest) {
                            cmp *= -1;
                        }
                        return cmp;
                    });
                    break;
            }

            //get after sort the first one
            label = labels.get(0);

        } else {
            //just one, so take it
            label = labels.get(0);
        }
        
        //the label as a string
        String labelString = label.getLexicalForm();

        //TODO object ref: maybe we use a string transformation for label
        //TODO put this as a transforming method that can be reused
        //transformation is maybe again a subset of transformations so that multiple ones can be applied in sequence
        //a) only certain tokens of the name
        //b) an acronym
        //c) other case (upper, lower, etc)
        //d) maybe rich text is used and some style is applied
        //e) spell error somewhere
        cell.setCellType("string");
        cell.setValueString(labelString);

        table.addStatement(cell, s, p, object);
        if (labelProperty != null) {
            table.addStatement(cell, object.asResource(), labelProperty, ResourceFactory.createPlainLiteral(labelString));
        } else {
            //we used localname or URI
        }
    }

    private boolean putMultipleObjects(Resource s, PropertyConfig pConfig, Map<Property, List<RDFNode>> property2objects, RdfsAnalyzer rdfAnalyzer, ExcelCell mergedCell, ExcelTable table, Setup setup) {
        //multiple objects
        //different ways to write multiple objects in one cell

        //Map<ExcelCell, Map<RDFNode, List<Property>>> cell2node2properties = new HashMap<>();
        Map<RDFNode, List<Property>> node2properties = new HashMap<>();
        Map<ExcelCell, List<RDFNode>> cell2nodes = new HashMap<>();
        
        //for each property and for each property value
        //create a cell that can be merged
        List<ExcelCell> cells = new ArrayList<>();
        for(Property prop : pConfig.getProperties()) {
            if(property2objects.containsKey(prop)) {
                for (RDFNode object : property2objects.get(prop)) {
                    ExcelCell tmpCell = new ExcelCell();
                    tmpCell.setId(table.getAndIncCellId());
                    //we do not set the address so it will not be in the provenance
                    //only the mergedCell should be in the provenance information later
                    
                    //single use case
                    Map<Property, List<RDFNode>> tmpProperty2objects = new HashMap<>();
                    tmpProperty2objects.put(prop, Arrays.asList(object));
                    
                    putObject(s, new PropertyConfig(prop), tmpProperty2objects, rdfAnalyzer, tmpCell, table, setup);
                    
                    cells.add(tmpCell);
                    
                    //update also for cells where the object occurs again
                    /*
                    cell2node2properties.entrySet().forEach(e -> {
                        List<Property> plist = e.getValue().get(object);
                        if(plist != null) {
                            plist.add(prop);
                        }
                    });
                    */
                    
                    //create an extra entry of the cell
                    //Map<RDFNode, List<Property>> node2properties = new HashMap<>(); 
                    //cell2node2properties.put(tmpCell, node2properties);
                    
                    node2properties.computeIfAbsent(object, o -> new ArrayList<>()).add(prop);
                    cell2nodes.computeIfAbsent(tmpCell, o -> new ArrayList<>()).add(object);
                }
            }
        }
        
        //we remove cells that have duplicate objects if isDistinctObjects is true
        List<ExcelCell> tmpCells;
        if(pConfig.isDistinctObjects()) {
            tmpCells = new ArrayList<>();
            
            //use it to check that you only get distinct objects when merged
            Set<RDFNode> objectSet = new HashSet<>();
            
            for (int i = 0; i < cells.size(); i++) {
                List<RDFNode> nodes = cell2nodes.get(cells.get(i));
                
                boolean duplicateFound = true;
                for(RDFNode node : nodes) {
                    if(!objectSet.contains(node)) {
                        duplicateFound = false;
                        break;
                    }
                }
                
                objectSet.addAll(nodes);
                
                if(!duplicateFound) {
                    tmpCells.add(cells.get(i));
                }
            }
            
            //wrong: subject vs object problematic: when literal is used
            /*
            for (int i = 0; i < cells.size(); i++) {
                boolean objectDuplicate = false;
                for (Statement stmt : table.getCellProvMap().get(cells.get(i)).getStatements()) {
                    //we use the subject of the cells provenance
                    if(objectSet.contains(stmt.getSubject())) {
                        objectDuplicate = true;
                        break;
                    }

                    objectSet.add(stmt.getObject());
                }

                //skip objects that would be duplicates
                if(objectDuplicate) {
                    continue;
                }
                
                tmpCells.add(cells.get(i));
            }
            */
            
        } else {
            //all of them
            tmpCells = cells;
        }
        
        Collections.shuffle(tmpCells, setup.getOrThrow(Setup.RANDOM, Random.class));
        
        //a merged cell will always be a string cell
        //because in a string we can represent every information
        mergedCell.setCellType("string");
        StringBuilder mergedCellSB = new StringBuilder();
        
        //keep in mind: different types could be possible that have to be merged
        //for native numeric, boolean we need a string representation of it
        //use toString(cell, ...) for this
        //what merge strategy do we follow
        MergeCellStrategy mergeStrategy = selectPattern("MergeCellStrategy", MergeCellStrategy.class, pConfig, setup, mergedCell, table);
        
        boolean richTextIsUsed = false;
        
        switch (mergeStrategy) {
            case Delimiter:
                String delimiter = selectPattern("MergeCellDelimiters", String.class, pConfig, setup, mergedCell, table);
                for (int i = 0; i < tmpCells.size(); i++) {
                    
                    //this str can be rich text
                    String str = toString(tmpCells.get(i), pConfig, table, setup);
                    //then use the right merge type if rich text is used in at least one tmpCell
                    richTextIsUsed |= tmpCells.get(i).getCellType().equals("string") && tmpCells.get(i).getValueRichText() != null;
                    
                    
                    //pattern: Partial Formatting Indicates Relations ============
                    String[] openCloseTags = null;
                    
                    //make only sense when more then one cell will be merged
                    //TODO maybe more tests need to be done to really check if Partial Formatting Indicates Relations
                    //should be used here
                    //update: maybe we should do it always because if the property 
                    //is disjunct and single entries in a cell we would like to see what property it has
                    //thus, size() > 0
                    if(tmpCells.size() > 0) {
                    
                        //we try to find a propery that has a PartialFormattingIndicatesRelations key in setup
                        List<RDFNode> nodes = cell2nodes.get(tmpCells.get(i));
                        for(RDFNode node : nodes) {
                            //one node is maybe in different relations
                            List<Property> plist = node2properties.get(node);

                            for(Property p : plist) {

                                String key = p.getURI() + ".PartialFormattingIndicatesRelations";
                                if(setup.containsKey(key)) {
                                    String formatting = setup.getOrThrow(key, String.class);
                                    
                                    table.putUsedPattern(mergedCell, key, formatting);

                                    if(!formatting.contains("|")) {
                                        throw new RuntimeException("put a '|' in the formatting of " + key);
                                    }

                                    openCloseTags = formatting.split("\\|");
                                    break;
                                }
                            }

                            if(openCloseTags != null) {
                                break;
                            }
                        }
                    }
                    
                    //in case of rich text: open a tag here
                    if(openCloseTags != null) {
                        richTextIsUsed = true;
                        
                        mergedCellSB.append(openCloseTags[0]);
                    }
                    
                    //pattern end ==============================================
                    
                    //plain text
                    if(richTextIsUsed) {
                        str = str.replace("<", "&lt;").replace(">", "&gt;");
                    }
                    
                    mergedCellSB.append(str);
                    
                    //in case of rich text: close a tag here
                    if(openCloseTags != null) {
                        mergedCellSB.append(openCloseTags[1]);
                    }
                    
                    if (i != tmpCells.size() - 1) {
                        mergedCellSB.append(delimiter);
                    }
                }
                break;
        }

        if(richTextIsUsed) {
            //rich text
            mergedCell.setValueRichText(mergedCellSB.toString());
        } else {
            //plain text
            mergedCell.setValueString(mergedCellSB.toString());
        }
        
        //provenance
        for(Property prop : pConfig.getProperties()) {
            if(property2objects.containsKey(prop)) {
                table.addStatements(mergedCell, s, prop, property2objects.get(prop));
            }
        }
        //use the provenance of the single cells
        //use cells variable here to have them all (not touched by isDistinctObjects)
        for (ExcelCell cell : cells) {
            Provenance prov = table.getCellProvMap().get(cell);
            
            for (Statement stmt : prov.getStatements()) {
                table.addStatement(mergedCell, stmt.getSubject(), stmt.getPredicate(), stmt.getObject());
            }
            
            for(Entry<String, Object> usedPattern : prov.getUsedPatterns().entrySet()) {
                table.putUsedPattern(mergedCell, usedPattern.getKey(), usedPattern.getValue());
            }
        }

        return true;
    }

    private String toString(ExcelCell cell, PropertyConfig pConfig, ExcelTable table, Setup setup) {
        if (cell.getCellType().equals("string")) {
            if(cell.getValueRichText() != null) {
                return cell.getValueRichText();
            }
            return cell.getValueString();
        } else if (cell.getCellType().equals("numeric")) {
            return numberToString(cell.getValueNumeric(), pConfig, setup, cell, table);
        } else if (cell.getCellType().equals("boolean")) {
            return booleanToString(cell.getValueBoolean(), pConfig, setup, cell, table);
        } else {
            throw new RuntimeException("no toString implemented for " + cell.getCellType());
        }
    }
    
    private String numberToString(Number number, PropertyConfig pConfig, Setup setup, ExcelCell cell, ExcelTable table) {
        
        //if there is a data format attached to it, use this
        if(cell.getDataFormat() != null) {
            DataFormatter dataFormatter = new DataFormatter();
            String formatted = dataFormatter.formatRawCellContents(cell.getValueNumeric(), 0, cell.getDataFormat());
            return formatted;
        }
        
        NumberStringFormats nrFormat = selectPattern("NumberStringFormats", NumberStringFormats.class, pConfig, setup, cell, table);
        
        String str = null;
        switch (nrFormat) {
            case StringValueOf:
                boolean isInt = number instanceof Integer || (number.doubleValue() % 1) == 0;

                if(isInt) {
                    str = String.valueOf(number);
                } else {
                    str = String.format(
                            (Locale) setup.getOrDefault("locale", Locale.ENGLISH), 
                            "%s",
                            number.doubleValue()
                    );
                }
                break;

            case RomanNumeral:
                str = RomanNumber.toRoman(number.intValue());
                break;
        }
        
        return str;
    }
    
    private String booleanToString(boolean value, PropertyConfig pConfig, Setup setup, ExcelCell cell, ExcelTable table) {
        //if there is a data format attached to it, use this
        if(cell.getDataFormat() != null) {
            DataFormatter dataFormatter = new DataFormatter();
            String formatted = dataFormatter.formatRawCellContents(cell.getValueNumeric(), 0, cell.getDataFormat());
            return formatted;
        }
        
        //select a symbol
        String symbol;
        if (value) {
            symbol = (String) selectPattern("BooleanTrueSymbols", String.class, pConfig, setup, cell, table);
        } else {
            symbol = (String) selectPattern("BooleanFalseSymbols", String.class, pConfig, setup, cell, table);
        }
        return symbol;
    }

    private boolean putNoObject(Resource s, PropertyConfig pConfig, ExcelCell cell, ExcelTable table, Setup setup) {
        
        //an empty cell could be expressed in different ways
        EmptyCellRendering emptyCellRendering = selectPattern("EmptyCellRendering", EmptyCellRendering.class, pConfig, setup, cell, table);

        //we do not need to save a provenance because there is no statement that is meant here
        //it is just empty and does not have any valuable knowledge for us
        switch (emptyCellRendering) {
            case Native:
                //just return falls this means the Excel Cell is null and is really empty
                return false;

            case EmptyString:
                //an empty string
                cell.setCellType("string");
                cell.setValueString("");
                return true;

            case BlankString:
                //a blank string that indicates emptiness
                List<String> wss = selectPattern("BlankStringWhitespaces", List.class, pConfig, setup, cell, table);
                int len = selectPattern("BlankStringLengths", Integer.class, pConfig, setup, cell, table);
                Random rnd = setup.getSingleOrThrow("random", Random.class);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < len; i++) {
                    sb.append(wss.get(rnd.nextInt(wss.size())));
                }

                if (sb.toString().isEmpty()) {
                    throw new RuntimeException("should be blank but was empty");
                }

                //a blank string
                cell.setCellType("string");
                cell.setValueString(sb.toString());
                return true;

            case Symbol:
                //a symbol that indicates emptiness
                String symbol = selectPattern("EmptyCellSymbols", String.class, pConfig, setup, cell, table);
                cell.setCellType("string");
                cell.setValueString(symbol);
                return true;

            case Numeric:
                //a number that indicates emptiness
                double number = selectPattern("EmptyCellNumbers", Double.class, pConfig, setup, cell, table);
                cell.setCellType("numeric");
                cell.setValueNumeric(number);
                return true;

            case BooleanTrue:
            case BooleanFalse:
                //a boolean that indicates emptiness
                cell.setCellType("boolean");
                cell.setValueBoolean(emptyCellRendering == EmptyCellRendering.BooleanTrue);
                return true;
        }

        return false;
    }

    //select a pattern from a possible list
    //if it is a list, try to distibute the values uniformly
    //use Property p to check if there is a special <p>.<pattern> setup for it
    private <T> T selectPattern(String patternName, Class<T> type, PropertyConfig pConfig, Setup setup, ExcelCell cell, ExcelTable table) {
        
        //prefer that there could be a special pattern to propery association
        String patternKey = patternName;
        if(pConfig != null) {
            //try to use the property uri to find a specific pattern configuration
            if(!pConfig.isMulti()) {
                if(setup.containsKey(pConfig.getProperty().getURI() + "." + patternName)) {
                    patternKey = pConfig.getProperty().getURI() + "." + patternName;
                }
            }
            //use this to create a temp uri that can be used for example if you have multiple properties in the
            //property config. this why you can refer to it with one uri.
            if(pConfig.hasUri()) {
                if(setup.containsKey(pConfig.getUri() + "." + patternName)) {
                    patternKey = pConfig.getUri() + "." + patternName;
                }
            }
        }

        T obj = (T) setup.getByDistribution(patternKey);
        
        if(obj == null) {
            throw new RuntimeException("no configuration for pattern key " + patternKey);
        }

        table.putUsedPattern(cell, patternName, obj);
        return obj;
            
            
        //deprecated: we use discrete probability distribution now 2021-02-18
        /*
        //to keep track how often a value was used
        Map<String, Map<Object, Integer>> pattern2value2count
                = (Map<String, Map<Object, Integer>>) setup.computeIfAbsent("pattern2value2count", a -> new HashMap<>());
        Map<Object, Integer> value2count = pattern2value2count.computeIfAbsent(patternName, pn -> new HashMap<>());

        //find minimum and maximum count
        int min = Integer.MAX_VALUE;
        Object minValue = null;
        int max = 0;
        for (Object value : list) {
            int count = value2count.getOrDefault(value, 0);
            max = Math.max(max, count);
            if (count < min) {
                min = count;
                minValue = value;
            }
        }

        T selectedValue;

        //if the minimum and maximum count differs by a certain threshold
        int diffThreshold = (int) setup.getOrDefault("selectPatternDiffThreshold", 3);
        if ((max - min) >= diffThreshold) {
            //we have to select the minimum so that it stays uniformly distributed
            selectedValue = (T) minValue;
        } else {
            Random rnd = (Random) setup.computeIfAbsent("selectPatternRandom", r -> new Random(8353));
            selectedValue = (T) list.get(rnd.nextInt(list.size()));
        }

        //update counts
        value2count.put(selectedValue, value2count.getOrDefault(selectedValue, 0) + 1);

        //provenance
        table.putUsedPattern(cell, patternName, selectedValue);
        */
    }

    public Map<String, Function<String, Object>> getDataTypeMap() {
        return dataTypeMap;
    }
    
}
