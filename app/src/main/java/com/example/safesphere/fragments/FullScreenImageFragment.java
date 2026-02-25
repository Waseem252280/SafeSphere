package com.example.safesphere.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.safesphere.R;
import com.example.safesphere.dto.UserDto;
import com.example.safesphere.utils.BadgeUtil;
import com.squareup.picasso.Picasso;

public class FullScreenImageFragment extends Fragment {

    private String profileUrl;
    private ImageView fullScreenImage;
    public FullScreenImageFragment(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_full_screen_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        openFullScreenImage(profileUrl);
    }

    private void initViews(View view){
        fullScreenImage = view.findViewById(R.id.fullScreenImage);
    }


    private void openFullScreenImage(String profileUrl){
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Picasso.get()
                    .load(profileUrl)
                    .noFade()
                    .config(Bitmap.Config.ARGB_8888) // high quality rendering
                    .into(fullScreenImage);

        } else {
            fullScreenImage.setImageResource(R.drawable.ic_person);
        }
    }
}