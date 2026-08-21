package com.tungsten.fcllibrary.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.Nullable;

import com.tungsten.fcl.R;
import com.tungsten.fcllibrary.browser.options.LibMode;
import com.tungsten.fcllibrary.browser.options.SelectionMode;
import com.tungsten.fcllibrary.component.ResultListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lớp cấu hình trình duyệt file
 * Hỗ trợ chọn đơn/chọn nhiều, lọc định dạng file, đặt thư mục bắt đầu
 */
public class FileBrowser implements Serializable {

    public static final String SELECTED_FILES = "SELECTED_FILES";

    /**
     * Lấy danh sách file đã chọn từ Intent
     */
    public static List<String> getSelectedFiles(Intent data) {
        if (data == null) {
            return null;
        }
        List<Uri> selectedFiles = data.getParcelableArrayListExtra(SELECTED_FILES);
        if (selectedFiles != null)
            return selectedFiles.stream().map(Uri::toString).collect(Collectors.toList());
        return Collections.emptyList();
    }

    private boolean externalSelection = true;
    private LibMode libMode = LibMode.FILE_BROWSER;
    private SelectionMode selectionMode = SelectionMode.SINGLE_SELECTION;
    private String initDir = Environment.getExternalStorageDirectory().getAbsolutePath();
    private List<String> suffix = new ArrayList<>();
    private String title;
    private int code;
    private transient Context context;
    private transient Callback callback;

    public FileBrowser(String title) {
        this.title = title;
    }

    public boolean isExternalSelection() {
        return externalSelection;
    }

    public LibMode getLibMode() {
        return libMode;
    }

    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    public String getInitDir() {
        return initDir;
    }

    public List<String> getSuffix() {
        return suffix;
    }

    public String getTitle() {
        return title;
    }

    public int getCode() {
        return code;
    }

    public Context getContext() {
        return context;
    }

    public Callback getCallback() {
        return callback;
    }

    /**
     * @deprecated Dùng FileBrowserLauncher thay thế
     */
    @Deprecated
    public void browse(Activity activity, int code, ResultListener.Listener listener) {
        Intent intent = new Intent(activity, FileBrowserActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("config", this);
        intent.putExtras(bundle);
        this.code = code;
        ResultListener.startActivityForResult(activity, intent, code, listener);
    }

    public void browse(Activity activity) {
        Intent intent = new Intent(activity, FileBrowserActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("config", this);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    /**
     * Interface callback - callback kết quả kiểu hiện đại
     */
    public interface Callback {
        void onResult(@Nullable List<String> files);
    }

    public static class Builder {

        private final FileBrowser fileBrowser;

        public Builder(Context context) {
            fileBrowser = new FileBrowser(context.getString(R.string.file_browser_title));
            fileBrowser.context = context;
        }

        public FileBrowser create() {
            return fileBrowser;
        }

        public Builder setExternalSelection(boolean externalSelection) {
            fileBrowser.externalSelection = externalSelection;
            return this;
        }

        public Builder setLibMode(LibMode libMode) {
            fileBrowser.libMode = libMode;
            return this;
        }

        public Builder setSelectionMode(SelectionMode selectionMode) {
            fileBrowser.selectionMode = selectionMode;
            return this;
        }

        public Builder setInitDir(String initDir) {
            if (initDir != null)
                fileBrowser.initDir = initDir;
            return this;
        }

        public Builder setSuffix(List<String> suffix) {
            if (suffix != null)
                fileBrowser.suffix = suffix;
            return this;
        }

        public Builder setTitle(String title) {
            fileBrowser.title = title;
            return this;
        }

        /**
         * Đặt callback kết quả (cách hiện đại)
         */
        public Builder setCallback(Callback callback) {
            fileBrowser.callback = callback;
            return this;
        }

        public boolean isExternalSelection() {
            return fileBrowser.externalSelection;
        }

        public LibMode getLibMode() {
            return fileBrowser.libMode;
        }

        public SelectionMode getSelectionMode() {
            return fileBrowser.selectionMode;
        }

        public String getInitDir() {
            return fileBrowser.initDir;
        }

        public List<String> getSuffix() {
            return fileBrowser.suffix;
        }

        public String getTitle() {
            return fileBrowser.title;
        }

    }

}
