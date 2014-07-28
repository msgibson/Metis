package uk.ac.abdn.csd.metis.p2p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import com.hp.hpl.jena.ontology.OntModel;

import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import uk.ac.abdn.csd.metis.Metis;
import uk.ac.abdn.csd.metis.app.Player;
import uk.ac.abdn.csd.metis.p2p.msg.Message;
import uk.ac.abdn.csd.metis.p2p.msg.Message.Type;

public class Peer implements CDProtocol, EDProtocol{

	private Metis metis;
	private String prefix = "";
	private String role = "";	
	
	private HashSet<Message> inputQueries;
	private HashSet<Message> inputInforms;
	private HashSet<Message> inputMessages;
	
	private HashSet<Message> outputQueries;
	private HashSet<Message> outputInforms;	
	private HashSet<Message> outputMessages;
	
	private HashSet<UUID> oldMessages;
	private HashSet<String> oldInforms;
	
	private HashMap<UUID, ArrayList<Integer>> participantOrder;
	private HashMap<String, Integer> directedInforms;
	
	private Player player;
	
	private OntModel ontology;
	
	private int helpfulness;
	
	private String[] lastTriple = {"", "", ""};
	
	// Query, Inform, EOL, Send, Receive and Hop count
	private int qCount = 0, iCount = 0, eCount = 0, sCount = 0, rCount = 0, hCount = 0;
	
	public Peer(String prefix){
		
	}
	
	public void initialize(){
		inputQueries = new HashSet<Message>();
		inputInforms = new HashSet<Message>();
		inputMessages = new HashSet<Message>();
		
		outputQueries = new HashSet<Message>();
		outputInforms = new HashSet<Message>();		
		outputMessages = new HashSet<Message>();
		
		oldMessages = new HashSet<UUID>();
		oldInforms = new HashSet<String>();
		
		participantOrder = new HashMap<UUID, ArrayList<Integer>>();
		
		directedInforms = new HashMap<String, Integer>();
	}
	
	public Object clone(){
		Peer peer = null;	
		try{
			peer = (Peer)super.clone();
			peer.initialize();
		}catch(CloneNotSupportedException e){
		}		
		return peer;
	}
	
	public void run(){
		metis.run();
	}
	
	public void createMetis(String prefix, String ontology, String rules, boolean traceOn){		
		this.prefix = prefix + "#"; // PeerSim script uses # as a comment, so have to force it here
		metis = new Metis(this.prefix, ontology, rules, rules, traceOn);
		this.ontology = metis.getOntology();
	}
	
	public ArrayList<Integer> getParticipantOrder(UUID uuid){
		return participantOrder.get(uuid);
	}
	
	public void putParticipantOrder(UUID uuid, ArrayList<Integer> participantList){
		participantOrder.put(uuid, participantList);
	}
	
	public void addNeighbour(String neighbour){		
		String neighbourName = "neighbour" + neighbour;
		String participantName = "participant" + neighbour;
		metis.addNeighbour(neighbourName);
		metis.addParticipant(participantName);
		metis.pair(neighbourName, participantName);		
	}
	
	public void addParticipant(String id, String participant){
		if(participant.equals("self")){
			player = new Player(id, this, ontology);
		}else{
			metis.addParticipant(participant);
		}
	}
	
	public Player getPlayer(){
		return player;
	}
	
	public void setRole(String participant, String role){		
		if(participant.equals("self")){
			player.setRole(role);
			this.role = role;
		}else{
			metis.setRole("participant" + participant, role);
		}
	}
	
	public void setMap(int width, int height, int[][] grid){
		player.setMap(width, height, grid);
	}
	
	public void setPosition(int x, int y){
		player.setPostion(x, y);
	}
	
	public void setHelpfulness(int help){
		player.setHelpfulness(help);
		helpfulness = help;
	}
	
	public void setTolerance(int tolerance){
		player.setTolerance(tolerance);
	}
	
	public void setPerformance(int performance){
		player.setPerformance(performance);
	}
	
	public void setWillCoord(int willCoord){
		player.setWillCoord(willCoord);
	}
	
	public String getRole(){
		return role;
	}
	
	public ArrayList<Integer> getBestParticipants(String[] triple, String type){
		return metis.getBestParticipants(triple, type);
	}
	
	
	// Only for Experiment 3 - patience of peer against reasoner
	/*
	public int getClosestNeighbour(int participantID, int limit, Linkable linkable){
		if(player.getTolerance() == 0){
			if(CommonState.r.nextInt(100) > 49){
				return (int)linkable.getNeighbor(CommonState.r.nextInt(linkable.degree())).getID();
			}else{
				return metis.getClosestNeighbour(participantID, limit);
			}
		}else if(player.getTolerance() == 1){
			if(CommonState.r.nextInt(100) > 74){
				return (int)linkable.getNeighbor(CommonState.r.nextInt(linkable.degree())).getID();
			}else{
				return metis.getClosestNeighbour(participantID, limit);
			}
		}else{
			return metis.getClosestNeighbour(participantID, limit);
		}		
	}
	*/		
	
	public int getClosestNeighbour(int participantID, int limit){		
		return metis.getClosestNeighbour(participantID, limit);			
	}	
	
