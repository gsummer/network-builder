package org.tno.networks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.tno.networks.args.AHelp;
import org.tno.networks.args.AIDMapper;
import org.tno.networks.args.ANeo4j;
import org.tno.networks.args.DIDMapper;
import org.tno.networks.graph.AttributeName;
import org.tno.networks.graph.GmlWriter;
import org.tno.networks.graph.Graph;
import org.tno.networks.graph.Graph.Edge;
import org.tno.networks.graph.Graph.Node;
import org.tno.networks.graph.InMemoryGraph;
import org.tno.networks.graph.Neo4jGraph;
import org.tno.networks.graph.XGMMLWriter;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Import transcription factor targets from the TFe wiki.
 * @author thomas
 */
public class TFeToNetwork {
	private final static Logger log = Logger.getLogger(TFeToNetwork.class.getName());

	private static final String TFE_URL = "http://www.cisreg.ca/cgi-bin/tfe/api.pl?";
	
	public static Graph importTFe(String species, Graph graph) throws MalformedURLException, IOException {
//		InMemoryGraph graph = new InMemoryGraph();
		graph.setTitle("TFe");
		graph.setDirected(true);
		
		//First get all TF ids
		String[] tfIds = readURL(TFE_URL + "code=all-tfids").split("\n");
		for(String id : tfIds) {
			log.info("Processing " + id);
			if(species != null) {
				String tfSpecies = readURL(TFE_URL + "tfid=" + id + "&code=species").trim();
				log.info(tfSpecies);
				if(!species.equals(tfSpecies)) {
					log.info("Input species " + species + " doesn't equal " + tfSpecies);
					continue;
				}
			}
			
			log.info("Querying info for " + id);
			Xref tf = new Xref(readURL(TFE_URL + "tfid=" + id + "&code=entrez-gene-id").trim(), BioDataSource.ENTREZ_GENE);
			
			Node nSrc = graph.addNode("" + tf);
			nSrc.setAttribute(AttributeName.Label.name(), readURL(TFE_URL + "tfid=" + id + "&code=symbol").trim());
			nSrc.setAttribute(AttributeName.XrefId.name(), tf.getId());
			nSrc.setAttribute(AttributeName.XrefDatasource.name(), tf.getDataSource().getFullName());
			
			//Get targets
			String[] lines = readURL(TFE_URL + "tfid=" + id + "&code=targets").split("\n");
			for(String l : lines) {
				if("".equals(l)) continue;
				String[] cols = l.split("\t");
				for(int i = 0; i < cols.length; i++) cols[i] = cols[i].trim();
				
				Xref target = new Xref(cols[0], BioDataSource.ENTREZ_GENE);
				
				Node nTgt = graph.addNode("" + target);
				nTgt.setAttribute(AttributeName.Label.name(), cols[1]);
				nTgt.setAttribute(AttributeName.XrefId.name(), target.getId());
				nTgt.setAttribute(AttributeName.XrefDatasource.name(), target.getDataSource().getFullName());
				nTgt.appendAttribute(AttributeName.TFeActingComplex.name(), cols[2]);
				
				Edge edge = graph.addEdge(nSrc + "target" + nTgt, nSrc, nTgt,AttributeName.Interaction.name(),"TF target");
				nTgt.setAttribute(AttributeName.TFeActingComplex.name(), cols[2]);
				edge.setAttribute(AttributeName.TFeEffect.name(), cols[3]);
				edge.setAttribute(AttributeName.PMID.name(), cols[4]);
				edge.setAttribute(AttributeName.TFeSource.name(), cols[5]);
				edge.setAttribute(AttributeName.Interaction.name(), "TF target");
				edge.setAttribute(AttributeName.Directed.name(), "true");
			}
			
			//Get interactors
			lines = readURL(TFE_URL + "tfid=" + id + "&code=interactors").split("\n");
			for(String l : lines) {
				if("".equals(l)) continue;
				String[] cols = l.split("\t");
				for(int i = 0; i < cols.length; i++) cols[i] = cols[i].trim();
				
				Xref target = new Xref(cols[0], BioDataSource.ENTREZ_GENE);
				
				Node nTgt = graph.addNode("" + target);
				nTgt.setAttribute(AttributeName.Label.name(), cols[1]);
				
				Edge edge = graph.addEdge(nSrc + cols[3] + nTgt, nSrc, nTgt,AttributeName.Interaction.name(),cols[3]);
				edge.setAttribute(AttributeName.TFeExperiment.name(), cols[2]);
				edge.setAttribute(AttributeName.Interaction.name(), cols[3]);
				edge.setAttribute(AttributeName.PMID.name(), cols[4]);
				edge.setAttribute(AttributeName.TFeSource.name(), cols[5]);
			}
		}
		
		return graph;
	}
	
	private static String readURL(String url) throws MalformedURLException, IOException {
		return IOUtils.toString(new URL(url).openStream());
	}
	
	public static void main(String[] args) {
		try {
			BioDataSource.init();

			Args pargs = CliFactory.parseArguments(Args.class, args);
			DIDMapper didm = new DIDMapper(pargs);

			Graph graph = null;
			
			if(pargs.getNeo4jConfig() != null && !pargs.getNeo4jConfig().isEmpty()){
				graph = new Neo4jGraph(pargs.getNeo4jConfig());
			} else {
				graph = new InMemoryGraph();
			}
			
			importTFe(pargs.isSpecies() ? pargs.getSpecies() : null,graph);
			
			//Translate ids if necessary
			if(didm.getDataSources().length != 1 || !BioDataSource.ENTREZ_GENE.equals(didm.getDataSources()[0])) {
				log.info("Mapping network to " + pargs.getDs());
				NetworkIDMapper nidm = new NetworkIDMapper(didm.getIDMapper(), didm.getDataSources());
				graph = nidm.mapIDs(graph);
			}
			
			if(pargs.getOut() != null){
				if(pargs.getOut().getName().endsWith(".gml")) {
					writeGml("" + pargs.getOut(), graph);
				} else if(pargs.getOut().getName().endsWith(".xgmml")) {
					writeXgmml("" + pargs.getOut(), graph);
				} else {
					writeGml(pargs.getOut() + ".gml", graph);
					writeXgmml(pargs.getOut() + ".xgmml", graph);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	private static void writeGml(String f, Graph graph) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(new File(f));
		GmlWriter.write(graph, out);
		out.close();
	}
	
	private static void writeXgmml(String f, Graph g) throws IOException {
		PrintWriter out = new PrintWriter(new File(f));
		XGMMLWriter.write(g, out);
		out.close();
	}
	
	private interface Args extends AHelp, AIDMapper, ANeo4j {
		@Option(shortName = "o", description = "The file to write the network to",defaultToNull=true)
		File getOut();
		
		@Option(description = "Only extract TFs for given species (latin name).")
		String getSpecies();
		boolean isSpecies();
	}
}
