package com.replock.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings

object PermissionUtils {
    /** PACKAGE_USAGE_STATS is a special app-op: must be granted from system settings. */
    fun hasUsageStatsAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** SYSTEM_ALERT_WINDOW is a special app-op: must be granted from system settings. */
    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun hasAllBlockingPermissions(context: Context): Boolean =
        hasUsageStatsAccess(context) && hasOverlayPermission(context)
}
