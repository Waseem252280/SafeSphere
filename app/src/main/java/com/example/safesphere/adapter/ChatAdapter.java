package com.example.safesphere.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safesphere.R;
import com.example.safesphere.dto.ChatDto;
import com.example.safesphere.fragments.ChatFragment;
import com.example.safesphere.fragments.FullScreenImageFragment;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.os.Handler;
import android.os.Looper;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnAudioListenListener {
        void onAudioListened(ChatDto msg);
    }


    public interface OnMessageActionListener {
        void onCopy(ChatDto msg);
        void onEdit(ChatDto msg);
        void onDeleteForMe(ChatDto msg);
        void onDeleteForEveryone(ChatDto msg);
    }

    private static String playingMessageId = null;

    private static final int TYPE_SENDER = 1;
    private static final int TYPE_RECEIVER = 2;

    private final Context context;
    private final List<ChatDto> messageList;
    private final String currentUserId;
    private final String receiverPhotoUrl;
    private final String senderPhotoUrl;
    private final OnMessageActionListener listener;

    private static MediaPlayer mediaPlayer;
    private static int playingPosition = -1;
    private OnAudioListenListener audioListenListener;

    //voice duration counter
    private final Handler audioHandler = new Handler(Looper.getMainLooper());
    private Runnable audioCountdownRunnable;




    // ... (Purana code)
    private final ChatFragment.DownloadInterface downloadInterface;

    public ChatAdapter(Context context,
                       List<ChatDto> messageList,
                       String currentUserId,
                       String receiverPhotoUrl,
                       String senderPhotoUrl,
                       OnMessageActionListener listener,
                       ChatFragment.DownloadInterface downloadInterface,
                       OnAudioListenListener audioListenListener) {

        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.receiverPhotoUrl = receiverPhotoUrl;
        this.senderPhotoUrl = senderPhotoUrl;
        this.listener = listener;
        this.downloadInterface = downloadInterface;
        this.audioListenListener = audioListenListener;
    }


    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getSenderId().equals(currentUserId) ? TYPE_SENDER : TYPE_RECEIVER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(
                viewType == TYPE_SENDER ? R.layout.item_chat_sender : R.layout.item_chat_reciever,
                parent, false);
        return viewType == TYPE_SENDER ? new SenderVH(v) : new ReceiverVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatDto msg = messageList.get(position);
        BaseViewHolder vh = (BaseViewHolder) holder;

        // 1. Reset Views (Avoid view recycling bugs)
        vh.resetUI();

        // 2. Set Time
        vh.time.setText(formatTime(msg.getTimestamp()));

        // 3. Handle Content Logic
        if ("This message was deleted".equals(msg.getMessage())) {
            vh.bindDeletedMessage();
            stopAudioIfThisMessageIsPlaying(msg.getMessageId());
        }
        else if ("audio".equals(msg.getMessageType())) {
            bindAudioMessage(vh, msg, position);
        }
        else if ("image".equals(msg.getMessageType())) {
            vh.bindImageMessage(msg, currentUserId, downloadInterface);
        }
        else if ("video".equals(msg.getMessageType())) {
            vh.bindVideoMessage(msg, currentUserId, downloadInterface);
        }
        else if ("document".equals(msg.getMessageType())) {
            vh.bindDocumentMessage(msg, currentUserId, downloadInterface);
        }else if ("contact".equals(msg.getMessageType())) {
            vh.bindContactMessage(msg);
        } else {
            vh.bindTextMessage(msg.getMessage());
        }


        // 4. Sender Specific Logic (Status)
        if (vh instanceof SenderVH) {
            ((SenderVH) vh).setStatusIcon(msg.getStatus());
        }

        // 5. Long Press Menu
        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, msg);
            return true;
        });

        vh.imgMessage.setOnLongClickListener(v->{
            showPopupMenu(v, msg);
            return true;
        });

        vh.docContainer.setOnLongClickListener(v->{
            showPopupMenu(v, msg);
            return true;
        });

        vh.videoContainer.setOnLongClickListener(v->{
            showPopupMenu(v, msg);
            return true;
        });

        vh.imgMessage.setOnClickListener(v->{
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new FullScreenImageFragment(msg.getMessage()))
                    .addToBackStack(null)
                    .commit();
        });
        vh.contactContainer.setOnLongClickListener(v->{
            showPopupMenu(v, msg);
            return true;
        });
    }

    //payload for smooth voice countdown duration
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains("COUNTER_ONLY")) {
            // Sirf Play/Pause icon ko refresh karo
            BaseViewHolder vh = (BaseViewHolder) holder;
            ChatDto msg = messageList.get(position);

            vh.audioPlayBtn.setImageResource(
                    position == playingPosition ? R.drawable.ic_pause : R.drawable.ic_play
            );
        } else {
            // Baaki sab cases ke liye normal bind
            super.onBindViewHolder(holder, position, payloads);
        }
    }


    private void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        playingPosition = -1;
        playingMessageId = null;
    }


    public void stopAudioIfThisMessageIsPlaying(String deletedMessageId) {

        if (playingMessageId == null) return;

        // 🔥 ONLY stop if SAME voice
        if (!playingMessageId.equals(deletedMessageId)) return;

        stopAudio();
    }

    private void showPopupMenu(View v, ChatDto msg) {
        PopupMenu menu = new PopupMenu(context, v);

        boolean isSender = msg.getSenderId().equals(currentUserId);
        boolean isText = "text".equals(msg.getMessageType());
        boolean isDeleted = "This message was deleted".equals(msg.getMessage());
        boolean isPending = "pending".equals(msg.getStatus());
        boolean isContact = "contact".equals(msg.getMessageType());

        // 🔴 MESSAGE ALREADY DELETED
        if (isDeleted) {

            menu.getMenu().add("Delete for me")
                    .setIcon(R.drawable.ic_delete);

        } else {

            // -------- SENDER --------
            if (isSender) {

                // ✅ Edit ONLY if text AND NOT pending
                if (isText && !isPending) {
                    menu.getMenu().add("Edit")
                            .setIcon(R.drawable.ic_editing);
                }

                if (isText|| isContact) {
                    menu.getMenu().add("Copy")
                            .setIcon(R.drawable.ic_copy);
                }

                menu.getMenu().add("Delete for me")
                        .setIcon(R.drawable.ic_delete);

                menu.getMenu().add("Delete for everyone")
                        .setIcon(R.drawable.ic_delete_forever);

            }
            // -------- RECEIVER --------
            else {

                if (isText || isContact) {
                    menu.getMenu().add("Copy")
                            .setIcon(R.drawable.ic_copy);
                }

                menu.getMenu().add("Delete for me")
                        .setIcon(R.drawable.ic_delete);
            }
        }

        forceShowIcons(menu);

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            switch (title) {
                case "Copy":
                    listener.onCopy(msg);
                    break;

                case "Edit":
                    listener.onEdit(msg);
                    break;

                case "Delete for me":
                    listener.onDeleteForMe(msg);
                    break;

                case "Delete for everyone":
                    listener.onDeleteForEveryone(msg);
                    break;
            }
            return true;
        });

        menu.show();
    }


    private void forceShowIcons(PopupMenu menu) {
        try {
            Field[] fields = menu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object helper = field.get(menu);
                    Method method = helper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class);
                    method.invoke(helper, true);
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private String formatTime(long time) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(time));
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    public void removeMessageById(String id) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).getMessageId().equals(id)) {
                messageList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    // ===================== VIEW HOLDERS =====================

    static abstract class BaseViewHolder extends RecyclerView.ViewHolder {
        TextView message, time, audioDuration;
        ImageView deletedIcon, audioPlayBtn, audioWave, profileImage, ivMic, retryDownloading;
        FrameLayout downloadingFrame, profileFrame;
        ImageView imgMessage, videoThumb, btnPlayVideo;
        FrameLayout videoContainer, imageContainer;
        LinearLayout docContainer;
        TextView docName, docSize, videoDuration;
        ImageView docIcon;
        LinearLayout contactContainer;
        TextView contactName, contactPhone;


        public BaseViewHolder(@NonNull View v) {
            super(v);
            imgMessage = v.findViewById(R.id.imgMessage);
            videoThumb = v.findViewById(R.id.videoThumb);
            btnPlayVideo = v.findViewById(R.id.btnPlayVideo);
            videoContainer = v.findViewById(R.id.videoContainer);
            docContainer = v.findViewById(R.id.docContainer);
            imageContainer = v.findViewById(R.id.imageContainer);
            docName = v.findViewById(R.id.docName);
            docSize = v.findViewById(R.id.docSize);
            docIcon = v.findViewById(R.id.docIcon);
            videoDuration = v.findViewById(R.id.videoDuration);

            message = v.findViewById(R.id.tvMessage);
            time = v.findViewById(R.id.tvTime);
            deletedIcon = v.findViewById(R.id.deletedIcon);
            audioPlayBtn = v.findViewById(R.id.btnPlayAudio);
            audioDuration = v.findViewById(R.id.audioDuration);
            audioWave = v.findViewById(R.id.audioWave);
            profileImage = v.findViewById(R.id.profile_image);
            ivMic = v.findViewById(R.id.ivMic);
            downloadingFrame = v.findViewById(R.id.donwloadingFrame);
            retryDownloading = v.findViewById(R.id.restartDownloading);
            profileFrame = v.findViewById(R.id.profileFrame);

            contactContainer = v.findViewById(R.id.contactContainer);
            contactName = v.findViewById(R.id.contactName);
            contactPhone = v.findViewById(R.id.contactPhone);

        }

        void resetUI() {
            // Contact
            if (contactContainer != null) {
                contactContainer.setVisibility(View.GONE);
            }

            // IMAGE
            if (imgMessage != null) {
                imgMessage.setImageDrawable(null);
                imageContainer.setVisibility(View.GONE);
            }

            // VIDEO
            if (videoThumb != null) {
                videoThumb.setImageDrawable(null);
            }
            if (videoContainer != null) {
                videoContainer.setVisibility(View.GONE);
            }

            // DOCUMENT
            if (docContainer != null) {
                docContainer.setVisibility(View.GONE);
            }
            if (docName != null) docName.setText("");
            if (docSize != null) docSize.setText("");

            // COMMON
            profileFrame.setVisibility(View.GONE);
            message.setVisibility(View.GONE);
            audioPlayBtn.setVisibility(View.GONE);
            deletedIcon.setVisibility(View.GONE);
            audioWave.setVisibility(View.GONE);
            audioDuration.setVisibility(View.GONE);
            downloadingFrame.setVisibility(View.GONE);
            retryDownloading.setVisibility(View.GONE);
        }


        void bindDeletedMessage() {
            message.setText("This message was deleted");
            message.setTextColor(itemView.getResources().getColor(R.color.lightGray));
            message.setVisibility(View.VISIBLE);
            deletedIcon.setVisibility(View.VISIBLE);
        }

        void bindTextMessage(String text) {
            message.setVisibility(View.VISIBLE);
            message.setText(text);
            message.setTextColor(itemView.getResources().getColor(R.color.white));
        }

        void bindAudioUI(String url) {
            profileFrame.setVisibility(View.VISIBLE);
            downloadingFrame.setVisibility(View.GONE);
            audioPlayBtn.setVisibility(View.VISIBLE);
            audioWave.setVisibility(View.VISIBLE);
            audioDuration.setVisibility(View.VISIBLE);
            ivMic.setVisibility(View.VISIBLE);
            Picasso.get().load(url).noFade().config(Bitmap.Config.ARGB_8888).into(profileImage);
        }

        void bindImageMessage(
                ChatDto msg,
                String currentUserId,
                ChatFragment.DownloadInterface downloadInterface) {

            imageContainer.setVisibility(View.VISIBLE);

            boolean isMe = msg.getSenderId().equals(currentUserId);
            String localPath = isMe ? msg.getSenderLocalPath() : msg.getReceiverLocalPath();
            String url = msg.getMessage();
            if (url != null && url.startsWith("http://")) {
                msg.setMessage(url.replace("http://", "https://"));
            }

            retryDownloading.setVisibility(View.GONE);
            downloadingFrame.setVisibility(View.GONE);

            // ================= FILE EXISTS =================
            if (localPath != null && new File(localPath).exists()) {

                Picasso.get()
                        .load(new File(localPath))
                        .fit()
                        .centerCrop()
                        .into(imgMessage);

                return;
            }

            // ================= DOWNLOADING =================
            if (msg.isDownloading()) {
                downloadingFrame.setVisibility(View.VISIBLE);
                return;
            }

            // ================= FAILED =================
            if (msg.isDownloadFailed()) {
                retryDownloading.setVisibility(View.VISIBLE);

                retryDownloading.setOnClickListener(v -> {
                    msg.setDownloadFailed(false);
                    msg.setDownloading(true);
                    downloadingFrame.setVisibility(View.VISIBLE);
                    retryDownloading.setVisibility(View.GONE);

                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && downloadInterface != null) {
                        downloadInterface.onDownloadRequested(msg, pos);
                    }

                });
                return;
            }

            // ================= AUTO DOWNLOAD =================
            if (!isMe) {
                msg.setDownloading(true);
                downloadingFrame.setVisibility(View.VISIBLE);

                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && downloadInterface != null) {
                    downloadInterface.onDownloadRequested(msg, pos);
                }

            }

            // ⛔ SENDER SIDE → show download icon only
            if (isMe) {
                retryDownloading.setVisibility(View.VISIBLE);

                retryDownloading.setOnClickListener(v -> {
                    msg.setDownloadFailed(false);
                    msg.setDownloading(true);

                    retryDownloading.setVisibility(View.GONE);
                    downloadingFrame.setVisibility(View.VISIBLE);

                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && downloadInterface != null) {
                        downloadInterface.onDownloadRequested(msg, pos);
                    }
                });
            }
        }

        void bindVideoMessage(
                ChatDto msg,
                String currentUserId,
                ChatFragment.DownloadInterface downloadInterface) {

            videoContainer.setVisibility(View.VISIBLE);

            boolean isMe = msg.getSenderId().equals(currentUserId);
            String localPath = isMe ? msg.getSenderLocalPath() : msg.getReceiverLocalPath();

            retryDownloading.setVisibility(View.GONE);
            downloadingFrame.setVisibility(View.GONE);

            // ================= FILE EXISTS =================
            if (localPath != null && new File(localPath).exists()) {

                Bitmap thumb = getVideoThumbnail(localPath);
                if (thumb != null) videoThumb.setImageBitmap(thumb);
                btnPlayVideo.setVisibility(View.VISIBLE);
                videoDuration.setText(getVideoDuration(localPath));

                //play video
                btnPlayVideo.setOnClickListener(v ->{
                        playVideo(itemView.getContext(),localPath, "video/*");
                });
                return;
            }

            // ================= DOWNLOADING =================
            if (msg.isDownloading()) {
                downloadingFrame.setVisibility(View.VISIBLE);
                return;
            }

            // ================= FAILED =================
            if (msg.isDownloadFailed()) {
                retryDownloading.setVisibility(View.VISIBLE);

                retryDownloading.setOnClickListener(v -> {
                    msg.setDownloadFailed(false);
                    msg.setDownloading(true);
                    downloadingFrame.setVisibility(View.VISIBLE);
                    retryDownloading.setVisibility(View.GONE);
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && downloadInterface != null) {
                        downloadInterface.onDownloadRequested(msg, pos);
                    }

                });
                return;
            }

            // ================= AUTO DOWNLOAD =================
            if (!isMe) {
                msg.setDownloading(true);
                downloadingFrame.setVisibility(View.VISIBLE);

                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && downloadInterface != null) {
                    downloadInterface.onDownloadRequested(msg, pos);
                }
            }


            // ⛔ SENDER SIDE → show download icon only
            if (isMe) {
                retryDownloading.setVisibility(View.VISIBLE);

                retryDownloading.setOnClickListener(v -> {
                    msg.setDownloadFailed(false);
                    msg.setDownloading(true);

                    retryDownloading.setVisibility(View.GONE);
                    downloadingFrame.setVisibility(View.VISIBLE);

                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && downloadInterface != null) {
                        downloadInterface.onDownloadRequested(msg, pos);
                    }
                });
            }
        }

        void playVideo(Context context, String localPath, String mimeType){
            try {
                File file = new File(localPath);

                Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".provider",
                        file
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                context.startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(context, "Unable to play media", Toast.LENGTH_SHORT).show();
            }
        }

        String getVideoDuration(String path) {
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(path);
                long durationMs = Long.parseLong(
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                );
                mmr.release();

                int sec = (int) (durationMs / 1000);
                int min = sec / 60;
                int s = sec % 60;

                return String.format(Locale.getDefault(), "%d:%02d", min, s);
            } catch (Exception e) {
                return "0:00";
            }
        }


        Bitmap getVideoThumbnail(String path) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);
            Bitmap bmp = retriever.getFrameAtTime(0);
            try {
                retriever.release();
            }catch (Exception e){
                Log.e("TAG", "getVideoThumbnail: ", new Throwable(e.getMessage()));
            }
            return bmp;
        }

        void bindDocumentMessage(
                ChatDto msg,
                String currentUserId,
                ChatFragment.DownloadInterface downloadInterface) {

            docContainer.setVisibility(View.VISIBLE);
            boolean isMe = msg.getSenderId().equals(currentUserId);
            String localPath = isMe ? msg.getSenderLocalPath() : msg.getReceiverLocalPath();

            retryDownloading.setVisibility(View.GONE);
            downloadingFrame.setVisibility(View.GONE);

            // ================= FILE EXISTS =================
            if (localPath != null && new File(localPath).exists()) {

                File file = new File(localPath);
                String fileName = file.getName();
                docName.setText(fileName);
                docSize.setText(getFileSize(file.length()));

                String fileType = fileName.substring(fileName.lastIndexOf("."));
                // bind file icon according to file type
                bindFileIcon(fileType);
                docContainer.setOnClickListener(v -> {
                    // open document intent
                    openDocument(itemView.getContext(), file);
                });
                return;
            }


            // ================= DOWNLOADING =================
            if (msg.isDownloading()) {
                downloadingFrame.setVisibility(View.VISIBLE);
                return;
            }

            // ================= FAILED =================
            if (msg.isDownloadFailed()) {
                retryDownloading.setVisibility(View.VISIBLE);

                retryDownloading.setOnClickListener(v -> {
                    msg.setDownloadFailed(false);
                    msg.setDownloading(true);
                    downloadingFrame.setVisibility(View.VISIBLE);
                    retryDownloading.setVisibility(View.GONE);
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && downloadInterface != null) {
                        downloadInterface.onDownloadRequested(msg, pos);
                    }
                });
                return;
            }

            // ================= AUTO DOWNLOAD =================
            if (!isMe) {
                msg.setDownloading(true);
                downloadingFrame.setVisibility(View.VISIBLE);
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && downloadInterface != null) {
                    downloadInterface.onDownloadRequested(msg, pos);
                }

            }

            // ⛔ SENDER SIDE → show download icon only
            if (isMe) {
                retryDownloading.setVisibility(View.VISIBLE);

                retryDownloading.setOnClickListener(v -> {
                    msg.setDownloadFailed(false);
                    msg.setDownloading(true);

                    retryDownloading.setVisibility(View.GONE);
                    downloadingFrame.setVisibility(View.VISIBLE);

                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && downloadInterface != null) {
                        downloadInterface.onDownloadRequested(msg, pos);
                    }
                });
            }
        }

        void bindFileIcon(String fileType){
            switch (fileType){
                case ".pdf":
                    docIcon.setImageResource(R.drawable.ic_pdf);
                    break;
                case ".doc":
                case ".docx":
                    docIcon.setImageResource(R.drawable.ic_docx);
                    break;
                case ".xls":
                case ".xlsx":
                    docIcon.setImageResource(R.drawable.ic_excel);
                    break;
                case ".zip":
                    docIcon.setImageResource(R.drawable.ic_zip);
                    break;
                case ".ppt":
                    case ".pptx":
                    docIcon.setImageResource(R.drawable.ic_pptx);
                    break;
                default:
            }
        }

        String getFileSize(long size) {
            if (size <= 0) return "0 KB";

            final String[] units = new String[]{"B", "KB", "MB", "GB"};
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            return String.format(Locale.getDefault(), "%.1f %s",
                    size / Math.pow(1024, digitGroups),
                    units[digitGroups]);
        }

