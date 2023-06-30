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
import javax.swing.*

private val dirCombo = JComboBox<String>()
private val fileChooser = JFileChooser()
private val textArea = JTextArea()
private val progress = JProgressBar()
private val statusPanel = JPanel(BorderLayout())
private val runButton = JButton("Run")
private val cancelButton = JButton("Cancel")
private val openButton = JButton("Choose...")
private var worker: SwingWorker<String, Message>? = null

fun makeUI(): Component {
  val model = DefaultComboBoxModel<String>()
  model.addElement(System.getProperty("user.dir"))
  dirCombo.model = model
  dirCombo.isFocusable = false
  textArea.isEditable = false
  statusPanel.add(progress)
  statusPanel.isVisible = false
  runButton.addActionListener {
    updateComponentStatus(true)
    executeWorker()
  }
  cancelButton.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
    worker = null
  }
  initOpenButton()

  val box1 = JPanel(BorderLayout(5, 5)).also {
    it.add(JLabel("Search folder:"), BorderLayout.WEST)
    it.add(dirCombo)
    it.add(openButton, BorderLayout.EAST)
  }

  val box2 = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(Box.createHorizontalGlue())
    it.add(runButton)
    it.add(Box.createHorizontalStrut(2))
    it.add(cancelButton)
  }

  val panel = JPanel(BorderLayout()).also {
    it.add(box1, BorderLayout.NORTH)
    it.add(box2, BorderLayout.SOUTH)
  }

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(JScrollPane(textArea))
    it.add(panel, BorderLayout.NORTH)
    it.add(statusPanel, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initOpenButton() {
  openButton.addActionListener {
    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    fileChooser.selectedFile = File(dirCombo.editor.item.toString())
    val c = dirCombo.rootPane
    textArea.text = when (fileChooser.showOpenDialog(c)) {
      JFileChooser.APPROVE_OPTION -> fileChooser.selectedFile?.takeIf { it.isDirectory }?.let {
        val path = it.absolutePath
        addItem(dirCombo, path, 4)
        statusPanel.repaint()
        path
      } ?: "Please select directory."
      JFileChooser.CANCEL_OPTION -> "JFileChooser cancelled."
      else -> "JFileChooser error.".also {
        UIManager.getLookAndFeel().provideErrorFeedback(c)
      }
    }
  }
}

private open class FileSearchTask(dir: File) : RecursiveFileSearchTask(dir) {
  override fun process(chunks: List<Message>) {
    if (isCancelled) {
      return
    }
    if (!statusPanel.isDisplayable) {
      cancel(true)
      return
    }
    chunks.forEach { processChunks(it) }
  }

  override fun done() {
    if (!statusPanel.isDisplayable) {
      cancel(true)
      return
    }
    updateComponentStatus(false)
    appendLine("----------------")
    val text = if (isCancelled) {
      "Cancelled"
    } else {
      runCatching {
        get()
      }.onFailure {
        if (it is InterruptedException) {
          Thread.currentThread().interrupt()
        }
      }.getOrNull() ?: "Interrupted"
    }
    appendLine(text)
  }
}

fun updateComponentStatus(start: Boolean) {
  if (start) {
    addItem(dirCombo, dirCombo.editor.item.toString())
    statusPanel.isVisible = true
    dirCombo.isEnabled = false
    openButton.isEnabled = false
    runButton.isEnabled = false
    cancelButton.isEnabled = true
    progress.isIndeterminate = true
    textArea.text = ""
  } else {
    dirCombo.isEnabled = true
    openButton.isEnabled = true
    runButton.isEnabled = true
    cancelButton.isEnabled = false
    statusPanel.isVisible = false
  }
}

private fun executeWorker() {
  val dir = File(dirCombo.getItemAt(dirCombo.selectedIndex))
  worker = FileSearchTask(dir).also {
    it.addPropertyChangeListener(ProgressListener(progress))
    it.execute()
  }
}

private fun processChunks(message: Message) {
  if (message.append) {
    appendLine(message.text)
  } else {
    textArea.text = "${message.text}\n"
  }
}

private fun appendLine(str: String?) {
  textArea.append("$str\n")
  textArea.caretPosition = textArea.document.length
}

private fun addItem(dirCombo: JComboBox<String>, str: String?, max: Int = 4) {
  val model = dirCombo.model as? DefaultComboBoxModel<String>
  if (str == null || str.isEmpty() || model == null) {
    return
  }
  dirCombo.isVisible = false
  model.removeElement(str)
  model.insertElementAt(str, 0)
  if (model.size > max) {
    model.removeElementAt(max)
  }
  dirCombo.selectedIndex = 0
  dirCombo.isVisible = true
}

private open class RecursiveFileSearchTask(private val dir: File) : SwingWorker<String, Message>() {
  protected var counter = 0

  @Throws(InterruptedException::class)
  override fun doInBackground() = if (!dir.exists()) {
    publish(Message("The directory does not exist.", true))
    "Error"
  } else {
    fileSearchInBackground(dir.toPath())
  }

  @Throws(InterruptedException::class)
  private fun fileSearchInBackground(path: Path): String {
    val list = mutableListOf<Path>()
    runCatching {
      counter = 0
      recursiveSearch(path, list)
    }.onFailure {
      publish(Message("The search was canceled", true))
      return "Interrupted1"
    }
    firePropertyChange("clear-JTextArea", "", "")
    val lengthOfTask = list.size
    publish(Message("Length Of Task: $lengthOfTask", false))
    publish(Message("----------------", true))
    var idx = 0
    while (idx < lengthOfTask && !isCancelled) {
      doSomething(list, idx)
      idx++
    }
    return "Done"
  }

  @Throws(InterruptedException::class)
  protected fun doSomething(list: List<Path>, idx: Int) {
    val lengthOfTask = list.size
    progress = 100 * idx / lengthOfTask
    Thread.sleep(10)
    val path = list[idx]
    val current = idx + 1
    publish(Message("$current/$lengthOfTask, $path", true))
  }

  @Throws(IOException::class)
  private fun recursiveSearch(dirPath: Path, list: MutableList<Path>) {
    val visitor = object : SimpleFileVisitor<Path>() {
      @Throws(IOException::class)
      override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (Thread.interrupted()) {
          throw IOException("Interrupted2")
        }
        if (attrs.isRegularFile) {
          counter++
          if (counter % 100 == 0) {
            publish(Message("Results:$counter\n", false))
          }
          list.add(file)
        }
        return FileVisitResult.CONTINUE
      }
    }
    Files.walkFileTree(dirPath, visitor)
  }
}

private class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
  init {
    progressBar.value = 0
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    if ("progress" == e.propertyName) {
      progressBar.isIndeterminate = false
      val progress = e.newValue as? Int ?: 0
      progressBar.value = progress
    }
  }
}

private data class Message(val text: String, val append: Boolean)

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
