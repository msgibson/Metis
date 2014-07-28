package uk.ac.abdn.csd.metis.app;

import java.util.ArrayList;
import java.util.List;

import peersim.core.CommonState;
import uk.ac.abdn.csd.metis.p2p.Peer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class Player {
	private Peer peer;
	
	private Model model;
	
	private Map map;
	private int[][] grid;
	private int width, height;
	
	private Participant self;
	private String name;
	
	private Role role;
	
	private AppResource nil, crop, gold, stone, tree, lake, penalty;
	private AppResource brick, food, money, wood, water;
	//private AppResource happiness, unhappiness;
	
	private double actualHappiness, possibleHappiness;
	
	private List<Tile> path;
	private Tile nextTile = null;
	private Resource destinationTile = null;
	private RDFNode lastBestTile = null;
	private Tile lastBestGather = null;
	private boolean newCoord = true;
	
	private Property pMove, pGather, pCoord, pCoordGather, pCoordAgree, pCoordEnd;
	private Property pExchange, pStartExchange, pPositionedAt, pXPosition, pYPosition, pImpatient;
	private Property pQuantity;
	
	private String prefix = "http://www.abdn.ac.uk/metis.owl#";	
	
	private int impatience = 0, tolerance = 0, performance = 0;
	private int newX = -1, newY = -1;
	private int coordID = -1;
	private int helpfulness = 0, willCoord = 0;
	private int oldFood = 0;
	private int oldMoney = 0;
	private int oldWater = 0;
	private int oldWood = 0;
	private int oldBrick = 0;
	
	// Increase happiness scores
	public final double MOVE_WEIGHT = 1.0;
	public final double GATHER_WEIGHT = 3.0;
	public final double COORD_GATHER_WEIGHT = 7.0;
	public final double EXCHANGE_WEIGHT = 5.0;
	public final double INFO = 1.0;
	
	// Increase unhappiness scores
	// TODO: Not needed anymore!
	//public final double CANCEL_COORD_WEIGHT = 1.0;
	//public final double CANCEL_EXCHANGE_WEIGHT = 1.0;
	//public final double IMPATIENT_MOVE_WEIGHT = 0.5;
	//public final double NO_INFO = 0.5;
	
	// Tolerance (patience)
	public final int IMPATIENT = 5;
	public final int SEMI_PATIENT = 10;
	public final int PATIENT = 20;
	
	private enum State{
		IDLE,
		MOVING,
		WAIT,		
		GATHERING,
		SENT_COORD,
		COORDING,
		GATHERING_COORD	
	}
	private State currentState = State.IDLE;
	private boolean exchanging = false; // The only state that can happen at any time, but can lead to transaction problems.
	//private boolean forcedMove = false, plannedPath = false, coordSent = false, coordReceived = false, wait = false;
	//private boolean exchangeSent = false, exchanging = true, gathering = false, cancelCoord = false;	
	//private Resource destinationC;
	
	public Player(String name, Peer peer, Model model){
		this.name = name;
		this.peer = peer;
		this.model = model;		
		
		path = new ArrayList<Tile>();
		
		// Player
		self = new Participant("self", prefix, model);		
		
		// Raw resources
		nil = new AppResource("nil", "Raw", prefix, model);		
		nil.setQuantity(-1);	
		
		crop = new AppResource("crop", "Raw", prefix, model);		
		crop.setQuantity(-1);	
		
		gold = new AppResource("gold", "Raw", prefix, model);		
		gold.setQuantity(-1);
		
		stone = new AppResource("stone", "Raw", prefix, model);		
		stone.setQuantity(-1);
		
		tree = new AppResource("tree", "Raw", prefix, model);		
		tree.setQuantity(-1);
		
		lake = new AppResource("lake", "Raw", prefix, model);		
		lake.setQuantity(-1);
		
		penalty = new AppResource("penalty", "Raw", prefix, model);		
		penalty.setQuantity(-1);
		
		// Processed resources
		brick = new AppResource("brick", "Processed", prefix, model);		
		brick.setQuantity(0);		
				
		food = new AppResource("food", "Processed", prefix, model);		
		food.setQuantity(0);
		
		money = new AppResource("money", "Processed", prefix, model);		
		money.setQuantity(0);
		
		wood = new AppResource("wood", "Processed", prefix, model);		
		wood.setQuantity(0);
		
		water = new AppResource("water", "Processed", prefix, model);		
		water.setQuantity(0);
		
		// Commodity resources
		//bread = new AppResource("bread", "Commodity", prefix, model);		
		//bread.setQuantity(0);
		
		// Metrics
		//happiness = new AppResource("happiness", "Raw", prefix, model);
		//happiness.setQuantity(0);
		actualHappiness = 0;
		
		//unhappiness = new AppResource("unhappiness", "Raw", prefix, model);
		//unhappiness.setQuantity(0);
		possibleHappiness = 0;
		
		// Properties
		pPositionedAt = model.getProperty(prefix + "positionedAt");
		pXPosition = model.getProperty(prefix + "xPosition");
		pYPosition = model.getProperty(prefix + "yPosition");
		pQuantity = model.createProperty(prefix + "quantity");		
		
		// Commands
		pMove = model.createProperty(prefix + "move");
		pGather = model.createProperty(prefix + "gather");
		pCoord = model.createProperty(prefix + "coord");
		pCoordAgree = model.createProperty(prefix + "coordAgree");
		pCoordGather = model.createProperty(prefix + "coordGather");
		pCoordEnd = model.createProperty(prefix + "coordEnd");
		pExchange = model.createProperty(prefix + "exchange");
		pStartExchange = model.createProperty(prefix + "startExchange");
		pImpatient = model.createProperty(prefix + "impatient");
	}
	
	public String getName(){
		return name;
	}
	
	public void setRole(String roleName){
		role = new Role(roleName, prefix, model);		
		self.setHasRole(role);
	}	
	
	public void setPostion(int x, int y){		
		Tile tile = map.getNode(x, y);
		tile.setBeenVisited(true);
		self.setPositionedAt(tile);		
	}
	
	public void setMap(int width, int height, int[][] grid){
		map = new Map(model, prefix, width, height, new TileFactory());
		this.grid = grid;
		this.width = width;
		this.height = height;
		
		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				int resourceType = grid[x][y];
				switch (resourceType){
					case 0:						
						map.setResource(x, y, nil);					
						break;
					case 1:
						map.setResource(x, y, penalty);					
						break;
					case 2:
						map.setResource(x, y, crop);					
						break;
					case 3:
						map.setResource(x, y, gold);					
						break;
					case 4:
						map.setResource(x, y, lake);					
						break;
					case 5:
						map.setResource(x, y, stone);					
						break;
					case 6:
						map.setResource(x, y, tree);					
						break;
				}
			}
		}		
	}
	
	public void setTolerance(int tolerance){
		if(tolerance == 0){
			this.tolerance = IMPATIENT;
		}else if(tolerance == 1){
			this.tolerance = SEMI_PATIENT;
		}else{
			this.tolerance = PATIENT;
		}
	}
	
	public int getTolerance(){
		return tolerance;
	}
	
	public void setPerformance(int performance){
		this.performance = performance;
	}
	
	public void setHelpfulness(int helpfulness){
		this.helpfulness = helpfulness;
	}
	
	public void setWillCoord(int willCoord){
		this.willCoord = willCoord;
	}
	
	public boolean isHelpful(){
		if(helpfulness == 0){
			// 50% chance of helping (0-99)
			if(CommonState.r.nextInt(100) > 49){
				return false;
			}else{
				return true;
			}			
		}else if(helpfulness == 1){
			// 75% chance of helping (0-99)
			if(CommonState.r.nextInt(100) > 74){
				return false;
			}else{
				return true;
			}	
		}else{
			// 100% chance of helping (0-99)			
			return true;			
		}
	}
	
	private boolean willCoord(){
		if(willCoord == 0){
			// 50% chance of helping (0-99)
			if(CommonState.r.nextInt(100) > 49){
				return false;
			}else{
				return true;
			}
		}else if(willCoord == 1){
			// 75% chance helping
			if(CommonState.r.nextInt(100) > 74){
				return false;
			}else{
				return true;
			}
		}else{
			// 100% chance of helping (0-99)
			return true;			
		}
	}
	
	private boolean willPerform(){
		if(performance == 0){
			// 50% chance of helping (0-99)
			if(CommonState.r.nextInt(100) > 49){
				return false;
			}else{
				return true;
			}
		}else if(performance == 1){
			// 75% chance helping
			if(CommonState.r.nextInt(100) > 74){
				return false;
			}else{
				return true;
			}
		}else{
			// 100% chance of helping (0-99)			
			return true;			
		}
	}
	
	public void increaseActualHappiness(double value){
		//int happy = happiness.getQuantity();
		//happiness.setQuantity(happy + value);
		actualHappiness += value;
	}
	
	public void increasePossibleHappiness(double value){
		//int unhappy = unhappiness.getQuantity();
		//unhappiness.setQuantity(unhappy + value);
		possibleHappiness += value;
	}
	
	public double getActualHappiness(){
		//return happiness.getQuantity();
		return actualHappiness;
	}
	
	public double getPossibleHappiness(){
		//return unhappiness.getQuantity();
		return possibleHappiness;
	}
	
	public double getTotalScore(){
		return (brick.getQuantity() + food.getQuantity() + money.getQuantity() + water.getQuantity() + wood.getQuantity());
	}
	
	public void run(){
		if(!willPerform()){
			impatience++;
			return;
		}
		RDFNode currentPosition = self.getPositionedAt();
		int oldX = currentPosition.asResource().getProperty(pXPosition).getInt();
		int oldY = currentPosition.asResource().getProperty(pYPosition).getInt();		
		String resourceName = map.getResource(oldX, oldY).getName();
		
		switch(currentState){
		case IDLE: case GATHERING:
			if(role.getName().equals("farmer")){
				decide(resourceName, "crop", currentPosition, food, money, water, wood, brick);
			}else if(role.getName().equals("banker")){
				decide(resourceName, "gold", currentPosition, money, water, wood, brick, food);
			}else if(role.getName().equals("purifier")){
				decide(resourceName, "lake", currentPosition, water, wood, brick, food, money);
			}else if(role.getName().equals("miner")){
				decide(resourceName, "stone", currentPosition, brick, food, money, water, wood);
			}else if(role.getName().equals("lumberjack")){
				decide(resourceName, "tree", currentPosition, wood, brick, food, money, water);
			}else{
				move();
			}
			break;
		case MOVING:
			// We already have a move command in place, so wait for this to kick in.
			// After the forced move, revert back to an idle state.
			currentState = State.IDLE;
			break;		
		case SENT_COORD:
			moveToCoordPosition();
			break;
		case COORDING:
			moveToCoordPosition();
			break;
		case GATHERING_COORD:
			break;		
		case WAIT:
			// Do nothing - this is for a coord partner to wait at given position.
			break;
		}
	}
	
	private void moveToCoordPosition(){
		RDFNode currentPosition = self.getPositionedAt();
		int oldX = currentPosition.asResource().getProperty(pXPosition).getInt();
		int oldY = currentPosition.asResource().getProperty(pYPosition).getInt();		
		String resourceName = map.getResource(oldX, oldY).getName();
		
		switch(currentState){
		case SENT_COORD:
			if(destinationTile == null){
				if(impatience < tolerance){
					impatience++;
				}else{
					impatience = 0;
					currentState = State.IDLE;
					increasePossibleHappiness(COORD_GATHER_WEIGHT);
				}
			}else{
				if(currentPosition.toString().equals(destinationTile.toString())){
					if(newCoord){
						impatience = 0;
						newCoord = false;
					}
					if(resourceName.equals("crop")){
						if(food.getQuantity() < 100){						
							if(impatience > tolerance){
								model.add(self.asIndividual(), pCoordEnd, food.asIndividual());
								currentState = State.IDLE;
								increasePossibleHappiness(COORD_GATHER_WEIGHT);
								newCoord = true;
								impatience = 0;
							}else{
								model.add(self.asIndividual(), pCoordGather, food.asIndividual());
								increaseActualHappiness(COORD_GATHER_WEIGHT);
								increasePossibleHappiness(COORD_GATHER_WEIGHT);
							}
							if(food.getQuantity() == oldFood){
								impatience++;
							}
						}else{
							model.add(self.asIndividual(), pCoordEnd, food.asIndividual());							
							impatience = 0;
							currentState = State.IDLE;
							newCoord = true;
						}
						return;
					}else if(resourceName.equals("gold")){
						if(money.getQuantity() < 100){						
							if(impatience > tolerance){
								model.add(self.asIndividual(), pCoordEnd, money.asIndividual());
								currentState = State.IDLE;
								increasePossibleHappiness(COORD_GATHER_WEIGHT);
								newCoord = true;
								impatience = 0;
							}else{
								model.add(self.asIndividual(), pCoordGather, money.asIndividual());
								increaseActualHappiness(COORD_GATHER_WEIGHT);
								increasePossibleHappiness(COORD_GATHER_WEIGHT);
							}
							if(money.getQuantity() == oldMoney){
								impatience++;
							}
						}else{
							model.add(self.asIndividual(), pCoordEnd, money.asIndividual());							
							impatience = 0;
							currentState = State.IDLE;
							newCoord = true;
						}
						return;
					}else if(resourceName.equals("lake")){
						if(water.getQuantity() < 100){						
							if(impatience > tolerance){
								model.add(self.asIndividual(), pCoordEnd, water.asIndividual());
								currentState = State.IDLE;
								increasePossibleHappiness(COORD_GATHER_WEIGHT);
								newCoord = true;
								impatience = 0;
							}else{
								model.add(self.asIndividual(), pCoordGather, water.asIndividual());
								increaseActualHappiness(COORD_GATHER_WEIGHT);
								increasePossibleHappiness(COORD_GATHER_WEIGHT);
							}
							if(water.getQuantity() == oldWater){
								impatience++;
							}
						}else{
							model.add(self.asIndividual(), pCoordEnd, water.asIndividual());							
							impatience = 0;
							currentState = State.IDLE;
							newCoord = true;
						}
						return;
					}else if(resourceName.equals("tree")){
						if(wood.getQuantity() < 100){						
							if(impatience > tolerance){
								model.add(self.asIndividual(), pCoordEnd, wood.asIndividual());
								currentState = State.IDLE;
								increasePossibleHappiness(COORD_GATHER_WEIGHT);
								newCoord = true;
								impatience = 0;
							}else{
								model.add(self.asIndividual(), pCoordGather, wood.asIndividual());
								increaseActualHappiness(COORD_GATHER_WEIGHT);
								increasePossibleHappiness(COORD_GATHER_WEIGHT);
							}
							if(wood.getQuantity() == oldWood){
								impatience++;
							}
						}else{
							model.add(self.asIndividual(), pCoordEnd, wood.asIndividual());							
							impatience = 0;
							currentState = State.IDLE;
							newCoord = true;
						}
						return;
					}else if(resourceName.equals("stone")){
						if(brick.getQuantity() < 100){						
							if(impatience > tolerance){
								model.add(self.asIndividual(), pCoordEnd, brick.asIndividual());
								currentState = State.IDLE;
								increasePossibleHappiness(COORD_GATHER_WEIGHT);
								newCoord = true;
								impatience = 0;
							}else{
								model.add(self.asIndividual(), pCoordGather, brick.asIndividual());
								increaseActualHappiness(COORD_GATHER_WEIGHT);
								increasePossibleHappiness(COORD_GATHER_WEIGHT);
							}
							if(brick.getQuantity() == oldBrick){
								impatience++;
							}
						}else{
							model.add(self.asIndividual(), pCoordEnd, brick.asIndividual());							
							impatience = 0;
							currentState = State.IDLE;
							newCoord = true;
						}
						return;
					}
				}else{
					move();
				}
			}
			break;
		case COORDING:
			if(!currentPosition.toString().equals(destinationTile.toString())){
				move();
			}else{
				if(currentState != State.WAIT){
					String[] triple = {prefix + "participant" + name, pPositionedAt.toString(), currentPosition.toString()};
					peer.addInform(coordID, triple);
				}
				currentState = State.WAIT;				
			}
			break;
		default:
			break;
		}
	}
	
	private void move(){
		RDFNode currentPosition = self.getPositionedAt();
		int oldX = currentPosition.asResource().getProperty(pXPosition).getInt();
		int oldY = currentPosition.asResource().getProperty(pYPosition).getInt();
		
		if(path.isEmpty() && (currentState != State.SENT_COORD && currentState != State.COORDING)){
			newX = CommonState.r.nextInt(width);
			newY = CommonState.r.nextInt(height);
			if(oldX == newX && oldY == newY){
				newX = CommonState.r.nextInt(width);
				newY = CommonState.r.nextInt(height);
			}
			path = map.findPath(oldX, oldY, newX, newY);
			nextTile = path.remove(0);
		}
		
		//if(currentPosition != nextTile){
		if(nextTile != null && ((oldX != nextTile.getXPosition()) || (oldY != nextTile.getYPosition()))){
			if(nextTile == null && (currentState == State.SENT_COORD || currentState == State.COORDING)){
				return;
			}
			if(impatience < tolerance){
				model.add(self.asIndividual(), pMove, nextTile.asIndividual());
				model.addLiteral(self.asIndividual(), pImpatient, false);
				if(nextTile.getBeenVisited()){
					increaseActualHappiness(MOVE_WEIGHT);
					increasePossibleHappiness(MOVE_WEIGHT);
				}
			}else{
				model.add(self.asIndividual(), pMove, nextTile.asIndividual());
				model.addLiteral(self.asIndividual(), pImpatient, true);
				impatience = 0;
				increasePossibleHappiness(MOVE_WEIGHT);
				return;
			}
			impatience++;
			return;
		}else{
			if(!path.isEmpty()){
				nextTile = path.remove(0);
				impatience = 0;
				return;
			}
		}
	}
	
	private void decide(String resourceName, String rawName, RDFNode currentPosition,
			AppResource primary, AppResource res2, AppResource res3, AppResource res4, AppResource res5){
		boolean choice = CommonState.r.nextBoolean();
		int resourceChoice = CommonState.r.nextInt(4);
		oldFood = food.getQuantity();
		oldMoney = money.getQuantity();
		oldWater = water.getQuantity();
		oldWood = wood.getQuantity();
		oldBrick = brick.getQuantity();
		if(primary.getQuantity() >= 100){
			currentState = State.IDLE;
		}
		if(primary.getQuantity() < 100){
			if(resourceName.equals(rawName)){
				model.add(self.asIndividual(), pGather, primary.asIndividual());
				// Override the force move if we have found something useful on the current tile
				if(currentState == State.MOVING){					
					model.removeAll(null, pMove, null);
					model.removeAll(null, pImpatient, null);
				}
				currentState = State.GATHERING;
				lastBestTile = currentPosition;
				increaseActualHappiness(GATHER_WEIGHT);
				increasePossibleHappiness(GATHER_WEIGHT);
				return;
			}
			/*
			else if(lastBestTile != null && !path.contains(lastBestGather)){
				int oldX = currentPosition.asResource().getProperty(pXPosition).getInt();
				int oldY = currentPosition.asResource().getProperty(pYPosition).getInt();
				int newX = lastBestTile.asResource().getProperty(pXPosition).getInt();
				int newY = lastBestTile.asResource().getProperty(pYPosition).getInt();
				lastBestGather = map.getNode(newX, newY);
				path = map.findPath(oldX, oldY, newX, newY);				
			}
			*/
		}else if(res2.getQuantity() < 100 && resourceChoice == 0){
			if(choice && currentState != State.SENT_COORD){
				model.add(self.asIndividual(), pCoord, res2.asIndividual());				
				currentState = State.SENT_COORD;				
			}else{
				model.add(primary.asIndividual(), pStartExchange, res2.asIndividual());				
				// Remove 10 from primary resource -- treat this as "sending 10 primary
				// resources in exchange message"				
				int quantity = primary.asIndividual().getProperty(pQuantity).getInt();				
				primary.asIndividual().removeAll(pQuantity);
				primary.asIndividual().addLiteral(pQuantity, new Integer(quantity - 10));				
			}
		}else if(res3.getQuantity() < 100 && resourceChoice == 1){
			if(choice && currentState != State.SENT_COORD){
				model.add(self.asIndividual(), pCoord, res3.asIndividual());				
				currentState = State.SENT_COORD;				
			}else{
				model.add(primary.asIndividual(), pStartExchange, res3.asIndividual());
				// Remove 10 from primary resource -- treat this as "sending 10 primary
				// resources in exchange message"				
				int quantity = primary.asIndividual().getProperty(pQuantity).getInt();				
				primary.asIndividual().removeAll(pQuantity);
				primary.asIndividual().addLiteral(pQuantity, new Integer(quantity - 10));				
			}
		}else if(res4.getQuantity() < 100 && resourceChoice == 2){
			if(choice && currentState != State.SENT_COORD){
				model.add(self.asIndividual(), pCoord, res4.asIndividual());				
				currentState = State.SENT_COORD;				
			}else{
				model.add(primary.asIndividual(), pStartExchange, res4.asIndividual());				
				// Remove 10 from primary resource -- treat this as "sending 10 primary
				// resources in exchange message"				
				int quantity = primary.asIndividual().getProperty(pQuantity).getInt();				
				primary.asIndividual().removeAll(pQuantity);
				primary.asIndividual().addLiteral(pQuantity, new Integer(quantity - 10));				
			}
		}else if(res5.getQuantity() < 100 && resourceChoice == 3){
			if(choice && currentState != State.SENT_COORD){
				model.add(self.asIndividual(), pCoord, res5.asIndividual());				
				currentState = State.SENT_COORD;				
			}else{
				model.add(primary.asIndividual(), pStartExchange, res5.asIndividual());
				// Remove 10 from primary resource -- treat this as "sending 10 primary
				// resources in exchange message"				
				int quantity = primary.asIndividual().getProperty(pQuantity).getInt();				
				primary.asIndividual().removeAll(pQuantity);
				primary.asIndividual().addLiteral(pQuantity, new Integer(quantity - 10));				
			}
		}
		move();
	}
	
	/**
	 * Player received an EOL about a tile, meaning no-one has visited the tile.
	 * Therefore, force a move there.
	 * @param triple Triple containing tile position for player to move to.
	 */
	public void forceMove(String[] triple){		
		if(currentState == State.IDLE 
				&& nextTile != null
				&& nextTile.asIndividual().toString().equals(triple[0])){
			model.add(self.asIndividual(), pMove, nextTile.asIndividual());
			model.addLiteral(self.asIndividual(), pImpatient, true);
			impatience = 0;
			if(!path.isEmpty()){
				nextTile = path.remove(0);
			}
			currentState = State.MOVING;
			// Don't call the standard move function next time.
			//forcedMove = true;
		}
	}
	
	public void cancelCoord(){
		if(currentState == State.SENT_COORD){
			currentState = State.IDLE;
		}
	}
	
	/**
	 * Attempt to co-ordinate with another player if they need a resource this player can gather.
	 * Randomise the chance of offering help if a suitable match has been found, otherwise
	 * pass on the query to some other player
	 * @param triple Co-ordinate command with player and resource in need.
	 */
	public String coord(int nodeID, String[] triple){		
		boolean offer = willCoord();
		String reply = "false";
		
		// If I will not coordinate, already coordinating or I don't know where a good tile is, don't help
		if(!offer || currentState == State.COORDING || lastBestTile == null){
			return reply;
		}
		
		RDFNode currentTile = self.getPositionedAt();
		int oldX = currentTile.asResource().getProperty(pXPosition).getInt();
		int oldY = currentTile.asResource().getProperty(pYPosition).getInt();
		int newX = lastBestTile.asResource().getProperty(pXPosition).getInt();
		int newY = lastBestTile.asResource().getProperty(pYPosition).getInt();
		coordID = nodeID;
		String requirement = triple[2];
		if(role.getName().equals("farmer") && requirement.contains("food")){
			reply = prefix + newX + "," + newY;
			path = map.findPath(oldX, oldY, newX, newY);
			nextTile = path.remove(0);
			impatience = 0;
			destinationTile = model.createResource(reply);
			currentState = State.COORDING;
		}else if(role.getName().equals("banker") && requirement.contains("money")){
			reply = prefix + newX + "," + newY;
			path = map.findPath(oldX, oldY, newX, newY);
			impatience = 0;
			destinationTile = model.createResource(reply);
			currentState = State.COORDING;
		}else if(role.getName().equals("purifier") && requirement.contains("water")){
			reply = prefix + newX + "," + newY;
			path = map.findPath(oldX, oldY, newX, newY);
			impatience = 0;
			destinationTile = model.createResource(reply);
			currentState = State.COORDING;
		}else if(role.getName().equals("miner") && requirement.contains("brick")){
			reply = prefix + newX + "," + newY;
			path = map.findPath(oldX, oldY, newX, newY);
			impatience = 0;
			destinationTile = model.createResource(reply);
			currentState = State.COORDING;
		}else if(role.getName().equals("lumberjack") && requirement.contains("wood")){
			reply = prefix + newX + "," + newY;
			path = map.findPath(oldX, oldY, newX, newY);
			impatience = 0;
			destinationTile = model.createResource(reply);
			currentState = State.COORDING;
		}
		return reply;
	}
	
	public String exchange(String[] triple){
		String reply = "false";
		// Limit to one exchange at a time, otherwise inventory mishaps will happen (blame the rules...)
		if(exchanging == true){
			return reply;
		}
		Resource selling = model.createResource(triple[0]);	// What other participant is giving us for exchange
		Resource buying = model.createResource(triple[2]);	// What other participant wants		
		//int sQuantity = selling.getProperty(pQuantity).getInt();
		int bQuantity = buying.getProperty(pQuantity).getInt();
		boolean choice = isHelpful();
		if(choice && bQuantity > 10){
			model.add(selling, pExchange, buying);
			reply = "true";
			exchanging = true;
		}
		return reply;
	}
	
	public void stopExchanging(){
		exchanging = false;
	}
	
	public void exchangeSuccessful(String[] triple){
		Resource selling = model.createResource(triple[0]);
		Resource buying = model.createResource(triple[2]);
		// Use "CompleteExchange" rule, but swap around buying and selling to make
		// sure correct resources are updated
		model.add(buying, pExchange, selling);
		stopExchanging();		
	}
	
	public void exchangeFailed(String[] triple){
		// Recover the 10 "sent primary resources" from exchange message.		
		Resource primary = model.createResource(triple[0]);
		int quantity = primary.getProperty(pQuantity).getInt();				
		primary.removeAll(pQuantity);
		primary.addLiteral(pQuantity, new Integer(quantity + 10));
		stopExchanging();
	}
	
	public void stopWaiting(){
		currentState = State.IDLE;
	}
	
	/**
	 * Move to a tile with the resource needed. This destination is known by the co-ord player.
	 * First part of a co-ord is getting to the destination tile. The second part is waiting
	 * for the co-ord player to get to the destination so that we can extract the resource
	 * without needing a matching role (the co-ord player "offers" their role for this).
	 * @param triple
	 */
	public void planCoord(String[] triple){
		if(currentState != State.SENT_COORD){
			currentState = State.IDLE;
			int id = Integer.parseInt(triple[0].toString().replaceAll("\\D+",""));
			String[] inform = {triple[0], pCoordEnd.toString(), ""+true};
			peer.addInform(id, inform);
		}else{			
			//Resource coordParticipant = model.createResource(triple[0]);
			//model.createStatement(self.asIndividual(), pCoordAgree, coordParticipant);
			RDFNode oldTile = self.getPositionedAt();
			int oldX = oldTile.asResource().getProperty(pXPosition).getInt();
			int oldY = oldTile.asResource().getProperty(pYPosition).getInt();
			destinationTile = model.createResource(triple[2]);
			int x = destinationTile.getProperty(pXPosition).getInt();
			int y = destinationTile.getProperty(pYPosition).getInt();
			path = map.findPath(oldX, oldY, x, y);
			nextTile = path.remove(0);
			impatience = 0;			
		}
	}
	
	public void planPath(String[] triple){	
		if(currentState != State.IDLE){
			return;
		}		
		RDFNode oldTile = self.getPositionedAt();
		int oldX = oldTile.asResource().getProperty(pXPosition).getInt();
		int oldY = oldTile.asResource().getProperty(pYPosition).getInt();
		Resource destination = model.createResource(triple[0]);
		int x = destination.getProperty(pXPosition).getInt();
		int y = destination.getProperty(pYPosition).getInt();
		switch(role.getName()){
			case "farmer":
				if(triple[2].equals("metis:crop")){
					path = map.findPath(oldX, oldY, x, y);
					nextTile = path.remove(0);
					impatience = 0;					
				}
				break;
			case "banker":
				if(triple[2].equals("metis:gold")){
					path = map.findPath(oldX, oldY, newX, newY);
					nextTile = path.remove(0);
					impatience = 0;					
				}
				break;
			case "purifier":
				if(triple[2].equals("metis:lake")){
					path = map.findPath(oldX, oldY, newX, newY);
					nextTile = path.remove(0);
					impatience = 0;					
				}
				break;
			case "miner":
				if(triple[2].equals("metis:stone")){
					path = map.findPath(oldX, oldY, newX, newY);
					nextTile = path.remove(0);
					impatience = 0;					
				}
				break;
			case "lumberjack":
				if(triple[2].equals("metis:tree")){
					path = map.findPath(oldX, oldY, newX, newY);
					nextTile = path.remove(0);
					impatience = 0;					
				}
				break;
		}
	}
	
	public String getRole(){
		return role.getName();
	}
	
	public int getXPosition(){
		return self.getPositionedAt().asResource().getProperty(pXPosition).getInt();
	}
	
	public int getYPosition(){
		return self.getPositionedAt().asResource().getProperty(pYPosition).getInt();
	}
	
	public String toString(){
		return role.getName() + ", (" + getXPosition() + ", " + getYPosition() + "), " +				
				"brick: " + brick.getQuantity() + ", " +
				"food: " + food.getQuantity() + ", " +
				"money: " + money.getQuantity() + ", " +
				"wood: " + wood.getQuantity() + ", " +
				"water: " + water.getQuantity() + ", " +
				"tolerance: " + tolerance + ", " +
				"helpfulness: " + helpfulness + ", " +
				"co-ord chance: " + willCoord + ", " +
				//"happiness: " + happiness.getQuantity() + ", " +
				"actualHappiness: " + actualHappiness + ", " +
				//"unhappiness: " + unhappiness.getQuantity() + ", " +
				"possibleHappiness: " + possibleHappiness + ", " +
				//"total happiness: " + (happiness.getQuantity() - unhappiness.getQuantity());
				"satisfaction: " + (actualHappiness / possibleHappiness) + "," +
				"totalResources: " + (brick.getQuantity() + food.getQuantity() + money.getQuantity() + water.getQuantity() + wood.getQuantity());
				
	}
	
	public String stats(){
		return role.getName() + "," +				
				brick.getQuantity() + "," +
				food.getQuantity() + "," +
				money.getQuantity() + "," +
				water.getQuantity() + "," +
				wood.getQuantity() + "," +
				tolerance + "," +
				helpfulness + "," +
				willCoord + "," +
				//happiness.getQuantity() + ", " +
				actualHappiness + "," +
				//unhappiness.getQuantity() + ", " +
				possibleHappiness + "," +
				//(happiness.getQuantity() - unhappiness.getQuantity());
				(actualHappiness / possibleHappiness) + "," +
				(brick.getQuantity() + food.getQuantity() + money.getQuantity() + water.getQuantity() + wood.getQuantity());
				
	}
}
