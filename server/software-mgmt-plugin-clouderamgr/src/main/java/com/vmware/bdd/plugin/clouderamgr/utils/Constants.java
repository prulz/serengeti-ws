/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.plugin.clouderamgr.utils;

/**
 * Author: Xiaoding Bian
 * Date: 7/3/14
 * Time: 10:37 AM
 */
public interface Constants {
   public static final String CDH_REPO_PREFIX = "CDH";
   public static final String CDH_DISTRO_VENDOR = "CDH";
   public static final String CDH_PLUGIN_NAME = "ClouderaManager";
   public static final String CMS_NAME_TOKEN_DELIM = "_";
   public static final int VERSION_UNBOUNDED = -1;

   public static final String CONFIG_DFS_NAME_DIR_LIST = "dfs_name_dir_list";
   public static final String CONFIG_DFS_DATA_DIR_LIST = "dfs_data_dir_list";
   public static final String CONFIG_FS_CHECKPOINT_DIR_LIST = "fs_checkpoint_dir_list";
   public static final String CONFIG_NM_LOCAL_DIRS = "yarn_nodemanager_local_dirs";
   public static final String CONFIG_MAPRED_JT_LOCAL_DIR_LIST = "jobtracker_mapred_local_dir_list";
   public static final String CONFIG_MAPRED_TT_LOCAL_DIR_LIST = "tasktracker_mapred_local_dir_list";

   // HA configs
   public static final String CONFIG_DFS_JOURNALNODE_EDITS_DIR = "dfs_journalnode_edits_dir";
   public static final String CONFIG_AUTO_FAILOVER_ENABLED = "autofailover_enabled";
   public static final String CONFIG_DFS_FEDERATION_NAMESERVICE = "dfs_federation_namenode_nameservice";
   public static final String CONFIG_DFS_NAMENODE_QUORUM_JOURNAL_NAME = "dfs_namenode_quorum_journal_name";

   public static final String CONFIG_ZOOKEEPER_SERVER_ID = "serverId";

   public static final String ROLE_CONFIG_GROUP_UPDATE_NOTES = "Update Base Role Config Group By VMware Big Data Extention";
}
