/*
 *    ALMA - Atacama Large Millimiter Array
 *    (c) European Southern Observatory, 2002
 *    Copyright by ESO (in the framework of the ALMA collaboration)
 *    and Cosylab 2002, All rights reserved
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
 *    MA 02111-1307  USA
 */
package com.cosylab.logging.client.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import com.cosylab.logging.engine.log.ILogEntry;
import com.cosylab.logging.engine.log.ILogEntry.Field;

/**
 * The class extends the cache on file implemented in LogBufferedFileCache
 * keeping a cache of logs in memory to avoid to access the file
 * for the most frequently accessed logs
 * 
 * The cache stores the logs into an HashMap using their index as key
 * 
 * The cache stores an array of the times and level of the logs
 * to speed up the sorting done by the table.
 * 
 * @author acaproni
 *
 */
public class LogCache extends LogMultiFileCache implements ILogMap {

	/**
	 * The name of the property to set the size of the cache.
	 * This is the size oif the logs buffered by LogCache
	 */
	public static final String CACHESIZE_PROPERTY_NAME = "jlog.cache.size";
	/**
	 * The default size of the buffer of logs
	 */
	public static final int DEFAULT_CACHESIZE = 16384;
	
	
	/**
	 * The size of the buffer of logs
	 */
	private int actualCacheSize;
	
	/**
	 * The logs are stored into an HashMap.
	 * The key is the index of the log.
	 * This choice make inefficient the deletion of a log.
	 * In fact when a log is removed, the logs having a greater position 
	 * are shifted one position toward 0 (newPos=oldPos-1) but it is not 
	 * possible to change the key of an entry in the HashMap.
	 * 
	 */
	private HashMap<Integer,ILogEntry> cache;
	
	/**
	 * The following list used to keep ordered the indexes
	 * in such a way it is fast to insert/remove logs in the cache.
	 * The indexes contained in this object are the indexes in the cache TreeMap
	 * and the size of the TreeMap and the ArrayBlockingQueue is always the same.
	 * 
	 * The functioning is the following:
	 *  - new elements are added in the tail
	 *  - old elemnts are removed from the head
	 *  - whenever an elements is accessed it is moved of one position
	 *    toward the tail reducing the chance to be removed
	 *    The moving operation is performed swapping the accessed elements
	 *    with its neighborhood
	 */
	private ArrayBlockingQueue<Integer> manager = null;
	
	/** 
	 * The aray with the level of each log in the cache
	 * (useful for setting the log level)
	 */
	private HashMap<Integer,Integer> logTypes = new HashMap<Integer,Integer>();
	
	/**
	 * The times of the logs
	 * Integer is the key of the log, Long is its timestamp
	 */
	private HashMap<Integer,Long> logTimes = new HashMap<Integer,Long>();
	
	/**
	 * Build a LogCache object
	 * 
	 * @throws IOException The exception is thrown if the base class
	 *                     is not able to create the cache on a file
	 */
	public LogCache() throws LogCacheException {
		this(getDefaultCacheSize()); 
	}
	
	/**
	 * Build a logCache object of the given size
	 * 
	 * @param size The size of the cache
	 * @throws LogCacheException
	 */
	public LogCache(int size) throws LogCacheException {
		super();
		if (size<=0) {
			throw new LogCacheException("Invalid initial size: "+size);
		}
		actualCacheSize = size;
		System.out.println("Jlog will use cache for " + actualCacheSize + " log records.");		
		cache = new HashMap<Integer,ILogEntry>();
		manager = new ArrayBlockingQueue<Integer>(actualCacheSize, true);
		clear();
	}
	
	/** 
	 * Adds a log in the cache.
	 * It does nothing because the adding is done by its parent class.
	 * What it does is to store the level and time of the log in the arrays
	 * 
	 * @param log The log to add in the cache
	 * @return The key of the added log in the cache
	 * @throws LogCacheException If an error happened while adding the log
	 */
	public synchronized int add(ILogEntry log) throws LogCacheException {
		Integer key = super.add(log);
		synchronized (logTypes) {
			logTypes.put(key,(log.getType()));
		}
		synchronized (logTimes) {
			logTimes.put(key,new Long(((Date)log.getField(Field.TIMESTAMP)).getTime()));
		}
		return key;
	}

	/** 
	 * 
	 * @param pos The key of the log
	 * @return The type of the log with the given key
	 */
	public int getLogType(Integer key)  throws LogCacheException {
		if (!logTypes.containsKey(key)) {
			throw new LogCacheException("Error: getting the type of a deleted log "+key);
		}
		return logTypes.get(key);
	}
	
	/** 
	 * 
	 * @param pos The key of the log
	 * @return The timestamp of the log with the given key
	 */
	public long getLogTimestamp(Integer key) throws LogCacheException {
		if (!logTimes.containsKey(key)) {
			throw new LogCacheException("Error: getting the time of a deleted log "+key);
		}
		
		return logTimes.get(key);
	}
	
	/**
	 * Gets the default cache size, which comes either from the system property
	 * <code>jlog.cache.size</code> (see {@link #CACHESIZE_PROPERTY_NAME}) 
	 * or, if this property is undefined or invalid, from the fixed size given by 
	 * {@link #DEFAULT_CACHESIZE}. 
	 * @return the default cache size to be used if none is given in the constructor
	 */
	private static int getDefaultCacheSize() {
		Integer cacheSizeFromProperty = Integer.getInteger(CACHESIZE_PROPERTY_NAME);
		if (cacheSizeFromProperty != null) {
			return cacheSizeFromProperty.intValue();
		}
		else {
			return DEFAULT_CACHESIZE;
		}
	}
	
