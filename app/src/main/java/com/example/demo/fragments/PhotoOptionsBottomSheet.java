package com.example.demo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.demo.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet offering 4 profile photo options.
 * Callbacks fire actions in the parent ProfileFragment.
 */
public class PhotoOptionsBottomSheet extends BottomSheetDialogFragment {

    public interface PhotoOptionListener {
        void onTakePhoto();
        void onChooseGallery();
        void onChooseAvatar();
        void onRemovePhoto();
    }

    private static final String ARG_HAS_PHOTO = "has_photo";

    private PhotoOptionListener listener;

    public static PhotoOptionsBottomSheet newInstance(boolean hasPhoto) {
        PhotoOptionsBottomSheet sheet = new PhotoOptionsBottomSheet();
        Bundle args = new Bundle();
        args.putBoolean(ARG_HAS_PHOTO, hasPhoto);
        sheet.setArguments(args);
        return sheet;
    }

    public void setListener(PhotoOptionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_photo_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean hasPhoto = getArguments() != null && getArguments().getBoolean(ARG_HAS_PHOTO, false);

        // Show "Remove Photo" only if user has an existing photo/avatar
        View removeOption = view.findViewById(R.id.option_remove_photo);
        removeOption.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);

        view.findViewById(R.id.option_take_photo).setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onTakePhoto();
        });

        view.findViewById(R.id.option_choose_gallery).setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onChooseGallery();
        });

        view.findViewById(R.id.option_choose_avatar).setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onChooseAvatar();
        });

        removeOption.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onRemovePhoto();
        });
    }
}
