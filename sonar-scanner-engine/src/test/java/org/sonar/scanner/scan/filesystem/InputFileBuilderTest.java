/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.config.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.PathUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InputFileBuilderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  LanguageDetection langDetection = mock(LanguageDetection.class);
  StatusDetection statusDetection = mock(StatusDetection.class);
  DefaultModuleFileSystem fs = mock(DefaultModuleFileSystem.class);

  @Test
  public void should_detect_charset_from_BOM() {
    File basedir = new File("src/test/resources/org/sonar/scanner/scan/filesystem/");
    when(fs.baseDir()).thenReturn(basedir);
    when(fs.encoding()).thenReturn(StandardCharsets.US_ASCII);
    when(langDetection.language(any(InputFile.class))).thenReturn("java");
    InputFileBuilder builder = new InputFileBuilder("moduleKey", new PathResolver(), langDetection, statusDetection, fs, new MapSettings(), new FileMetadata());

    assertThat(createAndComplete(builder, new File(basedir, "without_BOM.txt")).charset())
      .isEqualTo(StandardCharsets.US_ASCII);
    assertThat(createAndComplete(builder, new File(basedir, "UTF-8.txt")).charset())
      .isEqualTo(StandardCharsets.UTF_8);
    assertThat(createAndComplete(builder, new File(basedir, "UTF-16BE.txt")).charset())
      .isEqualTo(StandardCharsets.UTF_16BE);
    assertThat(createAndComplete(builder, new File(basedir, "UTF-16LE.txt")).charset())
      .isEqualTo(StandardCharsets.UTF_16LE);
    assertThat(createAndComplete(builder, new File(basedir, "UTF-32BE.txt")).charset())
      .isEqualTo(InputFileBuilder.UTF_32BE);
    assertThat(createAndComplete(builder, new File(basedir, "UTF-32LE.txt")).charset())
      .isEqualTo(InputFileBuilder.UTF_32LE);

    try {
      createAndComplete(builder, new File(basedir, "non_existing"));
      Assert.fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Unable to read file " + new File(basedir, "non_existing").getAbsolutePath());
      assertThat(e.getCause()).isInstanceOf(FileNotFoundException.class);
    }
  }

  private static DefaultInputFile createAndComplete(InputFileBuilder builder, File file) {
    DefaultInputFile inputFile = builder.create(file);
    builder.completeAndComputeMetadata(inputFile, InputFile.Type.MAIN);
    return inputFile;
  }

  @Test
  public void complete_input_file() throws Exception {
    // file system
    File basedir = temp.newFolder();
    File srcFile = new File(basedir, "src/main/java/foo/Bar.java");
    FileUtils.touch(srcFile);
    FileUtils.write(srcFile, "single line");
    when(fs.baseDir()).thenReturn(basedir);
    when(fs.encoding()).thenReturn(StandardCharsets.UTF_8);

    // lang
    when(langDetection.language(any(InputFile.class))).thenReturn("java");

    // status
    when(statusDetection.status("foo", "src/main/java/foo/Bar.java", "6c1d64c0b3555892fe7273e954f6fb5a"))
      .thenReturn(InputFile.Status.ADDED);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs, new MapSettings(), new FileMetadata());
    DefaultInputFile inputFile = builder.create(srcFile);
    builder.completeAndComputeMetadata(inputFile, InputFile.Type.MAIN);

    assertThat(inputFile.type()).isEqualTo(InputFile.Type.MAIN);
    assertThat(inputFile.file()).isEqualTo(srcFile.getAbsoluteFile());
    assertThat(inputFile.absolutePath()).isEqualTo(PathUtils.sanitize(srcFile.getAbsolutePath()));
    assertThat(inputFile.language()).isEqualTo("java");
    assertThat(inputFile.key()).isEqualTo("struts:src/main/java/foo/Bar.java");
    assertThat(inputFile.relativePath()).isEqualTo("src/main/java/foo/Bar.java");
    assertThat(inputFile.lines()).isEqualTo(1);
  }

  @Test
  public void return_null_if_file_outside_basedir() throws Exception {
    // file system
    File basedir = temp.newFolder();
    File otherDir = temp.newFolder();
    File srcFile = new File(otherDir, "src/main/java/foo/Bar.java");
    FileUtils.touch(srcFile);
    when(fs.baseDir()).thenReturn(basedir);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs, new MapSettings(), new FileMetadata());
    DefaultInputFile inputFile = builder.create(srcFile);

    assertThat(inputFile).isNull();
  }

  @Test
  public void return_null_if_language_not_detected() throws Exception {
    // file system
    File basedir = temp.newFolder();
    File srcFile = new File(basedir, "src/main/java/foo/Bar.java");
    FileUtils.touch(srcFile);
    FileUtils.write(srcFile, "single line");
    when(fs.baseDir()).thenReturn(basedir);
    when(fs.encoding()).thenReturn(StandardCharsets.UTF_8);

    // lang
    when(langDetection.language(any(InputFile.class))).thenReturn(null);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs, new MapSettings(), new FileMetadata());
    DefaultInputFile inputFile = builder.create(srcFile);
    inputFile = builder.completeAndComputeMetadata(inputFile, InputFile.Type.MAIN);

    assertThat(inputFile).isNull();
  }

}
