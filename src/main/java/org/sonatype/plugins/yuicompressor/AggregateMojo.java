package org.sonatype.plugins.yuicompressor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Scanner;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * @goal aggregate
 * @phase process-resources
 */
public class AggregateMojo
    extends AbstractMojo
{

    public static final String[] DEFAULT_INCLUDES = { "**/*.js" };

    /**
     * @parameter default-value="${basedir}/src/main/js"
     */
    private File sourceDirectory;

    /**
     * @parameter
     */
    private String[] includes;

    /**
     * @parameter
     */
    private String[] excludes;

    /**
     * @parameter default-value="${project.build.outputDirectory}/${project.artifactId}-all.js"
     */
    private File output;

    /**
     * Insert line breaks in output after the specified column number.
     * 
     * @parameter expression="${maven.yuicompressor.linebreakpos}" default-value="0"
     */
    private int linebreakpos;

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

    /**
     * Insert new line after each concatenation.
     * 
     * @parameter default-value="true"
     */
    private boolean insertNewLine;

    /** @component */
    private BuildContext buildContext;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // always scan all sources
        Scanner scanner = buildContext.newScanner( sourceDirectory, true );
        scanner.setIncludes( includes != null ? includes : DEFAULT_INCLUDES );
        scanner.setExcludes( excludes );
        scanner.addDefaultExcludes();
        scanner.scan();

        // see if there are any changes we need to include in the aggregate
        boolean uptodate = true;
        for ( String relPath : scanner.getIncludedFiles() )
        {
            uptodate = buildContext.isUptodate( output, new File( sourceDirectory, relPath ) );
            if ( !uptodate )
            {
                break;
            }
        }

        if ( !uptodate )
        {
            ErrorReporter errorReporter = new ErrorReporter()
            {
                public void error( String message, String sourceName, int line, String lineSource, int lineOffset )
                {
                    buildContext.addError( new File( sourceName ), line, lineOffset, message, null );
                }

                public void warning( String message, String sourceName, int line, String lineSource, int lineOffset )
                {
                    buildContext.addWarning( new File( sourceName ), line, lineOffset, message, null );
                }

                public EvaluatorException runtimeError( String message, String sourceName, int line, String lineSource,
                                                        int lineOffset )
                {
                    buildContext.addError( new File( sourceName ), line, lineOffset, message, null );
                    throw new EvaluatorException( message, sourceName, line, lineSource, lineOffset );
                }
            };

            try
            {
                StringWriter buf = new StringWriter();

                for ( String relPath : scanner.getIncludedFiles() )
                {
                    Reader in =
                        new BufferedReader( new InputStreamReader( new FileInputStream( new File( sourceDirectory,
                                                                                                  relPath ) ) ) );
                    try
                    {
                        JavaScriptCompressor compressor = new JavaScriptCompressor( in, errorReporter );
                        compressor.compress( buf, linebreakpos, !nomunge, jswarn, preserveAllSemiColons,
                                             disableOptimizations );
                    }
                    finally
                    {
                        IOUtil.close( in );
                    }
                    if ( insertNewLine )
                    {
                        buf.write( '\n' );
                    }
                }

                OutputStream out = buildContext.newFileOutputStream( output );
                try
                {
                    IOUtil.copy( buf.getBuffer().toString(), out );
                }
                finally
                {
                    IOUtil.close( out );
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Could not create aggregate javascript file", e );
            }
        }
    }

}