	/**
	 * Return the log with the given key.
	 * The method is synchronized because both the HashMap and
	 * the LinkedList must be synchronized if there is a chance
	 * to acces these objects from more then one thread in the same 
	 * time
	 * @see java.util.LinkedList
	 * @see java.util.HashMap
	 *  
	 * @param pos The key of the log
	 * @return The LogEntryXML or null in case of error
	 */
	public synchronized ILogEntry getLog(Integer key) throws LogCacheException {
		ILogEntry log;
		synchronized (cache) {
			log = cache.get(key);
		}
		if (log!=null) {
			// Hit! The log is in the cache
			return log;
		} else {
			// Oops we need to read a log from disk!
			return loadNewLog(key);
		}
	}
	
	/**
	 * Get a log from the cache on disk updating all the 
	 * internal lists
	 * 
	 * @param idx The position of the log
	 * @return The log read from the cache on disk
	 */
	private synchronized ILogEntry loadNewLog(Integer idx) throws LogCacheException {
		// Read the new log from the cache on disk
		ILogEntry log = super.getLog(idx);
		
		// There is enough room in the lists?
		synchronized (cache) {
			if (cache.size()==actualCacheSize) {
				// We need to create a room for the new element
				try {
					Integer itemToRemove = manager.take();
					cache.remove(itemToRemove);
				}
				catch(InterruptedException ex) {
					throw new LogCacheException ("Interrupted while waiting to take from the Queue!");
				}
			}
			// Add the log in the cache
			cache.put(idx,log);
		}
		
		// Append the index in the manager list
		manager.add(idx);
		
		return log; 
	}
	
	
	/**
	 * Empty the cache
	 * 
	 */
	public synchronized void clear() throws LogCacheException {
		synchronized (cache) {
			cache.clear();
		}
		manager.clear();
		synchronized (logTypes) {
			logTypes.clear();
		}
		synchronized (logTimes) {
			logTimes.clear();
		}
		super.clear();
	}
	
	/**
	 * Gets the actual cache size, which may come from {@link #getDefaultCacheSize()} or
	 * may be given in the constructor.
	 * @return
	 */
	public int getCacheSize() {
		return actualCacheSize;
	}
	
	/**
	 * Calculate and return the time frame of the logs managed by the GUI
	 * The time frame is the number of hours/minutes/seconds between the
	 * newest and the oldest log in the GUI
	 * 
	 * I prefer to evaluate the frame instead of storing the min and max value
	 * because it works even if one log is deleted from the cache.
	 * 
	 * @return The time frame
	 */
	public Calendar getTimeFrame() {
		Calendar cal = Calendar.getInstance();
		long min=Long.MAX_VALUE;
		long max=-1;
		synchronized (logTimes) {
			int len =logTimes.size();
			if (len<=1) {
				cal.setTimeInMillis(0);
				return cal;
			}
			Collection<Long> times = logTimes.values();
			for (Long time : times) {
				if (time>max) {
					max=time;
				}
				if (time<min) {
					min=time;
				}
			}
		}
		cal.setTimeInMillis(max-min);
		return cal;
	}
	
	/**
	 * Delete a log with the given key
	 * 
	 * @param pos The key of the log to delete
	 */
	public synchronized void deleteLog(Integer key) throws LogCacheException {
		synchronized (cache) {
			if (cache.containsKey(key)) {
				cache.remove(key);
				manager.remove(key);
			}
		}
		synchronized (logTimes) {
			logTimes.remove(key);
		}
		synchronized (logTypes) {
			logTypes.remove(key);
		}
		super.deleteLog(key);
	}
	
	/**
	 * Delete a collection of logs
	 * 
	 * @param keys The keys of the logs to delete
	 */
	public synchronized void deleteLogs(Collection<Integer> keys) throws LogCacheException {
		for (Integer key: keys) {
			deleteLog(key);
		}
	}
	
	/**
	 * Returns a set of number of logs (i.e. their position in cache)
	 * exceeding the given time frame.
	 * This operation is quite slow because it requires a double scan of the 
	 * logTimes array
	 * 
	 * @param timeframe The time frame to check in millisecond
	 * @return A collection of number of logs exceedding the given timeframe
	 */
	public synchronized Collection<Integer> getLogExceedingTimeFrame(long timeframe) {
		// Look for the newest log
		// We can't assume the oldest is the latest inserted log because the user
		// is allowed to load logs from different sources at any time. 
		long newestTime=-1;
		Collection<Long> times;
		synchronized (logTimes) {
			times = logTimes.values();
			for (Long time: times) {
				if (time>newestTime) {
					newestTime=time;
				}
			}
		}
		long limit = newestTime-timeframe;
		ArrayList<Integer>ret = new ArrayList<Integer>();
		synchronized (logTimes) {
			Set<Integer> keys = logTimes.keySet();
			for (Integer key: keys) {
				if (logTimes.get(key)>limit) {
					ret.add(key);
				}
			}
		}
		return ret;
	}
	
	/**
	 * The keys in the map
	 * 
	 * @return The keys in the map
	 */
	public Set<Integer> keySet() {
		return logTimes.keySet();
	}
	
	/**
	 * Return an iterator over the logs in cache
	 */
	public Iterator<ILogEntry> iterator() {
		return new LogIterator(this);
	}
}
