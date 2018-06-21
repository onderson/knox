/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.knox.gateway.util;

import org.apache.knox.gateway.SpiGatewayMessages;
import org.apache.knox.gateway.config.GatewayConfig;
import org.apache.knox.gateway.i18n.messages.MessagesFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WhitelistUtils {

  static final String LOCALHOST_REGEXP_SEGMENT = "(localhost|127\\.0\\.0\\.1|0:0:0:0:0:0:0:1|::1)";

  static final String LOCALHOST_REGEXP = "^" + LOCALHOST_REGEXP_SEGMENT + "$";

  static final String DEFAULT_DISPATCH_WHITELIST_TEMPLATE = "^/.*$;^https?://%s:[0-9]+/?.*$";

  private static final SpiGatewayMessages LOG = MessagesFactory.get(SpiGatewayMessages.class);

  private static final List<String> DEFAULT_SERVICE_ROLES = Arrays.asList("KNOXSSO");


  public static String getDispatchWhitelist(HttpServletRequest request) {
    String whitelist = null;

    GatewayConfig config = (GatewayConfig) request.getServletContext().getAttribute("org.apache.knox.gateway.config");
    if (config != null) {
      List<String> whitelistedServiceRoles = new ArrayList<>();
      whitelistedServiceRoles.addAll(DEFAULT_SERVICE_ROLES);
      whitelistedServiceRoles.addAll(config.getDispatchWhitelistServices());

      String serviceRole = (String) request.getAttribute("targetServiceRole");
      if (whitelistedServiceRoles.contains(serviceRole)) {
        // Check the whitelist against the URL to be dispatched
        whitelist = config.getDispatchWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
          whitelist = deriveDefaultDispatchWhitelist(request);
          LOG.derivedDispatchWhitelist(whitelist);
        }
      }
    }

    return whitelist;
  }

  private static String deriveDefaultDispatchWhitelist(HttpServletRequest request) {
    String defaultWhitelist = null;

    String thisHost = request.getHeader("X-Forwarded-Host");
    if (thisHost == null) {
      thisHost = request.getServerName();
    }

    // If the host is not some form of localhost, try to determine its domain
    if (!thisHost.matches(LOCALHOST_REGEXP)) {
      int domainIndex = thisHost.indexOf('.');
      if (domainIndex > 0) {
        String domain = thisHost.substring(thisHost.indexOf('.'));
        String domainPattern = ".+" + domain.replaceAll("\\.", "\\\\.");
        defaultWhitelist = String.format(DEFAULT_DISPATCH_WHITELIST_TEMPLATE, domainPattern);
      }
    }

    // If the host is localhost or the domain could not be determined, default to the local/relative whitelist
    if (defaultWhitelist == null) {
      defaultWhitelist = String.format(DEFAULT_DISPATCH_WHITELIST_TEMPLATE, LOCALHOST_REGEXP_SEGMENT);
    }

    return defaultWhitelist;
  }


}
