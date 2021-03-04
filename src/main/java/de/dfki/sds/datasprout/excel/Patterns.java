
package de.dfki.sds.datasprout.excel;

/**
 * 
 */
public class Patterns {
    
    //how a boolean is shown in the cell
    public enum BooleanRendering {
        
        //as a native boolean in excel 
        //look at BooleanNativeDataFormats
        Native,
        
        //as a symbol: 
        //look at BooleanFalseSymbols
        //look at BooleanTrueSymbols
        Symbol,
        
        //as a number
        //look at BooleanFalseNumbers
        //look at BooleanTrueNumbers
        Numeric
    }
    
    //how number is rendered
    public enum NumericRendering {
        
        //as a native number
        //look at NumericNativeDataFormats
        Native,
        
        //as a string
        //look at NumberStringFormats
        String
    }
    
    //used when NumericRendering = String
    public enum NumberStringFormats {
        
        //use String.valueOf
        //we use in fact: String.format(locale, "%f", doubleValue)
        StringValueOf,
        
        //TODO DecimalFormat with NumberStringDecimalFormats
        
        //roman numeral, e.g. 5 = V
        RomanNumeral
    }
    
    //datetime
    public enum DateTimeRendering {
        
        //as a native numeric in excel
        //look at DateTimeDataFormats
        Numeric,
        
        //in a string format
        //look at DateTimeStringFormats (use DateTimeFormatter.ofPattern("")) 
        //localDate.format(formatter);
        String
    }
    
    //date
    public enum DateRendering {
        
        //as a native numeric in excel
        //look at DateDataFormats
        Numeric,
        
        //in a string format
        //look at DateStringFormats (use DateTimeFormatter.ofPattern("")) 
        //localDate.format(formatter);
        String
    }
    
    //how an empty cell is rendered
    public enum EmptyCellRendering {
        
        //just do not fill the cell at all
        Native,
        
        //a string that is empty, literally ""
        EmptyString,
        
        //a string that has whitespace and is not empty
        //look at BlankStringWhitespaces for allowed whitespace strings (List)
        //look at BlankStringLengths for allowed lengthes
        BlankString,
        
        //a symbol to indicate that it is empty
        //look at EmptyCellSymbols
        Symbol,
        
        //a number to indicate that it is empty (no value)
        //look at EmptyCellNumbers
        Numeric,
        
        //a boolean that indicates it is empty
        BooleanTrue,
        BooleanFalse,
        
        //TODO using a style like diagonal line
        //Style
    }
    
    //when refering to an object
    //look at LabelProperties to define a list of possible
    //properties that lead to labels
    public enum LabelPickStrategy {
        
        Longest,
        
        Shortest,
        
        Alphabetical
        
        //TODO LanguageBased, look at LabelPickLanguages to get a language
    }
    
    //when multiple objects are put in one cell
    public enum MergeCellStrategy {
        
        //use a delimiter
        //look at MergeCellDelimiters (one string will be picked)
        Delimiter
        
    }
    
    
}
