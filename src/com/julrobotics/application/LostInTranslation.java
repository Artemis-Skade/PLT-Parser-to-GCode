package com.julrobotics.application;

import com.julrobotics.parser.PltHandler;
import com.julrobotics.parser.PltParser;
import org.apache.commons.cli.*;
import org.apache.log4j.*;

import java.io.FileInputStream;
import java.io.InputStreamReader;

public class LostInTranslation {
    private static Logger logger = Logger.getRootLogger();

    public static void main(String[] args) {
        Options options = new Options();

        Option input = new Option("i", "input", true, "input file path");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output", true, "output file");
        output.setRequired(false);
        output.setOptionalArg(true);
        options.addOption(output);

        Option logging = new Option("d","debug",false,"enable debugging output");
        logging.setOptionalArg(true);
        options.addOption(logging);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options,args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("lostInTranslation", options);

            System.exit(1);
        }

        String inputFilePath = cmd.getOptionValue("input");
        String outputFilePath = cmd.getOptionValue("output","out.gcode");
        boolean loggingEnabled = cmd.hasOption("debug");


        if (loggingEnabled){
            logger.setLevel(Level.INFO);
        } else {
            logger.setLevel(Level.WARN);
        }

        logger.log(Level.INFO,"input file: " + inputFilePath + ", output file: " + outputFilePath);


        PltParser pltParser = new PltParser(new PltHandler());
        try {
            pltParser.parse(new InputStreamReader(new FileInputStream(inputFilePath)));
        } catch (Exception ex){
            logger.log(Level.ERROR, ex.getMessage());
        }
    }
}
