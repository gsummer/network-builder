package org.tno.networks.graph;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

class Neo4jAttributeHolder implements AttributeHolder {
	protected final org.neo4j.graphdb.PropertyContainer container;

	public Neo4jAttributeHolder(org.neo4j.graphdb.PropertyContainer underlyingNode) {
		this.container = underlyingNode;
	}

	@Override
	public void setAttribute(String name, String value) {
		Transaction tx = container.getGraphDatabase().beginTx();

		try {
			container.setProperty(name, value);
			indexProperty(name, value, container);
			tx.success();
		} finally {
			tx.finish();
		}
	}

	@Override
	public void appendAttribute(String name, String value) {
		appendAttribute(name,value,";");
	}

	@Override
	public void appendAttribute(String name, String value, String sep) {
		Transaction tx = container.getGraphDatabase().beginTx();

		try {
			if(container.hasProperty(name))
				container.setProperty(name, (String)container.getProperty(name) + sep + value);
			else
				setAttribute(name, value);
			tx.success();
		} finally {
			tx.finish();
		}

	}

	@Override
	public Object getAttribute(String name) {
		return container.getProperty(name);
	}

	@Override
	public Set<String> getAttributeNames() {
		Set<String> names = new HashSet<String>();

		for(String name : getUnderlyingContainer().getPropertyKeys()){
			names.add(name);
		}

		return names;
	}

	public PropertyContainer getUnderlyingContainer(){
		return container;
	}

	@Override
	public int hashCode()
	{
		return container.hashCode();
	}

	@Override
	public boolean equals( Object o )
	{
		return o instanceof Neo4jAttributeHolder &&
				container.equals( ( (Neo4jAttributeHolder)o ).getUnderlyingContainer() );
	}

	private GraphDatabaseService getInstance() {
		return container.getGraphDatabase();
	}
	
	private Neo4jIndexer getIndexer(){
		try {
			return Neo4jFactory.getNeo4jFactory().makeIndexerInstance();
		} catch (Neo4jException e) {
			return null;
		}
	}
	
	private void indexProperty(String key, Object value, PropertyContainer p){
		if(p instanceof org.neo4j.graphdb.Node){
			indexProperty(key, value, (org.neo4j.graphdb.Node)p);
		} else if(p instanceof Relationship){
			indexProperty(key, value, (Relationship)p);
		}
	}
	
	// change into index/update Property
	private void indexProperty(String key, Object value, org.neo4j.graphdb.Node n){
		Index<org.neo4j.graphdb.Node> currentIndex = getInstance().index().forNodes(getIndexer().lookupNodeIndexName(key));
		currentIndex.add(n, getIndexer().lookupNodeIndexKey(key), value);
	}

	private void indexProperty(String key, Object value, Relationship r){
		Index<Relationship> currentIndex = getInstance().index().forRelationships(getIndexer().lookupEdgeIndexName(key));
		currentIndex.add(r, getIndexer().lookupEdgeIndexKey(key), value);
	}

}