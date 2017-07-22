package com.vidolima.doco;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.vidolima.doco.annotation.DocumentId;

@Entity
public class ObjectifyEntity {
	
	@Id
	@DocumentId
	Long id;
	
	String property1;
	
	public ObjectifyEntity(String property1){
		this.property1 = property1;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProperty1() {
		return property1;
	}

	public void setProperty1(String property1) {
		this.property1 = property1;
	}
	
	
}
