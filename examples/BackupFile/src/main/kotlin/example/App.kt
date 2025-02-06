package example

import java.awt.*
import java.io.File
import java.io.IOException
import java.nio.file.Files
import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

private const val FILE_NAME = "example.txt"
private val model1 = SpinnerNumberModel(0, 0, 6, 1)
private val model2 = SpinnerNumberModel(2, 0, 6, 1)
private val spinner1 = JSpinner(model1)
private val spinner2 = JSpinner(model2)
private val label = JLabel("2", SwingConstants.RIGHT)
private val jtp = JTextPane()

fun makeUI(): Component {
  jtp.isEditable = false
  val d = jtp.styledDocument
  val s = d.getStyle(StyleContext.DEFAULT_STYLE)
  StyleConstants.setForeground(d.addStyle(MessageType.ERROR.toString(), s), Color.RED)
  StyleConstants.setForeground(d.addStyle(MessageType.BLUE.toString(), s), Color.BLUE)

  val ok = JButton("Create new $FILE_NAME")
  ok.addActionListener { addActionPerformed() }

  val clear = JButton("clear")
  clear.addActionListener { jtp.text = "" }

  val box = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
    it.add(Box.createHorizontalGlue())
    it.add(ok)
    it.add(Box.createHorizontalStrut(5))
    it.add(clear)
  }

  val editor1 = JSpinner.NumberEditor(spinner1, "0")
  editor1.textField.isEditable = false
  spinner1.editor = editor1

  val editor2 = JSpinner.NumberEditor(spinner2, "0")
  editor2.textField.isEditable = false
  spinner2.editor = editor2

  val cl = ChangeListener {
    label.text = (model1.number.toInt() + model2.number.toInt()).toString()
  }
  model1.addChangeListener(cl)
  model2.addChangeListener(cl)
  label.border = BorderFactory.createEmptyBorder(0, 0, 0, 16)

  val scroll = JScrollPane(jtp).also {
    it.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    it.verticalScrollBar.unitIncrement = 25
  }

  return JPanel(BorderLayout()).also {
    it.add(makeNorthBox(), BorderLayout.NORTH)
    it.add(scroll)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addActionPerformed() {
  val file = File(System.getProperty("java.io.tmpdir"), FILE_NAME)
  object : BackgroundTask(file, model1.number.toInt(), model2.number.toInt()) {
    override fun process(chunks: List<Message>) {
      if (jtp.isDisplayable && !isCancelled) {
        chunks.forEach { append(it) }
      } else {
        cancel(true)
      }
    }

    override fun done() {
      runCatching {
        val newFile = get()
        when {
          newFile == null -> append(
            info("Failed to create backup file.", MessageType.ERROR),
          )
          newFile.createNewFile() -> append(
            info("Generated ${newFile.name}.", MessageType.REGULAR),
          )
          else -> append(
            info("Failed to generate ${newFile.name}.", MessageType.ERROR),
          )
        }
      }.onFailure {
        if (it is InterruptedException) {
          Thread.currentThread().interrupt()
        }
        append(info(it.message, MessageType.ERROR))
      }
      append(info("----------------------------------", MessageType.REGULAR))
    }
  }.execute()
}

private fun makeNorthBox(): Component {
  // val northBox = Box.createHorizontalBox()
  val northBox = JPanel(GridLayout(3, 2, 5, 5))
  northBox.add(JLabel("Number of backups to keep:", SwingConstants.RIGHT))
  northBox.add(spinner1)
  northBox.add(JLabel("Number of backups to delete in order:", SwingConstants.RIGHT))
  northBox.add(spinner2)
  northBox.add(JLabel("Total number of backups:", SwingConstants.RIGHT))
  northBox.add(label)
  return northBox
}

fun append(m: Message) {
  val doc = jtp.styledDocument
  runCatching {
    doc.insertString(doc.length, "${m.text}\n", doc.getStyle(m.type.toString()))
  }
}

enum class MessageType {
  REGULAR,
  ERROR,
  BLUE,
}

data class Message(
  val text: String?,
  val type: MessageType,
)

private open class BackgroundTask(
  private val orgFile: File,
  private val oldIndex: Int,
  private val newIndex: Int,
) : SwingWorker<File, Message>() {
  @Suppress("ReturnCount")
  @Throws(IOException::class)
  public override fun doInBackground(): File? {
    if (!orgFile.exists()) {
      return orgFile
    }
    val newFileName = orgFile.absolutePath
    if (oldIndex == 0 && newIndex == 0) { // = backup off
      return runCatching {
        Files.delete(orgFile.toPath())
        File(newFileName)
      }.onFailure {
        publish(info(it.message, MessageType.ERROR))
      }.getOrNull()
    }
    val tmpFile = renameAndBackup(orgFile, newFileName)
    if (tmpFile != null) {
      return tmpFile
    }
    return if (renameAndShiftBackup(orgFile)) {
      File(newFileName)
    } else {
      null
    }
  }

  @Throws(IOException::class)
  private fun renameAndBackup(
    file: File,
    newFileName: String,
  ): File? {
    var simpleRename = false
    var testFile: File? = null
    for (i in 1..oldIndex) {
      testFile = createBackupFile(file, i)
      if (!testFile.exists()) {
        simpleRename = true
        break
      }
    }
    if (!simpleRename) {
      for (i in oldIndex + 1..oldIndex + newIndex) {
        testFile = createBackupFile(file, i)
        if (!testFile.exists()) {
          simpleRename = true
          break
        }
      }
    }
    if (testFile != null && simpleRename) {
      val path = file.toPath()
      return runCatching {
        publish(info("Rename the older file", MessageType.REGULAR))
        publish(info("  %s -> %s".format(file.name, testFile.name), MessageType.BLUE))
        Files.move(path, path.resolveSibling(testFile.name))
        File(newFileName)
      }.onFailure {
        publish(info(it.message, MessageType.ERROR))
      }.getOrThrow()
    }
    return null
  }

  @Suppress("ReturnCount")
  private fun renameAndShiftBackup(file: File): Boolean {
    val tmpFile3 = File(file.parentFile, getBackupName(file.name, oldIndex + 1))
    publish(info("Delete old backup file", MessageType.REGULAR))
    publish(info("  del:" + tmpFile3.absolutePath, MessageType.BLUE))
    runCatching {
      Files.delete(tmpFile3.toPath())
    }.onFailure {
      publish(info(it.message, MessageType.ERROR))
      return false
    }
    for (i in oldIndex + 2..oldIndex + newIndex) {
      val tmpFile1 = createBackupFile(file, i)
      val tmpFile2 = createBackupFile(file, i - 1)
      val oldPath = tmpFile1.toPath()
      runCatching {
        Files.move(oldPath, oldPath.resolveSibling(tmpFile2.name))
      }.onFailure {
        publish(info(it.message, MessageType.ERROR))
        return false
      }
      publish(info("Update old backup file numbers", MessageType.REGULAR))
      publish(info("  " + tmpFile1.name + " -> " + tmpFile2.name, MessageType.BLUE))
    }
    val tmpFile = File(file.parentFile, getBackupName(file.name, oldIndex + newIndex))
    publish(info("Rename the older file", MessageType.REGULAR))
    publish(info("  " + file.name + " -> " + tmpFile.name, MessageType.BLUE))
    val path = file.toPath()
    return runCatching {
      Files.move(path, path.resolveSibling(tmpFile.name))
    }.onFailure {
      publish(info(it.message, MessageType.ERROR))
    }.isSuccess
  }

  companion object {
    fun info(
      text: String?,
      type: MessageType,
    ) = Message(text, type)

    private fun getBackupName(
      name: String,
      num: Int,
    ) = "%s.%d~".format(name, num)

    private fun createBackupFile(
      file: File,
      idx: Int,
    ) = File(file.parentFile, getBackupName(file.name, idx))
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
