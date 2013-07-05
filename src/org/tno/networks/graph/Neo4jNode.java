package org.tno.networks.graph;

import org.tno.networks.graph.Graph.Node;

public class Neo4jNode extends Neo4jAttributeHolder implements Node {

	public Neo4jNode(org.neo4j.graphdb.Node underlyingNode, String id) {
		super(underlyingNode);
		setAttribute("id", id);
	}

	@Override
	public String getId() {
		return (String)getAttribute("id");
	}

	public org.neo4j.graphdb.Node getUnderlyingNode() {
		return (org.neo4j.graphdb.Node)container;
	}

	public long getNeoId(){
		return getUnderlyingNode().getId();
	}

	@Override
	public int hashCode()
	{
		return getUnderlyingNode().hashCode();
	}

	@Override
	public boolean equals( Object o )
	{
		return o instanceof Neo4jNode &&
				getUnderlyingNode().equals( ( (Neo4jNode)o ).getUnderlyingNode() );
	}
}