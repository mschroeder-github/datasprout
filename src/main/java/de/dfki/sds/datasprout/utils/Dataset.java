package de.dfki.sds.datasprout.utils;

import java.io.File;

/**
 *
 */
public class Dataset {

    private String name;
    private File file;
    private String cite;
    private String tdb2GraphUri;

    public Dataset(String name, File file) {
        this.name = name;
        this.file = file;
    }

    public Dataset(String name, File file, String cite) {
        this.name = name;
        this.file = file;
        this.cite = cite;
    }

    public static String footnoteurl(String url) {
        return "\\footnote{\\url{" + url + "}}";
    }

    public static String footnote(String text) {
        return "\\footnote{" + text + "}";
    }

    public static String cite(String id) {
        return "\\cite{" + id + "}";
    }

    public String getName() {
        return name;
    }

    public String getCite() {
        return cite;
    }

    public String getLatexLine() {
        String line = getName();
        if (cite != null) {
            line += " " + cite;
        }
        return line;
    }

    public File getFile() {
        return file;
    }

}
