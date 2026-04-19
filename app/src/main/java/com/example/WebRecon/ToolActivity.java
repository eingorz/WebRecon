package com.example.WebRecon;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.WebRecon.databinding.ActivityToolBinding;
import com.example.WebRecon.fragment.tool.EncoderFragment;
import com.example.WebRecon.fragment.tool.HashFragment;
import com.example.WebRecon.fragment.tool.HttpInspectorFragment;
import com.example.WebRecon.fragment.tool.JwtFragment;

public class ToolActivity extends AppCompatActivity {

    public static final String EXTRA_TOOL_TYPE = "TOOL_TYPE";
    public static final String EXTRA_OPERATION_ID = "OPERATION_ID";

    public static final String TYPE_JWT = "JWT";
    public static final String TYPE_HTTP = "HTTP";
    public static final String TYPE_ENCODER = "ENCODER";
    public static final String TYPE_HASH = "HASH";

    private static final String KEY_TOOL_TYPE = "tool_type";

    private ActivityToolBinding binding;
    private String toolType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityToolBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            toolType = savedInstanceState.getString(KEY_TOOL_TYPE, TYPE_JWT);
        } else {
            toolType = getIntent().getStringExtra(EXTRA_TOOL_TYPE);
            if (toolType == null) toolType = TYPE_JWT;
        }

        binding.toolbar.setTitle(titleForType(toolType));

        if (savedInstanceState == null) {
            long operationId = getIntent().getLongExtra(EXTRA_OPERATION_ID, -1L);
            Fragment fragment = fragmentForType(toolType, operationId);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        }
    }

    private String titleForType(String type) {
        switch (type) {
            case TYPE_HTTP: return "HTTP Inspector";
            case TYPE_ENCODER: return "Encoder / Decoder";
            case TYPE_HASH: return "Hash Lab";
            default: return "JWT Inspector";
        }
    }

    private Fragment fragmentForType(String type, long operationId) {
        Bundle args = new Bundle();
        args.putLong(EXTRA_OPERATION_ID, operationId);
        switch (type) {
            case TYPE_HTTP: {
                HttpInspectorFragment f = new HttpInspectorFragment();
                f.setArguments(args);
                return f;
            }
            case TYPE_ENCODER: {
                EncoderFragment f = new EncoderFragment();
                f.setArguments(args);
                return f;
            }
            case TYPE_HASH: {
                HashFragment f = new HashFragment();
                f.setArguments(args);
                return f;
            }
            default: {
                JwtFragment f = new JwtFragment();
                f.setArguments(args);
                return f;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_TOOL_TYPE, toolType);
    }
}
