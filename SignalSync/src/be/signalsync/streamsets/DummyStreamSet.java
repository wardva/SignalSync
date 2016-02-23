package be.signalsync.streamsets;

import java.io.File;

public class DummyStreamSet extends FileStreamSet {
	private static String[] files;
	
	static {
		final File dir = new File("./testdata");
		final File dirFiles[] = dir.listFiles();
		files = new String[dirFiles.length];
		int i = 0;
		for (final File f : dirFiles) {
			files[i++] = f.getAbsolutePath();
		}
	}

	public DummyStreamSet() {
		super(files);
	}
}