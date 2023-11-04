package com.rusefi.maintenance;

public final class OpenbltJni {
    public interface OpenbltCallbacks
    {
        void log(String line);
        void updateProgress(int percent);
        void error(String line);

        void setPhase(String title, boolean hasProgress);
    }

    static {
        System.loadLibrary("openblt_jni");
    }

    public static native void flashSerial(String filename, String serialPort, OpenbltCallbacks callbacks);
    public static native void flashCan(String filename, OpenbltCallbacks callbacks);
    public static native void stop(OpenbltCallbacks callbacks);
}
