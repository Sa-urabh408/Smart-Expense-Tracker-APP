package com.example.demo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.demo.R;
import com.example.demo.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;

/**
 * Helper utility for all profile image/avatar operations:
 * - Bitmap ↔ Base64 conversion with JPEG compression (max 500 KB)
 * - Upload Base64 photo to Firestore
 * - Display profile with correct priority (photo > avatar > initial)
 */
public class ProfileImageHelper {

    private static final String TAG = "ProfileImageHelper";
    private static final int MAX_IMAGE_SIZE_KB = 500;
    private static final int MAX_DIMENSION_PX = 512;

    // ─────────────────────────────────────────────
    //  Bitmap ↔ Base64
    // ─────────────────────────────────────────────

    /**
     * Compress and encode a bitmap to Base64.
     * Scales down if the image exceeds 512 px on either side.
     * Reduces quality iteratively until the result is ≤ 500 KB.
     */
    public static String bitmapToBase64(Bitmap original) {
        Bitmap scaled = scaleBitmap(original);
        int quality = 90;
        byte[] bytes;
        do {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, bos);
            bytes = bos.toByteArray();
            quality -= 10;
        } while (bytes.length > MAX_IMAGE_SIZE_KB * 1024 && quality > 20);
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    /** Decode a Base64 string back to a Bitmap. Returns null if invalid. */
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Log.e(TAG, "base64ToBitmap failed", e);
            return null;
        }
    }

    /** Scale bitmap so the largest dimension ≤ MAX_DIMENSION_PX. */
    private static Bitmap scaleBitmap(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= MAX_DIMENSION_PX && h <= MAX_DIMENSION_PX) return src;
        float ratio = (float) MAX_DIMENSION_PX / Math.max(w, h);
        return Bitmap.createScaledBitmap(src, (int)(w * ratio), (int)(h * ratio), true);
    }

    // ─────────────────────────────────────────────
    //  Firestore Upload
    // ─────────────────────────────────────────────

    public interface UploadCallback {
        void onSuccess(String base64);
        void onFailure(Exception e);
    }

    /**
     * Upload Base64-encoded photo to Firestore at users/{userId}.profilePhotoBase64.
     * Also clears profileAvatar so photo takes priority.
     */
    public static void uploadPhotoToFirestore(String userId, String base64, UploadCallback callback) {
        java.util.Map<String, Object> update = new java.util.HashMap<>();
        update.put("profilePhotoBase64", base64);
        update.put("profileAvatar", null);   // clear avatar when a real photo is set
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(update)
                .addOnSuccessListener(v -> callback.onSuccess(base64))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Save an emoji avatar to Firestore. Clears photo so avatar shows.
     */
    public static void uploadAvatarToFirestore(String userId, String emoji, UploadCallback callback) {
        java.util.Map<String, Object> update = new java.util.HashMap<>();
        update.put("profileAvatar", emoji);
        update.put("profilePhotoBase64", null);  // clear photo when avatar is chosen
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(update)
                .addOnSuccessListener(v -> callback.onSuccess(emoji))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Remove profile photo and avatar from Firestore.
     */
    public static void removePhotoFromFirestore(String userId, UploadCallback callback) {
        java.util.Map<String, Object> update = new java.util.HashMap<>();
        update.put("profilePhotoBase64", null);
        update.put("profileAvatar", null);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(update)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    // ─────────────────────────────────────────────
    //  Display: Priority — Photo > Avatar > Initial
    // ─────────────────────────────────────────────

    /**
     * Set up profile display in a circular ImageView + initial/emoji TextView pair.
     *
     * @param context   Fragment/Activity context
     * @param user      User object with photo/avatar fields
     * @param imageView Circular ImageView for photos; shown/hidden based on priority
     * @param textView  TextView for emoji avatar or initials fallback; shown/hidden
     */
    public static void loadProfileIntoView(Context context, User user,
                                           ImageView imageView, TextView textView) {
        if (user == null) {
            showInitial(imageView, textView, "?");
            return;
        }

        if (user.hasPhoto()) {
            // Priority 1 — real photo
            Bitmap bmp = base64ToBitmap(user.getProfilePhotoBase64());
            if (bmp != null) {
                imageView.setVisibility(ImageView.VISIBLE);
                textView.setVisibility(TextView.GONE);
                Glide.with(context)
                        .load(bmp)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .placeholder(R.drawable.bg_circle_icon)
                        .into(imageView);
                return;
            }
        }

        if (user.hasAvatar()) {
            // Priority 2 — emoji avatar
            imageView.setVisibility(ImageView.GONE);
            textView.setVisibility(TextView.VISIBLE);
            textView.setText(user.getProfileAvatar());
            textView.setTextSize(36f);
            textView.setBackgroundResource(R.drawable.bg_avatar_circle);
            return;
        }

        // Priority 3 — initial letter
        showInitial(imageView, textView, user.getInitial());
    }

    /**
     * Minimal helper for places (chips, lists) that only have a TextView avatar circle.
     * Call this when you only have a single TextView for the avatar.
     */
    public static void loadProfileIntoTextView(User user, TextView textView) {
        if (user == null) {
            textView.setText("?");
            textView.setBackgroundResource(R.drawable.bg_avatar_circle);
            return;
        }
        if (user.hasAvatar()) {
            textView.setText(user.getProfileAvatar());
            textView.setTextSize(20f);
        } else {
            textView.setText(user.getInitial());
            textView.setTextSize(16f);
        }
        textView.setBackgroundResource(R.drawable.bg_avatar_circle);
    }

    private static void showInitial(ImageView imageView, TextView textView, String initial) {
        imageView.setVisibility(ImageView.GONE);
        textView.setVisibility(TextView.VISIBLE);
        textView.setText(initial);
        textView.setTextSize(32f);
        textView.setBackgroundResource(R.drawable.bg_avatar_circle);
    }
}
