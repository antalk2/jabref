package org.jabref.model.openoffice;

import java.util.List;
import java.util.Optional;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

public class StorageBase {

    public interface HasName {
        public String getName();
    }

    public interface HasTextRange {

        /**
         * @return Optional.empty if the mark is missing from the document.
         */
        public Optional<XTextRange> getMarkRange(XTextDocument doc)
            throws
            NoDocumentException,
            WrappedTargetException;

        /**
         * Cursor for the reference marks as is, not prepared for filling,
         * but does not need cleanFillCursorForCitationGroup either.
         */
        public Optional<XTextCursor> getRawCursor(XTextDocument doc)
            throws
            NoDocumentException,
            WrappedTargetException;

        /**
         * Get a cursor for filling in text.
         *
         * Must be followed by cleanFillCursor()
         */
        public XTextCursor getFillCursor(XTextDocument doc)
            throws
            NoDocumentException,
            WrappedTargetException,
            CreationException;

        /**
         * Remove brackets, but if the result would become empty, leave
         * them; if the result would be a single characer, leave the left bracket.
         *
         */
        public void cleanFillCursor(XTextDocument doc)
            throws
            NoDocumentException,
            WrappedTargetException,
            CreationException;

        /**
         *  Note: create is in NamedRangeManager
         */
        public void removeFromDocument(XTextDocument doc)
            throws
            WrappedTargetException,
            NoDocumentException,
            NoSuchElementException;
    }

    public interface NamedRange extends HasName, HasTextRange {
        // nothing new here
    }

    public interface NamedRangeManager {
        public NamedRange create(XTextDocument doc,
                                 String markName,
                                 XTextCursor position,
                                 boolean insertSpaceAfter,
                                 boolean withoutBrackets)
            throws
            CreationException;

        public List<String> getUsedNames(XTextDocument doc)
            throws
            NoDocumentException;

        public Optional<NamedRange> getFromDocument(XTextDocument doc, String markName)
            throws
            NoDocumentException,
            WrappedTargetException;
    }
}