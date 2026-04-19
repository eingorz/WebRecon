package com.example.WebRecon.fragment.tool;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.WebRecon.R;
import com.example.WebRecon.databinding.FragmentEncoderBinding;
import com.example.WebRecon.db.AppDatabase;
import com.example.WebRecon.db.ToolType;
import com.example.WebRecon.db.entity.ToolOperation;
import com.example.WebRecon.tools.EncoderTool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EncoderFragment extends Fragment {

    private static final String KEY_INPUT = "input";
    private static final String KEY_SPINNER = "spinner";
    private static final String KEY_OUTPUT = "output";

    private static final String[] FORMATS = {"Base64", "URL", "Hex", "HTML"};

    private FragmentEncoderBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String lastOutput = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEncoderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, FORMATS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFormat.setAdapter(adapter);

        if (savedInstanceState != null) {
            binding.etInput.setText(savedInstanceState.getString(KEY_INPUT, ""));
            binding.spinnerFormat.setSelection(savedInstanceState.getInt(KEY_SPINNER, 0));
            lastOutput = savedInstanceState.getString(KEY_OUTPUT, "");
            if (!lastOutput.isEmpty()) showOutput(lastOutput);
        }

        binding.btnEncode.setOnClickListener(v -> performAction(true));
        binding.btnDecode.setOnClickListener(v -> performAction(false));
        binding.btnAutoDetect.setOnClickListener(v -> autoDetect());
        binding.btnCopy.setOnClickListener(v -> copyOutput());
        binding.btnShare.setOnClickListener(v -> shareOutput());
    }

    private void performAction(boolean encode) {
        String input = binding.etInput.getText() != null
            ? binding.etInput.getText().toString() : "";
        if (input.isEmpty()) return;

        String format = FORMATS[binding.spinnerFormat.getSelectedItemPosition()];
        String result;
        try {
            if (encode) {
                switch (format) {
                    case "URL": result = EncoderTool.encodeUrl(input); break;
                    case "Hex": result = EncoderTool.encodeHex(input); break;
                    case "HTML": result = EncoderTool.encodeHtml(input); break;
                    default: result = EncoderTool.encodeBase64(input); break;
                }
            } else {
                switch (format) {
                    case "URL": result = EncoderTool.decodeUrl(input); break;
                    case "Hex": result = EncoderTool.decodeHex(input); break;
                    case "HTML": result = EncoderTool.decodeHtml(input); break;
                    default: result = EncoderTool.decodeBase64(input); break;
                }
            }
        } catch (Exception e) {
            result = "Error: " + e.getMessage();
        }
        showOutput(result);
        saveOperation(input, result);
    }

    private void autoDetect() {
        String input = binding.etInput.getText() != null
            ? binding.etInput.getText().toString() : "";
        if (input.isEmpty()) return;
        String result = EncoderTool.autoDecode(input);
        showOutput(result);
        saveOperation(input, result);
    }

    private void showOutput(String output) {
        lastOutput = output;
        binding.tvOutput.setText(output);
        binding.cardOutput.setVisibility(View.VISIBLE);
    }

    private void copyOutput() {
        ClipboardManager cm = (ClipboardManager)
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("encoded", lastOutput));
        Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show();
    }

    private void shareOutput() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, lastOutput);
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    private void saveOperation(String input, String output) {
        executor.submit(() -> {
            ToolOperation op = new ToolOperation(ToolType.ENCODER, input, output);
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
            outState.putInt(KEY_SPINNER, binding.spinnerFormat.getSelectedItemPosition());
        }
        outState.putString(KEY_OUTPUT, lastOutput);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
