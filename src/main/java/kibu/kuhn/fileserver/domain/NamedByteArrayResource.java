package kibu.kuhn.fileserver.domain;

import org.springframework.core.io.ByteArrayResource;

public class NamedByteArrayResource extends ByteArrayResource {

	private String name;
	
	public NamedByteArrayResource(byte[] byteArray, String description) {
		super(byteArray, description);
	}

	@Override
	public String getFilename() {
		return name;
	}

	public void setFilename(String filename) {
		name = filename;
	}
}
