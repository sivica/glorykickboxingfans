package com.ivicastojanoski.glorykickboxingfans.archive;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;

/** Platform TLS only. No redirects, authentication, cookies, custom trust manager or response-body logging. */
public final class HttpsArchiveTransport implements ArchiveUpdates.Transport {
    interface ConnectionFactory { HttpsURLConnection open(URI endpoint) throws IOException; }
    private final ConnectionFactory factory;
    public HttpsArchiveTransport() { this(endpoint -> (HttpsURLConnection) endpoint.toURL().openConnection()); }
    HttpsArchiveTransport(ConnectionFactory factory) { this.factory = factory; }
    @Override public InputStream open(URI endpoint) throws IOException {
        ArchiveUpdates.Config.forApprovedEndpoint(endpoint.toString());
        HttpsURLConnection connection = factory.open(endpoint);
        boolean handedOff = false;
        try {
            connection.setInstanceFollowRedirects(false); connection.setUseCaches(false);
            connection.setConnectTimeout(ArchiveUpdates.CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(ArchiveUpdates.READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Encoding", "identity");
            if (connection.getResponseCode() != 200) throw new IOException("Dataset response refused");
            String type = connection.getContentType();
            if (type == null || !type.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) throw new IOException("JSON content type required");
            for (String parameter : type.split(";")) {
                String[] field = parameter.split("=", 2);
                if (field[0].trim().equalsIgnoreCase("charset")) {
                    String value = field.length == 2 ? field[1].trim().toLowerCase(Locale.ROOT) : "";
                    if (!value.equals("utf-8") && !value.equals("\"utf-8\"")) throw new IOException("UTF-8 dataset required");
                }
            }
            String encoding = connection.getContentEncoding();
            if (encoding != null && !encoding.trim().equalsIgnoreCase("identity")) throw new IOException("Encoded dataset response refused");
            long length = connection.getContentLengthLong();
            if (length > ArchiveUpdates.MAX_BYTES) throw new IOException("Dataset response exceeds limit");
            InputStream input = connection.getInputStream(); handedOff = true;
            return new FilterInputStream(input) {
                @Override public void close() throws IOException {
                    try { super.close(); } finally { connection.disconnect(); }
                }
            };
        } finally { if (!handedOff) connection.disconnect(); }
    }
}
