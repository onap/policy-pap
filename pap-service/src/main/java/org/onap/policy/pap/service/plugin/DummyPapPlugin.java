/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.service.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.onap.policy.model.pdpclient.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This is just a dummy implementation for the purposes of demonstration, to be removed when a
 * proper implementation is developed
 */
public class DummyPapPlugin implements PapPlugin {

    private static Logger logger = LoggerFactory.getLogger(DummyPapPlugin.class);

    @Override
    public Policy generatePolicy(long policyId, final String policyName, final String policyVersion,
            final Collection<File> policyArtifacts, final Map<String, String> policyMetadata) {
        logger.info("PAP Plugin: Generating policy {} from {}", policyName, policyArtifacts);
        Policy policy = new Policy();
        policy.setPolicyId(Long.toString(policyId));
        policy.setPolicyName(policyName);
        policy.setPolicyVersion(policyVersion);

        List<File> drlFiles = new ArrayList<>();

        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("src/main/resources/pap.properties")) {
            properties.load(input);
        } catch (Exception e) {
            logger.error("Exception Occured while loading properties file" + e);
        }

        for (final File policyArtifact : policyArtifacts) {
            drlFiles.addAll(getDrlFilesFromJar(policyArtifact,
                    new File(properties.getProperty("nexus_download_dir") + "/papPlugin")));
        }
        policy.setPolicyFiles(drlFiles);

        return policy;
    }


    /**
     * Unzips the supplied JAR to a subdirectory.
     *
     * @param theJar The JAR file to unzip
     * @param targetDir The directory to unzip into
     */
    public static List<File> getDrlFilesFromJar(final File theJar, final File targetDir) {

        List<File> drlFiles = new ArrayList<>();

        final byte[] buffer = new byte[100000];

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(theJar))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (fileShouldBeExtracted(zipEntry)) {
                    final File newFile = new File(targetDir.getPath() + File.separator + zipEntry.getName());

                    if (!createDirectoryIfNotExisting(newFile.getParentFile())) {
                        throw new RuntimeException("Cannot create subdirectory for processing.");
                    }

                    try (FileOutputStream fileOutputStream = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, len);
                        }
                        drlFiles.add(newFile);
                    }
                }
                zipEntry = zipInputStream.getNextEntry();
            }

            zipInputStream.closeEntry();

            logger.info("PAP Plugin: Genertated following drl files: {}", drlFiles);
            return drlFiles;

        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static boolean createDirectoryIfNotExisting(final File newFileToBeCreated) {
        if (!newFileToBeCreated.exists()) {
            return newFileToBeCreated.mkdirs();
        }
        return true;
    }

    private static boolean fileShouldBeExtracted(final ZipEntry zipEntry) {
        return !zipEntry.isDirectory() && Paths.get(zipEntry.getName()).toString().endsWith(".drl");
    }


}
