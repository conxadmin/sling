package org.apache.sling.dm.jcr.jackrabbit.server.impl;


import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import aQute.bnd.annotation.metatype.Meta.Type;



@OCD(description = "SlingServerRepositoryManager configuration")
public interface SlingServerRepositoryManagerConfiguration {
	public static final String REPOSITORY_JTA_DATASOURCE_PROPERTY_NAME = "repositoryJTADataSourcePropertyName";
    public static final String DEFAULT_REPOSITORY_JTA_DATASOURCE_PROPERTY_NAME = "jta-cnxwks";
	@AD(deflt=DEFAULT_REPOSITORY_JTA_DATASOURCE_PROPERTY_NAME)
    public String repositoryJTADataSourcePropertyName();
    
	public static final String REPOSITORY_NON_JTA_DATASOURCE_PROPERTY_NAME = "repositoryNonJTADataSourcePropertyName";
    public static final String DEFAULT_REPOSITORY_NON_JTA_DATASOURCE_PROPERTY_NAME = "nonjta-cnxwks";
    @AD(deflt=DEFAULT_REPOSITORY_NON_JTA_DATASOURCE_PROPERTY_NAME)
    public String repositoryNonJTADataSourcePropertyName();

	public static final String START_REPOSITORY_AS_STANDALONE = "startStandalone";
    public static final Boolean DEFAULT_START_REPOSITORY_AS_STANDALONE = true;
    @AD(deflt="true")
    public Boolean startStandalone();
}
