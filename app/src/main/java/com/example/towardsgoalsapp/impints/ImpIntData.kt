package com.example.towardsgoalsapp.impints

import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType

data class ImpIntData(

    val impIntId: Long = Constants.IGNORE_ID_AS_LONG,
    val ifText: String = Constants.EMPTY_STRING,
    val thenText: String = Constants.EMPTY_STRING,
    val ownerId: Long = Constants.IGNORE_ID_AS_LONG,
    val typeOfOwner: OwnerType = OwnerType.TYPE_NONE
)

