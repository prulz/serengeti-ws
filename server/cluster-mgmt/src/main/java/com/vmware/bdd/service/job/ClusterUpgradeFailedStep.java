/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.service.IClusterUpgradeService;

public class ClusterUpgradeFailedStep extends TrackableTasklet {
   IClusterUpgradeService clusterUpgradeService;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);

      StatusUpdater statusUpdator = new DefaultStatusUpdater(jobExecutionStatusHolder, getJobExecutionId(chunkContext));
      boolean upgradeFailed = clusterUpgradeService.upgradeFailed(clusterName, statusUpdator);
      if (upgradeFailed) {
         throw TaskException.EXECUTION_FAILED("Failed to upgrade cluster " + clusterName);
      }
      return RepeatStatus.FINISHED;
   }

   public IClusterUpgradeService getClusterUpgradeService() {
      return clusterUpgradeService;
   }

   public void setClusterUpgradeService(IClusterUpgradeService clusterUpgradeService) {
      this.clusterUpgradeService = clusterUpgradeService;
   }

}
