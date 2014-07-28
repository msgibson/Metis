package uk.ac.abdn.csd.metis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import uk.ac.abdn.csd.metis.app.Player;
import uk.ac.abdn.csd.metis.app.Roles;
import uk.ac.abdn.csd.metis.app.Tile;
import uk.ac.abdn.csd.metis.p2p.Peer;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntTools;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.vocabulary.RDF;

public class Metis implements TripleEventListener{
	/**
	 * Holds information about the application and P2P neighbours.
	 */
	private OntModel ontology;
	
	/**
	 * The list of application rules.
	 */
	private List<Rule> rules;
	
	/**
	 * The OWL DL reasoner.
	 */
	private Reasoner OWLReasoner;
	
	/**
	 * The application rules and reasoning rules reasoners.
	 */
	private GenericRuleReasoner ruleReasoner;
	
	/**
	 * A custom built-in to handle property tests in a rule's antecedent.
	 * Essentially a way to allow full evaluation of a rule instead of
	 * the whole rule being rejected on the first failed property.
	 */
	private LHS LHS;
	
	/**
	 * The ontology prefix to use in front of properties and instances.
	 */
	private String prefix;
	
	/**
	 * Class names used in the Metis ontology to specify neighbours and participants.
	 */
	private Resource cNeighbour, cParticipant;
	
	/**
	 * Property name used in the Metis ontology to specify relationships between
	 * neighbours and participants.
	 */
	private Property pKnows;
	
	private Property pBeenVisited;
	
	private Property pHasResource;
	
	private Property pPositionedAt;
	
	private Property pCoordEnd;
	
	/**
	 * Set of queries and informs to be processed, as in to assign neighbours to.
	 */
	//private HashSet<String[]> queries, informs;
	private HashSet<String> queries, informs;
	
	/**
	 * List of roles to query or inform (value) based on the given resource (key).
	 */
	//private HashMap<String, ArrayList<String>> bestQueryRoles, bestInformRoles;
	
	/**
	 * Show debugging information.
	 */
	private boolean traceOn = false;	
	
	public Metis(String prefix, String ontologyFileName, String rulesFileName, String metarulesFileName,
			boolean traceOn){
		this.prefix = prefix;
		
		//queries = new HashSet<String[]>();
		//informs = new HashSet<String[]>();
		queries = new HashSet<String>();
		informs = new HashSet<String>();
		
		org.apache.jena.atlas.logging.Log.setLog4j();
		this.traceOn = traceOn;
		
		LHS = new LHS();
		LHS.addTripleEventListener(this);
		
		BuiltinRegistry.theRegistry.register("lhs", LHS);
		
		ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
		OWLReasoner = ReasonerRegistry.getOWLReasoner();
		
		readOntology(ontologyFileName);
		readRules(rulesFileName);
	}
	
	public OntModel getOntology(){
		return ontology;
	}
	
	private void test(){
		String SPARQL = "prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +				
				"prefix metis: <http://www.abdn.ac.uk/metis.owl#> \n";		
		SPARQL += String.format("select ?resource where {" +
								"?resource rdf:type metis:Tile ." +
								"}");
		Query query = QueryFactory.create(SPARQL);
		QueryExecution qexec = QueryExecutionFactory.create(query, ontology);
		try{
			ResultSet results = qexec.execSelect();
			while(results.hasNext()){
				QuerySolution soln = results.next();				
				Resource n = soln.getResource("resource");							
				System.out.println(n);				
			}
		}finally{
			qexec.close();
		}	
	}
	
	public void setTraceOn(boolean traceOn){
		this.traceOn = traceOn;
	}
	
	private void readOntology(String ontologyFileName){
		File ontologyFile = new File(ontologyFileName);
		if(ontologyFile.exists() && ontologyFile.isFile()){
			ontology.read("file:" + ontologyFileName);
			if(traceOn){
				print("Loaded ontology:", ontology.listStatements());
			}
			cNeighbour = ontology.createResource(prefix + "Neighbour");
			cParticipant = ontology.createResource(prefix + "Participant");
			pKnows = ontology.createProperty(prefix + "knows");
			pBeenVisited = ontology.createProperty(prefix + "beenVisited");
			pHasResource = ontology.createProperty(prefix + "hasResource");
			pPositionedAt = ontology.createProperty(prefix + "positionedAt");
			pCoordEnd = ontology.createProperty(prefix + "coordEnd");
		}else{
			System.err.println("Cound not read " + ontologyFileName);
		}
	}
	
