package com.example.securefolder.utils;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
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

@UnstableApi
public class EncryptedDataSource extends BaseDataSource {

    private final SecretKey mKey;
    private final File mFile;
    private CipherInputStream mCipherInputStream;
    private long mBytesRemaining;
    private boolean mOpened;
    private Uri mUri;

    public EncryptedDataSource(File file, SecretKey key, @Nullable TransferListener listener) {
        super(true);
        mFile = file;
        mKey = key;
        if (listener != null) addTransferListener(listener);
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        mUri = dataSpec.uri;
        transferInitializing(dataSpec);

        try {
            FileInputStream fis = new FileInputStream(mFile);
            byte[] iv = new byte[12];
            if (fis.read(iv) != 12) {
                fis.close();
                throw new EOFException("File too short for IV");
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, mKey, spec);

            mCipherInputStream = new CipherInputStream(fis, cipher);

            // GCM SEEKING LIMITATION FIX:
            // If the player asks to seek far ahead, we must skip.
            // WARNING: This is slow for large files but necessary.
            if (dataSpec.position > 0) {
                forceSkip(mCipherInputStream, dataSpec.position);
            }

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

    private void forceSkip(InputStream in, long bytesToSkip) throws IOException {
        long totalSkipped = 0;
        // Skip in smaller chunks to avoid OOM or Thread locks
        byte[] skipBuffer = new byte[8192];

        while (totalSkipped < bytesToSkip) {
            long remaining = bytesToSkip - totalSkipped;
            // Use read() instead of skip() because CipherInputStream implementation of skip() is often buggy
            int read = in.read(skipBuffer, 0, (int) Math.min(skipBuffer.length, remaining));
            if (read == -1) {
                throw new EOFException("End of stream while skipping");
            }
            totalSkipped += read;
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength == 0) return 0;
        if (mBytesRemaining == 0) return C.RESULT_END_OF_INPUT;

        int bytesToRead = (int) Math.min(readLength, mBytesRemaining);
        int bytesRead = mCipherInputStream.read(buffer, offset, bytesToRead);

        if (bytesRead == -1) {
            if (mBytesRemaining > 0) throw new EOFException();
            return C.RESULT_END_OF_INPUT;
        }

        mBytesRemaining -= bytesRead;
        bytesTransferred(bytesRead);
        return bytesRead;
    }

    @Override
    public Uri getUri() { return mUri; }

    @Override
    public void close() throws IOException {
        if (mCipherInputStream != null) {
            mCipherInputStream.close();
            mCipherInputStream = null;
        }
        if (mOpened) {
            mOpened = false;
            transferEnded();
        }
    }
}