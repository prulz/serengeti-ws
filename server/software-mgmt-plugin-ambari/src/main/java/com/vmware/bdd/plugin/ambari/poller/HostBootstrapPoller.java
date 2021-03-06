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
package com.vmware.bdd.plugin.ambari.poller;

import java.util.Map;

import org.apache.log4j.Logger;

import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrapHostStatus;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.BootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHost;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostList;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.StatusPoller;

public class HostBootstrapPoller extends StatusPoller {

   private static final Logger logger = Logger
         .getLogger(HostBootstrapPoller.class);

   private ApiManager apiManager;
   private ApiBootstrap apiBootstrap;
   private ClusterReport currentReport;
   private ClusterReportQueue reportQueue;
   private int endProgress;

   public HostBootstrapPoller(final ApiManager apiManager,
         final ApiBootstrap apiBootstrap, final ClusterReport currentReport,
         final ClusterReportQueue reportQueue, int endProgress) {
      this.apiManager = apiManager;
      this.apiBootstrap = apiBootstrap;
      this.currentReport = currentReport;
      this.reportQueue = reportQueue;
      this.endProgress = endProgress;
   }

   @Override
   public boolean poll() {
      Long requestId = apiBootstrap.getRequestId();
      logger.info("Waiting for bootstrap hosts request " + requestId
            + " to complete.");
      ApiBootstrapStatus apiBootstrapStatus = apiManager.getBootstrapStatus(requestId);
      if (apiBootstrapStatus.getApiBootstrapHostStatus() == null) {
         return false;
      }

      // wait for all hosts registration
      int registeredHostsCount = 0;
      ApiHostList apiHostList = apiManager.getRegisteredHosts();
      for ( ApiBootstrapHostStatus apiBootstrapHostStatus : apiBootstrapStatus.getApiBootstrapHostStatus()) {
         for (ApiHost apiHost : apiHostList.getApiHosts()) {
            if (apiHost.getApiHostInfo().getHostName().equals(apiBootstrapHostStatus.getHostName())) {
               registeredHostsCount++;
            }
         }
      }
      int bootstrapedHostCount = apiBootstrapStatus.getApiBootstrapHostStatus().size();
      BootstrapStatus bootstrapStatus = BootstrapStatus.valueOf(apiBootstrapStatus.getStatus());
      if (bootstrapStatus.isFailedState()
            || (bootstrapStatus.isSucceedState() && bootstrapedHostCount == registeredHostsCount)) {
         if (bootstrapStatus.isFailedState()) {
            Map<String, NodeReport> nodeReports = currentReport.getNodeReports();
            for (String nodeReportKey : nodeReports.keySet()) {
               for (ApiBootstrapHostStatus apiBootstrapHostStatus : apiBootstrapStatus.getApiBootstrapHostStatus()) {
                  if (Constants.HOST_BOOTSTRAP_FAILED.equals(apiBootstrapHostStatus.getStatus())) {
                     NodeReport nodeReport = nodeReports.get(nodeReportKey);
                     if (nodeReport.getHostname() != null && nodeReport.getHostname().equals(apiBootstrapHostStatus.getHostName())) {
                        nodeReport.setUseClusterMsg(false);
                        nodeReport.setAction("Failed to bootstrap host");
                        nodeReport.setErrMsg(apiBootstrapHostStatus.getLog());
                     }
                  }
               }
            }
         }
         currentReport.setProgress(endProgress);
         reportQueue.addClusterReport(currentReport.clone());
         return true;
      }

      return false;
   }

}
