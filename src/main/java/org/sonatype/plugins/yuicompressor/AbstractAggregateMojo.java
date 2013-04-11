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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

abstract class AbstractAggregateMojo
    extends AbstractProcessSourcesMojo
{
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

    @Override
    protected void processSources( List<File> sources )
        throws MojoExecutionException
    {
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

    protected abstract void processSourceFile( File source, Reader in, Writer buf )
        throws IOException;

}
