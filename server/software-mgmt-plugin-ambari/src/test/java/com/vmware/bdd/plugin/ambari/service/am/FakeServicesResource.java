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
package com.vmware.bdd.plugin.ambari.service.am;

import javax.ws.rs.core.Response;

import com.vmware.bdd.plugin.ambari.api.v1.resource.stacks.ComponentsResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.stacks.ServicesResource;

public class FakeServicesResource implements ServicesResource {

   private String stackVersion;

   public FakeServicesResource(String stackVersion) {
      this.stackVersion = stackVersion;
   }

   @Override
   public Response readServices() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Response readServicesWithFilter(String fields) {
      if ("configurations/StackConfigurations/type".equals(fields)) {
         return BuildResponse.buildResponse("stacks/versions/" + stackVersion + "/stackServices/simple_configurations.json");
      } else {
         return BuildResponse.buildResponse("stacks/versions/" + stackVersion + "/stackServices/simple_services.json");
      }
   }

   @Override
   public Response readService(String stackServiceName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Response readServiceWithFilter(String stackServiceName, String fields) {
   // TODO Auto-generated method stub
      return null;
   }

   @Override
   public ComponentsResource getComponentsResource(String stackServiceName) {
      // TODO Auto-generated method stub
      return null;
   }


}
