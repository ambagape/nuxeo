/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ftest.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;

/**
 * Tests the dev hot reload.
 *
 * @since 9.3
 */
public class ITDevHotReloadTest extends AbstractTest {

    public static final String NUXEO_RELOAD_PATH = "/sdk/reload";

    protected final static Function<URL, URI> URI_MAPPER = url -> {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new NuxeoException("Unable to map the url to uri", e);
        }
    };

    @Rule
    public final TestName TEST_NAME = new TestName();

    @After
    public void after() {
        RestHelper.cleanup();
        // reset dev.bundles file
        writeToDevBundles("# AFTER TEST: " + TEST_NAME.getMethodName());
    }

    @Test
    public void testEmptyHotReload() {
        writeToDevBundles("# EMPTY HOT RELOAD");
        // test create a document
        String id = RestHelper.createDocument("/", "File", "file", "description");
        assertNotNull(id);
    }

    @Test
    public void testHotReloadVocabulary() {
        deployDevBundle("testHotReloadVocabulary");
        // test fetch create entry
        Map<String, Object> entry = RestHelper.fetchDirectoryEntry("hierarchical", "child2");
        assertNotNull(entry);
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) entry.get("properties");
        assertNotNull(properties);
        assertEquals("root1", properties.get("parent"));
        assertEquals("child2", properties.get("label"));
    }

    @Test
    public void testHotReloadDocumentType() {
        deployDevBundle("testHotReloadDocumentType");
        // test create a document
        String id = RestHelper.createDocument("/", "HotReload", "hot reload",
                Collections.singletonMap("hr:content", "some content"));
        assertNotNull(id);
    }

    protected void deployDevBundle(String relativePaths) {
        // first lookup the absolute paths
        String relativeTestPath = "/ITDevHotReloadTest/" + relativePaths;
        URL url = getClass().getResource(relativeTestPath);
        URI uri = URI_MAPPER.apply(url);
        String absolutePath = Paths.get(uri).toAbsolutePath().toString();
        writeToDevBundles("Bundle:" + absolutePath);
    }

    protected void writeToDevBundles(String line) {
        // post new dev bundles to deploy
        if (!RestHelper.post(NUXEO_RELOAD_PATH, line)) {
            fail("Unable to reload dev bundles, for line=" + line);
        }
        // // locate dev.bundles file
        // Environment env = Environment.getDefault();
        // File devBundles = new File(env.getRuntimeHome(), "dev.bundles");
        // // write to dev.bundles file - this will trigger a hot reload of Nuxeo server
        // try (Writer writer = new FileWriter(devBundles)) {
        // writer.write(line);
        // } catch (IOException ioe) {
        // throw new NuxeoException("Unable to write to dev.bundles file", ioe);
        // }
        // trigger hot reload
        // Response response = CLIENT.get(NUXEO_URL + "/sdk/reload");
        // if (!response.isSuccessful()) {
        // fail("Unable to reload dev bundles");
        // }
    }

}
