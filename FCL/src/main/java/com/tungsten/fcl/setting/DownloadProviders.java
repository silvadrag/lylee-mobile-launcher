/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2020  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tungsten.fcl.setting;

import static com.tungsten.fcl.setting.ConfigHolder.config;
import static com.tungsten.fclcore.task.FetchTask.DEFAULT_CONCURRENCY;
import static com.tungsten.fclcore.util.Lang.mapOf;
import static com.tungsten.fclcore.util.Pair.pair;

import android.content.Context;

import com.tungsten.fcl.R;
import com.tungsten.fcl.util.AndroidUtils;
import com.tungsten.fcl.util.FXUtils;
import com.tungsten.fclcore.download.AdaptedDownloadProvider;
import com.tungsten.fclcore.download.ArtifactMalformedException;
import com.tungsten.fclcore.download.AutoDownloadProvider;
import com.tungsten.fclcore.download.BMCLAPIDownloadProvider;
import com.tungsten.fclcore.download.BalancedDownloadProvider;
import com.tungsten.fclcore.download.DownloadProvider;
import com.tungsten.fclcore.download.MojangDownloadProvider;
import com.tungsten.fclcore.fakefx.beans.InvalidationListener;
import com.tungsten.fclcore.task.DownloadException;
import com.tungsten.fclcore.task.FetchTask;
import com.tungsten.fclcore.util.StringUtils;
import com.tungsten.fclcore.util.io.ResponseCodeException;

import javax.net.ssl.SSLHandshakeException;
import java.io.FileNotFoundException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DownloadProviders {
    private DownloadProviders() {}

    private static DownloadProvider currentDownloadProvider;

    public static final Map<String, DownloadProvider> providersById;
    public static final Map<String, DownloadProvider> rawProviders;
    private static final AdaptedDownloadProvider fileDownloadProvider = new AdaptedDownloadProvider();

    private static final MojangDownloadProvider MOJANG;
    private static final BMCLAPIDownloadProvider BMCLAPI;

    public static final String DEFAULT_PROVIDER_ID = "balanced";
    public static final String DEFAULT_RAW_PROVIDER_ID = "bmclapi";

    private static final InvalidationListener observer;

    static {
        String bmclapiRoot = "https://bmclapi2.bangbang93.com";

        MOJANG = new MojangDownloadProvider();
        BMCLAPI = new BMCLAPIDownloadProvider(bmclapiRoot);
        rawProviders = mapOf(
                pair("mojang", MOJANG),
                pair("bmclapi", BMCLAPI)
        );

        AdaptedDownloadProvider fileProvider = new AdaptedDownloadProvider();
        fileProvider.setDownloadProviderCandidates(Arrays.asList(BMCLAPI, MOJANG));
        BalancedDownloadProvider balanced = new BalancedDownloadProvider(MOJANG, BMCLAPI);

        providersById = mapOf(
                pair("official", new AutoDownloadProvider(MOJANG, fileProvider)),
                pair("balanced", new AutoDownloadProvider(balanced, fileProvider)),
                pair("mirror", new AutoDownloadProvider(BMCLAPI, fileProvider)));

        observer = FXUtils.observeWeak(() -> {
            FetchTask.setDownloadExecutorConcurrency(
                    config().getAutoDownloadThreads() ? DEFAULT_CONCURRENCY : config().getDownloadThreads());
        }, config().autoDownloadThreadsProperty(), config().downloadThreadsProperty());
    }

    static void init() {
        FXUtils.onChangeAndOperate(config().versionListSourceProperty(), versionListSource -> {
            if (!providersById.containsKey(versionListSource)) {
                config().setVersionListSource(DEFAULT_PROVIDER_ID);
                return;
            }

            currentDownloadProvider = Optional.ofNullable(providersById.get(versionListSource))
                    .orElse(providersById.get(DEFAULT_PROVIDER_ID));
        });

        if (!rawProviders.containsKey(config().getDownloadType())) {
            config().setDownloadType(DEFAULT_RAW_PROVIDER_ID);
        }

        FXUtils.onChangeAndOperate(config().downloadTypeProperty(), downloadType -> {
            DownloadProvider primary = Optional.ofNullable(rawProviders.get(downloadType))
                    .orElse(rawProviders.get(DEFAULT_RAW_PROVIDER_ID));
            fileDownloadProvider.setDownloadProviderCandidates(
                    Stream.concat(
                            Stream.of(primary),
                            rawProviders.values().stream().filter(x -> x != primary)
                    ).collect(Collectors.toList())
            );
        });
    }

    public static String getPrimaryDownloadProviderId() {
        String downloadType = config().getDownloadType();
        if (providersById.containsKey(downloadType))
            return downloadType;
        else
            return DEFAULT_PROVIDER_ID;
    }

    public static DownloadProvider getDownloadProviderByPrimaryId(String primaryId) {
        return Optional.ofNullable(providersById.get(primaryId))
                .orElse(providersById.get(DEFAULT_PROVIDER_ID));
    }

    /**
     * Get current primary preferred download provider
     */
    public static DownloadProvider getDownloadProvider() {
        return config().isAutoChooseDownloadType() ? currentDownloadProvider : fileDownloadProvider;
    }

    private static final java.util.regex.Pattern IP_PATTERN =
            java.util.regex.Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d+)?\\b");

    public static String sanitizeErrorMessage(String text) {
        if (text == null) return "";
        return IP_PATTERN.matcher(text).replaceAll("[server]");
    }

    public static String localizeErrorMessage(Context context, Throwable exception) {
        String result;
        if (exception instanceof DownloadException) {
            URL rawUrl = ((DownloadException) exception).getUrl();
            String url = rawUrl != null ? sanitizeErrorMessage(rawUrl.toString()) : "";
            if (exception.getCause() instanceof SocketTimeoutException) {
                result = AndroidUtils.getLocalizedText(context, "install_failed_downloading_timeout", url);
            } else if (exception.getCause() instanceof ResponseCodeException) {
                ResponseCodeException responseCodeException = (ResponseCodeException) exception.getCause();
                if (AndroidUtils.hasStringId(context, "download_code_" + responseCodeException.getResponseCode())) {
                    result = AndroidUtils.getLocalizedText(context, "download_code_" + responseCodeException.getResponseCode(), url);
                } else {
                    result = AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", url) + "\n" + StringUtils.getStackTrace(exception.getCause());
                }
            } else if (exception.getCause() instanceof FileNotFoundException) {
                result = AndroidUtils.getLocalizedText(context, "download_code_404", url);
            } else if (exception.getCause() instanceof AccessDeniedException) {
                result = AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", url) + "\n" + AndroidUtils.getLocalizedText(context, "exception_access_denied", ((AccessDeniedException) exception.getCause()).getFile());
            } else if (exception.getCause() instanceof ArtifactMalformedException) {
                result = AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", url) + "\n" + context.getString(R.string.exception_artifact_malformed);
            } else if (exception.getCause() instanceof SSLHandshakeException) {
                result = AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", url) + "\n" + context.getString(R.string.exception_ssl_handshake);
            } else {
                result = AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", url) + "\n" + StringUtils.getStackTrace(exception.getCause());
            }
        } else if (exception instanceof ArtifactMalformedException) {
            result = context.getString(R.string.exception_artifact_malformed);
        } else if (exception instanceof CancellationException) {
            result = context.getString(R.string.message_cancelled);
        } else {
            result = StringUtils.getStackTrace(exception);
        }
        return sanitizeErrorMessage(result);
    }
}
