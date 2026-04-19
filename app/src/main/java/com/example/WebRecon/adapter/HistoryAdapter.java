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
import com.example.WebRecon.db.entity.ToolOperation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ENGAGEMENT = 0;
    private static final int TYPE_OPERATION = 1;

    private static final Executor dbExecutor = Executors.newCachedThreadPool();

    public interface EngagementClick { void onClick(Engagement e); }
    public interface OperationClick { void onClick(ToolOperation op); }

    private List<Object> allItems = new ArrayList<>();
    private List<Object> filteredItems = new ArrayList<>();
    private final EngagementClick engagementClick;
    private final OperationClick operationClick;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());

    public HistoryAdapter(EngagementClick ec, OperationClick oc) {
        this.engagementClick = ec;
        this.operationClick = oc;
    }

    public void setItems(List<Object> items, String query) {
        allItems = items != null ? items : new ArrayList<>();
        filter(query);
    }

    public void filter(String query) {
        if (query == null || query.isEmpty()) {
            filteredItems = new ArrayList<>(allItems);
        } else {
            String q = query.toLowerCase();
            filteredItems = new ArrayList<>();
            for (Object item : allItems) {
                if (item instanceof Engagement) {
                    if (((Engagement) item).domain.toLowerCase().contains(q)) {
                        filteredItems.add(item);
                    }
                } else if (item instanceof ToolOperation) {
                    ToolOperation op = (ToolOperation) item;
                    String typeStr = op.toolType != null ? op.toolType.name().toLowerCase() : "";
                    String inputStr = op.input != null ? op.input.toLowerCase() : "";
                    if (typeStr.contains(q) || inputStr.contains(q)) {
                        filteredItems.add(item);
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return filteredItems.get(position) instanceof Engagement ? TYPE_ENGAGEMENT : TYPE_OPERATION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ENGAGEMENT) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_engagement, parent, false);
            return new EngagementVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tool_operation, parent, false);
            return new OperationVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = filteredItems.get(position);
        if (holder instanceof EngagementVH) {
            Engagement e = (Engagement) item;
            EngagementVH h = (EngagementVH) holder;
            h.tvDomain.setText(e.domain);
            h.tvDate.setText(fmt.format(new Date(e.startedAt)));
            applyEngagementStatus(h, e.status);
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
            h.itemView.setOnClickListener(v -> engagementClick.onClick(e));
        } else if (holder instanceof OperationVH) {
            ToolOperation op = (ToolOperation) item;
            OperationVH h = (OperationVH) holder;
            h.tvToolType.setText(op.toolType != null ? op.toolType.name() : "");
            h.tvDate.setText(fmt.format(new Date(op.createdAt)));
            h.tvInput.setText(op.input != null ? op.input : "");
            h.itemView.setOnClickListener(v -> operationClick.onClick(op));
        }
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    private static void applyEngagementStatus(EngagementVH h, EngagementStatus status) {
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

    static class EngagementVH extends RecyclerView.ViewHolder {
        View statusBar;
        TextView tvDomain, tvDate, tvStatus, tvFindingCount;
        EngagementVH(@NonNull View v) {
            super(v);
            statusBar = v.findViewById(R.id.status_bar);
            tvDomain = v.findViewById(R.id.tv_domain);
            tvDate = v.findViewById(R.id.tv_date);
            tvStatus = v.findViewById(R.id.tv_status);
            tvFindingCount = v.findViewById(R.id.tv_finding_count);
        }
    }

    static class OperationVH extends RecyclerView.ViewHolder {
        TextView tvToolType, tvDate, tvInput;
        OperationVH(@NonNull View v) {
            super(v);
            tvToolType = v.findViewById(R.id.tv_tool_type);
            tvDate = v.findViewById(R.id.tv_date);
            tvInput = v.findViewById(R.id.tv_input);
        }
    }
}
