package org.nypl.simplified.ui.accounts

import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.TextView.BufferType.EDITABLE
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Basic
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput.EMAIL_ADDRESS
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput.NO_INPUT
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput.NUMBER_PAD
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsCancelButtonDisabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsCancelButtonEnabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLoginButtonDisabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLoginButtonEnabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLogoutButtonDisabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLogoutButtonEnabled
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A type representing a set of bound views for each possible type of authentication.
 */

sealed class AccountAuthenticationViewBindings {

  /**
   * The view group representing the root of the view hierarchy for the given views.
   */

  abstract val viewGroup: ViewGroup

  /**
   * "Lock" the form views, preventing the user from interacting with them.
   */

  abstract fun lock()

  /**
   * "Unlock" the form views, allowing the user to interact with them.
   */

  abstract fun unlock()

  /**
   * Clear the views. This method should be called exactly once, and all other methods
   * will raise exceptions after this method has been called.
   */

  abstract fun clear()

  /**
   * Set the status of any relevant login button.
   */

  abstract fun setLoginButtonStatus(status: AccountLoginButtonStatus)

  abstract class Base : AccountAuthenticationViewBindings() {
    private val cleared = AtomicBoolean(false)

    protected abstract fun clearActual()

    override fun clear() {
      if (this.cleared.compareAndSet(false, true)) {
        this.clearActual()
      }
    }
  }

  class ViewsForBasic(
    override val viewGroup: ViewGroup,
    val logo: Button,
    val pass: EditText,
    val passLabel: TextView,
    val showPass: CheckBox,
    val user: EditText,
    val userLabel: TextView,
    val onUsernamePasswordChangeListener: (AccountUsername, AccountPassword) -> Unit,
    val loginButton: Button
  ) : Base() {

    private val userTextListener =
      OnTextChangeListener(onChanged = { _, _, _, _ ->
        this.onUsernamePasswordChangeListener.invoke(
          AccountUsername(this.user.text.toString()),
          AccountPassword(this.pass.text.toString())
        )
      })

    private val passTextListener =
      OnTextChangeListener(onChanged = { _, _, _, _ ->
        this.onUsernamePasswordChangeListener.invoke(
          AccountUsername(this.user.text.toString()),
          AccountPassword(this.pass.text.toString())
        )
      })

    init {

      /*
       * Configure a checkbox listener that shows and hides the password field. Note that
       * this will trigger the "text changed" listener on the password field, so we lock this
       * checkbox during login/logout to avoid any chance of the UI becoming inconsistent.
       */

      this.showPass.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          this.pass.transformationMethod = null
        } else {
          this.pass.transformationMethod = PasswordTransformationMethod.getInstance()
        }
      }

