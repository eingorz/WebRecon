package com.example.WebRecon.recon.step;

import android.util.Log;

import com.example.WebRecon.recon.model.SubdomainResult;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DnsStep {

    private static final String TAG = "DnsStep";
    private static final int POOL_SIZE = 10;

    public static List<SubdomainResult> resolve(List<String> subdomains) {
        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
        List<Future<SubdomainResult>> futures = new ArrayList<>();

        for (String sub : subdomains) {
            futures.add(pool.submit(() -> resolveOne(sub)));
        }

        List<SubdomainResult> results = new ArrayList<>();
        for (Future<SubdomainResult> f : futures) {
            try {
                results.add(f.get());
            } catch (Exception e) {
                Log.w(TAG, "DNS future error: " + e.getMessage());
            }
        }
        pool.shutdown();
        return results;
    }

    private static SubdomainResult resolveOne(String sub) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(sub);
            List<String> ips = new ArrayList<>();
            for (InetAddress addr : addresses) {
                ips.add(addr.getHostAddress());
            }
            return new SubdomainResult(sub, ips, true);
        } catch (UnknownHostException e) {
            return new SubdomainResult(sub, new ArrayList<>(), false);
        }
    }
}
