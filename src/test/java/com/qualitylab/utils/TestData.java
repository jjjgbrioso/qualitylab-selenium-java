package com.qualitylab.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test data from src/test/resources/testdata/*.json, plus generated values that keep repeated runs
 * against the live site from colliding: unique amounts and far-future stay dates.
 */
public final class TestData {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, JsonNode> CACHE = new ConcurrentHashMap<>();

    private TestData() {
    }

    /** A value such as {@code TestData.get("ecommerce", "checkout.cardNumber")}. */
    public static String get(String file, String path) {
        JsonNode node = CACHE.computeIfAbsent(file, TestData::load);
        for (String part : path.split("\\.")) {
            node = node.path(part);
        }
        if (node.isMissingNode() || node.isNull()) {
            throw new IllegalArgumentException("No test data at " + file + ".json > " + path);
        }
        return node.asText();
    }

    /**
     * An amount between ₱1.01 and ₱9.99 with random centavos. The app refuses an identical transaction
     * within 30 seconds, so fixed amounts would make back-to-back runs fail.
     */
    public static BigDecimal uniqueSmallAmount() {
        int centavos = ThreadLocalRandom.current().nextInt(101, 1000);
        return BigDecimal.valueOf(centavos, 2).setScale(2, RoundingMode.UNNECESSARY);
    }

    /** A check-in date 400 to 700 days ahead, far from the seeded bookings and from other runs. */
    public static LocalDate freeCheckInDate() {
        return LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(400, 700));
    }

    private static JsonNode load(String file) {
        String resource = "testdata/" + file + ".json";
        try (InputStream stream = TestData.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalArgumentException(resource + " was not found.");
            }
            return MAPPER.readTree(stream);
        } catch (IOException error) {
            throw new UncheckedIOException("Could not read " + resource, error);
        }
    }
}
