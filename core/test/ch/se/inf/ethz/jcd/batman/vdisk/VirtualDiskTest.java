package ch.se.inf.ethz.jcd.batman.vdisk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.Test;

import ch.se.inf.ethz.jcd.batman.vdisk.impl.VirtualDisk;
import ch.se.inf.ethz.jcd.batman.vdisk.util.VirtualDiskUtil;

public class VirtualDiskTest extends NewDiskPerTest {

	@Test(expected = IllegalArgumentException.class)
	public void invalidCreateAlreadyExistsTest() throws IOException {
		VirtualDisk.create(disk.getHostLocation().getPath());
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidLoadDoesNotExistTest() throws IOException {
		VirtualDisk.load("notexistingfile.vdisk");
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidLoadCorruptDetaTest() throws IOException {
		File corruptDisk = new File("corruptdisk.vdisk");
		corruptDisk.createNewFile();
		try {
			VirtualDisk.load("corruptdisk.vdisk");
		} finally {
			corruptDisk.delete();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidLoadWrontTypeTest() throws IOException {
		RandomAccessFile corruptDisk = new RandomAccessFile("wrongType.vdisk",
				"rw");
		corruptDisk.setLength(1000);
		corruptDisk.close();
		try {
			VirtualDisk.load("wrongType.vdisk");
		} finally {
			File deleteFile = new File("wrongType.vdisk");
			deleteFile.delete();
		}
	}

	@Test()
	public void createFileTest() throws IOException {
		IVirtualFile file = disk.createFile(null, "foo", 100);
		assertEquals(null, file.getParent());
		disk.getRootDirectory().addMember(file);
		assertEquals(disk.getRootDirectory(), file.getParent());
	}

	@Test()
	public void freeListTest() throws IOException {
		IVirtualFile file1 = disk.createFile(disk.getRootDirectory(), "foo1",
				100);
		IVirtualFile file2 = disk.createFile(disk.getRootDirectory(), "foo2",
				150);
		IVirtualFile file3 = disk.createFile(disk.getRootDirectory(), "foo3",
				100);
		disk.createFile(disk.getRootDirectory(), "foo4", 100);
		long maxDiskSize = disk.getSize();
		file1.delete();
		file2.delete();
		file3.delete();
		disk.createFile(disk.getRootDirectory(), "foo5", 150);
		assertTrue(disk.getSize() <= maxDiskSize);
	}

	/**
	 * Creates a directory structure of the following form: <code>
	 * / --> /A/ --> /A/Asub/ --> /A/Asub/AsubSub/
	 *   \--> /B/ --> /B/BSub/
	 * </code>
	 * 
	 * After that the disk is closed and loaded again, to check if the directory
	 * structure is still correct.
	 * 
	 * @throws IOException
	 */
	@Test
	public void directoryStructureCloseLoadTest() throws IOException {
		IVirtualDirectory dirA = disk.createDirectory(disk.getRootDirectory(),
				"A");
		IVirtualDirectory dirB = disk.createDirectory(disk.getRootDirectory(),
				"B");
		IVirtualDirectory dirASub = disk.createDirectory(dirA, "Asub");
		IVirtualDirectory dirBSub = disk.createDirectory(dirB, "Bsub");
		IVirtualDirectory dirASubSub = disk.createDirectory(dirASub, "AsubSub");

		disk.close();
		disk = null; // just to make sure

		disk = VirtualDisk.load(diskFile.getPath());
		assertNotNull(disk);

		IVirtualDiskEntry loadedDirB = VirtualDiskUtil.getDirectoryMember(
				disk.getRootDirectory(), dirB.getName());
		assertNotNull(loadedDirB);

		IVirtualDiskEntry loadedDirA = VirtualDiskUtil.getDirectoryMember(
				disk.getRootDirectory(), dirA.getName());
		assertNotNull(loadedDirA);

		IVirtualDiskEntry loadedDirASub = VirtualDiskUtil.getDirectoryMember(
				(IVirtualDirectory) loadedDirA, dirASub.getName());
		assertNotNull(loadedDirASub);

		assertNotNull(VirtualDiskUtil.getDirectoryMember(
				(IVirtualDirectory) loadedDirASub, dirASubSub.getName()));

		assertNotNull(VirtualDiskUtil.getDirectoryMember(
				(IVirtualDirectory) loadedDirB, dirBSub.getName()));
	}

	/**
	 * Tests if the allocated block have the required size.
	 * 
	 * @throws IOException
	 */
	@Test
	public void blockAllocationSizeTest() throws IOException {
		IDataBlock[] allocateBlock = disk.allocateBlock(100);
		long blockSize = 0;
		for (IDataBlock block : allocateBlock) {
			blockSize += block.getDataSize();
		}
		assertEquals(100, blockSize);
	}

	// tests
	@Test
	public void rootDirectoryInitialStateTest() throws IOException {
		assertNotNull(disk.getRootDirectory());
		assertNull(disk.getRootDirectory().getNextEntry());
		assertNull(disk.getRootDirectory().getPreviousEntry());
		assertTrue(disk.getRootDirectory().exists());
	}

	/**
	 * Creates the following directory tree: <code>
	 * / ---> /A/ ---> /A/C/
	 *   \---> /B/
	 * </code>
	 * 
	 * @throws IOException
	 */
	@Test
	public void creatDirectoryStructure() throws IOException {
		String subDir1Name = "A";
		String subDir2Name = "B";
		String subSubDirName = "C";

		IVirtualDirectory root = disk.getRootDirectory();
		assertNull(root.getNextEntry());
		assertNull(root.getPreviousEntry());
		assertNull(root.getFirstMember());

		// add first sub dir
		IVirtualDirectory subDir1 = disk.createDirectory(root, subDir1Name);
		assertNotNull(subDir1);
		assertTrue(subDir1.exists());
		assertEquals(subDir1Name, subDir1.getName());
		assertEquals(root, subDir1.getParent());
		assertEquals(subDir1, root.getFirstMember());
		assertNull(subDir1.getFirstMember());

		// add second sub dir
		IVirtualDirectory subDir2 = disk.createDirectory(root, subDir2Name);
		assertNotNull(subDir2);
		assertTrue(subDir2.exists());
		assertEquals(subDir2Name, subDir2.getName());
		assertEquals(root, subDir2.getParent());
		assertNull(subDir2.getFirstMember());

		// add sub sub dir
		IVirtualDirectory subSubDir = disk.createDirectory(subDir1,
				subSubDirName);
		assertNotNull(subSubDir);
		assertTrue(subSubDir.exists());
		assertEquals(subSubDirName, subSubDir.getName());
		assertEquals(subDir1, subSubDir.getParent());
		assertNull(subSubDir.getFirstMember());
		assertNull(subSubDir.getNextEntry());
		assertNull(subSubDir.getPreviousEntry());

		// clean up the nice way
		subSubDir.delete();
		subDir2.delete();
		subDir1.delete();

		assertNull(root.getNextEntry());
		assertNull(root.getPreviousEntry());
		assertNull(root.getFirstMember());
		assertFalse(subSubDir.exists());
		assertFalse(subDir2.exists());
		assertFalse(subDir1.exists());
	}

	/**
	 * Creates the following directory tree: <code>
	 * /  --> /subDir/ --> /subDir/subSubDir1/ --> /subDir/subSubDir1/subSubSubDir/
	 *                 \-> /subDir/subSubDir2/
	 * </code>
	 * 
	 * and deletes after that /subDir/subSubDir1 to check if subSubSubDir was
	 * deleted too. After that, to clean up, we delete subDir and check for any
	 * problems too.
	 * 
	 * @throws IOException
	 */
	@Test
	public void deleteSubDirectoriesAutomaticallyTest() throws IOException {
		IVirtualDirectory subDir = disk.createDirectory(
				disk.getRootDirectory(), "subDir");
		IVirtualDirectory subSubDir1 = disk.createDirectory(subDir,
				"subSubDir1");
		IVirtualDirectory subSubDir2 = disk.createDirectory(subDir,
				"subSubDir2");
		IVirtualDirectory subSubSubDir = disk.createDirectory(subSubDir1,
				"subSubSubDir");

		// check for correct structure
		assertEquals(disk.getRootDirectory(), subDir.getParent());
		assertEquals(subDir, disk.getRootDirectory().getFirstMember());

		assertEquals(subDir, subSubDir1.getParent());
		assertEquals(subDir, subSubDir2.getParent());

		assertEquals(subSubDir1, subSubSubDir.getParent());
		assertEquals(subSubSubDir, subSubDir1.getFirstMember());

		// delete subSubDir1
		subSubDir1.delete();

		// check for deletion
		assertFalse(subSubDir1.exists());
		assertFalse(subSubSubDir.exists());

		// clean up (delete subDir)
		subDir.delete();

		// check clean up
		assertFalse(subDir.exists());
		assertFalse(subSubDir2.exists());
		assertNull(disk.getRootDirectory().getFirstMember());
	}

	@Test
	public void sameDirectoryNameExceptionTest() throws IOException {
		IVirtualDirectory dir = disk.createDirectory(disk.getRootDirectory(),
				"samename");

		boolean exceptionCatched = false;
		try {
			disk.createDirectory(disk.getRootDirectory(), "samename");
		} catch (VirtualDiskException ex) {
			exceptionCatched = true;
		} finally {
			dir.delete();

			assertTrue(exceptionCatched);
		}
	}

	@Test
	public void freeOccupiedTest() throws IOException {
		long diskStartSize = disk.getSize();
		assertEquals(disk.getSize(), disk.getOccupiedSpace());
		assertEquals(0, disk.getFreeSpace());
		IDataBlock[] allocateBlock = disk.allocateBlock(200);
		long totalAllocatedBlockSize = 0;
		for (IDataBlock block : allocateBlock) {
			totalAllocatedBlockSize += block.getDiskSize();
		}
		assertEquals(0, disk.getFreeSpace());
		assertEquals(diskStartSize + totalAllocatedBlockSize,
				disk.getOccupiedSpace());
		for (IDataBlock block : allocateBlock) {
			block.free();
		}
		assertEquals(diskStartSize, disk.getOccupiedSpace());
		assertEquals(0, disk.getFreeSpace());
	}
}
