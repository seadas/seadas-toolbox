package gov.nasa.gsfc.seadas.earthdatacloud.data;

import java.io.IOException;
import java.util.Optional;

public interface VariableCache {
    Optional<VariableCacheEntry> load(VariableCacheKey key) throws IOException;
    void save(VariableCacheKey key, VariableCacheEntry entry) throws IOException;
    void remove(VariableCacheKey key) throws IOException;
}
