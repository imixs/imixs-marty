package org.imixs.marty.ejb;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;

/**
 * This singleton ejb provides a cache to lookup orgunit member information. The
 * cache is used by the TeamLookupService EJB.
 * 
 * The bean reads its configuration imixs.properties file
 * 
 * team.cache-size = maximum number of entries
 * 
 * team.cache-expires = milliseconds after the cache is discarded
 * 
 * The cache-size should be set to the value of minimum concurrent user
 * sessions. cache-expires specifies the expire time of the cache in
 * milliseconds.
 * 
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class TeamCache {

	int DEFAULT_CACHE_SIZE = 500; // maximum 500 users
	int DEFAULT_EXPIRES_TIME = 3600000; // 60 minutes - just to cleanup wasted memory
	long expiresTime = 0;
	long lastReset = 0;
	private Properties configurationProperties = null;
	private Cache cache = null; // cache holds userdata

	private static Logger logger = Logger.getLogger(TeamCache.class.getName());

	@PostConstruct
	void init() {
		try {
			configurationProperties = new Properties();
			try {
				configurationProperties.load(
						Thread.currentThread().getContextClassLoader().getResource("imixs.properties").openStream());
			} catch (Exception e) {
				logger.warning("unable to find imixs.properties in current classpath");
				e.printStackTrace();
			}
			resetCache();
		} catch (Exception e) {
			logger.severe("unable to initalize cache");
			e.printStackTrace();
		}
	}

	/**
	 * resets the cache object and reads the config params....
	 * 
	 */
	public void resetCache() {
		// determine the cache size....
		logger.finest("......resetCache....");
		int iCacheSize = DEFAULT_CACHE_SIZE;
		try {
			iCacheSize = Integer
					.valueOf(configurationProperties.getProperty("team.cache-size", DEFAULT_CACHE_SIZE + ""));
		} catch (NumberFormatException nfe) {
			iCacheSize = DEFAULT_CACHE_SIZE;
		}
		if (iCacheSize <= 0)
			iCacheSize = DEFAULT_CACHE_SIZE;

		// initialize cache
		logger.finest("......resetCache - cache size = " + iCacheSize);
		cache = new Cache(iCacheSize);

		// read expires time...
		try {
			expiresTime = DEFAULT_EXPIRES_TIME;
			String sExpires = configurationProperties.getProperty("team.cache-expires", DEFAULT_EXPIRES_TIME + "");
			expiresTime = Long.valueOf(sExpires);
		} catch (NumberFormatException nfe) {
			expiresTime = DEFAULT_EXPIRES_TIME;
		}
		if (expiresTime <= 0)
			expiresTime = DEFAULT_EXPIRES_TIME;

		logger.finest("......resetCache - cache expires after = " + expiresTime + "ms");
		lastReset = System.currentTimeMillis();

	}

	public Object get(String key) {
		// test if cache is expired
		if (expiresTime > 0) {
			Long now = System.currentTimeMillis();
			if ((now - lastReset) > expiresTime) {
				logger.finest("......cache expired!");
				resetCache();
			}
		}
		return cache.get(key);
	}

	/**
	 * returns true if the key is contained in the cache.
	 * 
	 */
	public boolean contains(String key) {
		return cache.containsKey(key);
	}

	public void put(String key, Object value) {
		cache.put(key, value);
	}

	/**
	 * Cache implementation to hold userData objects
	 * 
	 * @author rsoika
	 * 
	 */
	class Cache extends ConcurrentHashMap<String, Object> implements Serializable {
		private static final long serialVersionUID = 1L;
		private final int capacity;

		public Cache(int capacity) {
			super(capacity + 1, 1.1f);
			this.capacity = capacity;
		}

		protected boolean removeEldestEntry(Entry<String, Object> eldest) {
			return size() > capacity;
		}
	}
}