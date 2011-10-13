/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.core;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.gov.nationalarchives.droid.core.interfaces.DroidCore;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationMethod;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationRequest;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResult;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResultCollection;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResultImpl;
import uk.gov.nationalarchives.droid.core.signature.ByteReader;
import uk.gov.nationalarchives.droid.core.signature.FileFormat;
import uk.gov.nationalarchives.droid.core.signature.FileFormatCollection;
import uk.gov.nationalarchives.droid.core.signature.FileFormatHit;
import uk.gov.nationalarchives.droid.core.signature.droid6.FFSignatureFile;

/**
 * Implementation of DroidCore which uses the droid binary signatures to identify files. 
 * identifications.
 * @author rflitcroft
 *
 */
public final class BinarySignatureIdentifier implements DroidCore {

    private final Log log = LogFactory.getLog(getClass());

    private FFSignatureFile sigFile;
    private SignatureFileParser sigFileParser = new SignatureFileParser();
    private URI signatureFile;
    
    /**
     * Default constructor.
     */
    public BinarySignatureIdentifier() { }
    
    /**
     * Initialises this droid core with its signature file.
     */
    public void init() {
        try {
            sigFile = sigFileParser.parseSigFile(signatureFile.getPath());
            sigFile.prepareForUse();
        } catch (SignatureParseException e) {
            String message = String.format("Could not parse the binary signature file at: %s", signatureFile.getPath());
            log.warn(message);
        }
    }
    
    /**
     * Sets the signature file.
     * @param signatureFile the signature file to set
     */
    @Override
    public void setSignatureFile(String signatureFile) {
        this.signatureFile = new File(signatureFile).toURI();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IdentificationResultCollection matchBinarySignatures(IdentificationRequest request) {
        IdentificationResultCollection results = new IdentificationResultCollection(request);
        results.setRequestMetaData(request.getRequestMetaData());
        ByteReader byteReader = new IdentificationRequestByteReaderAdapter(request);
        sigFile.runFileIdentification(byteReader);
        final int numHits = byteReader.getNumHits();
        for (int i = 0; i < numHits; i++) {
            FileFormatHit hit = byteReader.getHit(i);
            IdentificationResultImpl result = new IdentificationResultImpl();
            result.setMimeType(hit.getMimeType());
            result.setName(hit.getFileFormatName());
            result.setVersion(hit.getFileFormatVersion());
            result.setPuid(hit.getFileFormatPUID());
            result.setMethod(IdentificationMethod.BINARY_SIGNATURE);
            results.addResult(result);
        }
        results.setFileLength(request.size());
        results.setRequestMetaData(request.getRequestMetaData());
        return results;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IdentificationResultCollection matchExtensions(
            IdentificationRequest request, boolean allExtensions) {
        IdentificationResultCollection results = new IdentificationResultCollection(request);
        results.setRequestMetaData(request.getRequestMetaData());
        String fileExtension = request.getExtension();
        if (fileExtension != null && !fileExtension.isEmpty()) {
            List<FileFormat> fileFormats;
            if (allExtensions) {
                fileFormats = sigFile.getFileFormatsForExtension(fileExtension);
            } else {
                fileFormats = sigFile.getTentativeFormatsForExtension(fileExtension);
            }
            if (fileFormats != null) {
                for (FileFormat format : fileFormats) {
                    IdentificationResultImpl result = new IdentificationResultImpl();
                    result.setName(format.getName());
                    result.setVersion(format.getVersion());
                    result.setPuid(format.getPUID());
                    result.setMimeType(format.getMimeType());
                    result.setMethod(IdentificationMethod.EXTENSION);
                    results.addResult(result);
                }
            }
        }
        results.setFileLength(request.size());
        results.setRequestMetaData(request.getRequestMetaData());
        return results;
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void removeSignatureForPuid(String puid) {
        sigFile.puidHasOverridingSignatures(puid);
    }
    
    /**
     * @return the sigFile
     */
    FFSignatureFile getSigFile() {
        return sigFile;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setMaxBytesToScan(long maxBytes) {
        sigFile.setMaxBytesToScan(maxBytes);
    }

    /**
     * {@inheritDoc}   
     */
    @Override
    public void removeLowerPriorityHits(
            IdentificationResultCollection results) {
        // Build a set of format ids the results have priority over:
        FileFormatCollection allFormats = sigFile.getFileFormatCollection();
        Set<Integer> lowerPriorityIDs = new HashSet<Integer>();
        for (IdentificationResult result : results.getResults()) {
            final String resultPUID = result.getPuid();
            final FileFormat format = allFormats.getFormatForPUID(resultPUID);
            lowerPriorityIDs.addAll(format.getFormatIdsHasPriorityOver());
        }
        
        // If a result has an id in this set, add it to the remove list;
        List<IdentificationResult> lowerPriorityResults = new ArrayList<IdentificationResult>();
        for (IdentificationResult result : results.getResults()) {
            final String resultPUID = result.getPuid();
            final FileFormat format = allFormats.getFormatForPUID(resultPUID);
            if (lowerPriorityIDs.contains(format.getID())) {
                lowerPriorityResults.add(result);
            }
        }
         
        // Now remove any lower priority results from the collection:
        for (IdentificationResult result : lowerPriorityResults) {
            results.removeResult(result);
        }
    }

    /**
     * If there is no extension, then issue a mismatch warning if
     * any of the file formats have an extension defined.
     * 
     * If there is an extension, then issue a mismatch warning if
     * any of the result formats do not match the given extension,
     * 
     * If there are no identified file formats at all, then do not 
     * issue a format mismatch warning no matter what the extension.
     * 
     * {@inheritDoc}   
     */
    @Override
    public void checkForExtensionsMismatches(
            IdentificationResultCollection results, String fileExtension) {
        if (fileExtension == null || fileExtension.isEmpty()) {
            FileFormatCollection allFormats = sigFile.getFileFormatCollection();
            for (IdentificationResult result : results.getResults()) {
                final String resultPUID = result.getPuid();
                final FileFormat format = allFormats.getFormatForPUID(resultPUID);
                if (format.getNumExtensions() > 0) {
                    results.setExtensionMismatch(true);
                    break;
                }
            }
        } else {
            FileFormatCollection allFormats = sigFile.getFileFormatCollection();
            for (IdentificationResult result : results.getResults()) {
                final String resultPUID = result.getPuid();
                final FileFormat format = allFormats.getFormatForPUID(resultPUID);
                if (format.hasExtensionMismatch(fileExtension)) {
                    results.setExtensionMismatch(true);
                    break;
                }
            }
        }
    }
   
}
