package example

import java.awt.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ExecutionException
import javax.swing.*

private const val MAX_HISTORY = 4
private val dirCombo = JComboBox<String>()
private val fileChooser = JFileChooser()
private val textArea = JTextArea()
private val progressBar = JProgressBar()
private val statusPanel = JPanel(BorderLayout())
private val searchButton = JButton("Search")
private val cancelButton = JButton("Cancel")
private val chooseButton = JButton("Choose...")
private var worker: SwingWorker<String, Message>? = null

fun createUI(): Component {
  val model = DefaultComboBoxModel<String>()
  model.addElement(System.getProperty("user.dir"))
  dirCombo.model = model
  dirCombo.isFocusable = false
  textArea.isEditable = false
  statusPanel.add(progressBar)
  statusPanel.isVisible = false
  cancelButton.isEnabled = false

  searchButton.addActionListener { searchActionPerformed() }
  cancelButton.addActionListener { cancelActionPerformed() }
  chooseButton.addActionListener { chooseActionPerformed() }

  val dirPanel = JPanel(BorderLayout(5, 5)).also {
    it.add(JLabel("Search folder:"), BorderLayout.WEST)
    it.add(dirCombo)
    it.add(chooseButton, BorderLayout.EAST)
  }

  val buttonBox = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(Box.createHorizontalGlue())
    it.add(searchButton)
    it.add(Box.createHorizontalStrut(2))
    it.add(cancelButton)
  }

  val panel = JPanel(BorderLayout()).also {
    it.add(dirPanel, BorderLayout.NORTH)
    it.add(buttonBox, BorderLayout.SOUTH)
  }

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(JScrollPane(textArea))
    it.add(panel, BorderLayout.NORTH)
    it.add(statusPanel, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getSelectedDirectory() = dirCombo.selectedItem?.toString() ?: ""

private fun searchActionPerformed() {
  val dir = File(getSelectedDirectory())
  // Check the directory before disabling the components,
  // otherwise they will never be re-enabled because no worker is executed
  if (dir.isDirectory) {
    updateComponentStatus(true)
    executeWorker(dir.toPath())
  } else {
    textArea.text = "The directory does not exist."
  }
}

private fun cancelActionPerformed() {
  worker?.takeUnless { it.isDone }?.cancel(true)
}

private fun chooseActionPerformed() {
  fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
  fileChooser.selectedFile = File(getSelectedDirectory())
  val c = dirCombo.rootPane
  textArea.text = when (fileChooser.showOpenDialog(c)) {
    JFileChooser.APPROVE_OPTION -> {
      fileChooser.selectedFile
        ?.takeIf { it.isDirectory }
        ?.absolutePath
        ?.also { addItem(dirCombo, it, MAX_HISTORY) }
        ?: "Please select directory."
    }

    JFileChooser.CANCEL_OPTION -> {
      "JFileChooser cancelled."
    }

    else -> {
      "JFileChooser error.".also {
        UIManager.getLookAndFeel().provideErrorFeedback(c)
      }
    }
  }
}

private fun updateComponentStatus(start: Boolean) {
  if (start) {
    addItem(dirCombo, getSelectedDirectory(), MAX_HISTORY)
    progressBar.isIndeterminate = true
    textArea.text = ""
  }
  statusPanel.isVisible = start
  dirCombo.isEnabled = !start
  chooseButton.isEnabled = !start
  searchButton.isEnabled = !start
  cancelButton.isEnabled = start
}

private fun addItem(
  combo: JComboBox<String>,
  str: String?,
  max: Int,
) {
  val model = combo.model as? DefaultComboBoxModel<String>
  if (str.isNullOrEmpty() || model == null) {
    return
  }
  combo.isVisible = false
  model.removeElement(str)
  model.insertElementAt(str, 0)
  if (model.size > max) {
    model.removeElementAt(max)
  }
  combo.selectedIndex = 0
  combo.isVisible = true
}

private fun executeWorker(dir: Path) {
  worker = object : RecursiveFileSearchTask(dir) {
    override fun process(chunks: List<Message>) {
      if (statusPanel.isDisplayable && !isCancelled) {
        chunks.forEach { updateMessage(it) }
      } else {
        cancel(true)
      }
    }

    override fun done() {
      if (statusPanel.isDisplayable) {
        updateComponentStatus(false)
        appendLine("----------------")
        appendLine(getDoneMessage())
      }
    }
  }.also {
    it.addPropertyChangeListener(ProgressListener(progressBar))
    it.execute()
  }
}

private fun updateMessage(msg: Message) {
  if (msg.append) {
    appendLine(msg.text)
  } else {
    textArea.text = "${msg.text}\n"
  }
}

private fun appendLine(str: String) {
  textArea.append("$str\n")
  textArea.caretPosition = textArea.document.length
}

private open class RecursiveFileSearchTask(
  private val dir: Path,
) : SwingWorker<String, Message>() {
  @Throws(IOException::class, InterruptedException::class)
  override fun doInBackground(): String {
    val list = searchFiles(dir)
    val lengthOfTask = list.size
    publish(Message("Length Of Task: $lengthOfTask", false))
    publish(Message("----------------", true))
    var idx = 0
    while (idx < lengthOfTask && !isCancelled) {
      processFile(list, idx)
      idx++
    }
    return "Done"
  }

  @Throws(InterruptedException::class)
  protected fun processFile(
    list: List<Path>,
    idx: Int,
  ) {
    Thread.sleep(10)
    val lengthOfTask = list.size
    val current = idx + 1
    publish(Message("$current/$lengthOfTask, ${list[idx]}", true))
    progress = 100 * current / lengthOfTask
  }

  // Walking the File Tree (The Java™ Tutorials > Essential Classes > Basic I/O)
  // https://docs.oracle.com/javase/tutorial/essential/io/walk.html
  @Throws(IOException::class)
  private fun searchFiles(start: Path): List<Path> {
    val list = mutableListOf<Path>()
    val visitor = object : SimpleFileVisitor<Path>() {
      override fun visitFile(
        file: Path,
        attrs: BasicFileAttributes,
      ): FileVisitResult {
        if (attrs.isRegularFile) {
          list.add(file)
          if (list.size % 100 == 0) {
            publish(Message("Results: ${list.size}", false))
          }
        }
        return nextResult()
      }

      // Skip unreadable files or directories (e.g. AccessDeniedException)
      // instead of aborting the whole search
      override fun visitFileFailed(
        file: Path,
        exc: IOException,
      ) = nextResult()
    }
    Files.walkFileTree(start, visitor)
    return list
  }

  private fun nextResult() = if (isCancelled) {
    FileVisitResult.TERMINATE
  } else {
    FileVisitResult.CONTINUE
  }

  protected fun getDoneMessage() = if (isCancelled) {
    "Cancelled"
  } else {
    try {
      get()
    } catch (ex: InterruptedException) {
      Thread.currentThread().interrupt()
      "Interrupted"
    } catch (ex: ExecutionException) {
      "ExecutionException: ${ex.message}"
    }
  }
}

private class ProgressListener(
  private val progressBar: JProgressBar,
) : PropertyChangeListener {
  init {
    progressBar.value = 0
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    if ("progress" == e.propertyName) {
      progressBar.isIndeterminate = false
      progressBar.value = e.newValue as? Int ?: 0
    }
  }
}

private data class Message(
  val text: String,
  val append: Boolean,
)

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
