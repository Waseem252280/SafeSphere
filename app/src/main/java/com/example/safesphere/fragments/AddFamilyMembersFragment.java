package com.example.safesphere.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.safesphere.R;
import com.example.safesphere.activity.HomeActivity;
import com.example.safesphere.dto.FamilyCircleDto;
import com.example.safesphere.dto.NotificationDto;
import com.example.safesphere.dto.UserDto;
import com.example.safesphere.utils.LoaderUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class AddFamilyMembersFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private ImageView ivFoundUserProfile, ivFoundPendingUserProfile,ivFoundUserMemberProfile;
    private TextView tvFoundUserName,tvFoundPendingUserName,tvFoundUserMemberName,userRole;

    private LinearLayout result_relation_section,
            result_sent_request,
            result_family_member,
            errorBox;

    private ChipGroup chipGroupMaleRelations, chipGroupFemaleRelations, chipGroupFemaleToMaleRelation, chipGroupMaleToFemaleRelation;
    private MaterialButton btnSendRequest, btnSearch, btnCancelRequest, btnRemoveMember,btnRelation;

    private TextInputEditText etSearchEmail;
    private LoaderUtil loaderDialog;

    private UserDto foundUserDto = null;
    private FamilyCircleDto familyCircleDto = null;
    private String pendingNotificationId = null;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String currentUID = FirebaseAuth.getInstance().getUid();


    public AddFamilyMembersFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_family_members, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setClickListeners();
        if(requireActivity() instanceof HomeActivity){
            ((HomeActivity) requireActivity()).hideNavigation();
        }
    }


    private void initViews(View view) {
        sharedPreferences = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);

        loaderDialog = new LoaderUtil(getContext());

        ivFoundUserProfile = view.findViewById(R.id.ivFoundUserProfile);
        ivFoundPendingUserProfile = view.findViewById(R.id.ivFoundPendingUserProfile);
        ivFoundUserMemberProfile = view.findViewById(R.id.ivFoundUserMemberProfile);
        tvFoundPendingUserName = view.findViewById(R.id.tvFoundPendingUserName);
        tvFoundUserMemberName = view.findViewById(R.id.tvFoundUserMemberName);
        tvFoundUserName = view.findViewById(R.id.tvFoundUserName);
        userRole = view.findViewById(R.id.rolAdmin);

        result_relation_section = view.findViewById(R.id.result_relation_section);
        result_sent_request = view.findViewById(R.id.result_sent_request);
        result_family_member = view.findViewById(R.id.result_family_member);
        errorBox = view.findViewById(R.id.errorBox);

        chipGroupMaleRelations = view.findViewById(R.id.chipGroupMaleRelations);
        chipGroupFemaleRelations = view.findViewById(R.id.chipGroupFemaleRelations);
        chipGroupMaleToFemaleRelation = view.findViewById(R.id.chipGroupMaleToFemaleRelations);
        chipGroupFemaleToMaleRelation = view.findViewById(R.id.chipGroupFemaleToMaleRelations);


        btnSendRequest = view.findViewById(R.id.btnSendRequest);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnCancelRequest = view.findViewById(R.id.btnCancelRequest);
        btnRemoveMember = view.findViewById(R.id.btnRemoveMember);
        btnRelation = view.findViewById(R.id.btnRelation);

        etSearchEmail = view.findViewById(R.id.etSearchEmail);

        hideAllLayouts();
    }


    private void hideAllLayouts() {
        result_relation_section.setVisibility(View.GONE);
        result_sent_request.setVisibility(View.GONE);
        result_family_member.setVisibility(View.GONE);
        errorBox.setVisibility(View.GONE);
    }


    private void setClickListeners() {

        btnSendRequest.setOnClickListener(v -> {

            String selectedRelation = getSelectedRelation();

            if (selectedRelation == null) {
                Toast.makeText(getContext(), "Please select a relation.", Toast.LENGTH_LONG).show();
                return;
            }

            sendRelationRequest(foundUserDto.getUserId(), selectedRelation);
        });


        btnSearch.setOnClickListener(v -> {
            loaderDialog.showLoader();
            String email = etSearchEmail.getText().toString().trim();

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError();
                loaderDialog.stopLoader();
                return;
            }

            searchUserByEmail(email);
        });


        btnCancelRequest.setOnClickListener(v -> {
            if (pendingNotificationId != null) {
                cancelRequest(pendingNotificationId);
            }
        });


        btnRemoveMember.setOnClickListener(v -> {
            if (foundUserDto != null) {
                removeFamilyMember(foundUserDto.getUserId());
            }
        });
    }


    // ******************** SEARCH USER *************************

    private void searchUserByEmail(String email) {

        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        showError();
                        loaderDialog.stopLoader();
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    foundUserDto = doc.toObject(UserDto.class);

                    if (foundUserDto != null) {
                        showUserData(foundUserDto);
                        checkFamilyCircle(foundUserDto.getUserId());
                    }

                    loaderDialog.stopLoader();
                })
                .addOnFailureListener(e -> {
                    showError();
                    loaderDialog.stopLoader();
                });
    }


    private void showUserData(UserDto user) {
        hideError();

        String senderGender = sharedPreferences.getString("gender", null);

        tvFoundUserName.setText(user.getFullName());
        tvFoundPendingUserName.setText(user.getFullName());
        tvFoundUserMemberName.setText(user.getFullName());

        Picasso.get().load(user.getPhotoUrl()).into(ivFoundUserProfile);
        Picasso.get().load(user.getPhotoUrl()).into(ivFoundPendingUserProfile);
        Picasso.get().load(user.getPhotoUrl()).into(ivFoundUserMemberProfile);

        if ("male".equalsIgnoreCase(user.getGender()) && "male".equalsIgnoreCase(senderGender)) {
            chipGroupMaleRelations.setVisibility(View.VISIBLE);
            chipGroupFemaleRelations.setVisibility(View.GONE);
            chipGroupFemaleToMaleRelation.setVisibility(View.GONE);
            chipGroupMaleToFemaleRelation.setVisibility(View.GONE);
        } else if("female".equalsIgnoreCase(user.getGender()) && "female".equalsIgnoreCase(senderGender)) {
            chipGroupMaleRelations.setVisibility(View.GONE);
            chipGroupFemaleRelations.setVisibility(View.VISIBLE);
            chipGroupFemaleToMaleRelation.setVisibility(View.GONE);
            chipGroupMaleToFemaleRelation.setVisibility(View.GONE);
        } else if ("male".equalsIgnoreCase(user.getGender()) && "female".equalsIgnoreCase(senderGender)) {
            chipGroupMaleRelations.setVisibility(View.GONE);
            chipGroupFemaleRelations.setVisibility(View.GONE);
            chipGroupMaleToFemaleRelation.setVisibility(View.GONE);
            chipGroupFemaleToMaleRelation.setVisibility(View.VISIBLE);
        }else {
            chipGroupMaleRelations.setVisibility(View.GONE);
            chipGroupFemaleRelations.setVisibility(View.GONE);
            chipGroupFemaleToMaleRelation.setVisibility(View.GONE);
            chipGroupMaleToFemaleRelation.setVisibility(View.VISIBLE);
        }
    }


    // ******************** CHECK FAMILY / PENDING *************************

    private void checkFamilyCircle(String targetUID) {

        db.collection("users")
                .document(currentUID)
                .collection("family_circle")
                .document(targetUID)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        familyCircleDto = doc.toObject(FamilyCircleDto.class);
                        showFamilyMemberLayout(familyCircleDto);
                    } else {
                        checkPendingRequest(targetUID);
                    }

                });
    }


    private void checkPendingRequest(String targetUID) {

        db.collection("notifications")
                .whereEqualTo("senderId", currentUID)
                .whereEqualTo("recieverId", targetUID)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {

                    if (!snap.isEmpty()) {
                        pendingNotificationId = snap.getDocuments().get(0).getId();
                        showPendingLayout();
                    } else {
                        showAddRelationLayout();
                    }

                });
    }


    // ******************** LAYOUT CONTROL *************************

    private void showAddRelationLayout() {
        result_relation_section.setVisibility(View.VISIBLE);
        result_sent_request.setVisibility(View.GONE);
        result_family_member.setVisibility(View.GONE);
        errorBox.setVisibility(View.GONE);
    }

    private void showPendingLayout() {
        result_sent_request.setVisibility(View.VISIBLE);
        result_relation_section.setVisibility(View.GONE);
        result_family_member.setVisibility(View.GONE);
        errorBox.setVisibility(View.GONE);
    }

    private void showFamilyMemberLayout(FamilyCircleDto familyCircleDto) {
        btnRelation.setText(familyCircleDto.getRelation().substring(0,1).toUpperCase() + familyCircleDto.getRelation().substring(1));
        if (!familyCircleDto.getAdminId().equals(currentUID)) {
            userRole.setVisibility(View.VISIBLE);
        }else{
            userRole.setVisibility(View.GONE);
        }
        result_family_member.setVisibility(View.VISIBLE);
        result_relation_section.setVisibility(View.GONE);
        result_sent_request.setVisibility(View.GONE);
        errorBox.setVisibility(View.GONE);
    }


    private void showError() {
        hideAllLayouts();
        errorBox.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorBox.setVisibility(View.GONE);
    }


    // ******************** SEND REQUEST *************************

    private void sendRelationRequest(String targetUID, String relation) {

        NotificationDto dto = new NotificationDto(
                currentUID,
                relation,
                "pending",
                sharedPreferences.getString("fullName","User Name"),
                sharedPreferences.getString("photoUrl",""),
                sharedPreferences.getString("gender",""),
                targetUID
        );

        db.collection("notifications")
                .add(dto)
                .addOnSuccessListener(ref -> {

                    String generatedId = ref.getId();
                    dto.setId(generatedId); // local object me set

                    // Firestore document me bhi ID save karo
                    ref.update("id", generatedId);

                    pendingNotificationId = generatedId;

                    Toast.makeText(getContext(), "Request Sent", Toast.LENGTH_LONG).show();
                    showPendingLayout();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to send request", Toast.LENGTH_SHORT).show();
                });
    }


    // ******************** CANCEL REQUEST *************************

    private void cancelRequest(String notificationId) {

        db.collection("notifications")
                .document(notificationId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists() && "pending".equals(doc.getString("status"))) {

                        doc.getReference().delete()
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(getContext(), "Request Cancelled", Toast.LENGTH_LONG).show();
                                    showAddRelationLayout();
                                });

                    } else {
                        Toast.makeText(getContext(), "Your request is already accepted.", Toast.LENGTH_SHORT).show();
                        showFamilyMemberLayout(familyCircleDto);
                    }
                });

    }


    // ******************** REMOVE FAMILY MEMBER *************************

    private void removeFamilyMember(String targetUID) {

        if (familyCircleDto == null) {
            Toast.makeText(getContext(), "Family data not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String adminId = familyCircleDto.getAdminId();

        // 🔒 Admin only
        if (!currentUID.equals(adminId)) {
            Toast.makeText(
                    getContext(),
                    "Only admin can remove family members",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 🔹 Step 1: Remove family circle (both users)
        db.runBatch(batch -> {

            batch.delete(
                    db.collection("users")
                            .document(currentUID)
                            .collection("family_circle")
                            .document(targetUID)
            );

            batch.delete(
                    db.collection("users")
                            .document(targetUID)
                            .collection("family_circle")
                            .document(currentUID)
            );

        }).addOnSuccessListener(a -> {

            // 🔹 Step 2: Remove centralized notifications
            deleteCentralizedNotifications(targetUID);

            Toast.makeText(getContext(), "Family member removed", Toast.LENGTH_LONG).show();
            showAddRelationLayout();

        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to remove member", Toast.LENGTH_SHORT).show();
        });
    }
    //delete related notifications
    private void deleteCentralizedNotifications(String targetUID) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // current → target
        db.collection("notifications")
                .whereEqualTo("senderId", currentUID)
                .whereEqualTo("recieverId", targetUID)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap) {
                        doc.getReference().delete();
                    }
                });

        // target → current
        db.collection("notifications")
                .whereEqualTo("senderId", targetUID)
                .whereEqualTo("recieverId", currentUID)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap) {
                        doc.getReference().delete();
                    }
                });
    }

    // ******************** UTILS *************************

    private String getSelectedRelation() {

        int maleChipId = chipGroupMaleRelations.getCheckedChipId();
        int femaleChipId = chipGroupFemaleRelations.getCheckedChipId();
        int femaleToMaleChipId = chipGroupFemaleToMaleRelation.getCheckedChipId();
        int maleToFemaleChipId = chipGroupMaleToFemaleRelation.getCheckedChipId();

        if (maleChipId != -1) {
            Chip chip = chipGroupMaleRelations.findViewById(maleChipId);
            return chip.getText().toString();
        }

        if (femaleChipId != -1) {
            Chip chip = chipGroupFemaleRelations.findViewById(femaleChipId);
            return chip.getText().toString();
        }
        if (femaleToMaleChipId != -1) {
            Chip chip = chipGroupFemaleToMaleRelation.findViewById(femaleToMaleChipId);
            return chip.getText().toString();
        }
        if (maleToFemaleChipId != -1) {
            Chip chip = chipGroupMaleToFemaleRelation.findViewById(maleToFemaleChipId);
            return chip.getText().toString();
        }

        return null;
    }
}
