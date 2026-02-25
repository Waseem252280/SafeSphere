
//package com.example.safesphere.auth;
//
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.view.animation.Animation;
//import android.view.animation.AnimationUtils;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.AppCompatButton;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.airbnb.lottie.LottieAnimationView;
//import com.example.safesphere.R;
//import com.example.safesphere.activity.HomeActivity;
//import com.example.safesphere.dto.UserDto;
//import com.example.safesphere.utils.FirebaseUtil;
//import com.example.safesphere.utils.LoaderUtil;
//import com.example.safesphere.utils.SharedPrefferanceUtil;
//import com.facebook.CallbackManager;
//import com.google.firebase.auth.FirebaseUser;
//
//public class AuthActivity extends AppCompatActivity {
//
//    private AppCompatButton btnGoogleSignIn;
//    private AppCompatButton btnFacebookSignIn;
//    private LoaderUtil loaderDialog;
//    private Animation googleBtnAnimation, facebookBtnAnimation;
//    private CallbackManager fbCallbackManager; // facebook ka callback manager
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_auth);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Initialize views
//        initViews();
//        animate();
//        clickListeners();
//        //if user is logged in, go to home activity
//        UserDto userDetails = SharedPrefferanceUtil.isUserLoggedIn(this);
//        if (userDetails != null) {
//            Intent intent = new Intent(this, HomeActivity.class);
//            startActivity(intent);
//            finish();
//        }
//
//        // play lottie animation
//        LottieAnimationView animationView = findViewById(R.id.lottieAnimationView);
//        animationView.playAnimation();
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        // GOOGLE
//        if (requestCode == FirebaseUtil.RC_SIGN_IN) {
//            FirebaseUtil.handleSignInResult(this, data, new FirebaseUtil.FirebaseAuthCallback() {
//                @Override
//                public void onSuccess(FirebaseUser user) {
//                    checkIfUserExists(user);
//                }
//
//                @Override
//                public void onFailure(Exception e) {
//                    Toast.makeText(AuthActivity.this, "Auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    loaderDialog.stopLoader();
//                }
//            });
//        }
//
//        // FACEBOOK
//        if (fbCallbackManager != null) {
//            fbCallbackManager.onActivityResult(requestCode, resultCode, data);
//        }
//    }
//
//    public void initViews() {
//        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
//        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn);
//        googleBtnAnimation = AnimationUtils.loadAnimation(this, R.anim.google_button_scale);
//        facebookBtnAnimation = AnimationUtils.loadAnimation(this, R.anim.facebook_button_scale);
//        loaderDialog = new LoaderUtil(this);
//
//
//        // facebook init
//        fbCallbackManager = FirebaseUtil.initFacebookLogin();
//
//        // firebase auth initialization
//        FirebaseUtil.initGoogleSignIn(this);
//    }
//
//    public void animate() {
//        btnGoogleSignIn.startAnimation(googleBtnAnimation);
//        btnFacebookSignIn.startAnimation(facebookBtnAnimation);
//    }
//
//    public void clickListeners() {
//        // Google sign in button click
//        btnGoogleSignIn.setOnClickListener(v -> {
//            loaderDialog.showLoader();
//            FirebaseUtil.signInWithGoogle(this);
//        });
//
//        // Facebook sign in button click
//        btnFacebookSignIn.setOnClickListener(v -> {
//            loaderDialog.showLoader();
//            FirebaseUtil.signInWithFacebook(this, new FirebaseUtil.FirebaseAuthCallback() {
//                @Override
//                public void onSuccess(FirebaseUser user) {
//                    goToRegistration(user);
//                }
//
//                @Override
//                public void onFailure(Exception e) {
//                    Toast.makeText(AuthActivity.this, "Facebook login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    loaderDialog.stopLoader();
//                }
//            });
//        });
//    }
//
//
//    public void checkIfUserExists(FirebaseUser user) {
//        String email = FirebaseUtil.getCurrentUser().getEmail();
//        FirebaseUtil.getCollection("users")
//                .whereEqualTo("email", email)
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        if (!task.getResult().isEmpty()) {
//                            // ✅ User already exists → Login
//                            loaderDialog.stopLoader();
//                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
//                            Intent intent = new Intent(this, HomeActivity.class);
//                            startActivity(intent);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//                            finish();
//                        } else {
//                            // ❌ User not found → Register new user
//                            goToRegistration(user);
//                        }
//                    } else {
//                        loaderDialog.stopLoader();
//                        Toast.makeText(this, "Error checking user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
//
//
//    private void goToRegistration(FirebaseUser user) {
//        Toast.makeText(AuthActivity.this, "Welcome " + user.getEmail(), Toast.LENGTH_SHORT).show();
//        Intent intent = new Intent(AuthActivity.this, RegistrationActivity.class);
//        intent.putExtra("email", user.getEmail());
//        intent.putExtra("name", user.getDisplayName());
//        if (user.getPhotoUrl() != null) {
//            intent.putExtra("photoUrl", user.getPhotoUrl().toString());
//        }
//        loaderDialog.stopLoader();
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//    }
//}

package com.example.safesphere.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.example.safesphere.R;
import com.example.safesphere.activity.HomeActivity;
import com.example.safesphere.dto.UserDto;
import com.example.safesphere.utils.ContactOperationsHelper;
import com.example.safesphere.utils.FirebaseUtil;
import com.example.safesphere.utils.LoaderUtil;
import com.example.safesphere.utils.SharedPrefferanceUtil;
import com.example.safesphere.utils.ThemeUtils;
import com.facebook.CallbackManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private AppCompatButton btnGoogleSignIn;
    private AppCompatButton btnFacebookSignIn;
    private LoaderUtil loaderDialog;
    private Animation googleBtnAnimation, facebookBtnAnimation;
    private CallbackManager fbCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        animate();
        clickListeners();

        // ✅ If user is already logged in locally → Go directly to Home
        UserDto userDetails = SharedPrefferanceUtil.isUserLoggedIn(this);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }

        // Lottie animation
        LottieAnimationView animationView = findViewById(R.id.lottieAnimationView);
        animationView.playAnimation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // GOOGLE sign-in result
        if (requestCode == FirebaseUtil.RC_SIGN_IN) {
            FirebaseUtil.handleSignInResult(this, data, new FirebaseUtil.FirebaseAuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    checkIfUserExists(user);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(AuthActivity.this, "Auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loaderDialog.stopLoader();
                }
            });
        }

        // FACEBOOK result
        if (fbCallbackManager != null) {
            fbCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initViews() {
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
//        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn);
        googleBtnAnimation = AnimationUtils.loadAnimation(this, R.anim.google_button_scale);
        facebookBtnAnimation = AnimationUtils.loadAnimation(this, R.anim.facebook_button_scale);
        loaderDialog = new LoaderUtil(this);

        fbCallbackManager = FirebaseUtil.initFacebookLogin();
        FirebaseUtil.initGoogleSignIn(this);
    }

    private void animate() {
        btnGoogleSignIn.startAnimation(googleBtnAnimation);
//        btnFacebookSignIn.startAnimation(facebookBtnAnimation);
    }

    private void clickListeners() {
        // Google
        btnGoogleSignIn.setOnClickListener(v -> {
            loaderDialog.showLoader();
            FirebaseUtil.signInWithGoogle(this);
        });

        // Facebook
//        btnFacebookSignIn.setOnClickListener(v -> {
//            loaderDialog.showLoader();
//            FirebaseUtil.signInWithFacebook(this, new FirebaseUtil.FirebaseAuthCallback() {
//                @Override
//                public void onSuccess(FirebaseUser user) {
//                    checkIfUserExists(user);
//                }
//
//                @Override
//                public void onFailure(Exception e) {
//                    loaderDialog.stopLoader();
//                    Toast.makeText(AuthActivity.this, "Facebook login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                }
//            });
//        });
    }

    // ✅ Check user existence in Firestore
//    public void checkIfUserExists(FirebaseUser user) {
//        String email = user.getEmail();
//
//        FirebaseUtil.getCollection("users")
//                .whereEqualTo("email", email)
//                .get()
//                .addOnCompleteListener(task -> {
//                    loaderDialog.stopLoader();
//
//                    if (task.isSuccessful()) {
//                        if (!task.getResult().isEmpty()) {
//                            // ✅ User already exists in DB → Save to SharedPreferences
//                            List<UserDto> users = task.getResult().toObjects(UserDto.class);
//                            UserDto existingUser = users.get(0);
//
//                            SharedPrefferanceUtil.saveUserDetails(existingUser, AuthActivity.this);
//                            Toast.makeText(this, "Welcome back " + existingUser.getFullName(), Toast.LENGTH_SHORT).show();
//
//                            Intent intent = new Intent(this, HomeActivity.class);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(intent);
//                            finish();
//
//                        } else {
//                            // ❌ New user → Go to Registration Activity
//                            goToRegistration(user);
//                        }
//                    } else {
//                        Toast.makeText(this, "Error checking user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }




    // AuthActivity.java ke checkIfUserExists method mein update:
    public void checkIfUserExists(FirebaseUser user) {
        String email = user.getEmail();
        String uid = user.getUid(); // UID lein

        FirebaseUtil.getCollection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    loaderDialog.stopLoader();
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            List<UserDto> users = task.getResult().toObjects(UserDto.class);
                            UserDto existingUser = users.get(0);

                            // ✅ PROFESSIONAL FIX: Home par janay se pehle status update karein
                            DatabaseReference statusRef = FirebaseDatabase.getInstance()
                                    .getReference("status")
                                    .child(uid);

                            Map<String, Object> statusMap = new HashMap<>();
                            statusMap.put("loggedIn", true);
                            statusMap.put("online", true);

                            statusRef.updateChildren(statusMap);

                            SharedPrefferanceUtil.saveUserDetails(existingUser, AuthActivity.this);

//                            Intent intent = new Intent(this, HomeActivity.class);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(intent);
//                            finish();
                            // ✅ 3. SYNC TRUSTED CONTACTS (New Code)
                            new ContactOperationsHelper(this).syncFromServer(success -> {
                                loaderDialog.stopLoader(); // Sync ke baad loader stop karein
                                Intent intent = new Intent(this, HomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            loaderDialog.stopLoader();
                            goToRegistration(user);
                        }
                    }
                });
    }

    private void goToRegistration(FirebaseUser user) {
        Intent intent = new Intent(AuthActivity.this, RegistrationActivity.class);
        intent.putExtra("email", user.getEmail());
        intent.putExtra("name", user.getDisplayName());
        if (user.getPhotoUrl() != null) {
            intent.putExtra("photoUrl", user.getPhotoUrl().toString());
        }
        startActivity(intent);
        finish();
    }
}
