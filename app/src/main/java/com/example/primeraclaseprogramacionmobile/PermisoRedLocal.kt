package com.example.primeraclaseprogramacionmobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal fun Context.permisoRedConcedido(): Boolean = Build.VERSION.SDK_INT < 37 ||
    checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
