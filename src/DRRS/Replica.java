package DRRS;

import org.json.simple.JSONObject;

public interface Replica {
	
	/**
	 * Starts server threads
	 */
	void startServers();
	
	/**
	 * Stops server threads and frees their resources
	 */
	void stopServers();
	
	/**
	 * Get the replica's current data for all servers
	 * @return {String}
	 */
	JSONObject getCurrentData();
	
	/**
	 * Set the replica's current data for all servers
	 */
	void setCurrentData(JSONObject currentData);
	
	/**
	 * Executes the request by delegating to its servers
	 * @param request
	 */
	void executeRequest(JSONObject request);
}
