package org.tno.networks.args;

import com.lexicalscope.jewel.cli.Option;

public interface ANeo4j {
	
	@Option(description = "neo4j config",defaultToNull=true)
	String getNeo4jConfig();

}
