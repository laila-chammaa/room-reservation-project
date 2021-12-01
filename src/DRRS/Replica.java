package DRRS;

import Replicas.CampusServerInterface;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class Replica {
	
	protected CampusServerInterface dvlCampus;
	protected CampusServerInterface kklCampus;
	protected CampusServerInterface wstCampus;
	
	protected Map<String, CampusServerInterface> campusMap;
	
	protected Replica(CampusServerInterface dvlCampus, CampusServerInterface kklCampus, CampusServerInterface wstCampus) {
		this.dvlCampus = dvlCampus;
		this.kklCampus = kklCampus;
		this.wstCampus = wstCampus;
		
		this.campusMap = new HashMap<>();
		this.campusMap.put("DVL", dvlCampus);
		this.campusMap.put("KKL", kklCampus);
		this.campusMap.put("WST", wstCampus);
	}
	
	/**
	 * Starts server threads
	 */
	public abstract void startServers();
	
	/**
	 * Stops server threads and frees their resources
	 */
	public abstract void stopServers();
	
	/**
	 * Get the replica's current data for all servers
	 * @return {String}
	 */
	public abstract JSONObject getCurrentData();
	
	/**
	 * Set the replica's current data for all servers
	 */
	public abstract void setCurrentData(JSONObject currentData);
	
	/**
	 * Executes the request by delegating to its servers
	 * @param request
	 */
	public String executeRequest(JSONObject request) {
		String command = request.get(MessageKeys.COMMAND_TYPE).toString();
		String campusName = request.get(MessageKeys.CAMPUS).toString();
		
		CampusServerInterface selectedCampus = selectCampus(campusName);
		switch(command) {
			case Config.BOOK_ROOM:
				String studentId = request.get(MessageKeys.STUDENT_ID).toString();
				int roomNumber = Integer.parseInt(request.get(MessageKeys.ROOM_NUM).toString());
				String date = request.get(MessageKeys.DATE).toString();
				String timeSlot = request.get(MessageKeys.TIMESLOT).toString();
				return selectedCampus.bookRoom(studentId, campusName, roomNumber, date, timeSlot);
		}
		
		return null;
	}
	
	private CampusServerInterface selectCampus(String campusName) {
		return campusMap.get(campusName);
	}
}
