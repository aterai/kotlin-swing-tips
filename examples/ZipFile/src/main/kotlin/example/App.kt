package example

import java.awt.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.swing.*
import kotlin.streams.toList

private val logger = Logger.getLogger(MethodHandles.lookup().lookupClass().name)
private val textArea = JTextArea()

fun makeUI(): Component {
  logger.useParentHandlers = false
  textArea.isEditable = false
  logger.addHandler(TextAreaHandler(TextAreaOutputStream(textArea)))
  val p = JPanel(GridLayout(2, 1, 10, 10))
  p.add(makeZipPanel())
  p.add(makeUnzipPanel())
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeZipPanel(): Component {
  val field = JTextField(20)
  val button = JButton("select directory")
  button.addActionListener {
    val fileChooser = JFileChooser()
    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    val ret = fileChooser.showOpenDialog(button.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      field.text = fileChooser.selectedFile.absolutePath
    }
  }

  val button1 = JButton("zip")
  button1.addActionListener { zip(field.text) }

  val p = JPanel(BorderLayout(5, 2))
  p.border = BorderFactory.createTitledBorder("Zip")
  p.add(field)
  p.add(button, BorderLayout.EAST)
  p.add(button1, BorderLayout.SOUTH)
  return p
}

private fun makeUnzipPanel(): Component {
  val field = JTextField(20)
  val button = JButton("select .zip file")
  button.addActionListener {
    val fileChooser = JFileChooser()
    val ret = fileChooser.showOpenDialog(button.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      field.text = fileChooser.selectedFile.absolutePath
    }
  }
  val button1 = JButton("unzip")
  button1.addActionListener { unzip(field.text) }

  val p = JPanel(BorderLayout(5, 2))
  p.border = BorderFactory.createTitledBorder("Unzip")
  p.add(field)
  p.add(button, BorderLayout.EAST)
  p.add(button1, BorderLayout.SOUTH)
  return p
}

private fun zip(str: String) {
  val path = Paths.get(str)
  if (str.isEmpty() || !path.toFile().exists()) {
    return
  }
  val name = path.fileName.toString() + ".zip"
  val tgt = path.resolveSibling(name)
  if (tgt.toFile().exists()) {
    val m = "<html>$tgt already exists.<br>Do you want to overwrite it?"
    val c = textArea.rootPane
    val rv = JOptionPane.showConfirmDialog(c, m, "Zip", JOptionPane.YES_NO_OPTION)
    if (rv != JOptionPane.YES_OPTION) {
      return
    }
  }
  runCatching {
    ZipUtils.zip(path, tgt)
  }.onFailure {
    logger.info { "Cant zip! : $path" }
    UIManager.getLookAndFeel().provideErrorFeedback(textArea)
  }
}

private fun unzip(str: String) {
  makeTargetDirPath(str)?.also { dstDir ->
    val path = Paths.get(str)
    runCatching {
      if (dstDir.toFile().exists()) {
        val rv = JOptionPane.showConfirmDialog(
          textArea.rootPane,
          "<html>$dstDir already exists.<br>Do you want to overwrite it?",
          "Unzip",
          JOptionPane.YES_NO_OPTION,
        )
        if (rv != JOptionPane.YES_OPTION) {
          return
        }
      } else {
        logger.info { "mkdir0: $dstDir" }
        Files.createDirectories(dstDir)
      }
      ZipUtils.unzip(path, dstDir)
    }.onFailure {
      logger.info { "Cant unzip! : $path" }
      UIManager.getLookAndFeel().provideErrorFeedback(textArea)
    }
  }
}

private fun makeTargetDirPath(text: String): Path? {
  val path = Paths.get(text)
  if (text.isEmpty() || !path.toFile().exists()) {
    return null
  }
  var name = path.fileName.toString()
  val lastDotPos = name.lastIndexOf('.')
  if (lastDotPos > 0) {
    name = name.substring(0, lastDotPos)
  }
  return path.resolveSibling(name)
}

private object ZipUtils {
  @Throws(IOException::class)
  fun zip(
    srcDir: Path,
    zip: Path,
  ) {
    Files
      .walk(srcDir)
      .filter { it.toFile().isFile }
      .use {
        val files = it.toList() // it.collect(Collectors.toList())
        ZipOutputStream(Files.newOutputStream(zip)).use { zos ->
          for (path in files) {
            val relativePath = srcDir.relativize(path).toString().replace('\\', '/')
            logger.info { "zip: $relativePath" }
            zos.putNextEntry(ZipEntry(relativePath))
            Files.copy(path, zos)
            zos.closeEntry()
          }
        }
      }
  }

  @Throws(IOException::class)
  fun unzip(
    zipFilePath: Path,
    dstDir: Path,
  ) {
    ZipFile(zipFilePath.toString()).use { zipFile ->
      zipFile.entries().toList().forEach { entry ->
        val name = entry.name
        val path = dstDir.resolve(name)
        if (name.endsWith("/")) {
          logger.info { "mkdir1: $path" }
          Files.createDirectories(path)
        } else {
          path.parent?.takeUnless { it.toFile().exists() }?.also {
            logger.info { "mkdir2: $it" }
            Files.createDirectories(it)
          }
          logger.info { "copy: $path" }
          Files.copy(
            zipFile.getInputStream(entry),
            path,
            StandardCopyOption.REPLACE_EXISTING,
          )
        }
      }
    }
  }
}

private class TextAreaOutputStream(
  private val textArea: JTextArea,
) : OutputStream() {
  private val buffer = ByteArrayOutputStream()

  @Throws(IOException::class)
  override fun flush() {
    textArea.append(buffer.toString("UTF-8"))
    buffer.reset()
  }

  override fun write(b: Int) {
    buffer.write(b)
  }

  override fun write(
    b: ByteArray,
    off: Int,
    len: Int,
  ) {
    buffer.write(b, off, len)
  }
}

private class TextAreaHandler(
  os: OutputStream,
) : StreamHandler(os, SimpleFormatter()) {
  override fun getEncoding(): String = StandardCharsets.UTF_8.name()

  @Synchronized
  override fun publish(logRecord: LogRecord) {
    super.publish(logRecord)
    flush()
  }

  @Synchronized
  override fun close() {
    flush()
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
