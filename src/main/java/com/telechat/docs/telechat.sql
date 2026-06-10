/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : telechat

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 10/06/2026 11:16:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint NOT NULL,
  `conversation_id` bigint NOT NULL,
  `seq_id` bigint NOT NULL COMMENT '会话的序列ID，用于查找该会话的聊天信息。\r\n从0开始递增',
  `sender_id` bigint NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文本内容或URL',
  `message_type` tinyint NOT NULL DEFAULT 0,
  `extra_data` json NULL COMMENT '存储图片宽高、文件大小、视频时长等',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `reply_id` bigint NULL DEFAULT NULL COMMENT '引用的消息ID',
  `client_msg_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端生成的防重ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '0:发送中, 1:已发送, 2:撤回, 3:删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_conv_id_seq_id`(`conversation_id` ASC, `seq_id` ASC) USING BTREE COMMENT '根据会话ID和序列ID便于快速定位该会话的某个消息',
  INDEX `idx_msg_conv_id_id`(`conversation_id` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_message
-- ----------------------------

-- ----------------------------
-- Table structure for contact
-- ----------------------------
DROP TABLE IF EXISTS `contact`;
CREATE TABLE `contact`  (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `friend_id` bigint NOT NULL,
  `remark` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `conversation_id` bigint NOT NULL,
  `created_Time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC, `friend_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of contact
-- ----------------------------
INSERT INTO `contact` VALUES (299206206390669312, 294218395900055552, 287965917437104128, NULL, 295323436962680832, '2026-04-05 15:45:31');
INSERT INTO `contact` VALUES (318004349848653824, 287967118450888704, 287965917437104128, NULL, 287978054603640832, '2026-05-27 12:35:45');
INSERT INTO `contact` VALUES (318004349903179776, 287965917437104128, 287967118450888704, NULL, 287978054603640832, '2026-05-27 12:35:45');

-- ----------------------------
-- Table structure for contact_apply
-- ----------------------------
DROP TABLE IF EXISTS `contact_apply`;
CREATE TABLE `contact_apply`  (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `friend_id` bigint NOT NULL,
  `status` tinyint NULL DEFAULT 0 COMMENT '0:PENDING, 1ACCEPTED:, 2:REJECTED',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_read` tinyint(1) NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC, `friend_id` ASC) USING BTREE,
  INDEX `idx_receiver_read`(`friend_id` ASC, `is_read` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of contact_apply
-- ----------------------------
INSERT INTO `contact_apply` VALUES (287967749026746368, 287965917437104128, 287967118450888704, 1, '2026-05-27 10:59:42', 1);
INSERT INTO `contact_apply` VALUES (287973671430131712, 287967118450888704, 287965917437104128, 1, '2026-05-27 12:32:27', 1);
INSERT INTO `contact_apply` VALUES (294503321484529664, 287965917437104128, 294218395900055552, 1, '2026-04-05 15:45:23', 1);
INSERT INTO `contact_apply` VALUES (294507740758413312, 294218395900055552, 287965917437104128, 1, '2026-04-05 15:02:03', 1);

-- ----------------------------
-- Table structure for conversation
-- ----------------------------
DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation`  (
  `id` bigint NOT NULL,
  `type` tinyint NOT NULL COMMENT '会话类型：0:私聊 1:群聊 2:频道',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '群名/频道名，私聊可为空',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '群/频道头像，私聊为好友的头像',
  `unique_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用于回溯旧的会话记录：P:uid1_uid2;G:gid;C:cid;',
  `owner_id` bigint NULL DEFAULT NULL COMMENT '创建者（群主/频道主）',
  `status` tinyint NULL DEFAULT 1 COMMENT '1=正常 0=解散/禁用',
  `created_time` timestamp NOT NULL,
  `updated_time` timestamp NOT NULL,
  `last_message_id` bigint NULL DEFAULT NULL,
  `last_message_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '列表页显示的预览文本',
  `last_message_time` timestamp NULL DEFAULT NULL COMMENT '用于列表排序',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_key`(`unique_key` ASC) USING BTREE COMMENT 'unique_key唯一'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of conversation
-- ----------------------------
INSERT INTO `conversation` VALUES (287978054603640832, 0, NULL, NULL, 'P:287965917437104128_287967118450888704', NULL, 1, '2026-05-27 12:35:45', '2026-05-27 12:35:45', NULL, '好友1111', '2026-05-27 12:35:45');
INSERT INTO `conversation` VALUES (291597639475138560, 1, '群聊13', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597639475138560', 287965917437104128, 1, '2026-03-15 15:44:55', '2026-03-15 15:44:55', NULL, '新群聊已创建', '2026-03-15 15:44:55');
INSERT INTO `conversation` VALUES (291597652058050560, 1, '爱如火', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597652058050560', 287965917437104128, 1, '2026-03-15 15:44:58', '2026-03-15 15:44:58', NULL, '新群聊已创建', '2026-03-15 15:44:58');
INSERT INTO `conversation` VALUES (291597669476995072, 1, '4434', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597669476995072', 287965917437104128, 1, '2026-03-15 15:45:02', '2026-03-15 15:45:02', NULL, '新群聊已创建', '2026-03-15 15:45:02');
INSERT INTO `conversation` VALUES (291597683041374208, 1, '你好啊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597683041374208', 287965917437104128, 1, '2026-03-15 15:45:05', '2026-03-15 15:45:05', NULL, '新群聊已创建', '2026-03-15 15:45:05');
INSERT INTO `conversation` VALUES (291597696278597632, 1, '成年人的群', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597696278597632', 287965917437104128, 1, '2026-03-15 15:45:08', '2026-03-15 15:45:08', NULL, '新群聊已创建', '2026-03-15 15:45:08');
INSERT INTO `conversation` VALUES (291597709515821056, 1, '群聊777', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597709515821056', 287965917437104128, 1, '2026-03-15 15:45:12', '2026-03-15 15:45:12', NULL, '新群聊已创建', '2026-03-15 15:45:12');
INSERT INTO `conversation` VALUES (291597722820153344, 1, '6666', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597722820153344', 287965917437104128, 1, '2026-03-15 15:45:15', '2026-03-15 15:45:15', NULL, '新群聊已创建', '2026-03-15 15:45:15');
INSERT INTO `conversation` VALUES (291597738863366144, 1, '1314520', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597738863366144', 287965917437104128, 1, '2026-03-15 15:45:19', '2026-03-15 15:45:19', NULL, '新群聊已创建', '2026-03-15 15:45:19');
INSERT INTO `conversation` VALUES (291597752012509184, 1, '正经群', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597752012509184', 287965917437104128, 1, '2026-03-15 15:45:22', '2026-03-15 15:45:22', NULL, '新群聊已创建', '2026-03-15 15:45:22');
INSERT INTO `conversation` VALUES (291597766684184576, 1, '加密货币', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597766684184576', 287965917437104128, 1, '2026-03-15 15:45:25', '2026-03-15 15:45:25', NULL, '新群聊已创建', '2026-03-15 15:45:25');
INSERT INTO `conversation` VALUES (291597781074841600, 1, '21', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597781074841600', 287965917437104128, 1, '2026-03-15 15:45:29', '2026-03-15 15:45:29', NULL, '新群聊已创建', '2026-03-15 15:45:29');
INSERT INTO `conversation` VALUES (291597806479740928, 1, '以太坊3', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597806479740928', 287965917437104128, 1, '2026-03-15 15:45:35', '2026-03-15 15:45:35', NULL, '新群聊已创建', '2026-03-15 15:45:35');
INSERT INTO `conversation` VALUES (291597894883086336, 1, 'SOLNA', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597894883086336', 287965917437104128, 1, '2026-03-15 15:45:56', '2026-03-15 15:45:56', NULL, '新群聊已创建', '2026-03-15 15:45:56');
INSERT INTO `conversation` VALUES (291597928336855040, 1, '比特币', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291597928336855040', 287965917437104128, 1, '2026-03-15 15:46:04', '2026-03-15 15:46:04', NULL, '新群聊已创建', '2026-03-15 15:46:04');
INSERT INTO `conversation` VALUES (291606860434378752, 1, '尼哥', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291606860434378752', 287965917437104128, 1, '2026-03-15 16:21:33', '2026-03-15 16:21:33', NULL, '新群聊已创建', '2026-03-15 16:21:33');
INSERT INTO `conversation` VALUES (291606875768754176, 1, '非洲', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291606875768754176', 287965917437104128, 1, '2026-03-15 16:21:37', '2026-03-15 16:21:37', NULL, '新群聊已创建', '2026-03-15 16:21:37');
INSERT INTO `conversation` VALUES (291606886229348352, 1, '亚洲', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291606886229348352', 287965917437104128, 1, '2026-03-15 16:21:39', '2026-03-15 16:21:39', NULL, '新群聊已创建', '2026-03-15 16:21:39');
INSERT INTO `conversation` VALUES (291606903509880832, 1, '大洋洲', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291606903509880832', 287965917437104128, 1, '2026-03-15 16:21:44', '2026-03-15 16:21:44', NULL, '新群聊已创建', '2026-03-15 16:21:44');
INSERT INTO `conversation` VALUES (291606915354595328, 1, '太平洋', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291606915354595328', 287965917437104128, 1, '2026-03-15 16:21:46', '2026-03-15 16:21:46', NULL, '新群聊已创建', '2026-03-15 16:21:46');
INSERT INTO `conversation` VALUES (291606940096794624, 1, '中国', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291606940096794624', 287965917437104128, 1, '2026-03-15 16:21:52', '2026-03-15 16:21:52', NULL, '新群聊已创建', '2026-03-15 16:21:52');
INSERT INTO `conversation` VALUES (291611367440519168, 1, '日本', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291611367440519168', 287965917437104128, 1, '2026-03-15 16:39:28', '2026-03-15 16:39:28', NULL, '新群聊已创建', '2026-03-15 16:39:28');
INSERT INTO `conversation` VALUES (291612630030880768, 1, '米国', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291612630030880768', 287965917437104128, 1, '2026-03-15 16:44:29', '2026-03-15 16:44:29', NULL, '新群聊已创建', '2026-03-15 16:44:29');
INSERT INTO `conversation` VALUES (291613068025270272, 1, '呵呵呵', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291613068025270272', 287965917437104128, 1, '2026-03-15 16:46:13', '2026-03-15 16:46:13', NULL, '新群聊已创建', '2026-03-15 16:46:13');
INSERT INTO `conversation` VALUES (291621543891570688, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291621543891570688', 287965917437104128, 1, '2026-03-15 17:19:54', '2026-03-15 17:19:54', NULL, '新群聊已创建', '2026-03-15 17:19:54');
INSERT INTO `conversation` VALUES (291692352827953152, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291692352827953152', 287967118450888704, 1, '2026-03-15 22:01:16', '2026-03-15 22:01:16', NULL, '新群聊已创建', '2026-03-15 22:01:16');
INSERT INTO `conversation` VALUES (291864573861040128, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291864573861040128', 287965917437104128, 1, '2026-03-16 09:25:37', '2026-03-16 09:25:37', NULL, '新群聊已创建', '2026-03-16 09:25:37');
INSERT INTO `conversation` VALUES (291865371303088128, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291865371303088128', 287965917437104128, 1, '2026-03-16 09:28:47', '2026-03-16 09:28:47', NULL, '新群聊已创建', '2026-03-16 09:28:47');
INSERT INTO `conversation` VALUES (291865639939870720, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:291865639939870720', 287965917437104128, 1, '2026-03-16 09:29:51', '2026-03-16 09:29:51', NULL, '新群聊已创建', '2026-03-16 09:29:51');
INSERT INTO `conversation` VALUES (292037579098951680, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:292037579098951680', 287965917437104128, 1, '2026-03-16 20:53:05', '2026-03-16 20:53:05', NULL, '新群聊已创建', '2026-03-16 20:53:05');
INSERT INTO `conversation` VALUES (292037774331219968, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:292037774331219968', 287965917437104128, 1, '2026-03-16 20:53:51', '2026-03-16 20:53:51', NULL, '新群聊已创建', '2026-03-16 20:53:51');
INSERT INTO `conversation` VALUES (292402929640542208, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:292402929640542208', 287965917437104128, 1, '2026-03-17 21:04:51', '2026-03-17 21:04:51', NULL, '新群聊已创建', '2026-03-17 21:04:51');
INSERT INTO `conversation` VALUES (292404547048050688, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:292404547048050688', 287965917437104128, 1, '2026-03-17 21:11:17', '2026-03-17 21:11:17', NULL, '新群聊已创建', '2026-03-17 21:11:17');
INSERT INTO `conversation` VALUES (292406155538468864, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:292406155538468864', 287965917437104128, 1, '2026-03-17 21:17:40', '2026-03-17 21:17:40', NULL, '新群聊已创建', '2026-03-17 21:17:40');
INSERT INTO `conversation` VALUES (292413121757319168, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:292413121757319168', 287965917437104128, 1, '2026-03-17 21:45:21', '2026-03-17 21:45:21', NULL, '新群聊已创建', '2026-03-17 21:45:21');
INSERT INTO `conversation` VALUES (292415652923314176, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:292415652923314176', 287965917437104128, 1, '2026-03-17 21:55:24', '2026-03-17 21:55:24', NULL, '新群聊已创建', '2026-03-17 21:55:24');
INSERT INTO `conversation` VALUES (292438956270817280, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:292438956270817280', 287965917437104128, 1, '2026-03-17 23:28:00', '2026-03-17 23:28:00', NULL, '新群聊已创建', '2026-03-17 23:28:00');
INSERT INTO `conversation` VALUES (292439220939788288, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:292439220939788288', 287965917437104128, 1, '2026-03-17 23:29:04', '2026-03-17 23:29:04', NULL, '新群聊已创建', '2026-03-17 23:29:04');
INSERT INTO `conversation` VALUES (292439274110980096, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:292439274110980096', 287965917437104128, 1, '2026-03-17 23:29:16', '2026-03-17 23:29:16', NULL, '新群聊已创建', '2026-03-17 23:29:16');
INSERT INTO `conversation` VALUES (292439500351737856, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:292439500351737856', 287965917437104128, 1, '2026-03-17 23:30:10', '2026-03-17 23:30:10', NULL, '新群聊已创建', '2026-03-17 23:30:10');
INSERT INTO `conversation` VALUES (294496845399461888, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294496845399461888', 287965917437104128, 1, '2026-03-23 15:45:19', '2026-03-23 15:45:19', NULL, '新群聊已创建', '2026-03-23 15:45:19');
INSERT INTO `conversation` VALUES (294497390101139456, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294497390101139456', 287965917437104128, 1, '2026-03-23 15:47:29', '2026-03-23 15:47:29', NULL, '新群聊已创建', '2026-03-23 15:47:29');
INSERT INTO `conversation` VALUES (294498735667417088, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294498735667417088', 287965917437104128, 1, '2026-03-23 15:52:50', '2026-03-23 15:52:50', NULL, '新群聊已创建', '2026-03-23 15:52:50');
INSERT INTO `conversation` VALUES (294498905322819584, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294498905322819584', 287965917437104128, 1, '2026-03-23 15:53:31', '2026-03-23 15:53:31', NULL, '新群聊已创建', '2026-03-23 15:53:31');
INSERT INTO `conversation` VALUES (294503716613132288, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294503716613132288', 287965917437104128, 1, '2026-03-23 16:12:38', '2026-03-23 16:12:38', NULL, '新群聊已创建', '2026-03-23 16:12:38');
INSERT INTO `conversation` VALUES (294504071891652608, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294504071891652608', 287965917437104128, 1, '2026-03-23 16:14:02', '2026-03-23 16:14:02', NULL, '新群聊已创建', '2026-03-23 16:14:02');
INSERT INTO `conversation` VALUES (294504202418393088, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294504202418393088', 287965917437104128, 1, '2026-03-23 16:14:33', '2026-03-23 16:14:33', NULL, '新群聊已创建', '2026-03-23 16:14:33');
INSERT INTO `conversation` VALUES (294504354222837760, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294504354222837760', 287965917437104128, 1, '2026-03-23 16:15:10', '2026-03-23 16:15:10', NULL, '新群聊已创建', '2026-03-23 16:15:10');
INSERT INTO `conversation` VALUES (294504526151553024, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294504526151553024', 287965917437104128, 1, '2026-03-23 16:15:51', '2026-03-23 16:15:51', NULL, '新群聊已创建', '2026-03-23 16:15:51');
INSERT INTO `conversation` VALUES (294504787062427648, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294504787062427648', 287965917437104128, 1, '2026-03-23 16:16:53', '2026-03-23 16:16:53', NULL, '新群聊已创建', '2026-03-23 16:16:53');
INSERT INTO `conversation` VALUES (294505599452975104, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294505599452975104', 287965917437104128, 1, '2026-03-23 16:20:07', '2026-03-23 16:20:07', NULL, '新群聊已创建', '2026-03-23 16:20:07');
INSERT INTO `conversation` VALUES (294506434153025536, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294506434153025536', 287965917437104128, 1, '2026-03-23 16:23:26', '2026-03-23 16:23:26', NULL, '新群聊已创建', '2026-03-23 16:23:26');
INSERT INTO `conversation` VALUES (294506571583590400, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294506571583590400', 287965917437104128, 1, '2026-03-23 16:23:58', '2026-03-23 16:23:58', NULL, '新群聊已创建', '2026-03-23 16:23:58');
INSERT INTO `conversation` VALUES (294506960072609792, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294506960072609792', 287965917437104128, 1, '2026-03-23 16:25:31', '2026-03-23 16:25:31', NULL, '新群聊已创建', '2026-03-23 16:25:31');
INSERT INTO `conversation` VALUES (294507068625391616, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294507068625391616', 287965917437104128, 1, '2026-03-23 16:25:57', '2026-03-23 16:25:57', NULL, '新群聊已创建', '2026-03-23 16:25:57');
INSERT INTO `conversation` VALUES (294507124699041792, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294507124699041792', 287965917437104128, 1, '2026-03-23 16:26:10', '2026-03-23 16:26:10', NULL, '新群聊已创建', '2026-03-23 16:26:10');
INSERT INTO `conversation` VALUES (294558142556147712, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:294558142556147712', 287965917437104128, 1, '2026-03-23 19:48:54', '2026-03-23 19:48:54', NULL, '新群聊已创建', '2026-03-23 19:48:54');
INSERT INTO `conversation` VALUES (295323436962680832, 0, NULL, NULL, 'P:287965917437104128_294218395900055552', NULL, 1, '2026-03-25 22:29:54', '2026-03-25 22:29:54', NULL, '我们已经添加好友，开始聊天吧!', '2026-03-25 22:29:54');
INSERT INTO `conversation` VALUES (295665671445024768, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:295665671445024768', 287965917437104128, 1, '2026-03-26 21:09:49', '2026-03-26 21:09:49', NULL, '新群聊已创建', '2026-03-26 21:09:49');
INSERT INTO `conversation` VALUES (298991529433894912, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:298991529433894912', 287965917437104128, 1, '2026-04-05 01:25:36', '2026-04-05 01:25:36', NULL, '新群聊已创建', '2026-04-05 01:25:36');
INSERT INTO `conversation` VALUES (317798554473205760, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:317798554473205760', 287967118450888704, 1, '2026-05-26 22:58:00', '2026-05-26 22:58:00', NULL, '新群聊已创建', '2026-05-26 22:58:00');
INSERT INTO `conversation` VALUES (317798587872448512, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:317798587872448512', 287965917437104128, 1, '2026-05-26 22:58:08', '2026-05-26 22:58:08', NULL, '新群聊已创建', '2026-05-26 22:58:08');
INSERT INTO `conversation` VALUES (317983220870287360, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:317983220870287360', 287967118450888704, 1, '2026-05-27 11:11:48', '2026-05-27 11:11:48', NULL, '新群聊已创建', '2026-05-27 11:11:48');
INSERT INTO `conversation` VALUES (317983445173276672, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:317983445173276672', 287965917437104128, 1, '2026-05-27 11:12:41', '2026-05-27 11:12:41', NULL, '新群聊已创建', '2026-05-27 11:12:41');
INSERT INTO `conversation` VALUES (317995787663577088, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:317995787663577088', 287967118450888704, 1, '2026-05-27 12:01:44', '2026-05-27 12:01:44', NULL, '新群聊已创建', '2026-05-27 12:01:44');
INSERT INTO `conversation` VALUES (318003052328128512, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:318003052328128512', 287965917437104128, 1, '2026-05-27 12:30:36', '2026-05-27 12:30:36', NULL, '新群聊已创建', '2026-05-27 12:30:36');
INSERT INTO `conversation` VALUES (318016211600412672, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:318016211600412672', 287965917437104128, 1, '2026-05-27 13:22:53', '2026-05-27 13:22:53', NULL, '新群聊已创建', '2026-05-27 13:22:53');
INSERT INTO `conversation` VALUES (318016235298230272, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:318016235298230272', 287965917437104128, 1, '2026-05-27 13:22:59', '2026-05-27 13:22:59', NULL, '新群聊已创建', '2026-05-27 13:22:59');
INSERT INTO `conversation` VALUES (318016247587540992, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:318016247587540992', 287965917437104128, 1, '2026-05-27 13:23:02', '2026-05-27 13:23:02', NULL, '新群聊已创建', '2026-05-27 13:23:02');
INSERT INTO `conversation` VALUES (318016260355002368, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:318016260355002368', 287965917437104128, 1, '2026-05-27 13:23:05', '2026-05-27 13:23:05', NULL, '新群聊已创建', '2026-05-27 13:23:05');
INSERT INTO `conversation` VALUES (318016274087153664, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:318016274087153664', 287965917437104128, 1, '2026-05-27 13:23:08', '2026-05-27 13:23:08', NULL, '新群聊已创建', '2026-05-27 13:23:08');
INSERT INTO `conversation` VALUES (318016285118173184, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:318016285118173184', 287965917437104128, 1, '2026-05-27 13:23:11', '2026-05-27 13:23:11', NULL, '新群聊已创建', '2026-05-27 13:23:11');
INSERT INTO `conversation` VALUES (318016318651633664, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:318016318651633664', 287965917437104128, 1, '2026-05-27 13:23:19', '2026-05-27 13:23:19', NULL, '新群聊已创建', '2026-05-27 13:23:19');
INSERT INTO `conversation` VALUES (318016372871401472, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:318016372871401472', 287965917437104128, 1, '2026-05-27 13:23:32', '2026-05-27 13:23:32', NULL, '新群聊已创建', '2026-05-27 13:23:32');
INSERT INTO `conversation` VALUES (318018805639024640, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:318018805639024640', 287965917437104128, 1, '2026-05-27 13:33:12', '2026-05-27 13:33:12', NULL, '新群聊已创建', '2026-05-27 13:33:12');
INSERT INTO `conversation` VALUES (323057076479856640, 1, '群聊', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/default_group_avatar.jpg', 'G:323057076479856640', 287965917437104128, 1, '2026-06-10 11:13:29', '2026-06-10 11:13:29', NULL, '新群聊已创建', '2026-06-10 11:13:29');

-- ----------------------------
-- Table structure for conversation_member
-- ----------------------------
DROP TABLE IF EXISTS `conversation_member`;
CREATE TABLE `conversation_member`  (
  `id` bigint NOT NULL,
  `conversation_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` tinyint NOT NULL DEFAULT 2 COMMENT '0:OWNER, 1:ADMIN, 2:MEMBER',
  `is_muted` tinyint(1) NULL DEFAULT 0 COMMENT '是否免打扰会话',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除会话',
  `is_toped` tinyint(1) NULL DEFAULT 0 COMMENT '是否置顶会话',
  `last_read_message_id` bigint NULL DEFAULT 0 COMMENT '最后已读消息',
  `joined_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unidx_user_conversation`(`user_id` ASC, `conversation_id` ASC) USING BTREE COMMENT '唯一成员',
  INDEX `idx_user_conversation`(`user_id` ASC, `conversation_id` ASC) USING BTREE COMMENT '优化查询'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of conversation_member
-- ----------------------------
INSERT INTO `conversation_member` VALUES (291597639525470208, 291597639475138560, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:44:55');
INSERT INTO `conversation_member` VALUES (291597639525470209, 291597639475138560, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:44:55');
INSERT INTO `conversation_member` VALUES (291597652070633472, 291597652058050560, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:44:58');
INSERT INTO `conversation_member` VALUES (291597652070633473, 291597652058050560, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:44:58');
INSERT INTO `conversation_member` VALUES (291597669489577984, 291597669476995072, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:45:02');
INSERT INTO `conversation_member` VALUES (291597669489577985, 291597669476995072, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:45:02');
INSERT INTO `conversation_member` VALUES (291597683058151424, 291597683041374208, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:45:05');
INSERT INTO `conversation_member` VALUES (291597683058151425, 291597683041374208, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:45:05');
INSERT INTO `conversation_member` VALUES (291597696291180544, 291597696278597632, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:45:08');
INSERT INTO `conversation_member` VALUES (291597696291180545, 291597696278597632, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:45:08');
INSERT INTO `conversation_member` VALUES (291597709528403968, 291597709515821056, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:45:12');
INSERT INTO `conversation_member` VALUES (291597709528403969, 291597709515821056, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:45:12');
INSERT INTO `conversation_member` VALUES (291597722832736256, 291597722820153344, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:45:15');
INSERT INTO `conversation_member` VALUES (291597722832736257, 291597722820153344, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:45:15');
INSERT INTO `conversation_member` VALUES (291597738875949056, 291597738863366144, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:45:19');
INSERT INTO `conversation_member` VALUES (291597738875949057, 291597738863366144, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:45:19');
INSERT INTO `conversation_member` VALUES (291597752020897792, 291597752012509184, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:45:22');
INSERT INTO `conversation_member` VALUES (291597752020897793, 291597752012509184, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:45:22');
INSERT INTO `conversation_member` VALUES (291597766692573184, 291597766684184576, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:45:25');
INSERT INTO `conversation_member` VALUES (291597766692573185, 291597766684184576, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:45:25');
INSERT INTO `conversation_member` VALUES (291597781087424512, 291597781074841600, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:45:29');
INSERT INTO `conversation_member` VALUES (291597781087424513, 291597781074841600, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:45:29');
INSERT INTO `conversation_member` VALUES (291597806492323840, 291597806479740928, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:45:35');
INSERT INTO `conversation_member` VALUES (291597806492323841, 291597806479740928, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:45:35');
INSERT INTO `conversation_member` VALUES (291597894895669248, 291597894883086336, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:45:56');
INSERT INTO `conversation_member` VALUES (291597894895669249, 291597894883086336, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:45:56');
INSERT INTO `conversation_member` VALUES (291597928353632256, 291597928336855040, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 15:46:04');
INSERT INTO `conversation_member` VALUES (291597928353632257, 291597928336855040, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 15:46:04');
INSERT INTO `conversation_member` VALUES (291606860488904704, 291606860434378752, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 16:21:33');
INSERT INTO `conversation_member` VALUES (291606860488904705, 291606860434378752, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 16:21:33');
INSERT INTO `conversation_member` VALUES (291606875810697216, 291606875768754176, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 16:21:37');
INSERT INTO `conversation_member` VALUES (291606875810697217, 291606875768754176, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 16:21:37');
INSERT INTO `conversation_member` VALUES (291606886237736960, 291606886229348352, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 16:21:39');
INSERT INTO `conversation_member` VALUES (291606886237736961, 291606886229348352, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 16:21:39');
INSERT INTO `conversation_member` VALUES (291606903522463744, 291606903509880832, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 16:21:44');
INSERT INTO `conversation_member` VALUES (291606903522463745, 291606903509880832, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 16:21:44');
INSERT INTO `conversation_member` VALUES (291606915362983936, 291606915354595328, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 16:21:46');
INSERT INTO `conversation_member` VALUES (291606915362983937, 291606915354595328, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 16:21:46');
INSERT INTO `conversation_member` VALUES (291606940105183232, 291606940096794624, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 16:21:52');
INSERT INTO `conversation_member` VALUES (291606940105183233, 291606940096794624, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 16:21:52');
INSERT INTO `conversation_member` VALUES (291611367549571072, 291611367440519168, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 16:39:28');
INSERT INTO `conversation_member` VALUES (291611367549571073, 291611367440519168, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 16:39:28');
INSERT INTO `conversation_member` VALUES (291612630047657984, 291612630030880768, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 16:44:29');
INSERT INTO `conversation_member` VALUES (291612630047657985, 291612630030880768, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 16:44:29');
INSERT INTO `conversation_member` VALUES (291613068088184832, 291613068025270272, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 16:46:13');
INSERT INTO `conversation_member` VALUES (291613068088184833, 291613068025270272, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 16:46:13');
INSERT INTO `conversation_member` VALUES (291621543950290944, 291621543891570688, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-15 17:19:54');
INSERT INTO `conversation_member` VALUES (291621543950290945, 291621543891570688, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-15 17:19:54');
INSERT INTO `conversation_member` VALUES (291692352869896192, 291692352827953152, 287965917437104128, 2, 0, 0, 0, NULL, '2026-03-15 22:01:16');
INSERT INTO `conversation_member` VALUES (291692352869896193, 291692352827953152, 287967118450888704, 0, 0, 0, 0, NULL, '2026-03-15 22:01:16');
INSERT INTO `conversation_member` VALUES (291864573902983168, 291864573861040128, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-16 09:25:37');
INSERT INTO `conversation_member` VALUES (291864573902983169, 291864573861040128, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-16 09:25:37');
INSERT INTO `conversation_member` VALUES (291865371395362816, 291865371303088128, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-16 09:28:47');
INSERT INTO `conversation_member` VALUES (291865371395362817, 291865371303088128, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-16 09:28:47');
INSERT INTO `conversation_member` VALUES (291865640082477056, 291865639939870720, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-16 09:29:51');
INSERT INTO `conversation_member` VALUES (291865640082477057, 291865639939870720, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-16 09:29:51');
INSERT INTO `conversation_member` VALUES (292037579174449152, 292037579098951680, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-16 20:53:05');
INSERT INTO `conversation_member` VALUES (292037579174449153, 292037579098951680, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-16 20:53:05');
INSERT INTO `conversation_member` VALUES (292037774343802880, 292037774331219968, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-16 20:53:51');
INSERT INTO `conversation_member` VALUES (292037774343802881, 292037774331219968, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-16 20:53:51');
INSERT INTO `conversation_member` VALUES (292402929682485248, 292402929640542208, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-17 21:04:51');
INSERT INTO `conversation_member` VALUES (292402929682485249, 292402929640542208, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-17 21:04:51');
INSERT INTO `conversation_member` VALUES (292404547261960192, 292404547048050688, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-17 21:11:17');
INSERT INTO `conversation_member` VALUES (292404547261960193, 292404547048050688, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-17 21:11:17');
INSERT INTO `conversation_member` VALUES (292406156058562560, 292406155538468864, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-17 21:17:40');
INSERT INTO `conversation_member` VALUES (292406156058562561, 292406155538468864, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-17 21:17:40');
INSERT INTO `conversation_member` VALUES (292413121828622336, 292413121757319168, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-17 21:45:21');
INSERT INTO `conversation_member` VALUES (292413121828622337, 292413121757319168, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-17 21:45:21');
INSERT INTO `conversation_member` VALUES (292415653019783168, 292415652923314176, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-17 21:55:24');
INSERT INTO `conversation_member` VALUES (292415653019783169, 292415652923314176, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-17 21:55:24');
INSERT INTO `conversation_member` VALUES (292438956690247680, 292438956270817280, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-17 23:28:00');
INSERT INTO `conversation_member` VALUES (292438956690247681, 292438956270817280, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-17 23:28:00');
INSERT INTO `conversation_member` VALUES (292439221136920576, 292439220939788288, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-17 23:29:04');
INSERT INTO `conversation_member` VALUES (292439221136920577, 292439220939788288, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-17 23:29:04');
INSERT INTO `conversation_member` VALUES (292439274131951616, 292439274110980096, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-17 23:29:16');
INSERT INTO `conversation_member` VALUES (292439274131951617, 292439274110980096, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-17 23:29:16');
INSERT INTO `conversation_member` VALUES (292439500360126464, 292439500351737856, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-17 23:30:10');
INSERT INTO `conversation_member` VALUES (292439500360126465, 292439500351737856, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-17 23:30:10');
INSERT INTO `conversation_member` VALUES (294496845613371392, 294496845399461888, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 15:45:19');
INSERT INTO `conversation_member` VALUES (294496845613371393, 294496845399461888, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 15:45:19');
INSERT INTO `conversation_member` VALUES (294496845613371394, 294496845399461888, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-23 15:45:19');
INSERT INTO `conversation_member` VALUES (294497390109528064, 294497390101139456, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 15:47:29');
INSERT INTO `conversation_member` VALUES (294497390109528065, 294497390101139456, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 15:47:29');
INSERT INTO `conversation_member` VALUES (294497390109528066, 294497390101139456, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-23 15:47:29');
INSERT INTO `conversation_member` VALUES (294498735675805696, 294498735667417088, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 15:52:50');
INSERT INTO `conversation_member` VALUES (294498735675805697, 294498735667417088, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 15:52:50');
INSERT INTO `conversation_member` VALUES (294498905343791104, 294498905322819584, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 15:53:31');
INSERT INTO `conversation_member` VALUES (294498905343791105, 294498905322819584, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 15:53:31');
INSERT INTO `conversation_member` VALUES (294498905343791106, 294498905322819584, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-23 15:53:31');
INSERT INTO `conversation_member` VALUES (294503716629909504, 294503716613132288, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:12:38');
INSERT INTO `conversation_member` VALUES (294503716629909505, 294503716613132288, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:12:38');
INSERT INTO `conversation_member` VALUES (294504071904235520, 294504071891652608, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:14:02');
INSERT INTO `conversation_member` VALUES (294504071904235521, 294504071891652608, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:14:02');
INSERT INTO `conversation_member` VALUES (294504202430976000, 294504202418393088, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:14:33');
INSERT INTO `conversation_member` VALUES (294504202430976001, 294504202418393088, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:14:33');
INSERT INTO `conversation_member` VALUES (294504354231226368, 294504354222837760, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:15:10');
INSERT INTO `conversation_member` VALUES (294504354231226369, 294504354222837760, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:15:10');
INSERT INTO `conversation_member` VALUES (294504354231226370, 294504354222837760, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-23 16:15:10');
INSERT INTO `conversation_member` VALUES (294504526159941632, 294504526151553024, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:15:51');
INSERT INTO `conversation_member` VALUES (294504526159941633, 294504526151553024, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:15:51');
INSERT INTO `conversation_member` VALUES (294504787066621952, 294504787062427648, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:16:53');
INSERT INTO `conversation_member` VALUES (294504787066621953, 294504787062427648, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:16:53');
INSERT INTO `conversation_member` VALUES (294505599465558016, 294505599452975104, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:20:07');
INSERT INTO `conversation_member` VALUES (294505599465558017, 294505599452975104, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:20:07');
INSERT INTO `conversation_member` VALUES (294506434161414144, 294506434153025536, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:23:26');
INSERT INTO `conversation_member` VALUES (294506434161414145, 294506434153025536, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:23:26');
INSERT INTO `conversation_member` VALUES (294506571587784704, 294506571583590400, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:23:58');
INSERT INTO `conversation_member` VALUES (294506571587784705, 294506571583590400, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:23:58');
INSERT INTO `conversation_member` VALUES (294506960080998400, 294506960072609792, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:25:31');
INSERT INTO `conversation_member` VALUES (294506960080998401, 294506960072609792, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:25:31');
INSERT INTO `conversation_member` VALUES (294506960080998402, 294506960072609792, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-23 16:25:31');
INSERT INTO `conversation_member` VALUES (294507068629585920, 294507068625391616, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:25:57');
INSERT INTO `conversation_member` VALUES (294507068629585921, 294507068625391616, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:25:57');
INSERT INTO `conversation_member` VALUES (294507068629585922, 294507068625391616, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-23 16:25:57');
INSERT INTO `conversation_member` VALUES (294507124711624704, 294507124699041792, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 16:26:10');
INSERT INTO `conversation_member` VALUES (294507124711624705, 294507124699041792, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 16:26:10');
INSERT INTO `conversation_member` VALUES (294507124711624706, 294507124699041792, 287967118450888704, 2, 0, 0, 0, NULL, '2026-03-23 16:26:10');
INSERT INTO `conversation_member` VALUES (294558142564536320, 294558142556147712, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-23 19:48:54');
INSERT INTO `conversation_member` VALUES (294558142564536321, 294558142556147712, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-23 19:48:54');
INSERT INTO `conversation_member` VALUES (295665671465996288, 295665671445024768, 287965917437104128, 0, 0, 0, 0, NULL, '2026-03-26 21:09:49');
INSERT INTO `conversation_member` VALUES (295665671465996289, 295665671445024768, 294218395900055552, 2, 0, 0, 0, NULL, '2026-03-26 21:09:49');
INSERT INTO `conversation_member` VALUES (298991529450672128, 298991529433894912, 287965917437104128, 0, 0, 0, 0, NULL, '2026-04-05 01:25:36');
INSERT INTO `conversation_member` VALUES (298991529450672129, 298991529433894912, 294218395900055552, 2, 0, 0, 0, NULL, '2026-04-05 01:25:36');
INSERT INTO `conversation_member` VALUES (299206206399057920, 295323436962680832, 294218395900055552, 2, 0, 0, 0, 0, '2026-04-05 15:45:31');
INSERT INTO `conversation_member` VALUES (317798554494177280, 317798554473205760, 287965917437104128, 2, 0, 0, 0, NULL, '2026-05-26 22:58:00');
INSERT INTO `conversation_member` VALUES (317798554494177281, 317798554473205760, 287967118450888704, 0, 0, 0, 0, NULL, '2026-05-26 22:58:00');
INSERT INTO `conversation_member` VALUES (317798587889225728, 317798587872448512, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-26 22:58:08');
INSERT INTO `conversation_member` VALUES (317798587889225729, 317798587872448512, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-26 22:58:08');
INSERT INTO `conversation_member` VALUES (317983220933201920, 317983220870287360, 287965917437104128, 2, 0, 0, 0, NULL, '2026-05-27 11:11:48');
INSERT INTO `conversation_member` VALUES (317983220933201921, 317983220870287360, 287967118450888704, 0, 0, 0, 0, NULL, '2026-05-27 11:11:48');
INSERT INTO `conversation_member` VALUES (317983445190053888, 317983445173276672, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-27 11:12:41');
INSERT INTO `conversation_member` VALUES (317983445190053889, 317983445173276672, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-27 11:12:41');
INSERT INTO `conversation_member` VALUES (317995787730685952, 317995787663577088, 287965917437104128, 2, 0, 0, 0, NULL, '2026-05-27 12:01:44');
INSERT INTO `conversation_member` VALUES (317995787730685953, 317995787663577088, 287967118450888704, 0, 0, 0, 0, NULL, '2026-05-27 12:01:44');
INSERT INTO `conversation_member` VALUES (318003052399431680, 318003052328128512, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-27 12:30:36');
INSERT INTO `conversation_member` VALUES (318003052399431681, 318003052328128512, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-27 12:30:36');
INSERT INTO `conversation_member` VALUES (318004349953511424, 287978054603640832, 287967118450888704, 2, 0, 0, 0, 0, '2026-05-27 12:35:45');
INSERT INTO `conversation_member` VALUES (318004350020620288, 287978054603640832, 287965917437104128, 2, 0, 0, 0, 0, '2026-05-27 12:35:45');
INSERT INTO `conversation_member` VALUES (318016211705270272, 318016211600412672, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-27 13:22:53');
INSERT INTO `conversation_member` VALUES (318016211705270273, 318016211600412672, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-27 13:22:53');
INSERT INTO `conversation_member` VALUES (318016235315007488, 318016235298230272, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-27 13:22:59');
INSERT INTO `conversation_member` VALUES (318016235315007489, 318016235298230272, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-27 13:22:59');
INSERT INTO `conversation_member` VALUES (318016247600123904, 318016247587540992, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-27 13:23:02');
INSERT INTO `conversation_member` VALUES (318016247600123905, 318016247587540992, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-27 13:23:02');
INSERT INTO `conversation_member` VALUES (318016260367585280, 318016260355002368, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-27 13:23:05');
INSERT INTO `conversation_member` VALUES (318016260367585281, 318016260355002368, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-27 13:23:05');
INSERT INTO `conversation_member` VALUES (318016274116513792, 318016274087153664, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-27 13:23:08');
INSERT INTO `conversation_member` VALUES (318016274116513793, 318016274087153664, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-27 13:23:08');
INSERT INTO `conversation_member` VALUES (318016285130756096, 318016285118173184, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-27 13:23:11');
INSERT INTO `conversation_member` VALUES (318016285130756097, 318016285118173184, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-27 13:23:11');
INSERT INTO `conversation_member` VALUES (318016318660022272, 318016318651633664, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-27 13:23:19');
INSERT INTO `conversation_member` VALUES (318016318660022273, 318016318651633664, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-27 13:23:19');
INSERT INTO `conversation_member` VALUES (318016372883984384, 318016372871401472, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-27 13:23:32');
INSERT INTO `conversation_member` VALUES (318016372883984385, 318016372871401472, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-27 13:23:32');
INSERT INTO `conversation_member` VALUES (318018805655801856, 318018805639024640, 287965917437104128, 0, 0, 0, 0, NULL, '2026-05-27 13:33:12');
INSERT INTO `conversation_member` VALUES (318018805655801857, 318018805639024640, 287967118450888704, 2, 0, 0, 0, NULL, '2026-05-27 13:33:12');
INSERT INTO `conversation_member` VALUES (323057076580519936, 323057076479856640, 287965917437104128, 0, 0, 0, 0, NULL, '2026-06-10 11:13:29');
INSERT INTO `conversation_member` VALUES (323057076580519937, 323057076479856640, 287967118450888704, 2, 0, 0, 0, NULL, '2026-06-10 11:13:29');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL,
  `username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `nickname` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像URL',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别, 1男，2女,0不愿透露',
  `bio` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户简介',
  `status` tinyint NULL DEFAULT 1 COMMENT '用户状态, 1正常，0封禁',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `last_login_time` timestamp NULL DEFAULT NULL COMMENT '最后登录时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_pk_username`(`username` ASC) USING BTREE,
  INDEX `idx_username`(`nickname` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (287965917437104128, 'user_322683001', '那不勒斯的老大', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/ac0c1bb9-f842-4f44-89b9-c315afa16432.jpg', 1, '秧歌斯达', 1, '2026-03-05 15:13:45', '2026-03-05 15:13:45', '2026-05-27 12:29:51');
INSERT INTO `user` VALUES (287967118450888704, 'user_130534778', '七友', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/355211d7-80c9-456a-a934-db072085c23f.png', 1, '你好，世界！', 1, '2026-03-05 15:18:31', '2026-03-05 15:18:31', '2026-05-26 22:37:53');
INSERT INTO `user` VALUES (294218395900055552, 'user_560619684', 'nabelese@gmail.com', 'https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/3b863287-233b-4b6d-8de3-2e41fbacbfa9.png', 0, '该用户还没有介绍自己哦', 1, '2026-03-22 21:18:52', '2026-03-22 21:18:52', '2026-04-05 14:55:25');

-- ----------------------------
-- Table structure for user_auths
-- ----------------------------
DROP TABLE IF EXISTS `user_auths`;
CREATE TABLE `user_auths`  (
  `id` bigint NOT NULL,
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户id',
  `identity_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录类型',
  `identifier` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标识',
  `credential` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '凭证',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_identifier`(`identifier` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_auths
-- ----------------------------
INSERT INTO `user_auths` VALUES (287965917474852864, 287965917437104128, 'email', 'wjj20050528@163.com', NULL);
INSERT INTO `user_auths` VALUES (287967118459277312, 287967118450888704, 'email', 'wjj20050421@qq.com', NULL);
INSERT INTO `user_auths` VALUES (294218395937804288, 294218395900055552, 'email', 'nabelese@gmail.com', NULL);

SET FOREIGN_KEY_CHECKS = 1;
