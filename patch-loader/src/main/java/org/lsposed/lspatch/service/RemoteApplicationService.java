package org.lsposed.lspatch.service;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.widget.Toast;

import org.lsposed.lspatch.share.Constants;
import org.lsposed.lspd.models.Module;
import org.lsposed.lspd.service.ILSPApplicationService;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemoteApplicationService implements ILSPApplicationService {

    private static final String TAG = "LSPatch";
    private static final String MODULE_SERVICE = Constants.MANAGER_PACKAGE_NAME + ".manager.ModuleService";

    private volatile ILSPApplicationService service;
    private final Context context;

    @SuppressLint("DiscouragedPrivateApi")
    public RemoteApplicationService(Context context) throws RemoteException {
        this.context = context;

        // Set system properties to indicate LSPatch manager mode
        setLSPatchSystemProperties();

        try {
            var intent = new Intent()
                    .setComponent(new ComponentName(Constants.MANAGER_PACKAGE_NAME, MODULE_SERVICE))
                    .putExtra("packageName", context.getPackageName());
            // TODO: Authentication
            var latch = new CountDownLatch(1);
            var conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder binder) {
                    Log.i(TAG, "Manager binder received");
                    service = Stub.asInterface(binder);
                    latch.countDown();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.e(TAG, "Manager service died");
                    service = null;
                }
            };
            Log.i(TAG, "Request manager binder");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.bindService(intent, Context.BIND_AUTO_CREATE, Executors.newSingleThreadExecutor(), conn);
            } else {
                var handlerThread = new HandlerThread("RemoteApplicationService");
                handlerThread.start();
                var handler = new Handler(handlerThread.getLooper());
                var contextImplClass = context.getClass();
                var getUserMethod = contextImplClass.getMethod("getUser");
                var bindServiceAsUserMethod = contextImplClass.getDeclaredMethod(
                        "bindServiceAsUser", Intent.class, ServiceConnection.class, int.class, Handler.class, UserHandle.class);
                var userHandle = (UserHandle) getUserMethod.invoke(context);
                bindServiceAsUserMethod.invoke(context, intent, conn, Context.BIND_AUTO_CREATE, handler, userHandle);
            }
            boolean success = latch.await(1, TimeUnit.SECONDS);
            if (!success) throw new TimeoutException("Bind service timeout");

            Log.i(TAG, "RemoteApplicationService initialized successfully");

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 InterruptedException | TimeoutException e) {
            Toast.makeText(context, "Unable to connect to Manager", Toast.LENGTH_SHORT).show();
            var r = new RemoteException("Failed to get manager binder");
            r.initCause(e);
            throw r;
        }
    }

    /**
     * Set system properties to help modules detect LSPatch manager mode
     */
    private void setLSPatchSystemProperties() {
        try {
            System.setProperty("lspatch.mode", "manager");
            System.setProperty("lspatch.manager", "true");
            System.setProperty("lspatch.remote", "true");
            System.setProperty("lspatch.version", "1.0.0");
            Log.d(TAG, "LSPatch manager mode system properties set");
        } catch (Exception e) {
            Log.w(TAG, "Could not set all LSPatch system properties: " + e.getMessage());
        }
    }

    @Override
    public boolean isLogMuted() throws RemoteException {
        return false;
    }

    @Override
    public List<Module> getLegacyModulesList() throws RemoteException {
        List<Module> modules = service == null ? new ArrayList<>() : service.getLegacyModulesList();
        Log.d(TAG, "getLegacyModulesList called (RemoteService), returning " + modules.size() + " modules");

        // Enhanced logging for module detection in manager mode
        for (Module module : modules) {
            Log.d(TAG, "Remote module available: packageName=" + module.packageName +
                    ", apkPath=" + module.apkPath);
        }

        return modules;
    }

    @Override
    public List<Module> getModulesList() throws RemoteException {
        return service == null ? new ArrayList<>() : service.getModulesList();
    }

    @Override
    public String getPrefsPath(String packageName) {
        String prefsPath = new File(Environment.getDataDirectory(), "data/" + packageName + "/shared_prefs/").getAbsolutePath();
        Log.d(TAG, "getPrefsPath called (RemoteService) for " + packageName + ", returning: " + prefsPath);
        return prefsPath;
    }

    @Override
    public IBinder asBinder() {
        return service == null ? null : service.asBinder();
    }

    @Override
    public ParcelFileDescriptor requestInjectedManagerBinder(List<IBinder> binder) {
        return null;
    }

    /**
     * Enhanced method for remote service diagnostics
     */
    public String getDetailedStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== LSPatch RemoteApplicationService Status ===\n");
        status.append("Service Type: RemoteApplicationService (Manager Mode)\n");
        status.append("Context Package: ").append(context.getPackageName()).append("\n");
        status.append("Remote Service Connected: ").append(service != null).append("\n");

        if (service != null) {
            try {
                List<Module> modules = getLegacyModulesList();
                status.append("Modules Available: ").append(modules.size()).append("\n");

                status.append("\nModule Details:\n");
                for (Module module : modules) {
                    status.append("- Package: ").append(module.packageName).append("\n");
                    status.append("  APK Path: ").append(module.apkPath).append("\n");
                    status.append("  App ID: ").append(module.appId).append("\n");
                    status.append("\n");
                }
            } catch (RemoteException e) {
                status.append("Error getting modules: ").append(e.getMessage()).append("\n");
            }
        } else {
            status.append("Remote service is not connected\n");
        }

        status.append("\nSystem Properties:\n");
        String[] lspatchProps = {
                "lspatch.mode", "lspatch.manager", "lspatch.remote", "lspatch.version"
        };

        for (String prop : lspatchProps) {
            String value = System.getProperty(prop);
            if (value != null) {
                status.append("- ").append(prop).append("=").append(value).append("\n");
            }
        }

        return status.toString();
    }

    /**
     * Check if a specific module package is available through remote service
     */
    public boolean isModuleLoaded(String packageName) {
        if (service == null) {
            Log.w(TAG, "Cannot check module " + packageName + ": remote service not connected");
            return false;
        }

        try {
            List<Module> modules = getLegacyModulesList();
            for (Module module : modules) {
                if (packageName.equals(module.packageName)) {
                    Log.d(TAG, "Module " + packageName + " is available through remote service");
                    return true;
                }
            }
            Log.d(TAG, "Module " + packageName + " is NOT available through remote service");
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error checking if module " + packageName + " is loaded: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get module information for a specific package through remote service
     */
    public Module getModuleInfo(String packageName) {
        if (service == null) {
            return null;
        }

        try {
            List<Module> modules = getLegacyModulesList();
            for (Module module : modules) {
                if (packageName.equals(module.packageName)) {
                    return module;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting module info for " + packageName + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * Enhanced health check method for remote service
     */
    public boolean performHealthCheck() {
        try {
            // Verify context is available
            if (context == null) {
                Log.w(TAG, "Health check failed: context is null");
                return false;
            }

            // Verify remote service is connected
            if (service == null) {
                Log.w(TAG, "Health check failed: remote service not connected");
                return false;
            }

            // Try to call a simple method on the remote service
            boolean logMuted = isLogMuted();

            // Try to get modules list
            List<Module> modules = getLegacyModulesList();
            if (modules == null) {
                Log.w(TAG, "Health check failed: remote service returned null modules list");
                return false;
            }

            Log.i(TAG, "Health check passed: Remote service is functional");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Health check failed with exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if the remote service connection is healthy
     */
    public boolean isServiceConnected() {
        return service != null;
    }
}
