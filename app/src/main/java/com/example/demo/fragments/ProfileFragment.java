package com.example.demo.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.demo.MainActivity;
import com.example.demo.R;
import com.example.demo.data.TransactionRepository;
import com.example.demo.databinding.FragmentProfileBinding;
import com.example.demo.model.User;
import com.example.demo.utils.PdfExporter;
import com.example.demo.utils.ProfileImageHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Profile Fragment with photo upload (Base64), avatar/emoji selection, and
 * priority-based display: photo > emoji > initial.
 */
public class ProfileFragment extends Fragment
        implements PhotoOptionsBottomSheet.PhotoOptionListener,
                   AvatarSelectionDialog.AvatarSelectedListener {

    private FragmentProfileBinding binding;
    private TransactionRepository repository;
    private String userId;
    private User currentUser;
    private Uri cameraImageUri;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    // ─── Activity Result Launchers ──────────────────────────────────────────

    private final ActivityResultLauncher<String[]> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            // After permission grant, user taps avatar again
        });

    private final ActivityResultLauncher<Intent> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK
                    && result.getData() != null && result.getData().getData() != null) {
                Uri imageUri = result.getData().getData();
                processPickedImage(imageUri);
            }
        });

    private final ActivityResultLauncher<Intent> cameraLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && cameraImageUri != null) {
                processPickedImage(cameraImageUri);
            }
        });

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            userId = firebaseUser.getUid();
            binding.tvUserName.setText(firebaseUser.getDisplayName() != null
                    ? firebaseUser.getDisplayName() : "User");
            binding.tvUserEmail.setText(firebaseUser.getEmail() != null
                    ? firebaseUser.getEmail() : "No email");
        } else {
            userId = "local_user";
            binding.tvUserName.setText("User");
            binding.tvUserEmail.setText("local@user.com");
        }

        repository = new TransactionRepository(requireActivity().getApplication());

        observeStats();
        setupListeners();
        loadProfileFromFirestore();
    }

    // ─── Firestore Load ─────────────────────────────────────────────────────

    private void loadProfileFromFirestore() {
        if (userId == null || userId.equals("local_user")) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUser = doc.toObject(User.class);
                        if (currentUser == null) currentUser = new User();
                        currentUser.setUserId(userId);
                    } else {
                        currentUser = new User();
                        currentUser.setUserId(userId);
                    }
                    refreshProfileDisplay();
                })
                .addOnFailureListener(e -> {
                    currentUser = new User();
                    currentUser.setUserId(userId);
                    refreshProfileDisplay();
                });
    }

    private void refreshProfileDisplay() {
        if (binding == null || getContext() == null) return;
        String name = binding.tvUserName.getText().toString();
        if (currentUser != null) {
            currentUser.setDisplayName(name);
        }
        ProfileImageHelper.loadProfileIntoView(
                requireContext(), currentUser,
                binding.ivProfilePhoto, binding.tvProfileInitial);
    }

    // ─── Listeners ──────────────────────────────────────────────────────────

    private void setupListeners() {
        binding.layoutProfileAvatar.setOnClickListener(v -> showPhotoOptions());

        binding.btnSetBudget.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), com.example.demo.SetBudgetActivity.class);
            startActivity(intent);
        });

        binding.btnExportPdf.setOnClickListener(v -> exportPdf());
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void showPhotoOptions() {
        boolean hasMedia = currentUser != null && (currentUser.hasPhoto() || currentUser.hasAvatar());
        PhotoOptionsBottomSheet sheet = PhotoOptionsBottomSheet.newInstance(hasMedia);
        sheet.setListener(this);
        sheet.show(getChildFragmentManager(), "photo_options");
    }

    // ─── PhotoOptionListener callbacks ──────────────────────────────────────

    @Override
    public void onTakePhoto() {
        if (!hasCameraPermission()) {
            permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
            return;
        }
        launchCamera();
    }

    @Override
    public void onChooseGallery() {
        if (!hasGalleryPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_IMAGES});
            } else {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            }
            return;
        }
        launchGallery();
    }

    @Override
    public void onChooseAvatar() {
        AvatarSelectionDialog dialog = new AvatarSelectionDialog();
        dialog.setListener(this);
        dialog.show(getChildFragmentManager(), "avatar_dialog");
    }

    @Override
    public void onRemovePhoto() {
        showProgress(true);
        ProfileImageHelper.removePhotoFromFirestore(userId, new ProfileImageHelper.UploadCallback() {
            @Override
            public void onSuccess(String ignored) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showProgress(false);
                    if (currentUser != null) {
                        currentUser.setProfilePhotoBase64(null);
                        currentUser.setProfileAvatar(null);
                    }
                    refreshProfileDisplay();
                    Toast.makeText(requireContext(), "Photo removed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(requireContext(), "Failed to remove photo", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ─── AvatarSelectedListener callback ─────────────────────────────────────

    @Override
    public void onAvatarSelected(String emoji) {
        showProgress(true);
        ProfileImageHelper.uploadAvatarToFirestore(userId, emoji, new ProfileImageHelper.UploadCallback() {
            @Override
            public void onSuccess(String selected) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showProgress(false);
                    if (currentUser == null) currentUser = new User();
                    currentUser.setProfileAvatar(selected);
                    currentUser.setProfilePhotoBase64(null);
                    refreshProfileDisplay();
                    Toast.makeText(requireContext(), "Avatar updated!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(requireContext(), "Failed to save avatar", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ─── Camera / Gallery ───────────────────────────────────────────────────

    private void launchCamera() {
        try {
            File imageFile = createTempImageFile();
            cameraImageUri = FileProvider.getUriForFile(
                    requireContext(), "com.example.demo.fileprovider", imageFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Unable to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private File createTempImageFile() throws IOException {
        File storageDir = requireContext().getExternalCacheDir();
        return File.createTempFile("profile_", ".jpg", storageDir);
    }

    private void processPickedImage(Uri imageUri) {
        showProgress(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        requireContext().getContentResolver(), imageUri);
                String base64 = ProfileImageHelper.bitmapToBase64(bitmap);

                ProfileImageHelper.uploadPhotoToFirestore(userId, base64,
                        new ProfileImageHelper.UploadCallback() {
                            @Override
                            public void onSuccess(String b64) {
                                if (getActivity() == null) return;
                                getActivity().runOnUiThread(() -> {
                                    showProgress(false);
                                    if (currentUser == null) currentUser = new User();
                                    currentUser.setProfilePhotoBase64(b64);
                                    currentUser.setProfileAvatar(null);
                                    refreshProfileDisplay();
                                    Toast.makeText(requireContext(),
                                            "Photo updated!", Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                if (getActivity() == null) return;
                                getActivity().runOnUiThread(() -> {
                                    showProgress(false);
                                    Toast.makeText(requireContext(),
                                            "Upload failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                        });
            } catch (IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(requireContext(), "Could not load image", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ─── Permissions ─────────────────────────────────────────────────────────

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // ─── Stats ───────────────────────────────────────────────────────────────

    private void observeStats() {
        repository.getTransactionCount(userId).observe(getViewLifecycleOwner(), count ->
                binding.tvTotalTransactions.setText(String.valueOf(count != null ? count : 0)));

        repository.getTotalIncome(userId).observe(getViewLifecycleOwner(), income ->
                binding.tvTotalIncome.setText(currencyFormat.format(income != null ? income : 0)));

        repository.getTotalExpenses(userId).observe(getViewLifecycleOwner(), expenses ->
                binding.tvTotalExpenses.setText(currencyFormat.format(expenses != null ? expenses : 0)));
    }

    // ─── UI helpers ──────────────────────────────────────────────────────────

    private void showProgress(boolean show) {
        if (binding != null) {
            binding.progressUpload.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.layoutProfileAvatar.setEnabled(!show);
        }
    }

    private void exportPdf() {
        Toast.makeText(requireContext(), "Generating PDF...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. Get all transactions
                java.util.List<com.example.demo.model.Transaction> transactions =
                        repository.getAllTransactionsList(userId);

                String fileName = "expense_report_" + System.currentTimeMillis() + ".pdf";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // ── Android 10+ : Use MediaStore (no permission needed) ──
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS);

                    android.content.ContentResolver resolver =
                            requireContext().getContentResolver();
                    Uri pdfUri = resolver.insert(
                            MediaStore.Files.getContentUri("external"), values);

                    if (pdfUri == null) {
                        throw new IOException("Failed to create MediaStore entry");
                    }

                    // Write PDF to the MediaStore OutputStream
                    java.io.OutputStream outputStream = resolver.openOutputStream(pdfUri);
                    if (outputStream == null) {
                        throw new IOException("Failed to open output stream");
                    }

                    // Export via PdfExporter using the stream
                    exportPdfToStream(transactions, outputStream);
                    outputStream.close();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "✅ PDF saved to Downloads: " + fileName,
                                        Toast.LENGTH_LONG).show());
                    }

                } else {
                    // ── Android 9 and below : Write directly to Downloads ──
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) downloadsDir.mkdirs();

                    File pdfFile = new File(downloadsDir, fileName);
                    PdfExporter.exportToPdf(transactions, pdfFile.getAbsolutePath());

                    // Notify MediaScanner so file shows in gallery/Downloads
                    android.media.MediaScannerConnection.scanFile(
                            requireContext(),
                            new String[]{pdfFile.getAbsolutePath()},
                            new String[]{"application/pdf"},
                            null);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "✅ PDF saved to Downloads: " + pdfFile.getName(),
                                        Toast.LENGTH_LONG).show());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "❌ PDF export failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    /**
     * Write PDF content to an OutputStream (for MediaStore on Android 10+).
     */
    private void exportPdfToStream(java.util.List<com.example.demo.model.Transaction> transactions,
                                    java.io.OutputStream outputStream) throws Exception {
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(
                com.itextpdf.text.PageSize.A4);
        com.itextpdf.text.pdf.PdfWriter.getInstance(document, outputStream);
        document.open();

        java.text.SimpleDateFormat dateFormat =
                new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.US);

        // Title
        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 20,
                com.itextpdf.text.Font.BOLD,
                new com.itextpdf.text.BaseColor(0, 191, 165));
        com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph(
                "Smart Expense Tracker Report", titleFont);
        title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        // Date
        com.itextpdf.text.Font bodyFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10,
                com.itextpdf.text.Font.NORMAL,
                new com.itextpdf.text.BaseColor(50, 50, 50));
        com.itextpdf.text.Paragraph dateP = new com.itextpdf.text.Paragraph(
                "Generated: " + dateFormat.format(new java.util.Date()), bodyFont);
        dateP.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        dateP.setSpacingAfter(20);
        document.add(dateP);

        if (transactions == null || transactions.isEmpty()) {
            com.itextpdf.text.Paragraph empty = new com.itextpdf.text.Paragraph(
                    "No transactions found.", bodyFont);
            empty.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(empty);
            document.close();
            return;
        }

        // Summary
        double totalIncome = 0, totalExpense = 0;
        for (com.example.demo.model.Transaction t : transactions) {
            if ("income".equals(t.getType())) totalIncome += t.getAmount();
            else totalExpense += t.getAmount();
        }

        com.itextpdf.text.Font totalFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 12,
                com.itextpdf.text.Font.BOLD,
                new com.itextpdf.text.BaseColor(0, 191, 165));
        document.add(new com.itextpdf.text.Paragraph("Summary",
                new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                        14, com.itextpdf.text.Font.BOLD)));
        document.add(new com.itextpdf.text.Paragraph(
                "Total Income: " + currencyFormat.format(totalIncome), totalFont));
        document.add(new com.itextpdf.text.Paragraph(
                "Total Expenses: " + currencyFormat.format(totalExpense), totalFont));
        document.add(new com.itextpdf.text.Paragraph(
                "Balance: " + currencyFormat.format(totalIncome - totalExpense), totalFont));

        com.itextpdf.text.Paragraph space = new com.itextpdf.text.Paragraph(" ");
        space.setSpacingAfter(15);
        document.add(space);

        // Table
        com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2, 2, 2, 2});

        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 11,
                com.itextpdf.text.Font.BOLD,
                com.itextpdf.text.BaseColor.WHITE);
        com.itextpdf.text.BaseColor headerBg = new com.itextpdf.text.BaseColor(0, 191, 165);
        String[] headers = {"Title", "Type", "Category", "Amount", "Date"};
        for (String h : headers) {
            com.itextpdf.text.pdf.PdfPCell cell =
                    new com.itextpdf.text.pdf.PdfPCell(
                            new com.itextpdf.text.Phrase(h, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        for (com.example.demo.model.Transaction t : transactions) {
            table.addCell(createStreamCell(t.getTitle(), bodyFont));
            table.addCell(createStreamCell(t.getType().toUpperCase(), bodyFont));
            table.addCell(createStreamCell(t.getCategory(), bodyFont));
            table.addCell(createStreamCell(currencyFormat.format(t.getAmount()), bodyFont));
            table.addCell(createStreamCell(
                    dateFormat.format(new java.util.Date(t.getDate())), bodyFont));
        }

        document.add(table);
        document.close();
    }

    private com.itextpdf.text.pdf.PdfPCell createStreamCell(String text,
                                                              com.itextpdf.text.Font font) {
        com.itextpdf.text.pdf.PdfPCell cell =
                new com.itextpdf.text.pdf.PdfPCell(
                        new com.itextpdf.text.Phrase(text, font));
        cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        cell.setPadding(6);
        return cell;
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).logout();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
