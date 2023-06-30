package example

import sun.awt.shell.ShellFolder
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileSystemView

fun makeUI(): Component {
  val smallLabel = object : JLabel() {
    override fun getPreferredSize() = Dimension(16 + 1, 16 + 1)

    override fun getMaximumSize() = preferredSize

    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createLineBorder(Color.GRAY)
      alignmentY = BOTTOM_ALIGNMENT
    }
  }
  val largeLabel = object : JLabel() {
    override fun getPreferredSize() = Dimension(32 + 1, 32 + 1)

    override fun getMaximumSize() = preferredSize

    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createLineBorder(Color.GRAY)
      alignmentY = BOTTOM_ALIGNMENT
    }
  }

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createTitledBorder("drop File")
  box.add(smallLabel)
  box.add(Box.createHorizontalStrut(5))
  box.add(largeLabel)

  val dtl = object : DropTargetAdapter() {
    override fun dragOver(e: DropTargetDragEvent) {
      if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        e.acceptDrag(DnDConstants.ACTION_COPY)
      } else {
        e.rejectDrag()
      }
    }

    override fun drop(e: DropTargetDropEvent) {
      runCatching {
        if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
          e.acceptDrop(DnDConstants.ACTION_COPY)
          (e.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)?.also {
            val file = it[0]
            if (file is File) {
              smallLabel.icon = FileSystemView.getFileSystemView().getSystemIcon(file)
              largeLabel.icon = ImageIcon(ShellFolder.getShellFolder(file).getIcon(true))
            }
            e.dropComplete(true)
          } ?: e.rejectDrop()
        } else {
          e.rejectDrop()
        }
      }.onFailure {
        e.rejectDrop()
      }
    }
  }

  return JPanel().also {
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.add(box)
    val msg = """
      <html>warning: ShellFolder is internal proprietary API<br>
      and may be removed in a future release
    """.trimIndent()
    it.add(JLabel(msg))
    it.dropTarget = DropTarget(it, DnDConstants.ACTION_COPY, dtl, true)
    it.preferredSize = Dimension(320, 240)
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
