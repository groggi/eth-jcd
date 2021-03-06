package ch.se.inf.ethz.jcd.batman.model;

/**
 * Model (as defined in MVC-Pattern) for a directory inside a virtual disk.
 * 
 * @see Entry
 * 
 */
public class Directory extends Entry implements Cloneable {

	private static final long serialVersionUID = 5544830256663532103L;

	public Directory() {
		super();
	}

	public Directory(final Path path) {
		super(path);
	}

	public Directory(final Path path, final long timestamp) {
		super(path, timestamp);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}
