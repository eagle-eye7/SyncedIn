package test.book.glass;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class LunchRouletteServlet extends HttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		ServletContext ctx = getServletContext();

		String userId = SessionUtils.getUserId(req);

		LunchRoulette.insertAndSaveSimpleSportsHtmlTimelineItem(ctx, userId);
		LunchRoulette
				.insertAndSaveSimpleRestaurantHtmlTimelineItem(ctx, userId);
		LunchRoulette.insertAndSaveSimpleCuisineHtmlTimelineItem(ctx, userId);
		LunchRoulette.insertAndSaveSimpleBookHtmlTimelineItem(ctx, userId);
		LunchRoulette.insertAndSaveSimpleClothHtmlTimelineItem(ctx, userId);

		resp.setContentType("text/plain");
		resp.getWriter().append("Timeline Items successfully inserted");
	}
}
