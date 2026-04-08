package gov.nasa.gsfc.seadas.earthdatacloud.data;

import java.io.IOException;
import java.util.Optional;

public class JsonVariableCache implements VariableCache {
    @Override
    public Optional<VariableCacheEntry> load(VariableCacheKey key) throws IOException {
        return Optional.empty();
    }

    @Override
    public void save(VariableCacheKey key, VariableCacheEntry entry) throws IOException {

    }

    @Override
    public void remove(VariableCacheKey key) throws IOException {

    }
    // load/save JSON file under user cache directory
}
