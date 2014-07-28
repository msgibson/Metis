package uk.ac.abdn.csd.metis.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Roles{
	private static final String prefix = "http://www.abdn.ac.uk/metis.owl#";
	
	private static final List<String> penaltyRoles = 
			Collections.unmodifiableList(Arrays.asList(
					"metis:security"));
	
	private static final List<String> cropRoles = 
			Collections.unmodifiableList(Arrays.asList(
					"metis:farmer",
					"metis:security"));
	
	private static final List<String> goldRoles = 
			Collections.unmodifiableList(Arrays.asList(
					"metis:banker",
					"metis:security"));
	
	private static final List<String> stoneRoles = 
			Collections.unmodifiableList(Arrays.asList(
					"metis:miner",
					"metis:security"));
	
	private static final List<String> treeRoles = 
			Collections.unmodifiableList(Arrays.asList(
					"metis:lumberjack",
					"metis:security"));
	
	private static final List<String> lakeRoles = 
			Collections.unmodifiableList(Arrays.asList(
					"metis:purifier",
					"metis:security"));
	
	private static final List<String> foodRoles = 
			Collections.unmodifiableList(Arrays.asList(
					"metis:farmer",					
					"metis:security",
					"metis:lumberjack",
					"metis:miner",
					"metis:banker",
					"metis:purifier"));
	
	private static final List<String> woodRoles = 
			Collections.unmodifiableList(Arrays.asList(
					"metis:lumberjack",					
					"metis:security", 
					"metis:miner",
					"metis:banker",
					"metis:purifier",
					"metis:farmer"));
	
	private static final List<String> brickRoles = 
			Collections.unmodifiableList(Arrays.asList(
					"metis:miner",					
					"metis:security",
					"metis:banker",
					"metis:purifier",
					"metis:farmer",					
					"metis:lumberjack"));
	
	private static final List<String> moneyRoles = 
			Collections.unmodifiableList(Arrays.asList(
					"metis:banker",					
					"metis:security",
					"metis:purifier",
					"metis:farmer",					
					"metis:lumberjack",
					"metis:miner"));
	
	private static final List<String> waterRoles = 
			Collections.unmodifiableList(Arrays.asList(
					"metis:purifier",					
					"metis:security",				
					"metis:farmer",
					"metis:lumberjack",
					"metis:miner",
					"metis:banker"));
	
	public static final HashMap<String, List<String>> BESTQUERYROLES;
	static{
		BESTQUERYROLES = new HashMap<String, List<String>>();
		BESTQUERYROLES.put("metis:Raw", penaltyRoles);
		
		// Find peers most likely to know location of raw resource		
		BESTQUERYROLES.put("metis:crop", foodRoles);
		BESTQUERYROLES.put("metis:stone", brickRoles);
		BESTQUERYROLES.put("metis:tree", woodRoles);
		BESTQUERYROLES.put("metis:gold", moneyRoles);
		BESTQUERYROLES.put("metis:lake", waterRoles);
		
		// Find owners of produced resource
		BESTQUERYROLES.put("metis:food", foodRoles);
		BESTQUERYROLES.put("metis:brick", brickRoles);
		BESTQUERYROLES.put("metis:wood", woodRoles);
		BESTQUERYROLES.put("metis:money", moneyRoles);
		BESTQUERYROLES.put("metis:water", waterRoles);
		
		// Same as above, but with prefix (hack)
		BESTQUERYROLES.put(prefix + "food", foodRoles);
		BESTQUERYROLES.put(prefix + "brick", brickRoles);
		BESTQUERYROLES.put(prefix + "wood", woodRoles);
		BESTQUERYROLES.put(prefix + "money", moneyRoles);
		BESTQUERYROLES.put(prefix + "water", waterRoles);
	}
	
	public static final HashMap<String, List<String>> BESTINFORMROLES;
	static{
		BESTINFORMROLES = new HashMap<String, List<String>>();
		BESTINFORMROLES.put(prefix + "nil", penaltyRoles);
		BESTINFORMROLES.put(prefix + "penalty", penaltyRoles);
		BESTINFORMROLES.put(prefix + "crop", cropRoles);
		BESTINFORMROLES.put(prefix + "gold", goldRoles);
		BESTINFORMROLES.put(prefix + "stone", stoneRoles);
		BESTINFORMROLES.put(prefix + "tree", treeRoles);
		BESTINFORMROLES.put(prefix + "lake", lakeRoles);
	}
	
	
}