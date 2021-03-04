
package de.dfki.sds.datasprout.vocab;

import de.dfki.sds.datasprout.utils.Names;
import de.dfki.sds.datasprout.utils.SemanticUtility;
import de.dfki.sds.datasprout.utils.StringUtility;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * Guideline Ontology.
 */
public class GL {

    public static final String NS = "http://www.dfki.uni-kl.de/~mschroeder/ld/gl#";
    
    public static final Resource Guideline = ResourceFactory.createResource(NS + "Guideline");
    public static final Resource Attachment = ResourceFactory.createResource(NS + "Attachment");
    public static final Resource Procedure = ResourceFactory.createResource(NS + "Procedure");
    
    public static final Resource Department = ResourceFactory.createResource(NS + "Department");
    public static final Resource Kind = ResourceFactory.createResource(NS + "Kind");
    public static final Resource State = ResourceFactory.createResource(NS + "State");
    public static final Resource Category = ResourceFactory.createResource(NS + "Category");
    public static final Resource SecurityNeed = ResourceFactory.createResource(NS + "SecurityNeed");
    public static final Resource Progress = ResourceFactory.createResource(NS + "Progress");
    public static final Resource MailingList = ResourceFactory.createResource(NS + "MailingList");
    
    public static final Property manages = ResourceFactory.createProperty(NS + "manages");
    public static final Property hasProgress = ResourceFactory.createProperty(NS + "hasProgress");
    public static final Property hasId = ResourceFactory.createProperty(NS + "hasId");
    public static final Property hasNumber = ResourceFactory.createProperty(NS + "hasNumber");
    public static final Property validFrom = ResourceFactory.createProperty(NS + "validFrom");
    public static final Property invalidFrom = ResourceFactory.createProperty(NS + "invalidFrom");
    public static final Property lastModifiedDate = ResourceFactory.createProperty(NS + "lastModifiedDate");
    public static final Property isRecent = ResourceFactory.createProperty(NS + "isRecent");
    public static final Property hasCategory = ResourceFactory.createProperty(NS + "hasCategory");
    public static final Property hasNote = ResourceFactory.createProperty(NS + "hasNote");
    public static final Property hasAttachment = ResourceFactory.createProperty(NS + "hasAttachment");
    public static final Property hasAbbreviation = ResourceFactory.createProperty(NS + "hasAbbreviation");
    public static final Property hasState = ResourceFactory.createProperty(NS + "hasState");
    public static final Property hasSecurityNeed = ResourceFactory.createProperty(NS + "hasSecurityNeed");
    public static final Property hasDepartment = ResourceFactory.createProperty(NS + "hasDepartment");
    public static final Property worksAt = ResourceFactory.createProperty(NS + "worksAt");
    public static final Property inMailingList = ResourceFactory.createProperty(NS + "inMailingList");
    public static final Property hasTitle = ResourceFactory.createProperty(NS + "hasTitle");
    public static final Property hasKind = ResourceFactory.createProperty(NS + "hasKind");
    public static final Property hasEditorResponsible = ResourceFactory.createProperty(NS + "hasEditorResponsible");
    public static final Property hasEditor = ResourceFactory.createProperty(NS + "hasEditor");
    public static final Property hasReviewer = ResourceFactory.createProperty(NS + "hasReviewer");
    public static final Property plannedValidFrom = ResourceFactory.createProperty(NS + "plannedValidFrom");
    public static final Property wasFormerEditor = ResourceFactory.createProperty(NS + "wasFormerEditor");
    
    
    //==========================================================================
    //generate instance data
    
