package com.example.safesphere.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.safesphere.R;
import com.example.safesphere.adapter.ChatListAdapter;
import com.example.safesphere.dto.ChatDto;
import com.example.safesphere.dto.ChatListDto;
import com.example.safesphere.dto.FamilyMember;
import com.example.safesphere.utils.DialogHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class ChatListFragment extends Fragment {

    // UI
    private RecyclerView recyclerView;
    private EditText etSearchChats;
    private ImageView ivClearSearch;

    // Adapter
    private ChatListAdapter adapter;

    // Data
    private final List<ChatListDto> masterList = new ArrayList<>();
    private final List<ChatListDto> displayList = new ArrayList<>();
    private boolean isSearching = false;

    // no chats are availabe
    private LinearLayout emptyChats, emptyChatsSearch;

    // Firebase
    private String myId;

    // floating action button
    private FloatingActionButton fab;

    public static final Set<String> existingChatUserIds = new HashSet<>();

    private ActionMode actionMode;

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_chat_list, menu);
            // ✅ Force icons to show
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete_all_for_me) {
                // Selection ko yahan save kar lein kyunki mode.finish() isse clear kar sakta hai
                final Set<String> selectedRooms = new HashSet<>(adapter.getSelectedRoomIds());

                DialogHelper.showDeleteDialog(requireContext(), new DialogHelper.DeleteDialogListener() {

                    @Override
                    public void onDeleteForMe() {
                        if (!isNetworkAvailable()) {
                            Toast.makeText(requireContext(), "No internet connection!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("chats");

                        for (String roomId : selectedRooms) {
                            dbRef.child(roomId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Map<String, Object> updates = new HashMap<>();
                                    for (DataSnapshot msgSnap : snapshot.getChildren()) {
                                        String msgId = msgSnap.getKey();
                                        if (msgId != null) {
                                            updates.put(msgId + "/deletedFor/" + myId, true);
                                        }
                                    }

                                    // 🔥 Update hone ke baad list se remove karein
                                    dbRef.child(roomId).updateChildren(updates).addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            // Room ID se otherUserId nikalne ke liye (agar format other_my hai)
                                            String[] parts = roomId.split("_");
                                            String otherUserId = parts[0].equals(myId) ? parts[1] : parts[0];

                                            // Local list se remove karne ka helper function
                                            removeChatLocally(roomId, otherUserId);
                                        }
                                    });
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                        }
                    }

                    @Override
                    public void onDeleteForEveryone() {
                        if (!isNetworkAvailable()) {
                            Toast.makeText(requireContext(), "No internet connection!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("chats");

                        for (String roomId : selectedRooms) {
                            dbRef.child(roomId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Map<String, Object> updates = new HashMap<>();
                                    for (DataSnapshot msgSnap : snapshot.getChildren()) {
                                        String msgId = msgSnap.getKey();
                                        String senderId = msgSnap.child("senderId").getValue(String.class);
                                        if (msgId == null) continue;

                                        if (myId.equals(senderId)) {
                                            // Mere messages everyone ke liye "deleted" honge
                                            updates.put(msgId + "/message", "This message was deleted");
                                            updates.put(msgId + "/messageType", "deleted");
                                        } else {
                                            // Dusre ke messages sirf mere liye hide honge
                                            updates.put(msgId + "/deletedFor/" + myId, true);
                                        }
                                    }

                                    // 🔥 Update successful hone par UI se remove karein
                                    dbRef.child(roomId).updateChildren(updates).addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            String[] parts = roomId.split("_");
                                            String otherUserId = parts[0].equals(myId) ? parts[1] : parts[0];

                                            // Local cleaning
                                            removeChatLocally(roomId, otherUserId);
                                        } else {
                                            Toast.makeText(requireContext(), "Failed to delete some messages", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                        }
                    }

                    @Override public void onCancel() {}
                });

                mode.finish(); // ActionMode ko band kar dein
                return true;
            }
            return false;
        }

        private void removeChatLocally(String roomId, String otherUserId) {
            // 1. Master list se hatao
            Iterator<ChatListDto> iterator = masterList.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getChatRoomId().equals(roomId)) {
                    iterator.remove();
                    break;
                }
            }

            // 2. Set se hatao taaki user dobara chat start kar sake
            existingChatUserIds.remove(otherUserId);

            // 3. Display list update karo
            displayList.clear();
            displayList.addAll(masterList);

            // 4. UI Refresh
            adapter.notifyDataSetChanged();
            updateEmptyStates();
        }

        private boolean isNetworkAvailable() {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                    requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelection();
            actionMode = null;
        }
    };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        initViews(view);
        setupRecycler();
        setupSearch();
        setupClickListeners();

        myId = FirebaseAuth.getInstance().getUid();
        listenChatList();

        setHasOptionsMenu(true); // 🔥 IMPORTANT

        updateEmptyStates();

        return view;
    }

    // ===================== INIT =====================

    private void initViews(View v) {
        recyclerView = v.findViewById(R.id.rvChats);
        etSearchChats = v.findViewById(R.id.etSearchChats);
        ivClearSearch = v.findViewById(R.id.ivClearSearch);
        fab = v.findViewById(R.id.fabNewChat);
        emptyChats = v.findViewById(R.id.emptyChats);
        emptyChatsSearch = v.findViewById(R.id.emptyChatsSearch);
    }

    private void setupRecycler() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ChatListAdapter(
                requireContext(),
                displayList,
                this::openChat,
                count -> {

                    if (count > 0) {
                        if (actionMode == null) {
                            actionMode = requireActivity()
                                    .startActionMode(actionModeCallback);
                        }
                        actionMode.setTitle(count + " selected");
                    } else {
                        if (actionMode != null) {
                            actionMode.finish();
                        }
                    }
                }
        );

        recyclerView.setAdapter(adapter);
    }

    // ===================== SEARCH =====================

    private void setupSearch() {
        etSearchChats.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String query = s.toString().trim().toLowerCase();
                isSearching = !query.isEmpty();

                if (isSearching) {
                    filterChats(query);
                } else {
                    displayList.clear();
                    displayList.addAll(masterList);
                }

                adapter.setSearchQuery(query); // highlight
                adapter.notifyDataSetChanged();
                updateEmptyStates();

            }
        });
    }

    private void filterChats(String query) {
        displayList.clear();

        for (ChatListDto dto : masterList) {

            String name = dto.getChatUserName() == null ? "" :
                    dto.getChatUserName().toLowerCase();

            String lastMsg = dto.getLastMessage() == null ? "" :
                    dto.getLastMessage().toLowerCase();

            if (name.contains(query) || lastMsg.contains(query)) {
                displayList.add(dto);
            }
        }
    }

    // ===================== CLICK LISTENERS =====================

    private void setupClickListeners() {

        fab.setOnClickListener(v -> openNewChat());
        ivClearSearch.setOnClickListener(v ->
                etSearchChats.setText("")
        );

        etSearchChats.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ivClearSearch.setVisibility(
                        s.length() > 0 ? View.VISIBLE : View.GONE
                );
            }
        });
    }

    // ===================== FIREBASE =====================

    private void listenChatList() {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        // 👇 EMPTY STATE LISTENER (IMPORTANT)
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                boolean hasMyChats = false;

                for (DataSnapshot roomSnap : snapshot.getChildren()) {
                    String roomId = roomSnap.getKey();
                    if (roomId != null && roomId.contains(myId)) {
                        hasMyChats = true;
                        break;
                    }
                }

                if (!hasMyChats) {
                    masterList.clear();
                    displayList.clear();
                    adapter.notifyDataSetChanged();
                    updateEmptyStates(); // ✅ NOW IT WILL SHOW
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 👇 REAL-TIME UPDATES
        chatsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot roomSnap, String prevChildKey) {
                handleRoomUpdate(roomSnap);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot roomSnap, String prevChildKey) {
                handleRoomUpdate(roomSnap);
            }

           /** @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {} **/

           @Override
           public void onChildRemoved(@NonNull DataSnapshot snapshot) {

               String roomId = snapshot.getKey();
               if (roomId == null || !roomId.contains(myId)) return;

               String[] parts = roomId.split("_");
               String otherUserId = parts[0].equals(myId) ? parts[1] : parts[0];

               // 🔥 1. ChatList se remove
               for (int i = 0; i < masterList.size(); i++) {
                   if (masterList.get(i).getChatUserId().equals(otherUserId)) {
                       masterList.remove(i);
                       break;
                   }
               }

               // 🔥 2. Display list refresh
               displayList.clear();
               displayList.addAll(masterList);
               adapter.notifyDataSetChanged();

               // 🔥 3. NewChat ke liye allow karo
               existingChatUserIds.remove(otherUserId);

               // 🔥 4. Empty state update
               updateEmptyStates();
           }

            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String prevChildKey) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    // ================= ROOM UPDATE =================
    private void handleRoomUpdate(DataSnapshot roomSnap) {


        boolean hasAnyVisible = false;

        for (DataSnapshot msgSnap : roomSnap.getChildren()) {

            ChatDto msg = msgSnap.getValue(ChatDto.class);
            if (msg == null) continue;

            boolean deletedForMe =
                    msg.getDeletedFor() != null &&
                            Boolean.TRUE.equals(msg.getDeletedFor().get(myId));

            boolean isDeletedType =
                    "deleted".equals(msg.getMessageType());

            if (!deletedForMe && !isDeletedType) {
                hasAnyVisible = true;
                break;
            }
        }

        if (!hasAnyVisible) return; // 🔥 DO NOT ADD ROOM

        String roomId = roomSnap.getKey();
        if (roomId == null || !roomId.contains(myId)) return;

        String[] parts = roomId.split("_");
        String otherUserId = parts[0].equals(myId) ? parts[1] : parts[0];

        existingChatUserIds.add(otherUserId);

        ChatDto lastVisibleMsg = null;
        String lastVisibleMsgId = null;
        boolean isTyping = false, isRecording = false;

        for (DataSnapshot msgSnap : roomSnap.getChildren()) {

            ChatDto msg = msgSnap.getValue(ChatDto.class);
            if (msg == null) continue;

            boolean deletedForMe = msg.getDeletedFor() != null &&
                    Boolean.TRUE.equals(msg.getDeletedFor().get(myId));

            if (!deletedForMe) {
                lastVisibleMsg = msg;
                lastVisibleMsgId = msgSnap.getKey(); // 🔥 IMPORTANT
            }

            if (msg.getTyping() != null && msg.getTyping()) isTyping = true;
            if (msg.getRecording() != null && msg.getRecording()) isRecording = true;
        }


        if (lastVisibleMsg == null) return;

        final ChatListDto dto = new ChatListDto();
        dto.setChatRoomId(roomId);
        dto.setChatUserId(otherUserId);
        dto.setLastMessage(lastVisibleMsg.getMessage());
        dto.setLastMessageType(lastVisibleMsg.getMessageType());
        dto.setLastMessageTime(lastVisibleMsg.getTimestamp());
        dto.setLastMessageStatus(lastVisibleMsg.getStatus());
        dto.setSenderId(lastVisibleMsg.getSenderId());
        dto.setTyping(isTyping);
        dto.setRecording(isRecording);

        dto.setLastMessageId(lastVisibleMsgId);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(otherUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot u) {
                        dto.setChatUserName(u.child("fullName").getValue(String.class));
                        dto.setChatUserImage(u.child("photoUrl").getValue(String.class));

                        boolean exists = false;
                        for (int i = 0; i < masterList.size(); i++) {
                            if (masterList.get(i).getChatRoomId().equals(dto.getChatRoomId())) {
                                masterList.set(i, dto);
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) masterList.add(dto);

                        Collections.sort(masterList,
                                (a, b) -> Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));

                        if (!isSearching) {
                            displayList.clear();
                            displayList.addAll(masterList);
                        }

                        updateEmptyStates();

                        // ✅ Only update the changed chat item
                        adapter.updateChat(dto);
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateEmptyStates() {
        // no chats at all
        if (!isSearching && masterList.isEmpty()) {
            emptyChats.setVisibility(View.VISIBLE);
        } else {
            emptyChats.setVisibility(View.GONE);
        }

        // no search result
        if (isSearching && displayList.isEmpty()) {
            emptyChatsSearch.setVisibility(View.VISIBLE);
        } else {
            emptyChatsSearch.setVisibility(View.GONE);
        }
    }


    // ===================== NAVIGATION =====================

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

    private void openNewChat() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new NewChatFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_delete_all_for_me) {

            // 🔥 EXACT LINE YOU ASKED FOR
            Set<String> rooms = adapter.getSelectedRoomIds();

            for (String roomId : rooms) {

                // ✅ OPTION 1: Delete chat FOR ME
                FirebaseDatabase.getInstance()
                        .getReference("chats")
                        .child(roomId)
                        .removeValue();

                // (agar sirf "delete for me" chahiye to batao,
                // uska alag exact code dunga)
            }
            // 🔥 clear selection
            adapter.clearSelection();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
