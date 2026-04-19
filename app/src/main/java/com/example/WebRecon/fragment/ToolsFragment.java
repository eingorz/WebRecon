package com.example.WebRecon.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.WebRecon.R;
import com.example.WebRecon.ToolActivity;
import com.example.WebRecon.databinding.FragmentToolsBinding;
import com.google.android.material.card.MaterialCardView;

public class ToolsFragment extends Fragment {

    private FragmentToolsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentToolsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addToolCard("[JWT]", "JWT Inspector", "Decode & audit\nJSON Web Tokens", ToolActivity.TYPE_JWT);
        addToolCard("[HTTP]", "HTTP Inspector", "Send requests &\nanalyze headers", ToolActivity.TYPE_HTTP);
        addToolCard("[ENC]", "Encoder / Decoder", "Base64, URL, HTML\nencoding tools", ToolActivity.TYPE_ENCODER);
        addToolCard("[HSH]", "Hash Lab", "Generate & identify\nhash digests", ToolActivity.TYPE_HASH);
    }

    private void addToolCard(String tag, String label, String desc, String toolType) {
        float density = getResources().getDisplayMetrics().density;

        MaterialCardView card = new MaterialCardView(requireContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        int margin = (int) (8 * density);
        params.setMargins(margin, margin, margin, margin);
        card.setLayoutParams(params);
        card.setCardElevation(0f);
        card.setRadius(6 * density);
        card.setUseCompatPadding(false);
        card.setStrokeColor(getResources().getColor(R.color.outline_subtle, null));
        card.setStrokeWidth((int) density);
        card.setCardBackgroundColor(getResources().getColor(R.color.surface_card, null));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (14 * density);
        content.setPadding(pad, pad, pad, pad);

        TextView tvTag = new TextView(requireContext());
        tvTag.setText(tag);
        tvTag.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvTag.setTextSize(10f);
        tvTag.setTextColor(getResources().getColor(R.color.hacker_green, null));
        tvTag.setLetterSpacing(0.08f);

        GradientDrawable tagBg = new GradientDrawable();
        tagBg.setColor(Color.parseColor("#1A00E676"));
        tagBg.setCornerRadius(3 * density);
        tvTag.setBackground(tagBg);
        int hPad = (int) (6 * density);
        int vPad = (int) (2 * density);
        tvTag.setPadding(hPad, vPad, hPad, vPad);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvLabel.setTextSize(13f);
        tvLabel.setTextColor(getResources().getColor(R.color.on_surface, null));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = (int) (6 * density);
        tvLabel.setLayoutParams(labelParams);

        TextView tvDesc = new TextView(requireContext());
        tvDesc.setText(desc);
        tvDesc.setTypeface(Typeface.MONOSPACE);
        tvDesc.setTextSize(11f);
        tvDesc.setTextColor(getResources().getColor(R.color.on_surface_dim, null));
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = (int) (4 * density);
        tvDesc.setLayoutParams(descParams);

        content.addView(tvTag);
        content.addView(tvLabel);
        content.addView(tvDesc);
        card.addView(content);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), ToolActivity.class);
            intent.putExtra(ToolActivity.EXTRA_TOOL_TYPE, toolType);
            startActivity(intent);
        });

        binding.gridTools.addView(card);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
