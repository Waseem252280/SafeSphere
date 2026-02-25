package com.example.safesphere.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
// Add these imports at the top

import java.util.List;


import java.util.Arrays;

public class FirebaseUtil {

    private static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static GoogleSignInClient mGoogleSignInClient;
    private static CallbackManager mCallbackManager;  // facebook ke liye
    public static final int RC_SIGN_IN = 9001;

    // ---------------- FIRESTORE CRUD -----------------
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // CREATE
    public static <T> Task<Void> set(String collectionName, String documentId, T data) {
        return db.collection(collectionName)
                .document(documentId)
                .set(data);
    }


    // READ ALL
    public static <T> Task<List<T>> getAll(String collectionName, Class<T> clazz) {
        return db.collection(collectionName)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObjects(clazz);
                    }
                    return null;
                });
    }

    // READ BY ID
    public static <T> Task<T> getById(String collectionName, String documentId, Class<T> clazz) {
        return db.collection(collectionName)
                .document(documentId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        return task.getResult().toObject(clazz);
                    }
                    return null;
                });
    }

    // UPDATE (replace document)
    public static <T> Task<Void> update(String collectionName, String documentId, T data) {
        return db.collection(collectionName)
                .document(documentId)
                .set(data);
    }

    // DELETE
    public static Task<Void> delete(String collectionName, String documentId) {
        return db.collection(collectionName)
                .document(documentId)
                .delete();
    }

    // Get Collection Reference (optional helper)
    public static CollectionReference getCollection(String collectionName) {
        return db.collection(collectionName);
    }

    // Get single document reference
    public static DocumentReference getDocumentReference(String collectionName, String documentId) {
        return db.collection(collectionName).document(documentId);
    }


    // ---------------- GOOGLE -----------------
    public static void initGoogleSignIn(Activity activity) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(com.example.safesphere.R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public static void signInWithGoogle(Activity activity) {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public static void handleSignInResult(Activity activity, Intent data, FirebaseAuthCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(activity, account.getIdToken(), callback);
        } catch (ApiException e) {
            Toast.makeText(activity, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            callback.onFailure(e);
        }
    }

    private static void firebaseAuthWithGoogle(Activity activity, String idToken, FirebaseAuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // ---------------- FACEBOOK -----------------
    public static CallbackManager initFacebookLogin() {
        if (mCallbackManager == null) {
            mCallbackManager = CallbackManager.Factory.create();
        }
        return mCallbackManager;
    }

    public static void signInWithFacebook(Activity activity, FirebaseAuthCallback callback) {
        if (mCallbackManager == null) {
            mCallbackManager = CallbackManager.Factory.create();
        }

        LoginManager.getInstance().logInWithReadPermissions(
                activity,
                Arrays.asList("email", "public_profile")
        );

        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handleFacebookAccessToken(loginResult.getAccessToken(), callback);
                    }

                    @Override
                    public void onCancel() {
                        callback.onFailure(new Exception("Facebook login cancelled"));
                    }

                    @Override
                    public void onError(FacebookException error) {
                        callback.onFailure(error);
                    }
                });
    }

    private static void handleFacebookAccessToken(AccessToken token, FirebaseAuthCallback callback) {
        Log.d("FirebaseUtil", "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // ---------------- COMMON -----------------
    public static void signOut(Activity activity) {
        mAuth.signOut();
        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut();
        }
        SharedPrefferanceUtil.clearUserData(activity);
        Toast.makeText(activity, "Signed out", Toast.LENGTH_SHORT).show();
    }


    public static FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public interface FirebaseAuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }
}
