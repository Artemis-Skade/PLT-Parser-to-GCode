package com.julrobotics.parser;

// Credits: https://github.com/ralfstx/minimal-json


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A handler for parser events. Instances of this class can be given to a {@link PltParser}. The
 * parser will then call the methods of the given handler while reading the input.
 * <p>
 * The default implementations of these methods do nothing. Subclasses may override only those
 * methods they are interested in. They can use <code>getLocation()</code> to access the current
 * character position of the parser at any point. The <code>start*</code> methods will be called
 * while the location points to the first character of the parsed element. The <code>end*</code>
 * methods will be called while the location points to the character position that directly follows
 * the last character of the parsed element.

 *
 * @see PltParser
 */
public class PltHandler {
    private final Log log = LogFactory.getLog(PltHandler.class);

    PltParser parser;

    /**
     * Returns the current parser location.
     *
     * @return the current parser location
     */
    protected Location getLocation() {
        return parser.getLocation();
    }

    /**
     * Indicates a pen down command. This method will be called
     * after reading the last character of the literal.
     */
    public void penDown() {
        log.info("pen down");
    }

    /**
     * Indicates a pen up command. This method will be called
     * after reading the last character of the literal.
     */
    public void penUp() {
        log.info("pen up");
    }

    /**
     * Indicates a select pen command. This method will be called
     * after reading the last character of the literal.
     */
    public void selectPen(int pen) {
        log.info("select pen " + pen);
    }

    /**
     * Indicates a plot absolute command. This method will be called
     * after reading the last character of the literal.
     */
    public void plotAbsolute(int x, int y) {
        log.info("plot absolute x: " + x + ", y: " + y);
    }

    /**
     * Indicates an initialize command. This method will be called
     * after reading the last character of the literal.
     */
    public void initialize() {
        log.info("init");
    }

    /**
     * Indicates a select pen command without a given pen. This method will be called
     * after reading the last character of the literal.
     * Typically at the end of the plt file
     */
    public void resetPen() {
    }
}