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
 * Tab riêng "Lylee Cobblemon" trên thanh nav chính — nâng cấp từ 1 nút góc màn
 * hình chính (xem docs/PLAN.md mục 11+13) lên thành trang riêng, đúng tinh
 * thần "2 tab Cobblemon + Instances tự do" bên launcher PC. Logic kết nối/đồng
 * bộ vẫn dùng nguyên {@link LyleeCobblemonConnector}, trang này chỉ là lớp
 * trình bày (hiện trạng thái đã cài hay chưa + 1 nút CTA).
 */
public class LyleeCobblemonUI extends FCLCommonUI {

    private FCLTextView status;
    private FCLButton connect;
    private FCLTextView playtime;
    private FCLTextView serverStatus;

    public LyleeCobblemonUI(Context context, int id) {
        super(context, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        status = findViewById(R.id.status);
        connect = findViewById(R.id.connect);
        playtime = findViewById(R.id.playtime);
        serverStatus = findViewById(R.id.server_status);
        connect.setOnClickListener(v -> LyleeCobblemonConnector.connect(getContext(), Profiles.getSelectedProfile()));
        connect.setOnLongClickListener(v -> {
            LyleeCobblemonConnector.showInfo(getContext(), Profiles.getSelectedProfile());
            return true;
        });
        refreshStatus();
        refreshServerStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh mỗi lần quay lại trang (VD vừa cài xong rồi bấm Home rồi quay
        // lại) — trạng thái đã cài hay chưa có thể đổi từ lúc trang được tạo.
        refreshStatus();
        refreshServerStatus();
    }

    /** Số người chơi đang online server — xem LyleeServerStatus. Ẩn hẳn nếu
     *  server không báo số liệu (VD server đang tắt) hoặc lỗi mạng. */
    private void refreshServerStatus() {
        LyleeServerStatus.fetchOnlinePlayers()
                .whenComplete(Schedulers.androidUIThread(), (onlinePlayers, exception) -> {
                    if (exception != null || onlinePlayers == null) {
                        serverStatus.setVisibility(View.GONE);
                        return;
                    }
                    serverStatus.setText(AndroidUtils.getLocalizedText(getContext(), "lylee_cobblemon_online_players", onlinePlayers));
                    serverStatus.setVisibility(View.VISIBLE);
                }).start();
    }

    private void refreshStatus() {
        boolean installed = Profiles.getSelectedProfile().getRepository()
                .hasVersion(LyleeCobblemonConnector.VERSION_NAME);
        status.setText(installed
                ? R.string.lylee_cobblemon_status_installed
                : R.string.lylee_cobblemon_status_not_installed);
        connect.setText(installed
                ? R.string.lylee_cobblemon_cta_update
                : R.string.lylee_cobblemon_cta_install);
        refreshPlaytime();
    }

    /** Tổng thời gian chơi server Lylee Cobblemon — xem LyleeSessionTracker.
     *  Ẩn hẳn nếu chưa từng chơi/lỗi mạng, không hiện "0 phút" gây hiểu nhầm. */
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
                    playtime.setText(AndroidUtils.getLocalizedText(getContext(), "lylee_cobblemon_playtime", hours, minutes));
                    playtime.setVisibility(View.VISIBLE);
                }).start();
    }

    @Override
    public Task<?> refresh(Object... param) {
        return null;
    }
}