	public ArrayList<Integer> getPath(int neighbourID, int participantID){
		return metis.getPath(neighbourID, participantID);
	}
	
	public void addInputQuery(Message message){
		inputQueries.add(message);
	}
	
	public void addInputInform(Message message){
		inputInforms.add(message);
	}
	
	public void addInputMessage(Message message){
		inputMessages.add(message);
	}
	
	public void addOutputQuery(Message message){
		outputQueries.add(message);
	}
	
	public void addOutputInform(Message message){
		outputInforms.add(message);
	}
	
	public void addOutputMessage(Message message){
		outputMessages.add(message);
	}
	
	public void addQuery(String[] query){
		metis.addQuery(query);
	}
	
	public void addInform(int id, String[] inform){
		metis.addInform(inform);
		directedInforms.put(inform[0] + ";" + inform[1] + ";" + inform[2], id);
	}
	
	public void fullPrint(){
		metis.fullPrint();
	}
	
	public void writeOntology(String filename){
		metis.writeOntology(filename);
	}
	
	public HashSet<Message> getInputQueries(){
		HashSet<Message> _queries = new HashSet<Message>(inputQueries);
		inputQueries.clear();
		return _queries;
	}
	
	public HashSet<Message> getInputInforms(){
		HashSet<Message> _informs = new HashSet<Message>(inputInforms);
		inputInforms.clear();
		return _informs;
	}
	
	public HashSet<Message> getInputMessages(){
		HashSet<Message> _messages = new HashSet<Message>(inputMessages);
		inputMessages.clear();
		return _messages;
	}
	
	public HashSet<Message> getOutputQueries(){
		HashSet<Message> _queries = new HashSet<Message>(outputQueries);
		outputQueries.clear();
		//outputQueries = new HashSet<Message>();
		return _queries;
	}
	
	public HashSet<Message> getOutputInforms(){
		HashSet<Message> _informs = new HashSet<Message>(outputInforms);
		outputInforms.clear();
		//outputInforms = new HashSet<Message>();
		return _informs;
	}
	
	public HashSet<Message> getOutputMessages(){
		HashSet<Message> _messages = new HashSet<Message>(outputMessages);
		outputMessages.clear();
		//outputMessages = new HashSet<Message>();
		return _messages;
	}
	
	public HashSet<String[]> getQueryTriples(){
		return metis.getQueries();
	}
	
	public HashSet<String[]> getInformTriples(){
		return metis.getInforms();
	}
	
	public void addTriple(String[] triple){
		metis.addTriple(triple);
	}
	
	public void addQuantity(String[] triple){
		metis.addQuantity(triple);
	}
	
	public void removeQuantity(String[] triple){
		metis.removeQuantity(triple);
	}
	
	public boolean ask(String[] triple){
		if(helpfulness == 0){
			// 50% chance of helping (0-99)
			if(CommonState.r.nextInt(100) > 49){
				return false;
			}else{
				return metis.ask(triple);
			}
		}else if(helpfulness == 1){			
			// 75% chance helping
			if(CommonState.r.nextInt(100) > 74){
				return false;
			}else{
				return metis.ask(triple);
			}
		}else{
			// 100% chance helping
			return metis.ask(triple);
		}
	}
	
	/*
	 * Only help out if "helpfulness" is high. This is part of a
	 * peer's altruism.
	 */
	public ArrayList<String> selectTiles(String[] triple){		
		if(helpfulness == 0){
			return new ArrayList<String>();
		}else if(helpfulness == 1){
			if(CommonState.r.nextBoolean()){
				return metis.selectTiles(triple);
			}else{
				return new ArrayList<String>();
			}
		}else{
			return metis.selectTiles(triple);
		}
	}
	
	public String getResourceName(String[] triple){
		return metis.getResourceName(triple);
	}
	
	public void analyzePath(ArrayList<Integer> path, int pid, int thisID){
		metis.analyzePath(path, pid, thisID);
	}
	
	public void addOldMessage(UUID id){
		oldMessages.add(id);
	}
	
	public boolean containsOldMessage(UUID id){
		return oldMessages.contains(id);
	}
	
	public void addOldInform(String[] inform){
		oldInforms.add(inform[0] + ";" + inform[1] + ";" + inform[2]);
	}
	
	public boolean containsOldInform(String[] inform){
		return oldInforms.contains(inform[0] + ";" + inform[1] + ";" + inform[2]);
	}
	
