package net.yzwlab.twitter4j4scala.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Utility methods for I/O tasks.
 */
public final class IOUtils {

	/**
	 * Close a stream.
	 * 
	 * @param reader
	 *            a stream to close.
	 */
	public static void close(Reader reader) {
		if (reader == null) {
			return;
		}
		try {
			reader.close();
		} catch (IOException e) {
			;
		}
	}

	/**
	 * Close a stream.
	 * 
	 * @param writer
	 *            a stream to close.
	 */
	public static void close(Writer writer) {
		if (writer == null) {
			return;
		}
		try {
			writer.close();
		} catch (IOException e) {
			;
		}
	}

	/**
	 * Users cannot create any instances of this class.
	 */
	private IOUtils() {
		;
	}

}
