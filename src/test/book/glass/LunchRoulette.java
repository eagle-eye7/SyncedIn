package test.book.glass;

import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import test.book.glass.places.Place;
import test.book.glass.places.PlaceUtils;

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

	public static String img_link;

	public static void insertRandomRestaurantTimelineItem(ServletContext ctx,
			String userId) throws IOException, ServletException {

		Mirror mirror = MirrorUtils.getMirror(userId);
		try {
			Location location = mirror.locations().get("latest").execute();

			double latitude = location.getLatitude();// 28.5442190;
			double longitude = location.getLongitude();// 77.3339640;
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

	public static void insertAndSaveSimpleCuisineHtmlTimelineItem(
			ServletContext ctx, String userId) throws IOException,
			ServletException {
		Mirror mirror = MirrorUtils.getMirror(userId);
		Timeline timeline = mirror.timeline();

		// build a bundle id for all timeline items
		String bundleId = "syncedIn" + UUID.randomUUID();

		// get a cuisine, populate an object, and render the template
		String cuisine = getRandomCuisine();
		Map<String, String> data_cuisine = new HashMap<String, String>();
		data_cuisine.put("food", cuisine);
		data_cuisine.put("img_link", img_link);
		String html_cuisine = render(ctx, "glass/cuisine.ftl", data_cuisine);

		TimelineItem timelineItem_cuisine = new TimelineItem()
				.setTitle("SyncedIn").setHtml(html_cuisine)
				.setSpeakableText("You should eat " + cuisine + " for lunch");

		TimelineItem tiResp = timeline.insert(timelineItem_cuisine).execute();

		setLunchRouletteId(userId, tiResp.getId());
	}

	public static void insertAndSaveSimpleSportsHtmlTimelineItem(
			ServletContext ctx, String userId) throws IOException,
			ServletException {
		Mirror mirror = MirrorUtils.getMirror(userId);
		Timeline timeline = mirror.timeline();

		String sports_complex = "Feroz Shah Kotla Ground";
		Map<String, String> data_sports = new HashMap<String, String>();
		data_sports.put("sports_complex", sports_complex);
		data_sports.put("img_link", "https://dwible.com/img/ground.jpg");
		String html_sports = render(ctx, "glass/sports.ftl", data_sports);

		TimelineItem timelineItem_sports = new TimelineItem()
				.setTitle("SyncedIn")
				.setHtml(html_sports)
				.setSpeakableText(
						"You should visit " + sports_complex
								+ " for the match today");

		TimelineItem tiResp = timeline.insert(timelineItem_sports).execute();

		setLunchRouletteId(userId, tiResp.getId());
	}

	public static void insertAndSaveSimpleRestaurantHtmlTimelineItem(
			ServletContext ctx, String userId) throws IOException,
			ServletException {
		Mirror mirror = MirrorUtils.getMirror(userId);
		Timeline timeline = mirror.timeline();

		String restaurant = "Dosa Plaza";
		Map<String, String> data_restaurant = new HashMap<String, String>();
		data_restaurant.put("restaurant", restaurant);
		data_restaurant.put("img_link", "https://dwible.com/img/dosaPlaza.jpg");
		String html_restaurant = render(ctx, "glass/restaurant.ftl",
				data_restaurant);

		TimelineItem timelineItem_restaurant = new TimelineItem()
				.setTitle("SyncedIn")
				.setHtml(html_restaurant)
				.setSpeakableText(
						"You should visit " + restaurant
								+ " for the lunch today");

		TimelineItem tiResp = timeline.insert(timelineItem_restaurant)
				.execute();

		setLunchRouletteId(userId, tiResp.getId());
	}

	public static void insertAndSaveSimpleBookHtmlTimelineItem(
			ServletContext ctx, String userId) throws IOException,
			ServletException {
		Mirror mirror = MirrorUtils.getMirror(userId);
		Timeline timeline = mirror.timeline();

		String bookStore = "Teksons Bookshop";
		Map<String, String> data_book = new HashMap<String, String>();
		data_book.put("bookStore", bookStore);
		data_book.put("img_link", "https://dwible.com/img/bookshop.jpg");
		String html_bookStore = render(ctx, "glass/bookStore.ftl", data_book);

		TimelineItem timelineItem_book = new TimelineItem()
				.setTitle("SyncedIn")
				.setHtml(html_bookStore)
				.setSpeakableText(
						"You should visit " + bookStore
								+ " for buying books today");

		TimelineItem tiResp = timeline.insert(timelineItem_book).execute();

		setLunchRouletteId(userId, tiResp.getId());
	}

	public static void insertAndSaveSimpleClothHtmlTimelineItem(
			ServletContext ctx, String userId) throws IOException,
			ServletException {
		Mirror mirror = MirrorUtils.getMirror(userId);
		Timeline timeline = mirror.timeline();

		String clothStore = "Wills Lifestyle";
		Map<String, String> data_cloth = new HashMap<String, String>();
		data_cloth.put("clothStore", clothStore);
		data_cloth.put("img_link", "https://dwible.com/img/clothingStore.jpg");
		String html_clothStore = render(ctx, "glass/clothStore.ftl", data_cloth);

		TimelineItem timelineItem_cloth = new TimelineItem()
				.setTitle("SyncedIn")
				.setHtml(html_clothStore)
				.setSpeakableText(
						"You should visit " + clothStore
								+ " for buying clothes today");

		TimelineItem tiResp = timeline.insert(timelineItem_cloth).execute();

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
		String[] lunchOptions = { "North Indian", "South Indian", "Gujrati",
				"Rajasthani Thali", "Sweet Dish" };
		int choice = new Random().nextInt(lunchOptions.length);
		switch (choice) {
		case 0:
			img_link = "http://img.cooklime.com/Recipe/f200864/3.jpg";
			break;
		case 1:
			img_link = "http://www.explosivefashion.in/hospitality_images/NonVeg_Platter-Khaima_Puffs,_Mutton_Kola_Urundu,_Kerala_Chilli_Chicken,_Andhra_Chicken.jpg";
			break;
		case 2:
			img_link = "http://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Coconut_Ladoo_Indian_Sweets.jpg/240px-Coconut_Ladoo_Indian_Sweets.jpg";
			break;
		case 3:
			img_link = "http://www.explosivefashion.in/hospitality_images/2p.jpg";
			break;
		case 4:
			img_link = "http://blog.cleveland.com/taste_impact/2008/12/medium_flavor-kugel.jpg";
		}
		return lunchOptions[choice];
	}

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
