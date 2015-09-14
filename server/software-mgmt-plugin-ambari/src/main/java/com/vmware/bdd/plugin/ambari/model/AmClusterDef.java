/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.Expose;
import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprintInfo;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostGroup;
import com.vmware.bdd.plugin.ambari.utils.AmUtils;
import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;

public class AmClusterDef implements Serializable {
   private static final long serialVersionUID = 5585914268769234047L;

   @Expose
   private String name;

   @Expose
   private String version;

   @Expose
   private boolean verbose;

   @Expose
   private String sshKey;

   @Expose
   private String user;

   @Expose
   private List<AmNodeGroupDef> nodeGroups;

   @Expose
   private AmStackDef amStack;

   private ClusterReport currentReport;

   private String ambariServerVersion;

   @Expose
   private List<Map<String, Object>> configurations;

   private boolean isComputeOnly = false;

   private String externalNamenode;

   private String externalSecondaryNamenode;

   private static final Map<String, String> serviceName2ServiceUserConfigName;

   private Map<String, String> rackTopology;

   static {
      serviceName2ServiceUserConfigName = new HashMap<>();
      serviceName2ServiceUserConfigName.put("HDFS", "hdfs");
      serviceName2ServiceUserConfigName.put("YARN", "yarn");
   }

   public AmClusterDef(ClusterBlueprint blueprint, String privateKey, String ambariServerVersion) {
      this.name = blueprint.getName();
      this.version = blueprint.getHadoopStack().getFullVersion();
      this.verbose = true;
      this.sshKey = privateKey;
      this.user = Constants.AMBARI_SSH_USER;
      this.currentReport = new ClusterReport(blueprint);
      this.ambariServerVersion = ambariServerVersion;
      this.rackTopology = blueprint.getRackTopology();

      HdfsVersion hdfs = getDefaultHdfsVersion(this.version);
      if (blueprint.hasTopologyPolicy() && !AmUtils.isAmbariServerGreaterOrEquals_2_1_0(ambariServerVersion)) {
         setRackTopologyFileName(blueprint);
      }
      setAdditionalConfigurations(blueprint, ambariServerVersion);

      this.configurations = AmUtils.toAmConfigurations(blueprint.getConfiguration());

      this.nodeGroups = new ArrayList<AmNodeGroupDef>();
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {

         AmNodeGroupDef nodeGroupDef = new AmNodeGroupDef();
         nodeGroupDef.setName(group.getName());
         nodeGroupDef.setInstanceNum(group.getInstanceNum());
         nodeGroupDef.setRoles(group.getRoles());
         nodeGroupDef.setConfigurations(AmUtils.toAmConfigurations(group.getConfiguration()));
         nodeGroupDef.setClusterName(this.name);
         nodeGroupDef.setAmbariServerVersion(ambariServerVersion);

         List<AmNodeDef> nodes = new ArrayList<AmNodeDef>();
         for (NodeInfo node : group.getNodes()) {
            AmNodeDef nodeDef = new AmNodeDef();
            nodeDef.setName(node.getName());
            nodeDef.setIp(node.getMgtIpAddress());
            nodeDef.setFqdn(node.getHostname());
            nodeDef.setRackInfo(node.getRack());
            nodeDef.setConfigurations(AmUtils.toAmConfigurations(group
                  .getConfiguration()));
            nodeDef.setComponents(group.getRoles());
            nodeDef.setVolumes(node.getVolumes());
            nodeDef.setDirsConfig(hdfs, ambariServerVersion);
            nodes.add(nodeDef);
         }
         nodeGroupDef.setNodes(nodes);

         this.nodeGroups.add(nodeGroupDef);
      }
      if (blueprint.getExternalNamenode() != null) {
         this.isComputeOnly = true;

         this.externalNamenode = blueprint.getExternalNamenode();
         this.externalSecondaryNamenode = blueprint.getExternalSecondaryNamenode();

         String externalNameNodeGroupName = "external_namenode";
         AmNodeDef namenodeDef = new AmNodeDef();
         namenodeDef.setName(name + "-" + externalNameNodeGroupName + "-0");
         namenodeDef.setFqdn(externalNamenode);
         List<String> namenodeRoles = new ArrayList<String>();
         namenodeRoles.add("NAMENODE");
         if (!isValidExternalSecondaryNamenode()) {
            namenodeRoles.add("SECONDARY_NAMENODE");
         }
         namenodeDef.setComponents(namenodeRoles);
         namenodeDef.setVolumes(new ArrayList<String>());
         namenodeDef.setConfigurations(AmUtils.toAmConfigurations(null));

         AmNodeGroupDef externalNameNodeGroup = new AmNodeGroupDef();
         externalNameNodeGroup.setName(externalNameNodeGroupName);
         externalNameNodeGroup.setConfigurations(AmUtils.toAmConfigurations(null));
         externalNameNodeGroup.setRoles(namenodeRoles);
         externalNameNodeGroup.setInstanceNum(1);

         List<AmNodeDef> externalNameNodes = new ArrayList<AmNodeDef>();
         externalNameNodes.add(namenodeDef);
         externalNameNodeGroup.setNodes(externalNameNodes);

         this.nodeGroups.add(externalNameNodeGroup);

         if (isValidExternalSecondaryNamenode()) {
            String externalSecondaryNameNodeGroupName = "external_secondaryNamenode";

            AmNodeDef secondaryNamenodeDef = new AmNodeDef();
            secondaryNamenodeDef.setName(name + "-" + externalSecondaryNameNodeGroupName + "-0");
            secondaryNamenodeDef.setFqdn(externalSecondaryNamenode);
            List<String> secondaryNamenodeRoles = new ArrayList<String>();
            secondaryNamenodeRoles.add("SECONDARY_NAMENODE");
            secondaryNamenodeDef.setComponents(secondaryNamenodeRoles);
            secondaryNamenodeDef.setVolumes(new ArrayList<String>());
            secondaryNamenodeDef.setConfigurations(AmUtils.toAmConfigurations(null));

            AmNodeGroupDef externalSecondaryNameNodeGroup = new AmNodeGroupDef();
            externalSecondaryNameNodeGroup.setName(externalSecondaryNameNodeGroupName);
            externalSecondaryNameNodeGroup.setConfigurations(AmUtils.toAmConfigurations(null));
            externalSecondaryNameNodeGroup.setRoles(secondaryNamenodeRoles);
            externalSecondaryNameNodeGroup.setInstanceNum(1);

            List<AmNodeDef> externalSecondaryNameNodes = new ArrayList<AmNodeDef>();
            externalSecondaryNameNodes.add(secondaryNamenodeDef);
            externalSecondaryNameNodeGroup.setNodes(externalSecondaryNameNodes);

            this.nodeGroups.add(externalSecondaryNameNodeGroup);
         }

      }

      AmStackDef stackDef = new AmStackDef();
      stackDef.setName(blueprint.getHadoopStack().getVendor());
      stackDef.setVersion(blueprint.getHadoopStack().getFullVersion());
      this.amStack = stackDef;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public boolean isVerbose() {
      return verbose;
   }

   public void setVerbose(boolean verbose) {
      this.verbose = verbose;
   }

   public String getSshKey() {
      return sshKey;
   }

   public void setSshKey(String sshKey) {
      this.sshKey = sshKey;
   }

   public String getUser() {
      return user;
   }

   public void setUser(String user) {
      this.user = user;
   }

   public AmStackDef getAmStack() {
      return amStack;
   }

   public void setAmStack(AmStackDef amStack) {
      this.amStack = amStack;
   }

   public ClusterReport getCurrentReport() {
      return currentReport;
   }

   public void setCurrentReport(ClusterReport currentReport) {
      this.currentReport = currentReport;
   }

   public List<Map<String, Object>> getConfigurations() {
      return configurations;
   }

   public void setConfigurations(List<Map<String, Object>> configurations) {
      this.configurations = configurations;
   }

   public boolean isComputeOnly() {
      return isComputeOnly;
   }

   public void setComputeOnly(boolean isComputeOnly) {
      this.isComputeOnly = isComputeOnly;
   }

   public String getExternalNamenode() {
      return externalNamenode;
   }

   public void setExternalNamenode(String externalNamenode) {
      this.externalNamenode = externalNamenode;
   }

   public String getExternalSecondaryNamenode() {
      return externalSecondaryNamenode;
   }

   public void setExternalSecondaryNamenode(String externalSecondaryNamenode) {
      this.externalSecondaryNamenode = externalSecondaryNamenode;
   }

   public List<AmNodeGroupDef> getNodeGroups() {
      return nodeGroups;
   }

   public void setNodeGroups(List<AmNodeGroupDef> nodeGroups) {
      this.nodeGroups = nodeGroups;
   }

   public Map<String, String> getRackTopology() {
      return rackTopology;
   }

   public void setRackTopology(Map<String, String> rackTopology) {
      this.rackTopology = rackTopology;
   }

   public ApiBootstrap toApiBootStrap() {
      return toApiBootStrap(null);
   }

   public ApiBootstrap toApiBootStrap(List<String> hostNames) {
      ApiBootstrap apiBootstrap = new ApiBootstrap();
      apiBootstrap.setVerbose(verbose);
      List<String> hosts = new ArrayList<String>();
      for (AmNodeDef node : getNodes()) {

         // Generate all hosts for bootstrap except external namenode and secondary namenode
         if (isNodeGenerateFromExternalNamenode(node)) {
            continue;
         }

         if (isNodeGenerateFromExternalSecondaryNamenode(node)) {
            continue;
         }

         if (hostNames == null) {
            hosts.add(node.getFqdn());
         } else if (hostNames.contains(node.getName())) {
            hosts.add(node.getFqdn());
         }
      }
      apiBootstrap.setHosts(hosts);
      apiBootstrap.setSshKey(sshKey);
      apiBootstrap.setUser(user);
      if (!AmUtils.isAmbariServerBelow_2_0_0(ambariServerVersion)) {
         apiBootstrap.setUserRunAs(Configuration.getString("ambari.user_run_as"));
      }
      return apiBootstrap;
   }

   public ApiBlueprint toApiBlueprint() {
      ApiBlueprint apiBlueprint = new ApiBlueprint();

      apiBlueprint.setConfigurations(configurations);

      ApiBlueprintInfo apiBlueprintInfo = new ApiBlueprintInfo();
      apiBlueprintInfo.setStackName(amStack.getName());
      apiBlueprintInfo.setStackVersion(amStack.getVersion());
      apiBlueprint.setApiBlueprintInfo(apiBlueprintInfo);

      List<ApiHostGroup> apiHostGroups = new ArrayList<ApiHostGroup>();
      for (AmNodeGroupDef nodeGroup : nodeGroups) {
         apiHostGroups.addAll(nodeGroup.toApiHostGroupsForBlueprint());
      }
      apiBlueprint.setApiHostGroups(apiHostGroups);
      return apiBlueprint;
   }

   public int getNeedBootstrapHostCount(List<String> addedHosts) {
      int needBootstrapHostCount = -1;

      if (addedHosts == null) {

         needBootstrapHostCount = getNodes().size();

         if (isValidExternalNamenode()) {
            needBootstrapHostCount -= 1;
         }

         if (isValidExternalSecondaryNamenode()) {
            needBootstrapHostCount -= 1;
         }
      } else {
         needBootstrapHostCount = addedHosts.size();
      }

      return needBootstrapHostCount;
   }

   public String getAmbariServerVersion() {
      return ambariServerVersion;
   }

   public void setAmbariServerVersion(String ambariServerVersion) {
      this.ambariServerVersion = ambariServerVersion;
   }

   public ApiClusterBlueprint toApiClusterBlueprint() {
      ApiClusterBlueprint apiClusterBlueprint = new ApiClusterBlueprint();
      apiClusterBlueprint.setBlueprint(name);

      List<ApiHostGroup> apiHostGroups = new ArrayList<ApiHostGroup>();
      for (AmNodeGroupDef nodeGroup : nodeGroups) {
         apiHostGroups.addAll(nodeGroup.toApiHostGroupForClusterBlueprint());
      }
      apiClusterBlueprint.setApiHostGroups(apiHostGroups);
      return apiClusterBlueprint;
   }

   public boolean isValidExternalNamenode() {
      return this.externalNamenode != null && !this.externalNamenode.isEmpty();
   }

   public boolean isValidExternalSecondaryNamenode() {
      return this.externalSecondaryNamenode != null && !this.externalSecondaryNamenode.isEmpty() && !this.externalSecondaryNamenode.equals(this.externalNamenode);
   }

   public List<AmNodeDef> getNodes() {
      List<AmNodeDef> nodes = new ArrayList<AmNodeDef>();
      for (AmNodeGroupDef nodeGroup : this.nodeGroups) {
         nodes.addAll(nodeGroup.getNodes());
      }
      return nodes;
   }

   public List<AmNodeGroupDef> getNodeGroupsByNodes(List<AmNodeDef> originalNodes) {
      Map<String, AmNodeGroupDef> nodeGroupsMap = new HashMap<String, AmNodeGroupDef>();
      for (AmNodeDef node : originalNodes) {
         AmNodeGroupDef nodeGroup = getNodeGroupByNode(node);
         if (nodeGroup != null && !nodeGroupsMap.containsKey(nodeGroup.getName())) {
            nodeGroupsMap.put(nodeGroup.getName(), nodeGroup);
         }
      }
      List<AmNodeGroupDef> nodeGroups = new ArrayList<AmNodeGroupDef>();
      nodeGroups.addAll(nodeGroupsMap.values());
      return nodeGroups;
   }

   public AmNodeGroupDef getNodeGroupByNode(AmNodeDef originalNode) {
      AmNodeGroupDef nodeGroupbyNode = null;
      for (AmNodeGroupDef nodeGroup : this.nodeGroups) {
         for (AmNodeDef node : nodeGroup.getNodes()) {
            if (node.getName().equals(originalNode.getName())) {
               nodeGroupbyNode = nodeGroup;
               break;
            }
         }
      }
      return nodeGroupbyNode;
   }

   public List<AmHostGroupInfo> getAmHostGroupsInfoByNodeGroups(List<AmNodeGroupDef> nodeGroups, Map<String, String> configTypeToService) {
      List<AmHostGroupInfo> amHostGroupsInfo = new ArrayList<AmHostGroupInfo>();
      for (AmNodeGroupDef nodeGroup : nodeGroups) {
         amHostGroupsInfo.addAll(nodeGroup.generateHostGroupsInfo(configTypeToService));
      }
      return amHostGroupsInfo;
   }

   public Map<String, Set<String>> getRackHostsMap(List<String> addedNodeNames) {
      if (this.rackTopology == null || !AmUtils.isAmbariServerGreaterOrEquals_2_1_0(ambariServerVersion)) {
         return null;
      }

      Map<String, Set<String>> rackHostsMap = new HashMap<String, Set<String>> ();

      List<AmNodeDef> nodes = getNodes();
      for (AmNodeDef node : nodes) {

         // This logic is just for cluster resize
         if (addedNodeNames != null && !addedNodeNames.contains(node.getName())) {
            continue;
         }

         String rack = this.rackTopology.get(node.getIp());
         if (rack != null) {
            Set<String> hosts = rackHostsMap.get(rack);
            if (hosts == null) {
               hosts = new HashSet<String> ();
            }
            hosts.add(node.getFqdn());
            rackHostsMap.put(rack, hosts);
         }
      }

      return rackHostsMap;
   }

   private boolean isNodeGenerateFromExternalNamenode(AmNodeDef node) {
      return isValidExternalNamenode() && this.externalNamenode.equals(node.getFqdn());
   }

   private boolean isNodeGenerateFromExternalSecondaryNamenode(AmNodeDef node) {
      return isValidExternalSecondaryNamenode() && this.externalSecondaryNamenode.equals(node.getFqdn());
   }

   private static HdfsVersion getDefaultHdfsVersion(String distroVersion) {
      if (distroVersion.startsWith("2")) {
         return HdfsVersion.V2;
      } else {
         return HdfsVersion.V1;
      }
   }

   @SuppressWarnings("unchecked")
   private void setRackTopologyFileName(ClusterBlueprint blueprint) {
      String rackTopologyFileName = "/etc/hadoop/conf/topology.sh";
      Map<String, Object> conf = blueprint.getConfiguration();
      if (conf == null) {
         conf = new HashMap<String, Object>();
         blueprint.setConfiguration(conf);
      }

      Map<String, Object> confCoreSite = (Map<String, Object>) conf.get("core-site");
      if (confCoreSite == null) {
         confCoreSite = new HashMap<String, Object>();
         conf.put("core-site", confCoreSite);
      }
      if (confCoreSite.get("net.topology.script.file.name") == null) {
         confCoreSite.put("net.topology.script.file.name", rackTopologyFileName);
      }
      if (confCoreSite.get("topology.script.file.name") == null) {
         confCoreSite.put("topology.script.file.name", rackTopologyFileName);
      }
   }

   private void setAdditionalConfigurations(ClusterBlueprint blueprint, String ambariServerVersion) {
      if (AmUtils.isAmbariServerBelow_2_0_0(ambariServerVersion) ||
            !AmUtils.containsRole(blueprint, "RESOURCEMANAGER")) {
         return;
      }

      Map<String, Object> conf = blueprint.getConfiguration();
      if (conf == null) {
         conf = new HashMap<String, Object>();
         blueprint.setConfiguration(conf);
      }

      Map<String, Object> confYarnSite = (Map<String, Object>) conf.get("yarn-site");
      if (confYarnSite == null) {
         confYarnSite = new HashMap<String, Object>();
         conf.put("yarn-site", confYarnSite);
      }

      if (confYarnSite.get("yarn.resourcemanager.webapp.https.address") == null) {
         List<String> fqdnsOfRoleResourcemanager = getFqdnsWithRole(blueprint, "RESOURCEMANAGER");
         if (!fqdnsOfRoleResourcemanager.isEmpty()) {
            confYarnSite.put("yarn.resourcemanager.webapp.https.address", fqdnsOfRoleResourcemanager.get(0) + ":8090");
         }
      }
      if (confYarnSite.get("yarn.resourcemanager.zk-address") == null) {
         List<String> fqdnsOfRoleZookeeperServer = getFqdnsWithRole(blueprint, "ZOOKEEPER_SERVER");
         if (!fqdnsOfRoleZookeeperServer.isEmpty()) {
            confYarnSite.put("yarn.resourcemanager.zk-address", fqdnsOfRoleZookeeperServer.get(0) + ":2181");
         }
      }
   }

   private List<String> getFqdnsWithRole(ClusterBlueprint blueprint, String role) {
      List<String> fqdns = new ArrayList<String>();

      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         if (group.getRoles().contains(role)) {
            for (NodeInfo node : group.getNodes()) {
               fqdns.add(node.getHostname());
            }
         }
      }

      return fqdns;
   }
}
