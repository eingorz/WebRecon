package com.example.WebRecon.recon.model;

import java.util.List;

public class SubdomainResult {
    public String domain;
    public List<String> ips;
    public boolean alive;

    public SubdomainResult(String domain, List<String> ips, boolean alive) {
        this.domain = domain;
        this.ips = ips;
        this.alive = alive;
    }
}
