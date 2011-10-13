/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.planets.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import uk.gov.nationalarchives.droid.report.interfaces.ReportManager;
import uk.gov.nationalarchives.droid.results.handlers.ProgressObserver;

/**
 * @author Alok Kumar Dash
 */
public class PlanetsXMLGenerationSwingWorker extends SwingWorker {

    private ClassPathXmlApplicationContext context;
    private JProgressBar planetXMLGenerationProgressBar;
    private String filePath;
    private JButton cancelButton;
    private JButton okButton;
    private JLabel label;
    private String profileId;

    private ReportManager reportManager;

    /**
     */

    /**
     * 
     * @param planetXMLGenerationProgressBar
     *            Progress Bar for progress update.
     * @param filePath
     *            File path where planets xml to be saved.
     * @param cancelButton
     *            Cancel button
     * @param okButton
     *            Ok Button.
     * @param label
     *            Label to chege the message from doing to done.
     * @param profileId
     *            ProfileId
     * @param reportManager
     *            ReportManager
     */

    public PlanetsXMLGenerationSwingWorker(String profileId,
            JProgressBar planetXMLGenerationProgressBar, String filePath,
            JButton cancelButton, JButton okButton, JLabel label,
            ReportManager reportManager) {
        this.planetXMLGenerationProgressBar = planetXMLGenerationProgressBar;
        this.filePath = filePath;
        this.cancelButton = cancelButton;
        this.okButton = okButton;
        this.label = label;
        this.profileId = profileId;
        this.reportManager = reportManager;
    }

    @Override
    protected Integer doInBackground() {
        final ProgressObserver observer = new ProgressObserver() {
            @Override
            public void onProgress(Integer progress) {
                setProgress(progress);
            }
        };
        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress".equals(evt.getPropertyName())) {
                    Integer value = (Integer) evt.getNewValue();
                    planetXMLGenerationProgressBar.setValue(value);
                }
            }
        });

        reportManager.setObserver(observer);

        reportManager.generatePlanetsXML(profileId, filePath);

        return null;
    }

    @Override
    protected void done() {
        cancelButton.hide();
        okButton.show();
        label.setText("PLANETS XML is at : ");
    }

    /**
     * @param reportManager
     *            the reportManager to set
     */
    public void setReportManager(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

}
