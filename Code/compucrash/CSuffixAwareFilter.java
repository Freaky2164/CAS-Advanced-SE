package compucrash;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public abstract class CSuffixAwareFilter extends FileFilter {
	
	public String getSuffix(File f) {
		String s = f.getPath();
		int i = s.lastIndexOf('.');
		
		if (i > 0 && i < s.length() - 1) {
			s = s.substring(i + 1).toLowerCase();
		}
		return s;
	}

	public boolean accept(File f) {
	    return f.isDirectory();
	}
}
