package com.example.WebRecon.fragment.tool;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.WebRecon.R;
import com.example.WebRecon.ToolActivity;
import com.example.WebRecon.databinding.FragmentJwtBinding;
import com.example.WebRecon.db.AppDatabase;
import com.example.WebRecon.db.ToolType;
import com.example.WebRecon.db.entity.ToolOperation;
import com.example.WebRecon.tools.JwtTool;
import com.example.WebRecon.util.WordlistManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JwtFragment extends Fragment {

    private static final String KEY_TOKEN = "token";
    private static final String KEY_HEADER = "header";
    private static final String KEY_PAYLOAD = "payload";
    private static final String KEY_ISSUES = "issues";

    private FragmentJwtBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String lastHeader = "";
    private String lastPayload = "";
    private String lastIssues = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentJwtBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            binding.etToken.setText(savedInstanceState.getString(KEY_TOKEN, ""));
            lastHeader = savedInstanceState.getString(KEY_HEADER, "");
            lastPayload = savedInstanceState.getString(KEY_PAYLOAD, "");
            lastIssues = savedInstanceState.getString(KEY_ISSUES, "");
            if (!lastHeader.isEmpty()) restoreOutput();
        }

        binding.btnDecode.setOnClickListener(v -> decodeToken());
        binding.btnCheckSecrets.setOnClickListener(v -> checkSecrets());
        binding.btnShare.setOnClickListener(v -> shareResult());

        if (savedInstanceState == null) {
            long opId = getArguments() != null
                ? getArguments().getLong(ToolActivity.EXTRA_OPERATION_ID, -1L) : -1L;
            if (opId > 0) loadFromHistory(opId);
        }
    }

    private void loadFromHistory(long operationId) {
        executor.submit(() -> {
            ToolOperation op = AppDatabase.getInstance(requireContext())
                .toolOperationDao().getByIdSync(operationId);
            if (op == null || op.input == null || getActivity() == null) return;
            JwtTool.JwtDecodeResult result = JwtTool.decode(op.input);
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                binding.etToken.setText(op.input);
                if (result.success) {
                    lastHeader = result.headerJson;
                    lastPayload = result.payloadJson;
                    lastIssues = TextUtils.isEmpty(result.issues.toString())
                        ? getString(R.string.no_issues)
                        : android.text.TextUtils.join("\n", result.issues);
                    restoreOutput();
                }
            });
        });
    }

    private void decodeToken() {
        String token = binding.etToken.getText() != null
            ? binding.etToken.getText().toString().trim() : "";
        if (TextUtils.isEmpty(token)) return;

        JwtTool.JwtDecodeResult result = JwtTool.decode(token);

        if (!result.success) {
            binding.tvHeader.setText(result.error);
            binding.cardHeader.setVisibility(View.VISIBLE);
            binding.cardPayload.setVisibility(View.GONE);
            binding.cardIssues.setVisibility(View.GONE);
            binding.btnShare.setVisibility(View.GONE);
            return;
        }

        lastHeader = result.headerJson;
        lastPayload = result.payloadJson;
        lastIssues = TextUtils.isEmpty(result.issues.toString())
            ? getString(R.string.no_issues)
            : android.text.TextUtils.join("\n", result.issues);

        restoreOutput();
        saveOperation(token, buildShareText());
    }

    private void restoreOutput() {
        binding.tvHeader.setText(lastHeader);
        binding.tvPayload.setText(lastPayload);
        binding.tvIssues.setText(lastIssues);
        binding.cardHeader.setVisibility(View.VISIBLE);
        binding.cardPayload.setVisibility(View.VISIBLE);
        binding.cardIssues.setVisibility(View.VISIBLE);
        binding.btnShare.setVisibility(View.VISIBLE);
    }

    private void checkSecrets() {
        String token = binding.etToken.getText() != null
            ? binding.etToken.getText().toString().trim() : "";
        if (TextUtils.isEmpty(token)) return;

        executor.submit(() -> {
            List<String> secrets = WordlistManager.readLines(
                requireContext(), WordlistManager.FILE_JWT_SECRETS);
            String found = JwtTool.bruteSecret(token, secrets);
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                String msg = found != null
                    ? "CRITICAL: Weak secret found: \"" + found + "\""
                    : "No weak secret matched from wordlist.";
                String current = binding.tvIssues.getText().toString();
                binding.tvIssues.setText(current + "\n" + msg);
                binding.cardIssues.setVisibility(View.VISIBLE);
            });
        });
    }

    private void shareResult() {
        String text = buildShareText();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    private String buildShareText() {
        return "=== JWT Decode Result ===\n\n"
            + "HEADER:\n" + lastHeader + "\n\n"
            + "PAYLOAD:\n" + lastPayload + "\n\n"
            + "ISSUES:\n" + lastIssues;
    }

    private void saveOperation(String token, String output) {
        executor.submit(() -> {
            ToolOperation op = new ToolOperation(ToolType.JWT, token, output);
            AppDatabase.getInstance(requireContext()).toolOperationDao().insert(op);
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null && binding.etToken.getText() != null) {
            outState.putString(KEY_TOKEN, binding.etToken.getText().toString());
        }
        outState.putString(KEY_HEADER, lastHeader);
        outState.putString(KEY_PAYLOAD, lastPayload);
        outState.putString(KEY_ISSUES, lastIssues);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
