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

val LOGGER_NAME: String = MethodHandles.lookup().lookupClass().getName()
private val LOGGER = Logger.getLogger(LOGGER_NAME)
private val textArea = JTextArea()

fun createUI(): Component {
  LOGGER.useParentHandlers = false
  textArea.isEditable = false
  LOGGER.addHandler(TextAreaHandler(TextAreaOutputStream(textArea)))
  val p = JPanel(GridLayout(2, 1, 10, 10))
  p.add(createZipPanel())
  p.add(createUnzipPanel())
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createZipPanel(): Component {
  val field = JTextField(20)
  val selectButton = JButton("select directory")
  selectButton.addActionListener {
    val fileChooser = JFileChooser()
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
    val ret = fileChooser.showOpenDialog(selectButton.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      field.text = fileChooser.selectedFile.absolutePath
    }
  }

  val zipButton = JButton("zip")
  zipButton.addActionListener {
    val text = field.getText()
    val path = Paths.get(text)
    if (!text.isEmpty() && path.toFile().exists()) {
      val name = path.fileName.toString() + ".zip"
      zip(path, path.resolveSibling(name))
    }
  }

  val p = JPanel(BorderLayout(5, 2))
  p.setBorder(BorderFactory.createTitledBorder("Zip"))
  p.add(field)
  p.add(selectButton, BorderLayout.EAST)
  p.add(zipButton, BorderLayout.SOUTH)
  return p
}

private fun createUnzipPanel(): Component {
  val field = JTextField(20)
  val selectButton = JButton("select .zip file")
  selectButton.addActionListener {
    val fileChooser = JFileChooser()
    val ret = fileChooser.showOpenDialog(selectButton.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      field.text = fileChooser.selectedFile.absolutePath
    }
  }

  val unzipButton = JButton("unzip")
  unzipButton.addActionListener {
    val text = field.getText()
    createTargetDirPath(text)?.also {
      unzip(Paths.get(text), it)
    }
  }

  val p = JPanel(BorderLayout(5, 2))
  p.border = BorderFactory.createTitledBorder("Unzip")
  p.add(field)
  p.add(selectButton, BorderLayout.EAST)
  p.add(unzipButton, BorderLayout.SOUTH)
  return p
}

private fun zip(srcDir: Path, zipFile: Path) {
  val parent = textArea.rootPane
  runCatching {
    if (canOverwrite(parent, zipFile, "Zip")) {
      ZipUtils.zip(srcDir, zipFile)
    }
  }.onFailure {
    LOGGER.info { "Cant zip! : $zipFile" }
    UIManager.getLookAndFeel().provideErrorFeedback(textArea)
  }
}

private fun unzip(zipFile: Path, targetDir: Path) {
  val parent = textArea.rootPane
  runCatching {
    if (canOverwrite(parent, targetDir, "Unzip")) {
      createDirectoriesIfAbsent(targetDir)
      ZipUtils.unzip(zipFile, targetDir)
    }
  }.onFailure {
    LOGGER.info { "Cant unzip! : $zipFile" }
    UIManager.getLookAndFeel().provideErrorFeedback(textArea)
  }
}

private fun canOverwrite(
  parent: Component,
  path: Path,
  title: String,
) = !path.toFile().exists() || showOverwriteConfirm(parent, path, title)

private fun showOverwriteConfirm(
  parent: Component,
  path: Path,
  title: String,
): Boolean {
  val s1 = "$path already exists."
  val s2 = "Do you want to overwrite it?"
  val ret = JOptionPane.showConfirmDialog(
    parent,
    "<html>$s1<br>$s2",
    title,
    JOptionPane.YES_NO_OPTION,
  )
  return ret == JOptionPane.YES_OPTION
}

@Throws(IOException::class)
private fun createDirectoriesIfAbsent(dir: Path) {
  if (!dir.toFile().exists()) {
    LOGGER.info { "mkdir0: $dir" }
    Files.createDirectories(dir)
  }
}

private fun createTargetDirPath(text: String): Path? {
  val path = Paths.get(text)
  return if (text.isEmpty() || !path.toFile().exists()) {
    null
  } else {
    var name = path.fileName.toString()
    val lastDotPos = name.lastIndexOf('.')
    if (lastDotPos > 0) {
      name = name.take(lastDotPos)
    }
    path.resolveSibling(name)
  }
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
        val files = it.toList()
        ZipOutputStream(Files.newOutputStream(zip)).use { zos ->
          for (path in files) {
            val relativePath = srcDir.relativize(path).toString().replace('\\', '/')
            LOGGER.info { "zip: $relativePath" }
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
          LOGGER.info { "mkdir1: $path" }
          Files.createDirectories(path)
        } else {
          path.parent?.takeUnless { it.toFile().exists() }?.also {
            LOGGER.info { "mkdir2: $it" }
            Files.createDirectories(it)
          }
          LOGGER.info { "copy: $path" }
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
