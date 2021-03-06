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
package org.neo4j.internal.schema.constraints;

import org.neo4j.common.EntityType;
import org.neo4j.common.TokenNameLookup;
import org.neo4j.internal.schema.ConstraintDescriptor;
import org.neo4j.internal.schema.ConstraintType;
import org.neo4j.internal.schema.SchemaDescriptor;
import org.neo4j.internal.schema.SchemaRule;
import org.neo4j.token.api.TokenIdPrettyPrinter;

import static java.lang.String.format;
import static org.neo4j.common.EntityType.NODE;

/**
 * Internal representation of a graph constraint, including the schema unit it targets (eg. label-property combination)
 * and the how that schema unit is constrained (eg. "has to exist", or "must be unique").
 */
public class ConstraintDescriptorImplementation implements ConstraintDescriptor, NodeExistenceConstraintDescriptor, NodeKeyConstraintDescriptor,
        RelExistenceConstraintDescriptor, UniquenessConstraintDescriptor
{
    private final ConstraintType type;
    private final SchemaDescriptor schema;
    private final long id;
    private final String name;
    private final Long ownedIndex;

    ConstraintDescriptorImplementation( ConstraintType type, SchemaDescriptor schema )
    {
        this.type = type;
        this.schema = schema;
        this.id = -1;
        this.name = null;
        this.ownedIndex = null;
    }

    private ConstraintDescriptorImplementation( ConstraintType type, SchemaDescriptor schema, long id, String name, Long ownedIndex )
    {
        this.type = type;
        this.schema = schema;
        this.id = id;
        this.name = name;
        this.ownedIndex = ownedIndex;
    }

    // METHODS

    @Override
    public ConstraintType type()
    {
        return type;
    }

    @Override
    public SchemaDescriptor schema()
    {
        return schema;
    }

    @Override
    public boolean enforcesUniqueness()
    {
        return type.enforcesUniqueness();
    }

    @Override
    public boolean enforcesPropertyExistence()
    {
        return type.enforcesPropertyExistence();
    }

    /**
     * @param tokenNameLookup used for looking up names for token ids.
     * @return a user friendly description of this constraint.
     */
    @Override
    public String userDescription( TokenNameLookup tokenNameLookup )
    {
        return format( "Constraint( %s, %s )", type.name(), schema().userDescription( tokenNameLookup ) );
    }

    @Override
    public String prettyPrint( TokenNameLookup tokenNameLookup )
    {
        int[] entityTokenIds = schema().getEntityTokenIds();
        if ( entityTokenIds.length != 1 )
        {
            throw new IllegalArgumentException( "Cannot pretty-print multi-token constraints: " + userDescription( tokenNameLookup ) );
        }
        String entityTypeName = schema.entityType() == NODE ? tokenNameLookup.labelGetName( entityTokenIds[0] ) :
                                tokenNameLookup.relationshipTypeGetName( entityTokenIds[0] );
        entityTypeName = escapeLabelOrRelTyp( entityTypeName );
        String entityName = entityTypeName.toLowerCase();
        String properties = formatProperties( schema().getPropertyIds(), tokenNameLookup, entityName );

        switch ( type() )
        {
        case EXISTS:
            switch ( schema.entityType() )
            {
            case NODE:
                return "CONSTRAINT ON ( " + entityName + ":" + entityTypeName + " ) ASSERT exists" + properties;
            case RELATIONSHIP:
                return "CONSTRAINT ON ()-[ " + entityName + ":" + entityTypeName + " ]-() ASSERT exists" + properties;
            default:
                throw new IllegalStateException( "Unknown schema entity type: " + schema.entityType() + "." );
            }
        case UNIQUE:
            return "CONSTRAINT ON ( " + entityName + ":" + entityTypeName + " ) ASSERT " + properties + " IS UNIQUE";
        case UNIQUE_EXISTS:
            return "CONSTRAINT ON ( " + entityName + ":" + entityTypeName + " ) ASSERT " + properties + " IS NODE KEY";
        default:
            throw new IllegalStateException( "Unknown constraint type: " + type() + "." );
        }
    }

    @Override
    public boolean isRelationshipPropertyExistenceConstraint()
    {
        return schema.entityType() == EntityType.RELATIONSHIP && type == ConstraintType.EXISTS;
    }

    @Override
    public RelExistenceConstraintDescriptor asRelationshipPropertyExistenceConstraint()
    {
        if ( !isRelationshipPropertyExistenceConstraint() )
        {
            throw conversionException( RelExistenceConstraintDescriptor.class );
        }
        return this;
    }

    @Override
    public boolean isNodePropertyExistenceConstraint()
    {
        return schema.entityType() == NODE && type == ConstraintType.EXISTS;
    }

    @Override
    public NodeExistenceConstraintDescriptor asNodePropertyExistenceConstraint()
    {
        if ( !isNodePropertyExistenceConstraint() )
        {
            throw conversionException( NodeExistenceConstraintDescriptor.class );
        }
        return this;
    }

    @Override
    public boolean isUniquenessConstraint()
    {
        return schema.entityType() == NODE && type == ConstraintType.UNIQUE;
    }

    @Override
    public UniquenessConstraintDescriptor asUniquenessConstraint()
    {
        if ( !isUniquenessConstraint() )
        {
            throw conversionException( UniquenessConstraintDescriptor.class );
        }
        return this;
    }

    @Override
    public boolean isNodeKeyConstraint()
    {
        return schema.entityType() == NODE && type == ConstraintType.UNIQUE_EXISTS;
    }

    @Override
    public NodeKeyConstraintDescriptor asNodeKeyConstraint()
    {
        if ( !isNodeKeyConstraint() )
        {
            throw conversionException( NodeKeyConstraintDescriptor.class );
        }
        return this;
    }

    @Override
    public boolean isIndexBackedConstraint()
    {
        return isUniquenessConstraint() || isNodeKeyConstraint();
    }

    @Override
    public IndexBackedConstraintDescriptor asIndexBackedConstraint()
    {
        if ( !isIndexBackedConstraint() )
        {
            throw conversionException( IndexBackedConstraintDescriptor.class );
        }
        return this;
    }

    private IllegalStateException conversionException( Class<? extends ConstraintDescriptor> targetType )
    {
        return new IllegalStateException( "Cannot cast this schema to a " + targetType + " because it does not match that structure: " + this + "." );
    }

    private String formatProperties( int[] propertyIds, TokenNameLookup tokenNameLookup, String nodeName )
    {
        return TokenIdPrettyPrinter.niceProperties( tokenNameLookup, propertyIds, nodeName + "." );
    }

    @Override
    public final boolean equals( Object o )
    {
        if ( o instanceof ConstraintDescriptor )
        {
            ConstraintDescriptor that = (ConstraintDescriptor) o;
            return this.type() == that.type() && this.schema().equals( that.schema() );
        }
        return false;
    }

    @Override
    public final int hashCode()
    {
        return type.hashCode() & schema().hashCode();
    }

    private String escapeLabelOrRelTyp( String name )
    {
        if ( name.contains( ":" ) )
        {
            return "`" + name + "`";
        }
        else
        {
            return name;
        }
    }

    @Override
    public long getId()
    {
        if ( id == -1 )
        {
            throw new IllegalStateException( "This constraint descriptor have no id assigned: " + this );
        }
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean hasOwnedIndexId()
    {
        return ownedIndex != null;
    }

    @Override
    public long ownedIndexId()
    {
        if ( ownedIndex == null )
        {
            throw new IllegalStateException( "This constraint does not own an index." );
        }
        return ownedIndex;
    }

    @Override
    public ConstraintDescriptorImplementation withId( long id )
    {
        return new ConstraintDescriptorImplementation( type, schema, id, name, ownedIndex );
    }

    @Override
    public ConstraintDescriptorImplementation withName( String name )
    {
        if ( name == null )
        {
            return this;
        }
        name = SchemaRule.sanitiseName( name );
        return new ConstraintDescriptorImplementation( type, schema, id, name, ownedIndex );
    }

    @Override
    public ConstraintDescriptorImplementation withOwnedIndexId( long ownedIndex )
    {
        return new ConstraintDescriptorImplementation( type, schema, id, name, ownedIndex );
    }
}
