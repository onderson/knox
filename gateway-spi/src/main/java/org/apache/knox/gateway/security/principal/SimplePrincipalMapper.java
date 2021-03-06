/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.knox.gateway.security.principal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;

public class SimplePrincipalMapper implements PrincipalMapper {
  public HashMap<String, String[]> principalMappings = null;
  public HashMap<String, String[]> groupMappings = null;

  public SimplePrincipalMapper() {
  }
  
  /* (non-Javadoc)
   * @see org.apache.knox.gateway.filter.PrincipalMapper#loadMappingTable(java.lang.String)
   */
  @Override
  public void loadMappingTable(String principalMapping, String groupMapping) throws PrincipalMappingException {
//    System.out.println("+++++++++++++ Loading the Mapping Table");
    if (principalMapping != null) {
      principalMappings = parseMapping(principalMapping);
      groupMappings = parseMapping(groupMapping);
    }
  }

  private HashMap<String, String[]> parseMapping(String mappings)
      throws PrincipalMappingException {
    if (mappings == null) {
      return null;
    }
    HashMap<String, String[]> table = new HashMap<>();
    try {
      StringTokenizer t = new StringTokenizer(mappings, ";");
      if (t.hasMoreTokens()) {
        do {
          String mapping = t.nextToken();
   //        System.out.println("+++++++++++++ Mapping: " + mapping);
          String principals = mapping.substring(0, mapping.indexOf('='));
   //        System.out.println("+++++++++++++ Principals: " + principals);
          String value = mapping.substring(mapping.indexOf('=')+1);
          String[] v = value.split(",");
          String[] p = principals.split(",");
          for(int i = 0; i < p.length; i++) {
            table.put(p[i], v);
   //          System.out.println("+++++++++++++ Mapping into Table: " + p[i] + "->" + value);
          }
        } while(t.hasMoreTokens());
      }
      return table;
    }
    catch (Exception e) {
      // do not leave table in an unknown state - clear it instead
      // no principal mapping will occur
      table.clear();
      throw new PrincipalMappingException("Unable to load mappings from provided string: " + mappings + " - no principal mapping will be provided.", e);
    }
  }
  
  /* (non-Javadoc)
   * @see org.apache.knox.gateway.filter.PrincipalMapper#mapPrincipal(java.lang.String)
   */
  @Override
  public String mapUserPrincipal(String principalName) {
    String[] p = null;
    if (principalMappings != null) {
      p = principalMappings.get(principalName);
    }
    if (p == null) {
      return principalName;
    }
    
    return p[0];
  }

  /* (non-Javadoc)
   * @see org.apache.knox.gateway.filter.PrincipalMapper#mapPrincipal(java.lang.String)
   */
  @Override
  public String[] mapGroupPrincipal(String principalName) {
    String[] groups = null;
    String[] wildCardGroups = null;
    
    if (groupMappings != null) {
      groups = groupMappings.get(principalName);
      wildCardGroups = groupMappings.get("*");
      if (groups != null && wildCardGroups != null) {
        groups = concat(groups, wildCardGroups); 
      }
      else if (wildCardGroups != null) {
        return wildCardGroups;
      }
    }
    
    return groups;
  }

  /**
   * @param first
   * @param second
   * @return
   */
  public static <T> T[] concat(T[] first, T[] second) {
    T[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }
}
