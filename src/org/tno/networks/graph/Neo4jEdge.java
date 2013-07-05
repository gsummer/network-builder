package org.tno.networks.graph;

import org.tno.networks.graph.Graph.Edge;
import org.tno.networks.graph.Graph.Node;

public class Neo4jEdge extends Neo4jAttributeHolder implements Edge{

	private Neo4jNode src = null;
	private Neo4jNode tgt = null;
	
	public Neo4jEdge(Neo4jNode src, Neo4jNode tgt, String id,org.neo4j.graphdb.Relationship underlyingEdge) {
		super(underlyingEdge);
		this.src = src;
		this.tgt = tgt;
		setAttribute("id", id);
	}
	
	public Neo4jEdge(org.neo4j.graphdb.Relationship underlyingEdge,String id) {
		super(underlyingEdge);
	
		setAttribute("id", id);
		src = new Neo4jNode(underlyingEdge.getStartNode(), (String)underlyingEdge.getStartNode().getProperty("id"));
		tgt = new Neo4jNode(underlyingEdge.getEndNode(), (String)underlyingEdge.getEndNode().getProperty("id"));
		
	}

	@Override
	public Node getSrc() {			
		return src;
	}

	@Override
	public Node getTgt() {
		return tgt;
	}

	@Override
	public String getId() {
		return (String)getAttribute("id");
	}

	public org.neo4j.graphdb.Relationship getUnderlyingEdge(){
		return (org.neo4j.graphdb.Relationship)container;
	}

	@Override
	public int hashCode()
	{
		return getUnderlyingEdge().hashCode();
	}

	@Override
	public boolean equals( Object o )
	{
		return o instanceof Neo4jEdge &&
				getUnderlyingEdge().equals( ( (Neo4jEdge)o ).getUnderlyingEdge() );
	}

}