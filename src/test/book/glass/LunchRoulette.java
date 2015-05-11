package test.book.glass;

import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import test.book.glass.auth.AuthUtils;
import test.book.glass.places.Place;
import test.book.glass.places.PlaceUtils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.Base64;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.Mirror.Timeline;
import com.google.api.services.mirror.model.Location;
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
	public static void insertRandomRestaurantTimelineItem(ServletContext ctx,
			String userId) throws IOException, ServletException {
		Mirror mirror = MirrorUtils.getMirror(userId);
		try {
			Location location = mirror.locations().get("latest").execute();

			double latitude = location.getLatitude();// 28.5442190;//
			double longitude = location.getLongitude();// 77.3339640;//
			Place restaurant = getRandomRestaurant(latitude, longitude);
			/*
			 * restaurant.setLatitude(latitude);
			 * restaurant.setLongitude(longitude); restaurant.setKind("Indian");
			 * restaurant.setName("Dosa Plaza"); restaurant .setReference(
			 * "CmRdAAAAHu_Xh4VXBHXB-JdCluK2AKeFhhvLfxJPHplL84cqfKXK8kYL4RF_sAW9K4Mk7nsMDKzAvZ53Bgeg5jMxco1CUlwJiivv3tCE_GpuaBHQyFP1cjtJVateNlQuBytQAduZEhClCFkxZXYBnsRToc8NYEL3GhREXKKeK6C6DPOa6YfXWZF1rcB_iw"
			 * ); restaurant.setAddress("Sector 125, Noida");
			 */
			// create a timeline item with restaurant information
			String html = render(ctx, "glass/restaurant.ftl", restaurant);
			TimelineItem timelineItem = new TimelineItem()
					.setTitle("Synced In").setHtml(html);
			mirror.timeline().insert(timelineItem).execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// get a nearby restaurant from Google Places
	}

	public static void insertAndSaveSimpleHtmlTimelineItem(ServletContext ctx,
			String userId) throws IOException, ServletException {
		Mirror mirror = MirrorUtils.getMirror(userId);
		Timeline timeline = mirror.timeline();

		// get a cuisine, populate an object, and render the template
		String cuisine = getRandomCuisine();
		Map<String, String> data = Collections.singletonMap("food", cuisine);
		String html = render(ctx, "glass/cuisine.ftl", data);

		TimelineItem timelineItem = new TimelineItem()
				.setTitle("Lunch Roulette").setHtml(html)
				.setSpeakableText("You should eat " + cuisine + " for lunch");

		TimelineItem tiResp = timeline.insert(timelineItem).execute();

		setLunchRouletteId(userId, tiResp.getId());
	}

	public static String renderRestaurant(ServletContext ctx, Place restaurant)
			throws IOException, ServletException {
		return render(ctx, "glass/restaurant.ftl", restaurant);
	}

	public static String renderRandomCuisine(ServletContext ctx)
			throws IOException, ServletException {
		String cuisine = getRandomCuisine();
		Map<String, String> data = Collections.singletonMap("food", cuisine);
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

	public static void setSimpleMenuItems(TimelineItem ti, boolean hasRestaurant) {
		// Add blank menu list
		ti.setMenuItems(new LinkedList<MenuItem>());

		ti.getMenuItems().add(new MenuItem().setAction("READ_ALOUD"));
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

	public static Place getRandomRestaurant(double latitude, double longitude)
			throws IOException {
		return PlaceUtils
				.getRandom("restaurant", "indian", latitude, longitude);
	}

	/**
	 * @return one of many lunch choices.
	 */
	public static String getRandomCuisine() {
		String[] lunchOptions = { "American", "Chinese", "French", "Italian",
				"Japenese", "Thai" };
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
	public static String render(ServletContext ctx, String template, Object data)
			throws IOException, ServletException {
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

	public static String toSHA1(byte[] convertme) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return Base64.encodeBase64String(md.digest(convertme));
	}
}
