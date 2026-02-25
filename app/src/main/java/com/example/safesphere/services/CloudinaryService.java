package com.example.safesphere.services;
import android.content.Context;
import android.widget.ImageView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryService {

    private static boolean isInitialized = false;

    // 🔹 Initialize Cloudinary once (call in Application or Activity onCreate)
    public static void initCloudinary(Context context) {
        if (!isInitialized) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dpj6kwwb5");   // TODO: Replace with your Cloudinary cloud name
            config.put("api_key", "819271446531818");         // Optional if you need signed uploads
            config.put("api_secret", "6PPQdQm-JN36oL5od8vWWB7AWAM");   // Optional if you need signed uploads
            MediaManager.init(context, config);
            isInitialized = true;
        }
    }

    // 🔹 Upload image and return Task<String> instead of callback
    public static Task<String> uploadImage(String filePath) {
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        MediaManager.get().upload(filePath)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String uploadedUrl = resultData.get("url").toString();
                        tcs.setResult(uploadedUrl);  // ✅ Task complete with URL
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        tcs.setException(new Exception(error.getDescription()));
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        tcs.setException(new Exception(error.getDescription()));
                    }
                }).dispatch();

        return tcs.getTask();
    }

    // upload document
    public static Task<String> uploadDocumentWithName(String filePath) {
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        // Extract file name (with extension)
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1); // e.g., "name.pdf"

        // Remove extension for public_id (Cloudinary doesn't allow dots in public_id)
        String publicId = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;

        MediaManager.get().upload(filePath)
                .option("resource_type", "raw")
                .option("type", "authenticated") // keep secure
                .option("public_id", publicId)   // ✅ Set original file name
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Generate signed URL
                        String signedUrl = MediaManager.get().url()
                                .resourceType("raw")
                                .type("authenticated")
                                .generate(publicId);

                        tcs.setResult(signedUrl);
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        tcs.setException(new Exception(error.getDescription()));
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {
                        tcs.setException(new Exception(error.getDescription()));
                    }
                }).dispatch();

        return tcs.getTask();
    }


    public static Task<String> uploadMedia(
            String filePath,
            String resourceType
    ) {
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        MediaManager.get().upload(filePath)
                .option("resource_type", resourceType)
                .callback(new UploadCallback() {

                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = resultData.get("secure_url").toString();
                        tcs.setResult(url);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        tcs.setException(
                                new Exception(error.getDescription())
                        );
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        tcs.setException(
                                new Exception(error.getDescription())
                        );
                    }
                })
                .dispatch();

        return tcs.getTask();
    }


    // 🔹 Method 2: Get image and show into ImageView using Picasso
    public static void getImage(String url, ImageView targetView) {
        Picasso.get()
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery) // jab tak load ho raha hai
                .error(android.R.drawable.ic_delete) // agar fail ho jaye
                .into(targetView);
    }

    // 🔹 Callback Interface for Upload Result
    public interface UploadResultCallback {
        void onSuccess(String imageUrl);
        void onError(String errorMessage);
    }
}