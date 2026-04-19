package com.example.WebRecon.fragment.tool;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.WebRecon.R;
import com.example.WebRecon.databinding.FragmentHttpInspectorBinding;
import com.example.WebRecon.db.AppDatabase;
import com.example.WebRecon.db.ToolType;
import com.example.WebRecon.db.entity.ToolOperation;
import com.example.WebRecon.net.HttpClient;
import com.example.WebRecon.recon.step.HeadersAuditor;
import com.example.WebRecon.util.Prefs;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpInspectorFragment extends Fragment {

    private static final String KEY_URL = "url";
    private static final String KEY_METHOD = "method";
    private static final String KEY_HEADERS = "headers_text";
    private static final String KEY_BODY = "body_text";
    private static final String KEY_STATUS = "status";
    private static final String KEY_TIMING = "timing";
    private static final String KEY_RESP_HEADERS = "resp_headers";
    private static final String KEY_RESP_BODY = "resp_body";
    private static final String KEY_GRADE = "grade";

    private static final String[] METHODS = {"GET", "POST", "PUT", "DELETE", "HEAD"};

    private FragmentHttpInspectorBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String lastStatus = "";
    private String lastTiming = "";
    private String lastRespHeaders = "";
    private String lastRespBody = "";
    private String lastGrade = "";
    private String lastUrl = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHttpInspectorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, METHODS);
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMethod.setAdapter(methodAdapter);

        binding.spinnerMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String method = METHODS[position];
                boolean needsBody = method.equals("POST") || method.equals("PUT");
                binding.tilBody.setVisibility(needsBody ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (savedInstanceState != null) {
            binding.etUrl.setText(savedInstanceState.getString(KEY_URL, ""));
            binding.spinnerMethod.setSelection(savedInstanceState.getInt(KEY_METHOD, 0));
            binding.etHeaders.setText(savedInstanceState.getString(KEY_HEADERS, ""));
            binding.etBody.setText(savedInstanceState.getString(KEY_BODY, ""));
            lastStatus = savedInstanceState.getString(KEY_STATUS, "");
            lastTiming = savedInstanceState.getString(KEY_TIMING, "");
            lastRespHeaders = savedInstanceState.getString(KEY_RESP_HEADERS, "");
            lastRespBody = savedInstanceState.getString(KEY_RESP_BODY, "");
            lastGrade = savedInstanceState.getString(KEY_GRADE, "");
            lastUrl = savedInstanceState.getString(KEY_URL, "");
            if (!lastStatus.isEmpty()) restoreResponse();
        }

        binding.btnSend.setOnClickListener(v -> sendRequest());
        binding.btnOpenBrowser.setOnClickListener(v -> openInBrowser());
        binding.btnShareResponse.setOnClickListener(v -> shareResponse());
    }

    private void sendRequest() {
        String url = binding.etUrl.getText() != null
            ? binding.etUrl.getText().toString().trim() : "";
        if (TextUtils.isEmpty(url)) return;
        if (!url.startsWith("http")) url = "https://" + url;

        lastUrl = url;
        String method = METHODS[binding.spinnerMethod.getSelectedItemPosition()];
        String headersText = binding.etHeaders.getText() != null
            ? binding.etHeaders.getText().toString() : "";
        String bodyText = binding.etBody.getText() != null
            ? binding.etBody.getText().toString() : "";

        binding.btnSend.setEnabled(false);
        binding.btnSend.setText("Sending…");
        binding.cardResponse.setVisibility(View.GONE);

        final String finalUrl = url;
        executor.submit(() -> {
            try {
                OkHttpClient client = HttpClient.build(Prefs.getTimeoutSeconds(requireContext()));

                Request.Builder rb = new Request.Builder()
                    .url(finalUrl)
                    .header("User-Agent", Prefs.getUserAgent(requireContext()));

                // Parse extra headers
                for (String line : headersText.split("\n")) {
                    int idx = line.indexOf(':');
                    if (idx > 0) {
                        String name = line.substring(0, idx).trim();
                        String value = line.substring(idx + 1).trim();
                        if (!name.isEmpty()) rb.header(name, value);
                    }
                }

                // Body
                if (method.equals("POST") || method.equals("PUT")) {
                    MediaType mt = MediaType.parse("text/plain; charset=utf-8");
                    rb.method(method, RequestBody.create(bodyText, mt));
                } else if (method.equals("HEAD")) {
                    rb.head();
                } else if (method.equals("DELETE")) {
                    rb.delete();
                } else {
                    rb.get();
                }

                long start = System.currentTimeMillis();
                Response response = client.newCall(rb.build()).execute();
                long elapsed = System.currentTimeMillis() - start;

                String status = response.code() + " " + response.message();
                String timing = elapsed + " ms";
                StringBuilder respHeaders = new StringBuilder();
                for (int i = 0; i < response.headers().size(); i++) {
                    respHeaders.append(response.headers().name(i))
                        .append(": ")
                        .append(response.headers().value(i))
                        .append("\n");
                }
                HeadersAuditor.HeaderGrade grade = HeadersAuditor.audit(response.headers());
                String gradeText = HeadersAuditor.formatGradeReport(grade);

                String body = "";
                if (response.body() != null && !method.equals("HEAD")) {
                    String raw = response.body().string();
                    body = raw.length() > 50000 ? raw.substring(0, 50000) + "\n[truncated]" : raw;
                }

                final String fStatus = status;
                final String fTiming = timing;
                final String fRespHeaders = respHeaders.toString();
                final String fBody = body;
                final String fGrade = gradeText;

                mainHandler.post(() -> {
                    if (binding == null) return;
                    lastStatus = fStatus;
                    lastTiming = fTiming;
                    lastRespHeaders = fRespHeaders;
                    lastRespBody = fBody;
                    lastGrade = fGrade;
                    restoreResponse();
                    binding.btnSend.setEnabled(true);
                    binding.btnSend.setText(getString(R.string.btn_send));
                });

                saveOperation(finalUrl, status + "\n" + fRespHeaders);
                response.close();

            } catch (IOException e) {
                mainHandler.post(() -> {
                    if (binding == null) return;
                    lastStatus = "Error: " + e.getMessage();
                    lastTiming = "";
                    lastRespHeaders = "";
                    lastRespBody = "";
                    lastGrade = "";
                    restoreResponse();
                    binding.btnSend.setEnabled(true);
                    binding.btnSend.setText(getString(R.string.btn_send));
                });
            }
        });
    }

    private void restoreResponse() {
        binding.tvStatus.setText(lastStatus);
        binding.tvTiming.setText(lastTiming);
        binding.tvResponseHeaders.setText(lastRespHeaders);
        binding.tvBody.setText(lastRespBody);
        binding.tvGrade.setText(lastGrade);
        binding.cardResponse.setVisibility(View.VISIBLE);
    }

    private void openInBrowser() {
        String url = binding.etUrl.getText() != null
            ? binding.etUrl.getText().toString().trim() : lastUrl;
        if (url.isEmpty()) return;
        if (!url.startsWith("http")) url = "https://" + url;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void shareResponse() {
        String text = "URL: " + lastUrl + "\nStatus: " + lastStatus
            + "\nTiming: " + lastTiming + "\n\nHeaders:\n" + lastRespHeaders
            + "\n\nBody:\n" + lastRespBody;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    private void saveOperation(String input, String output) {
        executor.submit(() -> {
            ToolOperation op = new ToolOperation(ToolType.HTTP, input, output);
            AppDatabase.getInstance(requireContext()).toolOperationDao().insert(op);
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            if (binding.etUrl.getText() != null)
                outState.putString(KEY_URL, binding.etUrl.getText().toString());
            outState.putInt(KEY_METHOD, binding.spinnerMethod.getSelectedItemPosition());
            if (binding.etHeaders.getText() != null)
                outState.putString(KEY_HEADERS, binding.etHeaders.getText().toString());
            if (binding.etBody.getText() != null)
                outState.putString(KEY_BODY, binding.etBody.getText().toString());
        }
        outState.putString(KEY_STATUS, lastStatus);
        outState.putString(KEY_TIMING, lastTiming);
        outState.putString(KEY_RESP_HEADERS, lastRespHeaders);
        outState.putString(KEY_RESP_BODY, lastRespBody.length() > 50000
            ? lastRespBody.substring(0, 50000) : lastRespBody);
        outState.putString(KEY_GRADE, lastGrade);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
