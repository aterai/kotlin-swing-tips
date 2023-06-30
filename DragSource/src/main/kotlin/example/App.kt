package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

private val label = JLabel()
private val i1 = makeIcon("example/i03-04.gif", "OptionPane.errorIcon")
private val i2 = makeIcon("example/i03-10.gif", "OptionPane.warningIcon")

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

    override fun createTransferable(c: JComponent): Transferable? = file?.let {
      TempFileTransferable(it)
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
  label.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      val c = e.component
      if (c is JComponent && file != null) {
        c.transferHandler.exportAsDrag(c, e, TransferHandler.COPY)
      }
    }
  })

  val button = JButton("Create Temp File")
  button.addActionListener { e ->
    file = runCatching {
      File.createTempFile("test", ".tmp").also {
        it.deleteOnExit()
      }
    }.onFailure {
      val c = e.source as? JComponent
      UIManager.getLookAndFeel().provideErrorFeedback(c)
      val msg = "Could not create file."
      JOptionPane.showMessageDialog(c?.rootPane, msg, "Error", JOptionPane.ERROR_MESSAGE)
    }.getOrNull()
  }

  val clearButton = JButton("Clear")
  clearButton.addActionListener {
    file = null
    label.repaint()
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

private fun makeIcon(path: String, key: String): Icon {
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  return url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) }
    ?: UIManager.getIcon(key)
}

private class TempFileTransferable(private val file: File?) : Transferable {
  override fun getTransferData(flavor: DataFlavor) = listOf(file)

  override fun getTransferDataFlavors() = arrayOf(DataFlavor.javaFileListFlavor)

  override fun isDataFlavorSupported(flavor: DataFlavor) =
    flavor.equals(DataFlavor.javaFileListFlavor)
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
