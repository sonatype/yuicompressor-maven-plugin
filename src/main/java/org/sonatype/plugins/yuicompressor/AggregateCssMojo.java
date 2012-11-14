package org.sonatype.plugins.yuicompressor;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.yahoo.platform.yui.compressor.CssCompressor;

/**
 * @goal aggregate-css
 * @phase process-resources
 */
public class AggregateCssMojo
    extends AbstractAggregateMojo
{
    public static final String[] DEFAULT_INCLUDES = { "**/*.css" };

    /**
     * @parameter default-value="${basedir}/src/main/css"
     */
    private File sourceDirectory;

    /**
     * @parameter default-value="${project.build.outputDirectory}/${project.artifactId}-all.css"
     */
    private File output;

    @Override
    protected void processSourceFile( Reader in, Writer buf )
        throws IOException
    {
        CssCompressor compressor = new CssCompressor( in );
        compressor.compress( buf, linebreakpos );
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
