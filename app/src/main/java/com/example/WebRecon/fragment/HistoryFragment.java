package com.example.WebRecon.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MediatorLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.WebRecon.EngagementActivity;
import com.example.WebRecon.ToolActivity;
import com.example.WebRecon.adapter.HistoryAdapter;
import com.example.WebRecon.databinding.FragmentHistoryBinding;
import com.example.WebRecon.db.AppDatabase;
import com.example.WebRecon.db.entity.Engagement;
import com.example.WebRecon.db.entity.ToolOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryFragment extends Fragment {

    private static final String KEY_QUERY = "query";
    private static final String KEY_SCROLL = "scroll";

    private FragmentHistoryBinding binding;
    private HistoryAdapter adapter;
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            currentQuery = savedInstanceState.getString(KEY_QUERY, "");
        }

        adapter = new HistoryAdapter(
            engagement -> {
                Intent intent = new Intent(requireActivity(), EngagementActivity.class);
                intent.putExtra(EngagementActivity.EXTRA_ENGAGEMENT_ID, engagement.id);
                intent.putExtra(EngagementActivity.EXTRA_DOMAIN, engagement.domain);
                startActivity(intent);
            },
            operation -> {
                Intent intent = new Intent(requireActivity(), ToolActivity.class);
                intent.putExtra(ToolActivity.EXTRA_TOOL_TYPE, operation.toolType.name());
                intent.putExtra(ToolActivity.EXTRA_OPERATION_ID, operation.id);
                startActivity(intent);
            }
        );

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(adapter);

        AppDatabase db = AppDatabase.getInstance(requireContext());
        MediatorLiveData<List<Object>> merged = new MediatorLiveData<>();

        final List<Engagement>[] engagements = new List[]{new ArrayList<>()};
        final List<ToolOperation>[] operations = new List[]{new ArrayList<>()};

        merged.addSource(db.engagementDao().getAll(), list -> {
            engagements[0] = list != null ? list : new ArrayList<>();
            merged.setValue(buildMergedList(engagements[0], operations[0]));
        });
        merged.addSource(db.toolOperationDao().getAll(), list -> {
            operations[0] = list != null ? list : new ArrayList<>();
            merged.setValue(buildMergedList(engagements[0], operations[0]));
        });

        merged.observe(getViewLifecycleOwner(), items -> {
            adapter.setItems(items, currentQuery);
            boolean empty = items == null || items.isEmpty();
            binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                adapter.filter(currentQuery);
                return true;
            }
        });

        if (!currentQuery.isEmpty()) {
            binding.searchView.setQuery(currentQuery, false);
        }

        if (savedInstanceState != null) {
            int scroll = savedInstanceState.getInt(KEY_SCROLL, 0);
            binding.rvHistory.scrollToPosition(scroll);
        }
    }

    private List<Object> buildMergedList(List<Engagement> engagements, List<ToolOperation> operations) {
        List<Object> merged = new ArrayList<>();
        merged.addAll(engagements);
        merged.addAll(operations);
        Collections.sort(merged, (a, b) -> {
            long ta = a instanceof Engagement ? ((Engagement) a).startedAt : ((ToolOperation) a).createdAt;
            long tb = b instanceof Engagement ? ((Engagement) b).startedAt : ((ToolOperation) b).createdAt;
            return Long.compare(tb, ta); // newest first
        });
        return merged;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_QUERY, currentQuery);
        if (binding != null) {
            LinearLayoutManager lm = (LinearLayoutManager) binding.rvHistory.getLayoutManager();
            if (lm != null) {
                outState.putInt(KEY_SCROLL, lm.findFirstVisibleItemPosition());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
