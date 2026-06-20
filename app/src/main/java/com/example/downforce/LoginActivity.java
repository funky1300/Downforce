package com.example.downforce;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;
import com.google.firebase.auth.FirebaseAuth;

/**
 * LoginActivity — the app's launcher screen.
 *
 * PURPOSE (why): the entry gate. Authenticates existing users and keeps them
 * logged in between sessions, so they don't have to sign in every time.
 *
 * HOW (how): Firebase Auth email/password. onCreate checks getCurrentUser();
 * if already signed in it skips straight to MainActivity. The register flow uses
 * an ActivityResultLauncher (new format) — when RegisterActivity returns RESULT_OK
 * we move on to Main.
 *
 * Bagrut: Firebase Auth (req 7) + ActivityResultLauncher new format (req 6).
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etEmail, etPassword;
    private Button btnLogin;

    // ActivityResultLauncher (new format): opens RegisterActivity and listens for
    // its result. If registration succeeded (RESULT_OK) we go straight to Main.
    private final ActivityResultLauncher<Intent> registerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    goToMain();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v ->
                registerLauncher.launch(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> goToMain())
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
