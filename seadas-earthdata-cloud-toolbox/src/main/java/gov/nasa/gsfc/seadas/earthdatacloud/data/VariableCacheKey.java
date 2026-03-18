package gov.nasa.gsfc.seadas.earthdatacloud.data;

import java.util.Objects;

public class VariableCacheKey {
    private final String collectionConceptId;
    private final String shortName;
    private final String version;
    private final String fileFormat;

    public VariableCacheKey(String collectionConceptId, String shortName, String version, String fileFormat) {
        this.collectionConceptId = safe(collectionConceptId);
        this.shortName = safe(shortName);
        this.version = safe(version);
        this.fileFormat = safe(fileFormat);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public String asStorageKey() {
        return collectionConceptId + "|" + shortName + "|" + version + "|" + fileFormat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableCacheKey)) return false;
        VariableCacheKey that = (VariableCacheKey) o;
        return Objects.equals(collectionConceptId, that.collectionConceptId)
                && Objects.equals(shortName, that.shortName)
                && Objects.equals(version, that.version)
                && Objects.equals(fileFormat, that.fileFormat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collectionConceptId, shortName, version, fileFormat);
    }

    @Override
    public String toString() {
        return asStorageKey();
    }
}
