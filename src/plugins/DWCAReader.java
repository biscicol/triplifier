package plugins;

import java.io.File;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.StarRecord;
import org.gbif.utils.file.ClosableIterator;

public class DWCAReader implements TabularDataReader {

	 ClosableIterator<StarRecord> starRecordIterator;
	
	@Override
	public String getFormatString() {
        return "DWCA";
	}

	@Override
	public String getShortFormatDesc() {
        return "DWCA";
	}

	@Override
	public String getFormatDescription() {
        return "Darwin Core Archive";
	}

	@Override
	public String[] getFileExtensions() {
        return new String[] {""};
	}

	@Override
	public boolean testFile(String filepath) {
        try {
    		File archiveFolder = new File(filepath);
    		ArchiveFactory.openArchive(archiveFolder).iterator().close();
        }
        catch (Exception e) {
            return false;
        }
		return true;
	}

	@Override
	public boolean openFile(String filepath) {
        try {
    		File archiveFolder = new File(filepath);
    		starRecordIterator = ArchiveFactory.openArchive(archiveFolder).iterator();
        }
        catch (Exception e) {
            return false;
        }
		return true;
	}

	@Override
	public boolean hasNextRow() {
		return starRecordIterator.hasNext();
	}

	@Override
	public String[] getNextRow() {
		StarRecord starRecord = starRecordIterator.next();
		String[] row = new String[3];
		row[0] = starRecord.core().id();
		row[1] = "dwc:" + starRecord.core().value(DwcTerm.basisOfRecord);
		row[2] = starRecord.core().value("http://purl.org/dc/terms/modified");
        return row;
	}

	@Override
	public void closeFile() {
		starRecordIterator.close();
	}

}