	/**
	 * Store incoming messages for processing during node's turn (nextCycle).
	 */
	public void processEvent(Node node, int pid, Object event) {		
		Message msg = (Message)event;
		UUID id = msg.getID();		
		Peer peer = (Peer)node.getProtocol(pid);	
		
		ArrayList<Integer> messagePath = msg.getPath();
		
		// In some cirumstances, we don't want to see an old message (e.g. if we already exhausted our neighbour
		// list on a query), so don't bother processing again.
		if(peer.containsOldMessage(id)){
			if(msg.getType().equals(Message.Type.QUERY)){
				bounce(node, peer, msg);
			}
			return;
		}
		
		// As above, repeat messages shouldn't be circulated. Informs, however, present a dilemma -- freeflow of
		// information should continue, but eventually old messages being continuously circulated will overflow
		// the network. Therefore, if an inform triple has been spotted before, discard it.
		// Note: For now, only worrying about "hasResource" informs since they take the bulk!
		if(msg.getType().equals(Message.Type.INFORM)){
			if(peer.containsOldInform(msg.getTriple()) && msg.getTriple()[1].contains("hasResource")){
				return;
			}
		}
		switch(msg.getType()){
		case QUERY:
			peer.addInputQuery(msg);
			peer.addOldMessage(id);
			break;
		case INFORM:
			peer.addInputInform(msg);
			peer.addOldInform(msg.getTriple());
			break;			
		default:
			peer.addInputMessage(msg);			
			break;
		}
		rCount++;
		if(msg.getHops() > hCount){
			hCount = msg.getHops();
		}
		//System.out.println(node.getID() + " RECEIVED: " + msg.toString());
	}
	
	public void nextCycle(Node node, int pid) {	
		System.out.println("*** BEGIN: node " + node.getID() + ", time: " + CommonState.getTime() + " ***");
		Peer peer = (Peer)node.getProtocol(pid);	
		Linkable linkable = (Linkable)node.getProtocol(FastConfig.getLinkable(pid));
		
		// Handle inputs
		System.out.println("* INPUT *");
		input(node, pid, linkable);
		
		// Handle process
		System.out.println("* PROCESS *");
		process(node, peer);
		
		// Handle outputs
		System.out.println("* OUTPUT *");
		output(node, pid, linkable, peer);
		
		System.out.println("*** END: node " + node.getID() + ", time: " + CommonState.getTime() + " ***");
		System.out.println(player.toString());
	}
	
	private void bounce(Node node, Peer peer, Message msg){
		Message EOL = new Message(Type.EOL, node, msg.getStart(), msg.getTriple());
		EOL.setID(msg.getID());
		EOL.setPath(msg.getPath());
		EOL.setHops(msg.getHops());
		EOL.decrementHops();
		int hops = EOL.getHops();
		int previousID = msg.getPath().get(hops);		
		Node previousNode = Network.get(previousID);
		if(previousNode.getID() == node.getID()){
			// Something weird happened in enhanced document routing...
			// Send EOL to last peer in message path (although this should already work with the hops..!)
			previousNode = Network.get(msg.getPath().get(msg.getPath().size() - 1));
		}
		EOL.setReceiver(previousNode);
		peer.addOutputMessage(EOL);
		eCount++;		
	}
	
