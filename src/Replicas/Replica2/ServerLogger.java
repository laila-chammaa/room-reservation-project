package Replicas.Replica2;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

class ServerLog {
	String requestType;
	String message;
	String parameters;
	
	public ServerLog(String requestType, String message, String parameters) {
		this.requestType = requestType;
		this.message = message;
		this.parameters = parameters;
	}
	
	@Override
	public String toString() {
		return "\nRequest type: " + requestType + "\n"
				+ (parameters == null || parameters.equals("") ? "" : ("Parameters: " + parameters + "\n"))
				+ "Message: " + message + "\n"
				+ "-----------------------------------------------------------------------------------------------------";
	}
}

public class ServerLogger {
	private final Logger logger;
	
	public ServerLogger(String prefix) throws IOException {
		this.logger = Logger.getLogger(prefix);
		File dir = new File("../log/server");
		if (!dir.exists()){
			dir.mkdirs();
		}
		String filePath = "../log/server/" + prefix + ".txt";
		File file = new File(filePath);
		file.createNewFile();
		FileHandler fh = new FileHandler(filePath, true);
		logger.addHandler(fh);
		SimpleFormatter formatter = new SimpleFormatter();
		fh.setFormatter(formatter);
	}
	
	public void logAction(String requestType, boolean succeeded, String message, String parameters) {
		String log = (succeeded ? "SUCCESS" : "FAILURE") + new ServerLog(requestType, message, parameters).toString();
		this.logger.info(log);
	}
}
