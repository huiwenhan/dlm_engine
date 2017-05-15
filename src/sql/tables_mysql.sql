-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- DROP TABLE IF EXISTS QUARTZ_FIRED_TRIGGERS;
-- DROP TABLE IF EXISTS QUARTZ_PAUSED_TRIGGER_GRPS;
-- DROP TABLE IF EXISTS QUARTZ_SCHEDULER_STATE;
-- DROP TABLE IF EXISTS QUARTZ_LOCKS;
-- DROP TABLE IF EXISTS QUARTZ_SIMPLE_TRIGGERS;
-- DROP TABLE IF EXISTS QUARTZ_SIMPROP_TRIGGERS;
-- DROP TABLE IF EXISTS QUARTZ_CRON_TRIGGERS;
-- DROP TABLE IF EXISTS QUARTZ_BLOB_TRIGGERS;
-- DROP TABLE IF EXISTS QUARTZ_TRIGGERS;
-- DROP TABLE IF EXISTS QUARTZ_JOB_DETAILS;
-- DROP TABLE IF EXISTS QUARTZ_CALENDARS;
-- DROP TABLE IF EXISTS BEACON_POLICY;
-- DROP TABLE IF EXISTS BEACON_POLICY_PROP;
-- DROP TABLE IF EXISTS BEACON_POLICY_INSTANCE;
-- DROP TABLE IF EXISTS BEACON_INSTANCE_JOB;

