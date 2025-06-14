package org.lsposed.lspatch.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["appPkgName", "modulePkgName"],
    foreignKeys = [ForeignKey(entity = Module::class, parentColumns = ["pkgName"], childColumns = ["modulePkgName"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["modulePkgName"])]
)
data class Scope(
    val appPkgName: String,
    val modulePkgName: String
)
