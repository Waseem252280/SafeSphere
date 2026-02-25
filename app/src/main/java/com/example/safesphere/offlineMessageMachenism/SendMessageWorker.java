package com.example.safesphere.offlineMessageMachenism;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.safesphere.offlineMessageMachenism.AppDatabase;
import com.example.safesphere.dto.ChatDto;
import com.example.safesphere.services.CloudinaryService;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.FirebaseDatabase;

public class SendMessageWorker extends Worker {

    private PendingChatDao pendingDao;

    public SendMessageWorker(@NonNull Context context,
                             @NonNull WorkerParameters params) {
        super(context, params);
        pendingDao = AppDatabase.getInstance(context).pendingChatDao();
    }

    @NonNull
    @Override
    public Result doWork() {


        InputData data = readInputData();
        if (data == null) return Result.failure();

        try {
            if (!isMessageStillPending(data.messageId)) {
                return Result.success(); // deleted / cancelled
            }

            String finalContent = prepareContent(data);
            ChatDto dto = buildChatDto(data, finalContent);

            pushToFirebase(data.chatRoomId, data.messageId, dto);
            cleanupRoom(data.messageId);

            return Result.success();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

    // ===================== FUNCTIONS =====================

    /** 🔹 Step 1: Read input safely */
    private InputData readInputData() {

        String fileName = getInputData().getString("fileName");
        String fileExtension = getInputData().getString("fileExtension");
        String messageId  = getInputData().getString("messageId");
        String chatRoomId = getInputData().getString("chatRoomId");
        String senderId   = getInputData().getString("senderId");
        String receiverId = getInputData().getString("receiverId");
        String type       = getInputData().getString("messageType");
        String text       = getInputData().getString("text");
        String filePath   = getInputData().getString("filePath");
        long timestamp    = getInputData().getLong(
                "timestamp",
                System.currentTimeMillis()
        );

        if (messageId == null || chatRoomId == null || senderId == null) {
            return null;
        }

        return new InputData(
                messageId, chatRoomId, senderId,
                receiverId, type, text, filePath, fileName, fileExtension, timestamp
        );
    }

    /** 🔹 Step 2: Check pending message exists */
    private boolean isMessageStillPending(String messageId) {
        return pendingDao.getByMessageId(messageId) != null;
    }

    /** 🔹 Step 3: Prepare content (upload if audio) */
//    private String prepareContent(InputData data) throws Exception {
//
//        if ("audio".equals(data.type)
//                && data.filePath != null
//                && !data.filePath.isEmpty()) {
//
//            CloudinaryService.initCloudinary(getApplicationContext());
//            return Tasks.await(
//                    CloudinaryService.uploadAudio(data.filePath)
//            );
//        }
//
//        return data.text;
//    }

    private String prepareContent(InputData data) throws Exception {

        if (data.filePath != null && !data.filePath.isEmpty()) {

            CloudinaryService.initCloudinary(getApplicationContext());

            switch (data.type) {

                case "audio":
                    return Tasks.await(
                            CloudinaryService.uploadMedia(data.filePath,"video")
                    );

                case "image":
                    return Tasks.await(
                            CloudinaryService.uploadImage(data.filePath)
                    );

                case "video":
                    return Tasks.await(
                            CloudinaryService.uploadMedia(data.filePath,"video")
                    );

                case "document":
                    return Tasks.await(
                            CloudinaryService.uploadMedia(data.filePath,"raw")
                    );
            }
        }

        return data.text;
    }


    /** 🔹 Step 4: Build DTO */
    private ChatDto buildChatDto(InputData data, String finalContent) {

        ChatDto dto = new ChatDto(
                data.messageId,
                data.senderId,
                data.receiverId,
                finalContent,
                data.type,
                data.timestamp,
                "sent"
        );

        if (!"text".equals(data.type)) {
            dto.setSenderLocalPath(data.filePath);
        }

        if ("document".equals(data.type)) {
            dto.setFileName(data.fileName);
            dto.setFileExtension(data.fileExtension);
        }


        return dto;
    }

    /** 🔹 Step 5: Push to Firebase */
    private void pushToFirebase(
            String chatRoomId,
            String messageId,
            ChatDto dto
    ) throws Exception {

        Tasks.await(
                FirebaseDatabase.getInstance()
                        .getReference("chats")
                        .child(chatRoomId)
                        .child(messageId)
                        .setValue(dto)
        );
    }

    /** 🔹 Step 6: Cleanup Room */
    private void cleanupRoom(String messageId) {
        pendingDao.deleteById(messageId);
    }

    // ===================== HELPER MODEL =====================

    private static class InputData {
        String messageId;
        String chatRoomId;
        String senderId;
        String receiverId;
        String type;
        String text;
        String filePath;
        String fileName;
        String fileExtension;
        long timestamp;

        InputData(String messageId,
                  String chatRoomId,
                  String senderId,
                  String receiverId,
                  String type,
                  String text,
                  String filePath,
                  String fileName,
                  String fileExtension,
                  long timestamp) {

            this.messageId = messageId;
            this.chatRoomId = chatRoomId;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.type = type;
            this.text = text;
            this.filePath = filePath;
            this.fileName = fileName;
            this.fileExtension = fileExtension;
            this.timestamp = timestamp;
        }
    }
}
