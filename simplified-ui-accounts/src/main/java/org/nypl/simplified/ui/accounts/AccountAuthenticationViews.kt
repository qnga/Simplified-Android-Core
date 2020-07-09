package org.nypl.simplified.ui.accounts

import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Anonymous
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Basic
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.COPPAAgeGate
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.OAuthWithIntermediary
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.SAML2_0
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.ui.accounts.AccountAuthenticationViewBindings.ViewsForAnonymous
import org.nypl.simplified.ui.accounts.AccountAuthenticationViewBindings.ViewsForBasic
import org.nypl.simplified.ui.accounts.AccountAuthenticationViewBindings.ViewsForCOPPAAgeGate
import org.nypl.simplified.ui.accounts.AccountAuthenticationViewBindings.ViewsForOAuthWithIntermediary
import org.nypl.simplified.ui.accounts.AccountAuthenticationViewBindings.ViewsForSAML2_0

class AccountAuthenticationViews(
  private val viewGroup: ViewGroup,
  onUsernamePasswordChangeListener: (AccountUsername, AccountPassword) -> Unit
) {

  private val basic: ViewsForBasic =
    ViewsForBasic.bind(
      viewGroup = this.viewGroup.findViewById(R.id.authBasic),
      onUsernamePasswordChangeListener = onUsernamePasswordChangeListener
    )

  private val anonymous: ViewsForAnonymous =
    ViewsForAnonymous.bind(
      this.viewGroup.findViewById(R.id.authAnon))
  private val oAuthWithIntermediary: ViewsForOAuthWithIntermediary =
    ViewsForOAuthWithIntermediary.bind(
      this.viewGroup.findViewById(R.id.authOAuthIntermediary))
  private val saml20: ViewsForSAML2_0 =
    ViewsForSAML2_0.bind(
      this.viewGroup.findViewById(R.id.authSAML))
  private val coppa: ViewsForCOPPAAgeGate =
    ViewsForCOPPAAgeGate.bind(
      this.viewGroup.findViewById(R.id.authCOPPA))

  private val viewGroups =
    listOf<AccountAuthenticationViewBindings>(
      this.basic,
      this.anonymous,
      this.oAuthWithIntermediary,
      this.saml20,
      this.coppa
    )

  fun lock() {
    this.viewGroups.forEach(AccountAuthenticationViewBindings::lock)
  }

  fun unlock() {
    this.viewGroups.forEach(AccountAuthenticationViewBindings::unlock)
  }

  fun clear() {
    this.viewGroups.forEach(AccountAuthenticationViewBindings::clear)
  }

  fun setLoginButtonStatus(status: AccountLoginButtonStatus) {
    this.viewGroups.forEach {
      it.setLoginButtonStatus(status)
    }
  }

  fun showFor(description: AccountProviderAuthenticationDescription) {
    this.viewGroups.forEach { it.viewGroup.visibility = GONE }

    return when (description) {
      is COPPAAgeGate ->
        this.coppa.viewGroup.visibility = VISIBLE
      is Basic -> {
        this.basic.viewGroup.visibility = VISIBLE
        this.basic.configureFor(description)
      }
      is OAuthWithIntermediary ->
        this.oAuthWithIntermediary.viewGroup.visibility = VISIBLE
      Anonymous ->
        this.anonymous.viewGroup.visibility = VISIBLE
      is SAML2_0 ->
        this.saml20.viewGroup.visibility = VISIBLE
    }
  }

  fun setBasicUserAndPass(
    user: String,
    password: String
  ) {
    this.basic.setUserAndPass(
      user = user,
      password = password
    )
  }

  fun isSatisfiedFor(description: AccountProviderAuthenticationDescription): Boolean {
    return when (description) {
      is COPPAAgeGate ->
        true
      is Basic ->
        this.basic.isSatisfied(description)
      is OAuthWithIntermediary ->
        true
      Anonymous ->
        true
      is SAML2_0 ->
        true
    }
  }

  fun setCOPPAState(
    isOver13: Boolean,
    onAgeCheckboxClicked: (View) -> Unit
  ) {
    this.coppa.setState(
      isOver13 = isOver13,
      onAgeCheckboxClicked = onAgeCheckboxClicked
    )
  }

  fun getBasicPassword(): AccountPassword {
    return this.basic.getPassword()
  }

  fun getBasicUser(): AccountUsername {
    return this.basic.getUser()
  }
}
