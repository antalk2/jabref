package org.jabref.model.oostyle;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.Pair;

public class Citation implements CitationSort.ComparableCitation, CitationMarkerEntry {

    /** key in database */
    public final String citationKey;

    /** Result from database lookup. Optional.empty() if not found. */
    private Optional<CitationDatabaseLookup.Result> db;

    /** The number used for numbered citation styles . */
    private Optional<Integer> number;

    /** Letter that makes the in-text citation unique. */
    private Optional<String> uniqueLetter;

    /** pageInfo */
    private Optional<OOFormattedText> pageInfo;

    /** isFirstAppearanceOfSource */
    private boolean isFirstAppearanceOfSource;

    /**
     *
     */
    public Citation(String citationKey) {
        this.citationKey = citationKey;
        this.db = Optional.empty();
        this.number = Optional.empty();
        this.uniqueLetter = Optional.empty();
        this.pageInfo = Optional.empty();
        this.isFirstAppearanceOfSource = false;
    }

    @Override
    public String getCitationKey() {
        return citationKey;
    }

    @Override
    public Optional<OOFormattedText> getPageInfo() {
        return pageInfo;
    }

    @Override
    public boolean getIsFirstAppearanceOfSource() {
        return isFirstAppearanceOfSource;
    }

    @Override
    public Optional<BibEntry> getBibEntry() {
        return (db.isPresent()
                ? Optional.of(db.get().entry)
                : Optional.empty());
    }

    public void lookup(List<BibDatabase> databases) {
        db = CitationDatabaseLookup.lookup(databases, citationKey);
    }

    public Optional<CitationDatabaseLookup.Result> getDatabaseLookupResult() {
        return db;
    }

    public void setDatabaseLookupResult(Optional<CitationDatabaseLookup.Result> db) {
        this.db = db;
    }

    public boolean isUnresolved() {
        return db.isEmpty();
    }

    public Optional<Integer> getNumber() {
        return number;
    }

    public int getNumberOrThrow() {
        return number.get();
    }

    public Optional<String> getUniqueLetter() {
        return uniqueLetter;
    }

    public void setUniqueLetter(Optional<String> uniqueLetter) {
        this.uniqueLetter = uniqueLetter;
    }

    public void setPageInfo(Optional<OOFormattedText> v) {
        Optional<OOFormattedText> vv = normalizePageInfo(v);
        if (!vv.equals(v)) {
            throw new RuntimeException("setPageInfo argument is not normalized");
        }
        this.pageInfo = vv;
    }

    public void setIsFirstAppearanceOfSource(boolean value) {
        isFirstAppearanceOfSource = value;
    }

    /*
     * Setters for CitationGroups.distribute()
     */
    public static void setDatabaseLookupResult(Pair<Citation, Optional<CitationDatabaseLookup.Result>> x) {
        Citation cit = x.a;
        cit.db = x.b;
    }

    public static void setNumber(Pair<Citation, Optional<Integer>> x) {
        Citation cit = x.a;
        cit.number = x.b;
    }

    public static void setUniqueLetter(Pair<Citation, Optional<String>> x) {
        Citation cit = x.a;
        cit.uniqueLetter = x.b;
    }

    /*
     * pageInfo normalization
     */
    public static Optional<OOFormattedText> normalizePageInfo(Optional<OOFormattedText> o) {
        if (o == null || o.isEmpty() || "".equals(OOFormattedText.toString(o.get()))) {
            return Optional.empty();
        }
        String s = OOFormattedText.toString(o.get());
        if (s.trim().equals("")) {
            return Optional.empty();
        }
        return Optional.of(OOFormattedText.fromString(s.trim()));
    }
}
