package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.JTextComponent

private val tabbedPane = JTabbedPane()

private fun makeUI(): Component {
  EventQueue.invokeLater {
    val root = tabbedPane.rootPane
    root.transferHandler = FileTransferHandler()
    val layer = root.layeredPane
    layer.transferHandler = FileTransferHandler()
    val container = root.contentPane
    if (container is JComponent) {
      container.transferHandler = FileTransferHandler()
    }
    val window = SwingUtilities.getWindowAncestor(tabbedPane)
    if (window is JFrame) {
      window.transferHandler = FileTransferHandler()
    }
  }

  val textArea = JTextArea()
  textArea.dragEnabled = true

  tabbedPane.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  tabbedPane.addTab("Default", JScrollPane(textArea))
  addTab(null)

  val button = JButton("open")
  button.addActionListener {
    val chooser = JFileChooser()
    val ret = chooser.showOpenDialog(tabbedPane.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      addTab(chooser.selectedFile)
    }
  }

  val field = JTextField(16)
  field.text = "setDragEnabled(true)"
  field.dragEnabled = true

  val p = JPanel()
  p.add(field)
  p.add(button)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

fun addTab(file: File?) {
  val textArea = JTextArea()
  textArea.dragEnabled = true
  DropTarget(textArea, DnDConstants.ACTION_COPY, FileDropTargetListener(), true)
  if (file != null) {
    runCatching {
      Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8).use {
        textArea.read(it, "File")
        tabbedPane.addTab(file.name, JScrollPane(textArea))
      }
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(textArea)
    }
  } else {
    tabbedPane.addTab("*untitled*", JScrollPane(textArea))
  }
  tabbedPane.selectedIndex = tabbedPane.tabCount - 1
}

@Throws(UnsupportedFlavorException::class, IOException::class)
fun addFile(transferable: Transferable) {
  val list = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*> ?: return
  object : SwingWorker<Void?, Void?>() {
    override fun doInBackground(): Void? {
      for (o in list) {
        if (o is File) {
          addTab(o)
        }
      }
      return null
    }
  }.execute()
}

private class FileDropTargetListener : DropTargetAdapter() {
  override fun drop(e: DropTargetDropEvent) {
    val transferable = e.transferable
    if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
      e.acceptDrop(DnDConstants.ACTION_COPY)
      runCatching {
        addFile(transferable)
        e.dropComplete(true)
      }.onFailure {
        e.dropComplete(false)
      }
    } else {
      val textArea = e.source as? JTextComponent
      val textHandler = textArea?.transferHandler
      textHandler?.importData(textArea, transferable)
    }
  }
}

private class FileTransferHandler : TransferHandler() {
  override fun canImport(support: TransferSupport): Boolean {
    val supported = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
    return support.isDrop && supported
  }

  override fun importData(support: TransferSupport) = runCatching {
    addFile(support.transferable)
    true
  }.getOrNull() ?: false
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
