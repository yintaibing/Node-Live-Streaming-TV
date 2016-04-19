/*
 * @File:   RoomManager.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/18
 */

// this data structre is like:
// RoomManager: {
// 	roomId: {
// 		room: Room,
// 		publisher: User,
// 		audiences: [userId, userId, ...],
// 		rtmpConns: {
// 			streamId: RtmpConn
// 		},
// 		sockets: {
// 			socketId: Socket // transfer Danmaku and other things
// 		},
// 		public: {
// 			metaData: {},
// 			cacheAudioSeqBuf: Buffer,
// 			cacheVideoSeqBuf: Buffer
// 		}
// 	},

// 	roomId: {
// 		room: Room,
// 		publisher: User,
// 		audiences: {
// 			userId: User
// 		},
// 		rtmpConns: {
// 			streamId: RtmpConn
// 		},
// 		sockets: {
// 			socketId: Socket // transfer Danmaku and other things
// 		},
// 		public: {
// 			metaData: {},
// 			cacheAudioSeqBuf: Buffer,
// 			cacheVideoSeqBuf: Buffer
// 		}
// 	},

// 	// more
// }
