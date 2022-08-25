package kibu.kuhn.fileserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import kibu.kuhn.fileserver.service.FileService;

@Controller
public class HomeController {
	
	@Autowired
	private FileService fileService;
	
	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("downloadSupported", fileService.isDownload());
		model.addAttribute("uploadSupported", fileService.isUpload());
		return "home";
	}
}
