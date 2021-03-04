
package de.dfki.sds.datasprout.excel;

/**
 * 
 */
public class IdCounter {

    private int id;
    
    public int getAndIncId() {
        int i = id;
        id++;
        return i;
    }
    
}
