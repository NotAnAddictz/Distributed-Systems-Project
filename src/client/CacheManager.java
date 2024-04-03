package client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import common.Helper;

public class CacheManager {
    private Map<String, CacheEntry> cache;
    private long freshnessIntervalSeconds;

    public CacheManager(long freshnessIntervalSeconds) {
        this.cache = new HashMap<>();
        this.freshnessIntervalSeconds = freshnessIntervalSeconds;
    }

    // Add or Update cache content based on filename, offset and data to be inserted.
    // LastModifiedTime should be updated manually after server reply.
    public void addToCache(String filename, int offset, String data) {
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
        System.out.println("UPDATED CACHE: " + Arrays.toString(cacheEntry.getData()));
    }

    // Clear contents in cache and overwrites file content entirely with new data. LastModifiedTime is updated.
    public void clearAndReplaceCache(String filename, String newData, Long lastModifiedTime) {
        char[] charData = newData.toCharArray();
        CacheEntry cacheEntry = new CacheEntry(charData, System.currentTimeMillis());
        cache.put(filename, cacheEntry);
        setLastModified(filename, lastModifiedTime);
        System.out.println("UPDATED CACHE: " + Arrays.toString(charData));
    }

    // Reads from cache based on filename, offset and numBytes. return null if data is not found in cache.
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

    // Remove content from cache.
    public void removeFromCache(String filename) {
        cache.remove(filename);
        System.out.println(filename  + " removed from cache");
    }

    // Used to check that data does not have leading or trailing space assumed to not be in a valid file content.
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

    // Set lastValidated of cache content to current time. lastValidated applies to entire file.
    public void setValidated(String fileName) {
        CacheEntry entry = cache.get(fileName);
        entry.setLastValidated(System.currentTimeMillis());
    }
    
    // Method to check if a cache entry is valid based on freshness interval.
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

    // Set lastModified of a file after server replies the lastModifiedTime set for the specific file.
    public void setLastModified(String fileName, Long lastModifiedTime) {
        CacheEntry entry = cache.get(fileName);
        if (lastModifiedTime != null) {
            entry.setLastModified(lastModifiedTime);
        }
    }

    // Used to check if cache content is up to date with server. 
    // ServerLastModifiedTime is retrieved from server to compare.
    public boolean isModified(String fileName, Long serverLastModifiedTime) {
        CacheEntry entry = cache.get(fileName);
        if (entry.getLastModified() < serverLastModifiedTime) {
            return true;
        } else {
            return false;
        }
    }

    // Used to check if file exist in cache regardless of content.
    public boolean fileExistInCache(String fileName) {
        return cache.containsKey(fileName);
    }

    // Used to check that the string indicated is the only string in the cache of a file. 
    // Important for when deciding whether to update lastModifiedTime of a cache 
    // as there may be existing content in the cache that is not up to date.
    public boolean stringExists(String fileName, String str) {
        String arrayString = new String(cache.get(fileName).getData());
        int index = arrayString.indexOf(str);
        if (index != -1) {
            // Ensure there are no other characters before or after the matched substring
            if ((index == 0 || arrayString.charAt(index - 1) == ',') &&
                (index + str.length() == arrayString.length() || arrayString.charAt(index + str.length()) == ',')) {
                return true;
            }
        }
        return false;
    }

    // To visualise cache content in client side
    public void printCacheContents() {
        System.out.println();
        System.out.println("-------------------CACHE CONTENTS-------------------");
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            String filename = entry.getKey();
            CacheEntry cacheEntry = entry.getValue();
            char[] data = cacheEntry.getData();
            long lastModified = cacheEntry.getLastModified();
            System.out.println("Filename: " + filename + " | " + "Data: " + Arrays.toString(data) + " | " + "Last Modified Time: " + Helper.convertLastModifiedTime(lastModified));
        }
        System.out.println("----------------------------------------------------");
        System.out.println();
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
