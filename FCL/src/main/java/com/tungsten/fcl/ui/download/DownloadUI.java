package com.tungsten.fcl.ui.download;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.tabs.TabLayout;
import com.tungsten.fcl.R;
import com.tungsten.fcl.setting.Profile;
import com.tungsten.fcl.setting.Profiles;
import com.tungsten.fcl.ui.download.common.DownloadPage;
import com.tungsten.fcl.ui.download.version.VersionInstallPage;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fcllibrary.component.ui.FCLCommonUI;
import com.tungsten.fcllibrary.component.ui.FCLPage;
import com.tungsten.fcllibrary.component.view.FCLTabLayout;
import com.tungsten.fcllibrary.component.view.FCLUILayout;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Download UI: trang cài game và 5 chế độ tải (Mod/Modpack/Resource Pack/World/Shader) dùng chung 1
 * instance DownloadPage, chuyển tab chỉ cập nhật nguồn dữ liệu và khôi phục trạng thái, không tạo lại trang.
 */
public class DownloadUI extends FCLCommonUI {

    public static final int PAGE_ID_DOWNLOAD_GAME = 15010;
    public static final int PAGE_ID_DOWNLOAD_MODPACK = 15011;
    public static final int PAGE_ID_DOWNLOAD_MOD = 15012;
    public static final int PAGE_ID_DOWNLOAD_RESOURCE_PACK = 15013;
    public static final int PAGE_ID_DOWNLOAD_WORLD = 15014;
    public static final int PAGE_ID_DOWNLOAD_SHADER_PACK = 15015;

    private static final int TEMP_PAGE_ANIM_DURATION = 200;

    public FCLTabLayout tabLayout;
    public FCLUILayout container;

    private FrameLayout contentContainer;
    private FrameLayout overlay;

    private VersionInstallPage versionInstallPage;
    private DownloadPage downloadPage;

    private final ArrayList<FCLPage> tempPageStack = new ArrayList<>();
    private int currentPageId = PAGE_ID_DOWNLOAD_GAME;

    private final Consumer<Profile> versionsListener = this::loadVersions;
    private Profile listenerProfile;
    private Runnable selectedVersionListener;

