package org.imixs.sywapps.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class Cache extends LinkedHashMap {
	private final int capacity;

	public Cache(int capacity) {
		super(capacity + 1, 1.1f, true);
		this.capacity = capacity;
	}

	protected boolean removeEldestEntry(Entry eldest) {
		return size() > capacity;
	}
}