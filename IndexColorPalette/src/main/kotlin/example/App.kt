package example

import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.DataBuffer
import java.awt.image.IndexColorModel
import javax.imageio.ImageIO
import javax.swing.*

private val CELL_SIZE = Dimension(8, 8)

fun makeUI(): Component {
  val p = JPanel()
  val label1 = JLabel()
  p.add(label1)
  val label2 = JLabel()
  p.add(label2)

  val url = Thread.currentThread().contextClassLoader.getResource("example/duke.gif")
  val image = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()

  label1.icon = ImageIcon(image)

  val colorModel = image.colorModel
  val indexColorModel = colorModel as? IndexColorModel
  val transIndex = indexColorModel?.transparentPixel ?: -1
  val w = image.width
  val h = image.height
  val dataBuffer = image.raster.dataBuffer
  label2.icon = ImageIcon(makeTestImage(dataBuffer, colorModel, w, h, transIndex))

  val box = JPanel(GridBagLayout())
  if (indexColorModel != null) {
    val palette = object : JList<IndexedColor>(PaletteListModel(indexColorModel)) {
      override fun updateUI() {
        cellRenderer = null
        super.updateUI()
        layoutOrientation = HORIZONTAL_WRAP
        visibleRowCount = 8
        fixedCellWidth = CELL_SIZE.width
        fixedCellHeight = CELL_SIZE.height
        selectionModel.selectionMode = ListSelectionModel.SINGLE_INTERVAL_SELECTION
        border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        val r = cellRenderer
        setCellRenderer { list, value, index, isSelected, cellHasFocus ->
          r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
            (it as? JLabel)?.also { label ->
              label.icon = ColorIcon(value.color)
              label.toolTipText = "index: ${value.index}"
              val bc = if (value.isTransparent) Color.RED else Color.WHITE
              label.border = BorderFactory.createLineBorder(bc)
            }
          }
        }
      }
    }
    box.add(JScrollPane(palette), GridBagConstraints())
  } else {
    box.add(JLabel("No IndexColorModel"), GridBagConstraints())
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTestImage(
  dataBuffer: DataBuffer,
  colorModel: ColorModel,
  w: Int,
  h: Int,
  transIdx: Int,
): Image {
  val buf = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  for (y in 0..<h) {
    for (x in 0..<w) {
      val arrayIndex = x + y * w
      val colorIndex = dataBuffer.getElem(arrayIndex)
      if (transIdx == colorIndex) {
        buf.setRGB(x, y, Color.RED.rgb)
      } else {
        buf.setRGB(x, y, colorModel.getRGB(colorIndex))
      }
    }
  }
  return buf
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private data class IndexedColor(
  val index: Int,
  val color: Color,
  val isTransparent: Boolean,
)

private class PaletteListModel(
  private val model: IndexColorModel,
) : AbstractListModel<IndexedColor>() {
  override fun getSize() = model.mapSize

  override fun getElementAt(i: Int): IndexedColor {
    val isTransparent = i == model.transparentPixel
    return IndexedColor(i, Color(model.getRGB(i)), isTransparent)
  }
}

private class ColorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = CELL_SIZE.width - 2

  override fun getIconHeight() = CELL_SIZE.height - 2
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