	private void readRules(String rulesFileName){
		File rulesFile = new File(rulesFileName);
		if(rulesFile.exists() && rulesFile.isFile()){
			rules = Rule.rulesFromURL("file:" + rulesFileName);			
			ruleReasoner = new GenericRuleReasoner(rules);
			ruleReasoner.setOWLTranslation(true);
			ruleReasoner.setTransitiveClosureCaching(true);	
			ruleReasoner.setTraceOn(traceOn);			
		}else{
			System.err.println("Could not read " + rulesFileName);
		}
	}
	
	public void reason(){		
		InfModel infModel = ModelFactory.createInfModel(OWLReasoner, ontology);
		ontology.add(infModel.getDeductionsModel());
		
		if(traceOn){
			print("Reasoned ontology", ontology.listStatements());
		}
	}	
	
	public void run(){
		// This is where Metis should pay attention to the game's rules
		// to see which property checks fails within a rule and query
		// the right neighbour. It should also notify the right neighbour
		// if a rule successed and may benefit a peer on the network
		
		InfModel infModel = ModelFactory.createInfModel(ruleReasoner, ontology);
		//fullPrint();
		
		/* 
		 * Necessary hack to prevent infinite looping in ruleset.
		 * Removes "ruleFiredFor" property which is used as a flag
		 * to indicate a rule has already fired on a resource.
		 * 
		 * If the flag is not removed, the resource won't be able to "visit"
		 * the last resource it was on, i.e. the player won't be able to 
		 * visit any previsouly visited tiles.
		 * 
		 * Thanks to http://hydrogen.informatik.tu-cottbus.de/wiki/index.php/Advanced_Jena_Rules
		 * for the initial no-loop hack.
		 */
		Model deductions = infModel.getDeductionsModel();		
		Property pFired = ontology.getProperty(prefix + "ruleFiredFor");		
		deductions.removeAll(null, pFired, null);
		
		// Create Inform messages to send to other peers which may benefit from this information		
		createInforms(deductions);		
		
		ontology.add(deductions);
		//fullPrint();
		
		// Need to remove "move" command, otherwise player will try to keep moving to same spot
		// over and over! Remove other commands just in case... (although rules should do this)
		Property pMove = ontology.getProperty(prefix + "move");
		Property pImpatient = ontology.getProperty(prefix + "impatient");
		Property pGather = ontology.getProperty(prefix + "gather");
		Property pCoord = ontology.getProperty(prefix + "coord");
		Property pCoordGather = ontology.getProperty(prefix + "coordGather");
		Property pExchange = ontology.getProperty(prefix + "exchange");
		Property pStartExchange = ontology.getProperty(prefix + "startExchange");
		ontology.removeAll(null, pMove, null);
		ontology.removeAll(null, pImpatient, null);
		ontology.removeAll(null, pGather, null);
		ontology.removeAll(null, pCoord, null);
		ontology.removeAll(null, pCoordGather, null);
		ontology.removeAll(null, pCoordEnd, null);
		ontology.removeAll(null, pExchange, null);
		ontology.removeAll(null, pStartExchange, null);
		
		if(traceOn){
			print("Ruled ontology", infModel.getDeductionsModel().listStatements());
		}	
	}
	
	private void createInforms(Model newInformation){
		// Need to check each statement against (null, metis:beenVisited, true)
		// to get newly visited tile's raw resource and inform peers which may be
		// interested in this.
		
		StmtIterator it = newInformation.listStatements();
		while(it.hasNext()){
			Statement s = it.next();			
			if(s.getPredicate().equals(pBeenVisited)){
				Resource tile = ontology.createResource(s.getSubject().toString());
				Resource raw = tile.getPropertyResourceValue(pHasResource);				
				 // No point informing others about no information, so skip.				 
				if(raw.toString().contains("nil")){
					continue;
				}
				String[] triple = new String[3];
				triple[0] = tile.toString();
				triple[1] = pHasResource.toString();
				triple[2] = raw.toString();
				//informs.add(triple);
				informs.add(triple[0] + ";" + triple[1] + ";" + triple[2]);
			}else if(s.getPredicate().equals(pCoordEnd)){				
				String[] triple = new String[3];
				triple[0] = s.getSubject().toString();
				triple[1] = s.getPredicate().toString();
				triple[2] = s.getObject().toString();
				//informs.add(triple);		
				informs.add(triple[0] + ";" + triple[1] + ";" + triple[2]);
			}
		}
	}
	
