package ch.se.inf.ethz.jcd.batman.model.util;

import java.util.Comparator;

import ch.se.inf.ethz.jcd.batman.model.Entry;

public class EntryNameComperator implements Comparator<Entry> {

	@Override
	public int compare(Entry o1, Entry o2) {
		return o1.getPath().getName().toLowerCase()
				.compareTo(o2.getPath().getName().toLowerCase());
	}

}