    public DownloadUI(Context context, int id) {
        super(context, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        tabLayout = findViewById(R.id.tab_layout);
        container = findViewById(R.id.container);

        // Lớp nội dung: trang cài game + trang tải dùng chung
        contentContainer = new FrameLayout(getContext());
        container.addView(contentContainer, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        versionInstallPage = new VersionInstallPage(getContext(), PAGE_ID_DOWNLOAD_GAME, R.layout.page_install_version);
        downloadPage = new DownloadPage(getContext(), R.layout.page_download);
        contentContainer.addView(versionInstallPage.getContentView(), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        contentContainer.addView(downloadPage.getContentView(), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        downloadPage.getContentView().setVisibility(View.GONE);

        // Lớp phủ trang tạm
        overlay = new FrameLayout(getContext());
        overlay.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setVisibility(View.GONE);
        container.addView(overlay);

        // Chuyển tab: trang game độc lập, 5 chế độ tải dùng chung DownloadPage
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        Profiles.registerVersionsListener(versionsListener);
        downloadPage.loadVersion(Profiles.getSelectedProfile(), null);
        listenerProfile = Profiles.getSelectedProfile();
        selectedVersionListener = () -> loadVersions(Profiles.getSelectedProfile());
        listenerProfile.addSelectedVersionListener(selectedVersionListener);

        // Hủy đăng ký listener khi UI bị ViewPager thu hồi (thay cho vòng đời onDestroy cũ), tránh rò rỉ tích lũy vào list tĩnh
        getContentView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {

            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                Profiles.unregisterVersionsListener(versionsListener);
                if (selectedVersionListener != null) {
                    listenerProfile.removeSelectedVersionListener(selectedVersionListener);
                }
            }
        });
    }

    private void switchTab(int position) {
        // Đóng trang tạm khi chuyển tab (trang tạm thuộc ngữ cảnh trang gốc, tránh lớp phủ che trang mới)
        dismissAllTempPages();
        if (position == 0) {
            if (currentPageId == PAGE_ID_DOWNLOAD_GAME) return;
            currentPageId = PAGE_ID_DOWNLOAD_GAME;
            versionInstallPage.getContentView().setVisibility(View.VISIBLE);
            downloadPage.getContentView().setVisibility(View.GONE);
            playEnterAnimation(versionInstallPage.getContentView());
        } else {
            int pageId = tabPositionToPageId(position);
            if (currentPageId == pageId) return;
            currentPageId = pageId;
            downloadPage.getContentView().setVisibility(View.VISIBLE);
            versionInstallPage.getContentView().setVisibility(View.GONE);
            downloadPage.switchType(pageId);
            // 5 trang chế độ tải dùng chung 1 view, cập nhật nội dung xong thì phát hoạt ảnh chuyển cảnh (áp dụng cả trang game ↔ trang chế độ và giữa các trang chế độ)
            playEnterAnimation(downloadPage.getContentView());
        }
    }

    private static int tabPositionToPageId(int position) {
        return switch (position) {
            case 1 -> PAGE_ID_DOWNLOAD_MODPACK;
            case 2 -> PAGE_ID_DOWNLOAD_MOD;
            case 3 -> PAGE_ID_DOWNLOAD_RESOURCE_PACK;
            case 4 -> PAGE_ID_DOWNLOAD_WORLD;
            default -> PAGE_ID_DOWNLOAD_SHADER_PACK;
        };
    }

    private static int pageIdToTabPosition(int pageId) {
        return switch (pageId) {
            case PAGE_ID_DOWNLOAD_MODPACK -> 1;
            case PAGE_ID_DOWNLOAD_MOD -> 2;
            case PAGE_ID_DOWNLOAD_RESOURCE_PACK -> 3;
            case PAGE_ID_DOWNLOAD_WORLD -> 4;
            default -> 5;
        };
    }

    /**
     * Hoạt ảnh chuyển trang: mờ dần + trượt lên (chạy đồng bộ, lần đầu trang hiện chính là điểm bắt đầu hoạt ảnh, tránh chớp hiện-rồi-mờ)
     */
    private void playEnterAnimation(View view) {
        view.animate().cancel();
        view.setAlpha(0f);
        view.setTranslationY(view.getResources().getDisplayMetrics().density * 30f);
        view.animate().alpha(1f).translationY(0f).setDuration(250).start();
    }

    /**
     * Chuyển cảnh khi quay lại: trang dưới trượt lên (chỉ dịch chuyển không làm mờ, tránh chớp hình do alpha đổi lớp phần cứng khi chồng với trang tạm đang mờ dần)
     */
    private void slideIn(View view) {
        view.animate().cancel();
        view.setTranslationY(view.getResources().getDisplayMetrics().density * 30f);
        view.animate().translationY(0f).setDuration(250).start();
    }

    /**
     * Cho nơi khác điều hướng tới (VD trang quản lý mod): chuyển sang chế độ tải chỉ định và hiện trang tải
     */
    public void showDownloadPage(int pageId) {
        TabLayout.Tab tab = tabLayout.getTabAt(pageIdToTabPosition(pageId));
        if (tab != null) {
            tab.select();
        }
    }

    public DownloadPage getDownloadPage() {
        return downloadPage;
    }

    public boolean canReturn() {
        return !tempPageStack.isEmpty();
    }

    /**
     * Hiện trang tạm trên lớp phủ và đẩy vào ngăn xếp điều hướng (ẩn nội dung dưới, trang tạm chiếm toàn bộ hiển thị)
     */
    public void showTempPage(FCLPage page) {
        if (overlay == null) return;
        // Ẩn trang tạm đỉnh ngăn xếp hiện tại và nội dung dưới, tránh lộ nội dung qua nền trong suốt
        if (!tempPageStack.isEmpty()) {
            tempPageStack.get(tempPageStack.size() - 1).getContentView().setVisibility(View.GONE);
        }
        contentContainer.setVisibility(View.GONE);
        // Trang tạm mới hiện mờ dần vào
        View view = page.getContentView();
        view.setAlpha(0f);
        overlay.setVisibility(View.VISIBLE);
        overlay.addView(view, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        view.animate().alpha(1f).setDuration(TEMP_PAGE_ANIM_DURATION).start();
        tempPageStack.add(page);
    }

    /**
     * Đóng trang tạm ở đỉnh ngăn xếp (mờ dần rồi gỡ, khôi phục trang dưới)
     */
    public void dismissCurrentTempPage() {
        if (tempPageStack.isEmpty()) return;
        FCLPage page = tempPageStack.remove(tempPageStack.size() - 1);
        View view = page.getContentView();
        // Khôi phục trang dưới (chạy song song lúc trang tạm mờ dần, tạo hoạt ảnh chuyển cảnh khi quay lại)
        if (!tempPageStack.isEmpty()) {
            View lowerView = tempPageStack.get(tempPageStack.size() - 1).getContentView();
            lowerView.setVisibility(View.VISIBLE);
            slideIn(lowerView);
        } else {
            contentContainer.setVisibility(View.VISIBLE);
            slideIn(contentContainer);
        }
        view.animate().alpha(0f).setDuration(TEMP_PAGE_ANIM_DURATION).withEndAction(() -> {
            overlay.removeView(view);
            if (tempPageStack.isEmpty()) {
                overlay.setVisibility(View.GONE);
            }
        }).start();
    }

    /**
     * Đóng hết mọi trang tạm
     */
    public void dismissAllTempPages() {
        while (!tempPageStack.isEmpty()) {
            dismissCurrentTempPage();
        }
    }

    @Override
    public void onBackPressed() {
        if (canReturn()) {
            dismissCurrentTempPage();
        } else {
            super.onBackPressed();
        }
    }

    private void loadVersions(Profile profile) {
        if (profile == Profiles.getSelectedProfile()) {
            downloadPage.loadVersion(profile, null);
            // Gỡ listener cũ trước rồi mới thêm, tránh đăng ký lặp tích lũy (giữ tham chiếu instance UI cũ gây rò rỉ)
            if (selectedVersionListener != null) {
                listenerProfile.removeSelectedVersionListener(selectedVersionListener);
            }
            selectedVersionListener = () -> loadVersions(Profiles.getSelectedProfile());
            listenerProfile = profile;
            profile.addSelectedVersionListener(selectedVersionListener);
        }
    }

    @Override
    public Task<?> refresh(Object... param) {
        return null;
    }
}
