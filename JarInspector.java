import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarInspector {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java JarInspector <jar-path>");
            return;
        }

        try (JarFile jarFile = new JarFile(args[0])) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    System.out.println(entry.getName().replace("/", ".").replace(".class", ""));
                }
            }
        }
    }
}
