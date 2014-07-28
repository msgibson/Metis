package uk.ac.abdn.csd.metis.app;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class AppResource{
	private Resource resource;
	private Property pName, pQuantity;
	
	String prefix, name;
	
	public AppResource(String name, String type, String prefix, Model model){	
		this.prefix = prefix;
		this.name = name;
		Resource cType = model.createResource(prefix + type);
		pName = model.getProperty(prefix + "name");
		pQuantity = model.getProperty(prefix + "quantity");
		resource = model.createResource(prefix + name).addProperty(RDF.type, cType).addLiteral(pName, name);		
	}
	
	public String getName(){
		return name;
	}
	
	public Resource asIndividual(){
		return resource;
	}
	
	public int getQuantity(){
		return resource.getProperty(pQuantity).getInt();
	}
	
	public void setQuantity(int quantity){		
		resource.removeAll(pQuantity);
		resource.addLiteral(pQuantity, new Integer(quantity));
	}
}
