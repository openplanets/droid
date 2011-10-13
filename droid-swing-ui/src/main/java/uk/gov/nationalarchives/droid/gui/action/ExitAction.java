/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.gui.action;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.gov.nationalarchives.droid.gui.DroidUIContext;
import uk.gov.nationalarchives.droid.gui.ProfileForm;
import uk.gov.nationalarchives.droid.gui.widgetwrapper.FileChooserProxy;
import uk.gov.nationalarchives.droid.gui.widgetwrapper.FileChooserProxyImpl;
import uk.gov.nationalarchives.droid.gui.widgetwrapper.ProfileSelectionDialog;
import uk.gov.nationalarchives.droid.profile.ProfileInstance;
import uk.gov.nationalarchives.droid.profile.ProfileManager;

/**
 * Worjwe which exits the droid application.
 * @author rflitcroft
 *
 */
public class ExitAction extends SwingWorker<Void, Void> {

    private final Log log = LogFactory.getLog(getClass());

    private DroidUIContext context;
    private ProfileSelectionDialog dialog;
    private ProfileManager profileManager;
    private CountDownLatch latch;
    
    /**
     * 
     * @param context the droid ui context
     * @param dialog a dialog to be presented.
     * @param profileManager the profile manager
     */
    public ExitAction(DroidUIContext context, ProfileSelectionDialog dialog, ProfileManager profileManager) {
        this.context = context;
        this.dialog = dialog;
        this.profileManager = profileManager;
    }

    /**
     * Executes the exit action, which may involve saving unsaved profiles.
     * 
     */
    public void start() {
        Collection<ProfileForm> allProfiles = new ArrayList<ProfileForm>(context.allProfiles());
        latch = new CountDownLatch(allProfiles.size());
        
        List<ProfileForm> dirtyProfiles = context.allDirtyProfiles();
        if (!dirtyProfiles.isEmpty()) {
            
            dialog.open();
            int response = dialog.getResponse();
            if (response == ProfileSelectionDialog.YES) {
                List<ProfileForm> profilesToSave = dialog.getSelectedProfiles();
            
                for (final ProfileForm profile : profilesToSave) {
                    allProfiles.remove(profile);
                    final JFileChooser fileChooser = context.getProfileFileChooser();
                    fileChooser.setDialogTitle(String.format("Save profile '%s'", profile.getName()));
                    FileChooserProxy fileChooserDialog = new FileChooserProxyImpl(profile, fileChooser);
                    File loadedFrom = profile.getProfile().getLoadedFrom();
                    fileChooser.setSelectedFile(loadedFrom != null ? loadedFrom : new File(profile.getName()));
                    
                    SaveProfileWorker saveJob = new SaveProfileWorker(profileManager, profile, fileChooserDialog);
                    saveJob.addPropertyChangeListener(new SaveJobCompletionListener());
                    saveJob.start(false);
                }
            } else {
                cancel(true);
            }
        }
        
        if (!isCancelled()) {
            execute();
            for (ProfileForm profile : allProfiles) {
                try {
                    profileManager.closeProfile(profile.getProfile().getUuid());
                    context.remove(profile.getProfile().getUuid());
                } finally {
                    latch.countDown();
                }
            }
        }
    }
    
    @Override
    protected Void doInBackground() throws InterruptedException {
        latch.await();
        return null;
    }
    
    private final class SaveJobCompletionListener implements PropertyChangeListener {
        @SuppressWarnings("unchecked")
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("state".equals(evt.getPropertyName())
                    && evt.getNewValue() == SwingWorker.StateValue.DONE) {
                try {
                    Future<ProfileInstance> job = (Future<ProfileInstance>) evt.getSource();
                    ProfileInstance profile = job.get();
                    profileManager.closeProfile(profile.getUuid());
                    context.remove(profile.getUuid());
                } catch (InterruptedException e) {
                    log.debug(e);
                    throw new RuntimeException(e.getMessage(), e);
                } catch (ExecutionException e) {
                    cancel(true);
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            }
            
        }
        
    }
}
