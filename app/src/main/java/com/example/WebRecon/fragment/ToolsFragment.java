package com.example.WebRecon.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.WebRecon.R;
import com.example.WebRecon.ToolActivity;
import com.example.WebRecon.databinding.FragmentToolsBinding;
import com.google.android.material.card.MaterialCardView;
import android.widget.TextView;

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
        addToolCard("JWT Inspector", ToolActivity.TYPE_JWT);
        addToolCard("HTTP Inspector", ToolActivity.TYPE_HTTP);
        addToolCard("Encoder / Decoder", ToolActivity.TYPE_ENCODER);
        addToolCard("Hash Lab", ToolActivity.TYPE_HASH);
    }

    private void addToolCard(String label, String toolType) {
        MaterialCardView card = new MaterialCardView(requireContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.setMargins(8, 8, 8, 8);
        card.setLayoutParams(params);
        card.setCardElevation(4f);
        card.setRadius(12f);
        card.setUseCompatPadding(true);

        TextView tv = new TextView(requireContext());
        tv.setText(label);
        tv.setPadding(32, 48, 32, 48);
        tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        tv.setGravity(android.view.Gravity.CENTER);
        card.addView(tv);

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
