
package de.dfki.sds.datasprout.excelgen;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

/**
 * 
 */
public class FontStyle {

    private int startIndex;
    private int endIndex;
    
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikeout;
    
    private Color color;

    //use this to define it for the whole cell
    public FontStyle() {
        startIndex = -1;
        endIndex = -1;
    }
    
    //use this to define it for in text
    public FontStyle(int startIndex, int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public boolean isForCell() {
        return startIndex == -1;
    }
    
    public boolean isBold() {
        return bold;
    }

    public FontStyle setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public boolean isItalic() {
        return italic;
    }

    public FontStyle setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }

    public boolean isUnderline() {
        return underline;
    }

    public FontStyle setUnderline(boolean underline) {
        this.underline = underline;
        return this;
    }

    public boolean isStrikeout() {
        return strikeout;
    }

    public FontStyle setStrikeout(boolean strikeout) {
        this.strikeout = strikeout;
        return this;
    }

    public Color getColor() {
        return color;
    }

    public FontStyle setColor(Color color) {
        this.color = color;
        return this;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }
    
    public void applyTo(XSSFFont font) {
        font.setBold(bold);
        font.setItalic(italic);
        font.setUnderline(underline ? FontUnderline.SINGLE : FontUnderline.NONE);
        font.setStrikeout(strikeout);
        if(color != null) {
            font.setColor(new XSSFColor(color, new DefaultIndexedColorMap()));
        }
    }
    
    //this is used to pass a parse result
    public static class FontStyleParseResult {
        private String text;
        private List<FontStyle> styles;

        public FontStyleParseResult() {
            styles = new ArrayList<>();
        }

        public String getText() {
            return text;
        }

        public List<FontStyle> getStyles() {
            return styles;
        }
    }
    
    //short tests
    /*
    public static void main(String[] args) {
        FontStyleParseResult result = parse("<b>1234<font color='#0000ff'>&lt;678</font></b>");
        System.out.println(result.text);
        result.styles.forEach(fs -> System.out.println(fs.startIndex + " " + fs.endIndex));
    }
    */
    
    /**
     * Use this to parse the special richText (html-like) syntax to a FontStyle list together with the text.
     * @param richText
     * @return 
     */
    public static FontStyleParseResult parse(String richText) {
        FontStyleParseResult result = new FontStyleParseResult();
        
        StringBuilder sb = new StringBuilder();
        
        boolean tag = false;
        
        //<b>test <font color='#0000ff'>bla</font></b>
        
        String tagName = "";
        
        Map<String, Stack<FontStyle>> tag2fs = new HashMap<>();
        
        for(int i = 0; i < richText.length(); i++) {
            
            char c = richText.charAt(i);
            
            if(!tag && c == '<') {
                tag = true;
            } else if(tag && c == '>') {
                tag = false;
                
                if(tagName.startsWith("/")) {
                    //closing
                    //try {
                    tagName = tagName.substring(1).split(" ")[0];
                    tag2fs.get(tagName).pop().endIndex = sb.length();
                    ///} catch(Exception e) {
                    //   int a = 0;
                    //}
                    
                } else {
                    //opening
                    FontStyle fs = new FontStyle();
                    fs.startIndex = sb.length();
                    String tg = tagName.split(" ")[0];
                    
                    if(tg.equals("b")) {
                        fs.bold = true;
                    } else if(tg.equals("u")) {
                        fs.underline = true;
                    } else if(tg.equals("i")) {
                        fs.italic = true;
                    } else if(tg.equals("strike")) {
                        fs.strikeout = true;
                    } else if(tg.equals("font")) {
                       
                        Document doc = Jsoup.parse("<" + tagName + "></font>", "", Parser.xmlParser());
                        Element elem = doc.child(0);
                        
                        String colorStr = elem.attr("color");
                        if(colorStr != null) {
                            fs.color = Color.decode(colorStr);
                        }
                        
                        String face = elem.attr("face");
                        if(face != null) {
                            //TODO
                        }
                    }
                    
                    tag2fs.computeIfAbsent(tg, t -> new Stack<>()).add(fs);
                    
                    result.styles.add(fs);
                }
                
                tagName = "";
                
            } else if(!tag) {
                
                if(i + 4 <= richText.length()) {
                    String sub = richText.substring(i, i + 4);
                    if(sub.equals("&lt;")) {
                        c = '<';
                        i += 3;
                    } else if(sub.equals("&gt;")) {
                        c = '>';
                        i += 3;
                    }
                }
                
                sb.append(c);
            }
            
            if(tag && c != '<') {
                tagName += c;
            }
        }
        
        for(FontStyle fs : result.styles) {
            if(fs.startIndex > fs.endIndex) {
                throw new RuntimeException(fs + " index wrong for rich text: " + richText);
            }
        }
        
        //merge
        Map<String, List<FontStyle>> loc2styles = new HashMap<>();
        for(FontStyle fs : result.styles) {
            loc2styles.computeIfAbsent(fs.startIndex + "-" + fs.endIndex, s -> new ArrayList<>()).add(fs);
        }
        
        result.styles.clear();
        for(Entry<String, List<FontStyle>> e : loc2styles.entrySet()) {
            FontStyle first = e.getValue().get(0);
            if(e.getValue().size() > 1) {
                //multi => merge all the other with first one
                for(int i = 1; i < e.getValue().size(); i++) {
                    first.bold |= e.getValue().get(i).bold;
                    first.italic |= e.getValue().get(i).italic;
                    first.underline |= e.getValue().get(i).underline;
                    first.strikeout |= e.getValue().get(i).strikeout;
                    if(e.getValue().get(i).color != null) {
                        first.color = e.getValue().get(i).color;
                    }
                }
            }
            result.styles.add(first);
        }
        
        result.text = sb.toString();
        
        
        /*
        Document doc = Jsoup.parse(richText, "", Parser.xmlParser());
        
        StringBuilder sb = new StringBuilder();
        
        for(Element e : doc.getAllElements()) {
            if(e == doc)
                continue;
            
            FontStyle fontStyle = new FontStyle();
            
            fontStyle.startIndex = sb.length();
            if(!e.ownText().isEmpty()) {
                sb.append(e.ownText());
            }
            fontStyle.endIndex = sb.length();
            
            
            
            System.out.println(e);
        }
        
        result.text = sb.toString();
        */
        
        return result;
    }
    
}
