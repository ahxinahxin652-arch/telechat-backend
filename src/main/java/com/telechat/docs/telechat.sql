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
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '个人设置(置顶/免打扰)的更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unidx_user_conversation`(`user_id` ASC, `conversation_id` ASC) USING BTREE COMMENT '唯一成员',
  INDEX `idx_user_conversation`(`user_id` ASC, `conversation_id` ASC) USING BTREE COMMENT '优化查询'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

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

SET FOREIGN_KEY_CHECKS = 1;
