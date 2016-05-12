package com.turtletv.android.datagram;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.internal.LinkedTreeMap;
import com.turtletv.android.bean.Category;
import com.turtletv.android.bean.Danmaku;
import com.turtletv.android.bean.Room;
import com.turtletv.android.bean.User;

public class DatagramBuilder {
	@SuppressWarnings("unchecked")
	public static void convert(Datagram src, Map<String, Class<?>> strategy) {
		Iterator<String> strategyIt = strategy.keySet().iterator();
		while (strategyIt.hasNext()) {
			String key = strategyIt.next();
			if (src.containsKey(key)) {
				Object gsonObj = src.get(key);
				Class<?> clazz = strategy.get(key);

				if (gsonObj instanceof List) {
					// gson obj contains a obj list
					List<LinkedTreeMap<String, Object>> list = (List<LinkedTreeMap<String, Object>>) gsonObj;
					if (User.class.equals(clazz)) {
						List<User> result = new ArrayList<User>(list.size());
						for (LinkedTreeMap<String, Object> item : list) {
							result.add(buildUser(item));
						}
						src.put(key, result);
					} else if (Category.class.equals(clazz)) {
						List<Category> result = new ArrayList<Category>(list.size());
						for (LinkedTreeMap<String, Object> item : list) {
							result.add(buildCategory(item));
						}
						src.put(key, result);
					} else if (Room.class.equals(clazz)) {
						List<Room> result = new ArrayList<Room>(list.size());
						for (LinkedTreeMap<String, Object> item : list) {
							result.add(buildRoom(item));
						}
						src.put(key, result);
					}
				} else {
					// gsonObj is a single obj
					if (User.class.equals(clazz)) {
						User user = buildUser((LinkedTreeMap<String, Object>) gsonObj);
						src.put(key, user);
					} else if (Room.class.equals(clazz)) {
						Room room = buildRoom((LinkedTreeMap<String, Object>) gsonObj);
						src.put(key, room);
					} else if (Danmaku.class.equals(clazz)) {
						Danmaku danmaku = buildDanmaku((LinkedTreeMap<String, Object>) gsonObj);
						src.put(key, danmaku);
					} else {
						throw new RuntimeException("bad class");
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static User buildUser(LinkedTreeMap<String, Object> gsonMap) {
		User user = new User();
		user.setId(((Double) gsonMap.get(User.COL_ID)).intValue());
		user.setName((String) gsonMap.get(User.COL_NAME));
		user.setPassword((String) gsonMap.get(User.COL_PASSWORD));
		user.setCanPublish((Boolean) gsonMap.get(User.COL_CAN_PUBLISH));
		if (gsonMap.get(User.COL_PORTRAIT) != null) {
			try {
				user.setPortrait(((String) gsonMap.get(User.COL_PORTRAIT))
						.getBytes("UTF-8"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (gsonMap.get(User.COL_LIKES) != null) {
			List<Integer> likes = new LinkedList<Integer>();
			List<Double> dou = (List<Double>) gsonMap.get(User.COL_LIKES);
			for (Double roomId : dou) {
				likes.add(roomId.intValue());
			}
			user.setLikes(likes);
		}

		return user;
	}
	
	private static Danmaku buildDanmaku(LinkedTreeMap<String, Object> gsonMap) {
		Danmaku danmaku = new Danmaku();
		danmaku.setText((String) gsonMap.get(Danmaku.COL_TEXT));
		return danmaku;
	}

	private static Category buildCategory(LinkedTreeMap<String, Object> gsonMap) {
		Category category = new Category();
		category.setId(((Double) gsonMap.get(Category.COL_ID)).intValue());
		category.setName((String) gsonMap.get(Category.COL_NAME));
		category.setCoverPath((String) gsonMap.get(Category.COL_COVER_PATH));
		return category;
	}

	private static Room buildRoom(LinkedTreeMap<String, Object> gsonMap) {
		Room room = new Room();
		room.setId(((Double) gsonMap.get(Room.COL_ID)).intValue());
		room.setTitle((String) gsonMap.get(Room.COL_TITLE));
		room.setPublisherId(((Double) gsonMap.get(Room.COL_PUBLISHER_ID))
				.intValue());
		room.setPublisherName((String) gsonMap.get(Room.COL_PUBLISHER_NAME));
		room.setCategoryId(((Double) gsonMap.get(Room.COL_CATEGORY_ID))
				.intValue());
		room.setIsLiving((Boolean) gsonMap.get(Room.COL_IS_LIVING));
		Double audienceNum = (Double) gsonMap.get(Room.COL_AUDIENCE_NUM);
		room.setAudienceNum(audienceNum == null ? 0 : audienceNum.intValue());
		return room;
	}

	public static DatagramBuilder create() {
		return new DatagramBuilder();
	}

	private Datagram mDatagram;

	private DatagramBuilder() {
		mDatagram = new Datagram();
	}

	public DatagramBuilder put(String key, Object val) {
		mDatagram.put(key, val);
		return this;
	}

	public Datagram build() {
		return mDatagram;
	}
}
