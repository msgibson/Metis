package uk.ac.abdn.csd.metis.p2p;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
import uk.ac.abdn.csd.metis.app.Player;


public class PlotHappiness implements Control{

	/**
	 * The protocol to operate on.
	 */
	private static final String PAR_PROT = "protocol";	
	
	/** Protocol identifier, obtained from config property {@link #PAR_PROT}. */
    private final int pid;
    
    private static int experimentNumber = 0;
	
	public PlotHappiness(String prefix){
		pid = Configuration.getPid(prefix + "." + PAR_PROT);		
	}
	
	/*
	 * Silly hack time!
	 * 
	 * Although PeerSim says Controls can be called after Protocols during a cycle, there is no documentation stating where.
	 * Therefore, all Controls are called BEFORE a Protocol.
	 * 
	 * This makes stat collecting harder because the next cycle's Control reports the previous cycle's results instead, so
	 * this hack skips the first cycle (where all stats are set to default/0) but performs another cycle after the end
	 * of the simulation to get the updated protocol's stats. This also means shifting the timepoints by 1 to adjust for
	 * the extra cycle.
	 */
	public boolean execute() {	
		if(CommonState.getTime() == 0){
			return false;
		}
		for(int i = 0; i < Network.size(); i++){			
			Node node = Network.get(i);
			Peer peer = (Peer)node.getProtocol(pid);
			Player player = peer.getPlayer();
			try{
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(i + "happiness.csv", true)));
				if(CommonState.getPhase() == CommonState.POST_SIMULATION){
					out.println(CommonState.getTime() + "," + (player.getActualHappiness() / player.getPossibleHappiness()));
				}else{
					out.println(CommonState.getTime()-1 + "," + (player.getActualHappiness() / player.getPossibleHappiness()));
				}
				out.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}		
		
		return false;
	}
}
