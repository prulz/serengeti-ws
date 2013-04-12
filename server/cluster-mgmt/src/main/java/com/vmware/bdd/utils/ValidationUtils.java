package com.vmware.bdd.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupRacks;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupRacks.GroupRacksType;
import com.vmware.bdd.apitypes.RackInfo;
import com.vmware.bdd.entity.NodeGroupAssociation;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.manager.RackInfoManager;

public class ValidationUtils {

   public static void hasEnoughHost(RackInfoManager rackInfoMgr,
         ClusterEntityManager clusterEntityMgr, NodeGroupEntity nodeGroup,
         int instanceNum) {
      if (nodeGroup.getInstancePerHost() != null) {
         // assume this value is already validated
         int requiredHostNum = instanceNum / nodeGroup.getInstancePerHost();

         if (nodeGroup.getGroupRacks() != null) {
            GroupRacks groupRacks =
                  new Gson().fromJson(nodeGroup.getGroupRacks(),
                        GroupRacks.class);
            GroupRacksType rackType = groupRacks.getType();

            List<RackInfo> racksInfo = rackInfoMgr.getRackInfos();

            Set<String> specifiedRacks =
                  new HashSet<String>(Arrays.asList(groupRacks.getRacks()));
            List<String> IntersecRacks = new ArrayList<String>();
            Integer IntersecHostNum = 0;
            Integer maxIntersecHostNum = 0;
            for (RackInfo rackInfo : racksInfo) {
               if (specifiedRacks.isEmpty() || specifiedRacks.size() == 0
                     || specifiedRacks.contains(rackInfo.getName())) {
                  IntersecHostNum += rackInfo.getHosts().size();
                  IntersecRacks.add(rackInfo.getName());
                  if (rackInfo.getHosts().size() > maxIntersecHostNum) {
                     maxIntersecHostNum = rackInfo.getHosts().size();
                  }
               }
            }

            if (rackType.equals(GroupRacksType.ROUNDROBIN)
                  && IntersecHostNum < requiredHostNum) {
               throw ClusterConfigException.LACK_PHYSICAL_HOSTS(
                     nodeGroup.getName(), requiredHostNum, IntersecHostNum);
            } else if (rackType.equals(GroupRacksType.SAMERACK)
                  && requiredHostNum > maxIntersecHostNum) {
               throw ClusterConfigException.LACK_PHYSICAL_HOSTS(
                     nodeGroup.getName(), requiredHostNum, maxIntersecHostNum);
            }

            if (specifiedRacks.isEmpty()) {
               groupRacks.setRacks(new String[0]);
            } else {
               groupRacks.setRacks(IntersecRacks
                     .toArray(new String[IntersecRacks.size()]));
            }
            nodeGroup.setGroupRacks((new Gson()).toJson(groupRacks));
            clusterEntityMgr.update(nodeGroup);
         }
      }
   }

   public static void validHostNumber(ClusterEntityManager clusterEntityMgr,
         NodeGroupEntity nodeGroup, int instanceNum) {
      Set<NodeGroupAssociation> associations = nodeGroup.getGroupAssociations();
      if (associations != null && !associations.isEmpty()) {
         AuAssert.check(associations.size() == 1,
               "only support 1 group association now");
         NodeGroupAssociation association = associations.iterator().next();
         if (association.getAssociationType() == GroupAssociationType.STRICT) {
            NodeGroupEntity refGroup =
                  clusterEntityMgr.findByName(nodeGroup.getCluster(),
                        association.getReferencedGroup());
            AuAssert.check(refGroup != null, "shold not happens");

            int hostNum = 1;
            int refHostNum = refGroup.getDefineInstanceNum();
            if (nodeGroup.getInstancePerHost() != null) {
               hostNum = instanceNum / nodeGroup.getInstancePerHost();
            }
            if (refGroup.getInstancePerHost() != null) {
               refHostNum =
                     refGroup.getDefineInstanceNum()
                           / refGroup.getInstancePerHost();
            }

            if (hostNum > refHostNum) {
               throw BddException.INVALID_PARAMETER("instance number",
                     new StringBuilder(100)
                           .append(instanceNum)
                           .append(
                                 ": required host number is larger "
                                       + "than the referenced node group")
                           .toString());
            }
         }
      }
   }

   @SuppressWarnings("unchecked")
   public static boolean validate(Map<String, Object> mMap, String clusterName) {
      if (mMap.get(Constants.FINISH_FIELD) instanceof Boolean
            && mMap.get(Constants.SUCCEED_FIELD) instanceof Boolean
            && mMap.get(Constants.PROGRESS_FIELD) instanceof Double
            && (Double) mMap.get(Constants.PROGRESS_FIELD) <= 100
            && mMap.get(Constants.CLUSTER_DATA_FIELD) != null
            && ((HashMap<String, Object>) mMap
                  .get(Constants.CLUSTER_DATA_FIELD)).get(
                  Constants.CLUSTER_NAME_FIELD).equals(clusterName)) {
         return true;
      }

      return false;
   }

}