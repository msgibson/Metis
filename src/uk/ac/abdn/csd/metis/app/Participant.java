package uk.ac.abdn.csd.metis.app;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class Participant{
	private String name, prefix;
	private Role hasRole;
	private Tile positionedAt;
	
	private Resource rParticipant;
	private Property pName, pHasRole, pPositionedAt;
	
	public Participant(String name, String prefix, Model model){
		this.name = name;
		this.prefix = prefix;
		rParticipant = model.createResource(prefix + name).addProperty(RDF.type, "Participant");
		pName = model.getProperty(prefix + "name");
		pHasRole = model.getProperty(prefix + "hasRole");
		pPositionedAt = model.getProperty(prefix + "positionedAt");
		rParticipant.addLiteral(pName, name);		
	}
	
	public String getName(){
		return name;
	}
	
	public Resource asIndividual(){
		return rParticipant;
	}
	
	public void setName(String name){
		rParticipant.removeAll(pName);
		rParticipant.addLiteral(pName, name);
		this.name = name;
	}	
	
	public Role getHasRole(){
		return hasRole;
	}
	
	public void setHasRole(Role hasRole){
		rParticipant.removeAll(pHasRole);
		rParticipant.addProperty(pHasRole, hasRole.asIndividual());		
		this.hasRole = hasRole;
	}	
	
	public RDFNode getPositionedAt(){
		return rParticipant.getProperty(pPositionedAt).getObject();
	}
	
	public void setPositionedAt(Tile positionedAt){
		rParticipant.removeAll(pPositionedAt);
		rParticipant.addProperty(pPositionedAt, positionedAt.asIndividual());
		this.positionedAt = positionedAt;
	}
}
