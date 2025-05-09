package com.mbta.tid.mbta_app.android.location

import android.app.PendingIntent
import android.location.Location
import android.os.Looper
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.Executor

class MockFusedLocationProviderClient : FusedLocationProviderClient {
    override fun getApiKey(): ApiKey<Api.ApiOptions.NoOptions> {
        TODO("Not yet implemented")
    }

    override fun getLastLocation(): Task<Location> = Tasks.forCanceled()

    override fun getLastLocation(p0: LastLocationRequest): Task<Location> {
        TODO("Not yet implemented")
    }

    override fun getCurrentLocation(p0: Int, p1: CancellationToken?): Task<Location> {
        TODO("Not yet implemented")
    }

    override fun getCurrentLocation(
        p0: CurrentLocationRequest,
        p1: CancellationToken?,
    ): Task<Location> {
        TODO("Not yet implemented")
    }

    override fun getLocationAvailability(): Task<LocationAvailability> {
        TODO("Not yet implemented")
    }

    override fun requestLocationUpdates(
        p0: LocationRequest,
        p1: Executor,
        p2: LocationListener,
    ): Task<Void> {
        TODO("Not yet implemented")
    }

    override fun requestLocationUpdates(
        p0: LocationRequest,
        p1: LocationListener,
        p2: Looper?,
    ): Task<Void> = Tasks.forCanceled()

    override fun requestLocationUpdates(
        p0: LocationRequest,
        p1: LocationCallback,
        p2: Looper?,
    ): Task<Void> {
        TODO("Not yet implemented")
    }

    override fun requestLocationUpdates(
        p0: LocationRequest,
        p1: Executor,
        p2: LocationCallback,
    ): Task<Void> {
        TODO("Not yet implemented")
    }

    override fun requestLocationUpdates(p0: LocationRequest, p1: PendingIntent): Task<Void> {
        TODO("Not yet implemented")
    }

    override fun removeLocationUpdates(p0: LocationListener): Task<Void> = Tasks.forCanceled()

    override fun removeLocationUpdates(p0: LocationCallback): Task<Void> {
        TODO("Not yet implemented")
    }

    override fun removeLocationUpdates(p0: PendingIntent): Task<Void> {
        TODO("Not yet implemented")
    }

    override fun flushLocations(): Task<Void> {
        TODO("Not yet implemented")
    }

    override fun setMockMode(p0: Boolean): Task<Void> {
        TODO("Not yet implemented")
    }

    override fun setMockLocation(p0: Location): Task<Void> {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java")
    override fun requestDeviceOrientationUpdates(
        p0: DeviceOrientationRequest,
        p1: Executor,
        p2: DeviceOrientationListener,
    ): Task<Void> {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java")
    override fun requestDeviceOrientationUpdates(
        p0: DeviceOrientationRequest,
        p1: DeviceOrientationListener,
        p2: Looper?,
    ): Task<Void> {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java")
    override fun removeDeviceOrientationUpdates(p0: DeviceOrientationListener): Task<Void> {
        TODO("Not yet implemented")
    }
}
