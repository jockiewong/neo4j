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
package cypher.feature.parser.matchers;

import java.util.Map;
import java.util.Set;

public class RowMatcher implements Matcher<Map<String,Object>>
{
    private final Map<String,ValueMatcher> values;

    public RowMatcher( Map<String,ValueMatcher> values )
    {
        this.values = values;
    }

    @Override
    public boolean matches( Map<String,Object> value )
    {
        Set<String> keys = values.keySet();
        if ( keys.equals( value.keySet() ) )
        {
            for ( String key : keys )
            {
                if ( !values.get( key ).matches( value.get( key ) ) )
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "expectedRow:" + values;
    }
}