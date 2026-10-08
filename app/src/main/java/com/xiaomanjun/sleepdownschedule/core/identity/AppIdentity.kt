package com.xiaomanjun.sleepdownschedule.core.identity

object AppIdentity {
    const val PRODUCTION_PACKAGE_NAME = "com.scheduleplus.student"
    const val LEGACY_PACKAGE_NAME = "com.example.courseschedule"
    const val LEGACY_SOURCE_PACKAGE_NAME = "com.xiaomanjun.sleepdownschedule"

    fun isTrustedBackupSource(sourcePackageName: String, currentPackageName: String): Boolean =
        sourcePackageName == currentPackageName || sourcePackageName == LEGACY_PACKAGE_NAME ||
            sourcePackageName == LEGACY_SOURCE_PACKAGE_NAME

    fun requireTrustedBackupSource(sourcePackageName: String, currentPackageName: String) {
        require(isTrustedBackupSource(sourcePackageName, currentPackageName)) {
            "备份来源 package 不受信任"
        }
    }
}
