package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.LoginController;
import org.nypl.simplified.app.LoginControllerListenerType;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.BookBorrowListenerType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookStatusFailed;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.downloader.core.DownloadStatus;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.slf4j.Logger;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogAcquisitionController implements
  OnClickListener,
  LoginControllerListenerType,
  AccountSyncListenerType,
  BookBorrowListenerType
{
  private static final Logger   LOG;

  static {
    LOG = LogUtilities.getLog(CatalogAcquisitionController.class);
  }

  private final OPDSAcquisition acq;
  private final Activity        activity;
  private final BooksType       books;
  private final BookID          id;
  private final LoginController login_controller;
  private final String          title;

  public CatalogAcquisitionController(
    final Activity in_activity,
    final BooksType in_books,
    final BookID in_id,
    final OPDSAcquisition in_acq,
    final String in_title)
  {
    this.activity = NullCheck.notNull(in_activity);
    this.acq = NullCheck.notNull(in_acq);
    this.id = NullCheck.notNull(in_id);
    this.books = NullCheck.notNull(in_books);
    this.title = NullCheck.notNull(in_title);
    this.login_controller =
      new LoginController(this.activity, this.books, this);
  }

  private void failure(
    final String where,
    final OptionType<Throwable> error,
    final String message)
  {
    final String m =
      NullCheck.notNull(String.format("%s failed: %s", where, message));
    LogUtilities.errorWithOptionalException(
      CatalogAcquisitionController.LOG,
      m,
      error);

    final DownloadStatus status = DownloadStatus.STATUS_FAILED;
    final DownloadSnapshot snap =
      new DownloadSnapshot(
        0,
        0,
        -1,
        this.title,
        this.acq.getURI(),
        status,
        error);

    this.books.booksStatusUpdate(this.id, new BookStatusFailed(
      this.id,
      snap,
      error));
  }

  @Override public void onAccountSyncAuthenticationFailure(
    final String message)
  {
    /*
     * XXX: What's the correct thing to do here? Log in again?
     */

    CatalogAcquisitionController.LOG.debug(
      "account sync authentication failed: {}",
      message);
  }

  @Override public void onAccountSyncBook(
    final BookID book)
  {
    CatalogAcquisitionController.LOG.debug(
      "synced book: {} ({})",
      book,
      this.books.booksStatusGet(book));
  }

  @Override public void onAccountSyncFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    this.failure("account sync", error, message);
  }

  @Override public void onAccountSyncSuccess()
  {
    CatalogAcquisitionController.LOG.debug(
      "book: {} ({})",
      this.id,
      this.books.booksStatusGet(this.id));
    this.runDownload();
  }

  @Override public void onBookBorrowFailure(
    final BookID in_id,
    final OptionType<Throwable> in_e)
  {
    this.failure("borrow", in_e, "");
  }

  @Override public void onBookBorrowSuccess(
    final BookID in_id)
  {
    CatalogAcquisitionController.LOG.debug("borrow succeeded");
    this.books.accountSync(this);
  }

  @Override public void onClick(
    final @Nullable View v)
  {
    this.login_controller.onClick(v);
  }

  @Override public void onLoginAborted()
  {
    // Nothing
  }

  @Override public void onLoginFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    // Nothing
  }

  @Override public void onLoginSuccess()
  {
    CatalogAcquisitionController.LOG.debug("login succeeded");
    this.books.bookBorrow(this.id, this.acq, this);
  }

  private void runDownload()
  {
    CatalogAcquisitionController.LOG.debug(
      "starting type {} download",
      this.acq.getType());

    switch (this.acq.getType()) {
      case ACQUISITION_GENERIC:
      case ACQUISITION_BORROW:
      {
        this.books.bookDownloadOpenAccess(
          this.id,
          this.title,
          this.acq.getURI());
        break;
      }
      case ACQUISITION_OPEN_ACCESS:
      case ACQUISITION_BUY:
      case ACQUISITION_SAMPLE:
      case ACQUISITION_SUBSCRIBE:
      {
        break;
      }
    }
  }
}
