/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlets.post.impl.helper;

import java.util.Dictionary;
import java.util.Map;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ChunkCleanUpTask</code> implements a job run at regular intervals
 * to find incomplete chunk uploads and remove them from the repository to
 * prevent littering the repository with incomplete chunks.
 * <p>
 * This task is configured with OSGi configuration for the PID
 * <code>org.apache.sling.servlets.post.impl.helper.ChunkCleanUpTask</code> with
 * property <code>scheduler.expression</code> being the schedule to execute the
 * task. The schedule is a cron job expression as described at <a
 * href="http://www.docjar.com/docs/api/org/quartz/CronTrigger.html">Cron
 * Trigger</a> with the default value configured to run twice a day at 0h41m31s
 * and 12h4131s.
 * <p>
 * The property <code>chunk.cleanup.age</code> specifies chunk's age in minutes
 * before it is considered for clean up.
 * <p>
 * Currently the cleanup tasks connects as the administrative user to the
 * default workspace assuming users are stored in that workspace and the
 * administrative user has full access.
 */
public class ChunkCleanUpTask implements Runnable, ManagedService {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private volatile SlingRepository repository;

    private static final String CHUNK_CLEANUP_AGE = "chunk.cleanup.age";

    private SlingFileUploadHandler uploadhandler = new SlingFileUploadHandler();

    /**
     * Clean up age criterion in millisec.
     */
    private long chunkCleanUpAge;

	private Dictionary<String, ?> properties;

	private volatile BundleContext context;
    /**
     * Executes the job. Is called for each triggered schedule point.
     */
    public void run() {
        log.debug("ChunkCleanUpTask: Starting cleanup");
        cleanup();
    }

    /**
     * This method deletes chunks which are {@link #isEligibleForCleanUp(Node)}
     * for cleanup. It queries all
     * {@link SlingPostConstants#NT_SLING_CHUNK_MIXIN} nodes and filter nodes
     * which are {@link #isEligibleForCleanUp(Node)} for cleanup. It then
     * deletes old chunks upload.
     */
    private void cleanup() {

        long start = System.currentTimeMillis();

        int numCleaned = 0;
        int numLive = 0;

        Session admin = null;
        try {
            // assume chunks are stored in the default workspace
            admin = repository.loginAdministrative(null);
            QueryManager qm = admin.getWorkspace().getQueryManager();

            QueryResult queryres = qm.createQuery(
                "SELECT * FROM [sling:chunks] ", Query.JCR_SQL2).execute();
            NodeIterator nodeItr = queryres.getNodes();
            while (nodeItr.hasNext()) {
                Node node = nodeItr.nextNode();
                if (isEligibleForCleanUp(node)) {
                    numCleaned++;
                    uploadhandler.deleteChunks(node);
                } else {
                    numLive++;
                }
            }
            if (admin.hasPendingChanges()) {
                try {
                    admin.refresh(true);
                    admin.save();
                } catch (InvalidItemStateException iise) {
                    log.info("ChunkCleanUpTask: Concurrent modification to one or more of the chunk to be removed. Retrying later");
                } catch (RepositoryException re) {
                    log.info("ChunkCleanUpTask: Failed persisting chunk removal. Retrying later");
                }
            }

        } catch (Throwable t) {
            log.error(
                "ChunkCleanUpTask: General failure while trying to cleanup chunks",
                t);
        } finally {
            if (admin != null) {
                admin.logout();
            }
        }
        long end = System.currentTimeMillis();
        log.info(
            "ChunkCleanUpTask finished: Removed {} chunk upload(s) in {}ms ({} chunk upload(s) still active)",
            new Object[] { numCleaned, (end - start), numLive });
    }

    /**
     * Check if {@link Node} is eligible of
     * {@link SlingPostConstants#NT_SLING_CHUNK_NODETYPE} cleanup. To be
     * eligible the age of last
     * {@link SlingPostConstants#NT_SLING_CHUNK_NODETYPE} uploaded should be
     * greater than @link {@link #chunkCleanUpAge}
     *
     * @param node {@link Node} containing
     *            {@link SlingPostConstants#NT_SLING_CHUNK_NODETYPE}
     *            {@link Node}s
     * @return true if eligible else false.
     * @throws RepositoryException
     */
    private boolean isEligibleForCleanUp(Node node) throws RepositoryException {
        Node lastChunkNode = uploadhandler.getLastChunk(node);
        return lastChunkNode != null
            && (System.currentTimeMillis() - lastChunkNode.getProperty(
                javax.jcr.Property.JCR_CREATED).getDate().getTimeInMillis()) > chunkCleanUpAge;
    }
    
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
	}

    protected void activate() {
        chunkCleanUpAge = OsgiUtil.toInteger(
            this.properties.get(CHUNK_CLEANUP_AGE), 1) * 60 * 1000;
        log.info("scheduler config [{}], chunkGarbageTime  [{}] ms",
            OsgiUtil.toString(this.properties.get("scheduler.expression"), ""),
            chunkCleanUpAge);

    }
}