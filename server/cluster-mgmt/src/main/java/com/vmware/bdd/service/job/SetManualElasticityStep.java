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

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.apitypes.LimitInstruction;
import com.vmware.bdd.command.MessageHandler;
import com.vmware.bdd.command.VHMMessageTask;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.task.VHMReceiveListener;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.IExecutionService;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

public class SetManualElasticityStep extends TrackableTasklet {
   IExecutionService executionService;
   IClusteringService clusteringService;
   String vhmAction;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      Map<String, JobParameter> allParameters = getJobParameters(chunkContext).getParameters();
      if (!allParameters.containsKey(JobConstants.VHM_ACTION_JOB_PARAM) && !allParameters.containsKey(JobConstants.ACTIVE_COMPUTE_NODE_NUMBER_JOB_PARAM)) {
         return RepeatStatus.FINISHED;
      }
      String clusterName = getJobParameters(chunkContext).getString(
            JobConstants.CLUSTER_NAME_JOB_PARAM);
      Long activeComputeNodeNum = getJobParameters(chunkContext).getLong(
            JobConstants.ACTIVE_COMPUTE_NODE_NUMBER_JOB_PARAM);
      String action = getJobParameters(chunkContext).getString(JobConstants.VHM_ACTION_JOB_PARAM);

      // action value got from Job Parameters will override that initialized by Spring Bean
      if (action != null) {
         vhmAction = action;
      }

      // submit a MQ task
      MessageHandler listener = null;
      if (vhmAction == LimitInstruction.actionSetTarget || vhmAction == LimitInstruction.actionUnlimit) {
         StatusUpdater statusUpdater = new DefaultStatusUpdater(jobExecutionStatusHolder,
               getJobExecutionId(chunkContext));
         listener = new VHMReceiveListener(clusterName, statusUpdater);
      }

      Map<String, Object> sendParam = new HashMap<String, Object>();
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_VERSION, Constants.VHM_PROTOCOL_VERSION);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_CLUSTER_NAME, clusterName);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_INSTANCE_NUM, activeComputeNodeNum);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_RECEIVE_ROUTE_KEY, CommonUtil.getUUID());
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_ACTION, vhmAction);

      Map<String, Object> ret = executionService.execute(new VHMMessageTask(sendParam,
            listener));

      if (!(Boolean) ret.get("succeed")) {
         String errorMessage = (String) ret.get("errorMessage");
         putIntoJobExecutionContext(chunkContext, JobConstants.CURRENT_ERROR_MESSAGE,
               errorMessage);
         throw TaskException.EXECUTION_FAILED(errorMessage);
      }

      return RepeatStatus.FINISHED;
   }

   private boolean disableAutoElasticity(String clusterName) {
      AuAssert.check(clusteringService != null);
      AuAssert.check(getClusterEntityMgr() != null);
      ClusterEntity clusterEntity = getClusterEntityMgr().findByName(clusterName);
      if (clusterEntity.getAutomationEnable() == null || !clusterEntity.getAutomationEnable()) {
         return true;
      }
      clusterEntity.setAutomationEnable(false);
      getClusterEntityMgr().update(clusterEntity);
      return clusteringService.setAutoElasticity(clusterName, false);
   }

   public IExecutionService getExecutionService() {
      return executionService;
   }

   public void setExecutionService(IExecutionService executionService) {
      this.executionService = executionService;
   }

   public String getVhmAction() {
      return vhmAction;
   }

   public void setVhmAction(String vhmAction) {
      this.vhmAction = vhmAction;
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }
}
