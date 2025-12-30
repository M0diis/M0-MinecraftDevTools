package me.m0dii.nbteditor.multiversion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;

public class Version {

    private static Integer dataVersion;
    private static Map<String, Integer> dataVersions;

    public static int getDataVersion() {
        if (dataVersion == null) {
            readVersionJson();
        }

        return dataVersion;
    }

    private static void readVersionJson() {
        try (InputStream in = Version.class.getResourceAsStream("/version.json");
             InputStreamReader reader = new InputStreamReader(in)) {
            JsonObject data = new Gson().fromJson(reader, JsonObject.class);

            dataVersion = data.get("world_version").getAsInt();
        } catch (IOException e) {
            throw new UncheckedIOException("Error trying to parse version.json", e);
        }
    }

    public static Optional<Integer> getDataVersion(String version) {
        try {
            return Optional.of(Integer.parseInt(version));
        } catch (NumberFormatException ignored) {
        }

        if (dataVersions == null) {
            readDataVersionsJson();
        }

        return Optional.ofNullable(dataVersions.get(version));
    }

    private static void readDataVersionsJson() {
        try (InputStream in = MVMisc.getResource(IdentifierInst.of("m0-dev-tools", "data_versions.json")).orElseThrow()) {
            dataVersions = new Gson().fromJson(new InputStreamReader(in), new TypeToken<Map<String, Integer>>() {
            }.getType());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse data_versions.json", e);
        }
    }

}
