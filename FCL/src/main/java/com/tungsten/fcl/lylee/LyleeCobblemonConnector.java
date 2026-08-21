package com.tungsten.fcl.lylee;

import android.content.Context;

import androidx.appcompat.app.AppCompatDialog;

import com.tungsten.fcl.R;
import com.tungsten.fcl.setting.DownloadProviders;
import com.tungsten.fcl.setting.Profile;
import com.tungsten.fcl.ui.TaskDialog;
import com.tungsten.fcl.ui.download.version.VersionInstallInfoPage;
import com.tungsten.fcl.ui.version.Versions;
import com.tungsten.fcl.util.TaskCancellationAction;
import com.tungsten.fclcore.download.DownloadProvider;
import com.tungsten.fclcore.download.GameBuilder;
import com.tungsten.fclcore.download.RemoteVersion;
import com.tungsten.fclcore.download.VersionList;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.task.TaskExecutor;
import com.tungsten.fclcore.task.TaskListener;
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog;

import java.io.File;
import java.io.IOException;

/**
 * "Lylee Cobblemon" nối nhanh: bấm 1 nút trên màn hình chính là tự tạo (nếu
 * chưa có) hoặc cập nhật version tên cố định {@link #VERSION_NAME} trong
 * profile hiện tại, đúng minecraftVersion/loaderVersion server thật yêu cầu
 * (qua manifest — xem {@link LyleeCobblemonSync}), rồi đồng bộ file modpack.
 *
 * KHÔNG đụng tới các version/profile tự do khác của người chơi — tách biệt
 * theo tên version, giống mô hình 2 tab "Lylee Cobblemon" + "Instances tự do"
 * bên launcher PC, chỉ khác là bên mobile chưa có tab riêng (xem docs/PLAN.md
 * mục việc-cần-làm — dùng tạm 1 nút trên MainUI cho tới đợt thiết kế lại UI).
 */
public final class LyleeCobblemonConnector {

    public static final String VERSION_NAME = "LyleeCobblemon";

    private LyleeCobblemonConnector() {
    }

    public static void connect(Context context, Profile profile) {
        TaskDialog dialog = new TaskDialog(context, new TaskCancellationAction(AppCompatDialog::dismiss));
        dialog.setTitle(context.getString(R.string.lylee_cobblemon_connecting));

        Task<?> task = LyleeCobblemonSync.fetchManifest()
                .thenComposeAsync(manifest -> prepareVersion(profile, manifest));

        Schedulers.androidUIThread().execute(() -> {
            TaskExecutor executor = task.executor(new TaskListener() {
                @Override
                public void onStop(boolean success, TaskExecutor executor) {
                    Schedulers.androidUIThread().execute(() -> {
                        if (success) {
                            profile.getRepository().refreshVersions();
                            profile.setSelectedVersion(VERSION_NAME);
                            offerLaunch(context, profile);
                        } else {
                            if (executor.getException() == null) return;
                            VersionInstallInfoPage.alertFailureMessage(context, executor.getException(), () -> {
                            });
                        }
                    });
                }
            });
            dialog.setExecutor(executor);
            dialog.show();
            executor.start();
        });
    }

    /**
     * Version đã tồn tại (lần nối trước, hoặc người chơi tự tạo trùng tên) thì
     * chỉ đồng bộ file; chưa có thì tạo mới đúng minecraftVersion/loaderVersion
     * manifest ghi rồi mới đồng bộ.
     */
    private static Task<?> prepareVersion(Profile profile, LyleeManifest manifest) throws IOException {
        if (profile.getRepository().hasVersion(VERSION_NAME)) {
            File runDir = profile.getRepository().getRunDirectory(VERSION_NAME);
            return LyleeCobblemonSync.syncFiles(runDir, manifest);
        }

        DownloadProvider provider = DownloadProviders.getDownloadProvider();
        VersionList<?> fabricList = provider.getVersionListById("fabric");
        fabricList.loadAsync(manifest.minecraftVersion).join();
        RemoteVersion fabricVersion = fabricList.getVersion(manifest.minecraftVersion, manifest.loaderVersion)
                .orElseThrow(() -> new IOException(
                        "Không tìm thấy Fabric loader " + manifest.loaderVersion
                                + " cho Minecraft " + manifest.minecraftVersion + " (server đổi phiên bản?)."));

        GameBuilder builder = profile.getDependency(provider).gameBuilder();
        builder.name(VERSION_NAME);
        builder.gameVersion(manifest.minecraftVersion);
        builder.version(fabricVersion);

        return builder.buildAsync().thenComposeAsync(unused -> {
            File runDir = profile.getRepository().getRunDirectory(VERSION_NAME);
            new File(runDir, "mods").mkdirs();
            return LyleeCobblemonSync.syncFiles(runDir, manifest);
        });
    }

    private static void offerLaunch(Context context, Profile profile) {
        FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(context);
        builder.setAlertLevel(FCLAlertDialog.AlertLevel.INFO);
        builder.setCancelable(false);
        builder.setMessage(context.getString(R.string.lylee_cobblemon_ready));
        builder.setPositiveButton(context.getString(R.string.lylee_cobblemon_launch), () -> Versions.launch(context, profile));
        builder.setNegativeButton(null);
        builder.create().show();
    }
}
