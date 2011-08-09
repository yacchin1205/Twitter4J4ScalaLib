package net.yzwlab.twitter4j4scala;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.PropertyConfiguration;

/**
 * The class TwitterService implements services provided by the Twitter.
 */
public class TwitterService {

	/**
	 * The default value of an interval time for requests.
	 */
	private static final long DEFAULT_INTERVAL = 1000L * 60;

	/**
	 * The count property for the Paging.
	 */
	private static final int MAX_REQUEST_COUNT = 200;

	/**
	 * The logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(TwitterService.class);

	/**
	 * The Twitter4J instance.
	 */
	private Twitter twitter;

	/**
	 * An interval time for requests.
	 */
	private long interval;

	/**
	 * A Worker for asynchronous tasks.
	 */
	private HomeTimelineWorker homeTimelineWorker;

	/**
	 * Construct an instance with authentication information.
	 * 
	 * @param consumerKey
	 *            The consumer key of your service.
	 * @param consumerSecret
	 *            The consumer secret of your service.
	 * @param accessToken
	 *            An access token taken by the user.
	 */
	public TwitterService(String consumerKey, String consumerSecret,
			AccessToken accessToken) {
		if (consumerKey == null || consumerSecret == null
				|| accessToken == null) {
			throw new IllegalArgumentException();
		}
		this.interval = DEFAULT_INTERVAL;

		Properties props = new Properties();
		props.setProperty("oauth.consumerKey", consumerKey);
		props.setProperty("oauth.consumerSecret", consumerSecret);
		PropertyConfiguration conf = new PropertyConfiguration(props);

		TwitterFactory factory = new TwitterFactory(conf);
		this.twitter = factory.getInstance(accessToken);
	}

	/**
	 * Get an interval time for requests.
	 * 
	 * @return an interval time for requests.
	 */
	public long getInterval() {
		return interval;
	}

	/**
	 * Set an interval time for requests.
	 * 
	 * @param interval
	 *            an interval time for requests.
	 */
	public void setInterval(long interval) {
		this.interval = interval;
	}

	/**
	 * Dispose the instance.<br/>
	 * It will kill related tasks which are working. The owner of an instance of
	 * the TwitterService must be call this methods if it becomes unnecessary.
	 */
	public void dispose() {
		if (homeTimelineWorker != null) {
			homeTimelineWorker.stop();
			homeTimelineWorker = null;
		}
	}

	/**
	 * Get an iterator to enumerate the Home timeline.
	 * 
	 * @param lifespan
	 *            a time which the iterator lives(milliseconds)
	 * @return an iterator.
	 */
	public Iterator<Status> getHomeTimelineIterator(long lifespan) {
		HomeTimelineWorker worker = prepareHomeTimelineWorker();

		TweetIterator it = new TweetIterator(lifespan);
		worker.addIterator(it);
		return it;
	}

	/**
	 * Start a worker if there are no workers.
	 * 
	 * @return a worker.
	 */
	private HomeTimelineWorker prepareHomeTimelineWorker() {
		if (homeTimelineWorker != null) {
			return homeTimelineWorker;
		}
		homeTimelineWorker = new HomeTimelineWorker();
		(new Thread(homeTimelineWorker)).start();
		return homeTimelineWorker;
	}

	/**
	 * The class TweetIterator implements an iterator of statuses
	 */
	private class TweetIterator implements Iterator<Status> {

		/**
		 * Time limit.
		 */
		private long timeLimit;

		/**
		 * A queue for statuses.
		 */
		private Queue<Status> queue;

		/**
		 * Construct an instance.
		 * 
		 * @param lifespan
		 *            Time to live.
		 */
		public TweetIterator(long lifespan) {
			this.timeLimit = System.currentTimeMillis() + lifespan;
			this.queue = new ConcurrentLinkedQueue<Status>();
		}

