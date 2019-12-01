package org.nypl.simplified.profiles.api

import org.nypl.simplified.reader.api.ReaderPreferences

/**
 * The preferences for a profile.
 *
 * A _preference_ is distinct from an _attribute_ in that the application is permitted to
 * behave differently based upon the value of a _preference_. Preferences are typed.
 *
 * @see ProfileAttributes
 */

data class ProfilePreferences(

  /**
   * @return The date of birth of the reader (if one has been explicitly specified)
   */

  val dateOfBirth: ProfileDateOfBirth?,

  /**
   * @return `true` if non-production libraries should be displayed
   */

  val showTestingLibraries: Boolean,

  /**
   * @return The reader-specific preferences
   */

  val readerPreferences: ReaderPreferences)