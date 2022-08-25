package kibu.kuhn.fileserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FileServerApplication {

	public static final String LOG_DIR = "logDir";
	public static final String LOG_FILE = "logFile";

	static {
		initLogging();
	}

	private static void initLogging() {
		var logDir = System.getProperty(LOG_DIR);
		if (logDir == null) {
			logDir = System.getProperty("user.home");
			System.setProperty(LOG_DIR, logDir);
		}
		System.setProperty(LOG_FILE, "fileserver.log");
		String logLevel = System.getProperty("logLevel");
		if (logLevel == null) {
			System.setProperty("logLevel", "INFO");
		}
	}

	/**
	 * When applying commadline arguments, use the follwing syntax.<br>
	 * --first-argument=first-value --second-argument=second-value ...
	 * @param args
	 */
	public static void main(String[] args) {
		if (args != null && args.length > 0 && args[0].matches("-{0,2}\\?")) {
			printUsage();
			return;
		}
		
		SpringApplication.run(FileServerApplication.class, args);
	}

	private static void printUsage() {
		System.out.println("Usage");
		System.out.println("-----");
		System.out.println("Download directories:");
		System.out.println(" Comma separated list of directories: --downloadPaths=/path/to/some/folder,/path/to/some/other/folder, ... Do NOT use whitespaces between commas.");
		System.out.println("Upload directory:");
		System.out.println(" --uploadPath=/path/to/upload/folder");
		System.out.println("Symbolic links are not supported.");
		System.out.println("Logging:");
		System.out.println(" Default log folder is the home directoy of the current user.");
		System.out.println(" Default log file name is fileserver.log");
		System.out.println(" Use -DlogDir=... and -DlogFile=... respectively to change the defaults.");
	}

}
