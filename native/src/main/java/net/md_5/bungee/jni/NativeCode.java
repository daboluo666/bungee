package net.md_5.bungee.jni;

import com.google.common.io.ByteStreams;
import net.md_5.bungee.jni.cipher.BungeeCipher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class NativeCode<T>
{

    private final String name;
    private final Class<? extends T> javaImpl;
    private final Class<? extends T> nativeImpl;
    //
    private boolean loaded;
    private boolean enable;

    public NativeCode(String name, Class<? extends T> javaImpl, Class<? extends T> nativeImpl)
    {
        this.name = name;
        this.javaImpl = javaImpl;
        this.nativeImpl = nativeImpl;
    }

    public T newInstance()
    {
        try
        {
            return ( enable ) ? nativeImpl.newInstance() : javaImpl.newInstance();
        } catch ( IllegalAccessException | InstantiationException ex )
        {
            throw new RuntimeException( "Error getting instance", ex );
        }
    }

    public boolean load()
    {
        if ( !loaded && isSupported() )
        {
            String fullName = "bungeecord-" + name;

            try
            {
                System.loadLibrary( fullName );
                enable = loaded = true;
            } catch ( Throwable t )
            {
            }

            if ( !loaded )
            {
                try ( InputStream soFile = BungeeCipher.class.getClassLoader().getResourceAsStream( name + ".so" ) )
                {
                    // Else we will create and copy it to a temp file
                    File temp = File.createTempFile( fullName, ".so" );
                    // Don't leave cruft on filesystem
                    temp.deleteOnExit();

                    try ( OutputStream outputStream = new FileOutputStream( temp ) )
                    {
                        ByteStreams.copy( soFile, outputStream );
                    }

                    System.load( temp.getPath() );
                    enable = loaded = true;
                } catch ( IOException ex )
                {
                    // Can't write to tmp?
                } catch ( UnsatisfiedLinkError ex )
                {
                    System.out.println( "Could not load native library: " + ex.getMessage() );
                }
            }
        }

        return loaded;
    }

    public boolean setEnable(boolean enable) {
        if (loaded) {// Ignore if native not loaded
            boolean enable1 = this.enable;
            this.enable = enable;
            return enable1 ^ enable;
        }
        return false;
    }

    public static boolean isSupported()
    {
        return "Linux".equals( System.getProperty( "os.name" ) ) && "amd64".equals( System.getProperty( "os.arch" ) );
    }
}
