package org.jabref.logic.openoffice;

/**
 * This exception is used to indicate that connection to OpenOffice has been lost.
 */
public class ConnectionLostException extends RuntimeException {

    public ConnectionLostException(String s) {
        super(s);
    }
}