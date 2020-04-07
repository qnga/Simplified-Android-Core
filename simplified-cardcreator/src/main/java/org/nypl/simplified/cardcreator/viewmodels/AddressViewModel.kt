package org.nypl.simplified.cardcreator.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.models.Address
import org.nypl.simplified.cardcreator.models.ValidateAddressResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.slf4j.LoggerFactory
import java.lang.Exception

class AddressViewModel : ViewModel() {

  private val logger = LoggerFactory.getLogger(AddressViewModel::class.java)
  private val cardCreatorService = CardCreatorService()

  val validateAddressResponse = MutableLiveData<ValidateAddressResponse>()

  fun validateAddress(address: Address) {
    viewModelScope.launch {
      try {
        val response = cardCreatorService.validateAddress(address)
        validateAddressResponse.postValue(response)
      } catch (e: Exception) {
        logger.debug("validateAddress call failed!")
        e.printStackTrace()
      }
    }
  }

}