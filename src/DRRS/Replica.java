package DRRS;

import DRRS.config.ReplicaPorts;
import org.json.simple.JSONObject;

public abstract class Replica {
	
	// Should be set on construction
	protected ReplicaPorts ports;
	
	/**
	 * Starts server threads
	 */
	abstract void startServers();
	
	/**
	 * Stops server threads and frees their resources
	 */
	abstract void stopServers();
	
	/**
	 * Get the replica's current data for all servers
	 * @return {String}
	 */
	abstract JSONObject getCurrentData();
	
	/**
	 * Set the replica's current data for all servers
	 */
	public abstract void setCurrentData(JSONObject currentData);
	
	/**
	 * Executes the request
	 * @param request
	 */
	public void executeRequest(String request) {
	
	}
}
