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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.HostComponentsResource;

public class FakeHostComponentsResource implements HostComponentsResource {

   @Override
   @PUT
   @Path("/")
   @Consumes("application/xml")
   @Produces({ "application/json", "application/xml", "text/plain" })
   public Response operationWithFilter(String request) {
      return BuildResponse.buildResponse("clusters/simple_request.json");
   }

   @Override
   @DELETE
   @Path("/")
   public Response deleteAllComponents() {
      return BuildResponse.buildResponse("clusters/simple_request.json");
   }

   @Override
   public Response readComponentsAfterConfigChange(String fields, String stale_configs) {
      // TODO Auto-generated method stub
      return null;
   }


}
