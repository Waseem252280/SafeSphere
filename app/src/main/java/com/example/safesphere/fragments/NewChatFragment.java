package com.example.safesphere.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safesphere.R;
import com.example.safesphere.adapter.ChatListAdapter;
import com.example.safesphere.dto.ChatDto;
import com.example.safesphere.dto.ChatListDto;
import com.example.safesphere.dto.FamilyMember;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class NewChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;

    private List<ChatListDto> masterList = new ArrayList<>();
    private List<ChatListDto> displayList = new ArrayList<>();

    private String myId;

    private boolean isSearching = false;
    private View emptyChatsSearch;

    private final Set<String> existingChatUserIds = new HashSet<>();



    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_new_chat, container, false);

        myId = FirebaseAuth.getInstance().getUid();

        recyclerView = v.findViewById(R.id.rvUsers);
        emptyChatsSearch = v.findViewById(R.id.emptyChatsSearch);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ChatListAdapter(
                requireContext(),
                displayList,
                this::openChat,
                null
        );
        recyclerView.setAdapter(adapter);

        initSearch(v);

         loadExistingChats(this::loadUsers);

        return v;
    }


    private void updateEmptyStates() {

        // no search result
        if (isSearching && displayList.isEmpty()) {
            emptyChatsSearch.setVisibility(View.VISIBLE);
        } else {
            emptyChatsSearch.setVisibility(View.GONE);
        }
    }

    private void initSearch(View v) {

        EditText etSearch = v.findViewById(R.id.etSearchUsers);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String q = s.toString().trim().toLowerCase();
                isSearching = !q.isEmpty();

                filterUsers(q);
            }
        });
    }


    private void filterUsers(String q) {

        displayList.clear();

        if (q.isEmpty()) {
            displayList.addAll(masterList);
        } else {
            for (ChatListDto u : masterList) {

                String name = u.getChatUserName() == null ? "" :
                        u.getChatUserName().toLowerCase();

                String email = u.getEmail() == null ? "" :
                        u.getEmail().toLowerCase();

                if (name.contains(q) || email.contains(q)) {
                    displayList.add(u);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyStates();
    }

    /** new method added **/
    private void loadUsers() {

        FirebaseDatabase.getInstance()
                .getReference("users")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                masterList.clear();

                                for (DataSnapshot u : snapshot.getChildren()) {

                                    String uid = u.getKey();
                                    if (uid == null || uid.equals(myId)) continue;

                                    // 🚫 SKIP if chat already exists
                                    if (existingChatUserIds.contains(uid)) continue;

                                    ChatListDto dto = new ChatListDto();
                                    dto.setChatUserId(uid);
                                    dto.setChatUserName(
                                            u.child("fullName").getValue(String.class));
                                    dto.setChatUserImage(
                                            u.child("photoUrl").getValue(String.class));
                                    dto.setEmail(
                                            u.child("email").getValue(String.class));

                                    masterList.add(dto);
                                }

                                displayList.clear();
                                displayList.addAll(masterList);
                                adapter.notifyDataSetChanged();
                                updateEmptyStates();
                            }

                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
    }

    private void loadExistingChats(Runnable onDone) {

        DatabaseReference chatsRef =
                FirebaseDatabase.getInstance().getReference("chats");

        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                existingChatUserIds.clear();

                for (DataSnapshot roomSnap : snapshot.getChildren()) {

                    String roomId = roomSnap.getKey();
                    if (roomId == null || !roomId.contains(myId)) continue;

                    String[] parts = roomId.split("_");
                    String otherUserId =
                            parts[0].equals(myId) ? parts[1] : parts[0];

                    boolean hasRealVisibleMessage = false;

                    for (DataSnapshot msgSnap : roomSnap.getChildren()) {

                        ChatDto msg = msgSnap.getValue(ChatDto.class);
                        if (msg == null) continue;

                        boolean deletedForMe =
                                msg.getDeletedFor() != null &&
                                        Boolean.TRUE.equals(msg.getDeletedFor().get(myId));

                        boolean deletedForEveryone =
                                "deleted".equals(msg.getMessageType());

                        // 🔥 ONLY real messages count
                        if (!deletedForMe && !deletedForEveryone) {
                            hasRealVisibleMessage = true;
                            break;
                        }
                    }

                    // ✅ Block user ONLY if real visible chat exists
                    if (hasRealVisibleMessage) {
                        existingChatUserIds.add(otherUserId);
                    }
                }

                onDone.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onDone.run();
            }
        });
    }


    private void openChat(ChatListDto dto) {

            FamilyMember member = new FamilyMember();
            member.setUserId(dto.getChatUserId());
            member.setProfileImageUrl(dto.getChatUserImage());
            member.setName(dto.getChatUserName());
            ChatFragment chatFragment = ChatFragment.newInstance(member);

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, chatFragment)
                    .addToBackStack(null)
                    .commit();

    }
}
