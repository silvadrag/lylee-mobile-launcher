package com.tungsten.fcl.ui.download.common;

import static com.tungsten.fcl.ui.download.DownloadUI.PAGE_ID_DOWNLOAD_MOD;
import static com.tungsten.fcl.ui.download.DownloadUI.PAGE_ID_DOWNLOAD_MODPACK;
import static com.tungsten.fcl.ui.download.DownloadUI.PAGE_ID_DOWNLOAD_RESOURCE_PACK;
import static com.tungsten.fcl.ui.download.DownloadUI.PAGE_ID_DOWNLOAD_SHADER_PACK;
import static com.tungsten.fcl.ui.download.DownloadUI.PAGE_ID_DOWNLOAD_WORLD;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tungsten.fcl.R;
import com.tungsten.fcl.databinding.PageDownloadBinding;
import com.tungsten.fcl.game.LocalizedRemoteModRepository;
import com.tungsten.fcl.setting.DownloadProviders;
import com.tungsten.fcl.setting.Profile;
import com.tungsten.fcl.setting.Profiles;
import com.tungsten.fcl.ui.TaskDialog;
import com.tungsten.fcl.ui.UIManager;
import com.tungsten.fcl.ui.download.TranslationDialog;
import com.tungsten.fcl.ui.manage.ManageUI;
import com.tungsten.fcl.ui.version.Versions;
import com.tungsten.fcl.util.AndroidUtils;
import com.tungsten.fcl.util.FXUtils;
import com.tungsten.fcl.util.TaskCancellationAction;
import com.tungsten.fclcore.download.DownloadProvider;
import com.tungsten.fclcore.fakefx.beans.InvalidationListener;
import com.tungsten.fclcore.fakefx.beans.property.BooleanProperty;
import com.tungsten.fclcore.fakefx.beans.property.IntegerProperty;
import com.tungsten.fclcore.fakefx.beans.property.ListProperty;
import com.tungsten.fclcore.fakefx.beans.property.ObjectProperty;
import com.tungsten.fclcore.fakefx.beans.property.SimpleBooleanProperty;
import com.tungsten.fclcore.fakefx.beans.property.SimpleIntegerProperty;
import com.tungsten.fclcore.fakefx.beans.property.SimpleListProperty;
import com.tungsten.fclcore.fakefx.beans.property.SimpleObjectProperty;
import com.tungsten.fclcore.fakefx.beans.property.SimpleStringProperty;
import com.tungsten.fclcore.fakefx.beans.property.StringProperty;
import com.tungsten.fclcore.fakefx.collections.FXCollections;
import com.tungsten.fclcore.mod.ModLoaderType;
import com.tungsten.fclcore.mod.ModManager;
import com.tungsten.fclcore.mod.RemoteMod;
import com.tungsten.fclcore.mod.RemoteModRepository;
import com.tungsten.fclcore.mod.curse.CurseAddon;
import com.tungsten.fclcore.mod.curse.CurseForgeRemoteModRepository;
import com.tungsten.fclcore.mod.modrinth.ModrinthRemoteModRepository;
import com.tungsten.fclcore.task.FileDownloadTask;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.task.TaskExecutor;
import com.tungsten.fclcore.util.Lang;
import com.tungsten.fclcore.util.StringUtils;
import com.tungsten.fclcore.util.io.NetworkUtils;
import com.tungsten.fcllibrary.component.dialog.EditDialog;
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog;
import com.tungsten.fcllibrary.component.theme.ThemeEngine;
import com.tungsten.fcllibrary.component.ui.FCLPage;
import com.tungsten.fcllibrary.component.view.FCLButton;
import com.tungsten.fcllibrary.component.view.FCLEditText;
import com.tungsten.fcllibrary.component.view.FCLImageButton;
import com.tungsten.fcllibrary.component.view.FCLProgressBar;
import com.tungsten.fcllibrary.component.view.FCLSpinner;
import com.tungsten.fcllibrary.component.view.FCLTextView;
import com.tungsten.fcllibrary.util.LocaleUtils;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

import kotlin.Unit;

