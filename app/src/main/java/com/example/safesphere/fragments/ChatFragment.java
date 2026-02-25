
package com.example.safesphere.fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.airbnb.lottie.LottieAnimationView;
import com.example.safesphere.R;
import com.example.safesphere.activity.HomeActivity;
import com.example.safesphere.adapter.ChatAdapter;
import com.example.safesphere.dto.ChatDto;
import com.example.safesphere.dto.FamilyMember;
import com.example.safesphere.dto.UserDto;
import com.example.safesphere.media.CameraActivity;
import com.example.safesphere.offlineMessageMachenism.AppDatabase;
import com.example.safesphere.offlineMessageMachenism.PendingChatDao;
import com.example.safesphere.offlineMessageMachenism.PendingChatEntity;
import com.example.safesphere.offlineMessageMachenism.SendMessageWorker;
import com.example.safesphere.services.CloudinaryService;
import com.example.safesphere.utils.ChatRoomUtil;
import com.example.safesphere.utils.SharedPrefferanceUtil;
import com.example.safesphere.utils.ThemeUtils;
import com.example.safesphere.utils.UserStatusHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChatFragment extends Fragment
        implements ChatAdapter.OnAudioListenListener {

    // Interface for Adapter to call Fragment for downloading
    public interface DownloadInterface {
        void onDownloadRequested(ChatDto msg, int position);
    }

    // Voice recording
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;
    private LottieAnimationView recording;
    private UserDto user;

    // Last seen handler
    private android.os.Handler lastSeenHandler = new android.os.Handler();
    private Runnable lastSeenRunnable;
    private long lastSeenTime = -1;

    // UI
    private RecyclerView chatRecyclerView;
    private EditText etMessage;
    private ImageView btnSend, ivProfile, btnMic;
    private TextView tvUserName, tvUserStatus;
    private AppCompatButton btnAttach, btnCamera, btnVideoCall, btnVoiceCall, chatOptions;
    private LinearLayout chatRootLayout;

    // Adapter
    private ChatAdapter chatAdapter;
    private List<ChatDto> messageList;

    // Firebase
    private DatabaseReference chatRef;
    private DatabaseReference userRef;

    // IDs
    private String currentUserId;
    private String receiverId;
    private String chatRoomId;

    private boolean isConnected = false;

    private PendingChatDao pendingChatDao; // Imports: com.example.safesphere.offline.PendingChatDao

    private MaterialCardView attachmentSheet;
    private Button hideBottomsheet;

    private ImageView attach_camera, attach_gallery, attach_event, attach_doc, attach_audio, attach_contact;

    private ActivityResultLauncher<Intent> previewLauncher;

    // Pickers
    private ActivityResultLauncher<Intent> galleryPicker;
    private ActivityResultLauncher<Intent> audioPicker;
    private ActivityResultLauncher<Intent> contactPicker;
    private ActivityResultLauncher<Intent> documentPicker;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        markMessagesAsSeen();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        applyChatBackground();
        initUserIds();
        initFirebaseRefs();

        //get result from camera preview activity
        getResultFromPreviewActivity();

        setupClickListeners();
        setupRecyclerView();
        setupSendMessage();
        readMessages();
        setupTypingListener();
        listenTypingStatus();
        listenUserStatus();  //for recording.. status
        listenConnection();
        listenUserStatusRealtime();

        initPickers();
        
            // 1. Edge-to-edge enable karein (StatusBar aur NavigationBar ko transparent banata hai)
            WindowCompat.setDecorFitsSystemWindows(requireActivity().getWindow(), false);

            // Views ko find karein
            View chatToolbar = view.findViewById(R.id.chatToolbar);
            View messageBar = view.findViewById(R.id.messageBar);
            View root = view.findViewById(R.id.chatRootLayout);

            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

                // 2. Toolbar Fix: Top margin set karein taaki status bar ke niche dikhe
                LinearLayout.LayoutParams toolbarParams = (LinearLayout.LayoutParams) chatToolbar.getLayoutParams();
                toolbarParams.topMargin = systemBars.top;
                chatToolbar.setLayoutParams(toolbarParams);

                // 3. Bottom Bar Fix: Keyboard ya Navigation bar ke upar margin set karein
                int bottomSpace = Math.max(systemBars.bottom, ime.bottom);
                LinearLayout.LayoutParams messageParams = (LinearLayout.LayoutParams) messageBar.getLayoutParams();
                messageParams.bottomMargin = bottomSpace;
                messageBar.setLayoutParams(messageParams);

                // 4. Keyboard open hone par last message par scroll karein
                if (insets.isVisible(WindowInsetsCompat.Type.ime()) && !messageList.isEmpty()) {
                    chatRecyclerView.post(() -> chatRecyclerView.scrollToPosition(messageList.size() - 1));
                }

                return WindowInsetsCompat.CONSUMED;
            });
        pendingChatDao = AppDatabase.getInstance(requireContext()).pendingChatDao();
    }






    private void initPickers() {

        // 📷 IMAGE / VIDEO PICKER
        galleryPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                        Uri uri = result.getData().getData();
                        if (uri == null) return;

                        String type = requireContext().getContentResolver().getType(uri);

                        String localPath = copyToLocal(uri,
                                type != null && type.startsWith("video") ? "video" : "image");

                        if (localPath != null) {
                            sendMessage(
                                    type != null && type.startsWith("video") ? "video" : "image",
                                    null,
                                    localPath
                            );
                        }
                    }
                }
        );

        // 🎵 AUDIO PICKER
        audioPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                        Uri uri = result.getData().getData();
                        if (uri == null) return;

                        String localPath = copyToLocal(uri, "audio");
                        if (localPath != null) {
                            sendMessage("audio", null, localPath);
                        }
                    }
                }
        );

        // 👤 CONTACT PICKER
        contactPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                        Uri uri = result.getData().getData();
                        if (uri == null) return;

                        String contactName = getContactName(uri);
                        String contactNumber = getContactNumber(uri);
                        String message = contactName + "|" + contactNumber;
                        if (contactName != null) {
                            sendMessage("contact", message, null);
                        }
                    }
                }
        );

        // 📄 DOCUMENT PICKER (PDF, DOC, ZIP, etc.)
        documentPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                        Uri uri = result.getData().getData();
                        if (uri == null) return;

                        String localPath = copyDocumentToLocal(uri);

                        if (localPath != null) {
                            sendMessage("document", null, localPath);
                        }
                    }
                }
        );

    }

    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/zip",
                "text/plain"
        });

        documentPicker.launch(intent);
    }
    private String copyDocumentToLocal(Uri uri) {
        try {
            InputStream input = requireContext()
                    .getContentResolver().openInputStream(uri);

            File dir = getDocMediaDir("document");

            String fileName = getFileName(uri);
            if (fileName == null) {
                fileName = "DOC_" + System.currentTimeMillis();
            }

            File file = new File(dir, fileName);

            FileOutputStream output = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }

            output.close();
            input.close();

            return file.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileName(Uri uri) {
        Cursor cursor = requireContext().getContentResolver()
                .query(uri, null, null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    OpenableColumns.DISPLAY_NAME
                            )
                    );
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }


    private File getDocMediaDir(String type) {
        File baseDir = new File(requireContext().getFilesDir(), "chat_media");

        File dir;
        switch (type) {
            case "image":
                dir = new File(baseDir, "images");
                break;
            case "video":
                dir = new File(baseDir, "videos");
                break;
            case "audio":
                dir = new File(baseDir, "audio");
                break;
            case "document":
                dir = new File(baseDir, "documents");
                break;
            default:
                dir = new File(baseDir, "others");
        }

        if (!dir.exists()) dir.mkdirs();
        return dir;
    }


    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"image/*", "video/*"});
        galleryPicker.launch(intent);
    }

    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

