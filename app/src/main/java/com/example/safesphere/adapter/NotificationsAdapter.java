package com.example.safesphere.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safesphere.R;
import com.example.safesphere.dto.NotificationDto;
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotifViewHolder> {

    private List<NotificationDto> list;
    private OnActionClickListener listener;

    public interface OnActionClickListener {
        void onAccept(NotificationDto dto);
        void onReject(NotificationDto dto);
    }

    public NotificationsAdapter(List<NotificationDto> list, OnActionClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotifViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        NotificationDto dto = list.get(position);

        holder.tvSenderName.setText(dto.getSenderName());
        if ("male".equalsIgnoreCase(dto.getSenderGender())) {
            holder.relationMsg.setText("Wants to add you as his "+dto.getRelationRequested().toLowerCase()+".");
        }else{
            holder.relationMsg.setText("Wants to add you as her  "+dto.getRelationRequested().toLowerCase()+".");
        }
        Picasso.get().load(dto.getSenderPhoto()).into(holder.ivSenderImage);

        holder.btnAccept.setOnClickListener(v -> listener.onAccept(dto));
        holder.btnReject.setOnClickListener(v -> listener.onReject(dto));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class NotifViewHolder extends RecyclerView.ViewHolder {

        CircleImageView ivSenderImage;
        TextView tvSenderName, relationMsg;
        MaterialButton btnAccept, btnReject;

        public NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSenderImage = itemView.findViewById(R.id.ivSenderImage);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            relationMsg = itemView.findViewById(R.id.relationMsg);
        }
    }
}
