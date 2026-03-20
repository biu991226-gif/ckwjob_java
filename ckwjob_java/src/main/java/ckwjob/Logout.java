package ckwjob;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class Logout extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 現在の session を破棄して、トップへ戻る。
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.setContentType("application/json; charset=UTF-8");
        response.getWriter()
            .append("{")
            .append("\"success\":true,")
            .append("\"redirectUrl\":\"").append(request.getContextPath()).append("/\"")
            .append("}");
    }
}
