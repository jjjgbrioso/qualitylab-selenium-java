package com.qualitylab.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Saves screenshots under target/screenshots, named after the scenario and the time. */
public final class ScreenshotUtil {

    private static final Path FOLDER = Paths.get("target", "screenshots");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ScreenshotUtil() {
    }

    public static String save(byte[] png, String scenarioName) {
        String safeName = scenarioName.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "").toLowerCase();
        Path file = FOLDER.resolve(safeName + "-" + LocalDateTime.now().format(STAMP) + ".png");
        try {
            Files.createDirectories(FOLDER);
            Files.write(file, png);
            return file.toAbsolutePath().toString();
        } catch (IOException error) {
            throw new UncheckedIOException("Could not save screenshot " + file, error);
        }
    }
}
