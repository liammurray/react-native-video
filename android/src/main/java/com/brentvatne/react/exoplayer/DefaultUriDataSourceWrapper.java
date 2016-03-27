package com.brentvatne.react.exoplayer;

import android.content.ContentResolver;
import android.content.Context;

import com.google.android.exoplayer.upstream.ContentDataSource;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.TransferListener;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.Assertions;

import java.io.IOException;

/**
 * Work-around to support file in /raw resources
 */
public class DefaultUriDataSourceWrapper implements UriDataSource {

    public DefaultUriDataSource wrapped;

    private final UriDataSource rawResourceDataSource;

    private static final String SCHEME_RAW = ContentResolver.SCHEME_ANDROID_RESOURCE; //"android.resource";


    private boolean isRaw = false;

    private boolean isClosed = true;


    public DefaultUriDataSourceWrapper(Context context, String userAgent) {
        this(context, null, userAgent, false);
    }

    public DefaultUriDataSourceWrapper(Context context, TransferListener listener, String userAgent) {
        this(context, listener, userAgent, false);
    }


    public DefaultUriDataSourceWrapper(Context context, TransferListener listener, String userAgent,
                                       boolean allowCrossProtocolRedirects) {
        this(context, listener,
                new DefaultHttpDataSource(userAgent, null, listener,
                        DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                        DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, allowCrossProtocolRedirects));
    }

    public DefaultUriDataSourceWrapper(Context context, TransferListener listener,
                                       UriDataSource httpDataSource) {
        wrapped = new DefaultUriDataSource(context, listener, httpDataSource);
        rawResourceDataSource = new ContentDataSource(context, listener);
    }


    @Override
    public long open(DataSpec dataSpec) throws IOException {
        Assertions.checkState(isClosed);
        String scheme = dataSpec.uri.getScheme();
        if (SCHEME_RAW.equals(scheme)) {
            isRaw = true;
            return rawResourceDataSource.open(dataSpec);
        } else {
            return wrapped.open(dataSpec);
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return isRaw ? rawResourceDataSource.read(buffer, offset, readLength) : wrapped.read(buffer, offset, readLength);
    }

    @Override
    public String getUri() {
        // TODO return null if closed?
        return isRaw ? rawResourceDataSource.getUri() : wrapped.getUri();
    }

    @Override
    public void close() throws IOException {
        if (isRaw) {
            if (!isClosed) {
                try {
                    rawResourceDataSource.close();
                } finally {
                    isClosed = true;
                }
            }
        } else {
            wrapped.close();
        }

    }

}