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

    public void addToCache(String filename, int offset, String data, Long lastModifiedTime) {
        char[] charData = data.toCharArray();
        CacheEntry cacheEntry = cache.getOrDefault(filename, new CacheEntry(new char[offset + charData.length], System.currentTimeMillis()));

        if (cacheEntry.getData().length < offset + charData.length) {
            // Resize existing array
            char[] newData = new char[offset + charData.length];
            System.arraycopy(cacheEntry.getData(), 0, newData, 0, cacheEntry.getData().length);
            cacheEntry.setData(newData);
            cacheEntry.setLastValidated(System.currentTimeMillis());
        }

        System.arraycopy(charData, 0, cacheEntry.getData(), offset, charData.length);
        cacheEntry.setLastValidated(System.currentTimeMillis());
        cache.put(filename, cacheEntry);
        if (lastModifiedTime != null) {
            //null when we addToCache before sending changes to server, receive() will update lastModified instead in this case.
            setLastModified(filename, lastModifiedTime);
        }
        System.out.println("UPDATED CACHE: " + Arrays.toString(cacheEntry.getData()));
    }

    public void clearAndReplaceCache(String filename, String newData, Long lastModifiedTime) {
        char[] charData = newData.toCharArray();
        CacheEntry cacheEntry = new CacheEntry(charData, System.currentTimeMillis());
        cache.put(filename, cacheEntry);
        setLastModified(filename, lastModifiedTime);
        System.out.println("UPDATED CACHE: " + Arrays.toString(charData));
    }

    public String readFromCache(String filename, int offset, int numBytes) {
        CacheEntry cacheEntry = cache.get(filename);
        if (cacheEntry == null) {
            // If cache entry doesn't exist, return null
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

    public void setValidated(String fileName) {
        CacheEntry entry = cache.get(fileName);
        entry.setLastValidated(System.currentTimeMillis());
    }
    // Method to check if a cache entry is valid based on freshness interval
    public boolean isValidated(String fileName) {
        CacheEntry entry = cache.get(fileName);
        long tc = entry.getLastValidated();
        long freshnessInterval = freshnessIntervalSeconds*1000;
        if (System.currentTimeMillis() - tc < freshnessInterval) {
            return true;
        } else {
            System.out.println("Cache exceeds freshness interval");
            return false;
        }
    }

    public void setLastModified(String fileName, Long lastModifiedTime) {
        CacheEntry entry = cache.get(fileName);
        if (lastModifiedTime != null) {
            entry.setLastModified(lastModifiedTime);
        }
    }

    public boolean isModified(String fileName, Long serverLastModifiedTime) {
        CacheEntry entry = cache.get(fileName);
        if (entry.getLastModified() < serverLastModifiedTime) {
            return true;
        } else {
            return false;
        }
    }

    public boolean fileExistInCache(String fileName) {
        return cache.containsKey(fileName);
    }

    private static class CacheEntry {
        private char[] data;
        private long lastValidated;
        private long lastModified;

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

        public long getLastModified() {
            return lastModified;
        }
    
        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }
    }
}
