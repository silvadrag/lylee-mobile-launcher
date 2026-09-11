package com.tungsten.fcl.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.tungsten.fcl.R;
import com.tungsten.fcl.lylee.LyleeFriendsApi;
import com.tungsten.fcl.lylee.LyleeFriendsSession;
import com.tungsten.fcl.setting.Accounts;
import com.tungsten.fcl.ui.account.ForgotPasswordDialog;
import com.tungsten.fcl.ui.account.RecoveryKeyDialog;
import com.tungsten.fcl.util.AndroidUtils;
import com.tungsten.fclcore.auth.Account;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fcllibrary.component.FCLActivity;
import com.tungsten.fcllibrary.component.view.FCLButton;
import com.tungsten.fcllibrary.component.view.FCLEditText;
import com.tungsten.fcllibrary.component.view.FCLImageButton;
import com.tungsten.fcllibrary.component.view.FCLProgressBar;
import com.tungsten.fcllibrary.component.view.FCLTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết bạn/chat — lần đầu bên mobile, cùng backend PC đã dùng thật (xem
 * docs/PLAN.md mục 20). Gộp chung 1 Activity với 3 "màn hình" (đăng nhập,
 * danh sách bạn bè, chat) chuyển bằng ẩn/hiện container thay vì 3 Activity
 * riêng — đơn giản hơn, tránh chi phí chuyển Activity.
 * <p>
 * V1: CHƯA có sửa/thu hồi/react/forward/gõ-đang-nhập/gửi ảnh — chỉ xem danh
 * sách bạn bè, gửi lời mời, chấp nhận/từ chối, xóa bạn, chặn, xem hội thoại,
 * gửi tin nhắn chữ. Các tính năng nâng cao để dành đợt sau (xem PLAN.md).
 */
public class FriendsActivity extends FCLActivity {

    private static final int POLL_INTERVAL_MS = 3000;

    private enum Screen { LOGIN, LIST, CHAT, ACCOUNT_SETTINGS }

    // --- Đăng nhập ---
    private View loginBack;
    private View loginContainer;
    private FCLTextView loginSubtitle;
    private FCLEditText loginPassword;
    private FCLEditText loginConfirmPassword;
    private FCLTextView loginForgotPassword;
    private FCLButton loginSubmit;
    private FCLTextView loginError;
    private FCLProgressBar loginProgress;
    private View loginGoogle;
    private GoogleSignInClient googleSignInClient;
    private boolean isClaimed = false;

    // --- Danh sách ---
    private View listContainer;
    private ListView friendList;
    private FriendRowAdapter friendRowAdapter;
    private final List<Object> friendRows = new ArrayList<>();

    // --- Chat ---
    private View chatContainer;
    private FCLTextView chatTitle;
    private ListView chatList;
    private ChatMessageAdapter chatMessageAdapter;
    private final List<LyleeFriendsApi.MessageResponse> chatMessages = new ArrayList<>();
    private FCLEditText chatInput;
    private String chatOtherUsername;

    // --- Cài đặt tài khoản (thêm phương thức xác thực còn thiếu) ---
    private View settingsContainer;
    private FCLTextView settingsPasswordStatus;
    private FCLButton settingsAddPassword;
    private View settingsPasswordForm;
    private FCLEditText settingsNewPassword;
    private FCLEditText settingsConfirmPassword;
    private FCLButton settingsPasswordSubmit;
    private FCLTextView settingsGoogleStatus;
    private FCLButton settingsAddGoogle;
    private FCLTextView settingsError;
    private FCLProgressBar settingsProgress;

