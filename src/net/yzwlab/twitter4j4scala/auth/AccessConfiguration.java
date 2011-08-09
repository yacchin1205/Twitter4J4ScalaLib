package net.yzwlab.twitter4j4scala.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import net.yzwlab.twitter4j4scala.util.IOUtils;
import twitter4j.auth.AccessToken;

/**
 * The class AccessConfiguration implements mechanism to load/save a
 * configuration file for an authentication.
 */
public class AccessConfiguration {

	/**
	 * A key for AccessToken.token.
	 */
	public static final String KEY_ACCESS_TOKEN_TOKEN = "accessToken.token";

	/**
	 * A key for AccessToken.tokenSecret.
	 */
	public static final String KEY_ACCESS_TOKEN_SECRET = "accessToken.secret";

	/**
	 * An instance of File indicates the configuration file.
	 */
	private File file;

	/**
	 * Constructs a instance with a File.
	 * 
	 * @param file
	 *            a configuration file.
	 */
	public AccessConfiguration(File file) {
		if (file == null) {
			throw new IllegalArgumentException();
		}
		this.file = file;
	}

	/**
	 * Constructs a instance with a file path.
	 * 
	 * @param path
	 *            a configuration file.
	 */
	public AccessConfiguration(String path) {
		if (path == null) {
			throw new IllegalArgumentException();
		}
		this.file = new File(path);
	}

	/**
	 * Load an access token from the file.
	 * 
	 * @return an access token.
	 * @throws IOException
	 *             An exception related I/O.
	 */
	public AccessToken loadToken() throws IOException {
		Properties props = new Properties();
		Reader in = null;
		try {
			in = new InputStreamReader(new FileInputStream(file), "utf-8");

			props.load(in);
		} finally {
			IOUtils.close(in);
			in = null;
		}
		if (props.containsKey(KEY_ACCESS_TOKEN_TOKEN) == false
				|| props.containsKey(KEY_ACCESS_TOKEN_SECRET) == false) {
			throw new IOException("Properties is not defined");
		}
		return new AccessToken(props.getProperty(KEY_ACCESS_TOKEN_TOKEN),
				props.getProperty(KEY_ACCESS_TOKEN_SECRET));
	}

	/**
	 * Save an access token to the file.
	 * 
	 * @param accessToken
	 *            an access token.
	 * @throws IOException
	 *             An exception related I/O.
	 */
	public void saveToken(AccessToken accessToken) throws IOException {
		Properties props = new Properties();
		props.setProperty(KEY_ACCESS_TOKEN_TOKEN, accessToken.getToken());
		props.setProperty(KEY_ACCESS_TOKEN_SECRET, accessToken.getTokenSecret());

		Writer out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(file), "utf-8");

			props.store(out, "");
			out.flush();
		} finally {
			IOUtils.close(out);
			out = null;
		}
	}

}
