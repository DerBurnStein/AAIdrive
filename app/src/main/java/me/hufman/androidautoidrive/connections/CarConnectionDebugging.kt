package me.hufman.androidautoidrive.connections

import android.content.Context
import android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import io.bimmergestalt.idriveconnectkit.android.IDriveConnectionObserver
import io.bimmergestalt.idriveconnectkit.android.security.KnownSecurityServices
import io.bimmergestalt.idriveconnectkit.android.security.SecurityAccess
import io.github.g00fy2.versioncompare.Version
import me.hufman.androidautoidrive.carapp.music.MusicAppMode

/**
 * Assists in determining prerequisites and difficulties in the car connection
 */
class CarConnectionDebugging(val context: Context, val callback: () -> Unit) {
	companion object {
		const val TAG = "CarDebugging"
		const val SESSION_INIT_TIMEOUT = 1000
		const val BCL_REPORT_TIMEOUT = 1000
		const val BCL_REDRAW_DEBOUNCE = 100
	}

	val deviceName = Settings.Global.getString(context.contentResolver, "device_name") ?: ""

	private val securityAccess = SecurityAccess.getInstance(context).also {
		it.callback = callback
	}

	private val idriveListener = IDriveConnectionObserver { callback() }

	val isConnectedSecurityInstalled
		get() = SecurityAccess.installedSecurityServices.isNotEmpty()

	val isConnectedSecurityConnecting
		get() = securityAccess.isConnecting()

	val isConnectedSecurityConnected
		get() = securityAccess.isConnected()

	val isBMWInstalled
		get() = SecurityAccess.installedSecurityServices.any {
			it.name.startsWith("BMW")
		}

	val isMiniInstalled
		get() = SecurityAccess.installedSecurityServices.any {
			it.name.startsWith("Mini")
		}

	val isJ29Installed
		get() = SecurityAccess.installedSecurityServices.any {
			it.name.startsWith("J29")
		}

	val isBMWConnectedInstalled
		get() = SecurityAccess.installedSecurityServices.any {
			it.name.startsWith("BMWC")
		}

	val isMiniConnectedInstalled
		get() = SecurityAccess.installedSecurityServices.any {
			it.name.startsWith("MiniC")
		}

