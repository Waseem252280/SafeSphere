package com.example.safesphere.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.safesphere.R;
import com.example.safesphere.activity.HomeActivity;
import com.example.safesphere.dto.UserDto;
import com.example.safesphere.services.CloudinaryService;
import com.example.safesphere.services.MyFirebaseService;
import com.example.safesphere.utils.ContactOperationsHelper;
import com.example.safesphere.utils.FirebaseUtil;
import com.example.safesphere.utils.LoaderUtil;
import com.example.safesphere.utils.SharedPrefferanceUtil;
import com.example.safesphere.utils.ThemeUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.squareup.picasso.Picasso;

import org.checkerframework.framework.qual.DefaultQualifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegistrationActivity extends AppCompatActivity {

    private String uploadedImageUrl;

    private Animation animateRegisterBtn;
    private AppCompatButton btnContinue, btnCameraGallery;
    private CircleImageView profileImage;
    private EditText etFullName;
    private AppCompatButton maleBtn, femaleBtn;
    private LoaderUtil loader;
    private String gender = null;

    private static final int REQUEST_PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        initViews();
        // Get data from intent and set into views
        setViewsData();
        // Setup click listeners
        clickListeners();
        //if user is logged in, go to home activity
       UserDto userDetails = SharedPrefferanceUtil.isUserLoggedIn(this);
       if (userDetails != null) {
           Intent intent = new Intent(RegistrationActivity.this, HomeActivity.class);
           startActivity(intent);
           finish();
       }
    }

    private void initViews() {
        btnContinue = findViewById(R.id.btnRegister);
        profileImage = findViewById(R.id.profile_image);
        etFullName = findViewById(R.id.etFullName);
        btnCameraGallery = findViewById(R.id.btnCameraGallery);
        maleBtn = findViewById(R.id.btnMale);
        femaleBtn = findViewById(R.id.btnFemale);
        loader = new LoaderUtil(this);

        // Init Cloudinary service
        CloudinaryService.initCloudinary(this);
    }

    private void setViewsData() {
        Intent intent = getIntent();
        etFullName.setText(intent.getStringExtra("name"));
        Picasso.get()
                .load(intent.getStringExtra("photoUrl"))
                .into(profileImage);
    }

    private void clickListeners() {
        // Open gallery
        btnCameraGallery.setOnClickListener(v -> openGallery());

        // Gender buttons
        maleBtn.setOnClickListener(v -> {
            gender = "Male";
            maleBtn.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
            femaleBtn.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorOnPrimary)));
        });

        femaleBtn.setOnClickListener(v -> {
            gender = "Female";
            femaleBtn.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
            maleBtn.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorOnPrimary)));
        });

        // Submit form
        btnContinue.setOnClickListener(v -> {
            if (isValidForm()) {
                loader.showLoader();
                // Convert ImageView → Bitmap
                BitmapDrawable drawable = (BitmapDrawable) profileImage.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                // Convert Bitmap → Uri (safe method)
                Uri imageUri = getImageUriSafe(getApplicationContext(), bitmap);

                if (imageUri != null) {
                    checkIfUserExists(imageUri);
                } else {
                    loader.stopLoader();
                    Toast.makeText(this, "Image conversion failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private boolean isValidForm() {
        if (profileImage.getDrawable() == null) {
            Toast.makeText(this, "Profile image is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etFullName.getText().toString().isEmpty()) {
            etFullName.setError("Name is required");
            return false;
        }
        if (gender == null) {
            Toast.makeText(this, "Gender is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void checkIfUserExists(Uri imageUri) {
        String email = getIntent().getStringExtra("email");
        FirebaseUtil.getCollection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            //if user exists in db then get data and set into object
                            List<UserDto> users = task.getResult().toObjects(UserDto.class);
                            //then save user data into shared preferences storage
                            UserDto user = users.get(0);
                            SharedPrefferanceUtil.saveUserDetails(user,RegistrationActivity.this);
                            // ✅ User already exists → Login
                            loader.stopLoader();
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, HomeActivity.class);
                            startActivity(intent);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            finish();
                        } else {
                            // ❌ User not found → Register new user
                            uploadImageAndRegister(imageUri);
                        }
                    } else {
                        loader.stopLoader();
                        Toast.makeText(this, "Error checking user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


//    private void uploadImageAndRegister(Uri imageUri) {
//        try {
//            File file = getFileFromUri(this, imageUri);
//            Log.d("Cloudinary", "Uploading file: " + file.getAbsolutePath());
//
//            CloudinaryService.uploadImage(file.getAbsolutePath())
//                    .continueWithTask(task -> {
//                        if (!task.isSuccessful()) {
//                            throw task.getException();
//                        }
//                        String userId = FirebaseUtil.getCurrentUser().getUid();
//                        String imageUrl = task.getResult();
//                        if (imageUrl!=null && imageUrl.startsWith("http://")){
//                            imageUrl = imageUrl.replace("http://","https://");
//                        }
//                        uploadedImageUrl = imageUrl;
//                        Intent intent = getIntent();
//                        UserDto user = new UserDto(
//                                userId, etFullName.getText().toString(),
//                                intent.getStringExtra("email"), imageUrl,
//                                gender, null,
//                                "online", null,
//                                null, null,
//                                null, 0,
//                                false, true
//                        );
//                        Log.d("Firestore", "Trying to save user: " + user.getFullName());
//
//                        return FirebaseUtil.set("users", FirebaseUtil.getCurrentUser().getUid(), user);
//                    })
//                    .continueWithTask(task -> {
//                        if (!task.isSuccessful()) {
//                            throw task.getException();
//                        }
//                        return MyFirebaseService.updateTokenManually();
//                    })
//                    .addOnSuccessListener(aVoid -> {
//                        loader.stopLoader();
//                        Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();
//                        // ✅ Save user details in SharedPreferences
//                        Intent intent = getIntent();
//                        String userId = FirebaseUtil.getCurrentUser().getUid();
//                        String email = intent.getStringExtra("email");
//                        UserDto user = new UserDto(
//                                userId,
//                                etFullName.getText().toString(),
//                                email,
//                                uploadedImageUrl, // or use uploaded image URL variable
//                                gender,
//                                null,
//                                "online",
//                                null,
//                                null,
//                                null,
//                                null,
//                                0,
//                                false,
//                                true
//                        );
//                        SharedPrefferanceUtil.saveUserDetails(user, RegistrationActivity.this);
//                        // ✅ Redirect to HomeActivity
//                        Intent homeIntent = new Intent(this, HomeActivity.class);
//                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(homeIntent);
//                        finish();
//                    })
//                    .addOnFailureListener(e -> {
//                        loader.stopLoader();
//                        Log.e("Register", "Error: ", e);
//                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    });
//
//        } catch (IOException e) {
//            loader.stopLoader();
//            Log.e("File", "File conversion error", e);
//            Toast.makeText(this, "File conversion error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//    }

    private void uploadImageAndRegister(Uri imageUri) {
        try {
            File file = getFileFromUri(this, imageUri);
            Log.d("Cloudinary", "Uploading file: " + file.getAbsolutePath());

            CloudinaryService.uploadImage(file.getAbsolutePath())
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        String userId = FirebaseUtil.getCurrentUser().getUid();
                        String imageUrl = task.getResult();
                        if (imageUrl!=null && imageUrl.startsWith("http://")){
                            imageUrl = imageUrl.replace("http://","https://");
                        }
                        uploadedImageUrl = imageUrl;
                        Intent intent = getIntent();
                        UserDto user = new UserDto(
                                userId, etFullName.getText().toString(),
                                intent.getStringExtra("email"), imageUrl,
                                gender, null,
                                "online", null,
                                null, null,
                                null, 0,
                                false,
                                true,
                                null
                        );
                        Log.d("Firestore", "Trying to save user: " + user.getFullName());

                        return FirebaseUtil.set("users", FirebaseUtil.getCurrentUser().getUid(), user);
                    })// uploadImageAndRegister method ke Firestore save block ke baad:
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();

                        // ✅ Step: Naye user ka status node create karein
                        String uid = FirebaseUtil.getCurrentUser().getUid();
                        Map<String, Object> statusMap = new HashMap<>();
                        statusMap.put("loggedIn", true);
                        statusMap.put("online", true);
                        statusMap.put("lastSeen", ServerValue.TIMESTAMP);

                        return FirebaseDatabase.getInstance().getReference("status")
                                .child(uid)
                                .setValue(statusMap);
                    })
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return MyFirebaseService.updateTokenManually();
                    })
                    .addOnSuccessListener(aVoid -> {
                        loader.stopLoader();
                        Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();
                        // ✅ Save user details in SharedPreferences
                        Intent intent = getIntent();
                        String userId = FirebaseUtil.getCurrentUser().getUid();
                        String email = intent.getStringExtra("email");
                        UserDto user = new UserDto(
                                userId,
                                etFullName.getText().toString(),
                                email,
                                uploadedImageUrl, // or use uploaded image URL variable
                                gender,
                                null,
                                "online",
                                null,
                                null,
                                null,
                                null,
                                0,
                                false,
                                true,
                                null
                        );
                        SharedPrefferanceUtil.saveUserDetails(user, RegistrationActivity.this);
                        // ✅ Redirect to HomeActivity
//                        Intent homeIntent = new Intent(this, HomeActivity.class);
//                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(homeIntent);
//                        finish();
                        // ✅ SYNC TRUSTED CONTACTS (New Code)
                        // Naya user hai toh list null hogi, lekin sync call karne se local storage initialize ho jayegi
                        new ContactOperationsHelper(this).syncFromServer(success -> {
                            loader.stopLoader();
                            Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();

                            Intent homeIntent = new Intent(this, HomeActivity.class);
                            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(homeIntent);
                            finish();
                        });
                    })
                    .addOnFailureListener(e -> {
                        loader.stopLoader();
                        Log.e("Register", "Error: ", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            loader.stopLoader();
            Log.e("File", "File conversion error", e);
            Toast.makeText(this, "File conversion error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    // ✅ Safe method: Convert Bitmap → Uri using temp file
    private Uri getImageUriSafe(Context context, Bitmap bitmap) {
        try {
            File tempFile = File.createTempFile("profile", ".jpg", context.getCacheDir());
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
            return Uri.fromFile(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Convert Uri → Temp File
    private File getFileFromUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("upload", ".jpg", context.getCacheDir());
        FileOutputStream out = new FileOutputStream(tempFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        inputStream.close();
        return tempFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
    }
}
