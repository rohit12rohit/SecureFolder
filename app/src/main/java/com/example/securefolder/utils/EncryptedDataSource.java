package com.example.securefolder.utils;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi; // Import this
import androidx.media3.datasource.BaseDataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// Mark the entire class as using Unstable API
@UnstableApi
public class EncryptedDataSource extends BaseDataSource {

    private final SecretKey mKey;
    private final File mFile;
    private InputStream mInputStream;
    private long mBytesRemaining;
    private boolean mOpened;

    public EncryptedDataSource(File file, SecretKey key, @Nullable TransferListener listener) {
        super(true); // isNetwork = false
        mFile = file;
        mKey = key;
        if (listener != null) {
            addTransferListener(listener);
        }
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        try {
            transferInitializing(dataSpec);

            FileInputStream fis = new FileInputStream(mFile);

            // 1. Read IV (First 12 bytes)
            byte[] iv = new byte[12];
            if (fis.read(iv) != 12) {
                fis.close();
                throw new EOFException("File too short for IV");
            }

            // 2. Initialize Cipher
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, mKey, spec);

            // 3. Wrap in CipherInputStream
            CipherInputStream cis = new CipherInputStream(fis, cipher);
            mInputStream = cis;

            // 4. Handle Seeking
            long skipped = 0;
            if (dataSpec.position > 0) {
                skipped = mInputStream.skip(dataSpec.position);
                if (skipped < dataSpec.position) {
                    throw new EOFException("Could not skip to position");
                }
            }

            // 5. Calculate Length
            // Content Length = FileSize - 12 (IV) - 16 (Tag) - Position
            long contentLength = mFile.length() - 12 - 16;

            if (dataSpec.length != C.LENGTH_UNSET) {
                mBytesRemaining = dataSpec.length;
            } else {
                mBytesRemaining = contentLength - dataSpec.position;
            }

            mOpened = true;
            transferStarted(dataSpec);

            return mBytesRemaining;

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength == 0) return 0;
        if (mBytesRemaining == 0) return C.RESULT_END_OF_INPUT;

        int bytesToRead = (int) Math.min(readLength, mBytesRemaining);
        int bytesRead = mInputStream.read(buffer, offset, bytesToRead);

        if (bytesRead == -1) {
            if (mBytesRemaining > 0) {
                throw new EOFException();
            }
            return C.RESULT_END_OF_INPUT;
        }

        mBytesRemaining -= bytesRead;
        bytesTransferred(bytesRead);
        return bytesRead;
    }

    @Override
    public Uri getUri() {
        return Uri.fromFile(mFile);
    }

    @Override
    public void close() throws IOException {
        if (mInputStream != null) {
            mInputStream.close();
            mInputStream = null;
        }
        if (mOpened) {
            mOpened = false;
            transferEnded();
        }
    }
}