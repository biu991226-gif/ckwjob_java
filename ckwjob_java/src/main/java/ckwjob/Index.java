package ckwjob;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class Index extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // 検索キーワードと session 情報を先に受け取る。
        String keyword = trim(request.getParameter("keyword"));
        HttpSession session = request.getSession();
        String currentName = trim((String) session.getAttribute("name"));
        String currentRole = trim((String) session.getAttribute("role"));
        boolean isLoggedIn = !currentName.isEmpty() && !currentRole.isEmpty();
        StringBuilder jobsJson = new StringBuilder();
        String dbError = "";

        try (Connection conn = DbUtil.getConnection()) {
            // キーワード未入力時は最新求人をそのまま返す。
            if (keyword.isEmpty()) {
                String sql = "SELECT id, title, area, salary FROM jobs WHERE status = 'open' ORDER BY created_at DESC LIMIT 10";
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    appendJobs(rs, jobsJson);
                }
            } else {
                // 検索時はタイトル・地域・給与・説明文を対象に部分一致で探す。
                String sql = "SELECT id, title, area, salary FROM jobs "
                    + "WHERE status = 'open' AND (title LIKE ? OR area LIKE ? OR salary LIKE ? OR description LIKE ?) "
                    + "ORDER BY created_at DESC LIMIT 10";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String likeKeyword = "%" + keyword + "%";
                    stmt.setString(1, likeKeyword);
                    stmt.setString(2, likeKeyword);
                    stmt.setString(3, likeKeyword);
                    stmt.setString(4, likeKeyword);
                    try (ResultSet rs = stmt.executeQuery()) {
                        appendJobs(rs, jobsJson);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            dbError = "DB接続または求人取得に失敗しました。設定値を確認してください。";
        }

        // 画面描画は front 側で行うため、Servlet では JSON だけ返す。
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.append("{")
            .append("\"keyword\":\"").append(jsonEscape(keyword)).append("\",")
            .append("\"isLoggedIn\":").append(String.valueOf(isLoggedIn)).append(",")
            .append("\"currentName\":\"").append(jsonEscape(currentName)).append("\",")
            .append("\"currentRole\":\"").append(jsonEscape(currentRole)).append("\",")
            .append("\"dbError\":\"").append(jsonEscape(dbError)).append("\",")
            .append("\"jobs\":[").append(jobsJson).append("]")
            .append("}");
    }

    private static void appendJobs(ResultSet rs, StringBuilder jobsJson) throws SQLException {
        boolean first = true;
        while (rs.next()) {
            // 取得した求人を 1 件ずつ JSON 配列文字列に積む。
            if (!first) {
                jobsJson.append(",");
            }
            jobsJson.append("{")
                .append("\"id\":").append(String.valueOf(rs.getInt("id"))).append(",")
                .append("\"title\":\"").append(jsonEscape(rs.getString("title"))).append("\",")
                .append("\"area\":\"").append(jsonEscape(rs.getString("area"))).append("\",")
                .append("\"salary\":\"").append(jsonEscape(rs.getString("salary"))).append("\"")
                .append("}");
            first = false;
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String jsonEscape(String value) {
        // JSON 文字列で壊れやすい記号だけ最小限エスケープする。
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
