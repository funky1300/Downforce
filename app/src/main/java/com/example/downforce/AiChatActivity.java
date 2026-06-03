package com.example.downforce;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AiChatActivity extends AppCompatActivity {

    private static final String GROQ_API_KEY = ""; // get free key at console.groq.com
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private EditText editQuestion;
    private Button btnSend;
    private ProgressBar progressBar;
    private LinearLayout responseCard;
    private TextView textResponse;
    private RequestQueue requestQueue;

    //do not touch this function!!!!
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //^^^do not touch this function^^^

        editQuestion = findViewById(R.id.edit_question);
        btnSend = findViewById(R.id.btn_send);
        progressBar = findViewById(R.id.progress_bar);
        responseCard = findViewById(R.id.response_card);
        textResponse = findViewById(R.id.text_response);

        requestQueue = Volley.newRequestQueue(this);

        btnSend.setOnClickListener(v -> {
            String question = editQuestion.getText().toString().trim();
            if (question.isEmpty()) {
                Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show();
                return;
            }
            askGemini(question);
        });
    }

    private void askGemini(String question) {
        btnSend.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        responseCard.setVisibility(View.GONE);

        // Fetch live F1 context first, then ask the AI
        fetchF1ContextAndAsk(question);
    }

    private void fetchF1ContextAndAsk(String question) {
        String url = "https://api.jolpi.ca/ergast/f1/2026/last/results.json";
        JsonObjectRequest contextRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    String context = buildContext(response);
                    sendToGroq(question, context);
                },
                error -> sendToGroq(question, ""));
        requestQueue.add(contextRequest);
    }

    private String buildContext(JSONObject response) {
        StringBuilder sb = new StringBuilder();
        try {
            JSONArray races = response
                    .getJSONObject("MRData")
                    .getJSONObject("RaceTable")
                    .getJSONArray("Races");

            if (races.length() > 0) {
                JSONObject race = races.getJSONObject(0);
                sb.append("Latest 2026 F1 race: ").append(race.optString("raceName"))
                  .append(" on ").append(race.optString("date")).append(".\n");
                sb.append("Top 3 results: ");

                JSONArray results = race.getJSONArray("Results");
                for (int i = 0; i < Math.min(3, results.length()); i++) {
                    JSONObject r = results.getJSONObject(i);
                    JSONObject driver = r.getJSONObject("Driver");
                    String name = driver.optString("givenName") + " " + driver.optString("familyName");
                    sb.append("P").append(i + 1).append(" ").append(name);
                    if (i < 2) sb.append(", ");
                }
                sb.append(".\n");
            }
        } catch (JSONException ignored) {}
        return sb.toString();
    }

    private void sendToGroq(String question, String f1Context) {
        JSONObject requestBody = new JSONObject();
        try {
            String systemContent = "You are an F1 expert. Answer briefly and accurately. " +
                    "Today is 2026. Use this live data if relevant:\n" + f1Context;

            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemContent);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", question);

            JSONArray messages = new JSONArray();
            messages.put(systemMsg);
            messages.put(userMsg);

            requestBody.put("model", "llama-3.1-8b-instant");
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 500);
        } catch (JSONException e) {
            showError();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                GROQ_URL,
                requestBody,
                response -> {
                    try {
                        String answer = response
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        textResponse.setText(answer);
                        responseCard.setVisibility(View.VISIBLE);
                    } catch (JSONException e) {
                        showError();
                    }
                    btnSend.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    String msg = "Request failed.";
                    if (error.networkResponse != null) {
                        msg += " HTTP " + error.networkResponse.statusCode;
                        try {
                            msg += ": " + new String(error.networkResponse.data);
                        } catch (Exception ignored) {}
                    } else if (error.getMessage() != null) {
                        msg += " " + error.getMessage();
                    }
                    textResponse.setText(msg);
                    responseCard.setVisibility(View.VISIBLE);
                    btnSend.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Authorization", "Bearer " + GROQ_API_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void showError() {
        Toast.makeText(this, "Failed to get a response. Check your connection.", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
        btnSend.setEnabled(true);
    }
}
