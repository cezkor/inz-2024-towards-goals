package com.example.towardsgoalsapp.database.userdata

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import org.junit.Test
import com.google.common.truth.Truth.*
import org.junit.Rule
import kotlin.collections.ArrayList

class UserDataArraysManagerTests {

    @Rule
    @JvmField
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    class NextLongGenerator private constructor() {

        companion object {

            private var number: Long = 0L
            fun getNext() : Long {
                number += 1
                return number
            }
        }

    }

    data class DClass (
        val text: String = "text",
        val orderNumber: Long,
        val id: Long = NextLongGenerator.getNext()
    )

    class DClassReputter(mArray: ArrayList<MutableLiveData<DClass>>)
        : UserDataReputter<DClass>(mArray) {
        override fun getOrderNumber(userData: DClass): Long = userData.orderNumber

    }

    class DClassArrayManager(
        private val mtArray: ArrayList<MutableLiveData<DClass>>,
        arrayList: ArrayList<DClass>? = null
    ): UserDataManager<DClass>(mtArray, arrayList) {

        // this code is present in every class of the UserDataManager
        init {
            onInit()
        }
        override fun getIdOf(userData: DClass): Long {
            return userData.id
        }

        override fun getReputter(mtArray: ArrayList<MutableLiveData<DClass>>)
        : UserDataReputter<DClass> {
            return DClassReputter(mtArray)
        }

    }

    private val mutArray = ArrayList<MutableLiveData<DClass>>()

    @Test
    fun `elements put as ordered`() {
        mutArray.clear()

        var manager = DClassArrayManager(mutArray)

        var array = arrayListOf<DClass>(
            DClass("t1", 1),
            DClass("t2", 2), DClass("t3", 3)
        )

        manager.setUserDataArray(array)
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder()

        mutArray.clear()
        manager = DClassArrayManager(mutArray, array)

        manager.setUserDataArray(array)
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder()

        array = arrayListOf<DClass>(
            DClass("t1", 3),
            DClass("t2", 2), DClass("t3", 1)
        )
        manager.setUserDataArray(array)

        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder(
            java.util.Comparator<Long>{ o1: Long?, o2: Long? -> -o1?.compareTo(o2!!)!!}
            // reverse order
        )
    }

    @Test
    fun `can handle ids`() {

        mutArray.clear()
        val manager = DClassArrayManager(mutArray)
        val first = DClass("t1", 1)
        val second = DClass("t2", 2)
        val third = DClass("t3", 3)
        val array = arrayListOf<DClass>(
            first, third, second
        )

        assertThat(manager.getIdOf(first)).isEqualTo(first.id)
        assertThat(manager.getIdOf(second)).isEqualTo(second.id)
        assertThat(manager.getIdOf(third)).isEqualTo(third.id)

        manager.setUserDataArray(array)
        assertThat(mutArray.map { mut -> mut.value?.id })
            .containsExactly(first.id, second.id, third.id)

        assertThat(manager.hasUserDataOfId(first.id)).isTrue()
        assertThat(manager.hasUserDataOfId(second.id)).isTrue()
        assertThat(manager.hasUserDataOfId(third.id)).isTrue()

        manager.deleteOneOldUserDataById(second.id)

        assertThat(manager.addedCount.value).isEqualTo(0)
        assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.REPUTTED)

        assertThat(mutArray.map { mut -> mut.value?.id })
            .containsExactly(first.id, third.id, null)
        assertThat(manager.hasUserDataOfId(first.id)).isTrue()
        assertThat(manager.hasUserDataOfId(third.id)).isTrue()
        assertThat(manager.hasUserDataOfId(second.id)).isFalse()

        manager.updateOneUserData(second)

