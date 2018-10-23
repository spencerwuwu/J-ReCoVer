// https://searchcode.com/api/result/105623086/

/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.sakaiproject.kernel.presence.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.api.presence.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet deals with HTML inputs only and the presence POST/DELETE requests
 * 
 * Not using the sling selector here ///scr.property name="sling.servlet.selectors"
 * value="current"
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/presence"
 * @scr.property name="sling.servlet.methods" values.0="POST" values.1="PUT"
 *               values.2="DELETE"
 * @scr.property name="sling.servlet.extensions" value="html"
 * 
 * @scr.reference name="PresenceService"
 *                interface="org.sakaiproject.kernel.api.presence.PresenceService"
 */
public class PresenceControlServlet extends SlingAllMethodsServlet {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PresenceControlServlet.class);

  private static final long serialVersionUID = 11111111L;

  protected PresenceService presenceService;

  protected void bindPresenceService(PresenceService presenceService) {
    this.presenceService = presenceService;
  }

  protected void unbindPresenceService(PresenceService presenceService) {
    this.presenceService = null;
  }

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // get current user
    String user = request.getRemoteUser();
    if (user == null) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "User must be logged in to ping their status and set location");
    }
    LOGGER.info("POST to PresenceControlServlet (" + user + ")");

    String location = null; // null location will clear the location
    RequestParameter locationParam = request
        .getRequestParameter(PresenceService.PRESENCE_LOCATION_PROP);
    if (locationParam != null) {
      // update the status to something from the request parameter
      location = locationParam.getString("UTF-8");
    }
    try {
      presenceService.ping(user, location);
    } catch (Exception e) {
      response
          .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Failure setting current user (" + user + ") location (" + location + "): "
                  + e);
    }
    response.setContentLength(0);
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  @Override
  protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // get current user
    String user = request.getRemoteUser();
    if (user == null) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "User must be logged in to set their status");
    }
    LOGGER.info("PUT to PresenceControlServlet (" + user + ")");

    String status = null; // null status will clear the status
    RequestParameter statusParam = request
        .getRequestParameter(PresenceService.PRESENCE_STATUS_PROP);
    if (statusParam != null) {
      // update the status to something from the request parameter
      status = statusParam.getString("UTF-8");
    }
    // PUT allows a ping to happen as well to reduce number of requests
    String location = null; // null location will cause no change
    RequestParameter locationParam = request
        .getRequestParameter(PresenceService.PRESENCE_LOCATION_PROP);
    if (locationParam != null) {
      // update the status to something from the request parameter
      location = locationParam.getString("UTF-8");
    }
    try {
      if (location != null) {
        presenceService.ping(user, location);
      }
      presenceService.setStatus(user, status);
    } catch (Exception e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failure setting current user (" + user + ") status (" + status + ")"
              + (location == null ? "" : " and location (" + location + ")") + ": " + e);
    }
    response.setContentLength(0);
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  @Override
  protected void doDelete(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    // get current user
    String user = request.getRemoteUser();
    if (user == null) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "User must be logged in to control their status");
    }
    LOGGER.info("DELETE to PresenceControlServlet (" + user + ")");

    try {
      presenceService.clear(user);
    } catch (Exception e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failure deleting current user (" + user + ") status: " + e);
    }
    response.setContentLength(0);
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

}

