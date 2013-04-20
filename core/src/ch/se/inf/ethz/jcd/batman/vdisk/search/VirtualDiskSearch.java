package ch.se.inf.ethz.jcd.batman.vdisk.search;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import ch.se.inf.ethz.jcd.batman.io.VDiskFile;

/**
 * Implements a search over file an directory names.
 * 
 */
public class VirtualDiskSearch {

	public static final class Settings {
		private boolean caseSensitive;
		private boolean checkFolders;
		private boolean checkFiles;
		private boolean checkSubFolders;

		public Settings() {
			caseSensitive = false;
			checkFolders = false;
			checkFiles = false;
			checkSubFolders = false;
		}

		public boolean isCaseSensitive() {
			return caseSensitive;
		}

		public void setCaseSensitive(boolean caseSensitive) {
			this.caseSensitive = caseSensitive;
		}

		public boolean checkFolders() {
			return checkFolders;
		}

		public void setCheckFolders(boolean onlyFolders) {
			this.checkFolders = onlyFolders;
		}

		public boolean checkFiles() {
			return checkFiles;
		}

		public void setCheckFiles(boolean onlyFiles) {
			this.checkFiles = onlyFiles;
		}

		public boolean checkSubFolders() {
			return checkSubFolders;
		}

		public void setCheckSubFolders(boolean checkSubFolders) {
			this.checkSubFolders = checkSubFolders;
		}
	}

	/**
	 * Searches for the given name in the tree that starts from the given parent
	 * 
	 */
	public static List<VDiskFile> searchName(Settings settings, String term,
			VDiskFile... parents) throws IOException {
		List<VDiskFile> foundEntries = new LinkedList<>();

		String searchTerm = term;
		if (settings.isCaseSensitive()) {
			searchTerm = searchTerm.toLowerCase();
		}

		for (VDiskFile parent : parents) {
			VDiskFile[] children = parent.listFiles();
			for (VDiskFile child : children) {
				boolean check = (settings.checkFiles() && child.isFile())
						|| (settings.checkFolders && child.isDirectory());

				if (check) {
					if (settings.isCaseSensitive()) {
						if (child.getName().contains(searchTerm)) {
							foundEntries.add(child);
						}
					} else {
						if (child.getName().toLowerCase().contains(searchTerm)) {
							foundEntries.add(child);
						}
					}
				}
			}

			if (settings.checkSubFolders()) {
				foundEntries.addAll(searchName(settings, term, children));
			}
		}

		return foundEntries;
	}

}