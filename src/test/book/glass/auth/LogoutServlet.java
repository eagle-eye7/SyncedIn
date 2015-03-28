package test.book.glass.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import test.book.glass.SessionUtils;

@SuppressWarnings("serial")
public class LogoutServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		AuthUtils.deleteCredential(SessionUtils.getUserId(req));
		SessionUtils.removeUserId(req);
		res.getWriter().write("Goodbye!");
	}
}
