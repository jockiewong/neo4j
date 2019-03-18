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
package org.neo4j.configuration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.neo4j.cluster.ClusterSettings;
import org.neo4j.cluster.InstanceId;
import org.neo4j.graphdb.config.InvalidSettingException;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.configuration.Config;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.stringMap;

@RunWith( Parameterized.class )
public class HaConfigurationValidatorTest
{
    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Parameterized.Parameter
    public ClusterSettings.Mode mode;

    @Parameterized.Parameters( name = "{0}" )
    public static List<ClusterSettings.Mode> recordFormats()
    {
        return Arrays.asList( ClusterSettings.Mode.HA, ClusterSettings.Mode.ARBITER );
    }

    @Test
    public void validateOnlyIfModeIsHA() throws Exception
    {
        // when
        Config config = Config.embeddedDefaults(
                stringMap( ClusterSettings.mode.name(), ClusterSettings.Mode.SINGLE.name(),
                        ClusterSettings.initial_hosts.name(), "" ),
                Collections.singleton( new HaConfigurationValidator() ) );

        // then
        assertEquals( "", config.getRaw( ClusterSettings.initial_hosts.name() ).get() );
    }

    @Test
    public void validateSuccess() throws Exception
    {
        // when
        Config config = Config.embeddedDefaults(
                stringMap( ClusterSettings.mode.name(), mode.name(),
                        ClusterSettings.server_id.name(), "1",
                        ClusterSettings.initial_hosts.name(), "localhost,remotehost" ),
                Collections.singleton( new HaConfigurationValidator() ) );

        // then
        assertEquals( asList( new HostnamePort( "localhost" ),
                new HostnamePort( "remotehost" ) ),
                config.get( ClusterSettings.initial_hosts ) );
        assertEquals( new InstanceId( 1 ), config.get( ClusterSettings.server_id ) );
    }

    @Test
    public void missingServerId() throws Exception
    {
        // then
        expected.expect( InvalidSettingException.class );
        expected.expectMessage( "Missing mandatory value for 'ha.server_id'" );

        // when
        Config.embeddedDefaults(
                stringMap( ClusterSettings.mode.name(), mode.name() ),
                Collections.singleton( new HaConfigurationValidator() ) );
    }

    @Test
    public void missingInitialHosts() throws Exception
    {
        // then
        expected.expect( InvalidSettingException.class );
        expected.expectMessage( "Missing mandatory non-empty value for 'ha.initial_hosts'" );

        // when
        Config.embeddedDefaults(
                stringMap( ClusterSettings.mode.name(), mode.name(),
                        ClusterSettings.server_id.name(), "1" ),
                Collections.singleton( new HaConfigurationValidator() ) );
    }

    @Test
    public void initialHostsEmpty() throws Exception
    {
        // then
        expected.expect( InvalidSettingException.class );
        expected.expectMessage( "Missing mandatory non-empty value for 'ha.initial_hosts'" );

        // when
        Config.embeddedDefaults(
                stringMap( ClusterSettings.mode.name(), mode.name(),
                        ClusterSettings.server_id.name(), "1",
                        ClusterSettings.initial_hosts.name(), "," ),
                Collections.singleton( new HaConfigurationValidator() ) );
    }
}