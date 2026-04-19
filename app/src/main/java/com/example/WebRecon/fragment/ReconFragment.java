package com.example.WebRecon.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.WebRecon.EngagementActivity;
import com.example.WebRecon.adapter.EngagementAdapter;
import com.example.WebRecon.databinding.FragmentReconBinding;
import com.example.WebRecon.db.AppDatabase;
import com.example.WebRecon.db.entity.Engagement;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReconFragment extends Fragment {

    private static final String KEY_DOMAIN = "domain";

    private FragmentReconBinding binding;
    private EngagementAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReconBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            String saved = savedInstanceState.getString(KEY_DOMAIN, "");
            binding.etDomain.setText(saved);
        }

        adapter = new EngagementAdapter(engagement -> openEngagement(engagement));
        binding.rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecent.setAdapter(adapter);

        AppDatabase.getInstance(requireContext())
            .engagementDao()
            .getRecent()
            .observe(getViewLifecycleOwner(), engagements -> adapter.submitList(engagements));

        binding.btnRunRecon.setOnClickListener(v -> startRecon());
    }

    private void startRecon() {
        String domain = binding.etDomain.getText() != null
            ? binding.etDomain.getText().toString().trim() : "";

        if (TextUtils.isEmpty(domain)) {
            binding.tilDomain.setError("Enter a domain");
            return;
        }
        binding.tilDomain.setError(null);

        // Strip protocol prefix if pasted
        domain = domain.replaceFirst("^https?://", "").replaceAll("/.*$", "");

        final String finalDomain = domain;
        executor.submit(() -> {
            Engagement engagement = new Engagement(finalDomain);
            long id = AppDatabase.getInstance(requireContext()).engagementDao().insert(engagement);
            if (getActivity() == null) return;
            Intent intent = new Intent(requireActivity(), EngagementActivity.class);
            intent.putExtra(EngagementActivity.EXTRA_ENGAGEMENT_ID, id);
            intent.putExtra(EngagementActivity.EXTRA_DOMAIN, finalDomain);
            requireActivity().runOnUiThread(() -> startActivity(intent));
        });
    }

    private void openEngagement(Engagement engagement) {
        Intent intent = new Intent(requireActivity(), EngagementActivity.class);
        intent.putExtra(EngagementActivity.EXTRA_ENGAGEMENT_ID, engagement.id);
        intent.putExtra(EngagementActivity.EXTRA_DOMAIN, engagement.domain);
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null && binding.etDomain.getText() != null) {
            outState.putString(KEY_DOMAIN, binding.etDomain.getText().toString());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
