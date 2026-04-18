package com.example.downforce;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.List;

public class RaceAdapter extends RecyclerView.Adapter<RaceAdapter.RaceViewHolder> {

    private List<Race> races;

    public RaceAdapter(List<Race> races) {
        this.races = races;
    }


    @Override
    public RaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_race, parent, false);
        return new RaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder( RaceViewHolder holder, int position) {
        Race race = races.get(position);
        holder.raceName.setText(race.getName());
        holder.raceDate.setText(race.getDate() + " - " + race.getLocation());

        
        if (race.getFlag() != null && !race.getFlag().isEmpty()) {
            Picasso.get().load(race.getFlag()).into(holder.flagImage);
        }
    }

    @Override
    public int getItemCount() {
        return races.size();
    }

    static class RaceViewHolder extends RecyclerView.ViewHolder {
        TextView raceName, raceDate;
        ImageView flagImage;

        public RaceViewHolder( View itemView) {
            super(itemView);
            raceName = itemView.findViewById(R.id.item_race_name);
            raceDate = itemView.findViewById(R.id.item_race_date);
            flagImage = itemView.findViewById(R.id.item_flag);
        }
    }
}
