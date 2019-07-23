package org.nypl.simplified.splash

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.joda.time.format.DateTimeFormat
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.migration.api.MigrationReportXML
import org.nypl.simplified.migration.spi.MigrationEvent
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.StringBuilder
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

class SplashFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(SplashFragment::class.java)

  companion object {
    private const val parametersKey = "org.nypl.simplified.splash.parameters.main"

    fun newInstance(parameters: SplashParameters): SplashFragment {
      val args = Bundle()
      args.putSerializable(this.parametersKey, parameters)
      val fragment = SplashFragment()
      fragment.arguments = args
      return fragment
    }
  }

  private lateinit var listener: SplashListenerType
  private lateinit var parameters: SplashParameters
  private lateinit var bootSubscription: ObservableSubscriptionType<BootEvent>
  private lateinit var viewsForImage: ViewsImage
  private lateinit var viewsForEULA: ViewsEULA
  private lateinit var viewsForMigrationRunning: ViewsMigrationRunning
  private lateinit var viewsForMigrationReport: ViewsMigrationReport
  private lateinit var bootFuture: ListenableFuture<*>
  private var migrationSubscription: ObservableSubscriptionType<MigrationEvent>? = null
  private var migrationTried = false

  private class ViewsImage(
    val container: View,
    val image: ImageView,
    val text: TextView,
    val progress: ProgressBar)

  private class ViewsEULA(
    val container: View,
    val eulaAgree: Button,
    val eulaDisagree: Button,
    val eulaWebView: WebView)

  private class ViewsMigrationRunning(
    val container: View,
    val text: TextView,
    val progress: ProgressBar)

  private class ViewsMigrationReport(
    val container: View,
    val text: TextView,
    val list: RecyclerView,
    val sendButton: Button,
    val okButton: Button)

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)
    this.retainInstance = true
    this.parameters = this.arguments!!.getSerializable(parametersKey) as SplashParameters
  }

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    this.listener = this.activity as SplashListenerType
    this.bootFuture = this.listener.onSplashWantBootFuture()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?): View? {

    val stackView =
      inflater.inflate(R.layout.splash_stack, container, false) as ViewGroup
    val imageView =
      inflater.inflate(R.layout.splash_image, container, false)
    val eulaView =
      inflater.inflate(R.layout.splash_eula, container, false)
    val migrationProgressView =
      inflater.inflate(R.layout.splash_migration_running, container, false)
    val migrationReportView =
      inflater.inflate(R.layout.splash_migration_report, container, false)

    this.viewsForImage =
      ViewsImage(
        container = imageView,
        image = imageView.findViewById(R.id.splashImage),
        progress = imageView.findViewById(R.id.splashProgress),
        text = imageView.findViewById(R.id.splashText))

    this.viewsForEULA =
      ViewsEULA(
        container = eulaView,
        eulaAgree = eulaView.findViewById(R.id.splashEulaAgree),
        eulaDisagree = eulaView.findViewById(R.id.splashEulaDisagree),
        eulaWebView = eulaView.findViewById(R.id.splashEulaWebView))

    this.viewsForMigrationRunning =
      ViewsMigrationRunning(
        container = migrationProgressView,
        text = migrationProgressView.findViewById(R.id.splashMigrationProgressText),
        progress = migrationProgressView.findViewById(R.id.splashMigrationProgress))

    this.viewsForMigrationReport =
      ViewsMigrationReport(
        container = migrationReportView,
        text = migrationReportView.findViewById(R.id.splashMigrationReportTitle),
        list = migrationReportView.findViewById(R.id.splashMigrationReportList),
        sendButton = migrationReportView.findViewById(R.id.splashMigrationReportSend),
        okButton = migrationReportView.findViewById(R.id.splashMigrationReportOK))

    this.configureViewsForImage()

    stackView.addView(imageView)
    stackView.addView(eulaView)
    stackView.addView(migrationProgressView)
    stackView.addView(migrationReportView)

    imageView.visibility = View.VISIBLE
    eulaView.visibility = View.INVISIBLE
    migrationProgressView.visibility = View.INVISIBLE
    migrationReportView.visibility = View.INVISIBLE
    return stackView
  }

  private fun configureViewsForImage() {

    /*
     * Initially, only the image is shown.
     */

    this.viewsForImage.image.setImageResource(this.parameters.splashImageResource)
    this.viewsForImage.image.visibility = View.VISIBLE
    this.viewsForImage.progress.visibility = View.INVISIBLE
    this.viewsForImage.text.visibility = View.INVISIBLE
    this.viewsForImage.text.text = ""

    /*
     * Clicking the image makes the image invisible but makes the
     * progress bar visible.
     */

    this.viewsForImage.image.setOnClickListener {
      this.popImageView()
    }
  }

  private fun popImageView() {
    this.viewsForImage.progress.visibility = View.VISIBLE
    this.viewsForImage.text.visibility = View.VISIBLE
    this.viewsForImage.image.animation = AnimationUtils.loadAnimation(this.context, R.anim.zoom_fade)
  }

  private fun configureViewsForEULA(eula: EULAType) {
    this.viewsForEULA.eulaAgree.setOnClickListener {
      eula.eulaSetHasAgreed(true)
      this.onFinishEULASuccessfully()
    }
    this.viewsForEULA.eulaDisagree.setOnClickListener {
      eula.eulaSetHasAgreed(false)
      this.activity?.finish()
    }

    val url = eula.documentGetReadableURL()
    this.logger.debug("eula:     {}", eula)
    this.logger.debug("eula URL: {}", url)

    this.viewsForEULA.eulaWebView.settings.allowFileAccessFromFileURLs = true
    this.viewsForEULA.eulaWebView.settings.allowFileAccess = true
    this.viewsForEULA.eulaWebView.settings.allowContentAccess = true
    this.viewsForEULA.eulaWebView.settings.setSupportMultipleWindows(false)
    this.viewsForEULA.eulaWebView.settings.allowUniversalAccessFromFileURLs = false
    this.viewsForEULA.eulaWebView.settings.javaScriptEnabled = false

    this.viewsForEULA.eulaWebView.webViewClient = object : WebViewClient() {
      override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        this@SplashFragment.logger.error("onReceivedError: {} {} {}", errorCode, description, failingUrl)
      }

      override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        this@SplashFragment.logger.error("onReceivedError: {}", error)
      }
    }

    this.viewsForEULA.eulaWebView.loadUrl(url.toString())

    this.viewsForEULA.container.visibility = View.VISIBLE
    this.viewsForImage.container.visibility = View.INVISIBLE
    this.viewsForMigrationReport.container.visibility = View.INVISIBLE
    this.viewsForMigrationRunning.container.visibility = View.INVISIBLE
  }

  private fun configureViewsForMigrationProgress() {
    this.viewsForEULA.container.visibility = View.INVISIBLE
    this.viewsForImage.container.visibility = View.INVISIBLE
    this.viewsForMigrationReport.container.visibility = View.INVISIBLE
    this.viewsForMigrationRunning.container.visibility = View.VISIBLE

    this.viewsForMigrationRunning.progress.isIndeterminate = true
    this.viewsForMigrationRunning.text.text = ""
  }

  @UiThread
  private fun configureViewsForMigrationReport(
    report: MigrationReport,
    savedReportFile: File?,
    compressedLogFile: File?
  ) {
    this.viewsForEULA.container.visibility = View.INVISIBLE
    this.viewsForImage.container.visibility = View.INVISIBLE
    this.viewsForMigrationReport.container.visibility = View.VISIBLE
    this.viewsForMigrationRunning.container.visibility = View.INVISIBLE

    val errors =
      report.events.any { e -> e is MigrationEvent.MigrationStepError }
    val eventsToShow =
      report.events.filterNot { e -> e is MigrationEvent.MigrationStepInProgress }

    this.viewsForMigrationReport.list.adapter = SplashMigrationReportListAdapter(eventsToShow)
    this.viewsForMigrationReport.list.setHasFixedSize(false)
    this.viewsForMigrationReport.list.layoutManager = LinearLayoutManager(this.context);
    (this.viewsForMigrationReport.list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    this.viewsForMigrationReport.list.adapter.notifyDataSetChanged()

    if (errors) {
      this.viewsForMigrationReport.text.setText(R.string.migrationFailure)
    } else {
      this.viewsForMigrationReport.text.setText(R.string.migrationSuccess)
    }

    val reportEmail = this.parameters.splashMigrationReportEmail
    if (savedReportFile != null && reportEmail != null) {
      this.viewsForMigrationReport.sendButton.setOnClickListener {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
          this.type = "text/plain"
          this.putExtra(Intent.EXTRA_EMAIL, arrayOf(reportEmail))
          this.putExtra(Intent.EXTRA_SUBJECT, this@SplashFragment.reportEmailSubject(report))
          this.putExtra(Intent.EXTRA_TEXT, this@SplashFragment.reportEmailBody(report))

          val attachments = java.util.ArrayList<Uri>()
          attachments.add(Uri.fromFile(savedReportFile))
          if (compressedLogFile != null) {
            attachments.add(Uri.fromFile(compressedLogFile))
          }
          this.putExtra(Intent.EXTRA_STREAM, attachments)
        }
        this.startActivity(intent)
      }
    } else {
      this.viewsForMigrationReport.sendButton.visibility = View.INVISIBLE
    }

    this.viewsForMigrationReport.okButton.setOnClickListener {
      this.listener.onSplashDone()
    }
  }

  private fun reportEmailBody(report: MigrationReport): String {
    val errors = report.events.filterIsInstance<MigrationEvent.MigrationStepError>().size

    return StringBuilder(128)
      .append("On ${report.timestamp}, a migration of ${report.application} occurred.")
      .append("\n")
      .append("There were ${errors} errors.")
      .append("\n")
      .append("The attached log files give details of the migration.")
      .append("\n")
      .toString()
  }

  private fun reportEmailSubject(report: MigrationReport): String {
    val errors =
      report.events.any { e -> e is MigrationEvent.MigrationStepError}
    val outcome =
      if (errors) {
        "error"
      } else {
        "success"
      }

    return "[simplye-android-migration] ${report.application} ${outcome}"
  }

  override fun onStart() {
    super.onStart()

    this.bootSubscription =
      this.listener.onSplashWantBootEvents()
        .subscribe { event -> this.onBootEvent(event) }

    /*
     * Subscribe to the boot future specifically so that we don't risk missing the delivery
     * of important "boot completed" or "boot failed" messages.
     */

    this.bootFuture.addListener(Runnable {
      try {
        this.bootFuture.get(1L, TimeUnit.SECONDS)
        this.onBootFinished()
      } catch (e: Throwable) {
        this.onBootEvent(BootEvent.BootFailed(e.message ?: "", Exception(e)))
      }
    }, MoreExecutors.directExecutor())
  }

  override fun onStop() {
    super.onStop()
    this.bootSubscription.unsubscribe()
    this.migrationSubscription?.unsubscribe()
  }

  private fun onBootEvent(event: BootEvent) {
    this.runOnUIThread {
      this.onBootEventUI(event)
    }
  }

  @UiThread
  private fun onBootEventUI(event: BootEvent) {
    return when (event) {
      is BootEvent.BootInProgress -> {
        this.viewsForImage.text.text = event.message
      }

      is BootEvent.BootCompleted -> {
        // Don't care.
      }

      is BootEvent.BootFailed ->
        this.onBootEventFailedUI(event)
    }
  }

  @UiThread
  private fun onBootEventFailedUI(event: BootEvent.BootFailed) {
    if (this.viewsForImage.image.alpha > 0.0) {
      this.popImageView()
    }

    // XXX: We need to do better than this.
    // Print a useful message rather than a raw exception message, and allow
    // the user to do something such as submitting a report.
    this.viewsForImage.progress.isIndeterminate = false
    this.viewsForImage.progress.progress = 100
    this.viewsForImage.text.text = event.message
  }

  private fun onBootFinished() {
    this.runOnUIThread { this.onBootFinishedUI() }
  }

  @UiThread
  private fun onBootFinishedUI() {
    this.viewsForImage.progress.isIndeterminate = false
    this.viewsForImage.progress.progress = 100
    this.viewsForImage.text.text = ""

    val eulaProvided = this.listener.onSplashEULAIsProvided()
    if (!eulaProvided) {
      this.onFinishEULASuccessfully()
      return
    }

    val eula = this.listener.onSplashEULARequested()
    if (eula.eulaHasAgreed()) {
      this.onFinishEULASuccessfully()
      return
    }

    this.configureViewsForEULA(eula)
  }

  /**
   * Either no EULA was provided, or one was provided and the user agreed to it.
   */

  @UiThread
  private fun onFinishEULASuccessfully() {
    when (this.listener.onSplashWantProfilesMode()) {
      ANONYMOUS_PROFILE_ENABLED -> {
        this.listener.onSplashOpenProfileAnonymous()
      }
      ANONYMOUS_PROFILE_DISABLED -> {

      }
    }

    this.doMigrations()
  }

  @UiThread
  private fun doMigrations() {
    this.logger.debug("doMigrations")

    val migrations = this.listener.onSplashWantMigrations()

    if (this.migrationTried || (!migrations.anyNeedToRun())) {
      this.logger.debug("either migration has already been tried, or no migrations need to run")
      this.onMigrationsDone()
      return
    }

    this.migrationTried = true
    this.runOnUIThread {
      this.configureViewsForMigrationProgress()
    }

    this.migrationSubscription =
      migrations.events.subscribe { event -> this.onMigrationEvent(event) }

    val executor =
      this.listener.onSplashWantMigrationExecutor()
    val migrationFuture =
      executor.submit(Callable { migrations.runMigrations() })

    migrationFuture.addListener(
      Runnable {
        try {
          this.processMigrationReport(migrationFuture.get(1L, TimeUnit.SECONDS))
        } catch (e: Throwable) {
          this.processMigrationCrashed(e)
        }
      },
      MoreExecutors.directExecutor())
  }

  private fun onMigrationsDone() {
    this.listener.onSplashDone()
  }

  private fun processMigrationReport(report: MigrationReport?) {
    if (report == null) {
      this.listener.onSplashDone()
      return
    }

    try {
      this.listener.onSplashMigrationReport(report)
    } catch (e: Exception) {
      this.logger.error("ignored onSplashMigrationReport exception: ", e)
    }

    var savedReportFile : File? = null
    var compressedLogFile : File? = null

    try {
      savedReportFile = this.processMigrationReportSaveToDisk(report)
      compressedLogFile = this.compressLogFileIfAvailable()
    } catch (e: Exception) {
      this.logger.error("unable to save log and/or report file: ", e)
    }

    this.runOnUIThread {
      this.configureViewsForMigrationReport(report, savedReportFile, compressedLogFile)
    }
  }

  private fun compressLogFileIfAvailable(): File? {
    return try {
      val cacheDir = this.requireContext().externalCacheDir
      if (cacheDir == null) {
        AlertDialog.Builder(this.requireContext())
          .setTitle("Could not compress log.")
          .setMessage("External cache directory is unavailable.")
          .show()
        return null
      }

      val logFile = File(cacheDir, "log.txt")
      val logFileGz = File(cacheDir, "log.txt.gz")

      this.logger.debug("attempting to compress log file: {}", logFile)
      FileInputStream(logFile).use { inputStream ->
        FileOutputStream(logFileGz, false).use { stream ->
          BufferedOutputStream(stream).use { bstream ->
            GZIPOutputStream(stream).use { zstream ->
              inputStream.copyTo(zstream)
              zstream.finish()
              zstream.flush()
              logFileGz
            }
          }
        }
      }
    } catch (e: Exception) {
      this.logger.error("could not compress log file: ", e)
      null
    }
  }

  /**
   * Try saving the migration report to disk, but don't worry too much if the report can't
   * be saved. If the report can't be saved, the user likely has bigger problems than a failed
   * migration.
   */

  private fun processMigrationReportSaveToDisk(report: MigrationReport): File? {
    return try {
      val cacheDir = this.requireContext().externalCacheDir
      if (cacheDir == null) {
        AlertDialog.Builder(this.requireContext())
          .setTitle("Could not save migration report.")
          .setMessage("External cache directory is unavailable.")
          .show()
        return null
      }

      val formatter =
        DateTimeFormat.forPattern("YYYY-MM-dd-HHmmss-SSSS")
      val timestamp =
        formatter.print(report.timestamp)
      val reportsDir =
        File(cacheDir, "migrations")
      val reportFile =
        File(reportsDir, "report-${timestamp}.xml")

      reportsDir.mkdirs()
      FileOutputStream(reportFile).use { stream ->
        MigrationReportXML.serializeToXML(report, stream)
        reportFile
      }
    } catch (e: Exception) {
      this.logger.error("could not save report: ", e)
      null
    }
  }

  private fun processMigrationCrashed(e: Throwable) {
    this.logger.error("processMigrationCrashed: ", e)
  }

  private fun onMigrationEvent(event: MigrationEvent) {
    this.runOnUIThread {
      this.onMigrationEventUI(event)
    }
  }

  @UiThread
  private fun onMigrationEventUI(event: MigrationEvent) {
    this.viewsForMigrationRunning.text.text = event.message
  }

  /**
   * Run the given Runnable on the UI thread.
   *
   * @param r The runnable
   */

  private fun runOnUIThread(f: () -> Unit) {
    val looper = Looper.getMainLooper()
    val h = Handler(looper)
    h.post { f.invoke() }
  }
}