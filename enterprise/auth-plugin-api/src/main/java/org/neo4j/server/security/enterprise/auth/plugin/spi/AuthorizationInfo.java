/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.server.security.enterprise.auth.plugin.spi;

import java.io.Serializable;
import java.util.Collection;

/**
 * An object that can be returned as the result of authorization by an <tt>AuthorizationPlugin</tt>.
 *
 * @see AuthorizationPlugin#authorize(Collection)
 */
public interface AuthorizationInfo extends Serializable
{
    /**
     * Should return a collection of roles assigned to the principals recognized by an <tt>AuthorizationPlugin</tt>.
     *
     * @return the roles assigned to the principals recognized by an <tt>AuthorizationPlugin</tt>
     */
    Collection<String> roles();

    static AuthorizationInfo of( Collection<String> roles )
    {
        return () -> roles;
    }
}