package com.example.WebRecon.fragment.tool;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.WebRecon.databinding.FragmentHashBinding;
import com.example.WebRecon.db.AppDatabase;
import com.example.WebRecon.db.ToolType;
import com.example.WebRecon.db.entity.ToolOperation;
import com.example.WebRecon.net.HttpClient;
import com.example.WebRecon.tools.HashTool;
import com.example.WebRecon.tools.HibpChecker;
import com.example.WebRecon.util.Prefs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HashFragment extends Fragment {

    private static final String KEY_INPUT = "input";
    private static final String KEY_ALGO = "algo";
    private static final String KEY_HASH = "hash";
    private static final String KEY_IDENTIFY = "identify";
    private static final String KEY_HIBP = "hibp";

    private FragmentHashBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String lastHash = "";
    private String lastIdentify = "";
    private String lastHibp = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHashBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!Prefs.isHibpEnabled(requireContext())) {
            binding.btnHibp.setVisibility(View.GONE);
        }

        if (savedInstanceState != null) {
            binding.etInput.setText(savedInstanceState.getString(KEY_INPUT, ""));
            int algoId = savedInstanceState.getInt(KEY_ALGO, binding.rbSha256.getId());
            binding.rgAlgo.check(algoId);
            lastHash = savedInstanceState.getString(KEY_HASH, "");
            lastIdentify = savedInstanceState.getString(KEY_IDENTIFY, "");
            lastHibp = savedInstanceState.getString(KEY_HIBP, "");
            if (!lastHash.isEmpty()) restoreOutput();
        }

        binding.btnHash.setOnClickListener(v -> computeHash());
        binding.btnIdentify.setOnClickListener(v -> identifyHash());
        binding.btnHibp.setOnClickListener(v -> checkHibp());
    }

    private String getSelectedAlgo() {
        int id = binding.rgAlgo.getCheckedRadioButtonId();
        if (id == binding.rbMd5.getId()) return "MD5";
        if (id == binding.rbSha1.getId()) return "SHA-1";
        if (id == binding.rbSha512.getId()) return "SHA-512";
        return "SHA-256";
    }

    private void computeHash() {
        String input = getInput();
        if (TextUtils.isEmpty(input)) return;
        String algo = getSelectedAlgo();
        lastHash = HashTool.hash(input, algo);
        restoreOutput();
        saveOperation(input, lastHash);
    }

    private void identifyHash() {
        String input = getInput();
        if (TextUtils.isEmpty(input)) return;
        lastIdentify = HashTool.identifyHash(input);
        binding.tvIdentify.setText(lastIdentify);
        binding.tvIdentify.setVisibility(View.VISIBLE);
        binding.cardResult.setVisibility(View.VISIBLE);
    }

    private void checkHibp() {
        String input = getInput();
        if (TextUtils.isEmpty(input)) return;
        binding.tvHibp.setText("Checking…");
        binding.tvHibp.setVisibility(View.VISIBLE);
        binding.cardResult.setVisibility(View.VISIBLE);

        HibpChecker.checkPwned(input, HttpClient.get(requireContext()), new HibpChecker.HibpResult() {
            @Override
            public void onResult(int count) {
                mainHandler.post(() -> {
                    if (binding == null) return;
                    lastHibp = count > 0
                        ? String.format("Pwned %d times!", count)
                        : "Not found in breaches.";
                    binding.tvHibp.setText(lastHibp);
                    binding.tvHibp.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    if (binding == null) return;
                    lastHibp = "Error: " + message;
                    binding.tvHibp.setText(lastHibp);
                    binding.tvHibp.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void restoreOutput() {
        binding.tvHash.setText(lastHash);
        binding.cardResult.setVisibility(View.VISIBLE);
        if (!lastIdentify.isEmpty()) {
            binding.tvIdentify.setText(lastIdentify);
            binding.tvIdentify.setVisibility(View.VISIBLE);
        }
        if (!lastHibp.isEmpty()) {
            binding.tvHibp.setText(lastHibp);
            binding.tvHibp.setVisibility(View.VISIBLE);
        }
    }

    private String getInput() {
        return binding.etInput.getText() != null
            ? binding.etInput.getText().toString().trim() : "";
    }

    private void saveOperation(String input, String output) {
        executor.submit(() -> {
            ToolOperation op = new ToolOperation(ToolType.HASH, input, output);
            AppDatabase.getInstance(requireContext()).toolOperationDao().insert(op);
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            if (binding.etInput.getText() != null) {
                outState.putString(KEY_INPUT, binding.etInput.getText().toString());
            }
            outState.putInt(KEY_ALGO, binding.rgAlgo.getCheckedRadioButtonId());
        }
        outState.putString(KEY_HASH, lastHash);
        outState.putString(KEY_IDENTIFY, lastIdentify);
        outState.putString(KEY_HIBP, lastHibp);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