    private String myUsername;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Screen currentScreen = Screen.LOGIN;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentScreen == Screen.LIST) {
                loadFriends();
            } else if (currentScreen == Screen.CHAT) {
                loadConversation();
            }
            pollHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        Account account = Accounts.getSelectedAccount();
        if (account == null) {
            Toast.makeText(this, getString(R.string.friends_need_account), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        myUsername = account.getUsername();

        bindLoginViews();
        bindListViews();
        bindChatViews();
        bindSettingsViews();

        if (LyleeFriendsSession.isValid(this, myUsername)) {
            showScreen(Screen.LIST);
        } else {
            showScreen(Screen.LOGIN);
            checkClaimStatus();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentScreen == Screen.LIST || currentScreen == Screen.CHAT) {
            pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
        }
    }

    private void showScreen(Screen screen) {
        currentScreen = screen;
        loginBack.setVisibility(screen == Screen.LOGIN ? View.VISIBLE : View.GONE);
        loginContainer.setVisibility(screen == Screen.LOGIN ? View.VISIBLE : View.GONE);
        listContainer.setVisibility(screen == Screen.LIST ? View.VISIBLE : View.GONE);
        chatContainer.setVisibility(screen == Screen.CHAT ? View.VISIBLE : View.GONE);
        settingsContainer.setVisibility(screen == Screen.ACCOUNT_SETTINGS ? View.VISIBLE : View.GONE);
        pollHandler.removeCallbacks(pollRunnable);
        if (screen == Screen.LIST) {
            loadFriends();
            pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
        } else if (screen == Screen.CHAT) {
            loadConversation();
            pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
        }
    }

    @Override
    public void onBackPressed() {
        if (currentScreen == Screen.CHAT) {
            showScreen(Screen.LIST);
        } else if (currentScreen == Screen.ACCOUNT_SETTINGS) {
            showScreen(Screen.LIST);
        } else if (currentScreen == Screen.LIST) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    // ===================== Đăng nhập / Đăng ký =====================

    private void bindLoginViews() {
        loginBack = findViewById(R.id.login_back);
        loginBack.setOnClickListener(v -> finish());
        loginContainer = findViewById(R.id.login_container);
        loginSubtitle = findViewById(R.id.login_subtitle);
        loginPassword = findViewById(R.id.login_password);
        loginConfirmPassword = findViewById(R.id.login_confirm_password);
        loginForgotPassword = findViewById(R.id.login_forgot_password);
        loginSubmit = findViewById(R.id.login_submit);
        loginError = findViewById(R.id.login_error);
        loginProgress = findViewById(R.id.login_progress);
        loginGoogle = findViewById(R.id.login_google);
        loginSubmit.setOnClickListener(v -> onLoginSubmit());
        if (loginForgotPassword != null) {
            loginForgotPassword.setOnClickListener(v -> onForgotPassword());
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(LyleeFriendsApi.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        loginGoogle.setOnClickListener(v -> {
            loginError.setVisibility(View.GONE);
            startActivityForResult(googleSignInClient.getSignInIntent(), this::onGoogleSignInResult);
        });
    }

    private void onForgotPassword() {
        new ForgotPasswordDialog(this, myUsername, () -> {
            loginPassword.setText("");
            if (loginConfirmPassword != null) loginConfirmPassword.setText("");
        }).show();
    }

    private void onGoogleSignInResult(androidx.activity.result.ActivityResult result) {
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                    .getResult(ApiException.class);
            String idToken = account.getIdToken();
            if (idToken == null) {
                showLoginError(R.string.friends_google_login_failed);
                return;
            }
            setLoginBusy(true);
            LyleeFriendsApi.googleLogin(idToken, myUsername)
                    .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                        setLoginBusy(false);
                        if (ex != null || res == null) {
                            showLoginError(R.string.friends_google_login_failed);
                            return;
                        }
                        LyleeFriendsSession.save(this, res.token, res.username, res.expiresAt);
                        showScreen(Screen.LIST);
                    }).start();
        } catch (ApiException e) {
            // Mã 12501 = người dùng tự hủy chọn tài khoản — không phải lỗi thật, khỏi hiện thông báo đỏ
            if (e.getStatusCode() == 12501) {
                Toast.makeText(this, getString(R.string.friends_google_cancelled), Toast.LENGTH_SHORT).show();
            } else {
                showLoginError(R.string.friends_google_login_failed);
            }
        }
    }

    private void checkClaimStatus() {
        loginSubtitle.setText(AndroidUtils.getLocalizedText(this, "friends_login_checking", myUsername));
        LyleeFriendsApi.claimStatus(myUsername)
                .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                    if (ex != null || res == null) {
                        loginSubtitle.setText(getString(R.string.friends_login_check_failed));
                        return;
                    }
                    isClaimed = res.claimed;
                    if (isClaimed) {
                        loginSubtitle.setText(AndroidUtils.getLocalizedText(this, "friends_login_subtitle_existing", myUsername));
                        loginSubmit.setText(R.string.friends_login_button);
                        if (loginConfirmPassword != null) loginConfirmPassword.setVisibility(View.GONE);
                        if (loginForgotPassword != null) loginForgotPassword.setVisibility(View.VISIBLE);
                    } else {
                        loginSubtitle.setText(AndroidUtils.getLocalizedText(this, "friends_login_subtitle_new", myUsername));
                        loginSubmit.setText(R.string.friends_register_button);
                        if (loginConfirmPassword != null) loginConfirmPassword.setVisibility(View.VISIBLE);
                        if (loginForgotPassword != null) loginForgotPassword.setVisibility(View.GONE);
                    }
                }).start();
    }

    private void onLoginSubmit() {
        loginError.setVisibility(View.GONE);
        if (isClaimed) {
            String password = String.valueOf(loginPassword.getText());
            if (password.isEmpty()) return;
            setLoginBusy(true);
            LyleeFriendsApi.login(myUsername, password)
                    .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                        setLoginBusy(false);
                        if (ex != null || res == null) {
                            showLoginError(R.string.friends_login_failed);
                            return;
                        }
                        LyleeFriendsSession.save(this, res.token, res.username, res.expiresAt);
                        if (res.recoveryKey != null && !res.recoveryKey.isEmpty()) {
                            new RecoveryKeyDialog(
                                    this,
                                    "CẤP MÃ KHÔI PHỤC BẢO MẬT",
                                    "Hệ thống đã nâng cấp cơ chế bảo mật bằng Mã Khôi Phục (thay thế email). Dưới đây là Mã Khôi Phục duy nhất cho tài khoản của bạn. Vui lòng sao chép và lưu trữ an toàn ngay bây giờ!",
                                    res.recoveryKey,
                                    () -> showScreen(Screen.LIST)
                            ).show();
                        } else {
                            showScreen(Screen.LIST);
                        }
                    }).start();
        } else {
            String password = String.valueOf(loginPassword.getText());
            String confirmPassword = loginConfirmPassword != null ? String.valueOf(loginConfirmPassword.getText()) : "";
            if (password.length() < 4) {
                showLoginError(R.string.friends_password_too_short);
                return;
            }
            if (!password.equals(confirmPassword)) {
                showLoginError(R.string.friends_password_mismatch);
                return;
            }
            setLoginBusy(true);
            LyleeFriendsApi.register(myUsername, password)
                    .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                        setLoginBusy(false);
                        if (ex != null || res == null) {
                            showLoginError(R.string.friends_register_failed);
                            return;
                        }
                        LyleeFriendsSession.save(this, res.token, res.username, res.expiresAt);
                        new RecoveryKeyDialog(
                                this,
                                getString(R.string.recovery_key_dialog_title),
                                getString(R.string.recovery_key_dialog_subtitle),
                                res.recoveryKey,
                                () -> showScreen(Screen.LIST)
                        ).show();
                    }).start();
        }
    }

    private void setLoginBusy(boolean busy) {
        loginProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        loginSubmit.setEnabled(!busy);
        loginGoogle.setEnabled(!busy);
    }

    private void showLoginError(int resId) {
        loginError.setText(resId);
        loginError.setVisibility(View.VISIBLE);
    }

    // ===================== Cài đặt tài khoản =====================
    // Sau khi đã đăng nhập (Google HOẶC mật khẩu), cho phép thêm CÁCH CÒN LẠI —
    // trước đây đăng nhập 1 kiểu là hết cách đăng nhập kiểu kia, không có màn
    // nào để bổ sung. Dùng lại nguyên request registerStart/registerConfirm
    // (backend giờ chỉ chặn khi ĐÃ có mật khẩu, không còn chặn theo GoogleSub)
    // và luồng Google Sign-In đã có, chỉ khác ở chỗ không lưu session mới/
    // không chuyển màn sau khi xong — tài khoản đã đăng nhập sẵn rồi.

    private void bindSettingsViews() {
        settingsContainer = findViewById(R.id.settings_container);
        settingsPasswordStatus = findViewById(R.id.settings_password_status);
        settingsAddPassword = findViewById(R.id.settings_add_password);
        settingsPasswordForm = findViewById(R.id.settings_password_form);
        settingsNewPassword = findViewById(R.id.settings_new_password);
        settingsConfirmPassword = findViewById(R.id.settings_confirm_password);
        settingsPasswordSubmit = findViewById(R.id.settings_password_submit);
        settingsGoogleStatus = findViewById(R.id.settings_google_status);
        settingsAddGoogle = findViewById(R.id.settings_add_google);
        settingsError = findViewById(R.id.settings_error);
        settingsProgress = findViewById(R.id.settings_progress);
        FCLImageButton back = findViewById(R.id.settings_back);
        back.setOnClickListener(v -> showScreen(Screen.LIST));

        settingsAddPassword.setOnClickListener(v -> {
            settingsPasswordForm.setVisibility(View.VISIBLE);
            settingsAddPassword.setVisibility(View.GONE);
        });
        settingsPasswordSubmit.setOnClickListener(v -> onSettingsPasswordSubmit());
        settingsAddGoogle.setOnClickListener(v -> {
            settingsError.setVisibility(View.GONE);
            startActivityForResult(googleSignInClient.getSignInIntent(), this::onGoogleLinkResult);
        });
    }

    private void loadAccountSettings() {
        settingsError.setVisibility(View.GONE);
        settingsPasswordForm.setVisibility(View.GONE);
        setSettingsBusy(true);
        LyleeFriendsApi.claimStatus(myUsername)
                .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                    setSettingsBusy(false);
                    if (ex != null || res == null) {
                        settingsError.setText(R.string.friends_settings_load_failed);
                        settingsError.setVisibility(View.VISIBLE);
                        return;
                    }
                    settingsPasswordStatus.setText(res.hasPassword ? R.string.friends_settings_password_yes : R.string.friends_settings_password_no);
                    settingsAddPassword.setVisibility(res.hasPassword ? View.GONE : View.VISIBLE);
                    settingsGoogleStatus.setText(res.hasGoogle ? R.string.friends_settings_google_yes : R.string.friends_settings_google_no);
                    settingsAddGoogle.setVisibility(res.hasGoogle ? View.GONE : View.VISIBLE);
                }).start();
    }

    private void onSettingsPasswordSubmit() {
        settingsError.setVisibility(View.GONE);
        String password = settingsNewPassword != null ? String.valueOf(settingsNewPassword.getText()) : "";
        String confirm = settingsConfirmPassword != null ? String.valueOf(settingsConfirmPassword.getText()) : "";
        if (password.length() < 4) {
            showSettingsError(R.string.friends_password_too_short);
            return;
        }
        if (!password.equals(confirm)) {
            showSettingsError(R.string.friends_password_mismatch);
            return;
        }
        setSettingsBusy(true);
        LyleeFriendsApi.register(myUsername, password)
                .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                    setSettingsBusy(false);
                    if (ex != null || res == null) {
                        showSettingsError(R.string.friends_register_failed);
                        return;
                    }
                    Toast.makeText(this, getString(R.string.friends_settings_password_set), Toast.LENGTH_SHORT).show();
                    settingsPasswordForm.setVisibility(View.GONE);
                    new RecoveryKeyDialog(
                            this,
                            getString(R.string.recovery_key_dialog_title),
                            getString(R.string.recovery_key_dialog_subtitle),
                            res.recoveryKey,
                            null
                    ).show();
                    loadAccountSettings();
                }).start();
    }

    private void onGoogleLinkResult(androidx.activity.result.ActivityResult result) {
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                    .getResult(ApiException.class);
            String idToken = account.getIdToken();
            if (idToken == null) {
                showSettingsError(R.string.friends_google_login_failed);
                return;
            }
            setSettingsBusy(true);
            LyleeFriendsApi.googleLogin(idToken, myUsername)
                    .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                        setSettingsBusy(false);
                        if (ex != null || res == null) {
                            showSettingsError(R.string.friends_google_login_failed);
                            return;
                        }
                        Toast.makeText(this, getString(R.string.friends_settings_google_linked), Toast.LENGTH_SHORT).show();
                        loadAccountSettings();
                    }).start();
        } catch (ApiException e) {
            if (e.getStatusCode() != 12501) {
                showSettingsError(R.string.friends_google_login_failed);
            }
        }
    }

    private void setSettingsBusy(boolean busy) {
        settingsProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        settingsPasswordSubmit.setEnabled(!busy);
        settingsAddGoogle.setEnabled(!busy);
    }

    private void showSettingsError(int resId) {
        settingsError.setText(resId);
        settingsError.setVisibility(View.VISIBLE);
    }

    // ===================== Danh sách bạn bè =====================

    private void bindListViews() {
        listContainer = findViewById(R.id.list_container);
        friendList = findViewById(R.id.friend_list);
        FCLImageButton back = findViewById(R.id.list_back);
        FCLImageButton add = findViewById(R.id.list_add);
        FCLImageButton settings = findViewById(R.id.list_settings);
        back.setOnClickListener(v -> finish());
        add.setOnClickListener(v -> showAddFriendDialog());
        settings.setOnClickListener(v -> {
            showScreen(Screen.ACCOUNT_SETTINGS);
            loadAccountSettings();
        });
        friendRowAdapter = new FriendRowAdapter();
        friendList.setAdapter(friendRowAdapter);
    }

    private void loadFriends() {
        String token = LyleeFriendsSession.getToken(this);
        if (token == null) return;
        LyleeFriendsApi.listFriends(myUsername, token)
                .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                    if (ex != null || res == null) return;
                    friendRows.clear();
                    if (res.incomingRequests != null) friendRows.addAll(res.incomingRequests);
                    if (res.friends != null) friendRows.addAll(res.friends);
                    if (res.outgoingRequests != null) friendRows.addAll(res.outgoingRequests);
                    friendRowAdapter.notifyDataSetChanged();
                }).start();
    }

    private void showAddFriendDialog() {
        FCLEditText input = new FCLEditText(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.friends_add_title))
                .setView(input)
                .setPositiveButton(getString(R.string.friends_add_send), (dialog, which) -> {
                    String target = String.valueOf(input.getText()).trim();
                    if (target.isEmpty()) return;
                    String token = LyleeFriendsSession.getToken(this);
                    LyleeFriendsApi.sendFriendRequest(myUsername, token, target)
                            .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                                if (ex != null || res == null || !res.success) {
                                    Toast.makeText(this, getString(R.string.friends_add_failed), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, getString(R.string.friends_add_sent), Toast.LENGTH_SHORT).show();
                                    loadFriends();
                                }
                            }).start();
                })
                .setNegativeButton(getString(R.string.button_cancel), null)
                .show();
    }

    private class FriendRowAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return friendRows.size();
        }

        @Override
        public Object getItem(int position) {
            return friendRows.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView
                    : LayoutInflater.from(FriendsActivity.this).inflate(R.layout.item_friend_row, parent, false);
            LyleeFriendsApi.FriendshipResponse row = (LyleeFriendsApi.FriendshipResponse) friendRows.get(position);

            FCLTextView username = view.findViewById(R.id.row_username);
            FCLTextView subtitle = view.findViewById(R.id.row_subtitle);
            FCLButton accept = view.findViewById(R.id.row_action_accept);
            FCLButton decline = view.findViewById(R.id.row_action_decline);

            String display = row.otherUsername + (row.hasUnread ? " •" : "");
            username.setText(display);
            accept.setVisibility(View.GONE);
            decline.setVisibility(View.GONE);
            view.setOnClickListener(null);

            if ("Pending".equals(row.status) && row.isIncoming) {
                subtitle.setText(R.string.friends_status_incoming);
                accept.setVisibility(View.VISIBLE);
                decline.setVisibility(View.VISIBLE);
                accept.setOnClickListener(v -> respondRequest(row.friendshipId, "accept"));
                decline.setOnClickListener(v -> respondRequest(row.friendshipId, "decline"));
            } else if ("Pending".equals(row.status)) {
                subtitle.setText(R.string.friends_status_outgoing);
            } else if ("Blocked".equals(row.status)) {
                subtitle.setText(R.string.friends_status_blocked);
            } else {
                subtitle.setText(R.string.friends_status_friend);
                view.setOnClickListener(v -> openChat(row.otherUsername));
                view.setOnLongClickListener(v -> {
                    showFriendOptions(row.otherUsername);
                    return true;
                });
            }
            return view;
        }
    }

    private void respondRequest(int friendshipId, String action) {
        String token = LyleeFriendsSession.getToken(this);
        LyleeFriendsApi.respondFriendRequest(myUsername, token, friendshipId, action)
                .whenComplete(Schedulers.androidUIThread(), (res, ex) -> loadFriends()).start();
    }

    private void showFriendOptions(String other) {
        String[] options = {getString(R.string.friends_action_remove), getString(R.string.friends_action_block)};
        new android.app.AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    String token = LyleeFriendsSession.getToken(this);
                    if (which == 0) {
                        LyleeFriendsApi.removeFriend(myUsername, token, other)
                                .whenComplete(Schedulers.androidUIThread(), (res, ex) -> loadFriends()).start();
                    } else {
                        LyleeFriendsApi.blockFriend(myUsername, token, other)
                                .whenComplete(Schedulers.androidUIThread(), (res, ex) -> loadFriends()).start();
                    }
                })
                .show();
    }

    // ===================== Chat =====================

    private void bindChatViews() {
        chatContainer = findViewById(R.id.chat_container);
        chatTitle = findViewById(R.id.chat_title);
        chatList = findViewById(R.id.chat_list);
        chatInput = findViewById(R.id.chat_input);
        FCLImageButton back = findViewById(R.id.chat_back);
        FCLButton send = findViewById(R.id.chat_send);
        back.setOnClickListener(v -> showScreen(Screen.LIST));
        send.setOnClickListener(v -> sendChatMessage());
        chatMessageAdapter = new ChatMessageAdapter();
        chatList.setAdapter(chatMessageAdapter);
    }

    private void openChat(String other) {
        chatOtherUsername = other;
        chatTitle.setText(other);
        chatMessages.clear();
        chatMessageAdapter.notifyDataSetChanged();
        showScreen(Screen.CHAT);
    }

    private void loadConversation() {
        String token = LyleeFriendsSession.getToken(this);
        if (token == null || chatOtherUsername == null) return;
        LyleeFriendsApi.getConversation(myUsername, token, chatOtherUsername)
                .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                    if (ex != null || res == null) return;
                    chatMessages.clear();
                    chatMessages.addAll(res);
                    chatMessageAdapter.notifyDataSetChanged();
                }).start();
    }

    private void sendChatMessage() {
        String body = String.valueOf(chatInput.getText()).trim();
        if (body.isEmpty() || chatOtherUsername == null) return;
        String token = LyleeFriendsSession.getToken(this);
        chatInput.setText("");
        LyleeFriendsApi.sendMessage(myUsername, token, chatOtherUsername, body, null, null)
                .whenComplete(Schedulers.androidUIThread(), (res, ex) -> {
                    if (ex != null) {
                        Toast.makeText(this, getString(R.string.friends_chat_send_failed), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    loadConversation();
                }).start();
    }

    private class ChatMessageAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return chatMessages.size();
        }

        @Override
        public Object getItem(int position) {
            return chatMessages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return chatMessages.get(position).messageId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView
                    : LayoutInflater.from(FriendsActivity.this).inflate(R.layout.item_chat_message, parent, false);
            LyleeFriendsApi.MessageResponse msg = chatMessages.get(position);
            FCLTextView bubble = view.findViewById(R.id.msg_bubble);
            FCLTextView meta = view.findViewById(R.id.msg_meta);

            boolean isMine = myUsername.equals(msg.senderUsername);
            bubble.setText(msg.recalled ? getString(R.string.friends_message_recalled) : msg.body);
            bubble.setBackgroundResource(isMine ? R.drawable.bg_chat_bubble_mine : R.drawable.bg_chat_bubble_other);
            meta.setText((isMine ? getString(R.string.friends_you) : msg.senderUsername) + " · " + msg.sentAt);
            ((androidx.appcompat.widget.LinearLayoutCompat) view).setGravity(isMine ? android.view.Gravity.END : android.view.Gravity.START);
            return view;
        }
    }
}
