/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.submitter;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.gov.nationalarchives.droid.core.interfaces.ResourceId;
import uk.gov.nationalarchives.droid.profile.AbstractProfileResource;
import uk.gov.nationalarchives.droid.profile.ProfileSpec;
import uk.gov.nationalarchives.droid.results.handlers.ProgressMonitor;
import uk.gov.nationalarchives.droid.submitter.FileWalker.ProgressEntry;
import uk.gov.nationalarchives.droid.submitter.ProfileWalkState.WalkStatus;

/**
 * Iterates over all resources in the profile spec.
 * This is NOT thread safe, and you must instantiate a new instance for
 * any concurrent walking.
 * 
 * @author rflitcroft
 * 
 */
public class ProfileSpecWalkerImpl implements ProfileSpecWalker {

    private final Log log = LogFactory.getLog(getClass());

    private FileEventHandler fileEventHandler;
    private DirectoryEventHandler directoryEventHandler;
    private ProgressMonitor progressMonitor;
    
    private transient volatile boolean cancelled;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void walk(final ProfileSpec profileSpec, final ProfileWalkState walkState) throws IOException {
        
        final List<AbstractProfileResource> resources = profileSpec.getResources();

        boolean fastForward = false;
        
        int startIndex = 0;
        if (walkState.getWalkStatus().equals(WalkStatus.IN_PROGRESS)) {
            fastForward = true;
            startIndex = resources.indexOf(walkState.getCurrentResource());
        }
        
        for (int i = startIndex; i < resources.size(); i++) {
            AbstractProfileResource resource = resources.get(i);
            if (!fastForward) {
                walkState.setCurrentResource(resource);
                walkState.setCurrentFileWalker(null);
            }
            
            if (cancelled) {
                break;
            }
            
            if (resource.isDirectory()) {
                FileWalker fileWalker;
                if (!fastForward) {
                    walkState.setCurrentFileWalker(new FileWalker(resource.getUri(), resource.isRecursive()));
                }
                
                fileWalker = walkState.getCurrentFileWalker();
                
                fileWalker.setFileHandler(new FileWalkerHandler() {
                    @Override
                    public ResourceId handle(File file, int depth, ProgressEntry parent) { 
                        progressMonitor.startJob(file.toURI());
                        ResourceId parentId = parent == null ? null : parent.getResourceId();
                        fileEventHandler.onEvent(file, parentId, null);
                        return null;
                    }
                });
                
                fileWalker.setDirectoryHandler(new FileWalkerHandler() {
                    @Override
                    public ResourceId handle(File file, int depth, ProgressEntry parent) {
                        progressMonitor.startJob(file.toURI());
                        ResourceId parentId = parent == null ? null : parent.getResourceId();
                        return directoryEventHandler.onEvent(file, parentId, depth, false);
                    }
                });
                
                fileWalker.setRestrictedDirectoryHandler(new FileWalkerHandler() {
                    @Override
                    public ResourceId handle(File file, int depth, ProgressEntry parent) {
                        progressMonitor.startJob(file.toURI());
                        ResourceId parentId = parent == null ? null : parent.getResourceId();
                        return directoryEventHandler.onEvent(file, parentId, depth, true);
                    }
                });
                
                walkState.setWalkStatus(WalkStatus.IN_PROGRESS);
                fileWalker.walk();
            } else {
                progressMonitor.startJob(resource.getUri());
                fileEventHandler.onEvent(new File(resource.getUri()), null, null);
            }
            
            fastForward = false;
        }
        walkState.setWalkStatus(WalkStatus.FINISHED);
        progressMonitor.setTargetCount(progressMonitor.getIdentificationCount());
    }
    
    /**
     * @param fileEventHandler
     *            an event handler to be fired when a file is encountered.
     */
    public void setFileEventHandler(FileEventHandler fileEventHandler) {
        this.fileEventHandler = fileEventHandler;

    }

    /**
     * @param directoryEventHandler
     *            an event handler to be fired when a directory is encountered.
     */
    public void setDirectoryEventHandler(DirectoryEventHandler directoryEventHandler) {
        this.directoryEventHandler = directoryEventHandler;
    }

    /**
     *  To cancel Profile speck walker.
     */
    public void cancel() {
        cancelled = true;
    }

    /**
     * @param progressMonitor
     *            the progressMonitor to set
     */
    public void setProgressMonitor(ProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
    }
    
    /**
     * @return the progressMonitor
     */
    @Override
    public ProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public FileEventHandler getFileEventHandler() {
        return fileEventHandler;
    }
    
}
