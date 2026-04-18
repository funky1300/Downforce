package com.example.downforce;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class DriverSelectionAdapter
        extends RecyclerView.Adapter<DriverSelectionAdapter.DriverViewHolder> {

    public interface SelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    private final List<BettingFragment.Driver> drivers;
    private final List<Boolean> selected;
    private final int maxSelections;
    private final SelectionListener listener;

    public DriverSelectionAdapter(List<BettingFragment.Driver> drivers,
                                  int maxSelections,
                                  SelectionListener listener) {
        this.drivers = drivers;
        this.maxSelections = maxSelections;
        this.listener = listener;
        this.selected = new ArrayList<>();
        for (int i = 0; i < drivers.size(); i++) selected.add(false);
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_select, parent, false);
        return new DriverViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder h, int position) {
        BettingFragment.Driver driver = drivers.get(position);
        boolean isSelected = selected.get(position);

        h.tvCode.setText(driver.code);
        h.tvName.setText(driver.name);
        h.tvTeam.setText(driver.team);

        // Team colour strip
        try {
            int color = Color.parseColor(driver.teamColor);
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(color);
            gd.setCornerRadius(6f);
            h.teamColorStrip.setBackground(gd);
        } catch (Exception ignored) {}

        // Selection state
        h.card.setChecked(isSelected);
        h.card.setStrokeWidth(isSelected ? 3 : 0);
        h.card.setAlpha(isSelected ? 1f : 0.75f);
        h.tvCheckmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        h.card.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;

            boolean currentlySelected = selected.get(pos);

            if (!currentlySelected) {
                // Trying to select: check if we've hit the max
                if (countSelected() >= maxSelections) {
                    // If max is 1, just swap selection
                    if (maxSelections == 1) {
                        for (int i = 0; i < selected.size(); i++) selected.set(i, false);
                        notifyDataSetChanged();
                    } else {
                        return; // don't allow > max
                    }
                }
                selected.set(pos, true);
            } else {
                selected.set(pos, false);
            }

            notifyItemChanged(pos);
            listener.onSelectionChanged(countSelected());
        });
    }

    @Override
    public int getItemCount() { return drivers.size(); }

    public List<BettingFragment.Driver> getSelectedDrivers() {
        List<BettingFragment.Driver> result = new ArrayList<>();
        for (int i = 0; i < drivers.size(); i++) {
            if (selected.get(i)) result.add(drivers.get(i));
        }
        return result;
    }

    public void clearSelections() {
        for (int i = 0; i < selected.size(); i++) selected.set(i, false);
        notifyDataSetChanged();
        listener.onSelectionChanged(0);
    }

    private int countSelected() {
        int count = 0;
        for (Boolean b : selected) if (b) count++;
        return count;
    }

    static class DriverViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        View teamColorStrip;
        TextView tvCode, tvName, tvTeam, tvCheckmark;

        DriverViewHolder(View v) {
            super(v);
            card           = v.findViewById(R.id.card_driver);
            teamColorStrip = v.findViewById(R.id.team_color_strip);
            tvCode         = v.findViewById(R.id.tv_driver_code);
            tvName         = v.findViewById(R.id.tv_driver_name);
            tvTeam         = v.findViewById(R.id.tv_driver_team);
            tvCheckmark    = v.findViewById(R.id.tv_checkmark);
        }
    }
}