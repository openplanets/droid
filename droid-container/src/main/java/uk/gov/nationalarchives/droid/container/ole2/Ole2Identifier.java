/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.container.ole2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import uk.gov.nationalarchives.droid.container.AbstractContainerIdentifier;
import uk.gov.nationalarchives.droid.container.ContainerSignatureMatch;
import uk.gov.nationalarchives.droid.container.ContainerSignatureMatchCollection;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationRequest;
import uk.gov.nationalarchives.droid.core.signature.ByteReader;

/**
 * @author rflitcroft
 *
 */
public class Ole2Identifier extends AbstractContainerIdentifier {
    
    /**
     * {@inheritDoc}
     * @throws IOException 
     */
    @Override
    public final void process(IdentificationRequest request, 
        ContainerSignatureMatchCollection matches) throws IOException {
        final InputStream in = request.getSourceInputStream();
        try {
            POIFSFileSystem reader = new POIFSFileSystem(in);
            DirectoryEntry root = reader.getRoot();
            for (Iterator<Entry> it = root.getEntries(); it.hasNext();) {
                Entry entry = it.next();
                String entryName = entry.getName().trim();
    
                boolean needsBinaryMatch = false;
    
                for (ContainerSignatureMatch match : matches.getContainerSignatureMatches()) {
                    match.matchFileEntry(entryName);
                    if (match.needsBinaryMatch(entryName)) {
                        needsBinaryMatch = true;
                    }
                }
                
                if (needsBinaryMatch) {
                    DocumentInputStream docIn = null;
                    ByteReader byteReader = null;
                    try {
                        docIn = reader.createDocumentInputStream(entry.getName());
                        byteReader = newByteReader(docIn);
                        for (ContainerSignatureMatch match : matches.getContainerSignatureMatches()) {
                            match.matchBinaryContent(entryName, byteReader);
                        }
                    } finally {
                        if (byteReader != null) {
                            byteReader.close();
                        }
                        if (docIn != null) {
                            docIn.close();
                        }
                    }
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
