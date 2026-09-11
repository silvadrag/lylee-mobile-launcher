package com.tungsten.fcl.ui.account;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.tungsten.fcl.R;
import com.tungsten.fcl.lylee.LyleeFriendsApi;
import com.tungsten.fcl.lylee.LyleeFriendsSession;
import com.tungsten.fcl.util.AndroidUtils;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fcllibrary.component.dialog.FCLDialog;
import com.tungsten.fcllibrary.component.view.FCLButton;
import com.tungsten.fcllibrary.component.view.FCLEditText;
import com.tungsten.fcllibrary.component.view.FCLProgressBar;
import com.tungsten.fcllibrary.component.view.FCLTextView;

/**
 * Đặt mật khẩu + xác thực email cho tài khoản Lylee (kết bạn/chat) NGAY lúc
 * tạo tài khoản ngoại tuyến — gộp bước "claim" vào luồng tạo tài khoản thay vì
 * bắt vào riêng màn Bạn bè mới thấy (xem AccountUI, CreateAccountDialog
 * onSuccess). 2 bước y hệt FriendsActivity's register flow (registerStart rồi
 * registerConfirm), chỉ khác là hiện trong dialog gọn thay vì cả màn hình, và
 * có nút "Bỏ qua" (tạo tài khoản ngoại tuyến thường, không có mật khẩu Lylee —
 * vẫn liên kết Google được sau qua icon trên dòng tài khoản).
 */
public class SetFriendsPasswordDialog extends FCLDialog {

    private final String username;
    private final FCLTextView subtitle;
    private final FCLEditText passwordField;
    private final FCLEditText confirmPasswordField;
    private final FCLTextView errorText;
    private final FCLButton skipButton;
    private final FCLButton submitButton;
    private final FCLProgressBar progress;

    public SetFriendsPasswordDialog(Context context, String username) {
        super(context);
        this.username = username;
        setCancelable(false);
        setContentView(R.layout.dialog_set_password);

        subtitle = findViewById(R.id.subtitle);
        passwordField = findViewById(R.id.password);
        confirmPasswordField = findViewById(R.id.confirm_password);
        errorText = findViewById(R.id.error);
        skipButton = findViewById(R.id.skip);
        submitButton = findViewById(R.id.submit);
        progress = findViewById(R.id.progress);

        subtitle.setText(AndroidUtils.getLocalizedText(context, "account_set_password_subtitle", username));
        skipButton.setOnClickListener(v -> dismiss());
        submitButton.setOnClickListener(v -> onSubmit());
    }

    private void onSubmit() {
        errorText.setVisibility(View.GONE);
        String password = passwordField.getText() != null ? passwordField.getText().toString() : "";
        String confirmPassword = confirmPasswordField.getText() != null ? confirmPasswordField.getText().toString() : "";

        if (password.length() < 4) {
            showError(getContext().getString(R.string.friends_password_too_short));
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError(getContext().getString(R.string.friends_password_mismatch));
            return;
        }

        setBusy(true);
        LyleeFriendsApi.register(username, password)
                .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                    setBusy(false);
                    if (ex != null || res == null) {
                        String msg = ex != null && ex.getMessage() != null && !ex.getMessage().isEmpty()
                                ? ex.getMessage() : getContext().getString(R.string.friends_register_failed);
                        showError(msg);
                        return;
                    }

                    LyleeFriendsSession.save(getContext(), res.token, res.username, res.expiresAt);
                    dismiss();

                    new RecoveryKeyDialog(
                            getContext(),
                            getContext().getString(R.string.recovery_key_dialog_title),
                            getContext().getString(R.string.recovery_key_dialog_subtitle),
                            res.recoveryKey,
                            null
                    ).show();
                }).start();
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!busy);
        skipButton.setEnabled(!busy);
    }

    private void showError(String msg) {
        errorText.setText(msg);
        errorText.setVisibility(View.VISIBLE);
    }
}
