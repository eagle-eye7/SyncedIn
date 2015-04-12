package test.book.glass;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import test.book.glass.auth.AuthUtils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.Mirror.Timeline;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.MenuValue;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public final class LunchRoulette {
	public static void insertMultiHtmlTimelineItem(ServletContext ctx,
			HttpServletRequest req) throws IOException, ServletException {
		Mirror mirror = MirrorUtils.getMirror(req);
		Timeline timeline = mirror.timeline();

		// Generate a unique Lunch Roulette bundle id
		String bundleId = "SyncedIn" + UUID.randomUUID();

		// First Cuisine Option
		TimelineItem timelineItem1 = new TimelineItem()
				.setHtml(renderRandomCuisine(ctx)).setBundleId(bundleId)
				.setIsBundleCover(true);
		timeline.insert(timelineItem1).execute();

		// Alternate Cuisine Option
		TimelineItem timelineItem2 = new TimelineItem().setHtml(
				renderRandomCuisine(ctx)).setBundleId(bundleId);
		timeline.insert(timelineItem2).execute();
	}

	public static void insertAndSaveSimpleHtmlTimelineItem(ServletContext ctx,
			String userId) throws IOException, ServletException {
		Mirror mirror = MirrorUtils.getMirror(userId);
		Timeline timeline = mirror.timeline();

		// get a cuisine, populate an object, and render the template
		String cuisine = getRandomCuisine();
		Map<String, String> data = Collections.singletonMap("Name", cuisine);
		String html = render(ctx, "glass/cuisine.ftl", data);

		TimelineItem timelineItem = new TimelineItem()
				.setTitle("Lunch Roulette").setHtml(html)
				.setSpeakableText("You should eat " + cuisine + " for lunch");

		TimelineItem tiResp = timeline.insert(timelineItem).execute();

		setLunchRouletteId(userId, tiResp.getId());
	}

	public static String renderRandomCuisine(ServletContext ctx)
			throws IOException, ServletException {
		String cuisine = getRandomCuisine();
		Map<String, String> data = Collections.singletonMap("Name", cuisine);
		return render(ctx, "glass/cuisine.ftl", data);
	}

	public static void insertSimpleTextTimelineItem(HttpServletRequest req)
			throws IOException {
		Mirror mirror = MirrorUtils.getMirror(req);
		Timeline timeline = mirror.timeline();

		TimelineItem timelineItem = new TimelineItem()
				.setText(getRandomCuisine());

		timeline.insert(timelineItem).executeAndDownloadTo(System.out);
	}

	public static void insertAndSaveSimpleTextTimelineItem(
			HttpServletRequest req) throws IOException {
		String userId = SessionUtils.getUserId(req);
		Credential credential = AuthUtils.getCredential(userId);
		Mirror mirror = MirrorUtils.getMirror(credential);

		Timeline timeline = mirror.timeline();

		TimelineItem timelineItem = new TimelineItem().setTitle(
				"Lunch Roulette").setText(getRandomCuisine());

		setSimpleMenuItems(timelineItem);

		TimelineItem tiResp = timeline.insert(timelineItem).execute();

		setLunchRouletteId(userId, tiResp.getId());
	}

	public static TimelineItem getLastSavedTimelineItem(String userId)
			throws IOException {
		Credential credential = AuthUtils.getCredential(userId);
		Mirror mirror = MirrorUtils.getMirror(credential);
		Timeline timeline = mirror.timeline();

		String id = getLunchRouletteId(userId);

		TimelineItem timelineItem = null;
		if (id != null) {
			timelineItem = timeline.get(id).execute();
		}
		return timelineItem;
	}

	// This code is different. It adds the toggle pinned menu item
	public static void setSimpleMenuItems(TimelineItem ti) {
		// Create a blank menu list
		ti.setMenuItems(new LinkedList<MenuItem>());

		// Add menu items
		ti.getMenuItems().add(new MenuItem().setAction("READ_ALOUD"));
		ti.getMenuItems().add(new MenuItem().setAction("TOGGLE_PINNED"));
		ti.getMenuItems().add(new MenuItem().setAction("DELETE"));
	}

	public static void setAllMenuItems(TimelineItem ti, boolean hasRestaurant) {
		// Add blank menu list
		ti.setMenuItems(new LinkedList<MenuItem>());

		ti.getMenuItems().add(new MenuItem().setAction("READ_ALOUD"));
		ti.getMenuItems().add(new MenuItem().setAction("SHARE"));

		if (hasRestaurant) {
			// add custom menu item
			List<MenuValue> menuValues = new ArrayList<MenuValue>(2);
			menuValues.add(new MenuValue().setState("DEFAULT").setDisplayName(
					"Alternative")
			// .setIconUrl( "" )
					);
			menuValues.add(new MenuValue().setState("PENDING").setDisplayName(
					"Generating Alternative"));

			ti.getMenuItems().add(
					new MenuItem().setAction("CUSTOM").setId("ALT")
							.setPayload("ALT").setValues(menuValues));

			ti.getMenuItems()
					.add(new MenuItem()
							.setAction("CUSTOM")
							.setId("ADD_CONTACT")
							.setPayload("ADD_CONTACT")
							.setRemoveWhenSelected(true)
							.setValues(
									Collections.singletonList(new MenuValue()
											.setState("DEFAULT")
											.setDisplayName("Add As Contact"))));
		}

		ti.getMenuItems().add(new MenuItem().setAction("TOGGLE_PINNED"));

		if (hasRestaurant) {
			// Call and navigate to the restaurant, only if we have one
			ti.getMenuItems().add(new MenuItem().setAction("VOICE_CALL"));
			ti.getMenuItems().add(new MenuItem().setAction("NAVIGATE"));

			// only if we have a restaurant website
			// addMenuItem(ti, "VIEW_WEBSITE");
		}

		// It's good form to make DELETE the last item
		ti.getMenuItems().add(new MenuItem().setAction("DELETE"));
	}

	/**
	 * @return one of many lunch choices.
	 */
	public static String getRandomCuisine() {
		String[] lunchOptions = { "Farhan", "Rehan", "Mommy", "Mukul",
				"Shanky", "Abhi" };
		int choice = new Random().nextInt(lunchOptions.length);
		return lunchOptions[choice];
	}

	/**
	 * Render the HTML template with the given data
	 * 
	 * @param resp
	 * @param data
	 * @throws IOException
	 * @throws ServletException
	 */
	public static String render(ServletContext ctx, String template,
			Map<String, ? extends Object> data) throws IOException,
			ServletException {
		Configuration config = new Configuration();
		config.setServletContextForTemplateLoading(ctx, "WEB-INF/views");
		config.setDefaultEncoding("UTF-8");
		Template ftl = config.getTemplate(template);
		try {
			// use the data to render the template to the servlet output
			StringWriter writer = new StringWriter();
			ftl.process(data, writer);
			return writer.toString();
		} catch (TemplateException e) {
			throw new ServletException("Problem while processing template", e);
		}
	}

	public static boolean setLunchRouletteId(String userId,
			String lunchRouletteId) {
		DatastoreService store = DatastoreServiceFactory.getDatastoreService();
		Key key = KeyFactory.createKey(LunchRoulette.class.getSimpleName(),
				userId);
		Entity entity = new Entity(key);
		entity.setProperty("lastId", lunchRouletteId);
		store.put(entity);
		return true;
	}

	public static String getLunchRouletteId(String userId) {
		DatastoreService store = DatastoreServiceFactory.getDatastoreService();
		Key key = KeyFactory.createKey(LunchRoulette.class.getSimpleName(),
				userId);
		try {
			Entity userData = store.get(key);
			return (String) userData.getProperty("lastId");
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
}
