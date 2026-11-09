/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.realm;


import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.naming.Context;
import javax.sql.DataSource;

import org.apache.catalina.LifecycleException;
import org.apache.naming.ContextBindings;

/**
*
* Implementation of <b>Realm</b> that works with any JDBC JNDI DataSource.
* See the Realm How-To for more details on how to set up the database and
* for configuration options.
*
* @author Glenn L. Nielsen
* @author Craig R. McClanahan
* @author Carson McDonald
* @author Ignacio Ortega
* @author Dawud Tan < muhammad.dawud91@gmail.com >
*/
public class ManyUserRolesRealm extends DataSourceRealm {

    /**
     * The generated string for the roles PreparedStatement
     */
    private String preparedRoles = null;

    // ----------------------------------------------------- Instance Variables

    protected String columnInUserReferencedInUserRole = null;
    protected String referrencingColumnInUserRoleToUserTable = null;
    protected String roleEnumTable = null;
    protected String columnInRoleEnumReferredInUserRole = null;
    protected String referrencingColumnInUserRoleToRoleEnumTable = null;


    // ------------------------------------------------------------- Properties


    public String getColumnInUserReferencedInUserRole() {
        return columnInUserReferencedInUserRole;
    }

    public void setColumnInUserReferencedInUserRole( String columnInUserReferencedInUserRole ) {
      this.columnInUserReferencedInUserRole = columnInUserReferencedInUserRole;
    }

    public String getReferrencingColumnInUserRoleToUserTable() {
        return referrencingColumnInUserRoleToUserTable;
    }

    public void setReferrencingColumnInUserRoleToUserTable( String referrencingColumnInUserRoleToUserTable ) {
      this.referrencingColumnInUserRoleToUserTable = referrencingColumnInUserRoleToUserTable;
    }

    public String getRoleEnumTable() {
        return roleEnumTable;
    }

    public void setRoleEnumTable( String roleEnumTable ) {
      this.roleEnumTable = roleEnumTable;
    }
    public String getColumnInRoleEnumReferredInUserRole() {
        return columnInRoleEnumReferredInUserRole;
    }

    public void setColumnInRoleEnumReferredInUserRole( String columnInRoleEnumReferredInUserRole ) {
      this.columnInRoleEnumReferredInUserRole = columnInRoleEnumReferredInUserRole;
    }
    public String getReferrencingColumnInUserRoleToRoleEnumTable() {
        return referrencingColumnInUserRoleToRoleEnumTable;
    }

    public void setReferrencingColumnInUserRoleToRoleEnumTable( String referrencingColumnInUserRoleToRoleEnumTable ) {
      this.referrencingColumnInUserRoleToRoleEnumTable = referrencingColumnInUserRoleToRoleEnumTable;
    }


    /**
     * Return the roles associated with the given user name.
     *
     * @param dbConnection The database connection to be used
     * @param username User name for which roles should be retrieved
     *
     * @return an array list of the role names
     */
    protected ArrayList<String> getRoles(Connection dbConnection, String username) {

        if (allRolesMode != AllRolesMode.STRICT_MODE && !isRoleStoreDefined()) {
            // Using an authentication only configuration and no role store has
            // been defined so don't spend cycles looking
            return null;
        }

        ArrayList<String> list = null;

        try (PreparedStatement stmt = dbConnection.prepareStatement(preparedRoles)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                list = new ArrayList<>();

                while (rs.next()) {
                    String role = rs.getString(1);
                    if (role != null) {
                        list.add(role.trim());
                    }
                }
                return list;
            }
        } catch(SQLException e) {
            containerLog.error(sm.getString("dataSourceRealm.getRoles.exception", username), e);
        }

        return null;
    }


    private boolean isRoleStoreDefined() {
        return userRoleTable != null || roleNameCol != null;
    }


    // ------------------------------------------------------ Lifecycle Methods

    /**
     * Prepare for the beginning of active use of the public methods of this
     * component and implement the requirements of
     * {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    protected void startInternal() throws LifecycleException {
        super.startInternal();

        // Create the roles PreparedStatement string
        StringBuilder temp = new StringBuilder("SELECT c.");
		temp.append(roleNameCol);
        temp.append(" FROM ");
		temp.append(userTable);
		temp.append(" a INNER JOIN ");
		temp.append(userRoleTable);
		temp.append(" b ON a.");
		temp.append(columnInUserReferencedInUserRole);
		temp.append("=b.");
		temp.append(referrencingColumnInUserRoleToUserTable);
		temp.append(" INNER JOIN ");
		temp.append(roleEnumTable);
		temp.append(" c ON b.");
		temp.append(referrencingColumnInUserRoleToRoleEnumTable);
		temp.append("=c.");
		temp.append(columnInRoleEnumReferredInUserRole);
		temp.append(" WHERE a.");
		temp.append(userNameCol);
		temp.append(" = ?");
        preparedRoles = temp.toString();
    }
}
