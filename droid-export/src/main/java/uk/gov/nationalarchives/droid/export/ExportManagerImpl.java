/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.gov.nationalarchives.droid.core.interfaces.filter.Filter;
import uk.gov.nationalarchives.droid.export.interfaces.ExportManager;
import uk.gov.nationalarchives.droid.export.interfaces.ExportOptions;
import uk.gov.nationalarchives.droid.export.interfaces.ItemReader;
import uk.gov.nationalarchives.droid.export.interfaces.ItemReaderCallback;
import uk.gov.nationalarchives.droid.export.interfaces.ItemWriter;
import uk.gov.nationalarchives.droid.export.interfaces.JobCancellationException;
import uk.gov.nationalarchives.droid.profile.ProfileContextLocator;
import uk.gov.nationalarchives.droid.profile.ProfileInstance;
import uk.gov.nationalarchives.droid.profile.ProfileInstanceManager;
import uk.gov.nationalarchives.droid.profile.ProfileResourceNode;

/**
 * @author rflitcroft
 *
 */
public class ExportManagerImpl implements ExportManager {

    private final Log log = LogFactory.getLog(getClass());
    
    private ProfileContextLocator profileContextLocator;
    private ItemWriter<ProfileResourceNode> itemWriter;
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Future<?> exportProfiles(List<String> profileIds, String destination, Filter filter, ExportOptions options) {
        final ExportTask exportTask = new ExportTask(destination, profileIds, filter, options);
        FutureTask<?> task = new FutureTask<Object>(exportTask, null) {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (mayInterruptIfRunning) {
                    exportTask.cancel();
                }
                return super.cancel(false);
            }
        };
        executor.execute(task);
        return task;
    }
    
    /**
     * @param profileContextLocator the profileContextLocator to set
     */
    public void setProfileContextLocator(
            ProfileContextLocator profileContextLocator) {
        this.profileContextLocator = profileContextLocator;
    }
    
    /**
     * @param itemWriter the itemWriter to set
     */
    public void setItemWriter(ItemWriter<ProfileResourceNode> itemWriter) {
        this.itemWriter = itemWriter;
    }
    
    private final class ExportTask implements Runnable {
        
        private String destination;
        private List<String> profileIds;
        private Filter filterOverride;
        private ExportOptions options;
        
        private volatile boolean cancelled;
        
        public ExportTask(String destination, List<String> profileIds, Filter filterOverride, ExportOptions options) {
            this.destination = destination;
            this.profileIds = profileIds;
            this.filterOverride = filterOverride;
            this.options = options;
        }
        
        public void cancel() {
            cancelled = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            Writer writer;
            String destinationDescription = destination == null ? "System.out" : destination;
            if (destination == null) {
                writer = new PrintWriter(System.out);
            } else {
                try {
                    writer = new BufferedWriter(new FileWriter(destination));
                } catch (IOException e) {
                    String message = String.format("IO exception occurred trying to read from: %s",
                            destinationDescription);
                    log.error(message, e);
                    throw new RuntimeException(message, e);
                }
            }
            doExport(writer, destinationDescription);
        }

        private void doExport(Writer writer, String destinationDescription) {
            log.info(String.format("Exporting profiles to: [%s]", destinationDescription));
            itemWriter.setOptions(options);
            itemWriter.open(writer);
            StopWatch stopWatch = new StopWatch();
            try {
                for (String profileId : profileIds) {
                    stopWatch.start();
                    if (!profileContextLocator.hasProfileContext(profileId)) {
                        final String message = String.format("Profile not available for export: %s", profileId);
                        log.warn(message);
                        throw new RuntimeException(message);
                    }
                    ProfileInstance profile = profileContextLocator.getProfileInstance(profileId);
                    ProfileInstanceManager profileContext = profileContextLocator.openProfileInstanceManager(profile);
                    ItemReader<ProfileResourceNode> reader = profileContext.getNodeItemReader();
                    ItemReaderCallback<ProfileResourceNode> callback = new ItemReaderCallback<ProfileResourceNode>() {
                        @Override
                        public void onItem(List<? extends ProfileResourceNode> itemChunk) 
                            throws JobCancellationException {
                            itemWriter.write(itemChunk);
                            if (cancelled) {
                                log.info("Export interrupted");
                                throw new JobCancellationException("Cancelled");
                            }
                        }
                    };
                    
                    Filter filter = filterOverride != null ? filterOverride : profile.getFilter();
                    reader.readAll(callback, filter);
                    stopWatch.stop();
                    log.info(String.format("Time for export [%s]: %s ms", profileId, stopWatch.getTime()));
                    stopWatch.reset();
                }
            } catch (JobCancellationException e) {
                String message = String.format("Export cancelled - deleting export destination: %s",
                        destinationDescription);
                log.info(message);
                cancelled = true;
            } finally {          
                log.info(String.format("Closing export file: %s", destinationDescription));
                itemWriter.close();
                if (cancelled && destination != null) {
                    File toDelete = new File(destination);
                    if (!toDelete.delete()  && toDelete.exists()) {
                        log.warn(String.format("Could not delete export file: %s. "
                                + "Will try to delete on exit.", destination));
                        toDelete.deleteOnExit();
                    }
                }
            }
        }
    }

    
    /**
     * Shuts down the executor service.
     */
    public void destroy() {
        executor.shutdownNow();
    }
    
}
