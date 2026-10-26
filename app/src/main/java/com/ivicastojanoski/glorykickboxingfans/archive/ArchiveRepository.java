package com.ivicastojanoski.glorykickboxingfans.archive;

import android.content.Context;
import android.util.AtomicFile;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/** Single-process serialized store. Fixed private no-backup path, always with the bundled baseline. */
public final class ArchiveRepository implements ArchiveUpdates.Store {
    // No endpoint has been approved. Enabling also requires an explicit INTERNET permission review.
    private static final ArchiveUpdates.Config UPDATE_CONFIG = ArchiveUpdates.Config.disabled();
    private static ArchiveRepository instance;
    public static synchronized ArchiveRepository get(Context context) {
        if (instance == null) {
            Context application = context.getApplicationContext();
            instance = new ArchiveRepository(() -> application.getAssets().open("archive-demo-v1.json"),
                    new File(application.getNoBackupFilesDir(), "archive-update-v1.json"));
        }
        return instance;
    }
    public interface Bundled { InputStream open() throws IOException; }
    public static final class Snapshot {
        public final Archive archive;
        public final boolean cached;
        private Snapshot(Archive archive, boolean cached) { this.archive = archive; this.cached = cached; }
    }
    private final Bundled bundled;
    private final AtomicFile cache;
    // Explicit file/bundle injection permits deterministic platform persistence tests, without network.
    public ArchiveRepository(Bundled bundled, File cacheFile) { this.bundled = bundled; this.cache = new AtomicFile(cacheFile); }

    public synchronized Snapshot load() throws IOException {
        Archive baseline;
        try (InputStream input = bundled.open()) { baseline = ArchiveReader.read(input); }
        try (InputStream input = cache.openRead()) {
            Archive saved = ArchiveReader.read(input);
            if (saved.id.equals(baseline.id) && saved.revision > baseline.revision) return new Snapshot(saved, true);
        } catch (IOException error) { /* Missing/corrupt/incompatible cache leaves the baseline available. */ }
        return new Snapshot(baseline, false);
    }
    @Override public synchronized Archive current() throws IOException { return load().archive; }

    @Override public synchronized boolean activate(byte[] payload) throws IOException {
        if (payload.length > ArchiveUpdates.MAX_BYTES) throw new IOException("Dataset exceeds size limit");
        byte[] accepted = payload.clone();
        ArchiveUpdates.cancelled();
        // Reparse inside this lock: callers cannot bypass semantic validation or a newer concurrent commit.
        Archive candidate = ArchiveReader.read(new ByteArrayInputStream(accepted));
        Archive before = current();
        if (!candidate.id.equals(before.id)) throw new IOException("Dataset identity changed");
        if (candidate.revision <= before.revision) return false;
        FileOutputStream output = cache.startWrite();
        try {
            output.write(accepted); output.flush(); output.getFD().sync(); ArchiveUpdates.cancelled();
            cache.finishWrite(output);
        } catch (IOException | RuntimeException error) { cache.failWrite(output); throw error; }
        try (InputStream input = cache.openRead()) {
            if (!Arrays.equals(accepted, ArchiveUpdates.readBounded(input))) throw new IOException("Dataset commit readback failed");
        }
        return true;
    }

    /** Integration hook only; no scheduler/button/automatic request is installed while configuration is disabled. */
    public ArchiveUpdates.Outcome checkForUpdates() {
        return new ArchiveUpdates(UPDATE_CONFIG, new HttpsArchiveTransport(), ArchiveReader::read, this,
                () -> System.nanoTime() / 1000000).check();
    }
}