		@Override
		public synchronized boolean hasNext() {
			if (isActive() == false && queue.size() == 0) {
				// Stop when this instance is inactive and empty.
				return false;
			}
			while (queue.size() == 0 && isActive()) {
				try {
					wait(500L);
				} catch (InterruptedException e) {
					logger.warn("An exception occurred", e);
				}
			}
			if (isActive() == false && queue.size() == 0) {
				return false;
			}
			return true;
		}

		@Override
		public synchronized Status next() {
			return queue.poll();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Push the status to the queue.
		 * 
		 * @param status
		 *            a status to push.
		 */
		public synchronized void push(Status status) {
			if (status == null) {
				throw new IllegalArgumentException();
			}
			queue.add(status);
			notifyAll();
		}

		/**
		 * Check whether this is active.
		 * 
		 * @return true if this is active.
		 */
		public boolean isActive() {
			return (System.currentTimeMillis() < timeLimit);
		}

	}

	/**
	 * The class Worker implements asynchronous processes.
	 */
	private abstract class AbstractWorker implements Runnable {

		/**
		 * A flag indicates the worker is active/inactive.
		 */
		private boolean stopped;

		/**
		 * Active iterators.
		 */
		private ArrayList<TweetIterator> clients;

		/**
		 * Constructs a Worker.
		 */
		public AbstractWorker() {
			this.stopped = false;
			this.clients = new ArrayList<TweetIterator>();
		}

		/**
		 * Stop the worker.
		 */
		public synchronized void stop() {
			stopped = true;
			notifyAll();
		}

		/**
		 * Add an iterator.
		 * 
		 * @param it
		 *            an Iterator.
		 */
		public synchronized void addIterator(TweetIterator it) {
			this.clients.add(it);
		}

		@Override
		public void run() {
			while (isActive()) {
				try {
					process();
				} catch (TwitterException e) {
					logger.error("An exception occurred", e);

					// TODO notify users
				}

				try {
					synchronized (this) {
						wait(interval);
					}
				} catch (InterruptedException e) {
					logger.warn("An exception occurred", e);
				}
			}
		}

		/**
		 * Process a request.
		 * 
		 * @throws TwitterException
		 *             an exception related the Twitter4J.
		 */
		protected abstract void process() throws TwitterException;

		/**
		 * Push a status to iterators.
		 * 
		 * @param status
		 *            the status to push.
		 */
		protected synchronized void pushToIterators(Status status) {
			if (status == null) {
				throw new IllegalArgumentException();
			}
			ArrayList<TweetIterator> deadList = new ArrayList<TweetIterator>();
			for (TweetIterator it : clients) {
				if (it.isActive() == false) {
					deadList.add(it);
					continue;
				}
				it.push(status);
			}
			for (TweetIterator it : deadList) {
				clients.remove(it);
			}
		}

		/**
		 * Check whether the worker is active
		 * 
		 * @return true if the worker is active.
		 */
		private synchronized boolean isActive() {
			return (stopped == false);
		}

	}

	/**
	 * The class FriendTimelineWorker implements process to get the Friend
	 * Timeline.
	 */
	private class HomeTimelineWorker extends AbstractWorker {

		/**
		 * An id of last status.
		 */
		private Long lastId;

		/**
		 * Constructs a Worker.
		 */
		public HomeTimelineWorker() {
			this.lastId = null;
		}

		@Override
		protected void process() throws TwitterException {
			List<Status> statuses = null;
			if (lastId == null) {
				Paging paging = new Paging();
				paging.count(MAX_REQUEST_COUNT);
				statuses = twitter.getHomeTimeline(paging);
			} else {
				Paging paging = new Paging();
				paging.sinceId(lastId.longValue());
				paging.count(MAX_REQUEST_COUNT);
				statuses = twitter.getHomeTimeline(paging);
			}
			for (int i = statuses.size() - 1; i >= 0; i--) {
				Status status = statuses.get(i);
				pushToIterators(status);

				lastId = status.getId();
			}
		}

	}

}
