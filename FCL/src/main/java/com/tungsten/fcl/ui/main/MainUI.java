package com.tungsten.fcl.ui.main;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.reflect.TypeToken;
import com.tungsten.fcl.R;
import com.tungsten.fcl.activity.FriendsActivity;
import com.tungsten.fcl.activity.MainActivity;
import com.tungsten.fcl.game.TexturesLoader;
import com.tungsten.fcl.lylee.LyleeFriendsSession;
import com.tungsten.fcl.setting.Accounts;
import com.tungsten.fcl.ui.UIManager;
import com.tungsten.fclcore.auth.Account;
import com.tungsten.fclcore.fakefx.beans.property.ObjectProperty;
import com.tungsten.fclcore.fakefx.beans.property.SimpleObjectProperty;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.util.Logging;
import com.tungsten.fclcore.util.io.HttpRequest;
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog;
import com.tungsten.fcllibrary.component.theme.ThemeEngine;
import com.tungsten.fcllibrary.component.ui.FCLCommonUI;
import com.tungsten.fcllibrary.component.view.FCLButton;
import com.tungsten.fcllibrary.component.view.FCLImageButton;
import com.tungsten.fcllibrary.component.view.FCLTextView;
import com.tungsten.fcllibrary.skin.SkinRenderer;
import com.tungsten.fcllibrary.skin.SkinViewer;
import com.tungsten.fcllibrary.util.ConvertUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MainUI extends FCLCommonUI implements View.OnClickListener {

    // Endpoint thật — GET /api/mobile/announcement, xem ApiServer.getMobileAnnouncement
    // bên fabric-lyleelauncherAPI-mod-1.21.1 (trả 404 nếu chưa có thông báo nào
    // đang hiển thị, request tự fail êm, không hiện gì — catch Exception bên dưới).
    // Cố tình KHÔNG còn trỏ về repo GitHub thật của FCL-Team nữa (tránh hiện thông
    // báo/liên kết của đội FCL gốc bên trong app đã đổi thương hiệu Lylee).
    public static final String ANNOUNCEMENT_URL = "https://lylee-launcher-api.lyleelauncher.workers.dev/api/mobile/announcement";
    public static final String ANNOUNCEMENT_URL_CN = ANNOUNCEMENT_URL;

    // Danh sách nhiều thông báo gần đây (khác ANNOUNCEMENT_URL chỉ trả 1 cái mới
    // nhất) — dùng cho AnnouncementHistoryDialog mở từ nút chuông, xem
    // ApiServer.getMobileAnnouncements bên mod.
    public static final String ANNOUNCEMENT_LIST_URL = "https://lylee-launcher-api.lyleelauncher.workers.dev/api/mobile/announcements";

    private static final long AUTO_ADVANCE_MS = 6000;

    private LinearLayoutCompat announcementContainer;
    private LinearLayoutCompat announcementLayout;
    private FCLTextView title;
    private ViewPager2 announcementPager;
    private LinearLayoutCompat announcementDots;
    private FCLButton hide;
    private FCLImageButton announcementHistory;
    private FCLImageButton friendsButton;
    private final List<Announcement> announcements = new ArrayList<>();
    private final Handler autoAdvanceHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoAdvanceRunnable = new Runnable() {
        @Override
        public void run() {
            if (announcements.size() > 1) {
                announcementPager.setCurrentItem((announcementPager.getCurrentItem() + 1) % announcements.size(), true);
            }
            autoAdvanceHandler.postDelayed(this, AUTO_ADVANCE_MS);
        }
    };

    private SkinViewer skinViewer;
    private SkinRenderer renderer;

    private ObjectProperty<Account> currentAccount;

    public MainUI(Context context, int id) {
        super(context, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        announcementContainer = findViewById(R.id.announcement_container);
        announcementLayout = findViewById(R.id.announcement_layout);
        title = findViewById(R.id.title);
        announcementPager = findViewById(R.id.announcement_pager);
        announcementDots = findViewById(R.id.announcement_dots);
        hide = findViewById(R.id.hide);
        announcementHistory = findViewById(R.id.announcement_history);
        friendsButton = findViewById(R.id.friends_button);
        // Nền thẻ tối #252525 (đồng bộ launcher PC — "news card" không phủ đặc
        // màu hồng theme) thay vì tint theo màu accent như trước.
        announcementLayout.getBackground().setTint(ContextCompat.getColor(getContext(), R.color.card_bg));
        hide.setOnClickListener(this);
        announcementHistory.setOnClickListener(this);
        friendsButton.setOnClickListener(this);

        skinViewer = findViewById(R.id.skin_viewer);
        renderer = new SkinRenderer(getContext());
        skinViewer.setRenderer(renderer, 5f);
        checkAnnouncement();
        setupSkinDisplay();

        // Vẽ skin phục hồi/tạm dừng theo lúc trang gắn/thu hồi (thay cho vòng đời onStart/onStop cũ)
        getContentView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                if (skinViewer != null) {
                    if (!ThemeEngine.getInstance().getTheme().isCloseSkinModel()) {
                        skinViewer.setVisibility(View.VISIBLE);
                        skinViewer.onResume();
                        renderer.updateTexture(renderer.getTexture()[0], renderer.getTexture()[1]);
                    } else {
                        skinViewer.onPause();
                        skinViewer.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                if (skinViewer != null) {
                    skinViewer.onPause();
                    skinViewer.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (skinViewer != null) {
            skinViewer.onPause();
            skinViewer.setVisibility(View.GONE);
        }
        autoAdvanceHandler.removeCallbacks(autoAdvanceRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skinViewer != null && isShowing() && !ThemeEngine.getInstance().getTheme().isCloseSkinModel()) {
            skinViewer.setVisibility(View.VISIBLE);
            skinViewer.onResume();
        }
        if (announcements.size() > 1) {
            autoAdvanceHandler.removeCallbacks(autoAdvanceRunnable);
            autoAdvanceHandler.postDelayed(autoAdvanceRunnable, AUTO_ADVANCE_MS);
        }
    }

    @Override
    public Task<?> refresh(Object... param) {
        return Task.runAsync(() -> {

        });
    }

    private void checkAnnouncement() {
        try {
            Task.supplyAsync(() -> HttpRequest.HttpGetRequest.GET(MainUI.ANNOUNCEMENT_LIST_URL).getJson(new TypeToken<ArrayList<Announcement>>() {
                    }))
                    .thenAcceptAsync(Schedulers.androidUIThread(), result -> {
                        announcements.clear();
                        for (Announcement a : result) {
                            if (a.shouldDisplay(getContext())) announcements.add(a);
                        }
                        if (announcements.isEmpty()) {
                            announcementContainer.setVisibility(View.GONE);
                            return;
                        }
                        announcementContainer.setVisibility(View.VISIBLE);
                        announcementPager.setAdapter(new AnnouncementPagerAdapter(announcements, 6000));
                        buildDots();
                        autoAdvanceHandler.removeCallbacks(autoAdvanceRunnable);
                        if (announcements.size() > 1) {
                            autoAdvanceHandler.postDelayed(autoAdvanceRunnable, AUTO_ADVANCE_MS);
                        }
                    }).start();
        } catch (Exception e) {
            Logging.LOG.log(Level.WARNING, "Failed to get announcement!", e);
        }
    }

    private void buildDots() {
        announcementDots.removeAllViews();
        announcementDots.setVisibility(announcements.size() > 1 ? View.VISIBLE : View.GONE);
        if (announcements.size() <= 1) return;

        View[] dots = new View[announcements.size()];
        for (int i = 0; i < announcements.size(); i++) {
            View dot = new View(getContext());
            int size = ConvertUtils.dip2px(getContext(), 6);
            LinearLayoutCompat.LayoutParams params = new LinearLayoutCompat.LayoutParams(size, size);
            params.setMargins(ConvertUtils.dip2px(getContext(), 3), 0, ConvertUtils.dip2px(getContext(), 3), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundColor(i == 0 ? 0xFFFFFFFF : 0x80FFFFFF);
            announcementDots.addView(dot);
            dots[i] = dot;
        }

        announcementPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dots.length; i++) {
                    dots[i].setBackgroundColor(i == position ? 0xFFFFFFFF : 0x80FFFFFF);
                }
            }
        });
    }

    /** Ẩn đúng tin ĐANG hiện trên trang (không phải ẩn cả thẻ) — khớp với việc
     *  thẻ giờ xoay vòng nhiều tin thay vì chỉ 1 tin mới nhất cố định. */
    private void hideCurrentAnnouncement() {
        int position = announcementPager.getCurrentItem();
        if (position < 0 || position >= announcements.size()) return;
        Announcement current = announcements.get(position);
        current.hide(getContext());
        announcements.remove(position);
        if (announcements.isEmpty()) {
            announcementContainer.setVisibility(View.GONE);
            autoAdvanceHandler.removeCallbacks(autoAdvanceRunnable);
            return;
        }
        announcementPager.setAdapter(new AnnouncementPagerAdapter(announcements, 6000));
        buildDots();
    }

    private void setupSkinDisplay() {
        currentAccount = new SimpleObjectProperty<>() {

            @Override
            protected void invalidated() {
                Account account = get();
                renderer.textureProperty().unbind();
                if (account == null) {
                    renderer.updateTexture(BitmapFactory.decodeStream(MainUI.class.getResourceAsStream("/assets/img/alex.png")), null);
                } else {
                    renderer.textureProperty().bind(TexturesLoader.textureBinding(account));
                }
            }
        };
        currentAccount.bind(Accounts.selectedAccountProperty());
    }

    public void refreshSkin(Account account) {
        Schedulers.androidUIThread().execute(() -> {
            if (currentAccount.get() == account) {
                renderer.textureProperty().unbind();
                renderer.textureProperty().bind(TexturesLoader.textureBinding(currentAccount.get()));
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view == hide) {
            int position = announcementPager.getCurrentItem();
            boolean significant = position >= 0 && position < announcements.size() && announcements.get(position).isSignificant();
            if (significant) {
                FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(getContext());
                builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT);
                builder.setCancelable(false);
                builder.setMessage(getContext().getString(R.string.announcement_significant));
                builder.setPositiveButton(this::hideCurrentAnnouncement);
                builder.setNegativeButton(null);
                builder.create().show();
            } else {
                hideCurrentAnnouncement();
            }
        }
        if (view == announcementHistory) {
            new AnnouncementHistoryDialog(getContext()).show();
        }
        if (view == friendsButton) {
            onFriendsButtonClick();
        }
    }

    // Đăng nhập/đăng ký Lylee (bạn bè/chat) giờ nằm trong Quản lý tài khoản
    // (tạo tài khoản ngoại tuyến có mật khẩu, hoặc liên kết Google theo từng
    // dòng) — nút này KHÔNG còn tự mở màn đăng nhập riêng nữa, chỉ kiểm tra đã
    // có phiên hợp lệ cho tài khoản Minecraft đang chọn chưa: có thì vào thẳng
    // danh sách bạn bè, chưa thì báo + đưa sang tab Tài khoản.
    private void onFriendsButtonClick() {
        Account account = Accounts.getSelectedAccount();
        String username = account != null ? account.getUsername() : null;
        if (username != null && LyleeFriendsSession.isValid(getContext(), username)) {
            getContext().startActivity(new Intent(getContext(), FriendsActivity.class));
            return;
        }
        FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(getContext());
        builder.setMessage(getContext().getString(R.string.friends_need_login_redirect));
        builder.setPositiveButton(() -> {
            MainActivity.getInstance().getUiManager().switchUI(MainActivity.getInstance().getUiManager().getAccountUI());
        });
        builder.setNegativeButton(null);
        builder.create().show();
    }
}
