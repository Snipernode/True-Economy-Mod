package com.economy.mod.core;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Identity linking: 6-char codes, 5min TTL, merges via LinkMerger. */
public final class LinkManager {
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final long PENDING_TTL_MS = 300000L;

    public enum Result { LINKED, NO_SUCH_CODE, EXPIRED, ALREADY_LINKED, SELF_LINK }

    private final SecureRandom random = new SecureRandom();
    private final LinkMerger merger;
    private final Map<String, String> canonicalByKey = new HashMap<>();
    private final Map<String, String> canonicalByOwner = new HashMap<>();
    private final Map<String, PendingLink> pending = new HashMap<>();

    private static final class PendingLink {
        final String startKey; final String startOwner; final long expiresAt;
        PendingLink(String k, String o, long e) { startKey = k; startOwner = o; expiresAt = e; }
    }

    public LinkManager(LinkMerger merger) { this.merger = merger; }

    public String canonicalKey(String key) { return canonicalByKey.getOrDefault(key, key); }
    public String canonicalOwner(String owner) { return canonicalByOwner.getOrDefault(owner, owner); }
    public boolean isLinked(String identityKey) { return canonicalByKey.containsKey(identityKey); }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        return sb.toString();
    }

    private void pruneExpired() {
        pending.entrySet().removeIf(e -> e.getValue().expiresAt < System.currentTimeMillis());
    }

    public String startLink(String identityKey, String ownerName) {
        pruneExpired();
        String code;
        do { code = generateCode(); } while (pending.containsKey(code));
        pending.put(code, new PendingLink(canonicalKey(identityKey), canonicalOwner(ownerName), System.currentTimeMillis() + PENDING_TTL_MS));
        return code;
    }

    public Result completeLink(String code, String identityKey, String ownerName) {
        pruneExpired();
        if (code == null) return Result.NO_SUCH_CODE;
        String normalized = code.trim().toUpperCase();
        PendingLink p = pending.remove(normalized);
        if (p == null) return Result.NO_SUCH_CODE;
        if (p.expiresAt < System.currentTimeMillis()) return Result.EXPIRED;
        String callerKey = canonicalKey(identityKey);
        if (callerKey.equals(p.startKey)) return Result.SELF_LINK;
        if (isLinked(callerKey) || isLinked(p.startKey)) return Result.ALREADY_LINKED;

        String primaryKey, secondaryKey;
        boolean callerHasXuid = callerKey.startsWith("xuid:");
        if (callerHasXuid != p.startKey.startsWith("xuid:")) {
            if (callerHasXuid) { primaryKey = callerKey; secondaryKey = p.startKey; }
            else { primaryKey = p.startKey; secondaryKey = callerKey; }
        } else { primaryKey = p.startKey; secondaryKey = callerKey; }

        link(primaryKey, secondaryKey);
        merger.merge(primaryKey, secondaryKey);
        return Result.LINKED;
    }

    private void link(String primaryKey, String secondaryKey) {
        canonicalByKey.put(primaryKey, primaryKey);
        canonicalByKey.put(secondaryKey, primaryKey);
    }

    /** Global identity<->owner canonicalisation after restore. */
    public void restoreLink(String primaryKey, String secondaryKey) {
        canonicalByKey.put(primaryKey, primaryKey);
        canonicalByKey.put(secondaryKey, primaryKey);
    }

    public void lookupMaps(Map<String, String> keyMap, Map<String, String> ownerMap) {
        keyMap.putAll(canonicalByKey);
        ownerMap.putAll(canonicalByOwner);
    }

    public Set<String> pendingCodes() { return Set.copyOf(pending.keySet()); }
    public String pendingOwner(String code) {
        PendingLink p = pending.get(code);
        return p == null ? null : p.startOwner;
    }
}