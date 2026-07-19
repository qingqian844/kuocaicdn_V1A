-- KuocaiCDN V2.1.2.2 customer clean installation database
-- Schema plus required initial settings only; no production business data or credentials.


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DROP TABLE IF EXISTS `agent_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `agent_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `website_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `website_keyword` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `website_description` text COLLATE utf8mb4_unicode_ci,
  `about` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_dashboard` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `company` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icp` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `domain` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `licence` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `licence_url` text COLLATE utf8mb4_unicode_ci,
  `wechat_service_url` text COLLATE utf8mb4_unicode_ci,
  `email_config` text COLLATE utf8mb4_unicode_ci,
  `email_template_config` text COLLATE utf8mb4_unicode_ci,
  `sms_config` text COLLATE utf8mb4_unicode_ci,
  `sms_template_config` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `agent_config` WRITE;
/*!40000 ALTER TABLE `agent_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `agent_config` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `cdn_domain_route_binding`;
CREATE TABLE `cdn_domain_route_binding` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `domain_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `domain_name` varchar(255) NOT NULL,
  `service_area` varchar(64) NOT NULL,
  `route` varchar(64) NOT NULL,
  `vendor_account_id` bigint(20) DEFAULT NULL,
  `target_key` varchar(128) NOT NULL,
  `upstream_domain_id` varchar(255) DEFAULT NULL,
  `upstream_cname` varchar(255) DEFAULT NULL,
  `domain_snapshot_json` longtext,
  `local_domain_id` bigint(20) DEFAULT NULL,
  `dns_record_id` bigint(20) DEFAULT NULL,
  `primary_binding` tinyint(4) NOT NULL DEFAULT '0',
  `status` varchar(32) NOT NULL DEFAULT 'active',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domain_route_target` (`domain_id`,`target_key`),
  KEY `idx_route_binding_domain` (`domain_id`,`status`),
  KEY `idx_route_binding_account` (`route`,`vendor_account_id`),
  KEY `idx_route_binding_user` (`user_id`,`service_area`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DROP TABLE IF EXISTS `agent_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `agent_level` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `flow_order_rate` decimal(20,6) DEFAULT NULL,
  `package_rate` decimal(20,6) DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `agent_level` WRITE;
/*!40000 ALTER TABLE `agent_level` DISABLE KEYS */;
/*!40000 ALTER TABLE `agent_level` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `announcement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `user_id` bigint(20) DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `announcement` WRITE;
/*!40000 ALTER TABLE `announcement` DISABLE KEYS */;
/*!40000 ALTER TABLE `announcement` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `bonus_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bonus_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `transaction_order_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `agent_user_id` bigint(20) DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(20,6) DEFAULT NULL,
  `bonus` decimal(20,6) DEFAULT NULL,
  `bonus_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `bonus_record` WRITE;
/*!40000 ALTER TABLE `bonus_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `bonus_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `cache_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cache_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `task_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cdn_supplier` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `vendor_account_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cache_task_vendor_account` (`vendor_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `cache_task` WRITE;
/*!40000 ALTER TABLE `cache_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `cache_task` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `cdn_domain`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cdn_domain` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `domain_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `domain_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `service_area` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `domain_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tencent_dns_id` bigint(20) DEFAULT NULL,
  `route` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cname_huawei` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cname_volcengine` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cname_yifan` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cname_tencent` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cname_cdnetworks` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cname_aliyun` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cname_baidu` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cname_wangsu` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cname_kingsoft` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `vendor_account_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cdn_domain_vendor_account` (`vendor_account_id`),
  KEY `idx_cdn_domain_route_account` (`route`,`vendor_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `cdn_domain` WRITE;
/*!40000 ALTER TABLE `cdn_domain` DISABLE KEYS */;
/*!40000 ALTER TABLE `cdn_domain` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `cdn_vendor_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cdn_vendor_account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `vendor_code` varchar(64) NOT NULL,
  `account_name` varchar(128) NOT NULL,
  `config_json` longtext NOT NULL,
  `is_default` tinyint(4) NOT NULL DEFAULT '0',
  `status` varchar(32) NOT NULL DEFAULT 'enabled',
  `remark` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_vendor_account_default` (`vendor_code`,`is_default`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `cdn_vendor_account` WRITE;
/*!40000 ALTER TABLE `cdn_vendor_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `cdn_vendor_account` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `edgeone_domain_quota_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `edgeone_domain_quota_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `transaction_order_id` bigint(20) NOT NULL,
  `quota_count` int(11) DEFAULT '1',
  `deadline` datetime NOT NULL,
  `status` varchar(32) DEFAULT 'active',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edgeone_quota_order_transaction` (`transaction_order_id`),
  KEY `idx_edgeone_quota_user_deadline` (`user_id`,`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `edgeone_domain_quota_order` WRITE;
/*!40000 ALTER TABLE `edgeone_domain_quota_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `edgeone_domain_quota_order` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `edgeone_root_domain_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `edgeone_root_domain_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `root_domain` varchar(255) NOT NULL,
  `first_domain_name` varchar(255) DEFAULT NULL,
  `cdn_domain_id` bigint(20) DEFAULT NULL,
  `status` varchar(32) DEFAULT 'active',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edgeone_user_root` (`user_id`,`root_domain`),
  KEY `idx_edgeone_root_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `edgeone_root_domain_record` WRITE;
/*!40000 ALTER TABLE `edgeone_root_domain_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `edgeone_root_domain_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `example`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `example` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `cd` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `example` WRITE;
/*!40000 ALTER TABLE `example` DISABLE KEYS */;
/*!40000 ALTER TABLE `example` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `face_certify_verify`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `face_certify_verify` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `order_no` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `certify_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `no` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `face_certify_verify` WRITE;
/*!40000 ALTER TABLE `face_certify_verify` DISABLE KEYS */;
/*!40000 ALTER TABLE `face_certify_verify` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `flow_donate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flow_donate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `flow_package_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `flow_package_size` decimal(20,6) DEFAULT NULL,
  `deadline` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `purchased_flow_id` bigint(20) DEFAULT NULL,
  `edgeone_domain_quota` int(11) DEFAULT '0',
  `https_request_quota` bigint(20) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `flow_donate` WRITE;
/*!40000 ALTER TABLE `flow_donate` DISABLE KEYS */;
/*!40000 ALTER TABLE `flow_donate` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `flow_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flow_package` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `package_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `charge_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `package_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `day_limit` int(11) DEFAULT NULL,
  `user_limit` int(11) DEFAULT NULL,
  `buyer_rule` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  `price` decimal(20,6) DEFAULT NULL,
  `price3` decimal(20,6) DEFAULT NULL,
  `price12` decimal(20,6) DEFAULT NULL,
  `buy_count` int(11) DEFAULT NULL,
  `sort` int(11) DEFAULT NULL,
  `edgeone_domain_quota` int(11) DEFAULT '0',
  `https_request_quota` bigint(20) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `flow_package` WRITE;
/*!40000 ALTER TABLE `flow_package` DISABLE KEYS */;
/*!40000 ALTER TABLE `flow_package` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `gift`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gift` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `flow_package_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `flow_package_size` bigint(20) DEFAULT NULL,
  `deadline` datetime DEFAULT NULL,
  `expire_time` datetime DEFAULT NULL,
  `size` int(11) DEFAULT '1' COMMENT '????',
  `code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `purchased_record` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `gift` WRITE;
/*!40000 ALTER TABLE `gift` DISABLE KEYS */;
/*!40000 ALTER TABLE `gift` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `login_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `login_device` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `browser` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `login_ip` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `login_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `login_device` WRITE;
/*!40000 ALTER TABLE `login_device` DISABLE KEYS */;
/*!40000 ALTER TABLE `login_device` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `send_user_id` bigint(20) DEFAULT NULL,
  `receive_user_id` bigint(20) DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `message` WRITE;
/*!40000 ALTER TABLE `message` DISABLE KEYS */;
/*!40000 ALTER TABLE `message` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `operation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `user_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `module` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `service` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `op_describe` text COLLATE utf8mb4_unicode_ci,
  `request` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `response` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `method` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url` text COLLATE utf8mb4_unicode_ci,
  `ip` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_log` WRITE;
/*!40000 ALTER TABLE `operation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_log` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `purchased_flow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `purchased_flow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `transaction_order_id` bigint(20) DEFAULT NULL,
  `flow_package_id` bigint(20) DEFAULT NULL,
  `flow_package_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `flow_package_size` bigint(20) DEFAULT NULL,
  `used_flow` bigint(20) DEFAULT NULL,
  `deadline` datetime DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `baned_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `edgeone_domain_quota` int(11) DEFAULT '0',
  `https_request_quota` bigint(20) DEFAULT '0',
  `used_https_requests` bigint(20) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_purchased_flow_user_status_deadline` (`user_id`,`status`,`deadline`),
  KEY `idx_purchased_flow_status_create_time` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `purchased_flow` WRITE;
/*!40000 ALTER TABLE `purchased_flow` DISABLE KEYS */;
/*!40000 ALTER TABLE `purchased_flow` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `purchased_flow_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `purchased_flow_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `purchased_flow_id` bigint(20) DEFAULT NULL,
  `consume` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `purchased_flow_detail` WRITE;
/*!40000 ALTER TABLE `purchased_flow_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `purchased_flow_detail` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `real_name_authentication`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `real_name_authentication` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `authentication_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_card_num` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `front_img` text COLLATE utf8mb4_unicode_ci,
  `back_img` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `real_name_authentication` WRITE;
/*!40000 ALTER TABLE `real_name_authentication` DISABLE KEYS */;
/*!40000 ALTER TABLE `real_name_authentication` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `saved_certificate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `saved_certificate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `cert_name` varchar(100) NOT NULL,
  `certificate` longtext NOT NULL,
  `private_key` longtext NOT NULL,
  `domain_names` text,
  `status` varchar(32) DEFAULT 'active',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_saved_certificate_user` (`user_id`),
  KEY `idx_saved_certificate_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `saved_certificate` WRITE;
/*!40000 ALTER TABLE `saved_certificate` DISABLE KEYS */;
/*!40000 ALTER TABLE `saved_certificate` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `self_hosted_cache_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `self_hosted_cache_job` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_id` varchar(64) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `operation` varchar(32) NOT NULL,
  `target_type` varchar(32) NOT NULL,
  `targets_json` longtext NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'pending',
  `total_nodes` int(11) NOT NULL DEFAULT '0',
  `success_nodes` int(11) NOT NULL DEFAULT '0',
  `failed_nodes` int(11) NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_self_hosted_cache_task` (`task_id`),
  KEY `idx_self_hosted_cache_user` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `self_hosted_cache_job` WRITE;
/*!40000 ALTER TABLE `self_hosted_cache_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `self_hosted_cache_job` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `self_hosted_cache_job_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `self_hosted_cache_job_node` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `job_id` bigint(20) NOT NULL,
  `node_id` bigint(20) NOT NULL,
  `targets_json` longtext NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'pending',
  `last_error` varchar(1000) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_self_hosted_cache_job_node` (`job_id`,`node_id`),
  KEY `idx_self_hosted_cache_node_status` (`node_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `self_hosted_cache_job_node` WRITE;
/*!40000 ALTER TABLE `self_hosted_cache_job_node` DISABLE KEYS */;
/*!40000 ALTER TABLE `self_hosted_cache_job_node` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `self_hosted_domain_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `self_hosted_domain_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cdn_domain_id` bigint(20) NOT NULL,
  `node_group_id` bigint(20) NOT NULL,
  `origin_type` varchar(32) NOT NULL,
  `origin_address` varchar(2000) NOT NULL,
  `origin_protocol` varchar(16) NOT NULL DEFAULT 'http',
  `http_port` int(11) NOT NULL DEFAULT '80',
  `https_port` int(11) NOT NULL DEFAULT '443',
  `origin_host` varchar(255) DEFAULT NULL,
  `cache_config_json` longtext,
  `https_enabled` tinyint(4) NOT NULL DEFAULT '0',
  `certificate_cipher` longtext,
  `private_key_cipher` longtext,
  `force_redirect` varchar(16) NOT NULL DEFAULT 'off',
  `desired_config_version` bigint(20) NOT NULL DEFAULT '1',
  `status` varchar(32) NOT NULL DEFAULT 'enabled',
  `last_error` varchar(1000) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_self_hosted_domain_config` (`cdn_domain_id`),
  KEY `idx_self_hosted_domain_group` (`node_group_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `self_hosted_domain_config` WRITE;
/*!40000 ALTER TABLE `self_hosted_domain_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `self_hosted_domain_config` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `self_hosted_group_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `self_hosted_group_node` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `group_id` bigint(20) NOT NULL,
  `node_id` bigint(20) NOT NULL,
  `weight` int(11) NOT NULL DEFAULT '100',
  `priority` int(11) NOT NULL DEFAULT '100',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_self_hosted_group_node` (`group_id`,`node_id`),
  KEY `idx_self_hosted_group_node_node` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `self_hosted_group_node` WRITE;
/*!40000 ALTER TABLE `self_hosted_group_node` DISABLE KEYS */;
/*!40000 ALTER TABLE `self_hosted_group_node` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `self_hosted_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `self_hosted_node` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `node_name` varchar(128) NOT NULL,
  `host` varchar(255) NOT NULL,
  `ssh_port` int(11) NOT NULL DEFAULT '22',
  `ssh_username` varchar(128) NOT NULL,
  `ssh_password_cipher` longtext,
  `ssh_host_key` varchar(512) DEFAULT NULL,
  `agent_token_hash` varchar(128) DEFAULT NULL,
  `region` varchar(64) DEFAULT NULL,
  `weight` int(11) NOT NULL DEFAULT '100',
  `enabled` tinyint(4) NOT NULL DEFAULT '1',
  `status` varchar(32) NOT NULL DEFAULT 'pending',
  `last_heartbeat` datetime DEFAULT NULL,
  `agent_version` varchar(64) DEFAULT NULL,
  `desired_config_version` bigint(20) NOT NULL DEFAULT '0',
  `applied_config_version` bigint(20) NOT NULL DEFAULT '0',
  `cpu_usage` decimal(6,2) DEFAULT NULL,
  `memory_usage` decimal(6,2) DEFAULT NULL,
  `disk_usage` decimal(6,2) DEFAULT NULL,
  `rx_bytes` bigint(20) NOT NULL DEFAULT '0',
  `tx_bytes` bigint(20) NOT NULL DEFAULT '0',
  `cache_bytes` bigint(20) NOT NULL DEFAULT '0',
  `last_error` varchar(1000) DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_self_hosted_node_endpoint` (`host`,`ssh_port`),
  KEY `idx_self_hosted_node_status` (`enabled`,`status`,`last_heartbeat`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `self_hosted_node` WRITE;
/*!40000 ALTER TABLE `self_hosted_node` DISABLE KEYS */;
/*!40000 ALTER TABLE `self_hosted_node` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `self_hosted_node_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `self_hosted_node_group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `group_name` varchar(128) NOT NULL,
  `cname_label` varchar(64) NOT NULL,
  `coverage` varchar(32) NOT NULL DEFAULT 'legacy',
  `dns_record_ids` longtext,
  `is_default` tinyint(4) NOT NULL DEFAULT '0',
  `status` varchar(32) NOT NULL DEFAULT 'enabled',
  `remark` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_self_hosted_group_name` (`group_name`),
  UNIQUE KEY `uk_self_hosted_group_cname` (`cname_label`),
  KEY `idx_self_hosted_group_coverage` (`coverage`,`status`,`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `self_hosted_node_group` WRITE;
/*!40000 ALTER TABLE `self_hosted_node_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `self_hosted_node_group` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(128) DEFAULT NULL,
  `config_content` text,
  `create_by` bigint(20) DEFAULT NULL,
  `update_by` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (1,'website_base_config','{\"websiteName\":\"CDN Management System\",\"websiteAnnouncement\":\"\",\"defaultFlowPrice\":100,\"icpNumber\":\"\",\"websiteIconImg\":\"\",\"defaultAvatarImg\":\"/common/default-avatar.png\",\"adminPath\":\"kuocaiadmin\",\"websiteLogoImg\":\"\",\"wechatQrCodeImg\":\"\",\"qqGroupQrCodeImg\":\"\",\"expireTime\":30,\"maxDomainCount\":10,\"maxDomainCountProxy\":100,\"inviteRewardGb\":0,\"invitedRewardGb\":0,\"monthGiftGb\":0,\"edgeoneDomainQuotaEnabled\":false,\"edgeoneFreeDomainQuota\":1,\"edgeoneDomainQuotaPrice\":30,\"edgeoneDomainQuotaValidDays\":30,\"defaultUserRoute\":\"tencent\",\"httpsRequestFeeEnabled\":false,\"httpsRequestFeeRoutes\":\"\",\"httpsRequestFeeUnitCount\":10000,\"httpsRequestFeeUnitPrice\":0}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (2,'website_permission_config','{\"forceRealAuthentication\":false,\"forceBindingTel\":false,\"closeRegister\":false}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (3,'website_agreement_config','{\"agreementInfo\":\"\"}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (4,'website_home_code_config','{\"enabled\":false,\"htmlCode\":\"\"}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (5,'website_footer_code_config','{\"enabled\":false,\"htmlCode\":\"\"}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (6,'website_seo_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (7,'website_contact_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (8,'website_access_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (9,'wechat_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (10,'alipay_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (11,'ali_withdraw_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (12,'alipay_authentication_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (13,'tencent_face_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (14,'email_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (15,'email_template_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (16,'sms_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (17,'sms_template_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (18,'wechat_code_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (19,'api_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (20,'dns_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (21,'huawei_cloud_api_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (22,'volcanic_cloud_api_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (23,'white_mountain_cloud_api_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (24,'tencent_cloud_api_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (25,'tencent_edgeone_api_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (26,'cdnetworks_api_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (27,'aliyun_cdn_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (28,'wangsu_cdn_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (29,'baidu_cdn_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (30,'kingsoft_cdn_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
INSERT INTO `sys_config` (`id`, `biz_type`, `config_content`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (31,'merge_cdn_api_config','{}',1,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_menu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url` text COLLATE utf8mb4_unicode_ci,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `priority` int(11) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` (`id`, `name`, `level`, `url`, `type`, `priority`, `create_time`, `update_time`) VALUES (2069465359553048578,'新客特惠','1','/buy-flow-packages','only-main',100,'2026-06-24 00:59:25','2026-06-24 00:59:25');
INSERT INTO `sys_menu` (`id`, `name`, `level`, `url`, `type`, `priority`, `create_time`, `update_time`) VALUES (2069465863137964033,'开通套餐','1','/buy-flow-packages-customize','only-main',98,'2026-06-24 01:01:25','2026-06-24 01:01:25');
INSERT INTO `sys_menu` (`id`, `name`, `level`, `url`, `type`, `priority`, `create_time`, `update_time`) VALUES (2069465934986391554,'添加域名','1','/domain-list','only-main',96,'2026-06-24 01:01:42','2026-06-24 01:01:42');
INSERT INTO `sys_menu` (`id`, `name`, `level`, `url`, `type`, `priority`, `create_time`, `update_time`) VALUES (2069469164797603841,'数据统计','2','/data-board','only-main',95,'2026-06-24 01:14:32','2026-06-24 01:14:32');
INSERT INTO `sys_menu` (`id`, `name`, `level`, `url`, `type`, `priority`, `create_time`, `update_time`) VALUES (2069469285165740034,'操作日志','2','/operation-logs','only-main',94,'2026-06-24 01:15:01','2026-06-24 01:15:01');
INSERT INTO `sys_menu` (`id`, `name`, `level`, `url`, `type`, `priority`, `create_time`, `update_time`) VALUES (2069469517928640513,'消息中心','2','/message','only-main',93,'2026-06-24 01:15:56','2026-06-24 01:15:56');
INSERT INTO `sys_menu` (`id`, `name`, `level`, `url`, `type`, `priority`, `create_time`, `update_time`) VALUES (2069469956725075970,'发起工单','2','/order-create','only-main',92,'2026-06-24 01:17:41','2026-06-24 01:17:41');
INSERT INTO `sys_menu` (`id`, `name`, `level`, `url`, `type`, `priority`, `create_time`, `update_time`) VALUES (2073659778403348482,'站点管理','1','/domain-list','both',0,'2026-07-05 14:46:32','2026-07-05 14:46:32');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `remark`, `create_time`, `update_time`) VALUES (1,'admin','Admin','system init','2026-06-23 18:42:30','2026-06-23 18:42:30');
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `remark`, `create_time`, `update_time`) VALUES (2,'user','User','system init','2026-06-23 18:42:30','2026-06-23 18:42:30');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) DEFAULT NULL,
  `flow_price` decimal(20,6) DEFAULT NULL,
  `virtual_rate` decimal(20,6) DEFAULT NULL,
  `max_domain_count` int(11) DEFAULT NULL,
  `user_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_pwd` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pwd_salt` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `real_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_card_num` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `my_website` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `img` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_time` datetime DEFAULT NULL,
  `last_login_ip` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wechat_open_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qq_open_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weibo_open_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `auto_balance` int(11) DEFAULT NULL,
  `route` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enable_overseas` int(11) DEFAULT NULL,
  `enable_global` int(11) DEFAULT NULL,
  `referrer_id` bigint(20) DEFAULT NULL,
  `agent_user_id` bigint(20) DEFAULT NULL,
  `agent_level_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sys_user_email` (`email`),
  KEY `idx_sys_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` (`id`, `role_id`, `flow_price`, `virtual_rate`, `max_domain_count`, `user_name`, `user_pwd`, `pwd_salt`, `real_name`, `id_card_num`, `my_website`, `phone`, `email`, `img`, `status`, `create_time`, `update_time`, `last_login_time`, `last_login_ip`, `wechat_open_id`, `qq_open_id`, `weibo_open_id`, `auto_balance`, `route`, `enable_overseas`, `enable_global`, `referrer_id`, `agent_user_id`, `agent_level_id`) VALUES (1,1,100.000000,1.000000,1000,'admin','$2b$10$EjIZyC3VnbrGlSASV1gpx.KNtqHZRIU47Xw94hpzL.VRRgaP9hwI.',NULL,'System Administrator',NULL,NULL,NULL,NULL,'/common/default-avatar.png','certified','2026-07-17 23:38:33','2026-07-17 23:38:33',NULL,NULL,NULL,NULL,NULL,1,'tencent',0,0,NULL,NULL,NULL);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_user_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `user_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_balance` decimal(20,6) DEFAULT NULL,
  `amass_recharge` decimal(20,6) DEFAULT NULL,
  `bonus` decimal(20,6) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_user_account` WRITE;
/*!40000 ALTER TABLE `sys_user_account` DISABLE KEYS */;
INSERT INTO `sys_user_account` (`id`, `user_id`, `user_name`, `account_balance`, `amass_recharge`, `bonus`, `status`, `create_time`, `update_time`) VALUES (1,1,'admin',0.000000,0.000000,0.000000,1,'2026-07-17 23:38:33','2026-07-17 23:38:33');
/*!40000 ALTER TABLE `sys_user_account` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_user_banned`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_banned` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `user_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `banned_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `banned_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_user_banned` WRITE;
/*!40000 ALTER TABLE `sys_user_banned` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_user_banned` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_user_vendor_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_vendor_account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `vendor_code` varchar(64) NOT NULL,
  `vendor_account_id` bigint(20) NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_vendor_account` (`user_id`,`vendor_code`),
  KEY `idx_user_vendor_account_id` (`vendor_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_user_vendor_account` WRITE;
/*!40000 ALTER TABLE `sys_user_vendor_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_user_vendor_account` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `transaction_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transaction_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `user_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_num` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(20,6) DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pay_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pay_url` text COLLATE utf8mb4_unicode_ci,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `pay_time` datetime DEFAULT NULL,
  `product_id` bigint(20) DEFAULT NULL,
  `create_by` bigint(20) DEFAULT NULL,
  `update_by` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `transaction_order` WRITE;
/*!40000 ALTER TABLE `transaction_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `transaction_order` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user_notice_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_notice_settings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `notice_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email_accept` int(11) DEFAULT NULL,
  `sms_accept` int(11) DEFAULT NULL,
  `wechat_accept` int(11) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_notice_settings` WRITE;
/*!40000 ALTER TABLE `user_notice_settings` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_notice_settings` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `withdraw_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `withdraw_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `amount` decimal(20,6) DEFAULT NULL,
  `withdraw_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `withdraw_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `withdraw_account` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `reject_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `withdraw_record` WRITE;
/*!40000 ALTER TABLE `withdraw_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `withdraw_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `work_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `work_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `cd` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type_id` bigint(20) DEFAULT NULL,
  `urgent_level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `feedback` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `result` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `domain` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `work_order` WRITE;
/*!40000 ALTER TABLE `work_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `work_order` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `work_order_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `work_order_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `context` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `work_order_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `admin_id` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `work_order_message` WRITE;
/*!40000 ALTER TABLE `work_order_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `work_order_message` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `work_order_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `work_order_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `work_order_type` WRITE;
/*!40000 ALTER TABLE `work_order_type` DISABLE KEYS */;
INSERT INTO `work_order_type` (`id`, `type_name`, `remark`, `create_time`, `update_time`) VALUES (2069636392231694338,'配置问题','请详细描述您所遇到的问题、详细描述有助我们快速那帮您解决！','2026-06-24 12:19:02','2026-06-24 12:19:02');
INSERT INTO `work_order_type` (`id`, `type_name`, `remark`, `create_time`, `update_time`) VALUES (2069636729814446081,'证书问题','请详细描述您所遇到的问题、详细描述有助我们快速那帮您解决！','2026-06-24 12:20:23','2026-06-24 12:20:23');
INSERT INTO `work_order_type` (`id`, `type_name`, `remark`, `create_time`, `update_time`) VALUES (2069636969753800706,'缓存问题','请详细描述您所遇到的问题、详细描述有助我们快速那帮您解决！','2026-06-24 12:21:20','2026-06-24 12:21:20');
INSERT INTO `work_order_type` (`id`, `type_name`, `remark`, `create_time`, `update_time`) VALUES (2069637028490833921,'计费问题','请详细描述您所遇到的问题、详细描述有助我们快速那帮您解决！','2026-06-24 12:21:34','2026-06-24 12:21:34');
INSERT INTO `work_order_type` (`id`, `type_name`, `remark`, `create_time`, `update_time`) VALUES (2069637090251960321,'实名问题','请详细描述您所遇到的问题、详细描述有助我们快速那帮您解决！','2026-06-24 12:21:48','2026-06-24 12:21:48');
INSERT INTO `work_order_type` (`id`, `type_name`, `remark`, `create_time`, `update_time`) VALUES (2069637149119016962,'其他疑问','请详细描述您所遇到的问题、详细描述有助我们快速那帮您解决！','2026-06-24 12:22:03','2026-06-24 12:22:03');
/*!40000 ALTER TABLE `work_order_type` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- New customer deliveries enter the first-run wizard. Existing databases without
-- this record are treated as already initialized for backward compatibility.
INSERT INTO `sys_config` (`biz_type`, `config_content`, `create_by`, `update_by`)
VALUES ('installation_state', '{"status":"PENDING","currentStep":1,"bootstrapPasswordApplied":false,"adminConfigured":false,"domainVerified":false,"proxyConfigured":false,"websiteConfigured":false,"modules":{},"moduleTestHashes":{}}', 1, 1)
ON DUPLICATE KEY UPDATE `config_content` = VALUES(`config_content`), `update_by` = 1;
