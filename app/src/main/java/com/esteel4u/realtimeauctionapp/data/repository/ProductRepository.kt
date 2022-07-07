package com.esteel4u.realtimeauctionapp.data.repository

import android.content.ContentValues
import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.esteel4u.realtimeauctionapp.data.model.ProductData
import com.google.android.gms.common.util.DataUtils
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ptrbrynt.firestorelivedata.FirestoreResource
import com.ptrbrynt.firestorelivedata.asLiveData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

class ProductRepository(val lifecycleOwner: LifecycleOwner) {

    private var productAllList = MutableLiveData<List<ProductData>>()
    private var productTodayList = MutableLiveData<List<ProductData>>()
    private var productUserLikeList = MutableLiveData<List<ProductData>>()
    private var auth = Firebase.auth
    private val db = Firebase.firestore


    fun getAllPrdList(): MutableLiveData<List<ProductData>> {
        val myQuery = db.collection("products").orderBy("auctionProgressStatus") .asLiveData<ProductData>()
        myQuery.observe(lifecycleOwner, Observer { resource: FirestoreResource<List<ProductData>> ->
            if(resource.data !== null) {

                productAllList.postValue(resource.data!!)
            }
        })
        return productAllList
    }

    fun getTodayAuctionList(): MutableLiveData<List<ProductData>> {

        val myQuery = db.collection("products").orderBy("auctionProgressStatus") .asLiveData<ProductData>()
        var status = 0
        var alllist: MutableList<ProductData>

        myQuery.observe(lifecycleOwner, Observer { resource: FirestoreResource<List<ProductData>> ->
            if(resource.data !== null) {
                alllist = resource.data!!.toMutableList()
                resource.data?.forEach {

                    val myDocu = db.collection("products").document(it.prdId!!).asLiveData<ProductData>()
                    val localDateTime: LocalDateTime =
                        LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
                    val todaymidnight =
                        Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())

                    val localDateTime1: LocalDateTime =
                        LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)
                    val tomorrowmidnight =
                        Date.from(localDateTime1.atZone(ZoneId.systemDefault()).toInstant())

                    //check is today
                    if((it.startDate!!.toDate().before(todaymidnight) && it.endDate!!.toDate().before(todaymidnight)) ||
                        (it.startDate!!.toDate().after(tomorrowmidnight) && it.endDate!!.toDate().after(tomorrowmidnight))){
                        alllist!!.remove(it)
                    }
                    //진행중
                    else  if (DateUtils.isToday(it.startDate!!.toDate().time )
                        && it.startDate!!.toDate().after(Date())) {
                        status = 1
                    }
                    //진행예정
                    else if (it.startDate!!.toDate().before(Date())
                        && it.endDate!!.toDate().after(Date())) {
                        status = 2

                    }
                    //완료
                    else if (DateUtils.isToday(it.endDate!!.toDate().time)
                        && it.endDate!!.toDate().before(Date())) {
                        status = 3
                    }

                    myDocu.update( "auctionProgressStatus" , status)
                }
                productTodayList.postValue(alllist)
            }
        })
        return productTodayList
    }

    fun getUserLikePrdList(): MutableLiveData<List<ProductData>> {

        val myQuery = db.collection("products").orderBy("auctionProgressStatus") .asLiveData<ProductData>()
        myQuery.observe(lifecycleOwner, Observer { resource: FirestoreResource<List<ProductData>> ->
            if(resource.data !== null) {
                val data: MutableList<ProductData>? = resource.data!!.toMutableList()

                resource.data?.forEach {
                    if(!it.notifyOnUserId!!.contains(auth.uid!!)){
                        data!!.remove(it)
                    }
                }
                productUserLikeList.postValue(data!!)
            }
        })
        return productUserLikeList
    }

    fun updateUserLikePrdList(productData: ProductData){
        var oldList = productData.notifyOnUserId
        var newList = oldList!!.toMutableList()

        if(oldList.contains(auth.uid)){
            newList.remove(auth.uid)
        }else{
            newList.add(auth.uid!!)
        }
//        if(isButtonActive){
//            newList.remove(auth.uid)
//        }else{
//            newList.add(auth.uid!!)
//        }

        val myDocu = db.collection("products").document(productData.prdId!!).asLiveData<ProductData>()
        myDocu.update("notifyOnUserId", newList)
    }


    fun updateAuctionStatus(status: Int, prdId: String){
//        val myDocu = db.collection("products").document(prdId!!).asLiveData<ProductData>()
//        myDocu.update("auctionProgressStatus", status)


    }

}