/**
 * Trang tải: 5 chế độ tải (Mod/Modpack/Resource Pack/World/Shader) dùng chung 1 instance trang,
 * chuyển repository, callback tải và control riêng qua {@link #switchType(int)},
 * trạng thái tìm kiếm của mỗi chế độ do ViewModel lưu theo id trang, chuyển về là khôi phục ngay.
 */
public class DownloadPage extends FCLPage implements ManageUI.VersionLoadable, View.OnClickListener {

    private int pageId = PAGE_ID_DOWNLOAD_MOD;
    protected RemoteModRepository repository;
    private RemoteModVersionPage.DownloadCallback callback;
    private final IntegerProperty pageOffset = new SimpleIntegerProperty(0);
    private final IntegerProperty pageCount = new SimpleIntegerProperty(-1);
    protected final BooleanProperty supportChinese = new SimpleBooleanProperty();
    private final ObjectProperty<Profile.ProfileVersion> version = new SimpleObjectProperty<>();
    protected final ListProperty<String> downloadSources = new SimpleListProperty<>(this, "downloadSources", FXCollections.observableArrayList());
    protected final StringProperty downloadSource = new SimpleStringProperty();
    private final StringProperty gameVersion = new SimpleStringProperty(this, "gameVersion", "");
    private final ObjectProperty<CategoryIndented> category = new SimpleObjectProperty<>(this, "category", new CategoryIndented(0, null));
    private final ObjectProperty<RemoteModRepository.SortType> sortType = new SimpleObjectProperty<>(this, "sortType", RemoteModRepository.SortType.POPULARITY);
    private TaskExecutor executor;
    private Runnable retrySearch;
    private RemoteModListAdapter adapter;

    private ScrollView searchLayout;

    private FCLEditText nameEditText;
    private FCLTextView sourceText;
    private FCLSpinner<String> sourceSpinner;
    private FCLSpinner<String> gameVersionSpinner;
    private FCLSpinner<CategoryIndented> categorySpinner;
    private FCLSpinner<RemoteModRepository.SortType> sortSpinner;
    private final ArrayList<String> versionList = new ArrayList<>();

    private FCLButton search;
    private FCLButton installModpack;
    private FCLButton translate;
    private LinearLayoutCompat listLayout;
    private FCLTextView page;
    private FCLButton next;
    private FCLButton previous;
    private FCLButton first;
    private FCLButton last;
    private RecyclerView recyclerView;
    private FCLProgressBar progressBar;
    private FCLImageButton retry;

    protected PageDownloadBinding binding;
    protected ModLoaderType selectedModLoader;
    private ModManager modManager;
    private final DownloadProvider downloadProvider;
    /**
     * Trạng thái tìm kiếm (gắn ViewModel của Activity, khôi phục sau khi đổi chế độ và trang tạo lại)
     */
    protected DownloadSearchViewModel.State searchState;
    /**
     * Làm mới danh mục và tìm lại khi nguồn tải đổi (kích hoạt khi người dùng tự chuyển nguồn)
     */
    private final InvalidationListener sourceListener = observable -> refreshCategory(true);

    public DownloadPage(Context context, int resId) {
        super(context, FCLPage.PAGE_ID_TEMP, resId);
        this.downloadProvider = DownloadProviders.getDownloadProvider();
        create();
    }

    public int getPageId() {
        return pageId;
    }

    public ModManager getModManager() {
        return modManager;
    }

