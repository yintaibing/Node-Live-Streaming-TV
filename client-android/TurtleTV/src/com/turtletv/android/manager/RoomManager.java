package com.turtletv.android.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import android.util.SparseArray;

import com.turtletv.android.bean.Room;
import com.turtletv.android.util.LogUtil;

public class RoomManager {
	private static Comparator<Room> sComparator = new Comparator<Room>() {
		@Override
		public int compare(Room r1, Room r2) {
			if (r1.getIsLiving()) {
				if (!r2.getIsLiving()) {
					return -1;
				}
			} else if (r2.getIsLiving()) {
				return 1;
			}
			return r1.getId() - r2.getId();
		}
	};
	
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
		
		Collections.sort(list, sComparator);
		return list;
	}
	
	public void load(List<Room> rooms) {
		mAllRooms = rooms;
		mRooms = new SparseArray<List<Room>>();
		
		for (int i = 0; i < mAllRooms.size(); i++) {
			Room room = mAllRooms.get(i);
			List<Room> list = mRooms.get(room.getCategoryId());
			if (list == null) {
				mRooms.put(room.getCategoryId(), new LinkedList<Room>());
				list = mRooms.get(room.getCategoryId());
			}
			list.add(room);
			
			Collections.sort(list, sComparator);
		}
		
		Collections.sort(mAllRooms, sComparator);
	}
	
	public void add(Room room) {
		mAllRooms.add(room);
		List<Room> list = mRooms.get(room.getCategoryId());
		if (list == null) {
			mRooms.put(room.getCategoryId(), new LinkedList<Room>());
			list = mRooms.get(room.getCategoryId());
		}
		list.add(room);
		
		Collections.sort(list, sComparator);
	}
	
	public void clear() {
		mAllRooms.clear();
		mRooms.clear();
	}
}
