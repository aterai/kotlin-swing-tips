package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.* // ktlint-disable no-wildcard-imports

private val label = JLabel()
private val cl = Thread.currentThread().contextClassLoader
private val u1 = cl.getResource("example/i03-04.gif")
private val u2 = cl.getResource("example/i03-10.gif")
private val i1 = ImageIcon(u1)
private val i2 = ImageIcon(u2)

private var file: File? = null
  set(file) {
    if (file == null) {
      label.icon = i1
      label.text = "tmpFile#exists(): false"
    } else {
      label.icon = i2
      label.text = "tmpFile#exists(): true(draggable)"
    }
    field = file
  }

fun makeUI(): Component {
  label.verticalTextPosition = SwingConstants.BOTTOM
  label.verticalAlignment = SwingConstants.CENTER
  label.horizontalTextPosition = SwingConstants.CENTER
  label.horizontalAlignment = SwingConstants.CENTER
  label.border = BorderFactory.createTitledBorder("Drag Source JLabel")
  file = null

  label.transferHandler = object : TransferHandler() {
    override fun getSourceActions(c: JComponent) = COPY_OR_MOVE

    override fun createTransferable(c: JComponent): Transferable? {
      val tmpFile = file
      return if (tmpFile != null) {
        TempFileTransferable(tmpFile)
      } else {
        null
      }
    }

    override fun exportDone(c: JComponent, data: Transferable, action: Int) {
      cleanup(c, action == MOVE)
    }

    private fun cleanup(c: JComponent, isMoved: Boolean) {
      if (isMoved) {
        file = null
        c.repaint()
      }
    }
  }
  val ml = object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      val c = e.component as? JComponent ?: return
      c.transferHandler.exportAsDrag(c, e, TransferHandler.COPY)
    }
  }
  label.addMouseListener(ml)

  val button = JButton("Create Temp File")
  button.addActionListener {
    file = runCatching {
      File.createTempFile("test", ".tmp").also {
        it.deleteOnExit()
      }
    }.onFailure {
      Toolkit.getDefaultToolkit().beep()
      val msg = "Could not create file."
      JOptionPane.showMessageDialog(label.rootPane, msg, "Error", JOptionPane.ERROR_MESSAGE)
    }.getOrNull()
  }

  val clearButton = JButton("Clear")
  clearButton.addActionListener {
    file = null
    label.rootPane.repaint()
  }

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(Box.createHorizontalGlue())
  box.add(button)
  box.add(Box.createHorizontalStrut(2))
  box.add(clearButton)

  return JPanel(BorderLayout()).also {
    it.add(label)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TempFileTransferable(private val file: File?) : Transferable {
  override fun getTransferData(flavor: DataFlavor) = listOf(file)

  override fun getTransferDataFlavors() = arrayOf(DataFlavor.javaFileListFlavor)

  override fun isDataFlavorSupported(flavor: DataFlavor) = flavor.equals(DataFlavor.javaFileListFlavor)
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
