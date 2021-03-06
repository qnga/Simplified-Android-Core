package org.nypl.simplified.tests.books.reader.bookmarks

import com.io7m.jfunctional.Option
import org.joda.time.LocalDateTime
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.api.BookLocation
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkKind
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicy
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyAccountState
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyOutput
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyOutput.Command
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyState
import java.util.Random
import java.util.UUID

open class ReaderBookmarkPolicyContract {

  val accountID =
    org.nypl.simplified.accounts.api.AccountID(UUID.fromString("46d17029-14ba-4e34-bcaa-def02713575a"))

  val bookmark0 =
    Bookmark(
      opdsId = "opdsid",
      location = BookLocation.create(Option.none(), "id"),
      time = LocalDateTime.now(),
      kind = BookmarkKind.ReaderBookmarkExplicit,
      chapterTitle = "A Title",
      chapterProgress = 0.5,
      bookProgress = 0.25,
      uri = null,
      deviceID = "urn:uuid:28cad755-2a0e-48bc-b5c8-1d43d57ac3e9")

  val bookmark0Idle =
    Bookmark(
      opdsId = "opdsid",
      location = BookLocation.create(Option.none(), "id"),
      time = LocalDateTime.now(),
      kind = BookmarkKind.ReaderBookmarkLastReadLocation,
      chapterTitle = "A Title",
      chapterProgress = 0.5,
      bookProgress = 0.25,
      uri = null,
      deviceID = "urn:uuid:28cad755-2a0e-48bc-b5c8-1d43d57ac3e9")

  val bookmark1 =
    Bookmark(
      opdsId = "opdsid-x",
      location = BookLocation.create(Option.none(), "id"),
      time = LocalDateTime.now(),
      kind = BookmarkKind.ReaderBookmarkExplicit,
      chapterTitle = "A Title",
      chapterProgress = 0.5,
      bookProgress = 0.25,
      uri = null,
      deviceID = "urn:uuid:28cad755-2a0e-48bc-b5c8-1d43d57ac3e9")

  /**
   * If a local bookmark is created, it should be saved locally. It should not be synced remotely
   * if the account does not have syncing enabled.
   */

