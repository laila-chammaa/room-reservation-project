package DRRS;

import Replicas.CampusServerInterface;
import org.json.simple.JSONArray;
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
	public JSONObject getCurrentData() {
		JSONArray kklRecords = kklCampus.getRecords();
		JSONArray dvlRecords = dvlCampus.getRecords();
		JSONArray wstRecords = wstCampus.getRecords();
		JSONObject allRecords = new JSONObject();

		allRecords.put("KKL", kklRecords);
		allRecords.put("DVL", dvlRecords);
		allRecords.put("WST", wstRecords);

		return allRecords;
	}

	/**
	 * Set the replica's current data for all servers
	 */
	public void setCurrentData(JSONObject currentData) {
		JSONArray kklRecords = (JSONArray) currentData.get("KKL");
		JSONArray dvlRecords = (JSONArray) currentData.get("DVL");
		JSONArray wstRecords = (JSONArray) currentData.get("WST");

		kklCampus.setRecords(kklRecords);
		dvlCampus.setRecords(dvlRecords);
		wstCampus.setRecords(wstRecords);
	}

	/**
	 * Executes the request by delegating to its servers
	 * @param request
	 */
	public String executeRequest(JSONObject request) {
		String command = request.get(MessageKeys.COMMAND_TYPE).toString();
		String campusName = request.get(MessageKeys.CAMPUS).toString();
		int roomNumber;
		String adminId;
		String studentId;
		String date;
		String timeSlot;
		String timeSlots;
		String bookingId;

		try {
			switch(command) {
				case Config.CREATE_ROOM:
					adminId = request.get(MessageKeys.ADMIN_ID).toString();
					roomNumber = Integer.parseInt(request.get(MessageKeys.ROOM_NUM).toString());
					date = request.get(MessageKeys.DATE).toString();
					timeSlots = request.get(MessageKeys.TIMESLOTS).toString();
					return selectCampus(adminId.substring(0,3)).createRoom(adminId, roomNumber, date, timeSlots.split(","));
				case Config.DELETE_ROOM:
					adminId = request.get(MessageKeys.ADMIN_ID).toString();
					roomNumber = Integer.parseInt(request.get(MessageKeys.ROOM_NUM).toString());
					date = request.get(MessageKeys.DATE).toString();
					timeSlots = request.get(MessageKeys.TIMESLOTS).toString();
					return selectCampus(adminId.substring(0,3)).deleteRoom(adminId, roomNumber, date, timeSlots.split(","));
				case Config.BOOK_ROOM:
					studentId = request.get(MessageKeys.STUDENT_ID).toString();
					roomNumber = Integer.parseInt(request.get(MessageKeys.ROOM_NUM).toString());
					date = request.get(MessageKeys.DATE).toString();
					timeSlot = request.get(MessageKeys.TIMESLOT).toString();
					return selectCampus(studentId.substring(0,3)).bookRoom(studentId, campusName, roomNumber, date, timeSlot);
				case Config.GET_TIMESLOTS:
					studentId = request.get(MessageKeys.STUDENT_ID).toString();
					date = request.get(MessageKeys.DATE).toString();
					return selectCampus(studentId.substring(0,3)).getAvailableTimeSlot(date);
				case Config.CANCEL_BOOKING:
					studentId = request.get(MessageKeys.STUDENT_ID).toString();
					bookingId = request.get(MessageKeys.BOOKING_ID).toString();
					return selectCampus(studentId.substring(0,3)).cancelBooking(studentId, bookingId);
				case Config.CHANGE_RESERVATION:
					studentId = request.get(MessageKeys.STUDENT_ID).toString();
					bookingId = request.get(MessageKeys.BOOKING_ID).toString();
					roomNumber = Integer.parseInt(request.get(MessageKeys.ROOM_NUM).toString());
					timeSlot = request.get(MessageKeys.TIMESLOT).toString();
					return selectCampus(studentId.substring(0,3)).changeReservation(studentId, bookingId, campusName, roomNumber, timeSlot);
				default:
					return "INVALID_REQUEST";
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private CampusServerInterface selectCampus(String campusName) {
		return campusMap.get(campusName);
	}
}
