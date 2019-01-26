package com.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author:zhumeng
 * @desc:
 **/
public interface Sender extends Closeable {
    //“Ï≤Ω∑¢ÀÕ
    boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException;
}
