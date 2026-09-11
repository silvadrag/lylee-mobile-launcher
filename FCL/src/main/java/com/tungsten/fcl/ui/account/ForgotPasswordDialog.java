package com.tungsten.fcl.ui.account;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tungsten.fcl.R;
import com.tungsten.fcl.lylee.LyleeFriendsApi;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fcllibrary.component.dialog.FCLDialog;
import com.tungsten.fcllibrary.component.view.FCLButton;
import com.tungsten.fcllibrary.component.view.FCLEditText;
import com.tungsten.fcllibrary.component.view.FCLProgressBar;
import com.tungsten.fcllibrary.component.view.FCLTextView;

import java.util.ArrayList;
import java.util.List;

public class ForgotPasswordDialog extends FCLDialog {

    private final String username;
    private final FCLEditText recoveryKeyField;
    private final FCLEditText newPasswordField;
    private final FCLEditText confirmPasswordField;
    private final FCLTextView errorText;
    private final FCLButton cancelButton;
    private final FCLButton submitButton;
    private final FCLProgressBar progress;
    private final Runnable onSuccess;
    private boolean isFormattingKey = false;

    public ForgotPasswordDialog(@NonNull Context context, @NonNull String username, @Nullable Runnable onSuccess) {
        super(context);
        this.username = username;
        this.onSuccess = onSuccess;
        setCancelable(true);
        setContentView(R.layout.dialog_forgot_password);

        recoveryKeyField = findViewById(R.id.forgot_recovery_key);
        newPasswordField = findViewById(R.id.forgot_new_password);
        confirmPasswordField = findViewById(R.id.forgot_confirm_password);
        errorText = findViewById(R.id.forgot_error);
        cancelButton = findViewById(R.id.forgot_cancel);
        submitButton = findViewById(R.id.forgot_submit);
        progress = findViewById(R.id.forgot_progress);

        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> dismiss());
        }

        if (submitButton != null) {
            submitButton.setOnClickListener(v -> onSubmit());
        }

        if (recoveryKeyField != null) {
            recoveryKeyField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isFormattingKey || s == null) return;
                    isFormattingKey = true;
                    try {
                        String raw = s.toString();
                        StringBuilder cleanBuilder = new StringBuilder();
                        for (int i = 0; i < raw.length(); i++) {
                            char c = raw.charAt(i);
                            if (Character.isLetterOrDigit(c)) {
                                cleanBuilder.append(Character.toUpperCase(c));
                            }
                        }
                        String clean = cleanBuilder.toString();
                        String formatted;
                        if (clean.startsWith("LYLEE")) {
                            String body = clean.substring(5);
                            List<String> chunks = new ArrayList<>();
                            chunks.add("LYLEE");
                            for (int i = 0; i < body.length(); i += 4) {
                                chunks.add(body.substring(i, Math.min(body.length(), i + 4)));
                            }
                            formatted = android.text.TextUtils.join("-", chunks);
                        } else {
                            List<String> chunks = new ArrayList<>();
                            for (int i = 0; i < clean.length(); i += 4) {
                                chunks.add(clean.substring(i, Math.min(clean.length(), i + 4)));
                            }
                            formatted = android.text.TextUtils.join("-", chunks);
                        }
                        if (!formatted.equals(raw)) {
                            recoveryKeyField.setText(formatted);
                            recoveryKeyField.setSelection(formatted.length());
                        }
                    } finally {
                        isFormattingKey = false;
                    }
                }
            });
        }
    }

    private void onSubmit() {
        if (errorText != null) errorText.setVisibility(View.GONE);

        String rawKey = recoveryKeyField != null && recoveryKeyField.getText() != null
                ? recoveryKeyField.getText().toString().trim() : "";
        String newPassword = newPasswordField != null && newPasswordField.getText() != null
                ? newPasswordField.getText().toString() : "";
        String confirmPassword = confirmPasswordField != null && confirmPasswordField.getText() != null
                ? confirmPasswordField.getText().toString() : "";

        if (rawKey.isEmpty()) {
            showError(getContext().getString(R.string.friends_recovery_key_required));
            return;
        }

        if (newPassword.length() < 4) {
            showError(getContext().getString(R.string.friends_password_too_short));
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError(getContext().getString(R.string.friends_password_mismatch));
            return;
        }

        StringBuilder cleanKeyBuilder = new StringBuilder();
        for (int i = 0; i < rawKey.length(); i++) {
            char c = rawKey.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                cleanKeyBuilder.append(Character.toUpperCase(c));
            }
        }
        String cleanKey = cleanKeyBuilder.toString();

        setBusy(true);
        LyleeFriendsApi.resetPasswordRecovery(username, cleanKey, newPassword)
                .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                    setBusy(false);
                    if (ex != null || res == null || !res.success) {
                        String msg = ex != null && ex.getMessage() != null && !ex.getMessage().isEmpty()
                                ? ex.getMessage() : getContext().getString(R.string.friends_register_failed);
                        showError(msg);
                        return;
                    }

                    Toast.makeText(getContext(), getContext().getString(R.string.friends_reset_password_success), Toast.LENGTH_SHORT).show();
                    dismiss();

                    new RecoveryKeyDialog(
                            getContext(),
                            getContext().getString(R.string.friends_reset_password_success),
                            "Mã khôi phục cũ đã hết hiệu lực. Dưới đây là MÃ KHÔI PHỤC MỚI của bạn. Vui lòng sao chép và lưu trữ an toàn!",
                            res.newRecoveryKey,
                            onSuccess
                    ).show();
                }).start();
    }

    private void setBusy(boolean busy) {
        if (progress != null) progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (submitButton != null) submitButton.setEnabled(!busy);
        if (cancelButton != null) cancelButton.setEnabled(!busy);
    }

    private void showError(String msg) {
        if (errorText != null) {
            errorText.setText(msg);
            errorText.setVisibility(View.VISIBLE);
        }
    }
}