        assertThat(manager.addedCount.value).isEqualTo(0)
        assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.REPUTTED)

        assertThat(mutArray.map { mut -> mut.value?.id })
            .containsExactly(first.id, second.id, third.id)

        assertThat(manager.hasUserDataOfId(first.id)).isTrue()
        assertThat(manager.hasUserDataOfId(second.id)).isTrue()
        assertThat(manager.hasUserDataOfId(third.id)).isTrue()

        manager.deleteOneOldUserDataById(second.id)

        assertThat(manager.addedCount.value).isEqualTo(0)
        assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.REPUTTED)

        assertThat(mutArray.map { mut -> mut.value?.id })
            .containsExactly(first.id, third.id, null)
        assertThat(manager.hasUserDataOfId(first.id)).isTrue()
        assertThat(manager.hasUserDataOfId(third.id)).isTrue()
        assertThat(manager.hasUserDataOfId(second.id)).isFalse()

        val nonexistentID = -100L
        // see NextLongGenerator - it should never generate negative numbers
        assertThat(nonexistentID).isNotEqualTo(first.id)
        assertThat(nonexistentID).isNotEqualTo(second.id)
        assertThat(nonexistentID).isNotEqualTo(third.id)

        manager.deleteOneOldUserDataById(nonexistentID)

        assertThat(manager.addedCount.value).isEqualTo(0)
        assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.REPUTTED)

        assertThat(mutArray.map { mut -> mut.value?.id })
            .containsExactly(first.id, third.id, null)
        assertThat(manager.hasUserDataOfId(first.id)).isTrue()
        assertThat(manager.hasUserDataOfId(third.id)).isTrue()
        assertThat(manager.hasUserDataOfId(second.id)).isFalse()

        manager.deleteOneOldUserDataById(first.id)

        assertThat(manager.addedCount.value).isEqualTo(0)
        assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.REPUTTED)

        assertThat(mutArray.map { mut -> mut.value?.id })
            .containsExactly(third.id, null, null)
        assertThat(manager.hasUserDataOfId(first.id)).isFalse()
        assertThat(manager.hasUserDataOfId(third.id)).isTrue()
        assertThat(manager.hasUserDataOfId(second.id)).isFalse()

        manager.deleteOneOldUserDataById(first.id)

        assertThat(manager.addedCount.value).isEqualTo(0)
        assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.REPUTTED)

        assertThat(mutArray.map { mut -> mut.value?.id })
            .containsExactly(third.id, null, null)
        assertThat(manager.hasUserDataOfId(first.id)).isFalse()
        assertThat(manager.hasUserDataOfId(third.id)).isTrue()
        assertThat(manager.hasUserDataOfId(second.id)).isFalse()

        manager.deleteOneOldUserDataById(third.id)

        assertThat(manager.addedCount.value).isEqualTo(0)
        assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.REPUTTED)

        assertThat(mutArray.map { mut -> mut.value?.id })
            .containsExactly(null, null, null)
        assertThat(manager.hasUserDataOfId(first.id)).isFalse()
        assertThat(manager.hasUserDataOfId(third.id)).isFalse()
        assertThat(manager.hasUserDataOfId(second.id)).isFalse()

    }

    @Test
    fun `can handle nulled MutableLiveData correctly`() {
        mutArray.clear()
        mutArray.add(MutableLiveData())
        mutArray.add(MutableLiveData())
        mutArray.add(MutableLiveData())
        mutArray.add(MutableLiveData())
        mutArray.add(MutableLiveData())

        var array1 = arrayListOf<DClass>(
            DClass("t1", 1),
            DClass("t2", 2), DClass("t3", 3)
        )

        var array2 = arrayListOf<DClass>(
            DClass("t1", 4),
            DClass("t2", 5), DClass("t3", 6)
        )

        var array3 = arrayListOf<DClass>(
            DClass("t1", 10),
            DClass("t2", 11), DClass("t3", 12)
        )

        var array4 = arrayListOf<DClass>(
            DClass("t1", 20),
            DClass("t2", 21), DClass("t3", 22)
        )

        val aloneData = DClass("alone", 9)

        var manager = DClassArrayManager(mutArray)
        manager.setUserDataArray(array1)
        assertThat(mutArray.size).isEqualTo(5)
        assertThat(mutArray[3].value).isNull()
        assertThat(mutArray[4].value).isNull()

        manager.insertOneUserData(array2[0])
        assertThat(manager.contentState.value)
            .isEqualTo(MutablesArrayContentState.REPUTTED)
        assertThat(manager.addedCount.value).isEqualTo(0)
        manager.insertOneUserData(array2[1])
        assertThat(manager.contentState.value)
            .isEqualTo(MutablesArrayContentState.REPUTTED)
        assertThat(manager.addedCount.value).isEqualTo(0)
        manager.insertOneUserData(array2[2])
        assertThat(manager.contentState.value)
            .isEqualTo(MutablesArrayContentState.ADDED_NEW)
        assertThat(manager.addedCount.value).isEqualTo(1)

        assertThat(mutArray.size).isEqualTo(6)
        assertThat(mutArray[5].value).isEqualTo(array2[2])
        assertThat(mutArray[4].value).isEqualTo(array2[1])
        assertThat(mutArray[3].value).isEqualTo(array2[0])
        assertThat(mutArray[2].value).isEqualTo(array1[2])
        assertThat(mutArray[1].value).isEqualTo(array1[1])
        assertThat(mutArray[0].value).isEqualTo(array1[0])

        manager.deleteOneOldUserDataById(array1[2].id)

        assertThat(mutArray.size).isEqualTo(6)
        assertThat(mutArray[5].value).isNull()
        assertThat(mutArray[4].value).isEqualTo(array2[2])
        assertThat(mutArray[3].value).isEqualTo(array2[1])
        assertThat(mutArray[2].value).isEqualTo(array2[0])
        assertThat(mutArray[1].value).isEqualTo(array1[1])
        assertThat(mutArray[0].value).isEqualTo(array1[0])

        manager.insertOneUserData(aloneData)
        assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.REPUTTED)
        assertThat(manager.addedCount.value).isEqualTo(0)
        assertThat(mutArray.size).isEqualTo(6)
        assertThat(mutArray[5].value).isEqualTo(aloneData)
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder()

        manager.insertOneUserData(array1[2])
        assertThat(manager.addedCount.value).isEqualTo(1)
        assertThat(manager.contentState.value)
            .isEqualTo(MutablesArrayContentState.ADDED_NEW)
        assertThat(mutArray.size).isEqualTo(3 + 3 + 1)
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder()
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).containsExactly(
            1L, 2L, 3L, 4L, 5L, 6L, 9L
        )

        for (i in array3.indices) {
            manager.insertOneUserData(array3[i])
            assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.ADDED_NEW)
            assertThat(manager.addedCount.value).isEqualTo(1)
        }
        assertThat(mutArray.size).isEqualTo(3 + 3 + 1 + 3)
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder()
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).containsExactly(
            1L, 2L, 3L, 4L, 5L, 6L, 9L, 10L, 11L, 12L
        )

        manager.deleteOneOldUserDataById(aloneData.id)
        assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.REPUTTED)
        assertThat(manager.addedCount.value).isEqualTo(0)
        assertThat(mutArray.size).isEqualTo(10)
        assertThat(mutArray[mutArray.lastIndex].value).isNull()

        for (i in array4.indices) {
            manager.insertOneUserData(array4[i])
            if (i == 0) {
                assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.REPUTTED)
                assertThat(manager.addedCount.value).isEqualTo(0)
            }
            else {
                assertThat(manager.contentState.value).isEqualTo(MutablesArrayContentState.ADDED_NEW)
                assertThat(manager.addedCount.value).isEqualTo(1)
            }
        }
        assertThat(mutArray.size).isEqualTo(3 + 3 + 3 + 3)
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder()
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).containsExactly(
            1L, 2L, 3L, 4L, 5L, 6L, 10L, 11L, 12L, 20L, 21L, 22L
        )

    }

}