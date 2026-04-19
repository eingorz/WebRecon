package com.example.WebRecon.recon.model;

public class ReconProgress {

    public enum Stage {
        CRTSH, DNS, TECH, HEADERS, ROBOTS, PATHS, DONE, ERROR
    }

    public Stage stage;
    public String message;
    public int percent;

    public ReconProgress(Stage stage, String message, int percent) {
        this.stage = stage;
        this.message = message;
        this.percent = percent;
    }
}
