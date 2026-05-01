package live.streamcraft.model.response;

import java.util.List;

public record BatchImportResult(int created, int skipped, List<String> skippedNames) {

}
