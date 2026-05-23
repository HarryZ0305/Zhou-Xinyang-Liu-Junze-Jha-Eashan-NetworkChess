import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

//Locates bundled resources whether running from an IDE, a bin/ folder, or a jar
public final class Resources {
    private Resources() {}

    //Open a stream for relPath, returns null if not found. Caller closes it
    public static InputStream open(String relPath) {
        //Classpath, works once resources are bundled on the build path / in a jar
        InputStream cp = Resources.class.getClassLoader().getResourceAsStream(relPath);
        if (cp != null) return cp;

        //Filesystem fallback, try several base directories
        for (File base : candidateRoots()) {
            File f = new File(base, relPath);
            if (f.isFile()) {
                try { return new FileInputStream(f); }
                catch (FileNotFoundException ignored) {}
            }
        }
        return null;
    }

    private static List<File> candidateRoots() {
        List<File> roots = new ArrayList<>();
        roots.add(new File(".")); // current working directory
        roots.add(new File("ChessOL")); // repo root -> ChessOL
        try {
            URL loc = Resources.class.getProtectionDomain().getCodeSource().getLocation();
            File codeBase = new File(loc.toURI());           
            File dir = codeBase.isFile() ? codeBase.getParentFile() : codeBase;
            for (int i = 0; i < 4 && dir != null; i++) { // bin -> ChessOL -> repo root -> ...
                roots.add(dir);
                dir = dir.getParentFile();
            }
        } catch (Exception ignored) {}
        return roots;
    }
}