package uk.ac.abdn.csd.metis.p2p;

import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.reports.GraphObserver;
import uk.ac.abdn.csd.metis.app.Player;

public class PeerObserver extends GraphObserver{
	
	public PeerObserver(String prefix) {
		super(prefix);		
	}
	
	public boolean execute() {		
		updateGraph();
		
		Node node;
		Peer peer;		
		Player player;
		
		for(int i = 0; i < g.size(); i++){
			node = (Node)g.getNode(i);
			peer = (Peer)node.getProtocol(pid);
			player = peer.getPlayer();
			System.out.println(i + ": " + player.toString());			
		}
		
		return false;
	}
}