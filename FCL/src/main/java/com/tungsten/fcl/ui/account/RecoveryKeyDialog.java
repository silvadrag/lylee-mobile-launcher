package com.tungsten.fcl.ui.account;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tungsten.fcl.R;
import com.tungsten.fcllibrary.component.dialog.FCLDialog;
import com.tungsten.fcllibrary.component.view.FCLButton;
import com.tungsten.fcllibrary.component.view.FCLTextView;

public class RecoveryKeyDialog extends FCLDialog {

    public RecoveryKeyDialog(@NonNull Context context, @NonNull String recoveryKey, @Nullable Runnable onDismiss) {
        this(context, null, null, recoveryKey, onDismiss);
    }

    public RecoveryKeyDialog(@NonNull Context context, @Nullable String title, @Nullable String subtitle,
                             @NonNull String recoveryKey, @Nullable Runnable onDismiss) {
        super(context);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        setContentView(R.layout.dialog_recovery_key);

        FCLTextView titleView = findViewById(R.id.recovery_dialog_title);
        FCLTextView subtitleView = findViewById(R.id.recovery_dialog_subtitle);
        FCLTextView keyDisplay = findViewById(R.id.recovery_key_display);
        FCLButton copyButton = findViewById(R.id.recovery_copy_button);
        FCLTextView statusText = findViewById(R.id.recovery_status_text);
        FCLButton dismissButton = findViewById(R.id.recovery_dismiss_button);

        if (title != null && titleView != null) {
            titleView.setText(title);
        }
        if (subtitle != null && subtitleView != null) {
            subtitleView.setText(subtitle);
        }
        if (keyDisplay != null) {
            keyDisplay.setText(recoveryKey);
        }

        if (copyButton != null) {
            copyButton.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Lylee Recovery Key", recoveryKey);
                    clipboard.setPrimaryClip(clip);
                }
                if (statusText != null) {
                    statusText.setText(R.string.recovery_key_dialog_copied);
                    statusText.setVisibility(View.VISIBLE);
                }
            });
        }

        if (dismissButton != null) {
            dismissButton.setOnClickListener(v -> {
                dismiss();
                if (onDismiss != null) {
                    onDismiss.run();
                }
            });
        }
    }
}