  @Test
  fun testBookmarkLocalCreatedRemoteNotEnabled() {
    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = true,
            syncEnabledOnServer = false,
            syncPermittedByUser = true)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(Event.Local.BookmarkCreated(accountID, bookmark0)),
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertEquals(1, result.outputs.size)
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
  }

  /**
   * If a local bookmark is created, it should be saved locally. It should not be synced remotely
   * if the account does not have syncing permitted.
   */

  @Test
  fun testBookmarkLocalCreatedRemoteNotPermitted() {
    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = true,
            syncEnabledOnServer = true,
            syncPermittedByUser = false)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(Event.Local.BookmarkCreated(accountID, bookmark0)),
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertEquals(1, result.outputs.size)
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
  }

  /**
   * If a local bookmark is created, it should be saved locally. It should not be synced remotely
   * if the account does not have syncing supported.
   */

  @Test
  fun testBookmarkLocalCreatedRemoteNotSupported() {
    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = false,
            syncEnabledOnServer = true,
            syncPermittedByUser = true)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(Event.Local.BookmarkCreated(accountID, bookmark0)),
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertEquals(1, result.outputs.size)
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
  }

  /**
   * If a local bookmark is created, it should be saved locally. It should be synced remotely
   * if the account syncing is permitted, supported, and enabled.
   */

  @Test
  fun testBookmarkLocalCreatedRemoteOK() {
    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = true,
            syncEnabledOnServer = true,
            syncPermittedByUser = true)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(Event.Local.BookmarkCreated(accountID, bookmark0)),
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertEquals(2, result.outputs.size)
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
    Assert.assertEquals(Command.RemotelySendBookmark(accountID, bookmark0), result.outputs[1])
  }

  /**
   * If a local bookmark is received, and syncing is permitted, all bookmarks that are not
   * either currently being sent to the server, or are already on the server, should be sent
   * to the server.
   */

  @Test
  fun testBookmarkRemoteReceivedSyncMissing() {
    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(Pair(accountID, setOf(bookmark1))),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = true,
            syncEnabledOnServer = true,
            syncPermittedByUser = true)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(Event.Remote.BookmarkReceived(accountID, bookmark0)),
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark1.bookmarkId))
    Assert.assertEquals(1, result.outputs.size)
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
  }

  /**
   * If a local bookmark is created multiple times, an event is published notifying anyone
   * that cares.
   */

  @Test
  fun testBookmarkLocalCreatedTwice() {
    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf())

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(Event.Local.BookmarkCreated(accountID, bookmark0))
          .flatMap { ReaderBookmarkPolicy.evaluateInput(Event.Local.BookmarkCreated(accountID, bookmark0)) },
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
    Assert.assertEquals(ReaderBookmarkPolicyOutput.Event.LocalBookmarkAlreadyExists(accountID, bookmark0), result.outputs[1])
  }

  /**
   * If an idle bookmark exists, then creating an explicit bookmark for the same location
   * succeeds.
   */

  @Test
  fun testBookmarkLocalCreatedIdleExplicit() {
    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf())

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(Event.Local.BookmarkCreated(accountID, bookmark0))
          .flatMap { ReaderBookmarkPolicy.evaluateInput(Event.Local.BookmarkCreated(accountID, bookmark0Idle)) },
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0Idle), result.outputs[1])
  }

  /**
   * If an account exists but syncing is disabled on the server, and then syncing is enabled,
   * an attempt should be made to fetch bookmarks.
   */

  @Test
  fun testBookmarkSyncingRemotelyEnabled() {
    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = true,
            syncEnabledOnServer = false,
            syncPermittedByUser = true)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(Event.Remote.SyncingEnabled(accountID, true)),
        state)

    Assert.assertEquals(Command.RemotelyFetchBookmarks(accountID), result.outputs[0])
  }

  /**
   * If an account logs in, syncing happens.
   */

  @Test
  fun testBookmarkSyncingAccountLoggedIn() {
    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = true,
            syncEnabledOnServer = true,
            syncPermittedByUser = true)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.getAccountState(accountID)
          .andThen { s -> ReaderBookmarkPolicy.evaluateInput(Event.Local.AccountLoggedIn(s!!)) },
        state)

    Assert.assertEquals(Command.RemotelyFetchBookmarks(accountID), result.outputs[0])
  }

  /**
   * `forall a, f. return a >>= f = f a`
   */

  @Test
  fun testMonadLeftIdentity() {
    val state =
      ReaderBookmarkPolicyState.empty()

    val random = Random()
    val values = (1..100).map { random.nextInt() }
    val f = { z: Int -> ReaderBookmarkPolicy.unit(z * 2) }

    for (x in values) {
      val m = ReaderBookmarkPolicy.unit(x)
      Assert.assertEquals(
        ReaderBookmarkPolicy.evaluatePolicy(m.flatMap(f), state),
        ReaderBookmarkPolicy.evaluatePolicy(f.invoke(x), state))
    }
  }

  /**
   * `forall m. m >>= return = m`
   */

  @Test
  fun testMonadRightIdentity() {
    val state =
      ReaderBookmarkPolicyState.empty()

    val random = Random()
    val values = (1..100).map { random.nextInt() }

    for (x in values) {
      val m = ReaderBookmarkPolicy.unit(x)
      Assert.assertEquals(
        ReaderBookmarkPolicy.evaluatePolicy(m.flatMap({ n -> ReaderBookmarkPolicy.unit(n) }), state),
        ReaderBookmarkPolicy.evaluatePolicy(m, state))
    }
  }

  /**
   * `forall m, f, g. (m >>= f) >>= g = m >>= {x -> (f x) >>= g}`
   */

  @Test
  fun testMonadAssociativity() {
    val state =
      ReaderBookmarkPolicyState.empty()

    val random = Random()
    val values = (1..100).map { random.nextInt() }

    for (x in values) {
      val f = { z: Int -> ReaderBookmarkPolicy.unit(z + 1) }
      val g = { z: Int -> ReaderBookmarkPolicy.unit(z * 2) }

      val m = ReaderBookmarkPolicy.unit(x)
      val r0 = m.flatMap(f).flatMap(g)
      val r1 = m.flatMap({ z -> (f.invoke(z)).flatMap(g) })

      Assert.assertEquals(
        ReaderBookmarkPolicy.evaluatePolicy(r0, state),
        ReaderBookmarkPolicy.evaluatePolicy(r1, state))
    }
  }
}
