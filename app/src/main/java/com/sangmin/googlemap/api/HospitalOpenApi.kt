package com.sangmin.googlemap.api

import com.sangmin.googlemap.data.Hospital
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

class HospitalOpenApi {
    companion object {
        val DOMAIN ="http://openapi.seoul.go.kr:8088/"
        val API_KEY = "737375764564687737305168776473"
    }
}

interface HospitalOpenService {
    @GET("{api_key}/json/TvEmgcHospitalInfo/1/{end}")
    fun getHospital(@Path("api_key") key:String, @Path("end")limit:Int) : Call<Hospital>
}
