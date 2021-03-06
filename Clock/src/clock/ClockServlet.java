package clock;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.SimpleTimeZone;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;


@SuppressWarnings("serial")
public class ClockServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(ClockServlet.class.getName());

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSSSSS");
		fmt.setTimeZone(new SimpleTimeZone(0, ""));

		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		String loginUrl = userService.createLoginURL("/");
		String logoutUrl = userService.createLogoutURL("/");
		req.setAttribute("user", user);
		req.setAttribute("loginUrl", loginUrl);
		req.setAttribute("logoutUrl", logoutUrl);

		Entity userPrefs = null;

		if (user != null) {
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

			String cachekey = "UserPrefs:" + user.getUserId();
			userPrefs = (Entity) memcache.get(cachekey);
			
			if(userPrefs == null){
				log.warning("CACHE MISS");
			} else{
				log.warning("CACHE HIT");
			}
		

			if (userPrefs == null) {
				Key userKey = KeyFactory.createKey("UserPrefs", user.getUserId());
				try {
					userPrefs = ds.get(userKey);
					memcache.put(cachekey, userPrefs);

				} catch (EntityNotFoundException e) {
					// no preferences found
				}
			}

			if (userPrefs != null) {
				double tzOffset = ((Double) userPrefs.getProperty("tz_offset")).doubleValue();
				fmt.setTimeZone(new SimpleTimeZone((int) (tzOffset * 60 * 60 * 1000), ""));
				req.setAttribute("tzOffset", tzOffset);
			} else {
				req.setAttribute("tzOffset", 0);
			}
		}

			req.setAttribute("currentTime", fmt.format(new Date()));
			resp.setContentType("text/html");
			RequestDispatcher jsp = req.getRequestDispatcher("/WEB-INF/home.jsp");
			jsp.forward(req, resp);

		}
	}

