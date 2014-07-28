package uk.ac.abdn.csd.metis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.Functor;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra;

public class LHS extends BaseBuiltin{
	/**
	 * A list of all objects which listen for this class' event firings.
	 */
	private List<TripleEventListener> m_tripleEventListeners = new ArrayList<TripleEventListener>();
	
	/**
	 * Return the name of this Jena built-in.
	 */
	public String getName() {		
		return "lhs";
	}
	
	/**
	 * Called during Jena's rule processing.
	 * 
	 * Essentially, this built-in overrides Jena's lazy-evaluation of rules by passing standard built-ins
	 * into this built-in and running each one sequentially. If a built-in returns false, an event is
	 * fired to state which built-in failed. Once all built-ins have been processed, the net result is
	 * returned -- even if one built-in fails, the whole rule fails and, therefore, returns false.
	 */
	public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);
        boolean result = true;
        for(int i = 0; i < length; i++){
        	Node node = getArg(i, args, context);        	
        	String functor = node.getLiteralValue().toString();            
            String functorArgsString[] = functor.split("\\(", 2)[1].replace("r)", "r").split(" ", 4);
            String triple[] = {"", "", ""};
            boolean innerResult = true;
            for(int j = 0; j < functorArgsString.length; j++){  
            	String argString = functorArgsString[j];
            	if(argString.contains("Functor")){
            		String innerFuncName = argString.split("\\(")[0].replace("'", "");
            		String innerFuncArgsString[] = argString.split("\\(")[1].split("\\)")[0].split(" ");
            		Node[] innerFuncArgs = new Node[innerFuncArgsString.length];
                    for(int k = 0; k < innerFuncArgsString.length; k++){  
                    	// BUG: NodeFactoryExtra.parseNode cannot handle http:// - treats it as a prefix.
                    	// Therefore have to wrap XML literal in '<>'
                    	String innerArgString = innerFuncArgsString[k];
                    	innerArgString = innerArgString.replace("^^", "^^<");
                    	innerArgString += ">";            	
                    	innerFuncArgs[k] = NodeFactoryExtra.parseNode(innerArgString);            	
                    }
                    Functor innerFunctor = new Functor(innerFuncName, innerFuncArgs);
                    innerResult = innerFunctor.evalAsBodyClause(context);                    
                    result &= innerResult;
            	}else{	            	
	            	triple[j] = argString.replaceAll("'", "");
            	}            	
            }
            if(!innerResult){
            	fire(triple);
            }
        }
        return result;
    }
	
	/**
	 * Add an object which will listen for events.
	 * 
	 * @param tel An object implementing the TripleEventListener to listen for events.
	 */
	public synchronized void addTripleEventListener(TripleEventListener tel){
		m_tripleEventListeners.add(tel);
	}
	
	/**
	 * Remove an object listening for events.
	 * 
	 * @param tel An object implementing the TripleEventListener which is listening for events.
	 */
	public synchronized void removeTripleEventListener(TripleEventListener tel){
		m_tripleEventListeners.remove(tel);
	}
	
	/**
	 * Pass a triple representing the failed property test to each listening object.
	 * 
	 * @param triple A representation of the failed property test, typically in the form of
	 * what the condition of the resource to be tested has to be in to pass the test, e.g.
	 * if the test was "resource > 9", the triple will be "(resource quantity 10)"
	 */
	private synchronized void fire(String triple[]){
		TripleEvent te = new TripleEvent(this, triple);
		Iterator<TripleEventListener> listeners = m_tripleEventListeners.iterator();
		while(listeners.hasNext()){
			((TripleEventListener)listeners.next()).listen(te);
		}
	}

}
