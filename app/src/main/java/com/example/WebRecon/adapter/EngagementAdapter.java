package com.example.WebRecon.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.WebRecon.R;
import com.example.WebRecon.db.entity.Engagement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EngagementAdapter extends RecyclerView.Adapter<EngagementAdapter.VH> {

    public interface ClickListener {
        void onClick(Engagement engagement);
    }

    private List<Engagement> items = new ArrayList<>();
    private final ClickListener listener;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());

    public EngagementAdapter(ClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Engagement> list) {
        items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_engagement, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Engagement e = items.get(position);
        h.tvDomain.setText(e.domain);
        h.tvDate.setText(fmt.format(new Date(e.startedAt)));
        h.tvStatus.setText(e.status != null ? e.status.name() : "");
        h.tvFindingCount.setText("");
        h.itemView.setOnClickListener(v -> listener.onClick(e));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDomain, tvDate, tvStatus, tvFindingCount;

        VH(@NonNull View v) {
            super(v);
            tvDomain = v.findViewById(R.id.tv_domain);
            tvDate = v.findViewById(R.id.tv_date);
            tvStatus = v.findViewById(R.id.tv_status);
            tvFindingCount = v.findViewById(R.id.tv_finding_count);
        }
    }
}