//        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            audioPicker.launch(intent);
//        } else {
//            Toast.makeText(requireContext(), "No audio app found", Toast.LENGTH_SHORT).show();
//        }
    }


    private void openContactPicker() {
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        );
        contactPicker.launch(intent);
    }

    private String getContactName(Uri uri) {
        try (Cursor cursor = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        )
                );
            }
        }
        return null;
    }

    private String getContactNumber(Uri uri) {
        try (Cursor cursor = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                );
            }
        }
        return null;
    }


    private String copyToLocal(Uri uri, String type) {
        try {
            InputStream input = requireContext()
                    .getContentResolver().openInputStream(uri);

            File dir = getMediaDir(type);

            String ext = type.equals("image") ? ".jpg"
                    : type.equals("video") ? ".mp4"
                    : ".m4a";

            File file = new File(dir,
                    type.toUpperCase() + "_" + System.currentTimeMillis() + ext);

            FileOutputStream output = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }

            output.close();
            input.close();

            return file.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }









    private void getResultFromPreviewActivity(){
        previewLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                        String type = result.getData().getStringExtra("type");
                        String localPath = result.getData().getStringExtra("localPath");

                        if (type != null && localPath != null) {
                            sendMessage(type, null, localPath);
                        }
                    }
                }
        );

    }

    private void initViews(View view) {
        chatRootLayout = view.findViewById(R.id.chatRootLayout);
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        ivProfile = view.findViewById(R.id.profileImage);
        tvUserName = view.findViewById(R.id.tvUserName);
        chatOptions = view.findViewById(R.id.chatOptions);
        btnVideoCall = view.findViewById(R.id.btnVideoCall);
        btnVoiceCall = view.findViewById(R.id.btnVoiceCall);
        btnMic = view.findViewById(R.id.btnMic);
        btnAttach = view.findViewById(R.id.btnAttach);
        btnCamera = view.findViewById(R.id.btn_camera);

        tvUserStatus = view.findViewById(R.id.tvUserStatus);

        recording = view.findViewById(R.id.recording);
        recording.setVisibility(View.GONE);

        //attachment sheet views
        attachmentSheet = view.findViewById(R.id.attach_bottomsheet);
        attachmentSheet.setVisibility(View.GONE);
        attach_camera = view.findViewById(R.id.attach_camera);
        attach_gallery = view.findViewById(R.id.attach_gallery);
        attach_event = view.findViewById(R.id.attach_event);
        attach_audio = view.findViewById(R.id.attach_audio);
        attach_contact = view.findViewById(R.id.attach_person);
        attach_doc = view.findViewById(R.id.attach_doc);
        hideBottomsheet = view.findViewById(R.id.hide_bottomsheet);



        user = SharedPrefferanceUtil.isUserLoggedIn(requireContext());


        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).hideNavigation();
        }

        setupUserData(getReceiverDataFromArgs());
        btnSend.setVisibility(View.GONE);
        btnMic.setVisibility(View.VISIBLE);
    }

    private void initUserIds() {
        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId != null && currentUserId.equals(receiverId)) {
            throw new IllegalStateException("Self chat detected!");
        }
    }

    private void setupClickListeners() {
        Map<String, Object> memberInfo = getReceiverDataFromArgs();

        btnAttach.setOnClickListener(v -> {
            showAttachBottomSheet();
            attachmentSheet.setVisibility(View.VISIBLE);
        });

        attach_camera.setOnClickListener( v -> {
            Intent intent = new Intent(requireContext(), CameraActivity.class);
            previewLauncher.launch(intent);   // 🔥 IMPORTANT
        });

        attach_gallery.setOnClickListener(v -> openGallery());

        attach_audio.setOnClickListener(v -> openAudioPicker());

        attach_contact.setOnClickListener(v -> openContactPicker());

        attach_doc.setOnClickListener( v -> openDocumentPicker());

        attach_event.setOnClickListener( v -> {
            //add an event
        });




        hideBottomsheet.setOnClickListener(v -> attachmentSheet.setVisibility(View.GONE));

        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CameraActivity.class);
            previewLauncher.launch(intent);   // 🔥 IMPORTANT
        });

        ivProfile.setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container,
                        new FullScreenImageFragment(memberInfo.get("receiverProfileImageUrl").toString()))
                .addToBackStack(null)
                .commit());

        btnMic.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startAudioRecording();
                    attachmentSheet.setVisibility(View.GONE);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isRecording) stopAudioRecordingAndSend();
                    return true;
            }
            return false;
        });
    }



    private void stopAudioRecordingAndSend() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                //UI update status recording.. clear
                stopRecordingStatus();
                isRecording = false;

                File audioFile = new File(audioFilePath);
                if (!audioFile.exists() || audioFile.length() < 500) {
                    Toast.makeText(getContext(), "Voice too short", Toast.LENGTH_SHORT).show();
                    return;
                }

                sendAudioMessage();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAudioMessage() {
        sendMessage("audio", null, audioFilePath);
        recording.setVisibility(View.GONE);
    }

    //universal sendMessage method for any type of message
    public void sendMessage(String type, @Nullable String text, @Nullable String localFilePath) {
        String messageId = chatRef.push().getKey();
        if (messageId == null) return;

        long time = System.currentTimeMillis();

        // 1️⃣ UI Pending Message
        ChatDto msg = new ChatDto();
        msg.setMessageId(messageId);
        msg.setSenderId(currentUserId);
        msg.setReceiverId(receiverId);
        msg.setMessage(text);
        msg.setMessageType(type);
        msg.setTimestamp(time);
        msg.setStatus("pending");
        msg.setSenderLocalPath(localFilePath);

        messageList.add(msg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);

        // 2️⃣ Background Worker
        enqueueWork(messageId, type, text, localFilePath, time);
    }


    private Map<String, Object> getReceiverDataFromArgs() {
        Map<String, Object> memberInfo = new HashMap<>();
        Bundle args = getArguments();
        if (args == null) throw new IllegalStateException("ChatFragment arguments are NULL");

        receiverId = args.getString("receiverId");
        if (receiverId == null || receiverId.isEmpty())
            throw new IllegalStateException("receiverId is NULL or empty");

        memberInfo.put("receiverId", receiverId);
        memberInfo.put("receiverName", args.getString("receiverName"));
        memberInfo.put("receiverProfileImageUrl", args.getString("receiverProfileImageUrl"));

        return memberInfo;
    }

    private void initFirebaseRefs() {
        if (currentUserId == null || receiverId == null) return;
        chatRoomId = ChatRoomUtil.getChatRoomId(currentUserId, receiverId);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        chatRef = database.getReference("chats").child(chatRoomId);
        userRef = database.getReference("users");
        chatRef.keepSynced(true);
    }

    private final DownloadInterface downloadInterface = this::downloadFile;

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        // 🔥 Yahan interface pass karein
        chatAdapter = new ChatAdapter(
                requireContext(),
                messageList,
                currentUserId,
                getReceiverDataFromArgs().get("receiverProfileImageUrl").toString(),
                user.getPhotoUrl(),
                messageActionListener,
                downloadInterface,
                this // 👈 audio listen callback
        );

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }


    private void setupSendMessage() {
        btnSend.setOnClickListener(v -> sendTextMessage());
    }

    private void sendTextMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        sendMessage("text", text, null);
        etMessage.setText("");
        stopTyping();
    }

    private void normalizeLocalPaths(ChatDto msg) {

        // 🔒 Sender side only
        if (msg.getSenderId().equals(currentUserId)) {

            if (msg.getSenderLocalPath() != null) {
                File f = new File(msg.getSenderLocalPath());
                if (!f.exists()) {
                    msg.setSenderLocalPath(null); // ❗ invalid path
                }
            }

        } else {
            // 🔥 Receiver side NEVER trust senderLocalPath
            msg.setSenderLocalPath(null);
        }
    }

    private void readMessages() {
        if (chatRef == null) return;

        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final List<ChatDto> firebaseMessages = new ArrayList<>();

                // Check currentUserId here to be extra safe
                String myId = (currentUserId != null) ? currentUserId : FirebaseAuth.getInstance().getUid();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        ChatDto msg = ds.getValue(ChatDto.class);
                        if (msg != null && msg.getMessageId() != null) {
                            // isDeletedForMe ke andar bhi humne safety add ki hai
                            if (!isDeletedForMe(msg)) {
                                normalizeLocalPaths(msg);
                                firebaseMessages.add(msg);

                                // Delivered Status Update (Super Safe)
                                updateMessageStatus(myId, msg, ds);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("CHAT_DEBUG", "Error parsing: " + e.getMessage());
                    }
                }

                new Thread(() -> {
                    try {
                        if (pendingChatDao == null) return;
                        List<PendingChatEntity> pendingList = pendingChatDao.getPendingMessages(chatRoomId);

                        if (pendingList != null) {
                            for (PendingChatEntity p : pendingList) {
                                if (p.messageId == null) continue;
                                boolean exists = false;
                                for (ChatDto f : firebaseMessages) {
                                    if (Objects.equals(p.messageId, f.getMessageId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    ChatDto pDto = new ChatDto(p.messageId, p.senderId, p.receiverId,
                                            p.message, p.messageType, p.timestamp, "pending");
                                    pDto.setSenderLocalPath(p.senderLocalPath);
                                    firebaseMessages.add(pDto);
                                }
                            }
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Collections.sort(firebaseMessages, (m1, m2) ->
                                        Long.compare(m1.getTimestamp(), m2.getTimestamp()));
                                messageList.clear();
                                messageList.addAll(firebaseMessages);
                                chatAdapter.notifyDataSetChanged();
                                if (!messageList.isEmpty()) {
                                    chatRecyclerView.scrollToPosition(messageList.size() - 1);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("CHAT_ERR", "Worker thread error: " + e.getMessage());
                    }
                }).start();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateMessageStatus(String myId, ChatDto msg, DataSnapshot ds){
        if (myId != null &&
                Objects.equals(myId, msg.getReceiverId()) &&
                "sent".equals(msg.getStatus())) {
            ds.getRef().child("status").setValue("delivered");
        }
    }
    private boolean isDeletedForMe(ChatDto msg) {
        // Agar currentUserId null hai toh temporary check karein
        String myId = (currentUserId != null) ? currentUserId : FirebaseAuth.getInstance().getUid();

        if (msg == null || msg.getDeletedFor() == null || myId == null) {
            return false;
        }
        Boolean deleted = msg.getDeletedFor().get(myId);
        return deleted != null && deleted;
    }

    private void markMessagesAsSeen() {
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatDto msg = ds.getValue(ChatDto.class);
                    if (msg != null
                            && Objects.equals(msg.getReceiverId(), currentUserId)
                            && "delivered".equals(msg.getStatus())) {

                        ds.getRef().child("status").setValue("seen");
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setupTypingListener() {
        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    attachmentSheet.setVisibility(View.GONE);
                    btnMic.setVisibility(View.GONE);
                    btnSend.setVisibility(View.VISIBLE);
                    startTyping();
                } else {
                    btnMic.setVisibility(View.VISIBLE);
                    btnSend.setVisibility(View.GONE);
                    stopTyping();
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
    }

    private void startTyping() {
        FirebaseDatabase.getInstance().getReference("typing").child(chatRoomId)
                .child(currentUserId).setValue(true);
    }

    private void stopTyping() {
        FirebaseDatabase.getInstance().getReference("typing").child(chatRoomId)
                .child(currentUserId).setValue(false);
    }

    private void listenTypingStatus() {
        DatabaseReference typingRef = FirebaseDatabase.getInstance().getReference("typing")
                .child(chatRoomId).child(receiverId);
        typingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isTyping = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(isTyping)) tvUserStatus.setText("typing...");
                else tvUserStatus.setText("online");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void startRecordingStatus() {
        FirebaseDatabase.getInstance().getReference("recording")
                .child(chatRoomId)
                .child(currentUserId)
                .setValue(true); // recording started
    }

    private void stopRecordingStatus() {
        FirebaseDatabase.getInstance().getReference("recording")
                .child(chatRoomId)
                .child(currentUserId)
                .setValue(false); // recording stopped
    }

    private void listenUserStatus() {
//        DatabaseReference typingRef = FirebaseDatabase.getInstance().getReference("typing")
//                .child(chatRoomId)
//                .child(receiverId);

        DatabaseReference recordingRef = FirebaseDatabase.getInstance().getReference("recording")
                .child(chatRoomId)
                .child(receiverId);

        ValueEventListener statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Boolean isTyping = snapshot.getValue(Boolean.class);
                // check recording first
                recordingRef.get().addOnSuccessListener(recSnap -> {
                    Boolean isRecording = recSnap.getValue(Boolean.class);
                    if (Boolean.TRUE.equals(isRecording)) {
                        tvUserStatus.setText("recording audio...");
                    }
//                    else if (Boolean.TRUE.equals(isTyping)) {
//                        tvUserStatus.setText("typing...");
//                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };

//        typingRef.addValueEventListener(statusListener);
        recordingRef.addValueEventListener(statusListener);
    }


    private void listenConnection() {
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                isConnected = connected != null && connected;
                if (isConnected) {
                    tvUserStatus.setVisibility(View.VISIBLE);
                }else{
                    tvUserStatus.setVisibility(View.GONE);
                }
                Log.d("CHAT_DEBUG", connected != null && connected ? "Internet Connected" : "Internet Disconnected");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    private void listenUserStatusRealtime() {
        DatabaseReference statusRef = FirebaseDatabase.getInstance().getReference("status").child(receiverId);
        statusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean online = snapshot.child("online").getValue(Boolean.class);
                if (Boolean.TRUE.equals(online)) {
                    tvUserStatus.setText("online");
                    stopLastSeenTimer();
                } else {
                    Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);
                    if (lastSeen != null) {
                        lastSeenTime = lastSeen;
                        startLastSeenTimer();
                    } else tvUserStatus.setText("last seen recently");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void startLastSeenTimer() {
        stopLastSeenTimer();
        lastSeenRunnable = new Runnable() {
            @Override
            public void run() {
                tvUserStatus.setText(formatLastSeen(lastSeenTime));
                lastSeenHandler.postDelayed(this, 10_000);
            }
        };
        lastSeenHandler.post(lastSeenRunnable);
    }

    private void stopLastSeenTimer() {
        if (lastSeenRunnable != null) lastSeenHandler.removeCallbacks(lastSeenRunnable);
    }

    private String formatLastSeen(long time) {
        long diff = System.currentTimeMillis() - time;
        if (diff < 60_000) return "just now";
        else if (diff < 60 * 60_000) return diff / 60_000 + " min ago";
        java.util.Calendar lastCal = java.util.Calendar.getInstance();
        lastCal.setTimeInMillis(time);
        java.util.Calendar nowCal = java.util.Calendar.getInstance();
        boolean sameDay = lastCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR)
                && lastCal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR);
        if (sameDay)
            return "today at " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(time));
        return new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date(time));
    }

    private void applyChatBackground() {
        if (ThemeUtils.isDarkTheme(requireContext()))
            chatRootLayout.setBackgroundResource(R.drawable.chat_bg_dark);
        else chatRootLayout.setBackgroundResource(R.drawable.chat_bg_light);
    }

    private void setupUserData(Map<String, Object> userInfo) {
        tvUserName.setText(userInfo.get("receiverName").toString());
        Picasso.get().load(userInfo.get("receiverProfileImageUrl").toString()).into(ivProfile);
    }

    public static ChatFragment newInstance(FamilyMember member) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("receiverId", member.getUserId());
        args.putString("receiverName", member.getName());
        args.putString("receiverProfileImageUrl", member.getProfileImageUrl());
        fragment.setArguments(args);
        return fragment;
    }

    private final ChatAdapter.OnMessageActionListener messageActionListener =
            new ChatAdapter.OnMessageActionListener() {

//                @Override
//                public void onCopy(ChatDto msg) {
//                    ClipboardManager cm =
//                            (ClipboardManager) requireContext()
//                                    .getSystemService(Context.CLIPBOARD_SERVICE);
//                    cm.setPrimaryClip(
//                            ClipData.newPlainText("message", msg.getMessage())
//                    );
//                    Toast.makeText(getContext(), "Copied", Toast.LENGTH_SHORT).show();
//                }
@Override
public void onCopy(ChatDto msg) {

    ClipboardManager cm =
            (ClipboardManager) requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);

    String copyText;

    // 🔹 CONTACT MESSAGE
    if ("contact".equals(msg.getMessageType())) {

        if (msg.getMessage() == null) return;

        String[] parts = msg.getMessage().split("\\|");
        if (parts.length < 2) return;

        String phone = parts[1];

        // WhatsApp style copy
        copyText = phone;

    }
    // 🔹 NORMAL TEXT MESSAGE
    else {
        copyText = msg.getMessage();
    }

    cm.setPrimaryClip(
            ClipData.newPlainText("message", copyText)
    );

    Toast.makeText(getContext(), "Copied", Toast.LENGTH_SHORT).show();
}


                @Override
                public void onEdit(ChatDto msg) {

                    etMessage.setText(msg.getMessage());
                    etMessage.setSelection(msg.getMessage().length());

                    btnSend.setOnClickListener(v -> {

                        String newText = etMessage.getText().toString().trim();
                        if (newText.isEmpty()) return;

                        if (isPending(msg)) {
                            // 🔥 1. ROOM UPDATE (REAL SOURCE)
                            new Thread(() -> {
                                pendingChatDao.updateMessage(
                                        msg.getMessageId(),
                                        newText
                                );
                            }).start();
                            // 🔥 2. UI UPDATE
                            msg.setMessage(newText);

                            int pos = chatAdapter.getPositionById(msg.getMessageId());
                            if (pos != -1) {
                                chatAdapter.notifyItemChanged(pos);
                            }

                        } else {
                            // 🔥 SENT MESSAGE → FIREBASE
                            chatRef.child(msg.getMessageId())
                                    .child("message")
                                    .setValue(newText);
                        }
                        etMessage.setText("");
                        setupSendMessage(); // restore normal send
                    });
                }

                @Override
                public void onDeleteForMe(ChatDto msg) {

                    if (isPending(msg)) {

                        // 1️⃣ Room se delete
                        new Thread(() -> {
                            pendingChatDao.deleteById(msg.getMessageId());
                        }).start();

                        // 2️⃣ Worker cancel
                        WorkManager.getInstance(requireContext())
                                .cancelAllWorkByTag(msg.getMessageId());

                        // 3️⃣ Local file delete (audio)
                        if ("audio".equals(msg.getMessageType())) {
                            deleteLocalFile(msg);
                        }

                        // 4️⃣ UI remove
                        chatAdapter.removeMessageById(msg.getMessageId());

                    } else {

                        // 🔥 Normal Firebase delete
                        chatRef.child(msg.getMessageId())
                                .child("deletedFor")
                                .child(currentUserId)
                                .setValue(true);

                        chatAdapter.removeMessageById(msg.getMessageId());
                    }
                }


                @Override
                public void onDeleteForEveryone(ChatDto msg) {

                    if (isPending(msg)) {

                        // Pending = kabhi send hi nahi hona chahiye
                        new Thread(() -> {
                            pendingChatDao.deleteById(msg.getMessageId());
                        }).start();

                        WorkManager.getInstance(requireContext())
                                .cancelAllWorkByTag(msg.getMessageId());

                        if ("audio".equals(msg.getMessageType())) {
                            deleteLocalFile(msg);
                        }

                        chatAdapter.removeMessageById(msg.getMessageId());

                    } else {

                        // Firebase message already sent
                        Map<String, Object> update = new HashMap<>();
                        update.put("message", "This message was deleted");
                        update.put("messageType", "deleted");

                        chatRef.child(msg.getMessageId()).updateChildren(update);
                    }
                }

            };


    private void startAudioRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, 101);
            return;
        }

        try {
            File audioDir = getMediaDir("audio");
            File audioFile = new File(
                    audioDir,
                    "AUD_" + System.currentTimeMillis() + ".m4a"
            );
            audioFilePath = audioFile.getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            // 🔥 UI update status recording..
            startRecordingStatus();
            isRecording = true;
            recording.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            recording.setVisibility(View.GONE);
            e.printStackTrace();
            Toast.makeText(getContext(), "Recording failed", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onAudioListened(ChatDto msg) {
        if (msg == null || chatRef == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put("listen", true);

        chatRef.child(msg.getMessageId()).updateChildren(update);
    }


    private File getMediaDir(String type) {
        File base = new File(
                requireContext().getExternalFilesDir(null),
                "SafeSphere/media/" + type
        );
        if (!base.exists()) base.mkdirs();
        return base;
    }

    // 🔥 1. Purana downloadFile aur startDownloadMedia hata kar sirf ye ek rakhein
    public void downloadFile(ChatDto msg, int position) {
        // Check if chatRef is null to prevent crash
        if (chatRef == null) {
            initFirebaseRefs();
            if (chatRef == null) {
                Toast.makeText(getContext(), "Connection error, try again", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        new Thread(() -> {
            try {
                URL url = new URL(msg.getMessage());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.connect();

                // Folder creation
//      old          File folder = getMediaDir("audio");

                String mediaFolder = "audio"; // new

                if ("document".equals(msg.getMessageType())) { //new
                    mediaFolder = "documents";  //new
                }

                File folder = getMediaDir(mediaFolder);


                if (!folder.exists()) folder.mkdirs();

//    old            File file = new File(
//                        folder,
//                        "AUD_" + msg.getMessageId() + ".m4a"
//                );

                File file;  // new from this

//                if ("document".equals(msg.getMessageType())) {
//
//                    String name = msg.getFileName() != null
//                            ? msg.getFileName()
//                            : "DOC_" + msg.getMessageId();
//
//                    String ext = msg.getFileExtension() != null
//                            ? msg.getFileExtension()
//                            : "";
//
//                    file = new File(folder, name + (ext.isEmpty() ? "" : "." + ext));
//
//                }
                if ("document".equals(msg.getMessageType())) {
                    String name = msg.getFileName() != null
                            ? msg.getFileName()
                            : "DOC_" + msg.getMessageId();

                    // Agar filename me already "." hai, extension mat add karo
                    if (msg.getFileName() != null && msg.getFileName().contains(".")) {
                        file = new File(folder, name);
                    } else {
                        String ext = msg.getFileExtension() != null ? msg.getFileExtension() : "";
                        file = new File(folder, name + (ext.isEmpty() ? "" : "." + ext));
                    }
                }
                else {
                    // audio / video
                    file = new File(
                            folder,
                            "AUD_" + msg.getMessageId() + ".m4a"
                    );
                }   //to this


                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(file);

                byte[] data = new byte[4096];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                output.close();
                input.close();

                // Update UI and Firebase on Main Thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // ✅ UI + Firebase update (sender + receiver)
                        String localPath = file.getAbsolutePath();

                        Map<String, Object> map = new HashMap<>();
//                        map.put("listen", false);

                        if (currentUserId.equals(msg.getSenderId())) {
                            msg.setSenderLocalPath(localPath);
                            map.put("senderLocalPath", localPath);
                        } else {
                            msg.setReceiverLocalPath(localPath);
                            map.put("receiverLocalPath", localPath);
                        }

                        msg.setDownloading(false);
                        msg.setDownloadFailed(false);

                        chatRef.child(msg.getMessageId()).updateChildren(map);
                        chatAdapter.notifyItemChanged(position);

                        Log.d("CHAT_DEBUG", "Download success");
                    });

                }

            } catch (Exception e) {
                Log.e("CHAT_ERR", "Download error: " + e.getMessage());

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // ❌ UI refresh taake retry icon show ho
                        msg.setDownloading(false);
                        msg.setDownloadFailed(true);
                        chatAdapter.notifyItemChanged(position);
                        Toast.makeText(getContext(), "Download failed, tap to retry "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void deleteLocalFile(ChatDto msg) {
        String path = currentUserId.equals(msg.getSenderId())
                ? msg.getSenderLocalPath()
                : msg.getReceiverLocalPath();

        if (path == null){
            Toast.makeText(getContext(), "File not found: by deleteLocalFile() method in chatFragment", Toast.LENGTH_LONG).show();
            return;
        }

        File file = new File(path);
        if (file.exists()) file.delete();
    }

    private void enqueueWork(String msgId, String type, String text, String path, long time) {
        // 1. Tiny DB mein foran save (Background thread)
        new Thread(() -> {
            PendingChatEntity p = new PendingChatEntity();
            p.messageId = msgId;
            p.chatRoomId = chatRoomId;
            p.senderId = currentUserId;
            p.receiverId = receiverId;
            p.message = text;
            p.messageType = type;
            p.timestamp = time;
            p.senderLocalPath = path;
            if (pendingChatDao != null) pendingChatDao.insert(p);
        }).start();

        String fileName = null;
        String fileExtension = null;

        if ("document".equals(type) && path != null) {

            File f = new File(path);
            fileName = f.getName();        // e.g report.docx

            int dot = fileName.lastIndexOf(".");
            if (dot != -1) {
                fileExtension = fileName.substring(dot); // .docx
            }
        }


        // 2. WorkManager Setup
        androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putString("messageId", msgId)
                .putString("chatRoomId", chatRoomId)
                .putString("senderId", currentUserId)
                .putString("receiverId", receiverId)
                .putString("messageType", type)
                .putString("text", text)
                .putString("filePath", path)
                .putLong("timestamp", time)
                .putString("fileName", fileName)
                .putString("fileExtension", fileExtension)
                .build();

        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(
                        com.example.safesphere.offlineMessageMachenism.SendMessageWorker.class
                )
                        .addTag(msgId)   // 🔥 VERY IMPORTANT
                        .setInputData(inputData)
                        .setConstraints(
                                new Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .build()
                        )
                        .build();

        WorkManager.getInstance(requireContext())
                .enqueue(workRequest);
    }

    private boolean isPending(ChatDto msg) {
        return "pending".equals(msg.getStatus());
    }

    private void showAttachBottomSheet() {
        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext(),
                        R.style.BottomSheetDialogTheme);

        View view = getLayoutInflater()
                .inflate(R.layout.bottomsheet_attach, null);

        dialog.setContentView(view);
        dialog.setCanceledOnTouchOutside(true);

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLastSeenTimer();
    }

}
