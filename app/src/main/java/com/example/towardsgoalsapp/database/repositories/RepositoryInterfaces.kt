package com.example.towardsgoalsapp.database.repositories

import com.example.towardsgoalsapp.OwnerType

interface RepositoryInterface {

    // if allowUnfinished is set to false, this method HAS TO return null
    suspend fun getOneById(id: Long, allowUnfinished: Boolean = true): Any?

    suspend fun deleteById(id: Long)

}

interface UserDataInterface: RepositoryInterface {
    suspend fun updateTexts(id:Long, firstText: String, secondText: String)

    suspend fun markEditing(id: Long, isUnfinished: Boolean)

}

interface OwnedByTypedOwnerUserData: UserDataInterface {

    // if allowUnfinished is set to false, this method HAS NOT TO return unfinished user data
    suspend fun getAllByOwnerTypeAndId(
        ownerType: OwnerType,
        ownerId: Long,
        allowUnfinished: Boolean = true
    ): Any?

}

interface OwnedByOneTypeOnlyOwnerUserData: UserDataInterface {

    // if allowUnfinished is set to false, this method HAS NOT TO return unfinished user data
    suspend fun getAllByOwnerId(ownerId: Long, allowUnfinished: Boolean = true): Any?

}