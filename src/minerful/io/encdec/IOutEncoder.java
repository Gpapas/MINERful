package minerful.io.encdec;

import java.io.File;
import java.io.IOException;

@Deprecated
public interface IOutEncoder {

	public abstract void setTraces(String[] traces);

	public abstract File encodeToFile(File outFile) throws IOException;

	public abstract String encodeToString() throws IOException;

}