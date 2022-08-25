package kibu.kuhn.fileserver.domain;

import java.nio.file.Path;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class Node {
	@NonNull
	private String title;
	@NonNull
	private Path path;
	@NonNull
	private String fileName;
	private boolean folder;
	private List<Node> children;
	private boolean lazy;	
}
