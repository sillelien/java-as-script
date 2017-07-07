package com.sillelien.dollar.relproxy.impl.jproxy.core.clsmgr.comp.jfo;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * @author jmarranz
 */
public class JavaFileObjectInputClassInFile extends JavaFileObjectInputClassInFileSystem {
    protected File file;

    public JavaFileObjectInputClassInFile(File file, String binaryName, @NotNull URI uri) {
        super(binaryName, uri, uri.getPath());
        this.file = file;
    }

    @NotNull
    @Override
    public InputStream openInputStream() throws IOException {
        // Podríamos hacer uri.toURL().openStream() pero si tenemos el File es para algo
        return new BufferedInputStream(new FileInputStream(file), 10 * 1024);
    }

    @Override
    public long getLastModified() {
        return file.lastModified();
    }

    @NotNull
    @Override
    public String toString() {
        return "JavaFileObjectInputClassInFile{uri=" + uri + '}';
    }
}
