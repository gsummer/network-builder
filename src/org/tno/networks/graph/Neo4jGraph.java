package org.tno.networks.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;

public class Neo4jGraph extends InMemoryAttributeHolder implements Graph {

	boolean directed = false;
	private String title = "";
	private String id;

	private Neo4jFactory factory = null;
	private GraphDatabaseService instance = null;
	private Neo4jIndexer indexer = null;
	
	public Neo4jGraph(String configLocation) throws Neo4jException{
		factory = Neo4jFactory.getNeo4jFactory(configLocation);

		instance = factory.makeServiceInstance();
		indexer = factory.makeIndexerInstance();
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public boolean isDirected() {
		return directed;
	}

	@Override
	public void setDirected(boolean directed) {
		this.directed = directed;
	}

	@Override
	public Node addNode(String id) {
		Node n = getNode(id);

		if(n == null){
			Transaction tx = instance.beginTx();
			try {
				n = new Neo4jNode(instance.createNode(),id);

				tx.success();
			} finally {
				tx.finish();
			}
		}

		return n;
	}

	@Override
	public Edge addEdge(String id, Node src, Node tgt) {

		Edge e = getEdge(id);

		if(e == null){
			Transaction tx = instance.beginTx();

			try{
				Neo4jNode nSrc = (Neo4jNode)src;
				Neo4jNode nTgt = (Neo4jNode)tgt;

				Relationship r = nSrc.getUnderlyingNode().createRelationshipTo(nTgt.getUnderlyingNode(), factory.getRelationshipType()); 
				e = new Neo4jEdge(nSrc,nTgt,id,r);

				tx.success();
			} finally {
				tx.finish();
			}
		}

		return e;
	}

	@Override
	public Node getNode(String id) {
		IndexHits<org.neo4j.graphdb.Node> hits = instance.index().forNodes(indexer.lookupNodeIndexName("id")).get(indexer.lookupEdgeIndexKey("id"),id);
		
		org.neo4j.graphdb.Node hit = hits.getSingle();
		hits.close();
		
		Node n = null;
		
		if(hit != null)
			n = new Neo4jNode(hit,id);
		
		return n;
	}
	
	protected Edge getEdge(String id){
		String indexName = indexer.lookupEdgeIndexName("id");
		IndexHits<Relationship> hits = instance.index().forRelationships(indexName).get(indexer.lookupEdgeIndexKey("id"),id);
		
		Relationship hit = hits.getSingle();
		hits.close();
		
		Edge e = null;
		
		if(hit != null)
			e = new Neo4jEdge(hit,id);
		
		
		return e;
	}

	@Override
	public Collection<Node> getNodes() {
		
		Set<Node> result = new HashSet<Node>();
		
		for(org.neo4j.graphdb.Node n : instance.getAllNodes()){
			result.add(new Neo4jNode(n, (String)n.getProperty("id","huh?")));
		}
		
		return result;
	}

	@Override
	public Collection<Edge> getEdges() {
		Set<Edge> result = new HashSet<Edge>();
		
		for(org.neo4j.graphdb.Node n : instance.getAllNodes()){
			for(Relationship r : n.getRelationships()){
				result.add(new Neo4jEdge(r, (String)r.getProperty("id","hmm?")));
			}
		}
		
		return result;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
