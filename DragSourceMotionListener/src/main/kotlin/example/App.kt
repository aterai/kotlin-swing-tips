package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import javax.swing.*

fun makeUI() {
  val f1 = JFrame("@title@")
  val f2 = JFrame()
  f1.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
  f2.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

  val p1 = DragPanel()
  val p2 = DragPanel()

  p1.border = BorderFactory.createLineBorder(Color.BLACK)
  p2.border = BorderFactory.createLineBorder(Color.BLACK)

  p1.add(JLabel(UIManager.getIcon("OptionPane.warningIcon")))
  p1.add(JLabel(UIManager.getIcon("OptionPane.questionIcon")))
  p1.add(JLabel(UIManager.getIcon("OptionPane.informationIcon")))
  p1.add(JLabel(UIManager.getIcon("OptionPane.errorIcon")))
  p1.add(JLabel("Text"))

  val handler = Handler()
  p1.addMouseListener(handler)
  p2.addMouseListener(handler)

  val th = LabelTransferHandler()
  p1.transferHandler = th
  p2.transferHandler = th

  val p = JPanel(GridLayout(2, 1))
  p.add(JScrollPane(JTextArea()))
  p.add(p2)

  f1.contentPane.add(p1)
  f2.contentPane.add(p)
  f1.setSize(320, 240)
  f2.setSize(320, 240)
  f1.setLocationRelativeTo(null)

  val pt = f1.location
  pt.translate(340, 0)
  f2.location = pt
  f1.isVisible = true
  f2.isVisible = true
}

private class DragPanel : JPanel() {
  var draggingLabel: JLabel? = null
}

private class Handler : MouseAdapter() {
  override fun mousePressed(e: MouseEvent) {
    (e.component as? DragPanel)?.also { p ->
      val c = SwingUtilities.getDeepestComponentAt(p, e.x, e.y)
      if (c is JLabel) {
        p.draggingLabel = c
        p.transferHandler.exportAsDrag(p, e, TransferHandler.MOVE)
      }
    }
  }
}

private class LabelTransferHandler : TransferHandler("Text") {
  private val localObjectFlavor = DataFlavor(DragPanel::class.java, "DragPanel")
  private val label = object : JLabel() {
    override fun contains(
      x: Int,
      y: Int,
    ) = false
  }
  private val window = JWindow()

  init {
    window.add(label)
    window.background = Color(0x0, true)
    DragSource.getDefaultDragSource().addDragSourceMotionListener {
      window.location = it.location
    }
  }

  override fun createTransferable(c: JComponent): Transferable =
    LabelTransferable(localObjectFlavor, c as? DragPanel)

  override fun canImport(support: TransferSupport) =
    support.isDrop && support.isDataFlavorSupported(localObjectFlavor)

  override fun getSourceActions(c: JComponent): Int {
    (c as? DragPanel)?.draggingLabel?.also {
      label.icon = it.icon
      label.text = it.text
      window.pack()
      val pt = it.location
      SwingUtilities.convertPointToScreen(pt, c)
      window.location = pt
      window.isVisible = true
    }
    return MOVE
  }

  override fun importData(support: TransferSupport) = runCatching {
    val l = JLabel()
    (support.transferable.getTransferData(localObjectFlavor) as? DragPanel)?.also {
      l.icon = it.draggingLabel?.icon
      l.text = it.draggingLabel?.text
    }
    (support.component as? DragPanel)?.also {
      it.add(l)
      it.revalidate()
    }
    true
  }.isSuccess

  override fun exportDone(
    c: JComponent,
    data: Transferable,
    action: Int,
  ) {
    val src = c as? DragPanel ?: return
    if (action == MOVE) {
      src.remove(src.draggingLabel)
      src.revalidate()
      src.repaint()
    }
    src.draggingLabel = null
    window.isVisible = false
  }
}

private class LabelTransferable(
  private val localObjectFlavor: DataFlavor,
  private val panel: DragPanel?,
) : Transferable {
  private val ss: StringSelection?

  init {
    val txt = panel?.draggingLabel?.text
    ss = txt?.let { StringSelection(it.trim()) }
  }

  override fun getTransferDataFlavors() = mutableListOf<DataFlavor>().also {
    if (ss != null) {
      it.addAll(ss.transferDataFlavors)
    }
    it.add(localObjectFlavor)
  }.toTypedArray()

  override fun isDataFlavorSupported(f: DataFlavor) = transferDataFlavors.contains(f)

  @Throws(UnsupportedFlavorException::class, IOException::class)
  override fun getTransferData(flavor: DataFlavor) = if (flavor.equals(localObjectFlavor)) {
    panel
  } else {
    ss?.getTransferData(flavor)
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
    makeUI()
  }
}
