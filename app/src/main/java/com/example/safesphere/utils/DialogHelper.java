package com.example.safesphere.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.safesphere.R;

public class DialogHelper {

    public static void showDeleteDialog(Context context, final DeleteDialogListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_delete_chat, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Bind views
        TextView btnCancel = view.findViewById(R.id.btnCancel);
        TextView btnDeleteForMe = view.findViewById(R.id.btnDeleteForMe);
        TextView btnDeleteForEveryone = view.findViewById(R.id.btnDeleteForEveryone);

        // Set click listeners
        btnCancel.setOnClickListener(v -> {
            listener.onCancel();
            dialog.dismiss();
        });

        btnDeleteForMe.setOnClickListener(v -> {
            listener.onDeleteForMe();
            dialog.dismiss();
        });

        btnDeleteForEveryone.setOnClickListener(v -> {
            listener.onDeleteForEveryone();
            dialog.dismiss();
        });
    }

    // Listener interface
    public interface DeleteDialogListener {
        void onDeleteForMe();
        void onDeleteForEveryone();
        void onCancel();
    }
}
