package com.ivicastojanoski.glorykickboxingfans.archive;

import org.junit.Test;
import static org.junit.Assert.*;

/** The production syntax gate runs on the JVM; actual Android adapter/store coverage is separate. */
public class ArchiveJsonSyntaxTest {
    private static void valid(String value){ArchiveJsonSyntax.validate(value);}
    private static void invalid(String value){try{valid(value);fail("Accepted invalid JSON");}catch(IllegalArgumentException expected){}}
    private static String nested(int containers){StringBuilder s=new StringBuilder("{\"x\":");for(int i=1;i<containers;i++)s.append('[');s.append('0');for(int i=1;i<containers;i++)s.append(']');return s.append('}').toString();}
    @Test public void acceptsNestedStandardJsonAndOnlyJsonWhitespace(){valid(" \t\r\n{\"a\":[1,-2.5e+3,true,false,null,{\"b\":\"text\"}]}\n");valid("{}");}
    @Test public void requiresExactlyOneCompleteRootObject(){for(String value:new String[]{"","[]","null","{\"a\":1} trailing","{} {}","{}[]","{}\u0000","{}\u00a0"})invalid(value);}
    @Test public void rejectsDuplicateDecodedKeysAtEveryObjectLevel(){for(String value:new String[]{"{\"a\":1,\"a\":2}","{\"a\":1,\"\\u0061\":2}","{\"x\":{\"a\":1,\"a\":2}}"})invalid(value);valid("{\"x\":{\"a\":1},\"y\":{\"a\":2}}");}
    @Test public void rejectsCommentsSingleQuotesAndUnquotedNames(){for(String value:new String[]{"/*x*/{}","{}//x","{\"a\":/*x*/1}","{'a':1}","{a:1}","{\"a\":'x'}"})invalid(value);valid("{\"a\":\"it's /* a string */\"}");}
    @Test public void acceptsOnlyJsonNumberGrammar(){for(String n:new String[]{"0","-0","12","-2","0.1","1e2","1E-2","1.20e+03"})valid("{\"n\":"+n+"}");for(String n:new String[]{"+1","01","-01",".1","1.","1e","1e+","NaN","Infinity","0x1","--1","١"})invalid("{\"n\":"+n+"}");}
    @Test public void validatesStringEscapesControlsAndUnicodeHex(){for(String s:new String[]{"\\q","\\'","\\u00xz","\\uＦ000","line\nfeed","tab\t","\\"})invalid("{\"s\":\""+s+"\"}");valid("{\"s\":\"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u0041\"}");}
    @Test public void validatesSurrogatePairsWithoutReplacingUnknownText(){valid("{\"s\":\"\\uD83D\\uDE00\"}");valid("{\"s\":\"😀\"}");for(String s:new String[]{"\\uD800","\\uDC00","\\uD800\\u0041","\ud800","\udc00"})invalid("{\"s\":\""+s+"\"}");}
    @Test public void enforcesDepthBeforeRecursiveDescent(){valid(nested(ArchiveJsonSyntax.MAX_DEPTH));for(int depth:new int[]{33,4096}){try{valid(nested(depth));fail("Depth limit required");}catch(IllegalArgumentException expected){assertEquals("JSON nesting limit exceeded",expected.getMessage());}}}
    @Test public void stringDelimitersDoNotCountAsNesting(){StringBuilder s=new StringBuilder("{\"s\":\"");for(int i=0;i<4096;i++)s.append("[{}]");valid(s.append("\"}").toString());}
    @Test public void rejectsMissingSeparatorsAndUnterminatedStructures(){for(String s:new String[]{"{","{\"a\"}","{\"a\":}","{\"a\":1,}","{\"a\":[1,]}","{\"a\":[1 2]}","{\"a\":1;\"b\":2}","{\"a\":\"x}"})invalid(s);}
    @Test public void boundsCompleteDocumentLengthIncludingWhitespace(){StringBuilder s=new StringBuilder("{}");while(s.length()<ArchiveJsonSyntax.MAX_BYTES)s.append(' ');valid(s.toString());invalid(s.append(' ').toString());}
    @Test public void boundsUtf8BytesRatherThanOnlyCharacterCount(){StringBuilder s=new StringBuilder("{\"s\":\"");while(s.length()<600000)s.append('é');invalid(s.append("\"}").toString());}
}
