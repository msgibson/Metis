package uk.ac.abdn.csd.metis.app;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class Role{
	private String name, prefix;
	private AppResource consumes, produces;
	
	private Resource rRole;
	private Property pName, pConsumes, pProduces;
	
	public Role(String name, String prefix, Model model){
		this.name = name;
		this.prefix = prefix;
		Resource cRole = model.createResource(prefix + "Role");
		pName = model.getProperty(prefix + "name");
		pConsumes = model.getProperty(prefix + "consumes");
		pProduces = model.getProperty(prefix + "produces");
		rRole = model.createResource(prefix + name).addProperty(RDF.type, cRole);
		rRole.addLiteral(pName, name);
	}
	
	public String getName(){
		return name;
	}
	
	public Resource asIndividual(){
		return rRole;
	}
	
	public void setName(String name){
		rRole.removeAll(pName);
		rRole.addLiteral(pName, name);
		this.name = name;
	}	
	
	public AppResource getConsumes(){
		return consumes;
	}
	
	public void setConsumes(AppResource consumes){
		rRole.removeAll(pConsumes);
		rRole.addProperty(pConsumes, consumes.asIndividual());
		this.consumes = consumes;
	}
	
	public AppResource getProduces(){
		return produces;
	}
	
	public void setProduces(AppResource produces){
		rRole.removeAll(pProduces);
		rRole.addProperty(pProduces, produces.asIndividual());
		this.produces = produces;
	}
}
