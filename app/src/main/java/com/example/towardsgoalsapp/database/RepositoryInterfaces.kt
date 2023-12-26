package com.example.towardsgoalsapp.database

import com.example.towardsgoalsapp.OwnerType

interface RepositoryInterface {

    suspend fun getOneById(id: Long): Any?

    suspend fun deleteById(id: Long)

}

interface UserDataInterface: RepositoryInterface {
    suspend fun updateTexts(id:Long, firstText: String, secondText: String)

    suspend fun markEditing(id: Long, isUnfinished: Boolean)

}

interface OwnedByTypedOwnerUserData: UserDataInterface {

    suspend fun getAllByOwnerTypeAndId(ownerType: OwnerType, ownerId: Long): Any?

}

interface OwnedByOneTypeOnlyOwnerUserData: UserDataInterface {

    suspend fun getAllByOwnerId(ownerId: Long): Any?

}