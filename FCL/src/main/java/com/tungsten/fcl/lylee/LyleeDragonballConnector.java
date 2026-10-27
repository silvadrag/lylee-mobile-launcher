package com.tungsten.fcl.lylee;

import android.content.Context;

import androidx.appcompat.app.AppCompatDialog;

import com.tungsten.fcl.R;
import com.tungsten.fcl.setting.DownloadProviders;
import com.tungsten.fcl.setting.Profile;
import com.tungsten.fcl.ui.TaskDialog;
import com.tungsten.fcl.ui.download.version.VersionInstallInfoPage;
import com.tungsten.fcl.ui.version.Versions;
import com.tungsten.fcl.util.AndroidUtils;
import com.tungsten.fcl.util.TaskCancellationAction;
import com.tungsten.fclcore.download.DownloadProvider;
import com.tungsten.fclcore.download.GameBuilder;
import com.tungsten.fclcore.download.RemoteVersion;
import com.tungsten.fclcore.download.VersionList;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.task.TaskExecutor;
import com.tungsten.fclcore.task.TaskListener;
import com.tungsten.fclcore.util.platform.MemoryUtils;
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * "Lylee Dragonball" nối nhanh: tự tạo hoặc cập nhật version cố định {@link #VERSION_NAME}
 * trong profile hiện tại, chuẩn phiên bản Minecraft 1.21.4 và Fabric loader 0.19.3
 * (theo manifest server 4), đồng bộ file modpack (bao gồm mods, configs, resourcepacks, plugins).
 */
public final class LyleeDragonballConnector {

    public static final String VERSION_NAME = "LyleeDragonball";
    public static final int SERVER_PROFILE_ID = 4;

    private static final int RAM_WARNING_THRESHOLD_MB = 6144;

    private LyleeDragonballConnector() {
    }

    public static void connect(Context context, Profile profile) {
        int totalMemory = MemoryUtils.getTotalDeviceMemory(context);
        if (totalMemory < RAM_WARNING_THRESHOLD_MB) {
            FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(context);
            builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT);
            builder.setCancelable(false);
            builder.setMessage(AndroidUtils.getLocalizedText(context, "lylee_dragonball_ram_warning", totalMemory / 1024.0));
            builder.setPositiveButton(context.getString(R.string.lylee_dragonball_ram_warning_continue), () -> doConnect(context, profile));
            builder.setNegativeButton(null);
            builder.create().show();
            return;
        }
        doConnect(context, profile);
    }

    private static void doConnect(Context context, Profile profile) {
        boolean alreadyInstalled = profile.getRepository().hasVersion(VERSION_NAME);
        TaskDialog dialog = new TaskDialog(context, new TaskCancellationAction(AppCompatDialog::dismiss));
        dialog.setTitle(context.getString(alreadyInstalled
                ? R.string.lylee_dragonball_updating
                : R.string.lylee_dragonball_connecting));

        Task<?> task = LyleeDragonballSync.fetchManifest()
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
     * Version đã tồn tại thì chỉ đồng bộ file; chưa có thì tạo mới đúng minecraftVersion/loaderVersion
     * (mặc định 1.21.4 / fabric 0.19.3 theo yêu cầu server) rồi mới đồng bộ.
     */
    private static Task<?> prepareVersion(Profile profile, LyleeManifest manifest) throws IOException {
        if (profile.getRepository().hasVersion(VERSION_NAME)) {
            File runDir = profile.getRepository().getRunDirectory(VERSION_NAME);
            return LyleeDragonballSync.syncFiles(runDir, manifest);
        }

        String mcVersion = (manifest.minecraftVersion != null && !manifest.minecraftVersion.isEmpty())
                ? manifest.minecraftVersion : "1.21.4";
        String loaderType = (manifest.loaderType != null && !manifest.loaderType.isEmpty())
                ? manifest.loaderType.toLowerCase(Locale.ROOT) : "fabric";
        String loaderVersion = (manifest.loaderVersion != null && !manifest.loaderVersion.isEmpty())
                ? manifest.loaderVersion : "0.19.3";

        DownloadProvider provider = DownloadProviders.getDownloadProvider();
        VersionList<?> loaderList = provider.getVersionListById(loaderType);
        loaderList.loadAsync(mcVersion).join();
        RemoteVersion remoteVersion = loaderList.getVersion(mcVersion, loaderVersion)
                .orElseThrow(() -> new IOException(
                        "Không tìm thấy " + loaderType + " loader " + loaderVersion
                                + " cho Minecraft " + mcVersion + " (server đổi phiên bản?)."));

        GameBuilder builder = profile.getDependency(provider).gameBuilder();
        builder.name(VERSION_NAME);
        builder.gameVersion(mcVersion);
        builder.version(remoteVersion);

        return builder.buildAsync().thenComposeAsync(unused -> {
            File runDir = profile.getRepository().getRunDirectory(VERSION_NAME);
            new File(runDir, "mods").mkdirs();
            new File(runDir, "plugins").mkdirs();
            return LyleeDragonballSync.syncFiles(runDir, manifest);
        });
    }

    public static void showInfo(Context context, Profile profile) {
        boolean alreadyInstalled = profile.getRepository().hasVersion(VERSION_NAME);
        FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(context);
        builder.setAlertLevel(FCLAlertDialog.AlertLevel.INFO);
        builder.setMessage(context.getString(alreadyInstalled
                ? R.string.lylee_dragonball_info_installed
                : R.string.lylee_dragonball_info_new));
        builder.setNegativeButton(null);
        builder.create().show();
    }

    private static void offerLaunch(Context context, Profile profile) {
        FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(context);
        builder.setAlertLevel(FCLAlertDialog.AlertLevel.INFO);
        builder.setCancelable(false);
        builder.setMessage(context.getString(R.string.lylee_dragonball_ready));
        builder.setPositiveButton(context.getString(R.string.lylee_dragonball_launch), () -> Versions.launch(context, profile));
        builder.setNegativeButton(null);
        builder.create().show();
    }
}
