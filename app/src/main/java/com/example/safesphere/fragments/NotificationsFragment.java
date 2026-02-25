package com.example.safesphere.fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safesphere.activity.HomeActivity;
import com.example.safesphere.adapter.NotificationsAdapter;
import com.example.safesphere.dto.FamilyCircleDto;
import com.example.safesphere.dto.NotificationDto;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import com.example.safesphere.R;
import com.google.firebase.firestore.WriteBatch;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private NotificationsAdapter adapter;
    private List<NotificationDto> list = new ArrayList<>();
    private LinearLayout emptyNotificationsLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
       emptyNotificationsLayout = view.findViewById(R.id.emptyNotificationLayout);
        rvNotifications = view.findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationsAdapter(list, new NotificationsAdapter.OnActionClickListener() {
            @Override
            public void onAccept(NotificationDto dto) {
                acceptRequest(dto);
            }

            @Override
            public void onReject(NotificationDto dto) {rejectRequest(dto);
            }
        });

        rvNotifications.setAdapter(adapter);

        loadNotifications();
        if (requireActivity() instanceof HomeActivity) {
            ((HomeActivity) requireActivity()).hideNavigation();
        }
    }

    private void loadNotifications() {

        String currentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("recieverId", currentUID)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {

                    if (error != null) return;
                    if (value == null) return;

                    list.clear();

                    for (DocumentSnapshot d : value.getDocuments()) {
                        NotificationDto dto = d.toObject(NotificationDto.class);
                        dto.setId(d.getId());
                        list.add(dto);
                    }
                    if (list.isEmpty()){
                        emptyNotificationsLayout.setVisibility(View.VISIBLE);
                    }else {
                        emptyNotificationsLayout.setVisibility(View.GONE);
                    }

                    adapter.notifyDataSetChanged();
                });

    }

    private void acceptRequest(NotificationDto dto) {

        String currentUID = FirebaseAuth.getInstance().getCurrentUser().getUid(); // receiver
        String senderId = dto.getSenderId();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get sender user
        db.collection("users")
                .document(senderId)
                .get()
                .addOnSuccessListener(senderDoc -> {

                    String senderGender = senderDoc.getString("gender");

                    // Get receiver user
                    db.collection("users")
                            .document(currentUID)
                            .get()
                            .addOnSuccessListener(receiverDoc -> {

                                String receiverGender = receiverDoc.getString("gender");

                                // relation which sender selected
                                String senderRelation = dto.getRelationRequested();

                                // reverse relation creation
                                String receiverRelation =
                                        getReverseRelation(senderRelation, senderGender, receiverGender);

                                // Create DTOs
                                FamilyCircleDto senderSide = new FamilyCircleDto(
                                        senderId,
                                        currentUID,
                                        senderRelation,
                                        dto.getSenderId()
                                );

                                FamilyCircleDto receiverSide = new FamilyCircleDto(
                                        currentUID,
                                        senderId,
                                        receiverRelation,
                                        dto.getSenderId()
                                );

                                // BATCH → to write both together
                                WriteBatch batch = db.batch();

                                // sender family_circle
                                batch.set(
                                        db.collection("users")
                                                .document(senderId)
                                                .collection("family_circle")
                                                .document(currentUID),
                                        senderSide
                                );

                                // receiver family_circle
                                batch.set(
                                        db.collection("users")
                                                .document(currentUID)
                                                .collection("family_circle")
                                                .document(senderId),
                                        receiverSide
                                );

                                // update notification status
                                batch.update(
                                        db.collection("notifications")
                                                .document(dto.getId()),
                                        "status", "accepted"
                                );

                                batch.commit().addOnSuccessListener(a -> {
                                    Toast.makeText(getContext(), "Family Member Added", Toast.LENGTH_SHORT).show();

                                    // 🔥 UI se turant remove
                                    int index = list.indexOf(dto);
                                    if (index != -1) {
                                        list.remove(index);
                                        adapter.notifyItemRemoved(index);
                                    }

                                    Toast.makeText(getContext(), "Family Member Added", Toast.LENGTH_SHORT).show();

                                });

                            });
                });
    }


    private void rejectRequest(NotificationDto dto) {

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(dto.getId())
                .delete()
                .addOnSuccessListener(a -> {

                    // 🔥 UI se turant remove
                    int index = list.indexOf(dto);
                    if (index != -1) {
                        list.remove(index);
                        adapter.notifyItemRemoved(index);
                    }

                    Toast.makeText(getContext(), "Request Rejected", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to reject", Toast.LENGTH_SHORT).show();
                });
    }



    private String getReverseRelation(String senderRelation, String senderGender, String receiverGender) {

        senderRelation = senderRelation.toLowerCase();
        senderGender  = senderGender.toLowerCase();
        receiverGender = receiverGender.toLowerCase();

        switch (senderRelation) {

            // ----------------------
            // CHILD → PARENT
            // ----------------------
            case "son":
            case "daughter":
                if (receiverGender.equals("male")) return "father";
                else if (receiverGender.equals("female")) return "mother";
                else return "parent";


                // ----------------------
                // PARENT → CHILD
                // ----------------------
            case "father":
            case "mother":
                if (receiverGender.equals("male")) return "son";
                else if (receiverGender.equals("female")) return "daughter";
                else return "child";


                // ----------------------
                // SIBLINGS
                // ----------------------
            case "brother":
            case "sister":
                if (receiverGender.equals("male")) return "brother";
                else if (receiverGender.equals("female")) return "sister";
                else return "sibling";


                // ----------------------
                // SPOUSES
                // ----------------------
            case "husband":
                return "wife";

            case "wife":
                return "husband";


            // ----------------------
            // DEFAULT
            // ----------------------
            default:
                return "relative";
        }
    }

}