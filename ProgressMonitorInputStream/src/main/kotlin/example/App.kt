package example

import java.awt.*
import java.awt.event.ActionEvent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Scanner
import javax.swing.*

private val textArea = JTextArea()
private val runButton = JButton("Load")
private var monitor: ProgressMonitor? = null

private fun makeUI(): Component {
  textArea.isEditable = false
  runButton.addActionListener { e -> executeWorker(e) }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(runButton)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(JScrollPane(textArea))
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

fun executeWorker(e: ActionEvent) {
  val b = e.source as? JButton ?: return
  b.isEnabled = false
  textArea.text = ""

  val idx = 19 // 1 + random.nextInt(27 - 1)
  val path = "https://docs.oracle.com/javase/8/docs/api/index-files/index-$idx.html"
  append(path)
  val urlConnection = runCatching { URL(path).openConnection() }
    .onFailure { textArea.text = "error: " + it.message }
    .getOrNull()
    ?: return
  append("urlConnection.getContentEncoding(): " + urlConnection.contentEncoding)
  append("urlConnection.getContentType(): " + urlConnection.contentType)
  val cs = getCharset(urlConnection)
  val length = urlConnection.contentLength
  val loop = Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop()
  runCatching {
    urlConnection.getInputStream().use { stream ->
      ProgressMonitorInputStream(b.rootPane, "Loading", stream).use { pms ->
        monitor = pms.progressMonitor.also {
          it.note = " " // Need for JLabel#getPreferredSize
          it.millisToDecideToPopup = 0
          it.millisToPopup = 0
          it.minimum = 0
          it.maximum = length
        }
        val task = object : MonitorTask(pms, cs, length) {
          override fun done() {
            super.done()
            loop.exit()
          }
        }
        task.execute()
        loop.enter()
      }
    }
  }.onFailure {
    textArea.text = "error: " + it.message
  }
}

private open class MonitorTask(
  pms: ProgressMonitorInputStream,
  cs: Charset,
  length: Int
) : BackgroundTask(pms, cs, length) {
  override fun process(chunks: List<Chunk>) {
    if (isCancelled) {
      return
    }
    if (!textArea.isDisplayable) {
      cancel(true)
      return
    }
    chunks.forEach {
      append(it.line)
      monitor?.note = it.note
    }
  }

  override fun done() {
    runButton.isEnabled = true
    runCatching {
      pms.close()
      append(if (isCancelled) "Cancelled" else get())
    }.onFailure {
      append("Error:" + it.message)
    }
  }
}

fun append(str: String) {
  textArea.append(str + "\n")
  textArea.caretPosition = textArea.document.length
}

private fun getCharset(urlConnection: URLConnection): Charset {
  val encoding = urlConnection.contentEncoding
  return if (encoding != null) {
    Charset.forName(encoding)
  } else {
    StandardCharsets.UTF_8
//    val contentType = urlConnection.contentType
//    val list = contentType.split(";")
//    if (list.isNotEmpty()) {
//      val str = list.toList()
//        .first {
//          it.isNotEmpty() && it.lowercase(Locale.ENGLISH).startsWith("charset=")
//        }.substring("charset=".length)
//      if (str.isNotEmpty()) Charset.forName(str) else StandardCharsets.UTF_8
//    } else StandardCharsets.UTF_8
  }
}

private data class Chunk(val line: String, val note: String)

private open class BackgroundTask(
  protected val pms: ProgressMonitorInputStream,
  private val cs: Charset,
  private val lengthOfFile: Int
) : SwingWorker<String, Chunk>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    val ret = "Done"
    Scanner(BufferedReader(InputStreamReader(pms, cs))).use { scanner ->
      var i = 0
      var readied = 0
      while (scanner.hasNextLine()) {
        readied = doSomething(scanner, i++, readied)
      }
    }
    return ret
  }

  @Throws(InterruptedException::class)
  protected fun doSomething(scanner: Scanner, idx: Int, readied: Int): Int {
    if (idx % 50 == 0) {
      Thread.sleep(10)
    }
    val line = scanner.nextLine()
    val size = readied + line.toByteArray(cs).size + 1 // +1: \n
    val note = "%03d%% - %d/%d%n".format(100 * size / lengthOfFile, size, lengthOfFile)
    publish(Chunk(line, note))
    return size
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
