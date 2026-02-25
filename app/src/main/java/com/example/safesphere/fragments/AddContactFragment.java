package com.example.safesphere.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.safesphere.R;
import com.example.safesphere.activity.HomeActivity;
import com.example.safesphere.databinding.FragmentAddContactBinding;
import com.example.safesphere.dto.TrustedContact;
import com.example.safesphere.utils.ContactOperationsHelper;
import java.util.regex.Pattern;

public class AddContactFragment extends Fragment {

    private FragmentAddContactBinding binding;
    private ContactOperationsHelper contactHelper;
    private TrustedContact mContact; // Isme update hone wala contact aayega

    private static final String PHONE_PATTERN = "^(03|\\+923|923)\\d{9}$";

    // Constructor/NewInstance for passing contact
    public static AddContactFragment newInstance(TrustedContact contact) {
        AddContactFragment fragment = new AddContactFragment();
        Bundle args = new Bundle();
        args.putSerializable("contact_data", contact);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddContactBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getActivity() instanceof HomeActivity){
            ((HomeActivity) getActivity()).hideNavigation();
        }

        initTools();
        checkMode();
        setupClickListeners();
    }

    private void initTools() {
        contactHelper = new ContactOperationsHelper(requireContext());
        if (getArguments() != null) {
            mContact = (TrustedContact) getArguments().getSerializable("contact_data");
        }
    }

    private void checkMode() {
        if (mContact != null) {
            // Update Mode
            binding.etContactName.setText(mContact.getName());
            binding.etContactPhone.setText(mContact.getPhone());
            binding.tvTitle.setText("Update Trusted Contact");
            binding.btnSaveContact.setText("Update Trusted Contact");
            // TextView title change (Optional)
            // binding.tvTitle.setText("Update Contact");
        } else {
            // Add Mode
            binding.btnSaveContact.setText("Add Trusted Contact");
            binding.tvTitle.setText("Add Trusted Contact");
        }
    }

    private void setupClickListeners() {
        binding.btnSaveContact.setOnClickListener(v -> {
            if (validateInputs()) {
                handleSaveOrUpdate();
            }
        });
    }

    private boolean validateInputs() {
        String name = binding.etContactName.getText().toString().trim();
        String phone = binding.etContactPhone.getText().toString().trim();

        if (name.isEmpty()) {
            binding.etContactName.setError("Name is required");
            return false;
        }
        if (!Pattern.compile(PHONE_PATTERN).matcher(phone).matches()) {
            binding.etContactPhone.setError("Invalid format (e.g. 03xxxxxxxxx)");
            return false;
        }
        return true;
    }

//    private void handleSaveOrUpdate() {
//        String newName = binding.etContactName.getText().toString().trim();
//        String newPhone = binding.etContactPhone.getText().toString().trim();
//
//        setLoading(true);
//
//        if (mContact != null) {
//            // Logic for Update
//            contactHelper.updateContact(mContact, newName, newPhone, () -> {
//                setLoading(false);
//                showToast("Contact updated successfully!");
//                requireActivity().getSupportFragmentManager().popBackStack();
//            });
//        } else {
//            // Logic for Add New
//            contactHelper.addContact(newName, newPhone, () -> {
//                setLoading(false);
//                showToast("New contact added!");
//                clearFields();
//            });
//        }
//    }

    private void handleSaveOrUpdate() {
        String newName = binding.etContactName.getText().toString().trim();
        String newPhone = binding.etContactPhone.getText().toString().trim();

        setLoading(true);

        if (mContact != null) {
            // success parameter add kiya gaya hai (isSuccess)
            contactHelper.updateContact(mContact, newName, newPhone, isSuccess -> {
                setLoading(false);
                if (isSuccess) {
                    showToast("Contact updated successfully!");
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        } else {
            // success parameter add kiya gaya hai (isSuccess)
            contactHelper.addContact(newName, newPhone, isSuccess -> {
                setLoading(false);
                if (isSuccess) {
                    showToast("New contact added!");
                    clearFields();
                }
            });
        }
    }
    private void setLoading(boolean isLoading) {
        if (binding == null) return;

        // Button disable
        binding.btnSaveContact.setEnabled(!isLoading);

        if (isLoading) {
            // Progress dikhao aur icon hide karo
            binding.progressLoading.setVisibility(View.VISIBLE);
            binding.btnSaveContact.setIcon(null);
        } else {
            // Progress hide karo aur icon wapas lao
            binding.progressLoading.setVisibility(View.GONE);
            binding.btnSaveContact.setIconResource(R.drawable.ic_sent);
        }
    }

    private void clearFields() {
        binding.etContactName.setText("");
        binding.etContactPhone.setText("");
        binding.etContactName.clearFocus();
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}