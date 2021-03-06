/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.codegen;

import org.junit.jupiter.api.Test;

import org.neo4j.common.DependencyResolver;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.internal.kernel.api.CursorFactory;
import org.neo4j.internal.kernel.api.NodeCursor;
import org.neo4j.internal.kernel.api.Read;
import org.neo4j.internal.kernel.api.TokenWrite;
import org.neo4j.internal.kernel.api.Write;
import org.neo4j.internal.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.internal.kernel.api.security.LoginContext;
import org.neo4j.kernel.api.Kernel;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.test.extension.DbmsExtension;
import org.neo4j.test.extension.ExtensionCallback;
import org.neo4j.test.extension.Inject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.neo4j.cypher.internal.codegen.CompiledExpandUtils.nodeGetDegreeIfDense;
import static org.neo4j.graphdb.Direction.BOTH;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.kernel.api.KernelTransaction.Type.implicit;

@DbmsExtension( configurationCallback = "config" )
class CompiledExpandUtilsTest
{
    @Inject
    private GraphDatabaseAPI db;

    @ExtensionCallback
    void config( TestDatabaseManagementServiceBuilder builder )
    {
        builder.setConfig( GraphDatabaseSettings.dense_node_threshold, 1 );
    }

    private KernelTransaction transaction() throws TransactionFailureException
    {
        DependencyResolver resolver = db.getDependencyResolver();
        return resolver.resolveDependency( Kernel.class ).beginTransaction( implicit, LoginContext.AUTH_DISABLED );
    }

    @Test
    void shouldComputeDegreeWithoutType() throws Exception
    {
        // GIVEN
        long node;
        try ( KernelTransaction tx = transaction() )
        {
            Write write = tx.dataWrite();
            node = write.nodeCreate();
            write.relationshipCreate( node,
                    tx.tokenWrite().relationshipTypeGetOrCreateForName( "R1" ),
                    write.nodeCreate() );
            write.relationshipCreate( node,
                    tx.tokenWrite().relationshipTypeGetOrCreateForName( "R2" ),
                    write.nodeCreate() );
            write.relationshipCreate( write.nodeCreate(),
                    tx.tokenWrite().relationshipTypeGetOrCreateForName( "R3" ),
                    node );
            write.relationshipCreate( node,
                    tx.tokenWrite().relationshipTypeGetOrCreateForName( "R4" ), node );

            tx.commit();
        }

        try ( KernelTransaction tx = transaction() )
        {
            Read read = tx.dataRead();
            CursorFactory cursors = tx.cursors();
            try ( NodeCursor nodes = cursors.allocateNodeCursor() )
            {
                assertThat( CompiledExpandUtils.nodeGetDegreeIfDense( read, node, nodes, cursors, OUTGOING ), equalTo( 3 ) );
                assertThat( CompiledExpandUtils.nodeGetDegreeIfDense( read, node, nodes, cursors, INCOMING ), equalTo( 2 ) );
                assertThat( CompiledExpandUtils.nodeGetDegreeIfDense( read, node, nodes, cursors, BOTH ), equalTo( 4 ) );
            }
        }
    }

    @Test
    void shouldComputeDegreeWithType() throws Exception
    {
        // GIVEN
        long node;
        int in, out, loop;
        try ( KernelTransaction tx = transaction() )
        {
            Write write = tx.dataWrite();
            node = write.nodeCreate();
            TokenWrite tokenWrite = tx.tokenWrite();
            out = tokenWrite.relationshipTypeGetOrCreateForName( "OUT" );
            in = tokenWrite.relationshipTypeGetOrCreateForName( "IN" );
            loop = tokenWrite.relationshipTypeGetOrCreateForName( "LOOP" );
            write.relationshipCreate( node,
                    out,
                    write.nodeCreate() );
            write.relationshipCreate( node, out, write.nodeCreate() );
            write.relationshipCreate( write.nodeCreate(), in, node );
            write.relationshipCreate( node, loop, node );

            tx.commit();
        }

        try ( KernelTransaction tx = transaction() )
        {
            Read read = tx.dataRead();
            CursorFactory cursors = tx.cursors();
            try ( NodeCursor nodes = cursors.allocateNodeCursor() )
            {
                assertThat( nodeGetDegreeIfDense( read, node, nodes, cursors, OUTGOING, out ), equalTo( 2 ) );
                assertThat( nodeGetDegreeIfDense( read, node, nodes, cursors, OUTGOING, in ), equalTo( 0 ) );
                assertThat( nodeGetDegreeIfDense( read, node, nodes, cursors, OUTGOING, loop ), equalTo( 1 ) );

                assertThat( nodeGetDegreeIfDense( read, node, nodes, cursors, INCOMING, out ), equalTo( 0 ) );
                assertThat( nodeGetDegreeIfDense( read, node, nodes, cursors, INCOMING, in ), equalTo( 1 ) );
                assertThat( nodeGetDegreeIfDense( read, node, nodes, cursors, INCOMING, loop ), equalTo( 1 ) );

                assertThat( nodeGetDegreeIfDense( read, node, nodes, cursors, BOTH, out ), equalTo( 2 ) );
                assertThat( nodeGetDegreeIfDense( read, node, nodes, cursors, BOTH, in ), equalTo( 1 ) );
                assertThat( nodeGetDegreeIfDense( read, node, nodes, cursors, BOTH, loop ), equalTo( 1 ) );
            }
        }
    }
}
