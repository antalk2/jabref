package org.jabref.logic.openoffice;

import java.util.Optional;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.uno.Any;

public class UnoTextSection {

    /**
     *  @return An XNameAccess to find sections by name.
     */
    public static XNameAccess getNameAccess(XTextDocument doc)
        throws
        NoDocumentException {

        XTextSectionsSupplier supplier = UnoCast.unoQI(XTextSectionsSupplier.class, doc);
        try {
            return supplier.getTextSections();
        } catch (DisposedException ex) {
            throw new NoDocumentException("UnoTextSection.getNameAccess failed with" + ex);
        }
    }

    /**
     *  Get an XTextSection by name.
     */
    public static Optional<XTextSection> getByName(XTextDocument doc, String name)
        throws
        WrappedTargetException,
        NoDocumentException {
        XNameAccess nameAccess = getNameAccess(doc);
        try {
            return Optional.ofNullable((XTextSection)
                                       ((Any) nameAccess.getByName(name))
                                       .getObject());
        } catch (NoSuchElementException ex) {
            return Optional.empty();
        }
    }

    /**
     *  Create a text section with the provided name and insert it at
     *  the provided cursor.
     *
     *  @param name  The desired name for the section.
     *  @param range The location to insert at.
     *
     *  If an XTextSection by that name already exists,
     *  LibreOffice (6.4.6.2) creates a section with a name different from
     *  what we requested, in "Section {number}" format.
     */
    public static XNamed create(XTextDocument doc, String name, XTextRange range, boolean absorb)
        throws
        IllegalArgumentException,
        CreationException {

        return UnoNamed.insertNamedTextContent(doc,
                                               "com.sun.star.text.TextSection",
                                               name,
                                               range,
                                               absorb);
    }
}
