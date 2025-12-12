package com.example.securefolder.utils;

import androidx.media3.common.util.UnstableApi; // Import this
import androidx.media3.datasource.DataSource;
import java.io.File;
import javax.crypto.SecretKey;

// Mark class as UnstableApi
@UnstableApi
public class EncryptedDataSourceFactory implements DataSource.Factory {

    private final File mFile;
    private final SecretKey mKey;

    public EncryptedDataSourceFactory(File file, SecretKey key) {
        mFile = file;
        mKey = key;
    }

    @Override
    public DataSource createDataSource() {
        return new EncryptedDataSource(mFile, mKey, null);
    }
}