/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package org.sonatype.plugins.yuicompressor;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.sonatype.plexus.build.incremental.BuildContext;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import org.mozilla.javascript.EvaluatorException;

/**
 * Aggregate javascript sources.
 *
 * @goal aggregate
 * @phase process-resources
 */
public class AggregateMojo
    extends AbstractAggregateMojo
{
  public static final String[] DEFAULT_INCLUDES = {"**/*.js"};

  /**
   * @parameter default-value="${basedir}/src/main/js"
   */
  private File sourceDirectory;

  /**
   * @parameter default-value="${project.build.outputDirectory}/${project.artifactId}-all.js"
   */
  private File output;

  /**
   * [js only] Minify only, do not obfuscate.
   *
   * @parameter expression="${maven.yuicompressor.nomunge}" default-value="false"
   */
  private boolean nomunge;

  /**
   * [js only] Preserve unnecessary semicolons.
   *
   * @parameter expression="${maven.yuicompressor.preserveAllSemiColons}" default-value="false"
   */
  private boolean preserveAllSemiColons;

  /**
   * [js only] disable all micro optimizations.
   *
   * @parameter expression="${maven.yuicompressor.disableOptimizations}" default-value="false"
   */
  private boolean disableOptimizations;

  /**
   * [js only] Display possible errors in the code
   *
   * @parameter expression="${maven.yuicompressor.jswarm}" default-value="true"
   */
  private boolean jswarn;

  private class ErrorReporter
      implements org.mozilla.javascript.ErrorReporter
  {
    private final File source;

    public ErrorReporter(File source) {
      this.source = source;
    }

    public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
      buildContext.addMessage(source, line, lineOffset, message, BuildContext.SEVERITY_ERROR, null);
    }

    public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
      buildContext.addMessage(source, line, lineOffset, message, BuildContext.SEVERITY_WARNING, null);
    }

    public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource,
                                           int lineOffset)
    {
      buildContext.addMessage(source, line, lineOffset, message, BuildContext.SEVERITY_ERROR, null);
      throw new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
    }
  }

  ;

  @Override
  protected void processSourceFile(File source, Reader in, Writer buf) throws IOException {
    JavaScriptCompressor compressor = new JavaScriptCompressor(in, new ErrorReporter(source));
    compressor.compress(buf, linebreakpos, !nomunge, jswarn, preserveAllSemiColons, disableOptimizations);
  }

  @Override
  protected String[] getDefaultIncludes() {
    return DEFAULT_INCLUDES;
  }

  @Override
  protected File getOutput() {
    return output;
  }

  @Override
  protected File getSourceDirectory() {
    return sourceDirectory;
  }
}
