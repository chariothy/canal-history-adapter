CREATE TABLE IF NOT EXISTS `db_log` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	`time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
	`type` VARCHAR(20) NOT NULL COMMENT '操作类型' COLLATE 'utf8mb4_unicode_ci',
	`db_table` VARCHAR(50) NOT NULL COMMENT '数据库.表' COLLATE 'utf8mb4_unicode_ci',
	`json` JSON NULL DEFAULT NULL COMMENT 'JSON数据',
	PRIMARY KEY (`id`),
	INDEX `time` (`time`),
	INDEX `type` (`type`),
	INDEX `db_table` (`db_table`)
)
COMMENT='数据库操作日志'
COLLATE='utf8mb4_unicode_ci'
ENGINE=InnoDB;