package net.yzwlab.twitter4j4scala.auth;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Main class of authentication utility
 */
public class Main {

	/**
	 * Start an authentication process.
	 * 
	 * @param args
	 *            a consumer key and secret for your service.
	 */
	public static void main(String args[]) {
		try {
			Twitter twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(args[0], args[1]);
			RequestToken requestToken = twitter.getOAuthRequestToken();
			AccessToken accessToken = null;
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			while (null == accessToken) {
				System.out
						.println("Open the following URL and grant access to your account:");
				System.out.println(requestToken.getAuthorizationURL());
				System.out
						.print("Enter the PIN(if aviailable) or just hit enter.[PIN]:");
				String pin = br.readLine();
				try {
					if (pin.length() > 0) {
						accessToken = twitter.getOAuthAccessToken(requestToken,
								pin);
					} else {
						accessToken = twitter.getOAuthAccessToken();
					}
				} catch (TwitterException te) {
					if (401 == te.getStatusCode()) {
						System.out.println("Unable to get the access token.");
					} else {
						te.printStackTrace();
					}
				}
			}

			AccessConfiguration conf = new AccessConfiguration(
					".twitter4j4scala.properties");
			conf.saveToken(accessToken);
			System.out.println("Successfully saved the access information.");
			System.exit(0);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
