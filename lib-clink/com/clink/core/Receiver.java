package com.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author:zhumeng
 * @desc:
 **/
public interface Receiver extends Closeable {
    boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException;


}
