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

public class ManageJobs extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        respondWithJobs(request, response, "", "");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String message = "";
        String error = "";
        HttpSession session = request.getSession();
        String currentName = trim((String) session.getAttribute("name"));
        String currentRole = trim((String) session.getAttribute("role"));
        int currentUserId = toInt(session.getAttribute("user_id"));
        boolean isLoggedIn = !currentName.isEmpty() && !currentRole.isEmpty() && currentUserId > 0;

        if (!isLoggedIn) {
            error = "求人管理を利用するにはログインしてください。";
        } else if (!"company".equals(currentRole)) {
            error = "求人管理は企業アカウントのみ利用できます。";
        } else {
            try (Connection conn = DbUtil.getConnection()) {
                String action = trim(request.getParameter("action"));
                if ("delete".equals(action)) {
                    // 自社求人のみ削除できるようにする。
                    int deleteJobId = parseInt(request.getParameter("job_id"));
                    String sql = "DELETE FROM jobs WHERE id = ? AND company_user_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, deleteJobId);
                        stmt.setInt(2, currentUserId);
                        stmt.executeUpdate();
                        if (stmt.getUpdateCount() > 0) {
                            message = "求人を削除しました。";
                        } else {
                            error = "削除対象の求人が見つかりません。";
                        }
                    }
                } else if ("update_status".equals(action)) {
                    // 自社求人のみ募集状態を更新できるようにする。
                    int updateJobId = parseInt(request.getParameter("job_id"));
                    String newStatus = trim(request.getParameter("new_status"));
                    if (!"open".equals(newStatus) && !"closed".equals(newStatus)) {
                        error = "募集状態の値が不正です。";
                    } else {
                        String sql = "UPDATE jobs SET status = ? WHERE id = ? AND company_user_id = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, newStatus);
                            stmt.setInt(2, updateJobId);
                            stmt.setInt(3, currentUserId);
                            stmt.executeUpdate();
                            if (stmt.getUpdateCount() > 0) {
                                message = "募集状態を更新しました。";
                            } else {
                                error = "更新対象の求人が見つかりません。";
                            }
                        }
                    }
                } else {
                    error = "操作区分が不正です。";
                }
            } catch (SQLException e) {
                e.printStackTrace();
                error = "求人管理の更新に失敗しました。";
            }
        }

        respondWithJobs(request, response, message, error);
    }

    private void respondWithJobs(HttpServletRequest request, HttpServletResponse response, String message, String presetError)
            throws IOException {
        HttpSession session = request.getSession();
        String currentName = trim((String) session.getAttribute("name"));
        String currentRole = trim((String) session.getAttribute("role"));
        int currentUserId = toInt(session.getAttribute("user_id"));
        boolean isLoggedIn = !currentName.isEmpty() && !currentRole.isEmpty() && currentUserId > 0;

        String error = presetError;
        StringBuilder jobsJson = new StringBuilder();

        if (error.isEmpty()) {
            if (!isLoggedIn) {
                error = "求人管理を利用するにはログインしてください。";
            } else if (!"company".equals(currentRole)) {
                error = "求人管理は企業アカウントのみ利用できます。";
            } else {
                try (Connection conn = DbUtil.getConnection()) {
                    String sql = "SELECT j.id, j.title, j.area, j.status, j.created_at, COUNT(a.id) AS applicant_count "
                        + "FROM jobs j LEFT JOIN applications a ON j.id = a.job_id "
                        + "WHERE j.company_user_id = ? "
                        + "GROUP BY j.id, j.title, j.area, j.status, j.created_at "
                        + "ORDER BY j.created_at DESC";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, currentUserId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            boolean first = true;
                            while (rs.next()) {
                                if (!first) {
                                    jobsJson.append(",");
                                }
                                jobsJson.append("{")
                                    .append("\"id\":").append(String.valueOf(rs.getInt("id"))).append(",")
                                    .append("\"title\":\"").append(jsonEscape(rs.getString("title"))).append("\",")
                                    .append("\"area\":\"").append(jsonEscape(rs.getString("area"))).append("\",")
                                    .append("\"status\":\"").append(jsonEscape(rs.getString("status"))).append("\",")
                                    .append("\"createdAt\":\"").append(jsonEscape(rs.getString("created_at"))).append("\",")
                                    .append("\"applicantCount\":").append(String.valueOf(rs.getInt("applicant_count")))
                                    .append("}");
                                first = false;
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    error = "求人一覧の取得に失敗しました。";
                }
            }
        }

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.append("{")
            .append("\"isLoggedIn\":").append(String.valueOf(isLoggedIn)).append(",")
            .append("\"currentName\":\"").append(jsonEscape(currentName)).append("\",")
            .append("\"currentRole\":\"").append(jsonEscape(currentRole)).append("\",")
            .append("\"currentUserId\":").append(String.valueOf(currentUserId)).append(",")
            .append("\"message\":\"").append(jsonEscape(message)).append("\",")
            .append("\"error\":\"").append(jsonEscape(error)).append("\",")
            .append("\"jobs\":[").append(jobsJson).append("]")
            .append("}");
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(trim(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int toInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            return parseInt((String) value);
        }
        return 0;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String jsonEscape(String value) {
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
