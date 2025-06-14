package org.lsposed.lspatch.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.lsposed.lspd.service.ILSPApplicationService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;

/**
 * LSPatch compatible preferences implementation
 * 
 * This class provides a bridge between XSharedPreferences and LSPatch's
 * preference system, allowing modules to access preferences regardless
 * of the LSPatch mode (embedded or manager).
 */
public class LSPatchPreferences {
    
    private static final String TAG = "LSPatch-Prefs";
    
    private final String packageName;
    private final String prefsName;
    private final Context context;
    private final ILSPApplicationService service;
    
    private XSharedPreferences xSharedPrefs;
    private SharedPreferences sharedPrefs;
    private Properties filePrefs;
    private File prefsFile;
    
    public LSPatchPreferences(String packageName, String prefsName, 
                             Context context, ILSPApplicationService service) {
        this.packageName = packageName;
        this.prefsName = prefsName;
        this.context = context;
        this.service = service;
        
        initializePreferences();
    }
    
    public LSPatchPreferences(String packageName, Context context, ILSPApplicationService service) {
        this(packageName, packageName + "_preferences", context, service);
    }
    
    private void initializePreferences() {
        LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
        
        try {
            switch (mode) {
                case CLASSIC_XPOSED:
                    initXSharedPreferences();
                    break;
                    
                case LSPATCH_MANAGER:
                    initManagerModePreferences();
                    break;
                    
                case LSPATCH_EMBEDDED:
                    initEmbeddedModePreferences();
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing preferences for mode " + mode + ": " + e.getMessage());
            // Fallback to file-based preferences
            initFileBasedPreferences();
        }
    }
    
    private void initXSharedPreferences() {
        try {
            xSharedPrefs = new XSharedPreferences(packageName, prefsName);
            xSharedPrefs.makeWorldReadable();
            
            Log.d(TAG, "Using XSharedPreferences for " + packageName);
        } catch (Exception e) {
            Log.w(TAG, "Could not initialize XSharedPreferences: " + e.getMessage());
            initFileBasedPreferences();
        }
    }
    
    private void initManagerModePreferences() {
        try {
            if (service != null) {
                String prefsPath = service.getPrefsPath(packageName);
                if (prefsPath != null) {
                    prefsFile = new File(prefsPath, prefsName + ".xml");
                    loadFilePreferences();
                    Log.d(TAG, "Using manager mode preferences for " + packageName);
                    return;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not access manager preferences: " + e.getMessage());
        }
        
        // Fallback to shared preferences if available
        if (context != null) {
            try {
                // Use MODE_PRIVATE instead of deprecated MODE_WORLD_READABLE for security
                // If cross-process access is needed, use ContentProvider or other secure methods
                sharedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                Log.d(TAG, "Using SharedPreferences fallback for " + packageName);
            } catch (Exception e) {
                Log.w(TAG, "SharedPreferences fallback failed: " + e.getMessage());
                initFileBasedPreferences();
            }
        } else {
            initFileBasedPreferences();
        }
    }
    
    private void initEmbeddedModePreferences() {
        // In embedded mode, try SharedPreferences first
        if (context != null) {
            try {
                // Use MODE_PRIVATE instead of deprecated MODE_WORLD_READABLE for security
                sharedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                Log.d(TAG, "Using SharedPreferences for embedded mode: " + packageName);
                return;
            } catch (Exception e) {
                Log.w(TAG, "Could not use SharedPreferences: " + e.getMessage());
            }
        }
        
        // Fallback to file-based
        initFileBasedPreferences();
    }
    
    private void initFileBasedPreferences() {
        try {
            // Create preferences directory
            File prefsDir;
            if (context != null) {
                prefsDir = new File(context.getDataDir(), "shared_prefs");
            } else {
                prefsDir = new File("/data/data/" + packageName + "/shared_prefs");
            }
            
            if (!prefsDir.exists()) {
                prefsDir.mkdirs();
            }
            
            prefsFile = new File(prefsDir, prefsName + ".xml");
            loadFilePreferences();
            
            Log.d(TAG, "Using file-based preferences: " + prefsFile.getAbsolutePath());
            
        } catch (Exception e) {
            Log.e(TAG, "Could not initialize file-based preferences: " + e.getMessage());
            // Create empty Properties as last resort
            filePrefs = new Properties();
        }
    }
    
    private void loadFilePreferences() {
        filePrefs = new Properties();
        
        if (prefsFile != null && prefsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(prefsFile)) {
                filePrefs.load(fis);
            } catch (IOException e) {
                Log.w(TAG, "Could not load preferences file: " + e.getMessage());
            }
        }
    }
    
    private void saveFilePreferences() {
        if (prefsFile != null && filePrefs != null) {
            try (FileOutputStream fos = new FileOutputStream(prefsFile)) {
                filePrefs.store(fos, "LSPatch Preferences for " + packageName);
            } catch (IOException e) {
                Log.e(TAG, "Could not save preferences file: " + e.getMessage());
            }
        }
    }
    
    // Public API methods
    
    public String getString(String key, String defaultValue) {
        try {
            if (xSharedPrefs != null) {
                return xSharedPrefs.getString(key, defaultValue);
            } else if (sharedPrefs != null) {
                return sharedPrefs.getString(key, defaultValue);
            } else if (filePrefs != null) {
                return filePrefs.getProperty(key, defaultValue);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting string preference " + key + ": " + e.getMessage());
        }
        
        return defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            if (xSharedPrefs != null) {
                return xSharedPrefs.getBoolean(key, defaultValue);
            } else if (sharedPrefs != null) {
                return sharedPrefs.getBoolean(key, defaultValue);
            } else if (filePrefs != null) {
                String value = filePrefs.getProperty(key);
                return value != null ? Boolean.parseBoolean(value) : defaultValue;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting boolean preference " + key + ": " + e.getMessage());
        }
        
        return defaultValue;
    }
    
    public int getInt(String key, int defaultValue) {
        try {
            if (xSharedPrefs != null) {
                return xSharedPrefs.getInt(key, defaultValue);
            } else if (sharedPrefs != null) {
                return sharedPrefs.getInt(key, defaultValue);
            } else if (filePrefs != null) {
                String value = filePrefs.getProperty(key);
                return value != null ? Integer.parseInt(value) : defaultValue;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting int preference " + key + ": " + e.getMessage());
        }
        
        return defaultValue;
    }
    
    public long getLong(String key, long defaultValue) {
        try {
            if (xSharedPrefs != null) {
                return xSharedPrefs.getLong(key, defaultValue);
            } else if (sharedPrefs != null) {
                return sharedPrefs.getLong(key, defaultValue);
            } else if (filePrefs != null) {
                String value = filePrefs.getProperty(key);
                return value != null ? Long.parseLong(value) : defaultValue;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting long preference " + key + ": " + e.getMessage());
        }
        
        return defaultValue;
    }
    
    public float getFloat(String key, float defaultValue) {
        try {
            if (xSharedPrefs != null) {
                return xSharedPrefs.getFloat(key, defaultValue);
            } else if (sharedPrefs != null) {
                return sharedPrefs.getFloat(key, defaultValue);
            } else if (filePrefs != null) {
                String value = filePrefs.getProperty(key);
                return value != null ? Float.parseFloat(value) : defaultValue;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting float preference " + key + ": " + e.getMessage());
        }
        
        return defaultValue;
    }
    
    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        // Note: XSharedPreferences and file-based prefs don't support string sets natively
        // This would need custom serialization
        try {
            if (sharedPrefs != null) {
                return sharedPrefs.getStringSet(key, defaultValue);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting string set preference " + key + ": " + e.getMessage());
        }
        
        return defaultValue;
    }
    
    public boolean contains(String key) {
        try {
            if (xSharedPrefs != null) {
                return xSharedPrefs.contains(key);
            } else if (sharedPrefs != null) {
                return sharedPrefs.contains(key);
            } else if (filePrefs != null) {
                return filePrefs.containsKey(key);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking if preference contains " + key + ": " + e.getMessage());
        }
        
        return false;
    }
    
    public Map<String, ?> getAll() {
        try {
            if (xSharedPrefs != null) {
                return xSharedPrefs.getAll();
            } else if (sharedPrefs != null) {
                return sharedPrefs.getAll();
            } else if (filePrefs != null) {
                Map<String, Object> result = new HashMap<>();
                for (String key : filePrefs.stringPropertyNames()) {
                    result.put(key, filePrefs.getProperty(key));
                }
                return result;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting all preferences: " + e.getMessage());
        }
        
        return new HashMap<>();
    }
    
    // Write operations (for embedded mode with SharedPreferences or file-based)
    
    public Editor edit() {
        return new Editor();
    }
    
    public class Editor {
        private Map<String, Object> pendingChanges = new HashMap<>();
        
        public Editor putString(String key, String value) {
            pendingChanges.put(key, value);
            return this;
        }
        
        public Editor putBoolean(String key, boolean value) {
            pendingChanges.put(key, value);
            return this;
        }
        
        public Editor putInt(String key, int value) {
            pendingChanges.put(key, value);
            return this;
        }
        
        public Editor putLong(String key, long value) {
            pendingChanges.put(key, value);
            return this;
        }
        
        public Editor putFloat(String key, float value) {
            pendingChanges.put(key, value);
            return this;
        }
        
        public Editor remove(String key) {
            pendingChanges.put(key, null);
            return this;
        }
        
        public Editor clear() {
            pendingChanges.clear();
            if (filePrefs != null) {
                filePrefs.clear();
            }
            return this;
        }
        
        public boolean commit() {
            return apply();
        }
        
        public boolean apply() {
            try {
                if (sharedPrefs != null) {
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    for (Map.Entry<String, Object> entry : pendingChanges.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        
                        if (value == null) {
                            editor.remove(key);
                        } else if (value instanceof String) {
                            editor.putString(key, (String) value);
                        } else if (value instanceof Boolean) {
                            editor.putBoolean(key, (Boolean) value);
                        } else if (value instanceof Integer) {
                            editor.putInt(key, (Integer) value);
                        } else if (value instanceof Long) {
                            editor.putLong(key, (Long) value);
                        } else if (value instanceof Float) {
                            editor.putFloat(key, (Float) value);
                        }
                    }
                    return editor.commit();
                    
                } else if (filePrefs != null) {
                    for (Map.Entry<String, Object> entry : pendingChanges.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        
                        if (value == null) {
                            filePrefs.remove(key);
                        } else {
                            filePrefs.setProperty(key, value.toString());
                        }
                    }
                    saveFilePreferences();
                    return true;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error applying preference changes: " + e.getMessage());
            }
            
            return false;
        }
    }
    
    /**
     * Reload preferences from disk (useful for XSharedPreferences)
     */
    public void reload() {
        try {
            if (xSharedPrefs != null) {
                xSharedPrefs.reload();
            } else if (filePrefs != null) {
                loadFilePreferences();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reloading preferences: " + e.getMessage());
        }
    }
    
    /**
     * Check if preferences are accessible
     */
    public boolean isAccessible() {
        return (xSharedPrefs != null || sharedPrefs != null || filePrefs != null);
    }
    
    /**
     * Get the preferences file path
     */
    public String getPreferencesPath() {
        if (prefsFile != null) {
            return prefsFile.getAbsolutePath();
        }
        return null;
    }
}
