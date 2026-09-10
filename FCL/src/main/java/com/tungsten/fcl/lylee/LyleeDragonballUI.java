package com.tungsten.fcl.lylee;

import android.content.Context;
import android.view.View;

import com.tungsten.fcl.R;
import com.tungsten.fcl.setting.Accounts;
import com.tungsten.fcl.setting.Profiles;
import com.tungsten.fcl.util.AndroidUtils;
import com.tungsten.fclcore.auth.Account;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fcllibrary.component.ui.FCLCommonUI;
import com.tungsten.fcllibrary.component.view.FCLButton;
import com.tungsten.fcllibrary.component.view.FCLTextView;

/**
 * Tab riêng "Lylee Dragonball" trên thanh nav chính của FCL launcher.
 * Hiển thị trạng thái cài đặt, số người chơi online, thời gian chơi và nút cài/cập nhật.
 */
public class LyleeDragonballUI extends FCLCommonUI {

    private FCLTextView status;
    private FCLButton connect;
    private FCLTextView playtime;
    private FCLTextView serverStatus;

    public LyleeDragonballUI(Context context, int id) {
        super(context, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        status = findViewById(R.id.status);
        connect = findViewById(R.id.connect);
        playtime = findViewById(R.id.playtime);
        serverStatus = findViewById(R.id.server_status);
        connect.setOnClickListener(v -> LyleeDragonballConnector.connect(getContext(), Profiles.getSelectedProfile()));
        connect.setOnLongClickListener(v -> {
            LyleeDragonballConnector.showInfo(getContext(), Profiles.getSelectedProfile());
            return true;
        });
        refreshStatus();
        refreshServerStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatus();
        refreshServerStatus();
    }

    /** Số người chơi đang online server Dragonball (profile ID 4) */
    private void refreshServerStatus() {
        LyleeServerStatus.fetchOnlinePlayers(LyleeDragonballConnector.SERVER_PROFILE_ID)
                .whenComplete(Schedulers.androidUIThread(), (onlinePlayers, exception) -> {
                    if (exception != null || onlinePlayers == null) {
                        serverStatus.setVisibility(View.GONE);
                        return;
                    }
                    serverStatus.setText(AndroidUtils.getLocalizedText(getContext(), "lylee_dragonball_online_players", onlinePlayers));
                    serverStatus.setVisibility(View.VISIBLE);
                }).start();
    }

    private void refreshStatus() {
        boolean installed = Profiles.getSelectedProfile().getRepository()
                .hasVersion(LyleeDragonballConnector.VERSION_NAME);
        status.setText(installed
                ? R.string.lylee_dragonball_status_installed
                : R.string.lylee_dragonball_status_not_installed);
        connect.setText(installed
                ? R.string.lylee_dragonball_cta_update
                : R.string.lylee_dragonball_cta_install);
        refreshPlaytime();
    }

    /** Tổng thời gian chơi server Lylee Dragonball */
    private void refreshPlaytime() {
        Account account = Accounts.getSelectedAccount();
        if (account == null) {
            playtime.setVisibility(View.GONE);
            return;
        }
        LyleeSessionTracker.fetchTotalPlaytimeSeconds(account.getUsername())
                .whenComplete(Schedulers.androidUIThread(), (seconds, exception) -> {
                    if (exception != null || seconds == null || seconds <= 0) {
                        playtime.setVisibility(View.GONE);
                        return;
                    }
                    long hours = seconds / 3600;
                    long minutes = (seconds % 3600) / 60;
                    playtime.setText(AndroidUtils.getLocalizedText(getContext(), "lylee_dragonball_playtime", hours, minutes));
                    playtime.setVisibility(View.VISIBLE);
                }).start();
    }

    @Override
    public Task<?> refresh(Object... param) {
        return null;
    }
}
