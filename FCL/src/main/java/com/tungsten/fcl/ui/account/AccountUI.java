package com.tungsten.fcl.ui.account;

import android.content.Context;
import android.view.View;
import android.widget.ListView;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tungsten.fcl.R;
import com.tungsten.fcl.setting.Accounts;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fcllibrary.component.ui.FCLCommonUI;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class AccountUI extends FCLCommonUI implements View.OnClickListener {

    private LinearLayoutCompat addOfflineAccount;
    private LinearLayoutCompat addMicrosoftAccount;
    private LinearLayoutCompat addLoginServer;

    private RecyclerView recyclerView;
    private AccountListAdapter accountListAdapter;

    public AccountUI(Context context, int id) {
        super(context, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        addOfflineAccount = findViewById(R.id.offline);
        addMicrosoftAccount = findViewById(R.id.microsoft);
        addLoginServer = findViewById(R.id.add_login_server);
        addOfflineAccount.setOnClickListener(this);
        addMicrosoftAccount.setOnClickListener(this);
        addLoginServer.setOnClickListener(this);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        ListView serverListView = findViewById(R.id.server_list);
        serverListView.setAdapter(new ServerListAdapter(getContext()));

        // Tạo lần đầu là làm mới list tài khoản luôn (vòng đời onStart cũ, trang tạo lại thì khởi tạo lại)
        refresh().start();
    }

    @Override
    public Task<?> refresh(Object... param) {
        ArrayList<AccountListItem> accountList = Accounts.getAccounts().stream()
                .map(account -> new AccountListItem(getContext(), account))
                .collect(Collectors.toCollection(ArrayList::new));
        if (accountListAdapter == null) {
            accountListAdapter = new AccountListAdapter(getContext(), accountList);
            recyclerView.setAdapter(accountListAdapter);
        } else {
            accountListAdapter.refresh(accountList);
        }
        return Task.runAsync(() -> {

        });
    }

    @Override
    public void onClick(View view) {
        if (view == addOfflineAccount) {
            showOfflineCreateChoice();
        }
        if (view == addMicrosoftAccount) {
            CreateAccountDialog dialog = new CreateAccountDialog(getContext(), Accounts.FACTORY_MICROSOFT);
            dialog.show();
        }
        if (view == addLoginServer) {
            AddAuthlibInjectorServerDialog dialog = new AddAuthlibInjectorServerDialog(getContext());
            dialog.show();
        }
    }

    // Offline account giờ có 2 kiểu: không mật khẩu (y hệt trước giờ) hoặc có
    // mật khẩu+email — gộp thẳng bước "claim" tài khoản Lylee (bạn bè/chat) vào
    // NGAY lúc tạo, không cần vào riêng màn Bạn bè mới thấy nữa (xem
    // SetFriendsPasswordDialog, CreateAccountDialog.onSuccess).
    private void showOfflineCreateChoice() {
        String[] options = {
                getContext().getString(R.string.account_create_offline_no_password),
                getContext().getString(R.string.account_create_offline_with_password)
        };
        new android.app.AlertDialog.Builder(getContext())
                .setTitle(getContext().getString(R.string.account_create_offline_choice_title))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        new CreateAccountDialog(getContext(), Accounts.FACTORY_OFFLINE).show();
                    } else {
                        new CreateAccountDialog(getContext(), Accounts.FACTORY_OFFLINE, () -> {
                            var account = Accounts.getSelectedAccount();
                            if (account != null) {
                                new SetFriendsPasswordDialog(getContext(), account.getUsername()).show();
                            }
                        }).show();
                    }
                })
                .show();
    }

}