	private void input(Node node, int pid, Linkable linkable){
		Peer peer = (Peer)node.getProtocol(pid);	
		// Handle any messages received during last cycle and
		// reply appropriately.
		
		// INFORM: If this peer is the recipient of the message, update
		// the ontology with the Inform's information. Otherwise, pass
		// it back according to Inform's message path.
		HashSet<Message> informs = peer.getInputInforms();
		for(Message inform: informs){
			// If INFORM is for peer, add to ontology
			// Else, forward to peer next in message path
			// Also look at mesage path to see if a new
			// participant can be added
						
			Node end = inform.getEnd();
			ArrayList<Integer> messagePath = inform.getPath();
			if(node == end){	
				peer.analyzePath(messagePath, pid, (int)node.getID());
				String[] triple = inform.getTriple();
				if(triple[1].equals("metis:coord")){
					player.planCoord(triple);
				}else if(triple[1].equals(prefix + "positionedAt")){
					peer.addTriple(triple);
				}else if(triple[1].equals("metis:exchange")){
					player.exchangeSuccessful(triple);
					player.increaseActualHappiness(player.EXCHANGE_WEIGHT);
					player.increasePossibleHappiness(player.EXCHANGE_WEIGHT);
				}else if(triple[1].equals(prefix + "coordEnd")){
					// Participant who started coord has finished, so we don't have to wait/assist anymore.					
					player.stopWaiting();
				}else{
					peer.addTriple(triple);					
					player.planPath(triple);
					player.increaseActualHappiness(player.INFO);
					player.increasePossibleHappiness(player.INFO);
					// Pass on information (tile's resource) to other players
					if(player.isHelpful()){
						metis.addInform(triple);
					}
				}
			}else{
				int hops = inform.getHops();
				if(hops == 0){
					int endID = (int)inform.getEnd().getID();
					int closestNeighbour = peer.getClosestNeighbour(endID, 0);
					//int closestNeighbour = peer.getClosestNeighbour(endID, 0, linkable);
					if(closestNeighbour == -1){
						// If we can't find a path to the best peer (no closest neighbour found)
						// then randomly chose one
						int randomID = CommonState.r.nextInt(linkable.degree());
						Node randomNeighbour = linkable.getNeighbor(randomID);
						messagePath.add(0, (int)randomNeighbour.getID());
						inform.setSender(node);
						inform.setReceiver(randomNeighbour);
						peer.addOutputInform(inform);
					}else{
						Node nextNode = Network.get(closestNeighbour);
						messagePath.add(0, (int)nextNode.getID());
						inform.setSender(node);
						inform.setReceiver(nextNode);
						peer.addOutputInform(inform);
					}					
				}else{
					int nextNodeID = messagePath.get(hops - 1);
					Node nextNode = Network.get(nextNodeID);
					inform.setSender(node);
					inform.setReceiver(nextNode);
					inform.decrementHops();
					peer.addOutputInform(inform);
				}
			}
		}
		
		// QUERY: Reply with Inform if this peer can answer, otherwise
		// pass query on to another peer, either the path inside the
		// Query message or this peer makes a decision. If all else
		// fails, reply with EOL (end-of-line).
		HashSet<Message> queries = peer.getInputQueries();
		for(Message query: queries){			
			// Attempt to answer query
			// If successful, reply with an INFORM
			// Else forward to peer in message path,
			// but if message path is empty (this
			// peer is the last one in this path)
			// perform document routing
			
			String[] triple = query.getTriple();
			Node receiver = query.getReceiver();
			Node end = query.getEnd();
			if(end == node){
				// We can use the message path to create virtual peer paths for our gain.
				// This is especially useful if we want to inform a coord peer when
				// we arrive at the destination. The path must be reversed because we will
				// appear last in the message path, but we have to specify ourself first
				// to analyse the path.
				// Have to make another arraylist, otherwise the path in the message will be
				// reversed as well!
				
				ArrayList<Integer> path = new ArrayList<Integer>(query.getPath());				
				Collections.reverse(path);
				peer.analyzePath(path, pid, (int)node.getID());
			}
			if(receiver == node){
				// Query is about tile's raw resource
				if(triple[0].equals("?tile")){
					ArrayList<String> tiles = peer.selectTiles(triple);
					if(!tiles.isEmpty()){
						for(String tile: tiles){
							String[] newTriple = {tile, triple[1], triple[2]};
							Message inform = new Message(Type.INFORM, node, query.getStart(), newTriple);
							inform.setID(query.getID());
							inform.setPath(query.getPath());	// Still using the same path, but now in reverse
							inform.setHops(query.getHops());
							inform.setReceiver(query.getSender());
							inform.decrementHops();
							peer.addOutputInform(inform);	
						}
					}else{
						passQueryOn(node, peer, query, linkable);
					}
				}else if(triple[1].contains("metis:coord")){
					String reply = player.coord((int)query.getStart().getID(), triple);
					if(reply.equals("false")){
						passQueryOn(node, peer, query, linkable);
					}else{						
						triple[0] = prefix + "participant" + node.getID();
						triple[2] = reply;
						Message inform = new Message(Type.INFORM, node, query.getStart(), triple);
						inform.setID(query.getID());
						inform.setPath(query.getPath());	// Still using the same path, but now in reverse
						inform.setHops(query.getHops());
						inform.setReceiver(query.getSender());
						inform.decrementHops();
						peer.addOutputInform(inform);
					}
				}else if(triple[1].contains("metis:exchange")){
					String reply = player.exchange(triple);
					if(reply.equals("false")){
						passQueryOn(node, peer, query, linkable);
					}else{						
						Message inform = new Message(Type.INFORM, node, query.getStart(), triple);
						inform.setID(query.getID());
						inform.setPath(query.getPath());	// Still using the same path, but now in reverse
						inform.setHops(query.getHops());
						inform.setReceiver(query.getSender());
						inform.decrementHops();
						peer.addOutputInform(inform);
					}				
				}else{
					// Query is about this peer's resources or if tile has a penalty
					boolean result = peer.ask(triple);			
					if(result){						
						String resourceName = peer.getResourceName(triple);
						triple[2] = resourceName;
						Message inform = new Message(Type.INFORM, node, query.getStart(), triple);
						inform.setID(query.getID());
						inform.setPath(query.getPath());	// Still using the same path, but now in reverse
						inform.setHops(query.getHops());
						inform.setReceiver(query.getSender());
						inform.decrementHops();
						peer.addOutputInform(inform);				
					}else{
						passQueryOn(node, peer, query, linkable);
					}	
				}
			}else{
				passQueryOn(node, peer, query, linkable);
			}			
		}
		
		// Now that all exchange queries have been dealt with, we can start exchanging in the next round.
		player.stopExchanging();
		
		// Other messages (EOL): Re-route a failed query to another neighbour
		// or pass the EOL up the path. If the receiver of the EOL is the same
		// as the creator of the relating query, this means the network cannot
		// answer the query, so try something else!?
		HashSet<Message> messages = peer.getInputMessages();
		for(Message msg: messages){
			switch(msg.getType()){
			case EOL:
				Node receiver = msg.getEnd();
				// If receiving node is the same as related query creator, then network has failed answering query.
				if(node == receiver){
					ArrayList<Integer> messagePath = msg.getPath();
					peer.analyzePath(messagePath, pid, (int)node.getID());
					UUID uuid = msg.getID();
					ArrayList<Integer> participants = peer.getParticipantOrder(uuid);
					if(participants == null || participants.isEmpty()){
						//System.out.println("Could not find answer for " + msg.getTripleAsString());
						if(msg.getTriple()[1].contains("metis:hasResource")){
							player.forceMove(msg.getTriple());
							player.increasePossibleHappiness(player.MOVE_WEIGHT);
						}else if(msg.getTriple()[1].contains("metis:exchange")){
							player.exchangeFailed(msg.getTriple());
							player.increasePossibleHappiness(player.EXCHANGE_WEIGHT);
						}else if(msg.getTriple()[1].contains("metis:coord")){
							player.cancelCoord();
							player.increasePossibleHappiness(player.COORD_GATHER_WEIGHT);
						}						
					}else{
						Iterator<Integer> it = participants.iterator();
						while(it.hasNext()){							
							int nextBestID = it.next();
							it.remove();
							peer.putParticipantOrder(uuid, participants);							
							if(!messagePath.contains(nextBestID)){
								Node nextBestNode = Network.get(nextBestID);
								
								// Use next-best neighbour to pass query on
								Message query = new Message(Type.QUERY, node, msg.getStart(), msg.getTriple());
								query.setID(msg.getID());
								
								int bestNeighbourID = peer.getClosestNeighbour(nextBestID, 0);
								//int bestNeighbourID = peer.getClosestNeighbour(nextBestID, 0, linkable);
								if(bestNeighbourID == -1){
									System.err.println("No suitable neighbour found.");
									continue;
								}
								messagePath = peer.getPath(bestNeighbourID, nextBestID);
								messagePath.add(0, (int)node.getID());
								Node bestNeighbourNode = Network.get(bestNeighbourID);										
								query.setReceiver(bestNeighbourNode);								
								query.setEnd(nextBestNode);
								query.setPath(messagePath);
								query.incrementHops();						
								if(query.getSender() == query.getReceiver()){
									throw new IllegalArgumentException("These should not be the same!");
								}
								if(!linkable.contains(bestNeighbourNode)){									
									throw new IllegalArgumentException("Next-best node is not a neighbour!");
								}
								peer.addOutputQuery(query);
								break;
							}else{
								//System.out.println("Could not find answer for " + msg.getTripleAsString());								
								peer.analyzePath(messagePath, pid, (int)node.getID());
								player.forceMove(msg.getTriple());
								player.increasePossibleHappiness(player.INFO);
							}
						}
					}
				}else{
					UUID uuid = msg.getID();
					ArrayList<Integer> participants = peer.getParticipantOrder(uuid);
					if(participants == null || participants.isEmpty()){
						// No more neighbours to try, so pass EOL back up path
						ArrayList<Integer> messagePath = msg.getPath();
						int hops = msg.getHops();
						int previousID = messagePath.get(hops - 1);						
						Node previousNode = Network.get(previousID);
						msg.setSender(node);
						msg.setReceiver(previousNode);
						msg.decrementHops();
						peer.addOutputMessage(msg);
						System.out.println("input " + msg.getID().toString());
					}else{
						// Use next-best neighbour to pass query on
						Iterator<Integer> it = participants.iterator();
						nextNeighbour:
						while(it.hasNext()){
							Message query = new Message(Type.QUERY, msg.getEnd(), node, msg.getTriple());
							query.setID(msg.getID());
							ArrayList<Integer> messagePath = msg.getPath();
							int nextBestID = it.next();
							it.remove();
							peer.putParticipantOrder(uuid, participants);
							Node nextBestNode = Network.get(nextBestID);						
							int nextBestNeighbourID = peer.getClosestNeighbour(nextBestID, 0);
							//int nextBestNeighbourID = peer.getClosestNeighbour(nextBestID, 0, linkable);
							Node nextBestNeighbourNode = Network.get(nextBestNeighbourID);
							ArrayList<Integer> extendedPath = peer.getPath(nextBestNeighbourID, nextBestID);
							// There is the possibility that the path generated may "overlap" with the existing message path,
							// creating a loop. Fixing this loop is complex, so for now abandon this new path and try another
							// neighbour if possible
							for(int pathID: extendedPath){
								if(messagePath.contains(pathID)){
									break nextNeighbour;
								}
							}
							int position = messagePath.indexOf((int)node.getID());
							messagePath = new ArrayList<Integer>(messagePath.subList(0, position + 1));
							messagePath.addAll(extendedPath);
							query.setPath(messagePath);
							query.setSender(node);
							query.setReceiver(nextBestNeighbourNode);							
							query.setEnd(nextBestNode);							
							query.setHops(position + 1);
							if(query.getSender() == query.getReceiver()){
								throw new IllegalArgumentException("These should not be the same!");
							}
							if(!linkable.contains(nextBestNeighbourNode)){
								throw new IllegalArgumentException("Next-best node is not a neighbour!");
							}
							peer.addOutputQuery(query);
						}
					}
				}
				break;
			}
		}
	}
	
