package kibu.kuhn.fileserver.controller;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.net.URLConnection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kibu.kuhn.fileserver.domain.DownloadRequest;
import kibu.kuhn.fileserver.domain.Mode;
import kibu.kuhn.fileserver.domain.NamedByteArrayResource;
import kibu.kuhn.fileserver.domain.Node;
import kibu.kuhn.fileserver.service.FileService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/data")
public class FileController {

	@Autowired
	private FileService fileService;

	@GetMapping(path = "/getNodes", produces = APPLICATION_JSON_VALUE)
	public List<Node> getNodes(@RequestParam(name = "_") String underscore) throws IOException {
		log.info("underscore={}", underscore);
		return fileService.getNodes();
	}

	@GetMapping(path = "/getTreeData", produces = APPLICATION_JSON_VALUE)
	public List<Node> getTreeData(@RequestParam Mode mode, @RequestParam String path, @RequestParam(name = "_") String underscore) {
		var children = fileService.getChildren(path);
		log.info("Children={}", children);
		return children;
	}

	@PostMapping(path = "/download", consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<NamedByteArrayResource> download(@RequestBody DownloadRequest body) {
		log.info("downloadPath={}", body.getUri());
		var download = fileService.getDownload(body);
		//@formatter:off
		return ResponseEntity.ok()
				             .header(CONTENT_DISPOSITION, "inline;attachment; filename=" + download.getFilename())
				             .contentType(getContentType(download.getFilename()))
				             .body(download);
		//@formatter:on
	}

	private MediaType getContentType(String fileName) {
		var fileNameMap = URLConnection.getFileNameMap();
		var mimeType = fileNameMap.getContentTypeFor(fileName);
		try {
			return MediaType.valueOf(mimeType);
		} catch (InvalidMediaTypeException ex) {
			log.warn(ex.getMessage());
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}
	
	@PostMapping(path = "/upload")
	public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
		log.info("upload={}", file);
		fileService.upload(file);
		return ResponseEntity.ok().build();
	}
	
	
}
