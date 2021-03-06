package org.nypl.simplified.tests.books.book_database

import android.content.Context
import com.io7m.jfunctional.Option
import one.irradia.mime.vanilla.MIMEParser
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.api.BookLocation
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkKind
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.opds.core.OPDSJSONSerializer
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

abstract class BookDatabaseEPUBContract {

  private val logger =
    LoggerFactory.getLogger(BookDatabaseEPUBContract::class.java)

  private val accountID =
    org.nypl.simplified.accounts.api.AccountID(UUID.fromString("46d17029-14ba-4e34-bcaa-def02713575a"))

  protected abstract fun context(): Context

  /**
   * Creating a book database entry for a feed that contains an EPUB acquisition results in an
   * EPUB format. Reopening the database shows that the data is preserved.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryLastReadLocation() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      org.nypl.simplified.books.book_database.BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithEPUB()
    val bookID = BookIDs.newFromText("abcd")
    val databaseEntry0 = database0.createOrUpdate(bookID, feedEntry)

    this.run {
      val formatHandle =
        databaseEntry0.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

      Assert.assertTrue(
        "Format is present", formatHandle != null)

      formatHandle!!
      Assert.assertEquals(null, formatHandle.format.lastReadLocation)

      val bookmark =
        Bookmark(
          opdsId = "abcd",
          location = BookLocation.create(Option.some("xyz"), "abc"),
          time = LocalDateTime.now(),
          chapterTitle = "A title",
          kind = BookmarkKind.ReaderBookmarkLastReadLocation,
          chapterProgress = 0.5,
          bookProgress = 0.25,
          uri = null,
          deviceID = "3475fa24-25ca-4ddb-9d7b-762358d5f83a")

      formatHandle.setLastReadLocation(bookmark)
      Assert.assertEquals(bookmark, formatHandle.format.lastReadLocation)

      formatHandle.setLastReadLocation(null)
      Assert.assertEquals(null, formatHandle.format.lastReadLocation)
    }
  }

  /**
   * Saving and restoring bookmarks works.
   *
   * @throws Exception On errors
   */

  @Test
  fun testBookmarks() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      org.nypl.simplified.books.book_database.BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithEPUB()
    val bookID = BookIDs.newFromText("abcd")
    val databaseEntry0 = database0.createOrUpdate(bookID, feedEntry)

    val bookmark0 =
      Bookmark(
        opdsId = "abcd",
        location = BookLocation.create(Option.some("xyz"), "abc"),
        time = LocalDateTime.now(),
        kind = BookmarkKind.ReaderBookmarkExplicit,
        chapterTitle = "A title",
        chapterProgress = 0.5,
        bookProgress = 0.25,
        uri = null,
        deviceID = "3475fa24-25ca-4ddb-9d7b-762358d5f83a")

    val bookmark1 =
      Bookmark(
        opdsId = "abcd",
        location = BookLocation.create(Option.some("xyz"), "abc"),
        time = LocalDateTime.now(),
        kind = BookmarkKind.ReaderBookmarkExplicit,
        chapterTitle = "A title",
        chapterProgress = 0.6,
        bookProgress = 0.25,
        uri = null,
        deviceID = "3475fa24-25ca-4ddb-9d7b-762358d5f83a")

    val bookmark2 =
      Bookmark(
        opdsId = "abcd",
        location = BookLocation.create(Option.some("xyz"), "abc"),
        time = LocalDateTime.now(),
        kind = BookmarkKind.ReaderBookmarkExplicit,
        chapterTitle = "A title",
        chapterProgress = 0.7,
        bookProgress = 0.25,
        uri = null,
        deviceID = "3475fa24-25ca-4ddb-9d7b-762358d5f83a")

    val bookmarks0 = listOf(bookmark0)
    val bookmarks1 = listOf(bookmark0, bookmark1, bookmark2)

    this.run {
      val formatHandle =
        databaseEntry0.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

      Assert.assertTrue(
        "Format is present", formatHandle != null)

      formatHandle!!
      Assert.assertEquals(listOf<Bookmark>(), formatHandle.format.bookmarks)

      formatHandle.setBookmarks(bookmarks0)
      Assert.assertEquals(bookmarks0, formatHandle.format.bookmarks)

      formatHandle.setBookmarks(bookmarks1)
      Assert.assertEquals(bookmarks1, formatHandle.format.bookmarks)
    }

    val database1 =
      org.nypl.simplified.books.book_database.BookDatabase.open(context(), parser, serializer, accountID, directory)
    val databaseEntry1 =
      database1.createOrUpdate(bookID, feedEntry)

    this.run {
      val formatHandle =
        databaseEntry1.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

      Assert.assertTrue(
        "Format is present", formatHandle != null)

      formatHandle!!
      Assert.assertEquals(bookmarks1, formatHandle.format.bookmarks)
    }
  }

  private fun acquisitionFeedEntryWithEPUB(): OPDSAcquisitionFeedEntry {
    val revoke = Option.none<URI>()
    val eb = OPDSAcquisitionFeedEntry.newBuilder(
      "abcd",
      "Title",
      DateTime.now(),
      OPDSAvailabilityOpenAccess.get(revoke))

    eb.addAcquisition(
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://example.com"),
        Option.some(MIMEParser.parseRaisingException("application/epub+zip")),
        emptyList()))
    return eb.build()
  }
}
