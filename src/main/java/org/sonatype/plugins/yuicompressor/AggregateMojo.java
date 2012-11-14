package org.sonatype.plugins.yuicompressor;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * @goal aggregate
 * @phase process-resources
 */
public class AggregateMojo
    extends AbstractAggregateMojo
{
    public static final String[] DEFAULT_INCLUDES = { "**/*.js" };

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

    private ErrorReporter errorReporter = new ErrorReporter()
    {
        public void error( String message, String sourceName, int line, String lineSource, int lineOffset )
        {
            if ( sourceName != null )
            {
                buildContext.addMessage( new File( sourceName ), line, lineOffset, message,
                                         BuildContext.SEVERITY_ERROR, null );
            }
            else
            {
                getLog().error( message );
            }
        }

        public void warning( String message, String sourceName, int line, String lineSource, int lineOffset )
        {
            if ( sourceName != null )
            {
                buildContext.addMessage( new File( sourceName ), line, lineOffset, message,
                                         BuildContext.SEVERITY_WARNING, null );
            }
            else
            {
                getLog().warn( message );
            }
        }

        public EvaluatorException runtimeError( String message, String sourceName, int line, String lineSource,
                                                int lineOffset )
        {
            if ( sourceName != null )
            {
                buildContext.addMessage( new File( sourceName ), line, lineOffset, message,
                                         BuildContext.SEVERITY_ERROR, null );
            }
            throw new EvaluatorException( message, sourceName, line, lineSource, lineOffset );
        }
    };

    @Override
    protected void processSourceFile( Reader in, Writer buf )
        throws IOException
    {
        JavaScriptCompressor compressor = new JavaScriptCompressor( in, errorReporter );
        compressor.compress( buf, linebreakpos, !nomunge, jswarn, preserveAllSemiColons, disableOptimizations );
    }

    @Override
    protected String[] getDefaultIncludes()
    {
        return DEFAULT_INCLUDES;
    }

    @Override
    protected File getOutput()
    {
        return output;
    }

    @Override
    protected File getSourceDirectory()
    {
        return sourceDirectory;
    }
}
