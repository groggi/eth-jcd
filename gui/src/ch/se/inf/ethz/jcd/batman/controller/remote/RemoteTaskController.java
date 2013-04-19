package ch.se.inf.ethz.jcd.batman.controller.remote;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javafx.application.Platform;
import javafx.concurrent.Task;
import ch.se.inf.ethz.jcd.batman.browser.DiskEntryListener;
import ch.se.inf.ethz.jcd.batman.controller.ConnectionException;
import ch.se.inf.ethz.jcd.batman.controller.TaskController;
import ch.se.inf.ethz.jcd.batman.model.Directory;
import ch.se.inf.ethz.jcd.batman.model.Entry;
import ch.se.inf.ethz.jcd.batman.model.File;
import ch.se.inf.ethz.jcd.batman.model.Path;
import ch.se.inf.ethz.jcd.batman.server.IRemoteVirtualDisk;
import ch.se.inf.ethz.jcd.batman.server.VirtualDiskServer;
import ch.se.inf.ethz.jcd.batman.vdisk.VirtualDiskException;

public class RemoteTaskController implements TaskController {
	
	/**
	 * Sort order from smallest to biggest is: File -> Entry -> Directory
	 */
	private static final class FileBeforeDirectoryComparator implements Comparator<Entry> {

		@Override
		public int compare(Entry entry1, Entry entry2) {
			if (entry1 instanceof File) {
				if (entry2 instanceof File) {
					return 0;
				} else {
					return -1;
				}
			} else if (entry1 instanceof Directory) {
				if (entry2 instanceof Directory) {
					return 0;
				} else {
					return 1;
				}
			} else {
				return 0;
			}
		}
		
	}
	
	private static final String SERVICE_NAME = VirtualDiskServer.SERVICE_NAME;
	private static final int BUFFER_SIZE = 4*1024;
	
	private final Comparator<Entry> fileBeforeDirectoryComp = new FileBeforeDirectoryComparator();
	
	private URI uri;
	private Path diskPath;
	private Integer diskId;
	private IRemoteVirtualDisk remoteDisk;
	private List<DiskEntryListener> diskEntryListener = new LinkedList<DiskEntryListener>();
	
	public RemoteTaskController(URI uri) {
		this.uri = uri;
		this.diskPath = new Path(uri.getQuery());
	}

	public Integer getDiskId () {
		return diskId;
	}
	
	public IRemoteVirtualDisk getRemoteDisk () {
		return remoteDisk;
	}
	
