package org.springframework.roo.addon.roobot.client.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BundleVersion {
    private final String uri;
    private final String obrUrl;
    private final String version;
    private final String presentationName;
    private final Long size;
    private final String description;
    private Map<String, String> commands = new HashMap<String, String>();
    private final String pgpKey;
    private final String pgpDescriptions;
    private final String rooVersion;

    public BundleVersion(final String uri, final String obrUrl,
            final String version, final String presentationName,
            final Long size, final String description, final String pgpKey,
            final String pgpDescriptions, final String rooVersion,
            final Map<String, String> commands) {
        super();
        this.uri = uri;
        this.obrUrl = obrUrl;
        this.version = version;
        this.presentationName = presentationName;
        this.size = size;
        this.description = description;
        this.pgpKey = pgpKey;
        this.pgpDescriptions = pgpDescriptions;
        this.commands = commands;
        this.rooVersion = rooVersion;
    }

    public String getUri() {
        return uri;
    }

    public String getObrUrl() {
        return obrUrl;
    }

    public String getVersion() {
        return version;
    }

    public String getPresentationName() {
        return presentationName;
    }

    public Long getSize() {
        return size;
    }

    public String getRooVersion() {
        return rooVersion;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getCommands() {
        return commands;
    }

    public String getPgpKey() {
        return pgpKey;
    }

    public String getPgpDescriptions() {
        return pgpDescriptions;
    }

    public String getSummary() {
        return presentationName + "; " + description + "; " + pgpDescriptions
                + "; " + commands.toString();
    }

    /**
     * Returns a {@link List} of {@link BundleVersion} objects in ascending
     * version order, i.e. the object with the smallest version is in position 0
     * and the object with the highest version is in position n-1. This method
     * does not take into account release types, that is a 0.1.0.GA is seen as
     * an earlier version than a 0.1.0.M1 version.
     * 
     * @param versions
     * @return a {@link List} of {@link BundleVersion} objects in ascending
     *         order
     */
    public static List<BundleVersion> orderByVersion(
            final List<BundleVersion> versions) {
        Collections.sort(versions, new Comparator<BundleVersion>() {
            public int compare(final BundleVersion o1, final BundleVersion o2) {
                return o1.getVersion().compareToIgnoreCase(o2.getVersion());
            }
        });
        return Collections.unmodifiableList(versions);
    }
}
