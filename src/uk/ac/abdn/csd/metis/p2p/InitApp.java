package uk.ac.abdn.csd.metis.p2p;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class InitApp implements Control{
	
	/**
	 * The protocol to operate on.
	 */
	private static final String PAR_PROT = "protocol";
	
	/**
	 * The ontology file to work on.
	 */
	private static final String PAR_ONT = "ontology";
	
	/**
	 * The rules file to work on.
	 */
	private static final String PAR_RULES = "rules";
	
	/**
	 * The ontology prefix to create properties and instances.
	 */
	private static final String PAR_PREFIX = "prefix";
	
	/**
	 * The width of the game map.
	 */
	private static final String PAR_WIDTH = "width";
	
	/**
	 * The height of the game map.
	 */
	private static final String PAR_HEIGHT = "height";
	
	/**
	 * The chance of a non-helpful peer
	 */
	private static final String PAR_NOTHELPFUL = "nothelpful";
	
	/**
	 * The chance of a semi-helpful peer
	 */
	private static final String PAR_SEMIHELPFUL = "semihelpful";
	
	/**
	 * The chance of a helpful peer
	 */
	private static final String PAR_HELPFUL = "helpful";
	
	/**
	 * The chance of an impatient peer
	 */
	private static final String PAR_IMPATIENT = "impatient";
	
	/**
	 * The chance of a semi-patient peer
	 */
	private static final String PAR_SEMIPATIENT = "semipatient";
	
	/**
	 * The chance of a patient peer
	 */
	private static final String PAR_PATIENT = "patient";
	
	/**
	 * The chance of a low performance player
	 */
	private static final String PAR_LOWPERFORMANCE = "lowperformance";
	
	/**
	 * The chance of a medium performance player
	 */
	private static final String PAR_MEDPERFORMANCE = "medperformance";
	
	/**
	 * The chance of a high performance player
	 */
	private static final String PAR_HIGHPERFORMANCE = "highperformance";
	
	/**
	 * The chance of a non-coording peer
	 */
	private static final String PAR_NOTCOORD = "notcoord";
	
	/**
	 * The chance of a semi-coording peer
	 */
	private static final String PAR_SEMICOORD = "semicoord";
	
	/**
	 * The chance of a coordinating peer
	 */
	private static final String PAR_COORD = "coord";
	
	/** Protocol identifier, obtained from config property {@link #PAR_PROT}. */
    private final int pid;
    
    /** Ontology filename, obtained from config property {@link #PAR_ONT}. */
    private final String ontology;
    
    /** Rules filename, obtained from config property {@link #PAR_RULES}. */
    private final String rules;
    
    /** Prefix for ontology use, obtained from config property {@link #PAR_PREFIX}. */
    private final String ontPrefix;
    
    /** List of roles available for application. */
    private final String[] roles = {"banker", "lumberjack", "farmer", "miner", "purifier", "security"};
    
    /** Width and height of the game map. */
    private final int width, height;
    
    /** Chances of non-helpful, semi-helpful and helpful peers. */
    private final int notHelpful, semiHelpful, helpful;
    
    /** Chances of impatient, semi-patient and patient peers. */
    private final int impatient, semiPatient, patient;
    
    /** Chances of low, medium and high performance peers. */
    private final int lowPerformance, medPerformance, highPerformance;
    
    /** Chances of non-coording, semi-coording and coordinating peers. */
    private final int notCoord, semiCoord, coord;
    
    /** 
     * Probabilities of raw resources appearing in map.
     * 0 = nil
     * 1 = penalty
     * 2 = crop
     * 3 = gold
     * 4 = lake
     * 5 = stone
     * 6 = tree
     * */
    private int[] rawProbabilities = {0, 1, 2, 3, 4, 5, 6};
    
    /** Map of raw resources used by all peers. */    
    private int[][] grid;
	
    /**
     * Creates a new instance and read parameters from the config file.
     */
    public InitApp(String prefix){
    	pid = Configuration.getPid(prefix + "." + PAR_PROT);
    	ontology = Configuration.getString(prefix + "." + PAR_ONT);
    	rules = Configuration.getString(prefix + "." + PAR_RULES);
    	ontPrefix = Configuration.getString(prefix + "." + PAR_PREFIX);
    	width = Configuration.getInt(prefix + "." + PAR_WIDTH);
    	height = Configuration.getInt(prefix + "." + PAR_HEIGHT);
    	
    	notHelpful = Configuration.getInt(prefix + "." + PAR_NOTHELPFUL);
    	semiHelpful = Configuration.getInt(prefix + "." + PAR_SEMIHELPFUL);
    	helpful = Configuration.getInt(prefix + "." + PAR_HELPFUL);
    	
    	impatient = Configuration.getInt(prefix + "." + PAR_IMPATIENT);
    	semiPatient = Configuration.getInt(prefix + "." + PAR_SEMIPATIENT);
    	patient = Configuration.getInt(prefix + "." + PAR_PATIENT);
    	
    	lowPerformance = Configuration.getInt(prefix + "." + PAR_LOWPERFORMANCE);
    	medPerformance = Configuration.getInt(prefix + "." + PAR_MEDPERFORMANCE);
    	highPerformance = Configuration.getInt(prefix + "." + PAR_HIGHPERFORMANCE);
    	
    	notCoord = Configuration.getInt(prefix + "." + PAR_NOTCOORD);
    	semiCoord = Configuration.getInt(prefix + "." + PAR_SEMICOORD);
    	coord = Configuration.getInt(prefix + "." + PAR_COORD);    	
    	
    	grid = new int[width][height];
    	for(int x = 0; x < width; x++){
    		for(int y = 0; y < height; y++){
    			grid[x][y] = rawProbabilities[CommonState.r.nextInt(7)];
    			//System.out.print(grid[x][y]);
    		}
    		//System.out.println();
    	}
    }
    
    /**
     * Initialise the protocol by reading in the ontology and rules
     * files. Each node will receive a unique copy of the ontology
     * and rules and they will have to infer their neighbours
     * into their own ontology.
     */
	public boolean execute() {	
		int totalHelpful = notHelpful + semiHelpful + helpful;
		int totalPatience = impatient + semiPatient + patient;
		int totalPerformance = lowPerformance + medPerformance + highPerformance;
		int totalCoord = notCoord + semiCoord + coord;
		for(int i = 0; i < Network.size(); i++){			
			Node node = Network.get(i);			
			Peer peer = (Peer)node.getProtocol(pid);			
			peer.createMetis(ontPrefix, ontology, rules, false);
			// Add ourself to the ontology and assign a random role.
			// Only acting as a placeholder for bootstrapping and rules.
			int randomRole = CommonState.r.nextInt(6);	// TODO: Change to i % 6 for smart topology generator
			//int randomRole = i%6;
			int xStart = CommonState.r.nextInt(width);
			int yStart = CommonState.r.nextInt(height);
			String role = roles[randomRole];
			//String role = roles[i];
			peer.addParticipant(""+node.getID(), "self");			
			peer.setRole("self", role);
			peer.setMap(width, height, grid);
			peer.setPosition(xStart, yStart);			
			
			//peer.setTolerance(patience);
			// Calculate the chance of how helpful this peer is
			int helpfulChance = CommonState.r.nextInt(totalHelpful);
			if(helpfulChance < notHelpful){
				peer.setHelpfulness(0);
			}else if(helpfulChance >= notHelpful && helpfulChance < semiHelpful){
				peer.setHelpfulness(1);
			}else{
				peer.setHelpfulness(2);
			}
			
			// Calculate the chance of how patient this peer is
			int patienceChance = CommonState.r.nextInt(totalPatience);
			if(patienceChance < impatient){
				peer.setTolerance(0);
			}else if(patienceChance >= impatient && patienceChance < semiPatient){
				peer.setTolerance(1);
			}else{
				peer.setTolerance(2);
			}
			
			// Calculate the chance of how patient this peer is
			int performanceChance = CommonState.r.nextInt(totalPerformance);
			if(performanceChance < lowPerformance){
				peer.setPerformance(0);
			}else if(performanceChance >= lowPerformance && performanceChance < medPerformance){
				peer.setPerformance(1);
			}else{
				peer.setPerformance(2);
			}
			
			// Calculate the chance of how willing this peer will coordinate
			int coordChance = CommonState.r.nextInt(totalCoord);
			if(coordChance < notCoord){
				peer.setWillCoord(0);
			}else if(coordChance >= notCoord && coordChance < semiCoord){
				peer.setWillCoord(1);
			}else{
				peer.setWillCoord(2);
			}
			
		}
		return false;
	}
}
