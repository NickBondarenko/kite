/**
 * Copyright 2013 Cloudera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.cdk.data.filesystem;

import com.cloudera.cdk.data.DatasetDescriptor;
import com.cloudera.cdk.data.PartitionKey;
import com.cloudera.cdk.data.PartitionStrategy;
import com.google.common.io.Files;
import java.io.IOException;
import java.net.URI;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.cloudera.cdk.data.filesystem.DatasetTestUtilities.USER_SCHEMA;
import static com.cloudera.cdk.data.filesystem.FileSystemDatasetRepository
    .partitionKeyForPath;
import com.cloudera.cdk.data.impl.Accessor;
import org.apache.avro.generic.GenericData.Record;

public class TestFileSystemDatasetPartitionKeyForPath {

  private FileSystem fileSystem;
  private Path testDirectory;
  private PartitionStrategy partitionStrategy;
  private FileSystemDataset<Record> dataset;

  @Before
  public void setUp() throws IOException {
    fileSystem = FileSystem.get(new Configuration());
    testDirectory = fileSystem.makeQualified(
        new Path(Files.createTempDir().getAbsolutePath()));
    partitionStrategy = new PartitionStrategy.Builder()
        .hash("username", "username_part", 2).hash("email", 3).get();

    dataset = new FileSystemDataset.Builder()
        .name("partitioned-users")
        .configuration(new Configuration())
        .descriptor(new DatasetDescriptor.Builder()
            .schema(USER_SCHEMA)
            .location(testDirectory)
            .partitionStrategy(partitionStrategy)
            .get())
        .build();
  }

  @After
  public void tearDown() throws IOException {
    fileSystem.delete(testDirectory, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDifferentFileSystem() throws Exception {
    partitionKeyForPath(dataset, new URI("hdfs://namenode/"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDifferentRootDirectory() throws Exception {
    partitionKeyForPath(dataset, new Path(testDirectory.getParent(), "bogus").toUri());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNonExistentPartitionDirectory() throws Exception {
    partitionKeyForPath(dataset, new Path(testDirectory, "not_a_partition").toUri());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTooManyPartitionDirectories() throws Exception {
    partitionKeyForPath(dataset,
        new Path(testDirectory, "username_part=1/email=2/extra=3").toUri());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPartitionName() throws Exception {
    partitionKeyForPath(dataset, new Path(testDirectory, "not_a_partition=1").toUri());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingPartitionValue() throws Exception {
    partitionKeyForPath(dataset, new Path(testDirectory, "username_part").toUri());
  }

  @Test
  public void testValidPartition() throws Exception {
    PartitionKey key = partitionKeyForPath(dataset,
        new Path(testDirectory, "username_part=1").toUri());
    Assert.assertEquals(Accessor.getDefault().newPartitionKey(1), key);
  }

}
