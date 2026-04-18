package com.example.downforce;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PlacedBetsAdapter extends RecyclerView.Adapter<PlacedBetsAdapter.BetViewHolder> {

    public interface CancelListener { void onCancel(int position); }

    private final List<BettingFragment.PlacedBet> bets;
    private final CancelListener cancelListener;

    public PlacedBetsAdapter(List<BettingFragment.PlacedBet> bets, CancelListener cancelListener) {
        this.bets = bets;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public BetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_placed_bet, parent, false);
        return new BetViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BetViewHolder h, int position) {
        BettingFragment.PlacedBet bet = bets.get(position);
        h.tvType.setText(bet.type);
        h.tvSubtitle.setText(bet.subtitle);
        h.tvDetail.setText(bet.detail);
        h.tvAmount.setText("🪙 " + bet.amount + " pts");
        h.btnCancel.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) cancelListener.onCancel(pos);
        });
    }

    @Override
    public int getItemCount() { return bets.size(); }

    static class BetViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvSubtitle, tvDetail, tvAmount;
        MaterialButton btnCancel;

        BetViewHolder(View v) {
            super(v);
            tvType     = v.findViewById(R.id.tv_bet_type);
            tvSubtitle = v.findViewById(R.id.tv_bet_subtitle);
            tvDetail   = v.findViewById(R.id.tv_bet_detail);
            tvAmount   = v.findViewById(R.id.tv_bet_amount);
            btnCancel  = v.findViewById(R.id.btn_cancel_bet);
        }
    }
}