package net.yzwlab.twitter4j4scala.test;

import java.util.Iterator;

import net.yzwlab.twitter4j4scala.TwitterService;
import net.yzwlab.twitter4j4scala.auth.AccessConfiguration;
import twitter4j.Status;

/**
 * Test for the TwitterService.
 */
public class TwitterServiceTest {

	/**
	 * Start an authentication process.
	 * 
	 * @param args
	 *            a consumer key and secret for your service.
	 */
	public static void main(String args[]) {
		try {
			AccessConfiguration conf = new AccessConfiguration(
					".twitter4j4scala.properties");
			TwitterService service = new TwitterService(args[0], args[1],
					conf.loadToken());

			for (Iterator<Status> it = service
					.getHomeTimelineIterator(1000L * 60 * 3); it.hasNext();) {
				Status status = it.next();
				System.out.println("Status(" + status.getId() + "/"
						+ status.getCreatedAt() + "): " + status.getText()
						+ " by " + status.getUser().getName());
			}

			System.exit(0);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
