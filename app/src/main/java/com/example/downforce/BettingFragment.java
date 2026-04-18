package com.example.downforce;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BettingFragment extends Fragment {

    // ── State ──────────────────────────────────────────────────────────────
    private int userPoints = 1_000;
    private int lockedInPoints = 0; // points committed to active bets

    // ── Views ──────────────────────────────────────────────────────────────
    private TextView tvBalance;
    private TextView tvActiveBets;
    private TextView tvSelectedCount;

    // Race bet
    private DriverSelectionAdapter raceDriverAdapter;
    private TextInputEditText etRaceBetAmount;
    private MaterialButton btnPlaceRaceBet;
    private RecyclerView rvRaceDrivers;

    // Championship bet
    private DriverSelectionAdapter champDriverAdapter;
    private TextInputEditText etChampBetAmount;
    private MaterialButton btnPlaceChampBet;

    // My bets list
    private List<PlacedBet> activeBets = new ArrayList<>();
    private PlacedBetsAdapter betsAdapter;
    private RecyclerView rvActiveBets;
    private TextView tvNoBets;

    // ── Driver data (2026 grid) ────────────────────────────────────────────
    private static final List<Driver> ALL_DRIVERS = Arrays.asList(
            new Driver("Max Verstappen",    "VER", "Red Bull",          "#3671C6"),
            new Driver("Liam Lawson",       "LAW", "Red Bull",          "#3671C6"),
            new Driver("Lewis Hamilton",    "HAM", "Ferrari",           "#E8002D"),
            new Driver("Charles Leclerc",   "LEC", "Ferrari",           "#E8002D"),
            new Driver("George Russell",    "RUS", "Mercedes",          "#27F4D2"),
            new Driver("Kimi Antonelli",    "ANT", "Mercedes",          "#27F4D2"),
            new Driver("Fernando Alonso",   "ALO", "Aston Martin",      "#229971"),
            new Driver("Lance Stroll",      "STR", "Aston Martin",      "#229971"),
            new Driver("Lando Norris",      "NOR", "McLaren",           "#FF8000"),
            new Driver("Oscar Piastri",     "PIA", "McLaren",           "#FF8000"),
            new Driver("Carlos Sainz",      "SAI", "Williams",          "#64C4FF"),
            new Driver("Alexander Albon",   "ALB", "Williams",          "#64C4FF"),
            new Driver("Pierre Gasly",      "GAS", "Alpine",            "#FF87BC"),
            new Driver("Jack Doohan",       "DOO", "Alpine",            "#FF87BC"),
            new Driver("Nico Hülkenberg",   "HUL", "Sauber",            "#52E252"),
            new Driver("Gabriel Bortoleto", "BOR", "Sauber",            "#52E252"),
            new Driver("Yuki Tsunoda",      "TSU", "RB",                "#6692FF"),
            new Driver("Isack Hadjar",      "HAD", "RB",                "#6692FF"),
            new Driver("Esteban Ocon",      "OCO", "Haas",              "#B6BABD"),
            new Driver("Oliver Bearman",    "BEA", "Haas",              "#B6BABD")
    );

    private static final int MAX_RACE_SELECTIONS = 10;

    // ── Lifecycle ──────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_betting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupRaceBet(view);
        setupChampionshipBet(view);
        setupActiveBetsList(view);
        updateBalanceUI();
    }

    // ── View binding ───────────────────────────────────────────────────────
    private void bindViews(View v) {
        tvBalance    = v.findViewById(R.id.tv_balance);
        tvActiveBets = v.findViewById(R.id.tv_active_bets);
        tvSelectedCount = v.findViewById(R.id.tv_selected_count);
    }

    // ── Race Bet setup ─────────────────────────────────────────────────────
    private void setupRaceBet(View v) {
        rvRaceDrivers = v.findViewById(R.id.rv_race_drivers);
        etRaceBetAmount = v.findViewById(R.id.et_race_bet_amount);
        btnPlaceRaceBet = v.findViewById(R.id.btn_place_race_bet);

        raceDriverAdapter = new DriverSelectionAdapter(
                new ArrayList<>(ALL_DRIVERS),
                MAX_RACE_SELECTIONS,
                this::onRaceSelectionChanged
        );
        rvRaceDrivers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRaceDrivers.setAdapter(raceDriverAdapter);
        rvRaceDrivers.setNestedScrollingEnabled(false);

        btnPlaceRaceBet.setOnClickListener(x -> placeRaceBet());
    }

    private void onRaceSelectionChanged(int selectedCount) {
        tvSelectedCount.setText(selectedCount + " / " + MAX_RACE_SELECTIONS + " drivers selected");
        boolean allSelected = selectedCount == MAX_RACE_SELECTIONS;
        tvSelectedCount.setAlpha(allSelected ? 1f : 0.6f);
    }

    private void placeRaceBet() {
        List<Driver> selected = raceDriverAdapter.getSelectedDrivers();

        if (selected.size() != MAX_RACE_SELECTIONS) {
            toast("Select exactly 10 drivers for the Top 10 bet");
            return;
        }

        String amtStr = etRaceBetAmount.getText() != null
                ? etRaceBetAmount.getText().toString().trim() : "";
        if (amtStr.isEmpty()) { toast("Enter a point amount"); return; }

        int amount = Integer.parseInt(amtStr);
        if (amount <= 0) { toast("Enter a valid amount greater than 0"); return; }
        if (amount > availablePoints()) {
            toast("Not enough points! You have " + availablePoints() + " available"); return;
        }

        // Build a label of the selected drivers
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selected.size(); i++) {
            sb.append(selected.get(i).code);
            if (i < selected.size() - 1) sb.append(", ");
        }

        activeBets.add(new PlacedBet("RACE TOP 10", "Next Grand Prix", sb.toString(), amount));
        lockedInPoints += amount;

        betsAdapter.notifyItemInserted(activeBets.size() - 1);
        tvNoBets.setVisibility(View.GONE);

        raceDriverAdapter.clearSelections();
        etRaceBetAmount.setText("");
        updateBalanceUI();
        toast("Race bet placed! 🏎️");
    }

    // ── Championship Bet setup ─────────────────────────────────────────────
    private void setupChampionshipBet(View v) {
        RecyclerView rvChamp = v.findViewById(R.id.rv_champ_drivers);
        etChampBetAmount   = v.findViewById(R.id.et_champ_bet_amount);
        btnPlaceChampBet   = v.findViewById(R.id.btn_place_champ_bet);
        TextView tvChampSelected = v.findViewById(R.id.tv_champ_selected);

        champDriverAdapter = new DriverSelectionAdapter(
                new ArrayList<>(ALL_DRIVERS),
                1,
                count -> tvChampSelected.setText(
                        count == 1 ? "1 driver selected ✓" : "Select your champion"
                )
        );
        rvChamp.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChamp.setAdapter(champDriverAdapter);
        rvChamp.setNestedScrollingEnabled(false);

        btnPlaceChampBet.setOnClickListener(x -> placeChampBet());
    }

    private void placeChampBet() {
        List<Driver> selected = champDriverAdapter.getSelectedDrivers();
        if (selected.isEmpty()) { toast("Select a driver"); return; }

        String amtStr = etChampBetAmount.getText() != null
                ? etChampBetAmount.getText().toString().trim() : "";
        if (amtStr.isEmpty()) { toast("Enter a point amount"); return; }

        int amount = Integer.parseInt(amtStr);
        if (amount <= 0) { toast("Enter a valid amount greater than 0"); return; }
        if (amount > availablePoints()) {
            toast("Not enough points! You have " + availablePoints() + " available"); return;
        }

        Driver champion = selected.get(0);
        activeBets.add(new PlacedBet(
                "CHAMPIONSHIP", "2026 WDC Winner",
                champion.name + " (" + champion.code + ")", amount
        ));
        lockedInPoints += amount;

        betsAdapter.notifyItemInserted(activeBets.size() - 1);
        tvNoBets.setVisibility(View.GONE);

        champDriverAdapter.clearSelections();
        etChampBetAmount.setText("");
        updateBalanceUI();
        toast("Championship bet placed! 🏆");
    }

    // ── Active bets list ───────────────────────────────────────────────────
    private void setupActiveBetsList(View v) {
        rvActiveBets = v.findViewById(R.id.rv_active_bets);
        tvNoBets     = v.findViewById(R.id.tv_no_bets);

        betsAdapter = new PlacedBetsAdapter(activeBets, this::cancelBet);
        rvActiveBets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvActiveBets.setAdapter(betsAdapter);
        rvActiveBets.setNestedScrollingEnabled(false);
    }

    private void cancelBet(int position) {
        PlacedBet bet = activeBets.get(position);
        lockedInPoints -= bet.amount;
        activeBets.remove(position);
        betsAdapter.notifyItemRemoved(position);
        if (activeBets.isEmpty()) tvNoBets.setVisibility(View.VISIBLE);
        updateBalanceUI();
        toast("Bet cancelled — " + bet.amount + " pts returned");
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private int availablePoints() { return userPoints - lockedInPoints; }

    private void updateBalanceUI() {
        tvBalance.setText(availablePoints() + " pts");
        tvActiveBets.setText(activeBets.size() + " active");
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Inner model classes
    // ══════════════════════════════════════════════════════════════════════

    public static class Driver {
        public final String name, code, team, teamColor;
        public Driver(String name, String code, String team, String teamColor) {
            this.name = name; this.code = code;
            this.team = team; this.teamColor = teamColor;
        }
    }

    public static class PlacedBet {
        public final String type, subtitle, detail;
        public final int amount;
        public PlacedBet(String type, String subtitle, String detail, int amount) {
            this.type = type; this.subtitle = subtitle;
            this.detail = detail; this.amount = amount;
        }
    }
}