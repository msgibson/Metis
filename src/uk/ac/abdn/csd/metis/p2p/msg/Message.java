package uk.ac.abdn.csd.metis.p2p.msg;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import peersim.core.Node;

public class Message {
	public enum Type{
		QUERY,
		INFORM,
		EOL
	}
	
	private UUID id = null;
	private Type type = null;
	private ArrayList<Integer> path = null;
	private boolean privateMsg = false;
	private String[] triple = {"", "", ""};
	private Node sender = null, receiver = null, start = null, end = null;
	private int hops = 0;
	
	public Message(Message.Type type, Node sender, Node receiver, String[] triple){
		id = UUID.randomUUID();
		this.type = type;
		this.sender = sender;
		this.receiver = receiver;
		this.triple = triple;
		path = new ArrayList<Integer>();
		start = sender;
		end = receiver;
	}
	
	public Node getStart() {
		return start;
	}

	public void setStart(Node start) {
		this.start = start;
	}

	public Node getEnd() {
		return end;
	}

	public void setEnd(Node end) {
		this.end = end;
	}

	public void setSender(Node sender) {
		this.sender = sender;
	}

	public void setReceiver(Node receiver) {
		this.receiver = receiver;
	}

	public int getHops(){
		return hops;
	}
	
	public void setHops(int hops){
		this.hops = hops;
	}
	
	public UUID getID(){
		return id;
	}
	
	public void setID(UUID id){
		this.id = id;
	}
	
	public void incrementHops(){
		hops++;
	}
	
	public void decrementHops(){
		hops--;
	}

	public ArrayList<Integer> getPath() {
		return path;
	}

	public void setPath(ArrayList<Integer> path) {
		this.path = path;
	}

	public boolean isPrivateMsg() {
		return privateMsg;
	}

	public void setPrivateMsg(boolean privateMsg) {
		this.privateMsg = privateMsg;
	}	
	
	public Type getType(){
		return type;
	}
	
	public Node getSender(){
		return sender;
	}
	
	public Node getReceiver(){
		return receiver;
	}
	
	public String[] getTriple(){
		return triple;
	}
	
	public String getTripleAsString(){
		return triple[0] + " " + triple[1] + " " + triple[2];
	}
	
	public String toString(){
		return "" + type.name() + 
				"(messageID = " + id.toString() +
				", senderID = " + sender.getID() + 
				", receiverID = " + receiver.getID() +
				", startID = " + start.getID() +
				", endID = " + end.getID() +
				", path = " + path.toString() +
				", triple = " + "[" + triple[0] + ", " + triple[1] + ", " + triple[2] + "]" +
				", hop# = " + hops +
				")";
	}
}
