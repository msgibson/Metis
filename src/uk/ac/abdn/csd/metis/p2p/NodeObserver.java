package uk.ac.abdn.csd.metis.p2p;

import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.reports.GraphObserver;

public class NodeObserver extends GraphObserver{
	
	public NodeObserver(String prefix) {
		super(prefix);		
	}
	
	public boolean execute() {
		Node node;
		//Linkable linkable = (Linkable)node.getProtocol(pid);
		
		updateGraph();		
		
		for(int i = 0; i < g.size(); i++){
			node = (Node)g.getNode(i);
			System.out.println(node.getID() + ": " + g.getNeighbours((int)node.getID()));
		}		
		
		return false;
	}
}