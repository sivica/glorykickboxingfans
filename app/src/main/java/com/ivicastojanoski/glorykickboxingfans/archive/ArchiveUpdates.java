package com.ivicastojanoski.glorykickboxingfans.archive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** One explicit update attempt. No scheduler, network side effect when disabled, or UI status fiction. */
public final class ArchiveUpdates {
    public static final int MAX_BYTES = 1024 * 1024;
    public static final int CONNECT_TIMEOUT_MS = 3000, READ_TIMEOUT_MS = 5000;
    public static final long TRANSFER_BUDGET_MS = 10000;
    public enum Outcome { DISABLED, ACTIVATED, STALE, REJECTED, UNAVAILABLE, CANCELLED }
    public interface Decoder { Archive decode(InputStream stream) throws IOException; }
    public interface Transport { InputStream open(URI endpoint) throws IOException; }
    public interface Store {
        Archive current() throws IOException;
        /** Revalidate and recheck revision under the store's write lock before atomic commit. */
        boolean activate(byte[] payload) throws IOException;
    }
    public interface Clock { long millis(); }

    public static final class Config {
        public final URI endpoint;
        private Config(URI endpoint) { this.endpoint = endpoint; }
        public static Config disabled() { return new Config(null); }
        /** Calling this factory does not grant owner approval; production configuration needs review. */
        public static Config forApprovedEndpoint(String value) {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || host == null || !host.contains(".")
                    || host.matches("[0-9.]+") || host.contains(":") || host.endsWith(".")
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443) || !uri.equals(uri.normalize())
                    || uri.getRawPath() == null || !uri.getRawPath().startsWith("/")) {
                throw new IllegalArgumentException("An approved HTTPS dataset URL without credentials/query/redirects is required");
            }
            return new Config(uri);
        }
    }

    private final Config config;
    private final Transport transport;
    private final Decoder decoder;
    private final Store store;
    private final Clock clock;
    public ArchiveUpdates(Config config, Transport transport, Decoder decoder, Store store, Clock clock) {
        this.config = config; this.transport = transport; this.decoder = decoder; this.store = store; this.clock = clock;
    }

    /** Run on a worker thread. Existing screens retain their immutable snapshot until recreated. */
    public Outcome check() {
        if (config.endpoint == null) return Outcome.DISABLED;
        if (Thread.currentThread().isInterrupted()) return Outcome.CANCELLED;
        byte[] bytes;
        Archive current;
        long started = clock.millis();
        try {
            current = store.current();
            try (InputStream input = transport.open(config.endpoint)) {
                bytes = readBounded(input, () -> {
                    cancelled();
                    if (clock.millis() - started > TRANSFER_BUDGET_MS) throw new SocketTimeoutException("Dataset transfer budget exceeded");
                });
            }
        } catch (InterruptedIOException error) {
            return Thread.currentThread().isInterrupted() ? Outcome.CANCELLED : Outcome.UNAVAILABLE;
        } catch (IOException | SecurityException error) { return Outcome.UNAVAILABLE; }
        Archive candidate;
        try {
            // Reject invalid UTF-8 rather than silently replacing unknown bytes in names/history.
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            candidate = decoder.decode(new ByteArrayInputStream(bytes));
        } catch (IOException | IllegalArgumentException error) { return Outcome.REJECTED; }
        if (!candidate.id.equals(current.id)) return Outcome.REJECTED;
        if (candidate.revision <= current.revision) return Outcome.STALE;
        try {
            cancelled();
            return store.activate(bytes) ? Outcome.ACTIVATED : Outcome.STALE;
        } catch (InterruptedIOException error) {
            return Thread.currentThread().isInterrupted() ? Outcome.CANCELLED : Outcome.UNAVAILABLE;
        } catch (IOException error) { return Outcome.UNAVAILABLE; }
    }

    private interface Guard { void check() throws IOException; }
    static byte[] readBounded(InputStream input) throws IOException { return readBounded(input, ArchiveUpdates::cancelled); }
    private static byte[] readBounded(InputStream input, Guard guard) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] chunk = new byte[4096];
        while (true) {
            guard.check(); int count = input.read(chunk); guard.check();
            if (count == -1) return bytes.toByteArray();
            if (count == 0) throw new IOException("Dataset stream made no progress");
            if (count > MAX_BYTES - bytes.size()) throw new IOException("Dataset exceeds size limit");
            bytes.write(chunk, 0, count);
        }
    }
    static void cancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Dataset operation cancelled");
    }
}
