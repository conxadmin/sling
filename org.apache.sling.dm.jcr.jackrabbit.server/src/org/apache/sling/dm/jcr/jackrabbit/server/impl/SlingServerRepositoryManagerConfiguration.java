package org.apache.sling.dm.jcr.jackrabbit.server.impl;


import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import aQute.bnd.annotation.metatype.Meta.Type;



@OCD(description = "SlingServerRepositoryManager configuration")
public interface SlingServerRepositoryManagerConfiguration {
    public static final String REPOSITORY_JTA_DATASOURCE_PROPERTY_NAME = "repositoryJTADataSourcePropertyName";
	@AD
    public String repositoryJTADataSourcePropertyName();
    
    public static final String REPOSITORY_NON_JTA_DATASOURCE_PROPERTY_NAME = "repositoryNonJTADataSourcePropertyName";
    @AD
    public String repositoryNonJTADataSourcePropertyName();
}