	/**
	 * Add a peer which is directly linked to this peer to the ontology.
	 * 
	 * @param neighbour Name of neighbour.
	 */
	public void addNeighbour(String neighbour){
		ontology.createResource(prefix + neighbour).addProperty(RDF.type, cNeighbour);
	}
	
	/**
	 * Add a participant (non-direct peer) to the ontology.
	 * 
	 * @param participant Name of participant.
	 */
	public void addParticipant(String participant){
		ontology.createResource(prefix + participant).addProperty(RDF.type, cParticipant);
	}
	
	/**
	 * Relate a neighbour or participant (1) with another participant (2).
	 * 
	 * @param participant1 Name of first participant or neighbour.
	 * @param participant2 Name of second participant.
	 */
	public void pair(String participant1, String participant2){
		Resource _participant2 = ontology.createResource(prefix + participant2);
		ontology.createResource(prefix + participant1).addProperty(pKnows, _participant2);
	}
	
	/**
	 * Set a participant's role in the application.
	 * 
	 * @param role Name of role to be carried out by participant.
	 */
	public void setRole(String participant, String role){		
		Resource _role = ontology.createResource(prefix + role);
		Property pHasRole = ontology.createProperty(prefix + "hasRole");
		ontology.createResource(prefix + participant).addProperty(pHasRole, _role);
	}
	
	private void print(String stage, StmtIterator si){
		System.out.println("---------- BEGIN " + stage + " ----------");		
		Statement st = null;
		while(si.hasNext()){
			st = si.next();
			System.out.println(st);
		}
		System.out.println("------------ END " + stage + " ----------");		
	}
	
	public void fullPrint(){
		System.out.println("---------- BEGIN " + "FULL PRINT" + " ----------");
		StmtIterator si = ontology.listStatements();
		Statement st = null;
		while(si.hasNext()){
			st = si.next();
			System.out.println(st);
		}
		System.out.println("---------- END " + "FULL PRINT" + " ----------");
	}
	
