package com.tungsten.fcl.lylee;

import android.content.Context;

import com.tungsten.fcl.R;
import com.tungsten.fcl.setting.Profiles;
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

    public LyleeCobblemonUI(Context context, int id) {
        super(context, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        status = findViewById(R.id.status);
        connect = findViewById(R.id.connect);
        connect.setOnClickListener(v -> LyleeCobblemonConnector.connect(getContext(), Profiles.getSelectedProfile()));
        connect.setOnLongClickListener(v -> {
            LyleeCobblemonConnector.showInfo(getContext(), Profiles.getSelectedProfile());
            return true;
        });
        refreshStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh mỗi lần quay lại trang (VD vừa cài xong rồi bấm Home rồi quay
        // lại) — trạng thái đã cài hay chưa có thể đổi từ lúc trang được tạo.
        refreshStatus();
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
    }

    @Override
    public Task<?> refresh(Object... param) {
        return null;
    }
}
