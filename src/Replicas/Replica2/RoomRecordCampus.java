package Replicas.Replica2;

import DRRS.Config;
import DRRS.MessageKeys;
import Replicas.CampusServerInterface;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RoomRecordCampus implements CampusServerInterface {
	private static final Object removeBookingLock = new Object();
	private static final Object createRoomRequestLock = new Object();
	private static final Object deleteRoomRequestLock = new Object();
	private static final Object bookRoomRequestLock = new Object();
	private static final Object cancelBookingRequestLock = new Object();
	private static final Object reduceStudentCountRequestLock = new Object();
	
	private String campus;
	private int socketPort;
	private HashMap<String, Integer> otherSocketPorts;
	public Map<String, Map<Integer, List<RoomRecordBooking>>> db;
	private Map<String, Integer> studentBookingCount;
	
	private ServerLogger logger;
	
	public RoomRecordCampus(String campus, int socketPort, HashMap<String, Integer> otherSocketPorts) throws IOException {
		this.campus = campus;
		this.socketPort = socketPort;
		this.otherSocketPorts = otherSocketPorts;
		this.db = new ConcurrentHashMap<>();
		this.studentBookingCount = new ConcurrentHashMap<>();
		this.logger = new ServerLogger(campus);
	}

	public synchronized String createRoom(String userId, int roomNumber, String date, String[] timeSlots) {
		boolean succeeded = true;
		String message = "";
		String parameters = this.buildAdminRequestParams(roomNumber, date, timeSlots);
		
		if (!this.authorizeUser(userId)) {
			message = "User " + userId + " is not authorized to execute the createRoom operation";
			this.logger.logAction("createRoom", false, message, parameters);
			return this.campus + ": " + message;
		}
		
		Map<Integer, List<RoomRecordBooking>> dateEntry = this.db.computeIfAbsent(date, k -> new ConcurrentHashMap<>());
		
		List<RoomRecordBooking> bookings = dateEntry.get(roomNumber);
		
		if (bookings == null) {
			bookings = new ArrayList<>();
			dateEntry.put(roomNumber, bookings);
			message += "Room " + roomNumber + " created on " + date;
		}
		
		String bookingsMessage = appendNewBookings(bookings, timeSlots);
		
		if (!bookingsMessage.isEmpty()) {
			if (message.isEmpty()) {
				message += "Time slots " + bookingsMessage + " added to room " + roomNumber + " on " + date;
			} else {
				message += " with time slots " + bookingsMessage;
			}
		} else {
			message += "No new time slots added to room " + roomNumber + " on " + date;
			succeeded = false;
		}
		
		this.logger.logAction("createRoom", succeeded, message, parameters);
		return this.campus + ": " + message;
	}

	public synchronized String deleteRoom(String userId, int roomNumber, String date, String[] timeSlots) {
		String message;
		String parameters = this.buildAdminRequestParams(roomNumber, date, timeSlots);
		
		if (!this.authorizeUser(userId)) {
			message = "User " + userId + " is not authorized to execute the deleteRoom operation";
			this.logger.logAction("deleteRoom", false, message, parameters);
			return this.campus + ": " + message;
		}
		
		Map<Integer, List<RoomRecordBooking>> dateEntry = this.db.get(date);
		
		if (dateEntry == null) {
			message = "Failure: There are no rooms on " + date;
			this.logger.logAction("deleteRoom", false, message, parameters);
			return message;
		}
		
		List<RoomRecordBooking> bookings = dateEntry.get(roomNumber);
		
		if (bookings == null) {
			message = "Failure: Room " + roomNumber + " does not exist on " + date;
			this.logger.logAction("deleteRoom", false, message, parameters);
			return message;
		}
		
		List<RoomRecordBooking> removedBookings = bookings.stream().filter(
				b -> Arrays.asList(timeSlots).stream().anyMatch(ts -> ts.equals(b.timeSlot))
		).collect(Collectors.toList());
		
		if (removedBookings.isEmpty()) {
			message = "Failure: Room " + roomNumber + " on " + date + " does not have these timeslots: " + timeSlots.toString();
			this.logger.logAction("deleteRoom", false, message, parameters);
			return message;
		}
		
		synchronized(removeBookingLock) {
			for (RoomRecordBooking removedBooking : removedBookings) {
				bookings.remove(removedBooking);
				if (removedBooking.bookedBy != null) {
					if (removedBooking.bookedBy.startsWith(this.campus)) {
						reduceStudentBookingCount(removedBooking.bookedBy);
					} else {
						try {
							int port = this.otherSocketPorts.get(removedBooking.bookedBy.substring(0,3));
							requestReduceStudentBookingCount(port, removedBooking.bookedBy);
						} catch(Exception e) {
							message = "Failure: " + e.getMessage();
							this.logger.logAction("deleteRoom", false, message, parameters);
							return message;
						}
					}
				}
			}
		}
		
		message = "Timeslots " + timeSlots.toString() + " removed for room " + roomNumber + " on " + date;
		this.logger.logAction("deleteRoom", true, message, parameters);
		return this.campus + ": " + message;
	}

	public synchronized String bookRoom(String userId, String campusName, int roomNumber, String date, String timeSlot) {
		String message;
		String parameters = "roomNumber=" + roomNumber + ",date=" + date + ",timeSlot=" + timeSlot;

		if (studentBookingCount.containsKey(userId) && studentBookingCount.get(userId) >= 3) {
			message = "Failure: Student " + userId + " reached the limit of 3 bookings";
			this.logger.logAction("bookRoom", false, message, parameters);
			return message;
		}

		if (!campusName.equals(this.campus)) {
			try {
				message = requestBookRoom(userId, campusName, roomNumber, date, timeSlot);
				if (!message.contains("Failure")) {
					addStudentBookingCount(userId);
				}
			} catch(Exception e) {
				message = "Failure: " + e.getMessage();
				this.logger.logAction("bookRoom", false, message, parameters);
			}
			return message;
		}

		Map<Integer, List<RoomRecordBooking>> dateEntry = this.db.get(date);

		if (dateEntry == null) {
			message = "Failure: There are no rooms on " + date;
			this.logger.logAction("bookRoom", false, message, parameters);
			return message;
		}

		List<RoomRecordBooking> bookings = dateEntry.get(roomNumber);

		if (bookings == null) {
			message = "Failure: Room " + roomNumber + " does not exist on " + date;
			this.logger.logAction("bookRoom", false, message, parameters);
			return message;
		}

		RoomRecordBooking booking = bookings.stream()
				.filter(b -> b.timeSlot.equals(timeSlot))
				.findFirst()
				.orElse(null);

		if (booking == null) {
			message = "Failure: Room " + roomNumber + " does not have the desired timeslot " + timeSlot;
			this.logger.logAction("bookRoom", false, message, parameters);
			return message;
		}

		if (booking.bookedBy != null) {
			message = "Failure: Room " + roomNumber + " is already booked at the desired timeslot " + timeSlot;
			this.logger.logAction("bookRoom", false, message, parameters);
			return message;
		}

		String bookingId = booking.book(userId, campusName, roomNumber, date, timeSlot);
		if (campusName.equals(userId.substring(0,3))) {
			addStudentBookingCount(userId);
		}

		message = "Room " + roomNumber + " booked on " + date + " for timeslot " + timeSlot + ". Booking ID: " + bookingId;

		return campusName + ": " + message;
	}

	public synchronized String getAvailableTimeSlot(String date) {
		String message = this.getCountFromDate(date);
		try {
			for (int port : this.otherSocketPorts.values()) {
				message += ", " + requestAvailableTimeSlots(port, date);
			}
		} catch(IOException e) {
			message = "Failure: Couldn't get available time slots: " + e.getMessage();
		}
		this.logger.logAction("getAvailableTimeSlot", false, message, "date=" + date);
		
		return message;
	}

	public synchronized String cancelBooking(String userId, String bookingId) {
		RoomRecordBooking booking = null;
		String message = "";
		boolean succeeded = false;
		String requestedCampus = bookingId.substring(0,3);
		
		try {
			if (!this.campus.equals(requestedCampus)) {
				int port = this.otherSocketPorts.get(requestedCampus);
				return requestCancelBooking(port, userId, bookingId);
			}
		} catch(Exception e) {
			message = "Failure: " + e.getMessage();
			this.logger.logAction("cancelBooking", succeeded, message, "userId=" + userId + ",bookingId=" + bookingId);
			return message;
		}
		for (Map<Integer, List<RoomRecordBooking>> dateEntry : this.db.values()) {
			if (booking != null) {
				break;
			}
			for (List<RoomRecordBooking> bookings : dateEntry.values()) {
				for(RoomRecordBooking b : bookings) {
					if (b.bookedBy != null && b.bookingId != null && b.bookingId.equals(bookingId)) {
						booking = b;
						break;
					}
				}
				if (booking == null) {
					continue;
				}
				if (booking.bookedBy.equals(userId)) {
					booking.cancel();
					reduceStudentBookingCount(userId);
					message = "Booking " + bookingId + " was cancelled";
					succeeded = true;
				} else {
					message = "Failure: Student " + userId + " was not the original booker, so booking " + bookingId + " was not cancelled";
				}
				break;
			}
		}
		if (booking == null) {
			message = "Failure: Booking " + bookingId + " was not found";
		}
		
		this.logger.logAction("cancelBooking", succeeded, message, "userId=" + userId + ",bookingId=" + bookingId);
		return this.campus + ": " + message;
	}

	public String changeReservation(String userId, String bookingId, String newCampusName, int newRoomNo, String newTimeSlot) {
		String parameters = "bookingId=" + bookingId + ",newCampusName=" + newCampusName + ",newRoomNo=" + newRoomNo + ",newTimeSlot=" + newTimeSlot;
		boolean isFromOtherCampus = !this.campus.equals(bookingId.substring(0, 3));
		boolean isOtherCampus = !this.campus.equals(newCampusName);
		String message;
		boolean succeeded = false;
		String bookRoomMessage;
		
		if (isOtherCampus) {
			try {
				String date = "unknown";
				if (!isFromOtherCampus) {
					OldBooking oldBooking = this.findOldBooking(bookingId, userId);
					if (oldBooking == null) {
						message = "Failure: No booking to modify with id " + bookingId + " for user " + userId;
						this.logger.logAction("changeReservation", false, message, parameters);
						return message;
					}
					date = oldBooking.date;
				}
				int port = this.otherSocketPorts.get(newCampusName);
				bookRoomMessage = this.requestBookExistingRoom(port, userId, bookingId, newRoomNo, newTimeSlot, date);
			} catch (Exception e) {
				message = "Failure: request to other server could not be completed: " + e.getMessage();
				this.logger.logAction("changeReservation", false, message, parameters);
				return message;
			}
		} else {
			OldBooking oldBooking = findOldBooking(bookingId, userId);
			if (oldBooking == null) {
				bookRoomMessage = "Failure: No booking to modify with id " + bookingId + " for user " + userId;
			} else {
				bookRoomMessage = this.bookRoom(userId, this.campus, newRoomNo, oldBooking.date, newTimeSlot);
			}
		}
		
		if (bookRoomMessage.contains("Failure")) {
			message = bookRoomMessage;
			this.logger.logAction("changeReservation", false, message, parameters);
			return message;
		}
		
		if (isFromOtherCampus) {
			try {
				int port = this.otherSocketPorts.get(newCampusName);
				this.requestCancelBooking(port, userId, bookingId);
				message = "Reservation changed successfully. " + bookRoomMessage;
				succeeded = true;
			} catch (Exception e) {
				message = "Failure: request to other server could not be completed: " + e.getMessage();
				this.logger.logAction("changeReservation", false, message, parameters);
				return message;
			}
		} else {
			OldBooking oldBooking = findOldBooking(bookingId, userId);
			if (oldBooking == null) {
				message = "Failure: No booking to modify with id " + bookingId + " for user " + userId;
			} else {
				oldBooking.booking.cancel();
				reduceStudentBookingCount(userId);
				message = "Reservation changed successfully. " + bookRoomMessage;
				succeeded = true;
			}
		}

		this.logger.logAction("changeReservation", succeeded, message, parameters);
		return message;
	}

	private class OldBooking {
		public String date;
		public RoomRecordBooking booking;

		private OldBooking (String date, RoomRecordBooking booking) {
			this.date = date;
			this.booking = booking;
		}
	}

	private synchronized OldBooking findOldBooking(String bookingId, String userId) {
		for (String date : this.db.keySet()) {
			Map<Integer, List<RoomRecordBooking>> dateEntry = this.db.get(date);
			for (List<RoomRecordBooking> bookings : dateEntry.values()) {
				for (RoomRecordBooking b : bookings) {
					if (b.bookedBy != null && b.bookingId != null && b.bookedBy.equals(userId) && b.bookingId.equals(bookingId)) {
						return new OldBooking(date, b);
					}
				}
			}
		}
		return null;
	}

	private synchronized void addStudentBookingCount(String student) {
		if (this.studentBookingCount.containsKey(student)) {
			this.studentBookingCount.put(student, this.studentBookingCount.get(student) + 1);
		} else {
			this.studentBookingCount.put(student, 1);
		}
		
		this.logger.logAction(
				"addStudentBookingCount",
				true,
				"Raised booking count to " + this.studentBookingCount.get(student) + " for student " + student,
				null
		);
	}
	
	public synchronized String reduceStudentBookingCount(String studentId) {
		if (this.studentBookingCount.containsKey(studentId)) {
			int value = this.studentBookingCount.get(studentId);
			if (value > 0) {
				this.studentBookingCount.put(studentId, value - 1);
			}
			this.logger.logAction(
					"reduceStudentBookingCount",
					true,
					"Reduced booking count to " + this.studentBookingCount.get(studentId) + " for student " + studentId,
					null
			);
		} else {
			this.studentBookingCount.put(studentId, 0);
		}
		
		return "";
	}
	
	private synchronized String appendNewBookings(List<RoomRecordBooking> bookings, String[] newTimeSlots) {
		List<String> addedTimeSlots = new ArrayList<>();
		for(String timeSlot : newTimeSlots) {
			if(bookings.stream().noneMatch(b -> b.timeSlot.equals(timeSlot))) {
				bookings.add(new RoomRecordBooking(timeSlot));
				addedTimeSlots.add(timeSlot);
			}
		}
		return addedTimeSlots.isEmpty() ? "" : addedTimeSlots.toString();
	}
	
	private synchronized String buildAdminRequestParams(int roomNumber, String date, String[] timeSlots) {
		return "roomNumber=" + roomNumber + "," +
				"date=" + date + "," +
				"timeSlots=" + Arrays.toString(timeSlots);
	}
	
	private boolean authorizeUser(String userId) {
		char userType = userId.charAt(3);
		return userType == 'A';
	}
	
	/**
	 * Receiving UDP requests
	 */
	@Override
	public void run() {
		try (DatagramSocket socket = new DatagramSocket(socketPort)) {
			while(true) {
				byte[] receivedBytes = new byte[128];
				DatagramPacket request = new DatagramPacket(receivedBytes, receivedBytes.length);
				socket.receive(request);
				
				String data = new String(receivedBytes);
				
				String replyMessage;
				
				if (data.startsWith("getAvailableTimeSlot:")) {
					String dateString = data.split("getAvailableTimeSlot:")[1].trim();
					replyMessage = this.getCountFromDate(dateString);
				} else if (data.startsWith("createRoom:")) {
					synchronized(createRoomRequestLock) {
						String[] params = data.split("createRoom:")[1].trim().split(";");
						String userId = params[0];
						String roomNumber = params[1];
						String date = params[2];
						String timeSlots = params[3];
						replyMessage = this.createRoom(userId, Integer.parseInt(roomNumber), date, timeSlots.split(","));
					}
				} else if (data.startsWith("deleteRoom:")) {
					synchronized(deleteRoomRequestLock) {
						String[] params = data.split("deleteRoom:")[1].trim().split(";");
						String userId = params[0];
						String roomNumber = params[1];
						String date = params[2];
						String timeSlots = params[3];
						replyMessage = this.deleteRoom(userId, Integer.parseInt(roomNumber), date, timeSlots.split(","));
					}
				} else if (data.startsWith("bookRoom:")) {
					synchronized(bookRoomRequestLock) {
						String[] params = data.split("bookRoom:")[1].trim().split(";");
						String userId = params[0];
						String roomNumber = params[1];
						String date = params[2];
						String timeSlot = params[3];
						replyMessage = this.bookRoom(userId, this.campus, Integer.parseInt(roomNumber), date, timeSlot);
					}
				} else if (data.startsWith("bookExistingRoom:")) {
					synchronized(bookRoomRequestLock) {
						String[] params = data.split("bookExistingRoom:")[1].trim().split(";");
						String userId = params[0];
						String bookingId = params[1];
						String date = params[4];
						if (date.equals("unknown")) {
							OldBooking oldBooking = this.findOldBooking(bookingId, userId);
							if (oldBooking == null) {
								replyMessage = "Failure. No booking found with id " + bookingId + " for user " + userId;
							} else {
								replyMessage = this.bookRoom(userId, this.campus, Integer.parseInt(params[2]), oldBooking.date, params[3]);
							}
						} else {
							replyMessage = this.bookRoom(userId, this.campus, Integer.parseInt(params[2]), date, params[3]);
						}
					}
				} else if (data.startsWith("cancelBooking:")) {
					synchronized(cancelBookingRequestLock) {
						String[] params = data.split("cancelBooking:")[1].trim().split(";");
						replyMessage = this.cancelBooking(params[0], params[1]);
					}
				} else if (data.startsWith("reduceStudentBookingCount:")) {
					synchronized(reduceStudentCountRequestLock) {
						String[] params = data.split("reduceStudentBookingCount:")[1].trim().split(";");
						replyMessage = this.reduceStudentBookingCount(params[0]);
					}
				} else {
					replyMessage = "Error: invalid request";
				}
				
				DatagramPacket reply = new DatagramPacket(replyMessage.getBytes(), replyMessage.getBytes().length, request.getAddress(), request.getPort());
				socket.send(reply);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sending UDP request for available time slots
	 */
	private synchronized String requestAvailableTimeSlots(int port, String date) throws IOException {
		byte[] requestMessage = ("getAvailableTimeSlot:" + date).getBytes();
		String replyMessage;
		synchronized (RoomRecordCampus.class) {
			try (DatagramSocket socket = new DatagramSocket()) {
				DatagramPacket request = new DatagramPacket(requestMessage, requestMessage.length, InetAddress.getByName(Config.IPAddresses.REPLICA2), port);
				socket.send(request);
				byte[] buffer = new byte[64];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				socket.receive(reply);
				replyMessage = new String(reply.getData()).trim();
			}
		}
		
		return replyMessage;
	}
	
	/**
	 * Sending UDP request for available time slots
	 */
	private synchronized String requestReduceStudentBookingCount(int port, String userId) throws IOException {
		byte[] requestMessage = ("reduceStudentBookingCount:" + userId).getBytes();
		String replyMessage;
		synchronized (RoomRecordCampus.class) {
			try (DatagramSocket socket = new DatagramSocket()) {
				DatagramPacket request = new DatagramPacket(requestMessage, requestMessage.length, InetAddress.getByName(Config.IPAddresses.REPLICA2), port);
				socket.send(request);
				byte[] buffer = new byte[64];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				socket.receive(reply);
				replyMessage = new String(reply.getData()).trim();
			}
		}
		
		return replyMessage;
	}

	/**
	 * Sending UDP request to book a room
	 */
	private synchronized String requestBookExistingRoom(int port, String userId, String bookingId, int roomNo, String timeSlot, String date) throws IOException {
		String requestMessage = "bookExistingRoom:" + userId + ";" + bookingId + ";" + roomNo + ";" + timeSlot + ";" + date;
		byte[] requestBytes = requestMessage.getBytes();
		String replyMessage;
		synchronized (RoomRecordCampus.class) {
			try (DatagramSocket socket = new DatagramSocket()) {
				DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length, InetAddress.getByName(Config.IPAddresses.REPLICA2), port);
				socket.send(request);
				byte[] buffer = new byte[128];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				socket.receive(reply);
				replyMessage = new String(reply.getData()).trim();
			}
		}

		return replyMessage;
	}
	
	/**
	 * Sending UDP request to book an existing room
	 */
	private synchronized String requestBookRoom(String userId, String campusName, int roomNumber, String date, String timeSlot) throws IOException {
		int port = this.otherSocketPorts.get(campusName);
		String requestMessage = "bookRoom:" + userId + ";" + roomNumber + ";" + date + ";" + timeSlot;
		byte[] requestBytes = requestMessage.getBytes();
		String replyMessage;
		synchronized (RoomRecordCampus.class) {
			try (DatagramSocket socket = new DatagramSocket()) {
				DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length, InetAddress.getByName(Config.IPAddresses.REPLICA2), port);
				socket.send(request);
				byte[] buffer = new byte[128];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				socket.receive(reply);
				replyMessage = new String(reply.getData()).trim();
			}
		}

		return replyMessage;
	}
	
	/**
	 * Sending UDP request to cancel a booking
	 */
	private synchronized String requestCancelBooking(int port, String userId, String bookingId) throws IOException {
		String requestMessage = "cancelBooking:" + userId + ";" + bookingId;
		byte[] requestBytes = requestMessage.getBytes();
		String replyMessage;
		synchronized (RoomRecordCampus.class) {
			try (DatagramSocket socket = new DatagramSocket()) {
				DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length, InetAddress.getByName(Config.IPAddresses.REPLICA2), port);
				socket.send(request);
				byte[] buffer = new byte[64];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				socket.receive(reply);
				replyMessage = new String(reply.getData()).trim();
			}
		}
		
		return replyMessage;
	}
	
	private synchronized String getCountFromDate(String date) {
		int nbSlots = 0;
		synchronized (this.db) {
			Map<Integer, List<RoomRecordBooking>> dateEntry = this.db.get(date);
			if (dateEntry != null) {
				for (List<RoomRecordBooking> bookings : dateEntry.values()) {
					// Counting records that are not currently booked
					nbSlots += bookings.stream().filter(b -> b.bookedBy == null).count();
				}
			}
		}
		
		return this.campus + " " + nbSlots;
	}

	@Override
	public JSONArray getRecords() {
		JSONArray jsonRecords = new JSONArray();
		
		// For each date
		for (Map.Entry<String, Map<Integer, List<RoomRecordBooking>>> dateEntry : db.entrySet()) {
			Map<Integer, List<RoomRecordBooking>> rooms = dateEntry.getValue();
			String date = dateEntry.getKey();
			// For each room in date
			for (Map.Entry<Integer, List<RoomRecordBooking>> roomEntry : rooms.entrySet()) {
				int roomNb = roomEntry.getKey();
				// For each booking in room
				for (RoomRecordBooking booking : roomEntry.getValue()) {
					JSONObject jsonRecord = new JSONObject();
					jsonRecord.put(MessageKeys.DATE, date);
					jsonRecord.put(MessageKeys.ROOM_NUM, roomNb);
					jsonRecord.put(MessageKeys.TIMESLOT, booking.timeSlot);
					jsonRecord.put(MessageKeys.BOOKING_ID, booking.bookingId);
					jsonRecord.put(MessageKeys.STUDENT_ID, booking.bookedBy);
					jsonRecords.add(jsonRecord);
				}
			}
		}
		return jsonRecords;
	}

	@Override
	public void setRecords(JSONArray records) {
		this.db = new ConcurrentHashMap<>();
		for (JSONObject record : (ArrayList<JSONObject>) records) {
			String date = record.get(MessageKeys.DATE).toString();
			String timeslot = record.get(MessageKeys.TIMESLOT).toString();
			String bookedBy = (String) record.get(MessageKeys.STUDENT_ID);
			String bookingId = (String) record.get(MessageKeys.BOOKING_ID);
			int roomNb = Integer.parseInt(record.get(MessageKeys.ROOM_NUM).toString());
			
			// Create date or room entries if not already present
			Map<Integer, List<RoomRecordBooking>> dateEntry = this.db.computeIfAbsent(date, k -> new ConcurrentHashMap<>());
			List<RoomRecordBooking> bookings = dateEntry.computeIfAbsent(roomNb, k -> new ArrayList<>());
			
			bookings.add(new RoomRecordBooking(timeslot, bookedBy, bookingId));
		}
	}
}
