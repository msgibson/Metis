package uk.ac.abdn.csd.metis.p2p;

import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;

public class InitNeighbours implements Control{

	/**
	 * The protocol to operate on.
	 */
	private static final String PAR_PROT = "protocol";
	
	/** Protocol identifier, obtained from config property {@link #PAR_PROT}. */
    private final int pid;
    
    public InitNeighbours(String prefix){
    	pid = Configuration.getPid(prefix + "." + PAR_PROT);
    }
	
    /**
     * Add each node's list of neighbours into its ontology.
     */
	public boolean execute() {	
		// Create topology of peers
		for(int i = 0; i < Network.size(); i++){
			Node node = Network.get(i);
			Peer thisPeer = (Peer)node.getProtocol(pid);
			Linkable linkable = (Linkable)node.getProtocol(FastConfig.getLinkable(pid));
			if(linkable.degree() > 0){
				for(int j = 0; j < linkable.degree(); j++){
					Node neighbour = linkable.getNeighbor(j);					
					thisPeer.addNeighbour(""+neighbour.getID());
					// Add neighbour's role to ontology as well
					// Ideally this should have its own bootstrapping section, especially
					// if trying to pair types of roles together
					Peer participantN = (Peer)neighbour.getProtocol(pid);
					thisPeer.setRole(""+neighbour.getID(), participantN.getRole());					
				}
			}else{
				System.err.println("No neighbours");
			}			
		}		
		return false;
	}
}
