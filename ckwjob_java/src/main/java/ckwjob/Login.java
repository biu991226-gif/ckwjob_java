package ckwjob;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class Login extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 初期表示用として、現在のログイン状態だけ返す。
        HttpSession session = request.getSession();
        String currentName = trim((String) session.getAttribute("name"));
        String currentRole = trim((String) session.getAttribute("role"));
        boolean isLoggedIn = !currentName.isEmpty() && !currentRole.isEmpty();

        response.setContentType("application/json; charset=UTF-8");
        response.getWriter()
            .append("{")
            .append("\"isLoggedIn\":").append(String.valueOf(isLoggedIn)).append(",")
            .append("\"currentName\":\"").append(jsonEscape(currentName)).append("\",")
            .append("\"currentRole\":\"").append(jsonEscape(currentRole)).append("\"")
            .append("}");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // フォーム入力を受け取り、必要な項目だけ trim する。
        String mode = trim(request.getParameter("mode"));
        String name = trim(request.getParameter("name"));
        String email = trim(request.getParameter("email"));
        String phone = trim(request.getParameter("phone"));
        String password = request.getParameter("password") == null ? "" : request.getParameter("password");
        String role = trim(request.getParameter("role"));
        if (!"company".equals(role)) {
            role = "job_seeker";
        }

        List<String> errors = new ArrayList<>();
        String message = "";
        String redirectUrl = "";

        // MVP段階のため、まずは必須項目の最小チェックだけ行う。
        if (!"login".equals(mode) && !"register".equals(mode)) {
            errors.add("操作区分が不正です。");
        }
        if (email.isEmpty()) {
            errors.add("メールアドレスを入力してください。");
        }
        if (password.isEmpty()) {
            errors.add("パスワードを入力してください。");
        }
        if ("register".equals(mode) && name.isEmpty()) {
            errors.add("氏名を入力してください。");
        }

        if (errors.isEmpty()) {
            try (Connection conn = DbUtil.getConnection()) {
                // mode に応じて登録処理とログイン処理を分ける。
                if ("register".equals(mode)) {
                    registerUser(conn, name, email, password, phone, role, request.getSession(), errors);
                    if (errors.isEmpty()) {
                        message = "会員登録が完了しました。";
                        redirectUrl = request.getContextPath() + "/my.html";
                    }
                } else {
                    loginUser(conn, email, password, request.getSession(), errors);
                    if (errors.isEmpty()) {
                        message = "ログインが完了しました。";
                        redirectUrl = request.getContextPath() + "/my.html";
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                errors.add("DB接続または認証処理に失敗しました。");
            }
        }

        // front 側で結果表示できるように JSON で返す。
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.append("{")
            .append("\"success\":").append(String.valueOf(errors.isEmpty())).append(",")
            .append("\"message\":\"").append(jsonEscape(message)).append("\",")
            .append("\"redirectUrl\":\"").append(jsonEscape(redirectUrl)).append("\",")
            .append("\"errors\":[");
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                out.append(",");
            }
            out.append("\"").append(jsonEscape(errors.get(i))).append("\"");
        }
        out.append("]}");
    }

    private void registerUser(Connection conn, String name, String email, String password, String phone, String role,
            HttpSession session, List<String> errors) throws SQLException {
        // まず同じメールアドレスが既に存在しないか確認する。
        String checkSql = "SELECT id FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    errors.add("このメールアドレスは既に登録済みです。");
                    return;
                }
            }
        }

        // 重複がなければ users テーブルに新規登録する。
        String insertSql = "INSERT INTO users (name, email, password, phone, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            // 新規登録のパスワードは PHP 版と同じく hash 化して保存する。
            String passwordHash = hashPassword(password);
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            if (phone.isEmpty()) {
                stmt.setNull(4, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(4, phone);
            }
            stmt.setString(5, role);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                int userId = 0;
                if (rs.next()) {
                    userId = rs.getInt(1);
                }
                // 登録後はそのままログイン済み状態として session に保存する。
                session.setAttribute("name", name);
                session.setAttribute("role", role);
                session.setAttribute("user_id", userId);
                session.setAttribute("email", email);
            }
        }
    }

    private void loginUser(Connection conn, String email, String password, HttpSession session, List<String> errors)
            throws SQLException {
        // メールアドレスで対象ユーザーを 1 件取得する。
        String sql = "SELECT id, name, password, role FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    errors.add("メールアドレスまたはパスワードが正しくありません。");
                    return;
                }
                String dbPassword = trim(rs.getString("password"));
                // PHP 版と合わせて、bcrypt hash を照合する。
                if (!verifyPassword(password, dbPassword)) {
                    errors.add("メールアドレスまたはパスワードが正しくありません。");
                    return;
                }
                // 認証成功時は以後の画面で使う情報を session に入れる。
                session.setAttribute("name", trim(rs.getString("name")));
                session.setAttribute("role", trim(rs.getString("role")));
                session.setAttribute("user_id", rs.getInt("id"));
                session.setAttribute("email", email);
            }
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String hashPassword(String rawPassword) {
        // PHP の password_hash と合わせるため、bcrypt で保存する。
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    private static boolean verifyPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }

        // PHP 生成の $2y$ は jBCrypt 側で扱える形に寄せてから照合する。
        if (storedPassword.startsWith("$2y$")) {
            storedPassword = "$2a$" + storedPassword.substring(4);
        }

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            try {
                return BCrypt.checkpw(rawPassword, storedPassword);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return false;
    }

    private static String jsonEscape(String value) {
        // JSON 文字列に埋め込むための最小限のエスケープを行う。
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
