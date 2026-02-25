package com.example.safesphere.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.Image;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.safesphere.R;
import com.example.safesphere.dto.FamilyMember;
import com.squareup.picasso.Picasso;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;


public class FamilyAdapter extends RecyclerView.Adapter<FamilyAdapter.FamilyViewHolder> {
    public interface OnFamilyActionListener {
        void onMessageClick(FamilyMember member);
        void onLocationClick(FamilyMember member);
    }

    private final Context context;
    private final List<FamilyMember> familyList;

    private final OnFamilyActionListener listener;


    public FamilyAdapter(@Nullable Context context, @Nullable List<FamilyMember> familyList, @Nullable OnFamilyActionListener listener) {
        this.context = context;
        this.familyList = familyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FamilyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_family_members, parent, false);
        return new FamilyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FamilyViewHolder holder, int position) {

        FamilyMember member = familyList.get(position);
        if (member == null) return;

        // Name + relation
        if (member.getRelation() != null && !member.getRelation().isEmpty()) {
            holder.name.setText(member.getName() + " (" +
                    member.getRelation().substring(0, 1).toUpperCase() +
                    member.getRelation().substring(1).toLowerCase() + ")");
        } else {
            holder.name.setText(member.getName());
        }

        // Battery
        int batteryPercent = member.getBattery();

        if (batteryPercent <= 15) {

            int red = ContextCompat.getColor(context, android.R.color.holo_red_light);
            // 🔴 HOLO RED (Text)
            holder.batteryStatus.setTextColor(red);

        } else {

            // 🔁 Default theme text color
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(
                    android.R.attr.textColor,
                    typedValue,
                    true
            );

            holder.batteryStatus.setTextColor(typedValue.data);

        }
        holder.batteryStatus.setText(member.getBattery() + "%");


        // Location
        holder.locationInfo.setText(
                member.getLocation() != null ? " "+member.getLocation() : "--"
        );

        // FamilyAdapter.java mein BindViewHolder ke andar
        if ("online".equalsIgnoreCase(member.getStatus())) {
            holder.status.setText("Online");
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.green));
        } else {
            holder.status.setText("Offline");
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.muted_color));
        }
        
        // Profile Image
        if (member.getProfileImageUrl() != null &&
                !member.getProfileImageUrl().isEmpty()) {
            Picasso.get()
                    .load(member.getProfileImageUrl())
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.profile);
        }

        // Buttons
        holder.messageBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessageClick(member); // receiverId
            }
        });


        holder.videoBtn.setOnClickListener(v ->
                Toast.makeText(context,
                        "Video call " + member.getName(),
                        Toast.LENGTH_SHORT).show());

        holder.locationBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLocationClick(member);
            }
        });
    }

    // payload
    // FamilyAdapter.java ke andar

    @Override
    public void onBindViewHolder(@NonNull FamilyViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            // Agar koi payload nahi hai, to normal bind karo (poora card)
            super.onBindViewHolder(holder, position, payloads);
        } else {
            // Sirf wahi update karo jo bundle mein bheja gaya hai
            Bundle bundle = (Bundle) payloads.get(0);
            for (String key : bundle.keySet()) {
                if (key.equals("location")) {
                    holder.locationInfo.setText(bundle.getString("location"));
                }
                if (key.equals("battery")) {
                    int battery = bundle.getInt("battery");
                    holder.batteryStatus.setText(battery + "%");
                    // Color logic yahan bhi apply kar sakte hain
                    if (battery <= 15) {
                        holder.ivBattery.setColorFilter(R.color.red);
                        holder.batteryStatus.setTextColor(ContextCompat.getColor(context, R.color.red));
                    } else {
                        TypedValue typedValue = new TypedValue();
                        context.getTheme().resolveAttribute(android.R.attr.textColor, typedValue, true);
                        holder.batteryStatus.setTextColor(typedValue.data);
                    }
                }
                if (key.equals("status")) {
                    String status = bundle.getString("status");
                    holder.status.setText(status);
                    int color = "online".equalsIgnoreCase(status) ? R.color.green : R.color.muted_color;
                    holder.status.setTextColor(ContextCompat.getColor(context, color));
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return familyList.size();
    }

    public void updateBattery(String userId, int newBattery) {
        for (int i = 0; i < familyList.size(); i++) {
            if (familyList.get(i).getUserId().equals(userId)) {
                familyList.get(i).setBattery(newBattery);
                notifyItemChanged(i); // 👈 sirf ek item refresh
                break;
            }
        }
    }


    static class FamilyViewHolder extends RecyclerView.ViewHolder {

        CircleImageView profileImage;
        TextView name, status, batteryStatus, locationInfo;
        AppCompatButton messageBtn, videoBtn, locationBtn;
        ImageView ivBattery;

        public FamilyViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            name = itemView.findViewById(R.id.name);
            status = itemView.findViewById(R.id.status);
            batteryStatus = itemView.findViewById(R.id.batteryStatus);
            locationInfo = itemView.findViewById(R.id.locationInfo);
            messageBtn = itemView.findViewById(R.id.messageBtn);
            videoBtn = itemView.findViewById(R.id.videoBtn);
            locationBtn = itemView.findViewById(R.id.locationBtn);
            ivBattery = itemView.findViewById(R.id.ivBattery);
        }
    }
}
