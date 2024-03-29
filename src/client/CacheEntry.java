package client;

public class CacheEntry {
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