	/**
	 * This peer cannot answer the query, so pass it on to somebody else.
	 * The "somebody else" will be either:
	 * 	1. Next peer in message path (should be a neighbour of this peer), or
	 * 	2. Use document routing to get the next-best peer, or
	 * 	3. Nothing else this peer can do, so send an EOL to creator of query.
	 * This is similar to depth-first searching.
	 * 
	 * @param node Node who received message.
	 * @param query Message being handled.
	 */		
	private void passQueryOn(Node node, Peer peer, Message query, Linkable linkable){		
		// State 1: Use next peer in message path
		ArrayList<Integer> messagePath = query.getPath();		
		int hops = query.getHops();
		int pathLength = messagePath.size();
		// If there are still peers left on the message path, get next peer and send query to it.
		if((hops + 1) < pathLength){
			int nextPeerID = messagePath.get(hops + 1);
			Node receiver = Network.get(nextPeerID);
			query.setSender(node);
			query.setReceiver(receiver);
			query.incrementHops();
			if(query.getSender() == query.getReceiver()){
				throw new IllegalArgumentException("These should not be the same!");
			}
			if(!linkable.contains(receiver)){
				throw new IllegalArgumentException("Next-best node is not a neighbour!");
			}
			peer.addOutputQuery(query);
			return;
		}
		
		// State 1.9: Check for destination peer first; this takes priority over document-routing
		// (primarily useful for enhanced document-routing)
		// HACK: Turn off in other modes?
		/*
		if(node.getID() != query.getEnd().getID()){
			int endID = (int)query.getEnd().getID();
			int closestNeighbour = peer.getClosestNeighbour(endID, 0);
			if(closestNeighbour == -1){
				// If we can't find a path to the best peer (no closest neighbour found)
				// then randomly chose one
				int randomID = CommonState.r.nextInt(linkable.degree());
				Node randomNeighbour = linkable.getNeighbor(randomID);
				messagePath.add(0, (int)randomNeighbour.getID());
				query.setSender(node);
				query.setReceiver(randomNeighbour);
				peer.addOutputQuery(query);				
			}else{
				Node nextNode = Network.get(closestNeighbour);
				messagePath.add(0, (int)nextNode.getID());
				query.setSender(node);
				query.setReceiver(nextNode);
				peer.addOutputQuery(query);				
			}	
			return;
		}
		*/
		
		// State 2: Perform document routing
		UUID uuid = query.getID();
		ArrayList<Integer> participants = peer.getParticipantOrder(uuid);
		if(participants == null){			
			String[] triple = query.getTriple();
			participants = peer.getBestParticipants(triple, "query");
			// There's a chance the sender can have the role to best-answer the query.
			// But since the sender is passing the query on, it must not have the answer.
			// Therefore remove the sender as a possibility.
			// Don't ask why participants can be null in this state...
			if(participants != null){
				participants.remove(new Integer((int)query.getSender().getID()));
			}
		}
		if(participants != null && !participants.isEmpty()){
			Iterator<Integer> it = participants.iterator();
			nextNeighbour:
			while(it.hasNext()){
				//int nextBest = participants.remove(0);
				int nextBest = it.next();
				it.remove();
				peer.putParticipantOrder(uuid, participants);
				int bestNeighbourID = peer.getClosestNeighbour(nextBest, 0);
				//int bestNeighbourID = peer.getClosestNeighbour(nextBest, 0, linkable);
				if(bestNeighbourID != -1 && !messagePath.contains(bestNeighbourID)){
					ArrayList<Integer> extendedPath = peer.getPath(bestNeighbourID, nextBest);
					// There is the possibility that the path generated may "overlap" with the existing message path,
					// creating a loop. Fixing this loop is complex, so for now abandon this new path and try another
					// neighbour if possible
					for(int pathID: extendedPath){
						if(messagePath.contains(pathID)){
							break nextNeighbour;
						}
					}
					messagePath.addAll(extendedPath);
					Node receiver = Network.get(bestNeighbourID);
					Node end = Network.get(nextBest);
					query.setSender(node);
					query.setReceiver(receiver);
					query.setEnd(end);			
					query.setPath(messagePath);
					query.incrementHops();
					if(query.getSender() == query.getReceiver()){
						throw new IllegalArgumentException("These should not be the same!");
					}
					if(!linkable.contains(receiver)){
						throw new IllegalArgumentException("Next-best node is not a neighbour!");
					}
					peer.addOutputQuery(query);
					return;
				}
			}			
		}
		
		// State 3: Return EOL
		Message EOL = new Message(Type.EOL, node, query.getStart(), query.getTriple());
		EOL.setID(query.getID());
		EOL.setPath(messagePath);
		EOL.setHops(hops);
		EOL.decrementHops();
		hops = EOL.getHops();
		int previousID = messagePath.get(hops);
		Node previousNode = Network.get(previousID);
		EOL.setReceiver(previousNode);
		peer.addOutputMessage(EOL);
		eCount++;		
	}
	