	val isBMWConnected65Installed: Boolean
		get() = try {
			SecurityAccess.installedSecurityServices.filter {
				it.name.startsWith("BMWC")
			}.any {
				val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					context.packageManager.getPackageInfo(it.packageName, PackageInfoFlags.of(0)).versionName
				} else {
					context.packageManager.getPackageInfo(it.packageName, 0).versionName
				}
				version?.startsWith("6.5") == true
			}
		} catch (e: Exception) { false }

	val isMiniConnected65Installed: Boolean
		get() = try {
			SecurityAccess.installedSecurityServices.filter {
				it.name.startsWith("MiniC")
			}.any {
				val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					context.packageManager.getPackageInfo(it.packageName, PackageInfoFlags.of(0)).versionName
				} else {
					context.packageManager.getPackageInfo(it.packageName, 0).versionName
				}
				version?.startsWith("6.5") == true
			}
		} catch (e: Exception) { false }

	val isBMWMineInstalled
		get() = KnownSecurityServices.entries.any {
			it.name.startsWith("BMWMine") && SecurityAccess.installedSecurityServices.contains(it)
		}
	val isMiniMineInstalled
		get() = KnownSecurityServices.entries.any {
			it.name.startsWith("MiniMine") && SecurityAccess.installedSecurityServices.contains(it)
		}

	val isBMWMineDevicePermission
		get() = KnownSecurityServices.entries.any {
			it.name.startsWith("BMWMine") && isPermissionGranted(it.packageName, "android.permission.BLUETOOTH_CONNECT")
		}

	val isMiniMineDevicePermission
		get() = KnownSecurityServices.entries.any {
			it.name.startsWith("MiniMine") && isPermissionGranted(it.packageName, "android.permission.BLUETOOTH_CONNECT")
		}

	val isBMWMine56Installed
		get() = try {
			SecurityAccess.installedSecurityServices.filter {
				it.name.startsWith("BMWMine")
			}.any {
				val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					context.packageManager.getPackageInfo(it.packageName, PackageInfoFlags.of(0)).versionName
				} else {
					context.packageManager.getPackageInfo(it.packageName, 0).versionName
				}
				val versionObj = Version(version)
				versionObj >= Version("5.6")
			}
		} catch (e: Exception) { false }

	val isMiniMine56Installed
		get() = try {
			SecurityAccess.installedSecurityServices.filter {
				it.name.startsWith("MiniMine")
			}.any {
				val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					context.packageManager.getPackageInfo(it.packageName, PackageInfoFlags.of(0)).versionName
				} else {
					context.packageManager.getPackageInfo(it.packageName, 0).versionName
				}
				val versionObj = Version(version)
				versionObj >= Version("5.6")
			}
		} catch (e: Exception) { false }

	private val btStatus = BtStatus(context) { callback() }
	private val usbStatus = UsbStatus(context) { callback() }

	private var bclNextRedraw: Long = 0
	private val bclListener = BclStatusListener(context) {
		// need to watch for if we are stuck in SESSION_INIT_BYTES_SEND
		// which indicates whether BT Apps is enabled in the car
		if (bclNextRedraw < SystemClock.uptimeMillis()) {
			callback()
			bclNextRedraw = SystemClock.uptimeMillis() + BCL_REDRAW_DEBOUNCE
		}
	}

	// the summarized status
	val isHfConnected
		get() = btStatus.isHfConnected
	val isA2dpConnected
		get() = btStatus.isA2dpConnected
	val isSPPAvailable
		get() = btStatus.isSPPAvailable
	val isBTConnected
		get() = btStatus.isBTConnected

	val isUsbConnected
		get() = usbStatus.isUsbConnected
	val isUsbTransferConnected
		get() = usbStatus.isUsbTransferConnected
	val isUsbAccessoryConnected
		get() = usbStatus.isUsbAccessoryConnected


	// if the BCL tunnel has started
	val isBCLConnecting
		get() = bclListener.state != "UNKNOWN" && bclListener.state != "DETACHED" && bclListener.staleness < BCL_REPORT_TIMEOUT

	// indicates that SESSION_INIT is failing, and the Car's Apps setting is not enabled
	val isBCLStuck
		get() = bclListener.state == "SESSION_INIT_BYTES_SEND" && bclListener.stateAge > SESSION_INIT_TIMEOUT

	val isBCLConnected
		get() = idriveListener.isConnected

	val bclTransport
		get() = bclListener.transport ?: MusicAppMode.TRANSPORT_PORTS.fromPort(idriveListener.port)?.toString()

	val carBrand
		get() = idriveListener.brand

	val btCarBrand
		get() = btStatus.carBrand

	private fun isPermissionGranted(app: String, permission: String): Boolean {
		val packageInfo = try {
			context.packageManager.getPackageInfo(app, GET_PERMISSIONS)
		} catch (e: Exception) {
			return false
		}
		var found = false
		packageInfo.requestedPermissions?.forEachIndexed { index, perm ->
			val flags = packageInfo.requestedPermissionsFlags?.get(index) ?: 0
			val granted = (flags and REQUESTED_PERMISSION_GRANTED) > 0
			if (perm == permission) {
				found = granted
			}
		}
		return found
	}

	fun register() {
		idriveListener.callback = { callback() }
		btStatus.register()
		usbStatus.register()
		bclListener.subscribe()
	}

	fun _registerBtStatus() {
		btStatus.register()
	}
	fun _registerUsbStatus() {
		usbStatus.register()
	}

	fun probeSecurityModules() {
		securityAccess.connect()
	}

	fun unregister() {
		idriveListener.callback = {}
		btStatus.unregister()
		usbStatus.unregister()
		bclListener.unsubscribe()
	}
}