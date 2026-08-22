package com.tungsten.fcl.lylee;

import com.google.gson.reflect.TypeToken;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.util.gson.JsonUtils;
import com.tungsten.fclcore.util.io.HttpRequest;
import com.tungsten.fclcore.util.io.NetworkUtils;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Kết bạn/nhắn tin — cùng backend đúng y hệt PC đã dùng thật
 * ({@code FriendsService.cs}), lần đầu bên mobile gọi tới (xem
 * docs/PLAN.md mục 20). Chỉ định nghĩa DTO/lệnh gọi API ở đây — logic
 * UI/state nằm ở {@link LyleeFriendsSession} và các trang riêng.
 * <p>
 * Tên field các DTO PHẢI khớp CHÍNH XÁC JSON key thật (Gson đọc theo tên
 * field, không có @SerializedName) — xem comment từng lớp để biết field đó
 * ứng với gì.
 */
public final class LyleeFriendsApi {

    private static final String BASE_URL = "https://lylee-launcher-api.lyleelauncher.workers.dev";

    private LyleeFriendsApi() {
    }

    // ===================== DTO =====================

    public static final class SuccessResponse {
        public boolean success;
    }

    public static final class ErrorResponse {
        public String message;
    }

    // --- Đăng nhập / đăng ký ---

    public static final class PlayerLoginResponse {
        public String token;
        public String expiresAt;
        public String username;
        public String email;
    }

    public static final class ClaimStatusResponse {
        public boolean claimed;
    }

    // --- Bạn bè ---

    /** status: "Pending" | "Accepted" | "Blocked". isIncoming chỉ có ý nghĩa khi status=Pending. */
    public static final class FriendshipResponse {
        public int friendshipId;
        public String otherUsername;
        public String status;
        public boolean isIncoming;
        public String requestedAt;
        public boolean otherGoogleLinked;
        public boolean hasUnread;
    }

    public static final class FriendsListResponse {
        public List<FriendshipResponse> friends;
        public List<FriendshipResponse> incomingRequests;
        public List<FriendshipResponse> outgoingRequests;
    }

    // --- Tin nhắn ---

    public static final class ReplyPreview {
        public long messageId;
        public String senderUsername;
        public String snippet;
    }

    public static final class MessageResponse {
        public long messageId;
        public String senderUsername;
        public String recipientUsername;
        public String body;
        public String sentAt;
        public boolean isRead;
        public boolean recalled;
        public String attachmentUrl;
        /** null = chưa từng sửa; khác null = chuỗi thời điểm sửa gần nhất. */
        public String editedAt;
        public ReplyPreview replyPreview;
        public String myReaction;
        public String otherReaction;
    }

    public static final class UnreadCountResponse {
        public int unreadCount;
    }

    public static final class AttachmentUploadResponse {
        public String url;
    }

    // ===================== Đăng nhập / đăng ký =====================

    public static Task<ClaimStatusResponse> claimStatus(String username) {
        return getJson(BASE_URL + "/api/players/" + enc(username) + "/claim-status", ClaimStatusResponse.class);
    }