    /**
     * Chuyển sang chế độ tải chỉ định: cập nhật nguồn dữ liệu, callback tải và control riêng,
     * khôi phục trạng thái tìm kiếm của chế độ đó từ ViewModel (đã có kết quả thì không tìm lại).
     */
    public void switchType(int pageId) {
        this.pageId = pageId;
        // Lấy trạng thái tìm kiếm theo chế độ (mỗi chế độ độc lập, tránh khôi phục/ghi nhầm sang chế độ khác)
        searchState = new ViewModelProvider(getActivity()).get(DownloadSearchViewModel.class).getState(pageId);

        // Nguồn dữ liệu
        switch (pageId) {
            case PAGE_ID_DOWNLOAD_MODPACK:
                repository = new LocalizedRepository(ModrinthRemoteModRepository.MODPACKS, CurseForgeRemoteModRepository.MODPACKS, RemoteModRepository.Type.MODPACK);
                break;
            case PAGE_ID_DOWNLOAD_MOD:
                repository = new LocalizedRepository(ModrinthRemoteModRepository.MODS, CurseForgeRemoteModRepository.MODS, RemoteModRepository.Type.MOD);
                break;
            case PAGE_ID_DOWNLOAD_RESOURCE_PACK:
                repository = new LocalizedRepository(ModrinthRemoteModRepository.RESOURCE_PACKS, CurseForgeRemoteModRepository.RESOURCE_PACKS, RemoteModRepository.Type.MOD);
                break;
            case PAGE_ID_DOWNLOAD_SHADER_PACK:
                repository = new LocalizedRepository(ModrinthRemoteModRepository.SHADER_PACKS, CurseForgeRemoteModRepository.SHADER_PACKS, RemoteModRepository.Type.MOD);
                break;
            default:
                repository = CurseForgeRemoteModRepository.WORLDS;
                break;
        }

        // Callback tải (thư mục cài đặt tùy theo chế độ)
        switch (pageId) {
            case PAGE_ID_DOWNLOAD_MODPACK:
                callback = (profile, version, file) -> Versions.downloadModpackImpl(getContext(), profile, file);
                break;
            case PAGE_ID_DOWNLOAD_MOD:
                callback = (profile, version, file) -> download(getContext(), profile, version, file, "mods");
                break;
            case PAGE_ID_DOWNLOAD_RESOURCE_PACK:
                callback = (profile, version, file) -> download(getContext(), profile, version, file, "resourcepacks");
                break;
            case PAGE_ID_DOWNLOAD_SHADER_PACK:
                callback = (profile, version, file) -> download(getContext(), profile, version, file, "shaderpacks");
                break;
            default:
                callback = null;
                break;
        }

        // Nguồn tải (chế độ World cố định CurseForge, không có Modrinth).
        // Tạm gỡ listener lúc khôi phục, tránh downloadSource đổi kích hoạt refreshCategory tìm lặp
        downloadSource.removeListener(sourceListener);
        boolean localized = pageId != PAGE_ID_DOWNLOAD_WORLD;
        if (localized) {
            downloadSources.get().setAll(getContext().getString(R.string.mods_curseforge), getContext().getString(R.string.mods_modrinth));
            downloadSource.set(getContext().getString(R.string.mods_modrinth));
        } else {
            downloadSources.clear();
            downloadSource.set(getContext().getString(R.string.mods_curseforge));
        }
        initSourceSpinner();
        if (searchState.source != null) {
            downloadSource.set(searchState.source);
        }
        downloadSource.addListener(sourceListener);

        // Ẩn/hiện control riêng
        boolean mod = pageId == PAGE_ID_DOWNLOAD_MOD;
        binding.modloader.setVisibility(mod ? View.VISIBLE : View.GONE);
        binding.modloaderText.setVisibility(mod ? View.VISIBLE : View.GONE);
        installModpack.setVisibility(pageId == PAGE_ID_DOWNLOAD_MODPACK ? View.VISIBLE : View.GONE);
        supportChinese.set(mod || pageId == PAGE_ID_DOWNLOAD_MODPACK);
        boolean chinese = LocaleUtils.isChinese(getContext());
        translate.setVisibility((mod || pageId == PAGE_ID_DOWNLOAD_MODPACK) && chinese ? View.VISIBLE : View.GONE);
        nameEditText.setHint(supportChinese.get() ? getContext().getString(R.string.search_hint_chinese) : getContext().getString(R.string.search_hint_english));
        if (mod) {
            binding.modloader.setSelection(searchState.modLoaderPosition);
        }

        // Khôi phục điều kiện tìm kiếm của chế độ (ô tìm/version game/sắp xếp; danh mục khôi phục sau khi list danh mục sẵn sàng)
        nameEditText.setText(searchState.searchFilter);
        int versionIndex = versionList.indexOf(searchState.userGameVersion);
        gameVersionSpinner.setSelection(Math.max(versionIndex, 0));
        sortSpinner.setSelection(searchState.sortType.ordinal());

        // Làm mới danh mục và khôi phục trạng thái tìm kiếm (có kết quả thì khôi phục luôn, không tìm lại)
        refreshCategory(false);
        if (searchState.result != null) {
            restoreResult();
        } else {
            search(searchState.userGameVersion, searchState.category, searchState.pageOffset, searchState.searchFilter, searchState.sortType);
        }
    }