	@Override
	public Task<Void> createConnectTask(final boolean createNewIfNecessary) {
		if (isConnected()) {
			throw new IllegalStateException("Already connected.");
		}
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				if (isConnected()) {
					throw new IllegalStateException("Already connected.");
				}
				try {
					updateTitle("Connecting");
					updateMessage("Connecting to virtual disk...");
					Registry registry;
					if (uri.getPort() == -1) {
						registry = LocateRegistry.getRegistry(uri.getHost());
					} else {
						registry = LocateRegistry.getRegistry(uri.getHost(), uri.getPort());
					}
					remoteDisk = (IRemoteVirtualDisk) registry.lookup(SERVICE_NAME);
					if (remoteDisk.diskExists(diskPath)) {
						diskId = remoteDisk.loadDisk(diskPath);
					} else {
						if (createNewIfNecessary) {
							diskId = remoteDisk.createDisk(diskPath);
						} else {
							throw new ConnectionException("Disk does not exist.");
						}
					}
				} catch (RemoteException | NotBoundException | VirtualDiskException e) {
					throw new ConnectionException(e);
				}
				return null;
			}
			
		};
	}

	@Override
	public boolean isConnected() {
		return diskId != null;
	}
	
	private void checkIsConnected() {
		if (!isConnected()) {
			throw new IllegalStateException("Controller is not connected");
		}
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
	
	@Override
	public Task<Entry[]> createDirectoryEntriesTask(final Directory directory) {
		checkIsConnected();
		return new Task<Entry[]>() {

			@Override
			protected Entry[] call() throws Exception {
				checkIsConnected();
				updateTitle("Retrieve directory entries");
				updateMessage("Retrieving directory entries...");
				return remoteDisk.getEntries(diskId, directory);
			}
			
		};
	}

	@Override
	public Task<Long> createFreeSpaceTask() {
		checkIsConnected();
		return new Task<Long>() {

			@Override
			protected Long call() throws Exception {
				checkIsConnected();
				updateTitle("Calculate free space");
				updateMessage("Calculating free space...");
				return remoteDisk.getFreeSpace(diskId);
			}
			
		};
	}

	@Override
	public Task<Long> createOccupiedSpaceTask() {
		checkIsConnected();
		return new Task<Long>() {

			@Override
			protected Long call() throws Exception {
				checkIsConnected();
				updateTitle("Calculate occupied space");
				updateMessage("Calculating occupied space...");
				return remoteDisk.getOccupiedSpace(diskId);
			}
			
		};
	}

	@Override
	public Task<Long> createUsedSpaceTask() {
		checkIsConnected();
		return new Task<Long>() {

			@Override
			protected Long call() throws Exception {
				checkIsConnected();
				updateTitle("Calculate used space");
				updateMessage("Calculating used space...");
				return remoteDisk.getUsedSpace(diskId);
			}
			
		};
	}

	@Override
	public Task<Void> createFileTask(final File file) {
		checkIsConnected();
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				checkIsConnected();
				updateTitle("Create file");
				updateMessage("Creating file...");	
				remoteDisk.createFile(diskId, file);
				return null;
			}
			
		};
	}

	@Override
	public Task<Void> createDirectoryTask(final Directory directory) {
		checkIsConnected();
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				checkIsConnected();
				updateTitle("Create directory");
				updateMessage("Creating directory...");
				remoteDisk.createDirectory(diskId, directory);
				return null;
			}
			
		};
	}

	
	
	@Override
	public Task<Void> createDeleteEntriesTask(final Entry[] entries) {
		checkIsConnected();
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				checkIsConnected();
				updateTitle("Deleting entries");
				
				updateMessage("Discovering items");
				SortedSet<Entry> subEntries = new TreeSet<Entry>(fileBeforeDirectoryComp);
				for (int i = 0; i < entries.length; i++) {
					subEntries.addAll(Arrays.asList(remoteDisk.getAllSubEntries(diskId, entries[i])));
				}
				
				int totalEntries = subEntries.size();
				int currentEntryNumber = 1;
				updateProgress(0, totalEntries);
				for (Entry entry : subEntries) {
					updateMessage("Deleting entry " + currentEntryNumber + " of " + totalEntries);
					remoteDisk.deleteEntry(diskId, entry.getPath());
					currentEntryNumber++;
					updateProgress(currentEntryNumber, totalEntries);
				}
				return null;
			}
			
		};
	}

	private void checkEntriesAlreadyExist (final Path[] entries) throws VirtualDiskException, RemoteException {
		boolean[] entriesExist = remoteDisk.entriesExist(diskId, entries);
		for (int i = 0; i < entries.length; i++) {
			if (entriesExist[i]) {
				throw new VirtualDiskException("Destination entry " + entries[i].getPath() + " already exists");
			}
		}
	}
	
	@Override
	public Task<Void> createMoveTask(final Entry[] sourceEntries, final Path[] destinationPaths) {
		checkIsConnected();
		if (sourceEntries.length != destinationPaths.length) {
			throw new IllegalArgumentException("Source and destination arrays have to be the same size");
		}
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				checkIsConnected();
				updateTitle("Moving entries");
				//check if destination paths not already exist
				checkEntriesAlreadyExist(destinationPaths);
				//move entries
				int totalEntriesToMove = sourceEntries.length;
				for (int i = 0; i < totalEntriesToMove; i++) {
					updateProgress(i, totalEntriesToMove);
					updateMessage("Moving entry " + sourceEntries[i].getPath() + " to " + destinationPaths[i]);
					remoteDisk.renameEntry(diskId, sourceEntries[i], destinationPaths[i]);
				}
				updateProgress(totalEntriesToMove, totalEntriesToMove);
				return null;
			}
			
		};
	}
	
	private void getAllSubEntries(java.io.File file, List<java.io.File> subEntries) {
		if (file.exists()) {
			subEntries.add(file);
			if (file.isDirectory()) {
				for (java.io.File subFile : file.listFiles()) {
					getAllSubEntries(subFile, subEntries);
				}
			}
		}
	}
	
	@Override
	public Task<Void> createImportTask(final String[] sourcePaths, final Path[] destinationPaths) {
		checkIsConnected();
		if (sourcePaths.length != destinationPaths.length) {
			throw new IllegalArgumentException("Source and destination arrays have to be the same size");
		}
		return new Task<Void>() {
			
			private String getFilePath (java.io.File file) {
				return file.getPath().replaceAll("\\\\", "/");
			}
			
			private void importFile (java.io.File file, String destination) throws RemoteException, VirtualDiskException, IOException {
				updateMessage("Importing entry " + file.toString() + " to " + destination);
				if (file.isDirectory()) {
					remoteDisk.createDirectory(diskId, new Directory(new Path(destination), file.lastModified()));
				} else if (file.isFile()) {
					remoteDisk.createFile(diskId, new File(new Path(destination), file.lastModified(), file.length()));
					//Import data
					File diskFile = new File(new Path(destination));
					FileInputStream inputStream = null;
					try {
						inputStream = new FileInputStream(file);
						long bytesToRead = file.length();
						long bytesRead = 0;
						byte[] buffer = new byte[BUFFER_SIZE];
						while (bytesToRead > 0) {
							int currentBytesRead = inputStream.read(buffer);
							if (currentBytesRead < buffer.length) {
								remoteDisk.write(diskId, diskFile, bytesRead, Arrays.copyOf(buffer, currentBytesRead));
							} else {
								remoteDisk.write(diskId, diskFile, bytesRead, buffer);
							}
							bytesToRead -= currentBytesRead;
							bytesRead += currentBytesRead;
						}
						entryAdded(diskFile);
					} finally {
						if (inputStream != null) {
							inputStream.close();
						}
					}
				}
			}
			
			@Override
			protected Void call() throws Exception {
				checkIsConnected();
				updateTitle("Import entries");
				//check if destination paths not already exist
				checkEntriesAlreadyExist(destinationPaths);
				//check how many and which files need to be imported
				@SuppressWarnings("unchecked")
				List<java.io.File>[] importFiles = new List[sourcePaths.length];
				long totalEntriesToImport = 0;
				for (int i = 0; i < sourcePaths.length; i++) {
					importFiles[i] = new LinkedList<java.io.File>();
					getAllSubEntries(new java.io.File(sourcePaths[i]), importFiles[i]);
					totalEntriesToImport += importFiles[i].size();
				}
				//import all entries
				long entriesImported = 0;
				for (int i = 0; i < sourcePaths.length; i++) {
					java.io.File baseFile = importFiles[i].get(0);
					String baseFilePath = getFilePath(baseFile);
					for (java.io.File file : importFiles[i]) {
						String entryPath = getFilePath(file);
						String destination = destinationPaths[i] + entryPath.substring(baseFilePath.length(), entryPath.length());
						updateProgress(entriesImported, totalEntriesToImport);
						importFile(file, destination);
						entriesImported++;
					}
				}
				updateProgress(entriesImported, totalEntriesToImport);
				return null;
			}
			
		};
	}

	@Override
	public Task<Void> createExportTask(final Entry[] sourceEntries, final String[] destinationPaths) {
		checkIsConnected();
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				checkIsConnected();
				//TODO
				throw new UnsupportedOperationException();
			}
			
		};
	}

	@Override
	public Task<Void> createCopyTask(final Entry[] sourceEntries, final Path[] destinationPaths) {
		checkIsConnected();
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				checkIsConnected();
				//TODO
				throw new UnsupportedOperationException();
			}
			
		};
	}

	@Override
	public Task<Void> createDisconnectTask() {
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				close();
				return null;
			}
			
		};
	}

	public void close() {
		if (diskId != null) {
			try {
				remoteDisk.unloadDisk(diskId);
				diskId = null;
				remoteDisk = null;
			} catch (RemoteException | VirtualDiskException e) { }
		}
	}
	
	public void addDiskEntryListener(DiskEntryListener listener) {
		if (!diskEntryListener.contains(listener)) {
			diskEntryListener.add(listener);
		}
	}
	
	public void removeDiskEntryListener(DiskEntryListener listener) {
		diskEntryListener.remove(listener);
	}
	
	private void entryAdded (final Entry entry) {
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				for (DiskEntryListener listener : diskEntryListener) {
					listener.entryAdded(entry);
				}
			}
		});
	}
	
	private void entryDeleted(final Entry entry) {
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				for (DiskEntryListener listener : diskEntryListener) {
					listener.entryDeleted(entry);
				}
			}
		});
	}
	
	private void entryChanged(final Entry oldEntry, final Entry newEntry) {
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				for (DiskEntryListener listener : diskEntryListener) {
					listener.entryChanged(oldEntry, newEntry);
				}
			}
		});
	}
	
}
