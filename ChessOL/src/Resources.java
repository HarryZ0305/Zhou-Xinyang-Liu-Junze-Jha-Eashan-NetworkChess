import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralised resource loader that hides where an asset physically lives. The same
 * compiled application has to run from three very different launch contexts — an IDE
 * working tree, a packaged jar, and a plain bin/ folder produced by javac — and each
 * one exposes resource paths differently. This class tries them in order so the rest
 * of the program can simply request a relative path and trust that it will be found
 * if it exists anywhere reasonable.
 */
public final class Resources {
    // Private constructor: this is a static utility class and must never be instantiated.
    private Resources() {}

    /**
     * Opens an InputStream for the resource at relPath. Prefers the classpath (works
     * for jar deployments and IDE builds) and falls back to scanning a list of likely
     * filesystem roots so command-line runs from random working directories still work.
     * Returns null when no candidate location contains the file; the caller is
     * responsible for closing the stream — typically inside a try-with-resources.
     */
    public static InputStream open(String relPath) {
        // Classpath lookup — succeeds whenever the resource has been bundled onto the
        // build path or packaged inside the jar alongside the .class files.
        InputStream cp = Resources.class.getClassLoader().getResourceAsStream(relPath);
        if (cp != null) return cp;

        // Filesystem fallback — walk each candidate root and return the first hit.
        // Necessary because javac/java launches from a bin/ folder do not place
        // sibling resources on the classpath automatically.
        for (File base : candidateRoots()) {
            File f = new File(base, relPath);
            if (f.isFile()) {
                try { return new FileInputStream(f); }
                catch (FileNotFoundException ignored) {} // race with another process — try the next root
            }
        }
        return null;
    }

    /**
     * Builds the ordered list of directories that {@link #open(String)} will probe.
     * Includes the current working directory, the conventional repo subfolder, and a
     * chain of ancestor directories above the class's own code source so that an
     * exploded build (bin/) and the original repo root can both be discovered without
     * any per-deployment configuration.
     */
    private static List<File> candidateRoots() {
        List<File> roots = new ArrayList<>();
        roots.add(new File(".")); // current working directory — covers most IDE/CLI launches
        roots.add(new File("ChessOL")); // repo root -> ChessOL subproject layout
        try {
            // Locate the directory holding the .class files (or the jar itself) so we can
            // climb upward and add a handful of ancestor directories to the search list.
            URL loc = Resources.class.getProtectionDomain().getCodeSource().getLocation();
            File codeBase = new File(loc.toURI());
            File dir = codeBase.isFile() ? codeBase.getParentFile() : codeBase;
            // Walk up at most four levels: bin -> ChessOL -> repo root -> parent.
            // Bounded so a deeply nested launch directory cannot drag us all the way to the filesystem root.
            for (int i = 0; i < 4 && dir != null; i++) {
                roots.add(dir);
                dir = dir.getParentFile();
            }
        } catch (Exception ignored) {} // protection domain may be null in some sandboxes — skip silently
        return roots;
    }
}
