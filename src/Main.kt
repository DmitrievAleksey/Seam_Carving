import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    val fileImage = File(args[args.indexOf("-in") + 1])
    val image = ImageIO.read(fileImage)

    /*val image1 = ImageEditor(image)
    image1.createNegativeImage()
    val fileCreateImage1 = File("negative.png")
    ImageIO.write(image1.image,"png", fileCreateImage1)*/

    /*val image2 = ImageEditor(image)
    image2.createGradientImage()
    val fileCreateImage2 = File("gradient.png")
    ImageIO.write(image2.image,"png", fileCreateImage2)*/

    val image3 = ImageEditor(image)
    image3.resizeImage(args[args.indexOf("-width") + 1].toInt(), args[args.indexOf("-height") + 1].toInt())
    val fileImage3 = File(args[args.indexOf("-out") + 1])
    ImageIO.write(image3.image,"png", fileImage3)

}