      this.user.addTextChangedListener(this.userTextListener)
      this.pass.addTextChangedListener(this.passTextListener)
    }

    override fun lock() {
      this.user.isEnabled = false
      this.pass.isEnabled = false
      this.showPass.isEnabled = false
    }

    override fun unlock() {
      this.user.isEnabled = true
      this.pass.isEnabled = true
      this.showPass.isEnabled = true
    }

    override fun setLoginButtonStatus(status: AccountLoginButtonStatus) {
      return when (status) {
        is AsLoginButtonEnabled -> {
          this.loginButton.setText(R.string.accountLogin)
          this.loginButton.isEnabled = true
          this.loginButton.setOnClickListener { status.onClick.invoke() }
        }
        AsLoginButtonDisabled -> {
          this.loginButton.setText(R.string.accountLogin)
          this.loginButton.isEnabled = false
        }
        is AsCancelButtonEnabled -> {
          this.loginButton.setText(R.string.accountCancel)
          this.loginButton.isEnabled = true
          this.loginButton.setOnClickListener { status.onClick.invoke() }
        }
        is AsLogoutButtonEnabled -> {
          this.loginButton.setText(R.string.accountLogout)
          this.loginButton.isEnabled = true
          this.loginButton.setOnClickListener { status.onClick.invoke() }
        }
        AsLogoutButtonDisabled -> {
          this.loginButton.setText(R.string.accountLogout)
          this.loginButton.isEnabled = false
        }
        AsCancelButtonDisabled -> {
          this.loginButton.setText(R.string.accountCancel)
          this.loginButton.isEnabled = false
        }
      }
    }

    override fun clearActual() {
      this.user.removeTextChangedListener(this.userTextListener)
      this.pass.removeTextChangedListener(this.passTextListener)
    }

    fun setUserAndPass(
      user: String,
      password: String
    ) {
      this.user.setText(user, EDITABLE)
      this.pass.setText(password, EDITABLE)
    }

    fun isSatisfied(description: Basic): Boolean {
      val noUserRequired =
        description.keyboard == NO_INPUT
      val noPasswordRequired =
        description.passwordKeyboard == NO_INPUT
      val userOk =
        this.user.text.isNotBlank() || noUserRequired
      val passOk =
        this.pass.text.isNotBlank() || noPasswordRequired
      return userOk && passOk
    }

    fun configureFor(description: Basic) {
      /*
       * Configure the presence of the individual fields based on keyboard input values
       * given in the authentication document.
       *
       * TODO: Add the extra input validation for the more precise types such as NUMBER_PAD.
       */

      when (description.keyboard) {
        NO_INPUT -> {
          this.userLabel.visibility = View.GONE
          this.user.visibility = View.GONE
        }
        DEFAULT,
        EMAIL_ADDRESS,
        NUMBER_PAD -> {
          this.userLabel.visibility = View.VISIBLE
          this.user.visibility = View.VISIBLE
        }
      }

      when (description.passwordKeyboard) {
        NO_INPUT -> {
          this.passLabel.visibility = View.GONE
          this.pass.visibility = View.GONE
          this.showPass.visibility = View.GONE
        }
        DEFAULT,
        EMAIL_ADDRESS,
        NUMBER_PAD -> {
          this.passLabel.visibility = View.VISIBLE
          this.pass.visibility = View.VISIBLE
          this.showPass.visibility = View.VISIBLE
        }
      }

      this.userLabel.text =
        description.labels["LOGIN"] ?: this.userLabel.text
      this.passLabel.text =
        description.labels["PASSWORD"] ?: this.passLabel.text
    }

    fun getPassword(): AccountPassword {
      return AccountPassword(this.pass.text.toString())
    }

    fun getUser(): AccountUsername {
      return AccountUsername(this.user.text.toString())
    }

    companion object {
      fun bind(
        viewGroup: ViewGroup,
        onUsernamePasswordChangeListener: (AccountUsername, AccountPassword) -> Unit
      ): ViewsForBasic {
        return ViewsForBasic(
          viewGroup = viewGroup,
          logo = viewGroup.findViewById(R.id.authBasicLogo),
          pass = viewGroup.findViewById(R.id.authBasicPasswordField),
          passLabel = viewGroup.findViewById(R.id.authBasicPasswordLabel),
          user = viewGroup.findViewById(R.id.authBasicUserNameField),
          userLabel = viewGroup.findViewById(R.id.authBasicUserNameLabel),
          showPass = viewGroup.findViewById(R.id.authBasicPasswordShow),
          onUsernamePasswordChangeListener = onUsernamePasswordChangeListener,
          loginButton = viewGroup.findViewById(R.id.authBasicLogin)
        )
      }
    }
  }

  class ViewsForSAML2_0(
    override val viewGroup: ViewGroup
  ) : Base() {

    override fun lock() {
      // Nothing
    }

    override fun unlock() {
      // Nothing
    }

    override fun setLoginButtonStatus(status: AccountLoginButtonStatus) {
      // Nothing
    }

    override fun clearActual() {
      // Nothing
    }

    companion object {
      fun bind(viewGroup: ViewGroup): ViewsForSAML2_0 {
        return ViewsForSAML2_0(viewGroup)
      }
    }
  }

  class ViewsForAnonymous(
    override val viewGroup: ViewGroup
  ) : Base() {

    override fun lock() {
      // Nothing
    }

    override fun unlock() {
      // Nothing
    }

    override fun setLoginButtonStatus(status: AccountLoginButtonStatus) {
      // Nothing
    }

    override fun clearActual() {
      // Nothing
    }

    companion object {
      fun bind(viewGroup: ViewGroup): ViewsForAnonymous {
        return ViewsForAnonymous(viewGroup)
      }
    }
  }

  class ViewsForCOPPAAgeGate(
    override val viewGroup: ViewGroup,
    val over13: Switch
  ) : Base() {

    override fun lock() {
      // Nothing
    }

    override fun unlock() {
      // Nothing
    }

    override fun setLoginButtonStatus(status: AccountLoginButtonStatus) {
      // Nothing
    }

    override fun clearActual() {
      this.over13.setOnClickListener {}
    }

    fun setState(
      isOver13: Boolean,
      onAgeCheckboxClicked: (View) -> Unit
    ) {
      this.over13.setOnClickListener {}
      this.over13.isChecked = isOver13
      this.over13.setOnClickListener(onAgeCheckboxClicked)
      this.over13.isEnabled = true
    }

    companion object {
      fun bind(viewGroup: ViewGroup): ViewsForCOPPAAgeGate {
        return ViewsForCOPPAAgeGate(
          viewGroup = viewGroup,
          over13 = viewGroup.findViewById(R.id.authCOPPASwitch)
        )
      }
    }
  }

  class ViewsForOAuthWithIntermediary(
    override val viewGroup: ViewGroup
  ) : Base() {

    override fun lock() {
      // Nothing
    }

    override fun unlock() {
      // Nothing
    }

    override fun setLoginButtonStatus(status: AccountLoginButtonStatus) {
      // Nothing
    }

    override fun clearActual() {
      // Nothing
    }

    companion object {
      fun bind(viewGroup: ViewGroup): ViewsForOAuthWithIntermediary {
        return ViewsForOAuthWithIntermediary(viewGroup)
      }
    }
  }
}
