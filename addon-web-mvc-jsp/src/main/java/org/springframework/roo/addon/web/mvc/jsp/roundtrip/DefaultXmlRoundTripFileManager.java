package org.springframework.roo.addon.web.mvc.jsp.roundtrip;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.HexUtils;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;

/**
 * Default implementation of {@link XmlRoundTripFileManager}.
 * 
 * @author James Tyrrell
 * @since 1.2.0
 */
@Component
@Service
public class DefaultXmlRoundTripFileManager implements XmlRoundTripFileManager {

    // Fields
    @Reference private FileManager fileManager;
    private final Map<String, String> fileContentsMap = new HashMap<String, String>();

    private static MessageDigest sha = null;
    static {
        try {
            sha = MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException ignored) {
        }
    }

    public void writeToDiskIfNecessary(final String filename,
            final Document proposed) {
        Assert.notNull(filename, "The file name is required");
        Assert.notNull(proposed, "The proposed document is required");
        if (fileManager.exists(filename)) {
            String proposedContents = XmlUtils.nodeToString(proposed);
            try {
                if (sha != null) {
                    String contents = FileCopyUtils.copyToString(new File(
                            filename)) + proposedContents;
                    byte[] digest = sha.digest(contents.getBytes());
                    String contentsSha = HexUtils.toHex(digest);
                    String lastContents = fileContentsMap.get(filename);
                    if (lastContents != null
                            && contentsSha.equals(lastContents)) {
                        return;
                    }
                    fileContentsMap.put(filename, contentsSha);
                }
            }
            catch (IOException ignored) {
            }
            final Document original = XmlUtils.readXml(fileManager
                    .getInputStream(filename));
            if (XmlRoundTripUtils.compareDocuments(original, proposed)) {
                DomUtils.removeTextNodes(original);
                String updateContents = XmlUtils.nodeToString(original);
                fileManager.createOrUpdateTextFileIfRequired(filename,
                        updateContents, false);
            }
        }
        else {
            String contents = XmlUtils.nodeToString(proposed);
            if (sha != null) {
                byte[] digest = sha.digest((contents + contents).getBytes());
                String contentsSha = HexUtils.toHex(digest);
                fileContentsMap.put(filename, contentsSha);
            }
            fileManager.createOrUpdateTextFileIfRequired(filename, contents,
                    false);
        }
    }
}
