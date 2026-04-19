package com.example.WebRecon.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.WebRecon.R;
import com.example.WebRecon.db.Severity;
import com.example.WebRecon.db.entity.Finding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FindingAdapter extends RecyclerView.Adapter<FindingAdapter.VH> {

    private final List<Finding> items = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public void addFinding(Finding f) {
        items.add(f);
        notifyItemInserted(items.size() - 1);
    }

    public void setFindings(List<Finding> findings) {
        items.clear();
        if (findings != null) items.addAll(findings);
        notifyDataSetChanged();
    }

    public List<Finding> getFindings() {
        return new ArrayList<>(items);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_finding, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Finding f = items.get(position);

        h.tvTitle.setText(f.title);
        h.tvType.setText(f.type != null ? f.type.name() : "");
        h.tvTime.setText(fmt.format(new Date(f.discoveredAt)));

        if (f.detail != null && !f.detail.isEmpty()) {
            h.tvDetail.setText(f.detail);
            h.itemView.setOnClickListener(v -> {
                boolean shown = h.tvDetail.getVisibility() == View.VISIBLE;
                h.tvDetail.setVisibility(shown ? View.GONE : View.VISIBLE);
            });
        } else {
            h.tvDetail.setVisibility(View.GONE);
            h.itemView.setOnClickListener(null);
        }

        int barColor;
        String badge;
        int badgeTextColor;
        if (f.severity == Severity.CRIT) {
            barColor = 0xFFF85149;
            badge = "CRIT";
            badgeTextColor = 0xFF000000;
        } else if (f.severity == Severity.WARN) {
            barColor = 0xFFE3B341;
            badge = "WARN";
            badgeTextColor = 0xFF000000;
        } else {
            barColor = 0xFF58A6FF;
            badge = "INFO";
            badgeTextColor = 0xFF000000;
        }
        h.severityBar.setBackgroundColor(barColor);
        h.tvSevBadge.setText(badge);
        h.tvSevBadge.setTextColor(badgeTextColor);
        float r = 4 * h.itemView.getContext().getResources().getDisplayMetrics().density;
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(barColor);
        badgeBg.setCornerRadius(r);
        h.tvSevBadge.setBackground(badgeBg);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        View severityBar;
        TextView tvSevBadge, tvType, tvTime, tvTitle, tvDetail;

        VH(@NonNull View v) {
            super(v);
            severityBar = v.findViewById(R.id.severity_bar);
            tvSevBadge = v.findViewById(R.id.tv_sev_badge);
            tvType = v.findViewById(R.id.tv_type);
            tvTime = v.findViewById(R.id.tv_time);
            tvTitle = v.findViewById(R.id.tv_title);
            tvDetail = v.findViewById(R.id.tv_detail);
        }
    }
}
