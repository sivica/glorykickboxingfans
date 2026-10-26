package com.ivicastojanoski.glorykickboxingfans.archive;

import org.junit.Test;
import java.io.*;
import java.net.*;
import java.security.cert.Certificate;
import javax.net.ssl.HttpsURLConnection;
import static org.junit.Assert.*;

/** Fake connection protocol/cleanup evidence. No TLS handshake, DNS or live server is exercised. */
public class HttpsArchiveTransportTest {
    private static final URI ENDPOINT=URI.create("https://example.invalid/archive.json");
    private static final class Connection extends HttpsURLConnection {
        int code=200;String type="application/json; charset=utf-8",encoding;long length=1;boolean disconnected,closed,fail;
        Connection() throws Exception{super(ENDPOINT.toURL());}
        public int getResponseCode(){return code;}public String getContentType(){return type;}public String getContentEncoding(){return encoding;}public long getContentLengthLong(){return length;}
        public InputStream getInputStream() throws IOException {if(fail)throw new IOException("Simulated open failure");return new ByteArrayInputStream(new byte[]{'3'}){@Override public void close() throws IOException {closed=true;super.close();}};}
        public void disconnect(){disconnected=true;}public boolean usingProxy(){return false;}public void connect(){}
        public String getCipherSuite(){return "fake";}public Certificate[] getLocalCertificates(){return null;}public Certificate[] getServerCertificates(){return null;}
    }
    private static void refused(Connection c) throws Exception {
        try {new HttpsArchiveTransport(uri->c).open(ENDPOINT);fail("Must refuse response");}catch(IOException expected){}assertTrue(c.disconnected);
    }
    @Test public void successUsesBoundedHttpsPolicyAndClosesConnection() throws Exception {
        Connection c=new Connection();try(InputStream in=new HttpsArchiveTransport(uri->c).open(ENDPOINT)){assertEquals('3',in.read());assertFalse(c.disconnected);}
        assertTrue(c.closed);assertTrue(c.disconnected);assertFalse(c.getInstanceFollowRedirects());assertFalse(c.getUseCaches());
        assertEquals(3000,c.getConnectTimeout());assertEquals(5000,c.getReadTimeout());assertEquals("GET",c.getRequestMethod());
        assertEquals("identity",c.getRequestProperty("Accept-Encoding"));assertEquals("application/json",c.getRequestProperty("Accept"));assertNull(c.getRequestProperty("Authorization"));assertNull(c.getRequestProperty("Cookie"));
    }
    @Test public void redirectsAndErrorsAreNeverFollowed() throws Exception{for(int code:new int[]{301,302,307,308,404,500}){Connection c=new Connection();c.code=code;refused(c);assertFalse(c.getInstanceFollowRedirects());}}
    @Test public void absentOrNonJsonMimeIsRefused() throws Exception{for(String type:new String[]{null,"text/html","application/octet-stream"}){Connection c=new Connection();c.type=type;refused(c);}}
    @Test public void compressedResponseIsRefused() throws Exception{Connection c=new Connection();c.encoding="gzip";refused(c);}
    @Test public void oversizedDeclaredLengthIsRefused() throws Exception{Connection c=new Connection();c.length=ArchiveUpdates.MAX_BYTES+1;refused(c);}
    @Test public void conflictingCharsetIsRefused() throws Exception{for(String type:new String[]{"application/json; charset=utf-16","application/json; charset = UTF-16","application/json; charset","application/json; charset=\"utf-16\""}){Connection c=new Connection();c.type=type;refused(c);}}
    @Test public void failedStreamOpenDisconnects() throws Exception{Connection c=new Connection();c.fail=true;refused(c);}
    @Test public void httpIsRefusedBeforeOpeningConnection() throws Exception{try{new HttpsArchiveTransport(uri->{throw new AssertionError("Connection touched");}).open(URI.create("http://example.invalid/archive.json"));fail();}catch(IllegalArgumentException expected){}}
}
