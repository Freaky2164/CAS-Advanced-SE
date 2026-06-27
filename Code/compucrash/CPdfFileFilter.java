package compucrash;

import java.io.File;

public class CPdfFileFilter extends CSuffixAwareFilter {

	public boolean accept(File f) {
		String suffix = getSuffix(f);
		
		if (suffix != null) {
			return super.accept(f) || suffix.equalsIgnoreCase("pdf");
		}
		return false;
	}
	public String getDescription() {
		return "Portable Document Format (*.pdf)";
	}

}
