package org.sonatype.plugins.yuicompressor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

abstract class AbstractAggregateMojo
    extends AbstractMojo
{
    /**
     * @parameter
     */
    protected String[] sourceFiles;

    /**
     * @parameter
     */
    protected String[] includes;

    /**
     * @parameter
     */
    protected String[] excludes;

    /**
     * Insert line breaks in output after the specified column number.
     * 
     * @parameter expression="${maven.yuicompressor.linebreakpos}" default-value="0"
     */
    protected int linebreakpos;

    /**
     * Aggregate only, no minification.
     * 
     * @parameter expression="${maven.yuicompressor.nominify}" default-value="false"
     */
    protected boolean nominify;

    /**
     * Insert new line after each concatenation.
     * 
     * @parameter default-value="true"
     */
    protected boolean insertNewLine;

    /** @component */
    protected BuildContext buildContext;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        List<File> sources = new ArrayList<File>();

        if ( sourceFiles != null && sourceFiles.length > 0 )
        {
            for ( String sourceFile : sourceFiles )
            {
                sources.add( new File( sourceFile ) );
            }
        }
        else
        {
            Scanner scanner = buildContext.newScanner( getSourceDirectory(), true );
            scanner.setIncludes( includes != null ? includes : getDefaultIncludes() );
            scanner.setExcludes( excludes );
            scanner.addDefaultExcludes();
            scanner.scan();

            for ( String relPath : scanner.getIncludedFiles() )
            {
                sources.add( new File( getSourceDirectory(), relPath ) );
            }
        }

        File output = getOutput();

        // see if there are any changes we need to include in the aggregate
        boolean uptodate = true;
        for ( File source : sources )
        {
            uptodate = buildContext.isUptodate( output, source );
            if ( !uptodate )
            {
                break;
            }
        }

        if ( !uptodate )
        {
            try
            {
                StringWriter buf = new StringWriter();

                for ( File source : sources )
                {
                    Reader in = new BufferedReader( new InputStreamReader( new FileInputStream( source ), "UTF-8" ) );
                    try
                    {
                        // don't minify, simply write directly out to buffer
                        if ( nominify )
                        {
                            IOUtil.copy( in, buf );
                        }
                        // compress away then write out to buffer
                        else
                        {
                            processSourceFile( source, in, buf );
                        }
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

                output.getParentFile().mkdirs();

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
                throw new MojoExecutionException( "Could not create aggregate file", e );
            }
        }
    }

    protected abstract File getOutput();

    protected abstract String[] getDefaultIncludes();

    protected abstract File getSourceDirectory();

    protected abstract void processSourceFile( File source, Reader in, Writer buf )
        throws IOException;

}
