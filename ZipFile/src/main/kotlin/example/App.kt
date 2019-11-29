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

class MainPanel : JPanel(BorderLayout()) {
  private fun makeZipPanel(): Component {
    val field = JTextField(20)
    val button = JButton("select directory")
    button.addActionListener {
      val fileChooser = JFileChooser()
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
      val ret = fileChooser.showOpenDialog(button.rootPane)
      if (ret == JFileChooser.APPROVE_OPTION) {
        field.setText(fileChooser.getSelectedFile().getAbsolutePath())
      }
    }
    val button1 = JButton("zip")
    button1.addActionListener {
      val str = field.getText()
      val path = Paths.get(str)
      // if (str.isEmpty() || Files.notExists(path)) { // noticeably poor performance in JDK 8
      if (str.isEmpty() || !path.toFile().exists()) {
        return@addActionListener
      }
      val name = path.getFileName().toString() + ".zip"
      val tgt = path.resolveSibling(name)
      // if (Files.exists(tgt)) { // noticeably poor performance in JDK 8
      if (tgt.toFile().exists()) {
        val m = "<html>%s already exists.<br>Do you want to overwrite it?".format(tgt.toString())
        val rv = JOptionPane.showConfirmDialog(getRootPane(), m, "Zip", JOptionPane.YES_NO_OPTION)
        if (rv != JOptionPane.YES_OPTION) {
          return@addActionListener
        }
      }
      runCatching {
        ZipUtil.zip(path, tgt)
      }.onFailure {
        LOGGER.info { "Cant zip! : $path" }
        Toolkit.getDefaultToolkit().beep()
      }
    }
    val p = JPanel(BorderLayout(5, 2))
    p.setBorder(BorderFactory.createTitledBorder("Zip"))
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
      val ret = fileChooser.showOpenDialog(getRootPane())
      if (ret == JFileChooser.APPROVE_OPTION) {
        field.setText(fileChooser.getSelectedFile().getAbsolutePath())
      }
    }
    val button1 = JButton("unzip")
    button1.addActionListener {
      val str = field.getText()
      makeDestDirPath(str)?.also { destDir ->
        val path = Paths.get(str)
        runCatching {
          // if (Files.exists(destDir)) { // noticeably poor performance in JDK 8
          if (destDir.toFile().exists()) {
            val m = "<html>%s already exists.<br>Do you want to overwrite it?".format(destDir.toString())
            val rv = JOptionPane.showConfirmDialog(getRootPane(), m, "Unzip", JOptionPane.YES_NO_OPTION)
            if (rv != JOptionPane.YES_OPTION) {
              return@addActionListener
            }
          } else {
            LOGGER.info { "mkdir0: $destDir" }
            Files.createDirectories(destDir)
          }
          ZipUtil.unzip(path, destDir)
        }.onFailure {
          LOGGER.info { "Cant unzip! : $path" }
          Toolkit.getDefaultToolkit().beep()
        }
      }
    }
    val p = JPanel(BorderLayout(5, 2))
    p.setBorder(BorderFactory.createTitledBorder("Unzip"))
    p.add(field)
    p.add(button, BorderLayout.EAST)
    p.add(button1, BorderLayout.SOUTH)
    return p
  }

  init {
    LOGGER.setUseParentHandlers(false)
    val textArea = JTextArea()
    textArea.setEditable(false)
    LOGGER.addHandler(TextAreaHandler(TextAreaOutputStream(textArea)))
    val p = JPanel(GridLayout(2, 1, 10, 10))
    p.add(makeZipPanel())
    p.add(makeUnzipPanel())
    add(p, BorderLayout.NORTH)
    add(JScrollPane(textArea))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeDestDirPath(text: String): Path? {
    val path = Paths.get(text)
    // if (str.isEmpty() || Files.notExists(path)) { // noticeably poor performance in JDK 8
    if (text.isEmpty() || !path.toFile().exists()) {
      return null
    }
    var name = path.getFileName().toString()
    val lastDotPos = name.lastIndexOf('.')
    if (lastDotPos > 0) {
      name = name.substring(0, lastDotPos)
    }
    return path.resolveSibling(name)
  }

  companion object {
    val LOGGER_NAME: String = MethodHandles.lookup().lookupClass().getName()
    private val LOGGER = Logger.getLogger(LOGGER_NAME)
  }
}

object ZipUtil {
  private val logger = Logger.getLogger(MainPanel.LOGGER_NAME)
  @Throws(IOException::class)
  fun zip(srcDir: Path, zip: Path) {
    // try (Stream<Path> s = Files.walk(srcDir).filter(Files::isRegularFile)) { // noticeably poor performance in JDK 8
    Files.walk(srcDir)
      .filter { f: Path -> f.toFile().isFile() }
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
        val name = entry.getName()
        val path = destDir.resolve(name)
        if (name.endsWith("/")) { // if (Files.isDirectory(path)) {
          logger.info { "mkdir1: $path" }
          Files.createDirectories(path)
        } else {
          // if (Files.notExists(parent)) { // noticeably poor performance in JDK 8
          path.getParent()?.takeUnless { it.toFile().exists() }?.also {
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

class TextAreaOutputStream(private val textArea: JTextArea) : OutputStream() {
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

class TextAreaHandler(os: OutputStream) : StreamHandler() {
  private fun configure() {
    setFormatter(SimpleFormatter())
    runCatching {
      setEncoding("UTF-8")
    }.onFailure {
      // doing a setEncoding with null should always work.
      setEncoding(null)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
