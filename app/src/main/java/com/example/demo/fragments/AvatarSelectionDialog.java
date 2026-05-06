package com.example.demo.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.demo.R;

/**
 * Dialog fragment showing a 5-column grid of 24 emoji avatars.
 * On tap, fires onAvatarSelected with the chosen emoji string.
 */
public class AvatarSelectionDialog extends DialogFragment {

    public interface AvatarSelectedListener {
        void onAvatarSelected(String emoji);
    }

    private static final String[] AVATARS = {
        "😀", "😎", "🤓", "👨", "👩",
        "🧑", "🎯", "🔥", "⚡", "🌟",
        "💼", "🎨", "🦁", "🐯", "🦊",
        "🐧", "🦄", "🌈", "💎", "🏆",
        "🎮", "🚀", "🌙", "🌺"
    };

    private AvatarSelectedListener listener;

    public void setListener(AvatarSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_avatar_selection, null);

        GridView gridView = view.findViewById(R.id.grid_avatars);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(), R.layout.item_avatar_emoji, R.id.tv_emoji, AVATARS) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                // Scale emoji size in grid
                ((TextView) v.findViewById(R.id.tv_emoji)).setTextSize(26f);
                return v;
            }
        };
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener((parent, v, position, id) -> {
            if (listener != null) listener.onAvatarSelected(AVATARS[position]);
            dismiss();
        });

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }
}
