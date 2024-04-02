package server;

import java.util.HashMap;
import java.util.Map;

import common.Helper;

public class FileManager {
    private Map<String, Long> lastModifiedMap;

    public FileManager() {
        this.lastModifiedMap = new HashMap<>();
    }

    public void addFile(String filename, long lastModifiedTime) {
        lastModifiedMap.put(filename, lastModifiedTime);
    }

    public void updateLastModifiedTime(String filename, long lastModifiedTime) {
        lastModifiedMap.put(filename, lastModifiedTime);
    }

    public Long getLastModifiedTime(String filename) {
        return lastModifiedMap.getOrDefault(filename, null);
    }

    public void removeFile(String filename) {
        lastModifiedMap.remove(filename);
    }

    public void printAllFiles() {
        Helper.printFileLastModifiedTime(lastModifiedMap);
    }
}
