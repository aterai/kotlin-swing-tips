// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example

import java.awt.*
import javax.swing.*

class MainPanel : JPanel(BorderLayout(5, 5)) {
  init {
    println(UIManager.getLookAndFeelDefaults().get("TabbedPane.selectedLabelShift"))
    println(UIManager.getLookAndFeelDefaults().get("TabbedPane.labelShift"))

    val slsiv = UIManager.getLookAndFeelDefaults().getInt("TabbedPane.selectedLabelShift")
    val slsSpinner = JSpinner(SpinnerNumberModel(slsiv, -5, 5, 1))
    slsSpinner.addChangeListener({ e ->
      val source = e.getSource() as JSpinner
      val offset = source.getValue() as Int
      UIManager.put("TabbedPane.selectedLabelShift", offset)
      SwingUtilities.updateComponentTreeUI(source.getTopLevelAncestor())
    })

    val lsiv = UIManager.getLookAndFeelDefaults().getInt("TabbedPane.labelShift")
    val lsSpinner = JSpinner(SpinnerNumberModel(lsiv, -5, 5, 1))
    lsSpinner.addChangeListener({ e ->
      val source = e.getSource() as JSpinner
      val offset = source.getValue() as Int
      UIManager.put("TabbedPane.labelShift", offset)
      SwingUtilities.updateComponentTreeUI(source.getTopLevelAncestor())
    })

    val box1 = Box.createHorizontalBox()
    box1.setBorder(BorderFactory.createTitledBorder("UIManager.put(\"TabbedPane.selectedLabelShift\", offset)"))
    box1.add(JLabel("offset = "))
    box1.add(slsSpinner)
    box1.add(Box.createHorizontalGlue())

    val box2 = Box.createHorizontalBox()
    box2.setBorder(BorderFactory.createTitledBorder("UIManager.put(\"TabbedPane.labelShift\", offset)"))
    box2.add(JLabel("offset = "))
    box2.add(lsSpinner)
    box2.add(Box.createHorizontalGlue())

    val p = JPanel(GridLayout(2, 1))
    p.add(box1)
    p.add(box2)

    val tabbedPane = JTabbedPane()
    tabbedPane.addTab("title 0", ColorIcon(Color.RED), JScrollPane(JTree()))
    tabbedPane.addTab("title 1", ColorIcon(Color.GREEN), JButton("button"))
    tabbedPane.addTab("title 2", ColorIcon(Color.BLUE), JLabel("label"))
    tabbedPane.addTab("title 3", JPanel())
    tabbedPane.setTabComponentAt(3, JLabel("label", ColorIcon(Color.ORANGE), SwingConstants.LEFT))

    add(p, BorderLayout.NORTH)
    add(tabbedPane)
    setPreferredSize(Dimension(320, 240))
  }
}

internal class ColorIcon(private val color: Color) : Icon {

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.setPaint(color)
    g2.fillRect(0, 0, getIconWidth(), getIconHeight())
    g2.dispose()
  }

  override fun getIconWidth(): Int {
    return 16
  }

  override fun getIconHeight(): Int {
    return 16
  }
}

fun main() {
  EventQueue.invokeLater(object : Runnable {
    override fun run() {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      } catch (ex: ClassNotFoundException) {
        ex.printStackTrace()
      } catch (ex: InstantiationException) {
        ex.printStackTrace()
      } catch (ex: IllegalAccessException) {
        ex.printStackTrace()
      } catch (ex: UnsupportedLookAndFeelException) {
        ex.printStackTrace()
      }
      JFrame().apply {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
        getContentPane().add(MainPanel())
        pack()
        setLocationRelativeTo(null)
        setVisible(true)
      }
    }
  })
}
