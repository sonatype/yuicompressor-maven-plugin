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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * @goal jslint
 * @phase process-resources
 */
public class JSLintMojo
    extends AbstractProcessSourcesMojo
{
    public static final String[] DEFAULT_INCLUDES = { "**/*.js" };

    /**
     * @parameter default-value="${basedir}/src/main/js"
     */
    private File sourceDirectory;

    /**
     * JSLint options, a map, where key is jslint option name and value either true or false.
     * 
     * @TODO support non-boolean options
     * @see https://github.com/douglascrockford/JSLint/blob/master/jslint.js
     * @parameter
     */
    private Map<String, String> jslintOptions;

    /**
     * @parameter default-value="true"
     */
    private boolean fail;

    @Override
    protected void processSources( List<File> sources )
        throws MojoExecutionException
    {
        boolean passed = true;
        for ( File source : sources )
        {
            Context cx = Context.enter();
            try
            {
                passed = processSource( source, cx ) && passed;
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Could not execute jslint on " + source, e );
            }
            finally
            {
                Context.exit();
            }
        }

        if ( fail && !passed )
        {
            throw new MojoExecutionException( "There were jslint errors" );
        }

    }

    /**
     * Returns <code>true</code> if all jslint tests passed, <code>false</code> if there were problems.
     */
    protected boolean processSource( File source, Context cx )
        throws IOException, MojoExecutionException
    {
        if ( !buildContext.hasDelta( source ) )
        {
            // limitation of buildcontext api, no way to report errors from previous executions
            return true;
        }

        buildContext.removeMessages( source );

        Scriptable scope = cx.initStandardObjects();

        Reader fr = new InputStreamReader( getClass().getResourceAsStream( "/jslint.js" ) );
        cx.evaluateReader( scope, fr, "jslint.js", 0, null );

        Function jslint = (Function) scope.get( "JSLINT", scope );

        Scriptable options = cx.newObject( scope );
        if ( jslintOptions != null )
        {
            for ( Map.Entry<String, String> option : jslintOptions.entrySet() )
            {
                options.put( option.getKey(), options, toBoolean( option.getValue() ) );
            }
        }

        Object[] jsargs = { loadSource( source ), options };

        boolean passed = (Boolean) jslint.call( cx, scope, scope, jsargs );

        if ( !passed )
        {
            NativeArray errors = (NativeArray) jslint.get( "errors", jslint );

            for ( int i = 0; i < errors.getLength(); i++ )
            {
                Scriptable error = (Scriptable) errors.get( i, errors );
                if ( error == null )
                {
                    // apparent bug in jslint, when "too many errors" is reported, last array element is null
                    continue;
                }
                int line = ( (Number) ScriptableObject.getProperty( error, "line" ) ).intValue();
                int column = ( (Number) ScriptableObject.getProperty( error, "character" ) ).intValue();
                String reason = (String) ScriptableObject.getProperty( error, "reason" );

                int severity = fail ? BuildContext.SEVERITY_ERROR : BuildContext.SEVERITY_WARNING;
                buildContext.addMessage( source, line, column, reason, severity, null );
            }
        }

        return passed;
    }

    private boolean toBoolean( String value )
    {
        return Boolean.parseBoolean( value );
    }

    private String loadSource( File source )
        throws IOException
    {
        InputStream is = new FileInputStream( source );
        try
        {
            return IOUtil.toString( is );
        }
        finally
        {
            is.close();
        }
    }

    @Override
    protected String[] getDefaultIncludes()
    {
        return DEFAULT_INCLUDES;
    }

    @Override
    protected File getSourceDirectory()
    {
        return sourceDirectory;
    }

}
