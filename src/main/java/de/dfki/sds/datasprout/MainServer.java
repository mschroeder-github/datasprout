
package de.dfki.sds.datasprout;

import java.io.IOException;

public class MainServer {
    
    public static void main(String[] args) throws Exception {
        dataSproutServer(args);
    }
    
    private static void dataSproutServer(String[] args) throws IOException {
        DataSproutServer server = new DataSproutServer(args);
        server.start();
    }
}
