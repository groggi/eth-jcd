package ch.se.inf.ethz.jcd.batman.vdisk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;


/*
 * 
 * FileEntry 64 bytes
 * 0x00  1 Entry Type
 * 0x01  1 Continuous Entries
 * 0x02  8 Time Stamp
 * 0x0A  8 Starting Block of File
 * 0x12  8 BlockNr of next File/Directory
 * 0x1A 38 FileName (last byte reserved for block end if no continuous entries)
 * 
 * Continuous Entry 
 * 0x00 64 File/Directory name extended (last byte reserved for block end if last continuous entries)
 * 
 * FreeBlock Start 64 bytes
 * 0x00 8  Size and free identifier
 * 0x00 56 Free
 * 
 * FreeBlock Middle 64 bytes
 * 0x00 64 Free
 * 
 * FreeBlock End 64 bytes
 * 0x00 63 Free
 * 0xFF 1  Size and free identifier
 * 
 * DataBlock Start 64 bytes
 * 0x00 8  Size and free identifier
 * 0x08 8  Block Number of next block
 * 0x00 48 Data
 * 
 * DataBlock Middle 64 bytes
 * 0x00 64 Data
 * 
 * DataBlock End 64 bytes
 * 0x00 63 Data
 * 0xFF 1  Size and free/data block identifier 
 */
public class VirtualDisk implements IVirtualDisk {

	public static VirtualDisk load (String path) {
		return null;
	}
	
	public static VirtualDisk create (String path, long maxSize) throws IOException {
		return new VirtualDisk(path, maxSize);
	}
	
	private static final byte[] MAGIC_NUMBER = new byte[] {(byte) 0xDE, (byte) 0xAD, (byte) 0xC0, (byte) 0xFF, (byte) 0xEE, 0x00, 0x00, 0x00};
	private static final int SUPERBLOCK_SIZE = 192;  
	private static final int FREE_LISTS_POSITION = 16;
	private static final int POSITION_SIZE = 8;
	private static final int NR_FREE_LISTS = 22;
	private static final int FREE_LIST_SIZE = NR_FREE_LISTS*POSITION_SIZE;
	private static final String ROOT_DIRECTORY_NAME = "root";
	
	private long maxSize;
	private RandomAccessFile file;
	private IVirtualDirectory rootDirectory;
	
	public VirtualDisk(String path, long maxSize) throws IOException {
		setMaxSize(maxSize);
		File f = new File(path);
		if (f.exists()) {
			throw new IllegalArgumentException("Can't create Virtual Diks at " + path + ". File already exists");
		}
		file = new RandomAccessFile(f, "rw");
		
		/*
		 * 0x00 8byte   MagicNumber
		 * 0x08 8byte   Reserved
		 * 0x10 112byte FreeLists
		 */
		file.write(MAGIC_NUMBER);
		initializeFreeList();
		createRootDirectory();
	}
	
	/*
	 * Initialize the segregated free lists
	 * There is a class for each two power size
	 * 1 : 1-2
	 * 2 : 3-4
	 * 3 : 5-8
	 * ....
	 * 20 : 524229-1048576
	 * 21 : 1048577-2097152
	 * 22 : 2097153-infinity
	 */
	private void initializeFreeList () throws IOException {
		file.seek(FREE_LISTS_POSITION);
		for (int i = 0; i < FREE_LIST_SIZE; i++) {
			file.write(0);
		}
	}
	
	private void createRootDirectory() throws IOException {
		rootDirectory = createDirectory(null, ROOT_DIRECTORY_NAME);
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}
	
	@Override
	public void close() throws IOException {
		if (file != null) {
			file.close();
		}
	}

	@Override
	public void setMaxSize(long maxSize) throws IOException {
		long fileLength = file.length();
		if (fileLength > maxSize || getMinSize() > maxSize) {
			throw new IllegalArgumentException("Virtual file system can't be smaller than " + Math.max(maxSize, getMinSize()));
		}
		this.maxSize = maxSize;
	}
	
	@Override
	public long getMaxSize() {
		return maxSize;
	}

	@Override
	public long getSize() throws IOException {
		return file.length();
	}

	@Override
	public int getSuperblockSize() {
		return SUPERBLOCK_SIZE;
	}

	@Override
	public IVirtualDirectory getRootDirectory() {
		return rootDirectory;
	}

	@Override
	public long getMinSize() {
		return SUPERBLOCK_SIZE + DataBlock.MIN_BLOCK_SIZE;
	}

	@Override
	public IVirtualDirectory createDirectory(IVirtualDirectory parent,
			String name) throws IOException {
		return new VirtualDirectory(this, parent, name);
	}

	@Override
	public IVirtualFile createFile(IVirtualDirectory parent, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(long pos, byte b) throws IOException {
		file.seek(pos);
		file.write(b);
	}

	@Override
	public void write(long pos, byte[] b) throws IOException {
		file.seek(pos);
		file.write(b);
	}

	@Override
	public byte read(long pos) throws IOException {
		file.seek(pos);
		return file.readByte();
	}

	@Override
	public int read(long pos, byte[] b) throws IOException {
		file.seek(pos);
		return file.read(b);
	}

	@Override
	public void write(long pos, byte[] b, int offset, int length)
			throws IOException {
		file.seek(pos);
		file.write(b, offset, length);
	}

	@Override
	public int read(long pos, byte[] b, int offset, int length)
			throws IOException {
		file.seek(pos);
		return file.read(b, offset, length);
	}

	@Override
	public void freeBlock(IDataBlock block) throws IOException {
		long next = block.getNextBlock();
		freeRange(block.getBlockPosition(), block.getDiskSize());
		while (next != 0) {
			IDataBlock toFreeBlock = DataBlock.load(this, next);
			next = toFreeBlock.getNextBlock();
			freeRange(toFreeBlock.getBlockPosition(), toFreeBlock.getDiskSize());
		}
		
	}

	private void freeRange (long position, long length) {
		
	}
	
	@Override
	public IDataBlock[] allocateBlock(long size) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
}