    /**
     * Khởi tạo/làm mới spinner nguồn tải (dữ liệu và ẩn/hiện đổi theo chế độ)
     */
    private void initSourceSpinner() {
        sourceText.setVisibility(downloadSources.getSize() > 1 ? View.VISIBLE : View.GONE);
        sourceSpinner.setVisibility(downloadSources.getSize() > 1 ? View.VISIBLE : View.GONE);
        if (downloadSources.getSize() > 1) {
            sourceSpinner.setDataList(new ArrayList<>(downloadSources));
            ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(getContext(), R.layout.item_spinner_auto_tint, new ArrayList<>(downloadSources));
            sourceAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            sourceSpinner.setAdapter(sourceAdapter);
            sourceSpinner.setSelection(downloadSource.get().equals(getContext().getString(R.string.mods_modrinth)) ? 1 : 0);
            FXUtils.bindSelection(sourceSpinner, downloadSource);
        }
    }

    /**
     * Repository nội bộ hóa (2 nguồn Modrinth/CurseForge, repository và loại theo chế độ)
     */
    private class LocalizedRepository extends LocalizedRemoteModRepository {
        private final RemoteModRepository modrinthRepository;
        private final RemoteModRepository curseRepository;
        private final Type type;

        LocalizedRepository(RemoteModRepository modrinthRepository, RemoteModRepository curseRepository, Type type) {
            this.modrinthRepository = modrinthRepository;
            this.curseRepository = curseRepository;
            this.type = type;
        }

        @Override
        protected RemoteModRepository getBackedRemoteModRepository() {
            if (getContext().getString(R.string.mods_modrinth).equals(downloadSource.get())) {
                return modrinthRepository;
            } else {
                return curseRepository;
            }
        }

        @Override
        protected SortType getBackedRemoteModRepositorySortOrder() {
            if (getContext().getString(R.string.mods_modrinth).equals(downloadSource.get())) {
                return SortType.NAME;
            } else {
                return SortType.POPULARITY;
            }
        }

        @Override
        public Type getType() {
            return type;
        }
    }

    public void setLoading(boolean loading) {
        Schedulers.androidUIThread().execute(() -> {
            search.setEnabled(!loading);
            nameEditText.setEnabled(!loading);
            sourceSpinner.setEnabled(!loading);
            gameVersionSpinner.setEnabled(!loading);
            categorySpinner.setEnabled(!loading);
            sortSpinner.setEnabled(!loading);
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            listLayout.setVisibility(loading ? View.GONE : View.VISIBLE);
            recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
            if (loading) {
                retry.setVisibility(View.GONE);
            }
        });
    }

    public void setFailed() {
        Schedulers.androidUIThread().execute(() -> {
            retry.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            listLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        });
    }

    public void search() {
        search(gameVersion.get(),
                category.get().category(),
                pageOffset.get(),
                Objects.requireNonNull(nameEditText.getText()).toString(),
                sortType.get());
    }

