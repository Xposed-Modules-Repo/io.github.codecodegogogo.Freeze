package com.aistra.hail.work

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.HailData
import java.util.concurrent.TimeUnit

object HWork {
    fun setDeferredFrozen(
        packageName: String,
        frozen: Boolean = true,
        minutes: Long,
        workingMode: String = HailData.workingMode
    ) {
        WorkManager.getInstance(app).enqueueUniqueWork(
            packageName,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<FrozenWorker>().setInputData(
                workDataOf(
                    HailData.KEY_PACKAGE to packageName,
                    HailData.KEY_FROZEN to frozen,
                    HailData.WORKING_MODE to workingMode
                )
            ).setInitialDelay(minutes, TimeUnit.MINUTES).build()
        )
    }
}
