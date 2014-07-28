/*    
    Copyright (C) 2012 http://software-talk.org/ (developer@software-talk.org)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package uk.ac.abdn.csd.metis.app;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * This class represents an AbstractNode. It has all the appropriate fields as well
 * as getter and setter to be used by the A* algorithm.
 * <p>
 * <p>
 * An <code>AbstractNode</code> has x- and y-coordinates and can be walkable or not.
 * A previous AbstractNode may be set, as well as the
 * <code>fCosts</code>, <code>gCosts</code> and <code>hCosts</code>.
 * <p>
 * <p>
 * <code>fCosts</code>: <code>gCosts</code> + <code>hCosts</code>
 * <p>
 * <code>gCosts</code>: calculated costs from start AbstractNode to this AbstractNode
 * <p>
 * <code>hCosts</code>: estimated costs to get from this AbstractNode to end AbstractNode
 * <p>
 * <p>
 * A subclass has to override the heuristic function
 * <p>
 * <code>sethCosts(AbstractNode endAbstractNode)</code>
 * <p>
 * @see ExampleNode#sethCosts(Tile endNode) example Implementation using manhatten method
 * <p>
 *
 * @version 1.0
 */

public class Tile{

    /** costs to move sideways from one square to another. */
    protected static final int BASICMOVEMENTCOST = 10;
    /** costs to move diagonally from one square to another. */
    protected static final int DIAGONALMOVEMENTCOST = 14;

    protected int xPosition;
    protected int yPosition;
    private boolean walkable;
    
    private String prefix, name;	
	private AppResource hasResource;
	private boolean beenVisited;

    // for pathfinding:

    /** the previous AbstractNode of this one on the currently calculated path. */
    private Tile previous;

    /** weather or not the move from previous to this AbstractNode is diagonally. */
    private boolean diagonally;

    /** optional extra penalty. */
    private int movementPanelty;

    //private int fCosts; // g + h costs

    /** calculated costs from start AbstractNode to this AbstractNode. */
    private int gCosts;

    /** estimated costs to get from this AbstractNode to end AbstractNode. */
    private int hCosts;
    
    private Model model;
    private Resource rTile;
    private Property pXPosition, pYPosition, pName, pHasResource, pBeenVisited;

    /**
     * constructs a walkable AbstractNode with given coordinates.
     *
     * @param xPosition
     * @param yPosition
     */
    public Tile(Model model, String prefix, int xPosition, int yPosition) {
    	this.model = model;
    	this.prefix = prefix;
    	this.name = xPosition + "," + yPosition;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.beenVisited = false;
        this.walkable = true;
        this.movementPanelty = 0;
        
        Resource cTile = model.createResource(prefix + "Tile");
        pXPosition = model.getProperty(prefix + "xPosition");
        pYPosition = model.getProperty(prefix + "yPosition");
        pName = model.getProperty(prefix + "name");
        pHasResource = model.getProperty(prefix + "hasResource");
        pBeenVisited = model.getProperty(prefix + "beenVisited");
        rTile = model.createResource(prefix + name).addProperty(RDF.type, cTile);
        rTile.addLiteral(pName, name);
        rTile.addLiteral(pXPosition, new Integer(xPosition));
        rTile.addLiteral(pYPosition, new Integer(yPosition));  
        rTile.addLiteral(pBeenVisited, false);
    }
    
