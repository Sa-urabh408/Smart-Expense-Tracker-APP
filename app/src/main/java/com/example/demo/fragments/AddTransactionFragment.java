package com.example.demo.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.example.demo.databinding.FragmentAddTransactionBinding;
import com.example.demo.model.Transaction;
import com.example.demo.utils.ReceiptParser;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Add Transaction Fragment with OCR receipt scanning built in.
 *
 * SCAN FLOW:
 *  btnScanBill → camera → ML Kit OCR → ReceiptParser → auto-fill form
 */
public class AddTransactionFragment extends Fragment {

    private FragmentAddTransactionBinding binding;
    private TransactionRepository repository;
    private String userId;
    private long selectedDate;
    private Transaction editingTransaction;
    private Uri cameraImageUri;

    private final String[] expenseCategories = {"Food", "Travel", "Shopping", "Bills", "Entertainment", "Health", "Other"};
    private final String[] incomeCategories  = {"Salary", "Freelance", "Investment", "Other"};
    private final String[] walletTypes       = {"Cash", "Bank", "UPI", "Credit Card"};

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    // ─── Activity Result Launchers ──────────────────────────────────────────

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                launchCamera();
            } else {
                showCameraPermissionDeniedDialog();
            }
        });

    private final ActivityResultLauncher<Intent> cameraLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && cameraImageUri != null) {
                showScanOverlay("Scanning receipt...");
                processReceiptImage(cameraImageUri);
            }
        });

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "local_user";

        repository = new TransactionRepository(requireActivity().getApplication());

        selectedDate = System.currentTimeMillis();
        binding.etDate.setText(dateFormat.format(selectedDate));

        setupCategoryDropdown(expenseCategories);
        setupWalletDropdown(walletTypes);
        setupListeners();

        if (getArguments() != null) loadTransactionForEditing();
    }

    // ─── Listeners ──────────────────────────────────────────────────────────

    private void setupListeners() {
        // Income / Expense toggle
        binding.toggleTransactionType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnIncome) setupCategoryDropdown(incomeCategories);
                else setupCategoryDropdown(expenseCategories);
                binding.actvCategory.setText("", false);
            }
        });

        // Date picker
        binding.etDate.setOnClickListener(v -> showDatePicker());

        // Save
        binding.btnSave.setOnClickListener(v -> saveTransaction());

        // ── Scan Bill Button ───────────────────────────────────────────────
        binding.btnScanBill.setOnClickListener(v -> onScanBillClicked());
    }

    // ─── OCR: Scan button ───────────────────────────────────────────────────

    private void onScanBillClicked() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File imageFile = createTempImageFile();
            cameraImageUri = FileProvider.getUriForFile(
                    requireContext(), "com.example.demo.fileprovider", imageFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Could not open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createTempImageFile() throws IOException {
        File dir = requireContext().getExternalCacheDir();
        return File.createTempFile("receipt_", ".jpg", dir);
    }

    // ─── OCR: Processing ────────────────────────────────────────────────────

    private void processReceiptImage(Uri imageUri) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Scale bitmap to max 1024×1024 to keep ML Kit fast
                Bitmap raw = MediaStore.Images.Media.getBitmap(
                        requireContext().getContentResolver(), imageUri);
                Bitmap scaled = scaleBitmap(raw, 1024);

                InputImage inputImage = InputImage.fromBitmap(scaled, 0);
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        updateScanStatus("Processing data..."));

                recognizer.process(inputImage)
                    .addOnSuccessListener(visionText -> {
                        String rawText = visionText.getText();
                        hideScanOverlay();

                        if (rawText == null || rawText.trim().isEmpty()) {
                            showNoTextError();
                            return;
                        }

                        // Parse the OCR text
                        ReceiptParser.ParsedReceipt receipt = ReceiptParser.parse(rawText);
                        autoFillForm(receipt);

                        // Delete temp file
                        deleteTempFile(imageUri);
                    })
                    .addOnFailureListener(e -> {
                        hideScanOverlay();
                        if (getContext() != null)
                            Toast.makeText(requireContext(),
                                    "Scan failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

            } catch (IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    hideScanOverlay();
                    Toast.makeText(requireContext(),
                            "Could not read image", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ─── OCR: Auto-fill form ─────────────────────────────────────────────────

    private void autoFillForm(ReceiptParser.ParsedReceipt receipt) {
        if (binding == null) return;

        // Title (merchant name)
        if (!receipt.merchantName.isEmpty()) {
            binding.etTitle.setText(receipt.merchantName);
        }

        // Amount
        if (receipt.amountFound) {
            binding.etAmount.setText(String.format(Locale.US, "%.2f", receipt.amount));
        } else {
            binding.etAmount.setText("");
            Toast.makeText(requireContext(),
                    "Amount not found — please enter manually", Toast.LENGTH_LONG).show();
        }

        // Category
        binding.actvCategory.setText(receipt.category, false);

        // Date
        selectedDate = receipt.dateMillis;
        binding.etDate.setText(dateFormat.format(selectedDate));

        // Notes (items list)
        if (!receipt.notes.isEmpty()) {
            binding.etNotes.setText(receipt.notes);
        }

        // Summary toast
        String msg = receipt.amountFound
                ? "✅ Receipt scanned! Review and save."
                : "⚠️ Receipt scanned — amount not found, please fill manually.";
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
    }

    // ─── OCR: Loading overlay ────────────────────────────────────────────────

    private void showScanOverlay(String status) {
        if (binding == null) return;
        binding.layoutOcrLoading.setVisibility(View.VISIBLE);
        binding.tvScanStatus.setText(status);
    }

    private void updateScanStatus(String status) {
        if (binding == null) return;
        binding.tvScanStatus.setText(status);
    }

    private void hideScanOverlay() {
        if (binding == null) return;
        binding.layoutOcrLoading.setVisibility(View.GONE);
    }

    // ─── OCR: Error dialogs ──────────────────────────────────────────────────

    private void showNoTextError() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("No Text Detected")
                .setMessage("Could not read any text from the receipt.\n\nTips:\n• Take photo in good lighting\n• Hold phone steady\n• Make sure receipt is flat")
                .setPositiveButton("Try Again", (d, w) -> onScanBillClicked())
                .setNegativeButton("Enter Manually", null)
                .show();
    }

    private void showCameraPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Camera Permission Required")
                .setMessage("Camera access is needed to scan receipts. Please enable it in Settings.")
                .setPositiveButton("Open Settings", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Image Helper ────────────────────────────────────────────────────────

    private static Bitmap scaleBitmap(Bitmap src, int maxDim) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxDim && h <= maxDim) return src;
        float ratio = (float) maxDim / Math.max(w, h);
        return Bitmap.createScaledBitmap(src, (int)(w * ratio), (int)(h * ratio), true);
    }

    private void deleteTempFile(Uri uri) {
        try {
            if (uri != null && uri.getPath() != null) new File(uri.getPath()).delete();
        } catch (Exception ignored) {}
    }

    // ─── Existing form logic (unchanged) ─────────────────────────────────────

    private void setupCategoryDropdown(String[] categories) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        binding.actvCategory.setAdapter(adapter);
    }

    private void setupWalletDropdown(String[] wallets) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, wallets);
        binding.actvWallet.setAdapter(adapter);
        binding.actvWallet.setText(wallets[0], false);
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDate);
        android.app.DatePickerDialog dlg = new android.app.DatePickerDialog(
                requireContext(),
                R.style.Theme_SmartExpenseTracker,
                (dp, year, month, day) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, month, day);
                    selectedDate = sel.getTimeInMillis();
                    binding.etDate.setText(dateFormat.format(selectedDate));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void saveTransaction() {
        String title    = binding.etTitle.getText().toString().trim();
        String amtStr   = binding.etAmount.getText().toString().trim();
        String category = binding.actvCategory.getText().toString().trim();
        String wallet   = binding.actvWallet.getText().toString().trim();
        String notes    = binding.etNotes.getText().toString().trim();

        boolean isIncome = binding.toggleTransactionType.getCheckedButtonId() == R.id.btnIncome;
        String type      = isIncome ? "income" : "expense";

        if (TextUtils.isEmpty(title))    { binding.tilTitle.setError("Title is required");      return; }
        if (TextUtils.isEmpty(amtStr))   { binding.tilAmount.setError("Amount is required");    return; }
        if (TextUtils.isEmpty(category)) { binding.tilCategory.setError("Category is required");return; }
        if (TextUtils.isEmpty(wallet))   { binding.tilWallet.setError("Wallet Type is required");return; }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
            if (amount <= 0) { binding.tilAmount.setError("Amount must be positive"); return; }
        } catch (NumberFormatException e) {
            binding.tilAmount.setError("Invalid amount"); return;
        }

        binding.tilTitle.setError(null);
        binding.tilAmount.setError(null);
        binding.tilCategory.setError(null);
        binding.tilWallet.setError(null);

        if (editingTransaction != null) {
            editingTransaction.setTitle(title);
            editingTransaction.setAmount(amount);
            editingTransaction.setCategory(category);
            editingTransaction.setType(type);
            editingTransaction.setDate(selectedDate);
            editingTransaction.setWalletType(wallet);
            editingTransaction.setNotes(notes);
            repository.update(editingTransaction);
            Toast.makeText(requireContext(), R.string.success_transaction_updated, Toast.LENGTH_SHORT).show();
        } else {
            Transaction t = new Transaction(userId, title, amount, category, type, selectedDate, wallet, notes);
            repository.insert(t);
            Toast.makeText(requireContext(), R.string.success_transaction_saved, Toast.LENGTH_SHORT).show();
        }
        clearForm();
    }

    private void loadTransactionForEditing() {
        Bundle args = getArguments();
        if (args == null) return;
        int id = args.getInt("transaction_id", -1);
        if (id == -1) return;

        binding.tvHeader.setText(R.string.edit_transaction);
        binding.btnSave.setText(R.string.update);

        String title    = args.getString("title", "");
        double amount   = args.getDouble("amount", 0);
        String category = args.getString("category", "");
        String type     = args.getString("type", "expense");
        long date       = args.getLong("date", System.currentTimeMillis());
        String wallet   = args.getString("walletType", "Cash");
        String notes    = args.getString("notes", "");

        editingTransaction = new Transaction(userId, title, amount, category, type, date, wallet, notes);
        editingTransaction.setId(id);
        editingTransaction.setFirestoreId(args.getString("firestoreId", null));

        binding.etTitle.setText(title);
        binding.etAmount.setText(String.valueOf(amount));
        binding.actvCategory.setText(category, false);
        binding.actvWallet.setText(wallet, false);
        binding.etNotes.setText(notes);
        selectedDate = date;
        binding.etDate.setText(dateFormat.format(date));

        if ("income".equals(type)) binding.toggleTransactionType.check(R.id.btnIncome);
        else binding.toggleTransactionType.check(R.id.btnExpense);
    }

    public void editTransaction(Transaction transaction) {
        if (transaction == null || binding == null) return;
        editingTransaction = transaction;
        binding.tvHeader.setText(R.string.edit_transaction);
        binding.btnSave.setText(R.string.update);
        binding.etTitle.setText(transaction.getTitle());
        binding.etAmount.setText(String.valueOf(transaction.getAmount()));
        binding.actvCategory.setText(transaction.getCategory(), false);
        binding.actvWallet.setText(
                transaction.getWalletType() != null ? transaction.getWalletType() : walletTypes[0], false);
        binding.etNotes.setText(transaction.getNotes());
        selectedDate = transaction.getDate();
        binding.etDate.setText(dateFormat.format(selectedDate));
        if ("income".equals(transaction.getType())) binding.toggleTransactionType.check(R.id.btnIncome);
        else binding.toggleTransactionType.check(R.id.btnExpense);
    }

    private void clearForm() {
        binding.etTitle.setText("");
        binding.etAmount.setText("");
        binding.actvCategory.setText("", false);
        binding.actvWallet.setText(walletTypes[0], false);
        binding.etNotes.setText("");
        selectedDate = System.currentTimeMillis();
        binding.etDate.setText(dateFormat.format(selectedDate));
        binding.toggleTransactionType.check(R.id.btnExpense);
        editingTransaction = null;
        binding.tvHeader.setText(R.string.add_transaction);
        binding.btnSave.setText(R.string.save);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
