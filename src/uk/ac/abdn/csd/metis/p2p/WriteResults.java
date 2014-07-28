package uk.ac.abdn.csd.metis.p2p;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Scheduler;


public class WriteResults implements Control{

	/**
	 * The protocol to operate on.
	 */
	private static final String PAR_PROT = "protocol";	
	
	/** Protocol identifier, obtained from config property {@link #PAR_PROT}. */
    private final int pid;    
	
	public WriteResults(String prefix){
		pid = Configuration.getPid(prefix + "." + PAR_PROT);		
	}
	
	public boolean execute() {		
		if(CommonState.getPhase() == CommonState.POST_SIMULATION){
			Writer writer = null;			
			try{
			    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("results.csv"), "utf-8"));	
			    writer.write("id,role,brick,food,money,water,wood,tolerance,helpfulness,willCoord,actualHappiness,possibleHappiness,satisfaction,totalScore,queries,informs,EOLs,sent,received,highestHop,neighbours\n");
			}catch (Exception e){
				e.printStackTrace();
			}			
			for(int i = 0; i < Network.size(); i++){			
				Node node = Network.get(i);
				Peer peer = (Peer)node.getProtocol(pid);
				Linkable linkable = (Linkable)node.getProtocol(FastConfig.getLinkable(pid));
				peer.writeOntology(i + ".owl");	
				try{
					String neighbours = "[";
					for(int j = 0; j < linkable.degree(); j++){
						neighbours += linkable.getNeighbor(j).getID() + " ";
					}
					neighbours = neighbours.substring(0, neighbours.length() - 1);
					neighbours += "]";
					writer.write(i + "," + peer.playerStats() + "," + peer.peerStats() + "," + neighbours + "\n");					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			try {writer.close();} catch (Exception ex) {}
			System.out.println("END");
		}		
		return false;
	}
}
