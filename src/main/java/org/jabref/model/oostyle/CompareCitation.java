package org.jabref.model.oostyle;

import java.util.Comparator;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/*
 * Given a Comparator<BibEntry> provide a Comparator<ComparableCitation>
 * that can handle unresolved citation keys and take pageInfo into account.
 */
public class CompareCitation implements Comparator<ComparableCitation> {

    CompareCitedKey citedKeyComparator;

    CompareCitation(Comparator<BibEntry> entryComparator, boolean unresolvedComesFirst) {
        this.citedKeyComparator = new CompareCitedKey(entryComparator, unresolvedComesFirst);
    }

    public int compare(ComparableCitation a, ComparableCitation b) {
        int res = citedKeyComparator.compare(a, b);

        // Also consider pageInfo
        if (res == 0) {
            res = CompareCitation.comparePageInfo(a.getPageInfo(), b.getPageInfo());
        }
        return res;
    }

    /**
     * Defines sort order for pageInfo strings.
     *
     * Optional.empty comes before non-empty.
     */
    public static int comparePageInfo(Optional<OOText> a, Optional<OOText> b) {

        Optional<OOText> aa = Citation.normalizePageInfo(a);
        Optional<OOText> bb = Citation.normalizePageInfo(b);
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


