package org.springframework.roo.process.manager.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.undo.CreateDirectory;
import org.springframework.roo.file.undo.CreateFile;
import org.springframework.roo.file.undo.DeleteDirectory;
import org.springframework.roo.file.undo.DeleteFile;
import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.file.undo.UndoEvent;
import org.springframework.roo.file.undo.UndoListener;
import org.springframework.roo.file.undo.UndoManager;
import org.springframework.roo.file.undo.UpdateFile;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Default implementation of {@link FileManager}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class DefaultFileManager implements FileManager, UndoListener {

    // Fields
    @Reference private FilenameResolver filenameResolver;
    @Reference private NotifiableFileMonitorService fileMonitorService;
    @Reference private ProcessManager processManager;
    @Reference private UndoManager undoManager;

    /** key: file identifier, value: new textual content */
    private final Map<String, String> deferredFileWrites = new LinkedHashMap<String, String>();

    /** key: file identifier, value: new description of change */
    private final Map<String, String> deferredDescriptionOfChanges = new LinkedHashMap<String, String>();

    protected void activate(final ComponentContext context) {
        undoManager.addUndoListener(this);
    }

    protected void deactivate(final ComponentContext context) {
        undoManager.removeUndoListener(this);
    }

    public boolean exists(final String fileIdentifier) {
        Assert.hasText(fileIdentifier, "File identifier required");
        return new File(fileIdentifier).exists();
    }

    public InputStream getInputStream(final String fileIdentifier) {
        if (deferredFileWrites.containsKey(fileIdentifier)) {
            return new BufferedInputStream(new ByteArrayInputStream(
                    deferredFileWrites.get(fileIdentifier).getBytes()));
        }

        File file = new File(fileIdentifier);
        Assert.isTrue(file.exists(), "File '" + fileIdentifier
                + "' does not exist");
        Assert.isTrue(file.isFile(), "Path '" + fileIdentifier
                + "' is not a file");
        try {
            return new BufferedInputStream(new FileInputStream(new File(
                    fileIdentifier)));
        }
        catch (IOException ioe) {
            throw new IllegalStateException(
                    "Could not obtain input stream to file '" + fileIdentifier
                            + "'", ioe);
        }
    }

    public FileDetails createDirectory(final String fileIdentifier) {
        Assert.notNull(fileIdentifier, "File identifier required");
        File actual = new File(fileIdentifier);
        Assert.isTrue(!actual.exists(), "File '" + fileIdentifier
                + "' already exists");
        try {
            this.fileMonitorService.notifyCreated(actual.getCanonicalPath());
        }
        catch (IOException ignored) {
        }
        new CreateDirectory(undoManager, filenameResolver, actual);
        return new FileDetails(actual, actual.lastModified());
    }

    public FileDetails readFile(final String fileIdentifier) {
        Assert.notNull(fileIdentifier, "File identifier required");
        File f = new File(fileIdentifier);
        if (!f.exists()) {
            return null;
        }
        return new FileDetails(f, f.lastModified());
    }

    public MutableFile createFile(final String fileIdentifier) {
        Assert.notNull(fileIdentifier, "File identifier required");
        File actual = new File(fileIdentifier);
        Assert.isTrue(!actual.exists(), "File '" + fileIdentifier
                + "' already exists");
        try {
            this.fileMonitorService.notifyCreated(actual.getCanonicalPath());
        }
        catch (IOException ignored) {
        }
        File parentDirectory = new File(actual.getParent());
        if (!parentDirectory.exists()) {
            createDirectory(FileUtils.getCanonicalPath(parentDirectory));
        }
        new CreateFile(undoManager, filenameResolver, actual);
        ManagedMessageRenderer renderer = new ManagedMessageRenderer(
                filenameResolver, actual, true);
        renderer.setIncludeHashCode(processManager.isDevelopmentMode());
        return new DefaultMutableFile(actual, null, renderer);
    }

    public void delete(final String fileIdentifier) {
        delete(fileIdentifier, null);
    }

    public void delete(final String fileIdentifier,
            final String reasonForDeletion) {
        if (StringUtils.isBlank(fileIdentifier)) {
            return;
        }

        final File actual = new File(fileIdentifier);
        Assert.isTrue(actual.exists(), "File '" + fileIdentifier
                + "' does not exist");
        try {
            this.fileMonitorService.notifyDeleted(actual.getCanonicalPath());
        }
        catch (IOException ignored) {
        }
        if (actual.isDirectory()) {
            new DeleteDirectory(undoManager, filenameResolver, actual,
                    reasonForDeletion);
        }
        else {
            new DeleteFile(undoManager, filenameResolver, actual,
                    reasonForDeletion);
        }
    }

    public MutableFile updateFile(final String fileIdentifier) {
        Assert.notNull(fileIdentifier, "File identifier required");
        File actual = new File(fileIdentifier);
        Assert.isTrue(actual.exists(), "File '" + fileIdentifier
                + "' does not exist");
        new UpdateFile(undoManager, filenameResolver, actual);
        ManagedMessageRenderer renderer = new ManagedMessageRenderer(
                filenameResolver, actual, false);
        renderer.setIncludeHashCode(processManager.isDevelopmentMode());
        return new DefaultMutableFile(actual, fileMonitorService, renderer);
    }

    public SortedSet<FileDetails> findMatchingAntPath(final String antPath) {
        return fileMonitorService.findMatchingAntPath(antPath);
    }

    public void createOrUpdateTextFileIfRequired(final String fileIdentifier,
            final String newContents, final boolean writeImmediately) {
        createOrUpdateTextFileIfRequired(fileIdentifier, newContents, "",
                writeImmediately);
    }

    public void createOrUpdateTextFileIfRequired(final String fileIdentifier,
            final String newContents, final String descriptionOfChange,
            final boolean writeImmediately) {
        if (writeImmediately) {
            createOrUpdateTextFileIfRequired(fileIdentifier, newContents,
                    descriptionOfChange);
        }
        else {
            deferredFileWrites.put(fileIdentifier, newContents);

            String deferredDescriptionOfChange = StringUtils.defaultIfEmpty(
                    deferredDescriptionOfChanges.get(fileIdentifier), "");
            if (StringUtils.hasText(deferredDescriptionOfChange)
                    && !deferredDescriptionOfChange.trim().endsWith(";")) {
                deferredDescriptionOfChange += "; ";
            }
            deferredDescriptionOfChanges.put(
                    fileIdentifier,
                    deferredDescriptionOfChange
                            + StringUtils.trimToEmpty(descriptionOfChange));
        }
    }

    public void commit() {
        Map<String, String> toRemove = new LinkedHashMap<String, String>(
                deferredFileWrites);
        try {
            for (final Entry<String, String> entry : toRemove.entrySet()) {
                final String fileIdentifier = entry.getKey();
                final String newContents = entry.getValue();
                if (StringUtils.hasText(newContents)) {
                    createOrUpdateTextFileIfRequired(fileIdentifier,
                            newContents,
                            StringUtils
                                    .trimToEmpty(deferredDescriptionOfChanges
                                            .get(fileIdentifier)));
                }
                else if (exists(fileIdentifier)) {
                    delete(fileIdentifier, "empty");
                }
            }
        }
        finally {
            for (String remove : toRemove.keySet()) {
                deferredFileWrites.remove(remove);
            }
            deferredDescriptionOfChanges.clear();
        }
    }

    public void clear() {
        deferredFileWrites.clear();
        deferredDescriptionOfChanges.clear();
    }

    public int scan() {
        return fileMonitorService.scanNotified();
    }

    public void onUndoEvent(final UndoEvent event) {
        if (event.isUndoing()) {
            clear();
        }
        else {
            // It's a flush or a reset event
            commit();
        }
    }

    private void createOrUpdateTextFileIfRequired(final String fileIdentifier,
            final String newContents, final String descriptionOfChange) {
        MutableFile mutableFile = null;
        if (exists(fileIdentifier)) {
            // First verify if the file has even changed
            File file = new File(fileIdentifier);
            String existing = null;
            try {
                existing = FileCopyUtils.copyToString(file);
            }
            catch (IOException ignored) {
            }

            if (!newContents.equals(existing)) {
                mutableFile = updateFile(fileIdentifier);
            }
        }
        else {
            mutableFile = createFile(fileIdentifier);
            Assert.notNull(mutableFile, "Could not create file '"
                    + fileIdentifier + "'");
        }

        if (mutableFile != null) {
            try {
                if (StringUtils.hasText(descriptionOfChange)) {
                    mutableFile.setDescriptionOfChange(descriptionOfChange);
                }
                FileCopyUtils.copy(newContents.getBytes(),
                        mutableFile.getOutputStream());
            }
            catch (IOException e) {
                throw new IllegalStateException("Could not output '"
                        + mutableFile.getCanonicalPath() + "'", e);
            }
        }
    }
}