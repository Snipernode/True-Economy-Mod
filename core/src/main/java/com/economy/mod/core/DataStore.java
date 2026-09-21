package com.economy.mod.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** JSON persistence for all economy state. */
public final class DataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;

    public DataStore(Path file) { this.file = file; }
    public Path getFile() { return file; }

    private static final Type MAP_STRING_ACCOUNT = new TypeToken<Map<String, long[]>>() {}.getType();
    private static final Type MAP_STRING_SET = new TypeToken<Map<String, List<String>>>() {}.getType();
    private static final Type MAP_STRING_STRING = new TypeToken<Map<String, String>>() {}.getType();
    private static final Type MAP_STRING_DOUBLE = new TypeToken<Map<String, Double>>() {}.getType();

    public void loadInto(EconomyCore core) {
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file)) {
            Map<String, Object> root = GSON.fromJson(r, new TypeToken<Map<String, Object>>() {}.getType());
            if (root == null) return;

            Map<String, long[]> accounts = cast(GSON.fromJson(GSON.toJson(root.get("accounts")), MAP_STRING_ACCOUNT), new HashMap<>());
            accounts.forEach((owner, v) ->
                core.economy().restore(owner, v[0], v[1], (int) v[2]));

            Map<String, List<String>> disc = cast(GSON.fromJson(GSON.toJson(root.get("discoveries")), MAP_STRING_SET), new HashMap<>());
            disc.forEach((key, blocks) -> blocks.forEach(b -> core.catalog().record(key, b)));

            Map<String, String> links = cast(GSON.fromJson(GSON.toJson(root.get("links")), MAP_STRING_STRING), new HashMap<>());
            links.forEach((k, canon) -> { if (!k.equals(canon)) core.links().restoreLink(canon, k); });

            Map<String, Double> wallets = cast(GSON.fromJson(GSON.toJson(root.get("wallets")), MAP_STRING_DOUBLE), new HashMap<>());
            wallets.forEach((region, bal) -> core.government().wallet(region).deposit(bal));

        } catch (IOException | RuntimeException ignored) {
        }
    }

    public void save(EconomyCore core) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Writer w = Files.newBufferedWriter(file);
            Map<String, Object> root = new HashMap<>();
            Map<String, long[]> accounts = new HashMap<>();
            core.economy().accounts().forEach((o, a) -> accounts.put(o, new long[]{
                    Double.doubleToLongBits(a.getBalance()), Double.doubleToLongBits(a.getBankBalance()), a.getCreditScore()}));
            root.put("accounts", accounts);
            Map<String, List<String>> disc = new HashMap<>();
            core.catalog().discoveriesById().forEach((k, s) -> disc.put(k, List.copyOf(s)));
            root.put("discoveries", disc);
            Map<String, String> links = new HashMap<>();
            core.links().lookupMaps(links, new HashMap<>());
            root.put("links", links);
            Map<String, Double> wallets = new HashMap<>();
            core.government().wallets().forEach((r, wl) -> wallets.put(r, wl.getBalance()));
            root.put("wallets", wallets);
            GSON.toJson(root, w);
            w.flush();
            w.close();
        } catch (IOException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o, T fallback) {
        try { return o == null ? fallback : (T) o; } catch (ClassCastException e) { return fallback; }
    }
}