package com.example.safesphere.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.safesphere.R;
import com.example.safesphere.adapter.ContactAdapter;
import com.example.safesphere.databinding.FragmentContactListBinding;
import com.example.safesphere.dto.TrustedContact;
import com.example.safesphere.utils.ContactOperationsHelper;
import com.example.safesphere.utils.SharedPrefferanceUtil;

import java.util.ArrayList;
import java.util.List;

public class ContactListFragment extends Fragment implements ContactAdapter.OnContactActionListener {

    private FragmentContactListBinding binding;
    private ContactAdapter adapter;
    private List<TrustedContact> contactList = new ArrayList<>();
    private ContactOperationsHelper contactHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentContactListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        contactHelper = new ContactOperationsHelper(requireContext());
        setupRecyclerView();
        loadContacts();

        binding.fabAddContact.setOnClickListener(v -> {
            // Naya contact add karne ke liye AddContactFragment par bhejein
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AddContactFragment()) // container ki ID apne hisab se dein
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupRecyclerView() {
        adapter = new ContactAdapter(contactList, this);
        binding.rvContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvContacts.setAdapter(adapter);
    }

    private void loadContacts() {
        // SharedPrefs se data load karna
        List<TrustedContact> list = SharedPrefferanceUtil.getTrustedContacts(requireContext());
        contactList.clear();
        if (list != null) {
            contactList.addAll(list);
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        binding.emptyContactLayout.setVisibility(contactList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCall(TrustedContact contact) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + contact.getPhone()));
        startActivity(intent);
    }

    @Override
    public void onEdit(TrustedContact contact) {
        // Edit mode mein AddContactFragment par bhejein
        AddContactFragment fragment = AddContactFragment.newInstance(contact);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDelete(TrustedContact contact) {
        // Helper use karke delete karna
        contactHelper.deleteContact(contact, success -> {
            if (success) {
                loadContacts(); // List refresh karna
            }
        });
    }

    private void startContext(Intent intent) {
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}