    public static Model generate(double scalingFactor) {
        Random rnd = new Random(1234);
        Set distinctSet = new HashSet<>();
        
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(PrefixMapping.Standard);
        model.setNsPrefix("gl", NS);
        model.setNsPrefix("foaf", FOAF.NS);
        
        res(model, "Research and Development", true, GL.Department);
        res(model, "Human Resource Management", true, GL.Department);
        
        res(model, "Medical Guideline", true, GL.Kind);
        res(model, "Publicly Available Specification", true, GL.Kind);
        res(model, "Programming Style Guidelines", true, GL.Kind);
        res(model, "Toponymic Guidelines", true, GL.Kind);
        res(model, "Code of Practice", true, GL.Kind);
        
        res(model, "Uncertain", false, GL.State);
        res(model, "Deprecated", false, GL.State);
        res(model, "Valid", false, GL.State);
        res(model, "Prospective", false, GL.State);
        
        res(model, "Untouched", false, GL.Progress);
        res(model, "Edited", false, GL.Progress);
        res(model, "Canceled", false, GL.Progress);
        res(model, "Finished", false, GL.Progress);
        res(model, "Published", false, GL.Progress);
        
        res(model, "Classified", false, GL.SecurityNeed);
        res(model, "Confidential", false, GL.SecurityNeed);
        res(model, "None", false, GL.SecurityNeed);
        
        distinctSet.clear();
        for(int i = 0; i < Math.floor(50 * scalingFactor); i++) {
            String name = RandomStringUtils.randomAlphabetic(3).toUpperCase();
            
            if(distinctSet.contains(name)) {
                i--;
                continue;
            }
            
            res(model, name, false, GL.Category);
            distinctSet.add(name);
        }
        
        for(int i = 0; i < Math.floor(15 * scalingFactor); i++) {
            res(model, randomPhrase(3, rnd), true, GL.MailingList);
        }
        
        List<String> workAts = new ArrayList<>();
        for(int i = 0; i < Math.floor(75 * scalingFactor); i++) {
            workAts.add(randomWorkAt(rnd));
        }
        
        for(int i = 0; i < Math.floor(150 * scalingFactor); i++) {
            Resource person = res(model, null, false, FOAF.Person);
            String fn = randomlyPick(rnd.nextBoolean() ? Names.femaleFirstNames : Names.maleFirstNames, rnd);
            String ln = randomlyPick(Names.lastNames, rnd);
            model.add(person, FOAF.firstName, fn);
            model.add(person, FOAF.lastName, ln);
            model.add(person, RDFS.label, fn + " " + ln);
            model.add(person, GL.worksAt, randomlyPick(workAts, rnd));
        }
        
        List<Resource> securityNeeds = model.listSubjectsWithProperty(RDF.type, GL.SecurityNeed).toList();
        EnumeratedDistribution<Boolean> securityNeedsDist = new EnumeratedDistribution(Arrays.asList(Pair.create(true, 0.4), Pair.create(false, 0.6)));
        List<Resource> departments = model.listSubjectsWithProperty(RDF.type, GL.Department).toList();
        List<Resource> states = model.listSubjectsWithProperty(RDF.type, GL.State).toList();
        List<Resource> categories = model.listSubjectsWithProperty(RDF.type, GL.Category).toList();
        List<Resource> kinds = model.listSubjectsWithProperty(RDF.type, GL.Kind).toList();
        List<Resource> persons = model.listSubjectsWithProperty(RDF.type, FOAF.Person).toList();
        List<Resource> mailingLists = model.listSubjectsWithProperty(RDF.type, GL.MailingList).toList();
        EnumeratedDistribution<Boolean> noteDist = new EnumeratedDistribution(Arrays.asList(Pair.create(true, 0.1), Pair.create(false, 0.9)));
        EnumeratedDistribution<Boolean> invalidDist = new EnumeratedDistribution(Arrays.asList(Pair.create(true, 0.33), Pair.create(false, 0.66)));
        EnumeratedDistribution<Boolean> attInvalidDist = new EnumeratedDistribution(Arrays.asList(Pair.create(true, 0.1), Pair.create(false, 0.9)));
        EnumeratedDistribution<Boolean> lmdDist = new EnumeratedDistribution(Arrays.asList(Pair.create(true, 0.15), Pair.create(false, 0.85)));
        EnumeratedDistribution<Boolean> recentDist = new EnumeratedDistribution(Arrays.asList(Pair.create(true, 0.1), Pair.create(false, 0.9)));
        EnumeratedDistribution<Boolean> attachmentDist = new EnumeratedDistribution(Arrays.asList(Pair.create(true, 0.5), Pair.create(false, 0.5)));
        EnumeratedDistribution<Boolean> reviewerDist = new EnumeratedDistribution(Arrays.asList(Pair.create(true, 0.125), Pair.create(false, 0.875)));
        EnumeratedDistribution<Boolean> attPersonDist = new EnumeratedDistribution(Arrays.asList(Pair.create(true, 0.125), Pair.create(false, 0.875)));
        EnumeratedDistribution<Boolean> wasFormerEditorDist = new EnumeratedDistribution(Arrays.asList(Pair.create(true, 0.2), Pair.create(false, 0.8)));
        EnumeratedDistribution<Boolean> plannedValidDateDist = new EnumeratedDistribution(Arrays.asList(Pair.create(true, 0.2), Pair.create(false, 0.8)));
        for(int i = 0; i < Math.floor(850 * scalingFactor); i++) {
            Resource guideline = res(model, null, false, GL.Guideline);
            
            model.add(guideline, GL.hasTitle, randomPhrase(9, rnd));
            
            if(securityNeedsDist.sample()) {
                model.add(guideline, GL.hasSecurityNeed, randomlyPick(securityNeeds, rnd));
            }
            model.add(guideline, GL.hasDepartment, randomlyPick(departments, rnd));
            model.add(guideline, GL.hasState, randomlyPick(states, rnd));
            model.add(guideline, GL.hasCategory, randomlyPick(categories, rnd));
            model.add(guideline, GL.hasKind, randomlyPick(kinds, rnd));
            
            if(noteDist.sample()) {
                model.add(guideline, GL.hasNote, randomPhrase(15, rnd));
            }
            
            LocalDate valid = randomDate(1, 1, 1998, 1, 1, 2020, rnd);
            model.addLiteral(guideline, GL.validFrom, SemanticUtility.toXsdDate(valid));
         
            if(invalidDist.sample()) {
                LocalDate invalid = randomDate(valid.getDayOfMonth(), valid.getMonthValue(), valid.getYear(), 1, 1, 2021, rnd);
                model.addLiteral(guideline, GL.invalidFrom, SemanticUtility.toXsdDate(invalid));
            } else {
                if(plannedValidDateDist.sample()) {
                    LocalDate planned = randomDate(valid.getDayOfMonth(), valid.getMonthValue(), valid.getYear(), 1, 1, 2021, rnd);
                    model.addLiteral(guideline, GL.plannedValidFrom, SemanticUtility.toXsdDate(planned));
                }
            }
            
            if(lmdDist.sample()) {
                LocalDate lmd = randomDate(1, 1, 1998, 1, 1, 2020, rnd);
                model.addLiteral(guideline, GL.lastModifiedDate, SemanticUtility.toXsdDate(lmd));
            }
            
            model.addLiteral(guideline, GL.isRecent, (boolean) recentDist.sample());
            
            String cat = SemanticUtility.literal(model, SemanticUtility.resource(model, guideline, GL.hasCategory), RDFS.label).getString();
            String kind = SemanticUtility.literal(model, SemanticUtility.resource(model, guideline, GL.hasKind), GL.hasAbbreviation).getString();
            
            String id = kind + " " + 
                    RandomStringUtils.randomNumeric(1) + "-" + cat + 
                    RandomStringUtils.randomNumeric(2) + "." + 
                    RandomStringUtils.randomNumeric(4) + "/" + 
                    RandomStringUtils.randomNumeric(2);
            
            model.add(guideline, GL.hasId, id);
            model.add(guideline, RDFS.label, id);
            
            int personCount = 1 + rnd.nextInt(3);
            for(int j = 0; j < personCount; j++) {
                Resource person = randomlyPick(persons, rnd);
                if(j == 0) {
                    model.add(guideline, GL.hasEditorResponsible, person);
                }
                model.add(guideline, GL.hasEditor, person);
            }
            if(reviewerDist.sample()) {
                model.add(guideline, GL.hasReviewer, randomlyPick(persons, rnd));
            }
            if(wasFormerEditorDist.sample()) {
                model.add(guideline, GL.wasFormerEditor, randomlyPick(persons, rnd));
            }

            int mailingListCount = rnd.nextInt(5);
            for(int j = 0; j < mailingListCount; j++) {
                model.add(guideline, GL.inMailingList, randomlyPick(mailingLists, rnd));
            }
            
            if(attachmentDist.sample()) {
                int attachmentCount = 5 + (int) (rnd.nextGaussian()*5.0);
                if(attachmentCount < 1) {
                    attachmentCount = 1;
                }
                
                for(int j = 0; j < attachmentCount; j++) {
                    Resource attachment = res(model, null, false, GL.Attachment);
                    model.add(guideline, GL.hasAttachment, attachment);
                    
                    int number = (j+1);
                    model.addLiteral(attachment, GL.hasNumber, number);
                    
                    model.add(attachment, RDFS.label, id + " A" + number);
                    
                    model.add(attachment, GL.hasTitle, randomPhrase(9, rnd));
                    
                    //copy from guideline
                    model.add(attachment, GL.hasCategory, SemanticUtility.resource(model, guideline, GL.hasCategory));
                    model.add(attachment, GL.hasDepartment, SemanticUtility.resource(model, guideline, GL.hasDepartment));
                 
                    LocalDate attValid = randomDate(1, 1, 1998, 1, 1, 2020, rnd);
                    model.addLiteral(attachment, GL.validFrom, SemanticUtility.toXsdDate(attValid));
                    
                    model.add(attachment, GL.hasState, randomlyPick(states, rnd));
                    
                    if(attInvalidDist.sample()) {
                        LocalDate attInvalid = randomDate(attValid.getDayOfMonth(), attValid.getMonthValue(), attValid.getYear(), 1, 1, 2021, rnd);
                        model.addLiteral(attachment, GL.invalidFrom, SemanticUtility.toXsdDate(attInvalid));
                    } else {
                        if(plannedValidDateDist.sample()) {
                            LocalDate attPlanned = randomDate(attValid.getDayOfMonth(), attValid.getMonthValue(), attValid.getYear(), 1, 1, 2021, rnd);
                            model.addLiteral(attachment, GL.plannedValidFrom, SemanticUtility.toXsdDate(attPlanned));
                        }
                    }
                    
                    if(lmdDist.sample()) {
                        LocalDate attLmd = randomDate(1, 1, 1998, 1, 1, 2020, rnd);
                        model.addLiteral(attachment, GL.lastModifiedDate, SemanticUtility.toXsdDate(attLmd));
                    }
                    
                    model.addLiteral(attachment, GL.isRecent, (boolean) recentDist.sample());
                    
                    if(noteDist.sample()) {
                        model.add(attachment, GL.hasNote, randomPhrase(15, rnd));
                    }
                    
                    if(attPersonDist.sample()) {
                        int attPersonCount = 1 + rnd.nextInt(2);
                        for(int k = 0; k < attPersonCount; k++) {
                            Resource person = randomlyPick(persons, rnd);
                            if(k == 0) {
                                model.add(attachment, GL.hasEditorResponsible, person);
                            }
                            model.add(attachment, GL.hasEditor, person);
                        }
                        if(wasFormerEditorDist.sample()) {
                            model.add(attachment, GL.wasFormerEditor, randomlyPick(persons, rnd));
                        }
                    }
                }
            }
        } //guidelines
        
        List<Resource> guidelines = new ArrayList<>(model.listSubjectsWithProperty(RDF.type, GL.Guideline).toList());
        List<Resource> progresses = model.listSubjectsWithProperty(RDF.type, GL.Progress).toList();
        for(int i = 0; i < guidelines.size(); i++) {
            Resource procedure = res(model, null, false, GL.Procedure);
            
            Resource guideline = randomlyRemove(guidelines, rnd);
            
            model.add(procedure, GL.manages, guideline);
            model.add(procedure, GL.hasProgress, randomlyPick(progresses, rnd));
        }
        
        return model;
    }
    
