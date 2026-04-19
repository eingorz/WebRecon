package com.example.WebRecon.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.drawable.GradientDrawable;

import com.example.WebRecon.R;
import com.example.WebRecon.db.AppDatabase;
import com.example.WebRecon.db.EngagementStatus;
import com.example.WebRecon.db.entity.Engagement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EngagementAdapter extends RecyclerView.Adapter<EngagementAdapter.VH> {

    public interface ClickListener {
        void onClick(Engagement engagement);
    }

    private static final Executor dbExecutor = Executors.newCachedThreadPool();

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
        applyStatus(h, e.status);
        h.tvFindingCount.setText("");
        h.tvFindingCount.setTag(e.id);
        dbExecutor.execute(() -> {
            int count = AppDatabase.getInstance(h.itemView.getContext())
                .engagementDao().getFindingCount(e.id);
            h.itemView.post(() -> {
                if (Objects.equals(h.tvFindingCount.getTag(), e.id))
                    h.tvFindingCount.setText(count + " findings");
            });
        });
        h.itemView.setOnClickListener(v -> listener.onClick(e));
    }

    private static void applyStatus(VH h, EngagementStatus status) {
        int color;
        String label;
        if (status == EngagementStatus.COMPLETED) {
            color = 0xFF00E676; label = "DONE";
        } else if (status == EngagementStatus.FAILED) {
            color = 0xFFF85149; label = "FAIL";
        } else {
            color = 0xFFE3B341; label = "RUNNING";
        }
        h.statusBar.setBackgroundColor(color);
        h.tvStatus.setText(label);
        float r = 3 * h.itemView.getContext().getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(r);
        h.tvStatus.setBackground(bg);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        View statusBar;
        TextView tvDomain, tvDate, tvStatus, tvFindingCount;

        VH(@NonNull View v) {
            super(v);
            statusBar = v.findViewById(R.id.status_bar);
            tvDomain = v.findViewById(R.id.tv_domain);
            tvDate = v.findViewById(R.id.tv_date);
            tvStatus = v.findViewById(R.id.tv_status);
            tvFindingCount = v.findViewById(R.id.tv_finding_count);
        }
    }
}
