package com.tungsten.fcl.ui.account

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mio.ui.adapter.ViewHolder
import com.mio.util.copyToClipBoard
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.databinding.ItemAccountBinding
import com.tungsten.fcl.lylee.LyleeFriendsApi
import com.tungsten.fcl.setting.Accounts
import com.tungsten.fcl.ui.UIManager.Companion.instance
import com.tungsten.fclcore.auth.offline.OfflineAccount
import com.tungsten.fclcore.auth.offline.Skin
import com.tungsten.fclcore.task.Schedulers
import com.tungsten.fcllibrary.component.dialog.EditDialog
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AccountListAdapter(
    private val context: Context,
    private val list: MutableList<AccountListItem>
) : RecyclerView.Adapter<ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_account, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = list[position]
        val binding = ItemAccountBinding.bind(holder.itemView)
        binding.radio.isChecked = item.account == Accounts.getSelectedAccount()
        binding.avatar.imageProperty().unbind()
        binding.avatar.imageProperty().bind(item.image)
        binding.name.stringProperty().unbind()
        binding.name.stringProperty().bind(item.title)
        binding.name.setSelected(true)
        binding.type.stringProperty().unbind()
        binding.type.stringProperty().bind(item.subtitle)
        binding.type.setSelected(true)
        binding.skin.setVisibility(
            if (item.canUploadSkin().get()) View.VISIBLE else View.INVISIBLE
        )
        binding.edit.setVisibility(
            if (item.account is OfflineAccount) View.VISIBLE else View.GONE
        )
        bindGoogleLink(binding, item.account.username)
        binding.radio.setOnClickListener {
            Accounts.setSelectedAccount(item.account)
            instance.accountUI.refresh().start()
        }
        binding.refresh.setOnClickListener {
            binding.refresh.setVisibility(View.INVISIBLE)
            binding.refreshProgress.visibility = View.VISIBLE
            item.refreshAsync()
                .whenComplete(Schedulers.androidUIThread()) { ex: Exception? ->
                    binding.refresh.setVisibility(View.VISIBLE)
                    binding.refreshProgress.visibility = View.INVISIBLE
                    if (ex != null) {
                        val builder1 = FCLAlertDialog.Builder(context)
                        builder1.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
                        builder1.setMessage(Accounts.localizeErrorMessage(context, ex))
                        builder1.setNegativeButton(
                            context.getString(com.tungsten.fcl.R.string.dialog_positive),
                            null
                        )
                        builder1.create().show()
                    }
                    item.refreshSkinBinding()
                    instance.accountUI.refresh().start()
                }.start()
        }
        binding.skin.setOnClickListener {
            MainActivity.getInstance().lifecycleScope.launch(Dispatchers.IO) {
                try {
                    item.uploadSkin(
                        onUploading = {
                            binding.skin.visibility = View.INVISIBLE
                            binding.skinProgress.visibility = View.VISIBLE
                        }
                    )
                    withContext(Dispatchers.Main) {
                        binding.skin.visibility = View.VISIBLE
                        binding.skinProgress.visibility = View.INVISIBLE
                        item.refreshSkinBinding()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        binding.copyUuid.setOnClickListener {
            copyToClipBoard(context, item.account.uuid.toString())
            Toast.makeText(context, R.string.message_copy, Toast.LENGTH_SHORT).show()
        }
        binding.edit.setOnClickListener {
            if (item.account is OfflineAccount) {
                val dialog = EditDialog(context) { str ->
                    val uuid = runCatching { UUID.fromString(str) }.getOrNull()
                        ?: run {
                            Toast.makeText(context, R.string.message_failed, Toast.LENGTH_SHORT)
                                .show()
                            return@EditDialog
                        }
                    Accounts.FACTORY_OFFLINE.create(item.account.username, uuid)
                        .apply {
                            skin = item.account.skin
                            Accounts.replaceAccount(item.account.uuid, this)
                            Accounts.setSelectedAccount(this)
                        }
                    instance.accountUI.refresh().start()
                }
                dialog.binding.editText.setText(item.account.uuid.toString())
                dialog.show()
            }
        }
        binding.delete.setOnClickListener {
            val builder = FCLAlertDialog.Builder(context)
            builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
            builder.setMessage(
                String.format(
                    context.getString(R.string.version_manage_remove_confirm),
                    item.title.get()
                )
            )
            builder.setPositiveButton {
                item.remove()
                instance.accountUI.refresh().start()
            }
            builder.setNegativeButton(null)
            builder.create().show()
        }
        binding.skin.setOnLongClickListener {
            if (item.account !is OfflineAccount) {
                return@setOnLongClickListener true
            }
            MainActivity.getInstance().fileLauncher.launchSingleSelection(
                null,
                listOf(".png")
            ) {
                val path = it?.get(0) ?: return@launchSingleSelection
                item.account.skin =
                    Skin(Skin.Type.LOCAL_FILE, null, path, null)
                item.refreshSkinBinding()
            }
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refresh(list: List<AccountListItem>) {
        this.list.clear()
        this.list.addAll(list)
        notifyDataSetChanged()
    }

    // ===================== Liên kết/hủy liên kết Google (theo từng dòng tài khoản) =====================
    // 1 tài khoản Google chỉ liên kết được ĐÚNG 1 dòng (backend chặn nếu đã liên
    // kết chỗ khác, xem Database.loginWithGoogle) — bấm icon để liên kết nếu
    // chưa, hủy liên kết nếu đã có. Không cần phiên đăng nhập bạn bè có sẵn cho
    // dòng này (mobile chỉ giữ 1 phiên hiện tại, không phải theo từng tài khoản)
    // — cả liên kết lẫn hủy liên kết đều tự xác thực qua chính idToken Google
    // vừa đăng nhập, xem LyleeFriendsApi.googleLogin/unlinkGoogle.

    private fun googleSignInClient(context: Context) =
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(LyleeFriendsApi.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build()
        )

    // Tag đánh dấu trạng thái đã tải xong claim-status hay chưa (view bị tái sử
    // dụng khi cuộn — tránh áp kết quả cũ của dòng khác vào dòng mới nếu request
    // trước chưa kịp trả lời) — so username lúc bind với username lúc callback chạy.
    private fun bindGoogleLink(binding: ItemAccountBinding, username: String) {
        binding.googleLink.setColorFilter(Color.GRAY)
        binding.googleLink.contentDescription = username
        binding.googleLink.setOnClickListener(null)
        LyleeFriendsApi.claimStatus(username)
            .whenComplete(Schedulers.androidUIThread()) { res, _ ->
                // Dòng đã bị tái sử dụng cho username khác trong lúc chờ mạng — bỏ qua.
                if (binding.googleLink.contentDescription != username) return@whenComplete
                val linked = res?.hasGoogle ?: false
                applyGoogleLinkState(binding, username, linked)
            }.start()
    }

    private fun applyGoogleLinkState(binding: ItemAccountBinding, username: String, linked: Boolean) {
        binding.googleLink.setColorFilter(
            if (linked) context.resources.getColor(R.color.default_theme_color, null) else Color.GRAY
        )
        binding.googleLink.setOnClickListener {
            if (linked) confirmUnlinkGoogle(binding, username) else linkGoogleAccount(binding, username)
        }
    }

    private fun linkGoogleAccount(binding: ItemAccountBinding, username: String) {
        MainActivity.getInstance().startActivityForResult(googleSignInClient(context).signInIntent) { result ->
            var account: GoogleSignInAccount? = null
            try {
                account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            } catch (e: ApiException) {
                if (e.statusCode != 12501) { // 12501 = người dùng tự hủy chọn tài khoản, không phải lỗi thật
                    Toast.makeText(context, R.string.account_google_link_failed, Toast.LENGTH_SHORT).show()
                }
            }
            val idToken = account?.idToken
            if (idToken != null) {
                LyleeFriendsApi.googleLogin(idToken, username)
                    .whenComplete(Schedulers.androidUIThread()) { res, ex ->
                        if (ex != null || res == null) {
                            Toast.makeText(context, R.string.account_google_link_failed, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, R.string.account_google_linked, Toast.LENGTH_SHORT).show()
                            applyGoogleLinkState(binding, username, true)
                        }
                    }.start()
            }
        }
    }

    private fun confirmUnlinkGoogle(binding: ItemAccountBinding, username: String) {
        val builder = FCLAlertDialog.Builder(context)
        builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
        builder.setMessage(context.getString(R.string.account_google_unlink_confirm, username))
        builder.setPositiveButton {
            MainActivity.getInstance().startActivityForResult(googleSignInClient(context).signInIntent) { result ->
                var account: GoogleSignInAccount? = null
                try {
                    account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
                } catch (e: ApiException) {
                    if (e.statusCode != 12501) {
                        Toast.makeText(context, R.string.account_google_unlink_failed, Toast.LENGTH_SHORT).show()
                    }
                }
                val idToken = account?.idToken
                if (idToken != null) {
                    LyleeFriendsApi.unlinkGoogle(idToken)
                        .whenComplete(Schedulers.androidUIThread()) { res, ex ->
                            if (ex != null || res == null) {
                                Toast.makeText(context, R.string.account_google_unlink_failed, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, R.string.account_google_unlinked, Toast.LENGTH_SHORT).show()
                                applyGoogleLinkState(binding, username, false)
                            }
                        }.start()
                }
            }
        }
        builder.setNegativeButton(null)
        builder.create().show()
    }
}