    public void search(String userGameVersion, RemoteModRepository.Category category, int pageOffset, String searchFilter, RemoteModRepository.SortType sort) {
        retrySearch = null;
        setLoading(true);
        if (executor != null && !executor.isCancelled()) {
            executor.cancel();
        }
        // Lưu điều kiện tìm kiếm, dùng để khôi phục sau khi đổi chế độ
        searchState.userGameVersion = userGameVersion;
        searchState.category = category;
        searchState.pageOffset = pageOffset;
        searchState.searchFilter = searchFilter;
        searchState.sortType = sort;
        searchState.source = downloadSource.get();
        int searchPageId = pageId;
        executor = Task.supplyAsync(() -> {
                    RemoteModRepository.SearchResult result = repository.search(downloadProvider, userGameVersion, category, pageOffset, 30, searchFilter, sort, RemoteModRepository.SortOrder.DESC);
                    ArrayList<RemoteMod> list = (ArrayList<RemoteMod>) result.getResults().collect(Collectors.toList());
                    if (pageId == PAGE_ID_DOWNLOAD_MOD && selectedModLoader != null) {
                        list = (ArrayList<RemoteMod>) list.parallelStream().filter(mod -> {
                            try {
                                return mod.getData().loadVersions(repository).flatMap(v -> v.getLoaders().stream()).collect(Collectors.toCollection(ArrayList::new)).contains(selectedModLoader);
                            } catch (Throwable ignore) {
                            }
                            return true;
                        }).collect(Collectors.toList());
                    }
                    pageCount.set(result.getTotalPages());
                    return list;
                })
                .whenComplete(Schedulers.androidUIThread(), (list, exception) -> {
                    // Đã đổi chế độ thì bỏ qua callback quá hạn, tránh kết quả chế độ cũ đè lên trang hiện tại
                    if (searchPageId != pageId) {
                        return;
                    }
                    if (exception instanceof CancellationException) {
                        // Task bị hủy (tìm lại/đổi chế độ đã phát task mới): không đổi trạng thái giao diện
                        return;
                    }
                    setLoading(false);
                    if (exception == null) {
                        // Lưu kết quả tìm kiếm và adapter, chuyển về chế độ đó là hiện lại ngay
                        searchState.result = list;
                        searchState.pageCount = pageCount.get();
                        adapter = createAdapter(list);
                        searchState.adapter = adapter;
                        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                        recyclerView.setAdapter(adapter);
                    } else {
                        setFailed();
                        pageCount.set(-1);
                        searchState.result = null;
                        searchState.pageCount = -1;
                        searchState.adapter = null;
                        retrySearch = () -> search(userGameVersion, category, pageOffset, searchFilter, sort);
                    }
                }).executor(true);
    }

    private RemoteModListAdapter createAdapter(ArrayList<RemoteMod> list) {
        return new RemoteModListAdapter(getContext(), this, list, mod -> {
            RemoteModInfoPage page = new RemoteModInfoPage(getContext(), FCLPage.PAGE_ID_TEMP, R.layout.page_download_addon_info, this, mod, version.get(), callback);
            UIManager.getInstance().getDownloadUI().showTempPage(page);
        });
    }

