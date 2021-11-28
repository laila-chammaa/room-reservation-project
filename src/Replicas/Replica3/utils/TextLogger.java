package Replicas.Replica3.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

public class TextLogger {
    private Path file;

    public TextLogger(String fileName) {
        this.file = Paths.get("Replica3Logs"+ "/" + fileName);
        Path dirPath = Paths.get("Replica3Logs");
        try {
            if (!Files.exists(dirPath))
                Files.createDirectory(dirPath);

            if (!Files.exists(this.file))
                Files.createFile(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String line) {
        try {
            String lines = new Date().toString() + ": " + line + "\n";
            Files.write(this.file, lines.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