	public void writeOntology(String filename){
		try{
			File file = new File(filename);
			FileOutputStream fos = new FileOutputStream(file);
			ontology.write(fos, "RDF/XML");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Get queries to be sent, but clear on call since these should only
	 * be processed once.
	 * @return Queries to be sent to other peers. Current set is cleared
	 * on call.
	 */
	public HashSet<String[]> getQueries(){
		//HashSet<String[]> _queries = new HashSet<String[]>(queries);
		HashSet<String[]> _queries = new HashSet<String[]>();
		for(String triple: queries){
			String[] _triple = triple.split(";");
			_queries.add(_triple);
		}
		queries.clear();
		return _queries;
	}
	
	/**
	 * Get informs to be sent, but clear on call since these should only
	 * be processed once.
	 * @return Informs to be sent to other peers. Current set is cleared
	 * on call.
	 */
	public HashSet<String[]> getInforms(){
		//HashSet<String[]> _informs = new HashSet<String[]>(informs);
		HashSet<String[]> _informs = new HashSet<String[]>();
		for(String triple: informs){
			String[] _triple = triple.split(";");
			_informs.add(_triple);
		}
		informs.clear();
		return _informs;
	}
	
	/**
	 * Add a triple to the ontology. The triple will most likely come
	 * from an INFORM message.
	 * 
	 * @param triple Ontology triple in the form of <subject, predicate, object>
	 */
	public void addTriple(String[] triple){
		String predicate = triple[1];
		if(predicate.equals("metis:quantity")){		
			addQuantity(triple);
		}else if(predicate.equals("metis:hasResource")){		
			addResourcePosition(triple);			
		}else if(predicate.equals(prefix + "positionedAt")){
			Resource s = ontology.createResource(triple[0]);
			Resource o = ontology.createResource(triple[2]);
			ontology.add(s, pPositionedAt, o);
		}
	}
	
	/**
	 * Respond to TripleEventListener's event firing.
	 * 
	 * In this case, the event is fired when a property test fails in a rule. Therefore, this
	 * method should create a query to retrieve the resource to satisfied the failed rule
	 * and send it to the appropriate neighbour.
	 */
	public void listen(TripleEvent te){	
		String triple = te.triple()[0] + ";" + te.triple()[1] + ";" + te.triple()[2];
		//queries.add(te.triple());
		queries.add(triple);
	}
	
	public void addQuery(String[] query){
		String triple = query[0] + ";" + query[1] + ";" + query[2];
		//queries.add(query);	
		queries.add(triple);
	}
	
	public void addInform(String[] inform){		
		String triple = inform[0] + ";" + inform[1] + ";" + inform[2];
		//informs.add(inform);	
		informs.add(triple);
	}
	
	public boolean ask(String[] triple){
		boolean result = false;
		String ask = triple[0] + " " + triple[1] + " " + triple[2];		
		if(triple[1].equals("metis:quantity")){			
			ask = triple[0] + " " + triple[1] + " ?quantity . FILTER (?quantity >= " + triple[2] + ") .";
		}else if(triple[1].equals("metis:hasResource")){
			ask = "<" + triple[0] + "> " + "metis:beenVisited" + " ?visited . FILTER (?visited = true) .";
		}
		
		String SPARQL = "prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +				
				"prefix metis: <http://www.abdn.ac.uk/metis.owl#> \n";		
		SPARQL += String.format("ask where {" +						
						"%s" +								
						"}", ask);
		Query query = QueryFactory.create(SPARQL);
		QueryExecution qexec = QueryExecutionFactory.create(query, ontology);
		try{
			result = qexec.execAsk();
		}finally{
			qexec.close();
		}
		return result;
	}
	
	public ArrayList<String> selectTiles(String[] triple){		
		String SPARQL = "prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +				
				"prefix metis: <http://www.abdn.ac.uk/metis.owl#> \n";		
		SPARQL += String.format("select ?tile where {" +
						"?tile rdf:type metis:Tile ." +
						"?tile metis:beenVisited true ." +
						"?tile metis:hasResource %s ." +								
						"}", triple[2]);
		Query query = QueryFactory.create(SPARQL);
		QueryExecution qexec = QueryExecutionFactory.create(query, ontology);
		ResultSet result;
		ArrayList<String> tiles = new ArrayList<String>();
		try{
			result = qexec.execSelect();
			while(result.hasNext()){
				QuerySolution qs = result.next();
				tiles.add(qs.get("tile").toString());
			}
		}finally{
			qexec.close();
		}
		return tiles;
	}
	
	public void addQuantity(String[] triple){		
		String subject = triple[0].replaceAll("metis:", prefix);
		String predicate = triple[1].replaceAll("metis:", prefix);
		int object = Integer.parseInt(triple[2]);
		Resource r = ontology.createResource(subject);		
		Property p = ontology.createProperty(predicate);
		int quantity = r.getProperty(p).getInt();
		int newQuantity = quantity + object;
		r.removeAll(p);
		r.addLiteral(p, new Integer(newQuantity));		
	}	
	
	public void removeQuantity(String[] triple){		
		String subject = triple[0].replaceAll("metis:", prefix);
		String predicate = triple[1].replaceAll("metis:", prefix);
		int object = Integer.parseInt(triple[2]);
		Resource r = ontology.createResource(subject);		
		Property p = ontology.createProperty(predicate);
		int quantity = r.getProperty(p).getInt();
		int newQuantity = quantity - object;
		r.removeAll(p);
		r.addLiteral(p, new Integer(newQuantity));
	}
	
	public void addResourcePosition(String[] triple){
		String tile = triple[0].replaceAll("metis:", prefix);
		String predicate = triple[1].replaceAll("metis:", prefix);
		String raw = triple[2].replaceAll("metis:", prefix);
		Resource s = ontology.createResource(tile);
		Property p = ontology.getProperty(predicate);
		Resource o = ontology.createResource(raw);
		Resource check = s.getPropertyResourceValue(p);
		if(check.equals(o)){
			// Update the tile to say we've "visited it".
			// This prevents unnecessary INFORMS being created if we move to this tile. 
			Property pBeenVisited = ontology.getProperty(prefix + "beenVisited");
			s.removeAll(pBeenVisited);
			s.addLiteral(pBeenVisited, true);
			//System.out.println("Updated tile! " + tile);			
		}else{
			System.err.println("Received bad information about a tile!");
		}
	}
	
	public String getResourceName(String[] triple){
		String tile = triple[0].replaceAll("metis:", prefix);
		String predicate = triple[1].replaceAll("metis:", prefix);		
		Resource s = ontology.createResource(tile);
		Property p = ontology.getProperty(predicate);
		return s.getPropertyResourceValue(p).toString();
	}
	
	/**
	 * Add a virtual path of peers to the ontology. This path is considered
	 * to be a "branch" of the network itself. This path can be used for
	 * document-routing future messages to closer-matching roles.
	 * The path itself is the successful route from a query's original
	 * starting point to a peer who replied with an inform. Therefore,
	 * path[0] will be this peer, path[1] will be a neighbour of this
	 * peer and peer[length - 1] will be the peer who replied with an
	 * inform. All peers in-between will be added as well.
	 * 
	 * @param path List of peer IDs which passed a query from source
	 * to destination, i.e. the peer who replied with an INFORM.
	 */
	public void analyzePath(ArrayList<Integer> path, int pid, int thisID){		
		// 0 = this peer
		// 1 = neighbour peer
		int startIndex = 2;
		int endIndex = path.size();
		// We can save time by not having to analyse a path with only 2 participants:
		// these participants will be this peer and a neighbour peer.
		if(path.size() < 3){
			return;
		}		
		for(int i = startIndex; i < endIndex; i++){
			int nodeID = path.get(i);			
			if(nodeID == path.get(0) || nodeID == path.get(1) || nodeID == thisID){
				/*
				throw new IllegalArgumentException("No loops are permitted in message path - nodeID: " + path.get(0) + 
						", neighbourID: " + path.get(1) + 
						", expectedID: " + nodeID + 
						", path: " + path);
						*/
				System.err.println("ERROR - Loop detected in message path: " + path + " - Discarding path");
				return;
			}
			int prevNodeID = path.get(i-1);
			Node nextNode = Network.get(nodeID);
			Peer nextPeer = (Peer)nextNode.getProtocol(pid);
			String nextRole = nextPeer.getRole();
			addParticipant("participant"+nodeID);
			setRole("participant"+nodeID, nextRole);
			pair("participant"+prevNodeID, "participant"+nodeID);
		}
	}
	
//	public ArrayList<Integer> getBestParticipants(String[] triple, String type){
//		ArrayList<Integer> participants = new ArrayList<Integer>();
//		List<String> roles = new ArrayList<String>();
//		String subject = triple[0];
//		String object = triple[2];
//		String roleSPARQL = "FILTER (";
//		if(type.equals("query")){
//			roles = Roles.BESTQUERYROLES.get(object);			
//		}else if(type.equals("inform")){
//			roles = Roles.BESTINFORMROLES.get(object);
//		}
//		
//		/*
//		 * There are no roles to search for (likely to be a nil resource),
//		 * so return nothing.
//		 */
//		if(roles == null){
//			return null;
//		}
//		
//		for(String role: roles){
//			roleSPARQL += "?role = " + role + " || ";
//		}
//		roleSPARQL = roleSPARQL.substring(0, roleSPARQL.length() - 4); // Get rid of " || " after last role
//		roleSPARQL += ") .";
//		
//		// This SPARQL query retrieves participants who has a role which produces
//		// or consumes (depending on "type") the "subject" in the given "triple"
//		String SPARQL = "prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +				
//						"prefix metis: <http://www.abdn.ac.uk/metis.owl#> \n";		
//		SPARQL += String.format("select ?participant where {" +
//								"?participant metis:hasRole ?role ." +
//								"%s" +								
//								"}", roleSPARQL);
//		Query query = QueryFactory.create(SPARQL);
//		QueryExecution qexec = QueryExecutionFactory.create(query, ontology);
//		try{
//			ResultSet results = qexec.execSelect();
//			while(results.hasNext()){
//				QuerySolution soln = results.next();				
//				Resource p = soln.getResource("participant");
//				try{					
//					int participantID = Integer.parseInt(p.toString().replaceAll("\\D+",""));
//					participants.add(participantID);
//				}catch(NumberFormatException nfe){
//					// Query found participantself (i.e. itself) so no point querying itself.
//					//System.err.println("Could not retrieve participant ID: " + p.toString());					
//				}
//			}
//		}finally{
//			qexec.close();
//		}	
//		return participants;
//	}

	public ArrayList<Integer> getBestParticipants(String[] triple, String type){	
		ArrayList<Integer> participants = new ArrayList<Integer>();
		List<String> roles = new ArrayList<String>();
		String subject = triple[0];
		String object = triple[2];
		String roleSPARQL = "FILTER (";
		if(type.equals("query")){
			List<String> temp = Roles.BESTQUERYROLES.get(object);
			if(temp != null){
				roles = new ArrayList<String>(temp);
			}else{
				roles = null;
			}					
		}else if(type.equals("inform")){
			List<String> temp = Roles.BESTINFORMROLES.get(object);
			if(temp != null){
				roles = new ArrayList<String>(temp);
			}else{
				roles = null;
			}
		}
		
		/*
		// Random fail
		boolean fail = false;
		// 0, 25, 50, 75, 100
		int target = 75 - 1;
		if(CommonState.r.nextInt(100) > target){
			fail = true;
		}
		if(fail && roles != null){
			Collections.reverse(roles);
		}
		*/		
		
		
		/*
		 * There are no roles to search for (likely to be a nil resource),
		 * so return nothing.
		 */
		if(roles == null){
			return null;
		}
		
		for(String role: roles){
			roleSPARQL += "?role = " + role + " || ";
		}
		roleSPARQL = roleSPARQL.substring(0, roleSPARQL.length() - 4); // Get rid of " || " after last role
		roleSPARQL += ") .";
		
		// This SPARQL query retrieves participants who has a role which produces
		// or consumes (depending on "type") the "subject" in the given "triple"
		String SPARQL = "prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +				
						"prefix metis: <http://www.abdn.ac.uk/metis.owl#> \n";		
		SPARQL += String.format("select ?participant where {" +
								"?participant metis:hasRole ?role ." +
								"%s" +								
								"}", roleSPARQL);
		Query query = QueryFactory.create(SPARQL);
		QueryExecution qexec = QueryExecutionFactory.create(query, ontology);
		try{
			ResultSet results = qexec.execSelect();
			//while(results.hasNext()){
			if(results.hasNext()){
				QuerySolution soln = results.next();				
				Resource p = soln.getResource("participant");
				try{					
					int participantID = Integer.parseInt(p.toString().replaceAll("\\D+",""));
					participants.add(participantID);
				}catch(NumberFormatException nfe){
					// Query found participantself (i.e. itself) so no point querying itself.
					//System.err.println("Could not retrieve participant ID: " + p.toString());					
				}
			}
		}finally{
			qexec.close();
		}
		
		// Random order of peers to try	(still using PeerSim's random generator)
		//if(participants != null){
		//	Collections.shuffle(participants, CommonState.r);
		//}
		return participants;
	}
	
	public int getClosestNeighbour(int participantID, int limit){
		int neighbourID = -1;
		String pathLength = "+";
		if(limit > 0){
			pathLength = "{1," + limit + "}";
		}
		String SPARQL = "prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +				
				"prefix metis: <http://www.abdn.ac.uk/metis.owl#> \n";		
		SPARQL += String.format("select ?neighbour where {" +
								"metis:participant%s metis:knows%s ?neighbour ." +
								"?neighbour rdf:type metis:Neighbour ." +
								"}", participantID, pathLength);
		Query query = QueryFactory.create(SPARQL);
		QueryExecution qexec = QueryExecutionFactory.create(query, ontology);
		try{
			ResultSet results = qexec.execSelect();
			if(!results.hasNext()){
				return -1;			
			}					
			while(results.hasNext()){
				QuerySolution soln = results.next();				
				Resource n = soln.getResource("neighbour");
				try{					
					neighbourID = Integer.parseInt(n.toString().replaceAll("\\D+",""));					
					break;
				}catch(NumberFormatException nfe){
					throw new IllegalArgumentException("Could not retrieve neighbour ID: " + n.toString());
				}
			}
		}finally{
			qexec.close();
		}	
		return neighbourID;
	}
	
	public ArrayList<Integer> getPath(int neighbourID, int participantID){
		ArrayList<Integer> path = new ArrayList<Integer>();		
		// If there is no participant to talk to (i.e. randomised neighbour),
		// no need to calculate path.
		if(participantID == -1){
			path.add(neighbourID);
			return path;
		}
		Resource s = ontology.createResource(prefix + "neighbour" + neighbourID);
		RDFNode o = ontology.createResource(prefix + "participant" + participantID);
		Filter<Statement> filter = Filter.any();
		OntTools.Path ontPath = OntTools.findShortestPath(ontology, s, o, filter);
		if(ontPath == null){
			fullPrint();
		}
		for(Iterator<Statement> it = ontPath.iterator(); it.hasNext(); ){
			Statement statement = it.next();
			String intermediateParticipant = statement.getObject().toString();
			try{
				int intermediateID = Integer.parseInt(intermediateParticipant.replaceAll("\\D+",""));
				path.add(intermediateID);
			}catch(NumberFormatException nfe){
				System.err.println("Could not retrieve neighbour ID");
			}
		}		
		return path;
	}	
}
