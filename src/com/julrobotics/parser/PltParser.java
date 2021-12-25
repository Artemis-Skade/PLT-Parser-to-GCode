package com.julrobotics.parser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;


// Uses in parts code from: https://github.com/ralfstx/minimal-json

/*
 * Parser for .plt files using AutoCADs version of the HPGL File format
 * Only extracts the most important commands used to recreate the vector image from the file
 * The extracted commands are passed to a handler, which can be defined separately
 */
public class PltParser {
    private final Log log = LogFactory.getLog(PltParser.class);

    private static final int MIN_BUFFER_SIZE = 10;
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private Reader reader;
    private final PltHandler handler;
    private char[] buffer;
    private int bufferOffset;
    private int index;
    private int fill;
    private int line;
    private int lineOffset;
    private int current;
    private StringBuilder captureBuffer;
    private int captureStart;

    public PltParser(PltHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler is null");
        }
        this.handler = handler;
        handler.parser = this;
    }

    /**
     Parse the input and forward the extracted commands to the handler
     @param string: Simple String
     **/
    public void parse(String string) {
        if (string == null) {
            throw new NullPointerException("string is null");
        }

        int bufferSize = Math.max(MIN_BUFFER_SIZE, Math.min(DEFAULT_BUFFER_SIZE, string.length()));

        try {
            parse(new StringReader(string), bufferSize);
        } catch (IOException exception) {
            // StringReader does not throw IOException
            throw new RuntimeException(exception);
        }
    }

    /**
     Parse the input and forward the extracted commands to the handler
     @param reader: Some stream that supports the Reader interface
     **/
    public void parse(Reader reader) throws IOException {
        parse(reader, DEFAULT_BUFFER_SIZE);
    }

    /**
     Parse the input and forward the extracted commands to the handler
     @param reader: Some stream that supports the Reader interface
     @param bufferSize: Size of the buffer used to store intermediate chunks of data
     **/
    public void parse(Reader reader, int bufferSize) throws IOException {
        if (reader == null) {
            throw new NullPointerException("reader is null");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize is zero or negativ");
        }

        this.reader = reader;
        buffer = new char[bufferSize];
        bufferOffset = 0;
        index = 0;
        fill = 0;
        current = 0;
        captureStart = -1;
        // skip the first part which holds some AutoCAD specific stuff until the "initialize opcode"
        skipToInitialize();

        // count the parsed commands
        int numberOfCommands = 0;

        while(!isEndOfText()){
            readCommand();
            numberOfCommands++;
            skipWhiteSpace();
        }

        log.info("Parsed " + numberOfCommands + " commands");
    }

    /**
     * Execute specific parsing functions depending on the opcode
     * @throws IOException
     */
    private void readCommand() throws IOException {
        Operation operation = readOperation();
        switch (operation) {
            case Initialize -> readOpOnly(Operation.Initialize);
            case PenDown -> readOpOnly(Operation.PenDown);
            case PenUp -> readOpOnly(Operation.PenUp);
            case SelectPen -> readOpWithOptIntArg(Operation.SelectPen);
            case PlotAbsolute -> readOpTwoIntArg(Operation.PlotAbsolute);
            default -> readUnsupportedCommand(operation);
        }
    }

    private void readUnsupportedCommand(Operation op) throws IOException{
        skipToSemicolon();
        log.warn("unsupported operation: " + op.toString());
    }

    private void readOpWithOptIntArg(Operation op) throws IOException {
        skipWhiteSpace();
        boolean hasArg = false;
        Optional<Integer> arg = Optional.empty();
        if(Character.isDigit(current)) {
            arg = Optional.of(readInt());
        }
        skipWhiteSpace();
        readRequiredChar(';');
        if (arg.isPresent()){
            switch (op) {
                case SelectPen -> handler.selectPen(arg.get());
                default -> throw expected("readOpIntArg called with unsupported operation");
            }
        } else {
            switch (op) {
                case SelectPen -> handler.resetPen();
                default -> throw expected("readOpIntArg called with unsupported operation");
            }

        }
    }

    private void readOpTwoIntArg(Operation op) throws IOException {
        skipWhiteSpace();
        int arg1 = readInt();
        skipWhiteSpace();
        readRequiredChar(',');
        skipWhiteSpace();
        int arg2 = readInt();
        skipWhiteSpace();
        readRequiredChar(';');
        switch (op) {
            case PlotAbsolute -> handler.plotAbsolute(arg1,arg2);
            default -> throw expected("readOpIntArg called with unsupported operation");
        }
    }

    private void readOpOnly(Operation op) throws IOException {
        readRequiredChar(';');
        switch (op) {
            case Initialize -> handler.initialize();
            case PenDown -> handler.penDown();
            case PenUp -> handler.penUp();
            default -> throw expected("readOpOnly called with unsupported operation");
        }
    }

    private void readRequiredChar(char ch) throws IOException {
        if (!readChar(ch)) {
            throw expected("'" + ch + "'");
        }
    }

    private Operation readOperation() throws IOException {
        startCapture();
        read();
        read();
        return Operation.fromString(endCapture());
    }

    private int readInt() throws IOException {
        startCapture();
        readChar('-');
        int firstDigit = current;
        if (!readDigit()) {
            log.error("tried to parse integer but no digit found");
            throw expected("could not parse Integer");
        }
        if (firstDigit != '0') {
            while (readDigit()) ;
        }
        String number = endCapture();
        return Integer.parseInt(number);
    }

    private boolean readChar(char ch) throws IOException {
        if (current != ch) {
            return false;
        }
        read();
        return true;
    }

    private boolean readDigit() throws IOException {
        if (!Character.isDigit(current)) {
            return false;
        }
        read();
        return true;
    }

    private void skipWhiteSpace() throws IOException {
        while (Character.isWhitespace(current)) {
            read();
        }
    }

    private void skipToSemicolon() throws IOException {
        while (current != ';') {
            read();
        }
        readRequiredChar(';');
    }

    private void skipToInitialize() throws IOException {
        char a = ' ';
        char b = ' ';
        while (a != 'I' || b != 'N'){
            a = b;
            b = (char) current;
            read();
        }
        readRequiredChar(';');
        handler.initialize();
    }

    private void read() throws IOException {
        if (index == fill) {
            if (captureStart != -1) {
                captureBuffer.append(buffer, captureStart, fill - captureStart);
                captureStart = 0;
            }
            bufferOffset += fill;
            fill = reader.read(buffer, 0, buffer.length);
            index = 0;
            if (fill == -1) {
                current = -1;
                index++;
                return;
            }
        }
        if (current == '\n') {
            line++;
            lineOffset = bufferOffset + index;
        }
        current = buffer[index++];
    }

    private void startCapture() {
        if (captureBuffer == null) {
            captureBuffer = new StringBuilder();
        }
        captureStart = index - 1;
    }

    private String endCapture() {
        int start = captureStart;
        int end = index - 1;
        captureStart = -1;
        if (captureBuffer.length() > 0) {
            captureBuffer.append(buffer, start, end - start);
            String captured = captureBuffer.toString();
            captureBuffer.setLength(0);
            return captured;
        }
        return new String(buffer, start, end - start);
    }

    Location getLocation() {
        int offset = bufferOffset + index - 1;
        int column = offset - lineOffset + 1;
        return new Location(offset, line, column);
    }

    private ParseException expected(String expected) {
        return error("Expected " + expected);
    }

    private ParseException error(String message) {
        return new ParseException(message, getLocation());
    }

    private boolean isEndOfText() {
        return current == -1;
    }
}
