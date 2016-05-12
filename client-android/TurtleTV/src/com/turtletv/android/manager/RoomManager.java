package com.turtletv.android.manager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.util.SparseArray;

import com.turtletv.android.bean.Room;

public class RoomManager {
	private static RoomManager sInstance;
	
	public static RoomManager getInstance() {
		if (sInstance == null) {
			sInstance = new RoomManager();
		}
		return sInstance;
	}
	
	private List<Room> mAllRooms;
	private SparseArray<List<Room>> mRooms; // sorted by category the room belongs 
	
	private RoomManager() {
		mAllRooms = new ArrayList<Room>(0);
		mRooms = new SparseArray<List<Room>>();
	}
	
	public List<Room> getAllRooms() {
		return mAllRooms;
	}
	
	public List<Room> getRoomsByCategory(int categoryId) {
		return mRooms.get(categoryId);
	}
	
	public Room getRoomById(int roomId) {
		if (roomId < 0) {
			return null;
		}
		
		for (Room room : mAllRooms) {
			if (roomId == room.getId()) {
				return room;
			}
		}
		return null;
	}
	
	public Room getRoomByPublisher(int publisherId) {
		if (publisherId < 0) {
			return null;
		}
		
		for (Room room : mAllRooms) {
			if (publisherId == room.getPublisherId()) {
				return room;
			}
		}
		return null;
	}
	
	public List<Room> getLikeRooms(List<Integer> roomIds) {
		if (roomIds == null) {
			return null;
		}
		
		List<Room> list = new LinkedList<Room>();
		for (Room room : mAllRooms) {
			for (int id : roomIds) {
				if (room.getId() == id) {
					list.add(room);
					break;
				}
			}
		}
		return list;
	}
	
	public void load(List<Room> rooms) {
		mAllRooms = rooms;
		mRooms = new SparseArray<List<Room>>();
		int size = rooms.size();
		
		for (int i = 0; i < size; i++) {
			Room room = rooms.get(i);
			List<Room> list = mRooms.get(room.getCategoryId());
			if (list == null) {
				mRooms.put(room.getCategoryId(), new LinkedList<Room>());
				list = mRooms.get(room.getCategoryId());
			}
			list.add(room);
		}
	}
	
	public void add(Room room) {
		mAllRooms.add(room);
		List<Room> list = mRooms.get(room.getCategoryId());
		if (list == null) {
			mRooms.put(room.getCategoryId(), new LinkedList<Room>());
			list = mRooms.get(room.getCategoryId());
		}
		list.add(room);
	}
	
	public void clear() {
		mAllRooms.clear();
		mRooms.clear();
	}
}
