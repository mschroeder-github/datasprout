
package de.dfki.sds.datasprout.excel;

import de.dfki.sds.datasprout.utils.JsonUtility;
import org.apache.jena.shared.PrefixMapping;
import org.json.JSONObject;

/**
 * Generator options.
 */
public class ExcelSproutOptions {

    private long randomSeed;
    
    private int numberOfWorkbooks;

    private boolean writeExpectedModel;
    private boolean writeProvenanceModel;
    private boolean writeProvenanceCSV;
    
    private boolean writeGenerationSummaryJson;
    
    private boolean provenanceAsCellComment;
    
    private PatternsToSetups patternsToSetups;
    
    private PrefixMapping prefixMapping;
    
    private JSONObject generationSummary;
    
    public ExcelSproutOptions() {
        generationSummary = new JSONObject();
        JsonUtility.forceLinkedHashMap(generationSummary);
    }
    
    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public int getNumberOfWorkbooks() {
        return numberOfWorkbooks;
    }

    public void setNumberOfWorkbooks(int numberOfWorkbooks) {
        this.numberOfWorkbooks = numberOfWorkbooks;
    }

    public boolean isWriteExpectedModel() {
        return writeExpectedModel;
    }

    public void setWriteExpectedModel(boolean writeExpectedModel) {
        this.writeExpectedModel = writeExpectedModel;
    }

    public boolean isWriteProvenanceModel() {
        return writeProvenanceModel;
    }

    public void setWriteProvenanceModel(boolean writeProvenanceModel) {
        this.writeProvenanceModel = writeProvenanceModel;
    }

    public boolean isWriteProvenanceCSV() {
        return writeProvenanceCSV;
    }

    public void setWriteProvenanceCSV(boolean writeProvenanceCSV) {
        this.writeProvenanceCSV = writeProvenanceCSV;
    }

    public boolean isProvenanceAsCellComment() {
        return provenanceAsCellComment;
    }

    public void setProvenanceAsCellComment(boolean provenanceAsCellComment) {
        this.provenanceAsCellComment = provenanceAsCellComment;
    }

    public PatternsToSetups getPatternsToSetups() {
        return patternsToSetups;
    }

    public void setPatternsToSetups(PatternsToSetups patternsToSetups) {
        this.patternsToSetups = patternsToSetups;
    }

    public PrefixMapping getPrefixMapping() {
        return prefixMapping;
    }

    public void setPrefixMapping(PrefixMapping prefixMapping) {
        this.prefixMapping = prefixMapping;
    }

    public boolean isWriteGenerationSummaryJson() {
        return writeGenerationSummaryJson;
    }

    public void setWriteGenerationSummaryJson(boolean writeGenerationSummaryJson) {
        this.writeGenerationSummaryJson = writeGenerationSummaryJson;
    }

    public JSONObject getGenerationSummary() {
        return generationSummary;
    }
    
}