//        private void openDocument(Context context, File file) {
//            try {
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                intent.setDataAndType(
//                        androidx.core.content.FileProvider.getUriForFile(
//                                context,
//                                context.getPackageName() + ".provider",
//                                file
//                        ),
//                        "*/*"
//                );
//                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                context.startActivity(intent);
//            } catch (Exception e) {
//                Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show();
//            }
//        }

        private void openDocument(Context context, File file) {
            try {
                Uri uri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".provider",
                        file
                );

                String mimeType = getMimeType(file.getName());

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(
                        Intent.createChooser(intent, "Open document")
                );

            } catch (Exception e) {
                Toast.makeText(context, "No app found to open this document", Toast.LENGTH_SHORT).show();
            }
        }
        private String getMimeType(String fileName) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);

            if (extension != null && !extension.isEmpty()) {
                String mime = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(extension.toLowerCase());
                if (mime != null) return mime;
            }

            return "application/octet-stream";
        }



        void bindContactMessage(ChatDto msg) {

            contactContainer.setVisibility(View.VISIBLE);

            if (msg.getMessage() == null) return;

            String[] parts = msg.getMessage().split("\\|");
            if (parts.length < 2) return;

            String name = parts[0];
            String phone = parts[1];

            contactName.setText(name);
            contactPhone.setText(phone);

            // 👉 Click action (WhatsApp style)
            contactContainer.setOnClickListener(v -> {
                Context ctx = itemView.getContext();

                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setType("vnd.android.cursor.dir/contact");
                intent.putExtra(android.provider.ContactsContract.Intents.Insert.NAME, name);
                intent.putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, phone);

                ctx.startActivity(intent);
            });
        }



    }

    static class SenderVH extends BaseViewHolder {
        ImageView status;
        SenderVH(View v) { super(v); status = v.findViewById(R.id.imgStatus); }

        @Override
        void resetUI() { super.resetUI(); status.setVisibility(View.GONE); }

        void setStatusIcon(String statusStr) {
            if (statusStr == null) return;
            status.setVisibility(View.VISIBLE);
            switch (statusStr) {
                case "pending": status.setImageResource(R.drawable.ic_pending); break;
                case "sent": status.setImageResource(R.drawable.ic_sent); break;
                case "delivered": status.setImageResource(R.drawable.ic_delivered); break;
                case "seen": status.setImageResource(R.drawable.ic_seen); break;
                default: status.setVisibility(View.GONE);
            }
        }
    }

    static class ReceiverVH extends BaseViewHolder {
        ReceiverVH(View v) { super(v); }
    }

    private void bindAudioMessage(BaseViewHolder vh, ChatDto msg, int position) {

        boolean isMe = msg.getSenderId().equals(currentUserId);
        String profileUrl = isMe ? senderPhotoUrl : receiverPhotoUrl;
        vh.bindAudioUI(profileUrl);

        String localPath = isMe ? msg.getSenderLocalPath() : msg.getReceiverLocalPath();
        File audioFile = localPath != null ? new File(localPath) : null;

        vh.retryDownloading.setVisibility(View.GONE);
        vh.downloadingFrame.setVisibility(View.GONE);
        vh.audioPlayBtn.setVisibility(View.GONE);

        // ================= FILE EXISTS =================
        if (audioFile != null && audioFile.exists()) {

            vh.audioDuration.setText(getAudioDuration(localPath));


            vh.audioPlayBtn.setVisibility(View.VISIBLE);

            if (msg.isListen()) {
                // 🔵 listened → sender + receiver dono ke liye
                vh.ivMic.setColorFilter(context.getColor(R.color.sky_blue));
                vh.audioPlayBtn.setColorFilter(context.getColor(R.color.sky_blue));

            } else if (!isMe) {
                // 🟢 receiver & not listened
                vh.ivMic.setColorFilter(context.getColor(R.color.green));
                vh.audioPlayBtn.setColorFilter(context.getColor(R.color.green));

            } else {
                // ⚪ sender & not listened
                vh.ivMic.setColorFilter(context.getColor(R.color.lightGray));
                vh.audioPlayBtn.setColorFilter(context.getColor(R.color.lightGray));
            }

            vh.audioPlayBtn.setImageResource(
                    position == playingPosition ? R.drawable.ic_pause : R.drawable.ic_play
            );

            vh.audioPlayBtn.setOnClickListener(v ->
                    handleAudioPlayback(position, localPath, msg, vh.audioDuration)
            );

            return;
        }


        // ⛔ Downloading / retry → neutral mic
        vh.ivMic.setColorFilter(context.getColor(R.color.lightGray));

        // ❌ cancel downloading
        vh.downloadingFrame.setOnClickListener(v -> {
            vh.downloadingFrame.setVisibility(View.GONE);
            vh.retryDownloading.setVisibility(View.VISIBLE);
            msg.setDownloading(false);
            msg.setDownloadFailed(true);
//                notifyItemChanged(position);
            notifySafe(msg);
        });

        // ================= DOWNLOADING =================
        if (msg.isDownloading()) {
            vh.downloadingFrame.setVisibility(View.VISIBLE);
          return;
        }

        // ================= FAILED / NOT DOWNLOADED =================
        vh.retryDownloading.setVisibility(View.VISIBLE);

        vh.retryDownloading.setOnClickListener(v -> {
            msg.setDownloadFailed(false);
            msg.setDownloading(true);

            vh.retryDownloading.setVisibility(View.GONE);
            vh.downloadingFrame.setVisibility(View.VISIBLE);

            if (downloadInterface != null) {
                downloadInterface.onDownloadRequested(msg, position);
            }
        });

        // 🚫 Sender → NEVER auto download
        if (isMe) return;

        // ✅ Receiver → auto download ONCE
        msg.setDownloading(true);
        // receiver side auto donwloading view binds
        vh.retryDownloading.setVisibility(View.GONE);
        vh.downloadingFrame.setVisibility(View.VISIBLE);

        if (downloadInterface != null) {
            downloadInterface.onDownloadRequested(msg, position);
        }
    }

    private void handleAudioPlayback(
            int position,
            String path,
            ChatDto msg,
            TextView durationView
    ) {
        try {
            boolean isMe = msg.getSenderId().equals(currentUserId);

            // ▶️ pause same audio
            if (playingPosition == position && mediaPlayer != null && mediaPlayer.isPlaying()) {
                stopAudio();
                stopAudioCountdown();
                notifySafe(msg);
                return;
            }

            // 🛑 stop old
            ChatDto oldMsg = null;
            if (playingPosition != -1 && playingPosition < messageList.size()) {
                oldMsg = messageList.get(playingPosition);
            }

            stopAudio();
            stopAudioCountdown();
            if (oldMsg != null) notifySafe(oldMsg);

            // ▶️ start new
            playingPosition = position;
            playingMessageId = msg.getMessageId();
            notifySafe(msg);

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // ⏱️ total duration once
            int totalSeconds = mediaPlayer.getDuration() / 1000;
            durationView.setText(formatSeconds(totalSeconds));
            startAudioCountdown(
                    durationView,
                    totalSeconds,
                    msg.getMessageId()
            );

            mediaPlayer.setOnCompletionListener(mp -> {
                stopAudio();
                stopAudioCountdown();
                durationView.setText(formatSeconds(totalSeconds));

                notifySafe(msg);

                if (!isMe && !msg.isListen()) {
                    msg.setListen(true);
                    if (audioListenListener != null) {
                        audioListenListener.onAudioListened(msg);
                    }
                    notifySafe(msg);
                }
            });

        } catch (Exception e) {
            Toast.makeText(context, "Audio play error", Toast.LENGTH_SHORT).show();
        }
    }


    private String getAudioDuration(String path) {
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);
            long durationMs = Long.parseLong(
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            );
            mmr.release();

            long seconds = durationMs / 1000;
            long min = seconds / 60;
            long sec = seconds % 60;

            return String.format(Locale.getDefault(), "%d:%02d", min, sec);
        } catch (Exception e) {
            return "0:00";
        }
    }

    private void notifySafe(ChatDto msg) {
        int index = messageList.indexOf(msg);
        if (index != -1) {
            // Payload "COUNTER_ONLY" bhej rahe hain
            notifyItemChanged(index, "COUNTER_ONLY");
        }
    }

    private void startAudioCountdown(
            TextView durationView,
            int totalSeconds,
            String messageId
    ) {
        // Purana koi bhi running task khatam karein
        audioHandler.removeCallbacksAndMessages(null);

        audioCountdownRunnable = new Runnable() {
            @Override
            public void run() {
                // Check karein ke kya wahi audio chal rahi hai?
                if (mediaPlayer == null || playingMessageId == null || !messageId.equals(playingMessageId)) {
                    return;
                }

                if (mediaPlayer.isPlaying()) {
                    // Current position seconds mein nikaalein
                    int currentPos = mediaPlayer.getCurrentPosition() / 1000;

                    // Remaining time calculate karein
                    int remaining = totalSeconds - currentPos;

                    // Safety check taake minus mein na jaye
                    if (remaining < 0) remaining = 0;

                    // TRICK: Direct text update (No notifyItemChanged here!)
                    durationView.setText(formatSeconds(remaining));

                    // Har 500ms (aadha second) baad update karein (efficient for battery)
                    audioHandler.postDelayed(this, 500);
                }
            }
        };

        // Counter shuru karein
        audioHandler.post(audioCountdownRunnable);
    }

    private void stopAudioCountdown() {
        audioHandler.removeCallbacksAndMessages(null);
    }


    private String formatSeconds(int sec) {
        int min = sec / 60;
        int s = sec % 60;
        return String.format(Locale.getDefault(), "%d:%02d", min, s);
    }

    public int getPositionById(String messageId) {
        if (messageId == null) return -1;

        for (int i = 0; i < messageList.size(); i++) {
            ChatDto m = messageList.get(i);
            if (m != null && messageId.equals(m.getMessageId())) {
                return i;
            }
        }
        return -1;
    }
}