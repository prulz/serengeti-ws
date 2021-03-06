/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.exception.BddException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.JobUtils;
import com.vmware.bdd.utils.VcVmUtil;

public class ResizeClusterRemoveBadNodeStep extends TrackableTasklet {
   private IClusteringService clusteringService;
   private ClusterConfigManager configMgr;
   private SoftwareManagerCollector softwareMgrs;

   @Autowired
   public void setSoftwareMgrs(SoftwareManagerCollector softwareMgrs) {
      this.softwareMgrs = softwareMgrs;
   }

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);
      String groupName =
            getJobParameters(chunkContext).getString(
                  JobConstants.GROUP_NAME_JOB_PARAM);
      long newInstanceNum =
         getJobParameters(chunkContext).getLong(
               JobConstants.GROUP_INSTANCE_NEW_NUMBER_JOB_PARAM);
      long oldInstanceNum =
         getJobParameters(chunkContext).getLong(
               JobConstants.GROUP_INSTANCE_OLD_NUMBER_JOB_PARAM);
      ClusterCreate clusterSpec = configMgr.getClusterConfig(clusterName);
      List<BaseNode> existingNodes = JobUtils.getExistingNodes(
            clusterSpec, getClusterEntityMgr());
      List<BaseNode> deletedNodes = new ArrayList<BaseNode>();
      removeExcessiveOrWrongStatusNodes(existingNodes, 
            deletedNodes, groupName, newInstanceNum, oldInstanceNum);
      Map<String, Set<String>> occupiedIps = new HashMap<String, Set<String>>();
      JobUtils.removeNonExistNodes(existingNodes, occupiedIps);
      verifyExistingSuccessNodes(existingNodes, 
            groupName, oldInstanceNum, clusterSpec);
      StatusUpdater statusUpdator = new DefaultStatusUpdater(
            jobExecutionStatusHolder, getJobExecutionId(chunkContext));
      deleteServices(getClusterEntityMgr(),
            softwareMgrs.getSoftwareManagerByClusterName(clusterName),
            deletedNodes);
      boolean deleted = false;
      try {
         deleted = clusteringService.syncDeleteVMs(deletedNodes,
               statusUpdator, false);
      } catch (BddException e) {
         String errMsg = "Failed to remove bad nodes for resizing cluster " + clusterName + ": " + e.getMessage();
         JobUtils.recordErrorInClusterOperation(chunkContext, errMsg);
         if (!JobUtils.getJobParameterForceClusterOperation(chunkContext)) {
            throw e;
         }
      }
      putIntoJobExecutionContext(chunkContext, 
            JobConstants.CLUSTER_EXISTING_NODES_JOB_PARAM, existingNodes);
      putIntoJobExecutionContext(chunkContext, 
            JobConstants.CLUSTER_SPEC_JOB_PARAM, clusterSpec);
      putIntoJobExecutionContext(chunkContext,
      JobConstants.CLUSTER_USED_IP_JOB_PARAM, occupiedIps);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_DELETED_NODES_JOB_PARAM, deletedNodes);
      putIntoJobExecutionContext(chunkContext, 
            JobConstants.CLUSTER_DELETE_VM_OPERATION_SUCCESS, deleted);
      return RepeatStatus.FINISHED;
   }

   /**
    * This method will verify placement policy for existing nodes in old defined instance scope.
    * If they violate the placement policy defined in cluster spec, we don't allow resize operation
    */
   private void verifyExistingSuccessNodes(List<BaseNode> existingNodes, 
         String groupName, long oldInstanceNum, ClusterCreate clusterSpec) {
      List<BaseNode> needToBeVerified = new ArrayList<BaseNode>();
      for(BaseNode node : existingNodes) {
         if (node.getGroupName().equals(groupName)) {
            long index = CommonUtil.getVmIndex(node.getVmName());
            if (index >= oldInstanceNum) {
               continue;
            }
         }
         needToBeVerified.add(node);
      }
      List<BaseNode> badNodes = clusteringService.getBadNodes(
            clusterSpec, needToBeVerified);
      if (badNodes != null && badNodes.size() > 0) {
         List<String> vmNames = new ArrayList<String>();
         for (BaseNode node : badNodes) {
            vmNames.add(node.getVmName());
         }
         throw ClusteringServiceException.VM_VIOLATE_PLACEMENT_POLICY(vmNames);
      }
   }

   private void removeExcessiveOrWrongStatusNodes(List<BaseNode> existingNodes,
         List<BaseNode> deletedNodes, String groupName, 
         long newInstanceNum, long oldInstanceNum) {
      for(BaseNode node : existingNodes) {
         if (node.getGroupName().equals(groupName)) {
            long index = CommonUtil.getVmIndex(node.getVmName());
            if (index >= newInstanceNum) {
               deletedNodes.add(node);
               continue;
            }
            if (index >= oldInstanceNum) {
               if (node.getVmMobId() == null) {
                  deletedNodes.add(node);
                  continue;
               }
               VcVirtualMachine vm = VcCache.getIgnoreMissing(node.getVmMobId());

               if(!JobUtils.checkVcVmReachable(node, vm)) {
                  deletedNodes.add(node);
               }
            }
         }
      }
      existingNodes.removeAll(deletedNodes);
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   public ClusterConfigManager getConfigMgr() {
      return configMgr;
   }

   public void setConfigMgr(ClusterConfigManager configMgr) {
      this.configMgr = configMgr;
   }


   public static void deleteServices(IClusterEntityManager clusterEntityMgr, 
         SoftwareManager softMgr, List<BaseNode> toBeDeleted) {
      if (toBeDeleted.isEmpty()) {
         return;
      }

      ClusterBlueprint blueprint =
            clusterEntityMgr.toClusterBluePrint(toBeDeleted.get(0)
                  .getClusterName());
      ClusterReportQueue queue = new ClusterReportQueue();
      List<String> nodeNames = new ArrayList<>();
      for (BaseNode node : toBeDeleted) {
         if (node.getVmMobId() != null) {
            nodeNames.add(node.getVmName());
         }
      }
      try {
         softMgr.onDeleteNodes(blueprint, nodeNames);
      } catch (Exception e) {
         logger.error("Failed to delete services on bad nodes: " + nodeNames);
      }
   }
}
