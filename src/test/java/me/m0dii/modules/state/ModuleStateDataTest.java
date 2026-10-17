package me.m0dii.modules.state;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModuleStateDataTest {
    @Test
    void favoritesToggle() {
        ModuleStateData data = new ModuleStateData();
        assertTrue(data.toggleFavorite("xray"));
        assertTrue(data.isFavorite("xray"));
        assertFalse(data.toggleFavorite("xray"));
        assertFalse(data.isFavorite("xray"));
    }

    @Test
    void profilesAreIndependentCopies() {
        ModuleStateData data = new ModuleStateData();
        data.enabled.put("light_overlay", true);
        JsonObject settings = new JsonObject();
        settings.addProperty("xzRadius", 24);
        data.settings.put("light_overlay", settings);

        data.saveProfile("Redstone Build");
        settings.addProperty("xzRadius", 8);
        data.enabled.put("light_overlay", false);

        ModuleStateData.Profile profile = data.profiles.get("redstone_build");
        assertNotNull(profile);
        assertEquals(true, profile.enabled.get("light_overlay"));
        assertEquals(24, profile.settings.get("light_overlay").get("xzRadius").getAsInt());
        assertEquals("redstone_build", data.activeProfile);
    }

    @Test
    void nextProfileCyclesInNameOrder() {
        ModuleStateData data = new ModuleStateData();
        assertNull(data.nextProfileName());
        data.saveProfile("b");
        data.saveProfile("a");
        data.activeProfile = "a";
        assertEquals("b", data.nextProfileName());
        data.activeProfile = "b";
        assertEquals("a", data.nextProfileName());
    }

    @Test
    void survivesJsonRoundTripAndMissingFields() {
        ModuleStateData data = new ModuleStateData();
        data.enabled.put("waypoints", true);
        data.favorites.add("ruler");
        data.saveProfile("p");
        Gson gson = new Gson();
        ModuleStateData copy = gson.fromJson(gson.toJson(data), ModuleStateData.class).normalized();
        assertEquals(true, copy.enabled.get("waypoints"));
        assertTrue(copy.isFavorite("ruler"));
        assertTrue(copy.profiles.containsKey("p"));

        ModuleStateData sparse = gson.fromJson("{\"enabled\":{\"a\":true,\"b\":null}}", ModuleStateData.class).normalized();
        assertEquals(1, sparse.enabled.size());
        assertNotNull(sparse.favorites);
        assertNotNull(sparse.profiles);
    }
}
