package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.container.XNamed;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.document.XRedlinesSupplier;
import com.sun.star.document.XUndoManager;
import com.sun.star.document.XUndoManagerSupplier;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.Locale;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.style.XStyle;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.text.ReferenceFieldPart;
import com.sun.star.text.ReferenceFieldSource;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XFootnote;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.Any;
import com.sun.star.uno.Type;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;
import com.sun.star.util.DateTime;
import com.sun.star.util.InvalidStateException;
import com.sun.star.util.XRefreshable;
import com.sun.star.view.XSelectionSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Document-connection related variables.
 */
public class DocumentConnection {
    /** https://wiki.openoffice.org/wiki/Documentation/BASIC_Guide/
     *  Structure_of_Text_Documents#Character_Properties
     *  "CharStyleName" is an OpenOffice Property name.
     */
    private static final String CHAR_STYLE_NAME = "CharStyleName";
    private static final String PARA_STYLE_NAME = "ParaStyleName";
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentConnection.class);


    public XTextDocument mxDoc;

    // unoQI(XComponent.class, mxDoc);
    public XComponent xCurrentComponent;

    // unoQI(XMultiServiceFactory.class, mxDoc);
    public XMultiServiceFactory mxDocFactory;

    // XModel mo = unoQI(XModel.class, this.xCurrentComponent);
    // XController co = mo.getCurrentController();

    public XText xText;

    // xViewCursorSupplier = unoQI(XTextViewCursorSupplier.class, co);
    public XTextViewCursorSupplier xViewCursorSupplier;
    public XPropertyContainer userProperties;
    public XPropertySet propertySet;

    public DocumentConnection(XTextDocument mxDoc) {
        //        printServiceInfo(mxDoc);
        //        *** xserviceinfo
        //    object       is OK
        //    xserviceinfo is OK
        //        .getImplementationName: "SwXTextDocument"
        //        .getSupportedServiceNames:
        //              "com.sun.star.document.OfficeDocument"
        //              "com.sun.star.text.GenericTextDocument"
        //              "com.sun.star.text.TextDocument"

        this.mxDoc = mxDoc;
        this.xCurrentComponent = unoQI(XComponent.class, mxDoc);
        this.mxDocFactory = unoQI(XMultiServiceFactory.class, mxDoc);
        // unoQI(XDocumentIndexesSupplier.class, component);

        // get a reference to the body text of the document
        this.xText = mxDoc.getText();

        XModel mo = unoQI(XModel.class, this.xCurrentComponent);
        XController co = mo.getCurrentController();
        this.xViewCursorSupplier = unoQI(XTextViewCursorSupplier.class, co);

        XDocumentPropertiesSupplier supp = unoQI(XDocumentPropertiesSupplier.class, mxDoc);
        this.userProperties = supp.getDocumentProperties().getUserDefinedProperties();
        this.propertySet = unoQI(XPropertySet.class, userProperties);
    }

    /**
     * unoQI : short for UnoRuntime.queryInterface
     *
     * @return A reference to the requested UNO interface type if available,
     *         otherwise null
     */
    private static <T> T unoQI(Class<T> zInterface,
                               Object object) {
        return UnoRuntime.queryInterface(zInterface, object);
    }

    private XDocumentProperties getXDocumentProperties() {
        XDocumentPropertiesSupplier supp = unoQI(XDocumentPropertiesSupplier.class, mxDoc);
        return supp.getDocumentProperties();
    }

    private short getRedlineDisplayType()
        throws
        UnknownPropertyException,
        WrappedTargetException {
        // https://www.openoffice.org/api/docs/common/ref/com/sun/star/document/RedlineDisplayType.html
        // Constants (short)
        // 0 NONE                  no changes are displayed.
        // 1 INSERTED              only inserted parts are displayed and attributed.
        // 2 INSERTED_AND_REMOVED  only inserted parts are displayed and attributed.
        // 3 REMOVED               only removed parts are displayed and attributed.
        XPropertySet ps = unoQI(XPropertySet.class, mxDoc);
        return (short) ps.getPropertyValue("RedlineDisplayType");
    }

    public boolean getRecordChanges()
        throws
        UnknownPropertyException,
        WrappedTargetException {
        XPropertySet ps = unoQI(XPropertySet.class, mxDoc);
        // https://wiki.openoffice.org/wiki/Documentation/DevGuide/Text/Settings
        // "Properties of com.sun.star.text.TextDocument"
        if (ps == null) {
            throw new RuntimeException("getRecordChanges: ps is null");
        }
        return (boolean) ps.getPropertyValue("RecordChanges");
    }

    public void setRecordChanges(boolean value)
        throws
        UnknownPropertyException,
        WrappedTargetException,
        PropertyVetoException {
        XPropertySet ps = unoQI(XPropertySet.class, mxDoc);
        ps.setPropertyValue("RecordChanges", value);
    }

    private XRedlinesSupplier getRedlinesSupplier() {
        return unoQI(XRedlinesSupplier.class, mxDoc);
    }

    public int countRedlines() {
        XRedlinesSupplier rs = getRedlinesSupplier();
        XEnumerationAccess ea = rs.getRedlines();
        XEnumeration e = ea.createEnumeration(); // null for empty
        if (e == null) {
            // System.out.println("countRedLines: no redlines found");
            return 0;
        } else {
            int count = 0;
            for (; e.hasMoreElements(); ) {
                try {
                    Object o = e.nextElement();
                    count++;
                } catch (NoSuchElementException | WrappedTargetException ex) {
                    break;
                }
            }
            // System.out.println(String.format("countRedLines: found %d redlines", count));
            return count;
        }
    }

    private void printRedlines() {
        // https://docs.libreoffice.org/sw/html/unoredline_8cxx_source.html#l00290
        // http://www.openoffice.org/api/docs/common/ref/com/sun/star/text/RedlinePortion.html
        XRedlinesSupplier rs = getRedlinesSupplier();
        XEnumerationAccess ea = rs.getRedlines();
        XEnumeration e = ea.createEnumeration(); // null for empty
        if (e == null) {
            System.out.println("printRedLines: no redlines found");
            return;
        } else {
            int count = 0;
            for (; e.hasMoreElements(); ) {
                try {
                    count++;
                    Object o = e.nextElement();
                    XPropertySet ps = unoQI(XPropertySet.class, o);
                    if (ps == null) {
                        String msg = String.format("printRedLines: %d XPropertySet is null", count);
                        System.out.println(msg);
                        continue;
                    }
                    XPropertySetInfo psi = ps.getPropertySetInfo();
                    if (psi == null) {
                        String msg = String.format("printRedLines: %d XPropertySetInfo is null", count);
                        System.out.println(msg);
                    } else {
                        String msg = String.format("printRedLines: %d XPropertySetInfo is OK", count);
                        System.out.println(msg);
                        int propertyCount = 0;
                        for (Property p : psi.getProperties()) {
                            propertyCount++;
                            String m = String.format("printRedLines: %d/%d '%s' %d %s",
                                                     count,
                                                     propertyCount,
                                                     p.Name,
                                                     p.Handle,
                                                     p.Type);
                            switch (p.Name) {
                            case "RedlineEnd":
                                // printRedLines: 1/1 'RedlineEnd' 0 Type[com.sun.star.uno.XInterface]
                                // UNO_NAME_REDLINE_END   "RedlineEnd"
                            case "RedlineStart":
                                // printRedLines: 1/12 'RedlineStart' 0 Type[com.sun.star.uno.XInterface]
                                // UNO_NAME_REDLINE_START   "RedlineStart"
                                XInterface xi = (XInterface) ps.getPropertyValue(p.Name);
                                // printServiceInfo(xi);
                                //*** xserviceinfo
                                //    object       is OK
                                //    xserviceinfo is OK
                                //        .getImplementationName: "SwXTextRange"
                                //        .getSupportedServiceNames:
                                //              "com.sun.star.text.TextRange"
                                //              "com.sun.star.style.CharacterProperties"
                                //              "com.sun.star.style.CharacterPropertiesAsian"
                                //              "com.sun.star.style.CharacterPropertiesComplex"
                                //              "com.sun.star.style.ParagraphProperties"
                                //              "com.sun.star.style.ParagraphPropertiesAsian"
                                //              "com.sun.star.style.ParagraphPropertiesComplex"
                                break;
                            case "RedlineAuthor":
                                // printRedLines: 1/2 'RedlineAuthor' 0 Type[string]
                                // UNO_NAME_REDLINE_AUTHOR   "RedlineAuthor"
                            case "RedlineIdentifier":
                                // printRedLines: 1/3 'RedlineIdentifier' 0 Type[string]
                                // UNO_NAME_REDLINE_IDENTIFIER   "RedlineIdentifier"
                            case "RedlineDescription":
                                // printRedLines: 1/5 'RedlineDescription' 0 Type[string]
                                // UNO_NAME_REDLINE_DESCRIPTION   "RedlineDescription"
                            case "RedlineType":
                                // printRedLines: 1/7 'RedlineType' 0 Type[string]
                                // UNO_NAME_REDLINE_TYPE   "RedlineType"
                            case "RedlineComment":
                                // printRedLines: 1/8 'RedlineComment' 0 Type[string]
                                // UNO_NAME_REDLINE_COMMENT   "RedlineComment"
                                m += String.format(" '%s'", (String) ps.getPropertyValue(p.Name));
                                break;
                            case "RedlineDateTime":
                                // printRedLines: 1/4 'RedlineDateTime' 0 Type[com.sun.star.util.DateTime]
                                // UNO_NAME_REDLINE_DATE_TIME   "RedlineDateTime"
                                // OO http://www.openoffice.org/api/docs/common/ref/com/sun/star/util/DateTime.html
                                // LO https://api.libreoffice.org/docs/idl/ref/structcom_1_1sun_1_1star_1_1util_1_1DateTime.html
                                DateTime dt = (DateTime) ps.getPropertyValue(p.Name);
                                String s1 = String.format("%04d-%02d-%02d %02d:%02d:%02d",
                                                         dt.Year, dt.Month, dt.Day,
                                                         dt.Hours, dt.Minutes, dt.Seconds
                                                         /* OO: dt.HundredthSeconds */
                                                         /* LO: dt.NanoSeconds (but zero anyway) */);
                                m += String.format(" '%s'", s1);
                                break;

                            case "IsInHeaderFooter":
                                // printRedLines: 1/9 'IsInHeaderFooter' 0 Type[boolean]
                                // UNO_NAME_IS_IN_HEADER_FOOTER   "IsInHeaderFooter"
                            case "MergeLastPara":
                                // printRedLines: 1/11 'MergeLastPara' 0 Type[boolean]
                                // UNO_NAME_MERGE_LAST_PARA   "MergeLastPara"
                                boolean b = (boolean) ps.getPropertyValue(p.Name);
                                m += String.format(" '%s'", b);
                                break;

                            case "RedlineText":
                                // printRedLines: 1/10 'RedlineText' 0 Type[com.sun.star.text.XText]
                                // UNO_NAME_REDLINE_TEXT   "RedlineText"

                                // RedlineText: provides access to the text of the
                                // redline. This interface is only
                                // provided if the change is not
                                // visible. The visibility depends on
                                // the redline display options that
                                // are set at the documents property
                                // set (RedlineDisplayType).

                                XText t = null;
                                try {
                                    t = (XText) ps.getPropertyValue(p.Name);
                                    String s2 = t.getString();
                                    m += String.format(" '%s'", s2);
                                } catch (java.lang.ClassCastException ex) {
                                    m += " NotAvailable";
                                }
                                break;
                                // printRedLines: 1/6 'RedlineSuccessorData' 0 Type[[]com.sun.star.beans.PropertyValue]
                                // UNO_NAME_REDLINE_SUCCESSOR_DATA   "RedlineSuccessorData"

                                // printRedLines: 1/13 'StartRedline' 22275 Type[[]com.sun.star.beans.PropertyValue]
                                // UNO_NAME_START_REDLINE   "StartRedline"
                                // "contains the properties of a redline at the start of the document."

                                // printRedLines: 1/14 'EndRedline' 22276 Type[[]com.sun.star.beans.PropertyValue]
                                // UNO_NAME_END_REDLINE   "EndRedline"
                            }
                            System.out.println(m);
                        }
                    }

                } catch (NoSuchElementException
                         | WrappedTargetException
                         | UnknownPropertyException ex) {
                    break;
                }
            }
            System.out.println(String.format("printRedLines: found %d redlines", count));
            return;
        }
    }

    private Optional<XStyle> getStyleFromFamily(String familyName, String styleName)
        throws
        NoSuchElementException,
        WrappedTargetException {

        XStyleFamiliesSupplier fss = unoQI(XStyleFamiliesSupplier.class, mxDoc);
        XNameAccess fs = unoQI(XNameAccess.class, fss.getStyleFamilies());
        XNameContainer xFamily = unoQI(XNameContainer.class, fs.getByName(familyName));

        try {
            Object s = xFamily.getByName(styleName);
            XStyle xs = (XStyle) unoQI(XStyle.class, s);
            return Optional.ofNullable(xs);
        } catch (NoSuchElementException ex) {
            return Optional.empty();
        }
    }

    private Optional<XStyle> getParagraphStyle(String styleName)
        throws
        NoSuchElementException,
        WrappedTargetException {
        return getStyleFromFamily("ParagraphStyles", styleName);
    }

    private Optional<XStyle> getCharacterStyle(String styleName)
        throws
        NoSuchElementException,
        WrappedTargetException {
        return getStyleFromFamily("CharacterStyles", styleName);
    }

    public Optional<String> getInternalNameOfParagraphStyle(String name)
        throws
        NoSuchElementException,
        WrappedTargetException {
        return (getParagraphStyle(name)
                .map(e -> e.getName()));
    }

    public Optional<String> getInternalNameOfCharacterStyle(String name)
        throws
        NoSuchElementException,
        WrappedTargetException {
        return (getCharacterStyle(name)
                .map(e -> e.getName()));
    }

    /**
     * @param o An uno object, hopefully implementing XServiceInfo
     */
    public static void printServiceInfo(Object o) {
        XServiceInfo xserviceinfo = unoQI(XServiceInfo.class, o);
        System.out.printf("*** xserviceinfo%n");
        System.out.printf("    object       is %s%n", o == null ? "null" : "OK");
        System.out.printf("    xserviceinfo is %s%n", xserviceinfo == null ? "null" : "OK");
        if (xserviceinfo != null) {
            System.out.printf("        .getImplementationName: \"%s\"%n",
                              xserviceinfo.getImplementationName());
            System.out.printf("        .getSupportedServiceNames:%n");
            for (String s : xserviceinfo.getSupportedServiceNames()) {
                System.out.printf("              \"%s\"%n", s);
            }
        }
    }

    public XModel getModel() {
        return unoQI(XModel.class, this.xCurrentComponent);
    }

    public XController getCurrentController() {
        return this.getModel().getCurrentController();
    }

    public XSelectionSupplier getSelectionSupplier() {
        return unoQI(XSelectionSupplier.class, this.getCurrentController());
    }

    /**
     * @return may be null, or some type supporting XServiceInfo
     *
     * Experiments using printServiceInfo with cursor in various
     * positions in the document:
     *
     * With cursor within the frame, in text:
     * *** xserviceinfo.getImplementationName: "SwXTextRanges"
     *      "com.sun.star.text.TextRanges"
     *
     * With cursor somewehe else in text:
     * *** xserviceinfo.getImplementationName: "SwXTextRanges"
     *      "com.sun.star.text.TextRanges"
     *
     * With cursor in comment (AKA annotation):
     * *** XSelectionSupplier is OK
     * *** Object initialSelection is null
     * *** xserviceinfo is null
     *
     * With frame selected:
     * *** xserviceinfo.getImplementationName: "SwXTextFrame"
     *     "com.sun.star.text.BaseFrame"
     *     "com.sun.star.text.TextContent"
     *     "com.sun.star.document.LinkTarget"
     *     "com.sun.star.text.TextFrame"
     *     "com.sun.star.text.Text"
     *
     * With cursor selecting an inserted image:
     * *** XSelectionSupplier is OK
     * *** Object initialSelection is OK
     * *** xserviceinfo is OK
     * *** xserviceinfo.getImplementationName: "SwXTextGraphicObject"
     *      "com.sun.star.text.BaseFrame"
     *      "com.sun.star.text.TextContent"
     *      "com.sun.star.document.LinkTarget"
     *      "com.sun.star.text.TextGraphicObject"
     *
     */
    private Object getSelectionAsObject() {
        XSelectionSupplier xss = this.getSelectionSupplier();
        return xss.getSelection();
    }

    /**
     * So far it seems the first thing we have to do
     * with a selection is to decide what do we have.
     *
     * One way to do that is accessing its XServiceInfo interface.
     *
     * Note: may return null.
     */
    public Optional<XServiceInfo> getSelectionAsServiceInfo() {
        Object o = getSelectionAsObject();
        if (o == null) {
            return Optional.empty();
        }
        XServiceInfo xserviceinfo = unoQI(XServiceInfo.class, o);
        if (xserviceinfo == null) {
            // I do not know if this is possible: make a note
            // if it is.
            LOGGER.warn("DocumentConnection.getSelectionAsObject:"
                        + " XServiceInfo is null when Object is not");
        }
        return Optional.ofNullable(xserviceinfo);
    }

    /**
     * Select the object represented by {@code newSelection} if it is
     * known and selectable in this {@code XSelectionSupplier} object.
     *
     * Presumably result from {@code XSelectionSupplier.getSelection()} is
     * usually OK. It also accepted
     * {@code XTextRange newSelection = documentConnection.xText.getStart();}
     *
     * @return Apparently always returns true.
     *
     */
    public boolean select(Object newSelection) {
        XSelectionSupplier xss = this.getSelectionSupplier();
        return xss.select(newSelection);
    }

    /**
     * Each call to enterUndoContext must be paired by a call to
     * leaveUndoContext, otherwise, the document's undo stack is
     * left in an inconsistent state.
     */
    public void enterUndoContext(String title) {
        XUndoManagerSupplier mxUndoManagerSupplier = unoQI(XUndoManagerSupplier.class, mxDoc);
        XUndoManager um = mxUndoManagerSupplier.getUndoManager();
        // https://www.openoffice.org/api/docs/common/ref/com/sun/star/document/XUndoManager.html
        um.enterUndoContext(title);
    }

    public void leaveUndoContext()
        throws
        InvalidStateException {
        XUndoManagerSupplier mxUndoManagerSupplier = unoQI(XUndoManagerSupplier.class, mxDoc);
        XUndoManager um = mxUndoManagerSupplier.getUndoManager();
        // https://www.openoffice.org/api/docs/common/ref/com/sun/star/document/XUndoManager.html
        um.leaveUndoContext();
    }

    /**
     * Disable screen refresh.
     *
     * Must be paired with unlockControllers()
     *
     * https://www.openoffice.org/api/docs/common/ref/com/sun/star/frame/XModel.html
     *
     * While there is at least one lock remaining, some
     * notifications for display updates are not broadcasted.
     */
    public void lockControllers() {
        XModel mo = unoQI(XModel.class, this.xCurrentComponent);
        mo.lockControllers();
    }

    public void unlockControllers() {
        XModel mo = unoQI(XModel.class, this.xCurrentComponent);
        mo.unlockControllers();
    }

    public boolean hasControllersLocked() {
        XModel mo = unoQI(XModel.class, this.xCurrentComponent);
        return mo.hasControllersLocked();
    }

    /**
     *  @return True if we cannot reach the current document.
     */
    public boolean documentConnectionMissing() {

        boolean missing = false;
        // These are set by DocumentConnection constructor.
        if (null == this.mxDoc
            || null == this.xCurrentComponent
            || null == this.mxDocFactory
            || null == this.xText
            || null == this.xViewCursorSupplier
            || null == this.userProperties
            || null == this.propertySet) {
            missing = true;
        }

        // Attempt to check document is really available
        if (!missing) {
            try {
                getReferenceMarks();
            } catch (NoDocumentException ex) {
                missing = true;
            }
        }

        if (missing) {
            // release it
            this.mxDoc = null;
            this.xCurrentComponent = null;
            this.mxDocFactory = null;
            this.xText = null;
            this.xViewCursorSupplier = null;
            this.userProperties = null;
            this.propertySet = null;
        }
        return missing;
    }

    public static Object getProperty(Object o, String property)
            throws UnknownPropertyException, WrappedTargetException {
        XPropertySet props = UnoRuntime.queryInterface(
                XPropertySet.class, o);
        return props.getPropertyValue(property);
    }

    /**
     *  @param doc The XTextDocument we want the title for. Null allowed.
     *  @return The title or Optional.empty()
     */
    public static Optional<String> getDocumentTitle(XTextDocument doc) {

        if (doc == null) {
            return Optional.empty();
        }

        try {
            XFrame frame = doc.getCurrentController().getFrame();
            Object frameTitleObj = getProperty(frame, "Title");
            String frameTitleString = String.valueOf(frameTitleObj);
            return Optional.of(frameTitleString);
        } catch (UnknownPropertyException | WrappedTargetException e) {
            LOGGER.warn("Could not get document title", e);
            return Optional.empty();
        }
    }

    /**
     *  Get the title of the connected document.
     */
    public Optional<String> getDocumentTitle() {
        return DocumentConnection.getDocumentTitle(this.mxDoc);
    }

    public List<String> getCustomPropertyNames() {
        assert (this.propertySet != null);

        XPropertySetInfo psi = this.propertySet.getPropertySetInfo();

        List<String> names = new ArrayList<>();
        for (Property p : psi.getProperties()) {
            names.add(p.Name);
        }
        return names;
    }

    /**
     * @param property Name of a custom document property in the
     *        current document.
     *
     * @return The value of the property or Optional.empty()
     *
     * These properties are used to store extra data about
     * individual citation. In particular, the `pageInfo` part.
     *
     */
    public Optional<String> getCustomProperty(String property)
        throws
        WrappedTargetException {

        assert (this.propertySet != null);

        XPropertySetInfo psi = this.propertySet.getPropertySetInfo();

        if (psi.hasPropertyByName(property)) {
            try {
                String v =
                    this.propertySet
                    .getPropertyValue(property)
                    .toString();
                return Optional.ofNullable(v);
            } catch (UnknownPropertyException ex) {
                throw new RuntimeException("getCustomProperty: caught UnknownPropertyException");
            }
        }
        return Optional.empty();
    }

    /**
     * @param property Name of a custom document property in the
     *        current document.
     *
     * @param value The value to be stored.
     */
    public void setCustomProperty(String property, String value)
        throws
        NotRemoveableException,
        PropertyExistException,
        IllegalTypeException,
        IllegalArgumentException {

        XPropertySetInfo psi = this.propertySet.getPropertySetInfo();

        if (psi.hasPropertyByName(property)) {
            this.removeCustomProperty(property);
        }

        if (value != null) {
            this.userProperties
                .addProperty(property,
                             com.sun.star.beans.PropertyAttribute.REMOVEABLE,
                             new Any(Type.STRING, value));
        }
    }

    /**
     * @param property Name of a custom document property in the
     *        current document.
     */
    public void removeCustomProperty(String property)
        throws
        NotRemoveableException,
        PropertyExistException,
        IllegalTypeException,
        IllegalArgumentException {

        XPropertySetInfo psi = this.propertySet.getPropertySetInfo();

        if (psi.hasPropertyByName(property)) {
            try {
                this.userProperties.removeProperty(property);
            } catch (UnknownPropertyException ex) {
                throw new RuntimeException("removeCustomProperty caught UnknownPropertyException"
                                           + " (should be impossible)");
            }
        }
    }

    /**
     * @throws NoDocumentException If cannot get reference marks
     *
     * Note: also used by `documentConnectionMissing` to test if
     * we have a working connection.
     *
     */
    public XNameAccess getReferenceMarks()
        throws
        NoDocumentException {

        XReferenceMarksSupplier supplier = unoQI(XReferenceMarksSupplier.class,
                                                 this.xCurrentComponent);
        try {
            return supplier.getReferenceMarks();
        } catch (DisposedException ex) {
            throw new NoDocumentException("getReferenceMarks failed with" + ex);
        }
    }

    /**
     * Provides access to bookmarks by name.
     */
    public XNameAccess getBookmarks() {

        XBookmarksSupplier supplier = unoQI(XBookmarksSupplier.class,
                                            this.xCurrentComponent);
        return supplier.getBookmarks();
    }

    /**
     *  @return An XNameAccess to find sections by name.
     */
    public XNameAccess getTextSections() {

        XTextSectionsSupplier supplier = unoQI(XTextSectionsSupplier.class,
                                               this.mxDoc);
        return supplier.getTextSections();
    }

    /**
     * Names of all reference marks.
     *
     * Empty list for nothing.
     */
    public List<String> getReferenceMarkNames()
        throws NoDocumentException {

        XNameAccess nameAccess = getReferenceMarks();
        String[] names = nameAccess.getElementNames();
        if (names == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(names);
    }

    /**
     *  Get the {@cod XTextContent} interface.
     *
     * @return null for null and for no-such-interface
     */
    public static XTextContent asTextContent(Object mark) {
        if (mark == null) {
            return null;
        }
        return unoQI(XTextContent.class, mark);
    }

    /**
     * @return null if name not found, or if the result does not
     *         support the XTextContent interface.
     */
    public static Optional<XTextContent> nameAccessGetTextContentByName(XNameAccess nameAccess,
                                                                        String name)
        throws
        WrappedTargetException {

        if (!nameAccess.hasByName(name)) {
            return Optional.empty();
        }

        try {
            Object referenceMark = nameAccess.getByName(name);
            return Optional.ofNullable(asTextContent(referenceMark));
        } catch (NoSuchElementException ex) {
            String msg = String.format("nameAccessGetTextContentByName got NoSuchElementException"
                                       + " for '%s'",
                                       name);
            LOGGER.warn(msg);
            return Optional.empty();
        }
    }

    /**
     * Create a text cursor for a textContent.
     *
     * @return null if mark is null, otherwise cursor.
     *
     */
    public static XTextCursor getTextCursorOfTextContent(XTextContent mark) {
        if (mark == null) {
            return null;
        }
        XTextRange markAnchor = mark.getAnchor();
        return (markAnchor.getText()
                .createTextCursorByRange(markAnchor));
    }

    /**
     * Remove the named reference mark.
     *
     * Removes both the text and the mark itself.
     */
    public void removeReferenceMark(String name)
        throws
        WrappedTargetException,
        NoDocumentException,
        NoSuchElementException {

        XNameAccess xReferenceMarks = this.getReferenceMarks();

        if (xReferenceMarks.hasByName(name)) {
            Optional<XTextContent> mark = nameAccessGetTextContentByName(xReferenceMarks, name);
            if (mark.isEmpty()) {
                return;
            }
            this.xText.removeTextContent(mark.get());
        }
    }

    /**
     * Get the cursor positioned by the user.
     *
     */
    public XTextViewCursor getViewCursor() {
        return this.xViewCursorSupplier.getViewCursor();
    }

    /**
     * Get the XTextRange corresponding to the named bookmark.
     *
     * @param name The name of the bookmark to find.
     * @return The XTextRange for the bookmark, or null.
     */
    public Optional<XTextRange> getBookmarkRange(String name)
        throws
        WrappedTargetException {

        XNameAccess nameAccess = this.getBookmarks();
        return (nameAccessGetTextContentByName(nameAccess, name)
                .map(e -> e.getAnchor()));
    }

    /**
     *  @return reference mark as XTextContent, Optional.empty if not found.
     */
    public Optional<XTextContent> getReferenceMarkAsTextContent(String name)
        throws
        NoDocumentException,
        WrappedTargetException {

        XNameAccess nameAccess = this.getReferenceMarks();
        return nameAccessGetTextContentByName(nameAccess, name);
    }

    /**
     *  XTextRange for the named reference mark, Optional.empty if not found.
     */
    public Optional<XTextRange> getReferenceMarkRange(String name)
        throws
        NoDocumentException,
        WrappedTargetException {

        return (getReferenceMarkAsTextContent(name)
                .map(e -> e.getAnchor()));
    }

    /**
     * Insert a new instance of a service at the provided cursor
     * position.
     *
     * @param service For example
     *                 "com.sun.star.text.ReferenceMark",
     *                 "com.sun.star.text.Bookmark" or
     *                 "com.sun.star.text.TextSection".
     *
     *                 Passed to this.mxDocFactory.createInstance(service)
     *                 The result is expected to support the
     *                 XNamed and XTextContent interfaces.
     *
     * @param name     For the ReferenceMark, Bookmark, TextSection.
     *                 If the name is already in use, LibreOffice
     *                 may change the name.
     *
     * @param range   Marks the location or range for
     *                the thing to be inserted.
     *
     * @param absorb ReferenceMark, Bookmark and TextSection can
     *               incorporate a text range. If absorb is true,
     *               the text in the range becomes part of the thing.
     *               If absorb is false,  the thing is
     *               inserted at the end of the range.
     *
     * @return The XNamed interface, in case we need to check the actual name.
     *
     */
    private XNamed insertNamedTextContent(String service,
                                          String name,
                                          XTextRange range,
                                          boolean absorb)
        throws
        CreationException {

        Object xObject;
        try {
            xObject = this.mxDocFactory.createInstance(service);
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }

        XNamed xNamed = unoQI(XNamed.class, xObject);
        xNamed.setName(name);

        // get XTextContent interface
        XTextContent xTextContent = unoQI(XTextContent.class, xObject);
        range.getText().insertTextContent(range, xTextContent, absorb);
        return xNamed;
    }

    /**
     * Insert a new reference mark at the provided cursor
     * position.
     *
     * The text in the cursor range will be the text with gray
     * background.
     *
     * Note: LibreOffice 6.4.6.2 will create multiple reference marks
     *       with the same name without error or renaming.
     *       Its GUI does not allow this,
     *       but we can create them programmatically.
     *       In the GUI, clicking on any of those identical names
     *       will move the cursor to the same mark.
     *
     * @param name     For the reference mark.
     * @param range    Cursor marking the location or range for
     *                 the reference mark.
     */
    public XNamed insertReferenceMark(String name,
                                      XTextRange range,
                                      boolean absorb)
        throws
        CreationException {

        return insertNamedTextContent("com.sun.star.text.ReferenceMark",
                                      name,
                                      range,
                                      absorb);
    }

    /**
     * Insert a bookmark with the given name at the cursor provided,
     * or with another name if the one we asked for is already in use.
     *
     * In LibreOffice the another name is in "{name}{number}" format.
     *
     * @param name     For the bookmark.
     * @param range    Cursor marking the location or range for
     *                 the bookmark.
     * @param absorb   Shall we incorporate range?
     *
     * @return The XNamed interface of the bookmark.
     *
     *         result.getName() should be checked by the
     *         caller, because its name may differ from the one
     *         requested.
     */
    public XNamed insertBookmark(String name,
                                 XTextRange range,
                                 boolean absorb)
        throws
        IllegalArgumentException,
        CreationException {

        return insertNamedTextContent("com.sun.star.text.Bookmark",
                                      name,
                                      range,
                                      absorb);
    }

    /**
     * Insert a clickable cross-reference to a reference mark,
     * with a label containing the target's page number.
     *
     * May need a documentConnection.refresh() after, to update
     * the text shown.
     */
    public void insertReferenceToPageNumberOfReferenceMark(String referenceMarkName,
                                                           XTextRange cursor)
        throws
        CreationException,
        UnknownPropertyException,
        PropertyVetoException,
        WrappedTargetException {

        DocumentConnection documentConnection = this;
        // based on: https://wiki.openoffice.org/wiki/Documentation/DevGuide/Text/Reference_Marks

        // Create a 'GetReference' text field to refer to the reference mark we just inserted,
        // and get it's XPropertySet interface
        XPropertySet xFieldProps;
        try {
            String name = "com.sun.star.text.textfield.GetReference";
            xFieldProps = (XPropertySet) unoQI(XPropertySet.class,
                                               this.mxDocFactory.createInstance(name));
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }

        // Set the SourceName of the GetReference text field to the referenceMarkName
        xFieldProps.setPropertyValue("SourceName", referenceMarkName);

        // specify that the source is a reference mark (could also be a footnote,
        // bookmark or sequence field)
        xFieldProps.setPropertyValue("ReferenceFieldSource",
                                     new Short(ReferenceFieldSource.REFERENCE_MARK));

        // We want the reference displayed as page number
        xFieldProps.setPropertyValue("ReferenceFieldPart",
                                     new Short(ReferenceFieldPart.PAGE));

        // Get the XTextContent interface of the GetReference text field
        XTextContent xRefContent = (XTextContent) unoQI(XTextContent.class, xFieldProps);

        // Insert the text field
        this.xText.insertTextContent(cursor.getEnd(), xRefContent, false);
    }

    /**
     * Update TextFields, etc.
     */
    public void refresh() {
        // Refresh the document
        XRefreshable xRefresh = unoQI(XRefreshable.class, this.mxDoc);
        xRefresh.refresh();
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
    public XNamed insertTextSection(String name,
                                    XTextRange range,
                                    boolean absorb)
        throws
        IllegalArgumentException,
        CreationException {

        return insertNamedTextContent("com.sun.star.text.TextSection",
                                      name,
                                      range,
                                      absorb);
    }

    /**
     *  Get an XTextSection by name.
     */
    public Optional<XTextSection> getTextSectionByName(String name)
        throws
        NoSuchElementException,
        WrappedTargetException {

        XTextSectionsSupplier supplier = unoQI(XTextSectionsSupplier.class,
                                               this.mxDoc);

        return Optional.ofNullable((XTextSection)
                                   ((Any) supplier.getTextSections().getByName(name))
                                   .getObject());
    }

    /**
     *  If original is in a footnote, return a range containing
     *  the corresponding footnote marker.
     *
     *  Returns Optional.empty if not in a footnote.
     */
    public static Optional<XTextRange> getFootnoteMarkRange(XTextRange original) {
        XFootnote footer = unoQI(XFootnote.class, original.getText());
        if (footer != null) {
            // If we are inside a footnote,
            // find the linking footnote marker:
            // The footnote's anchor gives the correct position in the text:
            return Optional.ofNullable(footer.getAnchor());
        }
        return Optional.empty();
    }

    /**
     *  Apply a character style to a range of text selected by a
     *  cursor.
     *
     * @param position  The range to apply to.
     * @param charStyle Name of the character style as known by Openoffice.
     */
    public static void setCharStyle(XTextCursor position,
                                    String charStyle)
        throws
        UndefinedCharacterFormatException {

        XPropertySet xCursorProps = unoQI(XPropertySet.class, position);

        try {
            xCursorProps.setPropertyValue(CHAR_STYLE_NAME, charStyle);
        } catch (UnknownPropertyException
                 | PropertyVetoException
                 | IllegalArgumentException
                 | WrappedTargetException ex) {
            // Setting the character format failed, so we throw an exception that
            // will result in an error message for the user:
            throw new UndefinedCharacterFormatException(charStyle);
        }
    }

    public static String getCharStyle(XTextCursor position)
        throws
        UnknownPropertyException,
        WrappedTargetException {

        XPropertySet xCursorProps = unoQI(XPropertySet.class, position);
        return (String) xCursorProps.getPropertyValue(CHAR_STYLE_NAME);
    }

    public static void setParagraphStyle(XTextCursor cursor,
                                         String parStyle)
        throws
        UndefinedParagraphFormatException {
        XParagraphCursor parCursor = unoQI(XParagraphCursor.class, cursor);

        // Access the property set of the cursor, and set the currently selected text
        // (which is the string we just inserted) to be bold
        XPropertySet props = unoQI(XPropertySet.class, parCursor);
        try {
            props.setPropertyValue(PARA_STYLE_NAME, parStyle);
        } catch (UnknownPropertyException
                 | PropertyVetoException
                 | IllegalArgumentException
                 | WrappedTargetException ex) {
            throw new UndefinedParagraphFormatException(parStyle);
        }
    }

    /**
     * Apply direct character format "Italic" to a range of text.
     *
     * Ref: https://www.openoffice.org/api/docs/common/ref/com/sun/star/style/CharacterProperties.html
     */
    public static void setCharFormatItalic(XTextRange textRange)
        throws
        UnknownPropertyException,
        PropertyVetoException,
        WrappedTargetException {
        XPropertySet xcp = unoQI(XPropertySet.class, textRange);
        xcp.setPropertyValue("CharPosture", com.sun.star.awt.FontSlant.ITALIC);
    }

    /**
     * Apply direct character format "Bold" to a range of text.
     */
    public static void setCharFormatBold(XTextRange textRange)
        throws
        UnknownPropertyException,
        PropertyVetoException,
        WrappedTargetException {
        XPropertySet xcp = unoQI(XPropertySet.class, textRange);
        xcp.setPropertyValue("CharWeight", com.sun.star.awt.FontWeight.BOLD);
    }

    /**
     *  Set language to [None]
     *
     *  Note: "zxx" is an https://en.wikipedia.org/wiki/ISO_639 code for
     *        "No linguistic information at all"
     */
    public static void setCharLocaleNone(XTextRange textRange)
        throws
        UnknownPropertyException,
        PropertyVetoException,
        WrappedTargetException {
        XPropertySet xcp = unoQI(XPropertySet.class, textRange);
        xcp.setPropertyValue("CharLocale", new Locale("zxx", "", ""));
    }

    public static Locale getCharLocale(XTextRange textRange)
        throws
        UnknownPropertyException,
        WrappedTargetException {
        XPropertySet xcp = unoQI(XPropertySet.class, textRange);
        return (Locale) xcp.getPropertyValue("CharLocale");
    }

    /**
     * Test if two XTextRange values are comparable (i.e. they share
     * the same getText()).
     */
    public static boolean comparableRanges(XTextRange a,
                                           XTextRange b) {
        return a.getText() == b.getText();
    }

    /**
     * Test if two XTextRange values are equal.
     */
    public static boolean equalRanges(XTextRange a,
                                      XTextRange b) {
        if (!comparableRanges(a, b)) {
            return false;
        }
        final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
                                                a.getText());
        if (compare.compareRegionStarts(a, b) != 0) {
            return false;
        }
        if (compare.compareRegionEnds(a, b) != 0) {
            return false;
        }
        return true;
    }

    /**
     * @return follows OO conventions, the opposite of java conventions:
     *  1 if (a &lt; b), 0 if same start, (-1) if (b &lt; a)
     */
    private static int ooCompareRegionStarts(XTextRange a,
                                             XTextRange b) {
        if (!comparableRanges(a, b)) {
            throw new RuntimeException("ooCompareRegionStarts: got incomparable regions");
        }
        final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
                                                a.getText());
        return compare.compareRegionStarts(a, b);
    }

    /**
     * @return follows OO conventions, the opposite of java conventions:
     *  1 if  (a &lt; b), 0 if same start, (-1) if (b &lt; a)
     */
    private static int ooCompareRegionEnds(XTextRange a,
                                           XTextRange b) {
        if (!comparableRanges(a, b)) {
            throw new RuntimeException("ooCompareRegionEnds: got incomparable regions");
        }
        final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
                                                a.getText());
        return compare.compareRegionEnds(a, b);
    }

    /**
     * @return follows java conventions
     *
     * 1 if  (a &gt; b); (-1) if (a &lt; b)
     */
    public static int javaCompareRegionStarts(XTextRange a,
                                              XTextRange b) {
        return (-1) * ooCompareRegionStarts(a, b);
    }

    /**
     * @return follows java conventions
     *
     * 1 if  (a &gt; b); (-1) if (a &lt; b)
     */
    public static int javaCompareRegionEnds(XTextRange a,
                                            XTextRange b) {
        return (-1) * ooCompareRegionEnds(a, b);
    }
} // end DocumentConnection