	private void process(Node node, Peer peer){
		// AI/agent next-move decision here!
		
		// User selected rules (input commands, e.g. move, trade, convert resources, etc.)		
		player.run();
		//System.out.println(node.getID() + ", [Rules fired]: ");
		peer.run();		
	}
	
	private void output(Node node, int pid, Linkable linkable, Peer peer){
		// Get peer's own queries and informs to send
		HashSet<String[]> queryTriples = peer.getQueryTriples();
		HashSet<String[]> informTriples = peer.getInformTriples();
		
		// Process received messages to be passed on
		HashSet<Message> queryMessages = peer.getOutputQueries();
		HashSet<Message> informMessages = peer.getOutputInforms();
		HashSet<Message> otherMessages = peer.getOutputMessages();
		
		System.out.println("queryTriples: " + queryTriples.size()
				+ ", informTriples: " + informTriples.size()
				+ ", queryMessages: " + queryMessages.size()
				+ ", informMessages: " + informMessages.size()
				+ ", otherMessages: " + otherMessages.size());
		
		Iterator<Integer> it;
		
		for(Message query: queryMessages){
			Node sender = query.getSender();
			Node receiver = query.getReceiver();
			if(sender == node && linkable.contains(receiver)){
				if(receiver.isUp()){
					Transport tr = (Transport)node.getProtocol(FastConfig.getTransport(pid));
					tr.send(node, receiver, query, pid);
					//System.out.println(node.getID() + " SENT: " + query.toString());
				}else{
					throw new IllegalArgumentException("Neighbour not up");
				}
			}else{				
				if(sender != node){
					throw new IllegalArgumentException("Wrong sender");
				}
				if(!linkable.contains(receiver)){
					throw new IllegalArgumentException("Not a neighbour");
				}
			}
			sCount++;
		}
		
		for(Message inform: informMessages){
			Node sender = inform.getSender();
			Node receiver = inform.getReceiver();
			if(sender == node && linkable.contains(receiver)){
				if(receiver.isUp()){
					Transport tr = (Transport)node.getProtocol(FastConfig.getTransport(pid));
					tr.send(node, receiver, inform, pid);
					//System.out.println(node.getID() + " SENT: " + inform.toString());
				}else{
					throw new IllegalArgumentException("Neighbour not up");
				}
			}else{				
				if(sender != node){
					throw new IllegalArgumentException("Wrong sender");
				}
				if(!linkable.contains(receiver)){
					throw new IllegalArgumentException("Not a neighbour");
				}
			}
			sCount++;
		}
		
		for(Message message: otherMessages){
			Node sender = message.getSender();
			Node receiver = message.getReceiver();
			if(sender == node && linkable.contains(receiver)){
				if(receiver.isUp()){
					Transport tr = (Transport)node.getProtocol(FastConfig.getTransport(pid));
					tr.send(node, receiver, message, pid);
					//System.out.println(node.getID() + " SENT: " + message.toString());
				}else{
					throw new IllegalArgumentException("Neighbour not up");
				}
			}else{				
				if(sender != node){
					throw new IllegalArgumentException("Wrong sender");
				}
				if(!linkable.contains(receiver)){
					throw new IllegalArgumentException("Not a neighbour");
				}
			}
			sCount++;
		}		
		
		// Loop through query triples to send
		for(String[] triple: queryTriples){
			if(triple[0].equals(lastTriple[0]) && triple[1].equals(lastTriple[1]) && triple[2].equals(lastTriple[2])){
				continue;
			}
			lastTriple = triple;
			ArrayList<Integer> participants = peer.getBestParticipants(triple, "query");
			if(participants.isEmpty() || participants == null){
				// If we can't send a message, treat this as a failure by forcing an EOL.
				// Since the EOL is from us to us, we don't have to worry about analysing
				// the message path.
				Message EOL = new Message(Type.EOL, node, node, triple);
				peer.addInputMessage(EOL);
				eCount++;
				continue;
			}
			it = participants.iterator();
			while(it.hasNext()){
				// Get the best node to give the triple to
				int bestNodeID = it.next();
				Node bestNode = Network.get(bestNodeID);
				// Find a neighbour who can talk to the best node
				int bestNeighbourID = peer.getClosestNeighbour(bestNodeID, 0);
				//int bestNeighbourID = peer.getClosestNeighbour(bestNodeID, 0, linkable);
				// If we can't find a neighbour to talk to, cancel the query by creating an EOL.
				if(bestNeighbourID == -1){
					Message EOL = new Message(Type.EOL, node, node, triple);
					peer.addInputMessage(EOL);
					eCount++;
					continue;
				}
				Node bestNeighbour = Network.get(bestNeighbourID);
				if(linkable.contains(bestNeighbour)){
					if(bestNeighbour.isUp()){
						if(triple[0].contains("self")){
							triple[0] = prefix + "participant" + node.getID();
						}
						Message msg = new Message(Type.QUERY, node, bestNode, triple);	
						ArrayList<Integer> messagePath = peer.getPath(bestNeighbourID, bestNodeID);						
						messagePath.add(0, (int)node.getID());	// Add our own ID to path for backtracking
						
						Set<Integer> set = new HashSet<Integer>(messagePath);
						if(set.size() < messagePath.size()){
							System.err.println("Message path contains a loop! Fixing...");
							ArrayList<Integer> fixedPath = new ArrayList<Integer>();
							for(int i = messagePath.size() - 1; i >= 0; i--){
								if(messagePath.get(i) == (int)node.getID()){
									fixedPath = new ArrayList<Integer>(messagePath.subList(i, messagePath.size()));
									bestNeighbourID = fixedPath.get(1);
									bestNeighbour = Network.get(bestNeighbourID);
									break;
								}
							}
							messagePath = fixedPath;
							//throw new IllegalArgumentException("Loop detected! " + messagePath);
						}
						
						msg.setPath(messagePath);
						msg.setReceiver(bestNeighbour);						
						msg.incrementHops();
						if(msg.getSender() == msg.getReceiver()){
							throw new IllegalArgumentException("These should not be the same!");
						}
						Transport tr = (Transport)node.getProtocol(FastConfig.getTransport(pid));
						tr.send(node, bestNeighbour, msg, pid);
						//System.out.println(node.getID() + " SENT: " + msg.toString());
						it.remove();
						peer.putParticipantOrder(msg.getID(), participants);
						break;	// No need to try another neighbour if message was sent
					}
				}else{
					//peer.getClosestNeighbour(bestNodeID, 0);	
					throw new IllegalArgumentException("Best neighbour isn't actually a neighbour!");
				}				
			}
			qCount++;
			sCount++;
		}
		
		/*
		 * Since we are creating an inform message from scratch, the message path
		 * should be in the same order as a query. Therefore we have to REVERSE
		 * the list so that the destination peer comes first in the path and then
		 * add this peer to the end so that it looks like the destination peer
		 * sent a query with the required path first.
		 * This is a stupid way of doing it, but deal with it...
		 */
		for(String[] triple: informTriples){
			// See if we know of any peers with a suitable role for the triple
			ArrayList<Integer> participants = peer.getBestParticipants(triple, "inform");
			//System.out.println(triple[0] + " " + triple[1] + " " + triple[2]);
			if(participants == null){
				if(triple[1].contains("positionedAt")){
					// Hack to get co-ord participant
					participants = new ArrayList<Integer>();
					participants.add(directedInforms.remove(triple[0] + ";" + triple[1] + ";" + triple[2]));
				}else if(triple[1].contains("coordEnd")){
					participants = new ArrayList<Integer>();
					int participantID = Integer.parseInt(triple[0].replaceAll("\\D+",""));
					participants.add(participantID);
				}
			}
			participants.remove(new Integer((int)node.getID()));
			it = participants.iterator();
			while(it.hasNext()){
				// Get the best node to give the triple to
				int bestNodeID = it.next();
				Node bestNode = Network.get(bestNodeID);
				// Find a neighbour who can talk to the best node
				int bestNeighbourID = peer.getClosestNeighbour(bestNodeID, 0);
				//int bestNeighbourID = peer.getClosestNeighbour(bestNodeID, 0, linkable);
				if(bestNeighbourID == -1){
					//throw new IllegalArgumentException(node.getID() + ": " + bestNeighbourID + " does not exist!");					
					continue;
				}
				Node bestNeighbour = Network.get(bestNeighbourID);
				if(linkable.contains(bestNeighbour)){
					if(bestNeighbour.isUp()){						
						Message msg = new Message(Type.INFORM, node, bestNode, triple);	
						ArrayList<Integer> messagePath = peer.getPath(bestNeighbourID, bestNodeID);						
						messagePath.add(0, (int)node.getID());						

						// Hard to confirm, but SPARQL property paths don't guarantee the shortest path between
						// two properties. This can cause potential cycles in the path, but because of the unique
						// way I implemented Metis, there is no detection, except crashing! Therefore, this hack
						// trims the list so that the last occurance of this node becomes the first in the message
						// path, eliminating any previous cycles.
						Set<Integer> set = new HashSet<Integer>(messagePath);
						if(set.size() < messagePath.size()){
							System.err.println("ERROR: " + messagePath + " - Message path contains a loop! Fixing...");
							ArrayList<Integer> fixedPath = new ArrayList<Integer>();
							for(int i = messagePath.size() - 1; i >= 0; i--){
								if(messagePath.get(i) == (int)node.getID()){
									fixedPath = new ArrayList<Integer>(messagePath.subList(i, messagePath.size()));
									bestNeighbourID = fixedPath.get(1);
									bestNeighbour = Network.get(bestNeighbourID);
									break;
								}
							}
							messagePath = fixedPath;
							//throw new IllegalArgumentException("Loop detected! " + messagePath);
						}
						
						// To emulate the message path from a query, the inform's message path has to be
						// reversed so it looks like the recipient is first in the path and this peer last.
						Collections.reverse(messagePath);
						msg.setPath(messagePath);
						msg.setReceiver(bestNeighbour);						
						// Since the inform's message path is the same as if a query is sent, the hops
						// should be the size of the message path to be decremented by each peer in the
						// path.
						msg.setHops(messagePath.size() - 2);
						Transport tr = (Transport)node.getProtocol(FastConfig.getTransport(pid));
						tr.send(node, bestNeighbour, msg, pid);	
						//System.out.println(node.getID() + " SENT: " + msg.toString());
						it.remove();
						peer.putParticipantOrder(msg.getID(), participants);
					}else{
						throw new IllegalArgumentException(node.getID() + ": " + bestNeighbourID + " is not up!");
					}
				}else{
					throw new IllegalArgumentException(node.getID() + ": " + bestNeighbourID + " is not a neighbour!");
				}
			}
			iCount++;
			sCount++;
		}
	}
	
	public String playerToString(){
		return player.toString();
	}
	
	public String playerStats(){
		return player.stats();
	}
	
	public String peerStats(){
		return "" + qCount + "," + iCount + "," + eCount + "," + sCount + "," + rCount + "," + hCount;
	}
}
