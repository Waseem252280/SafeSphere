package com.example.safesphere.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safesphere.R;
import com.example.safesphere.databinding.ItemTrustedContactBinding;
import com.example.safesphere.dto.TrustedContact;

import java.lang.reflect.Field;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<TrustedContact> contactList;
    private OnContactActionListener listener;

    public interface OnContactActionListener {
        void onCall(TrustedContact contact);
        void onEdit(TrustedContact contact);
        void onDelete(TrustedContact contact);
    }

    public ContactAdapter(List<TrustedContact> contactList, OnContactActionListener listener) {
        this.contactList = contactList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrustedContactBinding binding = ItemTrustedContactBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ContactViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        TrustedContact contact = contactList.get(position);
        holder.binding.tvContactName.setText(contact.getName());
        holder.binding.tvContactPhone.setText(contact.getPhone());

        holder.binding.ivMenu.setOnClickListener(v -> showPopupMenu(v, contact));
    }

//    private void showPopupMenu(View view, TrustedContact contact) {
//        PopupMenu popup = new PopupMenu(view.getContext(), view);
//        popup.getMenuInflater().inflate(R.menu.contact_menu, popup.getMenu());
//
//        // Forcing icons to show in PopupMenu (Reflection hack for standard Android)
//        try {
//            Field field = popup.getClass().getDeclaredField("mPopup");
//            field.setAccessible(true);
//            Object menuHelper = field.get(popup);
//            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class).invoke(menuHelper, true);
//        } catch (Exception e) { e.printStackTrace(); }
//
//        popup.setOnMenuItemClickListener(item -> {
//            int id = item.getItemId();
//            if (id == R.id.menu_call) listener.onCall(contact);
//            else if (id == R.id.menu_edit) listener.onEdit(contact);
//            else if (id == R.id.menu_delete) listener.onDelete(contact);
//            return true;
//        });
//        popup.show();
//    }

//    private void showPopupMenu(View view, TrustedContact contact) {
//        // 1. Context ko apne custom style ke saath wrap karein
//        androidx.appcompat.view.ContextThemeWrapper wrapper =
//                new androidx.appcompat.view.ContextThemeWrapper(view.getContext(), R.style.CustomPopupMenuStyle);
//
//        // 2. PopupMenu ko wrapper context ke saath initialize karein
//        PopupMenu popup = new PopupMenu(wrapper, view);
//        popup.getMenuInflater().inflate(R.menu.contact_menu, popup.getMenu());
//
//        // Icons show karne wala hack (Jo aapne pehle se likha hai)
//        try {
//            Field field = popup.getClass().getDeclaredField("mPopup");
//            field.setAccessible(true);
//            Object menuHelper = field.get(popup);
//            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class).invoke(menuHelper, true);
//        } catch (Exception e) { e.printStackTrace(); }
//
//        popup.setOnMenuItemClickListener(item -> {
//            int id = item.getItemId();
//            if (id == R.id.menu_call) listener.onCall(contact);
//            else if (id == R.id.menu_edit) listener.onEdit(contact);
//            else if (id == R.id.menu_delete) listener.onDelete(contact);
//            return true;
//        });
//
//        popup.show();
//    }

    private void showPopupMenu(View view, TrustedContact contact) {
        // ContextWrapper use karein
        Context wrapper = new ContextThemeWrapper(view.getContext(), R.style.CustomPopupMenuStyle);

        // Yahan ensure karein ke import androidx.appcompat.widget.PopupMenu ho
        PopupMenu popup = new PopupMenu(wrapper, view);
        popup.getMenuInflater().inflate(R.menu.contact_menu, popup.getMenu());

        // --- ICONS HACK ---
        try {
            Field field = popup.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuHelper = field.get(popup);
            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class).invoke(menuHelper, true);
        } catch (Exception e) { e.printStackTrace(); }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_call) listener.onCall(contact);
            else if (id == R.id.menu_edit) listener.onEdit(contact);
            else if (id == R.id.menu_delete) listener.onDelete(contact);
            return true;
        });

        popup.show();
    }

    @Override
    public int getItemCount() { return contactList.size(); }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        ItemTrustedContactBinding binding;
        public ContactViewHolder(ItemTrustedContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}