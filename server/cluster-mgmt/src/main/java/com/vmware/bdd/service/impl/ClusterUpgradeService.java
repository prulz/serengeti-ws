package com.vmware.bdd.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.IClusterUpgradeService;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.bdd.service.sp.NodeUpgradeSP;
import com.vmware.bdd.utils.Constants;

public class ClusterUpgradeService implements IClusterUpgradeService {
   private static final Logger logger = Logger
         .getLogger(ClusterUpgradeService.class);

   private IClusterEntityManager clusterEntityMgr;
   private String serverVersion;

   @Override
   public boolean upgrade(final String clusterName, StatusUpdater statusUpdator) {
      logger.info("Upgrading cluster " + clusterName + ".");

      this.serverVersion = clusterEntityMgr.getServerVersion();
      List<NodeEntity> nodes = getNodes(clusterName);

      List<Callable<Void>> storeNodeProcedures = new ArrayList<Callable<Void>>();

      try {
         for (NodeEntity node : nodes) {
            if (NeedUpgrade(node)) {
               setActionToUpgrading(node);
               NodeUpgradeSP nodeUpgradeSP = new NodeUpgradeSP(node, serverVersion);
               storeNodeProcedures.add(nodeUpgradeSP);
            }
         }

         if (storeNodeProcedures.isEmpty()) {
            logger.info("no VM is available. Return directly.");
            return true;
         }

         Callable<Void>[] storeNodeProceduresArray = storeNodeProcedures.toArray(new Callable[0]);
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeNodeProceduresArray, callback);

         if (result == null || result.length == 0) {
            logger.error("No node is upgraded.");
            return false;
         }

         boolean success = true;
         int total = 0;
         for (int i = 0; i < storeNodeProceduresArray.length; i++) {
            Throwable nodeUpgradeSPException = result[i].throwable;
            NodeUpgradeSP sp = (NodeUpgradeSP) storeNodeProceduresArray[i];
            NodeEntity node = sp.getNode();
            if (result[i].finished && nodeUpgradeSPException == null) {
               updateNodeData(node);
               ++total;
            } else if (nodeUpgradeSPException != null) {
               updateNodeData(node, false, nodeUpgradeSPException.getMessage());
               logger.error("Failed to Upgrade cluster Node " + node.getVmName(), nodeUpgradeSPException);
               success = false;
            }
         }
         logger.info(total + " Nodes are upgraded.");

         return success;
      } catch (InterruptedException e) {
         logger.error("error in upgrading cluster nodes", e);
         throw BddException.UPGRADE(e, e.getMessage());
      }
   }

   @Override
   public boolean upgradeFailed(final String clusterName, StatusUpdater statusUpdator) {
      boolean upgradeFailed = false;
      List<NodeEntity> nodes = getNodes(clusterName);
      for (NodeEntity node : nodes) {
         if (Constants.NODE_ACTION_UPGRADE_FAILED.equals(node.getAction())) {
            upgradeFailed = true;
            break;
         }
      }
      return upgradeFailed;
   }

   private List<NodeEntity> getNodes(String clusterName) {
      return clusterEntityMgr.findAllNodes(clusterName);
   }

   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   private void updateNodeData(NodeEntity node) {
      updateNodeData(node, true, null);
   }

   @Transactional
   private void updateNodeData(NodeEntity node, boolean upgraded, String errorMessage) {
      node = clusterEntityMgr.findNodeById(node.getId());
      String nodeVmName = node.getVmName();
      String nodeIp = node.getPrimaryMgtIpV4();
      if (upgraded) {
         if (nodeIp != null && !Constants.NULL_IPV4_ADDRESS.equals(nodeIp)) {
            logger.info("Successfully upgrade cluster node " + nodeVmName);
            node.setVersion(serverVersion);
            node.setAction(Constants.NODE_ACTION_UPGRADE_SUCCESS);
            node.setActionFailed(false);
            node.setErrMessage(null);
         }
      } else {
         logger.error("Failed to upgrade cluster node " + nodeVmName);
         node.setAction(Constants.NODE_ACTION_UPGRADE_FAILED);
         node.setActionFailed(true);
         String[] messages = errorMessage.split(":");
         if (messages != null && messages.length > 0) {
            node.setErrMessage(messages[messages.length-1]);
         } else {
            node.setErrMessage("Upgrading node " + nodeVmName + " failed.");
         }
      }
   }

   private void setActionToUpgrading(NodeEntity node) {
      String nodeIp = node.getPrimaryMgtIpV4();
      if (nodeIp != null && !Constants.NULL_IPV4_ADDRESS.equals(nodeIp)) {
         logger.info("Set node " + node.getVmName() + " action to " + Constants.NODE_ACTION_UPGRADING);
         node.setAction(Constants.NODE_ACTION_UPGRADING);
      }
   }

   private boolean NeedUpgrade(NodeEntity node) {
      return (node.getMoId() != null && (node.getVersion() == null || !serverVersion.equals(node.getVersion())));
   }

}
