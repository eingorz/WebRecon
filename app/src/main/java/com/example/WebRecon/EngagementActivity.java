package com.example.WebRecon;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.WebRecon.adapter.FindingAdapter;
import com.example.WebRecon.databinding.ActivityEngagementBinding;
import com.example.WebRecon.db.AppDatabase;
import com.example.WebRecon.db.entity.Finding;
import com.example.WebRecon.recon.ReconEngine;
import com.example.WebRecon.recon.model.ReconProgress;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EngagementActivity extends AppCompatActivity {

    public static final String EXTRA_ENGAGEMENT_ID = "ENGAGEMENT_ID";
    public static final String EXTRA_DOMAIN = "DOMAIN";
    private static final String KEY_SCROLL = "scroll";

    private ActivityEngagementBinding binding;
    private FindingAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private long engagementId;
    private String domain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEngagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        engagementId = getIntent().getLongExtra(EXTRA_ENGAGEMENT_ID, -1L);
        domain = getIntent().getStringExtra(EXTRA_DOMAIN);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(domain != null ? domain : "Engagement");
        }

        adapter = new FindingAdapter();
        binding.rvFindings.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFindings.setAdapter(adapter);
        binding.rvFindings.addItemDecoration(
            new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        if (savedInstanceState != null) {
            int scroll = savedInstanceState.getInt(KEY_SCROLL, 0);
            binding.rvFindings.scrollToPosition(scroll);
        }

        // Determine mode: new scan or view existing
        executor.submit(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Finding> existing = db.findingDao().getForEngagementSync(engagementId);
            runOnUiThread(() -> {
                if (existing != null && !existing.isEmpty()) {
                    // View existing engagement
                    adapter.setFindings(existing);
                    binding.tvStatus.setText(getString(R.string.engagement_done));
                    binding.tvStatus.setVisibility(View.VISIBLE);
                } else {
                    // Start new scan
                    startRecon();
                }
            });
        });
    }

    private void startRecon() {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        binding.tvStatus.setVisibility(View.VISIBLE);
        binding.tvStatus.setText(getString(R.string.engagement_running));

        ReconEngine engine = new ReconEngine(this, domain, engagementId,
            new ReconEngine.ReconListener() {
                @Override
                public void onProgress(ReconProgress progress) {
                    binding.progressIndicator.setProgress(progress.percent);
                    binding.tvStatus.setText(progress.message);
                }

                @Override
                public void onFinding(Finding finding) {
                    adapter.addFinding(finding);
                    binding.rvFindings.smoothScrollToPosition(adapter.getItemCount() - 1);
                }

                @Override
                public void onComplete() {
                    binding.progressIndicator.setVisibility(View.GONE);
                    binding.tvStatus.setText(getString(R.string.engagement_done));
                    invalidateOptionsMenu();
                }

                @Override
                public void onError(String message) {
                    binding.progressIndicator.setVisibility(View.GONE);
                    binding.tvStatus.setText(getString(R.string.engagement_failed) + ": " + message);
                }
            });

        executor.submit(engine);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_engagement, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_share) {
            shareReport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareReport() {
        List<Finding> findings = adapter.getFindings();
        StringBuilder sb = new StringBuilder();
        sb.append("# WebRecon Report — ").append(domain).append("\n\n");
        for (Finding f : findings) {
            sb.append("## [").append(f.severity).append("] ")
                .append(f.title).append("\n");
            if (f.detail != null && !f.detail.isEmpty()) {
                sb.append(f.detail).append("\n");
            }
            sb.append("\n");
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "WebRecon Report — " + domain);
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LinearLayoutManager lm = (LinearLayoutManager) binding.rvFindings.getLayoutManager();
        if (lm != null) {
            outState.putInt(KEY_SCROLL, lm.findFirstVisibleItemPosition());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
