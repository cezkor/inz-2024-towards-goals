package com.example.towardsgoalsapp.etc

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.database.userdata.MutablesArrayContentState
import com.example.towardsgoalsapp.database.userdata.UserDataReputter
import org.junit.Test
import java.util.ArrayList
import com.google.common.truth.Truth.*
import org.junit.Rule

class UserDataReputterTest {

    @Rule
    @JvmField
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    data class DClass (
        val text: String = "text",
        val orderNumber: Long
    )

    class DClassReputter(mArray: ArrayList<MutableLiveData<DClass>>,
                        arrayList: ArrayList<DClass>? = null)
        : UserDataReputter<DClass>(mArray, arrayList) {
        override fun getOrderNumber(userData: DClass): Long = userData.orderNumber

    }

    private val mutArray = ArrayList<MutableLiveData<DClass>>()

    @Test
    fun `elements put as ordered`() {
        mutArray.clear()
        var reputter: DClassReputter = DClassReputter(mutArray)

        var array = arrayListOf<DClass>(DClass("t1", 1),
            DClass("t2", 2), DClass("t3", 3))

        reputter.setWholeBasedOnArrayList(array)
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder()

        mutArray.clear()
        reputter = DClassReputter(mutArray)
        array = arrayListOf<DClass>(DClass("t1", 3),
            DClass("t2", 2), DClass("t3", 1))

        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder(
            java.util.Comparator<Long>{ o1: Long?, o2: Long? -> -o1?.compareTo(o2!!)!!}
            // reverse order
        )
    }

    @Test
    fun `can handle nulled MutableLiveData correctly`() {
        mutArray.clear()
        mutArray.add(MutableLiveData())
        mutArray.add(MutableLiveData())
        mutArray.add(MutableLiveData())
        mutArray.add(MutableLiveData())
        mutArray.add(MutableLiveData())

        var array1 = arrayListOf<DClass>(DClass("t1", 1),
            DClass("t2", 2), DClass("t3", 3))

        var array2 = arrayListOf<DClass>(DClass("t1", 4),
            DClass("t2", 5), DClass("t3", 6))

        var array3 = arrayListOf<DClass>(DClass("t1", 10),
            DClass("t2", 11), DClass("t3", 12))

        var array4 = arrayListOf<DClass>(DClass("t1", 20),
            DClass("t2", 21), DClass("t3", 22))

        val aloneData = DClass("alone", 9)

        var reputter = DClassReputter(mutArray, array1)
        assertThat(mutArray.size).isEqualTo(5)
        assertThat(mutArray[3].value).isNull()
        assertThat(mutArray[4].value).isNull()

        assertThat(reputter.reputBasedOnInsertOfArrayList(array2).first)
            .isEqualTo(MutablesArrayContentState.ADDED_NEW)
        assertThat(mutArray.size).isEqualTo(6)

        assertThat(reputter.reputBasedOnDeleteOf(array1[2]).first)
            .isEqualTo(MutablesArrayContentState.REPUTTED)
        assertThat(mutArray[5].value).isNull()
        assertThat(mutArray[4].value).isEqualTo(array2[2])
        assertThat(mutArray[3].value).isEqualTo(array2[1])
        assertThat(mutArray[2].value).isEqualTo(array2[0])
        assertThat(mutArray[1].value).isEqualTo(array1[1])
        assertThat(mutArray[0].value).isEqualTo(array1[0])

        assertThat(reputter.reputBasedOnInsertOf(aloneData).first)
            .isEqualTo(MutablesArrayContentState.REPUTTED)
        assertThat(mutArray.size).isEqualTo(6)
        assertThat(mutArray[5].value).isEqualTo(aloneData)
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder()

        assertThat(reputter.reputBasedOnInsertOf(array1[2]).first)
            .isEqualTo(MutablesArrayContentState.ADDED_NEW)
        assertThat(mutArray.size).isEqualTo(3 + 3 + 1)
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder()
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).containsExactly(
            1L, 2L, 3L, 4L, 5L, 6L, 9L
        )

        assertThat(reputter.reputBasedOnInsertOfArrayList(array3).first)
            .isEqualTo(MutablesArrayContentState.ADDED_NEW)
        assertThat(mutArray.size).isEqualTo(3 + 3 + 1 + 3)
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder()
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).containsExactly(
            1L, 2L, 3L, 4L, 5L, 6L, 9L, 10L, 11L, 12L
        )

        assertThat(reputter.reputBasedOnDeleteOf(aloneData).first)
            .isEqualTo(MutablesArrayContentState.REPUTTED)
        assertThat(mutArray.size).isEqualTo(10)
        assertThat(mutArray[mutArray.lastIndex].value).isNull()

        assertThat(reputter.reputBasedOnInsertOfArrayList(array4).first)
            .isEqualTo(MutablesArrayContentState.ADDED_NEW)
        assertThat(mutArray.size).isEqualTo(3 + 3 + 3 + 3)
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).isInOrder()
        assertThat(mutArray.map{ it.value?.orderNumber ?: 0 }).containsExactly(
            1L, 2L, 3L, 4L, 5L, 6L, 10L, 11L, 12L, 20L, 21L, 22L
        )

    }

}