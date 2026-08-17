package com.money.mimi.fragments.home;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.money.mimi.R;

public class SaveWalletContactDialogFragment extends DialogFragment {

    public interface OnSaveWalletContactListener {
        void onSaveWalletContact(String firstName, String lastName, String walletAddress, String category);
    }

    private OnSaveWalletContactListener listener;
    private String walletAddress;

    public static SaveWalletContactDialogFragment newInstance(String walletAddress) {
        SaveWalletContactDialogFragment fragment = new SaveWalletContactDialogFragment();
        Bundle args = new Bundle();
        args.putString("walletAddress", walletAddress);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnSaveWalletContactListener(OnSaveWalletContactListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            walletAddress = getArguments().getString("walletAddress", "");
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_save_wallet_contact, null, false);

        final EditText etFirstName = view.findViewById(R.id.et_first_name);
        final EditText etLastName = view.findViewById(R.id.et_last_name);
        final TextView tvWalletAddress = view.findViewById(R.id.tv_wallet_address);
        final Spinner spCategory = view.findViewById(R.id.sp_category);

        tvWalletAddress.setText(walletAddress);

        String[] categories = {
                getString(R.string.contact_category_friend),
                getString(R.string.contact_category_family),
                getString(R.string.contact_category_business),
                getString(R.string.contact_category_other)
        };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(categoryAdapter);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.save_wallet_contact_title))
                .setView(view)
                .setPositiveButton(getString(R.string.save_wallet_action_save), null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btn.setOnClickListener(v -> {
                String firstName = etFirstName.getText().toString().trim();
                String lastName = etLastName.getText().toString().trim();
                String category = spCategory.getSelectedItem() != null ? spCategory.getSelectedItem().toString() : "";

                if (firstName.isEmpty()) {
                    etFirstName.setError(getString(R.string.field_required));
                    etFirstName.requestFocus();
                    return;
                }
                if (lastName.isEmpty()) {
                    etLastName.setError(getString(R.string.field_required));
                    etLastName.requestFocus();
                    return;
                }

                if (listener != null) {
                    listener.onSaveWalletContact(firstName, lastName, walletAddress, category);
                }
                dismiss();
            });
        });

        return dialog;
    }
}