    /**
     * Khôi phục kết quả tìm kiếm lần trước của chế độ (gọi khi chuyển về), không tìm lại;
     * Dùng lại adapter đã cache của chế độ thì không tạo lại list, tránh hoạt ảnh item trượt vào phát lại
     */
    private void restoreResult() {
        setLoading(false);
        retry.setVisibility(View.GONE);
        pageOffset.set(searchState.pageOffset);
        pageCount.set(searchState.pageCount);
        adapter = searchState.adapter;
        if (adapter == null) {
            adapter = createAdapter(searchState.result);
            searchState.adapter = adapter;
        }
        // Sau khi DownloadUI bị ViewPager2 thu hồi rồi tạo lại, RecyclerView là view hoàn toàn mới (không có LayoutManager),
        // Dùng lại adapter đã cache cũng cần bù lại, không thì list sẽ không render
        if (recyclerView.getLayoutManager() == null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        recyclerView.setAdapter(adapter);
    }

    protected String getLocalizedCategoryIndent(CategoryIndented indented) {
        if (indented.category() == null) {
            return getContext().getString(R.string.curse_category_0);
        }
        StringBuilder result = new StringBuilder();
        result.append(StringUtils.repeats(' ', indented.indent() * 4));

        String localized = getLocalizedCategory(indented.category().id());
        if (!localized.startsWith("curse_category_")) {
            result.append(localized);
            return result.toString();
        }
        Object self = indented.category().self();
        if (self instanceof CurseAddon.Category curseCategory) {
            result.append(curseCategory.getName());
        } else if (self instanceof ModrinthRemoteModRepository.Category modrinthCategory) {
            result.append(modrinthCategory.name());
        }
        return result.toString();
    }

    public void create() {
        binding = PageDownloadBinding.bind(getContentView());
        searchState = new ViewModelProvider(getActivity()).get(DownloadSearchViewModel.class).getState(getPageId());
        searchLayout = findViewById(R.id.search_layout);
        ThemeEngine.getInstance().registerEvent(searchLayout, () -> searchLayout.setBackgroundTintList(new ColorStateList(new int[][]{{}}, new int[]{getContext().getResources().getColor(R.color.card_bg, null)})));

        search = findViewById(R.id.search);
        search.setOnClickListener(this);
        installModpack = findViewById(R.id.install_modpack);
        installModpack.setOnClickListener(this);
        translate = findViewById(R.id.translate);
        translate.setOnClickListener(this);

        nameEditText = findViewById(R.id.name);
        sourceText = findViewById(R.id.download_source_text);
        sourceSpinner = findViewById(R.id.download_source);
        gameVersionSpinner = findViewById(R.id.game_version);
        categorySpinner = findViewById(R.id.category);
        sortSpinner = findViewById(R.id.sort);

        listLayout = findViewById(R.id.list_layout);
        page = findViewById(R.id.page);
        next = findViewById(R.id.next);
        previous = findViewById(R.id.previous);
        first = findViewById(R.id.first);
        last = findViewById(R.id.last);
        recyclerView = findViewById(R.id.list);
        progressBar = findViewById(R.id.progress);
        retry = findViewById(R.id.retry);
        next.setOnClickListener(this);
        previous.setOnClickListener(this);
        first.setOnClickListener(this);
        last.setOnClickListener(this);
        retry.setOnClickListener(this);
        page.setOnClickListener(this);

        nameEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
            }
            return false;
        });
        nameEditText.setHint(supportChinese.get() ? getContext().getString(R.string.search_hint_chinese) : getContext().getString(R.string.search_hint_english));

        versionList.addAll(Arrays.stream(RemoteModRepository.DEFAULT_GAME_VERSIONS).collect(Collectors.toList()));
        versionList.add(0, "");
        gameVersionSpinner.setDataList(versionList);
        ArrayAdapter<String> gameVersionAdapter = new ArrayAdapter<>(getContext(), R.layout.item_spinner_auto_tint, versionList);
        gameVersionAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        gameVersionSpinner.setAdapter(gameVersionAdapter);
        gameVersionSpinner.setSelection(0);
        FXUtils.bindSelection(gameVersionSpinner, gameVersion);

        ArrayList<CategoryIndented> categoryDataList = new ArrayList<>();
        categoryDataList.add(new CategoryIndented(0, null));
        categorySpinner.setDataList(categoryDataList);
        ArrayList<String> categoryStringList = categoryDataList.stream().map(this::getLocalizedCategoryIndent).collect(Collectors.toCollection(ArrayList::new));
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), R.layout.item_spinner_auto_tint, categoryStringList);
        categoryAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        categorySpinner.setAdapter(categoryAdapter);
        categorySpinner.setSelection(0);
        FXUtils.bindSelection(categorySpinner, category);
        downloadSource.addListener(sourceListener);

        sortSpinner.setDataList(new ArrayList<>(Arrays.stream(RemoteModRepository.SortType.values()).collect(Collectors.toList())));
        ArrayList<String> sorts = new ArrayList<>();
        sorts.add(getContext().getString(R.string.curse_sort_popularity));
        sorts.add(getContext().getString(R.string.curse_sort_name));
        sorts.add(getContext().getString(R.string.curse_sort_date_created));
        sorts.add(getContext().getString(R.string.curse_sort_last_updated));
        sorts.add(getContext().getString(R.string.curse_sort_author));
        sorts.add(getContext().getString(R.string.curse_sort_total_downloads));
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(getContext(), R.layout.item_spinner_auto_tint, sorts);
        sortAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setSelection(0);
        FXUtils.bindSelection(sortSpinner, sortType);
        pageOffset.addListener(observable -> getActivity().runOnUiThread(() -> page.setText(getContext().getString(R.string.search_page_n, pageOffset.get() + 1, pageCount.get() == -1 ? "-" : pageCount.getValue().toString()))));
        pageCount.addListener(observable -> getActivity().runOnUiThread(() -> page.setText(getContext().getString(R.string.search_page_n, pageOffset.get() + 1, pageCount.get() == -1 ? "-" : pageCount.getValue().toString()))));

        // Bộ lọc loader riêng của chế độ Mod
        List<String> modLoaderList = new ArrayList<>();
        modLoaderList.add(getContext().getString(R.string.curse_category_0));
        modLoaderList.add("Forge");
        modLoaderList.add("NeoForge");
        modLoaderList.add("Fabric");
        modLoaderList.add("Quilt");
        ArrayAdapter<String> modLoaderAdapter = new ArrayAdapter<>(getContext(), R.layout.item_spinner_auto_tint, modLoaderList);
        modLoaderAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        binding.modloader.setAdapter(modLoaderAdapter);
        binding.modloader.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                searchState.modLoaderPosition = position;
                switch (position) {
                    case 0:
                        selectedModLoader = null;
                        break;
                    case 1:
                        selectedModLoader = ModLoaderType.FORGE;
                        break;
                    case 2:
                        selectedModLoader = ModLoaderType.NEO_FORGED;
                        break;
                    case 3:
                        selectedModLoader = ModLoaderType.FABRIC;
                        break;
                    case 4:
                        selectedModLoader = ModLoaderType.QUILT;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedModLoader = null;
            }
        });
    }

    private static void download(Context context, Profile profile, @Nullable String version, RemoteMod.Version file, String subdirectoryName) {
        if (version == null) version = profile.getSelectedVersion();

        Path runDirectory = profile.getRepository().hasVersion(version) ? profile.getRepository().getRunDirectory(version).toPath() : profile.getRepository().getBaseDirectory().toPath();

        DownloadAddonDialog dialog = new DownloadAddonDialog(context, file.getFile().getFilename(), name -> {
            Path dest = runDirectory.resolve(subdirectoryName).resolve(name);

            TaskDialog taskDialog = new TaskDialog(context, new TaskCancellationAction(AppCompatDialog::dismiss));
            taskDialog.setTitle(context.getString(R.string.message_downloading));
            Schedulers.androidUIThread().execute(() -> {
                TaskExecutor executor = Task.composeAsync(() -> {
                    FileDownloadTask task = new FileDownloadTask(NetworkUtils.toURL(file.getFile().getUrl()), dest.toFile());
                    task.setName(file.getName());
                    return task;
                }).whenComplete(Schedulers.androidUIThread(), exception -> {
                    if (exception != null) {
                        if (exception instanceof CancellationException) {
                            Toast.makeText(context, context.getString(R.string.message_cancelled), Toast.LENGTH_SHORT).show();
                        } else {
                            FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(context);
                            builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT);
                            builder.setCancelable(false);
                            builder.setTitle(context.getString(R.string.install_failed_downloading));
                            builder.setMessage(DownloadProviders.localizeErrorMessage(context, exception));
                            builder.setNegativeButton(context.getString(com.tungsten.fcl.R.string.dialog_positive), null);
                            builder.create().show();
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.install_success), Toast.LENGTH_SHORT).show();
                    }
                }).executor();
                taskDialog.setExecutor(executor);
                taskDialog.show();
                executor.start();
            });
        });
        dialog.show();
    }

    @Override
    public void loadVersion(Profile profile, String version) {
        this.version.set(new Profile.ProfileVersion(profile, version));
        if (pageId == PAGE_ID_DOWNLOAD_MOD) {
            modManager = Profiles.getSelectedProfile().getRepository().getModManager(Profiles.getSelectedVersion());
        }
    }

    @Override
    public Task<?> refresh(Object... param) {
        return null;
    }

    @Override
    public void onClick(View v) {
        if (v == search) {
            pageOffset.set(0);
            search();
        }
        if (v == installModpack) {
            Versions.importModpack(getContext());
        }
        if (v == translate) {
            showTranslationDialog();
        }
        if (v == next && pageCount.get() > 1 && pageOffset.get() < pageCount.get() - 1) {
            pageOffset.set(pageOffset.get() + 1);
            search();
        }
        if (v == previous && pageOffset.get() > 0) {
            pageOffset.set(pageOffset.get() - 1);
            search();
        }
        if (v == first && pageCount.get() != 0 && pageCount.get() != -1) {
            pageOffset.set(0);
            search();
        }
        if (v == last && pageCount.get() != 0 && pageCount.get() != -1) {
            pageOffset.set(pageCount.get() - 1);
            search();
        }
        if (v == retry && retrySearch != null) {
            retrySearch.run();
        }
        if (v == page && pageCount.get() != 0 && pageCount.get() != -1) {
            new EditDialog(getContext(), s -> {
                try {
                    int i = Integer.parseInt(s);
                    if (i <= 0) {
                        i = 1;
                    } else if (i > pageCount.get()) {
                        i = pageCount.get();
                    }
                    pageOffset.set(i - 1);
                    search();
                } catch (Throwable ignore) {
                }
            }).show();
        }
    }

    public RemoteModRepository getRepository() {
        return repository;
    }

    private record CategoryIndented(int indent, RemoteModRepository.Category category) {
    }

    private static void resolveCategory(RemoteModRepository.Category category, int indent, List<CategoryIndented> result) {
        result.add(new CategoryIndented(indent, category));
        for (RemoteModRepository.Category subcategory : category.subcategories()) {
            resolveCategory(subcategory, indent + 1, result);
        }
    }

    private void refreshCategory(boolean search) {
        int refreshPageId = pageId;
        Task.supplyAsync(() -> repository.getCategories())
                .thenAcceptAsync(Schedulers.androidUIThread(), categories -> {
                    // Đã đổi chế độ thì bỏ qua callback quá hạn, tránh danh mục chế độ cũ đè lên trang hiện tại
                    if (refreshPageId != pageId) {
                        return;
                    }
                    ArrayList<CategoryIndented> result = new ArrayList<>();
                    result.add(new CategoryIndented(0, null));
                    for (RemoteModRepository.Category category : Lang.toIterable(categories)) {
                        resolveCategory(category, 0, result);
                    }
                    categorySpinner.setDataList(result);
                    ArrayList<String> resultStr = result.stream().map(this::getLocalizedCategoryIndent).collect(Collectors.toCollection(ArrayList::new));
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.item_spinner_auto_tint, resultStr);
                    adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
                    categorySpinner.setAdapter(adapter);
                    FXUtils.unbindSelection(categorySpinner, category);
                    categorySpinner.setSelection(0);
                    category.set(result.get(0));
                    FXUtils.bindSelection(categorySpinner, category);
                    // Khôi phục bộ lọc danh mục lần trước của chế độ (sau khi list danh mục sẵn sàng)
                    if (searchState.category != null) {
                        for (int i = 1; i < result.size(); i++) {
                            if (searchState.category.equals(result.get(i).category())) {
                                categorySpinner.setSelection(i);
                                category.set(result.get(i));
                                break;
                            }
                        }
                    }
                    if (search) search();
                }).start();
    }

    protected void showTranslationDialog() {
        new TranslationDialog(getContext(), repository, s -> {
            nameEditText.setText(s);
            search();
            return Unit.INSTANCE;
        }).show();
    }

    protected String getLocalizedCategory(String category) {
        if (pageId != PAGE_ID_DOWNLOAD_WORLD && downloadSource.get() != null
                && downloadSource.get().equals(getContext().getString(R.string.mods_modrinth))) {
            String key = "modrinth_category_" + category.replace("-", "_");
            if (pageId == PAGE_ID_DOWNLOAD_RESOURCE_PACK) {
                key = key.replaceAll("\\+", "");
            }
            return AndroidUtils.getLocalizedText(getContext(), key);
        }
        return AndroidUtils.getLocalizedText(getContext(), "curse_category_" + category);
    }

    public void jumpToModPage(RemoteMod mod) {
        if (mod.getData() instanceof CurseAddon) {
            sourceSpinner.setSelection(0);
            downloadSource.set(sourceSpinner.getItemAtPosition(0).toString());
        } else {
            sourceSpinner.setSelection(1);
            downloadSource.set(sourceSpinner.getItemAtPosition(1).toString());
        }
        RemoteModInfoPage page = new RemoteModInfoPage(getContext(), FCLPage.PAGE_ID_TEMP, R.layout.page_download_addon_info, this, mod, version.get(), callback);
        UIManager.getInstance().getDownloadUI().showTempPage(page);
    }
}
