/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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
package org.neo4j.graphalgo.path;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import common.Neo4jAlgoTestCase;
import org.junit.Test;
import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.path.Dijkstra;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;

import static org.junit.Assert.*;

public class DijkstraTest extends Neo4jAlgoTestCase
{
    @Test
    public void canGetPathsInTriangleGraph() throws Exception
    {
        Node nodeA = graph.makeNode( "A" );
        Node nodeB = graph.makeNode( "B" );
        Node nodeC = graph.makeNode( "C" );
        graph.makeEdge( "A", "B", "length", 2d );
        graph.makeEdge( "B", "C", "length", 3d );
        graph.makeEdge( "A", "C", "length", 10d );

        Dijkstra algo = new Dijkstra( PathExpanders.allTypesAndDirections(),
                CommonEvaluators.doubleCostEvaluator( "length" ) );

        Iterator<WeightedPath> paths = algo.findAllPaths( nodeA, nodeC ).iterator();
        assertTrue( "expected at least one path", paths.hasNext() );
        assertPath( paths.next(), nodeA, nodeB, nodeC );
        assertFalse( "expected at most one path", paths.hasNext() );

        assertPath( algo.findSinglePath( nodeA, nodeC ), nodeA, nodeB, nodeC );
    }

    @Test
    public void canGetMultiplePathsInTriangleGraph() throws Exception
    {
        Node nodeA = graph.makeNode( "A" );
        Node nodeB = graph.makeNode( "B" );
        Node nodeC = graph.makeNode( "C" );
        Set<Relationship> expectedFirsts = new HashSet<Relationship>();
        expectedFirsts.add( graph.makeEdge( "A", "B", "length", 1d ) );
        expectedFirsts.add( graph.makeEdge( "A", "B", "length", 1d ) );
        Relationship expectedSecond = graph.makeEdge( "B", "C", "length", 2d );
        graph.makeEdge( "A", "C", "length", 5d );

        Dijkstra algo = new Dijkstra( PathExpanders.allTypesAndDirections(),
                CommonEvaluators.doubleCostEvaluator( "length" ) );

        Iterator<WeightedPath> paths = algo.findAllPaths( nodeA, nodeC ).iterator();
        for ( int i = 0; i < 2; i++ )
        {
            assertTrue( "expected more paths", paths.hasNext() );
            Path path = paths.next();
            assertPath( path, nodeA, nodeB, nodeC );

            Iterator<Relationship> relationships = path.relationships().iterator();
            assertTrue( "found shorter path than expected",
                    relationships.hasNext() );
            assertTrue( "path contained unexpected relationship",
                    expectedFirsts.remove( relationships.next() ) );
            assertTrue( "found shorter path than expected",
                    relationships.hasNext() );
            assertEquals( expectedSecond, relationships.next() );
            assertFalse( "found longer path than expected",
                    relationships.hasNext() );
        }
        assertFalse( "expected at most two paths", paths.hasNext() );
    }

    @Test
    public void canGetMultiplePathsInASmallRoadNetwork() throws Exception
    {
        Node nodeA = graph.makeNode( "A" );
        Node nodeB = graph.makeNode( "B" );
        Node nodeC = graph.makeNode( "C" );
        Node nodeD = graph.makeNode( "D" );
        Node nodeE = graph.makeNode( "E" );
        Node nodeF = graph.makeNode( "F" );
        graph.makeEdge( "A", "B", "length", 2d );
        graph.makeEdge( "A", "C", "length", 2.5d );
        graph.makeEdge( "C", "D", "length", 7.3d );
        graph.makeEdge( "B", "D", "length", 2.5d );
        graph.makeEdge( "D", "E", "length", 3d );
        graph.makeEdge( "C", "E", "length", 5d );
        graph.makeEdge( "E", "F", "length", 5d );
        graph.makeEdge( "C", "F", "length", 12d );
        graph.makeEdge( "A", "F", "length", 25d );

        Dijkstra algo = new Dijkstra( PathExpanders.allTypesAndDirections(),
                CommonEvaluators.doubleCostEvaluator( "length" ) );

        // Try the search in both directions.
        for ( Node[] nodes : new Node[][] { { nodeA, nodeF }, { nodeF, nodeA } } )
        {
            int found = 0;
            Iterator<WeightedPath> paths = algo.findAllPaths( nodes[0],
                    nodes[1] ).iterator();
            for ( int i = 0; i < 2; i++ )
            {
                assertTrue( "expected more paths", paths.hasNext() );
                Path path = paths.next();
                if ( path.length() != found && path.length() == 3 )
                {
                    assertContains( path.nodes(), nodeA, nodeC, nodeE, nodeF );
                }
                else if ( path.length() != found && path.length() == 4 )
                {
                    assertContains( path.nodes(), nodeA, nodeB, nodeD, nodeE,
                            nodeF );
                }
                else
                {
                    fail( "unexpected path length: " + path.length() );
                }
                found = path.length();
            }
            assertFalse( "expected at most two paths", paths.hasNext() );
        }
    }
}
