package example

import java.awt.*
import java.awt.AlphaComposite
import javax.swing.*


class MainPanel : JPanel(BorderLayout()) {
  init {
    val cardLayout = CardLayout(50, 5)

    val cards: JPanel = object : JPanel(cardLayout) {
      override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        getComponents().first { it.isVisible() }?.also {
          paintSideComponents(g, getComponentZOrder(it))
        }
      }

      private fun paintSideComponents(g: Graphics, idx: Int) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.setComposite(AlphaComposite.SrcOver.derive(.5f))
        val insets = getInsets()
        val hgap = cardLayout.getHgap()
        val vgap = cardLayout.getVgap()
        val nc = getComponentCount()
        val cw = width - (hgap * 2 + insets.left + insets.right)
        // val ch = getHeight() - (vgap * 2 + insets.top + insets.bottom)
        val gap = 10
        val prev = getComponent(if (idx > 0) idx - 1 else nc - 1)
        g2.translate(hgap + insets.left - cw - gap, vgap + insets.top)
        prev.print(g2)
        val next = getComponent((idx + 1) % nc)
        g2.translate((cw + gap) * 2, 0)
        next.print(g2)
        g2.dispose()
      }
    }
    cards.add(JScrollPane(JTree()), "JTree")
    cards.add(JSplitPane(), "JSplitPane")
    cards.add(JScrollPane(JTable(9, 3)), "JTable")
    cards.add(JButton("JButton"), "JButton")

    val prevButton = JButton("Previous")
    prevButton.addActionListener {
      cardLayout.previous(cards)
      cards.repaint()
    }
    val nextButton = JButton("Next")
    nextButton.addActionListener {
      cardLayout.next(cards)
      cards.repaint()
    }
    val box: Box = Box.createHorizontalBox()
    box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    box.add(prevButton)
    box.add(Box.createHorizontalGlue())
    box.add(nextButton)
    add(cards)
    add(box, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
