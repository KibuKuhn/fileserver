package kibu.kuhn.fileserver.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;

import kibu.kuhn.fileserver.domain.DownloadRequest;
import kibu.kuhn.fileserver.domain.NamedByteArrayResource;
import kibu.kuhn.fileserver.domain.Node;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileService {

	private static Comparator<Node> nodeComparator = new NodeComparator();

	@Value("${uploadPath:#{null}}")
	private String uploadPath;
	@Value("${downloadPaths:#{null}}")
	private List<String> rootPaths;
	private List<Node> rootNodes;

	@PostConstruct
	void postConstruct() throws JsonProcessingException {
		if (rootPaths == null) {
			rootNodes = Collections.emptyList();
		} else {
			List<Path> roots = getRootNodes();
			rootNodes = transform(roots.stream());
		}
	}
	
	public boolean isUpload() {
		return uploadPath != null;
	}
	
	public boolean isDownload() {
		return !rootNodes.isEmpty();
	}

	public List<Node> getNodes() throws IOException {
		return rootNodes;
	}

	private List<Path> getRootNodes() {
		return rootPaths.stream().map(Path::of).collect(Collectors.toList());
	}

	private List<Node> transform(Stream<Path> pathsStream) {
		//@formatter:off
		return pathsStream.map(this::toNode)
				          .sorted(nodeComparator)
				          .collect(Collectors.toList());
		//@formatter:on
	}

	private Node toNode(Path path) {
		var file = path.toFile();
		var node = new Node();
		node.setFolder(file.isDirectory());
		node.setLazy(node.isFolder());
		node.setTitle(file.getName());
		node.setPath(path);
		node.setFileName(path.getFileName().toString());
		return node;
	}

	private Stream<Path> listFiles(Path dir) {
		try {
			//@formatter:off
			return Files.list(dir)
					    .filter(this::canBeListed);					    
			//@formatter:on
		} catch (IOException ex) {
			log.error(ex.getMessage(), ex);
			throw new ServerErrorException(ex.getMessage(), ex);
		}
	}

	private boolean canBeListed(Path path) {
		try {
			return Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isHidden(path);
		} catch (IOException ex) {
			log.warn(ex.getMessage(), ex);
			return false;
		}
	}

	public List<Node> getChildren(String pathString) {
		Path path = toPath(pathString);
		if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			log.error("Invalid path: {}", pathString);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid path: " + pathString);
		}

		return transform(listFiles(path));
	}

	private Path toPath(String pathString) {
		if (pathString.startsWith("file:///")) {
			URI uri = URI.create(pathString);
			return Path.of(uri);
		} else {
			return Path.of(pathString);
		}
	}

	public NamedByteArrayResource getDownload(DownloadRequest request) {
		try {
			var downloadPath = request.getUri();
			var path = toPath(downloadPath);
			if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
			}
			byte[] bytes = Files.readAllBytes(path);
			var fileName = path.getFileName().toString();
			var resource = new NamedByteArrayResource(bytes, fileName);
			resource.setFilename(fileName);
			return resource;
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			throw throwException(ex);
		}
	}

	private ResponseStatusException throwException(Exception ex) {
		if (ex instanceof ResponseStatusException) {
			throw (ResponseStatusException) ex;
		}

		throw new ServerErrorException(ex.getMessage(), ex);
	}

	public void upload(MultipartFile file) {
		if (file.isEmpty()) {
			return;
		}

		try {
			String fileName = file.getOriginalFilename();
			if (fileName == null || fileName.isBlank()) {
				fileName = file.getResource().getFilename();
			}
			if (fileName == null || fileName.isBlank()) {
				log.error("Cannot read file name");
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read file name");
			}

			Path path = Path.of(uploadPath, fileName);
			if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
				log.error("File '{}' already exists", fileName);
				throw new ResponseStatusException(HttpStatus.CONFLICT, "A file with this name already exists");
			}

			byte[] bytes = file.getBytes();
			Files.write(path, bytes, StandardOpenOption.CREATE);
		} catch (IOException ex) {
			log.error(ex.getMessage(), ex);
			throw new ServerErrorException(ex.getMessage(), ex);
		}

	}
}
