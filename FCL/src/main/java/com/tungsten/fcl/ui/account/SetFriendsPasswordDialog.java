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
    private final FCLEditText emailField;
    private final FCLEditText codeField;
    private final FCLEditText passwordField;
    private final FCLTextView errorText;
    private final FCLButton skipButton;
    private final FCLButton submitButton;
    private final FCLProgressBar progress;
    private boolean codeSent = false;

    public SetFriendsPasswordDialog(Context context, String username) {
        super(context);
        this.username = username;
        setCancelable(false);
        setContentView(R.layout.dialog_set_password);

        subtitle = findViewById(R.id.subtitle);
        emailField = findViewById(R.id.email);
        codeField = findViewById(R.id.code);
        passwordField = findViewById(R.id.password);
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
        if (!codeSent) {
            String email = String.valueOf(emailField.getText());
            if (email.isEmpty()) return;
            setBusy(true);
            LyleeFriendsApi.registerStart(username, email)
                    .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                        setBusy(false);
                        if (ex != null || res == null || !res.success) {
                            showError(R.string.friends_register_send_code_failed);
                            return;
                        }
                        codeSent = true;
                        codeField.setVisibility(View.VISIBLE);
                        passwordField.setVisibility(View.VISIBLE);
                        submitButton.setText(R.string.friends_register_confirm);
                        Toast.makeText(getContext(), getContext().getString(R.string.friends_register_code_sent), Toast.LENGTH_LONG).show();
                    }).start();
        } else {
            String code = String.valueOf(codeField.getText());
            String password = String.valueOf(passwordField.getText());
            if (code.isEmpty() || password.isEmpty()) return;
            setBusy(true);
            LyleeFriendsApi.registerConfirm(username, code, password)
                    .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                        setBusy(false);
                        if (ex != null || res == null) {
                            showError(R.string.friends_register_confirm_failed);
                            return;
                        }
                        LyleeFriendsSession.save(getContext(), res.token, res.username, res.expiresAt);
                        Toast.makeText(getContext(), getContext().getString(R.string.friends_settings_password_set), Toast.LENGTH_SHORT).show();
                        dismiss();
                    }).start();
        }
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!busy);
        skipButton.setEnabled(!busy);
    }

    private void showError(int resId) {
        errorText.setText(resId);
        errorText.setVisibility(View.VISIBLE);
    }
}
