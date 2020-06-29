package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.swing.* // ktlint-disable no-wildcard-imports

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
  // if (str.isEmpty() || Files.notExists(path)) { // noticeably poor performance in JDK 8
  if (str.isEmpty() || !path.toFile().exists()) {
    return
  }
  val name = path.fileName.toString() + ".zip"
  val tgt = path.resolveSibling(name)
  // if (Files.exists(tgt)) { // noticeably poor performance in JDK 8
  if (tgt.toFile().exists()) {
    val m = "<html>$tgt already exists.<br>Do you want to overwrite it?"
    val rv = JOptionPane.showConfirmDialog(textArea.rootPane, m, "Zip", JOptionPane.YES_NO_OPTION)
    if (rv != JOptionPane.YES_OPTION) {
      return
    }
  }
  runCatching {
    ZipUtil.zip(path, tgt)
  }.onFailure {
    logger.info { "Cant zip! : $path" }
    Toolkit.getDefaultToolkit().beep()
  }
}

private fun unzip(str: String) {
  makeDestDirPath(str)?.also { destDir ->
    val path = Paths.get(str)
    runCatching {
      // if (Files.exists(destDir)) { // noticeably poor performance in JDK 8
      if (destDir.toFile().exists()) {
        val m = "<html>$destDir already exists.<br>Do you want to overwrite it?"
        val rv = JOptionPane.showConfirmDialog(textArea.rootPane, m, "Unzip", JOptionPane.YES_NO_OPTION)
        if (rv != JOptionPane.YES_OPTION) {
          return
        }
      } else {
        logger.info { "mkdir0: $destDir" }
        Files.createDirectories(destDir)
      }
      ZipUtil.unzip(path, destDir)
    }.onFailure {
      logger.info { "Cant unzip! : $path" }
      Toolkit.getDefaultToolkit().beep()
    }
  }
}

private fun makeDestDirPath(text: String): Path? {
  val path = Paths.get(text)
  // if (str.isEmpty() || Files.notExists(path)) { // noticeably poor performance in JDK 8
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

private object ZipUtil {
  @Throws(IOException::class)
  fun zip(srcDir: Path, zip: Path) {
    // try (Stream<Path> s = Files.walk(srcDir).filter(Files::isRegularFile)) { // noticeably poor performance in JDK 8
    Files.walk(srcDir)
      .filter { it.toFile().isFile }
      .use {
        val files = it.collect(Collectors.toList())
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
  fun unzip(zipFilePath: Path, destDir: Path) {
    ZipFile(zipFilePath.toString()).use { zipFile ->
      zipFile.entries().toList().forEach { entry ->
        val name = entry.name
        val path = destDir.resolve(name)
        if (name.endsWith("/")) { // if (Files.isDirectory(path)) {
          logger.info { "mkdir1: $path" }
          Files.createDirectories(path)
        } else {
          // if (Files.notExists(parent)) { // noticeably poor performance in JDK 8
          path.parent?.takeUnless { it.toFile().exists() }?.also {
            logger.info { "mkdir2: $it" }
            Files.createDirectories(it)
          }
          logger.info { "copy: $path" }
          Files.copy(zipFile.getInputStream(entry), path, StandardCopyOption.REPLACE_EXISTING)
        }
      }
    }
  }
}

private class TextAreaOutputStream(private val textArea: JTextArea) : OutputStream() {
  private val buffer = ByteArrayOutputStream()

  @Throws(IOException::class)
  override fun flush() {
    textArea.append(buffer.toString("UTF-8"))
    buffer.reset()
  }

  override fun write(b: Int) {
    buffer.write(b)
  }

  override fun write(b: ByteArray, off: Int, len: Int) {
    buffer.write(b, off, len)
  }
}

private class TextAreaHandler(os: OutputStream) : StreamHandler() {
  private fun configure() {
    formatter = SimpleFormatter()
    runCatching {
      encoding = "UTF-8"
    }.onFailure {
      // doing a setEncoding with null should always work.
      encoding = null
    }
  }

  @Synchronized
  override fun publish(record: LogRecord?) {
    super.publish(record)
    flush()
  }

  @Synchronized
  override fun close() {
    flush()
  }

  init {
    configure()
    setOutputStream(os)
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