    private static Resource res(Model model) {
        return model.createResource("uuid:" + UUID.randomUUID().toString());
    }
    
    private static Resource res(Model model, String label, boolean acronym, Resource type) {
        Resource res = res(model);
        model.add(res, RDF.type, type);
        if(label != null) {
            model.add(res, RDFS.label, label);
        }
        if(acronym) {
            model.add(res, GL.hasAbbreviation, StringUtility.toAcronym(label));
        }
        return res;
    }
    
    private static String randomPhrase(int wordBound, Random rnd) {
        StringJoiner sj = new StringJoiner(" ");
        int words = 2 + rnd.nextInt(wordBound);
        for(int i = 0; i < words; i++) {
            sj.add(
                    StringUtility.toProperCase(
                            RandomStringUtils.randomAlphabetic(3 + rnd.nextInt(4))
                    )
            );
        }
        return sj.toString();
    }
    
    private static String randomWorkAt(Random rnd) {
        StringJoiner sj = new StringJoiner("-");
        int words = 1 + rnd.nextInt(3);
        for(int i = 0; i < words; i++) {
            sj.add(RandomStringUtils.randomAlphabetic(1 + rnd.nextInt(3)).toUpperCase());
        }
        return sj.toString();
    }
    
    private static LocalDate randomDate(int sDay, int sMonth, int sYear, int eDay, int eMonth, int eYear, Random rnd) {
        long minDay = LocalDate.of(sYear, sMonth, sDay).toEpochDay();
        long maxDay = LocalDate.of(eYear, eMonth, eDay).toEpochDay();
        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
        return LocalDate.ofEpochDay(randomDay);
    }
    
    private static <T> T randomlyPick(List<T> list, Random rnd) {
        return list.get(rnd.nextInt(list.size()));
    }
    
    private static <T> T randomlyRemove(List<T> list, Random rnd) {
        return list.remove(rnd.nextInt(list.size()));
    }
    
    public static void main(String[] args) throws IOException {
        Model model = GL.generate(1);
        
        String ttl = SemanticUtility.toTTL(model);
        //System.out.println(ttl);
        
        FileUtils.writeStringToFile(new File("dataset/GL.ttl"), ttl, StandardCharsets.UTF_8);
        //FileUtils.writeStringToFile(new File("src/main/resources/de/dfki/sds/datasprout/web/kg/GL.ttl"), ttl, StandardCharsets.UTF_8);
        
        System.out.println(model.size() + " statements stored");
    }
    
}
