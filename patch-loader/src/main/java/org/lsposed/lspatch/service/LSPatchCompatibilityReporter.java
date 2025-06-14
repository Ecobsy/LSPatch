package org.lsposed.lspatch.service;

import android.util.Log;

/**
 * LSPatch Compatibility Reporter
 * 
 * This class provides compatibility reporting functionality for LSPatch modules
 */
public class LSPatchCompatibilityReporter {
    private static final String TAG = "LSPatch-CompatReporter";
    
    /**
     * Report compatibility issues
     */
    public static void reportCompatibilityIssue(String module, String issue) {
        Log.w(TAG, "Compatibility issue in " + module + ": " + issue);
    }
    
    /**
     * Report compatibility success
     */
    public static void reportCompatibilitySuccess(String module, String feature) {
        Log.i(TAG, "Compatibility success in " + module + ": " + feature);
    }
}
