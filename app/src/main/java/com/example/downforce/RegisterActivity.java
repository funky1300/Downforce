package com.example.downforce;

import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.widget.EditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ImageView ivProfilePic;
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private Uri selectedImageUri;
    private Uri cameraUri;

    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success != null && success) {
                    selectedImageUri = cameraUri;
                    ivProfilePic.setImageURI(selectedImageUri);
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivProfilePic.setImageURI(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        ivProfilePic = findViewById(R.id.ivProfilePic);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        LinearLayout layoutProfilePic = findViewById(R.id.layoutProfilePic);

        layoutProfilePic.setOnClickListener(v -> showImagePickerDialog());
        btnRegister.setOnClickListener(v -> register());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Create Account");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Profile Picture")
                .setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        cameraPermLauncher.launch(Manifest.permission.CAMERA);
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void launchCamera() {
        try {
            File photoFile = File.createTempFile("profile_", ".jpg", getCacheDir());
            cameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(cameraUri);
        } catch (IOException e) {
            Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void register() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) return;
                    if (selectedImageUri != null) {
                        uploadPhotoAndSave(user, name, email);
                    } else {
                        saveUserToFirestore(user, name, email, null);
                    }
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadPhotoAndSave(FirebaseUser user, String name, String email) {
        StorageReference ref = storage.getReference()
                .child("profile_pictures/" + user.getUid() + ".jpg");

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> saveUserToFirestore(user, name, email, uri.toString()))
                .addOnFailureListener(e -> saveUserToFirestore(user, name, email, null));
    }

    private void saveUserToFirestore(FirebaseUser user, String name, String email, String photoUrl) {
        UserProfileChangeRequest.Builder profileBuilder =
                new UserProfileChangeRequest.Builder().setDisplayName(name);
        if (photoUrl != null) profileBuilder.setPhotoUri(Uri.parse(photoUrl));
        user.updateProfile(profileBuilder.build());

        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("uid", user.getUid());
        userDoc.put("displayName", name);
        userDoc.put("email", email);
        userDoc.put("photoUrl", photoUrl != null ? photoUrl : "");
        userDoc.put("points", 0);
        userDoc.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(user.getUid()).set(userDoc)
                .addOnCompleteListener(task -> {
                    // Navigate regardless — Firebase Auth account was already created
                    setResult(RESULT_OK);
                    finish();
                });
    }
}
