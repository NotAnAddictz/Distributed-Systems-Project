package client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CacheManager {
    private Map<String, CacheEntry> cache;
    private long freshnessIntervalSeconds;

    public CacheManager(long freshnessIntervalSeconds) {
        this.cache = new HashMap<>();
        this.freshnessIntervalSeconds = freshnessIntervalSeconds;
    }

    public void addToCache(String filename, int offset, String data) {
        char[] charData = data.toCharArray();
        CacheEntry cacheEntry = cache.getOrDefault(filename, new CacheEntry(new char[offset + charData.length], System.currentTimeMillis()));

        if (cacheEntry.getData().length < offset + charData.length) {
            // Resize existing array
            char[] newData = new char[offset + charData.length];
            System.arraycopy(cacheEntry.getData(), 0, newData, 0, cacheEntry.getData().length);
            cacheEntry.setData(newData);
        }

        System.arraycopy(charData, 0, cacheEntry.getData(), offset, charData.length);
        cacheEntry.setLastValidated(System.currentTimeMillis());
        cache.put(filename, cacheEntry);
        System.out.println("UPDATED CACHE: " + Arrays.toString(cacheEntry.getData()));
    }

    public void clearAndReplaceCache(String filename, String newData) {
        char[] charData = newData.toCharArray();
        CacheEntry cacheEntry = new CacheEntry(charData, System.currentTimeMillis());
        cache.put(filename, cacheEntry);
        System.out.println("UPDATED CACHE: " + Arrays.toString(charData));
    }

    public String readFromCache(String filename, int offset, int numBytes) {
        CacheEntry cacheEntry = cache.get(filename);
        if (cacheEntry == null || !isValidated(cacheEntry, System.currentTimeMillis())) {
            // If cache entry doesn't exist or data is not fresh, return null
            return null;
        }

        char[] data = cacheEntry.getData();

        if (offset >= data.length || offset + numBytes > data.length) {
            return null;
        }

        if (hasLeadingOrTrailingSpace(data, offset, numBytes)) {
            return null;
        }

        int endIndex = Math.min(offset + numBytes, data.length);
        char[] result = new char[endIndex - offset];
        System.arraycopy(data, offset, result, 0, endIndex - offset);
        return new String(result);
    }

    private boolean hasLeadingOrTrailingSpace(char[] data, int offset, int numBytes) {
        // Check for leading empty space
        for (int i = offset; i < offset + numBytes; i++) {
            if (data[i] != '\u0000') {
                if (i != offset) {
                    return true;
                }
                break;
            }
        }

        // Check for trailing empty space
        for (int i = offset + numBytes - 1; i >= offset; i--) {
            if (data[i] != '\u0000') {
                if (i != offset + numBytes - 1) {
                    return true;
                }
                break;
            }
        }

        return false;
    }

    // Method to check if a cache entry is valid based on freshness interval
    private boolean isValidated(CacheEntry entry, long currentTime) {
        long tc = entry.getLastValidated();
        long freshnessInterval = freshnessIntervalSeconds*1000;
        if (currentTime - tc < freshnessInterval) {
            return true;
        } else {
            System.out.println("Cache exceeds freshness interval");
            return false;
        }
    }

    private static class CacheEntry {
        private char[] data;
        private long lastValidated;

        public CacheEntry(char[] data, long lastValidated) {
            this.data = data;
            this.lastValidated = lastValidated;
        }

        public char[] getData() {
            return data;
        }

        public void setData(char[] data) {
            this.data = data;
        }

        public long getLastValidated() {
            return lastValidated;
        }

        public void setLastValidated(long lastValidated) {
            this.lastValidated = lastValidated;
        }
    }
}
