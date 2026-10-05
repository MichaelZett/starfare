package de.zettsystems.starfare.simulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;

final class SourceFingerprint {
    private SourceFingerprint() { }

    static String current() throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var files = new ArrayList<Path>();
            for (String root : new String[]{"src/main/java", "src/test/java"}) {
                try (var paths = Files.walk(Path.of(root))) {
                    files.addAll(paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java")).sorted().toList());
                }
            }
            files.add(Path.of("build.gradle"));
            for (Path file : files) {
                digest.update(file.toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(Files.readString(file).replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