    public Resource asIndividual(){
    	return rTile;
    }
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		rTile.removeAll(pName);
		rTile.addLiteral(pName, prefix + name);
		this.name = name;
	}	
	
	public int getXPosition(){
		return xPosition;
	}
	
	public void setXPosition(int xPosition){
		rTile.removeAll(pXPosition);
		rTile.addLiteral(pXPosition, new Integer(xPosition));
		this.xPosition = xPosition;
	}	
	
	public int getYPosition(){
		return yPosition;
	}
	
	public void setYPosition(int yPosition){
		rTile.removeAll(pYPosition);
		rTile.addLiteral(pYPosition, new Integer(yPosition));		
		this.yPosition = yPosition;
	}	
	
	public AppResource getHasResource(){
		return hasResource;
	}
	
	public void setHasResource(AppResource hasResource){
		rTile.removeAll(pHasResource);		
		rTile.addProperty(pHasResource, hasResource.asIndividual());
		this.hasResource = hasResource;
	}	
	
	public boolean getBeenVisited(){
		try{
			return rTile.getProperty(pBeenVisited).getBoolean();
		}catch(Exception e){
			System.out.println(name);
			System.out.println(rTile.getProperty(pBeenVisited));
			e.printStackTrace();
			StmtIterator si = model.listStatements();
			Statement st = null;
			while(si.hasNext()){
				st = si.next();
				System.out.println(st);
			}
			return false;
		}
	}
	
	public void setBeenVisited(boolean beenVisited){
		rTile.removeAll(pBeenVisited);
		rTile.addLiteral(pBeenVisited, beenVisited);		
		this.beenVisited = beenVisited;
	}

    /**
     * returns weather or not the move from the <code>previousAbstractNode</code> was
     * diagonally. If it is not diagonal, it is sideways.
     *
     * @return
     */
    public boolean isDiagonaly() {
        return diagonally;
    }

    /**
     * sets weather or not the move from the <code>previousAbstractNode</code> was
     * diagonally. If it is not diagonal, it is sideways.
     *
     * @param isDiagonaly
     */
    public void setIsDiagonaly(boolean isDiagonaly) {
        this.diagonally = isDiagonaly;
    }

    /**
     * sets x and y coordinates.
     *
     * @param x
     * @param y
     */
    public void setCoordinates(int x, int y) {
        this.xPosition = x;
        this.yPosition = y;
    }

    

    /**
     * @return the walkable
     */
    public boolean isWalkable() {
        return walkable;
    }

    /**
     * @param walkable the walkable to set
     */
    public void setWalkable(boolean walkable) {
        this.walkable = walkable;
    }

    /**
     * returns the node set as previous node on the current path.
     *
     * @return the previous
     */
    public Tile getPrevious() {
        return previous;
    }

    /**
     * @param previous the previous to set
     */
    public void setPrevious(Tile previous) {
        this.previous = previous;
    }

    /**
     * sets a general penalty for the movement on this node.
     *
     * @param movementPanelty the movementPanelty to set
     */
    public void setMovementPanelty(int movementPanelty) {
        this.movementPanelty = movementPanelty;
    }

    /**
     * returns <code>gCosts</code> + <code>hCosts</code>.
     * <p>
     *
     *
     * @return the fCosts
     */
    public int getfCosts() {
        return gCosts + hCosts;
    }

    /**
     * returns the calculated costs from start AbstractNode to this AbstractNode.
     *
     * @return the gCosts
     */
    public int getgCosts() {
        return gCosts;
    }

    /**
     * sets gCosts to <code>gCosts</code> plus <code>movementPanelty</code>
     * for this AbstractNode.
     *
     * @param gCosts the gCosts to set
     */
    private void setgCosts(int gCosts) {
        this.gCosts = gCosts + movementPanelty;
    }

    /**
     * sets gCosts to <code>gCosts</code> plus <code>movementPanelty</code>
     * for this AbstractNode given the previous AbstractNode as well as the basic cost
     * from it to this AbstractNode.
     *
     * @param previousAbstractNode
     * @param basicCost
     */
    public void setgCosts(Tile previousAbstractNode, int basicCost) {
        setgCosts(previousAbstractNode.getgCosts() + basicCost);
    }

    /**
     * sets gCosts to <code>gCosts</code> plus <code>movementPanelty</code>
     * for this AbstractNode given the previous AbstractNode.
     * <p>
     * It will assume <code>BASICMOVEMENTCOST</code> as the cost from
     * <code>previousAbstractNode</code> to itself if the movement is not diagonally,
     * otherwise it will assume <code>DIAGONALMOVEMENTCOST</code>.
     * Weather or not it is diagonally is set in the Map class method which
     * finds the adjacent AbstractNodes.
     *
     * @param previousAbstractNode
     */
    public void setgCosts(Tile previousAbstractNode) {
        if (diagonally) {
            setgCosts(previousAbstractNode, DIAGONALMOVEMENTCOST);
        } else {
            setgCosts(previousAbstractNode, BASICMOVEMENTCOST);
        }
    }

    /**
     * calculates - but does not set - g costs.
     * <p>
     * It will assume <code>BASICMOVEMENTCOST</code> as the cost from
     * <code>previousAbstractNode</code> to itself if the movement is not diagonally,
     * otherwise it will assume <code>DIAGONALMOVEMENTCOST</code>.
     * Weather or not it is diagonally is set in the Map class method which
     * finds the adjacent AbstractNodes.
     *
     * @param previousAbstractNode
     * @return gCosts
     */
    public int calculategCosts(Tile previousAbstractNode) {
        if (diagonally) {
            return (previousAbstractNode.getgCosts()
                    + DIAGONALMOVEMENTCOST + movementPanelty);
        } else {
            return (previousAbstractNode.getgCosts()
                    + BASICMOVEMENTCOST + movementPanelty);
        }
    }

    /**
     * calculates - but does not set - g costs, adding a movementPanelty.
     *
     * @param previousAbstractNode
     * @param movementCost costs from previous AbstractNode to this AbstractNode.
     * @return gCosts
     */
    public int calculategCosts(Tile previousAbstractNode, int movementCost) {
        return (previousAbstractNode.getgCosts() + movementCost + movementPanelty);
    }

    /**
     * returns estimated costs to get from this AbstractNode to end AbstractNode.
     *
     * @return the hCosts
     */
    public int gethCosts() {
        return hCosts;
    }

    /**
     * sets hCosts.
     *
     * @param hCosts the hCosts to set
     */
    protected void sethCosts(int hCosts) {
        this.hCosts = hCosts;
    }

    /**
     * calculates hCosts for this AbstractNode to a given end AbstractNode.
     * Uses Manhatten method.
     *
     * @param endAbstractNode
     */
    public void sethCosts(Tile endNode) {
        this.sethCosts((absolute(this.getXPosition() - endNode.getXPosition())
                + absolute(this.getYPosition() - endNode.getYPosition()))
                * BASICMOVEMENTCOST);
    }

    private int absolute(int a) {
        return a > 0 ? a : -a;
    }


    /*
     * @return the movementPanelty
     */
    private int getMovementPanelty() {
        return movementPanelty;
    }

    /**
     * returns a String containing the coordinates, as well as h, f and g
     * costs.
     *
     * @return
     */
    @Override
    public String toString() {
        return "(" + getXPosition() + ", " + getYPosition() + "): h: "
                + gethCosts() + " g: " + getgCosts() + " f: " + getfCosts();
    }

    /**
     * returns weather the coordinates of AbstractNodes are equal.
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tile other = (Tile) obj;
        if (this.xPosition != other.xPosition) {
            return false;
        }
        if (this.yPosition != other.yPosition) {
            return false;
        }
        return true;
    }

    /**
     * returns hash code calculated with coordinates.
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + this.xPosition;
        hash = 17 * hash + this.yPosition;
        return hash;
    }

}
