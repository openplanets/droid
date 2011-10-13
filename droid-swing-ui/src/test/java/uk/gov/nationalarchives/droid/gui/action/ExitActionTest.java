/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.gui.action;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.swing.JFileChooser;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import uk.gov.nationalarchives.droid.gui.DroidUIContext;
import uk.gov.nationalarchives.droid.gui.ProfileForm;
import uk.gov.nationalarchives.droid.gui.widgetwrapper.JOptionPaneProxy;
import uk.gov.nationalarchives.droid.gui.widgetwrapper.ProfileSelectionDialog;
import uk.gov.nationalarchives.droid.profile.ProfileInstance;
import uk.gov.nationalarchives.droid.profile.ProfileManager;
import uk.gov.nationalarchives.droid.results.handlers.ProgressObserver;

public class ExitActionTest {

    private ExitAction action;
    private DroidUIContext context;
    private ProfileManager profileManager;
    private ProfileSelectionDialog dialog;
    private ProfileInstance dirtyProfile1;
    private ProfileInstance dirtyProfile2;
    private ProfileForm dirtyProfileForm1;
    private ProfileForm dirtyProfileForm2;
    
    @Before
    public void setup() {
        context = mock(DroidUIContext.class);
        dialog = mock(ProfileSelectionDialog.class);
        profileManager = mock(ProfileManager.class);
        action = new ExitAction(context, dialog, profileManager);
        
        dirtyProfile1 = mock(ProfileInstance.class);
        dirtyProfile2 = mock(ProfileInstance.class);
        
        when(dirtyProfile1.getUuid()).thenReturn("dirty1");
        when(dirtyProfile2.getUuid()).thenReturn("dirty2");

        dirtyProfileForm1 = mock(ProfileForm.class);
        dirtyProfileForm2 = mock(ProfileForm.class);
        when(dirtyProfileForm1.getProfile()).thenReturn(dirtyProfile1);
        when(dirtyProfileForm2.getProfile()).thenReturn(dirtyProfile2);
        when(dirtyProfileForm1.getName()).thenReturn("profile 1");
        when(dirtyProfileForm2.getName()).thenReturn("profile 2");
        
        List<ProfileForm> allProfiles = new ArrayList<ProfileForm>();
        allProfiles.add(dirtyProfileForm1);
        allProfiles.add(dirtyProfileForm2);
        when(context.allProfiles()).thenReturn(allProfiles);
    }
    
    @Test
    public void testExitWhen2CleanProfilesAreLoaded() {
        
        List<ProfileForm> emptyList = Collections.emptyList();
        when(context.allDirtyProfiles()).thenReturn(emptyList);
        verify(dialog, never()).getResponse();
    }

    @Test
    public void testExitWhen2DirtyProfilesAreLoadedButWarningDialogReturnsCancel() throws Exception {
        List<ProfileForm> dirty = new ArrayList<ProfileForm>();
        dirty.add(dirtyProfileForm1);
        dirty.add(dirtyProfileForm2);
        
        when(dialog.getResponse()).thenReturn(JOptionPaneProxy.CANCEL);
        
        when(context.allDirtyProfiles()).thenReturn(dirty);

        action.start();
        try {
            action.get();
            fail("Expected CancellationException");
        } catch (CancellationException e) {
            
        }
        
        verify(profileManager, never()).save(anyString(), any(File.class), any(ProgressObserver.class));
        verify(profileManager, never()).closeProfile(anyString());
        verify(context, never()).remove(anyString());
    }

    @Test
    public void testExitWhen2DirtyProfilesAreLoadedAndBothWantSaving() throws Exception {
        
        JFileChooser fileChooser = mock(JFileChooser.class);
        when(fileChooser.getSelectedFile()).thenReturn(new File("foo"));
        
        when(context.getProfileFileChooser()).thenReturn(fileChooser);
        
        List<ProfileForm> dirty = new ArrayList<ProfileForm>();
        dirty.add(dirtyProfileForm1);
        dirty.add(dirtyProfileForm2);
        
        when(context.allDirtyProfiles()).thenReturn(dirty);
        when(dialog.getSelectedProfiles()).thenReturn(dirty);
        when(dialog.getResponse()).thenReturn(JOptionPaneProxy.YES);
        
        when(profileManager.save(eq("dirty1"), eq(new File("foo.droid")), 
                any(ProgressObserver.class))).thenReturn(dirtyProfile1);
        when(profileManager.save(eq("dirty2"), eq(new File("foo.droid")), 
                any(ProgressObserver.class))).thenReturn(dirtyProfile2);

        action.start();
        action.get();
        
        verify(profileManager).save(eq("dirty1"), eq(new File("foo.droid")), any(ProgressObserver.class));
        verify(profileManager).save(eq("dirty2"), eq(new File("foo.droid")), any(ProgressObserver.class));

        verify(profileManager).closeProfile("dirty1");
        verify(profileManager).closeProfile("dirty2");
        
        verify(context).remove("dirty1");
        verify(context).remove("dirty2");
    }

    @Test
    public void testExitWhen2DirtyProfilesAreLoadedAndNoneAreWantingSaving() {
        
        
        List<ProfileForm> dirty = new ArrayList<ProfileForm>();
        dirty.add(dirtyProfileForm1);
        dirty.add(dirtyProfileForm2);
        
        when(context.allDirtyProfiles()).thenReturn(dirty);
        when(dialog.getSelectedProfiles()).thenReturn(Collections.EMPTY_LIST);
        when(dialog.getResponse()).thenReturn(JOptionPaneProxy.YES);
        action.execute();
        
        verify(dirtyProfileForm1, never()).saveProfile(anyBoolean());
        verify(dirtyProfileForm2, never()).saveProfile(anyBoolean());
    }

    @Test
    public void testExitWhenTwoDirtyDirtyProfilesAreLoadedAndOneWantsSaving() throws Exception {
        
        JFileChooser fileChooser = mock(JFileChooser.class);
        when(fileChooser.getSelectedFile()).thenReturn(new File("foo"));
        
        when(context.getProfileFileChooser()).thenReturn(fileChooser);
        List<ProfileForm> dirty = new ArrayList<ProfileForm>();
        dirty.add(dirtyProfileForm1);
        dirty.add(dirtyProfileForm2);
        
        List<ProfileForm> selected = new ArrayList<ProfileForm>();
        selected.add(dirtyProfileForm2);
        
        when(profileManager.save(eq("dirty1"), eq(new File("foo.droid")), 
                any(ProgressObserver.class))).thenReturn(dirtyProfile1);
        when(profileManager.save(eq("dirty2"), eq(new File("foo.droid")), 
                any(ProgressObserver.class))).thenReturn(dirtyProfile2);

        when(context.allDirtyProfiles()).thenReturn(dirty);
        when(dialog.getSelectedProfiles()).thenReturn(selected);
        when(dialog.getResponse()).thenReturn(JOptionPaneProxy.YES);
        action.start();
        action.get();
        
        verify(profileManager, never()).save(eq("dirty1"), any(File.class), any(ProgressObserver.class));
        verify(profileManager).save(eq("dirty2"), eq(new File("foo.droid")), any(ProgressObserver.class));

        verify(profileManager).closeProfile("dirty1");
        verify(profileManager).closeProfile("dirty2");
        
        verify(context).remove("dirty1");
        verify(context).remove("dirty2");
       
    }
}