    public static Task<PlayerLoginResponse> login(String username, String password) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/login", null,
                new PasswordBody(password), PlayerLoginResponse.class);
    }

    /** Route KHÔNG nằm dưới /api/players/{username}/ như các route khác — cố định
     *  /api/auth/google, username đi trong body chứ không phải path (xem
     *  ApiServer.googleLogin bên mod backend). Response thật còn có thêm
     *  isPremium nhưng PlayerLoginResponse không cần field đó, Gson bỏ qua. */
    public static Task<PlayerLoginResponse> googleLogin(String idToken, String username) {
        return postJson(BASE_URL + "/api/auth/google", null,
                new GoogleLoginBody(idToken, username), PlayerLoginResponse.class);
    }

    public static Task<SuccessResponse> registerStart(String username, String email) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/register/start", null,
                new EmailBody(email), SuccessResponse.class);
    }

    public static Task<PlayerLoginResponse> registerConfirm(String username, String code, String password) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/register/confirm", null,
                new ConfirmRegistrationBody(code, password), PlayerLoginResponse.class);
    }

    public static Task<SuccessResponse> forgotPassword(String username, String email) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/forgot-password", null,
                new EmailBody(email), SuccessResponse.class);
    }

    public static Task<SuccessResponse> resetPassword(String username, String resetToken, String newPassword) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/reset-password", null,
                new ResetPasswordBody(resetToken, newPassword), SuccessResponse.class);
    }

    // ===================== Bạn bè =====================

    public static Task<FriendsListResponse> listFriends(String username, String token) {
        return getJsonAuth(BASE_URL + "/api/players/" + enc(username) + "/friends", token, FriendsListResponse.class);
    }

    public static Task<SuccessResponse> sendFriendRequest(String username, String token, String targetUsername) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/friends/requests", token,
                new TargetUsernameBody(targetUsername), SuccessResponse.class);
    }

    /** action: "accept" | "decline" | "block". */
    public static Task<SuccessResponse> respondFriendRequest(String username, String token, int friendshipId, String action) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/friends/requests/" + friendshipId + "/respond", token,
                new ActionBody(action), SuccessResponse.class);
    }

    public static Task<SuccessResponse> removeFriend(String username, String token, String other) {
        return deleteJson(BASE_URL + "/api/players/" + enc(username) + "/friends/" + enc(other), token);
    }

    public static Task<SuccessResponse> blockFriend(String username, String token, String other) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/friends/" + enc(other) + "/block", token,
                null, SuccessResponse.class);
    }

    // ===================== Tin nhắn =====================

    public static Task<UnreadCountResponse> unreadCount(String username, String token) {
        return getJsonAuth(BASE_URL + "/api/players/" + enc(username) + "/messages/unread-count", token, UnreadCountResponse.class);
    }

    public static Task<List<MessageResponse>> getConversation(String username, String token, String other) {
        return Task.supplyAsync(() -> {
            try {
                return HttpRequest.GET(BASE_URL + "/api/players/" + enc(username) + "/messages/" + enc(other))
                        .header("Authorization", "Bearer " + token)
                        .getJson(new TypeToken<List<MessageResponse>>() {
                        });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static Task<MessageResponse> sendMessage(String username, String token, String recipientUsername, String body, String attachmentUrl, Long replyToId) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/messages", token,
                new SendMessageBody(recipientUsername, body, attachmentUrl, replyToId), MessageResponse.class);
    }

    public static Task<SuccessResponse> markConversationUnread(String username, String token, String other) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/messages/" + enc(other) + "/mark-unread", token,
                null, SuccessResponse.class);
    }

    public static Task<MessageResponse> recallMessage(String username, String token, long messageId) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/messages/" + messageId + "/recall", token,
                null, MessageResponse.class);
    }

    public static Task<MessageResponse> editMessage(String username, String token, long messageId, String body) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/messages/" + messageId + "/edit", token,
                new EditMessageBody(body), MessageResponse.class);
    }

    /** emoji null/rỗng = bỏ react (toggle off). */
    public static Task<MessageResponse> reactToMessage(String username, String token, long messageId, String emoji) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/messages/" + messageId + "/react", token,
                new ReactMessageBody(emoji), MessageResponse.class);
    }

    public static Task<SuccessResponse> recordTyping(String username, String token, String other) {
        return postJson(BASE_URL + "/api/players/" + enc(username) + "/messages/" + enc(other) + "/typing", token,
                null, SuccessResponse.class);
    }

    public static final class TypingStatusResponse {
        public boolean typing;
    }

    public static Task<TypingStatusResponse> checkTyping(String username, String token, String other) {
        return getJsonAuth(BASE_URL + "/api/players/" + enc(username) + "/messages/" + enc(other) + "/typing", token, TypingStatusResponse.class);
    }

    /** Body ảnh RAW (không phải JSON, không phải multipart) — contentType phải là
     *  image/png|jpeg|gif|webp, tối đa 5MB. {@link HttpRequest.HttpPostRequest#string}
     *  luôn ép UTF-8 nên không dùng được cho byte nhị phân — tự viết HttpURLConnection
     *  ở đây thay vì qua framework GET/POST sẵn có. */
    public static Task<AttachmentUploadResponse> uploadAttachment(String username, String token, byte[] imageBytes, String contentType) {
        return Task.supplyAsync(() -> {
            try {
                URL url = new URL(BASE_URL + "/api/players/" + enc(username) + "/messages/attachments");
                HttpURLConnection conn = NetworkUtils.createHttpConnection(url);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", contentType);
                conn.setRequestProperty("Content-Length", String.valueOf(imageBytes.length));
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(imageBytes);
                }
                String response = NetworkUtils.readData(conn);
                if (conn.getResponseCode() / 100 != 2) {
                    throw new RuntimeException(response);
                }
                return JsonUtils.GSON.fromJson(response, AttachmentUploadResponse.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ===================== Request body (private) =====================

    private static final class PasswordBody {
        final String password;

        PasswordBody(String password) {
            this.password = password;
        }
    }

    private static final class GoogleLoginBody {
        final String idToken;
        final String username;

        GoogleLoginBody(String idToken, String username) {
            this.idToken = idToken;
            this.username = username;
        }
    }

    private static final class EmailBody {
        final String email;

        EmailBody(String email) {
            this.email = email;
        }
    }

    private static final class ConfirmRegistrationBody {
        final String code;
        final String password;

        ConfirmRegistrationBody(String code, String password) {
            this.code = code;
            this.password = password;
        }
    }

    private static final class ResetPasswordBody {
        final String token;
        final String newPassword;

        ResetPasswordBody(String token, String newPassword) {
            this.token = token;
            this.newPassword = newPassword;
        }
    }

    private static final class TargetUsernameBody {
        final String targetUsername;

        TargetUsernameBody(String targetUsername) {
            this.targetUsername = targetUsername;
        }
    }

    private static final class ActionBody {
        final String action;

        ActionBody(String action) {
            this.action = action;
        }
    }

    private static final class SendMessageBody {
        final String recipientUsername;
        final String body;
        final String attachmentUrl;
        final Long replyToId;

        SendMessageBody(String recipientUsername, String body, String attachmentUrl, Long replyToId) {
            this.recipientUsername = recipientUsername;
            this.body = body;
            this.attachmentUrl = attachmentUrl;
            this.replyToId = replyToId;
        }
    }

    private static final class EditMessageBody {
        final String body;

        EditMessageBody(String body) {
            this.body = body;
        }
    }

    private static final class ReactMessageBody {
        final String emoji;

        ReactMessageBody(String emoji) {
            this.emoji = emoji;
        }
    }

    // ===================== Helper =====================

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    private static <T> Task<T> getJson(String url, Class<T> type) {
        return Task.supplyAsync(() -> {
            try {
                return HttpRequest.GET(url).getJson(type);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <T> Task<T> getJsonAuth(String url, String token, Class<T> type) {
        return Task.supplyAsync(() -> {
            try {
                return HttpRequest.GET(url).header("Authorization", "Bearer " + token).getJson(type);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <T> Task<T> postJson(String url, String token, Object body, Class<T> type) {
        return Task.supplyAsync(() -> {
            try {
                HttpRequest.HttpPostRequest req = HttpRequest.POST(url);
                if (token != null) {
                    req.header("Authorization", "Bearer " + token);
                }
                req.json(body != null ? body : "{}");
                return req.getJson(type);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Không dùng framework GET/POST sẵn có — {@link HttpRequest} chưa hỗ trợ DELETE,
     *  tự viết HttpURLConnection ở đây (chỉ dùng cho removeFriend). */
    private static Task<SuccessResponse> deleteJson(String url, String token) {
        return Task.supplyAsync(() -> {
            try {
                HttpURLConnection conn = NetworkUtils.createHttpConnection(new URL(url));
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                String response = NetworkUtils.readData(conn);
                if (conn.getResponseCode() / 100 != 2) {
                    throw new RuntimeException(response);
                }
                return JsonUtils.GSON.fromJson(response, SuccessResponse.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
