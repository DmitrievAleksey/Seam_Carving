import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class ImageEditor(var image: BufferedImage) {

    private val energyOfPixels = Array(image.height) { DoubleArray(image.width) }
    private val trackOfWeightGraph = Array(image.height) { Array(image.width) { Triple(0.0, 0, 0) } }

    /* Creating a negative image: invert all the color components for each pixel (r, g, b)
    * to (255 - r, 255 - g, 255 - b) */
    fun createNegativeImage() {
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val color = Color(image.getRGB(x, y))
                image.setRGB(x, y, Color(255 - color.red,255 - color.green,255 - color.blue).rgb)
            }
        }
    }

    /* Creating a grayscale image: calculate the energy for all pixels of the image (fun dualGradientEnergy)
    * and invert all the color components of the image pixels to the calculated intensity. */
    fun createGrayScaleImage() {
        this.dualGradientEnergy()
        val maxEnergyValue = energyOfPixels.flatMap { line -> line.map { it } }.max()
        if (maxEnergyValue != null) {
            for (x in 0 until image.width) {
                for (y in 0 until image.height) {
                    val intensity = (255.0 * energyOfPixels[y][x] / maxEnergyValue).toInt()
                    image.setRGB(x, y, Color(intensity, intensity, intensity).rgb)
                }
            }
        }
    }

    /* Reduce the image by removing the specified number of vertical(width) and horizontal(height) seams */
    fun resizeImage(width: Int, height: Int) {
        repeat(width) {
            this.removeVerticalSeam()
        }
        repeat(height) {
            this.removeHorizontalSeam()
        }
    }

    /* Removing the found seam on the x-axis (fun xTrackOfMinWeight) with the lowest sum of pixel energies */
    private fun removeVerticalSeam() {
        this.dualGradientEnergy()
        this.xTrackOfMinWeight(energyOfPixels)
        val resizeImage = BufferedImage(image.width - 1, image.height, image.type)
        var x = trackOfWeightGraph.last().map { it.first }.indexOf(
                trackOfWeightGraph.last().map { it.first }.min())
        var y = trackOfWeightGraph.size - 1
        var index = 0
        for (i in image.height - 1 downTo  0) {
            for (j in 0 until image.width) {
                if (j != x) {
                    resizeImage.setRGB(index, i, image.getRGB(j, i))
                    index += 1
                }
            }
            index = 0
            x = trackOfWeightGraph[y][x].third
            y = trackOfWeightGraph[y][x].second
        }
        image = resizeImage
    }

    /* Removing the found seam on the y-axis (fun yTrackOfMinWeight) with the lowest sum of pixel energies */
    private fun removeHorizontalSeam() {
        this.dualGradientEnergy()
        this.yTrackOfMinWeight(energyOfPixels)
        val resizeImage = BufferedImage(image.width, image.height - 1, image.type)
        var x = trackOfWeightGraph[0].size - 1
        var y = trackOfWeightGraph.map { it.last().first }.indexOf(
                trackOfWeightGraph.map { it.last().first }.min())
        var index = 0
        for (i in image.width - 1 downTo  0) {
            for (j in 0 until image.height) {
                if (j != y) {
                    resizeImage.setRGB(i, index, image.getRGB(i, j))
                    index += 1
                }
            }
            index = 0
            y = trackOfWeightGraph[y][x].second
            x = trackOfWeightGraph[y][x].third
        }
        image = resizeImage
    }

    /* Calculate the energy for all pixels of the image: E(x, y) = sqrt(x-gradient + y-gradient) */
    private fun dualGradientEnergy() {
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                energyOfPixels[y][x] = sqrt(xGradient(x, y, image) + yGradient(x, y, image))
            }
        }
    }

    private fun xGradient(x: Int, y: Int, image: BufferedImage): Double {
        val xOfBorder = when (x) {
            0 -> x + 1
            (image.width - 1) -> x - 1
            else -> x
        }
        val colorNextX = Color(image.getRGB(xOfBorder + 1, y))
        val colorPrevX = Color(image.getRGB(xOfBorder - 1, y))
        return abs(colorNextX.red - colorPrevX.red).toDouble().pow(2) +
                abs(colorNextX.green - colorPrevX.green).toDouble().pow(2) +
                abs(colorNextX.blue - colorPrevX.blue).toDouble().pow(2)
    }

    private fun yGradient(x: Int, y: Int, image: BufferedImage): Double {
        val yOfBorder = when (y) {
            0 -> y + 1
            (image.height - 1) -> y - 1
            else -> y
        }
        val colorNextY = Color(image.getRGB(x, yOfBorder + 1))
        val colorPrevY = Color(image.getRGB(x, yOfBorder - 1))
        return abs(colorNextY.red - colorPrevY.red).toDouble().pow(2) +
                abs(colorNextY.green - colorPrevY.green).toDouble().pow(2) +
                abs(colorNextY.blue - colorPrevY.blue).toDouble().pow(2)
    }

    /* Identify the vertical track with the lowest sum of pixel energies out of all possible tracks
    * on the y-axis. Finding the best vertical seam is performed using Dijkstra's algorithm. */
    private fun xTrackOfMinWeight(graph: Array<DoubleArray>) {
        for (x in trackOfWeightGraph[0].indices) {
            trackOfWeightGraph[0][x] = Triple(graph[0][x], 0, x)
        }

        for (y in 1 until graph.size) {
            for (x in graph[0].indices) {
                val xNode = xMinNode(trackOfWeightGraph, x, y)
                trackOfWeightGraph[y][x] = Triple(graph[y][x] + trackOfWeightGraph[y - 1][xNode].first,
                        y - 1, xNode)
            }
        }
    }

    /* Identify the horizontal track with the lowest sum of pixel energies out of all possible tracks
    * on the x-axis. Finding the best vertical seam is performed using Dijkstra's algorithm. */
    private fun yTrackOfMinWeight(graph: Array<DoubleArray>) {
        for (y in trackOfWeightGraph.indices) {
            trackOfWeightGraph[y][0] = Triple(graph[y][0], y, 0)
        }

        for (x in 1 until graph[0].size) {
            for (y in graph.indices) {
                val yNode = yMinNode(trackOfWeightGraph, x, y)
                trackOfWeightGraph[y][x] = Triple(graph[y][x] + trackOfWeightGraph[yNode][x - 1].first,
                        yNode, x - 1)
            }
        }
    }

    private fun xMinNode(weightGraph: Array<Array<Triple<Double, Int, Int>>>, x: Int, y: Int): Int {
        when (x) {
            0 -> {
                return if (weightGraph[y - 1][x].first <= weightGraph[y - 1][x + 1].first) x
                       else x + 1
            }
            weightGraph[0].size - 1 -> {
                return if (weightGraph[y - 1][x - 1].first <= weightGraph[y - 1][x].first) x - 1
                       else x
            }
            else -> {
                return if (weightGraph[y - 1][x - 1].first <= weightGraph[y - 1][x].first &&
                        weightGraph[y - 1][x - 1].first <= weightGraph[y - 1][x + 1].first) {
                    x - 1
                } else if (weightGraph[y - 1][x].first < weightGraph[y - 1][x - 1].first &&
                        weightGraph[y - 1][x].first <= weightGraph[y - 1][x + 1].first) {
                    x
                } else x + 1
            }
        }
    }

    private fun yMinNode(weightGraph: Array<Array<Triple<Double, Int, Int>>>, x: Int, y: Int): Int {
        when (y) {
            0 -> {
                return if (weightGraph[y][x - 1].first <= weightGraph[y + 1][x - 1].first) y
                else y + 1
            }
            weightGraph.size - 1 -> {
                return if (weightGraph[y - 1][x - 1].first <= weightGraph[y][x - 1].first) y - 1
                else y
            }
            else -> {
                return if (weightGraph[y - 1][x - 1].first < weightGraph[y][x - 1].first &&
                        weightGraph[y - 1][x - 1].first < weightGraph[y + 1][x - 1].first) {
                    y - 1
                } else if (weightGraph[y][x - 1].first <= weightGraph[y - 1][x - 1].first &&
                        weightGraph[y][x - 1].first < weightGraph[y + 1][x - 1].first) {
                    y
                } else y + 1
            }
        }
    }
}