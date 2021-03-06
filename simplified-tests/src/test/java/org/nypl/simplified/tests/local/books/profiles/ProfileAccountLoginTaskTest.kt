package org.nypl.simplified.tests.local.books.profiles

import org.nypl.simplified.tests.books.profiles.ProfileAccountLoginTaskContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ProfileAccountLoginTaskTest : ProfileAccountLoginTaskContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(ProfileAccountLoginTaskTest::class.java)
}