CREATE TABLE QUARTZ_JOB_DETAILS
(
  SCHED_NAME        VARCHAR(120) NOT NULL,
  JOB_NAME          VARCHAR(255) NOT NULL,
  JOB_GROUP         VARCHAR(255) NOT NULL,
  DESCRIPTION       VARCHAR(250) NULL,
  JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
  IS_DURABLE        VARCHAR(1)   NOT NULL,
  IS_NONCONCURRENT  VARCHAR(1)   NOT NULL,
  IS_UPDATE_DATA    VARCHAR(1)   NOT NULL,
  REQUESTS_RECOVERY VARCHAR(1)   NOT NULL,
  JOB_DATA          BLOB         NULL,
  PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QUARTZ_TRIGGERS
(
  SCHED_NAME     VARCHAR(120) NOT NULL,
  TRIGGER_NAME   VARCHAR(255) NOT NULL,
  TRIGGER_GROUP  VARCHAR(255) NOT NULL,
  JOB_NAME       VARCHAR(255) NOT NULL,
  JOB_GROUP      VARCHAR(255) NOT NULL,
  DESCRIPTION    VARCHAR(250) NULL,
  NEXT_FIRE_TIME BIGINT(13)   NULL,
  PREV_FIRE_TIME BIGINT(13)   NULL,
  PRIORITY       INTEGER      NULL,
  TRIGGER_STATE  VARCHAR(16)  NOT NULL,
  TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
  START_TIME     BIGINT(13)   NOT NULL,
  END_TIME       BIGINT(13)   NULL,
  CALENDAR_NAME  VARCHAR(200) NULL,
  MISFIRE_INSTR  SMALLINT(2)  NULL,
  JOB_DATA       BLOB         NULL,
  PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
  REFERENCES QUARTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QUARTZ_SIMPLE_TRIGGERS
(
  SCHED_NAME      VARCHAR(120) NOT NULL,
  TRIGGER_NAME    VARCHAR(255) NOT NULL,
  TRIGGER_GROUP   VARCHAR(255) NOT NULL,
  REPEAT_COUNT    BIGINT(7)    NOT NULL,
  REPEAT_INTERVAL BIGINT(12)   NOT NULL,
  TIMES_TRIGGERED BIGINT(10)   NOT NULL,
  PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
  REFERENCES QUARTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QUARTZ_CRON_TRIGGERS
(
  SCHED_NAME      VARCHAR(120) NOT NULL,
  TRIGGER_NAME    VARCHAR(255) NOT NULL,
  TRIGGER_GROUP   VARCHAR(255) NOT NULL,
  CRON_EXPRESSION VARCHAR(200) NOT NULL,
  TIME_ZONE_ID    VARCHAR(80),
  PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
  REFERENCES QUARTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QUARTZ_SIMPROP_TRIGGERS
(
  SCHED_NAME    VARCHAR(120)   NOT NULL,
  TRIGGER_NAME  VARCHAR(255)   NOT NULL,
  TRIGGER_GROUP VARCHAR(255)   NOT NULL,
  STR_PROP_1    VARCHAR(512)   NULL,
  STR_PROP_2    VARCHAR(512)   NULL,
  STR_PROP_3    VARCHAR(512)   NULL,
  INT_PROP_1    INT            NULL,
  INT_PROP_2    INT            NULL,
  LONG_PROP_1   BIGINT         NULL,
  LONG_PROP_2   BIGINT         NULL,
  DEC_PROP_1    NUMERIC(13, 4) NULL,
  DEC_PROP_2    NUMERIC(13, 4) NULL,
  BOOL_PROP_1   VARCHAR(1)     NULL,
  BOOL_PROP_2   VARCHAR(1)     NULL,
  PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
  REFERENCES QUARTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QUARTZ_BLOB_TRIGGERS
(
  SCHED_NAME    VARCHAR(120) NOT NULL,
  TRIGGER_NAME  VARCHAR(255) NOT NULL,
  TRIGGER_GROUP VARCHAR(255) NOT NULL,
  BLOB_DATA     BLOB         NULL,
  PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
  REFERENCES QUARTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QUARTZ_CALENDARS
(
  SCHED_NAME    VARCHAR(120) NOT NULL,
  CALENDAR_NAME VARCHAR(200) NOT NULL,
  CALENDAR      BLOB         NOT NULL,
  PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);

CREATE TABLE QUARTZ_PAUSED_TRIGGER_GRPS
(
  SCHED_NAME    VARCHAR(120) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);

CREATE TABLE QUARTZ_FIRED_TRIGGERS
(
  SCHED_NAME        VARCHAR(120) NOT NULL,
  ENTRY_ID          VARCHAR(95)  NOT NULL,
  TRIGGER_NAME      VARCHAR(255) NOT NULL,
  TRIGGER_GROUP     VARCHAR(255) NOT NULL,
  INSTANCE_NAME     VARCHAR(200) NOT NULL,
  FIRED_TIME        BIGINT(13)   NOT NULL,
  SCHED_TIME        BIGINT(13)   NOT NULL,
  PRIORITY          INTEGER      NOT NULL,
  STATE             VARCHAR(16)  NOT NULL,
  JOB_NAME          VARCHAR(255) NULL,
  JOB_GROUP         VARCHAR(255) NULL,
  IS_NONCONCURRENT  VARCHAR(1)   NULL,
  REQUESTS_RECOVERY VARCHAR(1)   NULL,
  PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

CREATE TABLE QUARTZ_SCHEDULER_STATE
(
  SCHED_NAME        VARCHAR(120) NOT NULL,
  INSTANCE_NAME     VARCHAR(200) NOT NULL,
  LAST_CHECKIN_TIME BIGINT(13)   NOT NULL,
  CHECKIN_INTERVAL  BIGINT(13)   NOT NULL,
  PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE QUARTZ_LOCKS
(
  SCHED_NAME VARCHAR(120) NOT NULL,
  LOCK_NAME  VARCHAR(40)  NOT NULL,
  PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

CREATE TABLE IF NOT EXISTS BEACON_POLICY
(
  ID                 VARCHAR(255),
  NAME               VARCHAR(64),
  VERSION            INTEGER,
  CHANGE_ID          INTEGER,
  STATUS             VARCHAR(40),
  TYPE               VARCHAR(40),
  SOURCE_CLUSTER     VARCHAR(255),
  TARGET_CLUSTER     VARCHAR(255),
  SOURCE_DATASET     VARCHAR(4000),
  TARGET_DATASET     VARCHAR(4000),
  CREATED_TIME       DATETIME NULL DEFAULT NULL,
  LAST_MODIFIED_TIME DATETIME NULL DEFAULT NULL,
  START_TIME         DATETIME NULL DEFAULT NULL,
  END_TIME           DATETIME NULL DEFAULT NULL,
  FREQUENCY          INTEGER,
  NOTIFICATION_TYPE  VARCHAR(255),
  NOTIFICATION_TO    VARCHAR(255),
  RETRY_COUNT        INT,
  RETRY_DELAY        INT,
  TAGS               VARCHAR(1024),
  EXECUTION_TYPE     VARCHAR(64),
  RETIREMENT_TIME    DATETIME NULL DEFAULT NULL,
  JOBS               VARCHAR(1024),
  USERNAME           VARCHAR(64),
  PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS BEACON_POLICY_PROP
(
  ID           BIGINT    NOT NULL AUTO_INCREMENT,
  POLICY_ID    VARCHAR(255),
  CREATED_TIME DATETIME NULL DEFAULT NULL,
  NAME         VARCHAR(512),
  VALUE        VARCHAR(1024),
  TYPE         VARCHAR(20),
  PRIMARY KEY (ID),
  FOREIGN KEY (POLICY_ID) REFERENCES BEACON_POLICY (ID)
);

CREATE TABLE IF NOT EXISTS BEACON_POLICY_INSTANCE
(
  ID                 VARCHAR(255) NOT NULL,
  POLICY_ID          VARCHAR(255),
  START_TIME         DATETIME    NULL DEFAULT NULL,
  END_TIME           DATETIME    NULL DEFAULT NULL,
  RETIREMENT_TIME    DATETIME    NULL DEFAULT NULL,
  STATUS             VARCHAR(40),
  MESSAGE            VARCHAR(4000),
  RUN_COUNT          INTEGER,
  CURRENT_OFFSET     INTEGER,
  TRACKING_INFO      VARCHAR(4000),
  PRIMARY KEY (ID),
  FOREIGN KEY (POLICY_ID) REFERENCES BEACON_POLICY (ID)
);


CREATE TABLE IF NOT EXISTS BEACON_INSTANCE_JOB
(
  INSTANCE_ID     VARCHAR(255) NOT NULL,
  OFFSET          INTEGER      NOT NULL,
  STATUS          VARCHAR(40),
  START_TIME      DATETIME    NULL DEFAULT NULL,
  END_TIME        DATETIME    NULL DEFAULT NULL,
  MESSAGE         VARCHAR(4000),
  RETIREMENT_TIME DATETIME    NULL DEFAULT NULL,
  RUN_COUNT       INTEGER,
  CONTEXT_DATA    VARCHAR(4000),
  PRIMARY KEY (INSTANCE_ID, OFFSET),
  FOREIGN KEY (INSTANCE_ID) REFERENCES BEACON_POLICY_INSTANCE (ID)
);

CREATE TABLE BEACON_EVENT
(
  ID                  BIGINT NOT NULL AUTO_INCREMENT,
  POLICY_ID           VARCHAR(255),
  INSTANCE_ID         VARCHAR(255),
  EVENT_ENTITY_TYPE   VARCHAR(32),
  EVENT_ID            INTEGER NOT NULL,
  EVENT_TIMESTAMP     TIMESTAMP,
  EVENT_MESSAGE       VARCHAR(4000),
  PRIMARY KEY(ID)
);
