package uk.ac.abdn.csd.metis;

import java.util.EventObject;

public class TripleEvent extends EventObject{
	private static final long serialVersionUID = 1L;
	private String[] m_triple;
	
	public TripleEvent(Object source, String[] triple) {
		super(source);
		m_triple = (String[])triple;
	}
	
	public String[] triple(){
		return m_triple;
	}
}
