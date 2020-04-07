package org.nypl.simplified.cardcreator.network

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.nypl.simplified.cardcreator.models.*
import org.nypl.simplified.cardcreator.utils.Constants
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface CardCreatorService {

  /**
   * Validates the patron's address. Only 'valid-address' and 'non-residential-address'
   * validations will result in a library card. Patrons with addresses that result in a
   * 'non-residential-address' validation will receive 30-day temporary cards if a residential
   * NYS address is not provided.
   *
   * Docs: https://github.com/NYPL-Simplified/card-creator/wiki/API---V2#post-v2validateaddress
   */
  @POST("v2/validate/address")
  suspend fun validateAddress(
    @Body address: Address
  ): ValidateAddressResponse

  /**
   * Validates the patron's username, confirming both its format and its availability.
   *
   * https://github.com/NYPL-Simplified/card-creator/wiki/API---V2#post-v2validateusername
   */
  @POST("v2/validate/username")
  suspend fun validateUsername(
    @Body username: Username
  ): ValidateUsernameResponse

  /**
   * Creates a library card for a patron.
   *
   * https://github.com/NYPL-Simplified/card-creator/wiki/API---V2#post-v2create_patron
   */
  @POST("v2/create_patron")
  suspend fun createPatron(
    @Body patron: Patron
  ): CreatePatronResponse

  companion object {
    operator fun invoke(): CardCreatorService {
      val logging = run {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.apply {
          httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC
        }
      }

      val auth = Interceptor {
        val request = it.request().newBuilder()
          .addHeader("Authorization",
            Credentials.basic("username from cardcreator.conf",
              "password from cardcreator.conf"))
          .build()
        it.proceed(request)
      }

      val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(auth)
        .build()

      return Retrofit.Builder()
        .client(client)
        .baseUrl(Constants.LIBRARY_SIMPLIFIED_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(CardCreatorService::class.java)
    }
  }
}
