package org.jabref.model.oostyle;

// import org.jabref.model.oostyle.CitationDatabaseLookup;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class CitationDatabaseLookup {

    public static class Result {
        public final BibEntry entry;
        public final BibDatabase database;
        public Result(BibEntry entry, BibDatabase database) {
            Objects.requireNonNull(entry);
            Objects.requireNonNull(database);
            this.entry = entry;
            this.database = database;
        }
    }

    public static Optional<Result> lookup(List<BibDatabase> databases, String key) {
        for (BibDatabase database : databases) {
            Optional<BibEntry> entry = database.getEntryByCitationKey(key);
            if (entry.isPresent()) {
                return Optional.of(new Result(entry.get(), database));
            }
        }
        return Optional.empty();
    }

}
