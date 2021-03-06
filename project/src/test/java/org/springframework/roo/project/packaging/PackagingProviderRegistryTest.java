package org.springframework.roo.project.packaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit test of {@link PackagingProviderRegistryImpl}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PackagingProviderRegistryTest {

    // Constants
    private static final String CORE_JAR_ID = "jar";
    private static final String CUSTOM_JAR_ID = "jar_custom";
    private static final String CORE_WAR_ID = "war";

    // Fixture
    private PackagingProviderRegistryImpl registry;
    @Mock private CorePackagingProvider mockCoreJarPackaging;
    @Mock private CorePackagingProvider mockWarPackaging;
    @Mock private PackagingProvider mockCustomJarPackaging;

    @Before
    public void setUp() {
        // Mocks
        MockitoAnnotations.initMocks(this);
        setUpMockPackagingProvider(mockCoreJarPackaging, CORE_JAR_ID, true);
        setUpMockPackagingProvider(mockCustomJarPackaging, CUSTOM_JAR_ID, true);
        setUpMockPackagingProvider(mockWarPackaging, CORE_WAR_ID, false);

        // Object under test
        this.registry = new PackagingProviderRegistryImpl();
        this.registry.bindPackagingProvider(mockCoreJarPackaging);
        this.registry.bindPackagingProvider(mockCustomJarPackaging);
        this.registry.bindPackagingProvider(mockWarPackaging);
    }

    private void setUpMockPackagingProvider(
            final PackagingProvider mockPackagingProvider, final String id,
            final boolean isDefault) {
        when(mockPackagingProvider.getId()).thenReturn(id);
        when(mockPackagingProvider.isDefault()).thenReturn(isDefault);
    }

    @Test
    public void testGetAllPackagingProviders() {
        // Invoke
        final Collection<PackagingProvider> packagingProviders = registry
                .getAllPackagingProviders();

        // Check
        final List<PackagingProvider> expectedProviders = Arrays.asList(
                mockCoreJarPackaging, mockCustomJarPackaging, mockWarPackaging);
        assertEquals(expectedProviders.size(), packagingProviders.size());
        assertTrue(packagingProviders.containsAll(expectedProviders));
    }

    @Test
    public void testGetDefaultPackagingProviderWhenACustomIsDefault() {
        assertEquals(mockCustomJarPackaging,
                registry.getDefaultPackagingProvider());
    }

    @Test
    public void testGetDefaultPackagingProviderWhenNoCustomIsDefault() {
        when(mockCustomJarPackaging.isDefault()).thenReturn(false);
        assertEquals(mockCoreJarPackaging,
                registry.getDefaultPackagingProvider());
    }

    @Test
    public void testGetPackagingProviderByInvalidId() {
        assertNull(registry.getPackagingProvider("no-such-provider"));
    }

    @Test
    public void testGetPackagingProviderByValidId() {
        assertEquals(mockCustomJarPackaging,
                registry.getPackagingProvider(CUSTOM_JAR_ID));
    }
}
