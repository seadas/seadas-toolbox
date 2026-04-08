package gov.nasa.gsfc.seadas.earthdatacloud.data;

import java.time.Instant;
import java.util.List;

public class VariableCacheEntry {
    private final String storageKey;
    private final List<VariableDescriptor> variables;
    private final String sourceGranuleId;
    private final Instant createdAt;

    public VariableCacheEntry(String storageKey,
                              List<VariableDescriptor> variables,
                              String sourceGranuleId,
                              Instant createdAt) {
        this.storageKey = storageKey;
        this.variables = variables;
        this.sourceGranuleId = sourceGranuleId;
        this.createdAt = createdAt;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public List<VariableDescriptor> getVariables() {
        return variables;
    }

    public String getSourceGranuleId() {
        return sourceGranuleId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
