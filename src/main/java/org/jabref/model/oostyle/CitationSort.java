package org.jabref.model.oostyle;

import java.util.Comparator;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

public class CitationSort {

    public interface ComparableCitation {

        public String getCitationKey();

        public Optional<BibEntry> getBibEntry();

        public Optional<OOFormattedText> getPageInfo();
    }

    public static class CitationComparator implements Comparator<ComparableCitation> {

        Comparator<BibEntry> entryComparator;
        boolean unresolvedComesFirst;

        CitationComparator(Comparator<BibEntry> entryComparator, boolean unresolvedComesFirst) {
            this.entryComparator = entryComparator;
            this.unresolvedComesFirst = unresolvedComesFirst;
        }

        public int compare(ComparableCitation a, ComparableCitation b) {
            Optional<BibEntry> aBibEntry = a.getBibEntry();
            Optional<BibEntry> bBibEntry = b.getBibEntry();
            final int mul = unresolvedComesFirst ? (+1) : (-1);

            int res = 0;
            if (aBibEntry.isEmpty() && bBibEntry.isEmpty()) {
                // Both are unresolved: compare them by citation key.
                res = a.getCitationKey().compareTo(b.getCitationKey());
            } else if (aBibEntry.isEmpty()) {
                return -mul;
            } else if (bBibEntry.isEmpty()) {
                return mul;
            } else {
                // Proper comparison of entries
                res = entryComparator.compare(aBibEntry.get(), bBibEntry.get());
            }
            // Also consider pageInfo
            if (res == 0) {
                CitationSort.comparePageInfo(a.getPageInfo(),
                                             b.getPageInfo());
            }
            return res;
        }
    }

    /**
     * Defines sort order for pageInfo strings.
     *
     * null comes before non-null
     */
    public static int comparePageInfo(Optional<OOFormattedText> a, Optional<OOFormattedText> b) {

        Optional<OOFormattedText> aa = Citation.normalizePageInfo(a);
        Optional<OOFormattedText> bb = Citation.normalizePageInfo(b);
        if (aa.isEmpty() && bb.isEmpty()) {
            return 0;
        }
        if (aa.isEmpty()) {
            return -1;
        }
        if (bb.isEmpty()) {
            return +1;
        }
        return aa.get().asString().compareTo(bb.get().asString());
    }
}