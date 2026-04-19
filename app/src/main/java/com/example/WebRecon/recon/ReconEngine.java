package com.example.WebRecon.recon;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.WebRecon.db.AppDatabase;
import com.example.WebRecon.db.EngagementStatus;
import com.example.WebRecon.db.FindingType;
import com.example.WebRecon.db.Severity;
import com.example.WebRecon.db.entity.Engagement;
import com.example.WebRecon.db.entity.Finding;
import com.example.WebRecon.recon.model.ReconProgress;
import com.example.WebRecon.recon.model.SubdomainResult;
import com.example.WebRecon.recon.step.CrtShStep;
import com.example.WebRecon.recon.step.DnsStep;
import com.example.WebRecon.recon.step.HeadersAuditor;
import com.example.WebRecon.recon.step.RobotsTxtStep;
import com.example.WebRecon.recon.step.PortScanStep;
import com.example.WebRecon.recon.step.SensitivePathsStep;
import com.example.WebRecon.recon.step.TechFingerprintStep;
import com.example.WebRecon.util.WordlistManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReconEngine implements Runnable {

    public interface ReconListener {
        void onProgress(ReconProgress progress);
        void onFinding(Finding finding);
        void onComplete();
        void onError(String message);
    }

    private static final String TAG = "ReconEngine";

    private final Context context;
    private final String domain;
    private final long engagementId;
    private final ReconListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AppDatabase db;

    public ReconEngine(Context context, String domain, long engagementId, ReconListener listener) {
        this.context = context.getApplicationContext();
        this.domain = domain;
        this.engagementId = engagementId;
        this.listener = listener;
        this.db = AppDatabase.getInstance(context);
    }

    @Override
    public void run() {
        try {
            // Stage 1: crt.sh
            postProgress(new ReconProgress(ReconProgress.Stage.CRTSH,
                "Enumerating subdomains via crt.sh…", 5));
            List<String> crtSubdomains = CrtShStep.enumerate(domain);

            // Merge with common wordlist subdomains
            List<String> wordlistSubs = WordlistManager.readLines(context, WordlistManager.FILE_SUBDOMAINS);
            List<String> allSubdomains = new ArrayList<>(crtSubdomains);
            for (String sub : wordlistSubs) {
                String full = sub + "." + domain;
                if (!allSubdomains.contains(full)) allSubdomains.add(full);
            }
            // Include apex
            if (!allSubdomains.contains(domain)) allSubdomains.add(0, domain);

            String crtMsg = crtSubdomains.isEmpty()
                ? "crt.sh returned no results (API may be unavailable)"
                : "crt.sh found " + crtSubdomains.size() + " certificate entries";
            Finding crtFinding = new Finding(engagementId, FindingType.SUBDOMAIN, Severity.INFO,
                crtMsg, "Total unique subdomains to probe: " + allSubdomains.size());
            insertFinding(crtFinding);

            // Stage 2: DNS
            postProgress(new ReconProgress(ReconProgress.Stage.DNS,
                "Resolving " + allSubdomains.size() + " subdomains…", 20));
            List<SubdomainResult> dnsResults = DnsStep.resolve(allSubdomains);

            int alive = 0;
            for (SubdomainResult r : dnsResults) {
                if (r.alive) {
                    alive++;
                    Finding f = new Finding(engagementId, FindingType.DNS, Severity.INFO,
                        r.domain + " → alive",
                        "IPs: " + String.join(", ", r.ips));
                    insertFinding(f);
                }
            }
            Finding dnsFinding = new Finding(engagementId, FindingType.DNS, Severity.INFO,
                alive + " / " + allSubdomains.size() + " subdomains alive",
                "DNS resolution complete");
            insertFinding(dnsFinding);

            // Stage 3: Tech fingerprint
            postProgress(new ReconProgress(ReconProgress.Stage.TECH,
                "Fingerprinting " + domain + "…", 40));
            TechFingerprintStep.TechFingerprint tech = TechFingerprintStep.fingerprint(domain);
            if (!tech.techs.isEmpty()) {
                Finding f = new Finding(engagementId, FindingType.TECH, Severity.INFO,
                    "Detected: " + String.join(", ", tech.techs),
                    "Server: " + tech.server + "\nX-Powered-By: " + tech.poweredBy);
                insertFinding(f);
            } else {
                Finding f = new Finding(engagementId, FindingType.TECH, Severity.INFO,
                    "No technology fingerprint detected", "Could not identify stack on " + domain);
                insertFinding(f);
            }

            // Stage 4: Security headers
            postProgress(new ReconProgress(ReconProgress.Stage.HEADERS,
                "Auditing security headers…", 55));
            checkSecurityHeaders(domain);

            // Stage 5: robots.txt
            postProgress(new ReconProgress(ReconProgress.Stage.ROBOTS,
                "Checking robots.txt / sitemap.xml…", 70));
            List<Finding> robotsFindings = RobotsTxtStep.check(domain, engagementId);
            for (Finding f : robotsFindings) insertFinding(f);
            if (robotsFindings.isEmpty()) {
                Finding f = new Finding(engagementId, FindingType.ROBOTS, Severity.INFO,
                    "robots.txt not found or not accessible", "");
                insertFinding(f);
            }

            // Stage 6: Sensitive paths
            postProgress(new ReconProgress(ReconProgress.Stage.PATHS,
                "Probing sensitive paths…", 83));
            List<Finding> pathFindings = SensitivePathsStep.check(context, domain, engagementId);
            for (Finding f : pathFindings) insertFinding(f);
            if (pathFindings.isEmpty()) {
                Finding f = new Finding(engagementId, FindingType.PATH, Severity.INFO,
                    "No sensitive paths found", "All probed paths returned non-flagged status codes");
                insertFinding(f);
            }

            // Stage 7: Port scan
            postProgress(new ReconProgress(ReconProgress.Stage.PORT_SCAN,
                "Scanning common ports…", 92));
            List<Finding> portFindings = PortScanStep.scan(domain, engagementId);
            for (Finding f : portFindings) insertFinding(f);
            if (portFindings.isEmpty()) {
                Finding f = new Finding(engagementId, FindingType.PORT, Severity.INFO,
                    "No common ports open", "All probed ports were closed or filtered");
                insertFinding(f);
            }

            // Done
            updateEngagementStatus(EngagementStatus.COMPLETED);
            postProgress(new ReconProgress(ReconProgress.Stage.DONE, "Scan complete", 100));
            mainHandler.post(listener::onComplete);

        } catch (Exception e) {
            Log.e(TAG, "Recon error", e);
            updateEngagementStatus(EngagementStatus.FAILED);
            postProgress(new ReconProgress(ReconProgress.Stage.ERROR,
                "Error: " + e.getMessage(), 0));
            mainHandler.post(() -> listener.onError(e.getMessage()));
        }
    }

    private void checkSecurityHeaders(String domain) {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        try {
            Response resp = client.newCall(
                new Request.Builder().url("https://" + domain).build()
            ).execute();
            HeadersAuditor.HeaderGrade grade = HeadersAuditor.audit(resp.headers());
            resp.close();

            Severity sev;
            switch (grade.grade) {
                case "A": case "B": sev = Severity.INFO; break;
                case "C": case "D": sev = Severity.WARN; break;
                default: sev = Severity.CRIT; break;
            }

            Finding f = new Finding(engagementId, FindingType.HEADER, sev,
                "Security headers grade: " + grade.grade,
                HeadersAuditor.formatGradeReport(grade));
            insertFinding(f);

        } catch (IOException e) {
            Log.w(TAG, "Headers check failed: " + e.getMessage());
            Finding f = new Finding(engagementId, FindingType.HEADER, Severity.WARN,
                "Could not audit security headers",
                "Failed to connect: " + e.getMessage());
            insertFinding(f);
        }
    }

    private void insertFinding(Finding f) {
        db.findingDao().insert(f);
        mainHandler.post(() -> listener.onFinding(f));
    }

    private void updateEngagementStatus(EngagementStatus status) {
        Engagement e = db.engagementDao().getByIdSync(engagementId);
        if (e != null) {
            e.status = status;
            e.completedAt = System.currentTimeMillis();
            db.engagementDao().update(e);
        }
    }

    private void postProgress(ReconProgress progress) {
        mainHandler.post(() -> listener.onProgress(progress));
    }
}
