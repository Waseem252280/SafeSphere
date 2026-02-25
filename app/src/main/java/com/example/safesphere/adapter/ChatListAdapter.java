

package com.example.safesphere.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safesphere.R;
import com.example.safesphere.dto.ChatListDto;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import java.util.HashSet;
import java.util.Set;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.VH> {

    public interface OnChatClickListener {
        void onChatClick(ChatListDto dto);
    }

    private final Context context;
    private final List<ChatListDto> list;
    private final String myId;
    private final OnChatClickListener listener;
    private String searchQuery = "";

    private final Set<String> selectedRoomIds = new HashSet<>();
    private boolean selectionMode = false;

    public interface SelectionListener {
        void onSelectionChanged(int count);
    }

    public Set<String> getSelectedRoomIds() {
        return selectedRoomIds;
    }

    public void clearSelection() {
        selectedRoomIds.clear();
        selectionMode = false;
        notifyDataSetChanged();
    }

    private  SelectionListener selectionListener;

    public ChatListAdapter(Context context,
                           List<ChatListDto> list,
                           OnChatClickListener listener,
                           @Nullable SelectionListener selectionListener) {

        this.context = context;
        this.list = list;
        this.listener = listener;
        this.selectionListener = selectionListener;
        this.myId = FirebaseAuth.getInstance().getUid();
    }



    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ChatListDto c = list.get(pos);

        bindProfileImage(h, c);
        bindName(h, c);
        bindTime(h, c);
        bindLastMessage(h, c);
        bindAudioListenStatus(h, c);

        bindRealUnreadCount(h,c);

        // status
        bindOnlineStatus(h, c);
        bindRecordingStatus(h,c);
        bindTypingStatus(h,c);

        handleClick(h, c);

        // 🔥 Selection background
        boolean isSelected = selectedRoomIds.contains(c.getChatRoomId());
        h.itemView.setBackgroundColor(
                isSelected
                        ? ContextCompat.getColor(context, R.color.selected_bg)
                        : Color.TRANSPARENT
        );

    }

    @Override
    public int getItemCount() { return list.size(); }

    // ================== ONLINE STATUS ==================
    private void bindOnlineStatus(VH h, ChatListDto c) {

        if (h.onlineRef != null && h.onlineListener != null) {
            h.onlineRef.removeEventListener(h.onlineListener);
        }

        h.onlineRef = FirebaseDatabase.getInstance()
                .getReference("status")
                .child(c.getChatUserId())
                .child("online");

        h.onlineListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean online = snapshot.getValue(Boolean.class);
                boolean isOnline = online != null && online;
                h.ivStatusDot.setVisibility(isOnline ? View.VISIBLE : View.GONE);

                // update only this item
                int pos = getAdapterPositionForUserId(c.getChatUserId());
                if (pos != -1) notifyItemChanged(pos);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        h.onlineRef.addValueEventListener(h.onlineListener);
    }

    private void bindRecordingStatus(VH h, ChatListDto c) {

        // ✅ ADD THIS LINE FIRST
        if (c.getChatRoomId() == null || c.getChatUserId() == null) {
            h.lastMsg.setText("No messages yet");
            h.lastMsgIcon.setVisibility(View.GONE);
            return;
        }

        if (h.recordingRef != null && h.recordingListener != null) {
            h.recordingRef.removeEventListener(h.recordingListener);
        }

        h.recordingRef = FirebaseDatabase.getInstance()
                .getReference("recording")
                .child(c.getChatRoomId())
                .child(c.getChatUserId());

        h.recordingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean recording = snapshot.getValue(Boolean.class);
                boolean isRecording = recording != null && recording;

                c.setRecording(isRecording);
                bindLastMessage(h,c);

                // update only this item
                int pos = getAdapterPositionForUserId(c.getChatUserId());
                if (pos != -1) notifyItemChanged(pos);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        h.recordingRef.addValueEventListener(h.recordingListener);
    }

    private void bindTypingStatus(VH h, ChatListDto c) {

        // ✅ ADD THIS LINE FIRST
        if (c.getChatRoomId() == null || c.getChatUserId() == null) return;

        if (h.typingRef != null && h.typingListener != null) {
            h.typingRef.removeEventListener(h.typingListener);
        }

        h.typingRef = FirebaseDatabase.getInstance()
                .getReference("typing")
                .child(c.getChatRoomId())
                .child(c.getChatUserId());

        h.typingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean typing = snapshot.getValue(Boolean.class);
                boolean isTyping = typing != null && typing;

                c.setTyping(isTyping);
                bindLastMessage(h,c);

                // update only this item
                int pos = getAdapterPositionForUserId(c.getChatUserId());
                if (pos != -1) notifyItemChanged(pos);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        h.typingRef.addValueEventListener(h.typingListener);
    }


    private int getAdapterPositionForUserId(String userId){
        for (int i = 0; i < list.size(); i++){
            if (list.get(i).getChatUserId().equals(userId)) return i;
        }
        return -1;
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        if (holder.onlineRef != null && holder.onlineListener != null) {
            holder.onlineRef.removeEventListener(holder.onlineListener);
        }
        if (holder.unreadRef != null && holder.unreadListener != null)
            holder.unreadRef.removeEventListener(holder.unreadListener);

    }

    // ================== BIND VIEWS ==================
    private void bindProfileImage(VH h, ChatListDto c) {
        Picasso.get().load(c.getChatUserImage()).into(h.profile);
    }

    private void bindName(VH h, ChatListDto c) {
        String name = c.getChatUserName();
        if (searchQuery.isEmpty() || name == null) {
            h.name.setText(name);
            return;
        }

        SpannableString span = new SpannableString(name);
        String lower = name.toLowerCase();
        int start = lower.indexOf(searchQuery);
        if (start >= 0) {
            span.setSpan(new ForegroundColorSpan(
                            ContextCompat.getColor(context, R.color.green)),
                    start, start + searchQuery.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        h.name.setText(span);
    }

    private void bindTime(VH h, ChatListDto c) {
        if (c.getLastMessageTime() > 0)
            h.time.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(new Date(c.getLastMessageTime())));
        else h.time.setText("");
    }

    private boolean isMe(VH h, ChatListDto c) {
        return c.getSenderId() != null && myId.equals(c.getSenderId());
    }

    private void bindLastMessage(VH h, ChatListDto c) {
        h.lastMsgIcon.clearColorFilter();   // 🔥 MOST IMPORTANT
        h.lastMsgIcon.setVisibility(View.GONE);
        h.statusTick.setVisibility(View.GONE);
        h.lastMsg.setTextColor(Color.GRAY);

        String type = c.getLastMessageType();
        if (type == null) {
            h.lastMsg.setText("No messages yet");
            return;
        }

        if (c.isRecording()) {
            h.lastMsg.setText("recording audio...");
            h.lastMsg.setTextColor(Color.parseColor("#19E103"));
            h.lastMsgIcon.setImageResource(R.drawable.ic_mic);
            h.lastMsgIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.green),
                    PorterDuff.Mode.SRC_IN
            );
            h.lastMsgIcon.setVisibility(View.VISIBLE);

        } else if (c.isTyping()) {
            h.lastMsg.setText("typing...");
            h.lastMsg.setTextColor(Color.parseColor("#19E103"));

        } else {
            switch (c.getLastMessageType()) {
                case "text": h.lastMsg.setText(c.getLastMessage());
                    bindStatusTicks(h,c);
                    break;
                case "image":
                    bindImageMessage(h,c);
                    bindStatusTicks(h,c);
                    break;
                case "video":
                    bindVideoMessage(h,c);
                    bindStatusTicks(h,c);
                    break;
                case "audio":
                    bindAudioMessage(h,c);
                    bindStatusTicks(h,c);
                    break;
                case "document":
                    bindDocMessage(h,c);
                    bindStatusTicks(h,c);
                    break;
                case "deleted":
                    bindDeletedMessage(h,c);
                    bindStatusTicks(h,c);
                    break;
                case "contact":
                    bindContactMessage(h,c);
                    bindStatusTicks(h,c);
                    break;

            }
        }
    }

    private void bindStatusTicks(VH h, ChatListDto c){
        // message status tick for sender
        if (isMe(h, c)) {
            if (c.getLastMessageStatus() != null) {
                switch (c.getLastMessageStatus()) {
                    case "sent":
                        h.statusTick.setImageResource(R.drawable.ic_sent);
                        break;
                    case "delivered":
                        h.statusTick.setImageResource(R.drawable.ic_delivered);
                        break;
                    case "seen":
                        h.statusTick.setColorFilter(
                                ContextCompat.getColor(context, R.color.sky_blue),
                                PorterDuff.Mode.SRC_IN
                        );
                        h.statusTick.setImageResource(R.drawable.ic_seen);
                        break;
                }
                h.statusTick.setVisibility(View.VISIBLE);
            } else {
                h.statusTick.setVisibility(View.GONE);
            }
        } else {
            h.statusTick.setVisibility(View.GONE);
        }
    }

    private void bindDeletedMessage(VH h, ChatListDto c){
        h.lastMsgIcon.setImageResource(R.drawable.ic_removed);
        h.lastMsgIcon.setVisibility(View.VISIBLE);
        h.lastMsg.setText(c.getLastMessage());
        h.lastMsgIcon.setColorFilter(
                ContextCompat.getColor(context, R.color.lightGray),
                PorterDuff.Mode.SRC_IN
        );
    }

    private void bindImageMessage(VH h, ChatListDto c) {
        h.lastMsgIcon.setImageResource(R.drawable.ic_image);
        h.lastMsgIcon.setVisibility(View.VISIBLE);

        if (isMe(h,c)){
            h.lastMsg.setText("Sent Image");
        }else{
            h.lastMsg.setText("Received Image");
        }
    }

    private void bindVideoMessage(VH h, ChatListDto c) {
        h.lastMsgIcon.setImageResource(R.drawable.ic_video);
        h.lastMsgIcon.setVisibility(View.VISIBLE);
        h.lastMsgIcon.setColorFilter(
                ContextCompat.getColor(context, R.color.lightGray),
                PorterDuff.Mode.SRC_IN
        );
        if (isMe(h,c)){
            h.lastMsg.setText("Sent Video");
        }else{
            h.lastMsg.setText("Received Video");
        }
    }

    private void bindContactMessage(VH h, ChatListDto c) {
        h.lastMsgIcon.setImageResource(R.drawable.ic_add_person);
        h.lastMsgIcon.setVisibility(View.VISIBLE);
        h.lastMsgIcon.setColorFilter(
                ContextCompat.getColor(context, R.color.sky_blue),
                PorterDuff.Mode.SRC_IN
        );
        if (isMe(h,c)){
            h.lastMsg.setText("Sent Contact");
        }else{
            h.lastMsg.setText("Received Contact");
        }
    }

    private void bindAudioMessage(VH h, ChatListDto c) {
        h.lastMsgIcon.setImageResource(R.drawable.ic_mic);
        h.lastMsgIcon.setColorFilter(
                ContextCompat.getColor(context, R.color.lightGray),
                PorterDuff.Mode.SRC_IN
        );
        h.lastMsgIcon.setVisibility(View.VISIBLE);

        // show messaged based on receiver or sender
        if (isMe(h,c)){
            h.lastMsg.setText("Sent Voice");
        }else{
            h.lastMsg.setText("Received Voice");
        }

        //change mic color based on listen status
        if (!c.isListen()){
            if(isMe(h,c)){
                h.lastMsgIcon.setColorFilter(
                        ContextCompat.getColor(context, R.color.lightGray),
                        PorterDuff.Mode.SRC_IN
                );
            }else{
                h.lastMsgIcon.setColorFilter(
                        ContextCompat.getColor(context, R.color.green),
                        PorterDuff.Mode.SRC_IN
                );
            }
        }
        if(c.isListen()){
            h.lastMsgIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.sky_blue),
                    PorterDuff.Mode.SRC_IN
            );
        }
    }
    private void bindDocMessage(VH h, ChatListDto c) {
        h.lastMsgIcon.setImageResource(R.drawable.ic_doc);
        h.lastMsgIcon.setVisibility(View.VISIBLE);

        if (isMe(h,c)){
            h.lastMsg.setText("Sent Document");
        }else{
            h.lastMsg.setText("Received Document");
        }
    }

    private void bindAudioListenStatus(VH h, ChatListDto c) {

        // ✅ ADD THIS LINE FIRST
        if (c.getChatRoomId() == null || c.getLastMessageId() == null) return;

        if (!"audio".equals(c.getLastMessageType())) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(c.getChatRoomId())
                .child(c.getLastMessageId())
                .child("listen");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean listened = snapshot.getValue(Boolean.class);
                c.setListen(listened != null && listened);

                bindLastMessage(h, c);

                int pos = getAdapterPositionForUserId(c.getChatUserId());
                if (pos != -1) notifyItemChanged(pos);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void bindRealUnreadCount(VH h, ChatListDto c) {

        if (c.getChatRoomId() == null || c.getLastMessageId() == null) {
            h.unread.setVisibility(View.GONE);
            return;
        }

        // Remove previous listener if exists
        if (h.unreadRef != null && h.unreadListener != null) {
            h.unreadRef.removeEventListener(h.unreadListener);
        }

        // Reference to the chat messages
        h.unreadRef = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(c.getChatRoomId());

        h.unreadListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unread = 0;

                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    String senderId = msgSnap.child("senderId").getValue(String.class);
                    String status = msgSnap.child("status").getValue(String.class);

                    // Condition: sender = chat user, receiver = me, status != "seen"
                    if (myId != null && senderId != null && senderId.equals(c.getChatUserId())) {
                        if ("sent".equals(status) || "delivered".equals(status)) {
                            unread++;
                        }
                    }
                }

                c.setUnreadCount(unread);

                if (unread > 0) {
                    h.unread.setVisibility(View.VISIBLE);
                    h.unread.setText(String.valueOf(unread));
                } else {
                    h.unread.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        h.unreadRef.addValueEventListener(h.unreadListener);
    }

    private void handleClick(VH h, ChatListDto c) {

        // 🔥 ONLY enable long click if selectionListener exists
        if (selectionListener != null) {
            h.itemView.setOnLongClickListener(v -> {

                if (!selectionMode) selectionMode = true;

                toggleSelection(c.getChatRoomId());
                return true;
            });
        } else {
            h.itemView.setOnLongClickListener(null); // 🚫 disable
        }

        h.itemView.setOnClickListener(v -> {

            if (selectionMode) {
                toggleSelection(c.getChatRoomId());
                return;
            }

            // normal click (open chat)
            FirebaseDatabase.getInstance()
                    .getReference("chatList")
                    .child(myId)
                    .child(c.getChatUserId())
                    .child("unreadCount")
                    .setValue(0);

            if (listener != null)
                listener.onChatClick(c);
        });
    }

    private void toggleSelection(String roomId) {

        if (roomId == null) return;

        if (selectedRoomIds.contains(roomId)) {
            selectedRoomIds.remove(roomId);
        } else {
            selectedRoomIds.add(roomId);
        }

        if (selectedRoomIds.isEmpty()) {
            selectionMode = false;
        }
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedRoomIds.size());
        }
        notifyDataSetChanged();
    }

    public void setSearchQuery(String query) { this.searchQuery = query; }

    // ✅ Only update one chat item
    public void updateChat(ChatListDto updatedDto) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getChatRoomId().equals(updatedDto.getChatRoomId())) {
                list.set(i, updatedDto);
                notifyItemChanged(i);
                return;
            }
        }
    }


    static class VH extends RecyclerView.ViewHolder {
        CircleImageView profile;
        TextView name, lastMsg, time, unread;
        ImageView lastMsgIcon, statusTick, ivStatusDot;

        ValueEventListener onlineListener, typingListener, recordingListener, unreadListener;
        DatabaseReference onlineRef, typingRef, recordingRef, unreadRef;

        public VH(@NonNull View itemView) {
            super(itemView);
            profile = itemView.findViewById(R.id.profile_image);
            name = itemView.findViewById(R.id.tvName);
            lastMsg = itemView.findViewById(R.id.tvLastMessage);
            time = itemView.findViewById(R.id.tvTime);
            unread = itemView.findViewById(R.id.tvUnread);
            lastMsgIcon = itemView.findViewById(R.id.ivLastMessageIcon);
            statusTick = itemView.findViewById(R.id.ivStatus);
            ivStatusDot = itemView.findViewById(R.id.ivStatusDot);
        }
    }
}

