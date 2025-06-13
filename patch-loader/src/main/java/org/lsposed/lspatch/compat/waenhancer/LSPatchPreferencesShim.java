package org.lsposed.lspatch.compat.waenhancer;

import android.content.Context;
import android.util.Log;

import org.lsposed.lspatch.compat.LSPatchBridge;
import org.lsposed.lspatch.compat.LSPatchPreferences;
import org.lsposed.lspd.service.ILSPApplicationService;

/**
 * WaEnhancer LSPatch Preferences compatibility
 * 
 * This class provides the exact preference APIs that WaEnhancer expects
 * to use in LSPatch environments, with fallback mechanisms for different modes.
 */
public class LSPatchPreferencesShim extends LSPatchPreferences {
    
    private static final String TAG = "WaEnhancer-Prefs";
    
    public LSPatchPreferencesShim(String packageName, String prefsName, 
                                 Context context, ILSPApplicationService service) {
        super(packageName, prefsName, context, service);
    }
    
    public LSPatchPreferencesShim(String packageName, Context context, ILSPApplicationService service) {
        super(packageName, context, service);
    }
    
    /**
     * Factory method that WaEnhancer expects
     */
    public static LSPatchPreferencesShim create(String packageName) {
        Context context = LSPatchBridge.getApplicationContext();
        ILSPApplicationService service = LSPatchBridge.getApplicationService();
        
        return new LSPatchPreferencesShim(packageName, context, service);
    }
    
    /**
     * Factory method with custom preferences name
     */
    public static LSPatchPreferencesShim create(String packageName, String prefsName) {
        Context context = LSPatchBridge.getApplicationContext();
        ILSPApplicationService service = LSPatchBridge.getApplicationService();
        
        return new LSPatchPreferencesShim(packageName, prefsName, context, service);
    }
    
    /**
     * Check if preferences are working properly (WaEnhancer API)
     */
    public boolean isWorking() {
        return isAccessible();
    }
    
    /**
     * Get preference file path (WaEnhancer API)
     */
    public String getFilePath() {
        return getPreferencesPath();
    }
    
    /**
     * Force reload preferences (WaEnhancer API)
     */
    public void forceReload() {
        reload();
    }
}
