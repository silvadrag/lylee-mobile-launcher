package com.tungsten.fcl.ui.main;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;

import com.tungsten.fcl.R;
import com.tungsten.fcl.game.TexturesLoader;
import com.tungsten.fcl.setting.Accounts;
import com.tungsten.fcl.util.AndroidUtils;
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
import com.tungsten.fcllibrary.component.view.FCLTextView;
import com.tungsten.fcllibrary.skin.SkinRenderer;
import com.tungsten.fcllibrary.skin.SkinViewer;
import com.tungsten.fcllibrary.util.LocaleUtils;

import java.util.logging.Level;

public class MainUI extends FCLCommonUI implements View.OnClickListener {

    // Endpoint thật — GET /api/mobile/announcement, xem ApiServer.getMobileAnnouncement
    // bên fabric-lyleelauncherAPI-mod-1.21.1 (trả 404 nếu chưa có thông báo nào
    // đang hiển thị, request tự fail êm, không hiện gì — catch Exception bên dưới).
    // Cố tình KHÔNG còn trỏ về repo GitHub thật của FCL-Team nữa (tránh hiện thông
    // báo/liên kết của đội FCL gốc bên trong app đã đổi thương hiệu Lylee).
    public static final String ANNOUNCEMENT_URL = "https://lylee-launcher-api.lyleelauncher.workers.dev/api/mobile/announcement";
    public static final String ANNOUNCEMENT_URL_CN = ANNOUNCEMENT_URL;

    private LinearLayoutCompat announcementContainer;
    private LinearLayoutCompat announcementLayout;
    private FCLTextView title;
    private FCLTextView announcementView;
    private FCLTextView date;
    private FCLButton hide;
    private Announcement announcement = null;

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
        announcementView = findViewById(R.id.announcement);
        date = findViewById(R.id.date);
        hide = findViewById(R.id.hide);
        // Nền thẻ tối #252525 (đồng bộ launcher PC — "news card" không phủ đặc
        // màu hồng theme) thay vì tint theo màu accent như trước.
        announcementLayout.getBackground().setTint(ContextCompat.getColor(getContext(), R.color.card_bg));
        hide.setOnClickListener(this);

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
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skinViewer != null && isShowing() && !ThemeEngine.getInstance().getTheme().isCloseSkinModel()) {
            skinViewer.setVisibility(View.VISIBLE);
            skinViewer.onResume();
        }
    }

    @Override
    public Task<?> refresh(Object... param) {
        return Task.runAsync(() -> {

        });
    }

    private void checkAnnouncement() {
        try {
            String url = LocaleUtils.isChinese(getContext()) ? ANNOUNCEMENT_URL_CN : ANNOUNCEMENT_URL;
            Task.supplyAsync(() -> HttpRequest.HttpGetRequest.GET(url).getJson(Announcement.class))
                    .thenAcceptAsync(Schedulers.androidUIThread(), announcement -> {
                        this.announcement = announcement;
                        if (!announcement.shouldDisplay(getContext()))
                            return;
                        announcementContainer.setVisibility(View.VISIBLE);
                        title.setText(AndroidUtils.getLocalizedText(getContext(), "announcement", announcement.getDisplayTitle(getContext())));
                        announcementView.setText(announcement.getDisplayContent(getContext()));
                        date.setText(AndroidUtils.getLocalizedText(getContext(), "update_date", announcement.getDate()));
                    }).start();
        } catch (Exception e) {
            Logging.LOG.log(Level.WARNING, "Failed to get announcement!", e);
        }
    }

    private void hideAnnouncement() {
        announcementContainer.setVisibility(View.GONE);
        if (announcement != null) {
            announcement.hide(getContext());
        }
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
            if (announcement != null && announcement.isSignificant()) {
                FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(getContext());
                builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT);
                builder.setCancelable(false);
                builder.setMessage(getContext().getString(R.string.announcement_significant));
                builder.setPositiveButton(this::hideAnnouncement);
                builder.setNegativeButton(null);
                builder.create().show();
            } else {
                hideAnnouncement();
            }
        }
    }
}
