package example

import java.awt.*
import java.io.File
import java.nio.file.Files
import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

private const val FILE_NAME = "example.txt"
private val keepModel = SpinnerNumberModel(0, 0, 6, 1)
private val rotationModel = SpinnerNumberModel(2, 0, 6, 1)
private val keepSpinner = JSpinner(keepModel)
private val rotationSpinner = JSpinner(rotationModel)
private val totalLabel = JLabel("2", SwingConstants.RIGHT)
private val jtp = JTextPane()

fun createUI(): Component {
  jtp.isEditable = false
  val d = jtp.styledDocument
  val s = d.getStyle(StyleContext.DEFAULT_STYLE)
  StyleConstants.setForeground(
    d.addStyle(MessageType.ERROR.toString(), s),
    Color.RED,
  )
  StyleConstants.setForeground(
    d.addStyle(MessageType.DETAIL.toString(), s),
    Color.BLUE,
  )

  val createButton = JButton("Create new $FILE_NAME")
  createButton.addActionListener { addActionPerformed() }

  val clearButton = JButton("clear")
  clearButton.addActionListener { jtp.text = "" }

  val box = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
    it.add(Box.createHorizontalGlue())
    it.add(createButton)
    it.add(Box.createHorizontalStrut(5))
    it.add(clearButton)
  }

  val keepEditor = JSpinner.NumberEditor(keepSpinner, "0")
  keepEditor.textField.isEditable = false
  keepSpinner.editor = keepEditor

  val rotationEditor = JSpinner.NumberEditor(rotationSpinner, "0")
  rotationEditor.textField.isEditable = false
  rotationSpinner.editor = rotationEditor

  val cl = ChangeListener {
    val total = keepModel.number.toInt() + rotationModel.number.toInt()
    totalLabel.text = total.toString()
  }
  keepModel.addChangeListener(cl)
  rotationModel.addChangeListener(cl)
  totalLabel.border = BorderFactory.createEmptyBorder(0, 0, 0, 16)

  val scroll = JScrollPane(jtp).also {
    it.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    it.verticalScrollBar.unitIncrement = 25
  }

  return JPanel(BorderLayout()).also {
    it.add(createSettingsPanel(), BorderLayout.NORTH)
    it.add(scroll)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addActionPerformed() {
  val file = File(System.getProperty("java.io.tmpdir"), FILE_NAME)
  val keepCount = keepModel.number.toInt()
  val rotationCount = rotationModel.number.toInt()
  object : BackgroundTask(file, keepCount, rotationCount) {
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
            makeMessage("Failed to create backup file.", MessageType.ERROR),
          )

          newFile.createNewFile() -> append(
            makeMessage("Generated ${newFile.name}.", MessageType.REGULAR),
          )

          else -> append(
            makeMessage("Failed to generate ${newFile.name}.", MessageType.ERROR),
          )
        }
      }.onFailure {
        if (it is InterruptedException) {
          Thread.currentThread().interrupt()
        }
        append(makeMessage(it.message, MessageType.ERROR))
      }
      append(makeMessage("----------------------------------", MessageType.REGULAR))
    }
  }.execute()
}

private fun createSettingsPanel(): Component {
  // val panel = Box.createHorizontalBox()
  val panel = JPanel(GridLayout(3, 2, 5, 5))
  panel.add(JLabel("Number of backups to keep:", SwingConstants.RIGHT))
  panel.add(keepSpinner)
  panel.add(JLabel("Number of backups to rotate:", SwingConstants.RIGHT))
  panel.add(rotationSpinner)
  panel.add(JLabel("Total number of backups:", SwingConstants.RIGHT))
  panel.add(totalLabel)
  return panel
}

fun append(msg: Message) {
  val doc = jtp.styledDocument
  runCatching {
    doc.insertString(doc.length, "${msg.text}\n", doc.getStyle(msg.type.toString()))
  }
}

enum class MessageType {
  REGULAR,
  ERROR,
  DETAIL,
}

data class Message(
  val text: String?,
  val type: MessageType,
)

private open class BackgroundTask(
  private val originalFile: File,
  private val keepCount: Int,
  private val rotationCount: Int,
) : SwingWorker<File, Message>() {
  override fun doInBackground(): File? = runCatching {
    if (originalFile.exists()) {
      if (keepCount == 0 && rotationCount == 0) { // = backup off
        Files.delete(originalFile.toPath())
      } else {
        createBackup(originalFile)
      }
    }
    originalFile
  }.onFailure {
    publish(makeMessage(it.message, MessageType.ERROR))
  }.getOrNull()

  private fun createBackup(file: File) {
    val unusedBackup = findUnusedBackupFile(file)
    if (unusedBackup == null) {
      deleteOldestRotatingBackup(file)
      shiftBackupFileNumbers(file)
      renameFile(file, makeBackupFile(file, keepCount + rotationCount))
    } else {
      renameFile(file, unusedBackup)
    }
  }

  private fun findUnusedBackupFile(file: File) =
    (1..keepCount + rotationCount)
      .asSequence()
      .map { makeBackupFile(file, it) }
      .firstOrNull { !it.exists() }

  private fun deleteOldestRotatingBackup(file: File) {
    val oldest = makeBackupFile(file, keepCount + 1)
    publish(makeMessage("Delete old backup file", MessageType.REGULAR))
    publish(makeMessage("  del:" + oldest.absolutePath, MessageType.DETAIL))
    Files.delete(oldest.toPath())
  }

  private fun shiftBackupFileNumbers(file: File) {
    for (i in keepCount + 2..keepCount + rotationCount) {
      val src = makeBackupFile(file, i)
      val dst = makeBackupFile(file, i - 1)
      val path = src.toPath()
      Files.move(path, path.resolveSibling(dst.name))
      publish(makeMessage("Update old backup file numbers", MessageType.REGULAR))
      publish(makeMessage("  ${src.name} -> ${dst.name}", MessageType.DETAIL))
    }
  }

  private fun renameFile(
    src: File,
    dst: File,
  ) {
    publish(makeMessage("Rename the original file", MessageType.REGULAR))
    publish(
      makeMessage("  %s -> %s".format(src.name, dst.name), MessageType.DETAIL),
    )
    val path = src.toPath()
    Files.move(path, path.resolveSibling(dst.name))
  }

  companion object {
    fun makeMessage(
      text: String?,
      type: MessageType,
    ) = Message(text, type)

    private fun makeBackupFileName(
      name: String,
      num: Int,
    ) = "%s.%d~".format(name, num)

    private fun makeBackupFile(
      file: File,
      idx: Int,
    ) = File(file.parentFile, makeBackupFileName(file.name, idx))
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
