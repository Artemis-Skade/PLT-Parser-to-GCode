package com.julrobotics.parser;

/*
List of important HPGL Operations/Commands used for this purpose
 */

public enum Operation {
    PlotAbsolute("PA"),
    PlotRelative("PR"),
    PenDown("PD"),
    PenUp("PU"),
    Initialize("IN"),
    SelectPen("SP"),
    LineType("LT"),
    Scale("SC"),
    Input("IP"),
    Page("PG"),
    Unknown("**");

    private final String op;

    Operation(String op) {
        this.op = op;
    }

    static Operation fromString(String string){
        return switch (string) {
            case "PA" -> PlotAbsolute;
            case "PR" -> PlotRelative;
            case "PD" -> PenDown;
            case "PU" -> PenUp;
            case "IN" -> Initialize;
            case "SP" -> SelectPen;
            case "LT" -> LineType;
            case "SC" -> Scale;
            case "IP" -> Input;
            case "PG" -> Page;
            default -> Unknown;
        };
    }

    @Override
    public String toString(){
        return this.op;
    }
}
