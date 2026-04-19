package com.example.WebRecon.recon.step;

import com.example.WebRecon.db.FindingType;
import com.example.WebRecon.db.Severity;
import com.example.WebRecon.db.entity.Finding;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PortScanStep {

    private static final int POOL_SIZE = 20;
    private static final int CONNECT_TIMEOUT_MS = 2000;

    private static final Map<Integer, String> PORT_NAMES = new HashMap<>();
    private static final Map<Integer, Severity> PORT_SEVERITIES = new HashMap<>();

    static {
        // Standard web — INFO
        int[] webPorts = {80, 443, 8080, 8443, 8000, 8888};
        String[] webNames = {"HTTP", "HTTPS", "HTTP-Alt", "HTTPS-Alt", "HTTP-Alt", "HTTP-Alt"};
        for (int i = 0; i < webPorts.length; i++) {
            PORT_NAMES.put(webPorts[i], webNames[i]);
            PORT_SEVERITIES.put(webPorts[i], Severity.INFO);
        }

        // Dev / alt web — WARN
        int[] devPorts = {3000, 4000, 5000, 8090, 9000, 4443};
        String[] devNames = {"Node/Rails", "Dev", "Flask/Dev", "HTTP-Alt", "Dev", "HTTPS-Alt"};
        for (int i = 0; i < devPorts.length; i++) {
            PORT_NAMES.put(devPorts[i], devNames[i]);
            PORT_SEVERITIES.put(devPorts[i], Severity.WARN);
        }

        // Databases / infrastructure — CRIT
        int[] critPorts = {21, 22, 25, 3306, 5432, 27017, 6379, 9200, 9300, 5601};
        String[] critNames = {"FTP", "SSH", "SMTP", "MySQL", "PostgreSQL", "MongoDB", "Redis",
                "Elasticsearch", "Elasticsearch", "Kibana"};
        for (int i = 0; i < critPorts.length; i++) {
            PORT_NAMES.put(critPorts[i], critNames[i]);
            PORT_SEVERITIES.put(critPorts[i], Severity.CRIT);
        }
    }

    private static int extractPort(String title) {
        // title format: "Open port: 443 (HTTPS)"
        try {
            int colon = title.indexOf(':');
            int paren = title.indexOf('(');
            return Integer.parseInt(title.substring(colon + 1, paren).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public static List<Finding> scan(String domain, long engagementId) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName(domain);
        } catch (UnknownHostException e) {
            return Collections.emptyList();
        }

        List<Integer> ports = new ArrayList<>(PORT_NAMES.keySet());
        List<Finding> findings = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
        CountDownLatch latch = new CountDownLatch(ports.size());
        Object lock = new Object();

        for (int port : ports) {
            final int finalPort = port;
            pool.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(addr, finalPort), CONNECT_TIMEOUT_MS);
                    String name = PORT_NAMES.get(finalPort);
                    Severity sev = PORT_SEVERITIES.get(finalPort);
                    Finding f = new Finding(engagementId, FindingType.PORT, sev,
                        "Open port: " + finalPort + " (" + name + ")",
                        domain + ":" + finalPort + " is reachable");
                    synchronized (lock) {
                        findings.add(f);
                    }
                } catch (IOException ignored) {
                    // Port closed or filtered
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.shutdown();
        findings.sort((a, b) -> {
            int pa = extractPort(a.title);
            int pb = extractPort(b.title);
            return Integer.compare(pa, pb);
        });
        return findings;
    }
}
