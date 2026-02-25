package com.example.safesphere.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.safesphere.R;
import com.example.safesphere.dto.UserDto;
import com.example.safesphere.services.CloudinaryService;
import com.example.safesphere.utils.FirebaseUtil;
import com.example.safesphere.utils.LoaderUtil;
import com.example.safesphere.utils.SharedPrefferanceUtil;
import com.example.safesphere.utils.ThemeUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class UpdateProfileFragment extends Fragment {

    private String photoUrl;
    private CircleImageView profileImage;
    private EditText etFullName;
    private AppCompatButton btnCameraGallery, btnUpdate, btnCancel, maleBtn, femaleBtn;
    private LoaderUtil loader;
    private String gender = null;
    private static final int REQUEST_PICK_IMAGE = 101;
    private Uri selectedImageUri;
    private Context context;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_profile, container, false);
        context = requireContext();

        initViews(view);
        setExistingData();
        setListeners();

        return view;
    }

    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profile_image);
        etFullName = view.findViewById(R.id.etFullName);
        btnCameraGallery = view.findViewById(R.id.btnCameraGallery);
        btnUpdate = view.findViewById(R.id.btnRegister);
        btnCancel = view.findViewById(R.id.btnCancel);
        maleBtn = view.findViewById(R.id.btnMale);
        femaleBtn = view.findViewById(R.id.btnFemale);
        loader = new LoaderUtil(context);

        CloudinaryService.initCloudinary(context);
    }

    private void setExistingData() {
        UserDto user = SharedPrefferanceUtil.isUserLoggedIn(context);
        if (user != null) {
            etFullName.setText(user.getFullName());
            gender = user.getGender();

            if (gender != null) {
                if (gender.equalsIgnoreCase("Male")) {
                    TypedValue typedValuePrimary = new TypedValue();
                    context.getTheme().resolveAttribute(
                            com.google.android.material.R.attr.colorPrimary,
                            typedValuePrimary,
                            true
                    );
                    int colorPrimary = typedValuePrimary.data;
                    TypedValue typedValueOnPrimary = new TypedValue();
                    context.getTheme().resolveAttribute(
                            com.google.android.material.R.attr.colorOnPrimary,
                            typedValueOnPrimary,
                            true
                    );
                    maleBtn.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                } else {
                    TypedValue typedValuePrimary = new TypedValue();
                    context.getTheme().resolveAttribute(
                            com.google.android.material.R.attr.colorPrimary,
                            typedValuePrimary,
                            true
                    );
                    int colorPrimary = typedValuePrimary.data;
                    TypedValue typedValueOnPrimary = new TypedValue();
                    context.getTheme().resolveAttribute(
                            com.google.android.material.R.attr.colorOnPrimary,
                            typedValueOnPrimary,
                            true
                    );
                    femaleBtn.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                }
            }

            if (user.getPhotoUrl() != null) {
                Picasso.get().load(user.getPhotoUrl()).into(profileImage);
            }
        }
    }

    private void setListeners() {
        btnCameraGallery.setOnClickListener(v -> openGallery());

        femaleBtn.setOnClickListener(v -> {
            gender = "Female";
            TypedValue typedValuePrimary = new TypedValue();
            context.getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorPrimary,
                    typedValuePrimary,
                    true
            );
            int colorPrimary = typedValuePrimary.data;
            TypedValue typedValueOnPrimary = new TypedValue();
            context.getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorOnPrimary,
                    typedValueOnPrimary,
                    true
            );
            int colorOnPrimary = typedValueOnPrimary.data;
            femaleBtn.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
            maleBtn.setBackgroundTintList(ColorStateList.valueOf(colorOnPrimary));
        });

        maleBtn.setOnClickListener(v -> {
            gender = "Male";
            TypedValue typedValuePrimary = new TypedValue();
            context.getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorPrimary,
                    typedValuePrimary,
                    true
            );
            int colorPrimary = typedValuePrimary.data;
            TypedValue typedValueOnPrimary = new TypedValue();
            context.getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorOnPrimary,
                    typedValueOnPrimary,
                    true
            );
            int colorOnPrimary = typedValueOnPrimary.data;
            maleBtn.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
            femaleBtn.setBackgroundTintList(ColorStateList.valueOf(colorOnPrimary));
        });

        btnUpdate.setOnClickListener(v -> {
            if (isValidForm()) {
                loader.showLoader();
                BitmapDrawable drawable = (BitmapDrawable) profileImage.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                Uri imageUri = getImageUriSafe(context, bitmap);

                if (imageUri != null) {
                    uploadImageAndUpdate(imageUri);
                } else {
                    loader.stopLoader();
                    Toast.makeText(context, "Image conversion failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ProfileFragment())
                .commit()
        );
    }

    private boolean isValidForm() {
        if (profileImage.getDrawable() == null) {
            Toast.makeText(context, "Profile image is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etFullName.getText().toString().trim().isEmpty()) {
            etFullName.setError("Name is required");
            return false;
        }
        if (gender == null) {
            Toast.makeText(context, "Gender is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void uploadImageAndUpdate(Uri imageUri) {
        try {
            File file = getFileFromUri(context, imageUri);
            Log.d("Cloudinary", "Uploading file: " + file.getAbsolutePath());

            CloudinaryService.uploadImage(file.getAbsolutePath())
                    .addOnSuccessListener(url -> {
                        photoUrl = url;
                        if (url != null && url.startsWith("http://")) {
                            photoUrl = url.replace("http://", "https://");
                        }

                        String userId = FirebaseUtil.getCurrentUser().getUid();

                        FirebaseUtil.getDocumentReference("users", userId)
                                .update("fullName", etFullName.getText().toString(),
                                        "photoUrl", photoUrl,
                                        "gender", gender)
                                .addOnSuccessListener(aVoid -> {
                                    loader.stopLoader();
                                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                                    // Update shared preferences
                                    UserDto updatedUser = SharedPrefferanceUtil.isUserLoggedIn(context);
                                    if (updatedUser != null) {
                                        updatedUser.setFullName(etFullName.getText().toString());
                                        updatedUser.setPhotoUrl(photoUrl);
                                        updatedUser.setGender(gender);
                                        SharedPrefferanceUtil.saveUserDetails(updatedUser, context);
                                        requireActivity().getSupportFragmentManager()
                                                .beginTransaction()
                                                .replace(R.id.fragment_container, new ProfileFragment())
                                                .commit();
                                    }

                                })
                                .addOnFailureListener(e -> {
                                    loader.stopLoader();
                                    Toast.makeText(context, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                    })
                    .addOnFailureListener(e -> {
                        loader.stopLoader();
                        Toast.makeText(context, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            loader.stopLoader();
            Log.e("File", "File conversion error", e);
            Toast.makeText(context, "File conversion error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Uri getImageUriSafe(Context context, Bitmap bitmap) {
        try {
            File tempFile = File.createTempFile("profile_update", ".jpg", context.getCacheDir());
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
            return Uri.fromFile(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private File getFileFromUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("upload_update", ".jpg", context.getCacheDir());
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImage.setImageURI(selectedImageUri);
        }
    }
}
