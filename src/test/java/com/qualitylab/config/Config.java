package com.qualitylab.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Settings for a test run. Each value is looked up in this order, so the first one found wins:
 * <ol>
 *   <li>a JVM system property, such as {@code -DBASE_URL=...} or the short form {@code -Dbase.url=...}</li>
 *   <li>an environment variable, such as {@code BASE_URL}</li>
 *   <li>{@code config/secrets.properties}, which is git-ignored and optional</li>
 *   <li>{@code config/<env>.properties}, where env is {@code staging} (the default) or {@code local}</li>
 * </ol>
 * Credentials are never stored in the repository. They come from environment variables or the
 * git-ignored secrets file.
 */
public final class Config {

    public static final String BASE_URL = "BASE_URL";
    public static final String BROWSER = "BROWSER";
    public static final String HEADLESS = "HEADLESS";
    public static final String TEST_USERNAME = "TEST_USERNAME";
    public static final String TEST_PASSWORD = "TEST_PASSWORD";
    public static final String ADMIN_USERNAME = "ADMIN_USERNAME";
    public static final String ADMIN_PASSWORD = "ADMIN_PASSWORD";
    public static final String TIMEOUT_SECONDS = "TIMEOUT_SECONDS";

    private static final String ENVIRONMENT = firstNonBlank(System.getProperty("env"), System.getenv("ENV")).orElse("staging");
    private static final Properties FILE_VALUES = loadFiles();

    private Config() {
    }

    public static String environment() {
        return ENVIRONMENT;
    }

    public static String baseUrl() {
        String url = required(BASE_URL);
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static String browser() {
        return get(BROWSER).orElse("chrome").toLowerCase(Locale.ROOT);
    }

    public static boolean headless() {
        return Boolean.parseBoolean(get(HEADLESS).orElse("false"));
    }

    public static int timeoutSeconds() {
        return Integer.parseInt(get(TIMEOUT_SECONDS).orElse("20"));
    }

    /** Looks a setting up by its UPPER_SNAKE name. */
    public static Optional<String> get(String name) {
        String shortName = name.toLowerCase(Locale.ROOT).replace('_', '.');
        return firstNonBlank(
                System.getProperty(name),
                System.getProperty(shortName),
                System.getenv(name),
                FILE_VALUES.getProperty(name));
    }

    /** A setting the run cannot do without. The message says how to provide it. */
    public static String required(String name) {
        return get(name).orElseThrow(() -> new IllegalStateException(
                name + " is not set. Set the environment variable " + name
                        + ", pass -D" + name + "=..., or add it to src/test/resources/config/secrets.properties."));
    }

    private static Properties loadFiles() {
        Properties properties = new Properties();
        String environmentFile = "config/" + ENVIRONMENT + ".properties";
        if (!loadInto(properties, environmentFile)) {
            throw new IllegalStateException("Unknown environment '" + ENVIRONMENT + "': " + environmentFile + " was not found.");
        }
        loadInto(properties, "config/secrets.properties");
        return properties;
    }

    private static boolean loadInto(Properties properties, String resource) {
        try (InputStream stream = Config.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                return false;
            }
            properties.load(stream);
            return true;
        } catch (IOException error) {
            throw new IllegalStateException("Could not read " + resource, error);
        }
    }

    private static Optional<String> firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }
}
